# Sangeet Notes Editor — Design Specification v1.0

## Overview

A desktop notation editor for Hindustani classical music, designed primarily for sitar compositions in the Bhatkhande notation style. The editor stores compositions as local `.swar` files (JSON), renders notation in Devanagari with a Bhatkhande-style grid layout, supports audio playback, and exports to PDF.

### Goals

- Faithfully represent Bhatkhande-style notation digitally
- First-class support for sitar-specific notation (mizrab strokes, krintan, ghaseet, jhala)
- Local-first: compositions stored as individual `.swar` files on disk
- Roman keyboard input with Devanagari visual rendering
- Extensible ornamentation system (new ornament types without code changes)
- Audio playback for notation verification
- PDF export for printing clean notation sheets
- Cross-platform desktop app (macOS, Windows, Linux)

### Non-Goals (v1)

- Web or mobile version (future)
- Multi-user collaboration
- Carnatic music notation (may work incidentally but not designed for it)
- Tablature or staff notation
- Raga rule enforcement (the editor does not prevent "wrong" notes)

---

## 1. Core Data Model

### 1.1 Composition (Top-Level)

A composition is the root object. One composition = one `.swar` file.

```
Composition
  metadata: Metadata
  sections: List[Section]
  tihais: List[Tihai]
```

### 1.2 Metadata

```scala
case class Metadata(
  title: String,
  compositionType: CompositionType,  // Bandish | Gat | Palta | Custom
  raag: Raag,
  taal: Taal,
  laya: Option[Laya],               // Optional — paltas may not have laya
  instrument: Option[String],        // "Sitar", etc.
  composer: Option[String],          // Original composer (for traditional compositions)
  author: Option[String],            // Who wrote this notation (student, guruji)
  source: Option[String],            // "Guruji class, March 2026"
  createdAt: String,                 // ISO 8601 datetime
  updatedAt: String
)

enum CompositionType:
  case Bandish
  case Gat
  case Palta
  case Custom(name: String)

enum Laya:
  case AtiVilambit
  case Vilambit
  case Madhya
  case Drut
  case AtiDrut
```

**Note on Paltas:** Paltas (practice exercises) may not have a laya since they are practiced at varying speeds. When laya is `None`, the BPM for playback is set manually by the user via the BPM slider.

### 1.3 Raag

Raag metadata is stored for reference and rendered in the composition header. The editor does not enforce raga rules.

```scala
case class Raag(
  name: String,                      // "Yaman"
  thaat: Option[String],             // "Kalyan"
  arohana: Option[List[String]],     // ["S", "R", "G", "M+", "P", "D", "N", "S'"]
  avarohana: Option[List[String]],   // ["S'", "N", "D", "P", "M+", "G", "R", "S"]
  vadi: Option[String],              // "G"
  samvadi: Option[String],           // "N"
  pakad: Option[String],             // Characteristic phrase
  prahar: Option[Int]                // Time of day (1-8)
)
```

**Arohana/Avarohana shorthand** (used in raag metadata only):
- `S` = madhya Sa, `S'` = taar Sa, `S.` = mandra Sa
- `R` = shuddha Re, `r` = komal Re
- `G` = shuddha Ga, `g` = komal Ga
- `M` = shuddha Ma, `M+` = tivra Ma
- `P` = Pa
- `D` = shuddha Dha, `d` = komal Dha
- `N` = shuddha Ni, `n` = komal Ni

### 1.4 Taal

```scala
case class Taal(
  name: String,                      // "Teentaal"
  matras: Int,                       // 16
  vibhags: List[Vibhag],            // Sections with beat counts and markers
  theka: Option[List[String]]       // Tabla bols
)

case class Vibhag(
  beats: Int,                        // Number of matras in this vibhag
  marker: VibhagMarker
)

enum VibhagMarker:
  case Sam                           // X — first beat of cycle
  case Taali(number: Int)            // 2, 3, 4... — clap positions
  case Khali                         // 0 — wave/empty beat
```

**Built-in taals:**

