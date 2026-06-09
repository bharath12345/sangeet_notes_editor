module View.GridRenderer exposing (viewGridLine)

import Html exposing (Html, div, span, table, td, text, tr)
import Html.Attributes exposing (attribute, class, classList, style)
import Model.Composition exposing (Metadata)
import Model.Cursor exposing (CursorModel)
import Model.Event exposing (Event(..))
import Model.Layout exposing (BeatCell, GridLine)
import Model.Ornament
import Model.Taal exposing (VibhagMarker(..))
import Model.Types exposing (Stroke(..), SwarScript)
import View.Colors exposing (NotationColors)
import View.SwarGlyph as SwarGlyph


{-| Render a single GridLine as an HTML table with 5 notation rows:

1.  Taal markers (Sam, Taali, Khali)
2.  Ornament indicators
3.  Swar (main notation)
4.  Stroke (Da/Ra) -- only if showStrokeLine is true
5.  Sahitya (lyrics) -- only if showSahityaLine is true

-}
viewGridLine :
    NotationColors
    -> SwarScript
    -> Metadata
    -> CursorModel
    -> GridLine
    -> Html msg
viewGridLine colors script metadata cursor gridLine =
    let
        -- Build marker lookup: cellIndex -> VibhagMarker
        markerLookup =
            gridLine.markers
                |> List.map (\m -> ( m.cellIndex, m.marker ))

        lookupMarker idx =
            markerLookup
                |> List.filter (\( i, _ ) -> i == idx)
                |> List.head
                |> Maybe.map Tuple.second

        -- Check if a cell index is at a vibhag break
        isVibhagBreak idx =
            List.member idx gridLine.vibhagBreaks

        -- Check if cursor is in this cell
        isCursorAt cell =
            cell.cycle == cursor.cycle && cell.beat == cursor.beat
    in
    table [ class "grid-line" ]
        [ -- Row 1: Taal markers
          tr [ class "taal-marker-row" ]
            (List.indexedMap
                (\idx _ ->
                    td
                        [ classList
                            [ ( "beat-cell", True )
                            , ( "vibhag-break", isVibhagBreak idx )
                            ]
                        ]
                        [ viewMarker colors (lookupMarker idx) ]
                )
                gridLine.cells
            )
        , -- Row 2: Ornament indicators
          tr [ class "ornament-row" ]
            (List.indexedMap
                (\idx cell ->
                    td
                        [ classList
                            [ ( "beat-cell", True )
                            , ( "vibhag-break", isVibhagBreak idx )
                            ]
                        ]
                        [ viewOrnamentIndicators colors cell ]
                )
                gridLine.cells
            )
        , -- Row 3: Swar (main notation)
          tr [ class "swar-row" ]
            (List.indexedMap
                (\idx cell ->
                    td
                        [ classList
                            [ ( "beat-cell", True )
                            , ( "vibhag-break", isVibhagBreak idx )
                            , ( "cursor-cell", isCursorAt cell )
                            ]
                        , attribute "data-beat" (String.fromInt cell.beat)
                        , attribute "data-cycle" (String.fromInt cell.cycle)
                        ]
                        [ viewBeatEvents colors script cell ]
                )
                gridLine.cells
            )
        , -- Row 4: Strokes (if enabled)
          if metadata.showStrokeLine then
            tr [ class "stroke-row" ]
                (List.indexedMap
                    (\idx cell ->
                        td
                            [ classList
                                [ ( "beat-cell", True )
                                , ( "vibhag-break", isVibhagBreak idx )
                                ]
                            ]
                            [ viewStrokes colors cell ]
                    )
                    gridLine.cells
                )

          else
            text ""
        , -- Row 5: Sahitya (if enabled)
          if metadata.showSahityaLine then
            tr [ class "sahitya-row" ]
                (List.indexedMap
                    (\idx cell ->
                        td
                            [ classList
                                [ ( "beat-cell", True )
                                , ( "vibhag-break", isVibhagBreak idx )
                                ]
                            ]
                            [ viewSahitya colors cell ]
                    )
                    gridLine.cells
                )

          else
            text ""
        ]


