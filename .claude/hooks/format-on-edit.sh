#!/usr/bin/env bash
# PostToolUse hook: auto-format the file that an Edit/Write tool call just
# modified. Dispatches by extension. Fails silently if a formatter is missing
# or the file is outside the repo — we'd rather skip than block the agent.
#
# Wired in .claude/settings.json. Receives a JSON payload on stdin with
# tool_input.file_path; we extract the path with jq, decide which formatter
# to run, then exit 0 regardless of formatter outcome.
#
# Why exit 0 on failure: a hook that exits non-zero blocks the tool. Agents
# can already see formatter errors via the lint job in CI; failing here would
# only frustrate the iteration loop.

set -u

payload=$(cat)
file=$(printf '%s' "$payload" | jq -r '.tool_input.file_path // empty' 2>/dev/null || true)

if [ -z "$file" ] || [ ! -f "$file" ]; then
  exit 0
fi

repo_root=$(git rev-parse --show-toplevel 2>/dev/null || true)
if [ -z "$repo_root" ]; then
  exit 0
fi

# Only format files inside this repo. Tools sometimes edit files in /tmp or
# outside the worktree — we shouldn't run our formatters there.
case "$file" in
  "$repo_root"/*) ;;
  *) exit 0 ;;
esac

ext="${file##*.}"

case "$ext" in
  scala)
    # scalafmt --stdin would be cleaner but we don't pipe content here.
    # Targeted format: cd into the repo, run sbt with a tight task that only
    # formats the modified file. Skip silently if sbt isn't on PATH.
    if command -v sbt >/dev/null 2>&1; then
      ( cd "$repo_root" && sbt -batch "scalafmtOnly $file" ) >/dev/null 2>&1 || true
    fi
    ;;
  elm)
    if [ -x "$repo_root/sangeet-web/node_modules/.bin/elm-format" ]; then
      "$repo_root/sangeet-web/node_modules/.bin/elm-format" --yes "$file" >/dev/null 2>&1 || true
    fi
    ;;
  ts|tsx|js|jsx|css|json|md|yml|yaml)
    if [ -x "$repo_root/node_modules/.bin/prettier" ]; then
      "$repo_root/node_modules/.bin/prettier" --write "$file" >/dev/null 2>&1 || true
    fi
    ;;
esac

exit 0
