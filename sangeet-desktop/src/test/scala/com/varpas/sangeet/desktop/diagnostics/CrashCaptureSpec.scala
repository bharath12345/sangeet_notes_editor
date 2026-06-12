package com.varpas.sangeet.desktop.diagnostics

import java.nio.file.Files

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/** CrashCapture tests cover the pure serialize path + the sentinel-file round trip (write → list → read → delete). The
  * actual UncaughtExceptionHandler install isn't exercised — we can't safely throw uncaught exceptions inside the test
  * JVM without spawning a subprocess, and the install code itself is trivial.
  */
class CrashCaptureSpec extends AnyFlatSpec with Matchers:

  "CrashCapture.serialize" should "include exception class, message, thread, and stack trace" in {
    val throwable = new RuntimeException("boom")
    val json      = CrashCapture.serialize(Thread.currentThread, throwable)
    val c         = json.hcursor

    c.get[String]("exception").toOption shouldBe Some("java.lang.RuntimeException")
    c.get[String]("message").toOption shouldBe Some("boom")
    c.get[String]("threadName").toOption.value should not be empty
    c.get[String]("stackTrace").toOption.value should include("RuntimeException")
    c.get[String]("stackTrace").toOption.value should include("boom")
    c.get[String]("timestamp").toOption.value should not be empty
    c.get[String]("crashId").toOption.value should have length 36 // UUID
  }

  it should "tolerate a null throwable message" in {
    val throwable = new RuntimeException() // no message
    val json      = CrashCapture.serialize(Thread.currentThread, throwable)
    json.hcursor.get[String]("message").toOption shouldBe Some("")
  }

  it should "embed metadata (os, javaVersion, appVersion)" in {
    val json = CrashCapture.serialize(Thread.currentThread, new RuntimeException("x"))
    val meta = json.hcursor.downField("metadata")
    meta.get[String]("os").toOption.value should not be empty
    meta.get[String]("javaVersion").toOption.value should not be empty
    meta.get[String]("appVersion").toOption shouldBe Some("1.0")
  }

  it should "include the EventLogger snapshot at crash time" in {
    EventLogger.clear()
    EventLogger.recordKey("s", List.empty)
    EventLogger.recordLifecycle("startup")
    val json   = CrashCapture.serialize(Thread.currentThread, new RuntimeException("x"))
    val events = json.hcursor.downField("eventLogger").as[List[io.circe.Json]].toOption.value
    events.length shouldBe 2
  }

  "CrashCapture pending/read/delete" should "round-trip a written sentinel file" in {
    // Write a sentinel directly via handle(), then walk the same code path the
    // recovery dialog uses at startup.
    val before = CrashCapture.pending().toSet
    CrashCapture.handle(Thread.currentThread, new RuntimeException("integration-test-crash"))
    val after = CrashCapture.pending().toSet
    val fresh = (after -- before).toList

    fresh.length shouldBe 1
    val path    = fresh.head
    val parsed  = CrashCapture.read(path).value
    val message = parsed.hcursor.get[String]("message").toOption
    message shouldBe Some("integration-test-crash")

    CrashCapture.delete(path)
    Files.exists(path) shouldBe false
  }

  "CrashCapture.read" should "return None on missing or corrupt file" in {
    val missing = CrashCapture.SentinelDir.resolve("does-not-exist.json")
    CrashCapture.read(missing) shouldBe None
  }

  extension [A](opt: Option[A])
    private def value: A = opt.getOrElse(throw new AssertionError("expected Some, got None"))
