# Sangeet Notes Editor -- Frontend Specification

**Version:** 1.0
**Date:** 2026-04-17
**Status:** Living document -- reflects the current desktop implementation and planned multi-platform adaptation.

---

## 1. Overview

### 1.1 Multi-Platform Frontend Architecture

The Sangeet Notes Editor is a Hindustani classical music notation editor in the Bhatkhande style. The architecture is being refactored from a monolithic ScalaFX desktop app into a multi-platform system:

| Platform | Technology | Priority | Status |
|----------|-----------|----------|--------|
| Desktop  | Scala 3 + ScalaFX (JavaFX) | Primary | Implemented |
| Web      | Browser-based, client-server | Secondary | Planned |
| Android  | Native Android app | Tertiary | Planned |

### 1.2 State Ownership Model

The frontend owns ALL mutable state:

- **Composition document** (the full `Composition` object)
- **Cursor position** (cycle, beat, sub-index, subdivisions, octave modifier)
- **Undo/redo history** (stack of `CompositionEditor` snapshots, max 50)
- **Edit mode** (SwarEdit vs StrokeEdit)
- **Ornament mode** (multi-step ornament input state machine)
- **Playback state** (playing, paused, stopped)
- **UI preferences** (script, BPM, read-only flag)

The backend is stateless. On desktop and Android, backend operations are direct library calls (in-process). On web, they are HTTP API calls to a Scala backend.

### 1.3 Platform Priority

Desktop > Web > Android. The desktop implementation is the reference. Web and Android adapt the same interaction model to their respective input and rendering capabilities.

---

## 2. Application Layout

### 2.1 Desktop Window Layout

```
+----------------------------------------------------------------------+
| Sangeet Notes Editor                                          [_][X] |
+----------------------------------------------------------------------+
| [New][Open][Save][PDF][HTML] | [Props][+Sec][Ren][Rm][Up][Dn] |Script|Voice|  <- Row 1 Toolbar
+----------------------------------------------------------------------+
| [Play][Pause][Stop] | [x]Loop | BPM: [====slider====] 60  |  [About]|  <- Row 2 Toolbar
+----------------------------------------------------------------------+
|                                                |                     |
|  +------------------------------------------+  | Keyboard Reference |
|  | COMPOSITION HEADER                       |  | Script: Devanagari |
|  | Title         [Gat badge]                |  | __________________ |
|  | Raag: Yaman . Thaat: Kalyan . Taal: ...  |  |                    |
|  | Arohan: ...   Avrohan: ...               |  | Swar (Notes)       |
|  +------------------------------------------+  | s  Sa (...)        |
|  |                                          |  | r  Re (...)  shud. |
|  |  NOTATION CANVAS (ScrollPane)            |  | R  Re (...)  komal |
|  |                                          |  | ...                |
|  |  > Section Name                          |  | __________________ |
|  |  X        2        0        3            |  | Octave             |
|  |  ~~  ~~   ^        (SrGm)                |  | .  mandra          |
|  |  Sa  Re   Ga  Ma   Pa  Dha  Ni  Sa       |  | '  taar            |
|  |  Da  Ra   Da  Ra   Da  Ra   Da  Ra       |  | `  madhya          |
|  |  aa  aa   ee  ee                         |  | __________________ |
|  |  |        |        |        |            |  | Strokes            |
|  |                                          |  | ...                |
|  |  -- Section Name -------------------------  | Ornaments          |
|  |  ...                                     |  | ...                |
|  |                                          |  |                    |
|  +------------------------------------------+  +--------------------+
|                  ^ 72% width                     ^ 28% width        |
+----------------------------------------------------------------------+
| Log                                                          <- 18% |
| > New Gat created: Yaman Vilambit Gat                                |
| > Opened: yaman.swar                                                 |
+----------------------------------------------------------------------+
```

**Key layout details:**

- **Window size:** 1400 x 800 pixels (initial)
- **No menu bar** -- all actions are exposed via toolbar buttons
- **Horizontal SplitPane:** Editor+StatusBar (left, 72%) | Keyboard Legend (right, 28%). Resizable by user.
- **Vertical SplitPane:** Editor area (top, 82%) | Status bar / Log (bottom, 18%). Resizable by user.
- **Top toolbar:** Two rows in a VBox, outside the split pane.

### 2.2 Web Layout

Same logical structure. The toolbar rows become a sticky header. The keyboard legend becomes a collapsible right sidebar (hidden by default on narrow viewports). The log panel becomes a toast/notification area or collapsible bottom drawer.

### 2.3 Android Layout

- **Top:** Action bar with file/export actions in overflow menu
- **Center:** Full-width notation canvas (scrollable)
- **Bottom:** On-screen swar keyboard (replacing physical keyboard)
- **Keyboard legend:** Accessible via help icon in action bar
- **Status messages:** Snackbar toasts

---

## 3. Screens and Components

### 3.a Main Editor Screen

**Purpose:** The primary screen. Users spend ~95% of their time here composing, editing, and reviewing notation.

**Contains:** Toolbar (2 rows), Composition Header, Notation Canvas (in ScrollPane), Keyboard Legend sidebar, Status Bar / Log Panel.

**State it manages:** All state listed in Section 5.

**Platform adaptation:**
- Desktop: Full window as described above.
- Web: Single-page application with responsive sidebar.
- Android: Full-screen with bottom swar keyboard overlay.

### 3.b Notation Canvas

**Purpose:** Renders the Bhatkhande notation grid for the active composition. This is the core visual element.

**Visual structure:** Each taal cycle occupies one or more lines, depending on density-aware line breaking. Each line renders 5 rows (see Section 6.b). Sections are rendered sequentially with headers.

```
  Active Section (blue, bold, underline):
  > Gat
  ___________________________________________________________________

  X           2           0           3              <- Taal markers
  ⌐--⌐                                              <- Subdivision bracket
  ~~ ~~       ^           (SrGm)                     <- Ornaments (above swar)
  Sa  Re      Ga   Ma     Pa   Dha   Ni   Sa         <- Swar glyphs
  . .                                     '          <- Octave dots
  Da  Ra      Da   Ra     Da   Ra    Da   Ra         <- Strokes (optional)
  aa  aa      ee   ee                                <- Sahitya (optional)
  |           |           |           |              <- Vibhag separators

  Inactive Section (gray, dimmed):
  -- Antara ------------------------------------------------
  (empty)
