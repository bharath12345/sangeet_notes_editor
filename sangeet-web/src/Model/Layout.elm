module Model.Layout exposing
    ( LayoutConfig
    , encodeLayoutConfig
    , CycleAndBeat
    , BeatCell
    , beatCellDecoder
    , GridLine
    , gridLineDecoder
    , SectionGrid
    , sectionGridDecoder
    , GlyphInfo
    , glyphInfoDecoder
    , TimedNote
    , timedNoteDecoder
    , EditorResult
    , editorResultDecoder
    )

import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode exposing (Value)
import Model.Composition
    exposing
        ( Composition
        , SectionType
        , compositionDecoder
        , sectionTypeDecoder
        )
import Model.Cursor exposing (CursorModel, cursorDecoder)
import Model.Event exposing (Event, eventDecoder)
import Model.Taal exposing (VibhagMarker, vibhagMarkerDecoder)
import Model.Types
    exposing
        ( Note
        , Octave
        , Variant
        , noteDecoder
        , octaveDecoder
        , variantDecoder
        )


-- LAYOUT CONFIG (sent to server)


type alias LayoutConfig =
    { highDensityThreshold : Int
    , cellWidthBase : Float
    , cellOverflowExpand : Float
    , lineSpacing : Float
    , headerHeight : Float
    }


encodeLayoutConfig : LayoutConfig -> Value
encodeLayoutConfig c =
    Encode.object
        [ ( "highDensityThreshold", Encode.int c.highDensityThreshold )
        , ( "cellWidthBase", Encode.float c.cellWidthBase )
        , ( "cellOverflowExpand", Encode.float c.cellOverflowExpand )
        , ( "lineSpacing", Encode.float c.lineSpacing )
        , ( "headerHeight", Encode.float c.headerHeight )
        ]


-- CYCLE AND BEAT


type alias CycleAndBeat =
    { cycle : Int
    , beat : Int
    }


-- BEAT CELL


type alias BeatCell =
    { cycle : Int
    , beat : Int
    , events : List Event
    }


beatCellDecoder : Decoder BeatCell
beatCellDecoder =
    Decode.map3 BeatCell
        (Decode.field "cycle" Decode.int)
        (Decode.field "beat" Decode.int)
        (Decode.field "events" (Decode.list eventDecoder))


-- GRID LINE


type alias MarkerEntry =
    { cellIndex : Int
    , marker : VibhagMarker
    }


markerEntryDecoder : Decoder MarkerEntry
markerEntryDecoder =
    Decode.map2 MarkerEntry
        (Decode.field "cellIndex" Decode.int)
        (Decode.field "marker" vibhagMarkerDecoder)


type alias GridLine =
    { cells : List BeatCell
    , vibhagBreaks : List Int
    , markers : List MarkerEntry
    }


gridLineDecoder : Decoder GridLine
gridLineDecoder =
    Decode.map3 GridLine
        (Decode.field "cells" (Decode.list beatCellDecoder))
        (Decode.field "vibhagBreaks" (Decode.list Decode.int))
        (Decode.field "markers" (Decode.list markerEntryDecoder))


-- SECTION GRID


type alias SectionGrid =
    { sectionName : String
    , sectionType : SectionType
    , lines : List GridLine
    }


sectionGridDecoder : Decoder SectionGrid
sectionGridDecoder =
    Decode.map3 SectionGrid
        (Decode.field "sectionName" Decode.string)
        (Decode.field "sectionType" sectionTypeDecoder)
        (Decode.field "lines" (Decode.list gridLineDecoder))


-- GLYPH INFO


type alias GlyphInfo =
    { glyph : String
    , color : String
    }


glyphInfoDecoder : Decoder GlyphInfo
glyphInfoDecoder =
    Decode.map2 GlyphInfo
        (Decode.field "glyph" Decode.string)
        (Decode.field "color" Decode.string)


-- TIMED NOTE (for playback)


type alias TimedNote =
    { timeMs : Int
    , durationMs : Int
    , note : Note
    , variant : Variant
    , octave : Octave
    }


timedNoteDecoder : Decoder TimedNote
timedNoteDecoder =
    Decode.map5 TimedNote
        (Decode.field "timeMs" Decode.int)
        (Decode.field "durationMs" Decode.int)
        (Decode.field "note" noteDecoder)
        (Decode.field "variant" variantDecoder)
        (Decode.field "octave" octaveDecoder)


-- EDITOR RESULT (returned by editor/stroke/ornament operations)


type alias EditorResult =
    { composition : Composition
    , cursor : CursorModel
    , message : String
    }


editorResultDecoder : Decoder EditorResult
editorResultDecoder =
    Decode.map3 EditorResult
        (Decode.field "composition" compositionDecoder)
        (Decode.field "cursor" cursorDecoder)
        (Decode.field "message" Decode.string)
