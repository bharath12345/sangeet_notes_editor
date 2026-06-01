module UpdateEditorTest exposing (..)

import Expect
import State.Model as Model exposing (EditMode(..), Model)
import State.Msg exposing (Msg(..))
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultModel, strokeEditModel)


suite : Test
suite =
    describe "Update editor actions"
        [ swarInsertTests
        , restSustainTests
        , deleteTests
        , undoRedoTests
        , strokeModeTests
        ]


swarInsertTests : Test
swarInsertTests =
    describe "Swar key input dispatches"
        [ test "pressing 's' in SwarEdit mode does not set pendingApiCall immediately (defers via Time.now)" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "s" False False False) defaultModel
                in
                Expect.equal False newModel.pendingApiCall
        , test "pressing 'r' in SwarEdit mode does not set pendingApiCall immediately" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "r" False False False) defaultModel
                in
                Expect.equal False newModel.pendingApiCall
        ]


restSustainTests : Test
restSustainTests =
    describe "Rest and sustain insertion"
        [ test "pressing '-' sets pendingApiCall to True" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "-" False False False) defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "pressing '=' sets pendingApiCall to True" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "=" False False False) defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "pressing '-' clears groupingState" <|
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
                        update (KeyPressed "-" False False False) model
                in
                Expect.equal Nothing newModel.groupingState
        ]


deleteTests : Test
deleteTests =
    describe "Delete at cursor"
        [ test "Backspace sets pendingApiCall to True" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "Backspace" False False False) defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "Delete key sets pendingApiCall to True" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "Delete" False False False) defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "Backspace clears groupingState" <|
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
                        update (KeyPressed "Backspace" False False False) model
                in
                Expect.equal Nothing newModel.groupingState
        ]


undoRedoTests : Test
undoRedoTests =
    describe "Undo/Redo via keyboard"
        [ test "Ctrl+z on fresh model adds log 'Nothing to undo'" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "z" False True False) defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "Nothing to undo" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        , test "Ctrl+y on fresh model adds log 'Nothing to redo'" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "y" False True False) defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "Nothing to redo" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        , test "Undo button msg on fresh model adds log" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update Undo defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "Nothing to undo" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        , test "Redo button msg on fresh model adds log" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update Redo defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "Nothing to redo" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        ]


strokeModeTests : Test
strokeModeTests =
    describe "Stroke mode key remapping"
        [ test "pressing 'd' in StrokeEdit sets pendingApiCall (Da stroke)" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "d" False False False) strokeEditModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "pressing 'r' in StrokeEdit sets pendingApiCall (Ra stroke)" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "r" False False False) strokeEditModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "pressing 'c' in StrokeEdit sets pendingApiCall (Chikari)" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "c" False False False) strokeEditModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "pressing 'j' in StrokeEdit sets pendingApiCall (Jod)" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "j" False False False) strokeEditModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "pressing 'x' in StrokeEdit sets pendingApiCall (clear stroke)" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "x" False False False) strokeEditModel
                in
                Expect.equal True newModel.pendingApiCall
        ]
