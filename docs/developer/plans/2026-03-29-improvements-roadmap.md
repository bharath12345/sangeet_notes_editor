# Sangeet Notes Editor — Improvements Roadmap

**Date:** 2026-03-29
**Scope:** Features, performance, and test coverage improvements
**Baseline:** ~3,768 lines of production code, 23 test files, ~205 test cases

---

## Project Status Summary

The editor has a solid foundation: pure domain model, working canvas renderer, JSON serialization, basic MIDI playback, and HTML/PDF export. Core swar entry with ornaments, strokes, and multi-script support works well. However, the spec is roughly 40-50% implemented — many keyboard shortcuts, playback controls, and advanced features remain.

---

## Part A: Features to Add

### Priority 1 — Core Editing Gaps (High Impact, Moderate Effort)

#### A1. Undo/Redo System
**Why:** Single most impactful missing feature. Users lose work on mistakes; backspace only deletes the last note.

**Approach:** Immutable state history stack on `CompositionEditor`. Store snapshots (or diffs) of editor state. Cap at ~100 entries.

**Files:**
- Create: `src/main/scala/sangeet/editor/UndoHistory.scala`
- Modify: `EditorPane.scala` — Ctrl+Z / Ctrl+Shift+Z bindings
- Modify: `CompositionEditor.scala` — wrap mutations through history

**Key design:**
```scala
case class UndoHistory(
  past: List[CompositionEditor],    // most recent first
  present: CompositionEditor,
  future: List[CompositionEditor],  // for redo
  maxSize: Int = 100
)
```

---

#### A2. Missing Keyboard Shortcuts
**Why:** Many spec-defined shortcuts aren't wired up despite model support.

| Shortcut | Feature | Status |
|----------|---------|--------|
| `Tab` | Next beat (same as Right arrow) | Not bound |
| `Enter` (normal mode) | Next cycle | Only finishes ornaments |
| `Ctrl+2` through `Ctrl+8` | Set beat subdivision | Model exists, no binding |
| `Ctrl+C` | Chikari stroke | Model exists, no binding |
| `Ctrl+L` | Toggle sahitya editing mode | Not implemented |
| Double-tap (`ss`, `rr`) | Dual swar (auto-subdivide) | Not implemented |

**Files:**
- Modify: `EditorPane.scala` — add key bindings
- Modify: `KeyHandler.scala` — add double-tap detection (track last key + timestamp)
- Modify: `KeyboardLegend.scala` — update reference

---

#### A3. Section Management (Remove, Rename, Reorder)
**Why:** Users can add sections but can't reorganize or delete them.

**Approach:** Add right-click context menu on section headers in canvas, or menu items under "Composition" menu.

**Files:**
- Modify: `CompositionEditor.scala` — `removeSection(idx)`, `moveSection(from, to)`, `renameSection(idx, name)`
- Modify: `MainApp.scala` — menu items or context menu
- Modify: `EditorPane.scala` — right-click handler on section bounds

---

#### A4. Click-to-Place Cursor on Beat Cell
**Why:** Current click only switches sections. Users should be able to click a specific beat cell to place the cursor there.

**Approach:** Extend `SectionBounds` to include per-cell hit regions, or compute cell position from click X coordinate within the grid.

**Files:**
- Modify: `CanvasRenderer.scala` / `GridRenderer.scala` — return cell-level bounds
- Modify: `EditorPane.scala` — compute (cycle, beat) from click position

---

### Priority 2 — Playback & Audio (Medium Impact, Higher Effort)

#### A5. Playback Controls
**Why:** Only Play/Stop exist. Missing Pause, Loop, Play-from-cursor, Play-section, BPM slider.

**Approach:**
- Add a toolbar/control bar below menu with Play/Pause/Stop buttons and BPM slider
- `PlaybackController` already has state management; extend with pause/resume
- Loop = restart scheduler on completion
- Play-from-cursor = filter events to those at/after cursor position

**Files:**
- Create: `src/main/scala/sangeet/editor/PlaybackToolbar.scala`
- Modify: `PlaybackController.scala` — pause, resume, loop, startFrom(position)
- Modify: `MainApp.scala` — integrate toolbar

---

#### A6. Cursor Follows Playback
**Why:** During playback, users can't see which beat is currently playing.

**Approach:** `PlaybackController` emits current beat position via callback. `EditorPane` updates cursor position and redraws on each beat.

