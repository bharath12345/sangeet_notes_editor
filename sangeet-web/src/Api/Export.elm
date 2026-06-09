module Api.Export exposing (exportHtml)

import Api.Client exposing (ApiResult)
import Http
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Composition exposing (Composition, encodeComposition)
import Model.Types exposing (SwarScript, encodeSwarScript)


{-| Export composition to HTML string.
-}
exportHtml :
    String
    -> Composition
    -> SwarScript
    -> (Result Http.Error (ApiResult String) -> msg)
    -> Cmd msg
exportHtml baseUrl composition script onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/export/html"
        , body =
            Encode.object
                [ ( "composition", encodeComposition composition )
                , ( "script", encodeSwarScript script )
                ]
        , decoder = Decode.string
        , onResult = onResult
        }
