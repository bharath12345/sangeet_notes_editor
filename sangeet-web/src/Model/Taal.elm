module Model.Taal exposing
    ( VibhagMarker(..)
    , vibhagMarkerDecoder
    , encodeVibhagMarker
    , Vibhag
    , vibhagDecoder
    , encodeVibhag
    , Taal
    , taalDecoder
    , encodeTaal
    )

import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode exposing (Value)


-- VIBHAG MARKER


type VibhagMarker
    = Sam
    | TaaliMarker Int
    | KhaliMarker


vibhagMarkerDecoder : Decoder VibhagMarker
vibhagMarkerDecoder =
    Decode.oneOf
        [ Decode.string
            |> Decode.andThen
                (\s ->
                    case s of
                        "sam" ->
                            Decode.succeed Sam

                        "khali" ->
                            Decode.succeed KhaliMarker

                        other ->
                            Decode.fail ("Invalid VibhagMarker string: " ++ other)
                )
        , Decode.field "taali" Decode.int
            |> Decode.map TaaliMarker
        ]


encodeVibhagMarker : VibhagMarker -> Value
encodeVibhagMarker marker =
    case marker of
        Sam ->
            Encode.string "sam"

        KhaliMarker ->
            Encode.string "khali"

        TaaliMarker n ->
            Encode.object [ ( "taali", Encode.int n ) ]


-- VIBHAG


type alias Vibhag =
    { beats : Int
    , marker : VibhagMarker
    }


vibhagDecoder : Decoder Vibhag
vibhagDecoder =
    Decode.map2 Vibhag
        (Decode.field "beats" Decode.int)
        (Decode.field "marker" vibhagMarkerDecoder)


encodeVibhag : Vibhag -> Value
encodeVibhag v =
    Encode.object
        [ ( "beats", Encode.int v.beats )
        , ( "marker", encodeVibhagMarker v.marker )
        ]


-- TAAL


type alias Taal =
    { name : String
    , matras : Int
    , vibhags : List Vibhag
    , theka : Maybe (List String)
    }


taalDecoder : Decoder Taal
taalDecoder =
    Decode.map4 Taal
        (Decode.field "name" Decode.string)
        (Decode.field "matras" Decode.int)
        (Decode.field "vibhags" (Decode.list vibhagDecoder))
        (Decode.maybe (Decode.field "theka" (Decode.list Decode.string)))


encodeTaal : Taal -> Value
encodeTaal t =
    let
        base =
            [ ( "name", Encode.string t.name )
            , ( "matras", Encode.int t.matras )
            , ( "vibhags", Encode.list encodeVibhag t.vibhags )
            ]

        thekaField =
            case t.theka of
                Just th ->
                    [ ( "theka", Encode.list Encode.string th ) ]

                Nothing ->
                    []
    in
    Encode.object (base ++ thekaField)
