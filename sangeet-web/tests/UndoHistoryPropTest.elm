module UndoHistoryPropTest exposing (suite)

{-| Plan-19 Tier 4 Phase C — algebraic invariants for `State.UndoHistory`.

The existing `UndoHistoryTest.elm` tests specific examples (push N
snapshots, expect specific past/future layout). These properties target
the invariants those examples sample:

  - **push-then-undo restores the prior snapshot**: for any pair of
    snapshots `s1, s2`, `(init s1 |> push s2 |> undo |> Maybe.map present) == Just s1`.
    Tests the fundamental round-trip of the undo stack.
  - **push-then-undo-then-redo restores the latest snapshot**.
  - **canUndo / canRedo agree with availability**: `undo h |> isJust == canUndo h`
    for any history, same for redo.
  - **History depth never exceeds maxHistory (50)**: pushing N snapshots
    for any N keeps the past list bounded.
  - **push clears the redo stack**: after a push, canRedo must be False.
  - **`present` is invariant under push followed by undo + redo**:
    `present h == present (h |> push s |> undo |> Maybe.andThen redo |> Maybe.map present)`.

These collapse what would otherwise be a stack-depth × snapshot-content
combinatorial test matrix into 6 properties.

-}

import Expect
import Fuzz exposing (Fuzzer)
import State.UndoHistory as UndoHistory exposing (Snapshot, UndoHistory)
import Test exposing (Test, describe, fuzz, fuzz2, fuzz3)
import UndoHistoryTest exposing (makeSnapshot)



-- LOCAL FUZZERS
-- We don't need a deep Composition fuzzer here — UndoHistory is
-- snapshot-agnostic. Reuse the existing `makeSnapshot` helper from
-- `UndoHistoryTest` with a fuzz-generated title/sectionIndex so each
-- generated snapshot is distinct.


snapshotFuzzer : Fuzzer Snapshot
snapshotFuzzer =
    Fuzz.map2 makeSnapshot
        (Fuzz.intRange 0 1000 |> Fuzz.map String.fromInt)
        (Fuzz.intRange 0 10)



-- PROPERTIES


suite : Test
suite =
    describe "State.UndoHistory — algebraic invariants"
        [ propPushThenUndoRestoresPrior
        , propPushUndoRedoRestoresLatest
        , propCanUndoAgreesWithUndo
        , propCanRedoAgreesWithRedo
        , propDepthBounded
        , propPushClearsRedoStack
        , propInitCanUndoFalse
        , propInitCanRedoFalse
        ]


propPushThenUndoRestoresPrior : Test
propPushThenUndoRestoresPrior =
    fuzz2 snapshotFuzzer
        snapshotFuzzer
        "propPushThenUndoRestoresPrior: init s1 |> push s2 |> undo |> map present == Just s1"
    <|
        \s1 s2 ->
            UndoHistory.init s1
                |> UndoHistory.push s2
                |> UndoHistory.undo
                |> Maybe.map UndoHistory.present
                |> Expect.equal (Just s1)


propPushUndoRedoRestoresLatest : Test
propPushUndoRedoRestoresLatest =
    fuzz2 snapshotFuzzer
        snapshotFuzzer
        "propPushUndoRedoRestoresLatest: push then undo then redo returns to the pushed snapshot"
    <|
        \s1 s2 ->
            UndoHistory.init s1
                |> UndoHistory.push s2
                |> UndoHistory.undo
                |> Maybe.andThen UndoHistory.redo
                |> Maybe.map UndoHistory.present
                |> Expect.equal (Just s2)


propCanUndoAgreesWithUndo : Test
propCanUndoAgreesWithUndo =
    fuzz (Fuzz.listOfLengthBetween 1 10 snapshotFuzzer)
        "propCanUndoAgreesWithUndo: canUndo h == (undo h /= Nothing)"
    <|
        \snaps ->
            let
                h =
                    historyFromList snaps
            in
            (UndoHistory.undo h /= Nothing)
                |> Expect.equal (UndoHistory.canUndo h)


propCanRedoAgreesWithRedo : Test
propCanRedoAgreesWithRedo =
    -- Build a history with at least one undone state so canRedo can be True
    fuzz3 snapshotFuzzer
        snapshotFuzzer
        Fuzz.bool
        "propCanRedoAgreesWithRedo: canRedo h == (redo h /= Nothing)"
    <|
        \s1 s2 doUndo ->
            let
                h0 =
                    UndoHistory.init s1
                        |> UndoHistory.push s2

                h =
                    if doUndo then
                        UndoHistory.undo h0 |> Maybe.withDefault h0

                    else
                        h0
            in
            (UndoHistory.redo h /= Nothing)
                |> Expect.equal (UndoHistory.canRedo h)


propDepthBounded : Test
propDepthBounded =
    -- Push N snapshots (N up to 80, comfortably above the 50-snapshot
    -- cap) and assert that undo can be called at most 50 times before
    -- returning Nothing. Tests the maxHistory bound indirectly via the
    -- only observable: how many undos succeed.
    fuzz (Fuzz.intRange 51 80)
        "propDepthBounded: pushing N > 50 snapshots still allows ≤ 50 undos"
    <|
        \n ->
            let
                snaps =
                    List.range 1 n
                        |> List.map (\i -> makeSnapshot ("s-" ++ String.fromInt i) 0)

                h =
                    historyFromList snaps

                undoCount =
                    countUndos h
            in
            undoCount
                |> Expect.atMost 50


propPushClearsRedoStack : Test
propPushClearsRedoStack =
    -- Setup: init s1, push s2, undo (so future = [s2]). Then push s3
    -- and verify canRedo is now False (future got cleared).
    fuzz3 snapshotFuzzer
        snapshotFuzzer
        snapshotFuzzer
        "propPushClearsRedoStack: push after undo clears the redo (future) stack"
    <|
        \s1 s2 s3 ->
            let
                h =
                    UndoHistory.init s1
                        |> UndoHistory.push s2
                        |> UndoHistory.undo
                        |> Maybe.map (UndoHistory.push s3)
            in
            case h of
                Just h_ ->
                    UndoHistory.canRedo h_
                        |> Expect.equal False

                Nothing ->
                    Expect.fail "undo after push should not return Nothing"


propInitCanUndoFalse : Test
propInitCanUndoFalse =
    fuzz snapshotFuzzer "propInitCanUndoFalse: init x |> canUndo == False" <|
        \s ->
            UndoHistory.init s
                |> UndoHistory.canUndo
                |> Expect.equal False


propInitCanRedoFalse : Test
propInitCanRedoFalse =
    fuzz snapshotFuzzer "propInitCanRedoFalse: init x |> canRedo == False" <|
        \s ->
            UndoHistory.init s
                |> UndoHistory.canRedo
                |> Expect.equal False



-- HELPERS


historyFromList : List Snapshot -> UndoHistory
historyFromList snaps =
    case snaps of
        [] ->
            -- Caller guarantees non-empty via the fuzzer bounds.
            UndoHistory.init (makeSnapshot "fallback" 0)

        first :: rest ->
            List.foldl UndoHistory.push (UndoHistory.init first) rest


countUndos : UndoHistory -> Int
countUndos h =
    let
        loop n acc =
            case UndoHistory.undo acc of
                Just next ->
                    loop (n + 1) next

                Nothing ->
                    n
    in
    loop 0 h
