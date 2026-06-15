# Cross-platform parity checking

Sangeet ships on both desktop (Scala 3 + ScalaFX) and web (Elm 0.19) from a
shared `sangeet-core` model. Keeping them at feature parity is a continuous
problem — toolbar buttons get added on one and forgotten on the other,
behavioral bugs surface on web but not desktop, layouts render differently
because one side's renderer has an off-by-one. Three independent layers of
checks address different classes of drift.

## Layer 1 — Surface-area parity (on demand)

**What it catches.** Missing toolbar buttons, missing dialogs, missing key
bindings, missing API consumers, extra UI elements on one side, dialog field
gaps, validation asymmetries — anything where one platform has a code path or
UI element the other doesn't.

**How it works.** A Claude Code subagent (`cross-platform-parity-checker`)
defined at `.claude/agents/cross-platform-parity-checker.md` reads both
`sangeet-desktop/` and `sangeet-web/` source trees and reconciles them against
the canonical feature inventory at `.claude/parity-inventory.md`. The inventory
is a structured table of every toolbar item, dialog field, keyboard shortcut,
validation guard, and tab-lifecycle behavior on both platforms. The subagent
flags any asymmetry not already documented as a known gap or conscious
difference.

**When it runs.** On demand via `/feature-parity`, after any feature commit,
before a release. Not in CI — it's an LLM call and the output is advisory, not
pass/fail.

**What it doesn't catch.** Runtime behavior or CSS-rendering bugs. The subagent
reads code, not output. PR-B's vibhag-break off-by-one wasn't visible to it
because the layout function existed on both sides — the _result_ differed.

## Layer 2 — Behavioral byte-equality parity (CI)

**What it catches.** Mismatched `.swar` JSON and `.html` export output for the
same sequence of operations. If desktop's `EditorApi.changeTaal` and web's
`/editor/change-taal` round-trip the same input differently, this fires.

**How it works.** Each canonical fixture under `tests/integration/*.json`
declares a sequence of debug-bridge commands (Reset, TypeChar, SwitchSection,
etc.) plus checkpoints and golden-file assertions. Two runners consume them:

- **Desktop**: `sangeet-desktop/.../SharedIntegrationSpec.scala` connects to
  the in-process TCP debug console on port 28081 and drives a live JavaFX
  editor.
- **Web**: `e2e/integration/parity.spec.ts` drives the Elm app via the
  WebSocket debug bridge (gated by `?debug=ws://...` URL param) and asserts
  byte-identical `.swar` + `.html` output.

Both runners hit the same `DumpComposition` / `ExportHtml` debug commands and
diff against `tests/integration/golden/*.{swar,html}` files.

**When it runs.** Every CI run (`Desktop Tests (JavaFX)` job + `E2E Browser
Tests (shard *)` jobs).

**What it doesn't catch.** Rendering bugs that don't show up in `.swar` /
`.html`. The `.html` golden is the desktop-generated HTML export — if web's
`GridRenderer.elm` renders the in-app grid differently from how `HtmlExport.scala`
serializes the export, the golden still passes.

## Layer 3 — Rendered layout parity (CI, web-only initially)

**What it catches.** DOM-level layout bugs and CSS-rendering correctness.
Vibhag separator positions, cell counts per row, stroke-row presence, cursor
cell highlighting, animation visibility, contrast ratios.

**How it works.** `e2e/integration/dom-parity.spec.ts` builds a known
composition via the debug bridge, then reads the actual rendered DOM and
asserts:

- For Teen Taal, the `.swar-row .beat-cell.vibhag-break` cells are at indexes
  3, 7, 11 (the cells _after_ beats 4, 8, 12 — that's how
  `GridRenderer.elm:isVibhagBreak` flags them).
