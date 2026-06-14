# Specs & cross-platform codegen — exploration

> Status: study document. Not a plan, not a commitment. Written 2026-06-14 in response
> to Bharadwaj's ask about consolidating specs under `docs/specs/` and exploring
> codegen for UI logic shared across desktop, web, and the future Android port.

## 1. The ask, restated

Two threads:

1. **Specs scatter.** The `.swar` file format and the REST API "feel like specs" but
   live in code (Scala types + circe codecs) and prose (`docs/specs/backend-api-spec.md`).
   No machine-readable schema is committed. Should all formal specs live in `docs/specs/`?
2. **UI duplication.** Desktop (Scala 3 + ScalaFX) and web (Elm 0.19) have a lot
   of parallel UI logic. Some asymmetry is irreducible (JavaFX vs HTML/DOM), but is
   there a "logical UI" core that could be expressed as a spec and codegen'd into
   both languages — like `ui-strings.json` already is?

User wants all four downstream values realized: **Android port consumption, desktop/web
codegen, validation/onboarding, external interop**. So the bar is "would this hold up
when a third platform is added," not "would this just help today's two."

## 2. What's already a spec, in some form

| Artifact                                   | Where it lives today                                                                                                                                    | Status                                                                  |
| ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| UI strings catalog                         | `sangeet-core/src/main/resources/ui-strings.json`                                                                                                       | Real spec, drives codegen → `UiStrings.scala` + `UiStrings.elm`         |
| Domain model (Composition, Event, Raag, …) | Scala 3 `enum` / `case class` in `sangeet-core/.../model/` + circe codecs in `format/` + Elm mirrors in `sangeet-web/src/Model/`                        | Implicit — Scala is the de-facto spec, Elm hand-mirrors it              |
| `.swar` file format                        | Documented in prose at `docs/superpowers/specs/2026-03-28-sangeet-notes-editor-design.md` §2; round-tripped through Scala circe codecs and Elm decoders | No JSON Schema. No formal versioning beyond a `"version": "2.0"` field. |
| REST API                                   | Tapir endpoint definitions in `sangeet-server/.../endpoints/` (12 files, ~600 LOC). Tapir generates OpenAPI at runtime and serves it via Swagger UI.    | No committed OpenAPI file. Spec is the Scala code itself.               |
| Debug bridge protocol                      | Scala `enum DebugCommand` in `sangeet-core/.../debug/DebugCommand.scala` + Elm decoder in `sangeet-web/src/Debug/Interpreter.elm`                       | Round-tripped by tests; no committed schema.                            |
| Test definitions for parity runner         | `tests/integration/*.json` + TypeScript types in `e2e/integration/helpers/test-definition.ts`                                                           | Implicit shape, validated by TypeScript at parse time.                  |
| AppConfig (session persistence)            | `sangeet-core/.../config/AppConfig.scala` + circe codecs. Persisted to `~/.sangeet/config.json`.                                                        | No external schema.                                                     |
| Built-in raag/taal data                    | Hardcoded as Scala values in `sangeet-core/.../raag/Raags.scala` + `taal/Taals.scala`                                                                   | Code, not data. (Plan §3 below revisits this.)                          |
| Grafana dashboard                          | `docs/grafana/sangeet-server-health.json`                                                                                                               | External tool's own schema; not ours to define.                         |

**Conclusion of the inventory:** the codebase has _one_ full-stack codegen success
story (`ui-strings`). Everything else is either prose-only or "Scala is the canonical
source, every other language mirrors by hand."

## 3. Proposal: `docs/specs/` as the single home

```
docs/specs/
  README.md                    # index, conventions, versioning policy
  swar-file-format.json        # JSON Schema for .swar files (NEW)
  swar-file-format.md          # human-friendly companion (NEW)
  rest-api.openapi.yaml        # OpenAPI 3.1 export (NEW, generated at build)
  debug-command-protocol.json  # JSON Schema for DebugCommand (NEW)
  ui-strings-catalog.json      # symlink or move from sangeet-core/.../resources/
  ui-strings-catalog.schema.json   # schema for the catalog itself (NEW)
  app-config.json              # AppConfig schema (NEW, lower priority)
  backend-api-spec.md          # existing prose spec (keep)
  frontend-spec.md             # existing prose spec (keep)
```

Conventions to adopt:

- **One file = one artifact.** No bundled multi-spec files.
- **Machine-readable schemas live alongside their prose companions.** `.json` + `.md`
  with the same basename.
- **Versioning policy.** Every schema includes a `"version"` field. Breaking changes
  bump a `BREAKING:` line in `docs/specs/README.md`. Older versions kept under
  `docs/specs/archive/v1/` if anyone still ships clients on them.
- **No regeneration drift.** If a spec is generated from code (OpenAPI from Tapir),
  CI must regenerate and `git diff --exit-code` it. We already do this for `ui-strings`
  via `make check-strings`.

