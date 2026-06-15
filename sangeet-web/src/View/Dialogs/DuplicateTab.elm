module View.Dialogs.DuplicateTab exposing (view)

import Html exposing (Html, button, p, text)
import Html.Attributes exposing (class)
import Html.Events exposing (onClick)
import State.Model exposing (PendingTabOpen)
import State.Msg exposing (Msg(..))
import UiStrings
import View.Dialogs.Frame as Frame


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
    Frame.view
        { title = UiStrings.dialogDuplicateTabTitle
        , variantClass = "modal-duplicate-tab"
        , body =
            [ p [ class "modal-message" ]
                [ text (UiStrings.dialogDuplicateTabHeader conflictingTitle) ]
            , p [ class "modal-message-secondary" ]
                [ text UiStrings.dialogDuplicateTabBody ]
            ]
        , footer =
            [ button [ class "btn btn-secondary", onClick DuplicateTabCancel ]
                [ text UiStrings.dialogDuplicateTabButtonCancel ]
            , button [ class "btn btn-secondary", onClick DuplicateTabOpenWithNewName ]
                [ text (UiStrings.dialogDuplicateTabButtonRename pending.proposedTitle) ]
            , button [ class "btn btn-primary", onClick DuplicateTabSwitch ]
                [ text UiStrings.dialogDuplicateTabButtonSwitch ]
            ]
        }
