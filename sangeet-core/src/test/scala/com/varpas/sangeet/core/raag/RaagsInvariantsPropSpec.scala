package com.varpas.sangeet.core.raag

import org.scalacheck.Shrink
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators

/** Plan 19 T1C — Phase C gap-fill for the 26-raag catalog.
  *
  * T1B's `RaagsPropSpec` covers the easy structural well-formedness (non-empty name, defined fields, prahar range,
  * `byName` round-trip). What's missing — and what only a property over the whole catalog can guarantee — is the
  * stricter *Hindustani-music-theory* invariants on the arohana/avarohana note sequences and the vadi/samvadi tokens.
  *
  * Properties added here:
  *   - arohana always starts on "Sa" (the tonic; every ascent begins at the ground)
  *   - avarohana always ends on "Sa" (the descent resolves back to the tonic)
  *   - arohana always ends on "Sa'" (the upper-octave Sa)
  *   - avarohana always starts on "Sa'"
  *   - vadi and samvadi are drawn from the seven valid base swar tokens (Sa, Re, Ga, Ma, Pa, Dha, Ni) — they identify
  *     the dominant and sub-dominant notes of the raag and so must be one of the seven base notes
  *
  * These rules come from the `hindustani-music-theory` skill — see CLAUDE.md for the achal rule (Sa and Pa are fixed).
  * If a future raag is added that violates any of these, this spec fires immediately.
  */
class RaagsInvariantsPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  // Built-in raag names live in a fixed catalog (Gen.oneOf). Shrinking a String back to "" would walk out of the
  // catalog into invalid inputs — disable it so failures surface the actual generator-emitted raag.
  private given Shrink[String] = Shrink.shrinkAny

  /** The seven base swar tokens — the only valid values for vadi/samvadi (no variant or octave markers). */
  private val baseSwarTokens: Set[String] = Set("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni")

  test("propRaagArohanaStartsOnSa: every built-in raag's arohana begins on Sa (the tonic)"):
    forAll(Generators.genRaag) { r =>
      r.arohana.foreach { aro =>
        assert(aro.nonEmpty, s"${r.name}: arohana is empty")
        assert(aro.head == "Sa", s"${r.name}: arohana starts on '${aro.head}', expected 'Sa'")
      }
    }

  test("propRaagArohanaEndsOnUpperSa: every built-in raag's arohana ends on Sa' (the upper-octave Sa)"):
    forAll(Generators.genRaag) { r =>
      r.arohana.foreach { aro =>
        assert(aro.last == "Sa'", s"${r.name}: arohana ends on '${aro.last}', expected 'Sa''")
      }
    }

  test("propRaagAvarohanaStartsOnUpperSa: every built-in raag's avarohana begins on Sa'"):
    forAll(Generators.genRaag) { r =>
      r.avarohana.foreach { ava =>
        assert(ava.nonEmpty)
        assert(ava.head == "Sa'", s"${r.name}: avarohana starts on '${ava.head}', expected 'Sa''")
      }
    }

  test("propRaagAvarohanaEndsOnSa: every built-in raag's avarohana resolves to Sa"):
    forAll(Generators.genRaag) { r =>
      r.avarohana.foreach { ava =>
        assert(ava.last == "Sa", s"${r.name}: avarohana ends on '${ava.last}', expected 'Sa'")
      }
    }

  test("propRaagVadiIsBaseSwarToken: vadi is one of {Sa, Re, Ga, Ma, Pa, Dha, Ni}"):
    forAll(Generators.genRaag) { r =>
      r.vadi.foreach { v =>
        assert(baseSwarTokens.contains(v), s"${r.name}: vadi '$v' is not a base swar token")
      }
    }

  test("propRaagSamvadiIsBaseSwarToken: samvadi is one of {Sa, Re, Ga, Ma, Pa, Dha, Ni}"):
    forAll(Generators.genRaag) { r =>
      r.samvadi.foreach { s =>
        assert(baseSwarTokens.contains(s), s"${r.name}: samvadi '$s' is not a base swar token")
      }
    }
