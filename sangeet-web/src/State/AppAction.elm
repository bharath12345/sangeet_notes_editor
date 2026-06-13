module State.AppAction exposing (AppAction, all, filter)

import State.Msg exposing (Msg(..))


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

-}
all : List AppAction
all =
    [ -- File
      { title = "New composition", group = "File", shortcut = Nothing, msg = NewComposition }
    , { title = "Open file", group = "File", shortcut = Nothing, msg = OpenFile }
    , { title = "Save", group = "File", shortcut = Just "Ctrl+S", msg = SaveFile }
    , { title = "Export HTML", group = "File", shortcut = Nothing, msg = ExportHtml }

    -- Edit
    , { title = "Undo", group = "Edit", shortcut = Just "Ctrl+Z", msg = Undo }
    , { title = "Redo", group = "Edit", shortcut = Just "Ctrl+Y", msg = Redo }

    -- View
    , { title = "Toggle stroke line", group = "View", shortcut = Nothing, msg = ToggleStrokeLine }
    , { title = "Toggle sahitya line", group = "View", shortcut = Nothing, msg = ToggleSahityaLine }
    , { title = "Toggle keyboard legend", group = "View", shortcut = Nothing, msg = ToggleKeyboardLegend }

    -- Properties / dialogs
    , { title = "Edit composition properties", group = "Edit", shortcut = Nothing, msg = ShowPropsDialog }

    -- Help
    , { title = "Show keyboard shortcuts", group = "Help", shortcut = Just "?", msg = ShowKeyboardCheatSheet }
    , { title = "Report a bug", group = "Help", shortcut = Nothing, msg = ShowBugReportDialog }
    , { title = "Support the project", group = "Help", shortcut = Nothing, msg = ShowSupportDialog }
    , { title = "About Sangeet Notes Editor", group = "Help", shortcut = Nothing, msg = ShowAboutDialog }
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
