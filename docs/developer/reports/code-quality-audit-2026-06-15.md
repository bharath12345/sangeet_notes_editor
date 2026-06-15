# Code Quality Audit — Plan 18 PR-2a — 2026-06-15

**Auditor:** Claude (via Plan 18 PR-2a)
**Codebase snapshot:** main @ c51dce9c15773f249124107fd33cc9f5de520057 (`feat(plan-17): PR-5 section management cleanup`)
**Scope:** all 4 modules (`sangeet-core/`, `sangeet-desktop/`, `sangeet-server/`, `sangeet-web/`) + `e2e/`, `mcp-servers/`

## Executive summary

The codebase is healthy on average — clean module boundaries, a pure `sangeet-core/model/` package, generous test coverage, and a real metrics pipeline already in place on the server. The pain points cluster in three places.

First, `sangeet-web/src/State/Update.elm` (3627 lines, 87 `Msg` variants) has outgrown a single file: editor mutations, file/tab/dialog state, Drive integration, debug-bridge plumbing, and HTTP-result handlers all coexist there, and recent Plan-17 bug fixes had to thread changes through dozens of branches in one mega `case`. Second, the cross-platform fast-typing grouping logic exists in **two complete copies** (`EditorKeyHandler.scala` + `Update.elm`) — each Plan-17 fix to bug 4 had to be written twice and reviewed twice; the same fate awaits cursor-alignment, ornament-finish, and similar editor-state machines unless they move into shared core. Third, error handling is inconsistent: web HTTP failures are logged to a status bar with no user-visible affordance for retry or detail, several JSON parse failures swallow the underlying Circe error (`.toOption.getOrElse`), and there is no top-level JS error boundary on web.

**Top 3 findings by severity**: (1) `Update.elm` modularity (Critical, addressed by PR-2b), (2) cross-platform grouping/editor-state duplication (High, PR-2d), (3) no top-level error capture on web (High, PR-3c).

**Counts**: 18 findings total — 3 Critical, 5 High, 7 Medium, 3 Low. **12 committed for fix in Plan 18**, **6 filed as known debt** for follow-up plans.

## Severity-ranked top 10 findings

1. **`State/Update.elm` is a 3627-line god-module** (Critical) —
   `sangeet-web/src/State/Update.elm:1-3627`. 87 `Msg` branches across one `case msg of` cover keyboard, file ops, tab management, dialog state, sections, Drive, debug bridge, and 12 distinct API-result decoders. Recent plan-17 fixes (bugs 2/4/12/13) touched many independent branches in a single edit, raising merge-conflict and reasoning cost.
   → fix in **PR-2b**.

2. **Fast-typing grouping logic duplicated across Scala desktop and Elm web** (High) —
   `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/EditorKeyHandler.scala:23-168` defines `GroupingState`, `cursorMatchesGrouping`, `extending` heuristic, and undo-and-replay strategy. `sangeet-web/src/State/Update.elm:2085-2202` re-implements the same state machine in Elm. The bug-4 fix landed twice in PR #86 with two separate `cursorMatchesGrouping`/`cursorStillAlignedWithGroup` predicates.
   → fix in **PR-2d**.

3. **No top-level error boundary on web** (High) —
   `sangeet-web/public/index.html`, `sangeet-web/public/ports.js`. No `window.onerror` / `unhandledrejection` listener. Elm crashes that escape the runtime (port decoder mismatches, JSON parse failures in ports.js, third-party rrweb / Drive callbacks) show only as console output.
   → fix in **PR-3c**.

4. **ScalaFX dialog frame is hand-rolled in 7 dialogs** (High) —
   All of `AboutDialog`, `SupportDialog`, `CommandPaletteDialog`, `BugReportDialog`, `CrashRecoveryDialog`, `KeyboardCheatSheetDialog`, `NewCompositionDialog` repeat `new Stage { initStyle(StageStyle.Utility); initModality(Modality.WindowModal); scene = new Scene ... }`. `openInBrowser` is copy-pasted in `AboutDialog.scala:21-23` and `SupportDialog.scala:21-23` (and another copy in `BugReportDialog`).
   → fix in **PR-2c**.

5. **Debug-bridge HTTP handlers are 4× copies of the same case match** (High) —
   `sangeet-web/src/State/Update.elm:3186-3434` defines `handleDebugResetReceived`, `handleDebugDumpReceived`, `handleDebugExportReceived`, `handleDebugEditorResultReceived` — each is a 40+ line `case result of Ok (Success ...) | Ok (ApiFailure ...) | Ok (HttpError ...) | Err httpErr ->` that differs only in the success payload and (occasionally) the snapshot push.
   → fix in **PR-2b** (folded into the Net split).

