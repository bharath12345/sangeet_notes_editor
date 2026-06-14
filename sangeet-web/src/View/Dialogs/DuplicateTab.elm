module View.Dialogs.DuplicateTab exposing (view)

import Html exposing (Html, button, div, h2, p, text)
import Html.Attributes exposing (class)
import Html.Events exposing (onClick)
import State.Model exposing (PendingTabOpen)
import State.Msg exposing (Msg(..))
import UiStrings


{-| Modal that asks the user how to handle a tab-title collision. Three buttons:
[Switch to it] / [Open as "abc (2)"] / [Cancel].
Mirrors the desktop DuplicateTabDialog so the cross-platform UX matches.
-}
view : PendingTabOpen -> Html Msg
view pending =
    let
        conflictingTitle =
            pending.composition.metadata.title
    in
    div [ class "modal-overlay" ]
        [ div [ class "modal-dialog modal-duplicate-tab" ]
            [ h2 [ class "modal-title" ] [ text UiStrings.dialogDuplicateTabTitle ]
            , div [ class "modal-body" ]
                [ p [ class "modal-message" ]
                    [ text (UiStrings.dialogDuplicateTabHeader conflictingTitle) ]
                , p [ class "modal-message-secondary" ]
                    [ text UiStrings.dialogDuplicateTabBody ]
                ]
            , div [ class "modal-footer" ]
                [ button [ class "btn btn-secondary", onClick DuplicateTabCancel ]
                    [ text UiStrings.dialogDuplicateTabButtonCancel ]
                , button [ class "btn btn-secondary", onClick DuplicateTabOpenWithNewName ]
                    [ text (UiStrings.dialogDuplicateTabButtonRename pending.proposedTitle) ]
                , button [ class "btn btn-primary", onClick DuplicateTabSwitch ]
                    [ text UiStrings.dialogDuplicateTabButtonSwitch ]
                ]
            ]
        ]