- Two further DOM assertions ship as `test.skip` in `dom-parity.spec.ts`
  (taal-reflow on `SetTaal`, stroke-row presence on `Stroke`). The
  `SetTaal` / `Stroke` handlers in `Debug/Interpreter.elm` are wired and
  reachable through the MCP debug-console transport, but the shared
  `handleDebugEditorResultReceived` in `State/Update.elm` doesn't yet
  propagate the result snapshot to `model.tabs[activeTab]` or call
  `requestLayout` — so the rendered DOM stays at the pre-command layout
  even though the API call succeeded. Drop the `test.skip` once that
  handler is fixed.
- **Render-correctness tests** (added in plan-17 PR-7) check CSS-driven
  visibility. Two `test.fixme` cases (cursor cell animation opacity stays
  >= 0.4, cursor outline has WCAG 3:1 contrast) will fail until plan-17 PR-4
  fixes bug 12 (broken `@keyframes` rule). These tests caught a class of bug
  invisible to byte-equality checks — the HTML export was correct, but the
  in-app CSS rendering was broken.

These assertions check what the user _sees_, not what the model _says_.

**When it runs.** Same E2E CI jobs as Layer 2; tests live alongside the
byte-equality tests under `e2e/integration/`.

**What it doesn't catch.** Pixel-perfect visual differences (font rendering,
color shifts, sub-pixel positioning beyond what WCAG contrast tests cover).
Visual diff testing is heavier infrastructure and out of scope until we see
evidence we need it. The desktop side also doesn't participate yet — JavaFX
has no DOM, so the equivalent would be a serialized layout dump from
`CanvasRendererFX`. Worth adding only if we ship a desktop-only rendering bug
that escapes Layer 2.

## Decision matrix

| Bug class                                                     | Caught by                 |
| ------------------------------------------------------------- | ------------------------- |
| Missing toolbar button on one platform                        | Layer 1                   |
| **Extra toolbar button on one platform**                      | **Layer 1** (plan-17 PR-7)|
| API endpoint exists on one side, not the other                | Layer 1                   |
| Key binding wired on desktop, missing on web                  | Layer 1                   |
| **Dialog field missing on one platform**                      | **Layer 1** (plan-17 PR-7)|
| **Validation guard asymmetry (e.g. web allows empty title)**  | **Layer 1** (plan-17 PR-7)|
| `.swar` JSON encoded differently across platforms             | Layer 2                   |
| Same input produces different HTML export                     | Layer 2                   |
| Layout engine off-by-one (vibhag breaks at 5+4+4+3)           | Layer 3                   |
| Taal change doesn't reflow the rendered grid                  | Layer 3                   |
| Stroke row missing from web DOM despite stroke data present   | Layer 3                   |
| **Cursor cell invisible due to broken CSS @keyframes**        | **Layer 3** (plan-17 PR-7)|
| **Insufficient contrast on cursor outline**                   | **Layer 3** (plan-17 PR-7)|
| Font kerning differs between Chromium and Linux JavaFX        | Not caught (out of scope) |

## Conscious asymmetries

Real platform differences are listed in
`.claude/agents/cross-platform-parity-checker.md` under "Conscious asymmetries
to ignore". Examples:

- TCP debug console (desktop-only by design — web uses the equivalent
  WebSocket bridge on the same `DebugCommand` ADT).
- Single-instance lock on port 47633 (desktop only; browser tabs handle the
  equivalent natively).
- Theme toggle (`Ctrl+Shift+T`) — desktop has `ThemeManager`; web uses CSS
  `prefers-color-scheme`.
- F1 (browser-reserved), Ctrl+Shift+B (browser-reserved for bookmarks bar).

Add to this list any difference that's intentional and unlikely to flip
direction — drift on these isn't a bug.

## Adding a new check

- **New byte-equality scenario**: add a fixture file under `tests/integration/`
  following the existing JSON shape. The desktop runner and `parity.spec.ts`
  pick it up automatically.
- **New DOM-layout assertion**: add a test case to `dom-parity.spec.ts`. Use
  the debug bridge to build state deterministically, then read the rendered
  DOM with Playwright locators.
- **New surface-area heuristic**: update the subagent prompt in
  `.claude/agents/cross-platform-parity-checker.md`.
