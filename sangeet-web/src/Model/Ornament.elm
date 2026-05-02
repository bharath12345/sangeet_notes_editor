module Model.Ornament exposing
    ( Ornament(..)
    , ornamentDecoder
    , encodeOrnament
    )

import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode exposing (Value)
import Model.Types
    exposing
        ( MeendDirection
        , NoteRef
        , encodeMeendDirection
        , encodeNoteRef
        , meendDirectionDecoder
        , noteRefDecoder
        )


type Ornament
    = Meend
        { startNote : NoteRef
        , endNote : NoteRef
        , direction : MeendDirection
        , intermediateNotes : List NoteRef
        }
    | KanSwar { graceNote : NoteRef }
    | Murki { notes : List NoteRef }
    | Gamak
    | Andolan
    | Krintan { notes : List NoteRef }
    | Gitkari
    | Ghaseet { targetNote : NoteRef }
    | Sparsh { touchNote : NoteRef }
    | Zamzama { notes : List NoteRef }
    | CustomOrnament
        { name : String
        , parameters : List ( String, String )
        }


ornamentDecoder : Decoder Ornament
ornamentDecoder =
    Decode.field "type" Decode.string
        |> Decode.andThen ornamentByType


ornamentByType : String -> Decoder Ornament
ornamentByType typeName =
    case typeName of
        "meend" ->
            Decode.map4
                (\s e d i ->
                    Meend
                        { startNote = s
                        , endNote = e
                        , direction = d
                        , intermediateNotes = i
                        }
                )
                (Decode.field "startNote" noteRefDecoder)
                (Decode.field "endNote" noteRefDecoder)
                (Decode.field "direction" meendDirectionDecoder)
                (Decode.field "intermediateNotes" (Decode.list noteRefDecoder))

        "kanSwar" ->
            Decode.map (\g -> KanSwar { graceNote = g })
                (Decode.field "graceNote" noteRefDecoder)

        "murki" ->
            Decode.map (\ns -> Murki { notes = ns })
                (Decode.field "notes" (Decode.list noteRefDecoder))

        "gamak" ->
            Decode.succeed Gamak

        "andolan" ->
            Decode.succeed Andolan

        "krintan" ->
            Decode.map (\ns -> Krintan { notes = ns })
                (Decode.field "notes" (Decode.list noteRefDecoder))

        "gitkari" ->
            Decode.succeed Gitkari

        "ghaseet" ->
            Decode.map (\t -> Ghaseet { targetNote = t })
                (Decode.field "targetNote" noteRefDecoder)

        "sparsh" ->
            Decode.map (\t -> Sparsh { touchNote = t })
                (Decode.field "touchNote" noteRefDecoder)

        "zamzama" ->
            Decode.map (\ns -> Zamzama { notes = ns })
                (Decode.field "notes" (Decode.list noteRefDecoder))

        "custom" ->
            Decode.map2
                (\n p -> CustomOrnament { name = n, parameters = p })
                (Decode.field "name" Decode.string)
                (Decode.field "parameters" (Decode.keyValuePairs Decode.string))

        other ->
            Decode.fail ("Unknown ornament type: " ++ other)


encodeOrnament : Ornament -> Value
encodeOrnament ornament =
    case ornament of
        Meend r ->
            Encode.object
                [ ( "type", Encode.string "meend" )
                , ( "startNote", encodeNoteRef r.startNote )
                , ( "endNote", encodeNoteRef r.endNote )
                , ( "direction", encodeMeendDirection r.direction )
                , ( "intermediateNotes", Encode.list encodeNoteRef r.intermediateNotes )
                ]

        KanSwar r ->
            Encode.object
                [ ( "type", Encode.string "kanSwar" )
                , ( "graceNote", encodeNoteRef r.graceNote )
                ]

        Murki r ->
            Encode.object
                [ ( "type", Encode.string "murki" )
                , ( "notes", Encode.list encodeNoteRef r.notes )
                ]

        Gamak ->
            Encode.object [ ( "type", Encode.string "gamak" ) ]

        Andolan ->
            Encode.object [ ( "type", Encode.string "andolan" ) ]

        Krintan r ->
            Encode.object
                [ ( "type", Encode.string "krintan" )
                , ( "notes", Encode.list encodeNoteRef r.notes )
                ]

        Gitkari ->
            Encode.object [ ( "type", Encode.string "gitkari" ) ]

        Ghaseet r ->
            Encode.object
                [ ( "type", Encode.string "ghaseet" )
                , ( "targetNote", encodeNoteRef r.targetNote )
                ]

        Sparsh r ->
            Encode.object
                [ ( "type", Encode.string "sparsh" )
                , ( "touchNote", encodeNoteRef r.touchNote )
                ]

        Zamzama r ->
            Encode.object
                [ ( "type", Encode.string "zamzama" )
                , ( "notes", Encode.list encodeNoteRef r.notes )
                ]

        CustomOrnament r ->
            Encode.object
                [ ( "type", Encode.string "custom" )
                , ( "name", Encode.string r.name )
                , ( "parameters"
                  , Encode.object
                        (List.map (\( k, v ) -> ( k, Encode.string v )) r.parameters)
                  )
                ]