{-| Render a taal marker.
-}
viewMarker : NotationColors -> Maybe VibhagMarker -> Html msg
viewMarker colors maybeMarker =
    case maybeMarker of
        Just Sam ->
            span [ class "marker marker-sam", style "color" colors.taalMarkerSam ]
                [ text "X" ]

        Just KhaliMarker ->
            span [ class "marker marker-khali", style "color" colors.taalMarker ]
                [ text "0" ]

        Just (TaaliMarker n) ->
            span [ class "marker marker-taali", style "color" colors.taalMarker ]
                [ text (String.fromInt n) ]

        Nothing ->
            span [ class "marker marker-empty" ] [ text "\u{00A0}" ]


{-| Render ornament indicators for a beat cell.
Shows abbreviated ornament labels above the notes.
-}
viewOrnamentIndicators : NotationColors -> BeatCell -> Html msg
viewOrnamentIndicators colors cell =
    let
        ornaments =
            cell.events
                |> List.concatMap eventOrnamentLabels

        ornamentText =
            if List.isEmpty ornaments then
                "\u{00A0}"

            else
                String.join " " ornaments
    in
    span [ class "ornament-indicator", style "color" colors.ornament ]
        [ text ornamentText ]


eventOrnamentLabels : Event -> List String
eventOrnamentLabels event =
    case event of
        SwarEvent r ->
            List.map ornamentLabel r.ornaments

        _ ->
            []


ornamentLabel : Model.Ornament.Ornament -> String
ornamentLabel orn =
    case orn of
        Model.Ornament.Meend _ ->
            "~"

        Model.Ornament.KanSwar _ ->
            "k"

        Model.Ornament.Murki _ ->
            "m"

        Model.Ornament.Gamak ->
            "G"

        Model.Ornament.Andolan ->
            "A"

        Model.Ornament.Krintan _ ->
            "Kr"

        Model.Ornament.Gitkari ->
            "Gi"

        Model.Ornament.Ghaseet _ ->
            "Gh"

        Model.Ornament.Sparsh _ ->
            "Sp"

        Model.Ornament.Zamzama _ ->
            "Z"

        Model.Ornament.CustomOrnament r ->
            r.name


{-| Render all events in a beat cell as swar glyphs.
-}
viewBeatEvents : NotationColors -> SwarScript -> BeatCell -> Html msg
viewBeatEvents colors script cell =
    if List.isEmpty cell.events then
        span [ class "empty-beat" ] [ text "\u{00A0}" ]

    else
        div [ class "beat-events" ]
            (List.map (viewEvent colors script) cell.events)


viewEvent : NotationColors -> SwarScript -> Event -> Html msg
viewEvent colors script event =
    case event of
        SwarEvent r ->
            SwarGlyph.drawSwar colors script r.note r.variant r.octave

        RestEvent _ ->
            SwarGlyph.drawRest colors

        SustainEvent _ ->
            SwarGlyph.drawSustain colors

        ChikariEvent _ ->
            span [ class "swar-text", style "color" colors.swar ] [ text "1" ]


{-| Render stroke indicators for a beat cell.
-}
viewStrokes : NotationColors -> BeatCell -> Html msg
viewStrokes colors cell =
    let
        strokeTexts =
            cell.events
                |> List.filterMap eventStrokeText
    in
    span [ class "stroke-indicator", style "color" colors.stroke ]
        [ text
            (if List.isEmpty strokeTexts then
                "\u{00A0}"

             else
                String.join " " strokeTexts
            )
        ]


eventStrokeText : Event -> Maybe String
eventStrokeText event =
    case event of
        SwarEvent r ->
            Maybe.map strokeToString r.stroke

        ChikariEvent _ ->
            Just "ची"

        _ ->
            Nothing


strokeToString : Stroke -> String
strokeToString s =
    case s of
        Da ->
            "Da"

        Ra ->
            "Ra"

        Jod ->
            "Jo"


{-| Render sahitya (lyrics) for a beat cell.
-}
viewSahitya : NotationColors -> BeatCell -> Html msg
viewSahitya colors cell =
    let
        sahityaTexts =
            cell.events
                |> List.filterMap eventSahitya
    in
    span [ class "sahitya-text", style "color" colors.sahitya ]
        [ text
            (if List.isEmpty sahityaTexts then
                "\u{00A0}"

             else
                String.join " " sahityaTexts
            )
        ]


eventSahitya : Event -> Maybe String
eventSahitya event =
    case event of
        SwarEvent r ->
            r.sahitya

        _ ->
            Nothing
