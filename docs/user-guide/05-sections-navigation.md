# Sections & Navigation

A composition is made of one or more **sections** (e.g., Gat, Antara, Taan 1, Taan 2). Each section has its own grid of cycles and its own taal positions. Cycles within a section repeat until you fill them with notes.

## The cursor

The blinking vertical bar is the cursor. It tracks:
- The **current section**
- The **cycle** (row of the grid)
- The **beat** within the cycle
- The **sub-position** within the beat (if subdivisions > 1)

The status bar at the bottom shows the cursor's position whenever it changes.

## Moving the cursor

| Key | Effect |
|-----|--------|
| `←` `→` | Move one sub-position left / right |
| `Tab` | Jump to the next beat |
| `Shift+Tab` | Jump to the previous beat |
| `Enter` | Jump to the start of the next cycle |
| `Shift+Enter` | Jump to the start of the previous cycle |
| `Home` | Jump to the first beat of the current cycle |
| `End` | Jump to the last beat of the current cycle |
| **Mouse click** | Place cursor on the clicked cell (or just switch sections) |

If you click on a section that isn't the current section, the editor switches to that section. The cursor lands on the beat you clicked (or at the section's starting beat if you clicked outside any beat cell).

## Sections in the toolbar

Five buttons manage sections:

- **Add Section** — adds a new section after the current one. (Gat compositions only — Bandish and Palta have a fixed section set.) Pick from Gat, Sthayi, Antara, Taan, Jhala, Jod.
- **Rename Section** — change the current section's name. Useful for distinguishing multiple Taan sections beyond "Taan 1", "Taan 2", etc.
- **Remove Section** — delete the current section. You cannot remove the last section.
- **Move Up / Move Down** — reorder sections.

## Section types

| Type | Typical use |
|------|-------------|
| **Gat** | The main instrumental theme (masitkhani or razakhani) |
| **Sthayi** | The first phrase of a bandish, sung in lower-middle register |
| **Antara** | Higher-register continuation of a bandish or gat |
| **Sanchari** | Rare, traversing all registers |
| **Abhog** | Rare, concluding section |
| **Taan** | Fast melodic run (numbered: Taan 1, Taan 2, …) |
| **Toda** | Fast rhythmic improvisation (numbered) |
| **Jhala** | Rapid alternation between melody and chikari strings |
| **Palta** | Practice pattern (used in Palta compositions) |
| **Arohi / Avarohi** | Ascending / descending scale pattern |
| **Custom** | Any user-defined name |

## Across cycles

A section grows automatically as you type. The first cycle (row 0) shows beats 1 through *matras* (e.g., 1–16 for teentaal). Once you fill beat *matras*, the cursor moves to cycle 1, beat 1 — a new row appears.

If a section starts after sam (i.e., starting beat > 1), the first locked-beat cells before sam appear in cycle 0 only. Subsequent cycles use the full taal length. See [Starting Beat](10-starting-beat.md).

## Line breaking on the canvas

The grid layout engine decides where to break lines based on:
- **Density** — drut compositions show a full cycle per line; vilambit may split a cycle by vibhag (clap group) for readability
- **Window width** — cells scale to fit; wider windows pack more on each line

Vibhag separators (vertical lines) and taal markers (Sam X, Taali numbers, Khali 0) render above each beat that has one.

## Tabs

The editor supports multiple compositions open at the same time. Use:

- **`Ctrl+Tab`** / **`Ctrl+Shift+Tab`** — cycle through open tabs
- **`Ctrl+W`** — close the active tab (you'll be prompted if there are unsaved changes — though auto-save means there usually aren't)
- **Double-click a `.swar` file in the file browser** — opens it in a new tab

The active tab's file path appears in its tab header. An asterisk (`*`) appears briefly while auto-save is pending.

## What to read next

- [Editing & Clipboard](06-editing-clipboard.md) — selection, cut/copy/paste, undo
- [File Operations](07-file-operations.md) — save, open, file browser
