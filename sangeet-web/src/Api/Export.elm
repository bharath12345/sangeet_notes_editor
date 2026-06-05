module Api.Export exposing
    ( PdfExportRequest
    , exportHtml
    , exportPdf
    )

import Api.Client exposing (ApiResult)
import Http
import Json.Decode as Decode
import Json.Encode as Encode exposing (Value)
import Model.Composition exposing (Composition, encodeComposition)
import Model.Types exposing (SwarScript, encodeSwarScript)


{-| Request parameters for PDF export. The actual binary download
will be handled via ports (creating a Blob URL and triggering download
in JavaScript). This record is used to build the request body.
-}
type alias PdfExportRequest =
    { composition : Composition
    , script : SwarScript
    , landscape : Bool
    }


{-| Encode a PDF export request body for use in port-based download.
The PDF endpoint returns raw bytes (application/pdf), not a JSON envelope.
Use this to build the request body, then send it via a port to JavaScript
which can use fetch() and handle the binary response.
-}
exportPdf : PdfExportRequest -> Value
exportPdf req =
    Encode.object
        [ ( "composition", encodeComposition req.composition )
        , ( "script", encodeSwarScript req.script )
        , ( "landscape", Encode.bool req.landscape )
        ]


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
