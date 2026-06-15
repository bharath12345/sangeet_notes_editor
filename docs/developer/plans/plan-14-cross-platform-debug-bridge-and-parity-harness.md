# Plan 14 — Cross-Platform Debug Bridge + Test-Parity Harness + Shared UI Strings Catalog

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Two workstreams, two PRs, executable in parallel:**
>
> - **Workstream A (PR-A): Debug bridge + parity harness** — Phases 1–11
> - **Workstream B (PR-B): Shared UI strings catalog** — Phases 12–17
>
> See the **Workstream organization** section below for parallel-execution guardrails and merge-conflict surface.

**Goal:**

1. Give the web app the same agent-driveable + integration-testable surface the desktop has today via its TCP debug console, and prove that both stacks produce byte-identical `.swar`/`.html` exports for any given composition.
2. Establish a single source of truth for every user-visible string used by desktop and web, with CI-enforced parity: every catalog entry marked `platform: both` must be referenced on **both** stacks; entries marked `desktop` or `web` are explicit, justified exceptions reviewed via a generated report.

**Architecture:**

_Workstream A._ A shared `DebugCommand` ADT in `sangeet-core` (with circe codecs) is the single command vocabulary. Desktop's existing TCP console parses text → `DebugCommand` → dispatch. Web gains a WebSocket bridge: an external process (Playwright test, MCP server) hosts a WS server on a loopback port; the Elm app connects via `?debug=ws://localhost:PORT` URL param, receives JSON `DebugCommand` messages through `ports.js`, and a new `Debug.Interpreter` module maps each variant to existing `Msg`s. Two thin test runners (ScalaTest for desktop via TCP, Playwright for web via WS) load shared JSON test definitions from `tests/integration/` and assert against the same intermediate-state checkpoints plus byte-identical golden `.swar` + `.html` fixtures.

_Workstream B._ A canonical `sangeet-core/src/main/resources/ui-strings.json` is the source of truth. An sbt codegen task emits `UiStrings.scala` (Scala 3 object with typed `val` constants and `def` functions for parameterized entries); a Node/TS codegen script emits the parallel `UiStrings.elm` module. Both generated files are checked into git. A `scripts/check-string-parity.ts` job, wired into CI, walks both source trees for `UiStrings.<key>` references and fails if a `both` entry is missing on either side, if a `desktop` entry leaks into web (or vice versa), or if any source-code reference points at a key not in the catalog.

**Tech Stack:** Scala 3 (sangeet-core, sangeet-desktop, sangeet-server), Elm 0.19 (sangeet-web), TypeScript + Playwright (e2e), Python (MCP server), `ws` npm package (WS server in tests), circe for JSON, Node 20+ for codegen scripts.

---

## Context

Plan 13 closed 2026-06-12 with the desktop TCP debug console (`127.0.0.1:28081`) wrapped as an MCP server (`mcp-servers/sangeet-debug-console/`). That gives agents a way to drive the running desktop app — write a feature → connect via MCP → exercise via simulated keystrokes → inspect state → iterate. The web side has no equivalent: agents can't drive the running Elm app, and the existing 125 `DebugConsoleTcpSpec` integration tests are desktop-only.

This plan closes both gaps in one coordinated change:

1. **Web debug bridge** — gives the Elm app a JSON-over-WebSocket back door for programmatic control, gated behind a `?debug=ws://localhost:...` URL param so it can never accidentally ship to production.
2. **Cross-platform parity harness** — replaces 125 desktop-only ScalaTest cases with shared JSON test definitions that two thin runners (ScalaTest + Playwright) execute against both stacks. Each test asserts identical intermediate state AND byte-identical final `.swar`/`.html` exports against committed golden fixtures.

By construction, the two layers reinforce each other: the bridge is the mechanism the parity harness uses to drive the web app, and the harness is the regression net that proves the bridge faithfully maps commands to the same internal operations the desktop keystroke path uses.

## Decisions captured during brainstorm

### Workstream A — Debug bridge + parity harness

| #   | Decision                                                                                            | Spec section     |
| --- | --------------------------------------------------------------------------------------------------- | ---------------- |
| A1  | One coordinated project (bridge + harness)                                                          | Workstream A     |
| A2  | Single shared test definitions; one source of truth                                                 | Phase 5          |
| A3  | Cover intermediate state + final `.swar`/`.html` parity                                             | Phase 5, Phase 8 |
| A4  | Shared command schema in `sangeet-core` (`enum DebugCommand`)                                       | Phase 1          |
| A5  | Two thin runners (ScalaTest + Playwright), shared JSON inputs                                       | Phase 6, Phase 7 |
| A6  | External process hosts WS; Elm connects via `?debug=ws://localhost:PORT`                            | Phase 4, Phase 7 |
| A7  | Port all 125 existing tests into shared format; retire `DebugConsoleTcpSpec`                        | Phase 9          |
| A8  | Extend existing MCP server with `--transport tcp\|ws` flag                                          | Phase 10         |
| A9  | Server returns `.swar` as `String` (no Elm re-encoding); golden fixtures + byte-equality assertions | Phase 3, Phase 8 |
| A10 | Ships as PR-A (independent of Workstream B)                                                         | This plan        |

### Workstream B — Shared UI strings catalog

| #   | Decision                                                                                         | Spec section |
| --- | ------------------------------------------------------------------------------------------------ | ------------ |
| B1  | Two PRs, one plan doc; A and B are independent workstreams                                       | This plan    |
| B2  | Build-time codegen from canonical JSON (compile-time typed constants on both sides)              | Phase 12     |
| B3  | Broad scope + escape hatch: `platform: "both" \| "desktop" \| "web"` (default `both`)            | Phase 12     |
| B4  | Goal is to minimize `desktop`-only and `web`-only entries to near-zero via Phase 17 review       | Phase 17     |
| B5  | Typed function codegen for parameterized strings (def emits with typed args on both sides)       | Phase 12     |
| B6  | Generated `UiStrings.scala` and `UiStrings.elm` are checked into git (IDEs see real constants)   | Phase 12     |
| B7  | Phase 17 review report committed as `docs/strings-parity-report.md`; user reviews entry-by-entry | Phase 17     |
| B8  | Semantic keys (`area.component.element`), not English-string keys — i18n-future-proof structure  | Phase 12     |
| B9  | Parity check (`scripts/check-string-parity.ts`) wired into CI as a required check                | Phase 13     |
| B10 | Ships as PR-B (independent of Workstream A)                                                      | This plan    |

## Non-goals

