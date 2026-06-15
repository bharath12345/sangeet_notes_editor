module GroupingLogicTest exposing (groupAccumulationTests, groupClearTests, groupCursorMoveTests, groupMaxNotesTests, groupStartTests, groupThresholdTests, modelWithUndoableHistory, suite)

import Expect
import Model.Types exposing (Note(..), Octave(..), Variant(..))
import State.Model as Model
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultModel, defaultSnapshot)
import Time


suite : Test
suite =
    describe "Grouping logic (fast-typing detection)"
        [ groupStartTests
        , groupAccumulationTests
        , groupThresholdTests
        , groupMaxNotesTests
        , groupClearTests
        , groupCursorMoveTests
        ]


groupStartTests : Test
groupStartTests =
    describe "Starting a new group"
        [ test "GotSwarKeyTime with no existing group creates new groupingState" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update
                            (GotSwarKeyTime (Time.millisToPosix 1000) Sa Shuddha "s")
                            defaultModel
                in
                case newModel.groupingState of
                    Just gs ->
                        Expect.all
                            [ \g -> Expect.equal 1 (List.length g.notes)
                            , \g -> Expect.equal 1000 g.startTime
                            ]
                            gs

                    Nothing ->
                        Expect.fail "Expected groupingState to be set"
        , test "GotSwarKeyTime sets pendingApiCall" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update
                            (GotSwarKeyTime (Time.millisToPosix 1000) Sa Shuddha "s")
                            defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        ]


modelWithUndoableHistory : Model.Model
modelWithUndoableHistory =
    let
        history =
            UndoHistory.push defaultSnapshot defaultModel.history
    in
    { defaultModel | history = history }


