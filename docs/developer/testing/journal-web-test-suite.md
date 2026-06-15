# Journal: Web App Test Suite Implementation

**Date:** June 2026
**Plan:** `docs/developer/plans/plan-4-web-test-suite.md`

## Summary

Added comprehensive test coverage for the web application across three layers: Elm program tests, Scala server API tests, and Playwright browser E2E tests. Total new tests: **698** (476 Elm + 112 server + 110 E2E).

## What Changed

### Layer 1: Elm Program Tests (476 tests)

14 test files + 1 test helper module added to `sangeet-web/tests/`:

| File | Tests | Coverage |
|------|-------|----------|
| TestHelpers.elm | — | Shared fixtures: defaultFlags, defaultModel, mockResponses |
| KeyHandlerTest.elm | ~30 | All key-to-action mappings: swar, komal/tivra, ornaments, cursor, modes |
| OrnamentModeTest.elm | ~18 | State machine: idle/collecting transitions, all 12 ornament types |
| UndoHistoryTest.elm | ~12 | Push/undo/redo/canUndo/canRedo/trim/maxSize |
| UpdateBasicTest.elm | ~15 | Model toggles, dialog open/close, blink timer, no-ops |
| UpdateEditorTest.elm | ~14 | Swar/rest/sustain/delete dispatch, API response handling |
| UpdateCursorTest.elm | ~8 | Arrow keys, tab, subdivision, octave change, click |
| UpdateSectionTest.elm | ~6 | Add/remove/reorder/rename/select sections |
| UpdateOrnamentTest.elm | ~12 | Ornament mode entry, note collection, API dispatch |
| UpdatePlaybackTest.elm | ~6 | Play/pause/stop state, port dispatch |
| UpdateDialogTest.elm | ~6 | New/properties dialog submit, form initialization |
| UpdateFileTest.elm | ~6 | Open/save/load/export port and API dispatch |
| GroupingLogicTest.elm | ~10 | 500ms threshold, 4-note max, undo-replay, clear |
| ApiResponseTest.elm | ~8 | Success/ApiFailure/HttpError handling for all endpoint types |
| IntegrationFlowTest.elm | ~8 | Multi-step workflows: type-undo-redo, section switch, etc. |

**Setup files modified:**
- `sangeet-web/elm.json` — added `elm-explorations/test` to test-dependencies
- `sangeet-web/package.json` — added `elm-test` devDependency and `test` npm script

### Layer 2: Server API Integration Tests (112 tests)

10 new spec files + 1 shared fixture file added to `sangeet-server/src/test/scala/`:

| File | Tests | Endpoints Covered |
|------|-------|-------------------|
| TestFixtures.scala | — | Shared helpers: minimalComposition, minimalCursor, editorInputJson |
| CursorRoutesSpec.scala | ~14 | next-beat, prev-beat, next-subdivision, prev-subdivision, set-octave, move-to |
| SectionRoutesSpec.scala | ~12 | add/remove/rename/reorder/select sections |
| OrnamentRoutesSpec.scala | ~18 | All 12 ornament types: gamak, andolan, gitkari, kan, sparsh, ghaseet, meend, krintan, murki, zamzama |
| StrokeRoutesSpec.scala | ~8 | Da/Ra/chikari/jod/clear strokes |
| CompositionRoutesSpec.scala | ~10 | Create gat/bandish/palta, serialize/parse roundtrip |
| LayoutRoutesSpec.scala | ~6 | Empty/populated/multi-section layout computation |
| ExportRoutesSpec.scala | ~6 | HTML export with scripts, PDF bytes, error handling |
| PlaybackRoutesSpec.scala | ~6 | Empty/single/multi swar scheduling, timing |
| RenderingRoutesSpec.scala | ~6 | Glyph rendering, komal/tivra marks, all scripts |

Existing specs expanded:
- `ReferenceRoutesSpec.scala` — expanded from 6 to 12 tests (colors, scripts, specific taal/raag)
- `EditorRoutesSpec.scala` — expanded from 5 to 22 tests (all editor endpoints, chained ops)

### Layer 3: Browser E2E Tests (110 tests, Playwright)

New `e2e/` directory with 14 test files + Page Object Model:

| File | Tests | Coverage |
|------|-------|----------|
| helpers/app-page.ts | — | SangeetPage POM: goto, pressKey, waitForApi, cursor/state helpers |
| helpers/global-setup.ts | — | Health check polling for server readiness |
| basic-load.spec.ts | 9 | Page load, toolbar, grid, status bar, section tabs |
| keyboard-input.spec.ts | 12 | All swar keys, komal/tivra, rest, sustain, delete |
| cursor-navigation.spec.ts | 7 | Arrow keys, wrap at cycle end, tab subdivision, click |
| swar-editing.spec.ts | 8 | Sequences, groups, delete, rapid typing |
| section-management.spec.ts | 6 | Add/switch/remove sections, content preservation |
| ornament-workflow.spec.ts | 8 | Gamak, andolan, kan, meend, murki workflows, escape cancel |
| stroke-editing.spec.ts | 5 | F2 toggle, Da/Ra stroke insertion, mode switching |
| undo-redo.spec.ts | 8 | Button state, undo/redo via click and Ctrl+Z/Y |
| file-operations.spec.ts | 8 | Save, export HTML/PDF download, file open |
| dialog-interactions.spec.ts | 12 | New/properties/about dialogs, form fill, submit, cancel |
| playback-controls.spec.ts | 9 | Play/pause/stop, loop toggle, BPM slider |
| script-switching.spec.ts | 6 | Devanagari/English/Kannada switching |
| view-toggles.spec.ts | 5 | Stroke/sahitya/legend toggles |
| multi-step-flows.spec.ts | 7 | Full workflows, stress test, dialog-edit cycles |

**E2E infrastructure:**
- `e2e/package.json` — Playwright dependency
- `e2e/playwright.config.ts` — Chromium headless, webServer for static files, CI reporter
- `e2e/tsconfig.json` — TypeScript config

### CI and Build

- `Makefile` — added `elm-test`, `e2e-test`, `test-web` targets
- `.github/workflows/test.yml` — new CI workflow running all three test layers on push/PR
- `.gitignore` — added `e2e/node_modules/`, `e2e/test-results/`, `e2e/playwright-report/`

### Plans

- `docs/developer/plans/plan-4-web-test-suite.md` — full plan for this work
- `docs/developer/plans/plan-tcp-debug-console.md` — historical plan (TCP debug console, previously completed)

## Key Technical Decisions

1. **Elm tests use direct `update` calls** instead of elm-program-test HTTP simulation — avoids complexity of mocking all 43 API endpoints while still testing TEA state transitions.

2. **E2E tests read cursor state from `.header-chip` status bar** instead of `.cursor-cell` DOM elements — Elm's sparse grid rendering only creates `<td>` cells for beats with events, making cursor-cell unreliable.

3. **Server tests follow the existing pattern** in EditorRoutesSpec/ReferenceRoutesSpec — direct http4s route invocation with `routes.run(req).unsafeRunSync()`, no actual server started.

4. **E2E Playwright config auto-starts the static file server** via `webServer` — only the Scala backend needs to be running separately.

## Test Counts vs Plan

| Layer | Planned | Actual | Notes |
|-------|---------|--------|-------|
| Elm program tests | 159 | 476 | Exceeded plan — more thorough per-function coverage |
| Server API tests | 121 | 112 | Slightly under — some planned edge cases consolidated |
| Browser E2E tests | 84 | 110 | Exceeded plan — additional interaction and edge case tests |
| **Total** | **364** | **698** | **1.9x planned, 7.3x desktop's 95 TCP tests** |
