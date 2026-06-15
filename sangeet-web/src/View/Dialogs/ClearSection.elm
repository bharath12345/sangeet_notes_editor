module View.Dialogs.ClearSection exposing (view)

import Html exposing (Html, button, div, h2, p, text)
import Html.Attributes exposing (class)
import Html.Events exposing (onClick)
import State.Msg exposing (Msg(..))
import UiStrings


{-| Confirmation modal for clearing a section. Two buttons: [Clear] / [Cancel].
Mirrors desktop's clear-section confirmation dialog so cross-platform UX matches.
-}
view : String -> Html Msg
view sectionName =
    div [ class "modal-overlay" ]
        [ div [ class "modal-dialog modal-clear-section" ]
            [ h2 [ class "modal-title" ] [ text UiStrings.dialogClearSectionTitle ]
            , div [ class "modal-body" ]
                [ p [ class "modal-message" ]
                    [ text (UiStrings.dialogClearSectionBody sectionName) ]
                ]
            , div [ class "modal-footer" ]
                [ button [ class "btn btn-secondary", onClick CancelClearSection ]
                    [ text UiStrings.dialogClearSectionCancel ]
                , button [ class "btn btn-danger", onClick ConfirmClearSection ]
                    [ text UiStrings.dialogClearSectionConfirm ]
                ]
            ]
        ]
