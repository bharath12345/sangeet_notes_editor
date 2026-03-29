# Sangeet Notes Editor

<p align="center">
  <img src="packaging/icons/sangeet-icon-256.png" alt="Sangeet Notes Editor" width="128" />
</p>

A desktop notation editor for **Hindustani classical music**, designed for sitar compositions in the **Bhatkhande notation style**. Type notes on your keyboard, see them rendered in Devanagari, hear them through MIDI, and export to PDF or HTML.

## Features

- **Bhatkhande notation** — grid/tabular layout with Devanagari swar glyphs (सा, रे, ग, म, प, ध, नि)
- **Keyboard-driven input** — type `s r g m p d n` for notes, Shift for komal/tivra variants
- **Multi-script support** — Devanagari (default), with ScriptMap architecture for future scripts
- **11 built-in taals** — Teentaal, Ektaal, Jhaptaal, Rupak, Dadra, Keherwa, and more
- **26 built-in raags** — Yaman, Bhairav, Todi, Marwa, Hindol, Madmad Sarang, and more with full arohan/avrohan/pakad data
- **MIDI playback** — hear compositions with General MIDI sitar patch, play/pause/stop controls
- **Color-coded notation** — distinct colors for taal markers, swar, ornaments, octave dots, Da/Ra strokes, and sahitya across editor, PDF, and HTML
- **PDF export** — full multi-row rendering with Devanagari font support, ornaments, octave dots, strokes, and sahitya
- **HTML export** — browser-ready output with print-friendly CSS and all notation rows
- **`.swar` file format** — JSON-based, one file per composition
- **Sitar-specific** — mizrab strokes (Da/Ra), 10+ ornament types (meend, kan, murki, gamak, krintan, ghaseet, etc.)
- **Undo/redo** — full edit history
- **Section management** — add/remove/reorder sections (Sthayi, Antara, Taan, Jhala, Jod)
- **Sample composition** — opens with a rich Yaman Vilambit Gat showcasing all features
- **Cross-platform packaging** — native installers for macOS (.dmg), Windows (.msi), Linux (.deb) via GitHub Actions

## Keyboard Reference

| Key | Action |
|-----|--------|
| `s r g m p d n` | Enter swar (Sa Re Ga Ma Pa Dha Ni) |
| `Shift + key` | Komal variant (Re, Ga, Dha, Ni) or Tivra (Ma) |
| `ss rr gg` etc. | Dual swar (double-tap for SaSa, ReRe, etc.) |
| `.` (period) | Next note in mandra saptak (lower octave) |
| `'` (quote) | Next note in taar saptak (upper octave) |
| `` ` `` (backtick) | Return to madhya saptak |
| `Space` | Rest (silence) |
| `-` (minus) | Sustain (hold previous note) |
| `Backspace` | Delete last note |
| `Arrow keys` | Move cursor |
| `Tab` | Next section |
| `Ctrl+Z / Ctrl+Y` | Undo / Redo |
| `Ctrl+S` | Save |
| `Ctrl+E` | Export PDF |

## Download

Go to [Releases](../../releases) for pre-built installers (macOS `.dmg`, Windows `.msi`, Linux `.deb`). All installers include a bundled JVM — no Java installation required.

## Build from Source

**Requirements:** JDK 17+, sbt

```bash
# Run the app
sbt "runMain sangeet.editor.MainApp"

# Run tests (284 tests across 31 suites)
sbt test

# Build native installer for your platform
./packaging/package.sh
```

## Tech Stack

- **Scala 3** + **ScalaFX** (JavaFX wrapper)
- **circe** for JSON serialization
- **Apache PDFBox** for PDF export (with Noto Sans Devanagari font)
- **javax.sound.midi** for playback
- **ScalaTest** (284 tests)
- **sbt-assembly** + **jpackage** for native packaging
- **GitHub Actions** for CI/CD and cross-platform release builds

## Project Structure

```
sangeet/
  model/    — Pure domain types (Composition, Event, Swar, Taal, Raag, Ornament, Stroke)
  format/   — .swar JSON serialization (circe), PDF export (PDFBox), HTML export
  layout/   — BeatGrouper → LineBreaker → GridLayout
  render/   — Devanagari canvas rendering: SwarGlyph, OrnamentRenderer, GridRenderer, NotationColors
  audio/    — PlaybackScheduler, MidiEngine, PlaybackController
  editor/   — UI: MainApp, EditorPane, KeyHandler, CursorModel, CompositionHeader, StatusBar
  raag/     — 26 built-in raag definitions with arohan/avrohan/pakad
  taal/     — 11 built-in taal definitions
```

## License

This project is not yet licensed. All rights reserved.
