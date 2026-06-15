package com.varpas.sangeet.core.editor

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators
import com.varpas.sangeet.core.model._

/** Plan 19 T1B — `CompositionEditor` operation laws. Subsumes the cluster of `addEvent` / `removeLastEvent` /
  * `updateCurrentSection` examples in `CompositionEditorSpec` with properties that quantify over any starting
  * composition, raag, and taal.
  *
  * Laws covered:
  *   - addEvent: length grows by 1, the new event is the last
  *   - addEvent + removeLastEvent: identity (round-trip law for non-empty sections)
  *   - removeLastEvent on empty section: None
  *   - updateCurrentSection: get-after-set returns what was set
  *   - empty / create: starting state invariants (cursor at (0,0), section index 0, untitled has type Gat)
  *   - changeTaal: idempotent under reapplication of the same taal; matras update propagates
  */
class CompositionEditorPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  /** Editor built from arbitrary built-in raag + built-in taal — exercises every (raag, taal) combination. */
  private val genEmptyEditor: Gen[CompositionEditor] =
    for
      taal <- Generators.genTaal
      raag <- Generators.genRaag
    yield CompositionEditor.empty(taal, raag)

  test("propAddEventIncrementsLength: addEvent grows currentSection.events by exactly 1"):
    forAll(genEmptyEditor, Generators.genEvent) { (ed, e) =>
      val before = ed.currentSection.events.size
      val after  = ed.addEvent(e).currentSection.events.size
      assert(after == before + 1)
    }

  test("propAddEventAppends: the freshly added event is the last in currentSection.events"):
    forAll(genEmptyEditor, Generators.genEvent) { (ed, e) =>
      val updated = ed.addEvent(e)
      assert(updated.currentSection.events.last == e)
    }

  test("propAddRemoveRoundTrip: addEvent then removeLastEvent returns the original section state"):
    forAll(genEmptyEditor, Generators.genEvent) { (ed, e) =>
      val rebuilt = ed.addEvent(e).removeLastEvent
      assert(rebuilt.isDefined)
      assert(rebuilt.get.currentSection.events == ed.currentSection.events)
    }

  test("propRemoveLastEventOnEmptySectionIsNone: an empty section can't pop"):
    forAll(genEmptyEditor) { ed =>
      // Built via `empty`, so the Gat section starts with zero events.
      assert(ed.currentSection.events.isEmpty)
      assert(ed.removeLastEvent.isEmpty)
    }

  test("propUpdateCurrentSectionReplaces: updateCurrentSection(s) makes currentSection == s"):
    forAll(genEmptyEditor, Generators.genSection) { (ed, newSection) =>
      val updated = ed.updateCurrentSection(newSection)
      assert(updated.currentSection == newSection)
    }

  test("propUpdateCurrentSectionPreservesOtherSections: only the current section changes"):
    forAll(genEmptyEditor, Generators.genSection) { (ed, newSection) =>
      val updated = ed.updateCurrentSection(newSection)
      assert(updated.composition.sections.size == ed.composition.sections.size)
      updated.composition.sections.zipWithIndex.foreach { (s, i) =>
        if i == ed.currentSectionIndex then assert(s == newSection)
        else assert(s == ed.composition.sections(i))
      }
    }

  test("propEmptyEditorStartingInvariants: cursor at (0,0), section index 0, type is Gat"):
    forAll(genEmptyEditor) { ed =>
      assert(ed.cursor.cycle == 0)
      assert(ed.cursor.beat == 0)
      assert(ed.currentSectionIndex == 0)
      assert(ed.composition.metadata.compositionType == CompositionType.Gat)
      assert(ed.composition.metadata.title == "Untitled")
      assert(ed.composition.metadata.instrument == Some("Sitar"))
    }

  test("propEmptyEditorTaalAndRaagPreserved: the taal/raag passed to empty appears in metadata"):
    forAll(Generators.genTaal, Generators.genRaag) { (t, r) =>
      val ed = CompositionEditor.empty(t, r)
      assert(ed.composition.metadata.taal == t)
      assert(ed.composition.metadata.raag == r)
    }

  test("propAddEventsMonotonicLength: adding N events leaves the section with N events"):
    forAll(genEmptyEditor, Gen.listOf(Generators.genEvent).map(_.take(8))) { (ed, evts) =>
      val after = evts.foldLeft(ed)(_.addEvent(_))
      assert(after.currentSection.events.size == ed.currentSection.events.size + evts.size)
    }

  test("propMaxCycleEmptyIsZero: an editor with no events has maxCycle == 0"):
    forAll(genEmptyEditor) { ed =>
      assert(ed.maxCycle == 0)
    }

  test("propChangeTaalIdempotentForSameTaal: changeTaal(currentTaal) preserves the taal field"):
    forAll(genEmptyEditor) { ed =>
      val same = ed.changeTaal(ed.composition.metadata.taal)
      assert(same.composition.metadata.taal == ed.composition.metadata.taal)
    }

  test("propChangeTaalUpdatesMetadataTaal: changeTaal(t) sets metadata.taal to t"):
    forAll(genEmptyEditor, Generators.genTaal) { (ed, newTaal) =>
      val updated = ed.changeTaal(newTaal)
      assert(updated.composition.metadata.taal == newTaal)
      assert(updated.cursor.taal == newTaal)
    }

  test("propChangeTaalPreservesSectionCount: changing taal doesn't add or drop sections"):
    forAll(genEmptyEditor, Generators.genTaal) { (ed, newTaal) =>
      val updated = ed.changeTaal(newTaal)
      assert(updated.composition.sections.size == ed.composition.sections.size)
    }

  test("propSwarsAtBeatZeroOnEmpty: an empty section has zero swars at any (cycle, beat)"):
    forAll(genEmptyEditor, Gen.choose(0, 3), Gen.choose(0, 15)) { (ed, cycle, beat) =>
      assert(ed.swarsAtBeat(cycle, beat) == 0)
    }

  // --- ClipboardCodecs round-trip ---
  // PBT-flavoured complement to `ClipboardCodecsSpec` — for any generated event list, encode/decode is identity.

  test("propClipboardEventListRoundTrip: ClipboardData(events).asJson decodes back to ClipboardData(events)"):
    import com.varpas.sangeet.core.editor.ClipboardCodecs.given
    import io.circe.syntax._
    forAll(Gen.listOf(Generators.genEvent).map(_.take(8))) { events =>
      val cd      = ClipboardData(events)
      val decoded = cd.asJson.as[ClipboardData]
      assert(decoded == Right(cd))
    }
