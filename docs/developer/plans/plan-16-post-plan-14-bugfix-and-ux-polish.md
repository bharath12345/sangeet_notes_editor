# Plan 16 — Post Plan-14 bug-fix and UX polish

> Status: Plan. 4 PRs, executed in order (A → B → C → D). PR-D runs after A/B/C land because it includes a parity-checker sweep over the new state.

## Why

After Plan-14 closed, the user reviewed the web app and surfaced 17 issues: visual hygiene problems, several critical web-vs-desktop behavioral parity bugs, cross-platform UX gaps (dirty-state tracking, Save As, duplicate tabs, keyboard reference dialog), and a few specific bugs the cross-platform parity checker explicitly missed.

The questions worth answering up front:

- **Why did the parity checker miss "Save As"?** It's listed as a "conscious asymmetry to ignore" in `.claude/agents/cross-platform-parity-checker.md` with the justification "browser always prompts for download location, so Save IS Save As." The user disagrees with that reasoning. PR-D removes the exemption.
- **Why did the parity checker miss the grid-layout bugs (#6, #7) and the Da/Ra rendering bug (#5)?** It's a surface-area checker (reads code); it can't see runtime behavior. These are catchable by extending the `parity.spec.ts` runner (Plan-14 A Phase 7+9) to assert on grid layout and stroke-row presence. Currently it only compares final `.swar` + `.html` byte-equality. PR-D extends it.

## Locked-in UX decisions (from brainstorming with the user)

- **Duplicate tab name** (issue #11): soft prompt — `[Switch to it]` / `[Open with new name "abc (2)"]` / `[Cancel]`. Same UX for both create-new and open-file.
- **Close with unsaved changes** (issue #13): 3-button modal — `[Cancel]` / `[Discard]` / `[Save]` (label becomes `[Save As…]` for never-saved compositions). Asterisk in tab title whenever there are unsaved edits, cleared on save. Web gets the same 500ms-debounced autosave as desktop, but only after the user picks a Save As destination (browser file system is sandboxed; no path = no autosave possible).

## PR-A — Web visual hygiene

**Scope:** issues #1, #2, #3, #4, #15, #16. Web-only changes, no Scala touched. Lowest risk; ships fast.

### Tasks

#### A.1 Consolidate the two metadata pill rows (#1)

**Files:**

- Modify: `sangeet-web/src/View/Header.elm`
- Modify: `sangeet-web/src/View/Canvas.elm` (remove `viewHeader`, `viewRaagChip`, `viewTaalChip`, `viewLayaChip`, `viewTypeChip`, `viewArohanAvrohan`)
- Modify: `sangeet-web/src/View/Layout.elm` (`Header.view` already receives `comp.metadata`; nothing structural to change)
- Modify: `sangeet-web/public/styles.css` (drop `.composition-header`, `.composition-meta`, `.composition-title`, `.arohan-avrohan` rules — they become unused)

**What:** Move Raag, Taal, Laya, Type, Arohan, Avrohan chips out of `Canvas.viewHeader` and into `Header.view`. The result is one row with: Raag · Taal · Laya · Type · Cycle · Beat · Sub · Octave. Arohan / Avrohan stay below as a separate single line (they're long text strings, don't fit as chips) — but inside the editor header area, not the canvas.

The `Mode: Swar` chip is removed entirely (see A.3).

#### A.2 Remove duplicate composition title (#2)

**File:** `sangeet-web/src/View/Canvas.elm`

Remove the `h2.composition-title` element. The tab bar title is the sole source of truth.

#### A.3 Toolbar uniformity: every button is icon + text (#3)

**File:** `sangeet-web/src/View/Toolbar.elm`

Audit every `button` element. Each one gets both an emoji/symbol icon AND a text label, in that order. Buttons currently icon-only: Cut (✂), Copy (📋), Paste (📌), Support (💖). Buttons currently text-only: New, Open, Save, HTML, Strokes, Sahitya, Undo, Redo. Buttons that already have both: 🐞 Report bug.

After:

```
[📄 New] [📂 Open] [💾 Save] [✂ Cut] [📋 Copy] [📌 Paste] [📤 HTML]
[↶ Undo] [↷ Redo]
[⚙ Properties] [🐞 Report bug] [❓ Keys] [📖 Guide] [💖 Support] [ℹ About]
[🌐 Script: ▾]
```

Icon choices are sketch; pick consistent ones. The catalog (`ui-strings.json`) already has `*Tooltip` keys; the button text comes from catalog values like `toolbarFileNew = "New"` — no catalog change needed unless a `*Icon` key is desired (probably yes for symmetry).

Also: **remove the "Mode: Swar / Mode: Stroke" chip from `View/Header.elm`** (already covered in A.1). The mode is implicit in the editor state and the chip never showed anything useful because Stroke mode was always going to be removed (see A.4).

#### A.4 Remove Strokes/Sahitya toggle buttons (#15)

**Files:**

- Modify: `sangeet-web/src/View/Toolbar.elm` — drop the two buttons
- Modify: `sangeet-web/src/State/Msg.elm` — remove `ToggleStrokeLine`, `ToggleSahityaLine` (if unused elsewhere)
- Modify: `sangeet-web/src/State/Update.elm` — remove the handlers
- Modify: `sangeet-web/src/State/Model.elm` — remove the model fields if any
- Modify: `sangeet-web/src/View/GridRenderer.elm` — strokes and sahitya always render now; remove conditional gating

This is also tied to issue #5: the stroke row should render based on whether stroke data exists per beat, not on a toggle.

#### A.5 Fix "?" button mapping + add User Guide button (#16)

**Files:**

- Modify: `sangeet-web/src/View/Toolbar.elm` — the "?" button currently dispatches `ShowKeyboardCheatSheet`. Change to dispatch a new `OpenUserGuide` msg.
- Modify: `sangeet-web/src/State/Msg.elm` — add `OpenUserGuide` variant.
- Modify: `sangeet-web/src/State/Update.elm` — `OpenUserGuide` does `Browser.Navigation.load <userGuideUrl>` (or opens new tab via a port).
- Modify: `sangeet-web/src/View/Toolbar.elm` — add a separate "Keys" or "⌨" button bound to `ShowKeyboardCheatSheet`.

Result: `?` opens the User Guide (GitHub URL); a separate explicit button opens the keyboard cheat sheet (matching desktop's two-button layout).

#### A.6 Favicon (#4)

**Files:**

- Copy: `packaging/icons/sangeet-icon-32.png` → `sangeet-web/public/favicon.png` (or `favicon.ico` — convert from the existing `.ico` in packaging)
- Modify: `sangeet-web/public/index.html` — add `<link rel="icon" type="image/png" href="/favicon.png">`

Pick from the existing icons in `packaging/icons/`. The 32×32 PNG is the standard favicon size; can also link the 16×16 for legacy browsers.

### Verification

- `cd sangeet-web && npm test` — 593 tests still green (no behavior changes)
- Local visual check in browser: toolbar buttons all icon+text, one metadata row, no duplicate title, favicon shows in browser tab
- `make check-strings` — catalog still consistent (probably some entries become unused; clean up via `make strings-report` and inspect backlog)

### Commits (suggested)

1. `feat(web): consolidate metadata pills into single header row`
2. `feat(web): remove duplicate composition title from canvas`
3. `feat(web): unify toolbar buttons to icon + text + drop Mode chip`
4. `feat(web): drop Strokes/Sahitya toggles (always render)`
5. `feat(web): ? opens User Guide, add dedicated Keys button`
6. `feat(web): favicon`

---

## PR-B — Critical web behavioral parity

**Scope:** issues #5, #6, #7, #8, #9, #10, #12. These are the "big issues" — bugs where web behavior diverges from desktop. Each one needs investigation; estimates here are pessimistic until confirmed.

### B.1 Da/Ra stroke not rendering on web (#5)

**Symptom:** Web "Show Stroke Line (Da/Ra)" toggle has no effect; no stroke ever rendered below the swar.

**Root cause hypothesis:** Either
(a) PR-A removes the toggle and unconditionally renders; if the stroke row is missing from `GridRenderer.elm` entirely, that's the fix; OR
(b) The stroke row IS rendered conditionally on `model.showStrokeLine` but the toggle msg doesn't wire correctly; OR
(c) Stroke data isn't being attached to events when the user types `d`/`r` while a swar is at the cursor.

**Files to investigate:**

- `sangeet-web/src/View/GridRenderer.elm` — find the stroke-row rendering function
- `sangeet-web/src/State/Update.elm` — find `KeyPressed` for `d`/`r` keys
- `sangeet-web/src/Input/KeyHandler.elm` — stroke key mapping
- Compare against `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/render/GridRendererFX.scala` and `EditorKeyHandler.scala`

**Fix:** Make the stroke row always render (after PR-A removes the toggle). Ensure `d`/`r` keypresses while a swar is at cursor populate the swar's `stroke: Option[Stroke]` field. The render then picks it up.

**Verification:** Type a swar, position cursor, hit `d` — stroke row shows "द" (Da) below the swar. Same for `r` (Ra). Matches desktop.

### B.2 Teen Taal vibhag boundaries wrong on web (#6)

**Symptom:** Vibhag separator lines appear after beats 5, 9, 13 instead of 4, 8, 12. So the 16 beats render as 5+4+4+3 instead of 4+4+4+4.

**Root cause hypothesis:** Off-by-one in `vibhagBreaks` computation. Teen Taal's vibhags are `[{4,sam},{4,taali:2},{4,khali},{4,taali:3}]`. The breaks should be at cumulative indices `[4, 8, 12]` (0-indexed: places where the separator appears AFTER beat 3, 7, 11). If the code accidentally uses 1-indexed beats or `cumsum-includes-last-cell`, you get 5, 9, 13.

**Files to investigate:**

- `sangeet-core/src/main/scala/com/varpas/sangeet/core/layout/` — `BeatGrouper.scala`, `LineBreaker.scala`, `GridLayout.scala` (find vibhag-break logic)
- `sangeet-web/src/Model/Layout.elm` — Elm mirror of the layout types
- Whichever file computes `vibhagBreaks` on the web side (probably an Elm function ported from the Scala layout engine OR the layout is computed server-side via `/layout` endpoint and the Elm just consumes)

**Fix:** Identify where the off-by-one is. Likely a single-line fix once located. Add a unit test in `sangeet-core` test suite asserting Teen Taal's 16 beats produce breaks at `[4, 8, 12]`.

**Verification:** Open a new Teen Taal Gat, type 16 swar. Visual: separators at 4+4+4+4. Add an integration test in `tests/integration/` that asserts this via the parity runner.

### B.3 Taal change doesn't reflow row width (#7)

**Symptom:** Change Teen Taal (16 matras) → Ek Taal (12 matras). Web continues to show 16 beats per row with separators at wrong positions. Should show 12 beats per row.

**Root cause hypothesis:**
(a) The layout cache isn't invalidated on taal change, OR
(b) The row width is hard-coded to 16 somewhere, OR
(c) The taal change msg updates `metadata.taal` but doesn't trigger a layout recompute.

**Files to investigate:**

- `sangeet-web/src/State/Update.elm` — find the taal-change handler (probably `ChangeTaal` or similar)
- `sangeet-web/src/Model/Layout.elm` — see if layout is recomputed reactively or cached
- `sangeet-web/src/Api/Layout.elm` (if exists) — if layout is server-side, ensure the request fires on taal change

**Fix:** On taal change, recompute layout. Plus B.2's fix likely also clears B.3 since they share the vibhag-break logic.

**Verification:** Switch taal mid-edit; row width reflows. Add an integration test that switches taal and asserts the new vibhag breaks.

### B.4 Mouse click can't position cursor (#8)

**Symptom:** Clicking on a swar cell in the rendered grid does nothing. Only arrow keys move the cursor.

**Files to investigate:**

- `sangeet-web/src/View/GridRenderer.elm` — find the per-cell rendering function; check for `onClick` handler
- `sangeet-web/src/State/Msg.elm` — does a `CursorMoveToCell { cycle, beat, sub }` (or equivalent) msg exist?
- Desktop reference: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/render/CanvasRendererFX.scala` — find how mouse events translate to cursor moves

**Fix:** Add `onClick` to each cell in `GridRenderer.elm`. Dispatch a `CursorMoveTo` msg with `{ section, cycle, beat, subIndex }`. Update handler in `Update.elm` to set `model.cursor` accordingly.

**Verification:** Type some swar, click on any earlier cell, cursor jumps there. Add E2E test.

### B.5 Cursor disappears / never blinks (#9)

**Symptom:** After arrow-right past the last swar, the cursor disappears off the rendered grid and can't be recovered. No blinking cursor visible during normal editing.

**Two sub-bugs:**

(a) **Cursor off-screen:** Arrow-right past the end should either clamp at the last position OR auto-scroll/expand. Desktop probably advances to the next cycle (allocating a new line). Web seems to advance the cursor model but the rendered grid only has N rows, so the cursor is logically at cycle N+1 with no visible cell.

(b) **No blinking:** The cursor cell may render with a static highlight class. Desktop uses a JavaFX `Timeline` to toggle visibility every 500ms (`EditorPane.scala:resetCursorBlink`). Web needs an equivalent — probably a CSS `@keyframes` animation on the `.cursor-cell` class, or a `Tick` subscription that toggles a model field.

**Files to investigate:**

- `sangeet-web/src/View/GridRenderer.elm` — find cursor cell highlighting
- `sangeet-web/public/styles.css` — `.cursor` / `.cursor-cell` styles
- `sangeet-web/src/State/Update.elm` — `MoveCursorRight` handler; check if it clamps or extends
- Desktop reference: `EditorPane.scala` lines around `resetCursorBlink` and `redraw`

**Fix:**
(a) For off-screen: either clamp to last valid position OR ensure the grid auto-extends by one cycle when cursor moves past the end (desktop's behavior — needs verification).
(b) For blinking: add CSS animation on `.cursor-cell`:

```css
.cursor-cell {
  animation: cursor-blink 1s steps(2, end) infinite;
}
@keyframes cursor-blink {
  50% {
    opacity: 0.3;
  }
}
```

Pure CSS, no model changes needed.

**Verification:** Cursor always visible and blinks; arrow-right past last swar clamps or extends as desktop does.

### B.6 Clicking section header doesn't select section (#10)

**Symptom:** The big section heading (e.g., "Antara") in the canvas isn't clickable. Only the small section tabs in the toolbar switch sections.

**Files:**

- `sangeet-web/src/View/GridRenderer.elm` (or `Canvas.elm`) — find where section headings are rendered (probably `h3.section-heading`)
- `sangeet-web/src/State/Msg.elm` — `SelectSection` msg already exists (toolbar tabs use it)

**Fix:** Add `onClick (SelectSection idx)` to the section heading element. Same handler the toolbar tab uses; cursor will reset to start of that section via existing logic.

**Verification:** Click any section heading in the rendered grid; that section becomes active. Cursor jumps to its start.

### B.7 Sargam shouldn't say "(Palta)" (#12)

**Root cause confirmed:** `sangeet-core/src/main/scala/com/varpas/sangeet/core/editor/CompositionEditor.scala` has:

```scala
case CompositionType.Sargam =>
  List(Section("Sargam", SectionType.Palta, Nil))
```

The `SectionType.Palta` makes the section render as "Sargam (Palta)" because the renderer appends the section type to the name.

**Files:**

- Modify: `sangeet-core/src/main/scala/com/varpas/sangeet/core/model/Section.scala` — add `Sargam` variant to `SectionType` enum: `enum SectionType { case Sthayi, Antara, Taan, Tihai, Palta, Arohi, Avarohi, Sargam; case Custom(name: String) }`
- Modify: `sangeet-core/src/main/scala/com/varpas/sangeet/core/editor/CompositionEditor.scala` — `case CompositionType.Sargam => List(Section("Sargam", SectionType.Sargam, Nil))`
- Modify: `sangeet-core/src/main/scala/com/varpas/sangeet/core/format/SwarFormat.scala` — add encoder/decoder arm for `Sargam`
- Modify: `sangeet-web/src/Model/Composition.elm` — add `Sargam` to the Elm `SectionType` mirror + JSON codec
- Modify: any switch on `SectionType` in renderers (desktop + web) to handle `Sargam` (probably renders as "Sargam" plain, no parenthetical)

**Verification:** Create a new Sargam composition; section title shows "Sargam" with no parenthetical. `.swar` file's `sections[0].type` field shows `"sargam"`.

**Backward compat:** Existing `.swar` files with `"type": "palta"` in a Sargam composition still load (the loader keeps using `Palta`); only NEW compositions use `Sargam`. Not a breaking change.

### Verification (all of PR-B)

- `sangeet-core/test`, `sangeet-web/test`, `e2e/test` all green
- Each bug above gets at least one regression test in either `sangeet-core/test` (for logic bugs like B.2, B.3, B.7) or `parity.spec.ts` (for cross-platform parity like B.5)
- Manual checklist in browser: type swar in Teen Taal Gat → check vibhag breaks at 4+4+4+4; switch to Ek Taal → row reflows to 12; click mid-composition → cursor jumps; press `d` → Da renders below

### Commits (suggested)

One per bug, atomic. 7 commits total for B.1–B.7.

---

## PR-C — Cross-platform UX

**Scope:** issues #11, #13, #14, #17. Touches both desktop (Scala/ScalaFX) and web (Elm). Higher coordination cost than A/B.

### C.1 Unique tab names (#11)

**Behavior:** Soft prompt with options `[Switch to it]` / `[Open with new name "abc (2)"]` / `[Cancel]` whenever a create-new or open-file would result in a duplicate tab title.

**Disambiguation key:** tab title (display name). For files, that's the filename without `.swar`. For untitled compositions, the title field. Filename collisions across different folders still trip this — desired behavior per user.

**Files:**

Desktop:

- Modify: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/TabManager.scala` — add `findTabByTitle(title: String): Option[EditorTab]`. Add `addTabUnique(et: EditorTab): TabConflictResolution` enum returning `Opened` / `Switched` / `Cancelled`.
- Create: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/DuplicateTabDialog.scala` — modal asking user to choose. Returns `Switch` / `Rename` / `Cancel`.
- Modify: `MainApp.scala` — call sites for `New`, `Open` invoke the unique flow.

Web:

- Modify: `sangeet-web/src/State/Update.elm` — `OpenFile` and `ShowNewDialog` handlers check `Model.tabs` for title collision before adding.
- Create: `sangeet-web/src/View/Dialogs/DuplicateTab.elm` — modal with three buttons.
- Modify: `sangeet-web/src/State/Model.elm` — add `pendingTabOpen : Maybe { title : String, payload : ... }` to hold the queued open while the dialog is up.

**Auto-rename rule:** `"abc"` → `"abc (2)"` → `"abc (3)"` → ... Pick the lowest N not already in use.

### C.2 Dirty-state asterisk + close-with-unsaved prompt + web autosave (#13)

**Behavior:** Asterisk in tab title (e.g., `Yaman • abc *`) whenever there are unsaved edits. Close prompt: `[Cancel]` / `[Discard]` / `[Save]` (label becomes `[Save As…]` for never-saved compositions). Web gets 500ms debounced autosave matching desktop, but only after a Save As destination is picked.

**Files:**

Both stacks need a `dirty: Bool` (or equivalent) on each tab/composition state. Track:

- On any edit → dirty = true
- On successful save (manual or autosave) → dirty = false

Desktop:

- Modify: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/EditorTab.scala` — add `isDirty: Boolean` field
- Modify: `EditorPane.scala` — flip on `pushEditorState`, flip off in `autoSave` success path
- Modify: `TabManager.scala` — render tab title with trailing ` *` when dirty
- Modify: `MainApp.scala` — close handler intercepts; shows `UnsavedChangesDialog` if any tab dirty
- Create: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/UnsavedChangesDialog.scala` — 3-button modal
- Also handle window-close (`onCloseRequest`): walk all tabs, prompt for each dirty one

Web:

- Modify: `sangeet-web/src/State/Model.elm` — add `isDirty : Bool` per tab in `FileTab`
- Modify: `sangeet-web/src/State/Update.elm` — flip on edit msgs, flip off on save complete
- Modify: `sangeet-web/src/View/Toolbar.elm viewFileTab` — render trailing ` *`
- Create: `sangeet-web/src/View/Dialogs/UnsavedChanges.elm` — 3-button modal
- Modify: `sangeet-web/src/State/Update.elm` — `CloseTab` handler intercepts if dirty; queues confirmation
- Web autosave: add a `Tick` (debounced) that auto-saves when `model.activeTab.filePath` is Some and `dirty == true`. The save goes through the existing download port if no other API. Note browser limitation: autosave on web requires writing back to a previously-picked file via the File System Access API (Chrome only). If FSA unavailable, "autosave" becomes a noop and the asterisk just stays on; that's acceptable.

**Edge case:** quitting the desktop app with N dirty tabs. Walk them one at a time with the same modal, or show a single "N tabs have unsaved changes" summary modal with per-tab checkboxes. Pick the simpler one (sequential modals).

### C.3 Save As on web (#14)

**Files:**

- Modify: `sangeet-web/src/View/Toolbar.elm` — add Save As button (after Save)
- Modify: `sangeet-web/src/State/Msg.elm` — add `SaveFileAs` variant
- Modify: `sangeet-web/src/State/Update.elm` — `SaveFileAs` triggers the same flow as `SaveFile` but always prompts for filename (in browser context this is always the case since downloads always prompt). Functionally identical to current Save on web. The distinction matters for autosave tracking: `Save` writes to the known path silently; `Save As` prompts and updates `tab.filePath` to the new path.
- Modify: `sangeet-web/src/Ports.elm` and `sangeet-web/public/ports.js` — the download port; if using File System Access API (where supported), `Save` uses the previously-picked handle, `Save As` always prompts.
- Modify: `.claude/agents/cross-platform-parity-checker.md` — remove the "Save As as a distinct action" entry from "Conscious asymmetries to ignore" (PR-D scope but worth flagging here).

### C.4 Merge keyboard reference panel into cheat sheet dialog (#17)

**Behavior:** Remove the always-visible right-side `KeyboardLegend` panel from desktop. Merge its content into the existing `KeyboardCheatSheetDialog` (desktop) / `View/Dialogs/KeyboardCheatSheet.elm` (web). The cheat sheet dialog now contains BOTH the original shortcuts (Ctrl+S, Ctrl+Z, …) AND the full keyboard reference (which swar each letter maps to, ornament mode keys, octave keys, etc.).

**Files:**

Desktop:

- Delete: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/KeyboardLegend.scala`
- Modify: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/MainApp.scala` — remove the call site that places the legend panel; clean up the `ToggleKeyboardLegend` msg if unused
- Modify: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/KeyboardCheatSheetDialog.scala` — append the keyboard reference content (swar mapping, ornament keys, octave keys, modifier keys, navigation keys) as additional sections

Web:

- Delete: `sangeet-web/src/View/KeyboardLegend.elm`
- Modify: `sangeet-web/src/View/Layout.elm` — drop the conditional rendering of `KeyboardLegend.view` (the right-side panel that was already conditional on `model.showKeyboardLegend`)
- Modify: `sangeet-web/src/State/Model.elm` — drop `showKeyboardLegend` field
- Modify: `sangeet-web/src/State/Msg.elm` — drop `ToggleKeyboardLegend` msg
- Modify: `sangeet-web/src/View/Dialogs/KeyboardCheatSheet.elm` — append the keyboard reference content (mirror of desktop)
- Modify: `sangeet-web/src/View/Toolbar.elm` — drop the "Keys" toggle button (replaced by the cheat-sheet button from A.5)

The content of the keyboard reference (what was in the right-side panel) lives in two places: hardcoded text in `KeyboardLegend.scala` and `KeyboardLegend.elm`. After the merge, both Scala and Elm cheat-sheet dialogs should pull this content from the strings catalog (`ui-strings.json`) so it stays in sync.

### C.5 Fix user-guide keyboard reference page table rendering (#17 continued)

**File:**

- Modify: `docs/user-guide/08-keyboard-reference.md`

The user reported the tables don't render as tables. Probably a missing blank line before/after the table, or wrong column alignment markers, or pipes inside cells without escaping. Diagnose by previewing the rendered page on GitHub and fix the syntax. Often this is a one-liner.

Also verify: the keyboard reference is now ALSO inside the cheat sheet dialog (in-app); the user guide doc remains the canonical reference but should at least render correctly when someone clicks the User Guide button (A.5).

### Verification (all of PR-C)

- Manual: open two files with same name → soft prompt → both options work
- Manual: edit then close tab → 3-button modal → Cancel/Discard/Save each work; never-saved tab shows "Save As…"
- Manual: edit a saved composition → autosave fires silently after 500ms idle (both stacks)
- Manual: cheat sheet dialog now shows both shortcuts AND keyboard reference; no more right-side panel
- Tests: add unit tests for `TabManager.addTabUnique` (desktop) and dirty-state flip transitions (both stacks)

### Commits (suggested)

1. `feat(core): add SectionType.Sargam (groundwork for PR-B.7 if not already in B)` — actually moved to PR-B
2. `feat: unique tab name enforcement with soft-prompt resolution`
3. `feat: dirty-state asterisk + close-with-unsaved prompt`
4. `feat(web): autosave when file path known`
5. `feat(web): Save As button + port wiring`
6. `feat: merge keyboard legend panel into cheat sheet dialog`
7. `docs(user-guide): fix keyboard reference table rendering`

---

## PR-D — Parity-check hardening + final sweep

**Scope:** Distill what PR-A/B/C taught us about gaps in the parity checker, then close them. Final sweep.

### D.1 Remove "Save As" from conscious-asymmetry list

**File:** `.claude/agents/cross-platform-parity-checker.md`

Delete the bullet:

> **Save As as a distinct action** — desktop has Save / Save As (Save uses the prior file path; Save As always prompts). The browser ALWAYS prompts for download location on every save, so "Save" on web IS "Save As" by default; a separate Save As button would be redundant.

The user disagrees with this reasoning; Save As is now a real button on web (PR-C.3).

### D.2 Audit other conscious asymmetries

While in the file, re-read every entry in "Conscious asymmetries to ignore". For each, ask: is this still true after PR-A/B/C? Some that warrant a second look:

- **Auto-save / file restore on startup** — web now has autosave too (PR-C.2). Update the entry to clarify it's about session-restore (open-tabs persistence), not autosave itself.
- **Tab management (Cmd+W, Cmd+Tab, Cmd+Shift+Tab, Ctrl+B file browser)** — still mostly true, but unique-tab-name (PR-C.1) is now shared behavior. No change to the entry.

### D.3 Extend `parity.spec.ts` to assert grid layout + stroke row

**File:** `e2e/integration/parity.spec.ts` and helpers

Current parity tests assert byte-equality of `.swar` and `.html` outputs. They missed bugs #5, #6, #7 because the rendered DOM (and thus grid layout, vibhag breaks, stroke row) was never compared.

Add assertions that, after each canonical test runs:

(a) **Vibhag breaks render at expected positions.** For Teen Taal, breaks at indices `[4, 8, 12]`. Read this from the rendered DOM (`.vibhag-break` class) and assert.
(b) **Stroke row exists** if any swar has a stroke. The desktop renders 5 rows per cycle (markers, ornaments, swar, strokes, sahitya); the web should too. Assert the stroke row is present in the DOM when expected.
(c) **Row width matches taal.matras.** After typing N swar with the cursor wrap-around behavior, assert the rendered grid has the right number of cells per row.

These DOM assertions need to be reproducible on web only initially; later we can extend the desktop runner (`SharedIntegrationSpec`) to assert against a serialized layout dump (since JavaFX doesn't have a DOM).

### D.4 Run the parity-checker subagent on the post-A/B/C state

Invoke the `cross-platform-parity-checker` subagent. It produces a punch list of remaining asymmetries. Walk the list:

- Genuine gap → file as a follow-up issue OR fix in this PR if trivial
- New conscious asymmetry → add to the agent's exemption list with rationale
- Already-known known → ignore

### D.5 Documentation: what the checker covers + what it doesn't

**File:** `docs/developer/debug-bridge.md` (or a new `docs/developer/parity-checking.md`)

Document the three-layer parity stack:

1. **Surface-area parity** (the `cross-platform-parity-checker` subagent) — catches missing toolbar buttons, missing dialogs, missing key bindings, missing API consumers. Runs on demand via `/feature-parity`.
2. **Behavioral byte-equality parity** (`parity.spec.ts` + `SharedIntegrationSpec`) — catches mismatched `.swar` / `.html` output for the same input. Runs in CI.
3. **Rendered layout parity** (extension in D.3 above) — catches grid layout, stroke row presence, vibhag break positions. Runs in CI.

Each layer catches a different class of bug. Surface-area is fastest and cheapest. Byte-equality requires running both stacks. Layout parity requires DOM inspection (web only initially).

### Verification

- `gh pr checks` all green
- Parity-checker subagent run produces a clean report (or only conscious-asymmetry items)
- `parity.spec.ts` now includes the three new assertion families and they pass on all 10 canonical tests

### Commits (suggested)

1. `chore(parity): remove Save As exemption from conscious-asymmetry list`
2. `chore(parity): re-audit and refresh remaining conscious asymmetries`
3. `test(e2e): assert vibhag breaks + stroke row in parity runner`
4. `fix: close gaps surfaced by parity-checker on post-Plan-16 state` (whatever it finds)
5. `docs(developer): three-layer parity stack`

---

## Out of scope

- Theme toggle on web (task #214, intentionally deferred from Plan-14)
- Plan-15 playback sandbox (separate workstream)
- Tabla theka, ornament audio, MIDI export — Plan-15 territory
- Mobile / Android app — long-term roadmap
- Visual diff testing (full pixel-perfect comparison of rendered grids) — heavier infrastructure; the DOM-layout assertions in D.3 are a reasonable middle ground

## Open questions for future iteration

- Browser file system access: Chrome's File System Access API is the only way to do true autosave in browser. Other browsers fall back to download-on-save. Acceptable for MVP; revisit if Firefox/Safari users complain.
- Sequential vs summary "all dirty tabs on quit" modal (PR-C.2 edge case) — pick sequential for simplicity; revisit if it feels annoying.
- Whether the keyboard-reference content (after PR-C.4 merge) should be in `ui-strings.json` (translatable) or hardcoded in the dialog files (current state). Argument for catalog: consistency with everything else. Argument against: long blocks of text + table structure don't lend themselves to per-string codegen. Defer the decision; current implementation hardcodes for now.

## Effort estimate (rough, part-time)

| PR        | Effort                                  |
| --------- | --------------------------------------- |
| A         | 1–2 days                                |
| B         | 3–5 days (each bug needs investigation) |
| C         | 3–4 days                                |
| D         | 1–2 days                                |
| **Total** | **~10–13 days**                         |

## Execution plan

1. Open PR-A as draft, dispatch subagent, land when green
2. Open PR-B as draft, dispatch subagent (likely one per bug or grouped 2–3), land when green
3. Open PR-C as draft, dispatch subagent (likely split by stack: one for desktop, one for web)
4. Open PR-D as draft, dispatch parity-checker subagent first, then close any gaps it finds
5. Final verification: every test suite green, no parity-checker findings remain
