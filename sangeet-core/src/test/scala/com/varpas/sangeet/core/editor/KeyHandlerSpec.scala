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

  "handleSpecialKey BACKSPACE" should "remove last event" in {
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    withNote.currentSection.events should have length 1
    val (afterDelete, msg) = KeyHandler.handleSpecialKey(withNote, "BACKSPACE")
    afterDelete.currentSection.events shouldBe empty
    msg should include("Deleted")
  }

  it should "report error when nothing to delete" in {
    val (_, msg) = KeyHandler.handleSpecialKey(editor, "BACKSPACE")
    msg should include("Nothing")
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
