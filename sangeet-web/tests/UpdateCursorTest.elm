module UpdateCursorTest exposing (arrowKeyTests, canvasClickTests, octaveKeyTests, subdivisionKeyTests, suite, tabTests)

import Expect
import State.Msg exposing (Msg(..))
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
