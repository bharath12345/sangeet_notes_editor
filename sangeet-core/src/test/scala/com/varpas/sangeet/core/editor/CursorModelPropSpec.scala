package com.varpas.sangeet.core.editor

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators
import com.varpas.sangeet.core.model._

/** Plan 19 T1C — Phase C gap-fill for `CursorModel`.
  *
  * T1B left `CursorModelSpec`'s example assertions in place — every case is a hand-built (cycle, beat) pair. Phase C
  * adds the algebraic laws that hold for *any* taal sampled from the catalog and any (cycle, beat) within bounds:
  *   - nextBeat ∘ prevBeat == identity at any non-boundary position
  *   - nextBeat is monotonic in absolute beat (cycle × matras + beat)
  *   - withSubdivisions(n) leaves subIndex at 0 and totalSubdivisions at n
  *   - startSelection is idempotent (calling twice doesn't move the anchor)
  *   - clearSelection is idempotent (calling twice == once)
  *   - moveTo clears any existing selection
  */
class CursorModelPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  /** Cursor sampled from any taal in the catalog, positioned at any valid (cycle, beat) within [0..3] × [0..matras). */
  private val genCursorAtValidPosition: Gen[CursorModel] =
    for
      taal  <- Generators.genTaal
      cycle <- Gen.choose(0, 3)
      beat  <- Gen.choose(0, taal.matras - 1)
    yield CursorModel(taal).copy(cycle = cycle, beat = beat)

  /** Cursor positioned strictly inside (not at beat 0, not at the last beat of a cycle). */
  private val genCursorAtInterior: Gen[CursorModel] =
    for
      taal  <- Generators.genTaal if taal.matras >= 3
      cycle <- Gen.choose(0, 3)
      beat  <- Gen.choose(1, taal.matras - 2)
    yield CursorModel(taal).copy(cycle = cycle, beat = beat)

  test("propNextPrevIdentityAtInterior: nextBeat.prevBeat == this for any non-boundary position"):
    forAll(genCursorAtInterior) { c =>
      val round = c.nextBeat.prevBeat
      assert(round.cycle == c.cycle, s"cycle drift: $c -> $round")
      assert(round.beat == c.beat, s"beat drift: $c -> $round")
    }

  test("propPrevNextIdentityAtInterior: prevBeat.nextBeat == this for any non-boundary position"):
    forAll(genCursorAtInterior) { c =>
      val round = c.prevBeat.nextBeat
      assert(round.cycle == c.cycle)
      assert(round.beat == c.beat)
    }

  test("propNextBeatMonotonic: absoluteBeat(nextBeat) >= absoluteBeat(self), strictly greater when not bottoming out"):
    forAll(genCursorAtValidPosition) { c =>
      val before = c.cycle * c.taal.matras + c.beat
      val next   = c.nextBeat
      val after  = next.cycle * c.taal.matras + next.beat
      assert(after > before, s"nextBeat did not advance: $c -> $next")
    }

  test("propPrevBeatMonotonicOrFixedAtZero: prevBeat never moves forward; at (0,0) it stays put"):
    forAll(genCursorAtValidPosition) { c =>
      val before = c.cycle * c.taal.matras + c.beat
      val prev   = c.prevBeat
      val after  = prev.cycle * c.taal.matras + prev.beat
      if c.cycle == 0 && c.beat == 0 then assert(after == before)
      else assert(after < before, s"prevBeat did not retreat: $c -> $prev")
    }

  test("propWithSubdivisionsResetsSubIndex: withSubdivisions(n) yields totalSubdivisions=n, subIndex=0"):
    forAll(genCursorAtValidPosition, Gen.choose(1, 16)) { (c, n) =>
      val updated = c.withSubdivisions(n)
      assert(updated.totalSubdivisions == n)
      assert(updated.subIndex == 0)
      // Position fields (cycle, beat) are preserved.
      assert(updated.cycle == c.cycle)
      assert(updated.beat == c.beat)
    }

  test("propStartSelectionIdempotent: startSelection ∘ startSelection == startSelection (anchor doesn't move)"):
    forAll(genCursorAtValidPosition) { c =>
      val once  = c.startSelection
      val twice = once.startSelection
      assert(twice.selectionAnchor == once.selectionAnchor)
    }

  test("propClearSelectionIdempotent: clearSelection ∘ clearSelection == clearSelection (no anchor either way)"):
    forAll(genCursorAtValidPosition) { c =>
      val once  = c.startSelection.clearSelection
      val twice = once.clearSelection
      assert(once.hasSelection == false)
      assert(twice.hasSelection == false)
      assert(twice == once)
    }

  test("propMoveToClearsSelection: moveTo always drops the selection anchor"):
    forAll(genCursorAtValidPosition, Gen.choose(0, 3), Gen.choose(0, 15)) { (c, newCycle, newBeat) =>
      val withSel = c.startSelection
      assert(withSel.hasSelection)
      val moved = withSel.moveTo(newCycle, newBeat)
      assert(!moved.hasSelection, s"moveTo did not clear selection: $moved")
    }
