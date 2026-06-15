# Plan 19 — Property-Based Testing Migration

**Status:** Planning (2026-06-15)
**Owner:** Bharadwaj + Claude
**Branch family:** `plan-19-tN-pX-<slug>` (e.g., `plan-19-t1-pa-core-genesis`)

---

## Goal

Migrate the test suite from purely example-based to **property-based testing (PBT) led**, with example-based tests retained only where they materially carry value (regression markers, golden wire-format pins, E2E browser scripts, cross-platform parity scripts).

The migration is **per-tier**, with each tier optimised against its own goal — confidence, consolidation, or speed — so we don't force a one-size-fits-all tradeoff.

## Architecture

Per tier: introduce the appropriate PBT library, build a shared `Generators` module, add properties **alongside** existing examples (additive, no coverage loss), then audit + delete redundant examples. All 5 tiers run in parallel via worktrees following identical phase structure.

Nightly cron exercises the suite at N=1000 (vs PR-time N=100); any failure auto-files a GitHub issue **and** a draft regression test PR pinning the exact failing seed/input, so nightly findings never get lost.

## Tech Stack

- **Scala (sangeet-core, sangeet-server, sangeet-desktop):** ScalaCheck + `scalatestplus-scalacheck` (already compatible with current ScalaTest 3.2.x)
- **Elm (sangeet-web):** elm-test built-in `fuzz` (no new library)
- **Python (mcp-servers):** Hypothesis
- **CI:** GitHub Actions, nightly cron at 02:30 IST (Asia/Kolkata)

## Tier Inventory

| Tier | Module            | Files / Tests Today | PBT Lib         | Per-Tier Goal                                                | Target State                                             |
| ---- | ----------------- | ------------------- | --------------- | ------------------------------------------------------------ | -------------------------------------------------------- |
| 1    | `sangeet-core`    | 46 / ~648           | ScalaCheck      | **Confidence** — domain invariants, round-trips, editor laws | ~150 properties + ~30 retained examples; −60% file count |
| 2    | `sangeet-server`  | 18 / ~168           | ScalaCheck      | **Consolidation** — endpoint contracts collapse N→1          | ~50 properties; −40% file count                          |
| 3    | `sangeet-desktop` | 11 / ~178           | ScalaCheck      | **Speed** — mostly JavaFX integration, minimal PBT scope     | ~3 files get PBT; rest stays                             |
| 4    | `sangeet-web`     | 23 / ~662           | elm-test `fuzz` | **Confidence** — pure Elm update logic + decoders            | ~80 fuzz tests; −50% file count                          |
| 5    | `mcp-servers`     | 1 / 21              | Hypothesis      | **Warm-up** — text→JSON mapping properties                   | ~5 properties                                            |

---

## Per-Tier Phasing (4 PRs × 5 tiers = 20 PRs)

Each tier runs Phase A → B → C → D **sequentially within the tier**. Tiers run in parallel.

### Phase A — Genesis (1 PR per tier)

**Goal:** Introduce the PBT library, establish patterns, write one end-to-end sample property. **No deletions.**

Per-tier deliverables:

- Add library dep (`scalatestplus-scalacheck` for Scala; `hypothesis` to `pyproject.toml` for Python; nothing for Elm since `fuzz` is built-in)
- Create `Generators` module:
  - Scala: `src/test/scala/com/varpas/sangeet/{core,server,desktop}/generators/Generators.scala`
  - Elm: `sangeet-web/tests/Generators/Composition.elm` etc.
  - Python: `mcp-servers/sangeet-debug-console/tests/strategies.py`
- Write **one** property + one passing-locally example to anchor the pattern
- Update `docs/developer/testing/property-based-testing.md` with the chosen patterns for that tier

### Phase B — High-Duplication Categories (1 PR per tier)

**Goal:** Replace the obvious "N example tests with different inputs" clusters with single properties.

Example targets:

- `OrnamentSpec.scala` — 9 separate `test("X ornament construction")` cases → 1 `forAll(genOrnament)` property
- `RaagSpec.scala` if it exists, similar for taals
- Repeated table-driven tests in routes
- Elm `RaagDecoderTest` repeating per-raag decode cases

Still **additive** — the new properties run alongside the old examples. Both contribute to coverage.

### Phase C — Invariants, Round-Trips, Laws (1 PR per tier)

**Goal:** Add properties for the high-value invariants that example tests can't realistically cover.

Per-tier examples:

