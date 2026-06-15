# File Operations

## The `.swar` file format

Compositions are saved as **`.swar`** files — UTF-8 JSON, one file per composition. Open them in any text editor to inspect or back up; the structure is human-readable.

A `.swar` file contains:
- Format version (`"version": "1.0"`)
- Metadata: title, composition type, taal, raag, laya, show-stroke / show-sahitya flags, script
- Sections: name, type, starting beat, events (notes, rests, sustains, chikari, locked-beats), optional tihai
- Each event records its beat position (cycle, beat, subdivision) and any ornaments / strokes / sahitya text

Default values are omitted from the file (compact format), so a freshly created composition is small.

## Toolbar file operations

| Button | Effect |
|--------|--------|
| **New** | Open the New Composition dialog (see [Creating Compositions](02-creating-compositions.md)) |
| **Open File** | Choose a `.swar` file to open in a new tab |
| **Open Folder** | Add a folder to the file browser sidebar |
| **Save** | Save the active tab to its file path (auto-save also does this) |
| **Save As** | Save the active tab to a new file path; the new path becomes the tab's file |
| **HTML** | Export the active composition as a print-friendly HTML file |

> **Web:** File operations work the same, but files live in your browser's downloads / uploads rather than directly on disk. Use the download / upload icons in the web toolbar.

## File browser sidebar

The left sidebar shows folders you've opened. Each folder is a **bookmark** — it persists across sessions. Useful for organizing your composition collection (e.g., "Yaman", "Bhairav", "Practice patterns").

Inside each folder:
- **Double-click a `.swar` file** to open it in a new tab
- **Right-click** for context menu options (rename, delete, reveal in OS file manager)
- **Drag a folder header** to reorder bookmarks

To add a folder: click **Open Folder** in the toolbar, or use `Ctrl+Shift+O`.

To remove a bookmark: right-click the folder header and choose "Remove from bookmarks". The folder itself is not deleted from disk — only its bookmark in the editor.

## Session persistence

The editor remembers:
- Which tabs were open and which was active
- File browser bookmarks
- Panel collapse state (file browser, log, keyboard legend)
- Window size

Everything is saved to `AppConfig` automatically (every 30 seconds and on app close). On the next launch, your previous session is restored.

If the previous session had no tabs open (fresh install), the sample composition is loaded read-only as a demo.

## HTML export

Click **HTML** in the toolbar to export the active composition as a standalone HTML file. The output includes:
- All notation rows (taal markers, ornaments, swar, strokes, sahitya)
- Color-coded glyphs matching the canvas renderer
- Print-friendly CSS (page breaks between sections, no toolbar UI)
- The current script setting (Devanagari / Kannada / Telugu / English)

Use this to share compositions with people who don't have the editor, or to print them on paper.

## Backup and version control

The `.swar` JSON format is git-friendly — store your compositions in a git repository for full version history. Diffs are readable line-by-line. Two notes added in the same beat appear as two adjacent JSON entries.

## Debug console

The desktop app exposes a TCP debug console on **127.0.0.1:28081**. Connect from a terminal:

```bash
nc 127.0.0.1 28081
```

Useful commands:
- `state` — current cursor / section / mode
- `events` — list events in the current section
- `type s` — simulate typing the swar key 's'
- `press backspace` — simulate pressing a key
- `thread-dump` — get a thread dump (works even if the UI is frozen)
- `help` — list all commands

This is mainly for troubleshooting; you don't need it for normal use. It is not exposed on the web app. Developers — see [docs/developer/architecture/debug-console.md](../developer/architecture/debug-console.md) for the full command catalog, protocol details, and recipes.

## What to read next

- [Keyboard Reference](08-keyboard-reference.md) — complete keymap
- [Taals & Raags](09-taals-raags.md) — built-in catalog
