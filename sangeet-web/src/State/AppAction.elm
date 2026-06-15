module State.AppAction exposing (AppAction, all, filter)

import Model.Composition exposing (SectionType(..))
import State.Msg exposing (Msg(..))
import UiStrings


{-| An action discoverable from the Cmd+K command palette. The shortcut is a
display string only; key bindings live in handleKeyPress. `msg` is what gets
dispatched when the user picks the action.
-}
type alias AppAction =
    { title : String
    , group : String
    , shortcut : Maybe String
    , msg : Msg
    }


{-| The full catalog of palette-accessible actions.

Note: web-side keyboard shortcuts are scarce because browsers reserve many
Ctrl+ combos. The palette is the safe path to all toolbar actions regardless
of what's wired to a key.

Some entries depend on which section is currently active (rename / remove /
move). The caller supplies `currentSectionIndex` so the dispatched Msg
carries the right target without the catalog needing direct Model access.

-}
all : Int -> List AppAction
all currentSectionIndex =
    [ -- File
      { title = UiStrings.appActionNewComposition, group = UiStrings.appActionGroupFile, shortcut = Nothing, msg = NewComposition }
    , { title = UiStrings.appActionOpenFile, group = UiStrings.appActionGroupFile, shortcut = Nothing, msg = OpenFile }
    , { title = UiStrings.appActionSave, group = UiStrings.appActionGroupFile, shortcut = Just "Ctrl+S", msg = SaveFile }
    , { title = UiStrings.appActionSaveAs, group = UiStrings.appActionGroupFile, shortcut = Just "Ctrl+Shift+S", msg = SaveFileAs }
    , { title = UiStrings.appActionExportHtml, group = UiStrings.appActionGroupFile, shortcut = Nothing, msg = ExportHtml }

    -- Edit
    , { title = UiStrings.appActionUndo, group = UiStrings.appActionGroupEdit, shortcut = Just "Ctrl+Z", msg = Undo }
    , { title = UiStrings.appActionRedo, group = UiStrings.appActionGroupEdit, shortcut = Just "Ctrl+Y", msg = Redo }

    -- Properties / dialogs
    , { title = UiStrings.appActionEditCompositionProperties, group = UiStrings.appActionGroupEdit, shortcut = Just "Ctrl+,", msg = ShowPropsDialog }

    -- Sections
    , { title = UiStrings.appActionAddSection, group = UiStrings.appActionGroupSections, shortcut = Just "Ctrl+Shift+A", msg = AddSection UiStrings.actionAddSectionDefaultName Taan }
    , { title = UiStrings.appActionClearCurrentSection, group = UiStrings.appActionGroupSections, shortcut = Nothing, msg = RequestClearSection currentSectionIndex }
    , { title = UiStrings.appActionRemoveCurrentSection, group = UiStrings.appActionGroupSections, shortcut = Just "Ctrl+Shift+Backspace", msg = RemoveSection currentSectionIndex }
    , { title = UiStrings.appActionMoveCurrentSectionUp, group = UiStrings.appActionGroupSections, shortcut = Nothing, msg = MoveSectionUp currentSectionIndex }
    , { title = UiStrings.appActionMoveCurrentSectionDown, group = UiStrings.appActionGroupSections, shortcut = Nothing, msg = MoveSectionDown currentSectionIndex }

    -- View
    , { title = UiStrings.appActionToggleTheme, group = UiStrings.appActionGroupEdit, shortcut = Nothing, msg = ToggleTheme }

    -- Help
    , { title = UiStrings.appActionShowKeyboardShortcuts, group = UiStrings.appActionGroupHelp, shortcut = Just "?", msg = ShowKeyboardCheatSheet }
    , { title = UiStrings.appActionOpenUserGuide, group = UiStrings.appActionGroupHelp, shortcut = Nothing, msg = OpenUserGuide }
    , { title = UiStrings.appActionReportBug, group = UiStrings.appActionGroupHelp, shortcut = Nothing, msg = ShowBugReportDialog }
    , { title = UiStrings.appActionSupportProject, group = UiStrings.appActionGroupHelp, shortcut = Nothing, msg = ShowSupportDialog }
    , { title = UiStrings.appActionAboutSangeet, group = UiStrings.appActionGroupHelp, shortcut = Nothing, msg = ShowAboutDialog }
    ]


{-| Lowercase substring match on title or group. Empty query → all actions.
-}
filter : String -> List AppAction -> List AppAction
filter query actions =
    let
        needle =
            String.trim (String.toLower query)
    in
    if String.isEmpty needle then
        actions

    else
        List.filter
            (\a -> String.contains needle (String.toLower a.title) || String.contains needle (String.toLower a.group))
            actions
