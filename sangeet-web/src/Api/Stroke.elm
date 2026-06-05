module Api.Stroke exposing
    ( clearStroke
    , setStroke
    )

import Api.Client exposing (ApiResult)
import Http
import Json.Encode as Encode
import Model.Composition exposing (Composition, encodeComposition)
import Model.Cursor exposing (CursorModel, encodeCursor)
import Model.Layout exposing (EditorResult, editorResultDecoder)
import Model.Types exposing (Stroke, encodeStroke)


{-| Build the common editor input fields.
-}
editorInputFields : Composition -> Int -> CursorModel -> List ( String, Encode.Value )
editorInputFields composition sectionIndex cursor =
    [ ( "composition", encodeComposition composition )
    , ( "sectionIndex", Encode.int sectionIndex )
    , ( "cursor", encodeCursor cursor )
    ]


{-| Set a stroke on the swar at the cursor position.
-}
setStroke :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> Stroke
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
setStroke baseUrl composition sectionIndex cursor stroke onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/stroke/set"
        , body =
            Encode.object
                (editorInputFields composition sectionIndex cursor
                    ++ [ ( "stroke", encodeStroke stroke ) ]
                )
        , decoder = editorResultDecoder
        , onResult = onResult
        }


{-| Clear the stroke at the cursor position.
-}
clearStroke :
    String
    -> Composition
    -> Int
    -> CursorModel
    -> (Result Http.Error (ApiResult EditorResult) -> msg)
    -> Cmd msg
clearStroke baseUrl composition sectionIndex cursor onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/editor/stroke/clear"
        , body = Encode.object (editorInputFields composition sectionIndex cursor)
        , decoder = editorResultDecoder
        , onResult = onResult
        }
