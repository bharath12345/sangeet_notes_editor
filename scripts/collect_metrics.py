#!/usr/bin/env python3
"""Snapshot per-module repository metrics using scc.

Output: JSON document on stdout shaped as:
    {
      "sha": "<commit sha>",
      "timestamp": "<ISO-8601 UTC>",
      "subject": "<commit subject>",
      "modules": {
        "<module>": {
          "code":  {"loc": int, "files": int, "complexity": int},
          "tests": {"loc": int, "files": int, "complexity": int},
          "docs":  {"loc": int, "files": int, "complexity": int}
        },
        ...
      }
    }

`loc` is scc's `Code` field (executable lines, excluding blanks/comments) — the
metric most reviewers actually care about. `complexity` is scc's heuristic
cyclomatic-ish score: bumps for branches/loops/etc.

Invoked by .github/workflows/metrics.yml on every push to main. Also runnable
locally for ad-hoc snapshots. Requires `scc` on PATH (see README in this dir).
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from datetime import datetime, timezone

# Module roster + the directories that count as code/tests/docs for each.
# Paths are repo-relative. Missing dirs are silently skipped, so this list can
# grow with new modules without breaking older snapshots.
MODULES: dict[str, dict[str, list[str]]] = {
    "sangeet-core": {
        "code": ["sangeet-core/src/main"],
        "tests": ["sangeet-core/src/test"],
        "docs": [],
    },
    "sangeet-desktop": {
        "code": ["sangeet-desktop/src/main"],
        "tests": ["sangeet-desktop/src/test"],
        "docs": [],
    },
    "sangeet-server": {
        "code": ["sangeet-server/src/main"],
        "tests": ["sangeet-server/src/test"],
        "docs": [],
    },
    "sangeet-web": {
        # Elm sources + JS/CSS interop bundle = production code.
        "code": ["sangeet-web/src", "sangeet-web/public"],
        "tests": ["sangeet-web/tests"],
        "docs": [],
    },
    "e2e": {
        # Playwright suite is 100% tests — no production code lives here.
        "code": [],
        "tests": ["e2e/helpers", "e2e/tests"],
        "docs": [],
    },
    "docs": {
        "code": [],
        "tests": [],
        # Everything under docs/ counts, including subdirs (user-guide, plans, developer, ...).
        "docs": ["docs"],
    },
}

# Directories scc should never walk into. Build/IDE/dependency junk only —
# the actual filtering of which files count is done by MODULES above.
EXCLUDE_DIRS = ",".join(
    [
        "node_modules",
        "elm-stuff",
        "target",
        "build",
        "dist",
        ".git",
        ".bsp",
        ".idea",
        ".claude",
        ".superpowers",
        "playwright-report",
        "test-results",
    ]
)

# scc has built-in defaults (package-lock.json, etc.). Add our generated assets
# so they don't inflate sangeet-web's LoC — `elm.js` is the compiled bundle
# (~16k lines of generated JavaScript), not anything we wrote by hand.
EXCLUDE_FILES = ",".join(
    [
        "package-lock.json",
        "Cargo.lock",
        "yarn.lock",
        "pubspec.lock",
        "Podfile.lock",
        "pnpm-lock.yaml",
        "elm.js",
    ]
)


def run_scc(paths: list[str], repo_root: str) -> dict:
    """Aggregate scc totals over the given paths. Returns
    {"loc": int, "files": int, "complexity": int}. Missing paths contribute 0.
    """
    existing = [p for p in paths if os.path.isdir(os.path.join(repo_root, p))]
    if not existing:
        return {"loc": 0, "files": 0, "complexity": 0}

    cmd = [
        "scc",
        "--format",
        "json",
        "--no-cocomo",
        "--exclude-dir",
        EXCLUDE_DIRS,
        "--exclude-file",
        EXCLUDE_FILES,
        *existing,
    ]
    result = subprocess.run(cmd, cwd=repo_root, capture_output=True, text=True, check=True)
    # scc emits an array of language summaries; sum the fields we need.
    summaries = json.loads(result.stdout)
    return {
        "loc": sum(int(s.get("Code", 0)) for s in summaries),
        "files": sum(int(s.get("Count", 0)) for s in summaries),
        "complexity": sum(int(s.get("Complexity", 0)) for s in summaries),
    }


def git(args: list[str], repo_root: str) -> str:
    return subprocess.run(
        ["git", *args], cwd=repo_root, capture_output=True, text=True, check=True
    ).stdout.strip()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument(
        "--repo-root",
        default=None,
        help="Directory to snapshot (defaults to git --show-toplevel of cwd). "
        "Used by backfill_history.py to point at a worktree at an older commit.",
    )
    parser.add_argument(
        "--timestamp",
        default=None,
        help="ISO-8601 UTC timestamp to record (defaults to now). Used by backfill_history.py "
        "to record the committer date of the historical commit, not 'now'.",
    )
    args = parser.parse_args()

    repo_root = args.repo_root or git(["rev-parse", "--show-toplevel"], ".")

    snapshot = {
        "sha": git(["rev-parse", "HEAD"], repo_root),
        "timestamp": args.timestamp
        or datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "subject": git(["log", "-1", "--pretty=%s"], repo_root),
        "modules": {},
    }

    for module, roles in MODULES.items():
        snapshot["modules"][module] = {
            role: run_scc(paths, repo_root) for role, paths in roles.items()
        }

    json.dump(snapshot, sys.stdout, indent=2)
    sys.stdout.write("\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
