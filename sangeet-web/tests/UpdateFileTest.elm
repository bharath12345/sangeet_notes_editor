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
        , test "Ctrl+S triggers SaveFile (browser's Save Page As default is prevented in ports.js)" <|
            -- KeyPressed key shift ctrl alt. ctrl=True, shift=False, alt=False.
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "s" False True False) defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "Ctrl+Shift+S triggers SaveFileAs (not SaveFile)" <|
            -- SaveFileAs sets pendingSaveAs=True; SaveFile leaves it False.
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "S" True True False) defaultModel
                in
                Expect.equal True newModel.pendingSaveAs
        , test "Plain `s` does NOT trigger SaveFile (it inserts a swar)" <|
            -- SaveFile would addLog with a "Saved" status before the port
            -- fires; the swar-input path doesn't. We use that as the
            -- discriminator since both routes flip pendingApiCall.
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "s" False False False) defaultModel
                in
                newModel.statusLog
                    |> List.any (String.contains "Saved to")
                    |> Expect.equal False
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
