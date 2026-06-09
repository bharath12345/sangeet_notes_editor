module View.Layout exposing (view)

import Api.Reference
import Html exposing (Html, div, text)
import Html.Attributes exposing (class, classList, id, tabindex)
import State.Model as Model exposing (Model)
import State.Msg exposing (Msg)
import View.Canvas as Canvas
import View.Colors as Colors
import View.Dialogs.About as AboutDialog
import View.Dialogs.NewComposition as NewDialog
import View.Dialogs.Properties as PropsDialog
import View.FileBrowser as FileBrowser
import View.Header as Header
import View.KeyboardLegend as KeyboardLegend
import View.StatusBar as StatusBar
import View.Toolbar as Toolbar


{-| Top-level layout assembling all components:

  - Toolbar (top)
  - Editor header (cursor position info)
  - Main content area (notation canvas + optional keyboard legend)
  - Status bar (bottom)
  - Modal dialogs (overlaid when active)

-}
view : Model -> Html Msg
view model =
    let
        comp =
            Model.composition model

        cur =
            Model.cursor model

        colors =
            model.notationColors
                |> Maybe.map apiColorsToViewColors
                |> Maybe.withDefault Colors.defaultColors
    in
    div [ class "app-container", tabindex 0, id "app-container" ]
        [ div [ class "app-body" ]
            [ -- File browser panel (left)
              FileBrowser.view model

            -- Editor area (right)
            , div [ class "editor-area" ]
                [ -- Toolbar
                  Toolbar.view model

                -- Editor header (cursor position)
                , Header.view comp.metadata cur model.editMode

                -- Main content area
                , div [ class "main-content" ]
                    [ -- Notation canvas
                      div
                        [ classList
                            [ ( "canvas-area", True )
                            , ( "canvas-area-with-legend", model.showKeyboardLegend )
                            ]
                        ]
                        [ Canvas.view colors model.currentScript comp cur model.currentSectionIndex model.layoutGrids ]

                    -- Keyboard legend sidebar (optional)
                    , if model.showKeyboardLegend then
                        KeyboardLegend.view

                      else
                        text ""
                    ]

                -- Status bar
                , StatusBar.view model.statusLog
                ]
            ]

        -- Modal dialogs
        , if model.showNewDialog then
            NewDialog.view model.newDialogForm model.availableTaals model.availableRaags

          else
            text ""
        , if model.showPropsDialog then
            PropsDialog.view model.propsDialogForm model.availableTaals

          else
            text ""
        , if model.showAboutDialog then
            AboutDialog.view

          else
            text ""

        -- Loading indicator
        , if model.pendingApiCall then
            div [ class "loading-indicator" ] [ text "Loading..." ]

          else
            text ""
        ]


{-| Convert API NotationColors to View NotationColors.
The types are structurally identical but in different modules.
-}
apiColorsToViewColors : Api.Reference.NotationColors -> Colors.NotationColors
apiColorsToViewColors api =
    { taalMarker = api.taalMarker
    , taalMarkerSam = api.taalMarkerSam
    , swar = api.swar
    , octaveDot = api.octaveDot
    , ornament = api.ornament
    , stroke = api.stroke
    , sahitya = api.sahitya
    , rest = api.rest
    , sustain = api.sustain
    , komalMark = api.komalMark
    , tivraMark = api.tivraMark
    }
