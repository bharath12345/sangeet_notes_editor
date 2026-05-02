module Api.Cursor exposing
    ( nextBeat
    , prevBeat
    , nextSubBeat
    , setSubdivisions
    , setOctave
    , moveTo
    )

import Api.Client exposing (ApiResult)
import Http
import Json.Encode as Encode
import Model.Cursor exposing (CursorModel, cursorDecoder, encodeCursor)
import Model.Types exposing (Octave, encodeOctave)


{-| Move cursor to next beat.
-}
nextBeat :
    String
    -> CursorModel
    -> (Result Http.Error (ApiResult CursorModel) -> msg)
    -> Cmd msg
nextBeat baseUrl cursor onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/cursor/next-beat"
        , body = Encode.object [ ( "cursor", encodeCursor cursor ) ]
        , decoder = cursorDecoder
        , onResult = onResult
        }


{-| Move cursor to previous beat.
-}
prevBeat :
    String
    -> CursorModel
    -> (Result Http.Error (ApiResult CursorModel) -> msg)
    -> Cmd msg
prevBeat baseUrl cursor onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/cursor/prev-beat"
        , body = Encode.object [ ( "cursor", encodeCursor cursor ) ]
        , decoder = cursorDecoder
        , onResult = onResult
        }


{-| Move cursor to next sub-beat.
-}
nextSubBeat :
    String
    -> CursorModel
    -> (Result Http.Error (ApiResult CursorModel) -> msg)
    -> Cmd msg
nextSubBeat baseUrl cursor onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/cursor/next-sub-beat"
        , body = Encode.object [ ( "cursor", encodeCursor cursor ) ]
        , decoder = cursorDecoder
        , onResult = onResult
        }


{-| Set the number of beat subdivisions.
-}
setSubdivisions :
    String
    -> CursorModel
    -> Int
    -> (Result Http.Error (ApiResult CursorModel) -> msg)
    -> Cmd msg
setSubdivisions baseUrl cursor n onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/cursor/set-subdivisions"
        , body =
            Encode.object
                [ ( "cursor", encodeCursor cursor )
                , ( "subdivisions", Encode.int n )
                ]
        , decoder = cursorDecoder
        , onResult = onResult
        }


{-| Set the current octave for input.
-}
setOctave :
    String
    -> CursorModel
    -> Octave
    -> (Result Http.Error (ApiResult CursorModel) -> msg)
    -> Cmd msg
setOctave baseUrl cursor octave onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/cursor/set-octave"
        , body =
            Encode.object
                [ ( "cursor", encodeCursor cursor )
                , ( "octave", encodeOctave octave )
                ]
        , decoder = cursorDecoder
        , onResult = onResult
        }


{-| Move cursor to a specific cycle and beat.
-}
moveTo :
    String
    -> CursorModel
    -> Int
    -> Int
    -> (Result Http.Error (ApiResult CursorModel) -> msg)
    -> Cmd msg
moveTo baseUrl cursor cycle beat onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/cursor/move-to"
        , body =
            Encode.object
                [ ( "cursor", encodeCursor cursor )
                , ( "cycle", Encode.int cycle )
                , ( "beat", Encode.int beat )
                ]
        , decoder = cursorDecoder
        , onResult = onResult
        }
