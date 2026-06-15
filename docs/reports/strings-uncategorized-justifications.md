# Uncategorized String Literals — Justifications

> Generated during Phase 16 triage. Lists string literals found by the heuristic scanner that are intentionally NOT in the UI strings catalog.

## Debug Protocol Response Strings (NOT user-facing UI)

**Location:** `DebugCommandHandler.scala`, `DebugConsole.scala`

These are internal response strings sent over the TCP/WebSocket debug protocol to MCP clients and test runners. They are NOT displayed in the app UI. Examples:

- "Cursor back: cycle=..." — TCP response
- "Subdivision set to $n" — TCP response
- "KanSwar mode: type note" — Debug mode indicator
- "No active tab" — Debug console response
- "ERROR: unknown command..." — Debug protocol error message

**Justification:** Debug protocol responses are part of the wire format contract between the debug console and external clients (MCP servers, test runners). They are not user-visible UI text.  They belong in the debug command handler, not the UI strings catalog.

**Count:** 38 strings across `DebugCommandHandler.scala` (13) and `DebugConsole.scala` (12), and debug mode indicators in `EditorKeyHandler.scala` (13)

## Log Messages (Internal Diagnostics)

**Location:** `AppLogger.scala`

- "Log file: $logPath" — Logged to console at startup, not displayed in UI

**Justification:** Internal logging output, not user-facing UI.

**Count:** 1 string

## Hard-coded Data Values (Not UI Labels)

**Location:** `NewCompositionDialog.scala`

- "Swar Files" — FileChooser filter name (OS-native dialog, not app UI)
- "Devanagari (Hindi)", "Kannada", "Telugu", "English" — Script picker values

**Justification:** These are data values for a dropdown and file filter. The dropdown itself is already labeled with catalog entries. The script names could be moved to the catalog, but they're more appropriately treated as data (like raag names / taal names) that might eventually come from a config file.

**Decision:** Leave as hardcoded data for now. If we add i18n later, revisit.

**Count:** 4 strings (1 file filter + 3 script names; "Devanagari (Hindi)" appears twice)

## Summary

| Category | Count | Action |
| -------- | ----- | ------ |
| Debug protocol responses | 38 | Leave in source (not UI) |
| Log messages | 1 | Leave in source (not UI) |
| Hard-coded data values | 4 | Leave in source (data, not labels) |
| **Legitimate UI to catalog** | **44** | **Add to catalog in next commit** |

**Total scanned:** 87 hits (81 desktop + 6 web)
**Non-UI (justified above):** 43
**Net UI to catalog:** 44
