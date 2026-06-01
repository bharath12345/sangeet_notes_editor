# Sangeet Notes Editor

<p align="center">
  <img src="packaging/icons/sangeet-icon-256.png" alt="Sangeet Notes Editor" width="128" />
</p>

A multi-platform notation editor for **Hindustani classical music**, designed for sitar compositions in the **Bhatkhande notation style**. Type notes on your keyboard, see them rendered in Devanagari, hear them through MIDI, and export to PDF or HTML.

## Platforms

| Platform | Tech Stack | Status |
|----------|-----------|--------|
| **Desktop** | Scala 3 + ScalaFX (JavaFX) | Full-featured editor |
| **Web** | Elm 0.19 frontend + Scala 3 / Tapir REST backend | Editor at feature parity with desktop |
| **Android** | Planned | Not started |

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

## Download

Go to [Releases](../../releases) for pre-built installers (macOS `.dmg`, Windows `.msi`, Linux `.deb`). All installers include a bundled JVM — no Java installation required.

## Prerequisites

- **JDK 17+** (Temurin recommended)
- **sbt** (Scala build tool)
- **Node.js + npm** (for web frontend only)

## Desktop App

The desktop app is the primary platform — a standalone ScalaFX application with full editing, playback, PDF/HTML export capabilities.

```bash
# Compile
sbt sangeetDesktop/compile

# Run
sbt sangeetDesktop/run

# Build fat JAR
sbt sangeetDesktop/assembly
# Output: sangeet-desktop/target/scala-3.4.2/sangeet-notes-editor.jar
```

## Web App

The web app has two components: a Scala REST backend and an Elm SPA frontend. The backend exposes the core music model and editor logic as a stateless API. The frontend manages all state client-side. The web editor supports the same editing features as the desktop app: swar input with fast-typing grouping (2-4 notes within 500ms), stroke mode (Da/Ra/Chikari/Jod), cursor-aware deletion, ornament entry with Enter to finish, and undo/redo.

### Backend (Scala Server)

```bash
# Run the server (default port 28080)
sbt sangeetServer/run

# Or with a custom port
PORT=9090 sbt sangeetServer/run
```

- REST API: `http://localhost:28080/api/v1/`
- Swagger UI: `http://localhost:28080/docs`
- Health check: `http://localhost:28080/health`

### Frontend (Elm)

```bash
# Install dependencies (first time only)
cd sangeet-web && npm install

# Development with live reload
npx elm-live src/Main.elm --open --dir=public -- --output=public/elm.js

# Production build
npx elm make src/Main.elm --optimize --output=public/elm.js
```

### Running Both Together

1. Start the backend: `sbt sangeetServer/run`
2. In another terminal: `cd sangeet-web && npx elm-live src/Main.elm --open --dir=public -- --output=public/elm.js`

Or use the Makefile:

```bash
make server    # Terminal 1: start backend on port 28080
make web-dev   # Terminal 2: start frontend with live reload
```

## Debug Console

The desktop app includes a TCP debug console for inspecting and interacting with the running editor from a terminal. It starts automatically on `127.0.0.1:28081`.

```bash
# Connect
nc 127.0.0.1 28081

# Or with readline support
rlwrap nc 127.0.0.1 28081

# Scripted one-shot
echo "get-state" | nc 127.0.0.1 28081
```

| Command | Description |
|---------|-------------|
| `ping` | Health check |
| `help` | List all commands |
| `type <key>` | Simulate swar input (`type m` for Ma, `type S` for komal Re) |
| `press <key>` | Simulate special key (`space`, `backspace`, `minus`, `left`, `right`) |
| `dual <key>` | Insert dual swar (`dual s` for SaSa, `dual r` for ReRe) |
| `group <keys>` | Insert swar group on one beat (`group sr`, `group srg`, `group srgm`) |
| `get-state` | Cursor position, section, event count, edit mode |
| `get-events` | All events in current section |
| `dump-composition` | Full composition as JSON |
| `dump-history` | Undo/redo stack sizes |
| `check-focus` | Which UI node has keyboard focus |
| `focus` | Force focus back to editor |
| `thread-dump` | JVM thread dump (works even during UI freeze) |
| `set-debug on\|off` | Toggle verbose debug logging |

## Tests