- **CSS theme toggle on web** (separate plan — already tracked as Task #214 from PR #71).
- **i18n / translations.** Catalog is English-only for now. Keys are semantic (`toolbar.file.new` not `New`) so a future i18n PR is a structural change to the codegen, not a rename storm across 200 call sites.
- **Running the debug bridge in production** — the URL-param gate is intentional; this is a dev/test feature only.
- **Wire-format compatibility with the existing TCP text protocol** — TCP continues to speak text (parsed via `DebugCommand.fromText`); WS speaks JSON of the same `DebugCommand` enum. Both go through the same dispatch.
- **Cross-stack diff of `Composition` model literals.** Tests assert `.swar`/`.html` bytes match, not the in-memory model — the byte assertion implicitly tests model parity (round-trip through serialize).
- **Raag / taal / ornament names as catalog entries.** Those are data (live in `Raags.scala`, `Taals.scala`, ornament enum). UI labels that wrap them ("Raag:", "Taal:", "Ornament type:") are catalog entries; the data values are not.

## Workstream organization

Plan-14 splits into two independent workstreams that ship as two separate PRs:

| Workstream | Phases | PR   | Output                                            |
| ---------- | ------ | ---- | ------------------------------------------------- |
| A          | 1–11   | PR-A | Debug bridge + parity test harness                |
| B          | 12–17  | PR-B | Shared UI strings catalog + parity check + report |

### Parallel-execution rules

Both workstreams can be executed in parallel by separate subagents in separate worktrees:

1. **Each workstream gets its own git worktree** off `main` (e.g., `workstream-a-debug-bridge` and `workstream-b-strings-catalog`). Subagent-driven mode creates these natively.
2. **Workstream A agent directive:** "Do NOT introduce any new user-visible strings. If you must (e.g., a debug-mode indicator), hardcode it and tag with `// TODO(strings-catalog): migrate after PR-B merges`. Do NOT migrate any existing literals — that's PR-B's job."
3. **Whichever PR is ready first merges first.** The other rebases on `main` and resolves the trivial conflicts listed below.

### Shared / merge-conflict surface

These three files are touched by both workstreams. The conflicts are mechanical — different lines, different sections — and resolve cleanly on rebase:

| File                       | A's change                                          | B's change                                         |
| -------------------------- | --------------------------------------------------- | -------------------------------------------------- |
| `.github/workflows/ci.yml` | Adds `integration-parity` Playwright shard (or job) | Adds `string-parity` job                           |
| `Makefile`                 | Adds `integration-test` target                      | Adds `gen-strings`, `check-strings` targets        |
| `.lefthook.yml`            | (no change required)                                | Adds pre-commit hook running `gen-strings` (gated) |

No other files are touched by both workstreams.

## File structure

### Workstream A — new files

| Path                                                                                                | Responsibility                                                |
| --------------------------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| `sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/DebugCommand.scala`                      | The shared command ADT + circe codecs + `fromText` parser     |
| `sangeet-core/src/test/scala/com/varpas/sangeet/core/debug/DebugCommandSpec.scala`                  | Round-trip codec tests + `fromText` parser tests              |
| `sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/TestDefinition.scala`                    | The shared test definition ADT + circe codecs                 |
| `sangeet-core/src/test/scala/com/varpas/sangeet/core/debug/TestDefinitionSpec.scala`                | Round-trip codec tests for `TestDefinition`                   |
| `sangeet-desktop/src/test/scala/com/varpas/sangeet/desktop/integration/SharedIntegrationSpec.scala` | ScalaTest runner that iterates `tests/integration/*.json`     |
| `sangeet-web/src/Debug/Interpreter.elm`                                                             | Elm interpreter — maps `DebugCommand` JSON to existing `Msg`s |
| `sangeet-web/tests/Debug/InterpreterTest.elm`                                                       | Unit tests for each interpreter variant                       |
| `e2e/integration/parity.spec.ts`                                                                    | Playwright runner that iterates `tests/integration/*.json`    |
| `e2e/integration/helpers/ws-server.ts`                                                              | Reusable WS server utility for the runner                     |
| `e2e/integration/helpers/test-definition.ts`                                                        | TypeScript types mirroring `TestDefinition`                   |
| `e2e/integration/helpers/golden-fixtures.ts`                                                        | Golden fixture comparison helpers                             |
| `tests/integration/README.md`                                                                       | Documents the JSON test format                                |
| `tests/integration/golden/.gitkeep`                                                                 | Initial empty dir                                             |
| `tests/integration/*.json`                                                                          | 135 shared test definitions (10 new canonical + 125 ported)   |
| `tests/integration/golden/*.swar`, `tests/integration/golden/*.html`                                | 10 golden fixtures                                            |
| `mcp-servers/sangeet-debug-console/transport.py`                                                    | Transport abstraction (base class)                            |
| `mcp-servers/sangeet-debug-console/transport_tcp.py`                                                | TCP transport (refactored from existing `server.py`)          |
| `mcp-servers/sangeet-debug-console/transport_ws.py`                                                 | WS transport                                                  |
| `docs/developer/debug-bridge.md`                                                                    | Architecture + usage docs                                     |

### Workstream A — modified files

| Path                                                                                           | Change                                                                                                    |
| ---------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/DebugCommandHandler.scala`   | Split into `parseToDebugCommand` + `applyDebugCommand`; route text through `DebugCommand.fromText`        |
| `sangeet-desktop/src/test/scala/com/varpas/sangeet/desktop/editor/DebugConsoleTcpSpec.scala`   | **Delete** (replaced by `SharedIntegrationSpec`)                                                          |
| `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/CompositionEndpoints.scala` | `serializeComposition` output type: `jsonBody[Json]` → `stringBody`                                       |
| `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/CompositionRoutes.scala`       | Use `CompositionApi.serializeCompositionString`                                                           |
| `sangeet-web/src/Api/Composition.elm`                                                          | Decoder accepts plain string, not JSON value                                                              |
| `sangeet-web/src/State/Update.elm`                                                             | `GotSerializedComposition` branch: pass string to download port verbatim                                  |
| `sangeet-web/src/Ports.elm`                                                                    | Add `requestDebugConnection`, `debugCommandReceived`, `debugResponse` ports                               |
| `sangeet-web/src/Main.elm`                                                                     | Subscribe to `debugCommandReceived`; call `requestDebugConnection` at boot if `?debug=` present           |
| `sangeet-web/src/State/Msg.elm`                                                                | Add `DebugCommandReceived Decode.Value`                                                                   |
| `sangeet-web/src/State/Update.elm`                                                             | Add `DebugCommandReceived` branch that calls `Debug.Interpreter.interpret`                                |
| `sangeet-web/public/ports.js`                                                                  | Add WS bridge code (gated by URL param presence)                                                          |
| `mcp-servers/sangeet-debug-console/server.py`                                                  | Refactor to use transport abstraction; add `--transport` CLI flag                                         |
| `mcp-servers/sangeet-debug-console/README.md`                                                  | Document `--transport ws` mode                                                                            |
| `.github/workflows/ci.yml`                                                                     | Playwright matrix already covers `e2e/integration/`; no change needed _unless_ shard count needs increase |
| `CLAUDE.md`                                                                                    | Add reference to `docs/developer/debug-bridge.md`                                                         |

### Workstream B — new files

| Path                                                                                     | Responsibility                                                                                |
| ---------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| `sangeet-core/src/main/resources/ui-strings.json`                                        | **The canonical source of truth.** All user-visible strings, with platform tag + description. |
| `sangeet-core/src/main/scala/com/varpas/sangeet/core/strings/UiStrings.scala`            | **Generated.** Scala 3 object with typed `val` / `def` constants. Checked into git.           |
| `sangeet-web/src/UiStrings.elm`                                                          | **Generated.** Elm module with parallel constants / functions. Checked into git.              |
| `project/UiStringsCodegen.scala`                                                         | sbt task implementation — reads JSON, emits `UiStrings.scala`                                 |
| `scripts/gen-elm-strings.ts`                                                             | Node script — reads JSON, emits `UiStrings.elm`                                               |
| `scripts/check-string-parity.ts`                                                         | Parity-check script — walks both source trees, validates against catalog                      |
| `scripts/find-untracked-strings.ts`                                                      | Heuristic sweep for English-looking string literals not yet in catalog                        |
| `scripts/generate-strings-report.ts`                                                     | Emits `docs/strings-parity-report.md` for the Phase 17 review milestone                       |
| `scripts/lib/catalog.ts`                                                                 | Shared catalog reader / validator used by all 3 TS scripts                                    |
| `scripts/lib/source-scanner.ts`                                                          | Shared Scala/Elm source-tree scanner that finds `UiStrings.<key>` references                  |
| `scripts/README.md`                                                                      | Docs for the codegen + parity scripts                                                         |
| `sangeet-core/src/test/scala/com/varpas/sangeet/core/strings/UiStringsCodegenSpec.scala` | Tests for the sbt codegen task (round-trips, parameterized entries, escape characters)        |
| `scripts/__tests__/check-string-parity.test.ts`                                          | Jest/Vitest tests for the parity-check script                                                 |
| `scripts/__tests__/gen-elm-strings.test.ts`                                              | Tests for the Elm codegen                                                                     |
| `docs/strings-parity-report.md`                                                          | **Generated at Phase 17.** Lists all `desktop` / `web` / uncategorized entries for review     |
| `docs/developer/ui-strings-catalog.md`                                                   | Architecture + "how to add a string" + codegen + parity-check docs                            |

### Workstream B — modified files

| Path                                                                                                 | Change                                                                                                   |
| ---------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| `build.sbt`                                                                                          | Add `genUiStrings` sbt task to `sangeetCore` project; wire to `Compile / sourceGenerators`               |
| `package.json` (repo root, new)                                                                      | Add `scripts/` as a workspace; declare `tsx`, `vitest` dev-deps                                          |
| `Makefile`                                                                                           | Add `gen-strings`, `check-strings`, `find-untracked-strings`, `strings-report` targets; wire `format`    |
| `.lefthook.yml`                                                                                      | Add pre-commit hook: if `ui-strings.json` is staged, run `make gen-strings` and re-stage generated files |
| `.github/workflows/ci.yml`                                                                           | Add `string-parity` job (runs `make check-strings`); add gen-strings sync check                          |
| `sangeet-web/src/View/Toolbar.elm`                                                                   | Replace inlined literals (~50 strings) with `UiStrings.toolbar.*` references                             |
| `sangeet-web/src/View/Header.elm`                                                                    | Replace inlined literals (cursor position labels, mode indicator)                                        |
| `sangeet-web/src/View/StatusBar.elm`                                                                 | Replace inlined literals (status messages)                                                               |
| `sangeet-web/src/View/FileBrowser.elm`                                                               | Replace inlined literals (bookmark labels, breadcrumb)                                                   |
| `sangeet-web/src/View/KeyboardLegend.elm`                                                            | Replace inlined literals                                                                                 |
| `sangeet-web/src/View/Dialogs/About.elm`                                                             | Replace dialog title, body paragraphs, link labels                                                       |
| `sangeet-web/src/View/Dialogs/Support.elm`                                                           | Replace dialog title, body, UPI / PayPal labels                                                          |
| `sangeet-web/src/View/Dialogs/NewComposition.elm`                                                    | Replace form labels, button labels, dropdown placeholders                                                |
| `sangeet-web/src/View/Dialogs/Properties.elm`                                                        | Replace form labels                                                                                      |
| `sangeet-web/src/View/Dialogs/BugReport.elm`                                                         | Replace form labels + body copy                                                                          |
| `sangeet-web/src/View/Dialogs/KeyboardCheatSheet.elm`                                                | Replace section headers + descriptions                                                                   |
| `sangeet-web/src/View/Dialogs/CommandPalette.elm`                                                    | Replace placeholder, empty state messages                                                                |
| `sangeet-web/src/State/Update.elm`                                                                   | Replace status-log messages, error messages                                                              |
| `sangeet-web/src/State/AppAction.elm`                                                                | Replace command palette action labels + descriptions                                                     |
| `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/ToolbarBuilder.scala`              | Mirror toolbar literal replacements                                                                      |
| `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/CompositionHeader.scala`           | Mirror header literal replacements                                                                       |
| `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/StatusBar.scala`                   | Mirror status bar literal replacements                                                                   |
| `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/FileBrowserPanel.scala`            | Mirror file browser literal replacements                                                                 |
| `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/KeyboardLegend.scala`              | Mirror keyboard legend literal replacements                                                              |
| `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/AboutDialog.scala`                 | Mirror About dialog literal replacements                                                                 |
| `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/SupportDialog.scala`               | Mirror Support dialog literal replacements                                                               |
| `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/NewCompositionDialog.scala`        | Mirror New dialog literal replacements                                                                   |
| `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/CompositionPropertiesDialog.scala` | Mirror Properties dialog literal replacements                                                            |
| `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/BugReportDialog.scala`             | Mirror Bug Report dialog literal replacements                                                            |
| `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/MainApp.scala`                            | Replace window title, menu item labels                                                                   |
| `.gitignore`                                                                                         | Add entry for `scripts/node_modules/` and `scripts/coverage/`                                            |
| `CLAUDE.md`                                                                                          | Add reference to `docs/developer/ui-strings-catalog.md`                                                  |

### Boundaries

- `sangeet-core/debug/` is a new package. It depends only on `sangeet-core/model/` and circe. No UI / IO / transport dependencies.
- `sangeet-core/strings/` is a new package containing **only generated code**. Zero dependencies. Pure constants. No imports from `sangeet-core/model/` or anywhere else — this lets it be used from any layer without cycles.
- `sangeet-desktop/integration/` is a new test-only package. Production desktop code unchanged except `DebugCommandHandler.scala`.
- `e2e/integration/` lives parallel to `e2e/tests/` (existing UX specs). Different concern, different runner config.
- `tests/integration/` is a top-level dir (not under any single module) because both `sangeet-desktop` and `e2e` read from it. Both runners resolve it via relative paths.
- `scripts/` is a new top-level dir for Workstream B's Node tooling. Self-contained: own `package.json`, own `tsconfig.json`, own `node_modules`. Does not pollute `sangeet-web/`'s tooling.
- `ui-strings.json` lives in `sangeet-core/src/main/resources/` so the Scala codegen task can read it via classpath relative paths in the sbt build. Elm codegen reads it via repo-relative path.

---

## Phase 1 — Shared `DebugCommand` schema in sangeet-core

Foundation for everything else. Both transports parse to this enum; both interpreters dispatch on it.

### Task 1.1: Create `DebugCommand` enum with all variants

**Files:**

- Create: `sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/DebugCommand.scala`

The variant set mirrors the 31 commands the current `DebugCommandHandler` recognises. Source the list from `mcp-servers/sangeet-debug-console/server.py` (each `@mcp.tool()` decorator maps to one command).

- [ ] **Step 1: Create the enum file**

```scala
package com.varpas.sangeet.core.debug

import io.circe.Codec

/** Single source of truth for every command both the TCP debug console and the
  * web WebSocket debug bridge accept. Adding a new command means:
  *   1. add a case here
  *   2. add a dispatch arm in sangeet-desktop's DebugCommandHandler.applyDebugCommand
  *   3. add a dispatch arm in sangeet-web's Debug.Interpreter.interpret
  * Steps 2 and 3 won't compile until both are done, so drift is caught at build time.
  */
enum DebugCommand derives Codec.AsObject:
  // Connection / introspection
  case Ping
  case Help
  case ThreadDump
  case SetDebug(enabled: Boolean)
  case ThrowCrash

  // Tab management (desktop-only at apply time; web ignores)
  case ListTabs
  case SelectTab(id: String)
  case NewTab
  case CloseTab(id: String)
  case TabInfo

  // Composition reset / creation
  case Reset(compositionType: String, raag: Option[String], taal: String)
  case SetTaal(taal: String)

  // Editor focus
  case CheckFocus
  case FocusEditor

  // Cursor / mode setters
  case SetOctave(octave: String)
  case SetSubdivision(n: Int)

  // Swar input
  case TypeChar(ch: String)
  case Press(key: String)
  case TypeTimed(ch: String, delayMs: Int)
  case DualSwar(first: String, second: String)
  case SwarGroup(notes: List[String])

  // Strokes
  case Stroke(stroke: String)

  // Ornaments
  case SimpleOrnament(name: String)
  case OrnamentStart(kind: String)
  case OrnamentNote(note: String)
  case FinishOrnament

  // Sections
  case SwitchSection(idx: Int)

  // State read-back
  case GetState
  case GetEvents
  case DumpComposition
  case DumpHistory
```

- [ ] **Step 2: Verify it compiles**

```bash
sbt sangeetCore/compile
```

Expected: `[success] Total time: ...` with no errors.

- [ ] **Step 3: Commit**

```bash
git add sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/DebugCommand.scala
git commit -m "feat(core): introduce DebugCommand ADT for cross-transport debug protocol"
```

### Task 1.2: Round-trip codec tests for `DebugCommand`

**Files:**

- Create: `sangeet-core/src/test/scala/com/varpas/sangeet/core/debug/DebugCommandSpec.scala`

- [ ] **Step 1: Write the failing tests**

```scala
package com.varpas.sangeet.core.debug

import io.circe.parser.decode
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DebugCommandSpec extends AnyFlatSpec with Matchers:

  private def roundTrip(cmd: DebugCommand): Unit =
    val encoded = cmd.asJson.noSpaces
    val decoded = decode[DebugCommand](encoded)
    decoded shouldBe Right(cmd)

  "DebugCommand" should "round-trip Ping" in { roundTrip(DebugCommand.Ping) }
  it should "round-trip Reset with raag" in {
    roundTrip(DebugCommand.Reset("gat", Some("yaman"), "teentaal"))
  }
  it should "round-trip Reset without raag (Palta)" in {
    roundTrip(DebugCommand.Reset("palta", None, "teentaal"))
  }
  it should "round-trip TypeChar" in { roundTrip(DebugCommand.TypeChar("s")) }
  it should "round-trip TypeTimed" in { roundTrip(DebugCommand.TypeTimed("s", 250)) }
  it should "round-trip SwarGroup with 4 notes" in {
    roundTrip(DebugCommand.SwarGroup(List("s", "r", "g", "m")))
  }
  it should "round-trip SimpleOrnament" in {
    roundTrip(DebugCommand.SimpleOrnament("gamak"))
  }
  it should "round-trip GetState" in { roundTrip(DebugCommand.GetState) }

  it should "encode the discriminator as a top-level field" in {
    val json = DebugCommand.TypeChar("s").asJson.noSpaces
    json should include(""""TypeChar"""")
  }

  it should "reject unknown discriminator values" in {
    val bad = """{"NotARealCommand":{}}"""
    decode[DebugCommand](bad).isLeft shouldBe true
  }
```

- [ ] **Step 2: Run the tests**

```bash
sbt "sangeetCore/testOnly *DebugCommandSpec"
```

Expected: 10 passing tests.

- [ ] **Step 3: Commit**

```bash
git add sangeet-core/src/test/scala/com/varpas/sangeet/core/debug/DebugCommandSpec.scala
git commit -m "test(core): round-trip codec tests for DebugCommand"
```

### Task 1.3: Add `DebugCommand.fromText` parser

The existing TCP protocol speaks newline-delimited text commands like `type s r g m p` or `reset gat yaman teentaal`. To keep the TCP transport backward-compatible with the existing MCP server and existing tests during the porting window, parse text → `DebugCommand` rather than reinventing the protocol.

**Files:**

- Modify: `sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/DebugCommand.scala`
- Modify: `sangeet-core/src/test/scala/com/varpas/sangeet/core/debug/DebugCommandSpec.scala`

Source the text-format definitions from the existing `DebugCommandHandler.scala` (it already does this parsing inline; we're factoring it out).

- [ ] **Step 1: Read the existing text-parsing logic**

```bash
sed -n '50,240p' sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/DebugCommandHandler.scala
```

Note each `case "<cmd>"` arm and its argument shape — that's the mapping you're porting.

- [ ] **Step 2: Add the parser to DebugCommand**

Add to `DebugCommand.scala` after the `enum` block:

```scala
object DebugCommand:

  /** Parse the legacy TCP text format (newline-delimited "cmd args..." lines) into
    * a DebugCommand. Returns Left with a human-readable error if the line can't be
    * parsed. This is the source of truth for the TCP wire format — both
    * sangeet-desktop's DebugCommandHandler and the MCP server's TCP transport
    * should delegate here so the protocol stays in lock-step with the enum.
    */
  def fromText(line: String): Either[String, DebugCommand] =
    val tokens = line.trim.split("\\s+", -1).toList
    tokens match
      case Nil | "" :: Nil =>
        Left("empty command")
      case "ping" :: Nil => Right(Ping)
      case "help" :: Nil => Right(Help)
      case "thread-dump" :: Nil => Right(ThreadDump)
      case "set-debug" :: arg :: Nil =>
        arg.toBooleanOption.toRight(s"set-debug: bool expected, got '$arg'").map(SetDebug.apply)
      case "throw-crash" :: Nil => Right(ThrowCrash)
      case "list-tabs" :: Nil => Right(ListTabs)
      case "select-tab" :: id :: Nil => Right(SelectTab(id))
      case "new-tab" :: Nil => Right(NewTab)
      case "close-tab" :: id :: Nil => Right(CloseTab(id))
      case "tab-info" :: Nil => Right(TabInfo)

      // reset <type> [raag] <taal>
      // "reset gat yaman teentaal" — 3 args
      // "reset palta teentaal"     — 2 args (no raag for Palta)
      // "reset bandish yaman teentaal"
      case "reset" :: compType :: rest if rest.size == 1 || rest.size == 2 =>
        if rest.size == 2 then Right(Reset(compType, Some(rest.head), rest(1)))
        else Right(Reset(compType, None, rest.head))

      case "set-taal" :: taal :: Nil => Right(SetTaal(taal))
      case "check-focus" :: Nil => Right(CheckFocus)
      case "focus-editor" :: Nil => Right(FocusEditor)
      case "set-octave" :: oct :: Nil => Right(SetOctave(oct))
      case "set-subdivision" :: n :: Nil =>
        n.toIntOption.toRight(s"set-subdivision: int expected, got '$n'").map(SetSubdivision.apply)

      case "type" :: chars if chars.nonEmpty =>
        // "type s r g m p" => 5 separate TypeChar would be 5 commands; we keep it as
        // one TypeChar with concatenated chars to match the existing TCP behavior of
        // applying them in sequence as part of one apply.
        // Actually the existing handler dispatches one char at a time, so emit the
        // first-char form here and require callers to send one-char-per-line for clarity.
        // For backward compat with multi-char "type" lines, accept the whole string:
        Right(TypeChar(chars.mkString(" ").replace(" ", "")))

      case "press" :: key :: Nil => Right(Press(key))
      case "type-timed" :: ch :: delay :: Nil =>
        delay.toIntOption.toRight(s"type-timed: int expected, got '$delay'").map(d => TypeTimed(ch, d))

      case "dual-swar" :: first :: second :: Nil => Right(DualSwar(first, second))
      case "swar-group" :: notes if notes.nonEmpty => Right(SwarGroup(notes))

      case "stroke" :: kind :: Nil => Right(Stroke(kind))
      case "simple-ornament" :: name :: Nil => Right(SimpleOrnament(name))
      case "ornament-start" :: kind :: Nil => Right(OrnamentStart(kind))
      case "ornament-note" :: note :: Nil => Right(OrnamentNote(note))
      case "finish-ornament" :: Nil => Right(FinishOrnament)

      case "switch-section" :: idx :: Nil =>
        idx.toIntOption.toRight(s"switch-section: int expected, got '$idx'").map(SwitchSection.apply)

      case "get-state" :: Nil => Right(GetState)
      case "get-events" :: Nil => Right(GetEvents)
      case "dump-composition" :: Nil => Right(DumpComposition)
      case "dump-history" :: Nil => Right(DumpHistory)

      case cmd :: _ => Left(s"unknown command: '$cmd'")
```

- [ ] **Step 3: Add parser tests**

Append to `DebugCommandSpec.scala`:

```scala
  "DebugCommand.fromText" should "parse ping" in {
    DebugCommand.fromText("ping") shouldBe Right(DebugCommand.Ping)
  }
  it should "parse reset with raag" in {
    DebugCommand.fromText("reset gat yaman teentaal") shouldBe
      Right(DebugCommand.Reset("gat", Some("yaman"), "teentaal"))
  }
  it should "parse reset without raag (palta)" in {
    DebugCommand.fromText("reset palta teentaal") shouldBe
      Right(DebugCommand.Reset("palta", None, "teentaal"))
  }
  it should "parse type with multi-char arg" in {
    DebugCommand.fromText("type srgmp") shouldBe Right(DebugCommand.TypeChar("srgmp"))
  }
  it should "parse type-timed" in {
    DebugCommand.fromText("type-timed s 250") shouldBe Right(DebugCommand.TypeTimed("s", 250))
  }
  it should "parse swar-group" in {
    DebugCommand.fromText("swar-group s r g m") shouldBe
      Right(DebugCommand.SwarGroup(List("s", "r", "g", "m")))
  }
  it should "reject unknown commands" in {
    DebugCommand.fromText("not-a-real-command").isLeft shouldBe true
  }
  it should "reject empty input" in {
    DebugCommand.fromText("").isLeft shouldBe true
    DebugCommand.fromText("   ").isLeft shouldBe true
  }
```

- [ ] **Step 4: Run the tests**

```bash
sbt "sangeetCore/testOnly *DebugCommandSpec"
```

Expected: 18 passing tests (10 codec + 8 parser).

- [ ] **Step 5: Commit**

```bash
git add sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/DebugCommand.scala \
        sangeet-core/src/test/scala/com/varpas/sangeet/core/debug/DebugCommandSpec.scala
git commit -m "feat(core): DebugCommand.fromText parser for legacy TCP wire format"
```

---

## Phase 2 — Desktop refactor: route TCP through `DebugCommand`

Behavior-preserving refactor. After this phase, the existing 125 `DebugConsoleTcpSpec` tests must still pass with zero changes.

### Task 2.1: Refactor `DebugCommandHandler` to use `DebugCommand`

**Files:**

- Modify: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/DebugCommandHandler.scala`

The existing `handleCommand(line: String): String` method parses + dispatches inline. Split into two methods so the same `applyDebugCommand` can also be called by the (future) WS path on desktop tests.

- [ ] **Step 1: Read the current shape**

```bash
sed -n '1,40p' sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/DebugCommandHandler.scala
```

- [ ] **Step 2: Refactor**

At the top of the class, add the new dispatcher:

```scala
import com.varpas.sangeet.core.debug.DebugCommand

class DebugCommandHandler(pane: EditorPane, statusBar: StatusBar):

  /** Entry point preserved for backward compat with DebugConsole.scala which
    * sends raw text lines from sockets. Parse then dispatch.
    */
  def handleCommand(line: String): String =
    DebugCommand.fromText(line) match
      case Right(cmd) => applyDebugCommand(cmd)
      case Left(err)  => s"ERROR: $err"

  /** New entry point — apply a typed DebugCommand. Both the TCP path (via
    * handleCommand) and any future direct-call path (e.g., in-process tests)
    * route through here.
    */
  def applyDebugCommand(cmd: DebugCommand): String =
    import DebugCommand.*
    cmd match
      case Ping => "PONG"
      case Help => helpText
      case ThreadDump => threadDumpText
      case SetDebug(enabled) => setDebug(enabled)
      case ThrowCrash => throwCrash()
      case ListTabs => listTabs()
      case SelectTab(id) => selectTab(id)
      case NewTab => newTab()
      case CloseTab(id) => closeTab(id)
      case TabInfo => tabInfo()
      case Reset(t, raag, taal) => resetComposition(t, raag, taal)
      case SetTaal(taal) => setTaal(taal)
      case CheckFocus => checkFocus()
      case FocusEditor => focusEditor()
      case SetOctave(oct) => setOctave(oct)
      case SetSubdivision(n) => setSubdivision(n)
      case TypeChar(chars) => typeChars(chars)
      case Press(key) => pressKey(key)
      case TypeTimed(ch, delay) => typeTimed(ch, delay)
      case DualSwar(a, b) => dualSwar(a, b)
      case SwarGroup(notes) => swarGroup(notes)
      case Stroke(s) => stroke(s)
      case SimpleOrnament(name) => simpleOrnament(name)
      case OrnamentStart(kind) => ornamentStart(kind)
      case OrnamentNote(note) => ornamentNote(note)
      case FinishOrnament => finishOrnament()
      case SwitchSection(idx) => switchSection(idx)
      case GetState => getState()
      case GetEvents => getEvents()
      case DumpComposition => dumpComposition()
      case DumpHistory => dumpHistory()
```

Below, move every existing inline case body into a same-named `private def`. Each helper takes the typed args from the variant — no string-parsing left at this layer. The mechanical translation pattern: every `case "type" :: chars =>` body becomes `private def typeChars(chars: String): String = ...` with the same body, no `tokens` array indexing.

- [ ] **Step 3: Verify the refactor with existing tests**

```bash
sbt "sangeetDesktop/testOnly *DebugConsoleTcpSpec"
```

Expected: all 125 tests pass with zero behavioral change. If any fail, the refactor changed behavior — fix in place before moving on.

- [ ] **Step 4: Commit**

```bash
git add sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/DebugCommandHandler.scala
git commit -m "refactor(desktop): route TCP commands through DebugCommand ADT"
```

---

## Phase 3 — Server byte-equality fix

Today desktop saves `.swar` via `SwarFormat.toJson(comp).spaces2` (circe). Web saves via Tapir's `jsonBody[Json]` → wire → Elm receives `Decode.Value` → `Encode.encode 2 value` (Elm's encoder, different key ordering and whitespace rules from circe). Two stacks produce non-identical bytes for the same composition. Golden-fixture parity is impossible until this is fixed.

Fix: server endpoint returns a `String` (already serialized by sangeet-core via circe). Elm receives the string verbatim and downloads it without re-encoding.

### Task 3.1: Change the server endpoint to return `String`

**Files:**

- Modify: `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/CompositionEndpoints.scala`
- Modify: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/CompositionRoutes.scala`

- [ ] **Step 1: Locate the endpoint**

```bash
grep -n "serializeComposition" sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/CompositionEndpoints.scala
grep -n "serializeComposition" sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/CompositionRoutes.scala
```

- [ ] **Step 2: Change endpoint output type**

In `CompositionEndpoints.scala`, find the `serializeComposition` endpoint definition. Change:

```scala
.out(jsonBody[Json])
```

to:

```scala
.out(stringBody)
.out(header("Content-Type", "application/json; charset=utf-8"))
```

- [ ] **Step 3: Change the route implementation**

In `CompositionRoutes.scala`, find the route. Change:

```scala
CompositionApi.serializeComposition(comp)
```

to:

```scala
CompositionApi.serializeCompositionString(comp)
```

(`serializeCompositionString` already exists in sangeet-core and returns `SwarFormat.toJson(comp).spaces2`.)

- [ ] **Step 4: Compile**

```bash
sbt sangeetServer/compile
```

Expected: clean. Any failures point to the endpoint shape mismatch — fix imports / types.

- [ ] **Step 5: Commit**

```bash
git add sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/CompositionEndpoints.scala \
        sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/CompositionRoutes.scala
git commit -m "fix(server): serialize endpoint returns pre-formatted String not Json value"
```

### Task 3.2: Update Elm client to consume the string verbatim

**Files:**

- Modify: `sangeet-web/src/Api/Composition.elm`
- Modify: `sangeet-web/src/State/Msg.elm`
- Modify: `sangeet-web/src/State/Update.elm`

- [ ] **Step 1: Locate the current client decoder**

```bash
grep -n "serializeComposition\|GotSerializedComposition\|Decode.Value" sangeet-web/src/Api/Composition.elm sangeet-web/src/State/Update.elm sangeet-web/src/State/Msg.elm 2>/dev/null
```

- [ ] **Step 2: Change the Msg type**

`State/Msg.elm`: find `GotSerializedComposition (Result Http.Error (ApiResult Decode.Value))`. Change to:

```elm
| GotSerializedComposition (Result Http.Error (ApiResult String))
```

- [ ] **Step 3: Change the API client**

`Api/Composition.elm`: find the function that decodes the serialize response. Change its return decoder from `Decode.value` to `Decode.string`. Also remove `import Json.Decode as Decode exposing (Decoder, Value)` → just `Decode exposing (Decoder)` if `Value` is now unused.

- [ ] **Step 4: Change the Update branch**

`State/Update.elm`, around the existing `GotSerializedComposition` branch:

Before:

```elm
GotSerializedComposition result ->
    handleApiResult result
        (\jsonValue ->
            let
                comp = Model.composition model
                filename = comp.metadata.title ++ ".swar"
                content = Encode.encode 2 jsonValue
            in
            ( { model | pendingApiCall = False } |> addLog "Saving composition..."
            , Ports.downloadFile { filename = filename, mimeType = "application/json", content = content }
            )
        )
        model
```

After:

```elm
GotSerializedComposition result ->
    handleApiResult result
        (\swarString ->
            let
                comp = Model.composition model
                filename = comp.metadata.title ++ ".swar"
            in
            ( { model | pendingApiCall = False } |> addLog "Saving composition..."
            , Ports.downloadFile { filename = filename, mimeType = "application/json", content = swarString }
            )
        )
        model
```

Remove the now-unused `import Json.Encode as Encode` if nothing else uses it in this file.

- [ ] **Step 5: Compile**

```bash
cd sangeet-web && ./node_modules/.bin/elm make src/Main.elm --output=/tmp/check.js
```

Expected: `Success!`

- [ ] **Step 6: Run Elm tests**

```bash
cd sangeet-web && ./node_modules/.bin/elm-test
```

Expected: 558 passing.

- [ ] **Step 7: Commit**

```bash
git add sangeet-web/src/Api/Composition.elm \
        sangeet-web/src/State/Msg.elm \
        sangeet-web/src/State/Update.elm
git commit -m "fix(web): consume server's pre-serialized swar string verbatim"
```

### Task 3.3: Server-side byte-stability regression test

**Files:**

- Modify: existing `sangeet-server/src/test/scala/com/varpas/sangeet/server/routes/CompositionRoutesSpec.scala` (or create new `SerializeByteStabilitySpec.scala` if simpler)

- [ ] **Step 1: Add the test**

In whichever spec covers `serializeComposition`:

```scala
"serializeComposition" should "return byte-identical output for the same composition" in {
  val sample = SampleCompositions.yamanVilambitGat // or any deterministic Composition
  val first = CompositionApi.serializeCompositionString(sample)
  val second = CompositionApi.serializeCompositionString(sample)
  first shouldBe second
  // Stability assertion: a known prefix the format guarantees.
  first should startWith("""{""")
  first should include(""""version" : "1.0"""")
}
```

(Use `SampleComposition` from sangeet-desktop's editor package only if accessible from sangeet-server tests — otherwise build a tiny in-test `Composition` literal.)

- [ ] **Step 2: Run**

```bash
sbt "sangeetServer/test"
```

Expected: existing + 1 new passing.

- [ ] **Step 3: Commit**

```bash
git add sangeet-server/src/test/scala/com/varpas/sangeet/server/routes/CompositionRoutesSpec.scala
git commit -m "test(server): byte-stability test for serializeCompositionString"
```

---

## Phase 4 — Web debug bridge

Adds the WS connection, the two ports, the JS handler, and the Elm interpreter. After this phase, you can open the app at `http://localhost:3000/?debug=ws://localhost:9999`, connect from a `wscat` client on `localhost:9999`, send a `{"Reset":{"compositionType":"gat","raag":"yaman","taal":"teentaal"}}` JSON message and see the Elm app reset its composition.

### Task 4.1: Add ports

**Files:**

- Modify: `sangeet-web/src/Ports.elm`

- [ ] **Step 1: Add the port declarations**

In the existing port list (alphabetical) and below `submitBugReport`:

```elm
-- DEBUG BRIDGE
-- Gated by URL param presence (?debug=ws://localhost:PORT). JS in ports.js
-- opens the WebSocket and forwards messages in both directions. Production
-- bundles WITHOUT the param simply never call requestDebugConnection.


port requestDebugConnection : String -> Cmd msg


port debugCommandReceived : (Decode.Value -> msg) -> Sub msg


port debugResponse : { id : String, result : Decode.Value, error : Maybe String } -> Cmd msg
```

Add `import Json.Decode as Decode` if not already imported in the top of file (it likely is).

Add `requestDebugConnection`, `debugCommandReceived`, `debugResponse` to the `exposing` list.

- [ ] **Step 2: Compile**

```bash
cd sangeet-web && ./node_modules/.bin/elm make src/Main.elm --output=/tmp/check.js
```

Expected: `Success!`

- [ ] **Step 3: Commit**

```bash
git add sangeet-web/src/Ports.elm
git commit -m "feat(web): add debug bridge port declarations"
```

### Task 4.2: ports.js WebSocket handler

**Files:**

- Modify: `sangeet-web/public/ports.js`

- [ ] **Step 1: Add the WS handler**

Insert before the `CONFIG PERSISTENCE` section (which makes a natural anchor):

```javascript
// ===============================
// DEBUG BRIDGE
// ===============================
// Activated only when the page URL has ?debug=ws://localhost:PORT.
// Loopback-only by design: rejects anything that doesn't start with
// ws://localhost: or ws://127.0.0.1: so a hostile page can't trick the
// running app into shipping state to an attacker-controlled endpoint.
// Connection lifecycle: Elm calls requestDebugConnection with the URL once
// at boot; JS opens the socket, forwards inbound JSON messages to Elm via
// the debugCommandReceived subscription port, and forwards Elm's outbound
// responses (state snapshots, ack messages) over the socket.

var debugSocket = null;

function isLoopbackWs(url) {
  return /^ws:\/\/(localhost|127\.0\.0\.1)(:\d+)?(\/|$)/.test(url);
}

if (app.ports.requestDebugConnection) {
  app.ports.requestDebugConnection.subscribe(function (url) {
    if (!isLoopbackWs(url)) {
      console.warn('[debug-bridge] refusing non-loopback URL:', url);
      return;
    }
    if (debugSocket) {
      console.warn('[debug-bridge] already connected; ignoring second request');
      return;
    }
    try {
      debugSocket = new WebSocket(url);
    } catch (e) {
      console.error('[debug-bridge] WebSocket construction failed:', e);
      return;
    }
    debugSocket.onopen = function () {
      console.info('[debug-bridge] connected to', url);
    };
    debugSocket.onmessage = function (evt) {
      try {
        var parsed = JSON.parse(evt.data);
        if (app.ports.debugCommandReceived) {
          app.ports.debugCommandReceived.send(parsed);
        }
      } catch (e) {
        console.error('[debug-bridge] failed to parse incoming message:', e);
      }
    };
    debugSocket.onerror = function (e) {
      console.error('[debug-bridge] socket error:', e);
    };
    debugSocket.onclose = function () {
      console.info('[debug-bridge] socket closed');
      debugSocket = null;
    };
  });
}

if (app.ports.debugResponse) {
  app.ports.debugResponse.subscribe(function (payload) {
    if (debugSocket && debugSocket.readyState === WebSocket.OPEN) {
      debugSocket.send(JSON.stringify(payload));
    }
  });
}
```

- [ ] **Step 2: Commit**

```bash
git add sangeet-web/public/ports.js
git commit -m "feat(web): ports.js WebSocket debug bridge (loopback-only)"
```

### Task 4.3: Wire `requestDebugConnection` at boot

**Files:**

- Modify: `sangeet-web/src/Main.elm`

The Elm app needs to read `?debug=` from the page URL at init and request a connection if present. Browser-side reading of URL params is via `Browser.Navigation` and `Url.parser` — but a simpler approach for a single bootstrap-time lookup is to pass it through as a JS flag.

Two options. We'll use **option B** (pass URL via flag) because it's three lines and avoids pulling `Browser.Navigation` into a non-routed app.

- [ ] **Step 1: Update index.html to pass the URL param**

`sangeet-web/public/index.html`, in the script that calls `Elm.Main.init`:

Find:

```javascript
var app = Elm.Main.init({
  node: document.getElementById('elm-app'),
  flags: { apiBaseUrl: getApiBaseUrl() },
});
```

Replace with:

```javascript
var debugUrl = new URLSearchParams(window.location.search).get('debug') || null;
var app = Elm.Main.init({
  node: document.getElementById('elm-app'),
  flags: { apiBaseUrl: getApiBaseUrl(), debugUrl: debugUrl },
});
```

- [ ] **Step 2: Update Main.elm flags decoder**

Locate the existing `Flags` type alias in `Main.elm`:

```bash
grep -n "type alias Flags\|Flags " sangeet-web/src/Main.elm
```

Add `debugUrl : Maybe String` field. Update the `init` function to call `Ports.requestDebugConnection url` as part of the initial `Cmd.batch` when `flags.debugUrl` is `Just url`.

Example:

```elm
type alias Flags =
    { apiBaseUrl : String
    , debugUrl : Maybe String
    }


init : Flags -> ( Model, Cmd Msg )
init flags =
    let
        baseModel = Model.init flags.apiBaseUrl
        debugCmd =
            case flags.debugUrl of
                Just url -> Ports.requestDebugConnection url
                Nothing -> Cmd.none
    in
    ( baseModel
    , Cmd.batch
        [ -- ...existing init commands here...
          debugCmd
        ]
    )
```

(Look at the existing `init` to slot `debugCmd` into the existing `Cmd.batch`. Don't replace any of the existing commands.)

- [ ] **Step 3: Subscribe to `debugCommandReceived`**

In `subscriptions`:

```elm
, Ports.debugCommandReceived DebugCommandReceived
```

- [ ] **Step 4: Compile**

```bash
cd sangeet-web && ./node_modules/.bin/elm make src/Main.elm --output=/tmp/check.js
```

Expected: errors about undefined `DebugCommandReceived` Msg — that's Task 4.4 / 4.5.

- [ ] **Step 5: Don't commit yet** — comes after 4.5.

### Task 4.4: Create `Debug.Interpreter` skeleton

**Files:**

- Create: `sangeet-web/src/Debug/Interpreter.elm`

- [ ] **Step 1: Write the module skeleton**

```elm
module Debug.Interpreter exposing (interpret)

{-| Maps an incoming DebugCommand JSON value (produced by the WS bridge) to an
existing State.Msg. Each DebugCommand variant maps to one or more existing Msgs
so the editor logic stays identical to the keyboard path — the bridge is a
back-door for SENDING input, not a parallel editor implementation.

The decoder shape must match circe's encoded shape of
sangeet-core's enum DebugCommand. If circe is configured for default sealed-trait
encoding, that's: { "VariantName": { field1: value1, ... } } at the top level.

If a command requires a synchronous response back over WS (GetState, DumpComposition),
the interpreter returns ( Maybe Msg, Maybe DebugResponse ). The response carries the
correlated id from the inbound message.

See docs/developer/debug-bridge.md for the wire format.
-}

import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode
import State.Model as Model exposing (Model)
import State.Msg exposing (Msg(..))


type alias Response =
    { id : String, result : Encode.Value, error : Maybe String }


{-| Apply a DebugCommand JSON value to the model. Returns:
- the Msg to dispatch (or NoOp if the command is purely a state read)
- an optional Response to send back over WS (for state-read commands)
-}
interpret : Decode.Value -> Model -> ( Msg, Maybe Response )
interpret raw model =
    case Decode.decodeValue commandWithIdDecoder raw of
        Err _ ->
            ( NoOp, Just { id = "", result = Encode.null, error = Just "decode failed" } )

        Ok ( id, cmd ) ->
            applyCmd id cmd model


type DebugCmd
    = Ping
    | Reset { compositionType : String, raag : Maybe String, taal : String }
    | TypeChar String
    | SetOctave String
    | SetSubdivision Int
    | GetState
    | DumpComposition
      -- TODO Phase 4.5: add remaining variants
    | UnknownCmd String


commandWithIdDecoder : Decoder ( String, DebugCmd )
commandWithIdDecoder =
    Decode.map2 Tuple.pair
        (Decode.field "id" Decode.string)
        (Decode.field "cmd" cmdDecoder)


cmdDecoder : Decoder DebugCmd
cmdDecoder =
    Decode.oneOf
        [ Decode.field "Ping" (Decode.succeed Ping)
        , Decode.field "Reset" resetDecoder
        , Decode.field "TypeChar" typeCharDecoder
        , Decode.field "SetOctave" setOctaveDecoder
        , Decode.field "SetSubdivision" setSubdivisionDecoder
        , Decode.field "GetState" (Decode.succeed GetState)
        , Decode.field "DumpComposition" (Decode.succeed DumpComposition)
        , Decode.map UnknownCmd (Decode.succeed "unknown")
        ]


resetDecoder : Decoder DebugCmd
resetDecoder =
    Decode.map3 (\t r ta -> Reset { compositionType = t, raag = r, taal = ta })
        (Decode.field "compositionType" Decode.string)
        (Decode.maybe (Decode.field "raag" Decode.string))
        (Decode.field "taal" Decode.string)


typeCharDecoder : Decoder DebugCmd
typeCharDecoder =
    Decode.map TypeChar (Decode.field "ch" Decode.string)


setOctaveDecoder : Decoder DebugCmd
setOctaveDecoder =
    Decode.map SetOctave (Decode.field "octave" Decode.string)


setSubdivisionDecoder : Decoder DebugCmd
setSubdivisionDecoder =
    Decode.map SetSubdivision (Decode.field "n" Decode.int)


applyCmd : String -> DebugCmd -> Model -> ( Msg, Maybe Response )
applyCmd id cmd model =
    case cmd of
        Ping ->
            ( NoOp
            , Just { id = id, result = Encode.string "PONG", error = Nothing }
            )

        Reset r ->
            -- Reset is a composite of: dismiss any open dialog, then create the
            -- composition via the New Composition flow. For now, dispatch the
            -- equivalent NewDialog* + Submit Msgs in sequence. Phase 4.5 will
            -- wire this end-to-end after we read State/Update for the exact
            -- composition-creation Msg sequence.
            ( NoOp, Nothing ) -- TODO: implement in Phase 4.5

        TypeChar ch ->
            -- Synthesize a KeyPressed Msg as if the user typed the character.
            ( KeyPressed ch False False False, Nothing )

        SetOctave oct ->
            -- The existing keyboard binding for taar octave is "]"; mandra is "[";
            -- madhya is "\\". Map the string to the equivalent KeyPressed.
            let
                key =
                    case oct of
                        "mandra" -> "["
                        "taar" -> "]"
                        _ -> "\\"
            in
            ( KeyPressed key False False False, Nothing )

        SetSubdivision n ->
            ( KeyPressed (String.fromInt n) False False False, Nothing )

        GetState ->
            let
                snapshot =
                    encodeStateSnapshot model
            in
            ( NoOp, Just { id = id, result = snapshot, error = Nothing } )

        DumpComposition ->
            let
                comp =
                    encodeComposition model
            in
            ( NoOp, Just { id = id, result = comp, error = Nothing } )

        UnknownCmd _ ->
            ( NoOp, Just { id = id, result = Encode.null, error = Just "unknown command" } )


{-| Encode a small subset of Model for state-check assertions. Keep the shape
stable across versions: tests assert specific fields, so adding fields is fine
but renaming or removing them is a breaking change.
-}
encodeStateSnapshot : Model -> Encode.Value
encodeStateSnapshot _ =
    Encode.object
        [ ( "ok", Encode.bool True )
          -- TODO Phase 4.5: populate with eventCount, cursorBeat, cursorCycle,
          -- sectionName, etc. based on what test checkpoints need.
        ]


encodeComposition : Model -> Encode.Value
encodeComposition _ =
    Encode.null -- TODO Phase 4.5: encode the full composition or call the server's serialize endpoint
```

- [ ] **Step 2: Compile**

```bash
cd sangeet-web && ./node_modules/.bin/elm make src/Debug/Interpreter.elm --output=/tmp/check.js
```

Expected: `Success!`

- [ ] **Step 3: Commit**

```bash
git add sangeet-web/src/Debug/Interpreter.elm
git commit -m "feat(web): Debug.Interpreter skeleton with Ping/TypeChar/GetState wired"
```

### Task 4.5: Complete the interpreter — all DebugCommand variants

This is mechanical: for each `DebugCommand` variant, decide which existing `Msg` (or sequence of `Msg`s) maps to it, then add the decoder branch + the `applyCmd` arm.

**Files:**

- Modify: `sangeet-web/src/Debug/Interpreter.elm`
- Modify: `sangeet-web/src/State/Msg.elm` (add `DebugCommandReceived`)
- Modify: `sangeet-web/src/State/Update.elm` (handle `DebugCommandReceived`)

- [ ] **Step 1: Add the Msg**

`State/Msg.elm`, add to the Msg ADT:

```elm
| -- Debug bridge (WS only)
  DebugCommandReceived Decode.Value
```

Make sure `Json.Decode as Decode` is imported.

- [ ] **Step 2: Handle the Msg in Update**

`State/Update.elm`, add a branch (alphabetically or grouped with other debug-related; if no group exists, add near the end before `NoOp`):

```elm
DebugCommandReceived raw ->
    let
        ( nextMsg, maybeResponse ) =
            Debug.Interpreter.interpret raw model

        ( newModel, msgCmd ) =
            update nextMsg model

        responseCmd =
            case maybeResponse of
                Just r -> Ports.debugResponse r
                Nothing -> Cmd.none
    in
    ( newModel, Cmd.batch [ msgCmd, responseCmd ] )
```

Add `import Debug.Interpreter` at the top.

- [ ] **Step 3: Expand `Debug.Interpreter` to handle ALL 31 DebugCommand variants**

For each variant in `sangeet-core/.../debug/DebugCommand.scala`, add (a) a `DebugCmd` constructor in the Elm sum, (b) a decoder branch in `cmdDecoder`, (c) an `applyCmd` arm. The mapping table (refer to existing key handlers in `Input/KeyHandler.elm` and existing Msgs for what each maps to):

| DebugCommand           | Maps to (existing Msg or sequence)                                                                                                                                                                                                                                                                                                                                   |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Ping`                 | NoOp, response = "PONG"                                                                                                                                                                                                                                                                                                                                              |
| `Help`                 | NoOp, response = static help string                                                                                                                                                                                                                                                                                                                                  |
| `ThreadDump`           | NoOp, response = stack trace text (use `Debug.todo` for now; browser doesn't expose threads — return a placeholder)                                                                                                                                                                                                                                                  |
| `SetDebug`             | NoOp (web has no equivalent of desktop's debug toggle; accept the command for parity, no-op)                                                                                                                                                                                                                                                                         |
| `ThrowCrash`           | NoOp, response = "ERROR: crash injection not supported on web"                                                                                                                                                                                                                                                                                                       |
| `ListTabs`             | NoOp, response = JSON list of `model.tabs`                                                                                                                                                                                                                                                                                                                           |
| `SelectTab`            | `SwitchTab id`                                                                                                                                                                                                                                                                                                                                                       |
| `NewTab`               | `NewTab`                                                                                                                                                                                                                                                                                                                                                             |
| `CloseTab`             | `CloseTab id`                                                                                                                                                                                                                                                                                                                                                        |
| `TabInfo`              | NoOp, response = JSON of active tab fields                                                                                                                                                                                                                                                                                                                           |
| `Reset(t, raag, taal)` | Sequence: `NewComposition`, then dialog-form Msgs to set fields, then `NewDialogSubmit`. Implementation: emit a single `Reset` Msg with the typed args, add a corresponding handler in Update that mirrors the dialog-submit logic in one step.                                                                                                                      |
| `SetTaal`              | `PropsDialogSetTaal taal` then `PropsDialogSubmit` (or add a direct `Reset` variant — see Phase 4.5.b)                                                                                                                                                                                                                                                               |
| `CheckFocus`           | NoOp, response = always "true" on web (no JavaFX focus equivalent)                                                                                                                                                                                                                                                                                                   |
| `FocusEditor`          | NoOp                                                                                                                                                                                                                                                                                                                                                                 |
| `SetOctave`            | `KeyPressed` with bracket key                                                                                                                                                                                                                                                                                                                                        |
| `SetSubdivision`       | `KeyPressed` with digit                                                                                                                                                                                                                                                                                                                                              |
| `TypeChar(s)`          | If single char: `KeyPressed s False False False`. If multi-char: emit one Msg per char. **Implementation note:** the interpreter returns a single Msg, so multi-char `TypeChar` needs a new internal Msg `TypeCharSequence String` whose Update branch loops. Add that Msg.                                                                                          |
| `Press(key)`           | `KeyPressed key False False False` — but with the named-key mapping (e.g. "BACKSPACE" → "Backspace").                                                                                                                                                                                                                                                                |
| `TypeTimed(ch, delay)` | Same as TypeChar but emits a `Cmd` that uses `Process.sleep delay \|> Task.perform (\_ -> KeyPressed ch ...)` to mimic the inter-keystroke delay.                                                                                                                                                                                                                    |
| `DualSwar(a, b)`       | Two-Msg sequence via `TypeCharSequence`.                                                                                                                                                                                                                                                                                                                             |
| `SwarGroup(notes)`     | New internal Msg `SwarGroupCmd (List String)` whose handler issues the existing swar-grouping API call.                                                                                                                                                                                                                                                              |
| `Stroke(kind)`         | Toggle to stroke mode then `KeyPressed "d"` / `"r"` / `"j"`.                                                                                                                                                                                                                                                                                                         |
| `SimpleOrnament(name)` | `KeyPressed name False False True` (Alt + first letter) — see `Input/KeyHandler.elm` for the alt-ornament map.                                                                                                                                                                                                                                                       |
| `OrnamentStart(kind)`  | Same as SimpleOrnament.                                                                                                                                                                                                                                                                                                                                              |
| `OrnamentNote(note)`   | `KeyPressed note False False False` while ornament mode is open.                                                                                                                                                                                                                                                                                                     |
| `FinishOrnament`       | `KeyPressed "Enter" False False False`                                                                                                                                                                                                                                                                                                                               |
| `SwitchSection(idx)`   | `SelectSection idx`                                                                                                                                                                                                                                                                                                                                                  |
| `GetState`             | NoOp, response = encoded state snapshot (eventCount, cursorBeat, cursorCycle, currentSectionName, taalName, raagName)                                                                                                                                                                                                                                                |
| `GetEvents`            | NoOp, response = encoded list of events at the cursor                                                                                                                                                                                                                                                                                                                |
| `DumpComposition`      | NoOp, response = encoded composition JSON. Call `ApiComposition.serializeComposition` and wait for `GotSerializedComposition` — too async for one Msg. **Simpler:** encode the composition directly in Elm using a known-shape encoder, or accept that DumpComposition triggers an async chain and the WS response goes out when `GotSerializedComposition` returns. |
| `DumpHistory`          | NoOp, response = `model.history` encoded (undo/redo stack depth + lengths).                                                                                                                                                                                                                                                                                          |

This task is the most code-heavy in the plan. Budget: ~2 hours.

- [ ] **Step 4: Add internal helper Msgs to State/Msg.elm**

```elm
| TypeCharSequence String  -- emit one KeyPressed per char, threaded through Update
| ResetComposition String (Maybe String) String  -- compositionType, raag, taal
| SwarGroupCmd (List String)
```

Add Update branches for each (see existing `AddSection`, `NewDialogSubmit` for patterns).

- [ ] **Step 5: Compile + run Elm tests**

```bash
cd sangeet-web && ./node_modules/.bin/elm-test
```

Expected: 558+ passing (any new internal Msgs may need their own test coverage; see Phase 6 for the integration-level coverage).

- [ ] **Step 6: Commit**

```bash
git add sangeet-web/src/Debug/Interpreter.elm \
        sangeet-web/src/State/Msg.elm \
        sangeet-web/src/State/Update.elm
git commit -m "feat(web): Debug.Interpreter complete — all 31 DebugCommand variants"
```

### Task 4.6: Manual end-to-end smoke test of the bridge

Before writing test infrastructure, prove the bridge actually works.

- [ ] **Step 1: Start sangeet-server**

```bash
sbt sangeetServer/run
```

(Leave running in a separate terminal.)

- [ ] **Step 2: Start the web dev server**

```bash
cd sangeet-web && npm run dev
# or however the dev server is launched — likely `./node_modules/.bin/elm-live src/Main.elm --open --start-page=public/index.html -- --output=public/elm.js`
```

- [ ] **Step 3: Start a netcat WS listener on port 9999**

Use the `ws` npm package via a one-liner:

```bash
node -e "const WebSocket = require('ws'); const wss = new WebSocket.Server({ port: 9999 }); wss.on('connection', ws => { console.log('connected'); ws.on('message', m => console.log('elm→ws:', m.toString())); setTimeout(() => ws.send(JSON.stringify({ id: 't1', cmd: { Ping: {} } })), 1000); });"
```

- [ ] **Step 4: Open the app with debug URL**

In browser: `http://localhost:3000/?debug=ws://localhost:9999`

(Or whatever port the elm-live dev server is on.)

- [ ] **Step 5: Verify in the browser DevTools console + the node terminal**

Browser console should show `[debug-bridge] connected to ws://localhost:9999`.

Node terminal should show `elm→ws: {"id":"t1","result":"PONG","error":null}` (or similar — the exact shape is whatever your Response encoder produces).

- [ ] **Step 6: If working, commit a status note (no code change)**

```bash
git commit --allow-empty -m "chore: bridge end-to-end smoke verified"
```

If NOT working, debug now — every subsequent phase assumes this works.

---

## Phase 5 — Shared test definition schema

The format both runners read.

### Task 5.1: Define `TestDefinition` in sangeet-core

**Files:**

- Create: `sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/TestDefinition.scala`

- [ ] **Step 1: Write the file**

```scala
package com.varpas.sangeet.core.debug

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

/** A single integration test loaded from tests/integration/*.json. Both the
  * ScalaTest runner (desktop) and the Playwright runner (web) read the same
  * files and dispatch each step through their respective transport.
  */
case class TestDefinition(
    name: String,
    description: Option[String],
    steps: List[TestStep]
) derives Codec.AsObject

/** Each step is either (a) a DebugCommand to send over the wire, or (b) a
  * runner-side directive (Checkpoint, AssertGoldenSwar, AssertGoldenHtml) that
  * the runner interprets locally without sending it to the app.
  */
enum TestStep derives Codec.AsObject:
  case Cmd(cmd: DebugCommand)
  case Checkpoint(expect: ExpectedState)
  case AssertGoldenSwar(fixture: String)
  case AssertGoldenHtml(fixture: String)

case class ExpectedState(
    eventCount: Option[Int] = None,
    cursorBeat: Option[Int] = None,
    cursorCycle: Option[Int] = None,
    sectionName: Option[String] = None,
    taalName: Option[String] = None,
    raagName: Option[String] = None,
    sectionCount: Option[Int] = None
) derives Codec.AsObject
```

- [ ] **Step 2: Compile**

```bash
sbt sangeetCore/compile
```

- [ ] **Step 3: Add round-trip tests**

`sangeet-core/src/test/scala/com/varpas/sangeet/core/debug/TestDefinitionSpec.scala`:

```scala
package com.varpas.sangeet.core.debug

import io.circe.parser.decode
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TestDefinitionSpec extends AnyFlatSpec with Matchers:

  "TestDefinition" should "round-trip a small test" in {
    val defn = TestDefinition(
      name = "sample",
      description = Some("Smoke test"),
      steps = List(
        TestStep.Cmd(DebugCommand.Reset("gat", Some("yaman"), "teentaal")),
        TestStep.Cmd(DebugCommand.TypeChar("s")),
        TestStep.Checkpoint(ExpectedState(eventCount = Some(1), cursorBeat = Some(2))),
        TestStep.AssertGoldenSwar("golden/sample.swar")
      )
    )
    val json = defn.asJson.noSpaces
    decode[TestDefinition](json) shouldBe Right(defn)
  }
```

```bash
sbt "sangeetCore/testOnly *TestDefinitionSpec"
```

- [ ] **Step 4: Commit**

```bash
git add sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/TestDefinition.scala \
        sangeet-core/src/test/scala/com/varpas/sangeet/core/debug/TestDefinitionSpec.scala
git commit -m "feat(core): TestDefinition schema for shared cross-stack integration tests"
```

### Task 5.2: Mirror schema in TypeScript

**Files:**

- Create: `e2e/integration/helpers/test-definition.ts`

- [ ] **Step 1: Write the file**

```typescript
// Mirrors sangeet-core/.../debug/TestDefinition.scala.
// circe encodes Scala 3 enums as { "VariantName": { ...fields... } } at the top level.
// Keep these types in sync with that encoding shape.

export type DebugCommand =
  | { Ping: {} }
  | { Help: {} }
  | { Reset: { compositionType: string; raag?: string; taal: string } }
  | { TypeChar: { ch: string } }
  | { Press: { key: string } }
  | { TypeTimed: { ch: string; delayMs: number } }
  | { DualSwar: { first: string; second: string } }
  | { SwarGroup: { notes: string[] } }
  | { SetOctave: { octave: string } }
  | { SetSubdivision: { n: number } }
  | { Stroke: { stroke: string } }
  | { SimpleOrnament: { name: string } }
  | { OrnamentStart: { kind: string } }
  | { OrnamentNote: { note: string } }
  | { FinishOrnament: {} }
  | { SwitchSection: { idx: number } }
  | { GetState: {} }
  | { GetEvents: {} }
  | { DumpComposition: {} }
  | { DumpHistory: {} };
// (add remaining variants as needed — TS doesn't need every variant to compile, only
//  the ones tests actually use.)

export interface ExpectedState {
  eventCount?: number;
  cursorBeat?: number;
  cursorCycle?: number;
  sectionName?: string;
  taalName?: string;
  raagName?: string;
  sectionCount?: number;
}

export type TestStep =
  | { Cmd: { cmd: DebugCommand } }
  | { Checkpoint: { expect: ExpectedState } }
  | { AssertGoldenSwar: { fixture: string } }
  | { AssertGoldenHtml: { fixture: string } };

export interface TestDefinition {
  name: string;
  description?: string;
  steps: TestStep[];
}
```

- [ ] **Step 2: Verify TS compiles**

```bash
cd e2e && npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add e2e/integration/helpers/test-definition.ts
git commit -m "feat(e2e): TypeScript mirror of TestDefinition schema"
```

### Task 5.3: Create the `tests/integration/` directory + README

**Files:**

- Create: `tests/integration/README.md`
- Create: `tests/integration/golden/.gitkeep`

- [ ] **Step 1: Make the dirs**

```bash
mkdir -p tests/integration/golden
touch tests/integration/golden/.gitkeep
```

- [ ] **Step 2: Write README**

````bash
cat > tests/integration/README.md <<'EOF'
# Cross-platform integration tests

This directory holds **shared** integration tests that run against both the
desktop app (via TCP) and the web app (via WebSocket). Each `*.json` file is one
test, loaded by:

- `sangeet-desktop/src/test/scala/com/varpas/sangeet/desktop/integration/SharedIntegrationSpec.scala`
- `e2e/integration/parity.spec.ts`

Both runners apply the same `steps` and assert against the same checkpoints and
golden fixtures. If a test passes on one stack but fails on the other, that's
exactly the parity bug the harness is designed to catch.

## Schema

Each file is a `TestDefinition`:

```json
{
  "name": "build_gat_with_antara",
  "description": "Build a Gat with Antara section, assert event count + golden swar",
  "steps": [
    { "Cmd": { "cmd": { "Reset": { "compositionType": "gat", "raag": "yaman", "taal": "teentaal" } } } },
    { "Cmd": { "cmd": { "TypeChar": { "ch": "s" } } } },
    { "Cmd": { "cmd": { "TypeChar": { "ch": "r" } } } },
    { "Checkpoint": { "expect": { "eventCount": 2, "cursorBeat": 3 } } },
    { "AssertGoldenSwar": { "fixture": "golden/build-gat-with-antara.swar" } },
    { "AssertGoldenHtml": { "fixture": "golden/build-gat-with-antara.html" } }
  ]
}
````

## Step types

- `Cmd` — wire-transmittable `DebugCommand`. See `sangeet-core/.../debug/DebugCommand.scala`.
- `Checkpoint` — runner-side state assertion. Issues a `GetState` and compares fields. Only the fields present in `expect` are checked; others are ignored.
- `AssertGoldenSwar` / `AssertGoldenHtml` — runner-side byte-equality check of the current composition's exported `.swar` or `.html` against `tests/integration/<fixture-path>`.

## Golden fixtures

Live under `tests/integration/golden/`. Generated by:

```bash
./scripts/regenerate-golden-fixtures.sh
```

(See Phase 8 of `docs/plans/plan-14-...md` for the regeneration tooling.)
EOF

````

- [ ] **Step 3: Commit**

```bash
git add tests/integration/README.md tests/integration/golden/.gitkeep
git commit -m "docs(tests): create tests/integration/ with shared format README"
````

---

## Phase 6 — ScalaTest runner

### Task 6.1: Create `SharedIntegrationSpec`

**Files:**

- Create: `sangeet-desktop/src/test/scala/com/varpas/sangeet/desktop/integration/SharedIntegrationSpec.scala`

The new spec replaces `DebugConsoleTcpSpec`. It loads every `.json` from `tests/integration/`, parses it as a `TestDefinition`, and runs the steps via the existing TCP client (start a `DebugConsole` server, open a socket, send commands, parse responses, assert).

- [ ] **Step 1: Write the spec**

```scala
package com.varpas.sangeet.desktop.integration

import java.nio.file.{Files, Path, Paths}

import scala.jdk.CollectionConverters.*

import io.circe.parser.decode
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.core.debug.{DebugCommand, ExpectedState, TestDefinition, TestStep}

class SharedIntegrationSpec extends AnyFlatSpec with Matchers:

  private val testsDir = Paths.get("../tests/integration")
  private val goldenDir = testsDir.resolve("golden")

  // sbt resolves the cwd relative to the module being tested. If
  // `../tests/integration` doesn't resolve, fall back to the repo-rooted path.
  private def resolveTestsDir: Path =
    if Files.isDirectory(testsDir) then testsDir
    else Paths.get(System.getProperty("user.dir"), "../tests/integration")

  // Discover every .json file at load time so each shows as a separate test.
  private val testFiles: List[Path] =
    Files.list(resolveTestsDir).iterator.asScala
      .filter(_.toString.endsWith(".json"))
      .toList
      .sortBy(_.getFileName.toString)

  testFiles.foreach { path =>
    val raw = new String(Files.readAllBytes(path))
    val defn = decode[TestDefinition](raw).fold(
      err => throw new RuntimeException(s"Failed to parse $path: $err"),
      identity
    )

    s"${defn.name}" should "produce expected state on desktop (TCP)" in {
      withTcpClient { client =>
        defn.steps.foreach(step => executeStep(client, step))
      }
    }
  }

  // ---------------------------------------------------------------------------
  // TCP plumbing — start DebugConsole + open socket. Mirrors the helper in the
  // existing DebugConsoleTcpSpec (move that helper here when deleting that file).
  // ---------------------------------------------------------------------------
  private def withTcpClient(body: TcpClient => Unit): Unit =
    val console = startDebugConsole() // returns (DebugConsole, port)
    try
      val client = new TcpClient("127.0.0.1", console.port)
      try body(client) finally client.close()
    finally console.stop()

  private def startDebugConsole(): DebugConsoleHandle =
    // Identical setup to the deleted DebugConsoleTcpSpec's `withClient` —
    // copy that boilerplate here verbatim.
    ???

  // ---------------------------------------------------------------------------
  // Step dispatcher
  // ---------------------------------------------------------------------------
  private def executeStep(client: TcpClient, step: TestStep): Unit =
    step match
      case TestStep.Cmd(cmd) =>
        val response = client.send(cmd.toTcpText)
        // For dispatch commands, just verify we didn't get an ERROR line.
        if response.startsWith("ERROR") then
          throw new RuntimeException(s"Command failed: $cmd → $response")

      case TestStep.Checkpoint(expect) =>
        val stateJson = client.send("get-state")
        assertExpectedState(stateJson, expect)

      case TestStep.AssertGoldenSwar(fixture) =>
        val actual = client.send("dump-composition")
        val expected = new String(Files.readAllBytes(goldenDir.resolve(fixture.stripPrefix("golden/"))))
        actual shouldBe expected

      case TestStep.AssertGoldenHtml(fixture) =>
        val actual = client.send("export-html") // TODO: add this DebugCommand
        val expected = new String(Files.readAllBytes(goldenDir.resolve(fixture.stripPrefix("golden/"))))
        actual shouldBe expected

  private def assertExpectedState(stateJson: String, expect: ExpectedState): Unit =
    val parsed = io.circe.parser.parse(stateJson).getOrElse(
      throw new RuntimeException(s"State response was not valid JSON: $stateJson")
    )
    expect.eventCount.foreach { e =>
      parsed.hcursor.downField("eventCount").as[Int] shouldBe Right(e)
    }
    expect.cursorBeat.foreach { e =>
      parsed.hcursor.downField("cursorBeat").as[Int] shouldBe Right(e)
    }
    // ... repeat for each ExpectedState field

  extension (cmd: DebugCommand)
    private def toTcpText: String = cmd match
      case DebugCommand.Ping => "ping"
      case DebugCommand.Reset(t, r, ta) =>
        r.fold(s"reset $t $ta")(raag => s"reset $t $raag $ta")
      case DebugCommand.TypeChar(s) => s"type $s"
      case DebugCommand.TypeTimed(s, d) => s"type-timed $s $d"
      case DebugCommand.SwarGroup(notes) => s"swar-group ${notes.mkString(" ")}"
      case DebugCommand.SetOctave(o) => s"set-octave $o"
      case DebugCommand.SetSubdivision(n) => s"set-subdivision $n"
      case DebugCommand.GetState => "get-state"
      case DebugCommand.DumpComposition => "dump-composition"
      // ... one arm per variant
      case other => throw new RuntimeException(s"toTcpText: missing arm for $other")

class TcpClient(host: String, port: Int):
  // Existing TCP client class from DebugConsoleTcpSpec — copy here.
  def send(line: String): String = ???
  def close(): Unit = ???

case class DebugConsoleHandle(port: Int):
  def stop(): Unit = ???
```

The `???` placeholders mark code to copy from the existing `DebugConsoleTcpSpec.scala` — they're literally the same TCP-client + DebugConsole-startup boilerplate. Do that before the next step.

- [ ] **Step 2: Verify the runner picks up zero tests gracefully (no JSON files yet)**

```bash
sbt "sangeetDesktop/testOnly *SharedIntegrationSpec"
```

Expected: spec runs, finds 0 tests, exits cleanly.

- [ ] **Step 3: Commit**

```bash
git add sangeet-desktop/src/test/scala/com/varpas/sangeet/desktop/integration/SharedIntegrationSpec.scala
git commit -m "feat(desktop): SharedIntegrationSpec runner (iterates tests/integration/*.json)"
```

### Task 6.2: Add `ExportHtml` to `DebugCommand`

When porting tests, we'll need a way to read the HTML export over the wire (the existing `dump-composition` returns `.swar` JSON only). Add a new `DebugCommand` variant.

**Files:**

- Modify: `sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/DebugCommand.scala`
- Modify: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/DebugCommandHandler.scala`
- Modify: `sangeet-web/src/Debug/Interpreter.elm`

- [ ] **Step 1: Add the variant**

In `DebugCommand.scala`, add to the enum:

```scala
case ExportHtml
```

Add to `fromText`:

```scala
case "export-html" :: Nil => Right(ExportHtml)
```

- [ ] **Step 2: Add the desktop dispatch**

In `DebugCommandHandler.applyDebugCommand`, add:

```scala
case ExportHtml => exportHtml()
```

Add the helper:

```scala
private def exportHtml(): String =
  pane.getEditor match
    case None => "ERROR: no composition loaded"
    case Some(ed) =>
      com.varpas.sangeet.core.format.HtmlExporter.export(ed.composition)
```

(Verify `HtmlExporter.export` is the right call — `grep -rn 'HtmlExporter' sangeet-core/src/main`.)

- [ ] **Step 3: Add the web interpreter arm**

`Debug/Interpreter.elm`: extend the `DebugCmd` sum + `cmdDecoder` + `applyCmd` with an `ExportHtml` variant. On apply, dispatch a Msg that calls `ApiExport.exportHtml` and routes the eventual `GotExportHtml` response back over WS using a pending-debug-request map. (This is async; the response goes out on `GotExportHtml`, not on `DebugCommandReceived`.)

For this, add a `pendingDebugRequests : Dict String DebugRequestKind` field to Model, where `DebugRequestKind = AwaitingHtml String | AwaitingSwar String` etc. When `GotExportHtml` fires, look up which debug request id was pending and emit the WS response.

- [ ] **Step 4: Compile both sides**

```bash
sbt sangeetCore/compile sangeetDesktop/compile
cd sangeet-web && ./node_modules/.bin/elm make src/Main.elm --output=/tmp/check.js
```

- [ ] **Step 5: Commit**

```bash
git add sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/DebugCommand.scala \
        sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/DebugCommandHandler.scala \
        sangeet-web/src/State/Model.elm \
        sangeet-web/src/Debug/Interpreter.elm \
        sangeet-web/src/State/Update.elm
git commit -m "feat: ExportHtml DebugCommand for cross-stack html parity tests"
```

---

## Phase 7 — Playwright runner

### Task 7.1: WS server helper

**Files:**

- Create: `e2e/integration/helpers/ws-server.ts`

- [ ] **Step 1: Add `ws` to e2e devDependencies**

```bash
cd e2e && npm install --save-dev ws @types/ws
git add package.json package-lock.json
```

- [ ] **Step 2: Write the helper**

```typescript
import { WebSocketServer, WebSocket } from 'ws';
import { AddressInfo } from 'net';
import { DebugCommand } from './test-definition';

interface PendingRequest {
  resolve: (value: any) => void;
  reject: (err: any) => void;
}

/** Minimal request/response WS server used by the parity tests. Each call to
 * `send` returns a Promise that resolves when the Elm app responds with the
 * matching id. Lives only for the duration of one test; teardown closes the
 * socket.
 */
export class TestWsServer {
  private server: WebSocketServer;
  private socket: WebSocket | null = null;
  private nextId = 1;
  private pending = new Map<string, PendingRequest>();
  public readonly port: number;

  private constructor(server: WebSocketServer) {
    this.server = server;
    const addr = server.address() as AddressInfo;
    this.port = addr.port;
    server.on('connection', (ws) => {
      this.socket = ws;
      ws.on('message', (raw) => this.handleIncoming(raw.toString()));
    });
  }

  static async start(): Promise<TestWsServer> {
    return new Promise((resolve) => {
      const wss = new WebSocketServer({ port: 0, host: '127.0.0.1' });
      wss.on('listening', () => resolve(new TestWsServer(wss)));
    });
  }

  async waitForConnection(timeoutMs = 5000): Promise<void> {
    const start = Date.now();
    while (!this.socket) {
      if (Date.now() - start > timeoutMs) throw new Error('WS connection timeout');
      await new Promise((r) => setTimeout(r, 50));
    }
  }

  async send(cmd: DebugCommand): Promise<any> {
    if (!this.socket) throw new Error('No WS connection — did the page load with ?debug= ?');
    const id = `req-${this.nextId++}`;
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      this.socket!.send(JSON.stringify({ id, cmd }));
      // Per-request timeout: most commands are sub-100ms; allow 2s for export-html.
      setTimeout(() => {
        if (this.pending.has(id)) {
          this.pending.delete(id);
          reject(new Error(`Timeout waiting for response to ${JSON.stringify(cmd)}`));
        }
      }, 2000);
    });
  }

  private handleIncoming(raw: string): void {
    let parsed: any;
    try {
      parsed = JSON.parse(raw);
    } catch (e) {
      console.error('[ws-server] invalid JSON from Elm:', raw);
      return;
    }
    const pending = this.pending.get(parsed.id);
    if (!pending) {
      console.warn('[ws-server] response with unknown id:', parsed.id);
      return;
    }
    this.pending.delete(parsed.id);
    if (parsed.error) pending.reject(new Error(parsed.error));
    else pending.resolve(parsed.result);
  }

  async close(): Promise<void> {
    if (this.socket) this.socket.close();
    return new Promise((resolve) => this.server.close(() => resolve()));
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add e2e/integration/helpers/ws-server.ts e2e/package.json e2e/package-lock.json
git commit -m "feat(e2e): TestWsServer helper for parity tests"
```

### Task 7.2: Golden fixture helpers

**Files:**

- Create: `e2e/integration/helpers/golden-fixtures.ts`

- [ ] **Step 1: Write the helper**

```typescript
import * as fs from 'fs';
import * as path from 'path';
import { expect } from '@playwright/test';

const GOLDEN_ROOT = path.resolve(__dirname, '../../../tests/integration');

export function assertMatchesGolden(actual: string, fixturePath: string): void {
  const fullPath = path.resolve(GOLDEN_ROOT, fixturePath);
  const expected = fs.readFileSync(fullPath, 'utf-8');
  expect(actual).toEqual(expected); // byte-equality
}

export function loadDefinitions(): { name: string; path: string }[] {
  const dir = path.join(GOLDEN_ROOT);
  return fs
    .readdirSync(dir)
    .filter((f) => f.endsWith('.json'))
    .sort()
    .map((f) => ({ name: f.replace(/\.json$/, ''), path: path.join(dir, f) }));
}
```

- [ ] **Step 2: Commit**

```bash
git add e2e/integration/helpers/golden-fixtures.ts
git commit -m "feat(e2e): golden-fixture comparison helpers"
```

### Task 7.3: `parity.spec.ts`

**Files:**

- Create: `e2e/integration/parity.spec.ts`

- [ ] **Step 1: Write the spec**

```typescript
import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import { TestWsServer } from './helpers/ws-server';
import { assertMatchesGolden, loadDefinitions } from './helpers/golden-fixtures';
import { TestDefinition, TestStep, ExpectedState } from './helpers/test-definition';

for (const { name, path: filePath } of loadDefinitions()) {
  test(name, async ({ page }) => {
    const defn: TestDefinition = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
    const ws = await TestWsServer.start();
    try {
      await page.goto(`/?debug=ws://localhost:${ws.port}`);
      await ws.waitForConnection();

      for (const step of defn.steps) {
        if ('Cmd' in step) {
          await ws.send(step.Cmd.cmd);
        } else if ('Checkpoint' in step) {
          const state = await ws.send({ GetState: {} });
          assertCheckpoint(state, step.Checkpoint.expect);
        } else if ('AssertGoldenSwar' in step) {
          const actual = (await ws.send({ DumpComposition: {} })) as string;
          assertMatchesGolden(actual, step.AssertGoldenSwar.fixture);
        } else if ('AssertGoldenHtml' in step) {
          const actual = (await ws.send({ ExportHtml: {} })) as string;
          assertMatchesGolden(actual, step.AssertGoldenHtml.fixture);
        }
      }
    } finally {
      await ws.close();
    }
  });
}

function assertCheckpoint(state: any, expect_: ExpectedState): void {
  for (const key of Object.keys(expect_) as (keyof ExpectedState)[]) {
    if (expect_[key] !== undefined) {
      expect(state[key]).toEqual(expect_[key]);
    }
  }
}
```

- [ ] **Step 2: Add the new directory to Playwright's test glob**

Edit `e2e/playwright.config.ts` so `testDir` covers both `tests/` and `integration/`:

```typescript
export default defineConfig({
  testDir: '.',
  testMatch: ['tests/**/*.spec.ts', 'integration/**/*.spec.ts'],
  // ... existing config
});
```

(Confirm the exact existing shape via `cat e2e/playwright.config.ts` first.)

- [ ] **Step 3: Sanity check Playwright picks up zero parity tests gracefully**

```bash
cd e2e && npx playwright test --list integration/parity.spec.ts
```

Expected: 0 tests listed (since no JSON files yet).

- [ ] **Step 4: Commit**

```bash
git add e2e/integration/parity.spec.ts e2e/playwright.config.ts
git commit -m "feat(e2e): parity.spec.ts runner (iterates tests/integration/*.json)"
```

---

## Phase 8 — Golden fixtures + first canonical tests

Build 10 canonical compositions, save their `.swar`/`.html` as golden fixtures, write the matching JSON test definitions. After this phase, the parity harness has 10 working tests passing on both stacks.

### Task 8.1: Golden-fixture regeneration script

**Files:**

- Create: `scripts/regenerate_golden_fixtures.py`

- [ ] **Step 1: Write the script**

```python
#!/usr/bin/env python3
"""Re-generate all golden fixtures by replaying each tests/integration/*.json
against the desktop app (via TCP) and writing its dump-composition + export-html
outputs to tests/integration/golden/<name>.swar + .html.

Run after intentional changes to the .swar or .html format. The web side picks
up the new bytes automatically (same fixtures consumed by both runners).

Pre-condition: sangeet-desktop is built and the TCP debug console is reachable
on 127.0.0.1:28081. The simplest way is to start the app fresh in another
terminal: `sbt sangeetDesktop/run`.
"""
import json
import socket
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
TESTS_DIR = REPO_ROOT / 'tests' / 'integration'
GOLDEN_DIR = TESTS_DIR / 'golden'
HOST, PORT = '127.0.0.1', 28081


def tcp_send(sock, line):
    sock.sendall((line + '\n').encode('utf-8'))
    chunks = []
    while True:
        chunk = sock.recv(65536)
        if not chunk:
            break
        chunks.append(chunk.decode('utf-8'))
        if '---END---' in chunks[-1]:
            break
    return ''.join(chunks).rstrip('\n').rstrip('---END---').rstrip('\n')


def cmd_to_text(cmd):
    """Mirror the TCP text protocol from DebugCommandHandler. Used here for
    one-off regeneration; tests do the same conversion at runtime."""
    if 'Reset' in cmd:
        r = cmd['Reset']
        raag = r.get('raag')
        return f"reset {r['compositionType']} {raag + ' ' if raag else ''}{r['taal']}"
    if 'TypeChar' in cmd:
        return f"type {cmd['TypeChar']['ch']}"
    # ... add arms for each command shape this script needs to drive
    raise ValueError(f"Unsupported command for regeneration: {cmd}")


def replay(defn, sock):
    for step in defn['steps']:
        if 'Cmd' in step:
            tcp_send(sock, cmd_to_text(step['Cmd']['cmd']))
    return tcp_send(sock, 'dump-composition'), tcp_send(sock, 'export-html')


def main():
    GOLDEN_DIR.mkdir(parents=True, exist_ok=True)
    for defn_path in sorted(TESTS_DIR.glob('*.json')):
        defn = json.loads(defn_path.read_text())
        with socket.create_connection((HOST, PORT)) as sock:
            tcp_send(sock, 'reset gat yaman teentaal')  # ensure clean state
            swar, html = replay(defn, sock)
        swar_path = GOLDEN_DIR / f"{defn['name']}.swar"
        html_path = GOLDEN_DIR / f"{defn['name']}.html"
        swar_path.write_text(swar)
        html_path.write_text(html)
        print(f"  wrote {swar_path.name} + {html_path.name}")


if __name__ == '__main__':
    main()
```

- [ ] **Step 2: Make executable**

```bash
chmod +x scripts/regenerate_golden_fixtures.py
```

- [ ] **Step 3: Commit**

```bash
git add scripts/regenerate_golden_fixtures.py
git commit -m "tools(scripts): regenerate_golden_fixtures.py for cross-stack parity tests"
```

### Task 8.2: Author 10 canonical test definitions

Hand-write 10 JSON files covering the user's stated goal (Gat with Antara + multiple Taans, Palta, Bandish, Sargam, and edge cases).

**Files (create each):**

- `tests/integration/01-empty-gat.json` — Reset → checkpoint event count = 0
- `tests/integration/02-simple-gat-yaman-teentaal.json` — Type 16 notes → golden
- `tests/integration/03-gat-with-antara.json` — Sthayi + Antara, ~32 notes → golden
- `tests/integration/04-gat-with-antara-3-taans.json` — Sthayi + Antara + 3 Taans → golden
- `tests/integration/05-palta-rupak.json` — Palta in Rupak → golden
- `tests/integration/06-bandish-bhairavi.json` — Bandish + sahitya → golden
- `tests/integration/07-sargam-bilawal.json` — Sargam exercise → golden
- `tests/integration/08-ornament-meend-gat.json` — Gat with meend ornaments → golden
- `tests/integration/09-stroke-da-ra-jod.json` — Gat with all stroke types → golden
- `tests/integration/10-tihai-in-taan.json` — Gat with tihai in last taan → golden

- [ ] **Step 1: Write the first one as a template**

`tests/integration/01-empty-gat.json`:

```json
{
  "name": "01-empty-gat",
  "description": "Reset to empty Gat in Yaman + Teentaal. No notes. Verifies the baseline reset path produces identical state and bytes.",
  "steps": [
    {
      "Cmd": {
        "cmd": { "Reset": { "compositionType": "gat", "raag": "yaman", "taal": "teentaal" } }
      }
    },
    {
      "Checkpoint": {
        "expect": {
          "eventCount": 0,
          "cursorBeat": 1,
          "cursorCycle": 0,
          "taalName": "Teentaal",
          "raagName": "Yaman",
          "sectionCount": 1
        }
      }
    },
    { "AssertGoldenSwar": { "fixture": "golden/01-empty-gat.swar" } },
    { "AssertGoldenHtml": { "fixture": "golden/01-empty-gat.html" } }
  ]
}
```

- [ ] **Step 2: Write the remaining 9 files**

Each follows the same shape. See `sangeet-desktop/.../SampleComposition.scala` for canonical input patterns to model the more complex tests on (Yaman Vilambit Gat is the existing sample — use its `Type-Char` sequence as the source-of-truth for `04-gat-with-antara-3-taans.json`).

- [ ] **Step 3: Generate golden fixtures**

```bash
sbt sangeetDesktop/run &  # start desktop in another terminal
sleep 10                  # wait for it to come up
./scripts/regenerate_golden_fixtures.py
```

Expected: 10 `.swar` + 10 `.html` files appear under `tests/integration/golden/`.

Inspect a few to make sure they look right:

```bash
head -20 tests/integration/golden/01-empty-gat.swar
head -20 tests/integration/golden/04-gat-with-antara-3-taans.html
```

- [ ] **Step 4: Run desktop parity tests**

```bash
sbt "sangeetDesktop/testOnly *SharedIntegrationSpec"
```

Expected: 10 passing.

- [ ] **Step 5: Run web parity tests**

```bash
cd e2e && npx playwright test integration/parity.spec.ts
```

Expected: 10 passing.

If any test passes on one stack but fails on the other, **that's the bug the harness is built to catch** — investigate the divergence in the Interpreter / DebugCommandHandler / serialize path. Do not paper over with stack-specific assertions.

- [ ] **Step 6: Commit**

```bash
git add tests/integration/*.json tests/integration/golden/*.swar tests/integration/golden/*.html
git commit -m "test(integration): 10 canonical cross-stack parity tests with golden fixtures"
```

---

## Phase 9 — Port the 125 desktop tests

Mechanical conversion. Each `it should "..." in withClient { ... }` block becomes one JSON file. The body of the test (TCP sends + assertions) maps directly to `Cmd` + `Checkpoint` steps. There are no golden-file assertions in these 125 (they test intermediate behavior, not exports).

### Task 9.1: Convert one test as a worked example

**Files:**

- Create: `tests/integration/100-tcp-protocol-ping.json` (or similar — use `100-` prefix for the ported batch to keep them sorted after canonical tests)
- (No source-file deletions yet — those come in Task 9.3.)

- [ ] **Step 1: Convert the simplest test**

The existing first test:

```scala
"TCP protocol" should "respond to ping" in withClient { (w, r) =>
  w("ping")
  r() shouldBe "PONG"
}
```

Becomes `tests/integration/100-tcp-protocol-ping.json`:

```json
{
  "name": "100-tcp-protocol-ping",
  "description": "Ping returns PONG (legacy: TCP protocol smoke test).",
  "steps": [{ "Cmd": { "cmd": { "Ping": {} } } }]
}
```

(No checkpoint needed — the runner asserts a non-ERROR response by default. If you want stricter assertion, extend `Cmd` step type to take an optional `expectedResponse` field.)

- [ ] **Step 2: Run both runners**

```bash
sbt "sangeetDesktop/testOnly *SharedIntegrationSpec"
cd e2e && npx playwright test integration/parity.spec.ts -g "100-tcp-protocol-ping"
```

- [ ] **Step 3: Commit**

```bash
git add tests/integration/100-tcp-protocol-ping.json
git commit -m "test(integration): port first DebugConsoleTcpSpec test to shared format"
```

### Task 9.2: Convert the remaining 124 tests in batches

This is mechanical but not trivial. Budget 4-6 hours.

- [ ] **Step 1: Open both files side by side**

```bash
# Source:
$EDITOR sangeet-desktop/src/test/scala/com/varpas/sangeet/desktop/editor/DebugConsoleTcpSpec.scala
```

- [ ] **Step 2: Group the tests by behavior cluster**

The existing file is already grouped by `"<feature>" should "..."`. The clusters are roughly:

- TCP protocol (3)
- Reset command (6)
- Basic swar input (4)
- All swar keys (2)
- Octave changes (2)
- Rest and sustain (1)
- Dual swar (2)
- Swar grouping (10)
- ... (continue from the existing file's headings)

Number ported files as `1XX-<cluster>-<n>.json` so all ports sort together.

- [ ] **Step 3: For each test, write the JSON file**

Mechanical translation pattern:

| Scala                                        | JSON                                                                       |
| -------------------------------------------- | -------------------------------------------------------------------------- |
| `w("type s r g m p")`                        | `{ "Cmd": { "cmd": { "TypeChar": { "ch": "srgmp" } } } }`                  |
| `w("set-octave taar")`                       | `{ "Cmd": { "cmd": { "SetOctave": { "octave": "taar" } } } }`              |
| `w("get-events"); r() should include("...")` | `{ "Cmd": { "cmd": { "GetEvents": {} } } }` + a `Checkpoint` if structural |
| `getState should have eventCount == 10`      | `{ "Checkpoint": { "expect": { "eventCount": 10 } } }`                     |

If a test checks something the `Checkpoint` schema doesn't yet cover, **extend `ExpectedState`** (one place, both runners pick it up).

- [ ] **Step 4: After every ~15 ports, run both runners**

```bash
sbt "sangeetDesktop/testOnly *SharedIntegrationSpec" && \
  cd e2e && npx playwright test integration/parity.spec.ts
```

Catch any web-side bugs early. Commit per batch of ~15:

```bash
git add tests/integration/1XX-*.json
git commit -m "test(integration): port batch <N> of DebugConsoleTcpSpec → shared format"
```

### Task 9.3: Delete `DebugConsoleTcpSpec`

After all 125 are ported and passing on both stacks.

- [ ] **Step 1: Verify all are ported**

```bash
ls tests/integration/1??-*.json | wc -l
# Expected: 125
```

- [ ] **Step 2: Delete the source file**

```bash
git rm sangeet-desktop/src/test/scala/com/varpas/sangeet/desktop/editor/DebugConsoleTcpSpec.scala
```

- [ ] **Step 3: Verify all tests still pass**

```bash
sbt sangeetDesktop/test
```

Expected: the desktop module's test count = (previous count - 125 from DebugConsoleTcpSpec + however many distinct test cases SharedIntegrationSpec discovers from JSON files).

- [ ] **Step 4: Commit**

```bash
git commit -m "test(desktop): retire DebugConsoleTcpSpec — all cases now in shared format"
```

---

## Phase 10 — MCP server transport flag

Make `mcp-servers/sangeet-debug-console/` work over WS, not just TCP. Same 31 tools, configured by `--transport tcp|ws`.

### Task 10.1: Extract transport abstraction

**Files:**

- Modify: `mcp-servers/sangeet-debug-console/server.py`
- Create: `mcp-servers/sangeet-debug-console/transport.py`
- Create: `mcp-servers/sangeet-debug-console/transport_tcp.py`
- Create: `mcp-servers/sangeet-debug-console/transport_ws.py`

- [ ] **Step 1: Write the base class**

`transport.py`:

```python
from abc import ABC, abstractmethod


class Transport(ABC):
    """Abstract transport for sending DebugCommand-equivalent text/JSON to a
    running sangeet app. Subclasses speak TCP (desktop) or WebSocket (web)."""

    @abstractmethod
    def send(self, command_text: str) -> str:
        """Send a command (in the legacy text format) and return the response.
        For WS, the text command is parsed locally into JSON before sending."""

    @abstractmethod
    def close(self) -> None: ...
```

- [ ] **Step 2: Write TCP transport (factor from existing server.py)**

`transport_tcp.py`:

```python
import socket
from .transport import Transport


class TcpTransport(Transport):
    def __init__(self, host: str = '127.0.0.1', port: int = 28081):
        self.host = host
        self.port = port

    def send(self, command_text: str) -> str:
        with socket.create_connection((self.host, self.port)) as sock:
            sock.sendall((command_text + '\n').encode('utf-8'))
            chunks = []
            while True:
                chunk = sock.recv(65536)
                if not chunk:
                    break
                chunks.append(chunk.decode('utf-8'))
                if '---END---' in chunks[-1]:
                    break
            return ''.join(chunks)

    def close(self) -> None:
        pass
```

- [ ] **Step 3: Write WS transport**

`transport_ws.py`:

```python
import json
import threading
from queue import Queue
from .transport import Transport
import websockets.sync.server as wss
import websockets


# Reuse the text-to-JSON conversion: import the equivalent map from a shared
# constants file, OR keep it inline here mirroring DebugCommand.fromText.
def text_to_json_cmd(text: str) -> dict:
    parts = text.strip().split()
    cmd = parts[0]
    if cmd == 'ping':
        return {'Ping': {}}
    if cmd == 'reset':
        # reset <type> [raag] <taal>
        if len(parts) == 4:
            return {'Reset': {'compositionType': parts[1], 'raag': parts[2], 'taal': parts[3]}}
        elif len(parts) == 3:
            return {'Reset': {'compositionType': parts[1], 'taal': parts[2]}}
    if cmd == 'type':
        return {'TypeChar': {'ch': ''.join(parts[1:])}}
    # ... arms for each command (same shape as TS in test-definition.ts)
    raise ValueError(f"Unsupported command for WS transport: {text}")


class WsTransport(Transport):
    """Hosts a WS server on `port`. The Elm app (loaded with
    ?debug=ws://localhost:<port>) connects; subsequent send() calls go to that
    connection and block on the matching response."""

    def __init__(self, port: int):
        self.port = port
        self._next_id = 0
        self._pending: dict[str, Queue] = {}
        self._socket = None
        self._connected = threading.Event()
        self._server = wss.serve(self._handle, '127.0.0.1', port)
        threading.Thread(target=self._server.serve_forever, daemon=True).start()
        print(f"[ws-transport] listening on ws://localhost:{port}")
        print(f"[ws-transport] load the web app with ?debug=ws://localhost:{port}")

    def _handle(self, websocket):
        self._socket = websocket
        self._connected.set()
        for raw in websocket:
            data = json.loads(raw)
            q = self._pending.get(data.get('id'))
            if q:
                q.put(data)

    def send(self, command_text: str) -> str:
        if not self._connected.wait(timeout=30):
            return "ERROR: no WS client connected within 30s"
        self._next_id += 1
        req_id = f"req-{self._next_id}"
        q = Queue()
        self._pending[req_id] = q
        cmd_json = text_to_json_cmd(command_text)
        self._socket.send(json.dumps({'id': req_id, 'cmd': cmd_json}))
        try:
            response = q.get(timeout=5)
        finally:
            del self._pending[req_id]
        if response.get('error'):
            return f"ERROR: {response['error']}"
        return json.dumps(response.get('result'))

    def close(self) -> None:
        if self._socket:
            self._socket.close()
        self._server.shutdown()
```

- [ ] **Step 4: Add `websockets` to pyproject**

```bash
cd mcp-servers/sangeet-debug-console
# Edit pyproject.toml — add 'websockets>=12.0' to dependencies.
```

- [ ] **Step 5: Refactor server.py**

In `server.py`, add at the top:

```python
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--transport', choices=['tcp', 'ws'], default='tcp')
parser.add_argument('--port', type=int, default=None,
                    help='Override transport port (TCP default: 28081, WS default: 9999)')
args, _ = parser.parse_known_args()

if args.transport == 'tcp':
    from .transport_tcp import TcpTransport
    transport = TcpTransport(port=args.port or 28081)
else:
    from .transport_ws import WsTransport
    transport = WsTransport(port=args.port or 9999)
```

Then replace the existing module-level `send()` function (which open a TCP socket per call) with one that delegates to `transport.send()`:

```python
def send(line: str) -> str:
    return transport.send(line)
```

- [ ] **Step 6: Smoke-test both transports**

```bash
# TCP mode (existing behavior):
uvx --from . sangeet-debug-console --transport tcp
# In another terminal: configure ~/.claude.json to point an agent at this; try `ping` via MCP tool.

# WS mode:
uvx --from . sangeet-debug-console --transport ws --port 9999
# Open the web app at http://localhost:3000/?debug=ws://localhost:9999.
# In another terminal: configure ~/.claude.json; try `ping` via MCP tool.
```

- [ ] **Step 7: Commit**

```bash
git add mcp-servers/sangeet-debug-console/
git commit -m "feat(mcp): --transport tcp|ws flag for sangeet-debug-console"
```

### Task 10.2: Update README

**Files:**

- Modify: `mcp-servers/sangeet-debug-console/README.md`

- [ ] **Step 1: Add the WS section**

Append to the existing README:

````markdown
## WebSocket transport (web app)

The same MCP server can drive the running web app via the WebSocket debug bridge.
This lets agents iterate on web features the same way they iterate on desktop
features.

### Setup

1. Run the MCP server in WS mode:
   ```bash
   uvx --from . sangeet-debug-console --transport ws --port 9999
   ```
````

2. Open the web app with the matching `?debug=` URL param:

   ```
   http://localhost:3000/?debug=ws://localhost:9999
   ```

3. The MCP server's first command will block until the Elm app connects (max
   30s). After that, the same ~31 tools work identically to TCP mode.

### Security

The web app's `ports.js` enforces a loopback-only URL allowlist
(`ws://localhost:` or `ws://127.0.0.1:` only). Any other URL is silently
ignored — a hostile page can't talk the running Elm app into exfiltrating state
to a remote endpoint via this bridge.

````

- [ ] **Step 2: Commit**

```bash
git add mcp-servers/sangeet-debug-console/README.md
git commit -m "docs(mcp): document --transport ws mode + security model"
````

---

## Phase 11 — Documentation + final verification

### Task 11.1: Write `docs/developer/debug-bridge.md`

**Files:**

- Create: `docs/developer/debug-bridge.md`

- [ ] **Step 1: Write the doc**

```markdown
# Debug Bridge — Architecture & Usage

Both the desktop and web apps expose a back door for programmatic control:

- Desktop: TCP debug console on `127.0.0.1:28081` (since plan-tcp-debug-console)
- Web: WebSocket bridge on a loopback port, activated by the `?debug=ws://localhost:PORT` URL param (this plan)

Both speak the same vocabulary — the `DebugCommand` ADT defined in
`sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/DebugCommand.scala`.

## What it's for

- **Agent loops.** The `mcp-servers/sangeet-debug-console` MCP server wraps either
  transport. An agent writes a feature → connects via MCP → drives the running
  app to exercise the change → reads state → iterates. See the MCP server's
  README for setup.
- **Integration tests.** The `SharedIntegrationSpec` (desktop, via TCP) and
  `parity.spec.ts` (web, via WS) load shared JSON test definitions from
  `tests/integration/` and assert against identical state checkpoints + golden
  `.swar`/`.html` fixtures. If a test passes on one stack but fails on the
  other, that's exactly the parity bug the harness is built to catch.

## Adding a new command

1. Add a case to `enum DebugCommand` in sangeet-core.
2. Add a `fromText` arm if the legacy text protocol should accept it.
3. Add a dispatch arm in `DebugCommandHandler.applyDebugCommand` (desktop).
4. Add a dispatch arm in `Debug.Interpreter.applyCmd` (web).
5. Add a `text_to_json_cmd` arm in `mcp-servers/.../transport_ws.py` if the MCP
   server should support it over WS.

Both desktop and web fail to compile until both dispatch arms exist — drift
risk is bounded.

## Wire formats

- **TCP**: newline-delimited text. `ping`, `reset gat yaman teentaal`,
  `type srgmp`. Parser in `DebugCommand.fromText`.
- **WS**: JSON `{ "id": "req-1", "cmd": { "Ping": {} } }`. Response shape:
  `{ "id": "req-1", "result": <value>, "error": null }`. Encoder is whatever
  circe produces for the enum — see `DebugCommandSpec` for the exact shape.

## Security

- TCP: bound to `127.0.0.1` (loopback only).
- WS: bridge in `ports.js` rejects any URL that doesn't start with
  `ws://localhost:` or `ws://127.0.0.1:`. Production bundles still contain the
  code but never connect (no `?debug=` query param). For belt-and-suspenders,
  the WS server itself binds to `127.0.0.1` only.

## Tests

- Shared definitions: `tests/integration/*.json` (135 files: 10 canonical
  parity, 125 ported from the retired DebugConsoleTcpSpec).
- Golden fixtures: `tests/integration/golden/*.swar` + `*.html`. Regenerate with
  `./scripts/regenerate_golden_fixtures.py` when an intentional format change
  lands.
```

- [ ] **Step 2: Commit**

```bash
git add docs/developer/debug-bridge.md
git commit -m "docs(developer): debug bridge architecture + usage doc"
```

### Task 11.2: Update CLAUDE.md reference

**Files:**

- Modify: `CLAUDE.md`

- [ ] **Step 1: Add a line under "What's Built" → "TCP debug console" entry**

Find:

```
- TCP debug console on 127.0.0.1:28081 — connect via `nc` to simulate key input, inspect state, get thread dumps even during UI freeze
```

Change to:

```
- TCP debug console on 127.0.0.1:28081 (desktop) + WebSocket debug bridge on web (gated by `?debug=ws://localhost:PORT` URL param) — both speak the shared `DebugCommand` ADT from sangeet-core. See `docs/developer/debug-bridge.md`. Used by the MCP server (`mcp-servers/sangeet-debug-console/`) and the cross-platform parity harness (`tests/integration/*.json`).
```

- [ ] **Step 2: Update the test counts**

In the "What's Built" section, find the line citing test counts:

```
- 565 tests in sangeet-core ... 86 TCP integration tests in sangeet-desktop (773 Scala total)
```

Update to reflect the changes:

- `+18` from new DebugCommandSpec
- `-125` from retired DebugConsoleTcpSpec (now in shared format)
- `+135` from new SharedIntegrationSpec (auto-discovered, count varies)

Recompute via:

```bash
sbt 'show test:definedTests' 2>&1 | grep -c 'TestDefinition'
```

(Or simply: `sbt test` and read the summary line.)

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md
git commit -m "docs(claude): point to debug bridge doc + refresh test counts"
```

### Task 11.3: Final verification + PR

- [ ] **Step 1: Run the whole test suite locally**

```bash
sbt test
cd sangeet-web && ./node_modules/.bin/elm-test
cd e2e && npx playwright test
```

All three layers green.

- [ ] **Step 2: `make lint`**

```bash
make lint
```

All formatters/linters pass.

- [ ] **Step 3: Push and open PR**

```bash
git push -u origin feat/cross-platform-debug-bridge-and-parity-harness
gh pr create --base main --title "feat: cross-platform debug bridge + test parity harness" --body "$(cat <<'EOF'
## Summary

Plan 14 — atomic delivery of:

1. **Shared DebugCommand vocabulary** in sangeet-core; both TCP (desktop) and WebSocket (web) transports dispatch through it.
2. **WebSocket debug bridge on web**, gated by `?debug=ws://localhost:PORT` URL param. Loopback-only by design. Production bundles never connect.
3. **Shared cross-stack integration test runner** (`tests/integration/*.json` consumed by both ScalaTest's `SharedIntegrationSpec` and Playwright's `parity.spec.ts`).
4. **Server byte-equality** for `.swar` exports — server now returns pre-serialized string; Elm downloads verbatim instead of re-encoding.
5. **10 canonical parity tests** with golden `.swar`/`.html` fixtures covering Gat / Antara / Taan / Palta / Bandish / Sargam / ornaments / strokes / tihai.
6. **125 ported tests** from the retired `DebugConsoleTcpSpec`, now exercising both stacks.
7. **MCP server `--transport tcp|ws` flag** — same 31 tools, two transports.

See `docs/plans/plan-14-cross-platform-debug-bridge-and-parity-harness.md` for design rationale.

## Test plan

- [ ] CI lint / scala-tests / elm-tests / e2e all green
- [ ] After merge: smoke-test `--transport ws` MCP mode by driving a sample composition via an agent and confirming exported `.swar` matches the desktop equivalent byte-for-byte
- [ ] Theme toggle (Task #214) follow-up still pending; unaffected by this PR
EOF
)"
```

- [ ] **Step 4: Watch CI, merge when green**

---

# Workstream B — Shared UI Strings Catalog

> **This entire workstream ships as PR-B**, independent of PR-A. See "Workstream organization" at the top of this doc for parallel-execution rules and merge-conflict surface.

## Phase 12 — Codegen infrastructure

Build the entire codegen pipeline end-to-end with an _empty_ catalog before migrating any literals. Goal: at end of phase, `make gen-strings` produces `UiStrings.scala` + `UiStrings.elm` that both compile, both are committed, and the empty catalog passes round-trip tests.

### Task 12.1: Author the catalog JSON schema + empty catalog file

**Files:**

- Create: `sangeet-core/src/main/resources/ui-strings.json`
- Create: `docs/developer/ui-strings-catalog.md`

- [ ] **Step 1: Create the empty catalog with schema-documenting comment**

```json
{
  "$comment": "Source of truth for all user-visible strings shared between desktop and web. See docs/developer/ui-strings-catalog.md for schema, key naming, and migration guide.",
  "entries": {}
}
```

- [ ] **Step 2: Write `docs/developer/ui-strings-catalog.md`**

Cover: key naming (`area.component.element`), `value` vs `template`+`params` shape, `platform: both|desktop|web` semantics, `description` requirement, how to add a string (edit JSON → `make gen-strings` → use `UiStrings.xxx` on both sides), how parity check works, what to do if it fails.

- [ ] **Step 3: Commit**

```bash
git add sangeet-core/src/main/resources/ui-strings.json docs/developer/ui-strings-catalog.md
git commit -m "feat(strings): add empty UI strings catalog + schema docs"
```

### Task 12.2: Scala codegen — `UiStringsCodegen` sbt task

**Files:**

- Create: `project/UiStringsCodegen.scala`
- Create: `sangeet-core/src/test/scala/com/varpas/sangeet/core/strings/UiStringsCodegenSpec.scala`
- Modify: `build.sbt`

- [ ] **Step 1: Write failing tests** (`UiStringsCodegenSpec.scala`)

```scala
package com.varpas.sangeet.core.strings

import org.scalatest.funsuite.AnyFunSuite
import io.circe.parser.parse

class UiStringsCodegenSpec extends AnyFunSuite:

  test("emits compile-ready Scala header"):
    val out = UiStringsCodegen.emitScala(parse("""{"entries":{}}""").toOption.get)
    assert(out.contains("package com.varpas.sangeet.core.strings"))
    assert(out.contains("object UiStrings:"))
    assert(out.contains("GENERATED FILE"))

  test("emits typed val constant for 'value' entry"):
    val json = parse("""{"entries":{"toolbar.file.new":{"value":"New","platform":"both","description":""}}}""").toOption.get
    val out = UiStringsCodegen.emitScala(json)
    assert(out.contains("""val toolbarFileNew: String = "New""""))

  test("emits typed def function for parameterized entry"):
    val json = parse("""
      {"entries":{"toolbar.beatCount":{
        "template":"Beats: {current} / {total}",
        "params":[{"name":"current","type":"int"},{"name":"total","type":"int"}],
        "platform":"both","description":""
      }}}
    """).toOption.get
    val out = UiStringsCodegen.emitScala(json)
    assert(out.contains("def toolbarBeatCount(current: Int, total: Int): String"))
    assert(out.contains("""s"Beats: $current / $total""""))

  test("escapes double quotes and backslashes in values"):
    val json = parse("""{"entries":{"k":{"value":"\"quoted\" \\ back","platform":"both","description":""}}}""").toOption.get
    val out = UiStringsCodegen.emitScala(json)
    assert(out.contains("""val k: String = "\"quoted\" \\ back""""))

  test("sorts entries deterministically"):
    val json = parse("""{"entries":{"z":{"value":"Z","platform":"both","description":""},"a":{"value":"A","platform":"both","description":""}}}""").toOption.get
    val out = UiStringsCodegen.emitScala(json)
    assert(out.indexOf("val a:") < out.indexOf("val z:"))
```

- [ ] **Step 2: Run tests, verify they fail**

```bash
sbt sangeetCore/testOnly *UiStringsCodegenSpec*
```

Expected: compile error or test failure (object doesn't exist yet).

- [ ] **Step 3: Implement `project/UiStringsCodegen.scala`**

```scala
import sbt._
import java.nio.file.{Files, Path => JPath}
import scala.io.Source

object UiStringsCodegen:

  case class Param(name: String, paramType: String)
  case class Entry(
    key: String,
    value: Option[String],
    template: Option[String],
    params: List[Param],
    platform: String,
    description: String
  )

  // Public entry point for sbt
  def run(catalog: File, output: File): Seq[File] =
    val text = Source.fromFile(catalog, "UTF-8").mkString
    val json = io.circe.parser.parse(text).fold(throw _, identity)
    val rendered = emitScala(json)
    IO.write(output, rendered, java.nio.charset.StandardCharsets.UTF_8)
    Seq(output)

  // Public for tests
  def emitScala(json: io.circe.Json): String =
    val entries = parseCatalog(json)
    val (parameterized, constants) = entries.partition(_.template.isDefined)
    val sortedConsts = constants.sortBy(_.key)
    val sortedParams = parameterized.sortBy(_.key)

    val header =
      """package com.varpas.sangeet.core.strings
        |
        |// GENERATED FILE — DO NOT EDIT MANUALLY.
        |// Source:    sangeet-core/src/main/resources/ui-strings.json
        |// Regenerate: sbt sangeetCore/genUiStrings   (or: make gen-strings)
        |//
        |// To add or change a string: edit ui-strings.json, then run `make gen-strings`,
        |// then use `UiStrings.<key>` on both desktop and web. See
        |// docs/developer/ui-strings-catalog.md for the full guide.
        |
        |object UiStrings:""".stripMargin

    val constLines = sortedConsts.map { e =>
      val ident = keyToScalaIdent(e.key)
      val escaped = escapeScala(e.value.get)
      s"""  val $ident: String = "$escaped""""
    }

    val funcLines = sortedParams.map { e =>
      val ident = keyToScalaIdent(e.key)
      val args = e.params.map(p => s"${p.name}: ${typeToScala(p.paramType)}").mkString(", ")
      val body = emitScalaTemplateBody(e.template.get, e.params)
      s"""  def $ident($args): String = $body"""
    }

    (Seq(header, "") ++ constLines ++ (if funcLines.nonEmpty then Seq("") else Nil) ++ funcLines).mkString("\n") + "\n"

  private def parseCatalog(json: io.circe.Json): List[Entry] =
    val entriesObj = json.hcursor.downField("entries").as[Map[String, io.circe.Json]].fold(throw _, identity)
    entriesObj.toList.map { case (key, body) =>
      val c = body.hcursor
      Entry(
        key = key,
        value = c.downField("value").as[String].toOption,
        template = c.downField("template").as[String].toOption,
        params = c.downField("params").as[List[io.circe.Json]].getOrElse(Nil).map { p =>
          Param(p.hcursor.downField("name").as[String].fold(throw _, identity),
                p.hcursor.downField("type").as[String].fold(throw _, identity))
        },
        platform = c.downField("platform").as[String].getOrElse("both"),
        description = c.downField("description").as[String].getOrElse("")
      )
    }

  private def keyToScalaIdent(key: String): String =
    val parts = key.split('.')
    parts.head + parts.tail.map(p => p.head.toUpper +: p.tail).map(_.toString).mkString

  private def typeToScala(t: String): String = t match
    case "int"    => "Int"
    case "string" => "String"
    case other    => throw new IllegalArgumentException(s"Unsupported param type: $other")

  private def escapeScala(s: String): String =
    s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

  private def emitScalaTemplateBody(template: String, params: List[Param]): String =
    // Replace {name} placeholders with $name interpolation, then wrap in s"..."
    var body = template
    params.foreach { p => body = body.replace(s"{${p.name}}", s"$$${p.name}") }
    s"""s"${escapeScala(body).replace("\\$", "$")}""""
```

- [ ] **Step 4: Wire the sbt task in `build.sbt`**

In the `sangeetCore` project's `.settings(...)`:

```scala
lazy val genUiStrings = taskKey[Seq[File]]("Generate UiStrings.scala from ui-strings.json")

// inside sangeetCore project:
.settings(
  genUiStrings := UiStringsCodegen.run(
    catalog = (Compile / resourceDirectory).value / "ui-strings.json",
    output  = baseDirectory.value / "src" / "main" / "scala" / "com" / "varpas" / "sangeet" / "core" / "strings" / "UiStrings.scala"
  ),
  // NOTE: not wired to sourceGenerators — output is checked-in source, not generated-on-compile.
)
```

- [ ] **Step 5: Run the task, verify it produces a file**

```bash
sbt sangeetCore/genUiStrings
ls -la sangeet-core/src/main/scala/com/varpas/sangeet/core/strings/UiStrings.scala
```

Expected: file exists, contains `object UiStrings:` and the header comment. With empty catalog, the body is just the header.

- [ ] **Step 6: Run codegen tests, verify pass**

```bash
sbt sangeetCore/testOnly *UiStringsCodegenSpec*
```

Expected: all 5 tests pass.

- [ ] **Step 7: Run full Scala test suite to confirm no regressions**

```bash
sbt test
```

Expected: all green.

- [ ] **Step 8: Commit**

```bash
git add project/UiStringsCodegen.scala build.sbt \
        sangeet-core/src/main/scala/com/varpas/sangeet/core/strings/UiStrings.scala \
        sangeet-core/src/test/scala/com/varpas/sangeet/core/strings/UiStringsCodegenSpec.scala
git commit -m "feat(strings): Scala codegen for UiStrings"
```

### Task 12.3: Elm codegen — `gen-elm-strings.ts` Node script

**Files:**

- Create: `scripts/package.json`
- Create: `scripts/tsconfig.json`
- Create: `scripts/lib/catalog.ts`
- Create: `scripts/gen-elm-strings.ts`
- Create: `scripts/__tests__/gen-elm-strings.test.ts`
- Modify: `.gitignore`

- [ ] **Step 1: Bootstrap the scripts workspace**

```bash
mkdir -p scripts/lib scripts/__tests__
```

Create `scripts/package.json`:

```json
{
  "name": "@sangeet/scripts",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "test": "vitest run",
    "gen": "tsx gen-elm-strings.ts",
    "parity": "tsx check-string-parity.ts",
    "find-untracked": "tsx find-untracked-strings.ts",
    "report": "tsx generate-strings-report.ts"
  },
  "devDependencies": {
    "tsx": "^4.7.0",
    "vitest": "^1.2.0",
    "glob": "^10.3.0",
    "@types/node": "^20.10.0",
    "typescript": "^5.3.0"
  }
}
```

Create `scripts/tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "allowImportingTsExtensions": true,
    "noEmit": true
  }
}
```

Update `.gitignore`:

```
scripts/node_modules/
scripts/coverage/
```

- [ ] **Step 2: Install deps**

```bash
cd scripts && npm install
```

- [ ] **Step 3: Write `scripts/lib/catalog.ts`** (shared catalog reader)

```ts
import { readFileSync } from 'node:fs';

export type Param = { name: string; type: 'int' | 'string' };
export type Platform = 'both' | 'desktop' | 'web';

export type Entry = {
  value?: string;
  template?: string;
  params?: Param[];
  platform: Platform;
  description: string;
};

export type Catalog = {
  $comment?: string;
  entries: Record<string, Entry>;
};

export function loadCatalog(path: string): Catalog {
  const raw = JSON.parse(readFileSync(path, 'utf-8')) as Partial<Catalog>;
  if (!raw.entries || typeof raw.entries !== 'object') {
    throw new Error(`Invalid catalog at ${path}: missing 'entries' object`);
  }
  // normalize: default platform=both
  for (const [k, v] of Object.entries(raw.entries)) {
    v.platform ??= 'both';
    v.description ??= '';
    if (v.template && !v.params) v.params = [];
  }
  return raw as Catalog;
}

export function keyToElmIdent(key: string): string {
  const parts = key.split('.');
  return (
    parts[0] +
    parts
      .slice(1)
      .map((p) => p[0].toUpperCase() + p.slice(1))
      .join('')
  );
}

export function typeToElm(t: Param['type']): string {
  return t === 'int' ? 'Int' : 'String';
}
```

- [ ] **Step 4: Write failing test** (`scripts/__tests__/gen-elm-strings.test.ts`)

```ts
import { describe, it, expect } from 'vitest';
import { emitElm } from '../gen-elm-strings.ts';

describe('emitElm', () => {
  it('emits header for empty catalog', () => {
    const out = emitElm({ entries: {} });
    expect(out).toMatch(/^module UiStrings exposing \(\.\.\)/);
    expect(out).toContain('GENERATED FILE');
  });

  it("emits constant for 'value' entry", () => {
    const out = emitElm({
      entries: { 'toolbar.file.new': { value: 'New', platform: 'both', description: '' } },
    });
    expect(out).toMatch(/toolbarFileNew : String\ntoolbarFileNew =\n    "New"/);
  });

  it('emits typed function for parameterized entry', () => {
    const out = emitElm({
      entries: {
        'toolbar.beatCount': {
          template: 'Beats: {current} / {total}',
          params: [
            { name: 'current', type: 'int' },
            { name: 'total', type: 'int' },
          ],
          platform: 'both',
          description: '',
        },
      },
    });
    expect(out).toMatch(/toolbarBeatCount : Int -> Int -> String/);
    expect(out).toMatch(/toolbarBeatCount current total =/);
    expect(out).toMatch(
      /"Beats: " \+\+ String\.fromInt current \+\+ " \/ " \+\+ String\.fromInt total/,
    );
  });

  it('escapes double quotes and backslashes', () => {
    const out = emitElm({
      entries: { k: { value: '"quoted" \\ back', platform: 'both', description: '' } },
    });
    expect(out).toContain('"\\"quoted\\" \\\\ back"');
  });

  it('sorts entries deterministically', () => {
    const out = emitElm({
      entries: {
        z: { value: 'Z', platform: 'both', description: '' },
        a: { value: 'A', platform: 'both', description: '' },
      },
    });
    expect(out.indexOf('a :')).toBeLessThan(out.indexOf('z :'));
  });
});
```

Run, verify fails:

```bash
cd scripts && npm test
```

- [ ] **Step 5: Implement `scripts/gen-elm-strings.ts`**

```ts
import { writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadCatalog, keyToElmIdent, typeToElm, Catalog, Entry, Param } from './lib/catalog.ts';

export function emitElm(catalog: Catalog): string {
  const header = `module UiStrings exposing (..)

-- GENERATED FILE — DO NOT EDIT MANUALLY.
-- Source:    sangeet-core/src/main/resources/ui-strings.json
-- Regenerate: cd scripts && npm run gen   (or: make gen-strings)
--
-- To add or change a string: edit ui-strings.json, then run \`make gen-strings\`,
-- then use \`UiStrings.<key>\` on both desktop and web. See
-- docs/developer/ui-strings-catalog.md for the full guide.

`;

  const sorted = Object.entries(catalog.entries).sort(([a], [b]) => a.localeCompare(b));
  const constants = sorted.filter(([, e]) => !e.template);
  const functions = sorted.filter(([, e]) => e.template);

  const constLines = constants.map(([key, e]) => emitConstant(key, e));
  const funcLines = functions.map(([key, e]) => emitFunction(key, e));

  return (
    header +
    constLines.join('\n\n') +
    (constLines.length && funcLines.length ? '\n\n' : '') +
    funcLines.join('\n\n') +
    (sorted.length ? '\n' : '')
  );
}

function emitConstant(key: string, e: Entry): string {
  const ident = keyToElmIdent(key);
  const escaped = escapeElm(e.value!);
  return `${ident} : String\n${ident} =\n    "${escaped}"`;
}

function emitFunction(key: string, e: Entry): string {
  const ident = keyToElmIdent(key);
  const params = e.params!;
  const argTypes = [...params.map((p) => typeToElm(p.type)), 'String'].join(' -> ');
  const argNames = params.map((p) => p.name).join(' ');
  const body = emitElmTemplateBody(e.template!, params);
  return `${ident} : ${argTypes}\n${ident} ${argNames} =\n    ${body}`;
}

function escapeElm(s: string): string {
  return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
}

function emitElmTemplateBody(template: string, params: Param[]): string {
  // Split template by placeholders, intersperse with String concatenation.
  // "Beats: {current} / {total}" -> "Beats: " ++ String.fromInt current ++ " / " ++ String.fromInt total
  let remaining = template;
  const pieces: string[] = [];
  while (remaining.length) {
    const m = remaining.match(/\{([a-zA-Z_][a-zA-Z0-9_]*)\}/);
    if (!m) {
      if (remaining) pieces.push(`"${escapeElm(remaining)}"`);
      break;
    }
    const before = remaining.slice(0, m.index);
    if (before) pieces.push(`"${escapeElm(before)}"`);
    const p = params.find((x) => x.name === m[1]);
    if (!p) throw new Error(`Template references unknown param {${m[1]}}`);
    pieces.push(p.type === 'int' ? `String.fromInt ${p.name}` : p.name);
    remaining = remaining.slice(m.index! + m[0].length);
  }
  return pieces.join(' ++ ');
}

// CLI entry point
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const catalogPath = resolve(process.cwd(), 'sangeet-core/src/main/resources/ui-strings.json');
  const outputPath = resolve(process.cwd(), 'sangeet-web/src/UiStrings.elm');
  const catalog = loadCatalog(catalogPath);
  writeFileSync(outputPath, emitElm(catalog), 'utf-8');
  console.log(`Wrote ${outputPath} (${Object.keys(catalog.entries).length} entries)`);
}
```

- [ ] **Step 6: Run tests, verify pass**

```bash
cd scripts && npm test
```

- [ ] **Step 7: Run the codegen, verify Elm file produced**

```bash
cd /Users/bharadwaj/Work/Code/mine/sangeet_notes_editor
cd scripts && npm run gen
cat ../sangeet-web/src/UiStrings.elm
```

Expected: file exists with the module header. With empty catalog, body is empty.

- [ ] **Step 8: Verify Elm still compiles (UiStrings.elm parses; nothing references it yet)**

```bash
cd sangeet-web && elm make src/Main.elm --output=/dev/null
```

Expected: success.

- [ ] **Step 9: Commit**

```bash
git add scripts/ sangeet-web/src/UiStrings.elm .gitignore
git commit -m "feat(strings): Elm codegen for UiStrings"
```

### Task 12.4: Makefile + lefthook integration

**Files:**

- Modify: `Makefile`
- Modify: `.lefthook.yml`

- [ ] **Step 1: Add Makefile targets**

```make
.PHONY: gen-strings check-strings find-untracked-strings strings-report

gen-strings: ## Regenerate UiStrings.scala and UiStrings.elm from ui-strings.json
	sbt sangeetCore/genUiStrings
	cd scripts && npm install --silent && npm run gen

check-strings: ## Run cross-platform UI strings parity check
	cd scripts && npm install --silent && npm run parity

find-untracked-strings: ## Heuristic sweep for English-looking literals not in the catalog
	cd scripts && npm install --silent && npm run find-untracked

strings-report: ## Generate docs/strings-parity-report.md
	cd scripts && npm install --silent && npm run report
```

Then update the existing `format` target so it includes `gen-strings`:

```make
format: gen-strings
	sbt scalafmtAll
	elm-format sangeet-web/src --yes
	prettier --write "e2e/**/*.{ts,js,json,css}" "sangeet-web/public/*.{js,css,html}"
```

- [ ] **Step 2: Add lefthook hook (catalog-gated)**

In `.lefthook.yml`, add to the `pre-commit` group:

```yaml
pre-commit:
  commands:
    gen-strings-if-catalog-changed:
      glob: 'sangeet-core/src/main/resources/ui-strings.json'
      run: |
        make gen-strings
        git add sangeet-core/src/main/scala/com/varpas/sangeet/core/strings/UiStrings.scala
        git add sangeet-web/src/UiStrings.elm
```

- [ ] **Step 3: Test the hook by changing the catalog**

```bash
# Add a dummy entry just to trigger the hook
python3 -c "import json; c=json.load(open('sangeet-core/src/main/resources/ui-strings.json')); c['entries']['test.dummy']={'value':'Dummy','platform':'both','description':'temp'}; json.dump(c, open('sangeet-core/src/main/resources/ui-strings.json','w'), indent=2)"
git add sangeet-core/src/main/resources/ui-strings.json
git commit -m "test: trigger gen-strings hook"
# Hook should regenerate Scala+Elm files and stage them. Verify commit includes all 3 files.
git show --stat HEAD
# Revert
git reset --hard HEAD~1
make gen-strings  # restore generated files to empty state
```

- [ ] **Step 4: Commit Makefile + lefthook changes**

```bash
git add Makefile .lefthook.yml
git commit -m "feat(strings): Makefile + lefthook integration for gen-strings"
```

### Task 12.5: Phase 12 verification

- [ ] **Step 1: Run all test suites**

```bash
sbt test                                # Scala (including codegen tests)
cd scripts && npm test                  # TS codegen tests
cd sangeet-web && npm test              # Elm tests
sbt sangeetDesktop/test                 # Desktop tests
```

Expected: all green.

- [ ] **Step 2: Verify generated files committed**

```bash
git ls-files sangeet-core/src/main/scala/com/varpas/sangeet/core/strings/UiStrings.scala
git ls-files sangeet-web/src/UiStrings.elm
git ls-files sangeet-core/src/main/resources/ui-strings.json
```

Expected: all three paths listed.

- [ ] **Step 3: Verify codegen is deterministic**

```bash
make gen-strings
git diff --exit-code -- sangeet-core/src/main/scala/com/varpas/sangeet/core/strings/UiStrings.scala sangeet-web/src/UiStrings.elm
```

Expected: clean (no diff).

---

## Phase 13 — Parity check + CI integration

The check script catches three failure modes: `both` entries missing from one side, `desktop`/`web` entries leaking to the other side, and source-code references to keys not in the catalog.

### Task 13.1: Shared source scanner

**Files:**

- Create: `scripts/lib/source-scanner.ts`
- Create: `scripts/__tests__/source-scanner.test.ts`

- [ ] **Step 1: Write failing test**

```ts
import { describe, it, expect } from 'vitest';
import { extractScalaRefs, extractElmRefs } from '../lib/source-scanner.ts';

describe('source-scanner', () => {
  it('extracts UiStrings.foo refs from Scala', () => {
    const src = `
      val b = Button(UiStrings.toolbarFileNew) { ... }
      label.text = UiStrings.dialogAboutTitle
      val msg = UiStrings.toolbarBeatCount(3, 16)
    `;
    expect(extractScalaRefs(src)).toEqual(
      new Set(['toolbarFileNew', 'dialogAboutTitle', 'toolbarBeatCount']),
    );
  });

  it('extracts UiStrings.foo refs from Elm', () => {
    const src = `
      button [ onClick (...) ] [ text UiStrings.toolbarFileNew ]
      span [] [ text (UiStrings.toolbarBeatCount 3 16) ]
    `;
    expect(extractElmRefs(src)).toEqual(new Set(['toolbarFileNew', 'toolbarBeatCount']));
  });

  it('ignores UiStrings inside string literals (false positive guard)', () => {
    expect(extractScalaRefs('val s = "UiStrings.notAnIdent"')).toEqual(new Set());
  });
});
```

- [ ] **Step 2: Implement scanner**

```ts
import { readFileSync } from 'node:fs';
import { globSync } from 'glob';

const REF_PATTERN = /\bUiStrings\.([a-zA-Z][a-zA-Z0-9]*)/g;

export function extractScalaRefs(source: string): Set<string> {
  return extract(stripStringLiterals(source));
}

export function extractElmRefs(source: string): Set<string> {
  return extract(stripStringLiterals(source));
}

function extract(source: string): Set<string> {
  const refs = new Set<string>();
  let m: RegExpExecArray | null;
  while ((m = REF_PATTERN.exec(source)) !== null) refs.add(m[1]);
  return refs;
}

// Lightweight string-literal stripper. Not a full parser, but handles the
// common cases of "..." and triple-quoted strings.
function stripStringLiterals(source: string): string {
  return source.replace(/"""[\s\S]*?"""/g, '""').replace(/"(?:[^"\\]|\\.)*"/g, '""');
}

export function scanScalaTree(roots: string[]): Set<string> {
  const all = new Set<string>();
  for (const root of roots) {
    for (const file of globSync(`${root}/**/*.scala`)) {
      const src = readFileSync(file, 'utf-8');
      for (const r of extractScalaRefs(src)) all.add(r);
    }
  }
  return all;
}

export function scanElmTree(roots: string[]): Set<string> {
  const all = new Set<string>();
  for (const root of roots) {
    for (const file of globSync(`${root}/**/*.elm`)) {
      const src = readFileSync(file, 'utf-8');
      for (const r of extractElmRefs(src)) all.add(r);
    }
  }
  return all;
}
```

- [ ] **Step 3: Run tests, verify pass**

```bash
cd scripts && npm test
```

- [ ] **Step 4: Commit**

```bash
git add scripts/lib/source-scanner.ts scripts/__tests__/source-scanner.test.ts
git commit -m "feat(strings): source scanner for UiStrings references"
```

### Task 13.2: Parity-check script

**Files:**

- Create: `scripts/check-string-parity.ts`
- Create: `scripts/__tests__/check-string-parity.test.ts`

- [ ] **Step 1: Write failing tests covering every failure mode**

```ts
import { describe, it, expect } from 'vitest';
import { checkParity } from '../check-string-parity.ts';
import { keyToElmIdent } from '../lib/catalog.ts';

const both = (k: string) => ({ value: 'x', platform: 'both' as const, description: '' });
const desk = (k: string) => ({ value: 'x', platform: 'desktop' as const, description: '' });
const web = (k: string) => ({ value: 'x', platform: 'web' as const, description: '' });

describe('checkParity', () => {
  it("passes when 'both' entry used on both sides", () => {
    const r = checkParity(
      { entries: { 'toolbar.new': both('toolbar.new') } },
      new Set([keyToElmIdent('toolbar.new')]),
      new Set([keyToElmIdent('toolbar.new')]),
    );
    expect(r.failures).toEqual([]);
  });

  it("fails when 'both' entry missing from desktop", () => {
    const r = checkParity(
      { entries: { 'toolbar.new': both('toolbar.new') } },
      new Set(),
      new Set([keyToElmIdent('toolbar.new')]),
    );
    expect(r.failures.some((f) => f.kind === 'missing_on_desktop')).toBe(true);
  });

  it("fails when 'desktop' entry leaks into web", () => {
    const r = checkParity(
      { entries: { 'menu.exit': desk('menu.exit') } },
      new Set([keyToElmIdent('menu.exit')]),
      new Set([keyToElmIdent('menu.exit')]),
    );
    expect(r.failures.some((f) => f.kind === 'leaked_to_web')).toBe(true);
  });

  it('fails when source references unknown key', () => {
    const r = checkParity({ entries: {} }, new Set(['unknownKey']), new Set());
    expect(r.failures.some((f) => f.kind === 'unknown_reference')).toBe(true);
  });
});
```

- [ ] **Step 2: Implement `scripts/check-string-parity.ts`**

```ts
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadCatalog, keyToElmIdent, Catalog } from './lib/catalog.ts';
import { scanScalaTree, scanElmTree } from './lib/source-scanner.ts';

export type Failure =
  | { kind: 'missing_on_desktop'; key: string }
  | { kind: 'missing_on_web'; key: string }
  | { kind: 'leaked_to_desktop'; key: string }
  | { kind: 'leaked_to_web'; key: string }
  | { kind: 'unknown_reference'; ident: string; platform: 'desktop' | 'web' };

export type ParityResult = {
  failures: Failure[];
  formatReport(): string;
};

export function checkParity(
  catalog: Catalog,
  desktopRefs: Set<string>,
  webRefs: Set<string>,
): ParityResult {
  const failures: Failure[] = [];
  const knownIdents = new Set<string>();

  for (const [key, entry] of Object.entries(catalog.entries)) {
    const ident = keyToElmIdent(key);
    knownIdents.add(ident);

    const onDesktop = desktopRefs.has(ident);
    const onWeb = webRefs.has(ident);

    switch (entry.platform) {
      case 'both':
        if (!onDesktop) failures.push({ kind: 'missing_on_desktop', key });
        if (!onWeb) failures.push({ kind: 'missing_on_web', key });
        break;
      case 'desktop':
        if (!onDesktop) failures.push({ kind: 'missing_on_desktop', key });
        if (onWeb) failures.push({ kind: 'leaked_to_web', key });
        break;
      case 'web':
        if (!onWeb) failures.push({ kind: 'missing_on_web', key });
        if (onDesktop) failures.push({ kind: 'leaked_to_desktop', key });
        break;
    }
  }

  for (const ident of desktopRefs) {
    if (!knownIdents.has(ident))
      failures.push({ kind: 'unknown_reference', ident, platform: 'desktop' });
  }
  for (const ident of webRefs) {
    if (!knownIdents.has(ident))
      failures.push({ kind: 'unknown_reference', ident, platform: 'web' });
  }

  return {
    failures,
    formatReport() {
      if (!failures.length) return 'Strings parity OK.';
      return [
        `❌ ${failures.length} strings parity failure(s):`,
        ...failures.map((f) => {
          switch (f.kind) {
            case 'missing_on_desktop':
              return `  - MISSING on desktop:  ${f.key}`;
            case 'missing_on_web':
              return `  - MISSING on web:      ${f.key}`;
            case 'leaked_to_desktop':
              return `  - LEAKED to desktop:   ${f.key} (declared platform:web)`;
            case 'leaked_to_web':
              return `  - LEAKED to web:       ${f.key} (declared platform:desktop)`;
            case 'unknown_reference':
              return `  - UNKNOWN reference:   UiStrings.${f.ident} on ${f.platform} (not in catalog)`;
          }
        }),
      ].join('\n');
    },
  };
}

// CLI entry point
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const root = process.cwd();
  const catalog = loadCatalog(resolve(root, 'sangeet-core/src/main/resources/ui-strings.json'));
  const desktopRefs = scanScalaTree([
    resolve(root, 'sangeet-desktop/src/main'),
    resolve(root, 'sangeet-server/src/main'),
    resolve(root, 'sangeet-core/src/main'),
  ]);
  const webRefs = scanElmTree([
    resolve(root, 'sangeet-web/src'),
    resolve(root, 'sangeet-web/tests'),
  ]);
  const result = checkParity(catalog, desktopRefs, webRefs);
  console.log(result.formatReport());
  if (result.failures.length > 0) process.exit(1);
}
```

- [ ] **Step 3: Run tests, verify pass**

```bash
cd scripts && npm test
```

- [ ] **Step 4: Run parity check on the current (empty) catalog**

```bash
make check-strings
```

Expected: "Strings parity OK." (vacuous — empty catalog, no references).

- [ ] **Step 5: Force a failure to verify error reporting works**

```bash
# Temporarily add a 'both' entry with no usage
python3 -c "import json; c=json.load(open('sangeet-core/src/main/resources/ui-strings.json')); c['entries']['test.unused']={'value':'X','platform':'both','description':'temp'}; json.dump(c, open('sangeet-core/src/main/resources/ui-strings.json','w'), indent=2)"
make gen-strings
make check-strings   # expect FAIL with "MISSING on desktop" + "MISSING on web"
# Revert
git checkout sangeet-core/src/main/resources/ui-strings.json
make gen-strings
```

- [ ] **Step 6: Commit**

```bash
git add scripts/check-string-parity.ts scripts/__tests__/check-string-parity.test.ts
git commit -m "feat(strings): parity-check script with 5 failure modes"
```

### Task 13.3: CI integration

**Files:**

- Modify: `.github/workflows/ci.yml`

- [ ] **Step 1: Add two new jobs**

```yaml
string-parity:
  name: UI strings parity check
  runs-on: ubuntu-latest
  needs: [] # independent of other jobs
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v4
      with:
        node-version: 20
    - name: Install scripts deps
      working-directory: scripts
      run: npm ci || npm install
    - name: Parity check
      run: make check-strings

strings-gen-sync:
  name: UiStrings generated files in sync
  runs-on: ubuntu-latest
  needs: []
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v4
      with: { node-version: 20 }
    - uses: actions/setup-java@v4
      with: { distribution: temurin, java-version: 17 }
    - uses: sbt/setup-sbt@v1
    - name: Regenerate
      run: make gen-strings
    - name: Assert no diff
      run: |
        git diff --exit-code -- \
          sangeet-core/src/main/scala/com/varpas/sangeet/core/strings/UiStrings.scala \
          sangeet-web/src/UiStrings.elm
```

If `e2e-tests` has a `needs:` list that gates on `lint` + `scala-tests` + `elm-tests`, add `string-parity` to that list (parity is a required check).

- [ ] **Step 2: Push branch, watch CI**

```bash
git add .github/workflows/ci.yml
git commit -m "ci(strings): add string-parity + strings-gen-sync jobs"
git push -u origin workstream-b-strings-catalog
gh run watch
```

Expected: both new jobs green.

### Task 13.4: Phase 13 verification

- [ ] **Step 1: All CI jobs green** including the two new ones.
- [ ] **Step 2: Local `make check-strings` passes.**
- [ ] **Step 3: Force-fail test still works locally** (Task 13.2 Step 5 still produces a clean FAIL when run manually).

---

## Phase 14 — Migration wave 1: toolbar + core dialogs

First migration batch. Establishes the pattern for Phase 15. Each task migrates ONE area on BOTH stacks in lockstep, then runs parity check to confirm.

> **Pattern for every migration task:**
>
> 1. Add catalog entries.
> 2. `make gen-strings`.
> 3. Replace literals in the Elm view file (add `import UiStrings`).
> 4. Replace literals in the matching Scala desktop file (`import com.varpas.sangeet.core.strings.UiStrings`).
> 5. `make check-strings` — must pass.
> 6. `sbt sangeetDesktop/test && cd sangeet-web && npm test` — must pass.
> 7. `cd e2e && npm run test` (subset for the migrated area) — UI text unchanged.
> 8. Commit.

### Task 14.1: Migrate toolbar (~50 strings)

**Files:**

- Modify: `sangeet-core/src/main/resources/ui-strings.json`
- Modify: `sangeet-web/src/View/Toolbar.elm`
- Modify: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/ToolbarBuilder.scala`

- [ ] **Step 1: Add catalog entries**

Add to `ui-strings.json` under `entries` (sample shown — actual additions cover every button label + tooltip in `Toolbar.elm`):

```json
{
  "toolbar.file.new": {
    "value": "New",
    "platform": "both",
    "description": "Toolbar button: create new composition"
  },
  "toolbar.file.new.tooltip": {
    "value": "New Composition (Ctrl+N)",
    "platform": "both",
    "description": "Tooltip for New button"
  },
  "toolbar.file.open": { "value": "Open", "platform": "both", "description": "..." },
  "toolbar.file.open.tooltip": { "value": "Open File", "platform": "both", "description": "..." },
  "toolbar.file.save": { "value": "Save", "platform": "both", "description": "..." },
  "toolbar.file.save.tooltip": {
    "value": "Save File (Ctrl+S)",
    "platform": "both",
    "description": "..."
  },
  "toolbar.edit.undo": { "value": "Undo", "platform": "both", "description": "..." },
  "toolbar.edit.undo.tooltip": {
    "value": "Undo (Ctrl+Z)",
    "platform": "both",
    "description": "..."
  },
  "toolbar.mode.swar": { "value": "Mode: Swar", "platform": "both", "description": "..." },
  "toolbar.mode.stroke": { "value": "Mode: Stroke", "platform": "both", "description": "..." },
  "toolbar.ornament.singleNote": {
    "template": "Orn: {name} (type note)",
    "params": [{ "name": "name", "type": "string" }],
    "platform": "both",
    "description": "Ornament-mode badge for single-note ornaments (kan, gamak, andolan)"
  },
  "toolbar.ornament.murki": {
    "template": "Murki: {count} notes (Enter to apply)",
    "params": [{ "name": "count", "type": "int" }],
    "platform": "both",
    "description": "..."
  }
  // ... ~40 more toolbar entries
}
```

- [ ] **Step 2: Regenerate**

```bash
make gen-strings
```

- [ ] **Step 3: Migrate `View/Toolbar.elm` references**

```diff
+ import UiStrings
  ...
- button [ class "toolbar-btn", title "New Composition (Ctrl+N)", onClick ShowNewDialog ]
-     [ text "New" ]
+ button [ class "toolbar-btn", title UiStrings.toolbarFileNewTooltip, onClick ShowNewDialog ]
+     [ text UiStrings.toolbarFileNew ]
  ...
- span [ class "toolbar-badge ornament-badge" ]
-     [ text ("Orn: " ++ name ++ " (type note)") ]
+ span [ class "toolbar-badge ornament-badge" ]
+     [ text (UiStrings.toolbarOrnamentSingleNote name) ]
  ...
```

Walk every literal in `Toolbar.elm`, including tooltips, mode labels, badge text, and ornament strings.

- [ ] **Step 4: Mirror on `ToolbarBuilder.scala` (desktop)**

```diff
+ import com.varpas.sangeet.core.strings.UiStrings
  ...
- val newBtn = Button("New") { onNewComposition() }
- newBtn.tooltip = Tooltip("New Composition (Ctrl+N)")
+ val newBtn = Button(UiStrings.toolbarFileNew) { onNewComposition() }
+ newBtn.tooltip = Tooltip(UiStrings.toolbarFileNewTooltip)
```

(Repeat for every literal.)

- [ ] **Step 5: Verify**

```bash
make check-strings              # MUST pass — every new entry used on both sides
sbt sangeetDesktop/test
cd sangeet-web && npm test
cd ../e2e && npx playwright test tests/toolbar.spec.ts
```

- [ ] **Step 6: Commit**

```bash
git add sangeet-core/src/main/resources/ui-strings.json \
        sangeet-core/src/main/scala/com/varpas/sangeet/core/strings/UiStrings.scala \
        sangeet-web/src/UiStrings.elm \
        sangeet-web/src/View/Toolbar.elm \
        sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/ToolbarBuilder.scala
git commit -m "refactor(strings): migrate toolbar literals to UiStrings catalog"
```

### Task 14.2: Migrate About dialog

**Files:**

- Modify: `sangeet-core/src/main/resources/ui-strings.json`
- Modify: `sangeet-web/src/View/Dialogs/About.elm`
- Modify: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/AboutDialog.scala`

Follow the same pattern. Catalog keys go under `dialog.about.*` (title, body paragraphs, link labels, privacy text, license footer). Commit.

### Task 14.3: Migrate Support dialog

Same pattern, `dialog.support.*` keys. Commit.

### Task 14.4: Migrate NewComposition dialog

Same pattern, `dialog.newComposition.*` keys (form labels, raag/taal placeholders, button labels). Commit.

### Task 14.5: Migrate Properties dialog

Same pattern, `dialog.properties.*` keys. Commit.

### Task 14.6: Phase 14 verification

- [ ] **Step 1: Full parity check passes**

```bash
make check-strings
```

- [ ] **Step 2: Full test suites pass**

```bash
sbt test
cd sangeet-web && npm test
cd ../e2e && npm run test
```

- [ ] **Step 3: Manual smoke test in both stacks**

```bash
# Desktop
sbt sangeetDesktop/run
# Open every migrated dialog. Visually confirm labels match pre-migration.

# Web
cd sangeet-server && sbt sangeetServer/run &
cd sangeet-web && npm start
# Same visual check in browser.
```

---

## Phase 15 — Migration wave 2: remaining UI

Second migration batch. Same task pattern as Phase 14 but for the remaining surface area.

### Task 15.1: Migrate BugReport dialog

### Task 15.2: Migrate KeyboardCheatSheet dialog

### Task 15.3: Migrate CommandPalette dialog

### Task 15.4: Migrate StatusBar (web + desktop)

### Task 15.5: Migrate Header (cursor position labels, mode indicators)

### Task 15.6: Migrate FileBrowser (bookmark labels, breadcrumb, file actions)

### Task 15.7: Migrate KeyboardLegend

### Task 15.8: Migrate AppAction (command palette action labels + descriptions)

### Task 15.9: Migrate State/Update.elm error messages + status-log strings

Each task follows the Phase 14 pattern: catalog entries → `make gen-strings` → migrate Elm → migrate Scala → parity check → tests → commit.

### Task 15.10: Migrate MainApp.scala (desktop window title, system menu)

Note: window title is `platform: both` (web sets `document.title` via port too — see existing `Ports.elm`). System menu items (Quit etc.) are `platform: desktop`.

### Task 15.11: Phase 15 verification

Same as Task 14.6, but now against the full migrated surface.

---

## Phase 16 — Catalog dump + uncategorized literals sweep

After Phases 14–15, the catalog covers the deliberate UI surface. This phase catches anything missed via heuristic scanning.

### Task 16.1: Implement `find-untracked-strings.ts`

**Files:**

- Create: `scripts/find-untracked-strings.ts`

- [ ] **Step 1: Implement the heuristic scanner**

```ts
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { globSync } from 'glob';

// Heuristics for "looks like user-facing English text":
// - 3+ characters, contains a space OR is TitleCase OR ends with punctuation
// - Excludes CSS class names (contains "-"), file paths (contains "/"), HTTP
//   URLs, log tags ("[debug]"), JSON keys (preceded by `:` or `"`), etc.
const LIKELY_UI =
  /^(?=.{3,})(?:[A-Z][a-z]+(?:\s+\S+){1,}|[A-Z][a-zA-Z]{2,}(?:\s+\S+)+|[A-Za-z][^"]{3,}[.!?])$/;

const SCALA_STRING = /"((?:[^"\\]|\\.)*)"/g;
const ELM_STRING = /"((?:[^"\\]|\\.)*)"/g;

