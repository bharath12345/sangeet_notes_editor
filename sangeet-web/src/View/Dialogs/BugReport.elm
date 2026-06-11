module View.Dialogs.BugReport exposing (view)

import Html exposing (Html, button, div, h2, input, label, p, text, textarea)
import Html.Attributes exposing (class, disabled, for, id, placeholder, rows, type_, value)
import Html.Events exposing (onClick, onInput)
import State.Model exposing (BugReportForm)
import State.Msg exposing (Msg(..))


view : BugReportForm -> Html Msg
view form =
    let
        canSubmit : Bool
        canSubmit =
            not form.sending && String.trim form.description /= ""
    in
    div [ class "modal-overlay" ]
        [ div [ class "modal-dialog modal-bug-report" ]
            [ h2 [ class "modal-title" ] [ text "Report a bug" ]
            , div [ class "modal-body" ]
                [ div [ class "form-group" ]
                    [ label [ for "bug-description" ] [ text "What went wrong? What were you trying to do?" ]
                    , textarea
                        [ id "bug-description"
                        , class "form-input"
                        , rows 6
                        , placeholder "The more detail the better — keys pressed, expected vs actual, etc."
                        , value form.description
                        , onInput BugReportSetDescription
                        ]
                        []
                    ]
                , div [ class "form-group" ]
                    [ label [ for "bug-email" ] [ text "Email (optional, only if you want a reply)" ]
                    , input
                        [ id "bug-email"
                        , class "form-input"
                        , type_ "email"
                        , placeholder "you@example.com"
                        , value form.email
                        , onInput BugReportSetEmail
                        ]
                        []
                    ]
                , p [ class "bug-report-disclosure" ]
                    [ text
                        ("We'll include a short replay of your recent actions in the app "
                            ++ "(the last few minutes only) so the bug can be reproduced. "
                            ++ "Password fields are never captured. Nothing leaves your browser "
                            ++ "until you click Send below."
                        )
                    ]
                ]
            , div [ class "modal-footer" ]
                [ button
                    [ class "btn btn-secondary"
                    , disabled form.sending
                    , onClick BugReportCancel
                    ]
                    [ text "Cancel" ]
                , button
                    [ class "btn btn-primary"
                    , disabled (not canSubmit)
                    , onClick BugReportSubmit
                    ]
                    [ text
                        (if form.sending then
                            "Sending..."

                         else
                            "Send"
                        )
                    ]
                ]
            ]
        ]
