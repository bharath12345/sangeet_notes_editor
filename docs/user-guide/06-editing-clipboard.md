# Editing & Clipboard

## Selection

Select a range of beats by holding **Shift** while moving the cursor:

| Key combo | Effect |
|-----------|--------|
| `Shift+←` `Shift+→` | Extend selection by one beat |
| `Shift+Home` | Extend selection to the start of the section |
| `Shift+End` | Extend selection to the last cycle |

The selected range is highlighted in soft maroon on the canvas. Selection works within a single section only — you cannot select across sections.

To clear a selection, move the cursor without holding Shift (any plain arrow / Tab / Enter or a mouse click).

## Cut, copy, paste

| Key combo | Effect |
|-----------|--------|
| `Ctrl+X` | Cut selected events to clipboard |
| `Ctrl+C` | Copy selected events to clipboard |
| `Ctrl+V` | Paste clipboard at the cursor position |

The clipboard uses the **system clipboard** — you can copy from one Sangeet tab and paste into another, and even paste data later after closing and reopening the app (as long as you haven't overwritten the system clipboard with something else).

Pasted events are inserted at the cursor's current beat. If the paste would extend beyond the current cycle, new cycles are created as needed. Events from different subdivisions paste correctly — the original beat structure is preserved.

The clipboard format is JSON (with the events serialized using the same codec as `.swar` files). This means:
- Pasted ornaments, strokes, octaves, subdivisions all survive the round trip
- You can paste from outside the app — anything matching the format works
- Pasting non-Sangeet text is silently ignored

## Undo and redo

| Key combo | Effect |
|-----------|--------|
| `Ctrl+Z` | Undo last edit |
| `Ctrl+Shift+Z` | Redo |

Each tab has its own **undo history** — undoing in one composition doesn't affect another. History persists for the lifetime of the tab (it does not persist across app restarts).

What counts as one undoable edit:
- Typing one note (or one fast-typed group)
- Deleting one event (or one fast-typed group)
- Pasting a clipboard
- Cutting a selection
- Renaming a section, adding a section, removing a section
- Changing taal, raag, laya, script, or starting beats via Properties
- Setting a stroke (Da/Ra)
- Adding an ornament

Cursor moves and selection changes do **not** create undo entries — they're cursor-only.

## Auto-save

Every edit triggers an **auto-save** debounced by 500 ms after the last keystroke. Auto-save runs on a background thread so it doesn't pause your typing. The status bar shows a brief "Saved" message after each auto-save completes.

If auto-save fails (e.g., disk full, permission denied), a warning appears in the status bar. Your edit history is still intact in memory — you can keep working.

## Read-only mode

The sample composition that loads on first launch is **read-only** — a red "Read-only sample" notice appears at the top. Cursor moves work, but typing, deleting, and other edits are blocked. To start editing for real, click **New** to create your own composition (or **Open** to open an existing `.swar` file).

You can toggle read-only mode programmatically via the debug console (see [File Operations](07-file-operations.md#debug-console)), but there is no UI button to make a composition read-only by hand.

## Keyboard shortcuts and modes

A few keystrokes behave differently depending on **mode**:

- **Stroke edit mode** (`F2` to toggle): lowercase `d` and `r` set Da/Ra on cells instead of typing notes
- **Ornament entry mode** (`Ctrl+M`, `Ctrl+K`, etc.): the next 1-N notes you type become the ornament's notes instead of plain notes in the grid

The status bar and current edit-mode indicator tell you what mode you're in. `Esc` exits any special mode and returns to normal note input.

## What to read next

- [File Operations](07-file-operations.md) — save, open, file browser
- [Keyboard Reference](08-keyboard-reference.md) — full keymap