**Files:**
- Modify: `PlaybackController.scala` — add `onBeat: BeatPosition => Unit` callback
- Modify: `EditorPane.scala` — update cursor during playback

---

#### A7. Tabla Theka Accompaniment
**Why:** Spec requires optional tabla alongside melody. Taal objects already have `theka` data.

**Approach:** Second MIDI channel for percussion. Map theka bols to MIDI percussion notes (bass drum, hi-hat, etc.). Toggle via checkbox.

**Files:**
- Modify: `MidiEngine.scala` — add percussion channel support
- Modify: `PlaybackScheduler.scala` — schedule theka events
- Modify: `PlaybackToolbar.scala` — tabla toggle

---

### Priority 3 — Rendering & Export (Medium Impact, Medium Effort)

#### A8. PDF Export with Devanagari Glyphs
**Why:** Current PDF sanitizes to ASCII. Devanagari rendering is the entire point.

**Approach:** Embed Noto Sans Devanagari TTF in PDFBox. Render using the same glyph mapping as canvas. Draw ornament arcs as PDF vector paths.

**Files:**
- Add resource: `src/main/resources/fonts/NotoSansDevanagari-Regular.ttf`
- Modify: `PdfExport.scala` — embed font, render Devanagari, draw ornaments
- Modify: `build.sbt` — ensure font resource is included in JAR

---

#### A9. Mukhda/Pickup Beat Handling
**Why:** Many compositions start before sam. Currently no support for right-aligned pickup beats.

**Approach:** Add `pickupBeats: Option[Int]` to Section model. LineBreaker renders first line right-aligned with only the pickup beats. Sam gets a visual accent.

**Files:**
- Modify: `model/Section.scala` — add `pickupBeats` field
- Modify: `LineBreaker.scala` — right-align first line when pickup
- Modify: `GridRenderer.scala` — sam accent rendering
- Modify: `Codecs.scala` — serialize pickup

---

#### A10. Tihai Editor UI
**Why:** Data model supports tihais but no UI to mark them.

**Approach:** Ctrl+T enters tihai mode. User clicks start beat, then end beat. Tihai stored in composition. TihaiRenderer already draws brackets.

**Files:**
- Modify: `EditorPane.scala` — tihai marking mode
- Modify: `CompositionEditor.scala` — `addTihai(start, end)`
- Modify: `CanvasRenderer.scala` — pass tihais to TihaiRenderer

---

#### A11. Custom Taal Definition
**Why:** Spec says taals should be data, not hardcoded. Users may need taals not in the built-in set.

**Approach:** Add "Custom Taal..." dialog. Store custom taals in user config directory (`~/.sangeet/taals/`). Load on startup alongside built-in taals.

**Files:**
- Create: `src/main/scala/sangeet/editor/CustomTaalDialog.scala`
- Create: `src/main/scala/sangeet/taal/TaalLoader.scala` — JSON file loader
- Modify: `Taals.scala` — merge built-in + user-defined

---

### Priority 4 — Nice-to-Have (Lower Impact)

#### A12. Right-Click Context Menu
**Why:** Spec mentions context menu for ornaments, octave, stroke on right-click.

#### A13. Sahitya Editing Mode (Ctrl+L)
**Why:** Users should be able to enter lyrics aligned to beats without re-creating the composition.

#### A14. Additional Sample .swar Files
**Why:** Spec mentions 3 samples; only 1 exists. Useful for testing and demos.

#### A15. Drag-to-Create Meend
**Why:** More intuitive than keyboard shortcut for connecting two notes with a meend arc.

---

## Part B: Performance, Responsiveness & Code Quality

### B1. Cursor Blink Optimization — Partial Canvas Redraw
**Problem:** Every 530ms blink redraws the entire canvas (all sections, all notes, all ornaments).

**Fix:** Track cursor region bounds. On blink toggle, only clear and redraw the cursor rectangle area. Fall back to full redraw only when composition content changes.

**Files:**
- Modify: `EditorPane.scala` — store cursor bounds from last render
- Modify: `GridRenderer.scala` — return cursor region coordinates

**Impact:** Eliminates ~2 full canvas redraws/second for idle blinking.

---

### B2. Canvas Size Auto-Adjustment
**Problem:** Canvas is hardcoded at 1100x2000. Large compositions may overflow; small ones waste space.

**Fix:** After layout, compute actual content height. Resize canvas and canvasHolder to fit content + padding.

**Files:**
- Modify: `EditorPane.scala` — set canvas height after render
- Modify: `CanvasRenderer.scala` — return total content height

