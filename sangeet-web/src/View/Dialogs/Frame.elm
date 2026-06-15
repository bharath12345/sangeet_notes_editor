module View.Dialogs.Frame exposing
    ( Config
    , view
    , viewRaw
    )

{-| Shared modal-dialog chrome.

Every dialog used to hand-roll the overlay + container + title + body + footer
divs with the exact same CSS classes. This module centralizes that markup so
all 10 web dialogs share one source of truth for the modal frame.


# Design notes

  - The frame preserves the existing CSS class names (`modal-overlay`,
    `modal-dialog`, `modal-title`, `modal-body`, `modal-footer`) so that
    `public/styles.css`, the E2E tests in `e2e/tests/`, and the page-object
    selectors in `e2e/helpers/app-page.ts` continue to work unchanged.
  - ESC dismissal is **not** handled here — it is wired at the subscription
    layer (`Browser.Events.onKeyDown` in the Subscriptions module) so the same
    keyboard handling applies whether the modal is opened by mouse or keyboard.
  - Click-outside-to-close is **not** added here — the existing dialogs do not
    implement that behaviour and adding it now would silently change UX. Pass
    an explicit close button in `footer` instead.
  - Each dialog supplies its own per-dialog variant class (e.g. `modal-about`,
    `modal-bug-report`) via `variantClass` so per-dialog CSS rules continue to
    apply.

-}

import Html exposing (Html, div, h2, text)
import Html.Attributes exposing (class)


{-| Frame configuration. `body` and `footer` are caller-supplied.
-}
type alias Config msg =
    { title : String
    , variantClass : String
    , body : List (Html msg)
    , footer : List (Html msg)
    }


{-| Standard dialog frame: overlay → container → title header + body + footer.

Renders this DOM tree:

    div.modal-overlay
      div.modal-dialog.<variantClass>
        h2.modal-title  -- omitted when title == ""
        div.modal-body  -- omitted when body == []
        div.modal-footer  -- omitted when footer == []

-}
view : Config msg -> Html msg
view config =
    div [ class "modal-overlay" ]
        [ div [ class ("modal-dialog " ++ config.variantClass) ]
            (List.concat
                [ if String.isEmpty config.title then
                    []

                  else
                    [ h2 [ class "modal-title" ] [ text config.title ] ]
                , if List.isEmpty config.body then
                    []

                  else
                    [ div [ class "modal-body" ] config.body ]
                , if List.isEmpty config.footer then
                    []

                  else
                    [ div [ class "modal-footer" ] config.footer ]
                ]
            )
        ]


{-| Raw frame for dialogs that don't fit the title/body/footer shape — e.g.
the command palette has its own internal layout (search + results) and uses an
extra overlay class (`palette-overlay`).

`overlayExtraClass` is appended to the standard `modal-overlay` class.
`children` are placed inside the container as-is.

-}
viewRaw :
    { overlayExtraClass : String
    , variantClass : String
    , children : List (Html msg)
    }
    -> Html msg
viewRaw config =
    let
        overlayClass =
            if String.isEmpty config.overlayExtraClass then
                "modal-overlay"

            else
                "modal-overlay " ++ config.overlayExtraClass
    in
    div [ class overlayClass ]
        [ div [ class ("modal-dialog " ++ config.variantClass) ]
            config.children
        ]