```

**State it manages:**
- Canvas dimensions (auto-sized to content height, minimum matches ScrollPane viewport)
- Section bounds list (for click-to-beat mapping)
- Cached layout grids (reused when only cursor moves)

**User interactions:**
- **Mouse click:** Place cursor at the clicked beat cell, or switch to a different section if clicking in an inactive section's area. Beat position computed from click coordinates using `SectionBounds` and `LineBounds`.
- **Scroll:** Standard ScrollPane scroll for compositions longer than the viewport.

**Backend operations:** `GridLayout.layoutAll(composition, config)` computes the grid. This is cached and only recomputed when the composition changes (not on cursor-only moves).

**Platform adaptation:**
- Desktop: JavaFX `Canvas` with `GraphicsContext2D` drawing.
- Web: HTML5 `<canvas>` element or SVG. Click coordinates mapped the same way.
- Android: Android `Canvas` with `Paint`/`drawText`. Pinch-to-zoom for dense compositions.

### 3.c Toolbar Row 1 -- File and Composition

**Purpose:** File operations, section management, script selection, voice toggle.

**Layout (left to right):**

| Button/Control | Action | Keyboard Shortcut |
|----------------|--------|-------------------|
| New | Open New Composition dialog | -- |
| Open | Open `.swar` file via file chooser | -- |
| Save | Save to `.swar` via file chooser (Save As) | -- |
| PDF | Export to PDF via file chooser | -- |
| HTML | Export to HTML via file chooser | -- |
| *separator* | | |
| Properties | Edit composition metadata dialog | -- |
| Add Section | Add section (Gat compositions only) | -- |
| Rename Section | Rename current section via text input dialog | -- |
| Remove Section | Remove current section (fails if last section) | -- |
| Move Up | Move current section up in order | -- |
| Move Down | Move current section down in order | -- |
| *separator* | | |
| Script: [ComboBox] | Change rendering script (Devanagari/Kannada/Telugu/English) | -- |
| Voice [toggle] | Toggle voice input mode (currently disabled) | -- |

**State it manages:** None directly. All actions delegate to EditorPane or open dialogs.

**Backend operations:**
- New: `CompositionEditor.create(...)` to build initial composition.
- Open: `SwarFormat.readFile(path)` to deserialize `.swar` JSON.
- Save: `SwarFormat.writeFile(path, composition)` to serialize.
- PDF: `PdfExport.exportPdf(composition, path)`.
- HTML: `HtmlExport.exportHtml(composition, path)`.

**Platform adaptation:**
- Desktop: ScalaFX `ToolBar` with `Button` items.
- Web: HTML button bar. File operations use File API / download links instead of native file choosers.
- Android: Action bar with overflow menu for less-used items (Properties, Move Up/Down). Primary actions (New, Open, Save) in visible action bar.

### 3.d Toolbar Row 2 -- Playback and Misc

**Purpose:** Audio playback controls, tempo settings, about dialog.

**Layout (left to right):**

| Button/Control | Action |
|----------------|--------|
| Play | Start MIDI playback of all sections at current BPM |
| Pause | Pause playback (internally calls stop) |
| Stop | Stop playback |
| *separator* | |
| Loop [checkbox] | Toggle loop playback (UI only, not yet wired to playback) |
| *separator* | |
| BPM: [slider 10-300] [value label] | Set tempo. Default 60. Slider has major ticks at 50, block increment 5. |
| *spacer* | Pushes About to far right |
| About | Show about dialog |

**State it manages:** Play/Pause/Stop button disabled states. BPM is set from laya on composition load.

**Laya-to-BPM defaults:**

| Laya | BPM |
|------|-----|
| Ati-Vilambit | 30 |
| Vilambit | 40 |
| Madhya | 80 |
| Drut | 160 |
| Ati-Drut | 250 |
| None (Palta) | 60 |

**Backend operations:** `PlaybackController.play(events, bpm, matras)` schedules MIDI notes.

**Platform adaptation:**
- Desktop: ScalaFX `ToolBar`. Audio via `javax.sound.midi`.
- Web: HTML controls. Audio via Web Audio API with MIDI.js or Tone.js.
- Android: Media controls in action bar or floating action button. Audio via Android MIDI API.

### 3.e Keyboard Legend Panel

**Purpose:** Right sidebar showing all keyboard shortcuts, organized by category. Serves as a permanent reference while composing.

**Visual layout:** Scrollable vertical list with section headings and monospaced key-description entries. Light background (`#f5f5f0`) with left border.

**Preferred width:** 400px, minimum 180px.

**Categories (in order):**

1. **Swar (Notes)** -- All 12 note keys with script-specific glyph names. Updates when script changes.
   - `s` Sa, `r` Re (shuddha), `R` Re (komal), `g` Ga (shuddha), `G` Ga (komal), `m` Ma (shuddha), `M` Ma (tivra), `p` Pa, `d` Dha (shuddha), `D` Dha (komal), `n` Ni (shuddha), `N` Ni (komal)
2. **Octave** -- `.` mandra, `'` taar, `` ` `` madhya
3. **Special** -- Space (rest), `-` (sustain), Del (delete)
4. **Navigation** -- Arrow keys, Tab, Enter
5. **Undo / Redo** -- Ctrl+Z, Ctrl+Shift+Z
6. **Subdivisions** -- Ctrl+2-8, double-tap
7. **Strokes (Mizrab)** -- F2 toggle, d/r in stroke mode, Ctrl+D/R/C
8. **Ornaments -- Simple** -- Ctrl+G (Gamak), Ctrl+A (Andolan), Ctrl+I (Gitkari)
9. **Ornaments -- One Note** -- Ctrl+K (Kan Swar), Ctrl+H (Sparsh), Ctrl+E (Ghaseet)
10. **Ornaments -- Two Notes** -- Ctrl+M/Shift+M (Meend), Ctrl+J (Krintan)
11. **Ornaments -- Multi-Note** -- Ctrl+U (Murki), Ctrl+W (Zamzama)
12. **Ornament Keys** -- Legend for symbols used in ornament key descriptions
13. **Voice Input** -- Space hold/release instructions
14. **Tips** -- Shift behavior, octave reset behavior, stroke/ornament attachment

**State it manages:** Current script (to update swar glyph names).

**Platform adaptation:**
- Desktop: ScalaFX `ScrollPane` containing a `VBox` of `Label` items.
- Web: Collapsible right sidebar. Toggle via keyboard shortcut or button. Hidden by default on mobile viewports.
- Android: Accessible via help icon. Opens as a full-screen overlay or bottom sheet.

### 3.f Status Bar / Log Panel

**Purpose:** Shows a scrollable log of recent operations. Most recent message at top. Serves as feedback for every user action.

**Visual layout:**
```
+----------------------------------------------------------------------+
| Log                                                                   |
| > New Gat created: Yaman Vilambit Gat -> /path/to/file.swar          |
| > Script changed to Devanagari (Hindi)                                |
| > Cursor placed at cycle 0, beat 3                                    |
+----------------------------------------------------------------------+
```

**State it manages:**
- `ObservableBuffer[String]` of log messages (max 100, oldest trimmed).

**Behavior:**
- Each user action (note entry, cursor move, file operation, error) appends a message.
- Messages prefixed with symbols: `>` (success), `X` (error), diamond (state change), arrows (undo/redo/cursor).
- Auto-scrolls to show most recent (index 0).

**Dimensions:** Preferred height 120px, minimum 60px.

**Platform adaptation:**
- Desktop: ScalaFX `ListView` in a `VBox`.
- Web: Div with reverse-chronological entries. Could be a collapsible panel or toast notifications for the most recent message.
- Android: Snackbar toasts for the most recent message. Full log accessible via a dedicated screen.

### 3.g New Composition Dialog

**Purpose:** Modal dialog to create a new composition with all required metadata.

**Form fields (in order):**

| Row | Label | Control | Notes |
|-----|-------|---------|-------|
| 0 | Title | TextField | Required. Prompt: "e.g. Yaman Vilambit Gat" |
| 1 | Save to | TextField + Browse button | Required. File path for `.swar` file. Browse opens native Save dialog. |
| 2 | Type | ComboBox | Options: Gat, Bandish, Palta. Default: Gat. Controls conditional field visibility. |
| 3 | Raag | Editable ComboBox | Searchable dropdown of 26 built-in raags. Typing filters the list. Custom raag names accepted. |
| 4 | *(detected)* | Label | Shows "Raag X recognized" (green) or "not in database" (gray) |
| 5 | Laya | ComboBox | Options: (none), Ati-Vilambit, Vilambit, Madhya, Drut, Ati-Drut. **Conditional: hidden for Palta.** |
| 6 | Taans | Spinner (0-50) | Default: 5. **Conditional: shown only for Gat.** |
| 7 | Stroke line | Checkbox | "Show Da/Ra stroke indicators below swar" |
| 8 | Sahitya line | Checkbox | "Show lyrics row below swar". **Conditional: hidden for Palta.** |
| 9 | Taal | ComboBox | 11 built-in taals, sorted alphabetically. Default: Teentaal. |
| 10 | Thaat | TextField | Auto-filled from recognized raag, editable. |
| 11 | Arohan | TextField | Auto-filled, editable. Space-separated swar names. |
| 12 | Avrohan | TextField | Auto-filled, editable. |
| 13 | Vadi | TextField | Auto-filled. |
| 14 | Samvadi | TextField | Auto-filled. |
| 15 | Script | ComboBox | Devanagari (Hindi), Kannada, Telugu, English. Default: Devanagari. |
| 16 | *(errors)* | Label (red) | Validation error messages. |

**Conditional visibility by composition type:**

| Field | Gat | Bandish | Palta |
|-------|-----|---------|-------|
| Laya | Shown (required) | Shown | Hidden |
| Taan count | Shown | Hidden | Hidden |
| Stroke line | Shown | Shown | Shown |
| Sahitya line | Shown | Shown | Hidden |

**Validation (on OK click):**
- Title required
- File path required
- Raag required
- Laya required for Gat

Validation errors prevent dialog from closing (event consumed).

**Backend operations:** `CompositionEditor.create(...)` builds the initial composition with appropriate sections based on type.

**Sections created by type:**
- **Gat:** "Gat" (Custom) + "Antara" + N "Taan" sections
- **Bandish:** "Sthayi"
- **Palta:** "Palta"

**Platform adaptation:**
- Desktop: JavaFX `Dialog` with `GridPane` layout.
- Web: Modal dialog with form elements. File path replaced with filename input (file saved via download or server-side storage).
- Android: Full-screen form activity/fragment.

### 3.h Composition Properties Dialog

**Purpose:** Edit metadata of an existing composition. Simpler than New Composition -- only title and taal are editable.

**Form fields:**

| Row | Label | Control | Editable |
|-----|-------|---------|----------|
| 0 | Title | TextField | Yes |
| 1 | Type | Label (read-only) | No |
| 2 | Raag | Label (read-only) | No |
| 3 | Taal | ComboBox | Yes |

**Backend operations:** Returns modified `Metadata` with updated `title`, `taal`, and `updatedAt` timestamp.

**Platform adaptation:** Same as New Composition Dialog but with fewer fields.

### 3.i About Dialog

**Purpose:** Display application information.

**Content:**
```
Sangeet Notes Editor

