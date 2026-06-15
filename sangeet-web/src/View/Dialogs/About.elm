module View.Dialogs.About exposing (view)

import Html exposing (Html, a, button, div, h2, h3, hr, li, p, span, text, ul)
import Html.Attributes exposing (class, href, target)
import Html.Events exposing (onClick)
import State.Msg exposing (Msg(..))
import UiStrings


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
    repoUrl ++ "/blob/main/docs/developer/operations/hosting-gcp.md"


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
            [ h2 [ class "modal-title" ] [ text UiStrings.dialogAboutTitle ]
            , div [ class "modal-body" ]
                [ p [ class "about-version" ]
                    [ text (UiStrings.dialogAboutVersion version ++ " ")
                    , span [ class "toolbar-badge beta-badge" ] [ text UiStrings.toolbarBetaBadge ]
                    ]
                , p [ class "about-beta-note" ]
                    [ text UiStrings.dialogAboutBetaNote ]
                , p []
                    [ text UiStrings.dialogAboutDescriptionParagraph1 ]
                , p [] [ text UiStrings.dialogAboutDescriptionParagraph2 ]

                -- Links section
                , hr [] []
                , h3 [ class "about-section-header" ] [ text UiStrings.dialogAboutLinksHeader ]
                , ul [ class "about-links" ]
                    [ li [] [ a [ href releasesUrl, target "_blank" ] [ text UiStrings.dialogAboutLinksDownloadDesktop ] ]
                    , li [] [ a [ href userGuideUrl, target "_blank" ] [ text UiStrings.dialogAboutLinksUserGuide ] ]
                    , li [] [ a [ href hostingGuideUrl, target "_blank" ] [ text UiStrings.dialogAboutLinksSelfHosting ] ]
                    , li [] [ a [ href repoUrl, target "_blank" ] [ text UiStrings.dialogAboutLinksGithub ] ]
                    , li [] [ a [ href licenseUrl, target "_blank" ] [ text UiStrings.dialogAboutLinksLicense ] ]
                    ]

                -- Support section — full content lives in the dedicated Support dialog now
                , hr [] []
                , p [ class "about-tech" ]
                    [ text UiStrings.dialogAboutSupportText
                    , a [ class "about-link-inline", href "#", onClick ShowSupportDialog ]
                        [ text UiStrings.dialogAboutSupportLink ]
                    , text UiStrings.dialogAboutSupportSuffix
                    ]

                -- Privacy section
                , hr [] []
                , h3 [ class "about-section-header" ] [ text UiStrings.dialogAboutPrivacyHeader ]
                , p []
                    [ text UiStrings.dialogAboutPrivacyText ]

                -- Footer
                , hr [] []
                , p [ class "about-tech" ]
                    [ text UiStrings.dialogAboutTech ]
                , p [ class "about-license" ]
                    [ span [] [ text UiStrings.dialogAboutCopyright ]
                    , span [] [ text UiStrings.dialogAboutLicense ]
                    ]
                ]
            , div [ class "modal-footer" ]
                [ button [ class "btn btn-primary", onClick CloseAboutDialog ]
                    [ text UiStrings.dialogAboutClose ]
                ]
            ]
        ]
