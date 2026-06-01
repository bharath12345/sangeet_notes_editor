package com.varpas.sangeet.core.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.varpas.sangeet.core.model.*

class KeyHandlerSpec extends AnyFlatSpec with Matchers:

  val teentaal = Taal("Teentaal", 16, List(
    Vibhag(4, VibhagMarker.Sam),
    Vibhag(4, VibhagMarker.Taali(2)),
    Vibhag(4, VibhagMarker.Khali),
    Vibhag(4, VibhagMarker.Taali(3))
  ), None)

  val editor = CompositionEditor.empty(teentaal,
    Raag("Yaman", None, None, None, None, None, None, None))

  "KeyHandler.handleSwarKey" should "insert Sa on 's'" in {
    val (newEditor, msg) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val events = newEditor.currentSection.events
    events should have length 1
    events.head match
      case s: Event.Swar =>
        s.note shouldBe Note.Sa
        s.variant shouldBe Variant.Shuddha
        s.octave shouldBe Octave.Madhya
      case _ => fail("Expected Swar")
    msg should include("Sa")
  }

  it should "insert komal Re on Shift+R" in {
    val (newEditor, _) = KeyHandler.handleSwarKey(editor, 'r', shiftDown = true)
    newEditor.currentSection.events.head match
      case s: Event.Swar =>
        s.note shouldBe Note.Re
        s.variant shouldBe Variant.Komal
      case _ => fail("Expected Swar")
  }

  it should "insert tivra Ma on Shift+M" in {
    val (newEditor, _) = KeyHandler.handleSwarKey(editor, 'm', shiftDown = true)
    newEditor.currentSection.events.head match
      case s: Event.Swar =>
        s.note shouldBe Note.Ma
        s.variant shouldBe Variant.Tivra
      case _ => fail("Expected Swar")
  }

  it should "insert rest on space" in {
    val (newEditor, msg) = KeyHandler.handleSpecialKey(editor, "SPACE")
    newEditor.currentSection.events.head shouldBe a[Event.Rest]
    msg should include("Rest")
  }

  it should "insert sustain on dash" in {
    val (newEditor, msg) = KeyHandler.handleSpecialKey(editor, "MINUS")
    newEditor.currentSection.events.head shouldBe a[Event.Sustain]
    msg should include("Sustain")
  }

  it should "advance cursor after inserting a swar" in {
    val (newEditor, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    newEditor.cursor.beat shouldBe 1
  }

  it should "handle dot prefix for mandra" in {
    val editorWithMandra = editor.copy(
      cursor = editor.cursor.withOctave(Octave.Mandra))
    val (newEditor, msg) = KeyHandler.handleSwarKey(editorWithMandra, 's', shiftDown = false)
    newEditor.currentSection.events.head match
      case s: Event.Swar => s.octave shouldBe Octave.Mandra
      case _ => fail("Expected Swar")
    msg should include("mandra")
  }

  it should "reset octave to Madhya after inserting a note" in {
    val editorWithMandra = editor.copy(
      cursor = editor.cursor.withOctave(Octave.Mandra))
    val (newEditor, _) = KeyHandler.handleSwarKey(editorWithMandra, 's', shiftDown = false)
    newEditor.cursor.currentOctave shouldBe Octave.Madhya
  }

  it should "return error message for unknown keys" in {
    val (_, msg) = KeyHandler.handleSwarKey(editor, 'x', shiftDown = false)
    msg should include("Unknown")
  }

  "handleDualSwar" should "insert two identical notes" in {
    val (dualEditor, _) = KeyHandler.handleDualSwar(editor, 's', shiftDown = false)
    dualEditor.currentSection.events.size shouldBe 2
    val event1 = dualEditor.currentSection.events(0).asInstanceOf[Event.Swar]
    val event2 = dualEditor.currentSection.events(1).asInstanceOf[Event.Swar]
    event1.note shouldBe Note.Sa
    event2.note shouldBe Note.Sa
    event1.duration shouldBe Rational(1, 2)
    event2.duration shouldBe Rational(1, 2)
  }

  "handleSpecialKey BACKSPACE" should "delete note at cursor and move back" in {
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    withNote.currentSection.events should have length 1
    val edAtBeat0 = withNote.copy(cursor = withNote.cursor.moveTo(0, 0))
    val (afterDelete, msg) = KeyHandler.handleSpecialKey(edAtBeat0, "BACKSPACE")
    afterDelete.currentSection.events shouldBe empty
    msg should include("Deleted")
  }

  it should "delete note before cursor when nothing at cursor (text-editor style)" in {
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    // Cursor is at beat 1 after inserting Sa at beat 0
    withNote.cursor.beat shouldBe 1
    val (afterDelete, msg) = KeyHandler.handleSpecialKey(withNote, "BACKSPACE")
    afterDelete.currentSection.events shouldBe empty
    afterDelete.cursor.beat shouldBe 0
    msg should include("Deleted before cursor")
  }

  it should "move cursor back when no note at cursor or before" in {
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val edAtBeat3 = withNote.copy(cursor = withNote.cursor.moveTo(0, 3))
    val (result, msg) = KeyHandler.handleSpecialKey(edAtBeat3, "BACKSPACE")
    result.cursor.beat shouldBe 2
    msg should include("Moved back")
  }

  it should "report error at beginning with no note" in {
    val (_, msg) = KeyHandler.handleSpecialKey(editor, "BACKSPACE")
    msg should include("Nothing")
  }

  it should "delete note in the middle, not the last note" in {
    val (e1, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (e2, _) = KeyHandler.handleSwarKey(e1, 'r', shiftDown = false)
    val (e3, _) = KeyHandler.handleSwarKey(e2, 'g', shiftDown = false)
    e3.currentSection.events should have length 3
    // Cursor at beat 1 — has a note (Re), delete it
    val edAtBeat1 = e3.copy(cursor = e3.cursor.moveTo(0, 1))
    val (afterDelete, msg) = KeyHandler.handleSpecialKey(edAtBeat1, "BACKSPACE")
    afterDelete.currentSection.events should have length 2
    val notes = afterDelete.currentSection.events.map(_.asInstanceOf[Event.Swar].note)
    notes shouldBe List(Note.Sa, Note.Ga)
    msg should include("Deleted at cursor")
  }

  it should "delete from cursor position when cursor is between notes" in {
    val (e1, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (e2, _) = KeyHandler.handleSwarKey(e1, 'r', shiftDown = false)
    val (e3, _) = KeyHandler.handleSwarKey(e2, 'g', shiftDown = false)
    // Cursor at beat 3 (after Ga at beat 2), backspace should delete Ga (beat before cursor)
    e3.cursor.beat shouldBe 3
    val (afterDelete, msg) = KeyHandler.handleSpecialKey(e3, "BACKSPACE")
    afterDelete.currentSection.events should have length 2
    val notes = afterDelete.currentSection.events.map(_.asInstanceOf[Event.Swar].note)
    notes shouldBe List(Note.Sa, Note.Re)
    msg should include("Deleted before cursor")
  }

  "handleSpecialKey DELETE" should "remove event at cursor without moving back" in {
    val (e1, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (e2, _) = KeyHandler.handleSwarKey(e1, 'r', shiftDown = false)
    val (e3, _) = KeyHandler.handleSwarKey(e2, 'g', shiftDown = false)
    val edAtBeat1 = e3.copy(cursor = e3.cursor.moveTo(0, 1))
    val (afterDelete, msg) = KeyHandler.handleSpecialKey(edAtBeat1, "DELETE")
    afterDelete.currentSection.events should have length 2
    afterDelete.cursor.beat shouldBe 1
    msg should include("Deleted at cursor")
  }

  it should "report error when no note at cursor" in {
    val (e1, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val edAtBeat5 = e1.copy(cursor = e1.cursor.moveTo(0, 5))
    val (_, msg) = KeyHandler.handleSpecialKey(edAtBeat5, "DELETE")
    msg should include("No note at cursor")
  }

  "handleOctaveKey BACKTICK" should "return to Madhya saptak" in {
    val withTaar = editor.copy(cursor = editor.cursor.withOctave(Octave.Taar))
    val (result, msg) = KeyHandler.handleOctaveKey(withTaar, "BACKTICK")
    result.cursor.currentOctave shouldBe Octave.Madhya
    msg should include("Madhya")
  }

  "handleSubdivision" should "set cursor subdivision count" in {
    val result = KeyHandler.handleSubdivision(editor, 4)
    result.cursor.totalSubdivisions shouldBe 4
  }

  "handleStroke" should "add stroke to last swar" in {
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (result, msg) = KeyHandler.handleStroke(withNote, Stroke.Da)
    result.currentSection.events.head.asInstanceOf[Event.Swar].stroke shouldBe Some(Stroke.Da)
    msg should include("Da")
  }

  it should "return error when no swar to attach to" in {
    val (_, msg) = KeyHandler.handleStroke(editor, Stroke.Da)
    msg should include("No swar")
  }

  "handleSimpleOrnament" should "add ornament to last swar" in {
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val gamak = Gamak()
    val (result, msg) = KeyHandler.handleSimpleOrnament(withNote, gamak, "Gamak")
    result.currentSection.events.head.asInstanceOf[Event.Swar].ornaments.contains(gamak) shouldBe true
    msg should include("Gamak")
  }

  "handleNoteOrnament" should "handle KanSwar mode" in {
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (result, msg, nextMode) = KeyHandler.handleNoteOrnament(withNote, 'r', shiftDown = false, OrnamentMode.KanSwar)
    result.currentSection.events.head.asInstanceOf[Event.Swar].ornaments should have length 1
    msg should include("Kan swar")
    nextMode shouldBe None
  }

  it should "handle Meend start and end" in {
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (ed1, msg1, mode1) = KeyHandler.handleNoteOrnament(withNote, 'r', shiftDown = false, OrnamentMode.MeendStart(MeendDirection.Ascending))
    msg1 should include("Meend")
    mode1 shouldBe defined
    mode1.get match
      case OrnamentMode.MeendEnd(startRef, dir) =>
        startRef.note shouldBe Note.Re
        dir shouldBe MeendDirection.Ascending
      case _ => fail("Expected MeendEnd mode")

    val (ed2, msg2, mode2) = KeyHandler.handleNoteOrnament(ed1, 'g', shiftDown = false, mode1.get)
    msg2 should include("Meend")
    mode2 shouldBe None
    ed2.currentSection.events.head.asInstanceOf[Event.Swar].ornaments should have length 1
  }

  "finishMultiNoteOrnament" should "finish Murki with collected notes" in {
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val notes = List(NoteRef(Note.Sa, Variant.Shuddha, Octave.Madhya), NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya))
    val mode = OrnamentMode.MurkiCollect(notes)
    val (result, msg) = KeyHandler.finishMultiNoteOrnament(withNote, mode)
    msg should include("Murki")
    result.currentSection.events.head.asInstanceOf[Event.Swar].ornaments should have length 1
  }

  it should "return error for empty note collection" in {
    val mode = OrnamentMode.MurkiCollect(Nil)
    val (_, msg) = KeyHandler.finishMultiNoteOrnament(editor, mode)
    msg should include("No notes")
  }

  "handleSwarGroup" should "place 2 different notes on one beat with half duration each" in {
    val notes = List(
      (Note.Sa, Variant.Shuddha, Octave.Madhya),
      (Note.Re, Variant.Shuddha, Octave.Madhya)
    )
    val (result, msg) = KeyHandler.handleSwarGroup(editor, notes)
    val events = result.currentSection.events
    events should have length 2
    val s1 = events(0).asInstanceOf[Event.Swar]
    val s2 = events(1).asInstanceOf[Event.Swar]
    s1.note shouldBe Note.Sa
    s2.note shouldBe Note.Re
    s1.duration shouldBe Rational(1, 2)
    s2.duration shouldBe Rational(1, 2)
    s1.beat shouldBe BeatPosition(0, 0, Rational(0, 2))
    s2.beat shouldBe BeatPosition(0, 0, Rational(1, 2))
    msg should include("2-swar group")
  }

  it should "place 3 notes on one beat with third duration each" in {
    val notes = List(
      (Note.Sa, Variant.Shuddha, Octave.Madhya),
      (Note.Re, Variant.Shuddha, Octave.Madhya),
      (Note.Ga, Variant.Shuddha, Octave.Madhya)
    )
    val (result, msg) = KeyHandler.handleSwarGroup(editor, notes)
    val events = result.currentSection.events
    events should have length 3
    events.foreach { e =>
      e.asInstanceOf[Event.Swar].duration shouldBe Rational(1, 3)
    }
    val positions = events.map(_.position.subdivision)
    positions shouldBe List(Rational(0, 3), Rational(1, 3), Rational(2, 3))
    msg should include("3-swar group")
  }

  it should "place 4 notes on one beat with quarter duration each" in {
    val notes = List(
      (Note.Sa, Variant.Shuddha, Octave.Madhya),
      (Note.Re, Variant.Shuddha, Octave.Madhya),
      (Note.Ga, Variant.Shuddha, Octave.Madhya),
      (Note.Ma, Variant.Shuddha, Octave.Madhya)
    )
    val (result, msg) = KeyHandler.handleSwarGroup(editor, notes)
    val events = result.currentSection.events
    events should have length 4
    events.foreach { e =>
      e.asInstanceOf[Event.Swar].duration shouldBe Rational(1, 4)
    }
    msg should include("4-swar group")
  }

  it should "reject more than 4 notes" in {
    val notes = List.fill(5)((Note.Sa, Variant.Shuddha, Octave.Madhya))
    val (result, msg) = KeyHandler.handleSwarGroup(editor, notes)
    result.currentSection.events shouldBe empty
    msg should include("Maximum 4")
  }

  it should "reject empty note list" in {
    val (result, msg) = KeyHandler.handleSwarGroup(editor, Nil)
    result.currentSection.events shouldBe empty
    msg should include("No notes")
  }

  it should "advance cursor to next beat" in {
    val notes = List(
      (Note.Sa, Variant.Shuddha, Octave.Madhya),
      (Note.Re, Variant.Shuddha, Octave.Madhya)
    )
    val (result, _) = KeyHandler.handleSwarGroup(editor, notes)
    result.cursor.beat shouldBe 1
  }

  it should "reset octave to Madhya after group" in {
    val edInTaar = editor.copy(cursor = editor.cursor.withOctave(Octave.Taar))
    val notes = List(
      (Note.Sa, Variant.Shuddha, Octave.Taar),
      (Note.Re, Variant.Shuddha, Octave.Taar)
    )
    val (result, _) = KeyHandler.handleSwarGroup(edInTaar, notes)
    result.cursor.currentOctave shouldBe Octave.Madhya
  }

  "handleDualSwar" should "still work after refactoring to use handleSwarGroup" in {
    val (result, _) = KeyHandler.handleDualSwar(editor, 'r', shiftDown = false)
    val events = result.currentSection.events
    events should have length 2
    events(0).asInstanceOf[Event.Swar].note shouldBe Note.Re
    events(1).asInstanceOf[Event.Swar].note shouldBe Note.Re
    events(0).asInstanceOf[Event.Swar].duration shouldBe Rational(1, 2)
    events(1).asInstanceOf[Event.Swar].duration shouldBe Rational(1, 2)
  }

  "charToNote" should "map lowercase swar keys" in {
    KeyHandler.charToNote('s') shouldBe Some(Note.Sa)
    KeyHandler.charToNote('r') shouldBe Some(Note.Re)
    KeyHandler.charToNote('g') shouldBe Some(Note.Ga)
    KeyHandler.charToNote('m') shouldBe Some(Note.Ma)
    KeyHandler.charToNote('p') shouldBe Some(Note.Pa)
    KeyHandler.charToNote('d') shouldBe Some(Note.Dha)
    KeyHandler.charToNote('n') shouldBe Some(Note.Ni)
  }

  it should "return None for non-swar keys" in {
    KeyHandler.charToNote('x') shouldBe None
    KeyHandler.charToNote('z') shouldBe None
  }

  "BACKSPACE on grouped beat" should "delete entire dual-swar group" in {
    val notes = List(
      (Note.Sa, Variant.Shuddha, Octave.Madhya),
      (Note.Re, Variant.Shuddha, Octave.Madhya)
    )
    val (withGroup, _) = KeyHandler.handleSwarGroup(editor, notes)
    withGroup.currentSection.events should have length 2
    val edAtBeat0 = withGroup.copy(cursor = withGroup.cursor.moveTo(0, 0))
    val (afterDelete, msg) = KeyHandler.handleSpecialKey(edAtBeat0, "BACKSPACE")
    afterDelete.currentSection.events shouldBe empty
    msg should include("Deleted")
  }

  "DELETE on grouped beat" should "delete entire group without moving cursor" in {
    val notes = List(
      (Note.Sa, Variant.Shuddha, Octave.Madhya),
      (Note.Re, Variant.Shuddha, Octave.Madhya)
    )
    val (withGroup, _) = KeyHandler.handleSwarGroup(editor, notes)
    val edAtBeat0 = withGroup.copy(cursor = withGroup.cursor.moveTo(0, 0))
    val (afterDelete, msg) = KeyHandler.handleSpecialKey(edAtBeat0, "DELETE")
    afterDelete.currentSection.events shouldBe empty
    afterDelete.cursor.beat shouldBe 0
    msg should include("Deleted")
  }
