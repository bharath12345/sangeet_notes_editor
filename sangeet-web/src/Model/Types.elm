module Model.Types exposing
    ( Note(..)
    , noteDecoder
    , encodeNote
    , Variant(..)
    , variantDecoder
    , encodeVariant
    , Octave(..)
    , octaveDecoder
    , encodeOctave
    , Stroke(..)
    , strokeDecoder
    , encodeStroke
    , Laya(..)
    , layaDecoder
    , encodeLaya
    , SwarScript(..)
    , swarScriptDecoder
    , encodeSwarScript
    , MeendDirection(..)
    , meendDirectionDecoder
    , encodeMeendDirection
    , Rational
    , rational
    , rationalDecoder
    , encodeRational
    , BeatPosition
    , beatPositionDecoder
    , encodeBeatPosition
    , NoteRef
    , noteRefDecoder
    , encodeNoteRef
    )

import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode exposing (Value)


-- NOTE


type Note
    = Sa
    | Re
    | Ga
    | Ma
    | Pa
    | Dha
    | Ni


noteDecoder : Decoder Note
noteDecoder =
    Decode.string
        |> Decode.andThen
            (\s ->
                case String.toLower s of
                    "sa" ->
                        Decode.succeed Sa

                    "re" ->
                        Decode.succeed Re

                    "ga" ->
                        Decode.succeed Ga

                    "ma" ->
                        Decode.succeed Ma

                    "pa" ->
                        Decode.succeed Pa

                    "dha" ->
                        Decode.succeed Dha

                    "ni" ->
                        Decode.succeed Ni

                    _ ->
                        Decode.fail ("Invalid Note: " ++ s)
            )


encodeNote : Note -> Value
encodeNote note =
    Encode.string
        (case note of
            Sa ->
                "sa"

            Re ->
                "re"

            Ga ->
                "ga"

            Ma ->
                "ma"

            Pa ->
                "pa"

            Dha ->
                "dha"

            Ni ->
                "ni"
        )


-- VARIANT


type Variant
    = Shuddha
    | Komal
    | Tivra


variantDecoder : Decoder Variant
variantDecoder =
    Decode.string
        |> Decode.andThen
            (\s ->
                case String.toLower s of
                    "shuddha" ->
                        Decode.succeed Shuddha

                    "komal" ->
                        Decode.succeed Komal

                    "tivra" ->
                        Decode.succeed Tivra

                    _ ->
                        Decode.fail ("Invalid Variant: " ++ s)
            )


encodeVariant : Variant -> Value
encodeVariant variant =
    Encode.string
        (case variant of
            Shuddha ->
                "shuddha"

            Komal ->
                "komal"

            Tivra ->
                "tivra"
        )


-- OCTAVE


type Octave
    = AtiMandra
    | Mandra
    | Madhya
    | Taar
    | AtiTaar


octaveDecoder : Decoder Octave
octaveDecoder =
    Decode.string
        |> Decode.andThen
            (\s ->
                case String.toLower s of
                    "atimandra" ->
                        Decode.succeed AtiMandra

                    "mandra" ->
                        Decode.succeed Mandra

                    "madhya" ->
                        Decode.succeed Madhya

                    "taar" ->
                        Decode.succeed Taar

                    "atitaar" ->
                        Decode.succeed AtiTaar

                    _ ->
                        Decode.fail ("Invalid Octave: " ++ s)
            )


encodeOctave : Octave -> Value
encodeOctave octave =
    Encode.string
        (case octave of
            AtiMandra ->
                "atiMandra"

            Mandra ->
                "mandra"

            Madhya ->
                "madhya"

            Taar ->
                "taar"

            AtiTaar ->
                "atiTaar"
        )


-- STROKE


type Stroke
    = Da
    | Ra
    | Chikari
    | Jod


strokeDecoder : Decoder Stroke
strokeDecoder =
    Decode.string
        |> Decode.andThen
            (\s ->
                case String.toLower s of
                    "da" ->
                        Decode.succeed Da

                    "ra" ->
                        Decode.succeed Ra

                    "chikari" ->
                        Decode.succeed Chikari

                    "jod" ->
                        Decode.succeed Jod

                    _ ->
                        Decode.fail ("Invalid Stroke: " ++ s)
            )


encodeStroke : Stroke -> Value
encodeStroke stroke =
    Encode.string
        (case stroke of
            Da ->
                "da"

            Ra ->
                "ra"

            Chikari ->
                "chikari"

            Jod ->
                "jod"
        )


