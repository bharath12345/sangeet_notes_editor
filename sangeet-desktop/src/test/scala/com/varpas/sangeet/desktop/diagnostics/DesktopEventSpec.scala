package com.varpas.sangeet.desktop.diagnostics

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DesktopEventSpec extends AnyFlatSpec with Matchers:

  /** Hand-rolled list of every event we ship — duplicated from the ADT on purpose so the test acts as a contract:
    * adding a new case forces a test update, which forces you to think about whether the new event needs a dashboard.
    */
  private val allEvents: List[DesktopEvent] = List(
    DesktopEvent.AppStarted("0.2.0", "Mac OS X", "14.5", "21.0.5", 1920, 1080),
    DesktopEvent.AppQuit(60_000L, 250),
    DesktopEvent.TabOpened,
    DesktopEvent.CompositionOpened("teentaal", "file-browser"),
    DesktopEvent.CompositionCreated("gat", "jhaptaal"),
    DesktopEvent.CompositionSaved,
    DesktopEvent.CompositionExportedHtml,
    DesktopEvent.SectionAdded,
    DesktopEvent.SectionRemoved,
    DesktopEvent.OrnamentAdded("Gamak"),
    DesktopEvent.ScriptChanged,
    DesktopEvent.ThemeToggled,
    DesktopEvent.PropertiesEdited,
    DesktopEvent.BugReportSent,
    DesktopEvent.CrashRecoverySent,
    DesktopEvent.CrashRecoveryDiscarded,
    DesktopEvent.DialogOpened("about")
  )

  "every DesktopEvent" should "have a non-empty snake_case name" in {
    val snakeCase = "^[a-z][a-z0-9_]*$".r
    allEvents.foreach { e =>
      withClue(s"event ${e.getClass.getSimpleName}: ") {
        e.name should not be empty
        snakeCase.findFirstIn(e.name).isDefined shouldBe true
      }
    }
  }

  it should "have unique names across the ADT" in {
    val names = allEvents.map(_.name)
    names.distinct shouldBe names
  }

  "AppStarted.props" should "expose the platform + screen metadata" in {
    val e = DesktopEvent.AppStarted("0.2.0", "Mac OS X", "14.5", "21.0.5", 1920, 1080)
    e.props.keys should contain allOf ("appVersion", "os", "osVersion", "javaVersion", "screenW", "screenH")
    e.props("appVersion") shouldBe "0.2.0"
    e.props("screenW") shouldBe Integer.valueOf(1920)
  }

  "AppQuit.props" should "expose sessionDurationMs + swarInputCount as numerics" in {
    val e = DesktopEvent.AppQuit(60_000L, 250)
    e.props("sessionDurationMs") shouldBe java.lang.Long.valueOf(60_000L)
    e.props("swarInputCount") shouldBe Integer.valueOf(250)
  }

  "DialogOpened.props" should "carry the dialog name as a property" in {
    DesktopEvent.DialogOpened("about").props shouldBe Map("dialog" -> "about")
  }

  "OrnamentAdded.props" should "carry the ornament type" in {
    DesktopEvent.OrnamentAdded("Gamak").props shouldBe Map("ornamentType" -> "Gamak")
  }
