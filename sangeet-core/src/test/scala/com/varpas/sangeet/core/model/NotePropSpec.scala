package com.varpas.sangeet.core.model

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators

/** Plan 19 T1B — collapses `NoteSpec`'s enum-cardinality cases plus encodes the *achal* rule (Sa and Pa never carry
  * Komal/Tivra) as a property tested against every generator-driven `NoteRef`.
  *
  * The cardinality assertions still live in `NoteSpec.scala` (catalog tests — see conventions). What's new here is the
  * structural rules that hold across the whole domain.
  */
class NotePropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  test("propNoteEnumExhaustive: every generated Note is one of the 7 expected values"):
    val all = Set(Note.Sa, Note.Re, Note.Ga, Note.Ma, Note.Pa, Note.Dha, Note.Ni)
    forAll(Generators.genNote) { n =>
      assert(all.contains(n))
    }

  test("propVariantEnumExhaustive: every generated Variant is Shuddha/Komal/Tivra"):
    val all = Set(Variant.Shuddha, Variant.Komal, Variant.Tivra)
    forAll(Generators.genVariant) { v =>
      assert(all.contains(v))
    }

  test("propOctaveEnumExhaustive: every generated Octave is one of the 5 values"):
    val all = Set(Octave.AtiMandra, Octave.Mandra, Octave.Madhya, Octave.Taar, Octave.AtiTaar)
    forAll(Generators.genOctave) { o =>
      assert(all.contains(o))
    }

  test("propSaPaAchalRule: Sa and Pa from generators always carry Shuddha (achal)"):
    // The generator enforces the rule; this property pins it as a regression — if `variantFor` ever drifts to allow
    // Komal/Tivra on Sa/Pa, this fires within the first ~14 generated cases.
    forAll(Generators.genNoteRef) { nr =>
      if nr.note == Note.Sa || nr.note == Note.Pa then assert(nr.variant == Variant.Shuddha)
    }

  test("propReGaDhaNiAchalRule: Re/Ga/Dha/Ni never carry Tivra (only Shuddha or Komal are valid)"):
    forAll(Generators.genNoteRef) { nr =>
      nr.note match
        case Note.Re | Note.Ga | Note.Dha | Note.Ni =>
          assert(nr.variant != Variant.Tivra, s"$nr should not carry Tivra")
        case _ => ()
    }

  test("propMaAchalRule: Ma never carries Komal (only Shuddha or Tivra are valid)"):
    forAll(Generators.genNoteRef) { nr =>
      if nr.note == Note.Ma then assert(nr.variant != Variant.Komal, s"$nr should not carry Komal")
    }

  test("propVariantForRespectsAchalForEveryNote: hand-driven sweep of Note × generator outcomes"):
    // Independent check that doesn't go through `genNoteRef` — directly walks every Note and samples `variantFor`.
    forAll(Generators.genNote, Gen.choose(0, 100)) { (note, _) =>
      val v = Generators.variantFor(note).sample.get
      note match
        case Note.Sa | Note.Pa                      => assert(v == Variant.Shuddha)
        case Note.Re | Note.Ga | Note.Dha | Note.Ni => assert(v == Variant.Shuddha || v == Variant.Komal)
        case Note.Ma                                => assert(v == Variant.Shuddha || v == Variant.Tivra)
    }
