module View.Dialogs.ClearSection exposing (view)

import Html exposing (Html, button, p, text)
import Html.Attributes exposing (class)
import Html.Events exposing (onClick)
import State.Msg exposing (Msg(..))
import UiStrings
import View.Dialogs.Frame as Frame


{-| Confirmation modal for clearing a section. Two buttons: [Clear] / [Cancel].
Mirrors desktop's clear-section confirmation dialog so cross-platform UX matches.
-}
view : String -> Html Msg
view sectionName =
    Frame.view
        { title = UiStrings.dialogClearSectionTitle
        , variantClass = "modal-clear-section"
        , body =
            [ p [ class "modal-message" ]
                [ text (UiStrings.dialogClearSectionBody sectionName) ]
            ]
        , footer =
            [ button [ class "btn btn-secondary", onClick CancelClearSection ]
                [ text UiStrings.dialogClearSectionCancel ]
            , button [ class "btn btn-danger", onClick ConfirmClearSection ]
                [ text UiStrings.dialogClearSectionConfirm ]
            ]
        }
