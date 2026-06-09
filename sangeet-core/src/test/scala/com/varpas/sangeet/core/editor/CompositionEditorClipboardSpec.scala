package com.varpas.sangeet.core.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.core.model._

class CompositionEditorClipboardSpec extends AnyFlatSpec with Matchers:

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

  val raag = Raag("Yaman", None, None, None, None, None, None, None)

  def makeSwar(beat: Int, note: Note = Note.Sa): Event.Swar =
    Event.Swar(
      note,
      Variant.Shuddha,
      Octave.Madhya,
      BeatPosition(0, beat, Rational.onBeat),
      Rational.fullBeat,
      None,
      Nil,
      None
    )

  def editorWithEvents(events: List[Event]): CompositionEditor =
    val section = Section("Test", SectionType.Sthayi, events)
    val metadata =
      Metadata("Test", CompositionType.Gat, raag, teentaal, None, None, None, None, None, false, false, "", "")
    val composition = Composition(metadata, List(section))
    CompositionEditor(composition, 0, CursorModel(teentaal))

  "eventsInRange" should "return events within the range" in {
    val events = List(makeSwar(0), makeSwar(1, Note.Re), makeSwar(2, Note.Ga), makeSwar(3, Note.Ma))
    val editor = editorWithEvents(events)
    val result = editor.eventsInRange(BeatPosition(0, 1, Rational.onBeat), BeatPosition(0, 2, Rational.onBeat))
    result.size shouldBe 2
    result.head.asInstanceOf[Event.Swar].note shouldBe Note.Re
    result(1).asInstanceOf[Event.Swar].note shouldBe Note.Ga
  }

  it should "return empty list when no events in range" in {
    val events = List(makeSwar(0), makeSwar(5))
    val editor = editorWithEvents(events)
    val result = editor.eventsInRange(BeatPosition(0, 2, Rational.onBeat), BeatPosition(0, 4, Rational.onBeat))
    result shouldBe empty
  }

  it should "include boundary events" in {
    val events = List(makeSwar(1), makeSwar(2), makeSwar(3))
    val editor = editorWithEvents(events)
    val result = editor.eventsInRange(BeatPosition(0, 1, Rational.onBeat), BeatPosition(0, 3, Rational.onBeat))
    result.size shouldBe 3
  }

  "cutRange" should "remove events and return them" in {
    val events = List(makeSwar(0), makeSwar(1, Note.Re), makeSwar(2, Note.Ga), makeSwar(3, Note.Ma))
    val editor = editorWithEvents(events)
    val (newEditor, cutEvents) =
      editor.cutRange(BeatPosition(0, 1, Rational.onBeat), BeatPosition(0, 2, Rational.onBeat))
    cutEvents.size shouldBe 2
    newEditor.currentSection.events.size shouldBe 2
  }

  it should "shift subsequent events backward" in {
    val events         = List(makeSwar(0), makeSwar(1, Note.Re), makeSwar(2, Note.Ga), makeSwar(3, Note.Ma))
    val editor         = editorWithEvents(events)
    val (newEditor, _) = editor.cutRange(BeatPosition(0, 1, Rational.onBeat), BeatPosition(0, 1, Rational.onBeat))
    val remaining      = newEditor.currentSection.events
    remaining.size shouldBe 3
    remaining(0).position.beat shouldBe 0
  }

  "pasteEvents" should "insert events at cursor position" in {
    val events        = List(makeSwar(0))
    val editor        = editorWithEvents(events)
    val eventsToPaste = List(makeSwar(0, Note.Re), makeSwar(1, Note.Ga))
    val newEditor     = editor.pasteEvents(eventsToPaste, BeatPosition(0, 1, Rational.onBeat))
    newEditor.currentSection.events.size shouldBe 3
  }

  it should "shift existing events forward" in {
    val events    = List(makeSwar(0, Note.Sa), makeSwar(1, Note.Re))
    val editor    = editorWithEvents(events)
    val toPaste   = List(makeSwar(0, Note.Ga))
    val newEditor = editor.pasteEvents(toPaste, BeatPosition(0, 1, Rational.onBeat))
    val result    = newEditor.currentSection.events.sortBy(_.position)
    result.size shouldBe 3
    result(0).asInstanceOf[Event.Swar].note shouldBe Note.Sa
    result(0).position.beat shouldBe 0
    result(1).asInstanceOf[Event.Swar].note shouldBe Note.Ga
    result(1).position.beat shouldBe 1
    result(2).asInstanceOf[Event.Swar].note shouldBe Note.Re
    result(2).position.beat shouldBe 2
  }

  it should "handle empty paste" in {
    val events    = List(makeSwar(0))
    val editor    = editorWithEvents(events)
    val newEditor = editor.pasteEvents(Nil, BeatPosition(0, 1, Rational.onBeat))
    newEditor.currentSection.events.size shouldBe 1
  }

  it should "rebase pasted events relative to insert position" in {
    val events    = List(makeSwar(0, Note.Sa))
    val editor    = editorWithEvents(events)
    val toPaste   = List(makeSwar(5, Note.Re), makeSwar(6, Note.Ga))
    val newEditor = editor.pasteEvents(toPaste, BeatPosition(0, 2, Rational.onBeat))
    val result    = newEditor.currentSection.events.sortBy(_.position)
    result(1).position.beat shouldBe 2
    result(2).position.beat shouldBe 3
  }

  "copy-paste roundtrip" should "preserve events correctly" in {
    val events    = List(makeSwar(0, Note.Sa), makeSwar(1, Note.Re), makeSwar(2, Note.Ga), makeSwar(3, Note.Ma))
    val editor    = editorWithEvents(events)
    val copied    = editor.eventsInRange(BeatPosition(0, 0, Rational.onBeat), BeatPosition(0, 1, Rational.onBeat))
    val newEditor = editor.pasteEvents(copied, BeatPosition(0, 4, Rational.onBeat))
    val result    = newEditor.currentSection.events.sortBy(_.position)
    result.size shouldBe 6
    result(4).asInstanceOf[Event.Swar].note shouldBe Note.Sa
    result(5).asInstanceOf[Event.Swar].note shouldBe Note.Re
  }

  "cut-paste roundtrip" should "move events to new position" in {
    val events = List(makeSwar(0, Note.Sa), makeSwar(1, Note.Re), makeSwar(2, Note.Ga), makeSwar(3, Note.Ma))
    val editor = editorWithEvents(events)
    val (cutEditor, cutEvents) =
      editor.cutRange(BeatPosition(0, 0, Rational.onBeat), BeatPosition(0, 1, Rational.onBeat))
    val newEditor = cutEditor.pasteEvents(cutEvents, BeatPosition(0, 2, Rational.onBeat))
    newEditor.currentSection.events.size shouldBe 4
  }
