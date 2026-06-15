# Plan 18 — Hygiene, Observability, and Codegen

**Status:** ✅ Completed (2026-06-15) — 12/12 PRs merged (PR-3a was already on main pre-plan via PR #32)
**Owner:** Bharadwaj + Claude
**Branch family:** `plan-18-pNx-<slug>` (e.g., `plan-18-p1a-docs-reorg`)

---

## Goal

Tackle four interconnected workstreams as one phased plan after Plan 17's bug fixes shipped:

1. **Docs cleanup + reorganization** — consolidate `docs/superpowers/` into `docs/developer/`, classify root-level prose, separate auto-generated reports.
2. **Cross-platform font unification** — bundle the same Noto fonts the web uses (English, Devanagari, Kannada, Telugu) into the desktop JAR so notation renders identically on both surfaces.
3. **Code-quality audit + refactors** — survey, report, then commit to splitting `Update.elm` (3627 lines), unifying the 10+ duplicated dialog modal frames, deduplicating logic between desktop and web, and an error-handling pass.
4. **Observability + spec/codegen trio** — land HTTP request metrics (Tapir interceptor), app-level mutation/file counters, web JS error capture, plus `generateOpenApi` + `generateSwarSchema` sbt tasks with CI freshness check. Close PR #79.

## Architecture

Single plan, three phases. Phases are sequential; PRs within a phase run in parallel where they don't share files. One exception: Phase 2's audit PR (2a) gates the three Phase-2 refactor PRs so we can incorporate audit findings before touching code surfaces.

Branch family is `plan-18-pNx-<slug>`. Each PR is independently mergeable, opens green CI, and rebases on `main`.

## Tech Stack

- **Scala 3** (sangeet-core, sangeet-desktop, sangeet-server)
- **Elm 0.19** (sangeet-web)
- **sbt + scalafix + scalafmt**
- **Tapir + http4s + cats-effect IO** (server)
- **Micrometer + Prometheus + Stackdriver** (observability)
- **JavaFX `Font.loadFont`** (font bundling)
- **PostHog** (already wired on web)
- **GitHub Actions** (CI freshness check job)

---

## Phasing & PR Inventory

### Phase 1 — Foundation (4 PRs, fully parallel)

Quick wins. No file overlap across PRs. All can land within ~1 week.

#### PR-1a — Docs reorganization (audience-based)

**Branch:** `plan-18-p1a-docs-reorg`

**Files moved:**

```
docs/
  user-guide/                                  (UNCHANGED — already populated)
  developer/
    plans/                                     ← docs/plans/* + docs/superpowers/plans/*
    specs/                                     ← docs/specs/* + docs/superpowers/specs/*
    architecture/
      debug-bridge.md                          ← docs/developer/debug-bridge.md
      debug-console.md                         ← docs/developer/debug-console.md
      parity-checking.md                       ← docs/developer/parity-checking.md
      ui-strings-catalog.md                    ← docs/developer/ui-strings-catalog.md
    operations/
      hosting-gcp.md                           ← docs/hosting-gcp.md
      observability-and-bug-reporting.md       ← docs/observability-and-bug-reporting.md
      metrics-dashboard.md                     ← docs/developer/metrics-dashboard.md
      grafana/                                 ← docs/grafana/
    testing/
      journal-web-test-suite.md                ← docs/journal-web-test-suite.md
  reports/                                     (NEW)
    strings-parity-report.md                   ← docs/strings-parity-report.md
    strings-porting-backlog.md                 ← docs/strings-porting-backlog.md
    strings-uncategorized-justifications.md    ← docs/strings-uncategorized-justifications.md
  README.md                                    (NEW — top-level index)
```

**Also:** `docs/superpowers/` deleted after content moves.

**Link-fix pass:**

```bash
# Find every reference to a moved path
grep -rn "docs/\(plans\|specs\|superpowers\|grafana\|hosting-gcp\|observability\|journal-web\|strings-\)" \
  docs/ sangeet-* .github/ Makefile CLAUDE.md *.md \
  | tee /tmp/plan-18-link-fixes.txt
```

Then `sed`-rewrite each match. Verify with `make lint` + `make check-all` after.

**Plan files refresh:**

- This plan file (`plan-18-hygiene-obs-codegen.md`) is moved as part of the reorg from `docs/plans/` → `docs/developer/plans/`.
- Update `CLAUDE.md` reference: `docs/superpowers/specs/2026-03-28-sangeet-notes-editor-design.md` → `docs/developer/specs/2026-03-28-sangeet-notes-editor-design.md`.

**New docs/README.md content (index):**

- `developer/` — for contributors (plans, specs, architecture, operations, testing)
- `user-guide/` — for end users
- `reports/` — auto-generated reports (do not edit by hand)

**Verification:**

- `make lint` green (catches scalafmt/scalafix/prettier/elm-format)
- `make check-all` green
- All four CI jobs pass

---

#### PR-1b — Font unification (bundle Noto into desktop JAR)

**Branch:** `plan-18-p1b-font-unification`

**Why:** Web already uses Google Fonts CDN (`Noto Sans`, `Noto Sans Devanagari`, `Noto Sans Kannada`, `Noto Sans Telugu`). Desktop's `FontCache.scala` calls `Font(name, size)` which falls back silently if the OS doesn't have these installed → inconsistent rendering, especially on Linux + Windows.

**Files:**

```
sangeet-desktop/src/main/resources/fonts/
  NotoSans-Regular.ttf                  (NEW — bundled)
  NotoSans-Bold.ttf                     (NEW)
  NotoSansDevanagari-Regular.ttf        (NEW)
  NotoSansDevanagari-Bold.ttf           (NEW)
  NotoSansKannada-Regular.ttf           (NEW)
  NotoSansTelugu-Regular.ttf            (NEW)
  OFL.txt                               (NEW — SIL Open Font License attribution)
sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/render/FontCache.scala  (REWRITE)
sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/MainApp.scala           (init call)
docs/developer/architecture/font-bundling.md                                       (NEW — small note)
```

**FontCache rewrite:** Use `javafx.scene.text.Font.loadFont(getClass.getResourceAsStream("/fonts/NotoSans-Regular.ttf"), size)` at app startup; register loaded fonts so the existing `Font(name, size)` lookups continue to work. Keep the cache by `(name, size)` shape.

**Initialization order:** Fonts must load before any `Stage` is shown — call from `MainApp.start()` first thing.

**Bundle size delta:** ~3.2 MB added to JAR. Acceptable. Native installers (.dmg/.msi/.deb) inherit the same JAR.

**Verification:**

- App launches, sample composition renders with Noto Sans, Devanagari section renders with Noto Sans Devanagari (test by manually switching script in the dialog)
- Disable any system-installed Noto fonts and verify bundled fonts win
- Linux CI/dev box (no system Noto) renders correctly
- `sbt sangeetDesktop/Universal/stage` succeeds; resulting native package has the fonts directory

---

#### PR-1c — Specs trio: OpenAPI + JSON Schema + index

**Branch:** `plan-18-p1c-specs-trio`

**Files:**

```
build.sbt                                                           (add sbt tasks)
sangeet-server/src/main/scala/com/varpas/sangeet/server/codegen/
  OpenApiExporter.scala                                             (NEW)
sangeet-core/src/main/scala/com/varpas/sangeet/core/codegen/
  SwarSchemaExporter.scala                                          (NEW)
docs/developer/specs/
  openapi.yaml                                                      (NEW — generated)
  swar.schema.json                                                  (NEW — generated)
  README.md                                                         (NEW — index + how-to-regen)
.github/workflows/ci.yml                                            (add check-specs job)
Makefile                                                            (add `make gen-specs`)
```

**sbt tasks (in `build.sbt`):**

```scala
lazy val generateOpenApi = taskKey[Unit]("Export OpenAPI YAML from Tapir endpoints to docs/developer/specs/openapi.yaml")
lazy val generateSwarSchema = taskKey[Unit]("Export JSON Schema for .swar files to docs/developer/specs/swar.schema.json")
```

**OpenApiExporter:** Calls Tapir's `OpenAPIDocsInterpreter().toOpenAPI(allServerEndpoints, "Sangeet API", version)`, serializes to YAML via `circe-yaml`, writes to disk.

**SwarSchemaExporter:** Derives JSON Schema from `Composition` codec. Use `json-schema-circe` or hand-write a minimal exporter that walks the case classes.

**CI freshness check (.github/workflows/ci.yml):**

```yaml
check-specs:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - name: Regenerate specs
      run: sbt generateOpenApi generateSwarSchema
    - name: Fail if specs are stale
      run: |
        if ! git diff --exit-code docs/developer/specs/openapi.yaml docs/developer/specs/swar.schema.json; then
          echo "::error::Specs are stale. Run 'make gen-specs' and commit."
          exit 1
        fi
```

**Wires into existing CI:** Add `check-specs` to the `lint` group so it gates `scala-tests`/`elm-tests`/`e2e-tests` like other lint checks.

**Verification:**

- `make gen-specs` writes both files deterministically (run twice, no diff)
- `check-specs` CI job passes on PR-1c; fails on a deliberate test commit that adds an endpoint without regenerating
- `docs/developer/specs/README.md` describes the contract: "these files are generated; edit the source, not the output"

---

#### PR-1d — Close PR #79

**Branch:** `plan-18-p1d-close-exploration-pr`

**Action:**

1. Move `docs/specs-and-codegen-exploration.md` (from PR #79 branch) to `docs/developer/specs/codegen-exploration.md` on `main` via this PR.
2. PR description references the new location and PR-1c which implements the trio.
3. After merge, post a closing comment on PR #79 linking to the merged commit + new file location, then close (do not merge) PR #79.

**Verification:**

- File exists at new location with original content + a small header noting "this is an archived exploration; the trio it recommends shipped in PR-1c"
- PR #79 closed with explanatory comment

---

### Phase 2 — Code Quality (4 PRs, sequential start)

PR-2a (audit) lands first. PR-2b/c/d run in parallel once 2a is merged so the audit can flag any "don't touch this yet" issues.

#### PR-2a — Code-quality audit report

**Branch:** `plan-18-p2a-audit-report`

**Files:**

```
docs/developer/reports/code-quality-audit-2026-06-15.md   (NEW)
```

**Audit scope** (delegated to subagents for parallelism):

| Subagent                      | Surface                                                | Output                                                                     |
| ----------------------------- | ------------------------------------------------------ | -------------------------------------------------------------------------- |
| Elm reviewer                  | `sangeet-web/`                                         | Modularity, duplication, error handling, TEA conformance gaps              |
| Scala-style reviewer          | `sangeet-core/`, `sangeet-desktop/`, `sangeet-server/` | Scala 3 idiom drift, duplication, error swallowing, IO boundaries          |
| Cross-platform parity checker | desktop ↔ web                                          | Feature drift, logic duplication, asymmetric error handling                |
| General-purpose audit agent   | All                                                    | CPU/memory hot spots (suspected), debug-log discipline, observability gaps |

**Report structure:**

```
# Code Quality Audit — 2026-06-15

## Summary (top 10 findings, severity-ranked)
## Detailed findings
  ### Duplication
  ### Modularity
  ### Error handling
  ### Debuggability + observability
  ### Performance (CPU/memory)
## Committed for fix in Plan 18
  - PR-2b Update.elm split
  - PR-2c Dialog frame unification
  - PR-2d Shared logic dedup
  - PR-3d Error-handling pass
## Filed as known debt (not in Plan 18)
  - <each item with severity + suggested follow-up plan>
```

**Verification:**

- Report exists, all four audit dimensions covered, every finding cites file:line evidence
- "Committed" section maps cleanly to PR-2b/c/d + PR-3d scope
- "Known debt" items have severity ranking so a future plan can pick them up

---

#### PR-2b — Split `Update.elm` (3627 lines)

**Branch:** `plan-18-p2b-split-update`

**Files:**

```
sangeet-web/src/State/
  Update.elm                  (REDUCED to router/dispatcher, ~400 lines)
  Update/
    Editor.elm                (NEW — typing, cursor, ornament, grouping, ~1100 lines)
    File.elm                  (NEW — open, save, save-as, recent files, ~600 lines)
    Tab.elm                   (NEW — open/close/switch, duplicate, dirty tracking, ~500 lines)
    Dialog.elm                (NEW — dialog open/close, modal state, ~400 lines)
    Section.elm               (NEW — section switching, clear, properties, ~300 lines)
    Net.elm                   (NEW — HTTP request handlers, error→bug-report path, ~300 lines)
```

**Approach:**

1. Keep top-level `update : Msg -> Model -> (Model, Cmd Msg)` in `Update.elm` as a dispatcher.
2. Each submodule exposes the same shape: `update : Msg -> Model -> (Model, Cmd Msg)` for its slice.
3. Move helpers used by only one submodule into that submodule. Helpers used by 2+ stay in `Update.elm` (or get extracted to a `Update/Helpers.elm` if there's a clean cluster).
4. **No behavior change.** All 593 elm-tests + 126 Playwright tests stay green.

**Verification:**

- `make elm-test` shows all 593 pass with no message-handling drift
- `make e2e` passes
- Each new file is under 1200 lines
- `elm-review` reports no new issues

---

#### PR-2c — Unify dialog modal frame

**Branch:** `plan-18-p2c-dialog-frame`

**Files:**

```
sangeet-web/src/View/Dialogs/
  Frame.elm                                                              (NEW)
  About.elm BugReport.elm ClearSection.elm CommandPalette.elm
  DuplicateTab.elm KeyboardCheatSheet.elm NewComposition.elm
  Properties.elm Support.elm UnsavedChanges.elm                          (REFACTOR each)

sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/
  ModalFrame.scala                                                       (NEW — trait)
  AboutDialog.scala BugReportDialog.scala CommandPaletteDialog.scala
  CompositionPropertiesDialog.scala CrashRecoveryDialog.scala
  DuplicateTabDialog.scala KeyboardCheatSheetDialog.scala
  NewCompositionDialog.scala SupportDialog.scala
  UnsavedChangesDialog.scala                                             (REFACTOR each)
```

**Web `Frame.elm` API:**

```elm
view :
    { title : String
    , onClose : msg
    , size : Size                  -- Small | Medium | Large | Custom Int Int
    , footer : List (Html msg)
    , body : Html msg
    }
    -> Html msg
```

Encapsulates: overlay div, ESC handler wiring (via decoder), focus trap on first focusable, click-outside-to-close (configurable), aria-modal + role=dialog, themed styles.

**Desktop `ModalFrame` trait:**

```scala
trait ModalFrame:
  protected def buildStage(title: String, content: Node, size: Size): Stage = ...
  protected def installEscClose(stage: Stage, onClose: () => Unit): Unit = ...
  protected def installFocusTrap(stage: Stage): Unit = ...
```

**Verification:**

- 19 dialogs (10 web + 10 desktop -1 shared) all reuse the frame
- Visual diff (manual): all dialogs look unchanged before/after
- ESC closes every dialog (Playwright check exists for some — extend to all)
- Tab/Shift-Tab focus trap works in every dialog

---

#### PR-2d — Shared logic dedup desktop ↔ web

**Branch:** `plan-18-p2d-shared-logic`

**Approach:**

1. Inventory pass (output from 2a's parity checker): list of logic that lives in both `sangeet-desktop/` and `sangeet-web/`. Likely suspects: grouping rules (Plan 17 PR-86 fixed both sides separately), cursor advancement after delete, starting-beat shift on change, ornament finish heuristics.
2. For each duplicated rule, decide:
   - **Move to sangeet-core** — preferred if the rule is pure (input → output without UI/IO).
   - **Move to sangeet-server endpoint** — if the rule needs server-side data (raag definitions, taals).
   - **Leave as-is + document** — if duplication is necessary (e.g., Elm can't import Scala).
3. Each moved rule: write tests in `sangeet-core` if not already present.

**Scope cap:** PR-2d picks the **top 3** duplicated rules from the audit, not all of them. Remaining items file as debt in PR-2a's report.

**Verification:**

- All cross-platform parity tests (`tests/integration/*.json` via `parity.spec.ts`) still pass
- New `sangeet-core` tests cover the moved rules
- Elm side imports the rule via JSON-over-HTTP from a new endpoint (if needed) or via codegen (deferred — for now, hand-port and add a test that compares to the Scala output)

---

### Phase 3 — Observability & Errors (4 PRs, fully parallel)

#### PR-3a — Tapir HTTP request metrics — ✅ ALREADY SHIPPED PRE-PLAN (PR #32, 2026-06-11)

**Discovered during execution:** Plan 12 Phase 2 already landed this work on 2026-06-11 via PR #32 as `sangeet-server/.../metrics/HttpMetrics.scala`. The implementation uses Tapir's `Metric` SPI directly (since `tapir-micrometer-metrics` doesn't exist) and routes through the existing `MetricsRegistry.registry` composite. The plan entry is left here for traceability; no Plan-18 PR was needed.

**Files:**

```
build.sbt                                                              (add tapir-micrometer-metrics dep)
sangeet-server/src/main/scala/com/varpas/sangeet/server/Main.scala     (wire interceptor, ~10 lines)
sangeet-server/src/test/scala/com/varpas/sangeet/server/
  MetricsInterceptorSpec.scala                                         (NEW, smoke test)
```

**Verification:**

- `sbt sangeetServer/test` — 156 existing + 1 new pass
- After local run: `curl localhost:28080/metrics | grep tapir_request_total` shows path-template-labeled counter lines
- After production deploy: `tapir/request/{active,total,duration}` descriptors land in Cloud Monitoring within ~70s

---

#### PR-3b — App-level mutation + file metrics

**Branch:** `plan-18-p3b-app-metrics`

**Counters added:**

| Counter                         | Labels                                                                | Surface       |
| ------------------------------- | --------------------------------------------------------------------- | ------------- |
| `sangeet_editor_mutation_total` | `kind` (swar_insert, delete, ornament_finish, undo, redo, paste, cut) | desktop + web |
| `sangeet_file_op_total`         | `op` (open, save, save_as, export_html), `result` (success, error)    | desktop + web |
| `sangeet_section_switch_total`  | —                                                                     | desktop + web |
| `sangeet_clipboard_op_total`    | `op` (cut, copy, paste)                                               | desktop + web |
| `sangeet_ornament_finish_total` | `type` (meend, kan, gamak, andolan, custom)                           | desktop + web |

**Files:**

```
sangeet-server/src/main/scala/com/varpas/sangeet/server/
  endpoints/MetricsEventEndpoints.scala                                (NEW — POST /api/v1/metrics/event)
  routes/MetricsEventRoutes.scala                                      (NEW)
  metrics/AppMetrics.scala                                             (NEW — Micrometer counter defs)

sangeet-core/src/main/scala/com/varpas/sangeet/core/api/
  MetricsEventApi.scala                                                (NEW — shared model)

sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/metrics/
  DesktopMetrics.scala                                                 (NEW — emits via TCP debug bridge OR local Micrometer registry)

sangeet-web/src/
  Api/Metrics.elm                                                      (NEW — POST helper)
  State/Update/Editor.elm                                              (instrument mutations)
  State/Update/File.elm                                                (instrument file ops)
```

**Web emission path:** Counters fire as side-effect `Cmd` in the relevant `Update` handlers → `Api.Metrics.send` → server endpoint → Micrometer registry → Prometheus + Stackdriver.

**Desktop emission path:** Desktop already has Micrometer on-classpath via `sangeet-server` dep? Actually no — desktop is standalone. Two choices:

- **Option A:** Desktop emits over HTTP to the server (only works if user is online + has the server running)
- **Option B:** Desktop emits to a local file-backed counter that no one ever reads

**Decision:** **Option A** — desktop emits to the production server endpoint when network is available, drops silently otherwise. Configurable via existing `AppConfig` (telemetry toggle, defaulted on).

**Verification:**

- `curl localhost:28080/metrics | grep sangeet_editor_mutation_total` shows counter after a few typed swars (web)
- Cardinality budget: 5 counters × ~10 label values avg = 50 series. Within budget.
- New `MetricsEventRoutes` test covers happy path + bad payload

---

#### PR-3c — Web JS error capture → bug-report

**Branch:** `plan-18-p3c-web-error-capture`

**Files:**

```
sangeet-web/public/ports.js                                            (add window.onerror + unhandledrejection)
sangeet-web/src/Ports.elm                                              (add port for uncaught errors)
sangeet-web/src/State/Update/Net.elm                                   (handle uncaught-error port → POST bug-report)
sangeet-web/src/Api/BugReport.elm                                      (extend with auto-tagged source)
sangeet-server/src/main/scala/com/varpas/sangeet/server/
  routes/BugReportRoutes.scala                                         (accept new `source` field, no schema break)
```

**Privacy posture:** Auto-send, no user UI. Per decision: matches PostHog's existing posture. Errors are tagged `source: "uncaught"` to distinguish from manual reports in storage.

**ports.js wiring:**

```js
window.addEventListener('error', (ev) => {
  app.ports.uncaughtError.send({
    message: ev.message,
    stack: ev.error ? ev.error.stack : null,
    filename: ev.filename,
    line: ev.lineno,
    col: ev.colno,
  });
});
window.addEventListener('unhandledrejection', (ev) => {
  app.ports.uncaughtError.send({
    message: String(ev.reason),
    stack: ev.reason && ev.reason.stack ? ev.reason.stack : null,
    filename: null,
    line: null,
    col: null,
  });
});
```

**Privacy doc update:** Mention this in `docs/developer/operations/observability-and-bug-reporting.md` after the docs reorg.

**Verification:**

- Deliberate `throw new Error("test")` in browser console fires a POST to `/api/v1/bug-report`
- Bug report appears in server logs with `source: "uncaught"`
- No double-send if Elm runtime itself crashes (idempotency-of-fire-and-forget is fine here)

---

#### PR-3d — Error-handling pass

**Branch:** `plan-18-p3d-error-handling`

Driven by PR-2a's audit findings. For each error-handling site identified:

**Files (illustrative — exact set comes from audit):**

```
sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/*  (audit + fix swallowed errors)
sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/*  (same)
sangeet-web/src/State/Update/*  (same)
```

**Decision rule per site:**

| Failure mode                                   | Action                                               |
| ---------------------------------------------- | ---------------------------------------------------- |
| User-correctable (bad input, missing file)     | Surface as UI message                                |
| Programmer error (impossible-shouldn't-happen) | Log via existing AppLogger + bug-report hook         |
| Network/transient                              | Retry with exponential backoff, log on final failure |
| Silent recovery is correct                     | Comment in code explaining why                       |

**Verification:**

- Audit report's "error handling" section maps to per-file diffs in this PR
- New unit tests for each fixed site
- No `case _ => ()` or bare `try { ... } catch { case _: Throwable => }` introduced

---

## Parallelism Strategy

```
Week 1:  ┌─ PR-1a docs reorg ──┐  ┌─ PR-1b font unification ──┐
         ├─ PR-1c specs trio ──┤  └─ PR-1d close PR #79 ──────┘
         └─ All 4 in parallel ─┘

Week 2:  ┌─ PR-2a audit report (single PR, gates Phase 2) ──┐
         └─ 1 PR ──────────────────────────────────────────┘

Week 3:  ┌─ PR-2b Update.elm split ──┐  ┌─ PR-2c dialog frame ──┐
         ├─ PR-2d shared-logic dedup ┤
         └─ 3 PRs in parallel ──────┘

Week 4:  ┌─ PR-3a HTTP metrics ──────┐  ┌─ PR-3b app metrics ──┐
         ├─ PR-3c JS error capture ──┤  └─ PR-3d error handling ┘
         └─ 4 PRs in parallel ──────┘
```

**Wall-clock estimate:** ~3-4 weeks at a steady cadence, less if reviewers move fast on Phase 1 + Phase 3 (small, well-scoped).

---

## Verification (end-to-end)

After all 12 PRs land:

1. `make check-all` green
2. CI: all 5 jobs green (lint, check-specs, scala-tests+coverage, elm-tests, e2e-tests)
3. `docs/` matches the audience-based structure; no broken internal links (run a link-check on `docs/`)
4. Desktop sample composition renders with bundled Noto fonts on a fresh machine without system Noto installed
5. `curl prod/metrics | grep -E "tapir_request|sangeet_editor_mutation|sangeet_file_op"` returns expected series
6. Throw a deliberate JS error on the live web app → bug report appears with `source: "uncaught"`
7. `make gen-specs` writes deterministically; CI catches a stale spec
8. PR #79 closed with a link to the new specs location
9. Audit report exists with "filed as known debt" list rolled into a follow-up plan stub

---

## Out of Scope (Deferred)

- Distributed tracing (Zipkin/Tempo) — not needed at current traffic
- Web Vitals collection on the web app — useful but a separate plan
- Code coverage for the web app (currently only Scala) — separate plan
- Replacing PostHog with self-hosted analytics — separate plan
- Bundling fonts on web (currently CDN-loaded) — would help offline UX, separate plan
- Lower-priority items 4 + 5 from PR #79 (toolbar codegen, full domain-model codegen)
- The 6 lower-severity items from the code-quality audit — filed as debt

---

## Risks

| Risk                                                                    | Mitigation                                                                                            |
| ----------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| `Update.elm` split changes message dispatch order subtly                | Comprehensive elm-test + Playwright suite catches behavior drift; if a test fails, the split is wrong |
| Bundled fonts bloat the desktop JAR by 3+ MB                            | Acceptable — sample composition needs the Devanagari/Kannada/Telugu fonts to render correctly anyway  |
| CI `check-specs` job adds 30–60s to lint phase                          | Acceptable — keeps specs honest                                                                       |
| Desktop metrics requiring server availability is a usability regression | Gate behind existing telemetry toggle in AppConfig; fall back silently when offline                   |
| Auto-send of uncaught web errors raises privacy concerns                | Document in observability doc; matches existing PostHog posture                                       |

---

## Open Decisions (resolved in brainstorming, recorded here for reference)

- Docs shape → **Audience-based** (developer/ + user-guide/ + reports/)
- Code quality scope → **All four refactors committed** (split, dialog frame, error handling, shared-logic dedup)
- Observability scope → **HTTP metrics + app metrics + JS error capture all in**
- Plan packaging → **Single phased plan** (not 4 mini-plans)
- Audit gating → **Sequential** (audit lands first, then refactors)
- Spec freshness → **CI check, fail if stale**
- Web error capture privacy → **Auto-send, no user UI**
