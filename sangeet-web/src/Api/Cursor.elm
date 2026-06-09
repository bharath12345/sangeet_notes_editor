module Api.Cursor exposing
    ( moveTo
    , nextBeat
    , nextSubBeat
    , prevBeat
    , setOctave
    , setSubdivisions
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
    -> Int
    -> (Result Http.Error (ApiResult CursorModel) -> msg)
    -> Cmd msg
nextBeat baseUrl cursor startingBeat onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/cursor/next-beat"
        , body =
            Encode.object
                [ ( "cursor", encodeCursor cursor )
                , ( "startingBeat", Encode.int startingBeat )
                ]
        , decoder = cursorDecoder
        , onResult = onResult
        }


{-| Move cursor to previous beat.
-}
prevBeat :
    String
    -> CursorModel
    -> Int
    -> (Result Http.Error (ApiResult CursorModel) -> msg)
    -> Cmd msg
prevBeat baseUrl cursor startingBeat onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/cursor/prev-beat"
        , body =
            Encode.object
                [ ( "cursor", encodeCursor cursor )
                , ( "startingBeat", Encode.int startingBeat )
                ]
        , decoder = cursorDecoder
        , onResult = onResult
        }


{-| Move cursor to next sub-beat.
-}
nextSubBeat :
    String
    -> CursorModel
    -> Int
    -> (Result Http.Error (ApiResult CursorModel) -> msg)
    -> Cmd msg
nextSubBeat baseUrl cursor startingBeat onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/cursor/next-sub-beat"
        , body =
            Encode.object
                [ ( "cursor", encodeCursor cursor )
                , ( "startingBeat", Encode.int startingBeat )
                ]
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
    -> Int
    -> (Result Http.Error (ApiResult CursorModel) -> msg)
    -> Cmd msg
moveTo baseUrl cursor cycle beat startingBeat onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/cursor/move-to"
        , body =
            Encode.object
                [ ( "cursor", encodeCursor cursor )
                , ( "cycle", Encode.int cycle )
                , ( "beat", Encode.int beat )
                , ( "startingBeat", Encode.int startingBeat )
                ]
        , decoder = cursorDecoder
        , onResult = onResult
        }
