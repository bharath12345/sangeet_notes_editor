package com.varpas.sangeet.core.render

import org.scalatest.funsuite.AnyFunSuite

import com.varpas.sangeet.core.model._

class OrnamentLabelsSpec extends AnyFunSuite:

  private val saRef = NoteRef(Note.Sa, Variant.Shuddha, Octave.Madhya)
  private val reRef = NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya)

  // --- full labels ---

  test("full label for Meend") {
    assert(OrnamentLabels.full(Meend(saRef, reRef, MeendDirection.Ascending, Nil)) == "meend")
  }

  test("full label for KanSwar") {
    assert(OrnamentLabels.full(KanSwar(saRef)) == "kan")
  }

  test("full label for Gamak") {
    assert(OrnamentLabels.full(Gamak()) == "gamak")
  }

  test("full label for Andolan") {
    assert(OrnamentLabels.full(Andolan()) == "andolan")
  }

  test("full label for Gitkari") {
    assert(OrnamentLabels.full(Gitkari()) == "gitkari")
  }

  test("full label for Murki") {
    assert(OrnamentLabels.full(Murki(List(saRef, reRef))) == "murki")
  }

  test("full label for Krintan") {
    assert(OrnamentLabels.full(Krintan(List(saRef))) == "krintan")
  }

  test("full label for Ghaseet") {
    assert(OrnamentLabels.full(Ghaseet(reRef)) == "ghaseet")
  }

  test("full label for Sparsh") {
    assert(OrnamentLabels.full(Sparsh(reRef)) == "sparsh")
  }

  test("full label for Zamzama") {
    assert(OrnamentLabels.full(Zamzama(List(saRef, reRef))) == "zamzama")
  }

  test("full label for CustomOrnament uses custom name") {
    assert(OrnamentLabels.full(CustomOrnament("slide", Map.empty)) == "slide")
  }

  // --- abbreviated labels ---

  test("abbreviated label for Meend") {
    assert(OrnamentLabels.abbreviated(Meend(saRef, reRef, MeendDirection.Ascending, Nil)) == "~")
  }

  test("abbreviated label for KanSwar") {
    assert(OrnamentLabels.abbreviated(KanSwar(saRef)) == "k")
  }

  test("abbreviated label for Gamak") {
    assert(OrnamentLabels.abbreviated(Gamak()) == "G")
  }

  test("abbreviated label for Andolan") {
    assert(OrnamentLabels.abbreviated(Andolan()) == "A")
  }

  test("abbreviated label for Gitkari") {
    assert(OrnamentLabels.abbreviated(Gitkari()) == "tr")
  }

  test("abbreviated label for Murki") {
    assert(OrnamentLabels.abbreviated(Murki(List(saRef))) == "m")
  }

  test("abbreviated label for Krintan") {
    assert(OrnamentLabels.abbreviated(Krintan(List(saRef))) == "kr")
  }

  test("abbreviated label for Ghaseet") {
    assert(OrnamentLabels.abbreviated(Ghaseet(reRef)) == "gh")
  }

  test("abbreviated label for Sparsh") {
    assert(OrnamentLabels.abbreviated(Sparsh(reRef)) == "sp")
  }

  test("abbreviated label for Zamzama") {
    assert(OrnamentLabels.abbreviated(Zamzama(List(saRef))) == "zz")
  }

  test("abbreviated label for CustomOrnament uses custom name") {
    assert(OrnamentLabels.abbreviated(CustomOrnament("trill", Map("speed" -> "fast"))) == "trill")
  }
