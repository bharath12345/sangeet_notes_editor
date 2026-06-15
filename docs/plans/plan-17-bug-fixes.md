# Plan 17 — Bug cluster fixes + parity hardening

> **For agentic workers:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task.

**Goal:** Close 16 user-reported bugs across desktop and web (cursor model, file lifecycle, dialog parity, web tab/cursor/selection, section management, small surface fixes) and harden the parity-checker so this class of drift gets caught next time.

**Architecture:** Seven focused PRs, each in its own git worktree, dispatched in waves to maximise parallelism. Wave 1 (3 truly independent PRs) starts together. Wave 2 (4 PRs that touch the same shared files as PR-1 or each other) starts after PR-1 merges so they can rebase cleanly.

**Tech stack:** Scala 3 + ScalaFX (desktop), Elm 0.19 (web), Tapir + http4s (server), Playwright (e2e), sbt + npm.

---

## Decisions from brainstorming (2026-06-15)

1. **Constrained swar input** for unknown-raag Arohan / Avrohan / Vadi / Samvadi — plain text input, only `s/r/g/m/p/d/n` (case-sensitive for tivra), space, `_`, `'` accepted; same key-handling as the main editor.
2. **Web "Save to"** uses File System Access API (`showSaveFilePicker`) on Chrome/Edge with download fallback on Firefox/Safari.
3. **Cursor selectionAnchor** is cleared on section switch (mirrors desktop). Clipboard contents survive.
4. **Known-raag fields** are strictly read-only — no escape hatch. Custom raag = pick a different name.
5. **Cursor fix is root-cause**: introduce explicit `AtEndOfSection` state alongside `AtBeat(BeatPosition)`. Lands on both platforms together.
6. **Clear-section** = toolbar button + confirm modal + undoable (Ctrl+Z restores).
7. **Parity checker** gets feature-inventory checklist + render-correctness DOM tests.

---

## Wave 1 — start in parallel immediately

### PR-1 — Cursor model root-cause refactor (bugs 3, 4, 10)

**Branch:** `feat/plan-17-cursor-root-cause`
**Risk:** High — touches the editor model on both platforms, including `.swar` serialization and undo-history serialization. Must land cleanly because other PRs in Wave 2 build on the new shape.

**The change:**
- Replace `CursorModel.position: BeatPosition` with a discriminated cursor:
  - Scala: `enum CursorPosition { case AtBeat(pos: BeatPosition); case AtEndOfSection }`
  - Elm: `type CursorPosition = AtBeat BeatPosition | AtEndOfSection`
- `AtEndOfSection` represents "cursor blinks after the last event." All cursor-arithmetic helpers (`nextBeat`, `prevBeat`, `moveTo`) need updating to handle the new variant.
- **Bug 3 fix**: in `CursorModel.minBeat` (and Elm equivalent), the off-by-one — for `cycle == 0` and `startingBeat == 1`, minBeat must be `0`. Verify both platforms.
- **Bug 4 fix**: in `EditorKeyHandler` fast-type grouping path (`sangeet-core/src/main/scala/com/varpas/sangeet/core/editor/EditorKeyHandler.scala:101-116` and Elm equivalent), gate the regrouping on "same `AtBeat(samePos)` as last keystroke." If the cursor moved between keystrokes (different beat or now `AtEndOfSection`), do NOT regroup — insert as a new beat shifting subsequent events right.
- **Bug 10 fix**: in `CompositionEditor.pasteEvents` (`sangeet-core/.../editor/CompositionEditor.scala:236-249`), distinguish `AtBeat(pos)` (existing logic: shift events at `position >= pos`) from `AtEndOfSection` (append, no shift). Same fix in `sangeet-web/src/State/Update.elm` paste handler.

