module Api.Ornament exposing
    ( addKrintan
    , addMeend
    , addMurki
    , addSimple
    , addSingleNote
    , addZamzama
    )

import Api.Client exposing (ApiResult)
import Http
import Json.Encode as Encode
import Model.Composition exposing (Composition, encodeComposition)
import Model.Cursor exposing (CursorModel, encodeCursor)
import Model.Layout exposing (EditorResult, editorResultDecoder)
import Model.Types
    exposing
        ( MeendDirection
        , NoteRef
        , encodeMeendDirection
        , encodeNoteRef
        )


{-| Build the common editor input fields.
-}
editorInputFields : Composition -> Int -> CursorModel -> List ( String, Encode.Value )
editorInputFields composition sectionIndex cursor =
    [ ( "composition", encodeComposition composition )
    , ( "sectionIndex", Encode.int sectionIndex )
    , ( "cursor", encodeCursor cursor )
    ]


{-| Add a simple ornament (gamak, andolan, gitkari).
-}
addSimple :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> String
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
addSimple baseUrl composition sectionIndex cursor ornamentType onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/ornament/simple"
        , body =
            Encode.object
                (editorInputFields composition sectionIndex cursor
                    ++ [ ( "ornamentType", Encode.string ornamentType ) ]
                )
        , decoder = editorResultDecoder
        , onResult = onResult
        }


{-| Add a single-note ornament (kanSwar, sparsh, ghaseet).
-}
addSingleNote :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> String
    -> NoteRef
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
addSingleNote baseUrl composition sectionIndex cursor ornamentType noteRef onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/ornament/single-note"
        , body =
            Encode.object
                (editorInputFields composition sectionIndex cursor
                    ++ [ ( "ornamentType", Encode.string ornamentType )
                       , ( "noteRef", encodeNoteRef noteRef )
                       ]
                )
        , decoder = editorResultDecoder
        , onResult = onResult
        }


{-| Add a meend ornament.
-}
addMeend :
    String
    -> Composition
    -> Int
    -> CursorModel
    ->
        { startNote : NoteRef
        , endNote : NoteRef
        , direction : MeendDirection
        , intermediateNotes : List NoteRef
        }
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
addMeend baseUrl composition sectionIndex cursor params onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/ornament/meend"
        , body =
            Encode.object
                (editorInputFields composition sectionIndex cursor
                    ++ [ ( "startNote", encodeNoteRef params.startNote )
                       , ( "endNote", encodeNoteRef params.endNote )
                       , ( "direction", encodeMeendDirection params.direction )
                       , ( "intermediateNotes", Encode.list encodeNoteRef params.intermediateNotes )
                       ]
                )
        , decoder = editorResultDecoder
        , onResult = onResult
        }


{-| Add a krintan ornament.
-}
addKrintan :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> List NoteRef
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
addKrintan baseUrl composition sectionIndex cursor notes onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/ornament/krintan"
        , body =
            Encode.object
                (editorInputFields composition sectionIndex cursor
                    ++ [ ( "notes", Encode.list encodeNoteRef notes ) ]
                )
        , decoder = editorResultDecoder
        , onResult = onResult
        }


{-| Add a murki ornament.
-}
addMurki :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> List NoteRef
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
addMurki baseUrl composition sectionIndex cursor notes onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/ornament/murki"
        , body =
            Encode.object
                (editorInputFields composition sectionIndex cursor
                    ++ [ ( "notes", Encode.list encodeNoteRef notes ) ]
                )
        , decoder = editorResultDecoder
        , onResult = onResult
        }


{-| Add a zamzama ornament.
-}
addZamzama :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> List NoteRef
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
addZamzama baseUrl composition sectionIndex cursor notes onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/ornament/zamzama"
        , body =
            Encode.object
                (editorInputFields composition sectionIndex cursor
                    ++ [ ( "notes", Encode.list encodeNoteRef notes ) ]
                )
        , decoder = editorResultDecoder
        , onResult = onResult
        }
