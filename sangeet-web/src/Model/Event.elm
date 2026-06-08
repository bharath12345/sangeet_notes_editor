module Model.Event exposing
    ( Event(..)
    , encodeEvent
    , eventDecoder
    )

import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode exposing (Value)
import Model.Ornament exposing (Ornament, encodeOrnament, ornamentDecoder)
import Model.Types
    exposing
        ( BeatPosition
        , Note
        , Octave
        , Rational
        , Stroke
        , Variant
        , beatPositionDecoder
        , encodeBeatPosition
        , encodeNote
        , encodeOctave
        , encodeRational
        , encodeStroke
        , encodeVariant
        , noteDecoder
        , octaveDecoder
        , rationalDecoder
        , strokeDecoder
        , variantDecoder
        )


type Event
    = SwarEvent
        { note : Note
        , variant : Variant
        , octave : Octave
        , beat : BeatPosition
        , duration : Rational
        , stroke : Maybe Stroke
        , ornaments : List Ornament
        , sahitya : Maybe String
        }
    | RestEvent
        { beat : BeatPosition
        , duration : Rational
        }
    | SustainEvent
        { beat : BeatPosition
        , duration : Rational
        }
    | ChikariEvent
        { beat : BeatPosition
        , duration : Rational
        }


eventDecoder : Decoder Event
eventDecoder =
    Decode.field "type" Decode.string
        |> Decode.andThen eventByType


eventByType : String -> Decoder Event
eventByType typeName =
    case typeName of
        "swar" ->
            Decode.map8
                (\n v o b d s orn sah ->
                    SwarEvent
                        { note = n
                        , variant = v
                        , octave = o
                        , beat = b
                        , duration = d
                        , stroke = s
                        , ornaments = orn
                        , sahitya = sah
                        }
                )
                (Decode.field "note" noteDecoder)
                (Decode.field "variant" variantDecoder)
                (Decode.field "octave" octaveDecoder)
                (Decode.field "beat" beatPositionDecoder)
                (Decode.field "duration" rationalDecoder)
                (Decode.maybe (Decode.field "stroke" strokeDecoder))
                (Decode.field "ornaments" (Decode.list ornamentDecoder))
                (Decode.maybe (Decode.field "sahitya" Decode.string))

        "rest" ->
            Decode.map2
                (\b d -> RestEvent { beat = b, duration = d })
                (Decode.field "beat" beatPositionDecoder)
                (Decode.field "duration" rationalDecoder)

        "sustain" ->
            Decode.map2
                (\b d -> SustainEvent { beat = b, duration = d })
                (Decode.field "beat" beatPositionDecoder)
                (Decode.field "duration" rationalDecoder)

        "chikari" ->
            Decode.map2
                (\b d -> ChikariEvent { beat = b, duration = d })
                (Decode.field "beat" beatPositionDecoder)
                (Decode.field "duration" rationalDecoder)

        other ->
            Decode.fail ("Unknown event type: " ++ other)


encodeEvent : Event -> Value
encodeEvent event =
    case event of
        SwarEvent r ->
            let
                base =
                    [ ( "type", Encode.string "swar" )
                    , ( "note", encodeNote r.note )
                    , ( "variant", encodeVariant r.variant )
                    , ( "octave", encodeOctave r.octave )
                    , ( "beat", encodeBeatPosition r.beat )
                    , ( "duration", encodeRational r.duration )
                    , ( "ornaments", Encode.list encodeOrnament r.ornaments )
                    ]

                strokeField =
                    case r.stroke of
                        Just s ->
                            [ ( "stroke", encodeStroke s ) ]

                        Nothing ->
                            []

                sahityaField =
                    case r.sahitya of
                        Just s ->
                            [ ( "sahitya", Encode.string s ) ]

                        Nothing ->
                            []
            in
            Encode.object (base ++ strokeField ++ sahityaField)

        RestEvent r ->
            Encode.object
                [ ( "type", Encode.string "rest" )
                , ( "beat", encodeBeatPosition r.beat )
                , ( "duration", encodeRational r.duration )
                ]

        SustainEvent r ->
            Encode.object
                [ ( "type", Encode.string "sustain" )
                , ( "beat", encodeBeatPosition r.beat )
                , ( "duration", encodeRational r.duration )
                ]

        ChikariEvent r ->
            Encode.object
                [ ( "type", Encode.string "chikari" )
                , ( "beat", encodeBeatPosition r.beat )
                , ( "duration", encodeRational r.duration )
                ]
