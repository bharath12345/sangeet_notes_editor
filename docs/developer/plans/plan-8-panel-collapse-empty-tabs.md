# Plan 8: Panel Collapse UX + Empty Tab State + CI Fix

## Context

After testing the desktop app from the studio feature branch, three UX issues were identified:

1. **Closing the last tab auto-creates a Gat composition** — user expects an empty state, not an unsolicited new composition
2. **Left file browser panel not shown on first launch** — panel should always be visible (even if empty)
3. **Panel collapse buttons belong on panel edges, not in toolbar** — the three toolbar ToggleButtons (Files, Log, Keyboard) should be replaced with collapse/expand buttons on each panel's own border

Additionally, the CI lint job is failing on PR #15 due to scalafix `OrganizeImports` ordering issues in 3 files.

---

## Change 1: Fix CI — Scalafix Import Ordering

3 files have imports in wrong order per `OrganizeImports` rule:

- `sangeet-core/src/main/scala/com/varpas/sangeet/core/config/ConfigStore.scala` — `io.circe.syntax._` must come after `io.circe.parser._`
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/FileBrowserPanel.scala` — `scalafx.scene.input.InputIncludes` must come after `scalafx.scene.control._`
- `sangeet-core/src/test/scala/com/varpas/sangeet/core/config/AppConfigSpec.scala` — `io.circe.parser._` import needs reordering

**Fix:** Run `sbt scalafixAll` to auto-fix, then verify with `sbt "scalafixAll --check"`.

---

## Change 2: Empty State When Last Tab Closed

**File:** `sangeet-desktop/.../editor/TabManager.scala`

### What changes

- Remove the `if editorTabs.isEmpty then createTab()` calls in `closeTab()` (line 92), `createTab()` tab close handler (line 215), and `openHtml()` tab close handler (line 74)
- Replace with `if editorTabs.isEmpty then showEmptyState()`
- Add an `emptyPlaceholder` (StackPane with centered text: "No compositions open" / "Click New or Open File to begin")
- Wrap `tabPane` and `emptyPlaceholder` in a public `editorArea: StackPane` — parent layout uses this instead of `tabPane` directly
- `showEmptyState()` hides tabPane, shows placeholder
- `hideEmptyState()` (called at the start of `createTab()`, `openFile()`, `openHtml()`) shows tabPane, hides placeholder

**File:** `sangeet-desktop/.../MainApp.scala`

- Replace `tabManager.tabPane` with `tabManager.editorArea` in `verticalSplit.items`

---

## Change 3: Left Panel Always Visible on Startup

**File:** `sangeet-desktop/.../MainApp.scala`

Current startup code (lines 595-597) gates panel visibility on `config.bookmarks.nonEmpty`. Change to:

- Always add the file browser panel to the layout on startup
- Still load bookmarks if any exist
- Respect `leftPanelCollapsed` from config (collapse if true, expand if false)
- On first launch (no config): panel is expanded but empty

---

## Change 4: Panel-Edge Collapse/Expand Buttons

Remove the 3 toolbar ToggleButtons entirely. Each panel gets its own collapse/expand mechanism.

### Approach

Each panel has a collapse button in its header/edge area. When collapsed, the panel is replaced in its parent SplitPane by a thin strip (~24px) containing an expand button. The strip has `maxWidth`/`maxHeight` constrained so it stays thin.

### Config Model Expansion

**File:** `sangeet-core/.../config/AppConfig.scala`

Add two fields:
```
bottomPanelCollapsed: Boolean = false
rightPanelCollapsed: Boolean = false
```

**File:** `sangeet-core/.../config/ConfigCodecs.scala`

Add decoders with `getOrElse` for the two new fields (existing configs gracefully default to `false`).

### Panel Modifications

**File:** `sangeet-desktop/.../editor/FileBrowserPanel.scala`
- Add `setCollapseButton(btn)` method — inserts the button into the existing `headerBox`

**File:** `sangeet-desktop/.../editor/StatusBar.scala`
- Add `setCollapseButton(btn)` method — wraps `headerLabel` in an HBox with the button on the right

**File:** `sangeet-desktop/.../editor/KeyboardLegend.scala`
- Add `setCollapseButton(btn)` method — adds an HBox header at top of `legendBox` with the button

### MainApp Changes

**File:** `sangeet-desktop/.../MainApp.scala`

**Remove:**
- The 3 ToggleButton declarations (`toggleFilesBtn`, `toggleLogBtn`, `toggleKbdBtn`)
- The `_toggleFilesBtn` variable and its assignment
- The 3 `addListener` blocks that manipulate SplitPane items
- The 3 toggle buttons from `toolbar.items`
- The `_toggleFilesBtn` reference in `openFolderBtn` handler

**Add:**
- 3 state variables: `leftPanelExpanded`, `bottomPanelExpanded`, `rightPanelExpanded`
- 3 collapsed strips (thin VBox/HBox with expand arrow buttons):
  - `leftCollapsedStrip` — 24px wide VBox with `>>` button
  - `bottomCollapsedStrip` — 24px tall HBox with `^ Log` button
  - `rightCollapsedStrip` — 24px wide VBox with `<<` button
- 6 functions: `collapseLeftPanel()`, `expandLeftPanel()`, `collapseBottomPanel()`, `expandBottomPanel()`, `collapseRightPanel()`, `expandRightPanel()`
  - Each swaps the panel and its collapsed strip in the parent SplitPane and sets the divider position
- 3 collapse buttons injected into the panels via `setCollapseButton()`

**Update `buildConfig()`** to persist all 3 panel collapsed states.

**Update startup restoration** to apply collapsed states from config for all 3 panels.

**Update Ctrl+B** shortcut to call `collapseLeftPanel()`/`expandLeftPanel()` directly.

**Update `openFolderBtn`** handler: replace `_toggleFilesBtn` reference with `if !leftPanelExpanded then expandLeftPanel()`.

**Update initial layout:** `mainSplit` starts with `[fileBrowserPanel.panel, horizontalSplit]` (panel always present).

---

## Implementation Order

1. Fix CI (scalafix imports) — `sbt scalafixAll`
2. `AppConfig.scala` + `ConfigCodecs.scala` — add 2 new fields
3. `TabManager.scala` — empty state placeholder
4. `FileBrowserPanel.scala`, `StatusBar.scala`, `KeyboardLegend.scala` — add `setCollapseButton()`
5. `MainApp.scala` — all layout/toolbar/startup changes
6. Compile and test: `sbt sangeetDesktop/compile`
7. Launch and manually verify all behaviors
8. Run `sbt scalafmtAll && sbt "scalafixAll --check"` to ensure lint passes
9. Commit and push, verify CI passes

## Files Summary

| File | Change |
|------|--------|
| `ConfigStore.scala` | Fix import order |
| `FileBrowserPanel.scala` | Fix import order + add `setCollapseButton()` |
| `AppConfigSpec.scala` | Fix import order |
| `AppConfig.scala` | Add `bottomPanelCollapsed`, `rightPanelCollapsed` |
| `ConfigCodecs.scala` | Add decoders for new fields |
| `TabManager.scala` | Empty state placeholder, remove auto-create |
| `StatusBar.scala` | Add `setCollapseButton()` |
| `KeyboardLegend.scala` | Add `setCollapseButton()` |
| `MainApp.scala` | Remove toolbar toggles, add panel-edge collapse/expand, update layout/startup |

## Verification

- `sbt compile` — all modules
- `sbt test` — all Scala tests pass
- `sbt scalafmtAll && sbt "scalafixAll --check"` — lint clean
- Launch app: left panel visible on first start (empty)
- Close last tab: empty placeholder shown, no auto-created tab
- Click New: new tab appears, placeholder hidden
- Collapse left panel via button on panel edge: thin strip with expand arrow
- Expand via strip button: panel restores
- Same for bottom (log) and right (keyboard) panels
- Close app, reopen: panel states restored from config
- Push to PR: all CI checks pass
