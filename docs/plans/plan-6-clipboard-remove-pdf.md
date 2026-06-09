# Cut/Copy/Paste + Remove PDF Export

## Context

Two changes:
1. **Cut/Copy/Paste**: Add note selection (Shift+Arrow), clipboard operations (Ctrl+C/X/V), and toolbar buttons across all 4 modules. No selection mechanism exists today — must build from scratch.
2. **Remove PDF Export**: Delete all PDF-related code. HTML export exists; browsers can natively save HTML as PDF. PDFBox dependency (~3MB) removed.

### Design Decisions (confirmed with user)
- **Selection**: Shift+Arrow keys extend selection from cursor. Shift+Home/End to cycle boundaries. Ctrl+A selects all in section. No mouse selection.
- **Clipboard**: JSON on system clipboard with `"sangeet-clipboard": true` marker. Cross-session paste works.
- **Paste**: Insert at cursor, shift existing events rightward. Overflow wraps to next cycle.
- **Toolbar**: 3 icon buttons (Cut ✂, Copy 📋, Paste 📌) between Save As and HTML.

---

## Phase 0: Remove PDF Export

Delete/modify ~16 files. No dependencies on other phases — do this first.

### Delete entirely
- `sangeet-core/src/main/scala/com/varpas/sangeet/core/format/PdfExport.scala`
- `sangeet-core/src/test/scala/com/varpas/sangeet/core/format/PdfExportSpec.scala`
- Font file: check `sangeet-core/src/main/resources/` for `NotoSansDevanagari*.ttf` — delete if only used by PdfExport

### Modify (Scala)
- **`build.sbt`** — Remove `"org.apache.pdfbox" % "pdfbox" % "3.0.2"` dependency
- **`ExportApi.scala`** — Remove `exportPdf` method, remove PdfExport import. Only `exportHtml` remains.
- **`ExportEndpoints.scala`** — Remove `val pdf` endpoint definition. Update `val all` list.
- **`ExportRoutes.scala`** — Remove `val pdf` route. Update `val all` list.
- **`ExportRoutesSpec.scala`** — Remove PDF test block.
- **`MainApp.scala`** — Remove `pdfBtn` definition (lines 169-193), remove from toolbar items list (line 406), remove PdfExport import.

### Modify (Elm)
- **`Msg.elm`** — Remove `ExportPdf` variant (line 26)
- **`Update.elm`** — Remove `ExportPdf ->` case branch
- **`Api/Export.elm`** — Remove `PdfExportRequest` type alias and `exportPdf` function. Rename module exports to just `exportHtml`.
- **`Ports.elm`** — Remove `port exportPdf` (line 28), remove from module exposing list
- **`View/Toolbar.elm`** — Remove PDF button (line 32)

### Modify (JS/TS)
- **`ports.js`** — Remove entire PDF EXPORT section (lines 48-94)
- **`e2e/helpers/app-page.ts`** — Remove `pdfBtn` locator
- **`e2e/tests/file-operations.spec.ts`** — Remove PDF-related test lines

### Verification
- `sbt compile` — no PdfExport references remain
- `cd sangeet-web && elm make src/Main.elm` — no ExportPdf references
- `sbt test` — all tests pass (PDF tests deleted, not failing)

---

## Phase 1: Selection Model (sangeet-core)

### CursorModel changes (`editor/CursorModel.scala`)

Add an optional selection anchor to CursorModel:

```scala
case class CursorModel(
    taal: Taal,
    cycle: Int = 0,
    beat: Int = 0,
    subIndex: Int = 0,
    totalSubdivisions: Int = 1,
    currentOctave: Octave = Octave.Madhya,
    selectionAnchor: Option[BeatPosition] = None  // NEW
):
```