type Hit = { file: string; line: number; literal: string };

function scan(globPattern: string, regex: RegExp): Hit[] {
  const hits: Hit[] = [];
  for (const file of globSync(globPattern)) {
    const lines = readFileSync(file, 'utf-8').split('\n');
    lines.forEach((ln, i) => {
      let m: RegExpExecArray | null;
      const localRegex = new RegExp(regex.source, 'g');
      while ((m = localRegex.exec(ln)) !== null) {
        const literal = m[1];
        if (
          LIKELY_UI.test(literal) &&
          !ln.includes('class=') &&
          !ln.includes('href=') &&
          !ln.includes('logger.')
        ) {
          hits.push({ file, line: i + 1, literal });
        }
      }
    });
  }
  return hits;
}

const root = process.cwd();
const desktopHits = scan(
  resolve(
    root,
    'sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/{editor,dialog}/**/*.scala',
  ),
  SCALA_STRING,
);
const webHits = scan(resolve(root, 'sangeet-web/src/{View,State}/**/*.elm'), ELM_STRING);

console.log('# Untracked string candidates');
console.log('');
console.log('## Desktop');
desktopHits.forEach((h) => console.log(`- ${h.file}:${h.line} — "${h.literal}"`));
console.log('');
console.log('## Web');
webHits.forEach((h) => console.log(`- ${h.file}:${h.line} — "${h.literal}"`));
```

- [ ] **Step 2: Run + capture output**

```bash
make find-untracked-strings > /tmp/untracked.txt
wc -l /tmp/untracked.txt
```

- [ ] **Step 3: Triage**

For each hit:

- **Legitimate UI literal** → add catalog entry, migrate call site.
- **CSS class / log tag / internal label** → leave alone.
- **Edge case** → note in commit message.

Repeat the migration pattern (Task 14.1 steps 1–6) for each new entry.

- [ ] **Step 4: Commit (likely multiple commits, one per area)**

### Task 16.2: Phase 16 verification

- [ ] **Step 1: Re-run find-untracked-strings**

```bash
make find-untracked-strings > /tmp/untracked-after.txt
```

Manually confirm every remaining hit is justifiably not in the catalog (CSS class, log tag, etc.).

- [ ] **Step 2: Parity check + tests still pass**

```bash
make check-strings
sbt test && cd sangeet-web && npm test && cd ../e2e && npm run test
```

---

## Phase 17 — Review milestone: strings-parity-report

The forcing function. Generate a report covering every `desktop` / `web` entry, present to the user, dispose each entry-by-entry.

### Task 17.1: Implement `generate-strings-report.ts`

**Files:**

- Create: `scripts/generate-strings-report.ts`

- [ ] **Step 1: Implement the report generator**

```ts
import { writeFileSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
import { loadCatalog } from "./lib/catalog.ts";
import { scanScalaTree, scanElmTree } from "./lib/source-scanner.ts";

const root = process.cwd();
const catalog = loadCatalog(resolve(root, "sangeet-core/src/main/resources/ui-strings.json"));
const desktopRefs = scanScalaTree([
  resolve(root, "sangeet-desktop/src/main"),
  resolve(root, "sangeet-server/src/main"),
  resolve(root, "sangeet-core/src/main"),
]);
const webRefs = scanElmTree([resolve(root, "sangeet-web/src"), resolve(root, "sangeet-web/tests")]);

const both = Object.entries(catalog.entries).filter(([, e]) => e.platform === "both");
const desktopOnly = Object.entries(catalog.entries).filter(([, e]) => e.platform === "desktop");
const webOnly = Object.entries(catalog.entries).filter(([, e]) => e.platform === "web");

const now = new Date().toISOString().slice(0, 10);

const out = `# UI Strings Parity Report

> Generated: ${now}. Regenerate with \`make strings-report\`.

## Summary

| Bucket                         | Count                 |
| ------------------------------ | --------------------- |
| Shared (\`platform: both\`)    | ${both.length}        |
| Desktop-only                   | ${desktopOnly.length} |
| Web-only                       | ${webOnly.length}     |
| Total                          | ${Object.keys(catalog.entries).length} |

**Goal:** Minimize the Desktop-only and Web-only buckets toward zero.
Each entry below requires a disposition:
**PORT** (add equivalent to the other side) /
**REMOVE** (delete from the side that has it) /
**ACCEPT** (keep as justified platform-specific).

## Desktop-only entries (review one-by-one)

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
${desktopOnly.map(([k, e]) => `| \`${k}\` | \`${(e.value ?? e.template ?? "").replace(/\|/g, "\\|")}\` | ${e.description} | TODO |`).join("\n")}