| Taal | Matras | Vibhags | Pattern |
|------|--------|---------|---------|
| Teentaal | 16 | 4+4+4+4 | X, 2, 0, 3 |
| Ektaal | 12 | 2+2+2+2+2+2 | X, 0, 2, 0, 3, 4 |
| Jhaptaal | 10 | 2+3+2+3 | X, 2, 0, 3 |
| Rupak | 7 | 3+2+2 | 0, 1, 2 |
| Dadra | 6 | 3+3 | X, 0 |
| Keherwa | 8 | 4+4 | X, 0 |
| Chautaal | 12 | 2+2+2+2+2+2 | X, 0, 2, 0, 3, 4 |
| Dhamar | 14 | 5+2+3+4 | X, 2, 0, 3 |
| Tilwada | 16 | 4+4+4+4 | X, 2, 0, 3 |
| Jhoomra | 14 | 3+4+3+4 | X, 2, 0, 3 |
| Deepchandi | 14 | 3+4+3+4 | X, 2, 0, 3 |

Custom taal definitions are supported — stored as JSON data, not hardcoded.

### 1.5 Section

```scala
case class Section(
  name: String,                      // "Sthayi", "Antara", "Taan 1", "Palta"
  sectionType: SectionType,
  events: List[Event]
)

enum SectionType:
  case Sthayi
  case Antara
  case Sanchari
  case Abhog
  case Taan
  case Toda
  case Jhala
  case Palta
  case Arohi
  case Avarohi
  case Custom(name: String)
```

### 1.6 Event (Stream Unit)

Every musical moment is an Event. The stream of events is the core abstraction from which all rendering, playback, and export is derived.

```scala
enum Event:
  case Swar(
    note: Note,                      // Sa, Re, Ga, Ma, Pa, Dha, Ni
    variant: Variant,                // Shuddha, Komal, Tivra
    octave: Octave,                  // Mandra, Madhya, Taar
    beat: BeatPosition,
    duration: Duration,              // Fraction of a beat
    stroke: Option[Stroke],          // Da, Ra, Chikari, Jod
    ornaments: List[Ornament],
    sahitya: Option[String]          // Lyric syllable aligned to this note
  )

  case Rest(
    beat: BeatPosition,
    duration: Duration
  )

  case Sustain(
    beat: BeatPosition,
    duration: Duration
  )

enum Note:
  case Sa, Re, Ga, Ma, Pa, Dha, Ni

enum Variant:
  case Shuddha, Komal, Tivra

enum Octave:
  case AtiMandra, Mandra, Madhya, Taar, AtiTaar
```

**Note:** The default range is 3 octaves (Mandra, Madhya, Taar). AtiMandra and AtiTaar are supported by the data model for extensibility but not expected in typical use.

### 1.7 BeatPosition

Precise placement of an event within the taal cycle.

```scala
case class BeatPosition(
  cycle: Int,                        // Which aavart (0-indexed)
  beat: Int,                         // Which matra (0-indexed, 0 = sam)
  subdivision: Rational              // Position within the beat
)

case class Rational(
  numerator: Int,
  denominator: Int
)
```

**Subdivision examples:**
- `0/1` — exactly on the beat
- `1/2` — halfway through the beat
- `1/3` — one-third into the beat
- `2/3` — two-thirds into the beat
- `1/4`, `1/5`, `1/6`, `1/7`, `1/8` — finer subdivisions

This supports 1 to 8 notes per beat at any sub-beat position.

### 1.8 Stroke (Sitar-Specific)

```scala
enum Stroke:
  case Da                            // Inward/downstroke (mizrab)
  case Ra                            // Outward/upstroke (mizrab)
  case Chikari                       // Open chikari string stroke
  case Jod                           // Jod string
```

### 1.9 Ornament (Extensible)

```scala
sealed trait Ornament

case class Meend(
  startNote: NoteRef,
  endNote: NoteRef,
  direction: MeendDirection,
  intermediateNotes: List[NoteRef]   // Notes touched along the way
) extends Ornament

case class KanSwar(
  graceNote: NoteRef                 // The grace note before the main note
) extends Ornament

case class Murki(
  notes: List[NoteRef]               // Quick ornamental note sequence
) extends Ornament

case class Gamak() extends Ornament  // Heavy oscillation on current note

case class Andolan() extends Ornament // Slow gentle oscillation

case class Krintan(
  notes: List[NoteRef]               // Pull-off note sequence (sitar)
) extends Ornament

case class Gitkari() extends Ornament // Hammer-on/pull-off trill (sitar)

case class Ghaseet(
  targetNote: NoteRef                // Heavy lateral string pull (sitar)
) extends Ornament

case class Sparsh(
  touchNote: NoteRef                 // Light touch of adjacent note
) extends Ornament

case class Zamzama(
  notes: List[NoteRef]               // Rapid repeated note cluster
) extends Ornament

case class CustomOrnament(
  name: String,
  parameters: Map[String, String]    // Flexible key-value params
) extends Ornament

case class NoteRef(
  note: Note,
  variant: Variant,
  octave: Octave
)

enum MeendDirection:
  case Ascending                     // Lower to higher (pulling string)
  case Descending                    // Higher to lower (releasing string)
```

