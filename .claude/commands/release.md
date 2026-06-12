---
description: Cut a release. Usage:/release [major|minor|patch] (default: patch). Bumps version, runs all checks, tags, pushes.
---

You're cutting a release of Sangeet Notes Editor. The user invoked `/release $ARGUMENTS` where `$ARGUMENTS` is one of `major`, `minor`, `patch`, or empty (default `patch`).

The version lives in **two** places — both must move in lockstep:
- `build.sbt` line ~4: `ThisBuild / version := "X.Y.Z"`
- `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/MainApp.scala` line ~28: `val AppVersion: String = "X.Y.Z"`

## Step-by-step

1. **Read current version.** `grep -E 'ThisBuild / version' build.sbt` and `grep AppVersion sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/MainApp.scala`. Confirm both match. If they don't, **stop** and flag the mismatch to the user before bumping anything.

2. **Compute new version.** Apply semantic bump to the current version:
   - `patch`: X.Y.Z → X.Y.(Z+1)
   - `minor`: X.Y.Z → X.(Y+1).0
   - `major`: X.Y.Z → (X+1).0.0
   - empty: same as `patch`

3. **Confirm with the user.** Show them: current → new version, which type of bump, and ask if they want to proceed. Do NOT make changes without confirmation.

4. **Branch + bump.** Create a branch `release/vX.Y.Z`. Edit both files. Commit with message `chore(release): vX.Y.Z`.

5. **Pre-release checks.** Run in parallel:
   - `sbt scalafmtCheckAll` — format check
   - `sbt "scalafixAll --check"` — lint check
   - `sbt sangeetCore/test sangeetServer/test` — Scala tests
   - `cd sangeet-web && npx elm-format src/ tests/ --validate && npx elm-review && npx elm-test` — Elm checks
   - `cd e2e && npx eslint . && npx prettier --check .` — TS checks
   
   If any check fails, **stop** and report. Do not push.

6. **Push branch + open PR.** `git push -u origin release/vX.Y.Z`, then `gh pr create --title "chore(release): vX.Y.Z" --body "<changelog summary>"`.

7. **Watch CI.** Poll `gh pr checks` every ~60s until all required checks pass.

8. **Merge.** `gh pr merge <num> --squash --delete-branch`. Confirm merge succeeded.

9. **Tag the release.** After the squash-merge lands on main:
   - `git checkout main && git pull --ff-only`
   - `git tag -a vX.Y.Z -m "Release vX.Y.Z"`
   - `git push origin vX.Y.Z`

10. **Trigger the packaging workflow.** `gh workflow run package.yml --ref vX.Y.Z` if the workflow supports `workflow_dispatch`. Else, the tag push already triggers it.

11. **Report.** Tell the user:
    - The new version
    - The PR URL (now merged)
    - The tag (now pushed)
    - The packaging workflow URL (so they can grab the .dmg/.msi/.deb when it finishes)

## Safety

- **Never** push the tag before the PR merges.
- **Never** force-push to `main` or to a release tag.
- If any pre-release check fails, **stop** — do not silently fix and continue.
- If CI fails on the release PR, surface the failure and let the user decide whether to fix-forward or abandon.
- The `--no-verify` / `--no-gpg-sign` flags are forbidden.
