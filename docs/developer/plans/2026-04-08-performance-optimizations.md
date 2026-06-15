# Performance Optimizations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate performance bottlenecks in the notation editor — primarily redundant layout computation on every render, font object churn in render loops, UI-blocking auto-save, and excessive playback task scheduling.

**Architecture:** Cache layout results keyed by composition identity so cursor-only moves skip recomputation. Replace `def` font methods with cached `val`s. Debounce auto-save onto a background thread. Batch playback scheduling with a tick-based approach.

**Tech Stack:** Scala 3, ScalaFX, java.util.concurrent, java.util.Timer

---

### Task 1: Cache Font Instances in SwarGlyph

**Files:**
- Modify: `src/main/scala/sangeet/render/SwarGlyph.scala:10-11`

The `swarFont` and `smallFont` are `def` methods that create new `Font` objects on every call. Each note glyph draw creates a new Font. Change to lazy vals that update when script changes.

- [ ] **Step 1: Change def to lazy val with script-aware invalidation**

In `SwarGlyph.scala`, the current font "name" comes from `DevanagariMap.fontName` which reads a mutable `_script` var. Since font name can change at runtime (script switching), we need to track the current script and rebuild fonts only when it changes.

Replace lines 10-11:
```scala
private def swarFont: Font = Font(DevanagariMap.fontName, 16)
private def smallFont: Font = Font(DevanagariMap.fontName, 10)
```

With:
```scala
private var _cachedScript: String = ""
private var _swarFont: Font = _
private var _smallFont: Font = _

private def swarFont: Font =
  val name = DevanagariMap.fontName
  if name != _cachedScript then
    _cachedScript = name
    _swarFont = Font(name, 16)
    _smallFont = Font(name, 10)
  _swarFont

private def smallFont: Font =
  val name = DevanagariMap.fontName
  if name != _cachedScript then
    _cachedScript = name
    _swarFont = Font(name, 16)
    _smallFont = Font(name, 10)
  _smallFont
```

- [ ] **Step 2: Run tests**

Run: `sbt test`
Expected: All 284 tests pass. Font caching is transparent — no API change.

- [ ] **Step 3: Commit**

```bash
git add src/main/scala/sangeet/render/SwarGlyph.scala
git commit -m "perf: cache Font instances in SwarGlyph to avoid per-glyph allocation"
```

---

### Task 2: Cache Font Instances in OrnamentRenderer

**Files:**
- Modify: `src/main/scala/sangeet/render/OrnamentRenderer.scala`

Every ornament draw method creates inline `Font(...)` objects. Extract them as cached vals.

- [ ] **Step 1: Extract font vals at object level**

Add cached font vals at the top of `OrnamentRenderer` object (after line 11):
```scala
private val devaFont9 = Font("Noto Sans Devanagari", 9)
private val devaFont8 = Font("Noto Sans Devanagari", 8)
private val devaFont7 = Font("Noto Sans Devanagari", 7)
private val italicFont8 = Font("System Italic", 8)
```

Then replace all inline Font constructors:
- Line 55: `Font("Noto Sans Devanagari", 9)` → `devaFont9`
- Line 101: `Font("System Italic", 8)` → `italicFont8`
- Line 119: `Font("Noto Sans Devanagari", 8)` → `devaFont8`
- Line 137: `Font("Noto Sans Devanagari", 7)` → `devaFont7`
- Line 159: `Font("Noto Sans Devanagari", 7)` → `devaFont7`
- Line 173: `Font("Noto Sans Devanagari", 7)` → `devaFont7`
- Line 183: `Font("Noto Sans Devanagari", 8)` → `devaFont8`
- Line 194: `Font("System Italic", 8)` → `italicFont8`

- [ ] **Step 2: Run tests**

Run: `sbt test`
Expected: All tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/main/scala/sangeet/render/OrnamentRenderer.scala
git commit -m "perf: cache Font instances in OrnamentRenderer"
```

---

### Task 3: Cache Font in GridRenderer sahitya line

**Files:**
- Modify: `src/main/scala/sangeet/render/GridRenderer.scala:251`

Line 251 creates `Font("Noto Sans Devanagari", 11)` inside the per-cell loop.

- [ ] **Step 1: Extract font val**

Add at the top of `GridRenderer` object (after line 13):
```scala
val sahityaFont = Font("Noto Sans Devanagari", 11)
```

Replace line 251:
```scala
gc.font = Font("Noto Sans Devanagari", 11)
```
with:
```scala
gc.font = sahityaFont
```

- [ ] **Step 2: Run tests**

Run: `sbt test`
Expected: All tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/main/scala/sangeet/render/GridRenderer.scala
git commit -m "perf: cache sahitya Font in GridRenderer"
```

---

### Task 4: Layout Caching — Separate Layout from Cursor in CanvasRenderer