The `CustomOrnament` type ensures extensibility — new ornament types can be defined without changing the data model or code.

### 1.10 Tihai

```scala
case class Tihai(
  sectionName: String,               // Which section this tihai belongs to
  startBeat: BeatPosition,
  landingBeat: BeatPosition          // Should typically be sam
)
```

The tihai phrase itself is the events between `startBeat` and `landingBeat` — the editor marks the region and renders it with a bracket showing the 3 repetitions.

### 1.11 Duration

Durations are expressed as fractions of a beat using Rational:

```scala
type Duration = Rational
```

- `1/1` = full beat
- `1/2` = half beat
- `1/3` = one-third beat
- `1/4` = quarter beat
- etc.

---

## 2. File Format

### 2.1 Format Specification

- **File extension:** `.swar`
- **Encoding:** UTF-8 JSON
- **One file per composition**
- **Version field** for forward compatibility

### 2.2 JSON Structure

```json
{
  "version": "1.0",
  "metadata": {
    "title": "Vilambit Gat in Yaman",
    "compositionType": "gat",
    "raag": {
      "name": "Yaman",
      "thaat": "Kalyan",
      "arohana": ["S", "R", "G", "M+", "P", "D", "N", "S'"],
      "avarohana": ["S'", "N", "D", "P", "M+", "G", "R", "S"],
      "vadi": "G",
      "samvadi": "N",
      "pakad": "N.RGR S - R G, M+GM+DN.R S",
      "prahar": 1
    },
    "taal": {
      "name": "Teentaal",
      "matras": 16,
      "vibhags": [
        { "beats": 4, "marker": "sam" },
        { "beats": 4, "marker": { "taali": 2 } },
        { "beats": 4, "marker": "khali" },
        { "beats": 4, "marker": { "taali": 3 } }
      ],
      "theka": ["Dha","Dhin","Dhin","Dha","Dha","Dhin","Dhin","Dha",
                 "Dha","Tin","Tin","Ta","Ta","Dhin","Dhin","Dha"]
    },
    "laya": "vilambit",
    "instrument": "Sitar",
    "composer": "Traditional",
    "author": "Bharadwaj",
    "source": "Guruji class, March 2026",
    "createdAt": "2026-03-28T10:00:00Z",
    "updatedAt": "2026-03-28T10:30:00Z"
  },
  "sections": [
    {
      "name": "Sthayi",
      "type": "sthayi",
      "events": [
        {
          "type": "swar",
          "note": "P",
          "variant": "shuddha",
          "octave": "madhya",
          "beat": { "cycle": 0, "beat": 12, "subdivision": [0, 1] },
          "duration": [1, 1],
          "stroke": "da",
          "ornaments": []
        },
        {
          "type": "swar",
          "note": "M",
          "variant": "tivra",
          "octave": "madhya",
          "beat": { "cycle": 0, "beat": 13, "subdivision": [0, 1] },
          "duration": [1, 2],
          "stroke": "ra",
          "ornaments": [
            {
              "type": "meend",
              "startNote": { "note": "M", "variant": "tivra", "octave": "madhya" },
              "endNote": { "note": "G", "variant": "shuddha", "octave": "madhya" },
              "direction": "descending",
              "intermediateNotes": []
            }
          ]
        },
        {
          "type": "rest",
          "beat": { "cycle": 0, "beat": 14, "subdivision": [0, 1] },
          "duration": [1, 1]
        },
        {
          "type": "sustain",
          "beat": { "cycle": 0, "beat": 15, "subdivision": [0, 1] },
          "duration": [1, 1]
        }
      ]
    }
  ],
  "tihais": [
    {
      "section": "Sthayi",
      "startBeat": { "cycle": 2, "beat": 8, "subdivision": [0, 1] },
      "landingBeat": { "cycle": 3, "beat": 0, "subdivision": [0, 1] }
    }
  ]
}
```

### 2.3 Serialization Conventions

