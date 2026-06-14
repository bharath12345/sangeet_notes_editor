# Cross-platform parity checking

Sangeet ships on both desktop (Scala 3 + ScalaFX) and web (Elm 0.19) from a
shared `sangeet-core` model. Keeping them at feature parity is a continuous
problem — toolbar buttons get added on one and forgotten on the other,
behavioral bugs surface on web but not desktop, layouts render differently
because one side's renderer has an off-by-one. Three independent layers of
checks address different classes of drift.

## Layer 1 — Surface-area parity (on demand)

**What it catches.** Missing toolbar buttons, missing dialogs, missing key
bindings, missing API consumers — anything where one platform has a code path
the other doesn't.

**How it works.** A Claude Code subagent (`cross-platform-parity-checker`)
defined at `.claude/agents/cross-platform-parity-checker.md` reads both
`sangeet-desktop/` and `sangeet-web/` source trees, classifies findings as
either genuine gaps or documented conscious asymmetries, and produces a punch
list.

**When it runs.** On demand via `/feature-parity`, after any feature commit,
before a release. Not in CI — it's an LLM call and the output is advisory, not
pass/fail.

**What it doesn't catch.** Runtime behavior. The subagent reads code, not
output. PR-B's vibhag-break off-by-one wasn't visible to it because the layout
function existed on both sides — the *result* differed.

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

**What it catches.** DOM-level layout bugs. Vibhag separator positions, cell
counts per row, stroke-row presence, cursor cell highlighting.

**How it works.** `e2e/integration/dom-parity.spec.ts` builds a known
composition via the debug bridge, then reads the actual rendered DOM and
asserts:

- For Teen Taal, the `.swar-row .beat-cell.vibhag-break` cells are at indexes
  3, 7, 11 (the cells *after* beats 4, 8, 12 — that's how
  `GridRenderer.elm:isVibhagBreak` flags them).
- Two further DOM assertions are stubbed in comments awaiting debug-bridge
  support: row width after taal change (`SetTaal` command not yet
  implemented end-to-end on web), stroke row presence (`Stroke` command not
  yet implemented). Both shipped as a TODO comment in `dom-parity.spec.ts`
  for the next time someone touches that path.

These assertions check what the user *sees*, not what the model *says*.

**When it runs.** Same E2E CI jobs as Layer 2; tests live alongside the
byte-equality tests under `e2e/integration/`.

**What it doesn't catch.** Pixel-perfect visual differences (font rendering,
color shifts, sub-pixel positioning). Visual diff testing is heavier
infrastructure and out of scope until we see evidence we need it. The desktop
side also doesn't participate yet — JavaFX has no DOM, so the equivalent
would be a serialized layout dump from `CanvasRendererFX`. Worth adding only
if we ship a desktop-only rendering bug that escapes Layer 2.

## Decision matrix

| Bug class | Caught by |
|---|---|
| Missing toolbar button on one platform | Layer 1 |
| API endpoint exists on one side, not the other | Layer 1 |
| Key binding wired on desktop, missing on web | Layer 1 |
| `.swar` JSON encoded differently across platforms | Layer 2 |
| Same input produces different HTML export | Layer 2 |
| Layout engine off-by-one (vibhag breaks at 5+4+4+3) | Layer 3 |
| Taal change doesn't reflow the rendered grid | Layer 3 |
| Stroke row missing from web DOM despite stroke data present | Layer 3 |
| Font kerning differs between Chromium and Linux JavaFX | Not caught (out of scope) |

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
