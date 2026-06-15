package com.varpas.sangeet.core.editor

import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.core.format.{Codecs, SwarFormat}
import com.varpas.sangeet.core.model._

class LockedBeatSpec extends AnyFlatSpec with Matchers:
  import Codecs.given

  val teentaal = Taal(
    "Teentaal",
    16,
    List(
      Vibhag(4, VibhagMarker.Sam),
      Vibhag(4, VibhagMarker.Taali(2)),
      Vibhag(4, VibhagMarker.Khali),
      Vibhag(4, VibhagMarker.Taali(3))
    ),
    None
  )

  val testRaag = Raag("Yaman", None, None, None, None, None, None, None)

  def makeSection(
      name: String,
      events: List[Event] = Nil,
      startingBeat: Int = 1
  ): Section =
    Section(name, SectionType.Sthayi, events, None, startingBeat)

  def swarAt(beat: Int): Event =
    Event.Swar(
      Note.Sa,
      Variant.Shuddha,
      Octave.Madhya,
      BeatPosition(0, beat, Rational.onBeat),
      Rational.fullBeat,
      None,
      Nil,
      None
    )

  // --- Codec roundtrip ---

  "LockedBeat codec" should "roundtrip through JSON" in {
    val event: Event =
      Event.LockedBeat(BeatPosition(0, 3, Rational.onBeat), Rational.fullBeat)
    val json    = event.asJson
    val decoded = json.as[Event]
    decoded shouldBe Right(event)
  }

  it should "serialize with type discriminator 'lockedbeat'" in {
    val event: Event =
      Event.LockedBeat(BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat)
    val json = event.asJson
    json.hcursor.downField("type").as[String] shouldBe Right("lockedbeat")
  }

  it should "include beat and duration fields" in {
    val event: Event =
      Event.LockedBeat(BeatPosition(0, 5, Rational(1, 2)), Rational(1, 4))
    val json = event.asJson
    json.hcursor.downField("beat").as[BeatPosition] shouldBe Right(
      BeatPosition(0, 5, Rational(1, 2))
    )
    json.hcursor.downField("duration").as[Rational] shouldBe Right(
      Rational(1, 4)
    )
  }

  // --- generateLockedBeats ---

  "generateLockedBeats" should "return empty list for startingBeat 1" in {
    CompositionEditor.generateLockedBeats(16, 1) shouldBe Nil
  }

  it should "return empty list for startingBeat 0" in {
    CompositionEditor.generateLockedBeats(16, 0) shouldBe Nil
  }

  it should "generate 8 events for startingBeat 9" in {
    val result = CompositionEditor.generateLockedBeats(16, 9)
    result should have length 8
  }

  it should "place events at beats 0 through startingBeat-2" in {
    val result = CompositionEditor.generateLockedBeats(16, 5)
    result should have length 4
    result.zipWithIndex.foreach { (event, idx) =>
      event match
        case Event.LockedBeat(pos, _) =>
          pos.cycle shouldBe 0
          pos.beat shouldBe idx
          pos.subdivision shouldBe Rational.onBeat
        case other => fail(s"Expected LockedBeat, got $other")
    }
  }

  it should "set duration to full beat" in {
    val result = CompositionEditor.generateLockedBeats(16, 3)
    result.foreach {
      case Event.LockedBeat(_, dur) => dur shouldBe Rational.fullBeat
      case other                    => fail(s"Expected LockedBeat, got $other")
    }
  }

  // --- Deletion guard ---

  def makeEditor(section: Section): CompositionEditor =
    val comp = Composition(
      metadata = Metadata(
        title = "Test",
        compositionType = CompositionType.Gat,
        raag = testRaag,
        taal = teentaal,
        laya = None,
        script = None,
        instrument = None,
        composer = None,
        author = None,
        source = None,
        showStrokeLine = false,
        showSahityaLine = false,
        createdAt = "",
        updatedAt = ""
      ),
      sections = List(section)
    )
    val cursor = CursorModel(taal = teentaal, cycle = 0, beat = 0)
    CompositionEditor(comp, 0, cursor)

  "removeEventAt" should "refuse to delete a LockedBeat event" in {
    val locked  = Event.LockedBeat(BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat)
    val section = makeSection("Test", List(locked, swarAt(1)), startingBeat = 2)
    val editor  = makeEditor(section)
    val result  = editor.removeEventAt(editor.cursor)
    result shouldBe None
  }

  "removeGroupAt" should "refuse to delete a LockedBeat event" in {
    val locked  = Event.LockedBeat(BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat)
    val section = makeSection("Test", List(locked, swarAt(1)), startingBeat = 2)
    val editor  = makeEditor(section)
    val result  = editor.removeGroupAt(editor.cursor)
    result shouldBe None
  }

  // --- changeStartingBeat ---

  "changeStartingBeat" should "insert locked beats when increasing from 1" in {
    val section = makeSection("Test", List(swarAt(0), swarAt(1)), startingBeat = 1)
    val result  = CompositionEditor.changeStartingBeat(section, 5, 16)
    result.startingBeat shouldBe 5
    val lockedCount = result.events.count(_.isInstanceOf[Event.LockedBeat])
    lockedCount shouldBe 4
  }

  it should "shift existing events forward" in {
    val section   = makeSection("Test", List(swarAt(0), swarAt(1)), startingBeat = 1)
    val result    = CompositionEditor.changeStartingBeat(section, 5, 16)
    val nonLocked = result.events.filterNot(_.isInstanceOf[Event.LockedBeat])
    nonLocked.foreach { event =>
      val pos = event.position
      pos.beat should be >= 4
    }
  }

  it should "remove locked beats when decreasing" in {
    val locked  = CompositionEditor.generateLockedBeats(16, 9)
    val section = makeSection("Test", locked ++ List(swarAt(8), swarAt(9)), startingBeat = 9)
    val result  = CompositionEditor.changeStartingBeat(section, 5, 16)
    result.startingBeat shouldBe 5
    val lockedCount = result.events.count(_.isInstanceOf[Event.LockedBeat])
    lockedCount shouldBe 4
  }

  it should "shift events backward when decreasing" in {
    val locked    = CompositionEditor.generateLockedBeats(16, 9)
    val section   = makeSection("Test", locked ++ List(swarAt(8), swarAt(9)), startingBeat = 9)
    val result    = CompositionEditor.changeStartingBeat(section, 5, 16)
    val nonLocked = result.events.filterNot(_.isInstanceOf[Event.LockedBeat])
    nonLocked.foreach { event =>
      val pos = event.position
      pos.beat should be >= 4
    }
  }

  it should "be a no-op when startingBeat doesn't change" in {
    val locked  = CompositionEditor.generateLockedBeats(16, 5)
    val section = makeSection("Test", locked ++ List(swarAt(4)), startingBeat = 5)
    val result  = CompositionEditor.changeStartingBeat(section, 5, 16)
    result shouldBe section
  }

  // --- Migration on load ---

  "SwarFormat migration" should "inject LockedBeat events for old files with startingBeat > 1" in {
    val section = makeSection("Gat", List(swarAt(8)), startingBeat = 9)
    val comp = Composition(
      metadata = Metadata(
        title = "Old File",
        compositionType = CompositionType.Gat,
        raag = testRaag,
        taal = teentaal,
        laya = None,
        script = None,
        instrument = None,
        composer = None,
        author = None,
        source = None,
        showStrokeLine = false,
        showSahityaLine = false,
        createdAt = "",
        updatedAt = ""
      ),
      sections = List(section)
    )
    val json              = SwarFormat.toJson(comp)
    val jsonStr           = json.noSpaces
    val jsonWithoutLocked = jsonStr.replaceAll("""\{"type":"lockedbeat"[^}]*\},?""", "")
    val result            = SwarFormat.fromJson(jsonWithoutLocked)
    result.isRight shouldBe true
    val migrated    = result.toOption.get
    val lockedCount = migrated.sections.head.events.count(_.isInstanceOf[Event.LockedBeat])
    lockedCount shouldBe 8
  }

  it should "not double-inject on already migrated files" in {
    val locked  = CompositionEditor.generateLockedBeats(16, 5)
    val section = makeSection("Gat", locked ++ List(swarAt(4)), startingBeat = 5)
    val comp = Composition(
      metadata = Metadata(
        title = "Migrated File",
        compositionType = CompositionType.Gat,
        raag = testRaag,
        taal = teentaal,
        laya = None,
        script = None,
        instrument = None,
        composer = None,
        author = None,
        source = None,
        showStrokeLine = false,
        showSahityaLine = false,
        createdAt = "",
        updatedAt = ""
      ),
      sections = List(section)
    )
    val json   = SwarFormat.toJson(comp)
    val result = SwarFormat.fromJson(json.noSpaces)
    result.isRight shouldBe true
    val loaded      = result.toOption.get
    val lockedCount = loaded.sections.head.events.count(_.isInstanceOf[Event.LockedBeat])
    lockedCount shouldBe 4
  }

  it should "not inject for sections with startingBeat 1" in {
    val section = makeSection("Sthayi", List(swarAt(0)), startingBeat = 1)
    val comp = Composition(
      metadata = Metadata(
        title = "Normal File",
        compositionType = CompositionType.Bandish,
        raag = testRaag,
        taal = teentaal,
        laya = None,
        script = None,
        instrument = None,
        composer = None,
        author = None,
        source = None,
        showStrokeLine = false,
        showSahityaLine = false,
        createdAt = "",
        updatedAt = ""
      ),
      sections = List(section)
    )
    val json   = SwarFormat.toJson(comp)
    val result = SwarFormat.fromJson(json.noSpaces)
    result.isRight shouldBe true
    val loaded      = result.toOption.get
    val lockedCount = loaded.sections.head.events.count(_.isInstanceOf[Event.LockedBeat])
    lockedCount shouldBe 0
  }
