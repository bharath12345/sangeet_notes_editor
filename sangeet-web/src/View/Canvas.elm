module View.Canvas exposing (view)

import Html exposing (Html, div, h3, text)
import Html.Attributes exposing (class)
import Html.Events exposing (onClick)
import Model.Composition exposing (Composition, SectionType(..))
import Model.Cursor exposing (CursorModel)
import Model.Layout exposing (SectionGrid)
import Model.Types exposing (SwarScript)
import State.Msg exposing (Msg(..))
import View.Colors exposing (NotationColors)
import View.GridRenderer as GridRenderer


{-| Top-level notation view: just the rendered section grids.

The composition title is shown by the tab bar (single source of truth) and the
metadata chips (Raag/Taal/Laya/Type + Arohan/Avrohan) live in `View.Header`.

-}
view : NotationColors -> SwarScript -> Composition -> CursorModel -> Int -> List SectionGrid -> Html Msg
view colors script composition cursor currentSectionIndex grids =
    div [ class "notation-canvas" ]
        [ viewSections colors script composition cursor currentSectionIndex grids
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
                viewSectionGrid colors script cursor idx (idx == currentSectionIndex) (sectionStartingBeat idx) grid
            )
            grids
        )


viewSectionGrid : NotationColors -> SwarScript -> CursorModel -> Int -> Bool -> Int -> SectionGrid -> Html Msg
viewSectionGrid colors script cursor sectionIndex isActive startingBeat grid =
    div
        [ class
            (if isActive then
                "section-grid section-grid-active"

             else
                "section-grid"
            )
        ]
        [ -- Clickable heading: dispatches SelectSection (same msg the
          -- toolbar tabs use) so users can switch sections without
          -- hunting for the toolbar tab. Matches plan-16 issue #10.
          h3
            [ class "section-title section-title-clickable"
            , onClick (SelectSection sectionIndex)
            ]
            [ text (sectionDisplayName grid) ]
        , div [ class "section-lines" ]
            (List.map
                (\line ->
                    GridRenderer.viewGridLine colors script cursor startingBeat line
                )
                grid.lines
            )
        ]


{-| Build the heading text. Drop the "(Type)" parenthetical when the
section name and its type stringify identically (e.g., "Sargam" /
SectionType.Sargam, "Antara" / SectionType.Antara). Otherwise the
parenthetical disambiguates user-renamed sections (e.g., "Vilambit Gat
(Sthayi)").
-}
sectionDisplayName : SectionGrid -> String
sectionDisplayName grid =
    let
        typeName =
            sectionTypeToString grid.sectionType
    in
    if String.toLower grid.sectionName == String.toLower typeName then
        grid.sectionName

    else
        grid.sectionName ++ " (" ++ typeName ++ ")"


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

        SargamSection ->
            "Sargam"

        CustomSectionType name ->
            name