groupAccumulationTests : Test
groupAccumulationTests =
    describe "Accumulating notes in group"
        [ test "second note within threshold accumulates in group" <|
            \_ ->
                let
                    model =
                        { modelWithUndoableHistory
                            | groupingState =
                                Just
                                    { notes = [ { note = Sa, variant = Shuddha, octave = Madhya } ]
                                    , startTime = 1000
                                    , beat = 0
                                    , cycle = 0
                                    , nextBeat = 0
                                    , nextCycle = 0
                                    , nextSubIndex = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update
                            (GotSwarKeyTime (Time.millisToPosix 1200) Re Shuddha "r")
                            model
                in
                case newModel.groupingState of
                    Just gs ->
                        Expect.equal 2 (List.length gs.notes)

                    Nothing ->
                        Expect.fail "Expected groupingState with 2 notes"
        , test "third note within threshold accumulates" <|
            \_ ->
                let
                    model =
                        { modelWithUndoableHistory
                            | groupingState =
                                Just
                                    { notes =
                                        [ { note = Sa, variant = Shuddha, octave = Madhya }
                                        , { note = Re, variant = Shuddha, octave = Madhya }
                                        ]
                                    , startTime = 1000
                                    , beat = 0
                                    , cycle = 0
                                    , nextBeat = 0
                                    , nextCycle = 0
                                    , nextSubIndex = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update
                            (GotSwarKeyTime (Time.millisToPosix 1300) Ga Shuddha "g")
                            model
                in
                case newModel.groupingState of
                    Just gs ->
                        Expect.equal 3 (List.length gs.notes)

                    Nothing ->
                        Expect.fail "Expected groupingState with 3 notes"
        ]


groupThresholdTests : Test
groupThresholdTests =
    describe "Threshold boundary (500ms)"
        [ test "note at exactly 500ms still groups" <|
            \_ ->
                let
                    model =
                        { modelWithUndoableHistory
                            | groupingState =
                                Just
                                    { notes = [ { note = Sa, variant = Shuddha, octave = Madhya } ]
                                    , startTime = 1000
                                    , beat = 0
                                    , cycle = 0
                                    , nextBeat = 0
                                    , nextCycle = 0
                                    , nextSubIndex = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update
                            (GotSwarKeyTime (Time.millisToPosix 1499) Re Shuddha "r")
                            model
                in
                case newModel.groupingState of
                    Just gs ->
                        Expect.equal 2 (List.length gs.notes)

                    Nothing ->
                        Expect.fail "Expected groupingState with 2 notes"
        , test "note beyond 500ms starts new group" <|
            \_ ->
                let
                    model =
                        { defaultModel
                            | groupingState =
                                Just
                                    { notes = [ { note = Sa, variant = Shuddha, octave = Madhya } ]
                                    , startTime = 1000
                                    , beat = 0
                                    , cycle = 0
                                    , nextBeat = 0
                                    , nextCycle = 0
                                    , nextSubIndex = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update
                            (GotSwarKeyTime (Time.millisToPosix 1500) Re Shuddha "r")
                            model
                in
                case newModel.groupingState of
                    Just gs ->
                        Expect.all
                            [ \g -> Expect.equal 1 (List.length g.notes)
                            , \g -> Expect.equal 1500 g.startTime
                            ]
                            gs

                    Nothing ->
                        Expect.fail "Expected new groupingState"
        ]


groupMaxNotesTests : Test
groupMaxNotesTests =
    describe "Max 4 notes per group"
        [ test "5th note starts a new group" <|
            \_ ->
                let
                    model =
                        { defaultModel
                            | groupingState =
                                Just
                                    { notes =
                                        [ { note = Sa, variant = Shuddha, octave = Madhya }
                                        , { note = Re, variant = Shuddha, octave = Madhya }
                                        , { note = Ga, variant = Shuddha, octave = Madhya }
                                        , { note = Ma, variant = Shuddha, octave = Madhya }
                                        ]
                                    , startTime = 1000
                                    , beat = 0
                                    , cycle = 0
                                    , nextBeat = 0
                                    , nextCycle = 0
                                    , nextSubIndex = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update
                            (GotSwarKeyTime (Time.millisToPosix 1200) Pa Shuddha "p")
                            model
                in
                case newModel.groupingState of
                    Just gs ->
                        Expect.equal 1 (List.length gs.notes)

                    Nothing ->
                        Expect.fail "Expected new groupingState with 1 note"
        ]


groupClearTests : Test
groupClearTests =
    describe "Clearing grouping state"
        [ test "non-swar action clears groupingState" <|
            \_ ->
                let
                    model =
                        { defaultModel
                            | groupingState =
                                Just
                                    { notes = [ { note = Sa, variant = Shuddha, octave = Madhya } ]
                                    , startTime = 1000
                                    , beat = 0
                                    , cycle = 0
                                    , nextBeat = 0
                                    , nextCycle = 0
                                    , nextSubIndex = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update (KeyPressed "ArrowRight" False False False) model
                in
                Expect.equal Nothing newModel.groupingState
        , test "rest insertion clears groupingState" <|
            \_ ->
                let
                    model =
                        { defaultModel
                            | groupingState =
                                Just
                                    { notes = [ { note = Sa, variant = Shuddha, octave = Madhya } ]
                                    , startTime = 1000
                                    , beat = 0
                                    , cycle = 0
                                    , nextBeat = 0
                                    , nextCycle = 0
                                    , nextSubIndex = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update (KeyPressed "-" False False False) model
                in
                Expect.equal Nothing newModel.groupingState
        ]


{-| Plan-17 PR-1 bug 4 regression: if the user types a swar, navigates the
cursor away from where the editor advanced it, then types another swar
within the grouping window, the second swar must NOT collapse onto the
first's beat. The cursor-alignment guard in `handleSwarKeyTimed` is what
prevents this.
-}
groupCursorMoveTests : Test
groupCursorMoveTests =
    describe "Cursor movement invalidates grouping (bug 4)"
        [ test "second note within threshold but cursor moved starts a fresh group" <|
            \_ ->
                let
                    -- The current cursor in modelWithUndoableHistory is at
                    -- beat=0, cycle=0. We seed a group whose `nextBeat` is
                    -- DIFFERENT (beat=5) — simulating "user moved cursor
                    -- after the first insert advanced it to beat=5". When
                    -- the next keystroke arrives, the observed cursor
                    -- (beat=0) doesn't match the expected next-cursor
                    -- (beat=5), so the alignment check fails and a NEW
                    -- group is started instead of extending the old one.
                    model =
                        { modelWithUndoableHistory
                            | groupingState =
                                Just
                                    { notes = [ { note = Sa, variant = Shuddha, octave = Madhya } ]
                                    , startTime = 1000
                                    , beat = 0
                                    , cycle = 0
                                    , nextBeat = 5
                                    , nextCycle = 0
                                    , nextSubIndex = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update
                            (GotSwarKeyTime (Time.millisToPosix 1100) Re Shuddha "r")
                            model
                in
                case newModel.groupingState of
                    Just gs ->
                        Expect.all
                            [ \g -> Expect.equal 1 (List.length g.notes)
                            , \g -> Expect.equal 1100 g.startTime
                            ]
                            gs

                    Nothing ->
                        Expect.fail "Expected a fresh groupingState with one note"
        , test "second note within threshold and matching cursor still extends the group" <|
            \_ ->
                -- Mirror of the above: when nextBeat matches the observed
                -- cursor, the alignment check passes and grouping extends.
                let
                    model =
                        { modelWithUndoableHistory
                            | groupingState =
                                Just
                                    { notes = [ { note = Sa, variant = Shuddha, octave = Madhya } ]
                                    , startTime = 1000
                                    , beat = 0
                                    , cycle = 0
                                    , nextBeat = 0
                                    , nextCycle = 0
                                    , nextSubIndex = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update
                            (GotSwarKeyTime (Time.millisToPosix 1100) Re Shuddha "r")
                            model
                in
                case newModel.groupingState of
                    Just gs ->
                        Expect.equal 2 (List.length gs.notes)

                    Nothing ->
                        Expect.fail "Expected groupingState with 2 notes"
        ]