6. **Editor mutations are O(n²) in section size** (Medium, SUSPECTED) —
   `sangeet-core/src/main/scala/com/varpas/sangeet/core/editor/CompositionEditor.scala:36-74`. Every insert/delete does `section.events.zipWithIndex.collect` over all events, then a second `.map` to shift positions. Long compositions (think hundreds of beats) plus debounced auto-save plus full re-layout per keystroke could compound; profile before fixing.
   → file as **known debt** (Plan 19+, deferred — needs profiler data).

7. **Web HTTP errors surface only as a status-bar string** (High) —
   `sangeet-web/src/State/Update.elm:2697-2707` (handleApiResult) and the duplicate 4× pattern at 3186-3434. The user sees one line in the log feed; no retry, no error code, no link to detail. Drive-listing failures are logged as `statusFailedToParseDriveFolderListing` with the underlying decode error discarded (`Err _ -> ...`).
   → fix in **PR-3d** (error-handling pass).

8. **Three parallel matches on `ApiError` in `ErrorMapping.scala`** (Medium) —
   `sangeet-server/src/main/scala/com/varpas/sangeet/server/ErrorMapping.scala:10-65` has three exhaustive matches on the same 17-case ADT for status code, error code string, and message. Adding a new `ApiError` case requires editing all three; the compiler will flag missing cases but the duplication is verbose.
   → file as **known debt**; cheap to fix as a single method returning a 3-tuple.

9. **`MainApp.scala` `start()` is a 545-line giant** (Medium) —
   `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/MainApp.scala:51-595` contains splash, crash capture, analytics init, tab manager, file browser, toolbar wiring, vertical/horizontal split panes, panel-collapse state, ~70 lines of scene-level keyboard accelerators, config save/restore, exit handlers — all inline in `start()`. Hard to test, hard to skim.
   → file as **known debt** (Plan 19 candidate: `MainApp` modularization).

10. **`NewCompositionDialog.scala` mixes 12 form fields, raag-autofill, validation, and FileChooser dispatch in a 486-line `show()`** (Medium) —
    `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/NewCompositionDialog.scala:42-486`. Web has a structurally identical 339-line `View/Dialogs/NewComposition.elm`. The form-state shape is duplicated as `NewDialogForm` in `State/Model.elm:110-130`. Plan-17 PR-3 (bugs 5/6/7) had to make synchronized changes across all three files.
    → fix in **PR-2c** (dialog scaffolding) + **PR-2d** (shared form spec).

## Detailed findings

### Duplication

#### D1 — Fast-typing grouping state machine duplicated end-to-end

- **Locations**:
  - `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/EditorKeyHandler.scala:23-46, 103-168, 199-262`
  - `sangeet-web/src/State/Update.elm:55-60, 2085-2202`
  - Web grouping state in `sangeet-web/src/State/Model.elm:197-222`
- **Evidence**: both platforms define `GroupingState { notes, lastTypedTime / startTime, beat, cycle, nextCursor / nextBeat+nextCycle+nextSubIndex }`, both check `now - last < 500 && notes.size < 4 && cursorMatchesGrouping`, and both do undo-and-replay via `insertSwarGroup`. The constants are even copied: `private val fastTypeThresholdMs = 500L` in Scala vs `groupingThresholdMs = 500` in Elm.
- **Impact**: Plan-17 PR-1 (bug 4 — cursor-drift between keystrokes) had to land the `cursorMatchesGrouping` invariant in both `EditorKeyHandler.scala` and `Update.elm` independently; equivalent reviews and tests in both. The desktop path additionally has its OWN internal duplication: `typeCharTimed` (debug-console path, lines 103-168) and `handleKeyTyped` (interactive path, lines 199-262) are 95% the same code with subtle differences (statusBar logging, message strings).
- **Suggested fix**: extract a pure `GroupingFSM` in `sangeet-core/editor/` with two operations: `attempt(state, now, note, observedCursor) → (newState, action)` where `action` is `StartNew | Extend(group) | Reset`. Both Scala and Elm wrap it with their own UI glue. The Scala internal duplication of `typeCharTimed` vs `handleKeyTyped` collapses into one call to the FSM.

#### D2 — Dialog scaffolding repeated across 7 ScalaFX dialogs

- **Locations**: `sangeet-desktop/.../dialog/AboutDialog.scala:122-127`, `SupportDialog.scala:104-108`, `CommandPaletteDialog.scala:75-81`, `BugReportDialog.scala:90-94`, `CrashRecoveryDialog.scala:137-143`, `KeyboardCheatSheetDialog.scala:192-197`, `NewCompositionDialog.scala:42-47` (uses `javafx.scene.control.Dialog` instead, but the wiring is similar)
- **Evidence**:
  ```scala
  val dialogStage = new Stage:
    initStyle(StageStyle.Utility)
    initModality(Modality.WindowModal)
    title = ...
    scene = new Scene: ...
  ```
  Plus `openInBrowser` is reproduced verbatim in `AboutDialog.scala:21-23`, `SupportDialog.scala:21-23`, and a near-identical version in `BugReportDialog.scala:160`.
