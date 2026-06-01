# Comprehensive Web App Test Suite

## Context

The desktop app has 95+ TCP integration tests (DebugConsoleTcpSpec) covering all editor functionality. The web app (Elm + Tapir REST) has zero Elm tests and only 9 server tests out of 43 endpoints. The user wants full test coverage across three layers — matching or exceeding desktop coverage. Total target: ~364 tests across Elm unit/integration, server API, and browser E2E.

## Layer 1: Elm Program Tests (~159 tests)

### Setup

Add to `sangeet-web/elm.json` test-dependencies:
- `elm-explorations/test` (>= 2.2.0)
- `avh4/elm-program-test` (>= 3.6.3)

Add to `sangeet-web/package.json` devDependencies:
- `elm-test` (^0.19.1-revision12)
- npm scripts: `"test": "elm-test"`, `"test:watch": "elm-test --watch"`

### Test Files

```
sangeet-web/tests/
  TestHelpers.elm             -- Shared fixtures (defaultFlags, mockResponses, defaultModel)
  KeyHandlerTest.elm          -- 30 tests: pure mapKeyToAction for all key combos
  OrnamentModeTest.elm        -- 18 tests: state machine transitions for all ornament types
  UndoHistoryTest.elm         -- 12 tests: push/undo/redo/canUndo/canRedo/trim
  UpdateBasicTest.elm         -- 15 tests: model-only transitions (toggles, dialogs, blink, no-op)
  UpdateEditorTest.elm        -- 14 tests: swar/rest/sustain/delete dispatch, API response handling
  UpdateCursorTest.elm        -- 8 tests: arrow/tab/subdivision/octave/click dispatch
  UpdateSectionTest.elm       -- 6 tests: add/remove/reorder/rename/select
  UpdateOrnamentTest.elm      -- 12 tests: ornament mode entry, note collection, API dispatch
  UpdatePlaybackTest.elm      -- 6 tests: play/pause/stop state, port dispatch
  UpdateDialogTest.elm        -- 6 tests: new/props dialog submit, form init
  UpdateFileTest.elm          -- 6 tests: open/save/load/export port+API dispatch
  GroupingLogicTest.elm        -- 10 tests: 500ms threshold, 4-note max, undo-replay, clear
  ApiResponseTest.elm         -- 8 tests: Success/ApiFailure/HttpError handling
  IntegrationFlowTest.elm     -- 8 tests: multi-step workflows (type-undo-redo, section switch, etc.)
```

### Testing Strategy

- **Pure function tests** (KeyHandler, OrnamentMode, UndoHistory): direct function calls, no HTTP
- **Update tests**: call `update msg model` directly, inspect `(Model, Cmd Msg)` — avoids elm-program-test HTTP complexity
- **Grouping tests**: send `GotSwarKeyTime` messages directly with controlled `Time.millisToPosix` values to test the 500ms threshold precisely
- **Port tests**: verify model state changes (e.g., `playbackState == Playing`); actual port behavior tested in E2E layer

### Representative Pattern (KeyHandlerTest.elm)
```elm
test "s maps to Sa Shuddha" <|
    \_ ->
        mapKeyToAction "s" False False False
            |> Expect.equal (SwarInput Sa Shuddha)
```

### Representative Pattern (UpdateEditorTest.elm)
```elm
test "pressing '-' key sets pendingApiCall to True" <|
    \_ ->
        let
            model = Model.init "http://test-api"
            ( newModel, _ ) = update (KeyPressed "-" False False False) model
        in
        Expect.equal True newModel.pendingApiCall
```

---

## Layer 2: Server API Integration Tests (~121 tests)

### Setup

No new dependencies — ScalaTest + http4s already in build.sbt. Tests in existing directory.

### Test Files

