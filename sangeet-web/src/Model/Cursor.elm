module Model.Cursor exposing
    ( CursorModel
    , cursorDecoder
    , encodeCursor
    )

import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode exposing (Value)
import Model.Taal exposing (Taal, encodeTaal, taalDecoder)
import Model.Types exposing (BeatPosition, Octave, beatPositionDecoder, encodeBeatPosition, encodeOctave, octaveDecoder)


type alias CursorModel =
    { taal : Taal
    , cycle : Int
    , beat : Int
    , subIndex : Int
    , totalSubdivisions : Int
    , currentOctave : Octave
    , selectionAnchor : Maybe BeatPosition
    }


cursorDecoder : Decoder CursorModel
cursorDecoder =
    Decode.map7 CursorModel
        (Decode.field "taal" taalDecoder)
        (Decode.field "cycle" Decode.int)
        (Decode.field "beat" Decode.int)
        (Decode.field "subIndex" Decode.int)
        (Decode.field "totalSubdivisions" Decode.int)
        (Decode.field "currentOctave" octaveDecoder)
        (Decode.maybe (Decode.field "selectionAnchor" beatPositionDecoder))


encodeCursor : CursorModel -> Value
encodeCursor c =
    let
        baseFields =
            [ ( "taal", encodeTaal c.taal )
            , ( "cycle", Encode.int c.cycle )
            , ( "beat", Encode.int c.beat )
            , ( "subIndex", Encode.int c.subIndex )
            , ( "totalSubdivisions", Encode.int c.totalSubdivisions )
            , ( "currentOctave", encodeOctave c.currentOctave )
            ]

        anchorField =
            case c.selectionAnchor of
                Just bp ->
                    [ ( "selectionAnchor", encodeBeatPosition bp ) ]

                Nothing ->
                    []
    in
    Encode.object (baseFields ++ anchorField)
