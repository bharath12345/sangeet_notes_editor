package com.varpas.sangeet.desktop.diagnostics

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.time.{Duration, Instant}

import io.circe.Json
import io.circe.parser.parse

/** POSTs a bug-report payload to the same `/api/v1/bug-reports` endpoint the web client uses (Phase 5a). Reuses the JDK
  * 11 `java.net.http.HttpClient` rather than pulling in a Scala HTTP library — bug-report volume is at most
  * single-digit per day, the simple blocking call is fine.
  *
  * The payload shape mirrors what the web sends but is `type: "desktop"`. The server's `IssueBuilder` derives the
  * `platform-desktop` label from this so issues are filterable.
  */
trait BugReportClient:
  def submit(payload: BugReportPayload): Either[String, String]

object BugReportClient:

  /** Default endpoint — same Cloud Run revision the web bundle posts to. Override with `SANGEET_API_BASE_URL` env var
    * when developing against a local backend.
    */
  val DefaultApiBaseUrl: String =
    "https://sangeet-server-729103223940.asia-south1.run.app/api/v1"

  def fromEnv: BugReportClient =
    val base = sys.env.getOrElse("SANGEET_API_BASE_URL", DefaultApiBaseUrl).stripSuffix("/")
    new HttpBugReportClient(base)

/** Inputs that the caller (the dialog Send handler) gathers. Kept as a flat case class so tests can construct it
  * without a JavaFX runtime.
  */
final case class BugReportPayload(
    description: String,
    email: Option[String],
    eventLog: List[Json],
    composition: Option[Json],
    screenshotPngBase64: Option[String],
    metadata: BugReportMetadata,
    crashTrigger: Boolean = false
):
  /** Mark this payload as originating from auto-crash capture (next-launch recovery flow). The server's IssueBuilder
    * reads this flag and adds the `crash` label so crash reports are filterable in the issue tracker.
    */
  def withCrashTrigger: BugReportPayload = copy(crashTrigger = true)

  def toJson: Json =
    val base = Json.obj(
      "type"        -> Json.fromString("desktop"),
      "description" -> Json.fromString(description),
      "email"       -> email.fold(Json.Null)(Json.fromString),
      "replay"      -> Json.fromValues(eventLog),
      "metadata"    -> metadata.toJson
    )
    val withComp = composition.fold(base)(c => base.deepMerge(Json.obj("composition" -> c)))
    val withShot =
      screenshotPngBase64.fold(withComp)(s => withComp.deepMerge(Json.obj("screenshot" -> Json.fromString(s))))
    if crashTrigger then withShot.deepMerge(Json.obj("crashTrigger" -> Json.fromBoolean(true))) else withShot

/** Captured at submit time. Source of truth for "what environment was this report filed from". */
final case class BugReportMetadata(
    osName: String,
    osVersion: String,
    javaVersion: String,
    screenWidth: Int,
    screenHeight: Int,
    appVersion: String,
    timestamp: String
):
  def toJson: Json = Json.obj(
    "os"          -> Json.fromString(s"$osName $osVersion"),
    "javaVersion" -> Json.fromString(javaVersion),
    "viewportW"   -> Json.fromInt(screenWidth),
    "viewportH"   -> Json.fromInt(screenHeight),
    "appVersion"  -> Json.fromString(appVersion),
    "timestamp"   -> Json.fromString(timestamp)
  )

object BugReportMetadata:

  /** Build from the JVM's current state — call this from the Send handler. `screenWidth`/`screenHeight` are taken from
    * JavaFX's Screen.getPrimary so the caller doesn't have to thread it through. `appVersion` should be set by Main
    * from the manifest; for MVP we hardcode.
    */
  def current(appVersion: String, screenW: Int, screenH: Int): BugReportMetadata =
    BugReportMetadata(
      osName = sys.props.getOrElse("os.name", "unknown"),
      osVersion = sys.props.getOrElse("os.version", "unknown"),
      javaVersion = sys.props.getOrElse("java.version", "unknown"),
      screenWidth = screenW,
      screenHeight = screenH,
      appVersion = appVersion,
      timestamp = Instant.now().toString
    )

/** Real impl. Synchronous send wrapped at the call site (the dialog Send handler runs on a background fx Task). */
final class HttpBugReportClient(apiBaseUrl: String) extends BugReportClient:

  private val client: HttpClient =
    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

  def submit(payload: BugReportPayload): Either[String, String] =
    val body = payload.toJson.noSpaces
    val req = HttpRequest
      .newBuilder()
      .uri(URI.create(s"$apiBaseUrl/bug-reports"))
      .timeout(Duration.ofSeconds(60)) // large payload + slow Cloud Run cold-start
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .header("User-Agent", "sangeet-desktop-bug-reporter")
      .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
      .build()

    try
      val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
      if resp.statusCode() == 200 then
        parse(resp.body())
          .flatMap(_.hcursor.get[String]("reportId"))
          .left
          .map(err => s"Server returned 200 but no reportId: ${err.getMessage}")
      else
        val snippet = Option(resp.body()).getOrElse("").take(300)
        Left(s"HTTP ${resp.statusCode()}: $snippet")
    catch case t: Throwable => Left(s"${t.getClass.getSimpleName}: ${t.getMessage}")