- **Impact**: each new dialog re-derives the same boilerplate; a global change to dialog modality, scene padding, or theme handling has to touch 7 files.
- **Suggested fix**: extract `dialog.DialogFrame.modalScene(title, owner, content)` + `dialog.LinkHelpers.openInBrowser`. Cuts each dialog by ~15 lines and centralizes any future styling.

#### D3 — Modal-overlay markup repeated across 10 Elm dialogs

- **Locations**: every file under `sangeet-web/src/View/Dialogs/` (e.g. `ClearSection.elm:15-29`, `DuplicateTab.elm`, `UnsavedChanges.elm`, `About.elm`, `Support.elm`, etc.)
- **Evidence**: each uses the same `div [ class "modal-overlay" ] [ div [ class "modal-dialog modal-X" ] [ h2 [ class "modal-title" ] [ text title ], div [ class "modal-body" ] body, div [ class "modal-footer" ] footer ] ]` skeleton.
- **Impact**: small but real — adding ESC-to-close, click-outside-to-close, or focus-trap behaviour means editing 10 files.
- **Suggested fix**: extract `View.Dialogs.Frame.modal : String -> List (Html Msg) -> List (Html Msg) -> Html Msg` in a new `View/Dialogs/Frame.elm`. Each dialog body shrinks by 4-6 lines and behavioural changes localize.

#### D4 — Debug-bridge async response handlers (4 near-identical 40-line case matches)

- **Locations**: `sangeet-web/src/State/Update.elm:3186-3279` (`handleDebugResetReceived`), `3282-3323` (`handleDebugDumpReceived`), `3326-3367` (`handleDebugExportReceived`), `3382-3433` (`handleDebugEditorResultReceived`)
- **Evidence**: all four share the exact pattern:
  ```elm
  case result of
      Ok (Success payload) -> ...
      Ok (ApiFailure err)  -> respond ... Just ("API error: " ++ err.message)
      Ok (HttpError httpErr) -> respond ... Just ("HTTP error: " ++ httpErrorToString httpErr)
      Err httpErr -> respond ... Just ("HTTP error: " ++ httpErrorToString httpErr)
  ```
  Only the success branch differs.
- **Impact**: PR-2b (debug-bridge re-tests after each plan-17 PR) had to edit these in lockstep. Adding a new debug command means cloning a 4-clause case match.
- **Suggested fix**: extract `respondDebug : String -> (a -> Encode.Value) -> Result Http.Error (ApiResult a) -> Cmd Msg`. Each handler shrinks to one line.

#### D5 — Three parallel pattern matches on `ApiError`

- **Locations**: `sangeet-server/src/main/scala/com/varpas/sangeet/server/ErrorMapping.scala:10-28` (`toStatusCode`), `30-46` (`toErrorCode`), `48-65` (`toMessage`)
- **Evidence**: each of 17 `ApiError` variants is mentioned three times, once in each fn.
- **Impact**: adding a new error variant requires editing all three; the compiler will catch omissions but the maintenance cost compounds.
- **Suggested fix**: collapse into a single `def describe(error: ApiError): (StatusCode, String, String)` and have `toStatusCode`/`toErrorCode`/`toMessage` project from it; or define each variant's mapping once as a case-class.

#### D6 — Form-state shapes triplicated for "New Composition" dialog

- **Locations**: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/NewCompositionDialog.scala:15-29` (`Result` case class), `sangeet-web/src/State/Model.elm:110-130` (`NewDialogForm`), `sangeet-web/src/View/Dialogs/NewComposition.elm:1-339` (the corresponding view)
- **Evidence**: 14 fields per shape — title, compositionType, raag, taalName, laya, script, taanCount, showStrokeLine/Sahitya, filePath, gat/antara/taanStartingBeat — all spelled three different ways.
- **Impact**: bug 5/6/7 (Plan-17 PR-3) required synchronized changes across desktop + Elm view + Elm state.
- **Suggested fix**: define `NewCompositionSpec` in `sangeet-core/api/` as the canonical shape; Elm regenerates a mirror from the OpenAPI schema or via a code-gen step; desktop reads it directly. Out of scope for PR-2c (dialog frame only). File as known debt.

#### D7 — HTTP-route boilerplate (parseEditorInput + handleResult)

- **Locations**: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/EditorRoutes.scala:18-161`, `CursorRoutes.scala:16-66`, `OrnamentRoutes.scala`, `SectionRoutes.scala`
- **Evidence**: every endpoint is `EditorEndpoints.X.serverLogic { body => val c = body.hcursor; handleResult(for ... yield ...)(encoder) }`. 13 endpoints in EditorRoutes alone follow this template verbatim.
- **Impact**: low day-to-day cost (the boilerplate is mechanical) but adds noise when reading a route. A typo in `parseField[Note](c, "note")` cannot be caught by the compiler.
- **Suggested fix**: not a Plan-18 priority — file as known debt. A future Tapir-tighter integration could push the JSON parsing into the endpoint type and remove the manual `parseField` calls.