## 4. Ranked proposals — by ROI

### 4.1 `.swar` JSON Schema — HIGH value, MEDIUM effort

Why it matters most:

- **Validation.** `make validate-fixtures` could lint every hand-crafted test JSON
  before it hits the test runner. Currently the `swar-file-validator` subagent
  encodes this knowledge informally.
- **Android consumption.** kotlinx.serialization can be configured against a JSON
  Schema, or used to derive parser stubs.
- **External tooling.** Anyone writing a player, MIDI exporter, or web preview can
  bind against the schema. The schema is the contract.
- **Onboarding.** A schema with `$comment` fields per property is documentation
  that doesn't drift.

Effort:

- Translate `Composition` → `Section` → `Event` (Swar / Rest / Sustain / Chikari /
  LockedBeat) → `Ornament` (8 variants) → `Stroke` → `Tihai` → `Metadata` → … into
  JSON Schema draft 2020-12. ~400 lines of YAML or JSON.
- Author-time: 1–2 days.
- Maintenance: ScalaTest case asserts every `.swar` written by the editor validates
  against the schema. Anyone touching the model has to update one extra file.

Cost: low ongoing. The schema is hand-maintained; codegen _consumes_ it but doesn't
_produce_ it.

Risk: schema drift. Mitigation: a single CI check that round-trips a representative
`.swar` through both `circe` and an external JSON Schema validator (`ajv`,
`jsonschema-rs`, whatever).

### 4.2 OpenAPI export from Tapir — HIGH value, LOW effort

Tapir already builds `OpenAPI` at runtime to serve Swagger UI. The same code can
dump it to a file at build time:

```scala
// sangeet-server/build.sbt or a small SbtPlugin
val openapiDump = taskKey[Unit]("Dump OpenAPI to docs/specs/rest-api.openapi.yaml")
openapiDump := {
  val oas = OpenAPIDocsInterpreter().toOpenAPI(allEndpoints, "Sangeet", "1.0")
  IO.write(file("docs/specs/rest-api.openapi.yaml"), oas.toYaml)
}
```

Plug it into CI: `sbt openapiDump && git diff --exit-code docs/specs/rest-api.openapi.yaml`.

Why it matters:

- **Android.** Retrofit / Ktor clients can be generated from OpenAPI.
- **Web.** Could in principle generate Elm HTTP clients via openapi-elm-codegen
  (though current hand-written `Api/*.elm` is already small and clean — verify
  before adopting).
- **Interop.** Third parties can build clients without reading our Tapir code.

Effort: ~half a day to wire the sbt task + CI gate. Probably the highest
value-per-hour item in this entire document.

Risk: OpenAPI generation in Tapir occasionally drifts across Tapir versions.
Mitigation: pin the Tapir version in `build.sbt` (already done) and treat the
generated YAML as a checked-in artifact.

### 4.3 Move `ui-strings.json` into `docs/specs/` — LOW value, LOW effort

It already works. Moving the file is a 1-line `build.sbt` change (resource path)
and might cost more in confusion than it gains in tidiness. **Tentative recommend:**
_don't_ move it; leave a `docs/specs/README.md` entry pointing at its actual location.
The principle should be "specs are discoverable from `docs/specs/`," not "specs
must physically reside there."

### 4.4 Domain model codegen — HIGH potential value, HIGH effort, HIGH risk

The dream: a single `domain-model.spec.json` (or YAML) that describes Composition,
Event, Section, Ornament, etc. → generates Scala case classes + circe codecs, Elm
types + decoders/encoders, and (later) Kotlin data classes + kotlinx serializers.

This is what `ui-strings` does, but for the entire domain model — _currently the
single largest source of cross-platform duplication_.

| Layer                                                         | Today                                                                                                                                              | After codegen               |
| ------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------- |
| Scala `case class` + `enum` declarations                      | ~250 LOC across 13 files                                                                                                                           | Generated                   |
| circe codecs (semi-auto + a lot of hand-rolled enum dispatch) | ~500 LOC                                                                                                                                           | Generated                   |
| Elm types                                                     | ~1700 LOC across 8 files                                                                                                                           | Generated                   |
| Elm decoders + encoders                                       | ~1000 LOC (estimated, inside Model/\*.elm)                                                                                                         | Generated                   |
| Hand-written code per type                                    | One enum case added in 4 places (Scala model + Scala codec + Elm type + Elm codec) — the recent `SectionType.Sargam` change had to be made in each | Add to one spec file, regen |

Effort:

- Write the spec format (probably JSON Schema + extensions for sum types, since
  vanilla JSON Schema is awkward for tagged unions). 2–3 days.
