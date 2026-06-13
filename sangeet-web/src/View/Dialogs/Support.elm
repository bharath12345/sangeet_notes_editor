module View.Dialogs.Support exposing (view)

import Html exposing (Html, a, button, div, h2, h3, hr, img, p, span, text)
import Html.Attributes exposing (alt, class, href, src, target)
import Html.Events exposing (onClick)
import State.Msg exposing (Msg(..))
import UiStrings


{-| Donation / support modal. Lifted out of About so it has parity with the
desktop SupportDialog (sangeet-desktop/.../dialog/SupportDialog.scala). The
About dialog keeps a one-line pointer to this dialog.
-}
view : Html Msg
view =
    div [ class "modal-overlay" ]
        [ div [ class "modal-dialog modal-support" ]
            [ h2 [ class "modal-title" ] [ text UiStrings.dialogSupportTitle ]
            , div [ class "modal-body" ]
                [ p []
                    [ text UiStrings.dialogSupportIntro ]
                , hr [] []
                , h3 [ class "about-section-header" ] [ text UiStrings.dialogSupportUpiHeader ]
                , p [ class "about-support-row" ]
                    [ span [ class "about-support-label" ] [ text UiStrings.dialogSupportUpiHandleLabel ]
                    , span [ class "about-upi-handle" ] [ text UiStrings.dialogSupportUpiHandle ]
                    ]
                , img [ src "images/upi-qr.png", alt UiStrings.dialogSupportUpiQrAlt, class "about-upi-qr" ] []
                , hr [] []
                , h3 [ class "about-section-header" ] [ text UiStrings.dialogSupportInternationalHeader ]
                , p [ class "about-support-row" ]
                    [ a [ href paypalUrl, target "_blank" ]
                        [ text UiStrings.dialogSupportInternationalPaypalLink ]
                    ]
                , p [ class "about-tech" ]
                    [ text UiStrings.dialogSupportThankYou ]
                ]
            , div [ class "modal-footer" ]
                [ button [ class "btn btn-primary", onClick CloseSupportDialog ]
                    [ text UiStrings.dialogSupportClose ]
                ]
            ]
        ]


upiHandle : String
upiHandle =
    "bharath12345-1@oksbi"


paypalUrl : String
paypalUrl =
    "https://www.paypal.com/ncp/payment/4NZ6FZZFVQMR6"
