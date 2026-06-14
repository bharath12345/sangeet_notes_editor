# Sangeet Notes Editor

## Project Summary

A multi-platform notation editor for Hindustani classical music, designed primarily for sitar compositions in the Bhatkhande notation style. Local-first: compositions stored as `.swar` files (JSON) on disk.

**Platforms:**

- **Desktop:** Scala 3 + ScalaFX — full-featured editor (primary)
- **Web:** Elm 0.19 frontend + Scala 3 / Tapir REST backend — in development
- **Android:** Planned

The full design spec is at `docs/superpowers/specs/2026-03-28-sangeet-notes-editor-design.md`. Read it before doing any implementation work — it is the source of truth for all design decisions.

## The User

Bharadwaj is learning sitar under a guruji. He has a physical notebook full of compositions (raags — their gat, antara, taan, toda, palta, etc.) that he wants to digitize. He is technically capable and has opinions about technology choices. He chose Scala 3 specifically — do not suggest switching languages. He values getting the core model right before building UI. He wants to start local/desktop before considering web/mobile.

His guruji teaches him Hindustani classical music primarily, but occasionally Carnatic compositions too. Sahitya (lyrics) can be in multiple Indian languages including Hindi, Kannada, Telugu, Sanskrit, Braj Bhasha.

## Technology Stack — Non-Negotiable

- **Language:** Scala 3 (use Scala 3 idioms: enum, case class, extension methods, given/using, match types where appropriate)
- **Desktop UI:** ScalaFX (wrapper over JavaFX)
- **Web Frontend:** Elm 0.19 (The Elm Architecture)
- **Web Backend:** Tapir (type-safe endpoints) + http4s EmberServer + cats-effect IO
- **JSON:** circe (Scala), elm/json (Elm)
- **Build:** sbt (Scala), elm make (Elm), npm (Elm dev tooling)
- **Testing:** ScalaTest (Scala), elm-test (Elm), Playwright (E2E browser)
- **Target JVM:** 17+

## Domain Knowledge — Hindustani Classical Music

The full reference (swar variant rules, all 11 taals with vibhag patterns, ornament semantics, sitar-specific notation, rendering rows) lives in the **`hindustani-music-theory`** skill at `.claude/skills/hindustani-music-theory/SKILL.md`. Load it via the Skill tool whenever you touch raag/taal/ornament/swar code, the renderer, or anything in `sangeet-core/model/`.

Two rules that come up often enough to mention here so they're never forgotten:

- **Sa and Pa are fixed (achal).** They have no komal/tivra variants — any code that lets the user toggle komal on Sa or Pa is a bug.
- **Taals are data, not code.** Adding a hardcoded match on taal name is a smell; if behavior depends on a property of the taal, expose that property on the `Taal` model instead.

## Architecture Principles

1. **Model is pure** — `sangeet.model` package has zero UI/IO dependencies. Must be reusable for future ScalaJS web version.
2. **Layout is separate from rendering** — layout engine computes positions as data (RenderedGrid), renderers (Canvas, HTML) consume it.
3. **Taals are data, not code** — JSON resource files, user can add custom taals.
4. **Ornaments are extensible** — CustomOrnament with Map[String, String] parameters.
5. **Format versioning** — `.swar` files include `"version": "1.0"` field.

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

Multi-module sbt build with four sub-projects:

```
sangeet-core/   (com.varpas.sangeet.core.*)
  model/        — Pure domain types (Composition, Event, Swar, Taal, Raag, Ornament, Stroke, Section)
  editor/       — Pure editor logic (CursorModel, CompositionEditor, KeyHandler, UndoHistory, OrnamentMode)
  layout/       — Layout engine: BeatGrouper → LineBreaker → GridLayout
  render/       — Pure rendering data: ScriptMap, GlyphMetrics, NotationColors (no ScalaFX)
  format/       — .swar JSON serialization (circe), HTML export
  config/       — AppConfig, ConfigStore (session persistence)
  raag/         — 26 built-in raag definitions (Raags.scala)
  taal/         — 11 built-in taal definitions
  api/          — Public API layer (CompositionApi, EditorApi, CursorApi, etc.)

sangeet-desktop/  (com.varpas.sangeet.desktop.*)
  render/       — ScalaFX Canvas rendering: SwarGlyphRenderer, OrnamentRendererFX, GridRendererFX, CanvasRendererFX
  editor/       — UI: EditorPane, TabManager, FileBrowserPanel, CompositionHeader, StatusBar, KeyboardLegend, AppLogger, SampleComposition, DebugConsole
  dialog/       — NewCompositionDialog, CompositionPropertiesDialog
  MainApp.scala — Entry point with tabbed editor, file browser panel, toolbar

sangeet-server/  (com.varpas.sangeet.server.*)
  endpoints/    — Tapir endpoint definitions (Reference, Composition, Editor, Cursor, etc.)
  routes/       — Route implementations with http4s
  Main.scala    — EmberServer entry point on port 28080
  CorsMiddleware.scala, ApiEnvelope.scala, ErrorMapping.scala

sangeet-web/  (Elm 0.19 SPA)
  src/Model/    — Elm types mirroring sangeet-core domain model
  src/Api/      — HTTP clients for each server endpoint
  src/State/    — TEA state management (Model, Msg, Update)
  src/View/     — Rendering: SwarGlyph, GridRenderer, Canvas, Toolbar, Dialogs
  src/Input/    — KeyHandler, OrnamentMode
  src/Ports.elm — File download/upload ports
  public/       — index.html, styles.css, ports.js (JavaScript interop)
  tests/        — 593 Elm program tests (elm-test)

e2e/  (Playwright browser tests)
  helpers/      — Page Object Model (SangeetPage), global setup
  tests/        — 126 E2E specs across 14 files (headless Chromium)
```