A desktop notation editor for Hindustani classical music
in the Bhatkhande notation style.

Designed for sitar compositions -- Gat, Bandish, and Palta.

Version 1.0
Built with Scala 3 + ScalaFX
```

**Platform adaptation:**
- Desktop: JavaFX `Alert` dialog (INFORMATION type).
- Web: Modal overlay.
- Android: Standard About screen or dialog.

---

## 4. Input Model

### 4.a Keyboard Input (Desktop/Web)

All keyboard input is handled on the ScrollPane that wraps the canvas. Focus must be on the ScrollPane for key events to register.

#### 4.a.1 Swar Note Entry

| Key | Note | Variant |
|-----|------|---------|
| `s` | Sa | Shuddha (fixed -- Sa has no komal/tivra) |
| `r` | Re | Shuddha |
| `R` (Shift+r) | Re | Komal |
| `g` | Ga | Shuddha |
| `G` (Shift+g) | Ga | Komal |
| `m` | Ma | Shuddha |
| `M` (Shift+m) | Ma | Tivra |
| `p` | Pa | Shuddha (fixed -- Pa has no komal/tivra) |
| `d` | Dha | Shuddha |
| `D` (Shift+d) | Dha | Komal |
| `n` | Ni | Shuddha |
| `N` (Shift+n) | Ni | Komal |
| `S` (Shift+s) | Sa | Shuddha (Shift ignored for Sa) |
| `P` (Shift+p) | Pa | Shuddha (Shift ignored for Pa) |

**Processing:** Handled via `onKeyTyped`. The character is inspected; `isUpper` indicates Shift was held. Calls `KeyHandler.handleSwarKey(editor, char, isShifted)`.

**Result:** Creates an `Event.Swar` at the current cursor position with the cursor's current octave. Cursor advances to next sub-beat (or next beat if subdivisions exhausted). Octave resets to Madhya after each note.

#### 4.a.2 Double-Tap (Dual Swar)

Typing the same swar key twice within 350ms triggers dual swar entry:

1. First tap enters single note normally (via `handleSwarKey`).
2. Second tap within threshold: undo the first entry, then insert two notes at sub-positions 0/2 and 1/2 of the beat (via `handleDualSwar`).
3. Cursor advances to next full beat.

Example: `ss` produces two Sa notes on one beat (SaSa). Works for all notes: `rr`, `gg`, `mm`, etc.

#### 4.a.3 Octave Modifiers

| Key | Action | Effect |
|-----|--------|--------|
| `.` (period) | Set next note to Mandra | `cursor.withOctave(Octave.Mandra)` |
| `'` (single quote) | Set next note to Taar | `cursor.withOctave(Octave.Taar)` |
| `` ` `` (backtick) | Reset to Madhya | `cursor.withOctave(Octave.Madhya)` |

Octave modifiers are "sticky for one note" -- after entering a note, the octave resets to Madhya automatically.

#### 4.a.4 Special Keys

| Key | Action | Handler |
|-----|--------|---------|
| Space | Insert Rest event | `handleSpecialKey(ed, "SPACE")` -- creates `Event.Rest`, advances cursor one beat |
| `-` (minus) | Insert Sustain event | `handleSpecialKey(ed, "MINUS")` -- creates `Event.Sustain`, advances cursor one beat |
| Backspace / Delete | Delete last event | `handleSpecialKey(ed, "BACKSPACE")` -- removes last event, moves cursor back |

**Note:** Space is handled in an event filter (not `onKeyPressed`) to prevent the ScrollPane from scrolling. The event is consumed in the filter.

#### 4.a.5 Navigation

| Key | Action | Notes |
|-----|--------|-------|
| Right arrow | Next beat | Stops at maxCycle + 1 |
| Left arrow | Previous beat | Stops at cycle 0, beat 0 |
| Tab | Next beat | Same as Right arrow |
| Enter | Next cycle (beat 0 of cycle+1) | Stops at maxCycle + 1. In ornament collect mode (Murki/Zamzama), finishes the ornament instead. |

Navigation produces `CursorMove` actions (no undo history entry).

#### 4.a.6 Undo / Redo

| Key | Action |
|-----|--------|
| Ctrl+Z (or Cmd+Z on macOS) | Undo -- pop from undo stack |
| Ctrl+Shift+Z (or Cmd+Shift+Z) | Redo -- pop from redo stack |

Undo/redo operates on the `UndoHistory` stack directly. No backend call needed.

#### 4.a.7 Subdivisions

| Key | Action |
|-----|--------|
| Ctrl+2 | Set 2 subdivisions per beat |
| Ctrl+3 | Set 3 subdivisions per beat |
| Ctrl+4 | Set 4 subdivisions per beat |
| Ctrl+5 | Set 5 subdivisions per beat |
| Ctrl+6 | Set 6 subdivisions per beat |
| Ctrl+7 | Set 7 subdivisions per beat |
| Ctrl+8 | Set 8 subdivisions per beat |

Sets `cursor.totalSubdivisions` and resets `cursor.subIndex` to 0. Subsequent note entries will use `Rational(subIndex, totalSubdivisions)` for beat position and `Rational(1, totalSubdivisions)` for duration.

#### 4.a.8 Strokes

| Key | Action |
|-----|--------|
| Ctrl+D | Add Da stroke to last entered swar |
| Ctrl+R | Add Ra stroke to last entered swar |
| Ctrl+C | Add Chikari stroke to last entered swar |
| F2 | Toggle stroke edit mode (only if stroke line enabled) |

**Stroke Edit Mode (F2):**
- Cursor renders on the stroke line (orange, shorter) instead of the swar line (blue, full height).
- Only `d` and `r` keys are active (set Da/Ra on the swar at cursor position).
- Arrow keys navigate through swar positions (including sub-positions within a beat).
- Backspace clears the explicit stroke (reverts to auto-alternating Da/Ra).
- Escape or F2 returns to Swar Edit mode.

#### 4.a.9 Ornaments

**Simple ornaments (one key, applied to last swar):**

| Key | Ornament | Description |
|-----|----------|-------------|
| Ctrl+G | Gamak | Heavy oscillation |
| Ctrl+A | Andolan | Gentle oscillation |
| Ctrl+I | Gitkari | Hammer/pull trill |

**One-note ornaments (enter mode, then type one note):**

| Key | Ornament | Flow |
|-----|----------|------|
| Ctrl+K, then note | Kan Swar | Grace note before main note |
| Ctrl+H, then note | Sparsh | Light touch of adjacent note |
| Ctrl+E, then note | Ghaseet | Heavy lateral string pull to target |

**Two-note ornaments (enter mode, type start note, then end note):**

| Key | Ornament | Flow |
|-----|----------|------|
| Ctrl+M, note, note | Meend (ascending) | Ascending glide between two notes |
| Ctrl+Shift+M, note, note | Meend (descending) | Descending glide between two notes |
| Ctrl+J, note, note | Krintan | Pull-off sequence between two notes |

**Multi-note ornaments (enter mode, type notes, press Enter to finish):**

| Key | Ornament | Flow |
|-----|----------|------|
| Ctrl+U, notes..., Enter | Murki | Ornamental turn (3-5 notes) |
| Ctrl+W, notes..., Enter | Zamzama | Rapid repeated note cluster |

**Escape** cancels any active ornament mode.

**Ornament mode state machine:**

```
                    Ctrl+K/H/E