- Write the Scala generator. 3–5 days. (TypeScript is the natural choice — the
  `scripts/` directory already runs codegen for ui-strings, and JSON Schema
  consumption in TS via `json-schema-to-typescript` etc. is well-trodden.)
- Write the Elm generator. 3–5 days. Trickier because Elm has stricter
  decoder/encoder ergonomics.
- Migrate one type end-to-end (say, `Stroke`) as the proof of concept. 1 day.
- Migrate the rest. 1–2 weeks part-time.
- Total: ~3–4 weeks part-time.

Risks (real):

- **Generators are software too.** They need tests, maintenance, edge cases. A
  half-finished generator can be a tax forever.
- **Loss of Scala-3 idioms in the output.** circe semi-auto + `derives` is
  beautiful; a generator emitting verbose explicit decoders is uglier.
- **Elm decoder ergonomics.** `Decode.succeed` + `andMap` chains are mechanical
  enough to generate, but stay-current support for sum types (`oneOf` with
  discriminator) requires care.
- **The change cost is asymmetric.** Today: 4-file edit per enum addition.
  After codegen: 1-file edit _if the generator already handles the shape_; many-file
  generator change _if it doesn't_. Net win, but the first few "doesn't" cases hurt.

Recommendation: **defer until Android is real**. The break-even point lands somewhere
around three target languages, not two. Today, the cost of mirroring Scala↔Elm by
hand is real but bounded. Adding Kotlin as a third manual mirror is when the
generator pays for itself unambiguously.

If we _did_ do it now, the most valuable narrow slice is: **codecs only**, not type
declarations. Hand-written types are nicer to read than generated ones. But
hand-written codecs are tedious and bug-prone (we shipped `SectionType.Sargam` last
week and had to chase fixtures down). A codegen that emits just `compositionCodec :
Decoder Composition` and the Scala equivalent, leaving the types hand-written and
in sync via tests, might be 80% of the value at 30% of the effort.

### 4.5 REST endpoint definitions as the spec — already true

`sangeet-server/.../endpoints/*.scala` is the source of truth. Tapir's value
proposition is that this _is_ the spec. The OpenAPI export (§4.2) just makes the
spec readable to non-Scala tools. **No change recommended.**

### 4.6 Built-in raag/taal data — LOW value to extract, MEDIUM effort

The 26 raags and 11 taals live in Scala code today. Moving them to JSON resources
would be aesthetically tidier and would let non-Scala users (Android, hypothetical
web-only builds) consume them. But:

- The data is small (~600 LOC total).
- Adding a new raag is rare and easy in Scala already.
- The Android client will pull raag/taal data through the REST API (`/api/v1/raags`,
  `/api/v1/taals`) anyway, so it doesn't need direct file access.

Recommendation: defer. Revisit only if a use case emerges (user-contributed raags?).

### 4.7 AppConfig schema — LOW value

Session-persistence JSON is internal. The schema is the Scala codec. External
consumers don't care. **No change recommended.**

---

## 5. UI logic codegen — feasibility analysis

This is the harder question. Where is there _logical_ UI duplication that could be
codegen'd, vs where is the duplication irreducible?

### Codegen candidates (ordered by ROI)

#### 5.1 AppAction palette — already centralized via catalog

The command-palette entries (each with `id`, `label`, `group`, `keyBinding`,
`platform`) are already driven by `ui-strings.json` plus a small `AppAction.elm` /
`AppAction.scala` enum. This is the easy case and it works.

#### 5.2 Toolbar button declarations — high feasibility

Current state: ~17 toolbar buttons declared in `ToolbarBuilder.scala` (~300 LOC)
and `Toolbar.elm` (~350 LOC), with similar structure: per-button `icon`, `label`,
`tooltip`, `action`, `enabled-when`, `visible-when`.

A spec could look like:

```yaml
# docs/specs/toolbar-layout.yaml
rows:
  - id: file-row
    buttons:
      - { id: new, action: NewComposition, icon: "📄", label: "New", tooltip: "Create a new composition (Ctrl+N)" }
      - { id: open, action: OpenFile, icon: "📂", label: "Open", tooltip: "..." }
      ...
  - id: edit-row
    buttons:
      - { id: undo, action: Undo, icon: "↶", label: "Undo", tooltip: "...", enabledWhen: "history.canUndo" }
      ...
```

Generates:

- A Scala `List[ToolbarButtonSpec]` consumed by `ToolbarBuilder` to construct
  ScalaFX `Button`s.
- An Elm `List ToolbarButtonSpec` consumed by `Toolbar.view` to render `<button>`s.