---

### B3. Lazy Section Rendering
**Problem:** All sections render even if only one is visible in the scroll viewport.

**Fix:** Check scroll position against section Y bounds. Only render sections that overlap the visible viewport. Render placeholders for off-screen sections (just section name + height reservation).

**Files:**
- Modify: `CanvasRenderer.scala` — accept viewport bounds, skip off-screen sections
- Modify: `EditorPane.scala` — pass scroll viewport to renderer

**Impact:** Significant for compositions with 10+ sections (Gat + many taans).

---

### B4. DevanagariMap Thread Safety
**Problem:** Uses `@volatile var` for runtime script switching. Works on JavaFX thread but isn't enforced.

**Fix:** Replace volatile var with an explicit `currentScript` parameter threaded through the render pipeline, eliminating global mutable state.

**Files:**
- Modify: `DevanagariMap.scala` — make methods accept `SwarScript` parameter
- Modify: `SwarGlyph.scala`, `GridRenderer.scala` — pass script through
- Remove: `@volatile var _script`

---

### B5. Codecs.scala Decomposition
**Problem:** At 416 lines, it's the largest file. Mixing all codec concerns.

**Fix:** Split into:
- `ModelCodecs.scala` — Note, Variant, Octave, Stroke, Rational, BeatPosition
- `OrnamentCodecs.scala` — all ornament types
- `CompositionCodecs.scala` — Metadata, Section, Composition, Taal, Raag

**Files:**
- Create: `format/ModelCodecs.scala`, `format/OrnamentCodecs.scala`, `format/CompositionCodecs.scala`
- Delete: `format/Codecs.scala`
- Modify: `format/SwarFormat.scala` — import from new locations

---

### B6. Extract Event.beat Accessor
**Problem:** `CompositionEditor.maxCycle` uses a verbose match to access `beat.cycle` from each Event variant.

**Fix:** Add an extension method or accessor:
```scala
extension (e: Event)
  def beat: BeatPosition = e match
    case s: Event.Swar    => s.beat
    case r: Event.Rest    => r.beat
    case u: Event.Sustain => u.beat
```

**Files:**
- Modify: `model/Event.scala` — add extension
- Simplify: `CompositionEditor.scala`, any other places that match on Event variants

---

### B7. OrnamentRenderer DRY Cleanup
**Problem:** Minor duplication in arc+label drawing patterns across ornament types.

**Fix:** Extract `drawArcWithLabel(gc, x, y, width, color, label)` helper. Reduces ~30 lines.

**Files:**
- Modify: `render/OrnamentRenderer.scala`

---

### B8. Remove SanitySpec
**Problem:** `SanitySpec` (1+1=2) has no value.

**Fix:** Delete it.

---

## Part C: Test Coverage Improvements

### Current State

| Module | Source Lines | Test Cases | Coverage |
|--------|------------|------------|----------|
| model/ | 230 | ~50 | 95% |
| format/ | 859 | ~40 | 90% |
| layout/ | 120 | ~24 | 100% |
| render/ | 667 | ~33 | 30% |
| audio/ | 143 | ~4 | 25% |
| editor/ | 1,700 | ~39 | 20% |
| taal/raag | 205 | ~28 | 100% |

**Total untested code: ~1,330 lines (35%)** — mostly UI components, renderers, and audio.

### C1. Testing Framework Options for ScalaFX/JavaFX UI

#### Option 1: TestFX (Recommended for Integration Tests)
**Library:** `org.testfx:testfx-core:4.0.18` + `org.testfx:testfx-junit5:4.0.18`

**What it does:** Programmatically drives a real JavaFX stage. Can click buttons, type text, verify UI state. Works headlessly with Monocle (no display needed for CI).

**Best for:**
- Dialog flows (NewCompositionDialog, CompositionPropertiesDialog)
- Menu interactions (File > New, File > Save)
- Full keyboard input → canvas output verification
- End-to-end editing workflows

**Setup:**
```scala
// build.sbt
"org.testfx" % "testfx-core" % "4.0.18" % Test,
"org.testfx" % "testfx-junit5" % "4.0.18" % Test,
"org.testfx" % "openjfx-monocle" % "jdk-12.0.1+2" % Test  // headless
```

**Limitation:** Slower than unit tests (~1-3 sec per test). Requires JavaFX runtime initialization.

---

