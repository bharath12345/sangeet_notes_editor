package sangeet.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals

class KeyHandlerSpec extends AnyFlatSpec with Matchers:

  val editor = CompositionEditor.empty(Taals.teentaal,
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
    val (newEditor, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (dualEditor, _) = KeyHandler.handleSwarKey(newEditor, 's', shiftDown = false)
    dualEditor.currentSection.events.size should be >= 2
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