- **sangeet-core**: `forAll(genComposition) { c => decode(encode(c)) == c }` (round-trip); `forAll(genEvent, genCursor) { (e, cur) => applyEdit(applyEdit(s, e), undo) == s }` (undo law)
- **sangeet-server**: `forAll(genApiRequest) { req => statusCode(handle(req)) in {200, 400, 404} }` (contract); `forAll(genComposition) { c => roundTrip("/api/v1/compositions", c) == c }`
- **sangeet-desktop**: `forAll(genConfig) { c => loadConfig(saveConfig(c)) == c }`
- **sangeet-web**: `fuzz Generators.composition (\c -> Codec.encode c |> Codec.decode == Ok c)`
- **mcp**: `@given(text())` for the text→JSON shapes; round-trip through the DebugCommand decoder

### Phase D — Prune Redundant Examples (1 PR per tier)

**Goal:** Delete example tests now subsumed by properties. **This is the only phase that removes coverage.**

Per-deletion protocol:

1. List the property that subsumes the example (in the PR description)
2. Run the suite with the example deleted + scoverage report
3. Confirm `coverage(file) ≥ pre-deletion coverage(file)` — if not, the property doesn't actually subsume; restore the example
4. Land the deletion

Keep:

- Regression tests for shipped bugs (each has an explicit fail-case marker — value of the test is the marker)
- Golden serialization tests (PBT verifies round-trip but not wire format byte-stability)
- Tests that pin specific raag/taal values (24 raags, 11 taals — these are catalog tests, not behavior tests)

---

## Parallelism Strategy

```
Week 1: ┌─ T1A core genesis     ┐  ┌─ T2A server genesis  ┐  ┌─ T3A desktop genesis ┐
        │ T4A web genesis       │  │ T5A mcp genesis      │
        └─ 5 PRs in parallel ───┘
Week 2: ┌─ T1B core dup-collapse┐  ┌─ T2B server dup      ┐
        │ T4B web dup           │
        └─ start once tier's A merges (sequential within tier) ─┘
Week 3: Phase C across tiers
Week 4: Phase D across tiers + plan-19 close
```

Wall-clock estimate: **~3-4 weeks**, gated by Phase B (the bulk of new properties to write).

---

## Shared Conventions (`docs/developer/testing/property-based-testing.md`)

New file, written as part of T1A (the first Phase A PR). Contents:

### Where generators live

```
sangeet-core/src/test/scala/com/varpas/sangeet/core/generators/
  Generators.scala            (all domain generators)
sangeet-server/src/test/scala/com/varpas/sangeet/server/generators/
  RequestGenerators.scala     (API request generators; reuses core's domain ones)
sangeet-web/tests/Generators/
  Composition.elm
  Cursor.elm
  Editor.elm
mcp-servers/sangeet-debug-console/tests/
  strategies.py               (Hypothesis strategies)
```

Generators are **shared by reference** within a language; never copy-pasted across modules.

### Naming

- `propXxxRoundTrip` — `decode(encode(x)) == x` style
- `propXxxIdempotent` — `f(f(x)) == f(x)`
- `propXxxAssoc` — `f(f(a,b),c) == f(a,f(b,c))`
- `propXxxClosed` — output type matches input type's invariant
- `propXxxLaw` — algebraic law that doesn't fit the above

Test class names follow ScalaTest convention: `XxxPropSpec` (using `AnyPropSpec` style trait to distinguish from example specs).

### When to write a property vs example (decision tree)

```
Is the input a small fixed set (enum cardinality, lookup table)?
  yes → example test
  no  → continue
Is the test verifying a specific bug from a Plan-N PR?
  yes → keep as regression example; ALSO add a property if the bug class is generalizable
  no  → continue
Does the function have an invariant (round-trip, idempotent, monotonic, law)?
  yes → property test
  no  → continue
Is the assertion about a specific output byte/string format (golden)?
  yes → example test (golden)
  no  → property test
```

### Nightly findings → regression test workflow

1. Nightly cron at 02:30 IST runs N=1000 properties
2. On failure: ScalaCheck/Hypothesis emit `Seed.fromBase64("...")` or `@reproduce_failure` with the failing input
3. A Python helper script (`scripts/property_failure_to_regression.py`) reads the failure, generates a hand-written regression test using the shrunken input, opens a draft PR + GitHub issue cross-linked

---

## CI Policy

### PR-time CI (existing pipeline, augmented)

- ScalaCheck default: `minSuccessfulTests = 100` (no override)
- Hypothesis default: `max_examples = 100`
- elm-test `fuzz` default: 100 runs
- **Time budget:** PR-time PBT must not push `Scala Tests + Coverage` past the E2E shard time (~5-8 min) — i.e., PBT can grow Scala tests up to ~6 min, but no more. If a tier overruns, scale that tier's N down to 50.

