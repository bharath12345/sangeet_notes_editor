# Plan 13 — Beta Prep + AI Tooling

## Context

Plan 12 (cross-platform observability + bug reporting + analytics + crash recovery) shipped end-to-end on 2026-06-12. The app is now in **alpha** — Bharadwaj is the only user, exercising every feature himself before handing off to a handful of friends as **beta** testers, after which feedback will get folded in for a real **v1.0** public release.

This plan covers the small batch of usability + signaling + meta-tooling work that needs to happen *before* the beta testers see the app, plus a research-driven recommendation on how to make the AI tooling around the codebase pull more weight (since the entire app has been vibe-coded by AI).

## Goals

1. **Mouse-free operation** — every toolbar action keyboard-accessible on both desktop and web, with a command palette as the universal entry point.
2. **Make beta status unmissable** — testers should immediately understand they're not on a stable build, so their bug reports come in with the right expectations.
3. **Documented AI tooling strategy** for the codebase — both what's already in use and what's worth adding next.
4. **Technical-user reference for the TCP debug console** so power users can drive the app from a terminal.
5. **Frequent users can opt out of the read-only sample** on startup without losing it permanently.
6. **A growth dashboard** showing code / tests / docs growth per module over time, so the codebase's health is at-a-glance visible across the beta period.

## Non-goals

