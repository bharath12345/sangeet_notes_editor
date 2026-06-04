module Model.Composition exposing
    ( CompositionType(..)
    , compositionTypeDecoder
    , encodeCompositionType
    , SectionType(..)
    , sectionTypeDecoder
    , encodeSectionType
    , Tihai
    , tihaiDecoder
    , encodeTihai
    , Metadata
    , metadataDecoder
    , encodeMetadata
    , Section
    , sectionDecoder
    , encodeSection
    , Composition
    , compositionDecoder
    , encodeComposition
    )

import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode exposing (Value)
import Model.Event exposing (Event, encodeEvent, eventDecoder)
import Model.Raag exposing (Raag, encodeRaag, raagDecoder)
import Model.Taal exposing (Taal, encodeTaal, taalDecoder)
import Model.Types
    exposing
        ( BeatPosition
        , Laya
        , beatPositionDecoder
        , encodeBeatPosition
        , encodeLaya
        , layaDecoder
        )


-- COMPOSITION TYPE


type CompositionType
    = Bandish
    | Gat
    | Palta
    | Sargam
    | CustomCompositionType String


compositionTypeDecoder : Decoder CompositionType
compositionTypeDecoder =
    Decode.oneOf
        [ Decode.string
            |> Decode.andThen
                (\s ->
                    case String.toLower s of
                        "bandish" ->
                            Decode.succeed Bandish

                        "gat" ->
                            Decode.succeed Gat

                        "palta" ->
                            Decode.succeed Palta

                        "sargam" ->
                            Decode.succeed Sargam

                        other ->
                            Decode.fail ("Invalid CompositionType: " ++ other)
                )
        , Decode.field "custom" Decode.string
            |> Decode.map CustomCompositionType
        ]


encodeCompositionType : CompositionType -> Value
encodeCompositionType ct =
    case ct of
        Bandish ->
            Encode.string "bandish"

        Gat ->
            Encode.string "gat"

        Palta ->
            Encode.string "palta"

        Sargam ->
            Encode.string "sargam"

        CustomCompositionType name ->
            Encode.object [ ( "custom", Encode.string name ) ]


-- SECTION TYPE


type SectionType
    = Sthayi
    | Antara
    | Sanchari
    | Abhog
    | Taan
    | Toda
    | Jhala
    | PaltaSection
    | Arohi
    | Avarohi
    | CustomSectionType String


sectionTypeDecoder : Decoder SectionType
sectionTypeDecoder =
    Decode.oneOf
        [ Decode.string
            |> Decode.andThen
                (\s ->
                    case String.toLower s of
                        "sthayi" ->
                            Decode.succeed Sthayi

                        "antara" ->
                            Decode.succeed Antara

                        "sanchari" ->
                            Decode.succeed Sanchari

                        "abhog" ->
                            Decode.succeed Abhog

                        "taan" ->
                            Decode.succeed Taan

                        "toda" ->
                            Decode.succeed Toda

                        "jhala" ->
                            Decode.succeed Jhala

                        "palta" ->
                            Decode.succeed PaltaSection

                        "arohi" ->
                            Decode.succeed Arohi

                        "avarohi" ->
                            Decode.succeed Avarohi

                        other ->
                            Decode.fail ("Invalid SectionType: " ++ other)
                )
        , Decode.field "custom" Decode.string
            |> Decode.map CustomSectionType
        ]


encodeSectionType : SectionType -> Value
encodeSectionType st =
    case st of
        Sthayi ->
            Encode.string "sthayi"

        Antara ->
            Encode.string "antara"

        Sanchari ->
            Encode.string "sanchari"

        Abhog ->
            Encode.string "abhog"

        Taan ->
            Encode.string "taan"

        Toda ->
            Encode.string "toda"

        Jhala ->
            Encode.string "jhala"

        PaltaSection ->
            Encode.string "palta"

        Arohi ->
            Encode.string "arohi"

        Avarohi ->
            Encode.string "avarohi"

        CustomSectionType name ->
            Encode.object [ ( "custom", Encode.string name ) ]


-- TIHAI


type alias Tihai =
    { startBeat : BeatPosition
    , landingBeat : BeatPosition
    }


tihaiDecoder : Decoder Tihai
tihaiDecoder =
    Decode.map2 Tihai
        (Decode.field "startBeat" beatPositionDecoder)
        (Decode.field "landingBeat" beatPositionDecoder)


encodeTihai : Tihai -> Value
encodeTihai t =
    Encode.object
        [ ( "startBeat", encodeBeatPosition t.startBeat )
        , ( "landingBeat", encodeBeatPosition t.landingBeat )
        ]


