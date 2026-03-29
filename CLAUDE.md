# Sangeet Notes Editor

## Project Summary

A desktop notation editor for Hindustani classical music, designed primarily for sitar compositions in the Bhatkhande notation style. Local-first: compositions stored as `.swar` files (JSON) on disk. Built with Scala 3 + ScalaFX.

The full design spec is at `docs/superpowers/specs/2026-03-28-sangeet-notes-editor-design.md`. Read it before doing any implementation work — it is the source of truth for all design decisions.

## The User

Bharadwaj is learning sitar under a guruji. He has a physical notebook full of compositions (raags — their gat, antara, taan, toda, palta, etc.) that he wants to digitize. He is technically capable and has opinions about technology choices. He chose Scala 3 specifically — do not suggest switching languages. He values getting the core model right before building UI. He wants to start local/desktop before considering web/mobile.

His guruji teaches him Hindustani classical music primarily, but occasionally Carnatic compositions too. Sahitya (lyrics) can be in multiple Indian languages including Hindi, Kannada, Telugu, Sanskrit, Braj Bhasha.

## Technology Stack — Non-Negotiable

- **Language:** Scala 3 (use Scala 3 idioms: enum, case class, extension methods, given/using, match types where appropriate)
- **UI:** ScalaFX (wrapper over JavaFX)
- **JSON:** circe
- **PDF:** Apache PDFBox
- **Audio:** javax.sound.midi (Basic tier), javax.sound.sampled (Rich tier)
- **Build:** sbt
- **Testing:** ScalaTest
- **Target JVM:** 17+

## Domain Knowledge — Hindustani Classical Music

This is essential context for understanding the data model and making correct implementation decisions.

### Swar (Notes)
- 7 base notes: Sa, Re, Ga, Ma, Pa, Dha, Ni
- Sa and Pa are fixed (achal) — no komal/tivra variants
- Re, Ga, Dha, Ni can be komal (flat) — indicated by underline in Bhatkhande notation
- Ma can be tivra (sharp) — indicated by vertical stroke above in Bhatkhande notation
- This gives 12 chromatic notes per octave
- Roman input mapping: lowercase = shuddha, Shift = komal/tivra (Shift+R = komal Re, Shift+M = tivra Ma)

### Octaves (Saptak)
- Primary range: Mandra (lower), Madhya (middle, default/unmarked), Taar (upper)
- Extended range (supported in data model, rarely used): Ati-Mandra, Ati-Taar
- Bhatkhande convention: dot below = mandra, no dot = madhya, dot above = taar

### Taal (Rhythmic Cycle)
- A taal is a repeating cycle of matras (beats) divided into vibhags (sections)
- Each vibhag has a marker: Sam (X, first beat), Taali (numbered clap), Khali (0, wave)
- Common taals: Teentaal (16), Ektaal (12), Jhaptaal (10), Rupak (7), Dadra (6), Keherwa (8)
- Rupak is unusual: sam coincides with khali — handle this edge case
- Custom taals must be supported (stored as JSON data, not hardcoded)

### Beat Subdivision
- 1 to 8 notes can fall on a single beat
- Notes can fall at any sub-position: on the beat, halfway, one-third, one-quarter, up to one-eighth
- The data model uses Rational (numerator/denominator) for precise sub-beat positioning
- Dual swaras (SaSa, ReRe, GaGa) are common — double-tap shortcut: `ss`, `rr`, `gg` etc.

### Composition Structure
- **Bandish**: vocal composition with sthayi, antara, sanchari (rare), abhog (rare)
- **Gat**: instrumental (sitar) composition — masitkhani (vilambit), razakhani (drut)
- **Palta**: practice exercise/pattern. Has taal but NO laya (practiced at varying speeds). Can be authored by student or guruji.
- **Sections**: Sthayi, Antara, Sanchari, Abhog, Taan (numbered), Toda (numbered), Jhala, Palta, Arohi, Avarohi, Custom
- **Mukhda**: opening phrase that typically starts BEFORE sam and resolves on sam. The editor must handle pickup beats before sam.
- **Tihai**: rhythmic phrase repeated 3 times, landing on sam. Needs visual bracket with "x3" marker.

### Sitar-Specific Notation
- **Mizrab strokes**: Da (inward/down), Ra (outward/up) — MUST be notated
- **Strings**: main string, jod string, chikari strings
- **Krintan**: left-hand pull-off
- **Gitkari**: hammer-on/pull-off trill
- **Ghaseet**: heavy lateral string pull (a type of long meend)
- **Jhala**: rapid alternation between melody and open chikari strings

### Ornamentations
All these must be supported, plus a CustomOrnament type for extensibility:
- **Meend**: glide between notes. Has direction (Ascending = pulling string, Descending = releasing). Has start note, end note, optional intermediate notes. Does NOT store fret position — that's physical technique knowledge, not notation.
- **Kan Swar**: grace note before main note
- **Murki**: rapid ornamental turn (3-5 notes)
- **Gamak**: heavy oscillation
- **Andolan**: slow gentle oscillation
- **Krintan**: sitar pull-off sequence
- **Gitkari**: sitar hammer/pull trill
- **Ghaseet**: sitar heavy lateral pull
- **Sparsh**: light touch of adjacent note
- **Zamzama**: rapid repeated note cluster
- The ornament system MUST be extensible — guruji may teach new techniques in the future

### Laya (Tempo)
- Ati-vilambit (very slow, 20-30 BPM), Vilambit (slow, 30-60), Madhya (medium, 60-120), Drut (fast, 120-250), Ati-drut (very fast, 250+)
- BPM = matras per minute
- Vilambit compositions have high note density per beat (4-8 notes), drut have low (1-2)
- Paltas have no laya — BPM set manually via slider