```
sangeet-server/src/test/scala/com/varpas/sangeet/server/
  TestFixtures.scala          -- NEW: shared helpers (minimalComposition, minimalCursor, editorInputJson)
  HealthCheckSpec.scala       -- existing: 1 test
  ReferenceRoutesSpec.scala   -- expand from 6 to 12: add colors, scripts, specific taal/raag
  EditorRoutesSpec.scala      -- expand from 5 to 22: all 7 editor endpoints, chained ops
  CursorRoutesSpec.scala      -- NEW: 14 tests (next/prev beat, subdivision, octave, move-to)
  SectionRoutesSpec.scala     -- NEW: 12 tests (add/remove/rename/reorder, edge cases)
  OrnamentRoutesSpec.scala    -- NEW: 18 tests (gamak/andolan/gitkari, kan/sparsh/ghaseet, meend, krintan, murki, zamzama)
  StrokeRoutesSpec.scala      -- NEW: 8 tests (da/ra/chikari/jod/clear, edge cases)
  CompositionRoutesSpec.scala -- NEW: 10 tests (create gat/bandish/palta, serialize/parse roundtrip)
  LayoutRoutesSpec.scala      -- NEW: 6 tests (empty/populated/multi-section, custom config)
  ExportRoutesSpec.scala      -- NEW: 6 tests (HTML with scripts, PDF bytes, errors)
  PlaybackRoutesSpec.scala    -- NEW: 6 tests (empty/single/multi swar, timing, errors)
  RenderingRoutesSpec.scala   -- NEW: 6 tests (glyph rendering, komal/tivra marks, all scripts)
```

### Test Pattern (follows existing EditorRoutesSpec)
```scala
val routes = Http4sServerInterpreter[IO]().toRoutes(XxxRoutes.all).orNotFound

"POST /api/v1/cursor/next-beat" should "advance cursor beat" in {
  val body = editorInputJson(minimalComposition, minimalCursor)
  val req = Request[IO](Method.POST, uri"/api/v1/cursor/next-beat")
    .withEntity(body.noSpaces)
    .withContentType(headers.`Content-Type`(MediaType.application.json))
  val resp = routes.run(req).unsafeRunSync()
  resp.status shouldBe Status.Ok
  // parse envelope, assert data fields
}
```

### Shared Fixtures (TestFixtures.scala)

Provides `minimalComposition` (Gat + Teentaal + Yaman + 1 empty Sthayi section), `minimalCursor` (beat 0, cycle 0, Madhya), `editorInputJson()` helper, `insertSwarJson()` helper for chained operations.

---

## Layer 3: Browser E2E Tests (~84 tests, Playwright)

### Setup

New `e2e/` directory at project root:
```
e2e/
  package.json              -- @playwright/test dependency
  playwright.config.ts      -- webServer for static file server, globalSetup for health check
  tsconfig.json
  helpers/
    app-page.ts             -- Page Object Model (SangeetPage class)
    global-setup.ts         -- Polls /health until server ready
  tests/
    basic-load.spec.ts          -- 5 tests: page load, toolbar, grid, status, section tabs
    keyboard-input.spec.ts      -- 10 tests: all swar keys, komal/tivra, rest, sustain, octave, subdivision
    cursor-navigation.spec.ts   -- 6 tests: arrow keys, wrap, tab, click, blink
    swar-editing.spec.ts        -- 8 tests: sequences, groups, delete, fast-typing
    section-management.spec.ts  -- 6 tests: add/switch/remove sections, content preservation
    ornament-workflow.spec.ts   -- 8 tests: gamak, andolan, kan, meend, murki workflows, escape cancel
    stroke-editing.spec.ts      -- 5 tests: F2 toggle, da/ra stroke insertion, mode switching
    undo-redo.spec.ts           -- 6 tests: button state, undo/redo via click and Ctrl+Z/Y
    file-operations.spec.ts     -- 4 tests: save/export HTML/PDF download, file open
    dialog-interactions.spec.ts -- 8 tests: new/props/about dialogs, form fill, submit, cancel
    playback-controls.spec.ts   -- 4 tests: play/pause/stop, loop toggle, BPM slider
    script-switching.spec.ts    -- 4 tests: Devanagari/English/Kannada switching
    view-toggles.spec.ts        -- 4 tests: stroke/sahitya/legend toggles
    multi-step-flows.spec.ts    -- 6 tests: full workflows, stress test sequences
```

