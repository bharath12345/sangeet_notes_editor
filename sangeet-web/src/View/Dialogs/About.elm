module View.Dialogs.About exposing (view)

import Html exposing (Html, a, button, div, h2, h3, hr, li, p, span, text, ul)
import Html.Attributes exposing (class, href, target)
import Html.Events exposing (onClick)
import State.Msg exposing (Msg(..))


repoUrl : String
repoUrl =
    "https://github.com/bharath12345/sangeet_notes_editor"


releasesUrl : String
releasesUrl =
    repoUrl ++ "/releases"


userGuideUrl : String
userGuideUrl =
    repoUrl ++ "/tree/main/docs/user-guide"


hostingGuideUrl : String
hostingGuideUrl =
    repoUrl ++ "/blob/main/docs/hosting-gcp.md"


licenseUrl : String
licenseUrl =
    repoUrl ++ "/blob/main/LICENSE"


version : String
version =
    "1.0"


view : Html Msg
view =
    div [ class "modal-overlay" ]
        [ div [ class "modal-dialog modal-about" ]
            [ h2 [ class "modal-title" ] [ text "Sangeet Notes Editor" ]
            , div [ class "modal-body" ]
                [ p [ class "about-version" ]
                    [ text ("Version " ++ version ++ " ")
                    , span [ class "toolbar-badge beta-badge" ] [ text "BETA" ]
                    ]
                , p [ class "about-beta-note" ]
                    [ text "Beta release — actively iterating toward v1.0. Expect rough edges; please file bugs via the 🐞 Report bug button in the toolbar." ]
                , p []
                    [ text "A notation editor for Hindustani classical music in the Bhatkhande style. "
                    , text "Built for sitar compositions: gat, bandish, palta — with mizrab strokes, "
                    , text "meend, kan swar, gamak, and the full Bhatkhande notation set."
                    ]
                , p [] [ text "Supports Devanagari, Kannada, Telugu, and English scripts." ]

                -- Links section
                , hr [] []
                , h3 [ class "about-section-header" ] [ text "Links" ]
                , ul [ class "about-links" ]
                    [ li [] [ a [ href releasesUrl, target "_blank" ] [ text "Download desktop app" ] ]
                    , li [] [ a [ href userGuideUrl, target "_blank" ] [ text "User guide" ] ]
                    , li [] [ a [ href hostingGuideUrl, target "_blank" ] [ text "Self-hosting guide" ] ]
                    , li [] [ a [ href repoUrl, target "_blank" ] [ text "GitHub repository" ] ]
                    , li [] [ a [ href licenseUrl, target "_blank" ] [ text "MIT License" ] ]
                    ]

                -- Support section — full content lives in the dedicated Support dialog now
                , hr [] []
                , p [ class "about-tech" ]
                    [ text "💖 "
                    , a [ class "about-link-inline", href "#", onClick ShowSupportDialog ]
                        [ text "Support the project" ]
                    , text " — UPI / PayPal options."
                    ]

                -- Privacy section
                , hr [] []
                , h3 [ class "about-section-header" ] [ text "Privacy" ]
                , p []
                    [ text "While you use the app, anonymous usage events (clicks, keystrokes — never the text content of fields) are sent to PostHog so I can see which features people actually reach for. "
                    , text "If you click \"🐞 Report bug\", the last few minutes of your activity in this page are recorded as a video-like replay and sent along with your message so I can reproduce what you saw. "
                    , text "Password fields are never captured. Nothing leaves your browser unless you click Send. Reports auto-delete from storage after 90 days. "
                    , text "The desktop app sends a smaller, separate set of anonymous events to a different PostHog project for the same reason; users can opt out by setting SANGEET_ANALYTICS_DISABLED=1."
                    ]

                -- Footer
                , hr [] []
                , p [ class "about-tech" ]
                    [ text "Desktop: Scala 3 + ScalaFX. Web: Elm + Tapir." ]
                , p [ class "about-license" ]
                    [ span [] [ text "© 2026 Bharadwaj. " ]
                    , span [] [ text "Free and open source under the MIT License." ]
                    ]
                ]
            , div [ class "modal-footer" ]
                [ button [ class "btn btn-primary", onClick CloseAboutDialog ]
                    [ text "Close" ]
                ]
            ]
        ]
