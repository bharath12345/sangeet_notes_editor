package com.varpas.sangeet.core.raag

import org.scalacheck.Shrink
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators

/** Plan 19 T1B — collapses the per-raag and "all raags should have ..." assertions in `RaagsSpec` into properties that
  * sample from the 26-raag catalog via `Generators.genRaag` and verify the well-formedness rules in one place.
  *
  * The hand-pinned catalog tests (yaman has Kalyan thaat, malkauns prahar is 3, etc.) stay in `RaagsSpec.scala` —
  * they're catalog data validations and `docs/developer/testing/property-based-testing.md` keeps them as examples.
  *
  * What's new here: an arbitrary `Raag` drawn from the catalog satisfies the structural rules every built-in raag must
  * follow, regardless of which one is picked.
  */
class RaagsPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  // Built-in raag names are drawn from a fixed catalog (Gen.oneOf). Shrinking a String back to "" would walk OUT of
  // the catalog and into invalid inputs — disable it so failures report the actual generator-emitted name.
  private given Shrink[String] = Shrink.shrinkAny

  test("propRaagNameNonEmpty: every built-in raag has a non-empty name"):
    forAll(Generators.genRaag) { r =>
      assert(r.name.nonEmpty)
      assert(r.name.trim == r.name, s"raag name has leading/trailing whitespace: '${r.name}'")
    }

  test("propRaagArohanaDefinedAndNonEmpty: every built-in raag has a defined, non-empty arohana"):
    forAll(Generators.genRaag) { r =>
      assert(r.arohana.isDefined, s"${r.name} has no arohana")
      assert(r.arohana.get.nonEmpty, s"${r.name} has empty arohana")
    }

  test("propRaagAvarohanaDefinedAndNonEmpty: every built-in raag has a defined, non-empty avarohana"):
    forAll(Generators.genRaag) { r =>
      assert(r.avarohana.isDefined, s"${r.name} has no avarohana")
      assert(r.avarohana.get.nonEmpty, s"${r.name} has empty avarohana")
    }

  test("propRaagThaatDefined: every built-in raag has a thaat"):
    forAll(Generators.genRaag) { r =>
      assert(r.thaat.isDefined, s"${r.name} has no thaat")
      assert(r.thaat.get.nonEmpty)
    }

  test("propRaagVadiDefined: every built-in raag has a vadi"):
    forAll(Generators.genRaag) { r =>
      assert(r.vadi.isDefined, s"${r.name} has no vadi")
    }

  test("propRaagSamvadiDefined: every built-in raag has a samvadi"):
    forAll(Generators.genRaag) { r =>
      assert(r.samvadi.isDefined, s"${r.name} has no samvadi")
    }

  test("propRaagPraharInValidRange: prahar (when present) is in 1..4"):
    forAll(Generators.genRaag) { r =>
      r.prahar.foreach { p =>
        assert(p >= 1 && p <= 4, s"${r.name} has invalid prahar $p")
      }
    }

  test("propRaagByNameRoundTrip: byName(r.name) returns r for every built-in raag"):
    forAll(Generators.genRaag) { r =>
      assert(Raags.byName(r.name) == Some(r), s"byName failed for ${r.name}")
    }

  test("propRaagByNameCaseInsensitive: every built-in raag round-trips with arbitrary case folding"):
    forAll(Generators.genRaag) { r =>
      assert(Raags.byName(r.name.toUpperCase) == Some(r))
      assert(Raags.byName(r.name.toLowerCase) == Some(r))
    }

  test("propRaagByNameTrimsWhitespace: leading/trailing whitespace is stripped"):
    forAll(Generators.genRaag) { r =>
      assert(Raags.byName(s"  ${r.name}  ") == Some(r))
    }

  test(
    "propRaagCatalogContainsAllRegistered: every key in Raags.all maps to a Raag with matching name (case-insensitive)"
  ):
    // genRaagName emits the display name (mixed case); keys in Raags.all are lowercased — look up via byName.
    forAll(Generators.genRaagName) { displayName =>
      val r = Raags.byName(displayName)
      assert(r.isDefined, s"no raag found for display name '$displayName'")
      assert(r.get.name == displayName)
    }