## Web-only entries (review one-by-one)

| Key | Value / Template | Description | Disposition |
| --- | ---------------- | ----------- | ----------- |
${webOnly.map(([k, e]) => `| \`${k}\` | \`${(e.value ?? e.template ?? "").replace(/\|/g, "\\|")}\` | ${e.description} | TODO |`).join("\n")}

## Shared entries summary

${both.length} shared entries — full list omitted; query the catalog directly:
\`\`\`bash
jq '.entries | to_entries[] | select(.value.platform=="both") | .key' \\
  sangeet-core/src/main/resources/ui-strings.json
\`\`\`
`;

writeFileSync(resolve(root, "docs/strings-parity-report.md"), out, "utf-8");
console.log("Wrote docs/strings-parity-report.md");
`;
```

- [ ] **Step 2: Generate report**

```bash
make strings-report
cat docs/strings-parity-report.md
```

- [ ] **Step 3: Commit script + initial report**

```bash
git add scripts/generate-strings-report.ts docs/strings-parity-report.md
git commit -m "feat(strings): parity report generator + initial report"
```

### Task 17.2: Review checkpoint — STOP and present to user

**Files:** none (this is a process gate).

- [ ] **Step 1: Post the report URL + summary to the user**

> Phase 17 milestone reached. Report at `docs/strings-parity-report.md` lists
> X shared, Y desktop-only, Z web-only entries.
>
> Please walk through the Desktop-only and Web-only sections entry-by-entry.
> For each, tell me one of:
>
> - **PORT** — add equivalent UI to the missing side. I'll then implement.
> - **REMOVE** — delete the UI from the side that has it. I'll then implement.
> - **ACCEPT** — leave as platform-specific. Provide a justification I'll
>   write into the entry's `description` field.

