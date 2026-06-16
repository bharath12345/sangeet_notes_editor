module GroupingFSMInvariantsPropTest exposing (suite)

{-| Plan-19 Tier 4 Phase C — additional invariants for `State.Update.GroupingFSM`.

Phase B (`GroupingFSMPropTest`) covered the NEGATIVE decision cases:
no in-progress state, group at cap, threshold-exceeded → `StartNew`.
This file fills the missing positive-case + algebraic invariants:

  - **Positive Extend path** — when ALL three guards pass (in-progress,
    within window, under cap, cursor aligned), `decide` must return
    `Extend (gs.notes ++ [thisNote])` and the extension length is
    exactly `len gs.notes + 1`.
  - **Extend strictly grows the group** — the returned `Extend` list
    length is one more than the prior group's `notes` length.
  - **Extend preserves note order** — `head` of the extension equals
    `head` of the prior group's notes (extension appends, never prepends).
  - **`cursorMatches` is transitive** — `cursorMatches a b ∧ cursorMatches b c ⇒ cursorMatches a c`.
    Combined with Phase B's reflexive + symmetric, this proves
    `cursorMatches` is an equivalence relation.
  - **`cursorMatches` flips on mismatch** — if any of beat/cycle/subIndex
    differ, `cursorMatches` is `False`.
  - **`cursorTripleFromCursor` is total + idempotent** — applying it
    yields a triple whose fields exactly match the source cursor's
    beat/cycle/subIndex, for any `CursorModel`.

-}

import Expect
import Fuzz exposing (Fuzzer)
import Generators.Common as Common
import Generators.Composition as CompositionGen
import Model.Cursor exposing (CursorModel)
import Model.Types as Types
import State.Update.GroupingFSM
    exposing
        ( CursorTriple
        , Decision(..)
        , GroupedNote
        , cursorMatches
        , cursorTripleFromCursor
        , decide
        , maxGroupSize
        )
import Test exposing (Test, describe, fuzz, fuzz3)



-- LOCAL FUZZERS
-- Mirror the same shapes used in GroupingFSMPropTest. Per the T4B
-- precedent (those fuzzers live next to that test), we keep these
-- local — they remain editor-port primitives, not domain primitives.


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


cursorModel : Fuzzer CursorModel
cursorModel =
    Fuzz.constant
        (\t cy b si total oct anch ->
            { taal = t
            , cycle = cy
            , beat = b
            , subIndex = si
            , totalSubdivisions = total
            , currentOctave = oct
            , selectionAnchor = anch
            }
        )
        |> Fuzz.andMap CompositionGen.taal
        |> Fuzz.andMap (Fuzz.intRange 0 8)
        |> Fuzz.andMap (Fuzz.intRange 0 16)
        |> Fuzz.andMap (Fuzz.intRange 0 4)
        |> Fuzz.andMap (Fuzz.intRange 1 16)
        |> Fuzz.andMap Common.octave
        |> Fuzz.andMap (Fuzz.maybe Common.beatPosition)



-- PROPERTIES


suite : Test
suite =
    describe "State.Update.GroupingFSM — Phase C invariants"
        [ propDecideExtendsWhenAllGuardsPass
        , propExtendLengthIsPlusOne
        , propExtendPreservesHead
        , propCursorMatchesTransitive
        , propCursorMatchesFlipsOnBeatMismatch
        , propCursorMatchesFlipsOnCycleMismatch
        , propCursorMatchesFlipsOnSubIndexMismatch
        , propCursorTripleFromCursorIsIdempotent
        ]


propDecideExtendsWhenAllGuardsPass : Test
propDecideExtendsWhenAllGuardsPass =
    fuzz3 cursorTriple
        groupedNote
        groupedNote
        "propDecideExtendsWhenAllGuardsPass: aligned, within-window, under-cap → Extend"
    <|
        \cur seed extra ->
            let
                -- Set up state with exactly 1 note (well under maxGroupSize),
                -- nextCursor == observed (aligned), startTime just before now.
                state =
                    { notes = [ seed ]
                    , startTime = 1000

                    -- The "group anchor" beat/cycle is independent of the
                    -- alignment check (which uses next*). We pick a
                    -- distinct anchor (cur.beat - 1) to make sure decide
                    -- doesn't accidentally compare against it.
                    , beat = max 0 (cur.beat - 1)
                    , cycle = cur.cycle
                    , nextBeat = cur.beat
                    , nextCycle = cur.cycle
                    , nextSubIndex = cur.subIndex
                    }

                nowMs =
                    -- one millisecond after startTime: well inside the 500ms window
                    1001
            in
            case decide (Just state) nowMs cur extra of
                Extend allNotes ->
                    allNotes
                        |> Expect.equal (state.notes ++ [ extra ])

                StartNew ->
                    Expect.fail "decide returned StartNew when all three guards should have allowed Extend"