-- LAYA


type Laya
    = AtiVilambit
    | Vilambit
    | MadhyaLaya
    | Drut
    | AtiDrut


layaDecoder : Decoder Laya
layaDecoder =
    Decode.string
        |> Decode.andThen
            (\s ->
                case String.toLower s of
                    "ativilambit" ->
                        Decode.succeed AtiVilambit

                    "vilambit" ->
                        Decode.succeed Vilambit

                    "madhya" ->
                        Decode.succeed MadhyaLaya

                    "drut" ->
                        Decode.succeed Drut

                    "atidrut" ->
                        Decode.succeed AtiDrut

                    _ ->
                        Decode.fail ("Invalid Laya: " ++ s)
            )


encodeLaya : Laya -> Value
encodeLaya laya =
    Encode.string
        (case laya of
            AtiVilambit ->
                "atiVilambit"

            Vilambit ->
                "vilambit"

            MadhyaLaya ->
                "madhya"

            Drut ->
                "drut"

            AtiDrut ->
                "atiDrut"
        )


-- SWAR SCRIPT


type SwarScript
    = Devanagari
    | Kannada
    | Telugu
    | English


swarScriptDecoder : Decoder SwarScript
swarScriptDecoder =
    Decode.string
        |> Decode.andThen
            (\s ->
                case String.toLower s of
                    "devanagari" ->
                        Decode.succeed Devanagari

                    "kannada" ->
                        Decode.succeed Kannada

                    "telugu" ->
                        Decode.succeed Telugu

                    "english" ->
                        Decode.succeed English

                    _ ->
                        Decode.fail ("Invalid SwarScript: " ++ s)
            )


encodeSwarScript : SwarScript -> Value
encodeSwarScript script =
    Encode.string
        (case script of
            Devanagari ->
                "devanagari"

            Kannada ->
                "kannada"

            Telugu ->
                "telugu"

            English ->
                "english"
        )


-- MEEND DIRECTION


type MeendDirection
    = Ascending
    | Descending


meendDirectionDecoder : Decoder MeendDirection
meendDirectionDecoder =
    Decode.string
        |> Decode.andThen
            (\s ->
                case String.toLower s of
                    "ascending" ->
                        Decode.succeed Ascending

                    "descending" ->
                        Decode.succeed Descending

                    _ ->
                        Decode.fail ("Invalid MeendDirection: " ++ s)
            )


encodeMeendDirection : MeendDirection -> Value
encodeMeendDirection dir =
    Encode.string
        (case dir of
            Ascending ->
                "ascending"

            Descending ->
                "descending"
        )


-- RATIONAL


type alias Rational =
    { numerator : Int
    , denominator : Int
    }


rational : Int -> Int -> Rational
rational num den =
    { numerator = num, denominator = den }


rationalDecoder : Decoder Rational
rationalDecoder =
    Decode.map2 Rational
        (Decode.index 0 Decode.int)
        (Decode.index 1 Decode.int)


encodeRational : Rational -> Value
encodeRational r =
    Encode.list Encode.int [ r.numerator, r.denominator ]


-- BEAT POSITION


type alias BeatPosition =
    { cycle : Int
    , beat : Int
    , subdivision : Rational
    }


beatPositionDecoder : Decoder BeatPosition
beatPositionDecoder =
    Decode.map3 BeatPosition
        (Decode.field "cycle" Decode.int)
        (Decode.field "beat" Decode.int)
        (Decode.field "subdivision" rationalDecoder)


encodeBeatPosition : BeatPosition -> Value
encodeBeatPosition bp =
    Encode.object
        [ ( "cycle", Encode.int bp.cycle )
        , ( "beat", Encode.int bp.beat )
        , ( "subdivision", encodeRational bp.subdivision )
        ]


-- NOTE REF


type alias NoteRef =
    { note : Note
    , variant : Variant
    , octave : Octave
    }


noteRefDecoder : Decoder NoteRef
noteRefDecoder =
    Decode.map3 NoteRef
        (Decode.field "note" noteDecoder)
        (Decode.field "variant" variantDecoder)
        (Decode.field "octave" octaveDecoder)


encodeNoteRef : NoteRef -> Value
encodeNoteRef nr =
    Encode.object
        [ ( "note", encodeNote nr.note )
        , ( "variant", encodeVariant nr.variant )
        , ( "octave", encodeOctave nr.octave )
        ]