- **Rationals** are serialized as 2-element arrays: `[numerator, denominator]`. Example: `[1, 2]` = 1/2.
- **Enums** are serialized as lowercase strings: `"shuddha"`, `"komal"`, `"tivra"`, `"mandra"`, `"madhya"`, `"taar"`.
- **Optional fields** are omitted when null (not serialized as `null`).
- **Ornament type** is a discriminator field: `"type": "meend"`, `"type": "kan_swar"`, etc.
- **Custom ornaments** use `"type": "custom"` with a `"name"` field and `"parameters"` map.

### 2.4 Versioning

The `"version": "1.0"` field allows future format evolution. The editor will:
- Read any version it understands
- Warn on unknown versions but attempt best-effort parsing
- Always write the current version

---

## 3. Rendering & Layout Engine

### 3.1 Pipeline

```
Event Stream → Beat Grouping → Line Breaking → Grid Layout → Visual Rendering
```

Each stage is a pure transformation, producing an intermediate data structure consumed by the next stage.

### 3.2 Beat Grouping

- Group events by `(cycle, beat)`
- Each group becomes a "cell" — a list of swar events ordered by subdivision position
- Cells are organized into cycles, cycles into sections

### 3.3 Line Breaking

The layout engine decides how to split taal cycles into visual lines based on note density:

| Density | Notes per beat | Layout |
|---------|---------------|--------|
| Low | 1-2 | Full cycle per line |
| Medium | 2-4 | Full cycle per line, wider cells |
| High | 4-8 | Split by vibhag (one or two vibhags per line) |

The user can manually override the line breaking strategy per section.

### 3.4 Grid Layout

Each line becomes a visual row with the following layers:

```
┌─────────────────────────────────────────────────────┐
│ Taal markers:   X              2           0        │  sam/taali/khali
├─────────────────────────────────────────────────────┤
│ Swar line:      ग  म+ प  -  │  ध  नि सा' - │ ...  │  Devanagari swaras
│ Stroke line:    Da Ra Da     │  Ra Da  Da    │      │  mizrab indicators
├─────────────────────────────────────────────────────┤
│ Sahitya line:   जा  ग  त    │  के  र  खा   │ ...  │  lyrics (if present)
├─────────────────────────────────────────────────────┤
│ Ornament overlays: meend arcs, kan superscripts      │  drawn on top
└─────────────────────────────────────────────────────┘
```

### 3.5 Cell Width

**Fixed width with overflow:** Cells have a standard width. When a beat contains many notes (4+), the cell expands to accommodate them. This preserves the grid feel while handling variable density.

### 3.6 Composition Header

Rendered at the top of the composition (both on screen and in PDF):

```
Raag: Yaman (Kalyan Thaat)
Arohi:   S R G M+ P D N S'
Avarohi: S' N D P M+ G R S
Vadi: G  |  Samvadi: N
Taal: Teentaal (16 matras)  |  Laya: Vilambit
Composer: Traditional  |  Source: Guruji class, March 2026
```

### 3.7 Swar Glyph Rendering

Each swar is rendered as a Devanagari glyph with modifiers:

- **Base glyph:** स, रे, ग, म, प, ध, नि
- **Komal indicator:** Small horizontal line below the glyph
- **Tivra indicator:** Small vertical stroke above म
- **Octave dots:** Dot below for mandra, dot above for taar, no dot for madhya
- **Combined rendering:** A komal Re in mandra saptak = रे with underline AND dot below

### 3.8 Ornament Rendering

| Ornament | Visual |
|----------|--------|
| Meend (ascending) | Upward-curving arc connecting start and end notes |
| Meend (descending) | Downward-curving arc connecting start and end notes |
| Kan swar | Small superscript Devanagari glyph before the main note |
| Murki | Group of small notes in parentheses |
| Gamak | Wavy line above the note |
| Andolan | Gentler wavy line above the note |
| Krintan | Downward curve connecting pull-off notes |
| Gitkari | Trill-like wavy between two notes |
| Ghaseet | Heavy arc with directional arrow |
| Sparsh | Tiny superscript dot-note |
| Zamzama | Rapid note cluster in brackets |
| Tihai | Bracket spanning the phrase with "x3" marker |

Meend arcs are drawn as cubic Bezier curves. The curve direction (up/down) indicates ascending/descending meend.

### 3.9 Mukhda / Pickup Handling

When a section starts before sam (e.g., mukhda begins at beat 13 of a Teentaal cycle):

