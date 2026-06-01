module ApiResponseTest exposing (..)

import Api.Client exposing (ApiResult(..))
import Expect
import Http
import Model.Composition exposing (Composition, CompositionType(..), SectionType(..))
import Model.Cursor exposing (CursorModel)
import Model.Layout exposing (EditorResult)
import Model.Raag exposing (Raag)
import Model.Taal exposing (Taal, VibhagMarker(..))
import Model.Types exposing (Octave(..))
import State.Model as Model
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultComposition, defaultCursor, defaultModel)


suite : Test
suite =
    describe "API response handling"
        [ editorResultTests
        , cursorResultTests
        , apiFailureTests
        , httpErrorTests
        , referenceDataTests
        ]


makeEditorResult : EditorResult
makeEditorResult =
    { composition = defaultComposition
    , cursor = { defaultCursor | beat = 1 }
    , message = "Inserted Sa"
    }


editorResultTests : Test
editorResultTests =
    describe "GotEditorResult"
        [ test "Success pushes to undo history and clears pendingApiCall" <|
            \_ ->
                let
                    model =
                        { defaultModel | pendingApiCall = True }

                    ( newModel, _ ) =
                        update (GotEditorResult (Ok (Success makeEditorResult))) model
                in
                Expect.all
                    [ \m -> Expect.equal False m.pendingApiCall
                    , \m -> Expect.equal True (UndoHistory.canUndo m.history)
                    ]
                    newModel
        , test "Success adds message to log" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (GotEditorResult (Ok (Success makeEditorResult))) defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "Inserted Sa" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        ]


cursorResultTests : Test
cursorResultTests =
    describe "GotCursorResult"
        [ test "Success updates cursor in history" <|
            \_ ->
                let
                    newCursor =
                        { defaultCursor | beat = 3 }

                    ( newModel, _ ) =
                        update (GotCursorResult (Ok (Success newCursor))) defaultModel
                in
                Expect.equal 3 (Model.cursor newModel).beat
        ]


apiFailureTests : Test
apiFailureTests =
    describe "ApiFailure handling"
        [ test "ApiFailure clears pendingApiCall" <|
            \_ ->
                let
                    model =
                        { defaultModel | pendingApiCall = True }

                    error =
                        { code = "INVALID_INPUT", message = "Bad beat index" }

                    ( newModel, _ ) =
                        update (GotEditorResult (Ok (ApiFailure error))) model
                in
                Expect.equal False newModel.pendingApiCall
        , test "ApiFailure adds error message to log" <|
            \_ ->
                let
                    error =
                        { code = "INVALID_INPUT", message = "Bad beat index" }

                    ( newModel, _ ) =
                        update (GotEditorResult (Ok (ApiFailure error))) defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True
                            (String.contains "Bad beat index" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        ]


httpErrorTests : Test
httpErrorTests =
    describe "HTTP error handling"
        [ test "Network error clears pendingApiCall" <|
            \_ ->
                let
                    model =
                        { defaultModel | pendingApiCall = True }

                    ( newModel, _ ) =
                        update (GotEditorResult (Err Http.NetworkError)) model
                in
                Expect.equal False newModel.pendingApiCall
        , test "Network error adds log entry" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (GotEditorResult (Err Http.NetworkError)) defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True
                            (String.contains "Network" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        , test "Timeout error adds log entry" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (GotEditorResult (Err Http.Timeout)) defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True
                            (String.contains "timed out" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        , test "BadStatus error adds log entry" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (GotEditorResult (Err (Http.BadStatus 500))) defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True
                            (String.contains "500" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        ]


referenceDataTests : Test
referenceDataTests =
    describe "Reference data loading"
        [ test "GotTaals Success populates availableTaals" <|
            \_ ->
                let
                    taal =
                        { name = "Teentaal"
                        , matras = 16
                        , vibhags =
                            [ { beats = 4, marker = Sam } ]
                        , theka = Nothing
                        }

                    ( newModel, _ ) =
                        update (GotTaals (Ok (Success [ ( "teentaal", taal ) ]))) defaultModel
                in
                Expect.equal 1 (List.length newModel.availableTaals)
        , test "GotRaags Success populates availableRaags" <|
            \_ ->
                let
                    raag =
                        { name = "Yaman"
                        , thaat = Just "Kalyan"
                        , arohana = Nothing
                        , avarohana = Nothing
                        , vadi = Nothing
                        , samvadi = Nothing
                        , pakad = Nothing
                        , prahar = Nothing
                        }

                    ( newModel, _ ) =
                        update (GotRaags (Ok (Success [ ( "yaman", raag ) ]))) defaultModel
                in
                Expect.equal 1 (List.length newModel.availableRaags)
        ]
