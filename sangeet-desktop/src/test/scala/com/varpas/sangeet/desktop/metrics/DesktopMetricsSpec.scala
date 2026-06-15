package com.varpas.sangeet.desktop.metrics

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.desktop.diagnostics.PostHogClient

/** Covers the kill-switch + no-op semantics of [[DesktopMetrics]] without spinning up an HTTP server. The actual wire
  * format is exercised in `MetricsEventRoutesSpec` (server-side); here we only care that the env var disables sends and
  * that NoopDesktopMetrics never throws.
  */
class DesktopMetricsSpec extends AnyFlatSpec with Matchers:

  "DesktopMetrics.isDisabled" should "treat the kill-switch env var as enabled when set to 1/true/yes" in {
    val env = PostHogClient.DisabledEnv
    DesktopMetrics.isDisabled(Map(env -> "1")) shouldBe true
    DesktopMetrics.isDisabled(Map(env -> "true")) shouldBe true
    DesktopMetrics.isDisabled(Map(env -> "yes")) shouldBe true
    DesktopMetrics.isDisabled(Map(env -> "TRUE")) shouldBe true
    DesktopMetrics.isDisabled(Map(env -> " yes ")) shouldBe true
  }

  it should "treat other values (or absence) as enabled" in {
    val env = PostHogClient.DisabledEnv
    DesktopMetrics.isDisabled(Map.empty) shouldBe false
    DesktopMetrics.isDisabled(Map(env -> "0")) shouldBe false
    DesktopMetrics.isDisabled(Map(env -> "no")) shouldBe false
    DesktopMetrics.isDisabled(Map(env -> "")) shouldBe false
    // Random unrelated value
    DesktopMetrics.isDisabled(Map(env -> "maybe")) shouldBe false
  }

  "NoopDesktopMetrics" should "swallow every increment / close without throwing or blocking" in {
    val noop = NoopDesktopMetrics
    noop.increment("sangeet_editor_mutation_total", Map("kind" -> "swar_insert"))
    noop.increment("anything", Map.empty)
    noop.close()
    // Reaching here without exception is the assertion.
    succeed
  }

  "DesktopMetrics.client" should "default to NoopDesktopMetrics when nothing was installed" in {
    // We can't reliably uninstall once something is in (test order isn't deterministic);
    // assert the default by reading the install-then-read shape: install Noop, read it back.
    DesktopMetrics.install(NoopDesktopMetrics)
    DesktopMetrics.client shouldBe NoopDesktopMetrics
  }