- The first line shows only the pickup beats, right-aligned
- Sam is visually accented (bold border or highlight)
- A double bar marker indicates the sam landing point
- Subsequent cycles render as full lines

### 3.10 Shared Layout Engine

The same layout engine drives both screen rendering (ScalaFX Canvas) and PDF export. The layout engine outputs a `RenderedGrid` — a list of positioned glyphs, lines, arcs, and text — which is consumed by either the Canvas renderer or the PDF renderer.

---

## 4. Editor UI & Interaction

### 4.1 Application Layout

```
┌──────────────────────────────────────────────────────────────┐
│  Menu Bar: File | Edit | View | Composition | Playback       │
├──────────────────────────────────────────────────────────────┤
│  Toolbar: [Taal: Teentaal ▼] [Laya: Vilambit ▼]  [BPM: 40] │
│           [Section: Sthayi ▼] [+ Section]                    │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─ Composition Header ──────────────────────────────────┐   │
│  │  Raag: Yaman | Arohi: S R G M+ P D N S'              │   │
│  │  Avarohi: S' N D P M+ G R S | Taal: Teentaal         │   │
│  └────────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌─ Section: Sthayi ─────────────────────────────────────┐   │
│  │  X              2              0              3       │   │
│  │  सा रे ग  म+ │ प  ध  नि सा' │ नि  ध  प  म+ │ ...  │   │
│  │  Da Ra Da Ra  │ Da Ra  Da Ra  │ Ra  Da Ra Da  │       │   │
│  │                                                       │   │
│  │  X              2              0              3       │   │
│  │  ...next cycle...                                     │   │
│  └───────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌─ Section: Antara ─────────────────────────────────────┐   │
│  │  ...                                                  │   │
│  └───────────────────────────────────────────────────────┘   │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Status: Beat 5 | Cycle 1 | Madhya Saptak     [Play] [Stop] │
└──────────────────────────────────────────────────────────────┘
```

### 4.2 Keyboard Input — Swar Entry

| Key | Action |
|-----|--------|
| `s` | Sa |
| `r` | Shuddha Re |
| `g` | Shuddha Ga |
| `m` | Shuddha Ma |
| `p` | Pa |
| `d` | Shuddha Dha |
| `n` | Shuddha Ni |
| `Shift+R` | Komal Re |
| `Shift+G` | Komal Ga |
| `Shift+M` | Tivra Ma |
| `Shift+D` | Komal Dha |
| `Shift+N` | Komal Ni |
| `-` | Sustain (dash) |
| `Space` | Rest |
| `.` | Next note in mandra saptak |
| `'` | Next note in taar saptak |
| `→` / `←` | Move cursor to next/previous beat |
| `Tab` | Move to next beat |
| `Enter` | Move to next cycle |

**Dual swar shortcut:** Double-tap a swar key to create a repeated pair. `ss` = SaSa, `rr` = ReRe, `gg` = GaGa, etc. The beat is automatically subdivided into 2.

### 4.3 Sub-Beat Entry

- `Ctrl+2` through `Ctrl+8` — set subdivision for the current beat (2-8 slots)
- Alternatively, just keep typing swaras within a beat — the editor auto-subdivides

### 4.4 Ornament Shortcuts

| Shortcut | Ornament |
|----------|----------|
| `Alt+M` | Meend — select start note, then end note |
| `Alt+K` | Kan swar — next note typed becomes a grace note |
| `Alt+U` | Murki — enter quick note sequence in parentheses |
| `Alt+G` | Gamak on current note |
| `Alt+A` | Andolan on current note |
| `Alt+R` | Krintan |
| `Alt+I` | Gitkari |
| `Alt+H` | Ghaseet |
| `Alt+S` | Sparsh |
| `Alt+Z` | Zamzama |
| `Alt+T` | Tihai — mark start and end |

### 4.5 Stroke Entry (Sitar)

| Shortcut | Stroke |
|----------|--------|
| `Ctrl+D` | Da (inward/downstroke) |
| `Ctrl+R` | Ra (outward/upstroke) |
| `Ctrl+C` | Chikari |
| `Ctrl+J` | Jod string |

Strokes attach to the current or next swar event. A default stroke pattern can be set per section.

### 4.6 Sahitya (Lyrics) Entry