Normal Mode  ──────────────────>  Single-Note Mode  ──(note)──>  Applied, back to Normal
    |
    |── Ctrl+M ──>  MeendStart  ──(note)──>  MeendEnd  ──(note)──>  Applied
    |── Ctrl+J ──>  KrintanStart ──(note)──> KrintanEnd ──(note)──> Applied
    |── Ctrl+U ──>  MurkiCollect ──(notes)──> ──(Enter)──> Applied
    |── Ctrl+W ──>  ZamzamaCollect ──(notes)──> ──(Enter)──> Applied
    |
    └── Escape at any point returns to Normal Mode
```

#### 4.a.10 Voice Input (Push-to-Talk)

When voice mode is enabled (via Voice toggle button):
- **Space (hold):** Starts microphone capture. Red recording indicator appears on canvas.
- **Space (release):** Stops capture, sends audio samples to Whisper recognizer on background thread.
- Recognition result is inserted as a `Event.Swar` at cursor position using the octave set before speaking.
- Minimum capture length: 1600 samples (~0.1s at 16kHz). Shorter captures are rejected.

When voice mode is disabled, Space inserts a Rest as normal.

### 4.b Mouse Input (Desktop/Web)

| Action | Behavior |
|--------|----------|
| Click on beat cell | Places cursor at that beat. Computed via `SectionBounds` / `LineBounds` hit testing. |
| Click on inactive section | Switches active section and resets cursor to cycle 0, beat 0. |
| Click in section area but not on a cell | Keeps current cursor position. |

**Hit testing algorithm:**
1. Find the `SectionBounds` whose Y range contains the click Y.
2. Within that section, find the `LineBounds` whose Y range contains the click Y.
3. Compute cell index: `(clickX - startX) / cellWidth`.
4. Clamp beat to valid range `[0, taal.matras - 1]`.

### 4.c Touch Input (Android)

Touch input adapts the keyboard model for a touchscreen:

- **On-screen swar keyboard:** A custom keyboard at the bottom of the screen with buttons for each swar (Sa, Re, Ga, Ma, Pa, Dha, Ni), a row for variants (komal/tivra toggle), octave buttons, and special keys (Rest, Sustain, Delete).
- **Tap on beat cell:** Same as mouse click -- places cursor.
- **Long-press on swar button:** Could enter ornament mode (to be designed).
- **Swipe gestures:** Left/right to navigate beats, up/down to switch sections.
- **Pinch-to-zoom:** Scale the notation grid for readability.

---

## 5. State Management

### 5.1 State Inventory

| State | Type | Location | Persistence |
|-------|------|----------|-------------|
| Composition | `Composition` | `UndoHistory.present.composition` | Auto-saved to `.swar` file (debounced 500ms) |
| Cursor | `CursorModel` | `UndoHistory.present.cursor` | Not persisted |
| Current section index | `Int` | `UndoHistory.present.currentSectionIndex` | Not persisted |
| Undo history | `UndoHistory` (past: List, present, future: List) | `EditorPane.history` | Not persisted |
| Edit mode | `EditMode` enum (SwarEdit / StrokeEdit) | `EditorPane.editMode` | Not persisted |
| Ornament mode | `Option[OrnamentMode]` | `EditorPane.ornamentMode` | Not persisted |
| Current octave modifier | `Octave` | `CursorModel.currentOctave` | Not persisted |
| Double-tap state | `(lastTypedChar, lastTypedTime)` | `EditorPane` vars | Not persisted |
| Voice mode | `Boolean` | `EditorPane.voiceMode` | Not persisted |
| Voice capturing | `Boolean` (volatile) | `EditorPane.voiceCapturing` | Not persisted |
| Script | `SwarScript` enum | `DevanagariMap._script` (global mutable) | Not persisted |
| File path | `Option[Path]` | `EditorPane.currentFilePath` | Not persisted (derived from file open/save) |
| Read-only flag | `Boolean` | `EditorPane.readOnly` | Not persisted |
| Playback state | Button disabled states | `MainApp` local vars | Not persisted |
| BPM | `Double` | `bpmSlider.value` | Not persisted |
| Cursor visible (blink) | `Boolean` | `EditorPane.cursorVisible` | Not persisted |
| Layout cache | `Option[(Composition, List[SectionGrid])]` | `EditorPane.cachedGrids` | Not persisted |

### 5.2 State Update Patterns

**Content changes** (note entry, delete, stroke, ornament):
1. Call `KeyHandler` method to compute new `CompositionEditor`.
2. Call `pushEditor(newEd)` which pushes to undo stack and triggers auto-save.
3. Reset cursor blink.
4. Redraw canvas.

**Cursor-only moves** (arrow keys, octave modifiers, subdivision changes, section switch):
1. Compute new `CompositionEditor` with updated cursor.
2. Call `setEditorDirect(newEd)` which updates `history.present` without creating an undo entry.
3. Reset cursor blink.
4. Redraw canvas (layout cache is reused since composition hasn't changed).

**Undo / Redo:**
1. Call `history.undo` or `history.redo` to get new `UndoHistory`.
2. Update composition header.
3. Redraw.

### 5.3 Undo History

- Implemented as an immutable `UndoHistory` case class with `past: List[CompositionEditor]`, `present: CompositionEditor`, `future: List[CompositionEditor]`.
- Maximum stack size: 50 snapshots.
- `push()` adds current state to `past`, sets new state, clears `future` (redo stack).
- Each snapshot stores the full `CompositionEditor` (composition + section index + cursor).

### 5.4 Auto-Save

- Triggered after every content change via `pushEditor()`.
- Debounced: waits 500ms after the last edit before saving.
- Runs on a dedicated daemon thread (`auto-save` single-thread executor).
- Saves to the current file path using `SwarFormat.writeFile()`.
- No-op if no file path is set or no editor is active.

---

## 6. Rendering Model

### 6.a Grid Layout

The layout pipeline transforms a `Composition` into renderable grid data:

```
Composition
    |
    v
