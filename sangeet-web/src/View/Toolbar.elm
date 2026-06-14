module View.Toolbar exposing (view)

import Html exposing (Html, button, div, label, option, select, span, text)
import Html.Attributes exposing (class, disabled, selected, title, value)
import Html.Events exposing (onClick, onInput)
import Model.Composition
import Model.Types exposing (SwarScript(..))
import State.Model exposing (EditMode(..), FileTab, Model, OrnamentMode(..))
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import UiStrings


view : Model -> Html Msg
view model =
    div [ class "toolbar" ]
        [ viewTopRow model
        , viewTabBar model
        , viewBottomRow model
        ]


viewTopRow : Model -> Html Msg
viewTopRow model =
    div [ class "toolbar-row toolbar-row-top" ]
        [ -- Task 2: BETA badge. Anchors the left edge so it's always visible
          -- even when the toolbar overflows on narrow windows.
          span
            [ class "toolbar-badge beta-badge"
            , title UiStrings.toolbarBetaTooltip
            ]
            [ text UiStrings.toolbarBetaBadge ]
        , div [ class "toolbar-separator" ] []

        -- File group
        , div [ class "toolbar-group" ]
            [ button [ class "toolbar-btn", title UiStrings.toolbarFileNewTooltip, onClick ShowNewDialog ]
                [ text UiStrings.toolbarFileNew ]
            , button [ class "toolbar-btn", title UiStrings.toolbarFileOpenTooltip, onClick OpenFile ]
                [ text UiStrings.toolbarFileOpen ]
            , button [ class "toolbar-btn", title UiStrings.toolbarFileSaveTooltip, onClick SaveFile ]
                [ text UiStrings.toolbarFileSave ]
            , button [ class "toolbar-btn", title UiStrings.toolbarFileCutTooltip, onClick (KeyPressed "x" False True False) ]
                [ text UiStrings.toolbarFileCut ]
            , button [ class "toolbar-btn", title UiStrings.toolbarFileCopyTooltip, onClick (KeyPressed "c" False True False) ]
                [ text UiStrings.toolbarFileCopy ]
            , button [ class "toolbar-btn", title UiStrings.toolbarFilePasteTooltip, onClick (KeyPressed "v" False True False) ]
                [ text UiStrings.toolbarFilePaste ]
            , button [ class "toolbar-btn", title UiStrings.toolbarFileExportHtmlTooltip, onClick ExportHtml ]
                [ text UiStrings.toolbarFileExportHtml ]
            ]
        , div [ class "toolbar-separator" ] []

        -- Edit group
        , div [ class "toolbar-group" ]
            [ button
                [ class "toolbar-btn"
                , title UiStrings.toolbarEditUndoTooltip
                , onClick Undo
                , disabled (not (UndoHistory.canUndo model.history))
                ]
                [ text UiStrings.toolbarEditUndo ]
            , button
                [ class "toolbar-btn"
                , title UiStrings.toolbarEditRedoTooltip
                , onClick Redo
                , disabled (not (UndoHistory.canRedo model.history))
                ]
                [ text UiStrings.toolbarEditRedo ]
            ]
        , div [ class "toolbar-separator" ] []

        -- Edit mode indicator
        , div [ class "toolbar-group" ]
            [ span [ class "toolbar-label" ]
                [ text
                    (case model.editMode of
                        SwarEdit ->
                            UiStrings.toolbarModeSwar

                        StrokeEdit ->
                            UiStrings.toolbarModeStroke
                    )
                ]
            , ornamentModeIndicator model.ornamentMode
            ]
        , div [ class "toolbar-separator" ] []

        -- View toggles (only the keyboard legend panel toggle; the Strokes
        -- and Sahitya toggles were retired — those rows always render now.)
        , div [ class "toolbar-group" ]
            [ button
                [ class "toolbar-btn"
                , title UiStrings.toolbarViewToggleKeyboardLegendTooltip
                , onClick ToggleKeyboardLegend
                ]
                [ text UiStrings.toolbarViewToggleKeyboardLegend ]
            ]
        , div [ class "toolbar-separator" ] []

        -- Properties, Report Bug, About
        , div [ class "toolbar-group" ]
            [ button [ class "toolbar-btn", title UiStrings.toolbarHelpPropertiesTooltip, onClick ShowPropsDialog ]
                [ text UiStrings.toolbarHelpProperties ]
            , button [ class "toolbar-btn", title UiStrings.toolbarHelpReportBugTooltip, onClick ShowBugReportDialog ]
                [ text UiStrings.toolbarHelpReportBug ]
            , button [ class "toolbar-btn", title UiStrings.toolbarHelpKeyboardShortcutsTooltip, onClick ShowKeyboardCheatSheet ]
                [ text UiStrings.toolbarHelpKeyboardShortcuts ]
            , button [ class "toolbar-btn", title UiStrings.toolbarHelpSupportTooltip, onClick ShowSupportDialog ]
                [ text UiStrings.toolbarHelpSupport ]
            , button [ class "toolbar-btn", title UiStrings.toolbarHelpAboutTooltip, onClick ShowAboutDialog ]
                [ text UiStrings.toolbarHelpAbout ]
            ]
        , div [ class "toolbar-separator" ] []

        -- Script selector
        , div [ class "toolbar-group" ]
            [ label [ class "toolbar-label" ] [ text UiStrings.toolbarScriptLabel ]
            , select
                [ class "script-select"
                , title UiStrings.toolbarScriptTooltip
                , onInput (\s -> ChangeScript (stringToScript s))
                ]
                [ option [ value "devanagari", selected (model.currentScript == Devanagari) ]
                    [ text UiStrings.toolbarScriptDevanagari ]
                , option [ value "kannada", selected (model.currentScript == Kannada) ]
                    [ text UiStrings.toolbarScriptKannada ]
                , option [ value "telugu", selected (model.currentScript == Telugu) ]
                    [ text UiStrings.toolbarScriptTelugu ]
                , option [ value "english", selected (model.currentScript == English) ]
                    [ text UiStrings.toolbarScriptEnglish ]
                ]
            ]
        ]


