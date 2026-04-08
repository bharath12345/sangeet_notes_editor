package sangeet.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals
import sangeet.raag.Raags

class UndoHistorySpec extends AnyFlatSpec with Matchers:

  private def mkEditor: CompositionEditor =
    CompositionEditor.empty(Taals.teentaal, Raags.all("yaman"))

  private def addNote(ed: CompositionEditor, note: Note): CompositionEditor =
    val event = Event.Swar(note, Variant.Shuddha, Octave.Madhya,
      ed.cursor.position, Rational(1, 1), None, Nil, None)
    ed.addEvent(event).copy(cursor = ed.cursor.nextBeat)

  "UndoHistory" should "start with no undo/redo available" in {
    val h = UndoHistory(mkEditor)
    h.canUndo shouldBe false
    h.canRedo shouldBe false
  }

  it should "allow undo after push" in {
    val ed0 = mkEditor
    val ed1 = addNote(ed0, Note.Sa)
    val h = UndoHistory(ed0).push(ed1)
    h.canUndo shouldBe true
    h.canRedo shouldBe false
    h.present shouldBe ed1
  }

  it should "restore previous state on undo" in {
    val ed0 = mkEditor
    val ed1 = addNote(ed0, Note.Sa)
    val h = UndoHistory(ed0).push(ed1).undo.get
    h.present shouldBe ed0
    h.canRedo shouldBe true
  }

  it should "restore undone state on redo" in {
    val ed0 = mkEditor
    val ed1 = addNote(ed0, Note.Sa)
    val h = UndoHistory(ed0).push(ed1).undo.get.redo.get
    h.present shouldBe ed1
    h.canUndo shouldBe true
    h.canRedo shouldBe false
  }

  it should "clear redo stack on new push" in {
    val ed0 = mkEditor
    val ed1 = addNote(ed0, Note.Sa)
    val ed2 = addNote(ed1, Note.Re)
    val h = UndoHistory(ed0).push(ed1).undo.get.push(ed2)
    h.canRedo shouldBe false
    h.present shouldBe ed2
  }

  it should "handle multiple undo steps" in {
    val ed0 = mkEditor
    val ed1 = addNote(ed0, Note.Sa)
    val ed2 = addNote(ed1, Note.Re)
    val ed3 = addNote(ed2, Note.Ga)
    val h = UndoHistory(ed0).push(ed1).push(ed2).push(ed3)
    val h1 = h.undo.get
    h1.present shouldBe ed2
    val h2 = h1.undo.get
    h2.present shouldBe ed1
    val h3 = h2.undo.get
    h3.present shouldBe ed0
    h3.undo shouldBe None
  }

  it should "respect maxSize limit" in {
    val ed = mkEditor
    var h = UndoHistory(ed, maxSize = 3)
    val editors = (1 to 5).map(i => addNote(ed, Note.Sa))
    editors.foreach(e => h = h.push(e))
    // Should have at most 3 undo steps
    var count = 0
    var current: Option[UndoHistory] = Some(h)
    while current.flatMap(_.undo).isDefined do
      current = current.flatMap(_.undo)
      count += 1
    count shouldBe 3
  }

  it should "return None for undo on empty history" in {
    UndoHistory(mkEditor).undo shouldBe None
  }

  it should "return None for redo with no undone states" in {
    UndoHistory(mkEditor).redo shouldBe None
  }

  "UndoHistory.apply" should "create history with maxSize 50 by default" in {
    val h = UndoHistory(mkEditor)
    h.maxSize shouldBe 50
  }