GridLayout.layoutAll(composition, config)  -->  List[SectionGrid]
    |
    |  (one SectionGrid per section, each containing List[GridLine])
    v
GridRenderer.drawSection(gc, grid, ...)  -->  rendered pixels on canvas
```

**LayoutConfig values:**

| Parameter | Value | Description |
|-----------|-------|-------------|
| `cellWidthBase` | 60.0 px | Base width of each beat cell |
| `cellOverflowExpand` | 15.0 px | Extra width per note beyond 1 in a cell |
| `lineSpacing` | 40.0 px | Vertical gap between grid lines |
| `headerHeight` | 120.0 px | Space reserved for composition header |
| `highDensityThreshold` | 5 | Notes/beat above which lines are split by vibhag |

**SectionGrid** contains:
- `sectionName: String`
- `lines: List[GridLine]`

**GridLine** contains:
- `cells: List[BeatCell]` -- one per beat in this line
- `markers: List[(Int, VibhagMarker)]` -- taal markers at cell indices
- `vibhagBreaks: List[Int]` -- cell indices where vibhag separators are drawn

**BeatCell** contains:
- `position: BeatPosition` -- (cycle, beat, subBeat)
- `events: List[Event]` -- the events at this beat

### 6.b Notation Rows (5 per cycle line)

Each grid line is rendered top-to-bottom with the following row layout:

```
Y offset:  Row:                Height:
0          Taal markers        14px     Sam (X), Taali (2,3), Khali (0)
14         Subdivision bracket 10px     Top bracket for cells with >1 event
24         Ornament + Taar dot 18px     Meend arcs, kan glyphs, gamak waves, taar dots
42         Swar glyph          18px     Note text (Devanagari/Kannada/Telugu/English)
54         Mandra dot area     12px     Mandra octave dots below swar, komal underline
58         Stroke row (opt.)   16px     Da/Ra/Chikari indicators
74         Sahitya row (opt.)  14px     Lyrics text aligned per beat
```

**Total line height:**
- Without stroke/sahitya: 54px (up to mandra dot area)
- With stroke only: 74px
- With stroke + sahitya: 88px

**Row details:**

**1. Taal Markers Row**
- Rendered at `markerY` (top of line).
- Text centered horizontally within each cell at cell positions matching the taal's vibhag boundaries.
- `X` for Sam (color: `#D32F2F` bright red), numbers for Taali, `0` for Khali (color: `#B71C1C` dark red).
- Font: System 12px.

**2. Subdivision Bracket Row**
- Rendered at `bracketY` (14px from top).
- Shown only for cells with more than one event.
- A top bracket shape: left vertical tick, horizontal line, right vertical tick.
- Color: `rgb(120, 120, 120)`.

**3. Ornament Row**
- Rendered at `ornamentY` (24px from top), above the swar text baseline.
- Each ornament type has a distinct visual representation:
  - **Meend:** Arc curve above note. Ascending = upward curve, Descending = downward curve. Arrow at end. Line width 1.8px.
  - **Kan Swar:** Small Devanagari glyph (9px font) positioned 12px left and 10px above the main swar.
  - **Gamak:** Heavy zigzag line (4 segments, amplitude 3.5px, line width 1.8px).
  - **Andolan:** Gentle zigzag line (6 segments, amplitude 1.5px, line width 0.9px).
  - **Gitkari:** Italic "tr" text followed by short wavy tail (3 segments).
  - **Murki:** Notes in parentheses, 8px font, centered above swar.
  - **Krintan:** Downward arc with note text inside, 7px font.
  - **Ghaseet:** Heavy arc (line width 2.5px) with directional arrow and target note label.
  - **Sparsh:** Small dot + tiny note glyph (7px) to the right of main swar.
  - **Zamzama:** Notes in square brackets, 8px font, centered above swar.
  - **Custom:** Italic name label.
- All ornaments use color `#4A148C` (deep purple).

**4. Swar Row**
- Rendered at `swarY` (42px from top).
- Swar text drawn center-aligned at the cell center (or subdivided positions within the cell).
- Font: Script-dependent (Noto Sans Devanagari / Noto Sans Kannada / Noto Sans Telugu / System), 16px.
- Color: `#1A237E` (dark indigo).
- **Komal mark:** Horizontal underline 3px below the swar text (x-8 to x+8). Same color as swar.
- **Tivra mark:** Short vertical stroke above the swar (x-2, from y-16 to y-10). Same color as swar.
- **Octave dots:** Small circles (radius 2px) positioned above (Taar) or below (Mandra) the swar. Color: `#E65100` (deep orange).
  - Madhya: no dots.
  - Mandra: 1 dot below. Ati-Mandra: 2 dots below.
  - Taar: 1 dot above. Ati-Taar: 2 dots above.
- **Rest:** Rendered as `-` in gray (`#616161`).
- **Sustain:** Rendered as em-dash `---` in lighter gray (`#9E9E9E`).

**5. Stroke Row (optional)**
- Rendered at `strokeY` (58px from top), separated by a thin horizontal line from swar area.
- Da/Ra text in script-specific glyphs (e.g., "Da"/"Ra" in English, "Da"/"Ra" in Devanagari). Font: 10px.
- Color: `#00695C` (teal).
- Auto-alternating Da/Ra pattern applied to swar events that don't have an explicit stroke.
- Left margin label: "Da/Ra" (or script equivalent), 9px, light gray.

