module UpdateEditorTest exposing (clipboardTests, deleteTests, restSustainTests, strokeModeTests, suite, swarInsertTests, undoRedoTests)

import Expect
import State.Model as Model
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
        , clipboardTests
        ]


swarInsertTests : Test
swarInsertTests =
    describe "Swar key input dispatches"
        [ test "pressing 's' in SwarEdit mode sets pendingApiCall True (Time.now task in flight)" <|
            -- handleSwarKey now sets pendingApiCall=True optimistically so
            -- the debug bridge's deferred-ack drain waits for the full
            -- swar-insertion round trip (Time.now → /editor/insert-swar →
            -- GotEditorResult) instead of firing the ack between the
            -- KeyPressed dispatch and Task.perform's completion.
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "s" False False False) defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "pressing 'r' in SwarEdit mode sets pendingApiCall True" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "r" False False False) defaultModel
                in
                Expect.equal True newModel.pendingApiCall
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
        , test "pressing '1' inserts chikari" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "1" False False False) defaultModel
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


clipboardTests : Test
clipboardTests =
    describe "Clipboard operations"
        [ test "Ctrl+c without selection logs 'No selection'" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "c" False True False) defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "No selection" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        , test "Ctrl+x without selection logs 'No selection'" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "x" False True False) defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "No selection" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        , test "Shift+ArrowRight sets selectionAnchor" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "ArrowRight" True False False) defaultModel
                in
                Expect.notEqual Nothing (Model.cursor newModel).selectionAnchor
        , test "Shift+ArrowLeft sets selectionAnchor" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "ArrowLeft" True False False) defaultModel
                in
                Expect.notEqual Nothing (Model.cursor newModel).selectionAnchor
        , test "ClipboardContentReceived sends API call (pendingApiCall = True)" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (ClipboardContentReceived "{\"sangeet-clipboard\":true}") defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "ClipboardContentReceived with any text sends API call" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (ClipboardContentReceived "not json") defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "Ctrl+v is handled without error (no-op, waits for paste event)" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "v" False True False) defaultModel
                in
                Expect.equal defaultModel.pendingApiCall newModel.pendingApiCall
        ]