### Modularity

#### M1 — `Update.elm` (3627 lines, 87 Msg variants, one mega case)

- **Title**: `Update.elm` is a god-module
- **Locations**: `sangeet-web/src/State/Update.elm` (entire file)
- **Evidence**: top-level `case msg of` runs lines 165-1428; broad responsibility areas inferred from Msg names: keyboard (~5 branches), file ops (~5), section ops (~9), new-composition dialog (~22), properties dialog (~6), other dialogs (~14), command palette (~5), bug report (~6), API responses (~17), tab management (~3), Drive (~10), debug bridge (~6), unsaved-changes (~3), config (~1), autosave (~1). 87 branches.
- **Impact**: any plan-17 PR that touched Update.elm risked merge conflicts with concurrent PRs; reading the keyboard-handler logic requires scrolling past 2500 lines of dialog state.
- **Suggested fix** (PR-2b): split into
  - `State/Update/Editor.elm` — KeyPressed, CanvasClicked, Undo/Redo, SwarInput, OrnamentMode, GroupingState handling (~600 lines)
  - `State/Update/File.elm` — Open/Save/SaveFileAs/ExportHtml, FileSelected/FileLoaded, Drive (~700 lines)
  - `State/Update/Tab.elm` — SwitchTab/CloseTab/NewTab, DuplicateTab*, UnsavedChanges*, Autosave (~500 lines)
  - `State/Update/Dialog.elm` — NewDialog* (~22 branches), PropsDialog* (~6), About/Support/Cheat/Palette/BugReport (~14 branches) (~900 lines)
  - `State/Update/Section.elm` — SelectSection, AddSection, RemoveSection, MoveSection*, ClearSection (~400 lines)
  - `State/Update/Net.elm` — handleApiResult, GotEditorResult, GotCursor, GotLayout, GotTaals/Raags/Colors/Scripts, GotSerialized/Parsed, GotSectionAdd/Remove/Clear/Reorder, debug bridge async response handlers (~700 lines)
  - Top-level `Update.elm` becomes a thin dispatcher (~150 lines).

#### M2 — `MainApp.scala` `start()` mixes 9 responsibilities

- **Title**: `MainApp` startup logic is too dense to skim
- **Locations**: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/MainApp.scala:51-595`
- **Evidence**: 545 lines of inline code in one method. Concerns: splash screen, AppLogger init, CrashCapture install, EventLogger lifecycle event, PostHog analytics init, crash-recovery dialog dispatch, tab manager / file browser construction, debug console start, toolbar build, split-pane layout, panel-collapse state machine (`leftPanelExpanded`, `bottomPanelExpanded` with collapse/expand fns), scene-level keyboard accelerators (~70 lines, ~25 shortcuts), AppConfig save/restore, exit handlers.
- **Impact**: behaviour drift between desktop and web (where same accelerators are bound differently) is hard to detect because the desktop side isn't testable in isolation.
- **Suggested fix**: file as known debt (out of Plan-18 scope). Future plan extracts `app.Startup`, `app.SceneKeyBindings`, `app.PanelStateMachine`, `app.ConfigPersistence` from `MainApp`.

#### M3 — `Model` record has 43 top-level fields

- **Title**: web `Model` is a single bag of all UI state
- **Locations**: `sangeet-web/src/State/Model.elm:291-334`
- **Evidence**: editor state, dialog flags + form state for 8 dialogs, tab list, Drive state + folders, panel widths, palette query, pending API call, debug-bridge state, theme — all in one record.
- **Impact**: every test that constructs a Model needs to fill in all 43 fields. Any new dialog adds 2-3 fields and a default. Touching `Model.elm` becomes a merge-conflict magnet.
- **Suggested fix**: group into `Model.Dialogs`, `Model.Tabs`, `Model.Drive`, `Model.Palette` sub-records. Reduces top-level field count and clusters related fields together. File as known debt — orthogonal to PR-2b's Update split and best done after to avoid touching the same surface twice.

#### M4 — `NewCompositionDialog.scala` (486 lines, one giant `show()`)

- **Title**: NewCompositionDialog mixes 12 form fields with raag-autofill, validation, and FileChooser dispatch
- **Locations**: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/NewCompositionDialog.scala:42-486`
- **Evidence**: `show()` constructs TextField, ComboBox, Spinner, CheckBox controls for 12 fields, installs key filters via `installSwarFilter`, hooks up `fillRaagDetails`, `updateStartingBeatRange`, and `validate` callbacks, then composes a `GridPane`, wires OK/Cancel actions, and returns an `Option[Result]`.
- **Impact**: matches finding D6 in the duplication section. Together they motivate (a) extracting dialog scaffolding (PR-2c) and (b) deferred work on a canonical `NewCompositionSpec`.
- **Suggested fix**: PR-2c lifts scene/stage scaffolding into `DialogFrame`. Field-by-field decomposition into `formGroup(label, field)` helpers is also in scope. Form-spec unification is out of scope (D6).