#### Option 2: Snapshot/Golden File Testing (Recommended for Renderers)
**Approach:** Render to an off-screen canvas, capture as image, compare against golden files (pixel-by-pixel or perceptual hash).

**What it does:** Verifies visual correctness of GridRenderer, OrnamentRenderer, SwarGlyph without manual inspection.

**Best for:**
- Regression testing for renderer changes
- Verifying ornament visuals (gamak wavy, meend arc, kan superscript)
- Verifying grid layout (vibhag markers, stroke/sahitya rows)
- Multi-script rendering correctness

**Setup:**
```scala
// In test
val canvas = new Canvas(800, 200)
GridRenderer.drawSection(canvas.graphicsContext2D, grid, config, 0, 0, ...)
val snapshot = canvas.snapshot(null, null)  // WritableImage
// Compare against saved golden PNG
```

**Files:**
- Create: `src/test/scala/sangeet/render/SnapshotTestUtil.scala` — helper for capture + compare
- Create: `src/test/resources/golden/` — reference images
- Create: `GridRendererSnapshotSpec.scala`, `OrnamentRendererSnapshotSpec.scala`

**Limitation:** Golden files need regeneration when rendering intentionally changes. Pixel comparison can be brittle across platforms; use perceptual diff with tolerance.

---

#### Option 3: Headless Canvas Unit Tests (Recommended for Logic-Heavy Renderers)
**Approach:** Initialize JavaFX toolkit in test, create off-screen canvas, assert on graphics context state (verify methods were called with correct parameters).

**What it does:** Tests rendering logic without visual comparison. Faster than snapshot tests.

**Best for:**
- Verifying cursor position calculations
- Verifying section bounds computation
- Verifying line breaking decisions translate to correct Y positions

**Setup:**
```scala
// One-time JavaFX init for tests
object JavaFXInit:
  private var initialized = false
  def ensure(): Unit = synchronized {
    if !initialized then
      new javafx.embed.swing.JFXPanel() // initializes toolkit
      initialized = true
  }
```

---

### C2. Specific Test Improvements

#### C2.1 Renderer Tests (Priority: High)
**Current:** 0 tests for GridRenderer (246 lines), OrnamentRenderer (195 lines)

**Add:**
- `GridRendererSpec` — snapshot tests for:
  - Empty section rendering
  - Section with single note
  - Full Teentaal cycle
  - Stroke line and sahitya line visibility
  - Active vs inactive section styling
  - Cursor rendering at various positions
- `OrnamentRendererSpec` — snapshot tests for all 10 ornament types
- `SwarGlyphSpec` — verify glyph + octave dot + komal/tivra mark rendering

**Files:**
- Create: `src/test/scala/sangeet/render/GridRendererSpec.scala`
- Create: `src/test/scala/sangeet/render/OrnamentRendererSpec.scala`
- Create: `src/test/scala/sangeet/render/SwarGlyphSpec.scala`

---

#### C2.2 Audio Tests (Priority: Medium)
**Current:** 4 tests for PlaybackScheduler only. 0 tests for MidiEngine, PlaybackController.

**Add:**
- `MidiEngineSpec` — verify note-to-MIDI mapping for all 7 notes x 3 variants x 3 octaves (63 mappings)
- `PlaybackControllerSpec` — mock SoundEngine, verify scheduling timing
- `PlaybackSchedulerSpec` — expand: empty events, large compositions, various BPMs, sub-beat precision

**Files:**
- Create: `src/test/scala/sangeet/audio/MidiEngineSpec.scala`
- Create: `src/test/scala/sangeet/audio/PlaybackControllerSpec.scala`
- Modify: `src/test/scala/sangeet/audio/PlaybackSchedulerSpec.scala`

---

#### C2.3 Editor Integration Tests with TestFX (Priority: Medium)
**Current:** 0 UI tests.

**Add:**
- `NewCompositionFlowSpec` — TestFX test: open dialog, fill fields, click OK, verify composition created
- `KeyboardInputFlowSpec` — TestFX test: type swar keys, verify events added, cursor advances
- `OrnamentInputFlowSpec` — TestFX test: Ctrl+G adds gamak, Ctrl+K then note adds kan swar
- `FileIOFlowSpec` — TestFX test: new → edit → save → close → open → verify content

**Files:**
- Create: `src/test/scala/sangeet/editor/NewCompositionFlowSpec.scala`
- Create: `src/test/scala/sangeet/editor/KeyboardInputFlowSpec.scala`
- Create: `src/test/scala/sangeet/editor/OrnamentInputFlowSpec.scala`
- Create: `src/test/scala/sangeet/editor/FileIOFlowSpec.scala`