**6. Sahitya Row (optional)**
- Rendered at `sahityaY` (74px from top), separated by a thin horizontal line.
- Lyrics text center-aligned per beat cell. Font: Noto Sans Devanagari 11px.
- Color: `#2E7D32` (dark green).
- Left margin label: "Sahitya", 9px, light gray.

**Vibhag separators:** Vertical gray lines at vibhag break positions, spanning the full line height plus 5px padding above and below.

### 6.c Cursor Rendering

**Normal cursor (Swar Edit mode):**
- Vertical line (blue, `rgb(25, 118, 210)`, line width 2.5px) drawn 4px before the right edge of the current cell.
- Spans from marker row top (Y + 4) to bottom of the line (including stroke and sahitya rows if shown).
- Blinks on/off every 530ms via a `Timeline` animation.

**Stroke edit cursor:**
- Shorter vertical line (orange, `rgb(230, 120, 0)`, line width 2.0px) drawn 4px before the right edge.
- Spans only the stroke row area (strokeY - 10 to strokeY + 6).

**Cursor on empty sections:**
- Dashed blue rectangle (600px wide, 20px tall) with text "(empty -- start typing to add notes)".
- Thin blinking cursor line at left edge.

**Cursor beyond existing cells:**
- When cursor cycle matches a line but beat is beyond the last cell, cursor draws after the last cell.
- When cursor is on a cycle with no events, cursor draws at the bottom of the section.

**Blink reset:** Every user action (key press, mouse click) resets the blink timer to visible + restart.

### 6.d Composition Header

The composition header is rendered as a ScalaFX `VBox` panel above the canvas (not drawn on the canvas itself).

```
+--------------------------------------------------------------+
| Title                                            [Type badge] |
| Raag: Yaman . Thaat: Kalyan . Taal: Teentaal (16) . Laya: .. |
| Arohan: Sa Re Ga Ma Pa Dha Ni Sa'                             |
| Avrohan: Sa' Ni Dha Pa Ma Ga Re Sa                            |
+--------------------------------------------------------------+
```

**Visual details:**
- Background: `#f0efe8` (warm off-white), bottom border `#ccc`.
- Padding: 8px vertical, 15px horizontal.
- Title: 15px bold.
- Type badge: 10px white text on indigo background (`#5c6bc0`), 3px border radius, pill-shaped.
- Detail chips: 11px, dark gray (`#444`), separated by centered dot `*` in light gray.
- Arohan/Avrohan: Only shown if present in the raag metadata.

**Data displayed:**
- Title, composition type badge
- Raag name, Thaat (if available), Taal name + matras count, Laya (if available)
- Vadi, Samvadi (if available)
- Arohan, Avrohan (if available)

### 6.e Section Headers

**Active section:**
- Bold 15px text in blue (`rgb(25, 118, 210)`) with `>` prefix.
- Blue horizontal underline (2px wide, 600px long).
- Blue left accent bar (3px wide) running the full height of the section content.

**Inactive section:**
- 14px bold text in gray with `--` prefix.
- Light gray horizontal line at section header level.

---

## 7. Interaction Flows

### 7.a Create New Composition

```
User                         Frontend                          Backend
 |                              |                                |
 |-- clicks [New] ------------>|                                |
 |                              |-- show NewCompositionDialog -->|
 |                              |<-- dialog with form ----------|
 |-- fills fields, clicks OK ->|                                |
 |                              |-- validate fields             |
 |                              |   (title, raag, file path,    |
 |                              |    laya for Gat required)     |
 |                              |                                |
 |                              |-- CompositionEditor.create() ->|
 |                              |<-- CompositionEditor ----------|
 |                              |                                |
 |                              |-- setReadOnly(false)          |
 |                              |-- setEditor(editor)           |
 |                              |-- setFilePathAndSave(path)    |
 |                              |-- changeScript(script)        |
 |                              |-- header.update(metadata)     |
 |                              |-- redraw()                    |
 |                              |-- statusBar.log("New...")     |
 |<-- canvas shows empty       |                                |
 |    composition with cursor  |                                |
```

### 7.b Enter Notes

```
User                         Frontend                          Backend
 |                              |                                |
 |-- presses 'g' ------------->|                                |
 |                              |-- onKeyTyped handler          |
 |                              |   char='g', isShifted=false   |
 |                              |                                |
 |                              |-- KeyHandler.handleSwarKey() ->|
 |                              |   (editor, 'g', false)        |
 |                              |<-- (newEditor, "Ga") ---------|
 |                              |                                |
 |                              |   Event.Swar created:         |
 |                              |     note=Ga, variant=Shuddha, |
 |                              |     octave=Madhya,            |
 |                              |     beat=(cycle,beat,sub),    |
 |                              |     duration=1/subdivisions   |
 |                              |                                |
 |                              |-- pushEditor(newEditor)       |
 |                              |   (undo stack push +          |
 |                              |    auto-save scheduled)       |
 |                              |-- resetBlink()                |
 |                              |-- redraw()                    |
 |                              |   (cachedGrids invalidated    |
 |                              |    since composition changed) |
 |<-- canvas shows new note    |                                |
```

### 7.c Add Ornament (Multi-Step: Meend)

```
User                         Frontend                          Backend
 |                              |                                |
 |-- presses Ctrl+M ---------->|                                |
 |                              |-- ornamentMode = MeendStart(Ascending)
 |                              |-- statusBar: "Meend -- type start note"
 |                              |                                |
 |-- presses 's' ------------->|                                |
 |                              |-- handleNoteOrnament(ed, 's', false, MeendStart)
 |                              |-- ornamentMode = MeendEnd(Sa, Ascending)
 |                              |-- statusBar: "Meend start: Sa -- type end note"
 |                              |                                |
 |-- presses 'g' ------------->|                                |
 |                              |-- handleNoteOrnament(ed, 'g', false, MeendEnd)
 |                              |-- creates Meend(Sa, Ga, Ascending, [])
 |                              |-- modifyLastSwar: appends ornament to last swar
 |                              |-- ornamentMode = None
 |                              |-- pushEditor(newEditor)       |
 |                              |-- statusBar: "Meend (Sa -> Ga) added"
 |                              |-- redraw()                    |
 |<-- canvas shows meend arc   |                                |
```

### 7.d Undo/Redo

```
User                         Frontend                          Backend
 |                              |                                |
 |-- presses Ctrl+Z ---------->|                                |
 |                              |-- history.undo -> Some(newHistory)
 |                              |   (pops from past stack,       |
 |                              |    pushes current to future)   |
 |                              |-- history = newHistory         |
 |                              |-- header.update(metadata)     |
 |                              |-- statusBar: "Undo"           |
 |                              |-- resetBlink()                |
 |                              |-- redraw()                    |
 |<-- canvas shows previous    |                                |
 |    composition state        |                                |
```

### 7.e Play Composition

```
User                         Frontend                          Backend
 |                              |                                |
 |-- clicks [Play] ----------->|                                |
 |                              |-- get BPM from slider         |
 |                              |-- get matras from taal        |
 |                              |-- allEvents = sections.flatMap(_.events)
 |                              |                                |
 |                              |-- playbackController.play() ->|
 |                              |   (events, bpm, matras)       |
 |                              |<-- MIDI playback starts ------|
 |                              |                                |
 |                              |-- setPlaying(true)            |
 |                              |   (Play disabled, Pause/Stop  |
 |                              |    enabled)                   |
 |                              |-- statusBar: "Play at N BPM"  |
 |<-- audio plays              |                                |
```

