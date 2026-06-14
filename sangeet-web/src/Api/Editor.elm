module Api.Editor exposing
    ( changeStartingBeat
    , changeTaal
    , copySelection
    , cutSelection
    , deleteAtCursor
    , deleteLast
    , insertChikari
    , insertDualSwar
    , insertRest
    , insertSustain
    , insertSwar
    , insertSwarGroup
    , pasteClipboard
    )

import Api.Client exposing (ApiResult)
import Http
import Json.Encode as Encode
import Model.Composition exposing (Composition, encodeComposition)
import Model.Cursor exposing (CursorModel, encodeCursor)
import Model.Layout exposing (ClipboardResult, EditorResult, clipboardResultDecoder, editorResultDecoder)
import Model.Taal exposing (Taal, encodeTaal)
import Model.Types
    exposing
        ( Note
        , Octave
        , Variant
        , encodeNote
        , encodeOctave
        , encodeVariant
        )


{-| Build the common editor input fields: composition, sectionIndex, cursor.
-}
editorInputFields : Composition -> Int -> CursorModel -> List ( String, Encode.Value )
editorInputFields composition sectionIndex cursor =
    [ ( "composition", encodeComposition composition )
    , ( "sectionIndex", Encode.int sectionIndex )
    , ( "cursor", encodeCursor cursor )
    ]


{-| Insert a swar note at the cursor position.
-}
insertSwar :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> Note
    -> Variant
    -> Octave
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
insertSwar baseUrl composition sectionIndex cursor note variant octave onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/insert-swar"
        , body =
            Encode.object
                (editorInputFields composition sectionIndex cursor
                    ++ [ ( "note", encodeNote note )
                       , ( "variant", encodeVariant variant )
                       , ( "octave", encodeOctave octave )
                       ]
                )
        , decoder = editorResultDecoder
        , onResult = onResult
        }


{-| Insert a chikari (open strings) at the cursor position.
-}
insertChikari :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
insertChikari baseUrl composition sectionIndex cursor onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/insert-chikari"
        , body = Encode.object (editorInputFields composition sectionIndex cursor)
        , decoder = editorResultDecoder
        , onResult = onResult
        }


{-| Insert a rest at the cursor position.
-}
insertRest :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
insertRest baseUrl composition sectionIndex cursor onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/insert-rest"
        , body = Encode.object (editorInputFields composition sectionIndex cursor)
        , decoder = editorResultDecoder
        , onResult = onResult
        }


{-| Insert a sustain at the cursor position.
-}
insertSustain :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
insertSustain baseUrl composition sectionIndex cursor onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/insert-sustain"
        , body = Encode.object (editorInputFields composition sectionIndex cursor)
        , decoder = editorResultDecoder
        , onResult = onResult
        }


{-| Delete the last event in the current section.
-}
deleteLast :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
deleteLast baseUrl composition sectionIndex cursor onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/delete-last"
        , body = Encode.object (editorInputFields composition sectionIndex cursor)
        , decoder = editorResultDecoder
        , onResult = onResult
        }


{-| Insert a dual swar (two identical notes) at the cursor position.
-}
insertDualSwar :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> Note
    -> Variant
    -> Octave
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
insertDualSwar baseUrl composition sectionIndex cursor note variant octave onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/insert-dual-swar"
        , body =
            Encode.object
                (editorInputFields composition sectionIndex cursor
                    ++ [ ( "note", encodeNote note )
                       , ( "variant", encodeVariant variant )
                       , ( "octave", encodeOctave octave )
                       ]
                )
        , decoder = editorResultDecoder
        , onResult = onResult
        }


{-| Insert 2-4 notes on a single beat with equal subdivisions.
-}
insertSwarGroup :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> List { note : Note, variant : Variant, octave : Octave }
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
insertSwarGroup baseUrl composition sectionIndex cursor notes onResult =
    let
        encodeNoteEntry entry =
            Encode.object
                [ ( "note", encodeNote entry.note )
                , ( "variant", encodeVariant entry.variant )
                , ( "octave", encodeOctave entry.octave )
                ]
    in
    Api.Client.postJson
        { url = baseUrl ++ "/editor/insert-swar-group"
        , body =
            Encode.object
                (editorInputFields composition sectionIndex cursor
                    ++ [ ( "notes", Encode.list encodeNoteEntry notes ) ]
                )
        , decoder = editorResultDecoder
        , onResult = onResult
        }


{-| Delete events at cursor position with BACKSPACE semantics.
-}
deleteAtCursor :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
deleteAtCursor baseUrl composition sectionIndex cursor onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/delete-at-cursor"
        , body = Encode.object (editorInputFields composition sectionIndex cursor)
        , decoder = editorResultDecoder
        , onResult = onResult
        }


copySelection :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> (Result Http.Error (ApiResult ClipboardResult) -> msg)
    -> Cmd msg
copySelection baseUrl composition sectionIndex cursor onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/copy-selection"
        , body = Encode.object (editorInputFields composition sectionIndex cursor)
        , decoder = clipboardResultDecoder
        , onResult = onResult
        }


cutSelection :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> (Result Http.Error (ApiResult ClipboardResult) -> msg)
    -> Cmd msg
cutSelection baseUrl composition sectionIndex cursor onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/cut-selection"
        , body = Encode.object (editorInputFields composition sectionIndex cursor)
        , decoder = clipboardResultDecoder
        , onResult = onResult
        }


pasteClipboard :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> String
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
pasteClipboard baseUrl composition sectionIndex cursor clipboardJson onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/paste-clipboard"
        , body =
            Encode.object
                (editorInputFields composition sectionIndex cursor
                    ++ [ ( "clipboardJson", Encode.string clipboardJson ) ]
                )
        , decoder = editorResultDecoder
        , onResult = onResult
        }


changeStartingBeat :
    String
    -> Composition
    -> Int
    -> Int
    -> (Result Http.Error (ApiResult Composition) -> msg)
    -> Cmd msg
changeStartingBeat baseUrl composition sectionIndex startingBeat onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/change-starting-beat"
        , body =
            Encode.object
                [ ( "composition", encodeComposition composition )
                , ( "sectionIndex", Encode.int sectionIndex )
                , ( "startingBeat", Encode.int startingBeat )
                ]
        , decoder = Model.Composition.compositionDecoder
        , onResult = onResult
        }


{-| Change the composition's taal, re-mapping all event positions across
sections so events past the new taal's matras flow into subsequent
cycles. Mirrors desktop CompositionEditor.changeTaal. Returns the new
composition and a reset cursor (cycle 0, beat = startingBeat - 1).
-}
changeTaal :
    String
    -> Composition
    -> Int
    -> Taal
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
changeTaal baseUrl composition sectionIndex taal onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/change-taal"
        , body =
            Encode.object
                [ ( "composition", encodeComposition composition )
                , ( "sectionIndex", Encode.int sectionIndex )
                , ( "taal", encodeTaal taal )
                ]
        , decoder = editorResultDecoder
        , onResult = onResult
        }
