module Model.Raag exposing
    ( Raag
    , encodeRaag
    , raagDecoder
    )

import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode exposing (Value)


type alias Raag =
    { name : String
    , thaat : Maybe String
    , arohana : Maybe (List String)
    , avarohana : Maybe (List String)
    , vadi : Maybe String
    , samvadi : Maybe String
    , pakad : Maybe String
    , prahar : Maybe Int
    }


raagDecoder : Decoder Raag
raagDecoder =
    Decode.map8 Raag
        (Decode.field "name" Decode.string)
        (Decode.maybe (Decode.field "thaat" Decode.string))
        (Decode.maybe (Decode.field "arohana" (Decode.list Decode.string)))
        (Decode.maybe (Decode.field "avarohana" (Decode.list Decode.string)))
        (Decode.maybe (Decode.field "vadi" Decode.string))
        (Decode.maybe (Decode.field "samvadi" Decode.string))
        (Decode.maybe (Decode.field "pakad" Decode.string))
        (Decode.maybe (Decode.field "prahar" Decode.int))


encodeRaag : Raag -> Value
encodeRaag r =
    let
        optStr key val =
            case val of
                Just v ->
                    [ ( key, Encode.string v ) ]

                Nothing ->
                    []

        optInt key val =
            case val of
                Just v ->
                    [ ( key, Encode.int v ) ]

                Nothing ->
                    []

        optList key val =
            case val of
                Just v ->
                    [ ( key, Encode.list Encode.string v ) ]

                Nothing ->
                    []
    in
    Encode.object
        (( "name", Encode.string r.name )
            :: optStr "thaat" r.thaat
            ++ optList "arohana" r.arohana
            ++ optList "avarohana" r.avarohana
            ++ optStr "vadi" r.vadi
            ++ optStr "samvadi" r.samvadi
            ++ optStr "pakad" r.pakad
            ++ optInt "prahar" r.prahar
        )