### 7.f Open File

```
User                         Frontend                          Backend
 |                              |                                |
 |-- clicks [Open] ----------->|                                |
 |                              |-- FileChooser (*.swar filter) |
 |-- selects file ------------->|                                |
 |                              |-- SwarFormat.readFile(path) -->|
 |                              |<-- Right(composition) ---------|
 |                              |                                |
 |                              |-- setReadOnly(false)          |
 |                              |-- setComposition(comp)        |
 |                              |   (creates fresh UndoHistory, |
 |                              |    resets cursor, editMode)   |
 |                              |-- setFilePath(path)           |
 |                              |-- header.update(metadata)     |
 |                              |-- redraw()                    |
 |<-- composition rendered     |                                |
```

### 7.g Stroke Edit Mode

```
User                         Frontend                          Backend
 |                              |                                |
 |-- presses F2 -------------->|                                |
 |                              |-- editMode = StrokeEdit       |
 |                              |-- statusBar: "Stroke edit mode"
 |                              |-- redraw() (cursor moves to   |
 |                              |   stroke row, orange color)   |
 |                              |                                |
 |-- presses 'd' ------------->|                                |
 |                              |-- onKeyTyped in StrokeEdit    |
 |                              |-- editor.setStrokeAt(cursor, Da)
 |                              |-- pushEditor(newEditor)       |
 |                              |-- advance cursor to next swar |
 |                              |-- redraw()                    |
 |                              |                                |
 |-- presses Escape ---------->|                                |
 |                              |-- editMode = SwarEdit         |
 |                              |-- redraw()                    |
```

---

## 8. Platform Adaptation

### 8.a Desktop (ScalaFX / JavaFX)

This is the reference implementation. All details in this spec reflect the current desktop behavior.

| Aspect | Implementation |
|--------|---------------|
| UI framework | ScalaFX (Scala 3 wrapper over JavaFX) |
| Window | `JFXApp3.PrimaryStage`, 1400x800 initial size |
| Rendering | `Canvas` with `GraphicsContext2D` |
| Fonts | Noto Sans Devanagari (embedded), Noto Sans Kannada, Noto Sans Telugu, System |
| File I/O | `java.nio.file.Path`, `FileChooser` for open/save |
| Audio | `javax.sound.midi` via `MidiEngine` |
| Voice | whisper-jni (JNI wrapper for whisper.cpp), `javax.sound.sampled` for mic capture |
| Keyboard | `setOnKeyTyped` for character input, `setOnKeyPressed` for control/navigation keys, `addEventFilter` for Space |
| Mouse | `setOnMouseClicked` on canvas |
| Threading | JavaFX Application Thread for UI, daemon threads for auto-save and voice inference |
| Single instance | Server socket on localhost port 47633 |
| App icon | `packaging/icons/sangeet-icon-256.png`, set on stage and macOS dock |

### 8.b Web (Browser)

| Aspect | Approach |
|--------|----------|
| UI framework | HTML/CSS/JS (or ScalaJS with a UI library) |
| Rendering | HTML5 `<canvas>` element. Port `CanvasRenderer` / `GridRenderer` / `SwarGlyph` / `OrnamentRenderer` to draw on `CanvasRenderingContext2D`. Alternatively, SVG for resolution independence. |
| Fonts | Web fonts: Google Fonts Noto Sans Devanagari/Kannada/Telugu. Loaded via `@font-face`. |
| File I/O | File API for reading `.swar` files. Download links for save/export. Or server-side storage with user accounts. |
| Audio | Web Audio API. MIDI playback via Tone.js or similar. |
| Voice | Web Speech API or Whisper WASM. Push-to-talk via key hold events. |
| Keyboard | `document.addEventListener('keydown')` and `'keypress'`. Prevent default on Space, Tab, arrow keys to avoid browser scrolling. Map Ctrl to Cmd on macOS. |
| Mouse | `canvas.addEventListener('click')` with `offsetX`/`offsetY`. |
| Layout | CSS Grid or Flexbox for the overall layout. Responsive: sidebar collapses on narrow screens. |
| Backend communication | `fetch()` to REST API endpoints (`POST /api/layout`, `POST /api/playback/schedule`, etc.). Or use ScalaJS to run backend logic client-side. |
| State persistence | `localStorage` or `IndexedDB` for auto-save. File download for explicit save. |

**Key differences from desktop:**
- No native file dialogs -- use `<input type="file">` for open and programmatic download for save.
- Tab key needs careful handling to prevent focus changes.
- Ctrl+D conflicts with browser bookmark shortcut -- may need alternative binding.
- Touch events for mobile web.

### 8.c Android

| Aspect | Approach |
|--------|----------|
| UI framework | Jetpack Compose or Android Views |
| Rendering | Compose `Canvas` or Android `android.graphics.Canvas`. Port rendering code to use Android `Paint`, `drawText`, `drawArc`. |
| Fonts | Noto Sans Devanagari bundled as assets. Loaded via `Typeface.createFromAsset`. |
| File I/O | Storage Access Framework for file picking. Internal storage for auto-save. |
| Audio | Android MIDI API (`android.media.midi`) or `MediaPlayer` with generated audio. |
| Voice | Android SpeechRecognizer or whisper.cpp via JNI. |
| Input | On-screen swar keyboard (custom `Composable` or `View`). No physical keyboard assumed. |
| Backend | Shared Scala backend compiled as JAR, called directly via JNI or Scala Native interop. |

**On-screen swar keyboard concept:**

```
+-------------------------------------------+
|  .mandra  | Sa Re Ga Ma Pa Dha Ni | .taar |
+-------------------------------------------+
| komal/tivra toggle  | Rest | Sustain | Del|
+-------------------------------------------+
| << prev | >> next | Undo | Redo          |
+-------------------------------------------+
```

**Navigation patterns:**
- Bottom navigation bar: Editor, Files, Settings.
- Action bar: File operations, export, properties.
- Sections: Horizontal tab strip above canvas or dropdown selector.

---

## 9. Design Tokens / Visual Constants

### 9.1 Color Palette

These colors are defined in `NotationColors.scala` and must be used consistently across all renderers (canvas, PDF, HTML, and future web/Android).

| Token | Hex | Usage |
|-------|-----|-------|
| `taalMarker` | `#B71C1C` | Taali and Khali markers |
| `taalMarkerSam` | `#D32F2F` | Sam (X) marker -- brighter red |
| `swar` | `#1A237E` | Swar note glyphs, komal underline, tivra overbar |
| `octaveDot` | `#E65100` | Octave indicator dots (mandra/taar) |
| `ornament` | `#4A148C` | All ornament decorations |
| `stroke` | `#00695C` | Da/Ra/Chikari stroke indicators |
| `sahitya` | `#2E7D32` | Lyrics text |
| `rest` | `#616161` | Rest symbol |
| `sustain` | `#9E9E9E` | Sustain symbol |
| `komalMark` | `#1A237E` | Komal underline (same as swar) |
| `tivraMark` | `#1A237E` | Tivra vertical stroke (same as swar) |

**UI chrome colors (from component styles):**