#### M5 — `EditorKeyHandler.scala` mixes mode switching, grouping, dispatch, and per-mode side effects

- **Title**: `EditorKeyHandler` is a 565-line state-machine + JavaFX event handler
- **Locations**: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/EditorKeyHandler.scala:1-566`
- **Evidence**: `EditMode` enum, `GroupingState` case class, three event installers (`KEY_PRESSED`, `KEY_TYPED`, `KEY_PRESSED` again), `typeCharTimed` (debug-console entry point), `handleKeyTyped` (interactive entry point with FastTyping logic), `handleKeyPressed` (Ctrl/meta/Shift+Right/Left/Up/Down/Tab/etc.). The "what mode am I in" + "what character did I see" + "should I extend grouping" + "what UI effect do I trigger" concerns are interleaved.
- **Impact**: bug 4 (cursor drift cancelling grouping) had to be fixed in `typeCharTimed` AND `handleKeyTyped` separately.
- **Suggested fix**: PR-2d extracts the grouping FSM (D1). After that, `EditorKeyHandler` becomes "translate JavaFX KeyEvent → action; dispatch action to KeyHandler". The two entry points (debug + interactive) share the same dispatch path.

### Error handling

#### E1 — Silent `Result` discards in web HTTP error paths

- **Locations**: `sangeet-web/src/State/Update.elm:2971` (drive folder), `:2987` (drive file content), `:3143` (config load)
- **Evidence**:
  ```elm
  Err _ ->
      ( addLog UiStrings.statusFailedToParseDriveFolderListing model, Cmd.none )
  ```
  The underlying Decode.Error is discarded. The user sees the canned message; the developer trying to debug gets nothing.
- **Impact**: if a port produces a malformed payload (e.g., after a Drive API schema change), the developer has zero signal. Status-bar text is the only diagnostic.
- **Suggested fix**: PR-3d log the error via `console.error` (via a port if needed) and include the error string in `addLog`.

#### E2 — `case _ => ()` in FileBrowserPanel swallows menu actions

- **Locations**: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/FileBrowserPanel.scala:257, 273, 294, 330`
- **Evidence**: four sites where a menu action's matched branches end with `case _ => ()`. Could be silently ignoring a user click.
- **Impact**: hard to say without context; needs read of surrounding code to determine if the catch-all is intentional ("user clicked Cancel") or a missed branch. Audit PR-3d should review.
- **Suggested fix**: PR-3d. Convert each to an exhaustive `match` (Scala will then complain at compile time if a new branch is added).

#### E3 — `catch case _: Exception => ()` silently swallows browser-launch failures

- **Locations**: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/AboutDialog.scala:23`, `SupportDialog.scala:23`, `BugReportDialog.scala` (similar pattern)
- **Evidence**:
  ```scala
  private def openInBrowser(url: String): Unit =
    try java.awt.Desktop.getDesktop.browse(java.net.URI.create(url))
    catch case _: Exception => ()
  ```
- **Impact**: on systems without `java.awt.Desktop` support (some Linux distros without xdg-open), the user clicks a link and nothing happens. Zero feedback.
- **Suggested fix**: PR-3d. Log via `AppLogger.info` and surface "couldn't open browser: copy URL?" prompt as a fallback.

#### E4 — `.toOption.getOrElse(default)` in CrashRecoveryDialog silently masks malformed crash sentinels

- **Locations**: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/CrashRecoveryDialog.scala:57-61, 203-207`
- **Evidence**:
  ```scala
  val exception = c.get[String]("exception").toOption.getOrElse("unknown exception")
  val stackTrace = c.get[String]("stackTrace").toOption.getOrElse("(no stack trace)")
  ```
- **Impact**: the crash sentinel is the most important data we have when investigating a crash; silently filling in `"unknown"` defeats the purpose. If the schema changes, the developer sees clean-looking but content-free recovery dialogs.
- **Suggested fix**: PR-3d. If the field is missing, treat the sentinel as corrupt and log it. The fallback display is fine for the user-visible dialog but should not be the only path.

#### E5 — `BugReportRoutes` swallows GitHub-issue fiber crashes

