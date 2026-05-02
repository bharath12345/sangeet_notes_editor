module Api.Layout exposing (computeLayout)

import Api.Client exposing (ApiResult)
import Http
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Composition exposing (Composition, encodeComposition)
import Model.Layout exposing (LayoutConfig, SectionGrid, encodeLayoutConfig, sectionGridDecoder)


{-| Compute the grid layout for a composition.
-}
computeLayout :
    String
    -> Composition
    -> LayoutConfig
    -> (Result Http.Error (ApiResult (List SectionGrid)) -> msg)
    -> Cmd msg
computeLayout baseUrl composition config onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/layout/compute"
        , body =
            Encode.object
                [ ( "composition", encodeComposition composition )
                , ( "highDensityThreshold", Encode.int config.highDensityThreshold )
                , ( "cellWidthBase", Encode.float config.cellWidthBase )
                , ( "cellOverflowExpand", Encode.float config.cellOverflowExpand )
                , ( "lineSpacing", Encode.float config.lineSpacing )
                , ( "headerHeight", Encode.float config.headerHeight )
                ]
        , decoder = Decode.list sectionGridDecoder
        , onResult = onResult
        }
