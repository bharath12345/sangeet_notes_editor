# Plan 5: Chikari as Event + Compact .swar Format + Legend Font Size

## Context

Three changes requested:

1. **Chikari is a swar, not a stroke.** Currently `Stroke.Chikari` is treated like Da/Ra — a plucking direction. But chikari (open strings on sitar) is a distinct sound event, very frequent in Jhala. The user wants: press `1` in swar mode → insert chikari. Display "1" in swar row, "ची" in Da/Ra row. Remove Chikari from the Stroke enum entirely.

2. **Compact .swar files.** Currently `writeFile` uses `json.spaces2` (pretty-printed). Files are unnecessarily large. Switch to `json.noSpaces`. Bump version from "1.0" to "2.0". Keep reading both versions.

3. **Larger keyboard legend fonts.** Text is too small. Increase all font sizes by 2px in both desktop (KeyboardLegend.scala) and web (styles.css).

## Plan

### 1. Model changes

**`sangeet-core/.../model/Stroke.scala`** — Remove `Chikari`. Result: `enum Stroke: case Da, Ra, Jod`

**`sangeet-core/.../model/Event.scala`** — Add `case Chikari(beat: BeatPosition, duration: Rational)`. Extend `position` and `eventDuration` pattern matches.

### 2. JSON codecs + .swar format

**`sangeet-core/.../format/SwarFormat.scala`**
- `currentVersion = "2.0"`, `supportedVersions = Set("1.0", "2.0")`
- `writeFile`: change `json.spaces2` to `json.noSpaces`
- `fromJson` already accepts any version (lenient validation)

**`sangeet-core/.../format/CompositionCodecs.scala`**
- Add encoder/decoder for `Event.Chikari` with `"type": "chikari"`
- Backward compat: Swar stroke decoder must handle `"stroke": "chikari"` from v1.0 files → map to `stroke = None`

**`sangeet-core/.../format/ModelCodecs.scala`** — Stroke enum auto-derives from values; removing `Chikari` auto-updates the encoder. No code change needed.

### 3. Render helpers

**`sangeet-core/.../render/GlyphMetrics.scala`**
- Remove `Stroke.Chikari` from `strokeText`
- Add: `def chikariSwarText: String = "1"`
- Add: `def chikariStrokeText(script: SwarScript): String = if script == English then "Ch" else "ची"`

### 4. Editor logic

**`sangeet-core/.../editor/KeyHandler.scala`** — Add `handleChikariKey(editor)` returning `(CompositionEditor, String)`. Creates `Event.Chikari` at cursor position, advances cursor.

**`sangeet-core/.../editor/CompositionEditor.scala`** — Add `Chikari` case to `shiftEventBack` and `changeTaal` pattern matches (copy with new beat position).

### 5. API + Server

**`sangeet-core/.../api/EditorApi.scala`** — Add `insertChikari(input): Either[ApiError, EditorResult]`

**`sangeet-server/.../endpoints/EditorEndpoints.scala`** — Add `insertChikari` endpoint definition

**`sangeet-server/.../routes/EditorRoutes.scala`** — Add `insertChikari` route (same pattern as `insertRest`)

### 6. Desktop rendering

**`sangeet-desktop/.../render/GridRendererFX.scala`** — In event rendering loop, add `Event.Chikari` case: draw "1" in swar row, "ची" in stroke row. Do NOT increment swarCounter (preserves Da/Ra alternation for surrounding swars).

**`sangeet-desktop/.../render/SwarGlyphRenderer.scala`** — Add `drawChikari` and `drawChikariStroke` methods.

### 7. Desktop editor

**`sangeet-desktop/.../editor/EditorPane.scala`**
- Add `1` key in `onKeyTyped` handler → `KeyHandler.handleChikariKey`. Place before the unknown-key fallback. Set `groupingState = None`.
- Remove `Ctrl+C → Stroke.Chikari` binding

### 8. Desktop debug + legend

**`sangeet-desktop/.../editor/DebugCommandHandler.scala`** — Remove `"chikari"` case from stroke command
**`sangeet-desktop/.../editor/DebugConsole.scala`** — Update help text: `"da, ra, jod"` not `"da, ra, chikari, jod"`
**`sangeet-desktop/.../editor/KeyboardLegend.scala`** — Remove `Ctrl+C → Chikari stroke` entry, add `1 → Chikari` in Special section

### 9. Export rendering

**`sangeet-core/.../format/HtmlExport.scala`** — Add Chikari to swar row rendering ("1") and stroke row rendering ("ची"). Don't increment swarCounter.

**`sangeet-core/.../format/PdfExport.scala`** — Same pattern as HtmlExport.

### 10. Elm web frontend

**`sangeet-web/src/Model/Types.elm`** — Remove `Chikari` from `Stroke` type, decoder, encoder

**`sangeet-web/src/Model/Event.elm`** — Add `ChikariEvent { beat, duration }` with encoder/decoder for `"type": "chikari"`

**`sangeet-web/src/Input/KeyHandler.elm`** — Remove `StrokeChikari` from `KeyAction`, add `InsertChikari`. Map `"1"` → `InsertChikari` in `mapPlainKey`.

**`sangeet-web/src/Api/Editor.elm`** — Add `insertChikari` function (POST to `/editor/insert-chikari`)

**`sangeet-web/src/State/Update.elm`** — Wire `InsertChikari` → API call, remove `StrokeChikari` handler

**`sangeet-web/src/View/GridRenderer.elm`** — Add `ChikariEvent` rendering: "1" in swar, stroke text in stroke row. Remove `Chikari` from `strokeToString`.

### 11. Keyboard legend font sizes (+2px everywhere)

**`sangeet-desktop/.../editor/KeyboardLegend.scala`**
- Title: 13→15px, subtitle: 10→12px, headings: 12→14px, entries: 11→13px

**`sangeet-web/public/styles.css`**
- `.legend-title`: 15→17px, `.legend-section-title`: 13→15px, `.legend-table`: 12→14px, `.legend-key kbd`: 11→13px

### 12. Tests

Update exhaustive pattern matches and specific tests:
- `EditorStressSpec` — rewrite "attach Chikari and Jod strokes" → test chikari insertion + keep Jod stroke test
- `GlyphMetricsSpec` — remove `Stroke.Chikari` assertions, add chikari display tests
- `StrokeRoutesSpec` — remove chikari stroke test, add insert-chikari endpoint test
- `DebugConsoleTcpSpec` — remove `stroke chikari` test
- `CodecsSpec` / `CompositionCodecSpec` — add backward compat test (v1.0 with `"stroke":"chikari"` decodes to `stroke=None`), add `Event.Chikari` round-trip test, add compact format test
- Elm tests — update `UpdateEditorTest` (remove StrokeChikari test, add InsertChikari test), update `KeyHandlerTest` (test `1` key)

## Verification

1. `sbt compile` — all pattern matches exhaustive, no Stroke.Chikari references remain
2. `sbt test` — all Scala tests pass including new chikari + codec backward compat tests
3. `cd sangeet-web && elm make src/Main.elm` — Elm compiles with no Chikari stroke references
4. `cd sangeet-web && ./node_modules/.bin/elm-test` — Elm tests pass
5. Launch desktop app → press `1` → "1" appears in swar row, "ची" in stroke row
6. Open an existing v1.0 .swar file → loads without error
7. Save → file is compact JSON with `"version": "2.0"`
8. Keyboard legend text is visibly larger
9. `make lint` — formatting checks pass after running `make format`