```bash
# Scala tests
sbt sangeetCore/test       # Core library (523 tests, including 38 editor stress tests)
sbt sangeetServer/test     # Server API (112 tests)
sbt sangeetDesktop/test    # Desktop integration tests (95 TCP tests via DebugConsole)
sbt test                   # All Scala tests

# Elm tests
cd sangeet-web && npx elm-test   # Elm program tests (476 tests)

# E2E browser tests (requires server running on :28080)
cd e2e && ./node_modules/.bin/playwright test   # Playwright E2E (110 tests)

# Makefile shortcuts
make core-test             # Core library tests
make server-test           # Server API tests
make elm-test              # Elm program tests
make e2e-test              # Playwright E2E tests
make test-web              # Elm + server tests together
make test-all              # All sbt tests
```

### Test Coverage Summary

| Layer | Tests | What It Covers |
|-------|-------|----------------|
| Core library (ScalaTest) | 523 | Domain model, editor logic, layout, codecs, audio |
| Server API (ScalaTest) | 112 | All REST endpoints, error handling, chained operations |
| Desktop TCP (ScalaTest) | 95 | Full editor via TCP debug console, headless |
| Elm program (elm-test) | 476 | TEA logic, key handling, state transitions, API dispatch |
| Browser E2E (Playwright) | 110 | Full-stack user workflows, headless Chromium |
| **Total** | **1316** | |

### Desktop TCP Integration Tests (`DebugConsoleTcpSpec`)

The desktop module includes 95 integration tests that exercise the editor over a real TCP socket connection to the debug console. These tests run headless (no display needed) and cover:

- All swar keys, komal/tivra variants, octave changes, dual swar
- Fast-typing swar grouping (2/3/4 notes per beat), group-aware backspace/delete
- Rest, sustain, backspace, beat subdivisions
- All ornament types (meend, kan, murki, gamak, andolan, krintan, gitkari, ghaseet, sparsh, zamzama)
- Mizrab strokes (da, ra, chikari, jod)
- Section switching, composition reset, different taals
- Cursor-aware backspace/delete (position-based, not just last-event)
- Undo history tracking, cursor position verification
- JSON serialization round-trip, thread dump, focus management
- Log file verification (`/tmp/sangeet-notes-editor.*.log`)

### Web App Tests

The web app has three test layers:

**Elm Program Tests (476)** — Pure function and TEA update tests covering key handling, ornament state machine, undo history, cursor/editor/section/playback/dialog/file updates, swar grouping logic, API response handling, and integration flows.

**Server API Tests (112)** — HTTP route tests for all endpoint groups: reference data, composition CRUD, editor operations, cursor movement, section management, ornaments, strokes, layout, export, playback, and rendering.

**Playwright E2E Tests (110)** — Headless Chromium browser tests covering page load, keyboard input, cursor navigation, swar editing, section management, ornament workflows, stroke editing, undo/redo, file operations, dialogs, playback controls, script switching, view toggles, and multi-step workflows.

## Keyboard Reference

| Key | Action |
|-----|--------|
| `s r g m p d n` | Enter swar (Sa Re Ga Ma Pa Dha Ni) |
| `Shift + key` | Komal variant (Re, Ga, Dha, Ni) or Tivra (Ma) |
| `ss rr gg` etc. | Dual swar (double-tap for SaSa, ReRe, etc.) |
| `sr srg srgm` (fast) | Swar group — type 2–4 notes within 500ms to place on one beat |
| `.` (period) | Next note in mandra saptak (lower octave) |
| `'` (quote) | Next note in taar saptak (upper octave) |
| `` ` `` (backtick) | Return to madhya saptak |
| `Space` | Rest (silence) |
| `-` (minus) | Sustain (hold previous note) |
| `Backspace` | Delete note/group at cursor (group-aware) |
| `Arrow keys` | Move cursor |
| `Tab` | Next section |
| `Ctrl+Z / Ctrl+Y` | Undo / Redo |
| `Ctrl+S` | Save |
| `Ctrl+E` | Export PDF |

## Project Structure

```
sangeet-core/       Pure JVM library — domain model, editor logic, layout, codecs, audio, API layer
sangeet-desktop/    ScalaFX desktop application (primary UI)
sangeet-server/     Tapir HTTP server exposing core as REST API with Swagger
sangeet-web/        Elm 0.19 single-page application
```

## Tech Stack

- **Scala 3** + **ScalaFX** (desktop) / **Tapir + http4s** (server)
- **Elm 0.19** (web frontend)
- **circe** for JSON serialization
- **Apache PDFBox** for PDF export (with Noto Sans Devanagari font)
- **javax.sound.midi** for playback / **Web Audio API** (web)
- **ScalaTest** (730 Scala tests) + **elm-test** (476 Elm tests) + **Playwright** (110 E2E tests)
- **sbt-assembly** + **jpackage** for native packaging
- **GitHub Actions** for CI/CD and cross-platform release builds

## License

This project is not yet licensed. All rights reserved.
