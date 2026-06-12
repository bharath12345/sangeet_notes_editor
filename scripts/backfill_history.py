#!/usr/bin/env python3
"""Backfill history.json from every commit on main since the repo started.

The metrics dashboard only sees the future once it's set up — the past is dark
unless we replay the commit history through scc. This script does exactly that:
checks out each commit on main into a throwaway git worktree, runs the existing
collect_metrics.py against it, and writes a single history.json containing one
snapshot per commit.

Why a worktree: keeps the user's primary working tree untouched (no .git/HEAD
flicker, no risk of clobbering uncommitted work). The worktree is created in a
temp directory and removed at the end.

Why first-parent: linearises merge commits into "the main timeline" so we get
one snapshot per merge to main (PR-sized chunks) instead of N intermediate
commits per merge. The dashboard becomes "how the codebase looked after each
PR", which is what a growth chart wants.

Usage:
    scripts/backfill_history.py                       # writes /tmp/history-backfill.json
    scripts/backfill_history.py --output history.json # write to a specific path
    scripts/backfill_history.py --limit 20            # only the latest 20 commits (testing)
    scripts/backfill_history.py --branch main         # which branch to walk (default: main)

The output file is a drop-in replacement for the dashboard's history.json. To
publish: trigger the .github/workflows/backfill-metrics.yml workflow (runs this
script on CI and force-updates the metrics-data branch).

Idempotent: re-running with the same args produces byte-identical output (modulo
the worktree path) because each snapshot is keyed by commit SHA and timestamp.

Performance: ~1s per commit for scc; ~150 commits ≈ 3 minutes. Reasonable for a
one-time backfill that runs in CI.
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path


def git(args: list[str], cwd: str | None = None) -> str:
    return subprocess.run(
        ["git", *args], cwd=cwd, capture_output=True, text=True, check=True
    ).stdout.strip()


def list_commits(branch: str, repo_root: str, limit: int | None) -> list[str]:
    """Return commits on `branch` in chronological order (oldest first).
    Uses --first-parent so we get one entry per merge to main, not the entire
    side-branch ancestry of each merge.
    """
    args = ["rev-list", "--reverse", "--first-parent", branch]
    shas = git(args, cwd=repo_root).splitlines()
    if limit is not None:
        # When limited, take the latest N (still chronological) so the trailing
        # slice of history is exercised — that's the most likely test target.
        shas = shas[-limit:]
    return shas


def snapshot_commit(worktree: str, sha: str, collect_script: str) -> dict | None:
    """Check out `sha` in the worktree and run collect_metrics against it.
    Returns the parsed snapshot dict, or None if the commit can't be analyzed
    (e.g. very early commits that lack a buildable layout — we just skip).
    """
    # Detached checkout — the worktree HEAD floats per commit; no branch state.
    # --quiet keeps the output clean across hundreds of iterations.
    try:
        git(["checkout", "--quiet", "--detach", sha], cwd=worktree)
    except subprocess.CalledProcessError as e:
        print(f"  ! skipping {sha[:8]}: checkout failed ({e.stderr.strip()})", file=sys.stderr)
        return None

    # Committer date, not author date — committer is what `git log` shows by
    # default and matches "when this landed on main" semantics.
    committer_iso = git(["log", "-1", "--pretty=%cI", sha], cwd=worktree)
    # Normalise to UTC Z form to match the live workflow's output.
    # `%cI` is strict ISO-8601 with a numeric offset like 2026-06-12T14:33:00+05:30.
    # Convert via Python so dashboard timestamps are uniform.
    from datetime import datetime
    dt = datetime.fromisoformat(committer_iso).astimezone()
    from datetime import timezone as _tz
    timestamp = dt.astimezone(_tz.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    try:
        result = subprocess.run(
            [
                sys.executable,
                collect_script,
                "--repo-root",
                worktree,
                "--timestamp",
                timestamp,
            ],
            capture_output=True,
            text=True,
            check=True,
        )
    except subprocess.CalledProcessError as e:
        print(
            f"  ! skipping {sha[:8]}: collect_metrics failed ({e.stderr.strip()[:200]})",
            file=sys.stderr,
        )
        return None

    return json.loads(result.stdout)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--branch", default="main", help="Branch to walk (default: main)")
    parser.add_argument(
        "--output",
        default="/tmp/history-backfill.json",
        help="Path to write the populated history.json (default: /tmp/history-backfill.json)",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Process only the latest N commits (useful for smoke-testing)",
    )
    args = parser.parse_args()

    repo_root = git(["rev-parse", "--show-toplevel"], cwd=".")
    collect_script = os.path.join(repo_root, "scripts", "collect_metrics.py")
    if not os.path.isfile(collect_script):
        print(f"collect_metrics.py not found at {collect_script}", file=sys.stderr)
        return 2

    # Resolve to a remote-tracking ref if local doesn't exist (e.g. on CI's
    # checkout depth=1 — `main` may not be available locally).
    branch = args.branch
    try:
        git(["rev-parse", "--verify", branch], cwd=repo_root)
    except subprocess.CalledProcessError:
        branch = f"origin/{args.branch}"
        git(["rev-parse", "--verify", branch], cwd=repo_root)  # fail loud if even this is missing

    commits = list_commits(branch, repo_root, args.limit)
    print(f"Found {len(commits)} commits on {branch}", file=sys.stderr)

    with tempfile.TemporaryDirectory(prefix="sangeet-backfill-wt-") as tmp:
        worktree = os.path.join(tmp, "wt")
        # Worktree starts at the head commit; we'll detach-checkout each in turn.
        git(["worktree", "add", "--quiet", "--detach", worktree, commits[-1]], cwd=repo_root)
        try:
            snapshots: list[dict] = []
            for i, sha in enumerate(commits, 1):
                snap = snapshot_commit(worktree, sha, collect_script)
                if snap is not None:
                    snapshots.append(snap)
                    if i % 10 == 0 or i == len(commits):
                        print(
                            f"  [{i}/{len(commits)}] {sha[:8]} {snap['timestamp']} "
                            f"loc={sum(m['code']['loc'] + m['tests']['loc'] + m['docs']['loc'] for m in snap['modules'].values())}",
                            file=sys.stderr,
                        )
        finally:
            # Always clean up the worktree, even on Ctrl-C / exception.
            subprocess.run(
                ["git", "worktree", "remove", "--force", worktree],
                cwd=repo_root,
                capture_output=True,
            )

    snapshots.sort(key=lambda s: s["timestamp"])
    history = {"schema": 1, "snapshots": snapshots}
    Path(args.output).write_text(json.dumps(history, indent=2) + "\n")
    print(
        f"\nWrote {len(snapshots)} snapshots → {args.output}\n"
        f"To publish: trigger .github/workflows/backfill-metrics.yml (workflow_dispatch)",
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
