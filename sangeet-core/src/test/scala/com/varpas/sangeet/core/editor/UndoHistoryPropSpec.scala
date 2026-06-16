package com.varpas.sangeet.core.editor

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators
import com.varpas.sangeet.core.model._

/** Plan 19 T1C — Phase C gap-fill for `UndoHistory`.
  *
  * `UndoHistorySpec` is entirely example-based (push/undo/redo on a hand-built editor). Phase C adds the algebraic laws
  * that hold for *any* sequence of pushes, *any* taal/raag pair, and *any* maxSize:
  *   - undo ∘ push == identity on `present` for a single push
  *   - redo ∘ undo == identity on `present` (when push happened first)
  *   - push always clears the future stack
  *   - undo depth is bounded by maxSize even after many pushes
  *
  * These complement the existing examples — examples pin the specific call sequences from shipped bugs; these
  * properties pin the general law that should hold for any sequence.
  */
class UndoHistoryPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  /** Empty editor on any catalog (taal, raag) combination. */
  private val genEmptyEditor: Gen[CompositionEditor] =
    for
      taal <- Generators.genTaal
      raag <- Generators.genRaag
    yield CompositionEditor.empty(taal, raag)

  /** Mutate the editor in some deterministic way to get a distinct state to push. */
  private def withAddedSwar(ed: CompositionEditor, note: Note): CompositionEditor =
    val event = Event.Swar(note, Variant.Shuddha, Octave.Madhya, ed.cursor.position, Rational(1, 1), None, Nil, None)
    ed.addEvent(event)

  test("propUndoAfterPushRestoresPresent: undo(push(h, s')).present == h.present"):
    forAll(genEmptyEditor, Generators.genNote) { (ed, n) =>
      val ed1     = withAddedSwar(ed, n)
      val history = UndoHistory(ed).push(ed1)
      val undone  = history.undo
      assert(undone.isDefined, "undo should be available after a push")
      assert(undone.get.present == ed, s"undo did not restore previous state")
    }

  test("propRedoUndoRestoresPresent: push then undo then redo lands back on the pushed state"):
    forAll(genEmptyEditor, Generators.genNote) { (ed, n) =>
      val ed1     = withAddedSwar(ed, n)
      val history = UndoHistory(ed).push(ed1).undo.flatMap(_.redo)
      assert(history.isDefined)
      assert(history.get.present == ed1)
    }

  test("propPushClearsFuture: after any push, redo is impossible"):
    forAll(genEmptyEditor, Generators.genNote, Generators.genNote) { (ed, n1, n2) =>
      val ed1 = withAddedSwar(ed, n1)
      val ed2 = withAddedSwar(ed1, n2)
      // Set up a non-empty future by pushing then undoing.
      val withFuture = UndoHistory(ed).push(ed1).undo.get
      assert(withFuture.canRedo, "precondition: redo available after push+undo")
      // Now push a different state — future should be cleared.
      val pushedAgain = withFuture.push(ed2)
      assert(!pushedAgain.canRedo, "push did not clear redo stack")
      assert(pushedAgain.present == ed2)
    }

  test("propUndoDepthBoundedByMaxSize: after N pushes with maxSize=M, no more than M undos are possible"):
    val maxSizeGen = Gen.choose(1, 5)
    val pushCount  = Gen.choose(0, 12)
    forAll(genEmptyEditor, maxSizeGen, pushCount) { (ed, maxSize, n) =>
      val states = (0 until n).map(i => withAddedSwar(ed, Note.Sa).copy(currentSectionIndex = 0))
      val h0     = UndoHistory(ed, maxSize = maxSize)
      val final0 = states.foldLeft(h0)(_.push(_))
      // Count available undos.
      var count                    = 0
      var cur: Option[UndoHistory] = Some(final0)
      while cur.flatMap(_.undo).isDefined do
        cur = cur.flatMap(_.undo)
        count += 1
      assert(count <= maxSize, s"undo depth $count exceeded maxSize $maxSize for n=$n pushes")
    }

  test("propEmptyHistoryHasNoUndoOrRedo: a fresh UndoHistory has neither undo nor redo available"):
    forAll(genEmptyEditor) { ed =>
      val h = UndoHistory(ed)
      assert(!h.canUndo)
      assert(!h.canRedo)
      assert(h.undo.isEmpty)
      assert(h.redo.isEmpty)
    }