- **Locations**: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/BugReportRoutes.scala:38`
- **Evidence**:
  ```scala
  .handleErrorWith(t => IO.println(s"[bug-report] GitHub issue fiber crashed: ${t.getMessage}"))
  ```
  Goes to stdout only; no metric, no PagerDuty signal, no retry.
- **Impact**: if the GitHub API is rate-limited or down, bug reports silently fail to file issues. The user sees "thank you, your report was received" because the upload-to-GCS succeeded; we lose the GitHub-issue side effect with no visibility.
- **Suggested fix**: PR-3a wires a Micrometer counter `bug_report.github_issue.failures_total{reason=…}`. PR-3d additionally logs structured details (issue title, error class).

#### E6 — HTTP errors are user-invisible

- **Locations**: `sangeet-web/src/State/Update.elm:2697-2707`
- **Evidence**: `handleApiResult` for every API call ends in `addLog ... statusHttpError` — i.e., a status-bar string. No toast, no modal, no retry button, no error-code surfaced. A user typing rapidly during a 500ms server hiccup will see notes silently fail to appear; the only signal is a status-bar message they may have already scrolled past.
- **Impact**: this is the highest-bandwidth source of user-confusion bugs (Plan-17 bug 8 was a related symptom — read-only mode messaging).
- **Suggested fix**: PR-3c adds a top-level JS error boundary that catches uncaught exceptions and shows a banner. PR-3d additionally adds visible toast notifications for transient HTTP errors with a retry affordance.

**Swallowed error sites table**

| File                                                | Line(s)            | Discarded type           | Fix PR |
| --------------------------------------------------- | ------------------ | ------------------------ | ------ |
| Update.elm (Drive folder)                           | 2971               | `Decode.Error`           | PR-3d  |
| Update.elm (Drive file content)                     | 2987               | `Decode.Error`           | PR-3d  |
| Update.elm (config load)                            | 3143               | `Decode.Error`           | PR-3d  |
| Update.elm (handleApiResult, all variants)          | 2697-2707          | `Http.Error`, `ApiError` | PR-3d  |
| FileBrowserPanel.scala                              | 257, 273, 294, 330 | menu action `_`          | PR-3d  |
| AboutDialog.scala / SupportDialog.scala             | :23                | `java.awt` Exception     | PR-3d  |
| CrashRecoveryDialog.scala                           | 57-61, 203-207     | `circe DecodingFailure`  | PR-3d  |
| BugReportRoutes.scala (GitHub-issue fiber)          | 38                 | any `Throwable`          | PR-3a  |

### Debuggability + observability

#### O1 — Server has HTTP metrics; desktop and web do not have product-event metrics

- **Locations**: `sangeet-server/src/main/scala/com/varpas/sangeet/server/metrics/HttpMetrics.scala`, `MetricsRegistry.scala`
- **Evidence**: server reports `tapir.request.active`, `tapir.request.total`, `tapir.request.duration` (with `path`/`method`/`status_code` labels) plus JVM/process bindings. Both go to Prometheus scrape endpoint and Stackdriver push. Desktop has a single PostHog client (`PostHogClient.scala`) that fires a few lifecycle events (`AppStarted`, key-input counter aggregated by `SessionStats`). Web has no app-level metrics; only rrweb session replays.
- **Impact**: Plan-17 reproducing bug 12 (web tab focus) required guessing which user interaction triggered it. A metric like `web.tab_switch.total` with `from`/`to`/`source` labels would have shown the trigger rate.
- **Suggested fix**:
  - PR-3a: wire app-event metrics on server: counter for bug-report submissions, fiber-crash counter for GitHub issue path.
  - PR-3b: add app-event metrics on desktop (PostHog already wired; add counters for `editor.swar_insert`, `editor.delete`, `tab.switch`, `dialog.open` with tab-count/dialog-name labels). Web wires equivalents via PostHog frontend SDK.

#### O2 — `AppLogger` is desktop-only; web has no persistent logger

- **Locations**: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/AppLogger.scala` (52 lines)
- **Evidence**: desktop writes to `/tmp/sangeet-notes-editor.log` (rolled at 20MB × 5 files). Web only has `console.log/warn/error` in `ports.js` (12 sites) and `addLog` to in-memory status-bar list (`addLog`, capped at 100 entries via `List.take 100 ...`).
- **Impact**: web has no equivalent of the "I can email you my .log file" diagnostic loop that exists for desktop bug reports.
- **Suggested fix**: file as known debt — the rrweb replay buffer + Report-a-Bug GCS upload already provide a workable web-side path. Not Plan-18 scope.

#### O3 — Mutations are not logged with enough context to reconstruct a bug session

