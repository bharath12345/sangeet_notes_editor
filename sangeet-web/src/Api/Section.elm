module Api.Section exposing
    ( addSection
    , removeSection
    , renameSection
    , reorderSections
    , RemoveSectionResult
    , ReorderSectionResult
    )

import Api.Client exposing (ApiResult)
import Http
import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode
import Model.Composition
    exposing
        ( Composition
        , SectionType
        , compositionDecoder
        , encodeComposition
        , encodeSectionType
        )


-- RESULT TYPES


type alias RemoveSectionResult =
    { composition : Composition
    , currentSectionIndex : Int
    }


removeSectionResultDecoder : Decoder RemoveSectionResult
removeSectionResultDecoder =
    Decode.map2 RemoveSectionResult
        (Decode.field "composition" compositionDecoder)
        (Decode.field "currentSectionIndex" Decode.int)


type alias ReorderSectionResult =
    { composition : Composition
    , currentSectionIndex : Int
    }


reorderSectionResultDecoder : Decoder ReorderSectionResult
reorderSectionResultDecoder =
    Decode.map2 ReorderSectionResult
        (Decode.field "composition" compositionDecoder)
        (Decode.field "currentSectionIndex" Decode.int)


-- API FUNCTIONS


{-| Add a new section to the composition.
-}
addSection :
    String
    -> Composition
    -> String
    -> SectionType
    -> (Result Http.Error (ApiResult Composition) -> msg)
    -> Cmd msg
addSection baseUrl composition name sectionType onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/sections/add"
        , body =
            Encode.object
                [ ( "composition", encodeComposition composition )
                , ( "name", Encode.string name )
                , ( "sectionType", encodeSectionType sectionType )
                ]
        , decoder = compositionDecoder
        , onResult = onResult
        }


{-| Remove a section by index.
-}
removeSection :
    String
    -> Composition
    -> Int
    -> Int
    -> (Result Http.Error (ApiResult RemoveSectionResult) -> msg)
    -> Cmd msg
removeSection baseUrl composition currentSectionIndex indexToRemove onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/sections/remove"
        , body =
            Encode.object
                [ ( "composition", encodeComposition composition )
                , ( "currentSectionIndex", Encode.int currentSectionIndex )
                , ( "indexToRemove", Encode.int indexToRemove )
                ]
        , decoder = removeSectionResultDecoder
        , onResult = onResult
        }


{-| Rename a section by index.
-}
renameSection :
    String
    -> Composition
    -> Int
    -> String
    -> (Result Http.Error (ApiResult Composition) -> msg)
    -> Cmd msg
renameSection baseUrl composition index newName onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/sections/rename"
        , body =
            Encode.object
                [ ( "composition", encodeComposition composition )
                , ( "index", Encode.int index )
                , ( "newName", Encode.string newName )
                ]
        , decoder = compositionDecoder
        , onResult = onResult
        }


{-| Move a section from one index to another.
-}
reorderSections :
    String
    -> Composition
    -> Int
    -> Int
    -> Int
    -> (Result Http.Error (ApiResult ReorderSectionResult) -> msg)
    -> Cmd msg
reorderSections baseUrl composition currentSectionIndex from to onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/sections/reorder"
        , body =
            Encode.object
                [ ( "composition", encodeComposition composition )
                , ( "currentSectionIndex", Encode.int currentSectionIndex )
                , ( "from", Encode.int from )
                , ( "to", Encode.int to )
                ]
        , decoder = reorderSectionResultDecoder
        , onResult = onResult
        }