- Building the PostHog dashboards (Plan 12 leftover; deferred until beta traffic exists to look at).
- First-visit Report-a-Bug tooltip on web (Plan 12 leftover; deferred).
- GCP budget alert (Plan 12 leftover; trivial follow-up commit when there's time).
- v1.0 release itself — that's the next plan after this lands and beta feedback is gathered.
- Re-architecting any module. This plan is pure polish + meta.
- Internationalization of the keyboard cheat-sheet (English only for MVP).

---

## Task 1 — Keyboard shortcuts + command palette + discoverability

### Decision

**Hybrid model:** dedicated shortcuts for ~10 most-used actions, plus a **Cmd+K / Ctrl+K command palette** that fuzzy-searches every action. Discoverability via four reinforcing channels: markdown doc in user guide, in-app cheat-sheet dialog (?), tooltip suffixes on toolbar buttons, and the command palette itself listing each action's shortcut.

### Proposed shortcut bindings

These follow standard desktop conventions. JavaFX's `KeyEvent.isShortcutDown` automatically maps to Cmd on macOS and Ctrl on Windows/Linux. On web we use the same combos with `event.metaKey || event.ctrlKey`. Browser-preempted combos (`Cmd+W`, `Cmd+N`, `Cmd+T`) are skipped on web; users access those actions via the command palette instead.

| Action | Shortcut | Status | Notes |
|---|---|---|---|
| New composition | `Cmd+N` | new (desktop only — browser preempts) | Web: palette only |
| Open file | `Cmd+O` | new | Both platforms |
| Save | `Cmd+S` | new (preventDefault on web) | Both |
| Save As | `Cmd+Shift+S` | new | Both |
| Close tab | `Cmd+W` | already wired (desktop only) | Web: palette only |
| Next/Prev tab | `Cmd+Tab` / `Cmd+Shift+Tab` | already wired (desktop) | Web: `Cmd+Alt+→/←` |
| Undo | `Cmd+Z` | already wired | Both |
| Redo | `Cmd+Shift+Z` | already wired | Both |
| Cut/Copy/Paste | `Cmd+X / C / V` | already wired | Both |
| Toggle file browser | `Cmd+B` | already wired (desktop) | Add to web |
| Export HTML | `Cmd+E` | new | Both |
| Open Properties dialog | `Cmd+,` | new (Mac standard) | Both |
| Add Section | `Cmd+Shift+A` | new | Both |
| Rename Section | `F2` | new | Both |
| Remove Section | `Cmd+Shift+⌫` | new | Both |
| Toggle theme | `Cmd+Shift+T` | new | Both |
| Switch script | `Cmd+Shift+L` (L for language) | new | Both |
| Bug report | `Cmd+Shift+B` | new | Both |
| Help (user guide) | `F1` | new | Both |
| Keyboard cheat-sheet | `?` (no modifier) | new | Both — only when editor not focused for typing |
| **Command palette** | `Cmd+K` | new | Both — primary entry to every action |
| About | (palette only) | n/a | Low frequency |

**Conflict notes** — the cheat-sheet trigger `?` is tricky: it's also a valid swar input character if the editor accepts it. Disambiguate by only firing the cheat-sheet when the focused element is NOT the swar-input area. If that turns out to be brittle, fall back to `Cmd+/`.

### Files touched

**Desktop**
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/MainApp.scala` — extend the existing scene-level `addEventFilter` at line ~289 with the new bindings
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/ToolbarBuilder.scala` — append shortcut hints to each tooltip (`"Save composition (⌘S)"`)
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/CommandPaletteDialog.scala` *(new)* — Cmd+K modal with TextField + filtered ListView of actions
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/KeyboardCheatSheetDialog.scala` *(new)* — `?` modal grouping shortcuts by category, derived from the same action registry as the palette
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/action/AppAction.scala` *(new)* — strongly-typed action ADT: `case object Save extends AppAction { val title = "Save"; val shortcut = Some("Cmd+S"); val group = "File"; def execute(ctx: AppContext): Unit = ... }`. Single source of truth — palette + cheat-sheet + tooltip suffix + scene event filter all read from this list.

**Web**
- `sangeet-web/src/Input/KeyHandler.elm` — extend with the new shortcut bindings; reuse the existing key-decoding plumbing
- `sangeet-web/src/View/CommandPalette.elm` *(new)* — TEA component with a `Showing` / `Hidden` state; fuzzy filter on action title; `onKeyDown` handler for arrow nav + Enter
- `sangeet-web/src/View/KeyboardCheatSheet.elm` *(new)* — modal listing all shortcuts
- `sangeet-web/src/State/AppAction.elm` *(new)* — Elm mirror of the AppAction ADT so the palette and cheat-sheet share a single source

**Docs**
- `docs/user-guide/11-keyboard-shortcuts.md` *(new)* — bundled into desktop, rendered on the web user-guide route. Generated content matches the AppAction ADT (no manual sync drift).

### Sub-decisions still to nail down

- **Cheat-sheet trigger key.** Plan-of-record: `?`. Fallback: `Cmd+/` if `?` causes editor-focus problems.
- **Command palette fuzzy matching algorithm.** Simple substring match for MVP; upgrade to fuzzy (skim-style) if list grows past ~30 actions.
- **Should the AppAction list itself be loaded from JSON, or kept as Scala/Elm code?** Recommend code: type-safe, no startup-time parse cost, and the same list is mirrored cross-platform anyway. Generator script can keep Scala and Elm in lockstep if drift becomes a problem.
- **Tooltip suffix format.** macOS uses `⌘` glyph; Windows/Linux uses `Ctrl+`. Recommendation: detect OS at startup once and render the right glyph everywhere.

### Success criteria

- Bharadwaj can do a full editing session (new composition → enter swaras → save → export HTML → close) without touching the trackpad
- `Cmd+K` opens a palette that lists every toolbar action with its shortcut
- `?` opens a one-page cheat-sheet
- `docs/user-guide/11-keyboard-shortcuts.md` lists every shortcut and lives in the bundled user guide
- Beta testers don't need to memorize anything — the palette is enough

---

## Task 2 — BETA badge across the app

### Decision

Small orange/amber **BETA** pill in the toolbar, clickable to open the About dialog. Plus a prominent paragraph in the About dialog explaining what "beta" means in this context.

### Approach

A single config flag — `AppMode = Alpha | Beta | Stable` — sourced from `build.sbt`'s `ThisBuild / version` or a separate constant in `Versions.scala` / equivalent. The badge derives its color and label from this flag:
- Alpha → red `ALPHA` pill (so internal Bharadwaj testing is also clearly marked)
- Beta → amber `BETA` pill
- Stable → no badge (toolbar chip slot returns to nothing)

This means flipping a single constant when going from alpha → beta → v1.0 changes everything, no other code touched.

### Files touched

**Desktop**
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/MainApp.scala` — add `val AppMode: AppMode = AppMode.Beta` constant near `AppVersion`
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/ToolbarBuilder.scala` — first child in the toolbar HBox: `AppModeBadge.build(AppMode)` (returns `Option[Node]`)
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/AppModeBadge.scala` *(new)* — pill button with colored background, clickable → opens About
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/AboutDialog.scala` — add a "Release status" paragraph just under the title, themed by AppMode

**Web**
- `sangeet-web/src/State/AppMode.elm` *(new)* — mirror enum
- `sangeet-web/src/View/Toolbar.elm` — render the pill at the start of the toolbar
- `sangeet-web/src/View/Dialogs/About.elm` — release-status paragraph
- `sangeet-web/public/styles.css` — `.beta-pill` style

### Sub-decisions

- **Pill color/text** for each mode — proposing red `#C13C28` for Alpha, amber `#D97E2E` for Beta, no badge for Stable.
- **About paragraph text:**
  > "**You're using a beta build.** This means the app works but might have rough edges. Bharadwaj is actively collecting feedback before v1.0. If you spot a bug or something feels wrong, please click the 🐞 button — your reports go straight to his inbox."

### Success criteria

- Every beta tester sees an unmissable amber pill the first time they launch
- Clicking the pill → About dialog → understands what beta means
- Flipping `AppMode.Beta → AppMode.Stable` removes the pill everywhere with zero other code changes

---

## Task 3 — AI skills and harnesses recommendations

### Current state

What's already in place:
- **CLAUDE.md** — rich, domain-aware project file with Hindustani music theory, architecture principles, conventions, current implementation state
- **`docs/developer/specs/`** — design spec (the source-of-truth design doc)
- **`docs/developer/plans/`** — past planning docs
- **`.claude/settings.local.json`** — extensive tool allowlist tuned for this codebase
- **Superpowers plugin (skills)** — brainstorming + plan-mode workflows
- **lefthook** — pre-commit format hooks (scalafmt, elm-format, prettier)
- **Memory system** — auto-saved memories for project goals, architecture, user feedback

What's missing entirely:
- `.claude/agents/` — no custom subagents
- `.claude/commands/` — no custom slash commands
- `.claude/skills/` — no project-specific skills
- `.claude/hooks/` — no custom hooks beyond the default Bash allow/deny
- No custom MCP servers
- No bridge between AI agents and the running desktop app (the TCP debug console is exactly the right shape for this — see priority A below)

### Recommendations, prioritized by ROI

#### Priority A — high ROI, low effort, do first

**A1. MCP server wrapping the TCP debug console.** *Effort: ~half a day.*
The TCP debug console on `127.0.0.1:28081` already exposes ~30 commands (type, press, get-state, dump-composition, set-taal, throw, etc.). Wrapping it as an MCP server lets an AI agent drive the *running* desktop app via real tool calls — exactly the loop the user already wishes existed during UI work. Today an agent has to ask Bharadwaj to test changes manually; with this in place, an agent can write a feature → connect via MCP → exercise it via simulated keystrokes → check `get-state` → iterate. Game-changer for cross-platform feature parity and UI bug repro.
- File: `mcp-servers/sangeet-debug-console/server.ts` (or Python — whatever's simplest). Maps each TCP command to an MCP tool.
- Wire in `~/.claude.json` global MCP config so it's available across sessions.

**A2. Custom subagent: `cross-platform-parity-checker`.** *Effort: 1 hour.*
The app has a desktop + web stack with hard-won feature parity. The single most common AI mistake is shipping a feature on desktop and forgetting the Elm side (or vice versa). A subagent triggered manually (or via a hook after a feature commit) that diffs the two codebases for symmetric coverage would prevent half the future "oh, also do this for web" follow-up prompts.
- File: `.claude/agents/cross-platform-parity-checker.md`
- Tools: Read, Bash (`grep`, `find`)
- Output: a list of features present in one stack but not the other, with file paths.

**A3. Slash command `/feature-parity` and `/release`.** *Effort: 30 min each.*
- `/feature-parity` invokes the parity-checker subagent on demand
- `/release [major|minor|patch]` runs the release checklist: bump version in `build.sbt`, format check, run all tests, tag, push, watch CI

#### Priority B — medium ROI, medium effort

**B1. Skill: `hindustani-music-theory.md`.** *Effort: 2-4 hours.*
A reusable skill the AI loads when working on raag/taal/ornament code. Includes:
- All 7 swaras + variant rules (Sa/Pa fixed, others komal/tivra-able)
- The 26 raags with arohi/avrohi/vadi/samvadi
- The 11 taals with vibhag structures
- All ornament types
- Sitar-specific notation rules (mizrab, krintan, gitkari, ghaseet)
- Sahitya conventions across languages

This already lives in CLAUDE.md scattered throughout. A dedicated skill keeps the project's CLAUDE.md slimmer (faster to load every conversation) while still keeping the deep knowledge accessible *when needed*.

**B2. Custom subagent: `scala3-style-reviewer`.** *Effort: 1-2 hours.*
Reviews any new Scala code for proper Scala 3 idioms: `enum` not `sealed trait`, `given`/`using` not `implicit`, `extension` not implicit class, `case class derives` for typeclasses, no Java-isms unless needed. Catches the "AI defaulting to Scala 2 patterns" failure mode that happens periodically.

**B3. Custom subagent: `elm-architecture-reviewer`.** *Effort: 1-2 hours.*
Reviews any new Elm code for TEA conformance: pure update, no side effects in view, ports only at the boundary, Cmd composition done correctly. The Elm side has 558 tests but they don't catch architectural drift.

**B4. Hooks for auto-format on Edit/Write.** *Effort: 1 hour.*
Currently `make format` is manual. A PostToolUse hook in `.claude/settings.local.json` that runs the appropriate formatter on any file an agent edits would mean perfectly-formatted PRs without the agent having to remember. Schema:
```json
"hooks": {
  "PostToolUse": [{
    "matcher": "Edit|Write",
    "hooks": [{
      "type": "command",
      "command": "/Users/bharadwaj/Work/Code/mine/sangeet_notes_editor/.claude/format-edited-file.sh \"$CLAUDE_TOOL_INPUT\""
    }]
  }]
}
```
The script inspects the file extension and runs scalafmt / elm-format / prettier.

**B5. Skill: `release-checklist.md`.** *Effort: 1 hour.*
Step-by-step pre-release procedure (version bump, scalafmtAll, scalafix, test-all, e2e, packaging dry-run, GitHub release notes draft, tag). Pairs with the `/release` slash command from A3.

#### Priority C — lower ROI, exploratory

**C1. Custom subagent: `swar-format-validator`.** *Effort: 2-3 hours.*
Validates .swar files against the JSON schema (which doesn't formally exist yet — would need to write one). Useful when synthesizing test fixtures or when reviewing changes that touch SwarFormat.scala.

**C2. Slash command `/add-raag` and `/add-taal`.** *Effort: 1 hour each.*
Templated scaffolding for new raag/taal definitions, prompting for the canonical fields and writing a working Scala + Elm pair plus tests.

**C3. MCP server for PostHog queries.** *Effort: half a day.*
Lets agents pull recent event data from PostHog to investigate "which feature is being used least?" or "what's the most common ornament typed?". Currently this requires the user to open the PostHog UI.

**C4. CLAUDE.md restructuring.** *Effort: 1-2 hours.*
Move domain knowledge (Hindustani theory section) to a skill (per B1). Keep CLAUDE.md focused on conventions + architecture + "how this codebase wants to be worked on". Should make every conversation cheaper (less context consumed by always-loaded domain detail).

**C5. Memory hygiene pass.** *Effort: 30 min.*
The auto-memory system has 6 entries today (user profile, project goals, architecture, scala3 preference, no-auto-launch, plan-12-closed). Audit for staleness, add any feedback patterns that have emerged but aren't captured (e.g., "prefer stdin piping for secrets", "user wants me to launch the app only when explicitly asked" — these are already there but worth verifying).

### Recommended first cut

Start with **A1 + A2 + B4** in that order. They unlock the highest-leverage feedback loop (drive the running app from an agent), the most-common failure mode (cross-platform drift), and the most-annoying friction (forgetting to format). Everything else can come after beta testing reveals what's actually painful.

### Out of scope for this plan

Actually building any of the above. This task delivers a *plan* for what AI tooling to invest in. Implementation of any individual recommendation gets its own small PR after this plan is approved.

---

## Task 4 — TCP debug console docs

### Decision

New file `docs/developer/architecture/debug-console.md` covering: overview, how to connect via `nc`, full command reference (generated from / mirrored to `DebugConsole.scala` help text), example workflows (simulate a full editing session, dump state for debugging, trigger a crash for Phase 9 verification).

### Approach

This is pure documentation work — no code changes. The DebugConsole.scala help text already covers every command and is the canonical source; the doc references it and adds examples + workflow recipes that the help text can't easily show.

### Files touched

- `docs/developer/architecture/debug-console.md` *(new)*. Sections:
  - Overview (why this exists, when to use)
  - Connecting (`nc 127.0.0.1 28081`, `---END---` framing)
  - Command reference (table mirroring the help text — taal manipulation, swar typing, cursor nav, ornaments, sections, tabs, throw, diagnostics)
  - Example workflows:
    - Drive a full edit session
    - Inspect state during a UI freeze (use `thread-dump`)
    - Test Phase 9 crash recovery (`throw` + restart)
    - Verify a new feature behaves correctly
  - Caveats (loopback only by design, no auth, port collision with another instance fails fast)
- `README.md` — add a single line under "Debug tooling" linking to the new doc

### Sub-decisions

- Should the doc include a script to make connecting easier (e.g., `bin/sangeet-debug`)? Recommendation: yes, a one-liner wrapper `exec nc 127.0.0.1 28081` saves typing.
- Should we add a `--port` arg or env var for the console port? Currently hardcoded to 28081 in `DebugConsole.scala`. For now: keep hardcoded; document the limitation; revisit if multiple instances ever need to coexist.

### Success criteria

- A technical user new to the codebase can connect to the console and drive the app in under 5 minutes following the doc
- The example workflows actually work when copy-pasted
- The doc is discoverable from README

---

## Task 5 — Dismiss sample composition on startup

### Decision

**Dismiss-in-place with persistence:** the sample-Yaman tab gets a small banner at the top with a "Don't show on startup" button. Clicking it sets a config flag (`AppConfig.showSampleOnStartup: Boolean = true`); the sample is no longer loaded on subsequent launches. A toggle in the About dialog (or a future Preferences dialog) lets the user re-enable.

### Approach

A tiny banner above the sample composition's canvas, themed in muted amber. The button is the only interactive element — `Close` is just closing the tab (already supported via the close-x).

### Files touched

- `sangeet-core/src/main/scala/com/varpas/sangeet/core/config/AppConfig.scala` — add `showSampleOnStartup: Boolean = true` field with circe-derived codec
- `sangeet-core/src/main/scala/com/varpas/sangeet/core/config/ConfigCodecs.scala` — bump if needed (likely auto-handled by derivation)
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/MainApp.scala` — at startup, check `config.showSampleOnStartup` before loading the sample
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/SampleComposition.scala` (or wherever the sample is loaded) — add the banner UI as a `VBox` above the canvas
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/dialog/AboutDialog.scala` — small "Restore sample on startup" link/button shown only when the flag is false

### Sub-decisions

- **Banner text:** `"This is a read-only sample showing Yaman Vilambit Gat. ▸ [Don't show on startup]"`. Compact, single-line, dismissable.
- **Re-enable path** — for MVP: just a toggle in About dialog. Future: belongs in a Preferences dialog with other user preferences (theme default, autosave interval, etc.) once such a dialog exists.

### Success criteria

- Frequent user clicks "Don't show on startup" once → sample never appears on subsequent launches
- Sample state survives `AppConfig` schema changes (default `true` for existing configs)
- About-dialog toggle restores the sample for the next launch

---

## Task 6 — Code/test/docs growth dashboard

### Decision

A **static GitHub Pages dashboard** (`https://bharath12345.github.io/sangeet_notes_editor/`) with line charts of LoC + file count + complexity per (module × role), updated on every merge to `main`. History persisted as `history.json` on a `gh-pages` branch. Read-only insight tool — no PR-comment integration, no alerts; the user visits the URL when they want to see how the codebase has grown.

### Approach

1. **CI job on `push: main`** — checks out the repo, runs `scc --format=json` against each module directory, parses results into a per-module summary, appends a snapshot entry to `gh-pages:history.json` with timestamp + commit SHA, pushes back to `gh-pages`.
2. **Dashboard page** at `gh-pages:index.html` — fetches `history.json` and renders 3 Chart.js line charts (Code / Tests / Docs), with selectable metric (LoC | Files | Complexity) and one line per module.
3. **First run** seeds `history.json` with one snapshot of current state.

### Tool choice

**[scc](https://github.com/boyter/scc)** (Sloc Cloc and Code). Reasoning:
- Single binary, fast (Go), trivial to install in GH Actions (`apt install scc` not available, so download the release binary — ~3MB).
- Native Elm + Scala + TypeScript + Markdown support; consistent counting rules across languages.
- JSON output (`scc --format=json --by-file=false`) — gives LoC (Code/Blank/Comment), file count, **and** a complexity estimate per language, all in one pass.
- Cyclomatic complexity is a rough heuristic (counts branch/loop keywords) but is consistent across languages — good enough for trend visibility, not a code-review tool.

Alternative considered: `tokei` (Rust, faster, but Elm support is unclear and complexity-estimation is weaker). `cloc` (most mature but slowest and no complexity).

### Module + role mapping

| Module | Code paths | Test paths | Docs paths |
|---|---|---|---|
| sangeet-core | `sangeet-core/src/main/scala/**` | `sangeet-core/src/test/scala/**` | — |
| sangeet-desktop | `sangeet-desktop/src/main/scala/**` | `sangeet-desktop/src/test/scala/**` | — |
| sangeet-server | `sangeet-server/src/main/scala/**` | `sangeet-server/src/test/scala/**` | — |
| sangeet-web | `sangeet-web/src/**` | `sangeet-web/tests/**` | — |
| e2e | `e2e/helpers/**` | `e2e/tests/**` | — |
| docs | — | — | `docs/**/*.md`, `README.md`, `CLAUDE.md` |

Docs is one row keyed by "docs", since markdown isn't per-module. If `docs/user-guide/` vs `docs/developer/plans/` vs `docs/developer/` ever needs to be split, we just add columns then.

### Files touched

- `.github/workflows/metrics.yml` *(new)* — triggers on `push: main`, runs scc, updates `gh-pages:history.json`, pushes
- `scripts/collect-metrics.sh` *(new)* — invokes scc against each module, emits a single JSON snapshot object to stdout
- `scripts/append-metrics.py` *(new)* — appends the snapshot to `history.json` (Python's stdlib JSON handling is more robust than bash jq for this)
- `gh-pages` branch (new, or amends existing one if there is one):
  - `index.html` — header, three chart canvases, metric-toggle dropdown
  - `metrics.js` — fetches `history.json`, renders Chart.js charts
  - `style.css` — minimal, matches the project's amber/cream palette
  - `history.json` — array of snapshots; seeded with one entry at first deploy
- `docs/developer/operations/metrics-dashboard.md` *(new)* — explains the dashboard URL, how snapshots work, how to read the charts, how to manually trigger a snapshot

### Snapshot schema (versioned, additive-only)

```json
{
  "schemaVersion": 1,
  "snapshots": [
    {
      "timestamp": "2026-06-13T10:30:00Z",
      "commitSha": "a1b2c3d",
      "modules": {
        "sangeet-core":    { "code":    { "loc": 12450, "files": 78, "complexity": 412 },
                             "tests":   { "loc": 18200, "files": 39, "complexity": 280 } },
        "sangeet-desktop": { "code":    { "loc": 6800,  "files": 42, "complexity": 195 },
                             "tests":   { "loc": 3100,  "files": 3,  "complexity": 56  } },
        "sangeet-server":  { "code":    { "loc": 4200,  "files": 28, "complexity": 110 },
                             "tests":   { "loc": 6500,  "files": 16, "complexity": 188 } },
        "sangeet-web":     { "code":    { "loc": 9800,  "files": 45, "complexity": 320 },
                             "tests":   { "loc": 11200, "files": 22, "complexity": 230 } },
        "e2e":             { "code":    { "loc": 800,   "files": 4,  "complexity": 12  },
                             "tests":   { "loc": 4500,  "files": 14, "complexity": 90  } },
        "docs":            { "docs":    { "loc": 8900,  "files": 47, "complexity": 0   } }
      }
    }
  ]
}
```

The `schemaVersion` field lets the dashboard handle older snapshots if we ever extend the shape (e.g., add per-language breakdown later).

### Implementation steps

1. Pick a default `gh-pages` branch state. If it exists (for other GH Pages content), the metrics workflow must merge — but most projects start fresh.
2. Write `scripts/collect-metrics.sh`. Verify locally — should produce a clean JSON snapshot for the current state.
3. Write `scripts/append-metrics.py` — atomic read-mutate-write of `history.json`.
4. Build a minimal `index.html` + `metrics.js` that renders one canvas with one series. Verify locally by loading via `python3 -m http.server`.
5. Extend to all 3 charts + metric selector.
6. Write the workflow. Use `actions/checkout@v6` with `ref: gh-pages` for the read; `peaceiris/actions-gh-pages@v4` (or hand-rolled `git push`) for the write.
7. Manually trigger the first snapshot via `workflow_dispatch`. Verify dashboard renders.
8. Document in `docs/developer/operations/metrics-dashboard.md`.

### Sub-decisions

- **Snapshot frequency.** Recommend: every push to `main` (i.e., every merged PR). Pros: clean time series. Cons: many data points if you merge many small PRs. Could downsample on the client side if the chart gets noisy. Alternative: a daily cron — smoother but loses per-PR resolution.
- **Should the dashboard auto-detect modules?** Recommend: configure them explicitly in `collect-metrics.sh` for now (5 modules, doesn't change often). Auto-detect adds complexity for little gain at this scale.
- **What's "complexity"?** scc reports a single integer per file derived from branch/loop keyword counts. It's a rough proxy, not Halstead or McCabe. Document the caveat in the dashboard's footer so nobody treats it as gospel.
- **Cumulative or delta charts?** Recommend cumulative (total LoC over time) as the default; add a "show delta" toggle later if the per-PR change view becomes useful.
- **Color scheme.** Match the app's notation palette (the same `NotationColors` from sangeet-core). Distinct colors per module so the legend is readable.
- **History size limit.** None for now — at 1 snapshot per merge × 100 merges per year, the JSON stays under 100KB for years. Revisit if it hits 1MB+.

### Success criteria

- Every merge to `main` triggers the workflow; `history.json` grows by one entry
- Dashboard URL renders 3 charts within ~1s of page load
- Charts let you eyeball: "is the codebase growing? Is test growth keeping up with code growth? Are docs being maintained?"
- A new contributor can read `docs/developer/operations/metrics-dashboard.md` and understand what they're looking at in under 2 minutes

### Out of scope (deferred)

- Per-PR delta comment (explicitly skipped — dashboard-only per user choice).
- Per-language breakdown within modules.
- Test-case counts (vs test LoC) — would require per-language test parsers.
- Authors / contributors stats — already available in GitHub Insights, no value in duplicating.
- Code coverage trends — scoverage already enforces 80% min on CI; surfacing the actual percentage over time could be a Plan 14 add-on (would need scoverage to emit JSON, parse + append).
- Alerts on growth anomalies ("this PR added 5000 lines, are you sure?") — not asked for; could be added later as a PR-comment companion if useful.

---

## Open questions for review

These are decisions worth confirming before implementation starts:

1. **Shortcut bindings:** the table in Task 1 reflects standard conventions. Any specific shortcuts that conflict with your workflow or that you'd prefer different? (E.g., some users hate `Cmd+,` for properties.)
2. **Cheat-sheet trigger:** `?` (preferred) or `Cmd+/` (safer fallback)?
3. **BETA pill color:** the proposed `#D97E2E` is muted amber. Want a brighter orange / pure orange / different color entirely?
4. **AI tooling — first cut:** does the "A1 + A2 + B4" first cut feel right, or do you want to bias toward different items first?
5. **Debug console wrapper script:** ship `bin/sangeet-debug` for easier connection, or leave users to `nc` manually?
6. **Sample dismiss → About toggle:** acceptable interim location, or do you want a proper Preferences dialog as part of this plan?

---

## Implementation order

Each task is independently shippable. Recommended sequence: growth dashboard first (so all subsequent work shows up on the trend lines from day one), then the small user-facing items, then keyboard work, then AI tooling.

1. **Task 6** (growth dashboard) — first, so every subsequent PR's growth is captured in the time series from the very beginning. Half a day to a day. *Single PR + first manual snapshot.*
2. **Task 5** (sample dismiss) — smallest, isolated, immediate friction relief. Half a day. *Single PR.*
3. **Task 2** (BETA badge) — small, isolated, must ship before beta testers see the app. Half a day. *Single PR.*
4. **Task 4** (debug console docs) — pure docs, no code risk. Half a day. *Single PR.*
5. **Task 1** (keyboard + palette) — biggest task. Two phases: (a) shortcuts wired into existing scene filter + tooltip suffixes + cheat-sheet dialog + bundled doc (one PR); (b) command palette dialog + AppAction registry (separate PR). *Two PRs.*
6. **Task 3** (AI tooling) — meta. Build A1 (MCP server for debug console) first as it unblocks every other piece of agent-driven work. Then A2 + A3 + B4. Each as its own PR or scoped commit. *3-5 small PRs.*

Total: ~8-12 small PRs spread over a week or two of evenings.

---

## What's deliberately deferred

Items raised in the brainstorm that won't land in this plan:

- **PostHog dashboards** (Plan 12 leftover) — defer until beta testers generate real traffic to look at.
- **First-visit Report-a-Bug tooltip on web** (Plan 12 leftover) — adjacent to Task 2's beta-pill work; could opportunistically land in the same PR if it's easy.
- **GCP budget alert** (Plan 12 leftover) — trivial; pick up as a one-line follow-up commit.
- **Preferences dialog** — touched by Task 5 (re-enable sample) but explicitly out-of-scope; the About-dialog toggle is the interim solution. A real Preferences dialog can be a Plan 14 if/when there are >3 user-tweakable settings.
- **Auto-update banner for desktop releases** — would be lovely for beta testers, but adds release-channel infrastructure work that's not budgeted here.
- **Bug-report dedup by stack trace** — same; defer until duplicate reports actually become a problem.

---

## Beta-launch readiness checklist (for after this plan ships)

Once Plan 13 is done, the gates that need to clear before sending the app to beta testers:

- [ ] BETA pill visible in both apps; About dialog explains what beta means
- [ ] Keyboard shortcuts + cheat-sheet documented; command palette working
- [ ] Sample dismissal working; About-dialog toggle to restore
- [ ] All Plan 12 telemetry confirmed flowing (PostHog events from the beta tester's session, crash recovery + bug-report endpoints reachable from outside the dev machine)
- [ ] Packaged installers (.dmg / .msi / .deb) built via GitHub Actions on a tagged `v0.3.0-beta.1` release, with the PostHog key baked in (via PR #54 wiring)
- [ ] Web app deployed with the beta version
- [ ] Short README for beta testers (separate from the user guide): "What is alpha vs beta vs v1.0", "How to report bugs", "What feedback I'm looking for"
- [ ] (Optional) Pre-configured AppConfig template so testers start with the sample disabled if you prefer

These deliverables come *after* Plan 13 — they're the gate for actually inviting friends.
