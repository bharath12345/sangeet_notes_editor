package com.varpas.sangeet.desktop.diagnostics

import java.util.concurrent.atomic.AtomicInteger

import scala.jdk.CollectionConverters._

import com.posthog.server.PostHogCaptureOptions
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PostHogClientSpec extends AnyFlatSpec with Matchers:

  "NoopPostHogClient" should "swallow capture/flush/close without throwing" in {
    val client = NoopPostHogClient
    noException should be thrownBy client.capture(DesktopEvent.BugReportSent)
    noException should be thrownBy client.flush()
    noException should be thrownBy client.close()
  }

  "PostHogClient.isDisabled" should "treat 1/true/yes as disabled and other values as enabled" in {
    PostHogClient.isDisabled(Map(PostHogClient.DisabledEnv -> "1")) shouldBe true
    PostHogClient.isDisabled(Map(PostHogClient.DisabledEnv -> "true")) shouldBe true
    PostHogClient.isDisabled(Map(PostHogClient.DisabledEnv -> "YES")) shouldBe true
    PostHogClient.isDisabled(Map(PostHogClient.DisabledEnv -> "0")) shouldBe false
    PostHogClient.isDisabled(Map(PostHogClient.DisabledEnv -> "")) shouldBe false
    PostHogClient.isDisabled(Map.empty) shouldBe false
  }

  "PostHogClient.resolveApiKey" should "prefer the env var over the resource" in {
    val env      = Map(PostHogClient.ApiKeyEnv -> "phc_env_key")
    val resource = Some("phc_resource_key")
    PostHogClient.resolveApiKey(env, resource) shouldBe Some("phc_env_key")
  }

  it should "fall back to the resource when the env var is unset or empty" in {
    PostHogClient.resolveApiKey(Map.empty, Some("phc_resource_key")) shouldBe Some("phc_resource_key")
    PostHogClient.resolveApiKey(
      Map(PostHogClient.ApiKeyEnv -> "   "),
      Some("phc_resource_key")
    ) shouldBe Some("phc_resource_key")
  }

  it should "return None when both are absent" in {
    PostHogClient.resolveApiKey(Map.empty, None) shouldBe None
    PostHogClient.resolveApiKey(Map(PostHogClient.ApiKeyEnv -> ""), Some("")) shouldBe None
  }

  "HttpPostHogClient.capture" should "merge global props with event props and forward to the SDK" in {
    val fake   = new RecordingSdk()
    val global = Map[String, AnyRef]("appVersion" -> "0.2.0", "os" -> "Mac OS X")
    val client = new HttpPostHogClient("distinct-1", fake, global)

    client.capture(DesktopEvent.OrnamentAdded("Gamak"))

    fake.captures.size shouldBe 1
    val rec = fake.captures.get(0)
    rec.distinctId shouldBe "distinct-1"
    rec.eventName shouldBe "ornament_added"
    val props = rec.options.getProperties.asScala
    props("appVersion") shouldBe "0.2.0"
    props("os") shouldBe "Mac OS X"
    props("ornamentType") shouldBe "Gamak"
  }

  it should "swallow exceptions thrown by the SDK" in {
    val fake   = new RecordingSdk(throwOnCapture = true)
    val client = new HttpPostHogClient("distinct-1", fake, Map.empty)
    noException should be thrownBy client.capture(DesktopEvent.BugReportSent)
  }

  "HttpPostHogClient.flush/close" should "delegate to the SDK and swallow exceptions" in {
    val fake   = new RecordingSdk()
    val client = new HttpPostHogClient("d", fake, Map.empty)
    client.flush()
    client.close()
    fake.flushCount.get shouldBe 1
    fake.closeCount.get shouldBe 1

    val throwy = new RecordingSdk(throwOnFlush = true, throwOnClose = true)
    val c2     = new HttpPostHogClient("d", throwy, Map.empty)
    noException should be thrownBy c2.flush()
    noException should be thrownBy c2.close()
  }

  /** Minimal fake of the narrow [[PostHogSdk]] adapter. */
  private class RecordingSdk(
      throwOnCapture: Boolean = false,
      throwOnFlush: Boolean = false,
      throwOnClose: Boolean = false
  ) extends PostHogSdk:
    final case class Captured(distinctId: String, eventName: String, options: PostHogCaptureOptions)
    val captures: java.util.ArrayList[Captured] = new java.util.ArrayList()
    val flushCount: AtomicInteger               = new AtomicInteger(0)
    val closeCount: AtomicInteger               = new AtomicInteger(0)

    def capture(distinctId: String, eventName: String, options: PostHogCaptureOptions): Unit =
      if throwOnCapture then throw new RuntimeException("simulated")
      captures.add(Captured(distinctId, eventName, options))

    def flush(): Unit =
      flushCount.incrementAndGet()
      if throwOnFlush then throw new RuntimeException("simulated")

    def close(): Unit =
      closeCount.incrementAndGet()
      if throwOnClose then throw new RuntimeException("simulated")
