module View.Dialogs.Support exposing (view)

import Html exposing (Html, a, button, div, h2, h3, hr, img, p, span, text)
import Html.Attributes exposing (alt, class, href, src, target)
import Html.Events exposing (onClick)
import State.Msg exposing (Msg(..))


{-| Donation / support modal. Lifted out of About so it has parity with the
desktop SupportDialog (sangeet-desktop/.../dialog/SupportDialog.scala). The
About dialog keeps a one-line pointer to this dialog.
-}
view : Html Msg
view =
    div [ class "modal-overlay" ]
        [ div [ class "modal-dialog modal-support" ]
            [ h2 [ class "modal-title" ] [ text "Support the Project" ]
            , div [ class "modal-body" ]
                [ p []
                    [ text "Sangeet Notes Editor is free and always will be — all features, no restrictions. "
                    , text "If it has helped you preserve or share music, you can support continued development:"
                    ]
                , hr [] []
                , h3 [ class "about-section-header" ] [ text "For users in India — UPI" ]
                , p [ class "about-support-row" ]
                    [ span [ class "about-support-label" ] [ text "UPI handle: " ]
                    , span [ class "about-upi-handle" ] [ text upiHandle ]
                    ]
                , img [ src "images/upi-qr.png", alt "UPI QR code", class "about-upi-qr" ] []
                , hr [] []
                , h3 [ class "about-section-header" ] [ text "For international users" ]
                , p [ class "about-support-row" ]
                    [ a [ href paypalUrl, target "_blank" ]
                        [ text "Support via PayPal" ]
                    ]
                , p [ class "about-tech" ]
                    [ text "🙏 Thank you for your support." ]
                ]
            , div [ class "modal-footer" ]
                [ button [ class "btn btn-primary", onClick CloseSupportDialog ]
                    [ text "Close" ]
                ]
            ]
        ]


upiHandle : String
upiHandle =
    "bharath12345-1@oksbi"


paypalUrl : String
paypalUrl =
    "https://www.paypal.com/ncp/payment/4NZ6FZZFVQMR6"