-- METADATA


type alias Metadata =
    { title : String
    , compositionType : CompositionType
    , raag : Raag
    , taal : Taal
    , laya : Maybe Laya
    , instrument : Maybe String
    , composer : Maybe String
    , author : Maybe String
    , source : Maybe String
    , showStrokeLine : Bool
    , showSahityaLine : Bool
    , createdAt : String
    , updatedAt : String
    }


metadataDecoder : Decoder Metadata
metadataDecoder =
    -- Elm doesn't have map13, so we use andThen pipeline
    Decode.succeed Metadata
        |> andMap (Decode.field "title" Decode.string)
        |> andMap (Decode.field "compositionType" compositionTypeDecoder)
        |> andMap (Decode.field "raag" raagDecoder)
        |> andMap (Decode.field "taal" taalDecoder)
        |> andMap (Decode.maybe (Decode.field "laya" layaDecoder))
        |> andMap (Decode.maybe (Decode.field "instrument" Decode.string))
        |> andMap (Decode.maybe (Decode.field "composer" Decode.string))
        |> andMap (Decode.maybe (Decode.field "author" Decode.string))
        |> andMap (Decode.maybe (Decode.field "source" Decode.string))
        |> andMap (optionalFieldWithDefault "showStrokeLine" False Decode.bool)
        |> andMap (optionalFieldWithDefault "showSahityaLine" False Decode.bool)
        |> andMap (Decode.field "createdAt" Decode.string)
        |> andMap (Decode.field "updatedAt" Decode.string)


encodeMetadata : Metadata -> Value
encodeMetadata m =
    let
        optStr key val =
            case val of
                Just v ->
                    [ ( key, Encode.string v ) ]

                Nothing ->
                    []

        optLaya =
            case m.laya of
                Just l ->
                    [ ( "laya", encodeLaya l ) ]

                Nothing ->
                    []
    in
    Encode.object
        ([ ( "title", Encode.string m.title )
         , ( "compositionType", encodeCompositionType m.compositionType )
         , ( "raag", encodeRaag m.raag )
         , ( "taal", encodeTaal m.taal )
         ]
            ++ optLaya
            ++ optStr "instrument" m.instrument
            ++ optStr "composer" m.composer
            ++ optStr "author" m.author
            ++ optStr "source" m.source
            ++ [ ( "showStrokeLine", Encode.bool m.showStrokeLine )
               , ( "showSahityaLine", Encode.bool m.showSahityaLine )
               , ( "createdAt", Encode.string m.createdAt )
               , ( "updatedAt", Encode.string m.updatedAt )
               ]
        )


-- SECTION


type alias Section =
    { name : String
    , sectionType : SectionType
    , events : List Event
    , tihai : Maybe Tihai
    }


sectionDecoder : Decoder Section
sectionDecoder =
    Decode.map4 Section
        (Decode.field "name" Decode.string)
        (Decode.field "type" sectionTypeDecoder)
        (Decode.field "events" (Decode.list eventDecoder))
        (Decode.maybe (Decode.field "tihai" tihaiDecoder))


encodeSection : Section -> Value
encodeSection s =
    let
        base =
            [ ( "name", Encode.string s.name )
            , ( "type", encodeSectionType s.sectionType )
            , ( "events", Encode.list encodeEvent s.events )
            ]

        tihaiField =
            case s.tihai of
                Just t ->
                    [ ( "tihai", encodeTihai t ) ]

                Nothing ->
                    []
    in
    Encode.object (base ++ tihaiField)


-- COMPOSITION


type alias Composition =
    { metadata : Metadata
    , sections : List Section
    }


compositionDecoder : Decoder Composition
compositionDecoder =
    Decode.map2 Composition
        (Decode.field "metadata" metadataDecoder)
        (Decode.field "sections" (Decode.list sectionDecoder))


encodeComposition : Composition -> Value
encodeComposition c =
    Encode.object
        [ ( "metadata", encodeMetadata c.metadata )
        , ( "sections", Encode.list encodeSection c.sections )
        ]



-- HELPERS


{-| Apply a decoder to a function inside a decoder (applicative style).
-}
andMap : Decoder a -> Decoder (a -> b) -> Decoder b
andMap =
    Decode.map2 (|>)


{-| Decode an optional field with a default value.
-}
optionalFieldWithDefault : String -> a -> Decoder a -> Decoder a
optionalFieldWithDefault field default decoder =
    Decode.oneOf
        [ Decode.field field decoder
        , Decode.succeed default
        ]
