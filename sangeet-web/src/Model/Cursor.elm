module Model.Cursor exposing
    ( CursorModel
    , cursorDecoder
    , encodeCursor
    )

import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode exposing (Value)
import Model.Taal exposing (Taal, encodeTaal, taalDecoder)
import Model.Types exposing (Octave, encodeOctave, octaveDecoder)


type alias CursorModel =
    { taal : Taal
    , cycle : Int
    , beat : Int
    , subIndex : Int
    , totalSubdivisions : Int
    , currentOctave : Octave
    }


cursorDecoder : Decoder CursorModel
cursorDecoder =
    Decode.map6 CursorModel
        (Decode.field "taal" taalDecoder)
        (Decode.field "cycle" Decode.int)
        (Decode.field "beat" Decode.int)
        (Decode.field "subIndex" Decode.int)
        (Decode.field "totalSubdivisions" Decode.int)
        (Decode.field "currentOctave" octaveDecoder)


encodeCursor : CursorModel -> Value
encodeCursor c =
    Encode.object
        [ ( "taal", encodeTaal c.taal )
        , ( "cycle", Encode.int c.cycle )
        , ( "beat", Encode.int c.beat )
        , ( "subIndex", Encode.int c.subIndex )
        , ( "totalSubdivisions", Encode.int c.totalSubdivisions )
        , ( "currentOctave", encodeOctave c.currentOctave )
        ]