### Nightly cron (new)

`.github/workflows/nightly-pbt.yml`:

```yaml
on:
  schedule:
    - cron: '0 21 * * *' # 21:00 UTC = 02:30 IST (slight offset to avoid GitHub minute-0 load)
jobs:
  pbt-deep:
    runs-on: ubuntu-latest
    steps:
      - checkout
      - run sbt test with -Dscalacheck.minSuccessfulTests=1000
      - run cd sangeet-web && npx elm-test --fuzz 1000
      - run cd mcp-servers/sangeet-debug-console && pytest --hypothesis-profile=ci
      - on failure: invoke scripts/property_failure_to_regression.py
```

### `scripts/property_failure_to_regression.py` (new)

- Parses ScalaCheck / Hypothesis / elm-test failure output
- Extracts the seed and shrunken input
- Templates a new regression test file under `{module}/src/test/.../regressions/`
- Pushes branch + opens draft PR + opens issue
- Cross-links both

---

## Files Touched (cumulative across all 20 PRs)

### New

```
docs/developer/testing/property-based-testing.md              (conventions)
.github/workflows/nightly-pbt.yml                              (cron)
scripts/property_failure_to_regression.py                      (failure → PR helper)
sangeet-core/src/test/scala/com/varpas/sangeet/core/generators/Generators.scala
sangeet-server/src/test/scala/com/varpas/sangeet/server/generators/RequestGenerators.scala
sangeet-web/tests/Generators/*.elm
mcp-servers/sangeet-debug-console/tests/strategies.py
... + new PropSpec test files per tier
```

### Modified

```
build.sbt                                  (add scalatestplus-scalacheck)
mcp-servers/sangeet-debug-console/pyproject.toml  (add hypothesis)
... + tier-by-tier deletions in Phase D
```

### Deleted (Phase D)

Tier 1 estimated −20 example test files; Tier 2 −7; Tier 4 −11. Specifics from each tier's Phase D PR.

---

## Verification

### Per-tier (per Phase)

- All existing tests pass + new properties pass
- Coverage stays ≥80% (scoverage gate)
- CI time for the tier doesn't exceed the budget (see CI Policy)

### End-to-end (after Phase D of last tier)

1. `make check-all` green
2. Test file count: ~75 Scala → ~30; ~23 Elm → ~12
3. Total test invocations: roughly stable or modestly down
4. Nightly job has run at least 7 days without false-positive flakes
5. Conventions doc cross-linked from `docs/README.md` + `CLAUDE.md`

---

## Out of Scope

- **Playwright E2E (18 specs)** — UI integration, can't fuzz meaningfully
- **`tests/integration/*.json` parity scripts** — cross-platform contracts; the scripts ARE the spec
- **Golden / wire-format tests** — PBT verifies round-trip, not byte-exactness
- **Plan 17 + earlier shipped-bug regression tests** — keep the exact failing input as a marker
- **Catalog tests** (24 built-in raags, 11 built-in taals) — these are data validations, not behavior tests
- **Replacing ScalaTest entirely** (e.g., with Munit) — too disruptive; ScalaCheck plugs into ScalaTest

---

## Risks

| Risk                                                   | Mitigation                                                                              |
| ------------------------------------------------------ | --------------------------------------------------------------------------------------- |
| PBT misses an edge case the example covered            | Additive Phase A-C + coverage-gated Phase D pruning                                     |
| Generators drift between Scala modules                 | Single conventions doc; `core/generators` is the source-of-truth, server/desktop import |
| Nightly cron generates flake noise                     | Auto-issue + auto-draft-PR makes triage cheap; flake itself becomes a regression test   |
| Total PR-time CI grows past E2E shard time             | Per-tier N cap with fallback to N=50 if budget exceeded                                 |
| New contributors confused by mixed style               | Conventions doc has the decision tree                                                   |
| `scripts/property_failure_to_regression.py` is brittle | Start as best-effort; improve when it misfires                                          |

---

## Open Decisions (resolved during brainstorming, recorded for reference)

- Primary motivation → **all three** (tradeoff per layer)
- Off-limits (keep example-based) → **Playwright E2E + regression tests + golden tests + parity scripts**
- Scala library → **ScalaCheck** (de-facto, ScalaTest-integrated)
- Run count → **N=100 PRs + N=1000 nightly with auto-file regression**
- Migration shape → **Additive then prune**
- Tier ordering → **All in parallel** (with sequential phases within tier)
- Nightly failure routing → **Auto-create GitHub issue + draft regression PR**
