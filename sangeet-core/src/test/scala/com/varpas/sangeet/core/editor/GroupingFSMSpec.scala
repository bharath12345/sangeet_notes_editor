package com.varpas.sangeet.core.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.core.model._

class GroupingFSMSpec extends AnyFlatSpec with Matchers:

  private val sa: GroupingFSM.GroupedNote = (Note.Sa, Variant.Shuddha, Octave.Madhya)
  private val re: GroupingFSM.GroupedNote = (Note.Re, Variant.Shuddha, Octave.Madhya)
  private val ga: GroupingFSM.GroupedNote = (Note.Ga, Variant.Shuddha, Octave.Madhya)
  private val ma: GroupingFSM.GroupedNote = (Note.Ma, Variant.Shuddha, Octave.Madhya)
  private val pa: GroupingFSM.GroupedNote = (Note.Pa, Variant.Shuddha, Octave.Madhya)

  private val cursor0 = GroupingFSM.CursorTriple(beat = 0, cycle = 0, subIndex = 0)
  private val cursor1 = GroupingFSM.CursorTriple(beat = 1, cycle = 0, subIndex = 0)
  private val cursor5 = GroupingFSM.CursorTriple(beat = 5, cycle = 0, subIndex = 0)

  // --- decide ---

  "GroupingFSM.decide" should "StartNew when there is no in-progress group" in {
    GroupingFSM.decide(None, nowMs = 1000L, observed = cursor0, thisNote = sa) shouldBe GroupingFSM.Decision.StartNew
  }

  it should "Extend when within threshold, under cap, and cursor aligned" in {
    val state = GroupingFSM.State(
      notes = List(sa),
      beat = 0,
      cycle = 0,
      lastTypedTimeMs = 1000L,
      nextCursor = cursor1
    )
    GroupingFSM.decide(Some(state), nowMs = 1200L, observed = cursor1, thisNote = re) shouldBe
      GroupingFSM.Decision.Extend(List(sa, re))
  }

  it should "StartNew when more than ThresholdMs has elapsed (sliding window)" in {
    val state = GroupingFSM.State(
      notes = List(sa),
      beat = 0,
      cycle = 0,
      lastTypedTimeMs = 1000L,
      nextCursor = cursor1
    )
    // 1500ms - 1000ms = 500ms which is NOT < ThresholdMs (500)
    GroupingFSM.decide(
      Some(state),
      nowMs = 1500L,
      observed = cursor1,
      thisNote = re
    ) shouldBe GroupingFSM.Decision.StartNew
  }

  it should "Extend when exactly one ms before the threshold boundary" in {
    val state = GroupingFSM.State(
      notes = List(sa),
      beat = 0,
      cycle = 0,
      lastTypedTimeMs = 1000L,
      nextCursor = cursor1
    )
    // 1499 - 1000 = 499 < 500 → extend
    GroupingFSM.decide(Some(state), nowMs = 1499L, observed = cursor1, thisNote = re) shouldBe
      GroupingFSM.Decision.Extend(List(sa, re))
  }

  it should "StartNew when the group already has MaxGroupSize notes" in {
    val state = GroupingFSM.State(
      notes = List(sa, re, ga, ma), // 4 notes — at the cap
      beat = 0,
      cycle = 0,
      lastTypedTimeMs = 1000L,
      nextCursor = cursor1
    )
    GroupingFSM.decide(
      Some(state),
      nowMs = 1100L,
      observed = cursor1,
      thisNote = pa
    ) shouldBe GroupingFSM.Decision.StartNew
  }

  it should "StartNew when the observed cursor has drifted from nextCursor (bug 4)" in {
    val state = GroupingFSM.State(
      notes = List(sa),
      beat = 0,
      cycle = 0,
      lastTypedTimeMs = 1000L,
      nextCursor = cursor5 // group expected cursor to be at beat=5
    )
    // observed is at beat=0 — user navigated back between keystrokes
    GroupingFSM.decide(
      Some(state),
      nowMs = 1100L,
      observed = cursor0,
      thisNote = re
    ) shouldBe GroupingFSM.Decision.StartNew
  }

  it should "Extend when cursor differs only in fields the alignment check ignores" in {
    // The CursorTriple only tracks beat/cycle/subIndex — the FSM is invariant to
    // anything else (e.g. octave / selection anchors carried on a CursorModel).
    val state = GroupingFSM.State(
      notes = List(sa),
      beat = 0,
      cycle = 0,
      lastTypedTimeMs = 1000L,
      nextCursor = cursor1
    )
    GroupingFSM.decide(Some(state), nowMs = 1100L, observed = cursor1, thisNote = re) shouldBe
      GroupingFSM.Decision.Extend(List(sa, re))
  }

  it should "Extend up to exactly MaxGroupSize on consecutive keystrokes" in {
    val s1 = GroupingFSM.startedState(cursor0, sa, nowMs = 1000L, postInsertCursor = cursor1)
    val d2 = GroupingFSM.decide(Some(s1), nowMs = 1100L, observed = cursor1, thisNote = re)
    d2 shouldBe GroupingFSM.Decision.Extend(List(sa, re))
    val s2 = GroupingFSM.extendedState(s1, List(sa, re), nowMs = 1100L, newNextCursor = cursor1)

    val d3 = GroupingFSM.decide(Some(s2), nowMs = 1200L, observed = cursor1, thisNote = ga)
    d3 shouldBe GroupingFSM.Decision.Extend(List(sa, re, ga))
    val s3 = GroupingFSM.extendedState(s2, List(sa, re, ga), nowMs = 1200L, newNextCursor = cursor1)

    val d4 = GroupingFSM.decide(Some(s3), nowMs = 1300L, observed = cursor1, thisNote = ma)
    d4 shouldBe GroupingFSM.Decision.Extend(List(sa, re, ga, ma))
    val s4 = GroupingFSM.extendedState(s3, List(sa, re, ga, ma), nowMs = 1300L, newNextCursor = cursor1)

    // 5th keystroke at the cap → StartNew
    GroupingFSM.decide(
      Some(s4),
      nowMs = 1400L,
      observed = cursor1,
      thisNote = pa
    ) shouldBe GroupingFSM.Decision.StartNew
  }

  it should "stay extendable when slow keystrokes drift across the original first-keystroke time (sliding window)" in {
    // Sliding semantics: the window resets on every keystroke. Steady 400ms drift
    // forms one group even though total elapsed > 500ms.
    val s1 = GroupingFSM.startedState(cursor0, sa, nowMs = 0L, postInsertCursor = cursor1)
    val d2 = GroupingFSM.decide(Some(s1), nowMs = 400L, observed = cursor1, thisNote = re)
    d2 shouldBe GroupingFSM.Decision.Extend(List(sa, re))
    val s2 = GroupingFSM.extendedState(s1, List(sa, re), nowMs = 400L, newNextCursor = cursor1)
    val d3 = GroupingFSM.decide(Some(s2), nowMs = 800L, observed = cursor1, thisNote = ga)
    d3 shouldBe GroupingFSM.Decision.Extend(List(sa, re, ga))
  }

  // --- cursorMatches ---

  "GroupingFSM.cursorMatches" should "be true when all three components match" in {
    GroupingFSM.cursorMatches(cursor1, cursor1) shouldBe true
  }

  it should "be false when beat differs" in {
    GroupingFSM.cursorMatches(cursor0, cursor1) shouldBe false
  }

  it should "be false when cycle differs" in {
    val a = GroupingFSM.CursorTriple(beat = 0, cycle = 0, subIndex = 0)
    val b = GroupingFSM.CursorTriple(beat = 0, cycle = 1, subIndex = 0)
    GroupingFSM.cursorMatches(a, b) shouldBe false
  }

  it should "be false when subIndex differs" in {
    val a = GroupingFSM.CursorTriple(beat = 0, cycle = 0, subIndex = 0)
    val b = GroupingFSM.CursorTriple(beat = 0, cycle = 0, subIndex = 1)
    GroupingFSM.cursorMatches(a, b) shouldBe false
  }

  // --- startedState / extendedState ---

  "GroupingFSM.startedState" should "carry the pre-insert beat/cycle and post-insert nextCursor" in {
    val s = GroupingFSM.startedState(
      preInsertCursor = cursor0,
      thisNote = sa,
      nowMs = 1000L,
      postInsertCursor = cursor1
    )
    s.notes shouldBe List(sa)
    s.beat shouldBe 0
    s.cycle shouldBe 0
    s.lastTypedTimeMs shouldBe 1000L
    s.nextCursor shouldBe cursor1
  }

  "GroupingFSM.extendedState" should "advance lastTypedTimeMs and update nextCursor while preserving beat/cycle" in {
    val s0 = GroupingFSM.startedState(cursor0, sa, nowMs = 1000L, postInsertCursor = cursor1)
    val s1 = GroupingFSM.extendedState(s0, List(sa, re), nowMs = 1200L, newNextCursor = cursor1)
    s1.notes shouldBe List(sa, re)
    s1.beat shouldBe 0  // preserved from start
    s1.cycle shouldBe 0 // preserved from start
    s1.lastTypedTimeMs shouldBe 1200L
    s1.nextCursor shouldBe cursor1
  }

  // --- CursorTriple.of ---

  "GroupingFSM.CursorTriple.of" should "project beat/cycle/subIndex out of a CursorModel" in {
    val teentaal = Taal(
      "Teentaal",
      16,
      List(Vibhag(4, VibhagMarker.Sam), Vibhag(4, VibhagMarker.Taali(2))),
      None
    )
    val cm = CursorModel(taal = teentaal, cycle = 3, beat = 7, subIndex = 2)
    val t  = GroupingFSM.CursorTriple.of(cm)
    t.cycle shouldBe 3
    t.beat shouldBe 7
    t.subIndex shouldBe 2
  }

  // --- Constants ---

  "GroupingFSM constants" should "match the documented values" in {
    GroupingFSM.MaxGroupSize shouldBe 4
    GroupingFSM.ThresholdMs shouldBe 500L
  }
