# Growth Dashboard

A static dashboard that tracks repository growth over time, refreshed on every push to `main`. Lives at:

> `https://<user>.github.io/<repo>/metrics/`

Three line charts — **Code**, **Tests**, **Docs** — each with one line per module. You can switch the Y-axis between Lines of Code, Files, or Complexity via the radio buttons at the top.

## How it works

1. **Snapshot.** On every push to `main`, the `Deploy Frontend + Metrics (GitHub Pages)` workflow runs [`scripts/collect_metrics.py`](../../scripts/collect_metrics.py), which invokes [scc](https://github.com/boyter/scc) against each module's source dirs and emits a JSON snapshot.
2. **Append.** [`scripts/append_snapshot.py`](../../scripts/append_snapshot.py) appends the snapshot to [`scripts/dashboard/history.json`](../../scripts/dashboard/history.json), deduped by commit SHA, sorted by timestamp.
3. **Commit back.** The workflow commits the updated `history.json` back to `main` with `[skip ci]`. Because the commit uses `GITHUB_TOKEN`, it does **not** re-trigger the workflow — no infinite loop.
4. **Deploy.** The Elm app and the dashboard (`scripts/dashboard/*` + `history.json`) get bundled into one GitHub Pages artifact and deployed atomically — Elm at `/`, dashboard at `/metrics/`.

## Module → role mapping

Defined in `MODULES` at the top of `scripts/collect_metrics.py`. To track a new module:

```python
MODULES["new-module"] = {
    "code":  ["new-module/src/main"],
    "tests": ["new-module/src/test"],
    "docs":  [],
}
```

To make the new module appear on the dashboard, also add it to `MODULE_COLORS` in `scripts/dashboard/metrics.js` (lines that are zero everywhere are hidden automatically, so existing snapshots without the module won't break the chart).

## Excluded files

`scripts/collect_metrics.py` passes scc `--exclude-file elm.js,package-lock.json,...` — the generated Elm bundle (`sangeet-web/public/elm.js`) is the main thing this catches. Without that exclusion, sangeet-web/code shows ~23k LoC instead of the actual ~8k.

`--exclude-dir` skips `node_modules`, `elm-stuff`, `target`, `build`, IDE dirs, etc.

## Running locally

```bash
brew install scc                                              # one-time
./scripts/collect_metrics.py > /tmp/snap.json                 # snapshot the working tree
./scripts/append_snapshot.py scripts/dashboard/history.json /tmp/snap.json
cd scripts/dashboard && python3 -m http.server 8000           # preview at http://localhost:8000
```

## What's not tracked (yet)

- **Per-language breakdown** within a module (e.g. Scala vs Java vs SQL inside sangeet-core). All languages are summed under `loc`.
- **Per-file complexity hot-spots** — only module-aggregate complexity.
- **Coverage** — that's a separate dimension; coverage data lives in CI artifacts.

If any of these become useful, extend `collect_metrics.py` — the workflow and dashboard don't need to change so long as new fields land alongside `loc`/`files`/`complexity` in the per-role dict.