| Element | Color / Style |
|---------|--------------|
| Active section text | `rgb(25, 118, 210)` -- Material Blue 700 |
| Active section accent bar | `rgb(25, 118, 210)` -- 3px width |
| Inactive section text | Gray |
| Cursor (swar mode) | `rgb(25, 118, 210)` -- 2.5px line width |
| Cursor (stroke mode) | `rgb(230, 120, 0)` -- 2.0px line width |
| Composition header background | `#f0efe8` |
| Keyboard legend background | `#f5f5f0` |
| Type badge background | `#5c6bc0` -- Indigo 400 |
| Type badge text | White |
| Vibhag separator | Gray |
| Stroke/sahitya separator | `rgb(180, 180, 180)` -- 0.5px |

### 9.2 Fonts

| Context | Font | Size |
|---------|------|------|
| Swar glyphs | Script-dependent (see below) | 16px |
| Swar glyphs (small, e.g. stroke) | Script-dependent | 10px |
| Taal markers | System | 12px |
| Section name (active) | System Bold | 15px |
| Section name (inactive) | System Bold | 14px |
| Ornament Kan Swar | Noto Sans Devanagari | 9px |
| Ornament Murki/Zamzama | Noto Sans Devanagari | 8px |
| Ornament Krintan/Ghaseet/Sparsh | Noto Sans Devanagari | 7px |
| Ornament Gitkari/Custom | System Italic | 8px |
| Sahitya | Noto Sans Devanagari | 11px |
| Stroke margin label | Script-dependent | 9px |
| Sahitya margin label | System | 9px |
| Composition title | System | 15px bold |
| Header detail chips | System | 11px |

**Script-to-font mapping:**

| Script | Font Family |
|--------|------------|
| Devanagari | Noto Sans Devanagari |
| Kannada | Noto Sans Kannada |
| Telugu | Noto Sans Telugu |
| English | System |

Font instances are cached in `SwarGlyph` to avoid re-creation on every draw call. Cache is invalidated when the script changes.

### 9.3 Spacing and Dimensions

| Constant | Value | Description |
|----------|-------|-------------|
| Cell width (base) | 60px | Width of one beat cell |
| Cell overflow expand | 15px | Extra width per additional note in a subdivided cell |
| Line spacing | 40px | Vertical gap between grid lines |
| Header height | 120px | Reserved space for composition header |
| High density threshold | 5 | Notes/beat above which line breaking changes |
| Cursor blink interval | 530ms | Toggle visibility period |
| Double-tap threshold | 350ms | Max time between taps for dual swar |
| Auto-save debounce | 500ms | Delay after last edit before saving |
| Undo history max | 50 | Maximum undo snapshots |
| Log max entries | 100 | Maximum status bar log entries |
| Dot radius | 2px | Octave indicator dot size |
| Window initial size | 1400 x 800 px | Desktop window dimensions |
| Canvas initial size | 1100 x 600 px | Canvas element dimensions (auto-resized to content) |
| BPM slider range | 10 - 300 | Tempo range |

---

## 10. Accessibility

### 10.1 Keyboard-First Design

The editor is inherently keyboard-first. All primary composition operations are available via keyboard shortcuts. The mouse is optional for cursor placement and section switching but not required for note entry or editing.

### 10.2 Screen Reader Considerations (Web)

For the web platform:
- Swar entry feedback should be announced via ARIA live regions (e.g., "Sa shuddha madhya entered").
- Status bar messages should be mapped to `aria-live="polite"` announcements.
- Cursor position should be communicated: "Cursor at cycle 2, beat 5".
- Section names and taal markers should have semantic labels.
- The canvas itself is not accessible to screen readers -- a text-based alternative view may be needed for full accessibility.

### 10.3 Visual Accessibility

- **Color contrast:** All notation colors are chosen for high contrast against white background. The color palette uses dark, saturated colors (dark indigo, dark red, deep purple, teal, dark green).
- **High contrast mode:** Not currently implemented. A future enhancement could provide a monochrome or high-contrast color scheme via `NotationColors` substitution.
- **Font sizing:** Currently fixed. A future enhancement could add a zoom level or font scale factor.
- **Cursor visibility:** The blinking cursor uses a distinct color (blue) and substantial line width (2.5px) for visibility. In stroke edit mode, the orange cursor provides color differentiation.

### 10.4 Motor Accessibility

- Double-tap threshold (350ms) may need to be configurable for users with motor impairments.
- Ornament multi-step sequences have no timeout -- users can take as long as needed between steps.
- Escape always cancels the current ornament mode, providing an easy exit.

---

## Appendix A: Script Glyph Mapping

| Note | Devanagari | Kannada | Telugu | English |
|------|------------|---------|--------|---------|
| Sa | सा | ಸಾ | స | Sa |
| Re | रे | ರಿ | రి | Re |
| Ga | ग | ಗ | గ | Ga |
| Ma | म | ಮ | మ | Ma |
| Pa | प | ಪ | ప | Pa |
| Dha | ध | ಧ | ధ | Dha |
| Ni | नि | ನಿ | ని | Ni |

**Stroke glyphs:**

| Stroke | Devanagari | English |
|--------|------------|---------|
| Da | दा | Da |
| Ra | रा | Ra |
| Chikari | ची | Ch |
| Jod | जो | Jo |

**Special symbols:**
- Rest: `-`
- Sustain: `\u2014` (em-dash)
- Vibhag markers: `X` (Sam), `0` (Khali), `1`/`2`/`3`... (Taali)

## Appendix B: File Source Map

This spec is derived from the following source files:

| Component | Source File |
|-----------|------------|
| Application entry point | `src/main/scala/sangeet/editor/MainApp.scala` |
| Editor pane (state, input, canvas) | `src/main/scala/sangeet/editor/EditorPane.scala` |
| Keyboard input handling | `src/main/scala/sangeet/editor/KeyHandler.scala` |
| Cursor model | `src/main/scala/sangeet/editor/CursorModel.scala` |
| Composition editor (state mutations) | `src/main/scala/sangeet/editor/CompositionEditor.scala` |
| Undo history | `src/main/scala/sangeet/editor/UndoHistory.scala` |
| Keyboard legend sidebar | `src/main/scala/sangeet/editor/KeyboardLegend.scala` |
| Status bar / log | `src/main/scala/sangeet/editor/StatusBar.scala` |
| Composition header panel | `src/main/scala/sangeet/editor/CompositionHeader.scala` |
| New composition dialog | `src/main/scala/sangeet/editor/NewCompositionDialog.scala` |
| Composition properties dialog | `src/main/scala/sangeet/editor/CompositionPropertiesDialog.scala` |
| Canvas rendering orchestrator | `src/main/scala/sangeet/render/CanvasRenderer.scala` |
| Grid line renderer | `src/main/scala/sangeet/render/GridRenderer.scala` |
| Swar glyph renderer | `src/main/scala/sangeet/render/SwarGlyph.scala` |
| Ornament renderer | `src/main/scala/sangeet/render/OrnamentRenderer.scala` |
| Color palette | `src/main/scala/sangeet/render/NotationColors.scala` |
| Script glyph mappings | `src/main/scala/sangeet/render/ScriptMap.scala` |
| Script runtime switching | `src/main/scala/sangeet/render/DevanagariMap.scala` |
| Layout configuration | `src/main/scala/sangeet/layout/LayoutConfig.scala` |