- [ ] **Step 2: Wait for user dispositions**

Do not proceed to Task 17.3 until every entry in the Desktop-only and Web-only lists has been disposed of.

### Task 17.3: Apply dispositions

For each user-supplied disposition:

- **PORT** → add the missing UI element to the other side, flip the catalog entry to `platform: both`, run `make gen-strings && make check-strings`. Commit (one commit per port, or batch by area).
- **REMOVE** → delete the UI element from the side that has it, remove the catalog entry. Commit.
- **ACCEPT** → update the catalog entry's `description` with the justification. Commit.

After every batch, re-run:

```bash
make gen-strings
make check-strings
sbt test && cd sangeet-web && npm test && cd ../e2e && npm run test
make strings-report
```

The updated report should show the bucket counts shrinking after each PORT/REMOVE batch.

### Task 17.4: Final report + verification

- [ ] **Step 1: Regenerate the final report**

```bash
make strings-report
```

- [ ] **Step 2: Verify Desktop-only and Web-only buckets are minimized**

Each remaining entry in those buckets should have an `ACCEPT` justification in its `description` field.

- [ ] **Step 3: Full test sweep**

```bash
make check-strings
sbt test
cd sangeet-web && npm test
cd ../e2e && npm run test
```

- [ ] **Step 4: Commit final report**

