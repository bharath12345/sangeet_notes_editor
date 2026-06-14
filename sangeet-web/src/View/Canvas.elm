module View.Canvas exposing (view)

import Html exposing (Html, div, h2, h3, text)
import Html.Attributes exposing (class)
import Model.Composition exposing (Composition, Metadata, SectionType(..))
import Model.Cursor exposing (CursorModel)
import Model.Layout exposing (SectionGrid)
import Model.Types exposing (SwarScript)
import State.Msg exposing (Msg)
import View.Colors exposing (NotationColors)
import View.GridRenderer as GridRenderer


{-| Top-level notation view: composition title + grid sections.

Composition metadata chips (Raag/Taal/Laya/Type) and Arohan/Avrohan have moved
up to `View.Header` so the editor surface only shows one source of truth for
metadata. The canvas keeps the composition title as a heading for the rendered
content.

-}
view : NotationColors -> SwarScript -> Composition -> CursorModel -> Int -> List SectionGrid -> Html Msg
view colors script composition cursor currentSectionIndex grids =
    div [ class "notation-canvas" ]
        [ h2 [ class "composition-title" ] [ text composition.metadata.title ]
        , viewSections colors script composition cursor currentSectionIndex grids
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
