module View.Canvas exposing (view)

import Html exposing (Html, div, h2, h3, span, text)
import Html.Attributes exposing (class)
import Model.Composition exposing (Composition, CompositionType(..), Metadata, SectionType(..))
import Model.Cursor exposing (CursorModel)
import Model.Layout exposing (SectionGrid)
import Model.Raag exposing (Raag)
import Model.Types exposing (Laya(..), SwarScript)
import State.Msg exposing (Msg)
import UiStrings
import View.Colors exposing (NotationColors)
import View.GridRenderer as GridRenderer


{-| Top-level notation view: composition header + grid sections.
-}
view : NotationColors -> SwarScript -> Composition -> CursorModel -> Int -> List SectionGrid -> Html Msg
view colors script composition cursor currentSectionIndex grids =
    div [ class "notation-canvas" ]
        [ viewHeader composition.metadata
        , viewSections colors script composition cursor currentSectionIndex grids
        ]


{-| Render the composition header: title, raag info, taal info, laya.
-}
viewHeader : Metadata -> Html Msg
viewHeader metadata =
    div [ class "composition-header" ]
        [ h2 [ class "composition-title" ] [ text metadata.title ]
        , div [ class "composition-meta" ]
            [ viewRaagChip metadata.raag
            , viewTaalChip metadata
            , viewLayaChip metadata.laya
            , viewTypeChip metadata.compositionType
            ]
        , viewArohanAvrohan metadata.raag
        ]


viewRaagChip : Raag -> Html Msg
viewRaagChip raag =
    span [ class "meta-chip raag-chip" ]
        [ span [ class "chip-label" ] [ text UiStrings.headerRaagLabel ]
        , span [ class "chip-value" ] [ text raag.name ]
        ]


viewTaalChip : Metadata -> Html Msg
viewTaalChip metadata =
    span [ class "meta-chip taal-chip" ]
        [ span [ class "chip-label" ] [ text UiStrings.headerTaalLabel ]
        , span [ class "chip-value" ]
            [ text (metadata.taal.name ++ " (" ++ String.fromInt metadata.taal.matras ++ ")") ]
        ]


viewLayaChip : Maybe Laya -> Html Msg
viewLayaChip maybeLaya =
    case maybeLaya of
        Just laya ->
            span [ class "meta-chip laya-chip" ]
                [ span [ class "chip-label" ] [ text UiStrings.headerLayaLabel ]
                , span [ class "chip-value" ] [ text (layaToString laya) ]
                ]

        Nothing ->
            text ""


viewTypeChip : CompositionType -> Html Msg
viewTypeChip compType =
    span [ class "meta-chip type-chip" ]
        [ span [ class "chip-label" ] [ text "Type" ]
        , span [ class "chip-value" ] [ text (compositionTypeToString compType) ]
        ]


viewArohanAvrohan : Raag -> Html Msg
viewArohanAvrohan raag =
    let
        arohan =
            raag.arohana
                |> Maybe.map (\notes -> UiStrings.headerArohanLabel ++ ": " ++ String.join " " notes)
                |> Maybe.withDefault ""

        avrohan =
            raag.avarohana
                |> Maybe.map (\notes -> UiStrings.headerAvrohanLabel ++ ": " ++ String.join " " notes)
                |> Maybe.withDefault ""
    in
    if arohan == "" && avrohan == "" then
        text ""

    else
        div [ class "arohan-avrohan" ]
            [ if arohan /= "" then
                span [ class "arohan" ] [ text arohan ]

              else
                text ""
            , if avrohan /= "" then
                span [ class "avrohan" ] [ text avrohan ]

              else
                text ""
            ]


{-| Render all section grids.
-}
viewSections : NotationColors -> SwarScript -> Composition -> CursorModel -> Int -> List SectionGrid -> Html Msg
viewSections colors script composition cursor currentSectionIndex grids =
    let
        sectionStartingBeat idx =
            composition.sections
                |> List.drop idx
                |> List.head
                |> Maybe.map .startingBeat
                |> Maybe.withDefault 1
    in
    div [ class "notation-sections" ]
        (List.indexedMap
            (\idx grid ->
                viewSectionGrid colors script composition.metadata cursor (idx == currentSectionIndex) (sectionStartingBeat idx) grid
            )
            grids
        )


viewSectionGrid : NotationColors -> SwarScript -> Metadata -> CursorModel -> Bool -> Int -> SectionGrid -> Html Msg
viewSectionGrid colors script metadata cursor isActive startingBeat grid =
    div
        [ class
            (if isActive then
                "section-grid section-grid-active"

             else
                "section-grid"
            )
        ]
        [ h3 [ class "section-title" ]
            [ text (grid.sectionName ++ " (" ++ sectionTypeToString grid.sectionType ++ ")") ]
        , div [ class "section-lines" ]
            (List.map
                (\line ->
                    GridRenderer.viewGridLine colors script metadata cursor startingBeat line
                )
                grid.lines
            )
        ]


layaToString : Laya -> String
layaToString laya =
    case laya of
        AtiVilambit ->
            "Ati-vilambit"

        Vilambit ->
            "Vilambit"

        MadhyaLaya ->
            "Madhya"

        Drut ->
            "Drut"

        AtiDrut ->
            "Ati-drut"


compositionTypeToString : CompositionType -> String
compositionTypeToString ct =
    case ct of
        Bandish ->
            "Bandish"

        Gat ->
            "Gat"

        Palta ->
            "Palta"

        Sargam ->
            "Sargam"

        CustomCompositionType name ->
            name


sectionTypeToString : SectionType -> String
sectionTypeToString st =
    case st of
        Sthayi ->
            "Sthayi"

        Antara ->
            "Antara"

        Sanchari ->
            "Sanchari"

        Abhog ->
            "Abhog"

        Taan ->
            "Taan"

        Toda ->
            "Toda"

        Jhala ->
            "Jhala"

        PaltaSection ->
            "Palta"

        Arohi ->
            "Arohi"

        Avarohi ->
            "Avarohi"

        CustomSectionType name ->
            name
