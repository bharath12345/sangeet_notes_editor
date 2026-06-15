module GroupingFSMPropTest exposing (suite)

{-| Plan-19 Tier 4 Phase B — property tests for `State.Update.GroupingFSM`.

The existing `GroupingFSMTest.elm` stays — it's a hand-port of the Scala
spec and acts as the byte-aligned parity contract between the two
implementations. These properties are additive: they cover the
algebraic invariants of the FSM that no specific example can express.

Properties:

  - `propDecideNothingAlwaysStartNew`: with no in-progress state,
    `decide` is total and constantly returns `StartNew` regardless of
    inputs.
  - `propDecideAboveCapStartsNew`: once a group hits `maxGroupSize`,
    `decide` returns `StartNew` for any further keystroke (cap is
    strict — `<`, not `≤`).
  - `propDecideBeyondThresholdStartsNew`: when the time since the last
    keystroke is `≥ thresholdMs`, `decide` returns `StartNew`. Boundary
    is `<`, so a delta of exactly `thresholdMs` resets.
  - `propStartedStateInvariant`: the state emitted by `startedState`
    always has length-1 `notes`, carries the pre-insert beat/cycle, and
    sets `startTime` to the supplied `nowMs`.
  - `propExtendedStatePreservesAnchor`: `extendedState` preserves the
    original `beat`/`cycle` (the anchor of the group) and updates
    `startTime`/`nextBeat`/`nextCycle`/`nextSubIndex` to the new
    arguments — i.e., the group anchor never drifts.

-}

import Expect
import Fuzz exposing (Fuzzer)
import Generators.Common as Common
import Model.Types as Types exposing (Note, Octave, Variant)
import State.Update.GroupingFSM
    exposing
        ( CursorTriple
        , Decision(..)
        , GroupedNote
        , decide
        , extendedState
        , maxGroupSize
        , startedState
        , thresholdMs
        )
import Test exposing (Test, describe, fuzz, fuzz3)



-- LOCAL FUZZERS
-- These are FSM-shape primitives (CursorTriple, GroupedNote, smallTime)
-- that don't appear in `Generators.Composition` because they're internal
-- to the editor port. Per T4B constraint we don't add them to the shared
-- Generators modules — they live here next to the only test that needs
-- them. If a second test ever needs the same shapes, we promote them
-- then.


cursorTriple : Fuzzer CursorTriple
cursorTriple =
    Fuzz.map3 (\b c s -> { beat = b, cycle = c, subIndex = s })
        (Fuzz.intRange 0 16)
        (Fuzz.intRange 0 8)
        (Fuzz.intRange 0 4)


groupedNote : Fuzzer GroupedNote
groupedNote =
    Common.note
        |> Fuzz.andThen
            (\n ->
                Fuzz.map2 (\v o -> { note = n, variant = v, octave = o })
                    (Common.variantFor n)
                    Common.octave
            )


smallTime : Fuzzer Int
smallTime =
    Fuzz.intRange 0 100000



-- PROPERTIES


suite : Test
suite =
    describe "State.Update.GroupingFSM — property invariants"
        [ propDecideNothingAlwaysStartNew
        , propDecideAboveCapStartsNew
        , propDecideBeyondThresholdStartsNew
        , propStartedStateInvariant
        , propExtendedStatePreservesAnchor
        ]


propDecideNothingAlwaysStartNew : Test
propDecideNothingAlwaysStartNew =
    fuzz3 smallTime
        cursorTriple
        groupedNote
        "propDecideNothingAlwaysStartNew: Nothing in-progress → StartNew always"
    <|
        \nowMs observed thisNote ->
            decide Nothing nowMs observed thisNote
                |> Expect.equal StartNew


