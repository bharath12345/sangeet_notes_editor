module LockedBeatTest exposing (codecTests, startingBeatResponseTests, suite)

import Api.Client exposing (ApiResult(..))
import Expect
import Http
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Event exposing (Event(..), encodeEvent, eventDecoder)
import State.Model as Model
import State.Msg exposing (Msg(..))
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultComposition, defaultModel)


suite : Test
suite =
    describe "LockedBeat feature"
        [ codecTests
        , startingBeatResponseTests
        ]



-- CODEC TESTS


codecTests : Test
codecTests =
    describe "LockedBeatEvent codec"
        [ test "roundtrip through JSON" <|
            \_ ->
                let
                    event =
                        LockedBeatEvent
                            { beat = { cycle = 0, beat = 3, subdivision = { numerator = 0, denominator = 1 } }
                            , duration = { numerator = 1, denominator = 1 }
                            }

                    encoded =
                        encodeEvent event

                    decoded =
                        Decode.decodeValue eventDecoder encoded
                in
                Expect.equal (Ok event) decoded
        , test "serializes with type discriminator 'lockedbeat'" <|
            \_ ->
                let
                    event =
                        LockedBeatEvent
                            { beat = { cycle = 0, beat = 0, subdivision = { numerator = 0, denominator = 1 } }
                            , duration = { numerator = 1, denominator = 1 }
                            }

                    encoded =
                        encodeEvent event

                    typeResult =
                        Decode.decodeValue (Decode.field "type" Decode.string) encoded
                in
                Expect.equal (Ok "lockedbeat") typeResult
        , test "decodes from raw JSON" <|
            \_ ->
                let
                    json =
                        Encode.object
                            [ ( "type", Encode.string "lockedbeat" )
                            , ( "beat"
                              , Encode.object
                                    [ ( "cycle", Encode.int 0 )
                                    , ( "beat", Encode.int 5 )
                                    , ( "subdivision", Encode.list Encode.int [ 1, 2 ] )
                                    ]
                              )
                            , ( "duration", Encode.list Encode.int [ 1, 4 ] )
                            ]

                    decoded =
                        Decode.decodeValue eventDecoder json
                in
                case decoded of
                    Ok (LockedBeatEvent r) ->
                        Expect.all
                            [ \_ -> Expect.equal 5 r.beat.beat
                            , \_ -> Expect.equal 1 r.duration.numerator
                            , \_ -> Expect.equal 4 r.duration.denominator
                            ]
                            ()

                    Ok _ ->
                        Expect.fail "Expected LockedBeatEvent"

                    Err e ->
                        Expect.fail ("Decode failed: " ++ Decode.errorToString e)
        , test "includes beat and duration fields" <|
            \_ ->
                let
                    event =
                        LockedBeatEvent
                            { beat = { cycle = 0, beat = 7, subdivision = { numerator = 0, denominator = 1 } }
                            , duration = { numerator = 1, denominator = 1 }
                            }

                    encoded =
                        encodeEvent event

                    beatResult =
                        Decode.decodeValue (Decode.field "beat" (Decode.field "beat" Decode.int)) encoded

                    durationResult =
                        Decode.decodeValue (Decode.field "duration" (Decode.index 0 Decode.int)) encoded
                in
                Expect.all
                    [ \_ -> Expect.equal (Ok 7) beatResult
                    , \_ -> Expect.equal (Ok 1) durationResult
                    ]
                    ()
        ]



-- STARTING BEAT RESPONSE TESTS


startingBeatResponseTests : Test
startingBeatResponseTests =
    describe "GotStartingBeatResult handling"
        [ test "Success updates composition" <|
            \_ ->
                let
                    newComp =
                        { defaultComposition
                            | metadata =
                                let
                                    m =
                                        defaultComposition.metadata
                                in
                                { m | title = "Updated" }
                        }

                    ( newModel, _ ) =
                        update (GotStartingBeatResult (Ok (Success newComp))) defaultModel
                in
                Expect.equal "Updated" (Model.composition newModel).metadata.title
        , test "Success with pending changes stores remaining changes" <|
            \_ ->
                let
                    model =
                        { defaultModel | pendingStartingBeatChanges = [ ( 0, 5 ), ( 1, 9 ) ] }

                    ( newModel, _ ) =
                        update (GotStartingBeatResult (Ok (Success defaultComposition))) model
                in
                Expect.equal [ ( 1, 9 ) ] newModel.pendingStartingBeatChanges
        , test "Success with no pending changes clears list" <|
            \_ ->
                let
                    model =
                        { defaultModel | pendingStartingBeatChanges = [] }

                    ( newModel, _ ) =
                        update (GotStartingBeatResult (Ok (Success defaultComposition))) model
                in
                Expect.equal [] newModel.pendingStartingBeatChanges
        , test "Success with no pending changes adds log message" <|
            \_ ->
                let
                    model =
                        { defaultModel | pendingStartingBeatChanges = [] }

                    ( newModel, _ ) =
                        update (GotStartingBeatResult (Ok (Success defaultComposition))) model
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "Starting beats updated" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        , test "HTTP error clears pendingApiCall" <|
            \_ ->
                let
                    model =
                        { defaultModel | pendingApiCall = True }

                    ( newModel, _ ) =
                        update (GotStartingBeatResult (Err Http.NetworkError)) model
                in
                Expect.equal False newModel.pendingApiCall
        , test "ApiFailure clears pendingApiCall and logs error" <|
            \_ ->
                let
                    model =
                        { defaultModel | pendingApiCall = True }

                    error =
                        { code = "INVALID_INPUT", message = "Starting beat out of range" }

                    ( newModel, _ ) =
                        update (GotStartingBeatResult (Ok (ApiFailure error))) model
                in
                Expect.all
                    [ \m -> Expect.equal False m.pendingApiCall
                    , \m ->
                        case m.statusLog of
                            first :: _ ->
                                Expect.equal True (String.contains "Starting beat out of range" first)

                            [] ->
                                Expect.fail "statusLog should not be empty"
                    ]
                    newModel
        ]
