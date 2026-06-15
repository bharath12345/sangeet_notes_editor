module View.Dialogs.BugReport exposing (view)

import Html exposing (Html, button, div, input, label, p, text, textarea)
import Html.Attributes exposing (class, disabled, for, id, placeholder, rows, type_, value)
import Html.Events exposing (onClick, onInput)
import State.Model exposing (BugReportForm)
import State.Msg exposing (Msg(..))
import UiStrings
import View.Dialogs.Frame as Frame


view : BugReportForm -> Html Msg
view form =
    let
        canSubmit : Bool
        canSubmit =
            not form.sending && String.trim form.description /= ""
    in
    Frame.view
        { title = UiStrings.dialogBugReportTitle
        , variantClass = "modal-bug-report"
        , body =
            [ div [ class "form-group" ]
                [ label [ for "bug-description" ] [ text UiStrings.dialogBugReportDescriptionLabel ]
                , textarea
                    [ id "bug-description"
                    , class "form-input"
                    , rows 6
                    , placeholder UiStrings.dialogBugReportDescriptionPlaceholder
                    , value form.description
                    , onInput BugReportSetDescription
                    ]
                    []
                ]
            , div [ class "form-group" ]
                [ label [ for "bug-email" ] [ text UiStrings.dialogBugReportEmailLabel ]
                , input
                    [ id "bug-email"
                    , class "form-input"
                    , type_ "email"
                    , placeholder UiStrings.dialogBugReportEmailPlaceholder
                    , value form.email
                    , onInput BugReportSetEmail
                    ]
                    []
                ]
            , p [ class "bug-report-disclosure" ]
                [ text UiStrings.dialogBugReportDisclosureWeb ]
            ]
        , footer =
            [ button
                [ class "btn btn-secondary"
                , disabled form.sending
                , onClick BugReportCancel
                ]
                [ text UiStrings.dialogBugReportButtonCancel ]
            , button
                [ class "btn btn-primary"
                , disabled (not canSubmit)
                , onClick BugReportSubmit
                ]
                [ text
                    (if form.sending then
                        UiStrings.dialogBugReportButtonSending

                     else
                        UiStrings.dialogBugReportButtonSend
                    )
                ]
            ]
        }
