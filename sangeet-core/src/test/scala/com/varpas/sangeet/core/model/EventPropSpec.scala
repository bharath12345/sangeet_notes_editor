package com.varpas.sangeet.core.model

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators
import com.varpas.sangeet.core.generators.Generators.given

/** Plan 19 T1B — properties for every concrete `Event` subtype produced by the generator. Subsumes the construction +
  * accessor assertions scattered across `CompositionSpec` and the per-event-type helpers in `CompositionEditorSpec`.
  *
  * Key laws:
  *   - `event.withPosition(event.position)` is an identity
  *   - For any new position `p`, `event.withPosition(p).position == p`
  *   - `eventDuration` is non-negative for every generated event
  *   - Every `Event` matches exactly one of the 5 enum cases
  */
class EventPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  test("propEventPositionIdentity: withPosition(position) is a no-op"):
    forAll { (e: Event) =>
      assert(e.withPosition(e.position) == e)
    }

  test("propEventWithPositionRoundTrip: withPosition(p).position == p"):
    forAll { (e: Event, p: BeatPosition) =>
      assert(e.withPosition(p).position == p)
    }

  test("propEventDurationNonNegative: every generated event has a non-negative duration"):
    forAll { (e: Event) =>
      assert(e.eventDuration >= Rational(0, 1))
    }

  test("propEventExhaustivePatternMatch: every Event matches exactly one of the 5 cases"):
    forAll { (e: Event) =>
      val matched = e match
        case _: Event.Swar       => "swar"
        case _: Event.Rest       => "rest"
        case _: Event.Sustain    => "sustain"
        case _: Event.Chikari    => "chikari"
        case _: Event.LockedBeat => "locked"
      assert(matched.nonEmpty)
    }

  test("propSwarConstructible: every generated Swar is a Swar and round-trips through copy"):
    forAll(Generators.genSwar) { s =>
      assert(s.isInstanceOf[Event.Swar])
      assert(s.copy() == s)
      assert(s.position == s.beat)
      assert(s.eventDuration == s.duration)
    }

  test("propRestConstructible: every generated Rest round-trips through copy"):
    forAll(Generators.genRest) { r =>
      assert(r.copy() == r)
      assert(r.position == r.beat)
    }

  test("propSustainConstructible: every generated Sustain round-trips through copy"):
    forAll(Generators.genSustain) { u =>
      assert(u.copy() == u)
      assert(u.position == u.beat)
    }

  test("propChikariConstructible: every generated Chikari round-trips through copy"):
    forAll(Generators.genChikari) { c =>
      assert(c.copy() == c)
      assert(c.position == c.beat)
    }

  test("propLockedBeatConstructible: every generated LockedBeat round-trips through copy"):
    forAll(Generators.genLockedBeat) { l =>
      assert(l.copy() == l)
      assert(l.position == l.beat)
    }

  test("propSwarAchalConsistency: every generated Swar has variant valid for its note"):
    // The generator enforces the rule; this property pins it as a regression for `genSwar` specifically.
    forAll(Generators.genSwar) { s =>
      s.note match
        case Note.Sa | Note.Pa => assert(s.variant == Variant.Shuddha)
        case Note.Re | Note.Ga | Note.Dha | Note.Ni =>
          assert(s.variant != Variant.Tivra)
        case Note.Ma => assert(s.variant != Variant.Komal)
    }
