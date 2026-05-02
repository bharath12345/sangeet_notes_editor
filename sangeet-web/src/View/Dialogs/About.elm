module View.Dialogs.About exposing (view)

import Html exposing (Html, a, button, div, h2, p, text)
import Html.Attributes exposing (class, href, target)
import Html.Events exposing (onClick)
import State.Msg exposing (Msg(..))


{-| Simple about dialog with application information.
-}
view : Html Msg
view =
    div [ class "modal-overlay" ]
        [ div [ class "modal-dialog modal-about" ]
            [ h2 [ class "modal-title" ] [ text "Sangeet Notes Editor" ]
            , div [ class "modal-body" ]
                [ p [] [ text "A notation editor for Hindustani classical music in the Bhatkhande style." ]
                , p [] [ text "Built for sitar compositions: gat, antara, taan, toda, palta, and more." ]
                , p []
                    [ text "Supports Devanagari, Kannada, Telugu, and English scripts." ]
                , p [ class "about-tech" ]
                    [ text "Desktop: Scala 3 + ScalaFX | Web: Elm + Scala HTTP server" ]
                , p []
                    [ a [ href "https://github.com/bharadwaj/sangeet_notes_editor", target "_blank" ]
                        [ text "Source Code" ]
                    ]
                ]
            , div [ class "modal-footer" ]
                [ button [ class "btn btn-primary", onClick CloseAboutDialog ]
                    [ text "Close" ]
                ]
            ]
        ]