The button _declaration_ gets centralized. The _click handler_ (wiring `action` to
the platform's event system) stays per-platform — that's irreducible because
ScalaFX uses `onAction` and Elm uses `onClick (Msg ...)`. But the spec carries the
action _name_ and each platform looks it up in its own dispatch table.

Effort: ~2 days to spec the shape and write the generator. Migration: ~1 day per
side.

Value: future toolbar changes happen in one file instead of two. Removes a class
of "PR-A renamed Keys → Legend on web, desktop is still calling it Keys"
inconsistencies.

#### 5.3 Dialog button rows — medium feasibility

Many dialogs share the pattern `[Cancel] [Action1] [Action2]`. The labels for these
already live in `ui-strings`. The _layout_ and _action wiring_ is per-platform.
Codegen would help if we standardized on a "dialog spec" describing fields and
buttons. But the field rendering is so platform-specific (text input, dropdown,
multi-line) that the generator boundary is messy. **Tentative recommend:** _don't_
spec dialogs.

#### 5.4 Keyboard reference content — medium feasibility

The cheat-sheet dialog (post-PR-C) contains a long static table of keyboard
shortcuts. Currently hardcoded in both `KeyboardCheatSheetDialog.scala` and
`View/Dialogs/KeyboardCheatSheet.elm`. A spec:

```yaml
sections:
  - title: Swar
    bindings:
      - { key: "s", action: "Insert Sa" }
      - { key: "r", action: "Insert Re" }
      ...
```

Generates a Scala data structure and an Elm one, each rendered by hand into the
platform's table primitive. Spec ~80 lines; current duplication ~300 lines per side.

This is on the edge of "worth it." The content rarely changes. **Tentative
recommend:** defer.

### NOT codegen candidates

#### 5.5 View rendering itself

`View.GridRenderer` (Elm) renders a 5-row HTML table; `GridRendererFX` (Scala)
draws shapes on a JavaFX `Canvas`. These produce _visually equivalent_ output via
_fundamentally different_ primitives. No reasonable spec language can describe
both. The shared model (`SectionGrid`, `GridLine`, `BeatCell`) already lives in
sangeet-core and that's the right boundary.

#### 5.6 TEA update vs JavaFX event handlers

`State/Update.elm` is ~3300 lines of pure functions. `EditorPane.scala` and
related are stateful JavaFX. Both call into `sangeet-core` for the actual editor
logic. The remaining per-platform code is "translate platform event → core call →
update platform state" — this is the _adapter_ layer and resists generalization.

#### 5.7 Modal / dialog _plumbing_

Showing modals on web vs desktop differs at every level — z-index/CSS for the web,
JavaFX `Stage`/`Alert.showAndWait` for desktop. Trying to abstract this gives you
worse code in both.

---

## 6. Two anti-patterns to avoid

1. **"Spec everything" syndrome.** A spec is overhead until it's saving more
   maintenance than it costs to maintain itself. `ui-strings` pays off because
   string changes are frequent, mechanical, and used in many places. Domain
   model codegen is borderline today, will be clear winner with Android.
2. **Generator complexity outrunning the source code's complexity.** If the
   generator becomes harder to evolve than the generated code, you've inverted the
   relationship. Stay nimble; resist "generic" generators that handle every
   conceivable edge case.

## 7. Suggested order if any of this is to happen

1. **OpenAPI export (§4.2)** — half a day, immediate value, zero risk.
2. **`.swar` JSON Schema (§4.1)** — 1–2 days, sets up Android-readiness, validates
   fixtures, becomes the canonical answer to "what is a `.swar` file?".
3. **`docs/specs/README.md`** — index everything. Half a day.
4. **Toolbar button declaration spec (§5.2)** — 3 days end-to-end. Optional;
   removes a recurring drift class.
5. **Domain model codegen (§4.4)** — defer until Android. Revisit then.

Items 1–3 are clear wins. Item 4 is "nice to have." Item 5 is "wait for the right
moment."

## 8. Open questions for you

1. **Sequencing.** Do items 1+2+3 sound like a single PR-sized chunk, or a small
   "specs hygiene" plan in its own right?
2. **Toolbar codegen.** Worth it now, or defer until you see another instance of
   "the icon changed on web but not desktop"?
3. **Domain model codegen — Android pre-work.** When you do resurrect the Android
   port, do you want me to _start_ with the codegen and use the Android port as
   the forcing function, or build Android first with hand-mirrored types and
   introduce the codegen later when the duplication tax is undeniable?
4. **Versioning policy on `.swar`.** Today `"version": "2.0"`. If we publish a
   JSON Schema, we should commit to a deprecation policy. How much do you want to
   guarantee to external consumers (e.g., "we support reading any `version >= 2.0`
   for at least 12 months after a breaking change")?
5. **OpenAPI hosting.** Should the generated YAML be served at
   `https://sangeet-server.../api/v1/openapi.yaml` in addition to checked in?
   That'd let third parties always pull the live spec without checking out the
   repo.

Pick the questions you want to answer; the rest can wait. I'll write a plan for
whichever items you bless.
