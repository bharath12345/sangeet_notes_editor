package com.varpas.sangeet.core.model

import org.scalatest.funsuite.AnyFunSuite

class NoteSpec extends AnyFunSuite:

  test("Note enum has all 7 notes") {
    val notes = Note.values.toSet
    assert(notes.size == 7)
    assert(notes.contains(Note.Sa))
    assert(notes.contains(Note.Re))
    assert(notes.contains(Note.Ga))
    assert(notes.contains(Note.Ma))
    assert(notes.contains(Note.Pa))
    assert(notes.contains(Note.Dha))
    assert(notes.contains(Note.Ni))
  }

  test("Variant enum has all 3 variants") {
    val variants = Variant.values.toSet
    assert(variants.size == 3)
    assert(variants.contains(Variant.Shuddha))
    assert(variants.contains(Variant.Komal))
    assert(variants.contains(Variant.Tivra))
  }

  test("Octave enum has all 5 octaves") {
    val octaves = Octave.values.toSet
    assert(octaves.size == 5)
    assert(octaves.contains(Octave.AtiMandra))
    assert(octaves.contains(Octave.Mandra))
    assert(octaves.contains(Octave.Madhya))
    assert(octaves.contains(Octave.Taar))
    assert(octaves.contains(Octave.AtiTaar))
  }
