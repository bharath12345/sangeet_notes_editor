#!/usr/bin/env python3
"""Append a snapshot to history.json on the gh-pages branch.

Usage: append_snapshot.py <history.json path> <snapshot.json path>

- Creates history.json with a fresh schema if it doesn't exist.
- Appends `snapshot` to `snapshots[]`, deduped by `sha` (later run wins —
  keeps the timestamps monotonic if the same commit gets re-snapshotted).
- Sorts the array by timestamp before writing so the dashboard's X-axis
  is always chronological even if a workflow ran out of order.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: append_snapshot.py <history.json> <snapshot.json>", file=sys.stderr)
        return 2

    history_path = Path(sys.argv[1])
    snapshot_path = Path(sys.argv[2])

    snapshot = json.loads(snapshot_path.read_text())

    if history_path.exists():
        history = json.loads(history_path.read_text())
    else:
        history = {"schema": 1, "snapshots": []}

    # Dedupe by sha — drop any prior entry with the same commit, then append.
    history["snapshots"] = [s for s in history["snapshots"] if s.get("sha") != snapshot["sha"]]
    history["snapshots"].append(snapshot)
    history["snapshots"].sort(key=lambda s: s["timestamp"])

    history_path.write_text(json.dumps(history, indent=2) + "\n")
    print(f"appended sha={snapshot['sha'][:8]} → {len(history['snapshots'])} snapshot(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