- `Ctrl+L` toggles sahitya mode
- Cursor drops to the lyrics row below the swar line
- Space-separated syllables align with corresponding beats
- Full Unicode support: Devanagari, Kannada, Telugu, and any other script

### 4.7 Mouse/Click Interaction

- Click any beat cell to place cursor
- Right-click for context menu (add ornament, change octave, set stroke)
- Drag between two notes to create a meend arc
- Click section header to rename or change section type

### 4.8 Composition Metadata Dialog

`Ctrl+I` opens a metadata dialog for editing:
- Title, composition type
- Raag: name, thaat, arohana, avarohana, vadi, samvadi, pakad, prahar
- Taal: selection from built-in list or custom definition
- Laya, instrument, composer, author, source

---

## 5. Audio Playback

### 5.1 Architecture

```
Event Stream → Playback Scheduler → Sound Engine → Audio Output
                    |                      |
              Tempo/BPM clock        Sound Source (MIDI or Sampled)
```

### 5.2 Playback Scheduler

- Reads events from the stream in order
- Converts BeatPosition to absolute time using BPM
- Handles sub-beat timing precisely
- Respects sustain (holds note) and rest (silence) events

### 5.3 Sound Engine — Two Tiers

**Basic Tier (MIDI):**
- Uses Java's built-in MIDI synthesizer
- General MIDI sitar patch (#104) for melodic notes
- MIDI percussion for tabla theka accompaniment
- Immediate to implement, serviceable quality
- Meend approximated via MIDI pitch bend

**Rich Tier (Hybrid Sampled + Synthesized):**
- ~72 core samples: 12 notes x 3 octaves x 2 strokes (Da/Ra)
- Additional samples: chikari strokes, jod string
- Ornaments synthesized from base samples:
  - Meend: crossfade + pitch-shift interpolation between note samples
  - Gamak: periodic pitch oscillation applied via DSP
  - Kan swar: 30-50ms trigger of grace note sample before main note
  - Krintan: rapid sequential playback of descending note samples
  - Murki: rapid sequential triggering at short intervals
- Samples sourced by recording user's own sitar or from sample libraries

### 5.4 Playback Controls

- **Play / Pause / Stop**
- **Play from cursor** — start at current beat position
- **Play section** — play only the current section
- **Loop** — loop current section or entire composition
- **BPM slider** — adjustable tempo
  - Ati-vilambit default: 20-30 BPM
  - Vilambit default: 30-60 BPM
  - Madhya default: 60-120 BPM
  - Drut default: 120-250 BPM
- **Cursor follows playback** — grid highlights current beat during playback

### 5.5 Taal Accompaniment

- Optional tabla theka playback alongside composition
- Sam gets an audible accent
- Toggleable independently of melodic playback

---

## 6. PDF Export

### 6.1 Rendering

The same layout engine that drives screen rendering also generates PDF. This ensures WYSIWYG output.

### 6.2 Page Layout

```
┌─────────────────────────────────────────────┐
│  Raag: Yaman (Kalyan Thaat)          Page 1 │
│  Arohi:   S R G M+ P D N S'                │
│  Avarohi: S' N D P M+ G R S                │
│  Vadi: G | Samvadi: N                       │
│  Taal: Teentaal | Laya: Vilambit            │
│  Composer: Traditional                      │
├─────────────────────────────────────────────┤
│                                             │
│  ── Sthayi ──────────────────────────────── │
│  [notation grid]                            │
│                                             │
│  ── Antara ──────────────────────────────── │
│  [notation grid]                            │
│                                             │
├─────────────────────────────────────────────┤
│  Sangeet Notes Editor                     1 │
└─────────────────────────────────────────────┘
```

### 6.3 PDF Features

- **Page size:** A4 (default), Letter, configurable
- **Orientation:** Landscape for dense compositions, portrait for sparse
- **Font:** Embedded Noto Sans Devanagari (or similar Unicode font)
- **Vector rendering:** All notation elements (glyphs, arcs, lines) rendered as PDF vector paths for crisp output at any resolution
- **Multi-page:** Automatic page breaks between sections or after N cycles
- **Header:** Raag, arohi, avarohi, taal, laya, composer info
- **Footer:** Page numbers, application name

---

## 7. Project Architecture

### 7.1 Technology Stack

- **Language:** Scala 3
- **UI Framework:** ScalaFX (wrapper over JavaFX)
- **JSON:** circe (serialization/deserialization)
- **PDF:** Apache PDFBox
- **Audio:** javax.sound.midi (Basic), javax.sound.sampled (Rich)
- **Build:** sbt
- **Testing:** ScalaTest
- **Target JVM:** 17+

### 7.2 Module Layout

```
sangeet-notes-editor/
├── build.sbt
├── project/
│   └── plugins.sbt
├── src/
│   ├── main/
│   │   ├── scala/
│   │   │   └── sangeet/
│   │   │       ├── model/                # Core domain model (pure, no dependencies)
│   │   │       │   ├── Swar.scala        # Note, Variant, Octave enums
│   │   │       │   ├── Taal.scala        # Taal, Vibhag, VibhagMarker
│   │   │       │   ├── Raag.scala        # Raag metadata
│   │   │       │   ├── Event.scala       # Event ADT (Swar, Rest, Sustain)
│   │   │       │   ├── Ornament.scala    # Ornament type hierarchy
│   │   │       │   ├── Section.scala     # Section, SectionType
│   │   │       │   ├── Composition.scala # Top-level composition
│   │   │       │   └── Stroke.scala      # Da, Ra, Chikari, Jod
│   │   │       │
│   │   │       ├── format/               # File I/O
│   │   │       │   ├── SwarFormat.scala  # .swar JSON serialization
│   │   │       │   └── PdfExport.scala   # PDF generation
│   │   │       │
│   │   │       ├── layout/              # Layout engine (pure)
│   │   │       │   ├── BeatGrouper.scala    # Events → beat cells
│   │   │       │   ├── LineBreaker.scala    # Cells → lines (density-aware)
│   │   │       │   ├── GridLayout.scala     # Lines → positioned grid
│   │   │       │   └── LayoutConfig.scala   # Density, spacing settings
│   │   │       │
│   │   │       ├── render/              # Visual rendering
│   │   │       │   ├── SwarGlyph.scala      # Devanagari with dots/lines
│   │   │       │   ├── OrnamentRenderer.scala # Meend arcs, gamak waves
│   │   │       │   ├── GridRenderer.scala   # Grid lines, vibhag markers
│   │   │       │   ├── TihaiRenderer.scala  # Tihai brackets
│   │   │       │   └── CanvasRenderer.scala # ScalaFX Canvas coordinator
│   │   │       │
│   │   │       ├── audio/               # Playback engine
│   │   │       │   ├── PlaybackScheduler.scala
│   │   │       │   ├── MidiEngine.scala     # Basic MIDI playback
│   │   │       │   ├── SampleEngine.scala   # Rich sampled playback
│   │   │       │   └── MeendSynth.scala     # Meend pitch interpolation
│   │   │       │
│   │   │       ├── editor/              # UI layer
│   │   │       │   ├── MainApp.scala        # Application entry point
│   │   │       │   ├── EditorPane.scala     # Main notation editing area
│   │   │       │   ├── ToolBar.scala        # Controls
│   │   │       │   ├── KeyHandler.scala     # Keyboard input
│   │   │       │   ├── CursorModel.scala    # Cursor position & navigation
│   │   │       │   ├── MetadataDialog.scala # Composition info dialog
│   │   │       │   └── SahityaEditor.scala  # Lyrics input mode
│   │   │       │
│   │   │       └── taal/                # Built-in taal definitions
│   │   │           └── Taals.scala
│   │   │
│   │   └── resources/
│   │       ├── fonts/
│   │       │   └── NotoSansDevanagari.ttf
│   │       ├── samples/                 # Audio samples (Rich tier)
│   │       └── taals/                   # Taal definitions as JSON
│   │
│   └── test/
│       └── scala/
│           └── sangeet/
│               ├── model/               # Domain model tests
│               ├── format/              # Serialization roundtrip tests
│               ├── layout/              # Layout engine tests
│               └── audio/               # Playback timing tests
│
├── docs/
│   └── superpowers/
│       └── specs/
│           └── 2026-03-28-sangeet-notes-editor-design.md
│
└── samples/                             # Example .swar files
    ├── yaman-vilambit-gat.swar
    ├── yaman-palta-1.swar
    └── bhimpalasi-drut-gat.swar
```

### 7.3 Key Architectural Principles

1. **Model is pure** — no UI or I/O dependencies in `sangeet.model`. This package can be reused in a future ScalaJS web version.
2. **Layout is separate from rendering** — the layout engine computes positions as data; renderers (Canvas, PDF) consume that data. Adding a new output format means writing a new renderer, not changing the layout engine.
3. **Audio is pluggable** — `MidiEngine` and `SampleEngine` implement a common `SoundEngine` trait. Start with MIDI, swap in sampled audio later.
4. **Taals are data, not code** — built-in taals are defined in JSON resource files. Users can add custom taals without modifying source code.
5. **Ornaments are extensible** — the `CustomOrnament` type allows new ornament types without schema or code changes.
6. **Format versioning** — the `.swar` file format includes a version field for forward-compatible evolution.

### 7.4 Dependencies

```scala
// build.sbt
val scala3Version = "3.4.x"

lazy val root = project
  .in(file("."))
  .settings(
    name := "sangeet-notes-editor",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.scalafx"       %% "scalafx"        % "21.0.0-R32",
      "io.circe"          %% "circe-core"     % "0.14.7",
      "io.circe"          %% "circe-parser"   % "0.14.7",
      "io.circe"          %% "circe-generic"  % "0.14.7",
      "org.apache.pdfbox"  % "pdfbox"         % "3.0.2",
      "org.scalatest"     %% "scalatest"      % "3.2.18" % Test
    )
  )
```

---

## Appendix A: Built-In Taal Reference

| Taal | Matras | Vibhag Pattern | Markers | Common Use |
|------|--------|---------------|---------|------------|
| Teentaal | 16 | 4+4+4+4 | X, 2, 0, 3 | Khayal, gat, most common |
| Ektaal | 12 | 2+2+2+2+2+2 | X, 0, 2, 0, 3, 4 | Vilambit khayal, dhrupad |
| Jhaptaal | 10 | 2+3+2+3 | X, 2, 0, 3 | Medium khayal, instrumental |
| Rupak | 7 | 3+2+2 | 0, 1, 2 | Khayal (sam=khali) |
| Dadra | 6 | 3+3 | X, 0 | Light classical, thumri |
| Keherwa | 8 | 4+4 | X, 0 | Light/semi-classical, folk |
| Chautaal | 12 | 2+2+2+2+2+2 | X, 0, 2, 0, 3, 4 | Dhrupad |
| Dhamar | 14 | 5+2+3+4 | X, 2, 0, 3 | Dhamar genre (Holi) |
| Tilwada | 16 | 4+4+4+4 | X, 2, 0, 3 | Vilambit khayal |
| Jhoomra | 14 | 3+4+3+4 | X, 2, 0, 3 | Vilambit khayal |
| Deepchandi | 14 | 3+4+3+4 | X, 2, 0, 3 | Thumri |

## Appendix B: Ornament Reference

| Ornament | Hindi | Description | Sitar Technique |
|----------|-------|-------------|-----------------|
| Meend | मींड | Continuous glide between notes | Lateral string pull/release |
| Kan Swar | कण स्वर | Grace note before main note | Quick pluck before landing |
| Murki | मुर्की | Rapid ornamental turn (3-5 notes) | Quick finger sequence |
| Gamak | गमक | Heavy oscillation between notes | Forceful repeated pulls |
| Andolan | आंदोलन | Slow gentle oscillation | Subtle finger movement |
| Krintan | कृंतन | Pull-off creating descending notes | Left-hand pull-off |
| Gitkari | गिटकरी | Hammer-on/pull-off trill | Alternating hammer/pull |
| Ghaseet | घसीट | Heavy lateral string pull (long meend) | Strong sideways pull |
| Sparsh | स्पर्श | Light touch of adjacent note | Brief contact with string |
| Zamzama | ज़मज़मा | Rapid repeated note cluster | Fast repeated strokes |

## Appendix C: Swar Devanagari Mapping

| Roman Input | Devanagari Glyph | Note |
|-------------|-----------------|------|
| S / s | स | Sa (Shadja) |
| R / r | रे | Shuddha Re (Rishabh) |
| G / g | ग | Shuddha Ga (Gandhar) |
| M / m | म | Shuddha Ma (Madhyam) |
| P / p | प | Pa (Pancham) |
| D / d | ध | Shuddha Dha (Dhaivat) |
| N / n | नि | Shuddha Ni (Nishad) |
| Shift+R | रे̱ | Komal Re |
| Shift+G | ग̱ | Komal Ga |
| Shift+M | म॑ | Tivra Ma |
| Shift+D | ध̱ | Komal Dha |
| Shift+N | नि̱ | Komal Ni |
