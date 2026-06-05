module Api.Composition exposing
    ( createComposition
    , parseComposition
    , serializeComposition
    )

import Api.Client exposing (ApiResult)
import Http
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Composition
    exposing
        ( Composition
        , CompositionType
        , compositionDecoder
        , encodeComposition
        , encodeCompositionType
        )
import Model.Raag exposing (Raag, encodeRaag)
import Model.Taal exposing (Taal, encodeTaal)
import Model.Types exposing (Laya, encodeLaya)


{-| Create a new composition with the given parameters.
-}
createComposition :
    String
    ->
        { title : String
        , compositionType : CompositionType
        , taal : Taal
        , raag : Raag
        , laya : Maybe Laya
        , taanCount : Int
        , showStrokeLine : Bool
        , showSahityaLine : Bool
        }
    -> (Result Http.Error (ApiResult Composition) -> msg)
    -> Cmd msg
createComposition baseUrl params onResult =
    let
        layaField =
            case params.laya of
                Just l ->
                    [ ( "laya", encodeLaya l ) ]

                Nothing ->
                    []

        body =
            Encode.object
                ([ ( "title", Encode.string params.title )
                 , ( "compositionType", encodeCompositionType params.compositionType )
                 , ( "taal", encodeTaal params.taal )
                 , ( "raag", encodeRaag params.raag )
                 , ( "taanCount", Encode.int params.taanCount )
                 , ( "showStrokeLine", Encode.bool params.showStrokeLine )
                 , ( "showSahityaLine", Encode.bool params.showSahityaLine )
                 ]
                    ++ layaField
                )
    in
    Api.Client.postJson
        { url = baseUrl ++ "/compositions"
        , body = body
        , decoder = compositionDecoder
        , onResult = onResult
        }


{-| Parse a .swar JSON string into a Composition.
-}
parseComposition :
    String
    -> String
    -> (Result Http.Error (ApiResult Composition) -> msg)
    -> Cmd msg
parseComposition baseUrl jsonString onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/compositions/parse"
        , body = Encode.object [ ( "json", Encode.string jsonString ) ]
        , decoder = compositionDecoder
        , onResult = onResult
        }


{-| Serialize a Composition to .swar JSON (returned as raw JSON value).
-}
serializeComposition :
    String
    -> Composition
    -> (Result Http.Error (ApiResult Decode.Value) -> msg)
    -> Cmd msg
serializeComposition baseUrl composition onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/compositions/serialize"
        , body = Encode.object [ ( "composition", encodeComposition composition ) ]
        , decoder = Decode.value
        , onResult = onResult
        }
