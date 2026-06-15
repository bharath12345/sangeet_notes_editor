package com.varpas.sangeet.core.layout

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators
import com.varpas.sangeet.core.generators.Generators.given
import com.varpas.sangeet.core.model._

/** Plan 19 T1B — layout invariants. Subsumes the empty/multi-cycle/preservation cases in `GridLayoutSpec` with
  * property-based versions that quantify over any generated `Section` / `Taal` pair.
  *
  * Properties exercise the BeatGrouper → LineBreaker → GridLayout pipeline through `GridLayout.layout` and
  * `GridLayout.layoutAll`, which is the only public surface for the layout engine.
  */
class GridLayoutPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  test("propLayoutPreservesSectionName: SectionGrid.sectionName == input Section.name"):
    forAll { (s: Section, t: Taal) =>
      val grid = GridLayout.layout(s, t, LayoutConfig())
      assert(grid.sectionName == s.name)
    }

  test("propLayoutPreservesSectionType: SectionGrid.sectionType == input Section.sectionType"):
    forAll { (s: Section, t: Taal) =>
      val grid = GridLayout.layout(s, t, LayoutConfig())
      assert(grid.sectionType == s.sectionType)
    }

  test("propLayoutEmptySectionHasNoLines: a section with no events produces an empty grid"):
    forAll(Generators.genSectionType, Generators.genShortText, Generators.genTaal) { (sType, name, t) =>
      val section = Section(name, sType, Nil)
      val grid    = GridLayout.layout(section, t, LayoutConfig())
      assert(grid.lines.isEmpty)
    }

  test("propLayoutPreservesEventCount: total cells across all lines hold every input event"):
    forAll { (s: Section, t: Taal) =>
      val grid       = GridLayout.layout(s, t, LayoutConfig())
      val cellEvents = grid.lines.flatMap(_.cells).flatMap(_.events)
      assert(cellEvents.size == s.events.size, s"event count drift: in=${s.events.size}, out=${cellEvents.size}")
    }

  test("propLayoutPreservesEventSet: cells contain exactly the input events (as a multiset)"):
    forAll { (s: Section, t: Taal) =>
      val grid     = GridLayout.layout(s, t, LayoutConfig())
      val outMulti = grid.lines.flatMap(_.cells).flatMap(_.events).groupBy(identity).view.mapValues(_.size).toMap
      val inMulti  = s.events.groupBy(identity).view.mapValues(_.size).toMap
      assert(outMulti == inMulti)
    }

  test("propLayoutCellsCarryConsistentPosition: every event in a cell has the cell's (cycle, beat)"):
    forAll { (s: Section, t: Taal) =>
      val grid = GridLayout.layout(s, t, LayoutConfig())
      grid.lines.foreach { line =>
        line.cells.foreach { cell =>
          cell.events.foreach { e =>
            val pos = e.position
            assert(pos.cycle == cell.position.cycle, s"cell ${cell.position} contains event at $pos (cycle mismatch)")
            assert(pos.beat == cell.position.beat, s"cell ${cell.position} contains event at $pos (beat mismatch)")
          }
        }
      }
    }

  test("propLayoutCellsSortedBySubdivision: within a cell, events appear in ascending subdivision order"):
    forAll { (s: Section, t: Taal) =>
      val grid = GridLayout.layout(s, t, LayoutConfig())
      grid.lines.foreach { line =>
        line.cells.foreach { cell =>
          val subs = cell.events.map(_.position.subdivision)
          assert(subs == subs.sorted, s"cell ${cell.position} subdivisions not sorted: $subs")
        }
      }
    }

  test("propLayoutLinesSortedByCycle: lines appear in non-decreasing cycle order"):
    forAll { (s: Section, t: Taal) =>
      val grid = GridLayout.layout(s, t, LayoutConfig())
      // Each line's cells are all the same cycle when split-by-vibhag, or a mixed cycle on low-density paths;
      // but a line's first non-empty cell's cycle never decreases.
      val cycles = grid.lines.flatMap(_.cells.headOption.map(_.position.cycle))
      assert(cycles == cycles.sorted, s"line cycles not non-decreasing: $cycles")
    }

  test("propLayoutAllProducesGridPerSection: layoutAll yields exactly one grid per composition section"):
    forAll { (c: Composition) =>
      val grids = GridLayout.layoutAll(c, LayoutConfig())
      assert(grids.size == c.sections.size)
      grids.zip(c.sections).foreach { (g, s) =>
        assert(g.sectionName == s.name)
        assert(g.sectionType == s.sectionType)
      }
    }

  test("propBeatGrouperPreservesEvents: BeatGrouper.group keeps all events (as a multiset)"):
    forAll { (s: Section) =>
      val cells    = BeatGrouper.group(s.events)
      val outMulti = cells.flatMap(_.events).groupBy(identity).view.mapValues(_.size).toMap
      val inMulti  = s.events.groupBy(identity).view.mapValues(_.size).toMap
      assert(outMulti == inMulti)
    }

  test("propBeatGrouperCellsSortedByCycleThenBeat: groups appear in (cycle, beat) order"):
    forAll { (s: Section) =>
      val cells = BeatGrouper.group(s.events)
      val keys  = cells.map(c => (c.position.cycle, c.position.beat))
      assert(keys == keys.sorted)
    }

  test("propBeatGrouperGroupsByCycleBeat: every cell holds events that all share the cell's (cycle, beat)"):
    forAll { (s: Section) =>
      val cells = BeatGrouper.group(s.events)
      cells.foreach { c =>
        c.events.foreach { e =>
          assert(e.position.cycle == c.position.cycle)
          assert(e.position.beat == c.position.beat)
        }
      }
    }
