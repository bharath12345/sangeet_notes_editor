package com.varpas.sangeet.core.model

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators
import com.varpas.sangeet.core.generators.Generators.given

/** Plan 19 T1B — collapses `OrnamentSpec`'s 11 hand-written "X ornament construction" cases into single properties that
  * cover every concrete `Ornament` subtype produced by the generator.
  *
  * The original example tests assert two things per case: (a) the ornament is constructible without throwing, and (b)
  * fields round-trip through the case-class accessors. These properties subsume both shape checks for every value
  * `genOrnament` can emit (Meend, KanSwar, Murki, Gamak, Andolan, Krintan, Gitkari, Ghaseet, Sparsh, Zamzama,
  * CustomOrnament).
  *
  * Naming follows `docs/developer/testing/property-based-testing.md`.
  */
class OrnamentPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  test("propOrnamentClosed: every generated ornament is an instance of Ornament"):
    forAll { (o: Ornament) =>
      assert(o.isInstanceOf[Ornament])
    }

  test("propOrnamentExhaustivePatternMatch: every ornament matches exactly one of the 11 subtypes"):
    forAll { (o: Ornament) =>
      val matched = o match
        case _: Meend          => 1
        case _: KanSwar        => 1
        case _: Murki          => 1
        case _: Gamak          => 1
        case _: Andolan        => 1
        case _: Krintan        => 1
        case _: Gitkari        => 1
        case _: Ghaseet        => 1
        case _: Sparsh         => 1
        case _: Zamzama        => 1
        case _: CustomOrnament => 1
      assert(matched == 1)
    }

  test("propMeendAccessorsRoundTrip: Meend preserves start/end/direction/intermediate fields"):
    forAll(Generators.genMeend) { m =>
      val rebuilt = Meend(m.startNote, m.endNote, m.direction, m.intermediateNotes)
      assert(rebuilt == m)
      assert(rebuilt.startNote == m.startNote)
      assert(rebuilt.endNote == m.endNote)
      assert(rebuilt.direction == m.direction)
      assert(rebuilt.intermediateNotes == m.intermediateNotes)
    }

  test("propKanSwarAccessorRoundTrip: KanSwar preserves graceNote"):
    forAll(Generators.genKanSwar) { k =>
      assert(KanSwar(k.graceNote) == k)
    }

  test("propMurkiAccessorRoundTrip: Murki preserves notes list"):
    forAll(Generators.genMurki) { m =>
      assert(Murki(m.notes) == m)
      assert(m.notes.nonEmpty) // generator emits 1..3 notes
    }

  test("propKrintanAccessorRoundTrip: Krintan preserves notes list"):
    forAll(Generators.genKrintan) { k =>
      assert(Krintan(k.notes) == k)
      assert(k.notes.nonEmpty)
    }

  test("propZamzamaAccessorRoundTrip: Zamzama preserves notes list"):
    forAll(Generators.genZamzama) { z =>
      assert(Zamzama(z.notes) == z)
      assert(z.notes.nonEmpty)
    }

  test("propGhaseetAccessorRoundTrip: Ghaseet preserves targetNote"):
    forAll(Generators.genGhaseet) { g =>
      assert(Ghaseet(g.targetNote) == g)
    }

  test("propSparshAccessorRoundTrip: Sparsh preserves touchNote"):
    forAll(Generators.genSparsh) { s =>
      assert(Sparsh(s.touchNote) == s)
    }

  test("propGamakIsSingleton: every Gamak is equal to a default-constructed Gamak"):
    forAll(Generators.genGamak) { g =>
      assert(g == Gamak())
    }

  test("propAndolanIsSingleton: every Andolan is equal to a default-constructed Andolan"):
    forAll(Generators.genAndolan) { a =>
      assert(a == Andolan())
    }

  test("propGitkariIsSingleton: every Gitkari is equal to a default-constructed Gitkari"):
    forAll(Generators.genGitkari) { g =>
      assert(g == Gitkari())
    }

  test("propCustomOrnamentAccessorRoundTrip: CustomOrnament preserves name + parameters"):
    forAll(Generators.genCustomOrnament) { c =>
      val rebuilt = CustomOrnament(c.name, c.parameters)
      assert(rebuilt == c)
      assert(rebuilt.name == c.name)
      assert(rebuilt.parameters == c.parameters)
    }