**Files:**
- Modify: `sangeet-core/src/main/scala/com/varpas/sangeet/core/editor/CursorModel.scala`
- Modify: `sangeet-core/src/main/scala/com/varpas/sangeet/core/editor/CompositionEditor.scala`
- Modify: `sangeet-core/src/main/scala/com/varpas/sangeet/core/editor/EditorKeyHandler.scala`
- Modify: `sangeet-core/src/main/scala/com/varpas/sangeet/core/format/Codecs.scala` (Cursor codec — circe)
- Modify: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/render/GridRendererFX.scala` (or wherever cursor is drawn — handle `AtEndOfSection`)
- Modify: `sangeet-web/src/Model/Cursor.elm`
- Modify: `sangeet-web/src/State/Update.elm` (paste handler, key handler, grouping)
- Modify: `sangeet-web/src/View/GridRenderer.elm` (cursor render: `AtEndOfSection` paints the trailing-cell border)
- Modify: `sangeet-web/src/Api/*.elm` (any cursor JSON encode/decode)
- Modify: `sangeet-core/src/test/scala/.../CursorModelSpec.scala` + add cases for `AtEndOfSection`
- Modify: `sangeet-web/tests/State/UpdateTest.elm` + cases for new variant
- Regenerate: `tests/integration/golden/*.swar` — undo-history serialization may differ; re-run golden generation

**Verification:**
- `sbt sangeetCore/test sangeetServer/test sangeetDesktop/test` — all green
- `cd sangeet-web && npx elm-test` — all green
- `cd e2e && npx playwright test` — all green (esp. parity.spec.ts + dom-parity.spec.ts)
- Manual: open a .swar with Sa, Re, Ga in beats 1-3 on desktop. Confirm cursor reaches before Sa (bug 3). Confirm typing between Sa and Re shifts Re and Ga right (bug 4). Confirm pasting at end appends without merge (bug 10).

---

### PR-2 — Desktop file lifecycle hardening (bugs 1, 11)

**Branch:** `feat/plan-17-desktop-file-lifecycle`
**Risk:** Low — desktop-only, contained to `TabManager` + `EditorTab`.

**Bug 1** — false "modified externally" on tab switch. Root cause at `sangeet-desktop/.../editor/TabManager.scala:123-129`: mtime is captured via the `filePath =` setter side-effect *after* `SwarFormat.readFile(path)` returns, leaving a window for clock-skew or concurrent writes.

**Bug 11** — `" (deleted)"` suffix gets permanently appended at `EditorTab.scala:80-81` whenever `Files.exists(path)` returns false, including transient gaps (atomic-rename save), and never cleared when the file reappears.

**The change:**
- In `TabManager.scala:123-129`, call `et.refreshMtime()` immediately after `SwarFormat.readFile(path)` succeeds (before any `filePath =` setter side-effects).
- In `EditorTab.wasDeletedExternally` (line 80-81), add a 200ms retry: if `Files.exists(p)` returns false, sleep 200ms and check once more. Only flag as deleted if both checks fail.
- In `TabManager.checkExternalChanges` (line 311-313), when applying the `" (deleted)"` suffix, also check on every subsequent invocation: if the file now exists, strip the suffix from `tab.text`.
- In `TabManager.checkExternalChanges`, throttle: don't run if an autosave is in flight (`EditorPane.saveTimer` is active). Skip until next tab switch.

**Files:**
- Modify: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/TabManager.scala`
- Modify: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/EditorTab.scala`
- Modify: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/EditorPane.scala` (expose `isSavePending: Boolean`)
- Modify: `sangeet-desktop/src/test/scala/.../TabManagerSpec.scala` — add test for mtime-after-read invariant, deleted-suffix transient + cleared

**Verification:**
- `sbt sangeetDesktop/test` — green
- Manual: open two .swar files in two tabs. Switch between them several times — no "modified externally" prompt fires. Save tab A (Ctrl+S), immediately switch to B — no false prompt. Delete a file from Finder while tab open — suffix appears. Restore the file — suffix disappears on next tab switch.

---

### PR-7 — Parity-checker hardening

**Branch:** `feat/plan-17-parity-hardening`
**Risk:** Low — docs + agent prompt + one new test file. No production code touched.

**The change:**
- Rewrite `.claude/agents/cross-platform-parity-checker.md` to enumerate, for each platform:
  - Toolbar items (with intent: **both missing AND extra** flagged)
  - Dialog field lists (per dialog: New Composition, Properties, Support, etc.)
  - Keyboard shortcut tables (per global / editor / dialog scope)
  - Validation guards (which fields are required at submit time, on each dialog)
  - Tab lifecycle behavior (close, close-all, switch, file-modified-externally)
- Create `.claude/parity-inventory.md` — canonical ground-truth document listing every toolbar item, every dialog field, every keybinding, every section-type/composition-type validation. Subagent reconciles both sides against this.
- Extend `e2e/integration/dom-parity.spec.ts` with **render-correctness** tests:
  - Cursor visible: animation opacity never < 0.5 at any keyframe; outline contrast > 3:1 against background.
  - Selection visible: contrast > 3:1 against unselected cell.
- Update `docs/developer/parity-checking.md` Layer 1 section to reference the new inventory file. Add a "what catches what" subsection for the new categories.

**Files:**
- Modify: `.claude/agents/cross-platform-parity-checker.md`
- Create: `.claude/parity-inventory.md`
- Modify: `e2e/integration/dom-parity.spec.ts`
- Modify: `docs/developer/parity-checking.md`

**Verification:**
- Run the subagent once on the current main: `/feature-parity`. Confirm the new categories surface findings that the old prompt would have missed (e.g., the bugs that motivated this plan).
- Run `cd e2e && npx playwright test integration/dom-parity.spec.ts` — green.

---

## Wave 2 — start after PR-1 merges

### PR-3 — New-composition dialog overhaul (bugs 5, 6, 7)

**Branch:** `feat/plan-17-new-comp-dialog`
**Risk:** Medium — biggest UI delta (especially on web). Independent of cursor model, can technically run in Wave 1, but bundled here to keep Wave 1 narrow.

**Desktop changes:**
- For known raags (`Raags.all.contains(selectedRaag)`): auto-fill thaat / arohan / avrohan / vadi / samvadi and make those 5 fields read-only (disabled / non-editable styling).
- For custom raags (typed name not in `Raags.all`): those 5 fields become editable, but use a constrained input that accepts only swar keys. Specifically:
  - **Arohan / Avrohan** — accept `s/r/g/m/p/d/n` (lower), `S/R/G/M/P/D/N` (upper = tivra), `_` (komal prefix), `'` (octave mark), space. Block all other characters at the keydown handler. Parse into `List[String]` of swar names at submit time using the same `SwarKeyMap` as the editor.
  - **Vadi / Samvadi** — single-swar variant of the same rule; max one swar accepted.

**Web changes (much bigger):**
- Add all 7 missing fields to `NewCompositionDialog.elm`: Save to, Thaat, Arohan, Avrohan, Vadi, Samvadi, Script. Match desktop layout.
- "Save to" uses the File System Access API. Add a JS port that calls `window.showSaveFilePicker({ types: [{ description: 'Swar', accept: { 'application/json': ['.swar'] } }] })` and returns the handle. Store handle in `Model.activeTab.fileHandle: Maybe FileHandle`. On Firefox/Safari (no API), fall back to downloading the .swar on first save.
- Same auto-fill / read-only behavior for known raags. Same constrained-input behavior for custom raag swar fields.
- Add submit-time validation: title required (non-empty), laya required (not "none") if composition type is Gat or Bandish, save-to required.
- Per-composition script (replace app-global currentScript field with `Composition.metadata.script`).

**Files:**
- Modify: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/NewCompositionDialog.scala`
- Modify: `sangeet-web/src/View/Dialog/NewCompositionDialog.elm` (or wherever it lives)
- Modify: `sangeet-web/src/State/Model.elm` (dialog state for 7 new fields, fileHandle on tab)
- Modify: `sangeet-web/src/State/Update.elm` (handlers for new fields, validation)
- Modify: `sangeet-web/src/Ports.elm` (new port: showSaveFilePicker)
- Modify: `sangeet-web/public/ports.js` (JS bridge for File System Access API + fallback)
- Modify: `sangeet-core/src/main/scala/com/varpas/sangeet/core/model/Composition.scala` (move script from app-global to per-composition metadata if not already)
- Add: `sangeet-web/tests/View/Dialog/NewCompositionDialogTest.elm`
- Modify: `sangeet-desktop/src/test/scala/.../NewCompositionDialogSpec.scala` (assertion: custom raag rejects "abc xyz", known raag locks fields)
- Modify: `sangeet-core/src/main/resources/ui-strings.json` (new labels)
- Regenerate ui-strings via `make gen-strings`

**Verification:**
- `sbt sangeetCore/test sangeetDesktop/test` green
- `cd sangeet-web && npx elm-test` green
- `cd e2e && npx playwright test` green
- Manual desktop: open New Composition, pick Yaman — verify thaat/arohan/avrohan/vadi/samvadi auto-fill and are non-editable. Type "MyRaag" — those fields become editable; type "abc" → no characters accepted; type "srgm" → "Sa Re Ga Ma" accepted.
- Manual web: same checks. Confirm Save-to picker fires on Chrome. Confirm download fallback fires on Firefox (or simulate).

---

### PR-4 — Web tab / cursor / selection (bugs 2, 12, 13)

**Branch:** `feat/plan-17-web-tab-cursor-selection`
**Risk:** Low — three independent web-only fixes bundled.

**Bug 2 fix** (`sangeet-web/src/State/Update.elm:1367-1373` `doCloseTabImmediate`):
- Replace unconditional `handleNewTabHelper` call on empty tabs with: branch on `List.isEmpty remainingTabs` → set `model.activeTabId = Nothing` and render an empty-state placeholder.
- Update `sangeet-web/src/View/Canvas.elm` to render `<div class="empty-state">No composition open</div>` (matching desktop wording) when `activeTabId` is `Nothing`.

**Bug 12 fix** (`sangeet-web/public/styles.css:687-700`):
- Replace the broken `@keyframes cursor-blink { 50% { opacity: 0.3; } }` with explicit 0% / 50% / 100% stops:
  ```css
  @keyframes cursor-blink {
    0%, 100% { opacity: 1; }
    50%      { opacity: 0.4; }
  }
  ```
- Bump `.cursor-cell` outline color contrast (current `#5c6bc0` on `#e8eaf6` is too low). Use `var(--accent)` for theme compatibility.

**Bug 13 fix** (`Update.elm:242` `SelectSection`):
- After updating `currentSectionIndex`, clear `model.history.present.cursor.selectionAnchor` (push a new snapshot with cursor.selectionAnchor = Nothing). Tab's clipboard stays in place.

**Files:**
- Modify: `sangeet-web/src/State/Update.elm`
- Modify: `sangeet-web/src/View/Canvas.elm`
- Modify: `sangeet-web/public/styles.css`
- Add tests: `sangeet-web/tests/State/UpdateTest.elm` (close-all → empty, select-section clears anchor)
- Modify: `e2e/integration/dom-parity.spec.ts` — add assertions (cursor opacity never below 0.4; close all tabs shows empty state; copy in Gat + switch to Antara renders no selection in either)

**Verification:**
- `cd sangeet-web && npx elm-test` green
- `cd e2e && npx playwright test` green
- Manual: close all tabs → "No composition open" shown. Cursor blinks visibly. Copy Sa Re in Gat, switch to Antara — no blinking box in either.

---

### PR-5 — Section management cleanup (bugs 9, 14, 15)

**Branch:** `feat/plan-17-section-management`
**Risk:** Low-medium — touches both platforms but each change is small.

**Bug 9** — web "Add Section" not gated on composition type:
- In `sangeet-web/src/State/Update.elm` `AddSection` handler, check `composition.metadata.compositionType == Gat`. If not, dispatch the same "sections can only be added to Gat compositions" message that desktop logs to the status bar.

**Bug 14** — remove rename-section everywhere:
- Desktop: remove the rename button from the section toolbar (`sangeet-desktop/.../editor/SectionToolbar.scala` or whichever component holds it), remove the rename dialog, remove the `RenameSection` message + handler.
- Web: remove rename button, remove dialog, remove `RenameSection` Msg + handler from `Update.elm`.
- Strip rename strings from `ui-strings.json` and regenerate.

**Bug 15** — add clear-section everywhere:
- Add a new `ClearSection` toolbar button where Rename used to be (both platforms). Trash-can icon, "Clear" label.
- On click: confirm modal "Clear all swars in {sectionName}? Yes / No". Both platforms.
- On confirm: dispatch `ClearSection` message that calls `CompositionEditor.clearSection(comp, sectionIdx)` (new method in `sangeet-core`). Returns a composition with the same section but `events = List.empty`. Push undo step.
- Web: same flow via `Api.Editor.clearSection` (new endpoint) → `Tapir` route.

**Files:**
- Add: `sangeet-core/src/main/scala/com/varpas/sangeet/core/api/EditorApi.scala` (`clearSection` method)
- Modify: `sangeet-core/src/main/scala/com/varpas/sangeet/core/editor/CompositionEditor.scala` (`clearSection` helper)
- Add: `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/EditorEndpoints.scala` (`clearSection` endpoint at `/api/v1/editor/section/clear`)
- Add: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/EditorRoutes.scala` (route handler)
- Modify: `sangeet-desktop/.../editor/SectionToolbar.scala` (remove rename, add clear)
- Modify: `sangeet-desktop/.../editor/EditorPane.scala` (wire clear handler)
- Modify: `sangeet-desktop/.../dialog/*` — remove rename dialog
- Modify: `sangeet-web/src/View/SectionToolbar.elm` (or equivalent)
- Modify: `sangeet-web/src/State/Update.elm` (AddSection gated, RenameSection removed, ClearSection added with confirm dialog)
- Modify: `sangeet-web/src/State/Model.elm` (drop rename dialog state, add clear confirm dialog state)
- Modify: `sangeet-web/src/Api/Editor.elm` (`clearSection` HTTP client)
- Modify: `sangeet-core/src/main/resources/ui-strings.json` (remove "Rename Section", add "Clear Section", "Clear all swars in {section}?")
- Regenerate ui-strings
- Tests: `sangeet-core/.../EditorApiSpec.scala` (clearSection), `sangeet-web/tests/State/UpdateTest.elm` (gated add, clear with undo)

**Verification:**
- `sbt sangeetCore/test sangeetServer/test sangeetDesktop/test` green
- `cd sangeet-web && npx elm-test` green
- `cd e2e && npx playwright test` green
- Manual: on a Sargam composition, try to add a Taan section — blocked with status log message. Clear a section, Ctrl+Z restores. Rename button gone on both platforms.

---

### PR-6 — Web small fixes (bugs 8, 16)

**Branch:** `feat/plan-17-web-small-fixes`
**Risk:** Trivial — two unrelated small web fixes.

**Bug 8** — remove "mode: swar" pill from toolbar:
- Find the `<span>` at toolbar xpath `/html/body/div/div/div[2]/div[1]/div[1]/div[6]/span` — likely in `sangeet-web/src/View/Toolbar.elm`. Remove the element and its CSS.

**Bug 16** — Ctrl+Z and Ctrl+Shift+Z don't fire from keyboard on web:
- In `sangeet-web/src/State/Update.elm` global keydown handler (the same one that handles Ctrl+S from plan-16), add bindings for:
  - `Ctrl+Z` (without shift) → dispatch `Undo` Msg (same Msg the toolbar's undo button fires) + `preventDefault`
  - `Ctrl+Shift+Z` → dispatch `Redo` Msg + `preventDefault`
- Also accept `Cmd` on macOS (`metaKey` modifier).

**Files:**
- Modify: `sangeet-web/src/View/Toolbar.elm` (remove pill)
- Modify: `sangeet-web/src/State/Update.elm` (keydown bindings)
- Modify: `sangeet-web/public/styles.css` (remove pill CSS if separate)
- Tests: `sangeet-web/tests/Input/KeyHandlerTest.elm` (Ctrl+Z → Undo, Ctrl+Shift+Z → Redo)

**Verification:**
- `cd sangeet-web && npx elm-test` green
- `cd e2e && npx playwright test` green
- Manual: web — toolbar no longer has "mode: swar" text. Type Sa, press Ctrl+Z — Sa removed. Press Ctrl+Shift+Z — Sa restored.

---

## Parallelism strategy

**Wave 1 (dispatch immediately, 3 parallel agents):**
- PR-1 cursor root-cause (opus, highest risk)
- PR-2 desktop file lifecycle (sonnet)
- PR-7 parity-checker hardening (sonnet)

These are truly independent (no shared file overlap).

**Wave 2 (dispatch after PR-1 merges, 4 parallel agents):**
- PR-3 new-comp dialog overhaul (sonnet)
- PR-4 web tab / cursor / selection (sonnet)
- PR-5 section management cleanup (sonnet)
- PR-6 web small fixes (sonnet)

These touch web `Update.elm` / `Model.elm` / `Toolbar.elm` in different sections so file-level conflicts are unlikely, but each rebases on PR-1 to pick up the new `CursorPosition` shape.

**Conflict resolution:** Each PR lives in `.worktrees/plan-17-<short-name>/`. Merges happen sequentially (Wave 1 in any order, then Wave 2 in any order). User's rule "Fix CI before next task" applies — each PR must be green before the next merges.

---

## Per-PR contract

Each agent implementing one PR:
1. Creates its own worktree from latest `origin/main` (Wave 2 includes PR-1's merge commit).
2. Implements the change against the listed files.
3. Runs the full local test suite for the affected modules.
4. Runs `make lint` (or the equivalent per-language linters).
5. Opens a PR titled `feat(plan-17): <short description>` with body listing the bugs fixed and the verification steps performed.
6. **Does NOT merge** — the controller (me) merges in order, handling rebases when CI breaks.