propDecideAboveCapStartsNew : Test
propDecideAboveCapStartsNew =
    fuzz3 cursorTriple
        groupedNote
        groupedNote
        "propDecideAboveCapStartsNew: a group at maxGroupSize cannot extend"
    <|
        \cur seed extra ->
            let
                -- Pad the notes list to exactly maxGroupSize so the cap
                -- guard fires regardless of the cursor-alignment and
                -- threshold guards. We deliberately use `aligned ==
                -- True` and `withinWindow == True` so the cap guard is
                -- the ONLY reason `decide` can return `StartNew`.
                fullNotes =
                    List.repeat maxGroupSize seed

                state =
                    { notes = fullNotes
                    , startTime = 1000
                    , beat = cur.beat
                    , cycle = cur.cycle
                    , nextBeat = cur.beat
                    , nextCycle = cur.cycle
                    , nextSubIndex = cur.subIndex
                    }
            in
            decide (Just state) 1001 cur extra
                |> Expect.equal StartNew


propDecideBeyondThresholdStartsNew : Test
propDecideBeyondThresholdStartsNew =
    fuzz3 cursorTriple
        groupedNote
        groupedNote
        "propDecideBeyondThresholdStartsNew: delta ≥ thresholdMs → StartNew"
    <|
        \cur seed extra ->
            let
                state =
                    { notes = [ seed ]
                    , startTime = 1000
                    , beat = cur.beat
                    , cycle = cur.cycle
                    , nextBeat = cur.beat
                    , nextCycle = cur.cycle
                    , nextSubIndex = cur.subIndex
                    }

                -- exactly at the boundary — strict `<` in the FSM means
                -- this should NOT extend (boundary is exclusive)
                nowMs =
                    1000 + thresholdMs
            in
            decide (Just state) nowMs cur extra
                |> Expect.equal StartNew


propStartedStateInvariant : Test
propStartedStateInvariant =
    fuzz3 cursorTriple
        groupedNote
        cursorTriple
        "propStartedStateInvariant: startedState always seeds a 1-note group at the pre-insert cursor"
    <|
        \preInsert thisNote postInsert ->
            let
                nowMs =
                    12345

                s =
                    startedState preInsert thisNote nowMs postInsert
            in
            Expect.all
                [ \st -> List.length st.notes |> Expect.equal 1
                , \st -> st.notes |> Expect.equal [ thisNote ]
                , \st -> st.beat |> Expect.equal preInsert.beat
                , \st -> st.cycle |> Expect.equal preInsert.cycle
                , \st -> st.startTime |> Expect.equal nowMs
                , \st -> st.nextBeat |> Expect.equal postInsert.beat
                , \st -> st.nextCycle |> Expect.equal postInsert.cycle
                , \st -> st.nextSubIndex |> Expect.equal postInsert.subIndex
                ]
                s


propExtendedStatePreservesAnchor : Test
propExtendedStatePreservesAnchor =
    fuzz (Fuzz.pair cursorTriple cursorTriple)
        "propExtendedStatePreservesAnchor: extendedState updates startTime + nextCursor, preserves beat/cycle anchor"
    <|
        \( preInsert, newNext ) ->
            let
                seed =
                    { note = pickFirstNote, variant = pickShuddha, octave = pickMadhya }

                s0 =
                    startedState preInsert seed 1000 preInsert

                s1 =
                    extendedState s0 [ seed, seed ] 1200 newNext
            in
            Expect.all
                [ \st -> st.beat |> Expect.equal preInsert.beat
                , \st -> st.cycle |> Expect.equal preInsert.cycle
                , \st -> st.startTime |> Expect.equal 1200
                , \st -> st.nextBeat |> Expect.equal newNext.beat
                , \st -> st.nextCycle |> Expect.equal newNext.cycle
                , \st -> st.nextSubIndex |> Expect.equal newNext.subIndex
                , \st -> List.length st.notes |> Expect.equal 2
                ]
                s1



-- LOCAL CONSTANTS USED BY THE EXTENDED-STATE PROPERTY


pickFirstNote : Note
pickFirstNote =
    Types.Sa


pickShuddha : Variant
pickShuddha =
    Types.Shuddha


pickMadhya : Octave
pickMadhya =
    Types.Madhya
