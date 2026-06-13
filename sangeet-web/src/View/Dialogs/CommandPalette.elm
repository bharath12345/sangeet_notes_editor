module View.Dialogs.CommandPalette exposing (view)

import Html exposing (Html, div, input, kbd, span, text)
import Html.Attributes exposing (autofocus, class, placeholder, type_, value)
import Html.Events exposing (onClick, onInput)
import State.AppAction as AppAction exposing (AppAction)
import State.Msg exposing (Msg(..))
import UiStrings


view : String -> Int -> Html Msg
view query selectedIndex =
    let
        results =
            AppAction.filter query AppAction.all
    in
    div [ class "modal-overlay palette-overlay" ]
        [ div [ class "modal-dialog modal-palette" ]
            [ input
                [ class "palette-search"
                , type_ "text"
                , placeholder UiStrings.dialogCommandPaletteSearchPlaceholderWeb
                , value query
                , onInput PaletteQueryChanged
                , autofocus True
                ]
                []
            , div [ class "palette-results" ]
                (if List.isEmpty results then
                    [ div [ class "palette-empty" ] [ text UiStrings.dialogCommandPaletteNoResults ] ]

                 else
                    List.indexedMap (viewRow selectedIndex) results
                )
            ]
        ]


viewRow : Int -> Int -> AppAction -> Html Msg
viewRow selectedIndex idx action =
    div
        [ class
            (if idx == selectedIndex then
                "palette-row palette-row-selected"

             else
                "palette-row"
            )
        , onClick (PaletteRunIndex idx)
        ]
        [ span [ class "palette-row-title" ] [ text action.title ]
        , span [ class "palette-row-group" ] [ text action.group ]
        , case action.shortcut of
            Just s ->
                kbd [ class "palette-row-shortcut" ] [ text s ]

            Nothing ->
                text ""
        ]
