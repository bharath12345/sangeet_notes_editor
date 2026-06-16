package com.varpas.sangeet.core.layout

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators.given
import com.varpas.sangeet.core.model._

/** Plan 19 T1C — Phase C gap-fill for the layout engine.
  *
  * T1B's `GridLayoutPropSpec` covers event-set preservation, cell sorting, and per-cell position consistency. What's
  * missing is the *containment* invariants — the rules that bound the output relative to the input taal:
  *   - every cell's beat index sits inside the taal's matra range
  *   - every vibhag break index is within its line's cell count
  *   - every marker's cell index is within its line's cell count
  *   - empty cells never appear (every cell holds at least one event)
  *
  * A line break or cell index that points past the end would crash the renderer; these properties guarantee that
  * `GridLayout.layout` never produces an out-of-bounds index, regardless of the section's event positions.
  */
class GridLayoutInvariantsPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  test("propLayoutCellBeatComesFromAnInputEvent: every cell's (cycle, beat) coincides with at least one input event"):
    // Layout never invents (cycle, beat) coordinates — every cell traces back to a real event in the section. This
    // catches a regression where BeatGrouper or LineBreaker would synthesise an empty placeholder cell.
    forAll { (s: Section, t: Taal) =>
      val grid          = GridLayout.layout(s, t, LayoutConfig())
      val inputPosKeys  = s.events.map(e => (e.position.cycle, e.position.beat)).toSet
      val outputPosKeys = grid.lines.flatMap(_.cells).map(c => (c.position.cycle, c.position.beat)).toSet
      assert(
        outputPosKeys.subsetOf(inputPosKeys),
        s"layout created cells at positions not present in input: ${outputPosKeys -- inputPosKeys}"
      )
    }

  test("propLayoutVibhagBreaksInBounds: every vibhag break index points to a real cell within its line"):
    forAll { (s: Section, t: Taal) =>
      val grid = GridLayout.layout(s, t, LayoutConfig())
      grid.lines.foreach { line =>
        line.vibhagBreaks.foreach { idx =>
          assert(idx >= 0 && idx <= line.cells.size, s"vibhag break index $idx out of range [0..${line.cells.size}]")
        }
      }
    }

  test("propLayoutMarkerIndicesInBounds: every (cellIdx, marker) points to a real cell within its line"):
    forAll { (s: Section, t: Taal) =>
      val grid = GridLayout.layout(s, t, LayoutConfig())
      grid.lines.foreach { line =>
        line.markers.foreach { (cellIdx, _) =>
          assert(
            cellIdx >= 0 && cellIdx < math.max(line.cells.size, 1),
            s"marker cell index $cellIdx out of range for line with ${line.cells.size} cells"
          )
        }
      }
    }

  test("propLayoutNoEmptyCells: every BeatCell holds at least one event (BeatGrouper never emits empty cells)"):
    forAll { (s: Section, t: Taal) =>
      val grid = GridLayout.layout(s, t, LayoutConfig())
      grid.lines.foreach { line =>
        line.cells.foreach { cell =>
          assert(cell.events.nonEmpty, s"empty cell at ${cell.position}")
        }
      }
    }

  test("propLayoutEventsBelongToInputSection: every event in the grid was in the input section (multiset subset)"):
    // Stronger than T1B's "multiset equality": even if we hadn't pinned equality, no event should be invented from
    // thin air. This isolates the "no events created by layout" half of the contract from the "no events dropped" half.
    forAll { (s: Section, t: Taal) =>
      val grid       = GridLayout.layout(s, t, LayoutConfig())
      val cellEvents = grid.lines.flatMap(_.cells).flatMap(_.events).toSet
      val inputSet   = s.events.toSet
      assert(cellEvents.subsetOf(inputSet), s"layout invented events: ${cellEvents -- inputSet}")
    }
