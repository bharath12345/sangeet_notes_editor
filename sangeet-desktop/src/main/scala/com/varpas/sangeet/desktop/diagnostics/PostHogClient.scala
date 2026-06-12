package com.varpas.sangeet.desktop.diagnostics

import java.io.InputStream
import java.util.Properties

import scala.jdk.CollectionConverters._

import com.posthog.server.{PostHog, PostHogCaptureOptions, PostHogConfig}

/** Thin façade over the PostHog Java SDK that:
  *   - Returns a `NoopPostHogClient` whenever analytics shouldn't run (no key, opt-out env, init failure) — so call
  *     sites never have to null-check
  *   - Swallows every SDK exception silently — analytics must never crash the editor
  *   - Merges global properties (appVersion, os, etc.) into every capture so dashboards always have version filters
  *
  * Mirrors [[BugReportClient]]'s trait + env-based factory shape; same conventions for env-var override and silent
  * degradation.
  */
trait PostHogClient:
  def capture(event: DesktopEvent): Unit
  def flush(): Unit
  def close(): Unit

object PostHogClient:

  /** PostHog Cloud US endpoint — chosen to keep the desktop project in the same region as the web project (which also
    * uses us.i.posthog.com), so billing/quota lives in one bucket.
    */
  val DefaultHost: String = "https://us.i.posthog.com"

  /** Runtime env var. Overrides the build-time key baked into `posthog.properties` (Phase 10 build.sbt resource
    * generator). Empty/unset means "no runtime override; fall back to the resource".
    */
  val ApiKeyEnv: String = "SANGEET_POSTHOG_API_KEY"

  /** Kill switch. Setting this to `1` / `true` / `yes` forces a noop client even if a key is present. Honored by CI
    * (the GitHub Actions workflow sets it) and by privacy-conscious users.
    */
  val DisabledEnv: String = "SANGEET_ANALYTICS_DISABLED"

  /** Resource baked in at build time by `Compile / resourceGenerators` in build.sbt. Contains `apiKey=<value>` or
    * `apiKey=` (empty) — the latter means the build didn't ship a key, so packaged installs are noop unless the user
    * sets SANGEET_POSTHOG_API_KEY themselves.
    */
  private val ResourcePath = "posthog.properties"

  /** Build a real client if a key is available AND the kill switch isn't set; otherwise return Noop. Never throws. Logs
    * which branch was taken to stderr so the user can see what's going on.
    */
  def fromEnv(distinctId: String, appVersion: String): PostHogClient =
    if isDisabled(sys.env) then
      System.err.println(s"[posthog] Analytics disabled by $DisabledEnv")
      NoopPostHogClient
    else
      resolveApiKey(sys.env, loadResourceApiKey()) match
        case None =>
          System.err.println(s"[posthog] Analytics disabled (no $ApiKeyEnv, no build-time key)")
          NoopPostHogClient
        case Some(apiKey) =>
          buildHttpClient(apiKey, distinctId, appVersion).getOrElse {
            System.err.println("[posthog] Analytics disabled (SDK init failed)")
            NoopPostHogClient
          }

  /** Visible for tests. Decides whether the kill switch is engaged. */
  private[diagnostics] def isDisabled(env: Map[String, String]): Boolean =
    env.get(DisabledEnv).map(_.trim.toLowerCase).exists(v => v == "1" || v == "true" || v == "yes")

  /** Visible for tests. Env-var wins over the build-time resource. Returns None if both are empty. */
  private[diagnostics] def resolveApiKey(env: Map[String, String], resourceKey: Option[String]): Option[String] =
    env.get(ApiKeyEnv).map(_.trim).filter(_.nonEmpty).orElse(resourceKey.map(_.trim).filter(_.nonEmpty))

  private def loadResourceApiKey(): Option[String] =
    try
      val cl: ClassLoader = Option(Thread.currentThread.getContextClassLoader)
        .getOrElse(getClass.getClassLoader)
      val is: InputStream = cl.getResourceAsStream(ResourcePath)
      if is == null then None
      else
        try
          val props = new Properties()
          props.load(is)
          Option(props.getProperty("apiKey")).map(_.trim).filter(_.nonEmpty)
        finally is.close()
    catch case _: Throwable => None

  private def buildHttpClient(apiKey: String, distinctId: String, appVersion: String): Option[PostHogClient] =
    try
      val config = new PostHogConfig.Builder(apiKey).host(DefaultHost).build()
      val sdk    = new PostHog()
      sdk.setup(config)
      val globalProps: Map[String, AnyRef] = Map(
        "appVersion"  -> appVersion,
        "os"          -> sys.props.getOrElse("os.name", "?"),
        "javaVersion" -> sys.props.getOrElse("java.version", "?")
      )
      val sdkAdapter: PostHogSdk = new PostHogSdk:
        def capture(d: String, e: String, o: PostHogCaptureOptions): Unit = sdk.capture(d, e, o)
        def flush(): Unit                                                 = sdk.flush()
        def close(): Unit                                                 = sdk.close()
      System.err.println(s"[posthog] Analytics enabled (distinctId=${distinctId.take(8)}…)")
      Some(new HttpPostHogClient(distinctId, sdkAdapter, globalProps))
    catch case _: Throwable => None

/** Returned whenever analytics shouldn't run. Every call is a no-op so the rest of the app doesn't have to branch.
  */
object NoopPostHogClient extends PostHogClient:
  def capture(event: DesktopEvent): Unit = ()
  def flush(): Unit                      = ()
  def close(): Unit                      = ()

/** Narrow interface over the bits of the PostHog SDK that HttpPostHogClient actually calls. Decouples tests from the
  * SDK's wide surface area (feature flags, identify, etc.) — we never touch those, and stubbing them all in a Scala
  * fake is painful.
  */
trait PostHogSdk:
  def capture(distinctId: String, eventName: String, options: PostHogCaptureOptions): Unit
  def flush(): Unit
  def close(): Unit

/** Real implementation. Takes a [[PostHogSdk]] adapter (built in `fromEnv`) so tests can pass in a fake. */
final class HttpPostHogClient(
    distinctId: String,
    sdk: PostHogSdk,
    globalProps: Map[String, AnyRef]
) extends PostHogClient:

  def capture(event: DesktopEvent): Unit =
    try
      val merged: Map[String, AnyRef] = globalProps ++ event.props
      val opts = new PostHogCaptureOptions.Builder()
        .properties(merged.asJava)
        .build()
      sdk.capture(distinctId, event.name, opts)
    catch case _: Throwable => ()

  def flush(): Unit =
    try sdk.flush()
    catch case _: Throwable => ()

  def close(): Unit =
    try sdk.close()
    catch case _: Throwable => ()
