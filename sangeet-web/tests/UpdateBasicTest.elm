module UpdateBasicTest exposing (..)

import Expect
import Model.Types exposing (SwarScript(..))
import State.Model exposing (EditMode(..), OrnamentMode(..), PlaybackState(..))
import State.Msg exposing (Msg(..))
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultModel)
import Time


suite : Test
suite =
    describe "Update basic model transitions"
        [ noOpTests
        , cursorBlinkTests
        , scriptChangeTests
        , editModeTests
        , viewToggleTests
        , playbackStateTests
        , bpmAndLoopTests
        ]


noOpTests : Test
noOpTests =
    describe "NoOp"
        [ test "NoOp does not change model" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update NoOp defaultModel
                in
                Expect.equal defaultModel newModel
        ]


cursorBlinkTests : Test
cursorBlinkTests =
    describe "CursorBlink"
        [ test "CursorBlink toggles cursorVisible from True to False" <|
            \_ ->
                let
                    model =
                        { defaultModel | cursorVisible = True }

                    ( newModel, _ ) =
                        update (CursorBlink (Time.millisToPosix 0)) model
                in
                Expect.equal False newModel.cursorVisible
        , test "CursorBlink toggles cursorVisible from False to True" <|
            \_ ->
                let
                    model =
                        { defaultModel | cursorVisible = False }

                    ( newModel, _ ) =
                        update (CursorBlink (Time.millisToPosix 0)) model
                in
                Expect.equal True newModel.cursorVisible
        ]


scriptChangeTests : Test
scriptChangeTests =
    describe "ChangeScript"
        [ test "ChangeScript to Kannada updates currentScript" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (ChangeScript Kannada) defaultModel
                in
                Expect.equal Kannada newModel.currentScript
        , test "ChangeScript to English updates currentScript" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (ChangeScript English) defaultModel
                in
                Expect.equal English newModel.currentScript
        , test "ChangeScript to Telugu updates currentScript" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (ChangeScript Telugu) defaultModel
                in
                Expect.equal Telugu newModel.currentScript
        , test "ChangeScript adds log entry" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (ChangeScript Kannada) defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "Kannada" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        ]


editModeTests : Test
editModeTests =
    describe "ToggleEditMode via KeyPressed"
        [ test "F2 toggles SwarEdit to StrokeEdit" <|
            \_ ->
                let
                    model =
                        { defaultModel | editMode = SwarEdit }

                    ( newModel, _ ) =
                        update (KeyPressed "F2" False False False) model
                in
                Expect.equal StrokeEdit newModel.editMode
        , test "F2 toggles StrokeEdit to SwarEdit" <|
            \_ ->
                let
                    model =
                        { defaultModel | editMode = StrokeEdit }

                    ( newModel, _ ) =
                        update (KeyPressed "F2" False False False) model
                in
                Expect.equal SwarEdit newModel.editMode
        , test "Shift+Tab toggles edit mode" <|
            \_ ->
                let
                    model =
                        { defaultModel | editMode = SwarEdit }

                    ( newModel, _ ) =
                        update (KeyPressed "Tab" True False False) model
                in
                Expect.equal StrokeEdit newModel.editMode
        ]


viewToggleTests : Test
viewToggleTests =
    describe "View toggles"
        [ test "ToggleKeyboardLegend shows legend" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update ToggleKeyboardLegend defaultModel
                in
                Expect.equal True newModel.showKeyboardLegend
        , test "ToggleKeyboardLegend hides legend" <|
            \_ ->
                let
                    model =
                        { defaultModel | showKeyboardLegend = True }

                    ( newModel, _ ) =
                        update ToggleKeyboardLegend model
                in
                Expect.equal False newModel.showKeyboardLegend
        ]


playbackStateTests : Test
playbackStateTests =
    describe "Playback state transitions"
        [ test "Play sets playbackState to Playing" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update Play defaultModel
                in
                Expect.equal Playing newModel.playbackState
        , test "Pause sets playbackState to Paused" <|
            \_ ->
                let
                    model =
                        { defaultModel | playbackState = Playing }

                    ( newModel, _ ) =
                        update Pause model
                in
                Expect.equal Paused newModel.playbackState
        , test "Stop sets playbackState to Stopped" <|
            \_ ->
                let
                    model =
                        { defaultModel | playbackState = Playing }

                    ( newModel, _ ) =
                        update Stop model
                in
                Expect.equal Stopped newModel.playbackState
        , test "Play sets pendingApiCall to True" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update Play defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        ]


bpmAndLoopTests : Test
bpmAndLoopTests =
    describe "BPM and loop controls"
        [ test "SetBpm updates bpm" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (SetBpm 120.0) defaultModel
                in
                Expect.within (Expect.Absolute 0.001) 120.0 newModel.bpm
        , test "ToggleLoop enables loop" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update ToggleLoop defaultModel
                in
                Expect.equal True newModel.loopEnabled
        , test "ToggleLoop disables loop" <|
            \_ ->
                let
                    model =
                        { defaultModel | loopEnabled = True }

                    ( newModel, _ ) =
                        update ToggleLoop model
                in
                Expect.equal False newModel.loopEnabled
        ]