### Rendering — Bhatkhande Style
- Roman keyboard input → Devanagari visual rendering (स, रे, ग, म, प, ध, नि)
- Grid/tabular layout: columns = beats, rows = taal cycles
- Vibhag separators as vertical lines, sam/taali/khali markers above
- Arohi and Avarohi displayed in composition header
- Sahitya (lyrics) row below swar row, aligned by beat
- Stroke indicators (Da/Ra) below swar row
- Fixed cell width with overflow for dense beats
- Density-aware line breaking: full cycle per line (drut) vs split by vibhag (vilambit)
- User can manually override line breaking per section

## Architecture Principles

1. **Model is pure** — `sangeet.model` package has zero UI/IO dependencies. Must be reusable for future ScalaJS web version.
2. **Layout is separate from rendering** — layout engine computes positions as data (RenderedGrid), renderers (Canvas, PDF) consume it.
3. **Audio is pluggable** — MidiEngine and SampleEngine share a common SoundEngine trait.
4. **Taals are data, not code** — JSON resource files, user can add custom taals.
5. **Ornaments are extensible** — CustomOrnament with Map[String, String] parameters.
6. **Format versioning** — `.swar` files include `"version": "1.0"` field.

## File Format

- Extension: `.swar`
- One file per composition
- UTF-8 JSON
- Rationals serialized as `[numerator, denominator]` arrays
- Enums serialized as lowercase strings
- Optional fields omitted when absent (not serialized as null)
- Ornament type uses discriminator field: `"type": "meend"` etc.
- See spec Section 2 for full JSON example

## Module Layout

```
sangeet/
  model/        — Pure domain types (Composition, Event, Swar, Taal, Raag, Ornament, Stroke, Section)
  format/       — .swar JSON serialization (circe), PDF export (PDFBox), HTML export
  layout/       — Layout engine: BeatGrouper → LineBreaker → GridLayout
  render/       — ScalaFX Canvas rendering: SwarGlyph, OrnamentRenderer, GridRenderer, NotationColors, ScriptMap
  audio/        — Playback: PlaybackScheduler, MidiEngine, PlaybackController
  editor/       — UI: MainApp, EditorPane, KeyHandler, CursorModel, CompositionHeader, SampleComposition
  raag/         — 26 built-in raag definitions (Raags.scala)
  taal/         — 11 built-in taal definitions
```

## Current Implementation State

### What's Built
- Full composition model with events, sections, ornaments, strokes, sahitya, tihai
- Canvas editor with keyboard input, cursor navigation, section switching, undo/redo
- Grid layout engine (BeatGrouper → LineBreaker → GridLayout) with density-aware line breaking
- MIDI playback with play/pause/stop
- PDF export with Devanagari font (Noto Sans Devanagari), mixed-script text rendering, all 5 notation rows
- HTML export with print-friendly CSS and all notation rows
- Color-coded notation: shared NotationColors palette used across canvas, PDF, and HTML renderers
- 26 raags with full metadata (arohan, avrohan, vadi, samvadi, pakad, thaat, prahar)
- 11 taals with vibhag structure and markers
- Sample Yaman Vilambit Gat loaded on startup (read-only) showcasing all features
- GitHub Actions CI/CD with cross-platform packaging (macOS .dmg, Windows .msi, Linux .deb)
- 284 tests across 31 suites

### Notation Row Rendering (5 rows per grid line)
Each taal cycle line renders these rows top-to-bottom:
1. **Taal markers** — Sam (X), Taali (2,3...), Khali (0) — dark red
2. **Ornaments** — meend, kan, gamak, andolan, etc. — deep purple
3. **Swar** — note glyphs with octave dots above/below and komal/tivra marks — dark indigo (dots in orange)
4. **Da/Ra strokes** — mizrab stroke indicators — teal
5. **Sahitya** — lyrics aligned per beat — dark green

### PDF Export Font Handling
- Devanagari text uses embedded Noto Sans Devanagari font
- Latin/ASCII text uses Helvetica
- `splitByScript` splits mixed text into (text, isIndic) runs for proper font selection
- `sanitizeForFont` replaces Unicode punctuation (em dash, smart quotes) with ASCII equivalents
- `isIndicChar` checks specific Unicode blocks (Devanagari, Bengali, Gujarati, etc.)

### Tihai Model
- Tihai belongs inside `Section` as `Option[Tihai]`, not at `Composition` level
- A section can have zero or one tihai (tihais are part of specific taans, not composition-wide)

## Key Design Decisions (Do Not Revisit)

- Bhatkhande notation style (not Paluskar)
- Hybrid architecture: stream data model with grid rendering
- Roman input / Devanagari output
- 3 octaves default (mandra, madhya, taar), data model supports 5
- `.swar` file extension (not `.sangeet`)
- One file per composition (not notebook/collection format)
- Audio playback is essential (not optional)
- MIDI basic tier first, then hybrid sampled Rich tier
- Fixed cell width with overflow (not proportional)
- Scala 3 + ScalaFX (user specifically chose this over other options)
- circe for JSON (not play-json, not upickle)
- Cross-platform via JVM (not native macOS-only)

## Coding Conventions

- Use Scala 3 syntax: `enum`, `case class`, `extension`, `given`/`using`, `derives`
- Prefer immutable data structures
- Use algebraic data types (sealed trait / enum) for closed hierarchies
- Use circe semi-auto derivation for JSON codecs
- Tests in ScalaTest with FunSuite or AnyFlatSpec style
- No println debugging — use proper logging if needed
