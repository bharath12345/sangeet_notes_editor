package com.varpas.sangeet.core.model

import org.scalatest.funsuite.AnyFunSuite

class OrnamentSpec extends AnyFunSuite:

  val sampleNoteRef  = NoteRef(Note.Sa, Variant.Shuddha, Octave.Madhya)
  val sampleNoteRef2 = NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya)

  test("Meend ornament construction") {
    val meend = Meend(sampleNoteRef, sampleNoteRef2, MeendDirection.Ascending, List.empty)
    assert(meend.startNote == sampleNoteRef)
    assert(meend.endNote == sampleNoteRef2)
    assert(meend.direction == MeendDirection.Ascending)
  }

  test("KanSwar ornament construction") {
    val kan = KanSwar(sampleNoteRef)
    assert(kan.graceNote == sampleNoteRef)
  }

  test("Murki ornament construction") {
    val murki = Murki(List(sampleNoteRef, sampleNoteRef2))
    assert(murki.notes.size == 2)
  }

  test("Gamak ornament construction") {
    val gamak = Gamak()
    assert(gamak.isInstanceOf[Ornament])
  }

  test("Andolan ornament construction") {
    val andolan = Andolan()
    assert(andolan.isInstanceOf[Ornament])
  }

  test("Krintan ornament construction") {
    val krintan = Krintan(List(sampleNoteRef))
    assert(krintan.notes.size == 1)
  }

  test("Gitkari ornament construction") {
    val gitkari = Gitkari()
    assert(gitkari.isInstanceOf[Ornament])
  }

  test("Ghaseet ornament construction") {
    val ghaseet = Ghaseet(sampleNoteRef)
    assert(ghaseet.targetNote == sampleNoteRef)
  }

  test("Sparsh ornament construction") {
    val sparsh = Sparsh(sampleNoteRef)
    assert(sparsh.touchNote == sampleNoteRef)
  }

  test("Zamzama ornament construction") {
    val zamzama = Zamzama(List(sampleNoteRef, sampleNoteRef2))
    assert(zamzama.notes.size == 2)
  }

  test("CustomOrnament construction") {
    val custom = CustomOrnament("MyOrnament", Map("param1" -> "value1"))
    assert(custom.name == "MyOrnament")
    assert(custom.parameters("param1") == "value1")
  }