**Files:**
- Modify: `src/main/scala/sangeet/render/CanvasRenderer.scala`
- Modify: `src/main/scala/sangeet/editor/EditorPane.scala`

Currently `CanvasRenderer.render()` calls `GridLayout.layoutAll()` on every render (including cursor blinks). Layout only changes when the composition's events change, not on cursor moves. Cache the layout result in EditorPane and pass it to the renderer.

- [ ] **Step 1: Add layout parameter to CanvasRenderer.render**

Modify `CanvasRenderer.render` to accept pre-computed grids instead of computing them internally.

Change the signature and body in `CanvasRenderer.scala`:
```scala
def render(canvas: Canvas, composition: Composition, grids: List[SectionGrid], config: LayoutConfig,
           cursorPos: Option[(Int, Int, Int)] = None,
           cursorVisible: Boolean = true,
           strokeEditMode: Boolean = false): List[SectionBounds] =
```

Remove the line that computes grids:
```scala
val grids = GridLayout.layoutAll(composition, config)
```

The rest of the method stays the same — it already uses `grids`.

- [ ] **Step 2: Cache layout in EditorPane**

In `EditorPane.scala`, add a cached grids field and invalidation logic:

Add after `private var readOnly: Boolean = false` (line 47):
```scala
private var cachedGrids: Option[(Composition, List[sangeet.layout.SectionGrid])] = None

private def getGrids(comp: Composition): List[sangeet.layout.SectionGrid] =
  cachedGrids match
    case Some((cached, grids)) if cached eq comp => grids
    case _ =>
      val grids = sangeet.layout.GridLayout.layoutAll(comp, config)
      cachedGrids = Some((comp, grids))
      grids
```

Note: Uses reference equality (`eq`) — since `Composition` is a case class and we create new instances on each edit, this correctly invalidates when events change. Cursor-only moves via `setEditorDirect` reuse the same composition object.

Update `redraw()` to pass cached grids:
```scala
def redraw(): Unit =
  editor.foreach { ed =>
    val strokeEditMode = editMode == EditMode.StrokeEdit
    val grids = getGrids(ed.composition)
    sectionBounds = CanvasRenderer.render(canvas, ed.composition, grids, config,
      Some(ed.currentSectionIndex, ed.cursor.cycle, ed.cursor.beat), cursorVisible, strokeEditMode)
    val contentHeight = sectionBounds.lastOption.map(_.endY + 40).getOrElse(200.0)
    val minHeight = scrollPane.height.value.max(400)
    val newHeight = contentHeight.max(minHeight)
    if Math.abs(canvas.height.value - newHeight) > 10 then
      canvas.height = newHeight
      canvasHolder.prefHeight = newHeight
      sectionBounds = CanvasRenderer.render(canvas, ed.composition, grids, config,
        Some(ed.currentSectionIndex, ed.cursor.cycle, ed.cursor.beat), cursorVisible, strokeEditMode)
  }
```

- [ ] **Step 3: Add import for SectionGrid in EditorPane**

Add to imports in EditorPane.scala:
```scala
import sangeet.layout.{LayoutConfig, SectionGrid, GridLayout}
```

Also add import in CanvasRenderer.scala if not present:
```scala
import sangeet.layout.SectionGrid
```

- [ ] **Step 4: Run tests**

Run: `sbt test`
Expected: All tests pass. The `GridLayoutSpec` tests don't test `CanvasRenderer` directly.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/sangeet/render/CanvasRenderer.scala src/main/scala/sangeet/editor/EditorPane.scala
git commit -m "perf: cache layout computation, skip recomputation on cursor-only moves"
```

---

### Task 5: Debounced Background Auto-Save

**Files:**
- Modify: `src/main/scala/sangeet/editor/EditorPane.scala`

Currently `autoSave()` calls `SwarFormat.writeFile()` synchronously on the JavaFX UI thread on every keystroke. This blocks rendering. Debounce it and run on a background thread.

- [ ] **Step 1: Add timer and executor for debounced save**

Add imports at the top of `EditorPane.scala`:
```scala
import java.util.{Timer, TimerTask}
import java.util.concurrent.Executors
```

Add fields after the `doubleTapThresholdMs` field:
```scala
// Debounced auto-save: saves 500ms after last edit, on background thread
private val saveExecutor = Executors.newSingleThreadExecutor(r => {
  val t = new Thread(r, "auto-save")
  t.setDaemon(true)
  t
})
private var saveTimer: Option[TimerTask] = None
private val saveTimerScheduler = new Timer("auto-save-timer", true)
```

- [ ] **Step 2: Replace autoSave with debounced version**

Replace the existing `autoSave()` method:
```scala
/** Auto-save current composition to its file path (debounced, background thread). */
private def autoSave(): Unit =
  // Cancel any pending save
  saveTimer.foreach(_.cancel())
  for
    ed <- editor
    path <- currentFilePath
  do
    val comp = ed.composition
    val task = new TimerTask:
      def run(): Unit =
        saveExecutor.submit(new Runnable:
          def run(): Unit =
            try SwarFormat.writeFile(path, comp)
            catch case _: Exception => ()
        )
    saveTimer = Some(task)
    saveTimerScheduler.schedule(task, 500L)
