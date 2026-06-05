module UpdateFileTest exposing (exportTests, filePortTests, saveFileTests, suite)

import Expect
import State.Msg exposing (Msg(..))
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultModel)


suite : Test
suite =
    describe "Update file operations"
        [ saveFileTests
        , exportTests
        , filePortTests
        ]


saveFileTests : Test
saveFileTests =
    describe "SaveFile"
        [ test "SaveFile sets pendingApiCall" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update SaveFile defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        ]


exportTests : Test
exportTests =
    describe "Export operations"
        [ test "ExportHtml sets pendingApiCall" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update ExportHtml defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "ExportPdf adds log entry" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update ExportPdf defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "PDF" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        ]


filePortTests : Test
filePortTests =
    describe "File port responses"
        [ test "FileSelected adds log with filename" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (FileSelected "test.swar") defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "test.swar" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        , test "FileLoaded sets pendingApiCall" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (FileLoaded "{}") defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        ]