---

#### C2.4 PDF Export Content Tests (Priority: Medium)
**Current:** 1 test (file exists and non-zero size). No content validation.

**Add:**
- Extract text from generated PDF using PDFBox `PDFTextStripper`
- Verify: title present, section names present, raag metadata present
- Verify: page count for multi-section compositions
- Verify: page breaks between sections

**Files:**
- Modify: `src/test/scala/sangeet/format/PdfExportSpec.scala`

---

#### C2.5 Edge Case and Error Tests (Priority: Low-Medium)
**Current:** Almost no error/edge case tests.

**Add:**
- `CodecsEdgeCaseSpec` — malformed JSON, missing fields, unknown enum values, backward compat
- `CompositionEditorEdgeCaseSpec` — empty sections, max cycle bounds, section switching
- `CursorModelEdgeCaseSpec` — different taals (Rupak 7, Dadra 6), extreme positions
- `LineBreakerMultiTaalSpec` — all 11 taals, not just Teentaal

**Files:**
- Create: `src/test/scala/sangeet/format/CodecsEdgeCaseSpec.scala`
- Create: `src/test/scala/sangeet/editor/CompositionEditorEdgeCaseSpec.scala`
- Create: `src/test/scala/sangeet/editor/CursorModelEdgeCaseSpec.scala`
- Create: `src/test/scala/sangeet/layout/LineBreakerMultiTaalSpec.scala`

---

### C3. CI/Testing Infrastructure

#### C3.1 Headless JavaFX for CI
**Problem:** TestFX and snapshot tests need JavaFX, which needs a display.

**Fix:** Use Monocle (headless JavaFX backend) in CI. Add to `build.sbt`:
```scala
Test / javaOptions ++= Seq(
  "-Dtestfx.robot=glass",
  "-Dglass.platform=Monocle",
  "-Dmonocle.platform=Headless",
  "-Dprism.order=sw"
)
```

#### C3.2 Golden File Management
**Problem:** Snapshot tests produce golden files that differ across platforms/JDKs.

**Fix:**
- Generate goldens per-platform (`golden/macos/`, `golden/linux/`)
- Use perceptual hash comparison with 2% tolerance
- Script to regenerate: `sbt "testOnly *Snapshot* -- -Dregenerate=true"`

---

## Implementation Priority Matrix

| Item | Impact | Effort | Priority |
|------|--------|--------|----------|
| A1. Undo/Redo | Critical | Medium | P0 |
| A2. Missing shortcuts | High | Low | P0 |
| A4. Click-to-place cursor | High | Low | P0 |
| B1. Cursor blink optimization | Medium | Low | P1 |
| B6. Event.beat extension | Low | Low | P1 |
| A3. Section management | High | Medium | P1 |
| C2.1 Renderer snapshot tests | High | Medium | P1 |
| C2.5 Edge case tests | Medium | Low | P1 |
| A5. Playback controls | High | High | P2 |
| A8. PDF Devanagari | High | High | P2 |
| C2.2 Audio tests | Medium | Medium | P2 |
| C2.3 TestFX UI tests | Medium | High | P2 |
| B2. Canvas auto-size | Medium | Low | P2 |
| B3. Lazy section rendering | Medium | Medium | P3 |
| B4. DevanagariMap thread safety | Low | Low | P3 |
| B5. Codecs decomposition | Low | Medium | P3 |
| A6. Cursor follows playback | Medium | Medium | P3 |
| A7. Tabla accompaniment | Medium | High | P3 |
| A9. Mukhda handling | Medium | High | P3 |
| A10. Tihai editor | Medium | Medium | P3 |
| A11. Custom taal | Medium | Medium | P3 |
| C2.4 PDF content tests | Medium | Low | P3 |

---

## Dependencies

```
A1 (Undo) ← A2 (Shortcuts) — undo should be in place before adding more shortcuts
A5 (Playback) ← A6 (Cursor follow) — need controls before cursor sync
A5 (Playback) ← A7 (Tabla) — need controls before tabla toggle
A8 (PDF fonts) ← C2.4 (PDF tests) — embed fonts before testing PDF content
C3.1 (Headless) ← C2.1 (Snapshot) — need CI setup before snapshot tests
C3.1 (Headless) ← C2.3 (TestFX) — need CI setup before UI tests
```