```

Note: We capture `comp` and `path` by value before scheduling — this avoids race conditions where the composition changes between scheduling and execution.

- [ ] **Step 3: Run tests**

Run: `sbt test`
Expected: All tests pass. Auto-save is not directly tested (it's a side effect).

- [ ] **Step 4: Commit**

```bash
git add src/main/scala/sangeet/editor/EditorPane.scala
git commit -m "perf: debounce auto-save (500ms) and run on background thread"
```

---

### Task 6: Batch Playback Scheduling

**Files:**
- Modify: `src/main/scala/sangeet/audio/PlaybackController.scala`

Currently creates 2 `ScheduledExecutorService` tasks per note (noteOn + noteOff). For a 100-note piece = 200 queued tasks all at once. Use a single timer thread that processes batches.

- [ ] **Step 1: Refactor to windowed scheduling**

Replace the `play` method in `PlaybackController.scala`:
```scala
def play(events: List[Event], bpm: Double, matras: Int): Unit =
  if playing then stop()
  engine.init()
  playing = true

  val timedNotes = PlaybackScheduler.schedule(events, bpm, matras).sortBy(_.timeMs)
  if timedNotes.isEmpty then return

  // Schedule a single tick thread that processes notes in time windows
  val noteArray = timedNotes.toArray
  var noteIdx = 0
  val startTime = System.currentTimeMillis()

  executor.submit(new Runnable { def run(): Unit =
    while playing && noteIdx < noteArray.length do
      val now = System.currentTimeMillis() - startTime
      // Process all notes whose time has arrived
      while noteIdx < noteArray.length && noteArray(noteIdx).timeMs <= now do
        val tn = noteArray(noteIdx)
        if playing then
          engine.playNote(tn)
          // Schedule noteOff relative to now
          executor.schedule(
            new Runnable { def run(): Unit =
              if playing then
                val midi = engine match
                  case m: MidiEngine => m.toMidiNote(tn.note, tn.variant, tn.octave)
                  case _ => 60
                engine.noteOff(midi)
            },
            tn.durationMs,
            TimeUnit.MILLISECONDS
          )
        noteIdx += 1
      // Sleep briefly before checking again (1ms resolution)
      if playing && noteIdx < noteArray.length then
        val nextTime = noteArray(noteIdx).timeMs
        val sleepMs = math.max(1, nextTime - (System.currentTimeMillis() - startTime))
        Thread.sleep(math.min(sleepMs, 10))
  })
```

This replaces N×2 scheduled tasks with 1 polling thread + N noteOff tasks (which are spread over time rather than all queued at once).

- [ ] **Step 2: Run tests**

Run: `sbt test`
Expected: All tests pass. The `PlaybackControllerSpec` tests basic play/stop behavior.

- [ ] **Step 3: Commit**

```bash
git add src/main/scala/sangeet/audio/PlaybackController.scala
git commit -m "perf: batch playback scheduling with single tick thread instead of per-note tasks"
```

---

### Task 7: Reduce Undo History Memory with Configurable Size

**Files:**
- Modify: `src/main/scala/sangeet/editor/UndoHistory.scala`

The default max size of 100 full composition snapshots can use significant memory for large compositions. Reduce to 50 and document the trade-off.

- [ ] **Step 1: Reduce default max size**

In `UndoHistory.scala`, change line 34:
```scala
def apply(initial: CompositionEditor, maxSize: Int = 100): UndoHistory =
```
to:
```scala
def apply(initial: CompositionEditor, maxSize: Int = 50): UndoHistory =
```

- [ ] **Step 2: Run tests**

Run: `sbt test`
Expected: All tests pass. The `UndoHistorySpec` tests don't hardcode 100.

- [ ] **Step 3: Commit**

```bash
git add src/main/scala/sangeet/editor/UndoHistory.scala
git commit -m "perf: reduce undo history default from 100 to 50 snapshots to save memory"
```

---

### Task 8: Final Verification

- [ ] **Step 1: Run full test suite**

Run: `sbt test`
Expected: All 284+ tests pass with zero failures.

- [ ] **Step 2: Launch the app and verify**

Run: `sbt run`
Verify:
- App launches, sample composition renders correctly
- Cursor blinks without lag
- Typing notes feels responsive
- MIDI playback works (play button)
- File > Save works (auto-save triggers on edits)
- Undo/redo works (Ctrl+Z / Ctrl+Shift+Z)

- [ ] **Step 3: Final commit (if any fixups needed)**
