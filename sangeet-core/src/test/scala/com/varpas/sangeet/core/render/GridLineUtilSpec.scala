package com.varpas.sangeet.core.render

import org.scalatest.funsuite.AnyFunSuite

import com.varpas.sangeet.core.layout.{BeatCell, CycleAndBeat, GridLine}
import com.varpas.sangeet.core.model._

class GridLineUtilSpec extends AnyFunSuite:

  private val beat  = BeatPosition(0, 0, Rational(0, 1))
  private val dur   = Rational(1, 1)
  private val saRef = NoteRef(Note.Sa, Variant.Shuddha, Octave.Madhya)

  private def swar(ornaments: List[Ornament] = Nil, sahitya: Option[String] = None): Event.Swar =
    Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya, beat, dur, None, ornaments, sahitya)

  private def rest: Event.Rest =
    Event.Rest(beat, dur)

  private def cell(events: Event*): BeatCell =
    BeatCell(CycleAndBeat(0, 0), events.toList)

  private def line(cells: BeatCell*): GridLine =
    GridLine(cells.toList, Nil, Nil)

  // --- hasOrnaments ---

  test("hasOrnaments returns false for empty line") {
    assert(!GridLineUtil.hasOrnaments(line()))
  }

  test("hasOrnaments returns false for line with plain swars") {
    assert(!GridLineUtil.hasOrnaments(line(cell(swar()), cell(swar()))))
  }

  test("hasOrnaments returns false for line with only rests") {
    assert(!GridLineUtil.hasOrnaments(line(cell(rest), cell(rest))))
  }

  test("hasOrnaments returns true when a swar has ornaments") {
    val ornSwar = swar(ornaments = List(Gamak()))
    assert(GridLineUtil.hasOrnaments(line(cell(swar()), cell(ornSwar))))
  }

  test("hasOrnaments returns true with meend ornament") {
    val meend = Meend(saRef, NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya), MeendDirection.Ascending, Nil)
    assert(GridLineUtil.hasOrnaments(line(cell(swar(ornaments = List(meend))))))
  }

  test("hasOrnaments ignores rests when checking") {
    assert(!GridLineUtil.hasOrnaments(line(cell(rest, swar()))))
  }

  // --- hasSahitya ---

  test("hasSahitya returns false for empty line") {
    assert(!GridLineUtil.hasSahitya(line()))
  }

  test("hasSahitya returns false for line with plain swars") {
    assert(!GridLineUtil.hasSahitya(line(cell(swar()), cell(swar()))))
  }

  test("hasSahitya returns false for line with only rests") {
    assert(!GridLineUtil.hasSahitya(line(cell(rest))))
  }

  test("hasSahitya returns true when a swar has sahitya") {
    val sahityaSwar = swar(sahitya = Some("पा"))
    assert(GridLineUtil.hasSahitya(line(cell(swar()), cell(sahityaSwar))))
  }

  test("hasSahitya returns true for single cell with sahitya") {
    assert(GridLineUtil.hasSahitya(line(cell(swar(sahitya = Some("lyrics"))))))
  }

  test("hasSahitya ignores rests when checking") {
    assert(!GridLineUtil.hasSahitya(line(cell(rest, swar()))))
  }

  // --- combined ---

  test("swar with both ornaments and sahitya triggers both checks") {
    val bothSwar = swar(ornaments = List(Andolan()), sahitya = Some("धा"))
    val l        = line(cell(bothSwar))
    assert(GridLineUtil.hasOrnaments(l))
    assert(GridLineUtil.hasSahitya(l))
  }
