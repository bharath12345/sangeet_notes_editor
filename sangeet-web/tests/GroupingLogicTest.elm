module GroupingLogicTest exposing (..)

import Expect
import Model.Types exposing (Note(..), Octave(..), Variant(..))
import State.Model as Model exposing (GroupingState)
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
                                    }
                        }

                    ( newModel, _ ) =
                        update (KeyPressed "-" False False False) model
                in
                Expect.equal Nothing newModel.groupingState
        ]