New methods:
- `startSelection: CursorModel` — sets `selectionAnchor = Some(position)` if not already set
- `clearSelection: CursorModel` — sets `selectionAnchor = None`
- `selectNextBeat: CursorModel` — calls `startSelection` then advances cursor (beat+1). Anchor stays, cursor moves.
- `selectPrevBeat: CursorModel` — calls `startSelection` then moves cursor back. Anchor stays.
- `selectToStart: CursorModel` — anchor at current pos (if not set), cursor to beat 0 cycle 0
- `selectToEnd(maxCycle: Int): CursorModel` — anchor at current pos (if not set), cursor to last beat of maxCycle
- `selectAll(maxCycle: Int): CursorModel` — anchor at (0,0), cursor at end
- `selectionRange: Option[(BeatPosition, BeatPosition)]` — returns `(min, max)` of anchor and current position, or None if no anchor
- `hasSelection: Boolean` — `selectionAnchor.isDefined`

Existing `nextBeat`/`prevBeat` must call `clearSelection` — plain navigation cancels selection (standard text-editor behavior).

### Verification
- `sbt sangeet-core/compile`
- New unit tests in `CursorModelSpec`: selection start/extend/clear, range computation, selectAll, boundary conditions

---

## Phase 2: Clipboard Data Model (sangeet-core)

### New file: `sangeet-core/.../editor/ClipboardData.scala`

```scala
case class ClipboardData(events: List[Event])
```

### New file: `sangeet-core/.../editor/ClipboardCodecs.scala`

Circe encoder/decoder for `ClipboardData`. Reuses existing `Event` codecs from `CompositionCodecs`. The encoder adds `"sangeet-clipboard": true` and `"version": "2.0"` markers. The decoder checks for the marker to validate clipboard content.

System clipboard JSON format:
```json
{
  "sangeet-clipboard": true,
  "version": "2.0",
  "events": [ ... ]
}
```

### Verification
- Roundtrip test: encode → decode ClipboardData with sample events

---

## Phase 3: Core Editor Logic (sangeet-core)

### CompositionEditor additions (`editor/CompositionEditor.scala`)

**`eventsInRange(start: BeatPosition, end: BeatPosition): List[Event]`**
- Filter `currentSection.events` where `position >= start && position <= end`, sorted by position

**`cutRange(start: BeatPosition, end: BeatPosition): (CompositionEditor, List[Event])`**
- Get events in range, remove them, shift subsequent events backward using existing `shiftEventBack`
- Return updated editor + cut events

**`pasteEvents(events: List[Event], atPosition: BeatPosition): CompositionEditor`**
- Rebase pasted events so first event lands at `atPosition`
- Shift existing events at/after `atPosition` forward by total duration of pasted events
- Add `shiftEventForward` / `shiftPositionForward` (symmetric to existing `shiftEventBack` / `shiftPositionBack`)
- Insert rebased events, sort by position

**Helper: `rebaseEvents(events: List[Event], newStart: BeatPosition): List[Event]`**
- Calculate offset from first event's position to newStart, apply to all positions

### EditorApi additions (`api/EditorApi.scala`)

**`copySelection(input: EditorInput): Either[ApiError, ClipboardResult]`**
**`cutSelection(input: EditorInput): Either[ApiError, ClipboardResult]`**
**`pasteClipboard(input: EditorInput, clipboardJson: String): Either[ApiError, EditorResult]`**

### New types

```scala
// api/ClipboardResult.scala
case class ClipboardResult(clipboardJson: String, composition: Composition, cursor: CursorModel, message: String)

// api/PasteInput.scala
case class PasteInput(composition: Composition, sectionIndex: Int, cursor: CursorModel, clipboardJson: String)
```

**`ApiError`** — add `EmptySelection` and `InvalidClipboard(message: String)`

### Verification
- Unit tests: eventsInRange, cutRange, pasteEvents, rebaseEvents
- Roundtrip: copy → serialize → paste → correct positions
- Edge cases: paste at end of cycle wraps, empty selection returns error

---

## Phase 4: Server Endpoints (sangeet-server)

### New endpoints (`endpoints/EditorEndpoints.scala`)
- `POST /api/v1/editor/copy-selection` → ClipboardResult
- `POST /api/v1/editor/cut-selection` → ClipboardResult
- `POST /api/v1/editor/paste-clipboard` (body includes clipboardJson) → EditorResult

### New routes (`routes/EditorRoutes.scala`)
Wire to EditorApi. Follow existing insertRest pattern.

### Verification
- Integration tests for each endpoint (success + error cases)

---

## Phase 5: Desktop UI (sangeet-desktop)

