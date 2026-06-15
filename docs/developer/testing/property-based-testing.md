# Property-Based Testing Conventions

**Status:** Living document — created 2026-06-15 alongside Plan 19 T1A.
**Scope:** All language tiers in this repo. Tier-by-tier rollout is tracked in `docs/developer/plans/plan-19-property-based-testing.md`; this doc is the evergreen reference once that plan closes.

---

## Overview

Property-based testing (PBT) checks invariants by generating many inputs from a generator and asserting an algebraic law holds for every one — versus example-based testing, which asserts that one hand-written input produces one hand-written output. PBT catches whole classes of bugs (encode/decode mismatches, unhandled enum cases, off-by-ones at boundaries) with far fewer lines than enumerating examples.

We use it where the assertion is structural — round-trips, idempotency, commutativity, totality of pattern matches. We keep example tests where the value of the test IS the specific input (regression markers for shipped bugs, golden serialization bytes, catalog validation of the 26 raags and 11 taals).

### When to write a property vs. an example

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

---

## Library choices

| Tier              | Library                                    | Trait / API                                     |
| ----------------- | ------------------------------------------ | ----------------------------------------------- |
| sangeet-core      | ScalaCheck via `scalatestplus-scalacheck`  | `AnyFunSuite with ScalaCheckPropertyChecks`     |
| sangeet-server    | same                                       | same; reuses `sangeet-core` generators by import |
| sangeet-desktop   | same                                       | minimal use — mostly JavaFX integration         |
| sangeet-web (Elm) | elm-test `Fuzz`                            | `Test.fuzz Generators.composition`              |
| mcp-servers       | Hypothesis (Python)                        | `@given(strategies.command())`                  |

ScalaCheck plugs into the existing ScalaTest 3.2.x runner — no breaking changes to CI, sbt config, or coverage tooling.

---

## Where generators live

Each language has one source-of-truth generator module per tier. Other modules in the same language **import** from it; never copy-paste a domain generator across modules.

```
sangeet-core/src/test/scala/com/varpas/sangeet/core/generators/
  Generators.scala            (canonical: domain types — Note, Swar, Event, Composition, ...)

sangeet-server/src/test/scala/com/varpas/sangeet/server/generators/
  RequestGenerators.scala     (API request shapes; reuses sangeet-core's domain generators)

sangeet-desktop/              (no shared generators planned — minimal PBT scope; tests ad-hoc)

sangeet-web/tests/Generators/
  Composition.elm
  Cursor.elm
  Editor.elm

mcp-servers/sangeet-debug-console/tests/
  strategies.py               (Hypothesis strategies)
```

### Domain rules encoded in generators

Generators should produce **only musically valid data by construction**, so domain code never sees illegal inputs:

- **Sa and Pa are achal** (fixed) — they only ever carry the `Shuddha` variant. Komal/Tivra on Sa or Pa is a bug; `Generators.variantFor(note)` enforces this.
- **Re, Ga, Dha, Ni** can be `Shuddha` or `Komal` (not `Tivra`).
- **Ma** can be `Shuddha` or `Tivra` (not `Komal`).

Tests that need to verify the rejection of *invalid* data construct it by hand.

### Sizing

PR-time CI runs each property `N=100` times. Keep individual generators small so the budget stays well under E2E shard time:

- Lists of events / sections / ornaments: bounded with `Gen.choose(0, max)`, never `Gen.listOf` (which is unbounded).
- Strings: 1–16 chars from a safe alphabet (alphanumerics + space + dash).
- `Composition`: ≤ 3 sections, ≤ 16 events per section, ≤ 2 ornaments per swar.

---

## Naming

Test methods follow these prefixes so the intent is visible without reading the body:

| Prefix              | Shape                                         |
| ------------------- | --------------------------------------------- |
| `propXxxRoundTrip`  | `decode(encode(x)) == x`                      |
| `propXxxIdempotent` | `f(f(x)) == f(x)`                             |
| `propXxxAssoc`      | `f(f(a, b), c) == f(a, f(b, c))`              |
| `propXxxClosed`     | output type matches input type's invariant    |
| `propXxxLaw`        | algebraic law that doesn't fit the categories above |

Test class names: `XxxPropSpec` (e.g. `SwarFormatPropSpec`) to distinguish from example specs (`XxxSpec`).

---

## CI policy

### PR-time (existing pipeline, augmented)

- ScalaCheck default `minSuccessfulTests = 100` (no override).
- Hypothesis default `max_examples = 100`.
- elm-test `fuzz` default 100 runs.
- **Time budget:** PR-time PBT must not push `Scala Tests + Coverage` past E2E shard time (~5–8 min). If a tier overruns, scale that tier's N down to 50.

### Nightly cron (`.github/workflows/nightly-pbt.yml`)

Cron at 21:00 UTC (02:30 IST) runs the full suite at `N=1000`. On failure:

1. ScalaCheck / Hypothesis emit the seed + shrunken input.
2. `scripts/property_failure_to_regression.py` parses the failure, templates a regression test under `{module}/src/test/.../regressions/`, opens a draft PR + GitHub issue, cross-links both.

So nightly findings never get lost — every failure becomes a pinned regression test before the next morning.

---

## What we do NOT migrate

Some tests have value precisely because the input is fixed. Don't replace these with properties:

- **Playwright E2E specs** — UI integration that can't be fuzzed meaningfully.
- **`tests/integration/*.json` parity scripts** — these scripts ARE the cross-platform contract.
- **Golden / wire-format tests** — PBT verifies round-trip, not byte-exact stability.
- **Plan-17 and earlier regression tests** — the specific failing input from the bug report IS the test's value.
- **Catalog tests** (26 built-in raags, 11 taals) — these are data validations, not behaviour tests.

---

## See also

- `docs/developer/plans/plan-19-property-based-testing.md` — migration plan (5 tiers × 4 phases)
- `sangeet-core/src/test/scala/com/varpas/sangeet/core/generators/Generators.scala` — canonical Scala generators
- `sangeet-core/src/test/scala/com/varpas/sangeet/core/format/SwarFormatPropSpec.scala` — seed property: composition round-trip
