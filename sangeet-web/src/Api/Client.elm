module Api.Client exposing
    ( ApiResult(..)
    , ApiError
    , apiResultDecoder
    , postJson
    , getJson
    )

import Http
import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode exposing (Value)


-- API ERROR


type alias ApiError =
    { code : String
    , message : String
    }


apiErrorDecoder : Decoder ApiError
apiErrorDecoder =
    Decode.map2 ApiError
        (Decode.field "code" Decode.string)
        (Decode.field "message" Decode.string)


-- API RESULT


type ApiResult a
    = Success a
    | ApiFailure ApiError
    | HttpError Http.Error


{-| Decode the standard API envelope: { success: bool, data: ..., error: ... }
-}
apiResultDecoder : Decoder a -> Decoder (ApiResult a)
apiResultDecoder dataDecoder =
    Decode.field "success" Decode.bool
        |> Decode.andThen
            (\success ->
                if success then
                    Decode.field "data" dataDecoder
                        |> Decode.map Success

                else
                    Decode.field "error" apiErrorDecoder
                        |> Decode.map ApiFailure
            )


{-| Send a POST request with a JSON body, expecting a JSON envelope response.
-}
postJson :
    { url : String
    , body : Value
    , decoder : Decoder a
    , onResult : Result Http.Error (ApiResult a) -> msg
    }
    -> Cmd msg
postJson config =
    Http.post
        { url = config.url
        , body = Http.jsonBody config.body
        , expect =
            Http.expectJson config.onResult
                (apiResultDecoder config.decoder)
        }


{-| Send a GET request, expecting a JSON envelope response.
-}
getJson :
    { url : String
    , decoder : Decoder a
    , onResult : Result Http.Error (ApiResult a) -> msg
    }
    -> Cmd msg
getJson config =
    Http.get
        { url = config.url
        , expect =
            Http.expectJson config.onResult
                (apiResultDecoder config.decoder)
        }
