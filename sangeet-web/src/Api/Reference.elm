module Api.Reference exposing
    ( fetchTaals
    , fetchRaags
    , fetchColors
    , fetchScripts
    , NotationColors
    , ScriptInfo
    )

import Api.Client exposing (ApiResult)
import Http
import Json.Decode as Decode exposing (Decoder)
import Model.Raag exposing (Raag, raagDecoder)
import Model.Taal exposing (Taal, taalDecoder)


-- NOTATION COLORS


type alias NotationColors =
    { taalMarker : String
    , taalMarkerSam : String
    , swar : String
    , octaveDot : String
    , ornament : String
    , stroke : String
    , sahitya : String
    , rest : String
    , sustain : String
    , komalMark : String
    , tivraMark : String
    }


notationColorsDecoder : Decoder NotationColors
notationColorsDecoder =
    Decode.succeed NotationColors
        |> andMap (Decode.field "taalMarker" Decode.string)
        |> andMap (Decode.field "taalMarkerSam" Decode.string)
        |> andMap (Decode.field "swar" Decode.string)
        |> andMap (Decode.field "octaveDot" Decode.string)
        |> andMap (Decode.field "ornament" Decode.string)
        |> andMap (Decode.field "stroke" Decode.string)
        |> andMap (Decode.field "sahitya" Decode.string)
        |> andMap (Decode.field "rest" Decode.string)
        |> andMap (Decode.field "sustain" Decode.string)
        |> andMap (Decode.field "komalMark" Decode.string)
        |> andMap (Decode.field "tivraMark" Decode.string)


-- SCRIPT INFO


type alias ScriptInfo =
    { displayName : String
    , fontName : String
    , notes : List ( String, String )
    }


scriptInfoDecoder : Decoder ScriptInfo
scriptInfoDecoder =
    Decode.map3 ScriptInfo
        (Decode.field "displayName" Decode.string)
        (Decode.field "fontName" Decode.string)
        (Decode.field "notes" (Decode.keyValuePairs Decode.string))


-- API FUNCTIONS


{-| Fetch all built-in taals as a name-keyed list.
-}
fetchTaals :
    String
    -> (Result Http.Error (ApiResult (List ( String, Taal ))) -> msg)
    -> Cmd msg
fetchTaals baseUrl onResult =
    Api.Client.getJson
        { url = baseUrl ++ "/taals"
        , decoder = Decode.keyValuePairs taalDecoder
        , onResult = onResult
        }


{-| Fetch all built-in raags as a name-keyed list.
-}
fetchRaags :
    String
    -> (Result Http.Error (ApiResult (List ( String, Raag ))) -> msg)
    -> Cmd msg
fetchRaags baseUrl onResult =
    Api.Client.getJson
        { url = baseUrl ++ "/raags"
        , decoder = Decode.keyValuePairs raagDecoder
        , onResult = onResult
        }


{-| Fetch the notation color palette.
-}
fetchColors :
    String
    -> (Result Http.Error (ApiResult NotationColors) -> msg)
    -> Cmd msg
fetchColors baseUrl onResult =
    Api.Client.getJson
        { url = baseUrl ++ "/rendering/colors"
        , decoder = notationColorsDecoder
        , onResult = onResult
        }


{-| Fetch available script mappings.
-}
fetchScripts :
    String
    -> (Result Http.Error (ApiResult (List ( String, ScriptInfo ))) -> msg)
    -> Cmd msg
fetchScripts baseUrl onResult =
    Api.Client.getJson
        { url = baseUrl ++ "/rendering/scripts"
        , decoder = Decode.keyValuePairs scriptInfoDecoder
        , onResult = onResult
        }



-- HELPERS


andMap : Decoder a -> Decoder (a -> b) -> Decoder b
andMap =
    Decode.map2 (|>)
