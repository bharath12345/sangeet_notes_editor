module UpdateCursorTest exposing (arrowKeyTests, canvasClickTests, octaveKeyTests, subdivisionKeyTests, suite, tabTests)

import Expect
import State.Model as Model
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultModel)


suite : Test
suite =
    describe "Update cursor navigation"
        [ arrowKeyTests
        , tabTests
        , subdivisionKeyTests
        , octaveKeyTests
        , canvasClickTests
        ]


arrowKeyTests : Test
arrowKeyTests =
    describe "Arrow key navigation"
        [ test "ArrowRight clears groupingState" <|
            \_ ->
                let
                    model =
                        { defaultModel
                            | groupingState =
                                Just
                                    { notes = []
                                    , startTime = 100
                                    , beat = 0
                                    , cycle = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update (KeyPressed "ArrowRight" False False False) model
                in
                Expect.equal Nothing newModel.groupingState
        , test "ArrowLeft clears groupingState" <|
            \_ ->
                let
                    model =
                        { defaultModel
                            | groupingState =
                                Just
                                    { notes = []
                                    , startTime = 100
                                    , beat = 0
                                    , cycle = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update (KeyPressed "ArrowLeft" False False False) model
                in
                Expect.equal Nothing newModel.groupingState
        , test "ArrowRight at end of empty-section cycle 1 last beat is a no-op (PR-B B.5a)" <|
            -- Plan-16 issue #9 sub-bug (a): pressing right past the last
            -- valid position used to advance the cursor into an unrendered
            -- cycle, making it visually disappear. Desktop clamps with
            -- "if next.cycle > maxCycle + 1 then NoOp"; web should too.
            -- defaultModel has an empty section → maxCycle = 0 →
            -- maxAllowedCycle = 1, so positioning at (cycle=1, beat=15)
            -- and pressing right should NOT fire the API call.
            \_ ->
                let
                    cur =
                        Model.cursor defaultModel

                    atLastBeat =
                        { cur | cycle = 1, beat = cur.taal.matras - 1 }

                    snap =
                        UndoHistory.present defaultModel.history

                    boundaryModel =
                        { defaultModel
                            | history =
                                UndoHistory.push
                                    { snap | cursor = atLastBeat }
                                    defaultModel.history
                        }

                    ( newModel, _ ) =
                        update (KeyPressed "ArrowRight" False False False) boundaryModel
                in
                Expect.equal False newModel.pendingApiCall
        ]


tabTests : Test
tabTests =
    describe "Tab navigation"
        [ test "Tab clears groupingState" <|
            \_ ->
                let
                    model =
                        { defaultModel
                            | groupingState =
                                Just
                                    { notes = []
                                    , startTime = 100
                                    , beat = 0
                                    , cycle = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update (KeyPressed "Tab" False False False) model
                in
                Expect.equal Nothing newModel.groupingState
        ]


subdivisionKeyTests : Test
subdivisionKeyTests =
    describe "Subdivision keys"
        [ test "pressing '1' clears groupingState" <|
            \_ ->
                let
                    model =
                        { defaultModel
                            | groupingState =
                                Just
                                    { notes = []
                                    , startTime = 100
                                    , beat = 0
                                    , cycle = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update (KeyPressed "1" False False False) model
                in
                Expect.equal Nothing newModel.groupingState
        , test "pressing '4' clears groupingState" <|
            \_ ->
                let
                    model =
                        { defaultModel
                            | groupingState =
                                Just
                                    { notes = []
                                    , startTime = 100
                                    , beat = 0
                                    , cycle = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update (KeyPressed "4" False False False) model
                in
                Expect.equal Nothing newModel.groupingState
        ]


octaveKeyTests : Test
octaveKeyTests =
    describe "Octave keys"
        [ test "pressing '[' clears groupingState" <|
            \_ ->
                let
                    model =
                        { defaultModel
                            | groupingState =
                                Just
                                    { notes = []
                                    , startTime = 100
                                    , beat = 0
                                    , cycle = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update (KeyPressed "[" False False False) model
                in
                Expect.equal Nothing newModel.groupingState
        ]


canvasClickTests : Test
canvasClickTests =
    describe "Canvas click"
        [ test "CanvasClicked clears groupingState" <|
            \_ ->
                let
                    model =
                        { defaultModel
                            | groupingState =
                                Just
                                    { notes = []
                                    , startTime = 100
                                    , beat = 0
                                    , cycle = 0
                                    }
                        }

                    ( newModel, _ ) =
                        update (CanvasClicked 0 0) model
                in
                Expect.equal Nothing newModel.groupingState
        ]