### Prerequisites

1. **Server running** on port 28080 (`sbt sangeetServer/run`)
2. **Elm compiled** to `sangeet-web/public/elm.js`
3. Playwright starts a static file server for the Elm frontend via `webServer` config
4. `globalSetup` polls `/health` endpoint until server is ready

### Page Object Model (app-page.ts)

`SangeetPage` class provides:
- `goto()` — navigates and waits for initial API calls (taals, raags)
- `pressKey(key)` / `pressKeys([...])` — dispatches keyboard events and waits for API response
- `pressWithModifier(mod, key)` — Ctrl/Shift/Alt combos
- `clickButton(title)` — toolbar button clicks
- `getEditMode()`, `getBeatContent(n)`, `getActiveSection()` — state inspection
- `waitForApi()` — watches `.loading-indicator` appear/disappear

### DOM Selectors

The Elm app uses CSS classes (`.swar-row`, `.beat-cell`, `.cursor-cell`, `.section-tab`, etc.) and toolbar button `title` attributes. **Recommended small change**: add `data-beat` and `data-cycle` attributes to beat cells in `View/GridRenderer.elm` (2 `Html.Attributes.attribute` calls) for reliable E2E targeting.

---

## Summary

| Layer | Tests | Focus |
|-------|-------|-------|
| Elm program tests | 159 | TEA logic, pure functions, state transitions, API dispatch |
| Server API tests | 121 | All 43 endpoints, error handling, chained operations |
| Browser E2E tests | 84 | User-visible behavior, full-stack workflows |
| **Total** | **364** | **2.8x desktop coverage** |

## Challenges and Solutions

| Challenge | Solution |
|-----------|----------|
| Time.now for grouping | Send `GotSwarKeyTime` directly with controlled timestamps |
| Outgoing ports | Test model state; actual port behavior via E2E |
| HTTP in Elm tests | Direct `update` function calls; ProgramTest only for integration flows |
| E2E API waits | `waitForApi()` watches `.loading-indicator`; `waitForResponse()` for URL patterns |
| E2E server lifecycle | `globalSetup` polls `/health`; server started separately or as CI background job |
| DOM selectors | CSS classes + add `data-beat`/`data-cycle` attributes to GridRenderer |

## Implementation Sequence

1. **Phase 1** — Pure Elm tests: KeyHandler, OrnamentMode, UndoHistory (~60 tests)
2. **Phase 2** — Elm update tests: all Update*Test, Grouping, ApiResponse, Integration (~99 tests)
3. **Phase 3** — Server tests: TestFixtures + all new *RoutesSpec files (~109 new tests)
4. **Phase 4** — E2E infrastructure: Playwright config, page object, basic-load smoke test (~5 tests)
5. **Phase 5** — E2E suite: all remaining spec files + `data-beat`/`data-cycle` attributes (~79 tests)
6. **Phase 6** — CI: GitHub Actions workflow, Makefile targets

## Verification

1. `cd sangeet-web && npx elm-test` — all 159 Elm tests pass
2. `sbt sangeetServer/test` — all 121 server tests pass (including existing 12)
3. Start server + compile Elm, then `cd e2e && npx playwright test` — all 84 E2E tests pass
4. `sbt sangeetCore/test` — existing 523 core tests still pass (no regressions)
5. CI workflow runs all layers in parallel (elm-tests + server-tests), then E2E after both pass

## Key Files to Modify

- `sangeet-web/elm.json` — add test-dependencies
- `sangeet-web/package.json` — add elm-test devDependency and test script
- `sangeet-web/src/View/GridRenderer.elm` — add data-beat/data-cycle attributes (4 lines)
- `sangeet-server/src/test/scala/.../` — new test fixtures + 10 spec files
- `e2e/` — new directory with Playwright config + 14 test files + helpers
- `Makefile` — add elm-test, e2e-test, test-web targets
- `.github/workflows/` — add or update CI workflow