- **Locations**: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/diagnostics/EventLogger.scala`
- **Evidence**: ring buffer captures `Key(code, modifiers)` and `Lifecycle(kind, detail)` only. No editor-state events (`insertSwar(note, cursor)`, `changeStartingBeat(sectionIdx, oldBeat, newBeat)`, `cutSelection(rangeStart, rangeEnd)`, etc.).
- **Impact**: Plan-17 bug 14 (section clear after add) — to reproduce, an investigator has to read the key-event stream and infer what was on screen. Direct editor events would have made the bug obvious.
- **Suggested fix**: extend `EventLogger.LoggedEvent` enum with `EditorMutation(name, summary)` cases. Wire EditorKeyHandler / DebugCommandHandler / Toolbar action handlers to record entries. File as known debt — out of Plan-18 scope.

#### O4 — No top-level JS error boundary on web

- **Locations**: `sangeet-web/public/index.html`, `sangeet-web/public/ports.js`
- **Evidence**: no `window.onerror = ...`, no `window.addEventListener('unhandledrejection', ...)`. Errors during port callbacks (Drive auth, rrweb start, FSA save) only reach `console.error` (e.g. `ports.js:158, 209, 219, 232, 236, 264, 299, 903, 906`).
- **Impact**: an unhandled exception during a port callback bricks subsequent calls silently. Reported web bugs that "I clicked X and nothing happened" trace to this class of failure.
- **Suggested fix**: PR-3c. Install `window.onerror` + `unhandledrejection` listeners that POST to a `/api/v1/error-report` endpoint AND surface a "something went wrong" banner. Reuse the bug-report POST shape.

#### O5 — Per-keystroke debug spam noise vs production-useful tracing

- **Locations**: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/EditorPane.scala:354`, `EditorKeyHandler.scala:172, 275`
- **Evidence**: `AppLogger.debug("redraw()")`, `AppLogger.debug(s"keyTyped: char='$ch'...")` — fires on every keystroke when `debugEnabled = true`. No structured fields, no correlation id linking a keystroke to its downstream redraw.
- **Impact**: turning debug on produces a wall of text that's hard to grep. Not a blocker; file as known debt.
- **Suggested fix**: structured logging with a session-scoped correlation id. Out of Plan-18 scope.

### Performance (suspected)

#### P1 — `CompositionEditor` editor mutations are O(n) per call, potentially O(n²) for batched inserts (SUSPECTED)

- **Locations**: `sangeet-core/src/main/scala/com/varpas/sangeet/core/editor/CompositionEditor.scala:36-74, 207-243`
- **Evidence**: `removeEventAt` does `section.events.zipWithIndex.collect` (O(n)) + `section.events.patch` (O(n)) + `.zipWithIndex.map` (O(n)). `removeGroupAt` similarly. `pasteEvents` does `section.events.map` to shift forward + a `++ rebased` + `.sortBy(_.position)` (O(n log n)). Each interactive insert via `EditorApi.insertSwar → editor.addEvent → section.events :+ event` is O(n) due to immutable list append.
- **Reasoning**: most user compositions are short (8-32 cycles × 16 beats = 128-512 events) so this is moot at typical sizes. A long taan (50 cycles × 16 beats = 800 events) hits 800² = 640k operations per redraw if a layout recompute is triggered per keystroke.
- **Suggested fix**: SUSPECTED — recommend profiling before optimizing. File as known debt. Cheap mitigation if it becomes a problem: back `section.events` with a Vector or a custom sorted structure indexed by `BeatPosition`.

#### P2 — Layout recomputes on every keystroke (SUSPECTED)