## Current Implementation State

### What's Built

- Full composition model with events (Swar, Rest, Sustain, Chikari, LockedBeat), sections, ornaments, strokes, sahitya, tihai
- Tabbed editor: multiple compositions open simultaneously, each in its own tab with independent undo history
- File browser panel: directory tree with bookmarks, double-click to open `.swar` files in tabs
- Session persistence: AppConfig stores open tabs, bookmarks, panel state, window size — restores on restart
- Canvas editor with keyboard input, cursor navigation, section switching, undo/redo, read-only mode with red notice
- Cut/copy/paste with beat-range selection (Ctrl+X/C/V) — serialized as JSON on system clipboard
- Per-section starting beat for Gat/Bandish compositions — locked beats (Event.LockedBeat) before sam on cycle 0, deletion-guarded, shifted on startingBeat change
- Grid layout engine (BeatGrouper → LineBreaker → GridLayout) with density-aware line breaking
- Dynamic canvas grid width: cells scale to fill available width for any taal, responsive to window resize
- HTML export with print-friendly CSS and all notation rows
- Color-coded notation: shared NotationColors palette used across canvas and HTML renderers
- 26 raags with full metadata (arohan, avrohan, vadi, samvadi, pakad, thaat, prahar)
- 11 taals with vibhag structure and markers
- Sample Yaman Vilambit Gat loaded on startup (read-only) showcasing all features
- Web app: Elm 0.19 SPA + Tapir REST backend (stateless API, client owns all state), at feature parity with desktop for editing (swar input, grouping, stroke mode, cursor-aware deletion, ornament finish, clipboard, starting beat)
- Swagger UI auto-generated from Tapir endpoint definitions
- TCP debug console on 127.0.0.1:28081 (desktop) + WebSocket debug bridge on web (gated by `?debug=ws://localhost:PORT` URL param) — both speak the shared `DebugCommand` ADT from sangeet-core. See `docs/developer/debug-bridge.md`. Used by the MCP server (`mcp-servers/sangeet-debug-console/`, `--transport tcp|ws`) and the cross-platform parity harness (`tests/integration/*.json`, run by `SharedIntegrationSpec` on desktop and `parity.spec.ts` on web).
- GitHub Actions CI/CD with cross-platform packaging (macOS .dmg, Windows .msi, Linux .deb)
- Fast-typing swar grouping: type 2–4 notes within 500ms to place them on one beat with equal subdivisions; group-aware backspace/delete removes entire groups
- Compact `.swar` format: omits default values for smaller file sizes
- 604 tests in sangeet-core (incl. DebugCommandSpec round-trip + 38 editor stress tests), 156 tests in sangeet-server, ~146 integration tests in sangeet-desktop (DebugConsoleTcpSpec + SharedIntegrationSpec, runs against live JavaFX)
- 593 Elm program tests (key handling, ornament mode, undo history, TEA update logic, grouping, API responses, integration flows, Debug.Interpreter decoder)
- Playwright E2E browser tests (headless Chromium: 126 specs covering keyboard input, cursor nav, dialogs, swar editing, sections, ornaments, strokes, undo/redo, file ops, scripts, view toggles, multi-step workflows) + `parity.spec.ts` runner iterating `tests/integration/*.json`
- 21 pytest cases in `mcp-servers/sangeet-debug-console/` for the WS transport text→JSON mapping + round-trip
- GitHub Actions CI runs all three web test layers (Elm + server + E2E) on push/PR

### Notation Row Rendering (5 rows per grid line)

Each taal cycle line renders these rows top-to-bottom:

1. **Taal markers** — Sam (X), Taali (2,3...), Khali (0) — dark red
2. **Ornaments** — meend, kan, gamak, andolan, etc. — deep purple
3. **Swar** — note glyphs with octave dots above/below and komal/tivra marks — dark indigo (dots in orange)
4. **Da/Ra strokes** — mizrab stroke indicators — teal
5. **Sahitya** — lyrics aligned per beat — dark green

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
- Dynamic cell width filling available canvas width (not fixed 60px)
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

## Code Quality Tooling

### Formatting

- **Scala:** scalafmt (`.scalafmt.conf`) — `sbt scalafmtAll` to fix, `sbt scalafmtCheckAll` to check
- **Elm:** elm-format — zero-config canonical formatter
- **TS/JS/CSS:** prettier (`.prettierrc`) — shared across e2e/ and sangeet-web/public/

### Linting

- **Scala:** scalafix (`.scalafix.conf`) — currently OrganizeImports only. `sbt scalafixAll` to fix, `sbt "scalafixAll --check"` to check
- **Elm:** elm-review (`sangeet-web/review/`) — NoUnused, Simplify, NoDebug, NoExposingEverything (suppressed for tests). 49 suppressed baseline issues
- **TS:** eslint (`e2e/eslint.config.js`) — @typescript-eslint/recommended for E2E tests

### Test Coverage

- scoverage: 80% statement coverage minimum, enforced on CI
- Desktop module excluded (ScalaFX UI code)
- Current: ~91% aggregate

### Pre-commit Hooks

- lefthook (`.lefthook.yml`) — runs scalafmt, elm-format, prettier checks in parallel on commit

### CI Pipeline (`.github/workflows/ci.yml`)

4 jobs: `lint` → `scala-tests` (with coverage) → `elm-tests` → `e2e-tests` (gated on all 3)

### Quality Commands

- `make format` — auto-format all code
- `make lint` — check all formatting and linting
- `make coverage` — run tests with coverage report
- `make check-all` — lint + test-all + coverage