```bash
git add docs/strings-parity-report.md \
        sangeet-core/src/main/resources/ui-strings.json \
        sangeet-core/src/main/scala/com/varpas/sangeet/core/strings/UiStrings.scala \
        sangeet-web/src/UiStrings.elm
git commit -m "docs(strings): final parity report after Phase 17 review"
```

### Task 17.5: Create PR-B

- [ ] **Step 1: Push branch + open PR**

```bash
gh pr create --title "feat(strings): shared UI strings catalog + parity check" --body "$(cat <<'EOF'
## Summary

- Single source of truth for every user-visible string in `sangeet-core/src/main/resources/ui-strings.json`
- Codegen to typed Scala (`UiStrings.scala`) + Elm (`UiStrings.elm`) constants/functions
- CI-enforced parity check: every `platform: both` entry used on both sides, no leaks for `desktop`/`web` entries, no source refs to keys not in catalog
- Phase 17 review report (`docs/strings-parity-report.md`) — desktop-only and web-only buckets minimized; each remaining entry has a documented justification

## Test plan

- [ ] All CI jobs green including new `string-parity` and `strings-gen-sync` jobs
- [ ] Manual smoke: open every dialog on both stacks, confirm visible text unchanged
- [ ] Review `docs/strings-parity-report.md` — bucket counts in PR description

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 2: Watch CI, merge when green**

---

## Summary of file changes

### Workstream A (PR-A)

- **New files:** 6 Scala source + 1 Scala test + 1 Elm source + 1 Elm test + 4 TS files + 1 Python file + 135 JSON test definitions + 20 golden fixtures + 2 docs = ~170 new files
- **Modified files:** 7 source files + 4 config files = ~11 modified files
- **Deleted files:** 1 (DebugConsoleTcpSpec.scala)
- **Lines added:** ~3500 (mostly the 135 JSON definitions and golden fixtures)
- **Lines removed:** ~1500 (DebugConsoleTcpSpec.scala body)

### Workstream B (PR-B)

- **New files:** 1 catalog JSON + 2 generated source files (Scala + Elm) + 1 sbt codegen + 5 TS scripts + 2 shared TS libs + 4 TS test files + 1 Scala codegen test + 3 docs (`ui-strings-catalog.md`, `strings-parity-report.md`, `scripts/README.md`) + 2 config (`scripts/package.json`, `scripts/tsconfig.json`) = ~22 new files
- **Modified files:** `build.sbt`, `Makefile`, `.lefthook.yml`, `.github/workflows/ci.yml`, `.gitignore`, `CLAUDE.md` + ~12 Elm view/state files + ~10 Scala desktop editor/dialog files = ~30 modified files
- **Deleted files:** 0
- **Lines added:** ~2000 (catalog grows incrementally to ~200 entries; codegen + scripts ~1200; remainder is migration churn that mostly substitutes equivalent constructs)
- **Lines removed:** ~600 (inlined literals replaced with `UiStrings.xxx` references)

### Combined (if both PRs land)

- **New files:** ~192
- **Modified files:** ~41
- **Lines added:** ~5500
- **Lines removed:** ~2100

## Risk register

### Workstream A risks

| Risk                                                                                  | Mitigation                                                                                                                 |
| ------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| Elm's encoder produces different bytes than circe for the response shape Tests assume | Phase 3 fixes this for serialize endpoint; rely on `noSpaces` (compact) JSON on the WS wire to avoid whitespace divergence |
| 125-test mechanical port introduces silent regressions                                | Run both runners after every batch of ~15 ports (Task 9.2)                                                                 |
| WS connection timing — Elm hasn't connected when first command sent                   | `TestWsServer.waitForConnection` + 5s timeout (Task 7.1)                                                                   |
| Golden fixtures churn from unrelated format changes                                   | Regenerate via `regenerate_golden_fixtures.py`; review the diff carefully; commit as its own step                          |
| `parity.spec.ts` runs serially per test; 135 tests × ~5s each = ~11 min single-shard  | Playwright matrix is already 4-shard; ~3 min per shard. Consider 8-shard if it grows                                       |
| Desktop refactor (Phase 2) accidentally changes behavior                              | Existing 125 tests run unchanged after Phase 2; if any fail, fix in place before Phase 3                                   |

### Workstream B risks

| Risk                                                                                               | Mitigation                                                                                                                                                                                              |
| -------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Generated `UiStrings.scala`/`UiStrings.elm` drift from `ui-strings.json` (someone edits manually)  | `strings-gen-sync` CI job catches it; header comment on generated files warns against manual edits; lefthook regen-on-commit closes most windows                                                        |
| Codegen step adds ~3–5s to every Scala build (sbt task overhead) on the shared `format` target     | Only re-runs when `ui-strings.json` changes (sbt task caching by input file); negligible in practice                                                                                                    |
| `find-untracked-strings.ts` heuristic produces false positives (CSS class names that look English) | Phase 16 explicitly triages output manually; report format makes file/line trivially navigable                                                                                                          |
| Migration introduces visible UI regressions (typo, wrong key, ASCII vs Unicode mismatch)           | Each migration task runs E2E for the affected area; manual smoke test in Task 14.6 / 15.11; visual diff against pre-migration screenshots if needed                                                     |
| Phase 17 review surfaces 50+ asymmetric entries the user has to walk through                       | Phase 16 sweep upstream already minimized this; the report ranks entries by suspected ease of disposition (PORT candidates first)                                                                       |
| `desktop`/`web` escape hatch becomes a dumping ground over time                                    | Every PR adding a `platform: "desktop"` or `"web"` entry must populate `description` with justification; code review enforces; Phase 17 review re-runs periodically as part of release readiness checks |
| PR-A merges first and introduces a new user-visible string PR-B hasn't catalogued                  | Workstream A directive (see "Workstream organization") forbids new strings; if violated, PR-B catches it during Phase 16 sweep or Phase 17 review                                                       |
