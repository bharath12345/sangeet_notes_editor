module View.Layout exposing (view)

import Api.Reference
import Html exposing (Html, div, text)
import Html.Attributes exposing (class, id, tabindex)
import State.Model as Model exposing (Model)
import State.Msg exposing (Msg)
import UiStrings
import View.Canvas as Canvas
import View.Colors as Colors
import View.Dialogs.About as AboutDialog
import View.Dialogs.BugReport as BugReportDialog
import View.Dialogs.CommandPalette as CommandPalette
import View.Dialogs.DuplicateTab as DuplicateTabDialog
import View.Dialogs.KeyboardCheatSheet as KeyboardCheatSheet
import View.Dialogs.NewComposition as NewDialog
import View.Dialogs.Properties as PropsDialog
import View.Dialogs.Support as SupportDialog
import View.Dialogs.UnsavedChanges as UnsavedChangesDialog
import View.FileBrowser as FileBrowser
import View.Header as Header
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

                -- Editor header (cursor position + metadata chips)
                , Header.view comp.metadata cur

                -- Main content area
                , div [ class "main-content" ]
                    [ -- Notation canvas. The right-side keyboard legend panel
                      -- was retired in PR-C C.4; its content now lives inside
                      -- the cheat sheet dialog (`?` opens it).
                      if List.isEmpty model.tabs then
                        div [ class "empty-state" ]
                            [ text UiStrings.editorNoCompositionOpen ]

                      else
                        div [ class "canvas-area" ]
                            [ Canvas.view colors model.currentScript comp cur model.currentSectionIndex model.layoutGrids ]
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
        , if model.showSupportDialog then
            SupportDialog.view

          else
            text ""
        , if model.showBugReportDialog then
            BugReportDialog.view model.bugReportForm

          else
            text ""
        , if model.showKeyboardCheatSheet then
            KeyboardCheatSheet.view

          else
            text ""
        , if model.showCommandPalette then
            CommandPalette.view model.paletteQuery model.paletteSelectedIndex model.currentSectionIndex

          else
            text ""
        , case ( model.showDuplicateTabDialog, model.pendingTabOpen ) of
            ( True, Just pending ) ->
                DuplicateTabDialog.view pending

            _ ->
                text ""
        , case model.showUnsavedChangesDialog of
            Just tabId ->
                case model.tabs |> List.filter (\t -> t.id == tabId) |> List.head of
                    Just tab ->
                        UnsavedChangesDialog.view tab

                    Nothing ->
                        text ""

            Nothing ->
                text ""

        -- Loading indicator
        , if model.pendingApiCall then
            div [ class "loading-indicator" ] [ text UiStrings.viewLoading ]

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
