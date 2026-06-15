module View.Dialogs.UnsavedChanges exposing (view)

import Html exposing (Html, button, p, text)
import Html.Attributes exposing (class)
import Html.Events exposing (onClick)
import State.Model exposing (FileTab)
import State.Msg exposing (Msg(..))
import UiStrings
import View.Dialogs.Frame as Frame


{-| 3-button modal that confirms tab close when there are unsaved changes.
The Save button becomes Save As… when the tab has no filePath yet — same UX as
desktop's UnsavedChangesDialog.
-}
view : FileTab -> Html Msg
view tab =
    let
        saveLabel =
            case tab.filePath of
                Just _ ->
                    UiStrings.dialogUnsavedChangesButtonSave

                Nothing ->
                    UiStrings.dialogUnsavedChangesButtonSaveAs
    in
    Frame.view
        { title = UiStrings.dialogUnsavedChangesTitle
        , variantClass = "modal-unsaved-changes"
        , body =
            [ p [ class "modal-message" ]
                [ text (UiStrings.dialogUnsavedChangesHeader tab.filename) ]
            , p [ class "modal-message-secondary" ]
                [ text UiStrings.dialogUnsavedChangesBody ]
            ]
        , footer =
            [ button [ class "btn btn-secondary", onClick UnsavedChangesCancel ]
                [ text UiStrings.dialogUnsavedChangesButtonCancel ]
            , button [ class "btn btn-secondary", onClick UnsavedChangesDiscard ]
                [ text UiStrings.dialogUnsavedChangesButtonDiscard ]
            , button [ class "btn btn-primary", onClick UnsavedChangesSave ]
                [ text saveLabel ]
            ]
        }
