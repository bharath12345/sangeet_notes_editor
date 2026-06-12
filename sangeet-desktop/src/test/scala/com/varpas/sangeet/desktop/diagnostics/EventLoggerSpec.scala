package com.varpas.sangeet.desktop.diagnostics

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EventLoggerSpec extends AnyFlatSpec with Matchers:

  // EventLogger is a singleton — tests share state. Each test clears first.
  // In practice this is fine because tests run sequentially within a suite.

  "EventLogger" should "record key events into the buffer" in {
    EventLogger.clear()
    EventLogger.recordKey("s", List.empty)
    EventLogger.recordKey("r", List("Shift"))
    EventLogger.size shouldBe 2

    val snap = EventLogger.snapshot()
    snap.length shouldBe 2
    snap.head.hcursor.get[String]("type").toOption shouldBe Some("key")
    snap.head.hcursor.get[String]("code").toOption shouldBe Some("s")
    snap(1).hcursor.get[String]("code").toOption shouldBe Some("r")
    snap(1).hcursor.downField("modifiers").as[List[String]].toOption shouldBe Some(List("Shift"))
  }

  it should "record lifecycle events with optional detail" in {
    EventLogger.clear()
    EventLogger.recordLifecycle("startup")
    EventLogger.recordLifecycle("file-open", Some("yaman-vilambit.swar"))

    val snap = EventLogger.snapshot()
    snap.length shouldBe 2
    snap.head.hcursor.get[String]("type").toOption shouldBe Some("lifecycle")
    snap.head.hcursor.get[String]("kind").toOption shouldBe Some("startup")
    snap.head.hcursor.get[String]("detail").toOption shouldBe None

    snap(1).hcursor.get[String]("detail").toOption shouldBe Some("yaman-vilambit.swar")
  }

  it should "evict events beyond the hard cap of 5000" in {
    EventLogger.clear()
    // Fire 5100 events; only the most-recent 5000 should survive.
    (1 to 5100).foreach(i => EventLogger.recordKey(s"k$i", List.empty))
    EventLogger.size shouldBe 5000

    // The first 100 entries should have been evicted; head should now be k101.
    val snap = EventLogger.snapshot()
    snap.head.hcursor.get[String]("code").toOption shouldBe Some("k101")
    snap.last.hcursor.get[String]("code").toOption shouldBe Some("k5100")
  }

  it should "produce a JSON-serializable snapshot suitable for the bug-report payload" in {
    EventLogger.clear()
    EventLogger.recordKey("Enter", List("Ctrl"))
    val first = EventLogger.snapshot().head

    // Required fields for the desktop payload schema.
    first.hcursor.get[String]("type").toOption shouldBe Some("key")
    first.hcursor.get[Long]("timestamp").toOption.isDefined shouldBe true
    first.hcursor.get[String]("code").toOption shouldBe Some("Enter")
    first.hcursor.downField("modifiers").as[List[String]].toOption shouldBe Some(List("Ctrl"))
  }

  it should "snapshot returns an independent copy that doesn't reflect later writes" in {
    EventLogger.clear()
    EventLogger.recordKey("a", List.empty)
    val snap = EventLogger.snapshot()
    EventLogger.recordKey("b", List.empty)
    snap.length shouldBe 1
  }
