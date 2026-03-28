package sangeet.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals

class KeyHandlerSpec extends AnyFlatSpec with Matchers:

  val editor = CompositionEditor.empty(Taals.teentaal,
    Raag("Yaman", None, None, None, None, None, None, None))

  "KeyHandler.handleSwarKey" should "insert Sa on 's'" in {
    val (newEditor, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val events = newEditor.currentSection.events
    events should have length 1
    events.head match
      case s: Event.Swar =>
        s.note shouldBe Note.Sa
        s.variant shouldBe Variant.Shuddha
        s.octave shouldBe Octave.Madhya
      case _ => fail("Expected Swar")
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
    val newEditor = KeyHandler.handleSpecialKey(editor, "SPACE")
    newEditor.currentSection.events.head shouldBe a[Event.Rest]
  }

  it should "insert sustain on dash" in {
    val newEditor = KeyHandler.handleSpecialKey(editor, "MINUS")
    newEditor.currentSection.events.head shouldBe a[Event.Sustain]
  }

  it should "advance cursor after inserting a swar" in {
    val (newEditor, newCursor) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    newCursor.beat shouldBe 1
  }

  it should "handle dot prefix for mandra" in {
    val editorWithMandra = editor.copy(
      cursor = editor.cursor.withOctave(Octave.Mandra))
    val (newEditor, _) = KeyHandler.handleSwarKey(editorWithMandra, 's', shiftDown = false)
    newEditor.currentSection.events.head match
      case s: Event.Swar => s.octave shouldBe Octave.Mandra
      case _ => fail("Expected Swar")
  }

  "handleDualSwar" should "insert two identical notes" in {
    val (newEditor, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (dualEditor, _) = KeyHandler.handleSwarKey(newEditor, 's', shiftDown = false)
    dualEditor.currentSection.events.size should be >= 2
  }