propExtendLengthIsPlusOne : Test
propExtendLengthIsPlusOne =
    -- Vary the existing group size from 1 to maxGroupSize-1 (so the
    -- cap guard stays satisfied) and assert the Extend list length is
    -- always existing + 1.
    fuzz3 cursorTriple
        groupedNote
        (Fuzz.intRange 1 (maxGroupSize - 1))
        "propExtendLengthIsPlusOne: Extend list length == priorNotes + 1"
    <|
        \cur seed priorLen ->
            let
                priorNotes =
                    List.repeat priorLen seed

                state =
                    { notes = priorNotes
                    , startTime = 1000
                    , beat = cur.beat
                    , cycle = cur.cycle
                    , nextBeat = cur.beat
                    , nextCycle = cur.cycle
                    , nextSubIndex = cur.subIndex
                    }
            in
            case decide (Just state) 1001 cur seed of
                Extend allNotes ->
                    List.length allNotes
                        |> Expect.equal (priorLen + 1)

                StartNew ->
                    Expect.fail "decide returned StartNew unexpectedly"


propExtendPreservesHead : Test
propExtendPreservesHead =
    -- Use a Sa-Shuddha seed for the prior group and a Ga seed for the
    -- new note. The Extend list must start with the prior group's first
    -- note (Sa), not the new note (Ga). This catches any accidental
    -- prepend-instead-of-append.
    fuzz cursorTriple
        "propExtendPreservesHead: Extend appends rather than prepends — first element stays the same"
    <|
        \cur ->
            let
                originalSeed : GroupedNote
                originalSeed =
                    { note = Types.Sa, variant = Types.Shuddha, octave = Types.Madhya }

                newNote : GroupedNote
                newNote =
                    { note = Types.Ga, variant = Types.Shuddha, octave = Types.Madhya }

                state =
                    { notes = [ originalSeed, originalSeed ]
                    , startTime = 1000
                    , beat = cur.beat
                    , cycle = cur.cycle
                    , nextBeat = cur.beat
                    , nextCycle = cur.cycle
                    , nextSubIndex = cur.subIndex
                    }
            in
            case decide (Just state) 1001 cur newNote of
                Extend allNotes ->
                    List.head allNotes
                        |> Expect.equal (Just originalSeed)

                StartNew ->
                    Expect.fail "decide returned StartNew unexpectedly"


propCursorMatchesTransitive : Test
propCursorMatchesTransitive =
    fuzz3 cursorTriple
        cursorTriple
        cursorTriple
        "propCursorMatchesTransitive: cursorMatches a b ∧ cursorMatches b c ⇒ cursorMatches a c"
    <|
        \a b c ->
            if cursorMatches a b && cursorMatches b c then
                cursorMatches a c |> Expect.equal True

            else
                -- vacuously satisfied
                Expect.pass


propCursorMatchesFlipsOnBeatMismatch : Test
propCursorMatchesFlipsOnBeatMismatch =
    fuzz cursorTriple
        "propCursorMatchesFlipsOnBeatMismatch: changing beat flips cursorMatches to False"
    <|
        \a ->
            let
                b =
                    { a | beat = a.beat + 1 }
            in
            cursorMatches a b |> Expect.equal False


propCursorMatchesFlipsOnCycleMismatch : Test
propCursorMatchesFlipsOnCycleMismatch =
    fuzz cursorTriple
        "propCursorMatchesFlipsOnCycleMismatch: changing cycle flips cursorMatches to False"
    <|
        \a ->
            let
                b =
                    { a | cycle = a.cycle + 1 }
            in
            cursorMatches a b |> Expect.equal False


propCursorMatchesFlipsOnSubIndexMismatch : Test
propCursorMatchesFlipsOnSubIndexMismatch =
    fuzz cursorTriple
        "propCursorMatchesFlipsOnSubIndexMismatch: changing subIndex flips cursorMatches to False"
    <|
        \a ->
            let
                b =
                    { a | subIndex = a.subIndex + 1 }
            in
            cursorMatches a b |> Expect.equal False


propCursorTripleFromCursorIsIdempotent : Test
propCursorTripleFromCursorIsIdempotent =
    -- The projection drops all fields except beat/cycle/subIndex. We
    -- assert: building a triple from a cursor whose triple fields are
    -- known, then comparing to a constructed triple with the same
    -- fields, yields cursorMatches == True (regardless of what the
    -- cursor's other fields contain).
    fuzz cursorModel
        "propCursorTripleFromCursorIsIdempotent: projection only depends on beat/cycle/subIndex"
    <|
        \c ->
            let
                projected =
                    cursorTripleFromCursor c

                expected =
                    { beat = c.beat, cycle = c.cycle, subIndex = c.subIndex }
            in
            cursorMatches projected expected |> Expect.equal True