### Toolbar buttons (`MainApp.scala`)
3 buttons after saveAsBtn, before htmlBtn: Cut (✂), Copy (📋), Paste (📌)

### Keyboard handling (`EditorPane.scala`)
- `Ctrl+C` → copy selection to `javafx.scene.input.Clipboard` (replace current NoOp at line 667)
- `Ctrl+X` → cut selection to clipboard
- `Ctrl+V` → read clipboard, parse, paste
- `Shift+Arrow` → extend selection (in onKeyPressed, before plain arrow handling)
- `Shift+Home/End` → select to start/end
- `Ctrl+A` → select all

### Visual highlight
In grid renderer: draw semi-transparent blue overlay on selected beat range when `cursor.hasSelection`.

### Verification
- Launch app → type notes → Shift+Right → highlight visible → Ctrl+C → navigate → Ctrl+V → notes appear → Ctrl+Z undoes

---

## Phase 6: Web Frontend (sangeet-web)

### Model changes
- **`Model/Cursor.elm`** — Add `selectionAnchor : Maybe BeatPosition`, update encoder/decoder

### KeyHandler (`Input/KeyHandler.elm`)
New KeyAction variants: `SelectRight`, `SelectLeft`, `SelectToStart`, `SelectToEnd`, `SelectAll`, `CopySelection`, `CutSelection`, `PasteClipboard`

Wire: Ctrl+C/X/V/A in `mapCtrlKey`, Shift+Arrow in main dispatcher.

### Ports (`Ports.elm` + `ports.js`)
```elm
port copyToClipboard : String -> Cmd msg
port clipboardContent : (String -> msg) -> Sub msg
```

JS: `navigator.clipboard.writeText()` for copy, `document.addEventListener('paste', ...)` for paste.

### API client (`Api/Editor.elm`)
`copySelection`, `cutSelection`, `pasteClipboard` functions.

### State/Update
- `Msg.elm` — Add clipboard-related variants
- `Update.elm` — Handle copy/cut/paste/select actions, wire API calls and port commands
- Selection state (anchor) managed locally in cursor, no API call needed for Shift+Arrow

### Toolbar (`View/Toolbar.elm`)
Add Cut/Copy/Paste buttons in file group, after Save button, before HTML.

### Visual highlight (`View/GridRenderer.elm` + `styles.css`)
Add `.selected` CSS class to cells in selection range. Light-blue background.

### Verification
- `elm make src/Main.elm`, `elm-test`
- Browser: type notes → Shift+Right → Ctrl+C → navigate → Ctrl+V

---

## Phase 7: Tests

### Scala core
- `CursorModelSpec` — selection mechanics
- `ClipboardCodecsSpec` — roundtrip, invalid JSON, missing marker
- `CompositionEditorSpec` — eventsInRange, cutRange, pasteEvents

### Scala server
- `EditorRoutesSpec` — copy/cut/paste endpoints

### Elm
- `KeyHandlerTest` — Ctrl+C/X/V/A, Shift+Arrow mappings
- `UpdateEditorTest` — clipboard flow

### E2E
- `clipboard.spec.ts` — full copy/paste workflow
- Update `app-page.ts` — add button locators

---

## Implementation Order

1. Phase 0 — Remove PDF (independent cleanup)
2. Phase 1 — Selection model
3. Phase 2 — Clipboard data model
4. Phase 3 — Core editor logic
5. Phase 4 — Server endpoints
6. Phase 5 — Desktop UI
7. Phase 6 — Web frontend
8. Phase 7 — Tests

Each phase is independently compilable. Phases 1-3 are pure core. Phase 4 adds API. Phases 5-6 wire UI (parallelizable). Phase 7 adds comprehensive tests.

## Final Verification

1. `sbt compile` — no PDF references, all pattern matches exhaustive
2. `sbt test` — all Scala tests pass
3. `elm make src/Main.elm` + `elm-test` — Elm compiles and passes
4. Desktop: notes → Shift+Right → Ctrl+C → Ctrl+V → undo works
5. Web: same flow
6. Cross-session: copy → new session → paste works (JSON clipboard)
7. `make lint` + `make check-all` — CI green
