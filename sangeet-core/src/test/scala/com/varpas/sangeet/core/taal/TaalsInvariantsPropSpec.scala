package com.varpas.sangeet.core.taal

import org.scalacheck.Shrink
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators
import com.varpas.sangeet.core.model.VibhagMarker

/** Plan 19 T1C — Phase C gap-fill for the 11-taal catalog.
  *
  * T1B's `TaalsPropSpec` covers the basic counting invariants (matras > 0, vibhag beats sum to matras, theka length
  * matches matras, byName round-trip). What's missing — and what catches the *data-not-code* class of bugs — is the
  * structural rules on the vibhag-marker sequence itself.
  *
  * Properties added here:
  *   - at most one Sam marker per taal — Rupak has zero (khali-first); everyone else has exactly one
  *   - Taali numbers within a single taal are unique — no two vibhags share the same Taali(N)
  *   - first vibhag always carries an "anchor" marker (Sam or Khali), never an unnumbered Taali
  *   - every vibhag's beat count is in [1..matras]
  *
  * These reinforce the CLAUDE.md rule "Taals are data, not code": if behaviour depends on whether a taal starts on Sam,
  * the test asserts the property of the data — not a hardcoded match on the taal's name.
  */
class TaalsInvariantsPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  // Built-in taal names live in a fixed catalog (Gen.oneOf). Shrinking a String back to "" would walk out of the
  // catalog — disable it so failures surface the actual generator-emitted taal.
  private given Shrink[String] = Shrink.shrinkAny

  test("propTaalAtMostOneSamMarker: every built-in taal has 0 or 1 Sam markers (Rupak is the well-known 0 case)"):
    forAll(Generators.genTaal) { t =>
      val samCount = t.vibhags.count(_.marker == VibhagMarker.Sam)
      assert(samCount <= 1, s"${t.name}: has $samCount Sam markers, expected at most 1")
    }

  test("propTaalTaaliNumbersUnique: within any built-in taal, Taali(N) numbers are distinct"):
    forAll(Generators.genTaal) { t =>
      val numbers = t.vibhags.map(_.marker).collect { case VibhagMarker.Taali(n) => n }
      assert(numbers.distinct == numbers, s"${t.name}: duplicate Taali numbers in $numbers")
    }

  test("propTaalFirstVibhagIsSamOrKhali: first vibhag is the cycle anchor — Sam or Khali, never an isolated Taali"):
    forAll(Generators.genTaal) { t =>
      val firstMarker = t.vibhags.head.marker
      assert(
        firstMarker == VibhagMarker.Sam || firstMarker == VibhagMarker.Khali,
        s"${t.name}: first vibhag is $firstMarker; expected Sam or Khali"
      )
    }

  test("propTaalEveryVibhagBeatsConsistent: each vibhag's beats fit inside the taal's total matras"):
    forAll(Generators.genTaal) { t =>
      t.vibhags.foreach { v =>
        assert(v.beats >= 1 && v.beats <= t.matras, s"${t.name}: vibhag with ${v.beats} beats out of [1..${t.matras}]")
      }
    }
