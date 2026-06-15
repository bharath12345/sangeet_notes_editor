package com.varpas.sangeet.desktop.metrics

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.{ExecutorService, Executors}

import io.circe.Json

import com.varpas.sangeet.core.api.MetricsEvent

/** Fire-and-forget POST client for `sangeet-server`'s `/api/v1/metrics/event` endpoint (Plan 18 PR-3b).
  *
  * Mirrors web's `Api.Metrics.incrementCounter` so server-side dashboards aggregate desktop + web traffic on the same
  * counters. The wire format ([[MetricsEvent]]) is shared via sangeet-core so both clients can't drift from the
  * server's whitelist independently.
  *
  * Design constraints baked in:
  *   - **Never blocks the UI thread.** Every increment is submitted to a dedicated single-threaded daemon executor
  *     (matches BugReportClient's pattern) and the actual HTTP send happens off-thread.
  *   - **Never crashes.** Every error path — connection refused, timeout, non-2xx response, JSON write failure — is
  *     swallowed. Telemetry must never disrupt the editor.
  *   - **Honors the kill switch.** Reuses `PostHogClient.DisabledEnv` so a single env var turns off ALL analytics
  *     (PostHog + AppMetrics). The CI workflow already sets it, so we don't generate fake telemetry from headless test
  *     runs.
  *   - **Optional, but on by default.** No AppConfig field — matches PostHog's posture documented in the project plan.
  *     Users who want to opt out flip the env var.
  *
  * Lifecycle: instantiated once by `MainApp`, kept for the JVM's lifetime. The executor is a daemon so it never
  * prevents JVM exit; callers shouldn't bother calling [[close]] on app shutdown but it's harmless if they do.
  */
trait DesktopMetrics:
  def increment(counter: String, labels: Map[String, String]): Unit
  def close(): Unit

object DesktopMetrics:

  /** Default endpoint — same Cloud Run revision the BugReportClient + web bundle posts to. Override with
    * `SANGEET_API_BASE_URL` env var when developing against a local backend (e.g. `http://localhost:28080/api/v1`).
    */
  val DefaultApiBaseUrl: String =
    "https://sangeet-server-729103223940.asia-south1.run.app/api/v1"

  /** Build from env. Returns a real client unless the kill switch is engaged; in that case returns a noop. Never
    * throws.
    */
  def fromEnv: DesktopMetrics =
    if isDisabled(sys.env) then
      System.err.println(
        s"[metrics] App metrics disabled by ${com.varpas.sangeet.desktop.diagnostics.PostHogClient.DisabledEnv}"
      )
      NoopDesktopMetrics
    else
      val base = sys.env.getOrElse("SANGEET_API_BASE_URL", DefaultApiBaseUrl).stripSuffix("/")
      new HttpDesktopMetrics(base)

  /** Visible for tests. Same env var as PostHog so users get one knob, not two. */
  private[metrics] def isDisabled(env: Map[String, String]): Boolean =
    env
      .get(com.varpas.sangeet.desktop.diagnostics.PostHogClient.DisabledEnv)
      .map(_.trim.toLowerCase)
      .exists(v => v == "1" || v == "true" || v == "yes")

  /** Singleton accessor — install via [[install]] from MainApp at startup, then any call site can call
    * `DesktopMetrics.client.increment(...)` without having the dep threaded through 5 constructors.
    *
    * Mirrors the EventLogger object's "one process, one instance" assumption — the editor only ever has a single JavaFX
    * runtime. Tests can install a fake by calling [[install]] with their own implementation.
    *
    * Defaults to NoopDesktopMetrics if nothing was installed, so a test that exercises code calling `client.increment`
    * without running MainApp first won't NPE — it just silently no-ops, the same as a production install that hit the
    * kill switch.
    */
  @volatile private var instance: DesktopMetrics = NoopDesktopMetrics

  def install(c: DesktopMetrics): Unit = instance = c
  def client: DesktopMetrics           = instance

/** Returned whenever metrics shouldn't run. Every call is a no-op so the rest of the app doesn't have to branch. */
object NoopDesktopMetrics extends DesktopMetrics:
  def increment(counter: String, labels: Map[String, String]): Unit = ()
  def close(): Unit                                                 = ()

/** Real HTTP implementation. All actual work happens on a daemon executor — `increment` returns to the caller within
  * microseconds (just an `executor.submit`).
  */
final class HttpDesktopMetrics(apiBaseUrl: String) extends DesktopMetrics:

  // Short timeouts: this is fire-and-forget, we don't want a hung connection
  // to back up the executor with stalled work. 100ms connect + 1s read is
  // plenty for a counter POST (the server has no meaningful work beyond a
  // micrometer increment).
  private val client: HttpClient =
    HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build()

  // Single-threaded daemon executor: ordered sends, no chance of starving
  // the UI thread, and the JVM exits cleanly even if metrics are mid-flight.
  // BugReportClient uses the same pattern.
  private val executor: ExecutorService = Executors.newSingleThreadExecutor { r =>
    val t = new Thread(r, "desktop-metrics")
    t.setDaemon(true)
    t
  }

  def increment(counter: String, labels: Map[String, String]): Unit =
    // The submit itself is non-blocking. The Runnable runs on the daemon
    // thread; any failure inside is swallowed there. If the executor is
    // already shut down (close was called), submit will throw — wrap in
    // try/catch so a late-fired metric after shutdown doesn't propagate.
    try
      executor.submit(
        new Runnable:
          def run(): Unit = send(counter, labels)
      ): Unit
    catch case _: Throwable => ()

  private def send(counter: String, labels: Map[String, String]): Unit =
    try
      val event = MetricsEvent(counter, labels)
      // Hand-rolled JSON encode is fine here — MetricsEvent has a circe codec
      // but importing it just to encode 2 fields adds a Codec import surface
      // we don't otherwise need on the desktop side, and the wire format is
      // trivially stable.
      val labelsJson = Json
        .obj(labels.toSeq.map { case (k, v) => k -> Json.fromString(v) }*)
      val body = Json
        .obj(
          "counter" -> Json.fromString(counter),
          "labels"  -> labelsJson
        )
        .noSpaces
      // Touch `event` so an unused-variable lint stays quiet AND we get a
      // compile error if the shared model ever changes shape incompatibly.
      val _ = event

      val req = HttpRequest
        .newBuilder()
        .uri(URI.create(s"$apiBaseUrl/metrics/event"))
        .timeout(Duration.ofSeconds(1))
        .header("Content-Type", "application/json")
        .header("User-Agent", "sangeet-desktop-metrics")
        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
        .build()
      // discarding(): we don't care about the response body. 204 expected.
      // Any non-2xx is silently dropped — there's nothing the user could do
      // about it, and we'd rather lose telemetry than disrupt the editor.
      val _ = client.send(req, HttpResponse.BodyHandlers.discarding())
    catch case _: Throwable => ()

  def close(): Unit =
    try executor.shutdown()
    catch case _: Throwable => ()
