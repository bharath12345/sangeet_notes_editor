# Renovate — Dependency Update Automation

## What it is

[Renovate](https://docs.renovatebot.com/) is a GitHub App that watches every dependency file in the repo (npm `package.json`, sbt `build.sbt`, GitHub Actions YAML, Python `pyproject.toml`, etc.), notices when a newer version is released upstream, and opens a pull request that bumps it. CI then runs against the bump; if it stays green and any auto-merge rules apply, the PR merges itself.

We use Renovate instead of GitHub's built-in Dependabot because Dependabot doesn't speak sbt (it shipped a "preview" only) and gives no control over grouping, scheduling, or auto-merge policy. Renovate handles every language we use in one config file.

## Setup state

- **Config:** `renovate.json` at the repo root — single source of truth, version-controlled like any other file. Edit it via PR.
- **App install:** Renovate is installed as a GitHub App on this repo by the repo owner (`bharath12345`). Access is granted only to this repo, not the full account.
- **Old Dependabot config:** `.github/dependabot.yml` was removed in PR #106 when Renovate took over.
- **Dashboards:** The Renovate-hosted dashboard at <https://app.renovatebot.com/dashboard> shows scan history, errors, and the queue. The bot also opens a "Dependency Dashboard" issue inside the repo (auto-updating) when there's a backlog of pending updates.

## How a scan happens

A scan is when Renovate reads `renovate.json`, reads every manifest file (`package.json`, `build.sbt`, etc.), queries each registry (npm, Maven Central, GitHub API) for newer versions, and decides what PRs to open.

Three triggers:

1. **Push to default branch** — every commit to `main` triggers a scan within a few minutes via a webhook from GitHub to Renovate. This is the most common trigger.
2. **Renovate's own cron** — the Mend-hosted Renovate runs at least once an hour for every installed repo, independent of pushes. So even if no one pushes for a week, you still get a scan.
3. **Manual** — clicking "Recreate" or "Run now" on the repo's job page at app.renovatebot.com forces a scan immediately.

A scan _can complete and open zero PRs_ if our config tells it to wait (see "Schedule" below) or if every dependency is already current.

## The config — what it does, in plain terms

`renovate.json` has a few interesting sections. The full file is the source of truth; this is a guided tour.

### `extends: ["config:recommended", ":semanticCommitTypeAll(chore)"]`

`config:recommended` is Renovate's batteries-included preset (sensible defaults — auto-create the Dependency Dashboard issue, group monorepo packages together, separate major from minor/patch, etc.). `:semanticCommitTypeAll(chore)` makes every Renovate PR's commit prefix `chore(...)` so they don't pollute the changelog as features or fixes.

### `schedule` (currently absent; will be restored)

When present (e.g. `"schedule": ["before 8am on monday"]`), Renovate scans whenever it likes but only _opens PRs_ within that window. We use this to batch all the week's noise into a single Monday morning review. While the field is absent, PRs open as soon as a scan finds them.

The schedule was temporarily removed in PR #134 to flush the first batch of updates immediately. A follow-up PR restores it once the backlog drains.

### `prConcurrentLimit: 10`, `prHourlyLimit: 4`

Safety caps. Renovate will never have more than 10 open PRs from itself at once, and never opens more than 4 per hour. Prevents runaway days where a hundred packages upgrade simultaneously and flood the queue.

### `rangeStrategy: "bump"`

When updating `^1.2.3` to a newer version, _change the range itself_ (to `^1.2.4`), don't just update the lockfile. Keeps `package.json` honest about what we've actually tested against.

### `packageRules`

The interesting part. Renovate evaluates rules in order; later rules override earlier ones for matching packages. Ours do four things:

1. **Grouping** — instead of one PR per package, related updates share a PR.
   - `tapir + http4s` — Tapir and http4s versions move in lockstep; a half-update breaks compile
   - `circe` — all `io.circe.*` packages bump together
   - `micrometer + prometheus` — observability stack moves as one
   - `scalatest`, `playwright`, `prettier + eslint`, `github-actions` — same idea
2. **Auto-merge** — patch-level dev-dependency updates and GitHub Actions patch+minor updates merge themselves once CI passes. No human review needed for `lefthook 1.6.4 → 1.6.5` or `actions/checkout@v4.1.1 → v4.1.2`.
3. **Pins (`enabled: false`)** — Renovate is explicitly told never to bump:
   - `scala3-library` / `scala-library` — the Scala 3 minor version is a project-wide decision, not a Renovate one
   - `elm` — Renovate has no native Elm manager, so this is defensive against any custom regex manager added later

### `lockFileMaintenance: { enabled: true }`

Once a week, Renovate refreshes lockfiles (`package-lock.json`) even when no manifest entry changed. Picks up transitive-dependency security fixes that haven't yet caused a direct-dep version bump.

### Per-manager toggles (`sbt`, `pip_requirements`, etc.)

Defensive — these are on by default in `config:recommended` but explicitly enabling them documents intent and survives future preset changes.

### `vulnerabilityAlerts` + `osvVulnerabilityAlerts`

When GitHub's vulnerability database or the OSV database flags a CVE in one of our dependencies, Renovate opens a PR labeled `security` _immediately_, regardless of schedule. Critical fixes don't wait for Monday.

### `ignorePaths`

Don't scan generated directories (`node_modules`, `target`, `elm-stuff`), the per-worktree subdirectories we use for parallel agent work (`.claude/worktrees`), or test fixtures.

## Operating procedures

### "Renovate isn't opening any PRs"

Most likely the schedule is gating them. Check the Dependency Dashboard issue on GitHub or the dashboard at app.renovatebot.com — it lists pending updates the schedule is holding back. To flush them now, open a one-shot PR removing the `schedule` field, merge it, let Renovate open everything within an hour, then restore the schedule in a follow-up PR.

Alternatively, click "Recreate" on the repo's job page at <https://app.renovatebot.com/dashboard> — this triggers a scan that ignores the schedule for that one run.

### "I see 'Repository problems' on the dashboard"

The bot found something wrong with `renovate.json` and skipped the scan entirely. Open the dashboard, expand "Repository problems", read the message, fix `renovate.json`, open a PR. Past examples:

- _"Invalid configuration option: elm"_ — we declared an `elm` manager that Renovate doesn't support. Fix in PR #133 dropped the bad key.

### "A Renovate PR is failing CI"

Treat it exactly like any other PR: read the failure, push a fix commit to the Renovate-managed branch (`renovate/...`), and Renovate will pick up your changes and continue. Or close the PR with a comment explaining why we're not taking that update yet — Renovate will respect the close and not reopen until the upstream version changes again.

### "I want to skip / pin / hold an update permanently"

Add a `packageRules` entry in `renovate.json` matching the package with `"enabled": false` (skip entirely) or `"allowedVersions": "<2.0"` (pin to a major). Examples already in our config: `scala3-library`, `elm`.

### "I want to change the schedule"

Edit the top-level `schedule` field — Renovate's [schedule syntax docs](https://docs.renovatebot.com/configuration-options/#schedule) explain the grammar (`"after 10pm and before 5am every weekday"`, `"every weekend"`, etc.). Timezone is set by the `timezone` field (currently `Asia/Kolkata`).

### "Renovate opened 30 PRs and I'm overwhelmed"

The `prConcurrentLimit` cap is the safety net here — if we hit 10 open PRs, Renovate pauses opening new ones until some close. If the limit feels too high, lower it. The auto-merge rules also keep the queue moving without human intervention for the low-risk updates.

### "Auto-merge isn't merging a PR I think it should"

Three things must all be true: (1) CI is green on the PR, (2) the PR matches one of the `automerge: true` `packageRules`, and (3) GitHub's branch protection lets the bot push. Our repo currently has branch protection that requires the `CI Merge Gate` check — once green, Renovate's auto-merge will fire. If a PR is sitting green and unmerged after an hour, check the Renovate dashboard for that PR's "Renovate Bot" comment — it explains why.

### "Renovate is touching files it shouldn't"

Add the path to `ignorePaths`. Force a rescan with a push to `main` or "Recreate" on the dashboard.

## Where to look when things break

| Symptom                            | First place to look                                             |
| ---------------------------------- | --------------------------------------------------------------- |
| No PRs opening                     | Renovate dashboard → repo job → schedule + problems sections    |
| Config-validation error            | Renovate dashboard → "Repository problems"                      |
| One PR stuck                       | The PR's Renovate Bot comment thread                            |
| Auto-merge not firing              | PR's branch protection + check status                           |
| Need to see what's queued          | The "Dependency Dashboard" issue inside the repo (auto-created) |
| Want to know what Renovate scanned | Dashboard → repo job → expand the most recent run               |

## Elm — outside Renovate

Renovate has no native Elm manager, so `sangeet-web/elm.json` is updated by a separate GitHub Actions workflow: `.github/workflows/elm-deps-monthly.yml`. Once a month (1st of the month at 03:13 UTC = 08:43 IST) it runs `elm-json install` against every direct Elm dependency, runs `elm-test` against the new versions, and opens a PR titled `chore(deps): monthly Elm dependency update (YYYY-MM)` if anything changed. The PR body shows whether `elm-test` passed — so review is "scan the diff, glance at the badge, merge or close". If no dependency moved, the workflow exits without opening a PR (no noise on stable months).

You can also trigger the workflow on demand via the **Actions** tab → **Elm Dependencies (Monthly)** → **Run workflow**.

## Related files

- `renovate.json` — the Renovate config
- `.github/workflows/elm-deps-monthly.yml` — the Elm-only update workflow
- `.github/workflows/*.yml` — the CI Renovate PRs must pass
- `docs/developer/plans/plan-19-property-based-testing.md` — example of a parallel migration that also went through Renovate-style version pinning (`scalatestplus-scalacheck` pinned to `3.2.18.0` to match ScalaTest)