- **Locations**:
  - Desktop: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/EditorPane.scala:55-64, 353-397` — `redraw()` calls `getGrids(comp)` which checks cache by reference equality (`cached eq comp`); any composition mutation invalidates the cache and triggers full `GridLayout.layoutAll`. `redraw()` also fires every 530ms from the blink timer (cursor-only redraw, layout cache hit).
  - Web: `sangeet-web/src/State/Update.elm:2835-2841` — `requestLayout` POSTs the entire composition JSON to the server for every layout-triggering action (section change, taal change, every editor mutation that needs new grids).
- **Evidence**: 19 call sites of `requestLayout` in Update.elm; even small mutations like `SelectSection`, `AddSection`, `RemoveSection`, `ClearSection`, etc., serialize the full composition over the wire.
- **Reasoning**: SUSPECTED — for short compositions, `GridLayout.layoutAll` is microsecond-scale and JSON-serializing 500 events is sub-millisecond. For long compositions, JSON serialization on every keystroke dominates the editor-feel latency.
- **Suggested fix**: file as known debt. Profile first; if confirmed, options include (a) server-side cache keyed by composition hash, (b) layout-delta API, or (c) move layout back to the Elm client for small compositions and call server only for HTML export.

#### P3 — Blink timer triggers a full canvas re-render every 530ms on desktop (SUSPECTED)

- **Locations**: `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/EditorPane.scala:76-86, 353-397`
- **Evidence**: `blinkTimeline` calls `redraw()` every 530ms; `redraw` re-runs `CanvasRendererFX.render` on the whole canvas (and reuses the cached grids). Even with caching, that's a full canvas paint twice a second.
- **Reasoning**: SUSPECTED — JavaFX canvas paints are GPU-accelerated and a 1000×400 canvas should be sub-millisecond, but on lower-end hardware (older Linux laptops without HW accel) the constant repaint may show in CPU graphs. Could also affect battery.
- **Suggested fix**: file as known debt. Better solution is to draw the cursor as a separate JavaFX `Rectangle` node overlaid on the canvas — blink it by toggling visibility, not by re-painting the whole canvas.

#### P4 — `List.length gs.notes < 4` and `(model.composition).sections |> List.drop idx |> List.head` in hot paths

- **Locations**: `sangeet-web/src/State/Update.elm:2131` (per-keystroke check), `:243-245, :675-676` (per-section-select)
- **Evidence**:
  ```elm
  if now - gs.startTime < groupingThresholdMs && List.length gs.notes < 4 && cursorStillAlignedWithGroup then
  ```
  `List.length` is O(n) in Elm. `gs.notes` is bounded by 4 here so the actual cost is tiny — but the idiom is wrong and lends itself to copy-paste into hot paths where n isn't bounded.
- **Reasoning**: cheap to fix; substituting `List.take 4 gs.notes |> List.length` with a `< 4` Maybe equivalent or `List.isEmpty <| List.drop 3 gs.notes` would be more idiomatic. Not a real perf issue here.
- **Suggested fix**: file as known debt; a cleanup-pass PR could grep these out across Update.elm.

## Committed for fix in Plan 18

- **PR-2b** (split Update.elm into State/Update/{Editor,File,Tab,Dialog,Section,Net}.elm) addresses: M1, D4
- **PR-2c** (dialog frame unification on desktop + web) addresses: D2, D3, partial M4
- **PR-2d** (shared logic dedup — extract grouping FSM into sangeet-core) addresses: D1, partial M5
- **PR-3a** (HTTP metrics for non-Tapir paths — server) addresses: E5 (GitHub-issue fiber counter), O1 (server-side product metrics)
- **PR-3b** (app metrics on desktop + web) addresses: O1 (client-side product metrics)
- **PR-3c** (JS error capture on web) addresses: O4, partial E6
- **PR-3d** (error-handling pass) addresses: E1, E2, E3, E4, E6

## Filed as known debt (for future plans)

| Finding | Severity | Suggested follow-up |
| ------- | -------- | ------------------- |
| D5 — `ErrorMapping.scala` 3-parallel matches | Medium | Plan-19 cleanup (single `describe` fn) |
| D6 — `NewComposition` form-state triplicated | Medium | Plan-19 — canonical `NewCompositionSpec` in sangeet-core |
| D7 — HTTP-route boilerplate | Low | Plan-20+ — Tapir-tighter endpoint integration |
| M2 — `MainApp.scala` `start()` is 545 lines | Medium | Plan-19 — `app.Startup`, `app.SceneKeyBindings`, etc. |
| M3 — `Model` record has 43 fields | Medium | Plan-19 — group into `Model.Dialogs`, `Model.Tabs`, `Model.Drive`, `Model.Palette` (do after PR-2b lands) |
| O2 — web has no persistent logger | Low | Probably out-of-scope until web becomes the primary platform |
| O3 — EventLogger doesn't record editor mutations | Medium | Plan-19 — extend `LoggedEvent` enum |
| O5 — per-keystroke debug spam | Low | Plan-20+ — structured logging with session correlation id |
| P1 — O(n) per editor mutation (suspected) | Medium | Profile first; Plan-19+ if confirmed |
| P2 — layout recompute on every keystroke (suspected) | Medium | Profile first; cache + delta API if confirmed |
| P3 — blink timer triggers full canvas repaint (suspected) | Low | Refactor cursor to overlay node |
| P4 — `List.length` in hot paths | Low | Cleanup-pass PR |

## Methodology notes

**Tools used**: `grep -rn` for code-smell pattern searches (silent error sites, duplicated state machines, dialog scaffolding), `wc -l` for file-size sweep, `find ... -size +30k` for big-file discovery, `Read` for inspecting specific suspect files end-to-end (`Update.elm`, `EditorKeyHandler.scala`, `CompositionEditor.scala`, `NewCompositionDialog.scala`, `EditorRoutes.scala`, `ErrorMapping.scala`, the 7 desktop dialogs, the 10 Elm dialogs), `git log` for commit-history correlation between Plan-17 fixes and the files they touched.

**What was NOT checked**:

- No profilers were run (CPU/memory hotspots are flagged as SUSPECTED).
- No runtime experiments (e.g., synthetic load against the server to validate `tapir.request.duration` shape).
- The Elm bundle size was not measured.
- Test coverage gaps (`scoverage` report not opened — coverage is already enforced at 80% in CI).
- `mcp-servers/sangeet-debug-console/` was not inspected in depth (it's 21 pytest cases over text→JSON mapping; out of the main editor's quality envelope).
- Android module — does not exist yet (planned, not built).
- Build / sbt config — not in scope; recent Plan-13 work touched it.

**Areas that would benefit from deeper investigation**:

- **Performance under load**: synthesize a 100-cycle Teentaal composition (1600 events) and measure (a) interactive insert latency from keystroke to render, (b) `requestLayout` round-trip from server, (c) `redraw()` time. Confirm or refute P1/P2/P3.
- **Web error visibility**: instrument the existing rrweb pipeline to surface unhandled JS errors as a top-level rrweb event so replays show them inline. Complements PR-3c.
- **Cross-platform parity drift**: the `parity-inventory.md` exists but a periodic re-run of the cross-platform-parity-checker subagent against Update.elm + EditorKeyHandler.scala may surface silent drift before the next bug cluster forms.
