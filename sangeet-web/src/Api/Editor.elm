module Api.Editor exposing
    ( insertSwar
    , insertRest
    , insertSustain
    , deleteLast
    , insertDualSwar
    )

import Api.Client exposing (ApiResult)
import Http
import Json.Encode as Encode
import Model.Composition exposing (Composition, encodeComposition)
import Model.Cursor exposing (CursorModel, encodeCursor)
import Model.Layout exposing (EditorResult, editorResultDecoder)
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
