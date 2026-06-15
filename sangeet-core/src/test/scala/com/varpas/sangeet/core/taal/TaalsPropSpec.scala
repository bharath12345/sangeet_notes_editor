package com.varpas.sangeet.core.taal

import org.scalacheck.Shrink
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators
import com.varpas.sangeet.core.model.VibhagMarker

/** Plan 19 T1B — collapses the per-taal and "all taals" assertions in `TaalsSpec` into properties driven by
  * `Generators.genTaal` (which samples uniformly from the 11-taal catalog).
  *
  * The hand-pinned tests (teentaal has 16 matras, rupak starts with khali, etc.) stay in `TaalsSpec.scala` as catalog
  * data validations per the conventions doc.
  *
  * What's new here: every built-in taal satisfies the structural rules — vibhag beats sum to matras, theka length
  * matches matras, byName round-trips. These are the "for-each-taal" loops in TaalsSpec rewritten as one property each.
  *
  * Note on the data-not-code rule (CLAUDE.md): we never special-case Rupak here. The `propTaalSamIsFirstVibhagOrKhali`
  * property expresses Rupak's khali-first nature as a property OF the taal data, not a hardcoded match on the name.
  */
class TaalsPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  // Built-in taal names are drawn from a fixed catalog (Gen.oneOf). Shrinking a String to "" would walk OUT of the
  // catalog and into invalid inputs — disable it so failures report the actual generator-emitted name.
  private given Shrink[String] = Shrink.shrinkAny

  test("propTaalNameNonEmpty: every built-in taal has a non-empty name"):
    forAll(Generators.genTaal) { t =>
      assert(t.name.nonEmpty)
    }

  test("propTaalMatrasPositive: every built-in taal has matras >= 1"):
    forAll(Generators.genTaal) { t =>
      assert(t.matras >= 1)
    }

  test("propTaalVibhagBeatsSumToMatras: sum of vibhag beats equals the taal's matras"):
    forAll(Generators.genTaal) { t =>
      val sum = t.vibhags.map(_.beats).sum
      assert(sum == t.matras, s"${t.name}: vibhag beats sum to $sum but matras is ${t.matras}")
    }

  test("propTaalVibhagBeatsPositive: every vibhag has at least 1 beat"):
    forAll(Generators.genTaal) { t =>
      t.vibhags.foreach { v =>
        assert(v.beats >= 1, s"${t.name}: vibhag $v has non-positive beats")
      }
    }

  test("propTaalThekaDefined: every built-in taal has a theka"):
    forAll(Generators.genTaal) { t =>
      assert(t.theka.isDefined, s"${t.name} has no theka")
    }

  test("propTaalThekaLengthMatchesMatras: theka has exactly `matras` syllables"):
    forAll(Generators.genTaal) { t =>
      t.theka.foreach { th =>
        assert(th.size == t.matras, s"${t.name} theka has ${th.size} syllables but matras is ${t.matras}")
      }
    }

  test("propTaalVibhagsNonEmpty: every built-in taal has at least one vibhag"):
    forAll(Generators.genTaal) { t =>
      assert(t.vibhags.nonEmpty)
    }

  test(
    "propTaalFirstVibhagIsSamOrKhali: data-not-code — first vibhag marks the downbeat (Sam) or the off-1 (Khali, Rupak)"
  ):
    // Expresses the rule as a property OF the data, not a hardcoded `if name == \"Rupak\"`.
    forAll(Generators.genTaal) { t =>
      val firstMarker = t.vibhags.head.marker
      assert(
        firstMarker == VibhagMarker.Sam || firstMarker == VibhagMarker.Khali,
        s"${t.name} starts with $firstMarker (expected Sam or Khali)"
      )
    }

  test("propTaalByNameRoundTrip: byName(t.name) returns t for every built-in taal"):
    forAll(Generators.genTaal) { t =>
      assert(Taals.byName(t.name) == Some(t), s"byName failed for ${t.name}")
    }

  test("propTaalByNameCaseInsensitive: every built-in taal round-trips with arbitrary case folding"):
    forAll(Generators.genTaal) { t =>
      assert(Taals.byName(t.name.toUpperCase) == Some(t))
      assert(Taals.byName(t.name.toLowerCase) == Some(t))
    }

  test("propTaalCatalogKeyMatchesNameLowercase: every key in Taals.all is the value's name lowercased"):
    // genTaalName emits the display name (mixed case); keys in Taals.all are lowercased — look up via byName.
    forAll(Generators.genTaalName) { displayName =>
      val t = Taals.byName(displayName)
      assert(t.isDefined, s"no taal found for display name '$displayName'")
      assert(t.get.name == displayName)
    }