viewTabBar : Model -> Html Msg
viewTabBar model =
    div [ class "toolbar-row toolbar-row-tabs" ]
        [ div [ class "toolbar-group file-tabs" ]
            (List.map (viewFileTab model.activeTabId) model.tabs
                ++ [ button
                        [ class "file-tab file-tab-add"
                        , title UiStrings.toolbarTabsNewTooltip
                        , onClick NewTab
                        ]
                        [ text "+" ]
                   ]
            )
        ]


viewFileTab : Maybe String -> FileTab -> Html Msg
viewFileTab activeId tab =
    let
        isActive =
            activeId == Just tab.id
    in
    div
        [ class
            (if isActive then
                "file-tab file-tab-active"

             else
                "file-tab"
            )
        ]
        [ span
            [ class "file-tab-label"
            , onClick (SwitchTab tab.id)
            , title tab.filename
            ]
            [ text tab.filename ]
        , button
            [ class "file-tab-close"
            , onClick (CloseTab tab.id)
            , title UiStrings.toolbarTabsCloseTooltip
            ]
            [ text "×" ]
        ]


ornamentModeIndicator : OrnamentMode -> Html msg
ornamentModeIndicator mode =
    case mode of
        NoOrnament ->
            text ""

        SingleNoteMode name ->
            span [ class "toolbar-badge ornament-badge" ]
                [ text (UiStrings.toolbarOrnamentSingleNote name) ]

        MeendStartMode _ ->
            span [ class "toolbar-badge ornament-badge" ]
                [ text UiStrings.toolbarOrnamentMeendStart ]

        MeendEndMode _ _ ->
            span [ class "toolbar-badge ornament-badge" ]
                [ text UiStrings.toolbarOrnamentMeendEnd ]

        KrintanStartMode ->
            span [ class "toolbar-badge ornament-badge" ]
                [ text UiStrings.toolbarOrnamentKrintanStart ]

        KrintanEndMode _ ->
            span [ class "toolbar-badge ornament-badge" ]
                [ text UiStrings.toolbarOrnamentKrintanEnd ]

        MurkiCollectMode notes ->
            span [ class "toolbar-badge ornament-badge" ]
                [ text (UiStrings.toolbarOrnamentMurki (List.length notes)) ]

        ZamzamaCollectMode notes ->
            span [ class "toolbar-badge ornament-badge" ]
                [ text (UiStrings.toolbarOrnamentZamzama (List.length notes)) ]


viewBottomRow : Model -> Html Msg
viewBottomRow model =
    div [ class "toolbar-row toolbar-row-bottom" ]
        [ -- Section tabs
          div [ class "toolbar-group section-tabs" ]
            (viewSectionTabs model)
        , viewSectionActions model
        ]


viewSectionTabs : Model -> List (Html Msg)
viewSectionTabs model =
    let
        comp =
            (UndoHistory.present model.history).composition

        sections =
            comp.sections
    in
    List.indexedMap
        (\idx section ->
            button
                [ class
                    (if idx == model.currentSectionIndex then
                        "section-tab section-tab-active"

                     else
                        "section-tab"
                    )
                , onClick (SelectSection idx)
                , title section.name
                ]
                [ text section.name ]
        )
        sections
        ++ [ button
                [ class "section-tab section-tab-add"
                , title UiStrings.toolbarSectionAddTooltip
                , onClick (AddSection UiStrings.actionAddSectionDefaultName Model.Composition.Taan)
                ]
                [ text "+" ]
           ]


{-| Rename / Remove / Move-up / Move-down buttons that act on the current
section. Mirrors ToolbarBuilder.scala's section-management cluster on desktop.
Buttons disable themselves when the action wouldn't apply (e.g. Move Up on the
first section, Remove when there's only one section left).
-}
viewSectionActions : Model -> Html Msg
viewSectionActions model =
    let
        comp =
            (UndoHistory.present model.history).composition

        sections =
            comp.sections

        idx =
            model.currentSectionIndex

        currentName =
            sections
                |> List.drop idx
                |> List.head
                |> Maybe.map .name
                |> Maybe.withDefault ""

        sectionCount =
            List.length sections

        atTop =
            idx <= 0

        atBottom =
            idx >= sectionCount - 1

        onlyOne =
            sectionCount <= 1
    in
    div [ class "toolbar-group section-actions" ]
        [ button
            [ class "toolbar-btn"
            , title UiStrings.toolbarSectionMoveUpTooltip
            , onClick (MoveSectionUp idx)
            , disabled atTop
            ]
            [ text "↑" ]
        , button
            [ class "toolbar-btn"
            , title UiStrings.toolbarSectionMoveDownTooltip
            , onClick (MoveSectionDown idx)
            , disabled atBottom
            ]
            [ text "↓" ]
        , button
            [ class "toolbar-btn"
            , title UiStrings.toolbarSectionRenameTooltip
            , onClick (RequestRenameSection idx currentName)
            ]
            [ text "✎" ]
        , button
            [ class "toolbar-btn"
            , title UiStrings.toolbarSectionRemoveTooltip
            , onClick (RemoveSection idx)
            , disabled onlyOne
            ]
            [ text "✕" ]
        ]


stringToScript : String -> SwarScript
stringToScript s =
    case s of
        "kannada" ->
            Kannada

        "telugu" ->
            Telugu

        "english" ->
            English

        _ ->
            Devanagari
