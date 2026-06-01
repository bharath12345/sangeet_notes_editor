module UpdatePlaybackTest exposing (..)

import Expect
import State.Model exposing (PlaybackState(..))
import State.Msg exposing (Msg(..))
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultModel)


suite : Test
suite =
    describe "Update playback operations"
        [ playTests
        , pauseTests
        , stopTests
        , bpmTests
        , loopTests
        ]


playTests : Test
playTests =
    describe "Play"
        [ test "Play from Stopped sets Playing" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update Play defaultModel
                in
                Expect.equal Playing newModel.playbackState
        , test "Play from Paused sets Playing" <|
            \_ ->
                let
                    model =
                        { defaultModel | playbackState = Paused }

                    ( newModel, _ ) =
                        update Play model
                in
                Expect.equal Playing newModel.playbackState
        , test "Play sets pendingApiCall" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update Play defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        ]


pauseTests : Test
pauseTests =
    describe "Pause"
        [ test "Pause from Playing sets Paused" <|
            \_ ->
                let
                    model =
                        { defaultModel | playbackState = Playing }

                    ( newModel, _ ) =
                        update Pause model
                in
                Expect.equal Paused newModel.playbackState
        , test "Pause adds log" <|
            \_ ->
                let
                    model =
                        { defaultModel | playbackState = Playing }

                    ( newModel, _ ) =
                        update Pause model
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "paused" (String.toLower first))

                    [] ->
                        Expect.fail "statusLog should not be empty"
        ]


stopTests : Test
stopTests =
    describe "Stop"
        [ test "Stop from Playing sets Stopped" <|
            \_ ->
                let
                    model =
                        { defaultModel | playbackState = Playing }

                    ( newModel, _ ) =
                        update Stop model
                in
                Expect.equal Stopped newModel.playbackState
        , test "Stop adds log" <|
            \_ ->
                let
                    model =
                        { defaultModel | playbackState = Playing }

                    ( newModel, _ ) =
                        update Stop model
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "stopped" (String.toLower first))

                    [] ->
                        Expect.fail "statusLog should not be empty"
        ]


bpmTests : Test
bpmTests =
    describe "BPM"
        [ test "SetBpm updates bpm value" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (SetBpm 180.0) defaultModel
                in
                Expect.within (Expect.Absolute 0.001) 180.0 newModel.bpm
        , test "SetBpm to 0 is allowed" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (SetBpm 0.0) defaultModel
                in
                Expect.within (Expect.Absolute 0.001) 0.0 newModel.bpm
        ]


loopTests : Test
loopTests =
    describe "Loop toggle"
        [ test "ToggleLoop from false to true" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update ToggleLoop defaultModel
                in
                Expect.equal True newModel.loopEnabled
        , test "ToggleLoop from true to false" <|
            \_ ->
                let
                    model =
                        { defaultModel | loopEnabled = True }

                    ( newModel, _ ) =
                        update ToggleLoop model
                in
                Expect.equal False newModel.loopEnabled
        ]
