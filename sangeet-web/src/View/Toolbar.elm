module View.Toolbar exposing (view)

import Html exposing (Html, button, div, label, option, select, span, text)
import Html.Attributes exposing (class, disabled, selected, title, value)
import Html.Events exposing (onClick, onInput)
import Model.Composition
import Model.Types exposing (SwarScript(..))
import State.Model exposing (EditMode(..), FileTab, Model, OrnamentMode(..))
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory


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
            , title "Beta software — actively iterating toward v1.0. Use 🐞 Report bug for issues."
            ]
            [ text "BETA" ]
        , div [ class "toolbar-separator" ] []

        -- File group
        , div [ class "toolbar-group" ]
            [ button [ class "toolbar-btn", title "New Composition (Ctrl+N)", onClick ShowNewDialog ]
                [ text "New" ]
            , button [ class "toolbar-btn", title "Open File", onClick OpenFile ]
                [ text "Open" ]
            , button [ class "toolbar-btn", title "Save File (Ctrl+S)", onClick SaveFile ]
                [ text "Save" ]
            , button [ class "toolbar-btn", title "Cut (Ctrl+X)", onClick (KeyPressed "x" False True False) ]
                [ text "✂" ]
            , button [ class "toolbar-btn", title "Copy (Ctrl+C)", onClick (KeyPressed "c" False True False) ]
                [ text "📋" ]
            , button [ class "toolbar-btn", title "Paste (Ctrl+V)", onClick (KeyPressed "v" False True False) ]
                [ text "📌" ]
            , button [ class "toolbar-btn", title "Export HTML", onClick ExportHtml ]
                [ text "HTML" ]
            ]
        , div [ class "toolbar-separator" ] []

        -- Edit group
        , div [ class "toolbar-group" ]
            [ button
                [ class "toolbar-btn"
                , title "Undo (Ctrl+Z)"
                , onClick Undo
                , disabled (not (UndoHistory.canUndo model.history))
                ]
                [ text "Undo" ]
            , button
                [ class "toolbar-btn"
                , title "Redo (Ctrl+Y)"
                , onClick Redo
                , disabled (not (UndoHistory.canRedo model.history))
                ]
                [ text "Redo" ]
            ]
        , div [ class "toolbar-separator" ] []

        -- Edit mode indicator
        , div [ class "toolbar-group" ]
            [ span [ class "toolbar-label" ]
                [ text
                    (case model.editMode of
                        SwarEdit ->
                            "Mode: Swar"

                        StrokeEdit ->
                            "Mode: Stroke"
                    )
                ]
            , ornamentModeIndicator model.ornamentMode
            ]
        , div [ class "toolbar-separator" ] []

        -- View toggles
        , div [ class "toolbar-group" ]
            [ button
                [ class "toolbar-btn"
                , title "Toggle Stroke Line"
                , onClick ToggleStrokeLine
                ]
                [ text "Strokes" ]
            , button
                [ class "toolbar-btn"
                , title "Toggle Sahitya Line"
                , onClick ToggleSahityaLine
                ]
                [ text "Sahitya" ]
            , button
                [ class "toolbar-btn"
                , title "Keyboard Shortcuts"
                , onClick ToggleKeyboardLegend
                ]
                [ text "Keys" ]
            ]
        , div [ class "toolbar-separator" ] []

        -- Properties, Report Bug, About
        , div [ class "toolbar-group" ]
            [ button [ class "toolbar-btn", title "Composition Properties", onClick ShowPropsDialog ]
                [ text "Properties" ]
            , button [ class "toolbar-btn", title "Report a bug — includes a short replay so it can be reproduced", onClick ShowBugReportDialog ]
                [ text "🐞 Report bug" ]
            , button [ class "toolbar-btn", title "Keyboard shortcuts (?)", onClick ShowKeyboardCheatSheet ]
                [ text "?" ]
            , button [ class "toolbar-btn", title "About", onClick ShowAboutDialog ]
                [ text "About" ]
            ]
        , div [ class "toolbar-separator" ] []

        -- Script selector
        , div [ class "toolbar-group" ]
            [ label [ class "toolbar-label" ] [ text "Script:" ]
            , select
                [ class "script-select"
                , onInput (\s -> ChangeScript (stringToScript s))
                ]
                [ option [ value "devanagari", selected (model.currentScript == Devanagari) ]
                    [ text "Devanagari" ]
                , option [ value "kannada", selected (model.currentScript == Kannada) ]
                    [ text "Kannada" ]
                , option [ value "telugu", selected (model.currentScript == Telugu) ]
                    [ text "Telugu" ]
                , option [ value "english", selected (model.currentScript == English) ]
                    [ text "English" ]
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
                        , title "New Tab"
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
            , title "Close tab"
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
                [ text ("Orn: " ++ name ++ " (type note)") ]

        MeendStartMode _ ->
            span [ class "toolbar-badge ornament-badge" ]
                [ text "Meend: type start note" ]

        MeendEndMode _ _ ->
            span [ class "toolbar-badge ornament-badge" ]
                [ text "Meend: type end note" ]

        KrintanStartMode ->
            span [ class "toolbar-badge ornament-badge" ]
                [ text "Krintan: type start note" ]

        KrintanEndMode _ ->
            span [ class "toolbar-badge ornament-badge" ]
                [ text "Krintan: type end note / Enter" ]

        MurkiCollectMode notes ->
            span [ class "toolbar-badge ornament-badge" ]
                [ text ("Murki: " ++ String.fromInt (List.length notes) ++ " notes (Enter to apply)") ]

        ZamzamaCollectMode notes ->
            span [ class "toolbar-badge ornament-badge" ]
                [ text ("Zamzama: " ++ String.fromInt (List.length notes) ++ " notes (Enter to apply)") ]


viewBottomRow : Model -> Html Msg
viewBottomRow model =
    div [ class "toolbar-row toolbar-row-bottom" ]
        [ -- Section tabs
          div [ class "toolbar-group section-tabs" ]
            (viewSectionTabs model)
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
                , title "Add Section"
                , onClick (AddSection "New Section" Model.Composition.Taan)
                ]
                [ text "+" ]
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
