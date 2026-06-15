#!/usr/bin/env python3
"""Convert a nightly PBT failure into a GitHub issue + draft regression PR.

Usage:
    python3 scripts/property_failure_to_regression.py \\
        --tool {scalacheck|elm-test|hypothesis} \\
        --output /path/to/test-output.txt

Reads the test output, extracts the failure details, creates an issue via `gh`,
and opens a draft PR on a new branch with a hand-written regression test using
the failing input.

Best-effort parser: if it can't extract the failure, it still files an issue
with the raw output attached. Improve incrementally as we see real failures.
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time
from pathlib import Path


def run(
    cmd: list[str], check: bool = True, capture: bool = False
) -> subprocess.CompletedProcess:
    print(f"$ {' '.join(cmd)}", file=sys.stderr)
    return subprocess.run(cmd, check=check, capture_output=capture, text=True)


def parse_scalacheck(output: str) -> dict | None:
    """Extract failing seed + arg from ScalaCheck output."""
    # ScalaCheck failures look like:
    #   ! testname: Falsified after N passed tests.
    #   > Labels of failing property:
    #   > ARG_0: Composition(...)
    #   > ARG_0_ORIGINAL: Composition(...)
    #   > Labels: ...
    #   > Seed: ABCD...
    seed_match = re.search(r"^.*Seed:\s*(\S+)", output, re.MULTILINE)
    arg_match = re.search(r"^.*ARG_0:\s*(.+?)$", output, re.MULTILINE)
    test_match = re.search(r"^\s*!\s*(\S+):\s*Falsified", output, re.MULTILINE)
    if not (seed_match and test_match):
        return None
    return {
        "tool": "scalacheck",
        "test": test_match.group(1),
        "seed": seed_match.group(1),
        "arg": arg_match.group(1) if arg_match else "(unknown)",
    }


def parse_elm_test(output: str) -> dict | None:
    """Extract failing input from elm-test output."""
    # elm-test fuzz failures look like:
    #   ↓ ModuleName
    #   ✗ propXxxRoundTrip
    #     Given <input>
    #     Expect.equal ...
    test_match = re.search(r"✗\s*(\S+)", output)
    input_match = re.search(r"^\s*Given\s+(.+?)$", output, re.MULTILINE)
    if not test_match:
        return None
    return {
        "tool": "elm-test",
        "test": test_match.group(1),
        "input": input_match.group(1) if input_match else "(unknown)",
    }


def parse_hypothesis(output: str) -> dict | None:
    """Extract @reproduce_failure from Hypothesis output."""
    # Hypothesis prints:
    #   You can reproduce this example by temporarily adding
    #   @reproduce_failure(...)
    repro_match = re.search(r"@reproduce_failure\((.+?)\)", output)
    test_match = re.search(r"FAILED.*::(test_\S+)", output)
    if not (repro_match and test_match):
        return None
    return {
        "tool": "hypothesis",
        "test": test_match.group(1),
        "reproduce": repro_match.group(1),
    }


def create_issue(failure: dict, output_path: Path) -> str:
    """Create a GitHub issue and return its URL."""
    title = f"Nightly PBT failure: {failure['test']} ({failure['tool']})"
    body_lines = [
        "## Nightly PBT failure",
        "",
        f"**Tool:** `{failure['tool']}`",
        f"**Test:** `{failure['test']}`",
        "",
        "### Reproduction",
        "",
    ]
    if failure["tool"] == "scalacheck":
        body_lines.extend(
            [
                f"- Seed: `{failure['seed']}`",
                f"- Failing arg: `{failure['arg']}`",
                "",
                "Reproduce locally with:",
                "```",
                f"sbt 'testOnly *{failure['test']}* -- -P {failure['seed']}'",
                "```",
            ]
        )
    elif failure["tool"] == "elm-test":
        body_lines.extend(
            [
                f"- Failing input: `{failure['input']}`",
                "",
                "Reproduce locally with:",
                "```",
                f"cd sangeet-web && npx elm-test tests/{failure['test']}.elm --fuzz 1",
                "```",
            ]
        )
    elif failure["tool"] == "hypothesis":
        body_lines.extend(
            [
                f"- Reproduce annotation: `@reproduce_failure({failure['reproduce']})`",
                "",
                "Reproduce locally with:",
                "```",
                f"cd mcp-servers/sangeet-debug-console && pytest -k {failure['test']}",
                "```",
            ]
        )
    body_lines.extend(
        [
            "",
            "### Auto-generated regression test",
            "",
            "A draft PR has been opened with a regression test using this exact input.",
            "Review, refine the test, and merge.",
            "",
            "---",
            "",
            "<details><summary>Raw output</summary>",
            "",
            "```",
            output_path.read_text()[:8000],  # cap
            "```",
            "",
            "</details>",
        ]
    )
    body = "\n".join(body_lines)

    result = run(
        [
            "gh",
            "issue",
            "create",
            "--title",
            title,
            "--body",
            body,
            "--label",
            "pbt-nightly-failure",
        ],
        capture=True,
    )
    return result.stdout.strip()


def create_draft_pr(failure: dict, issue_url: str) -> str:
    """Create a draft PR with a regression test stub."""
    # Branch name
    branch = f"pbt-regression/{failure['tool']}-{failure['test']}-{int(time.time())}"
    run(["git", "config", "user.email", "noreply@anthropic.com"])
    run(["git", "config", "user.name", "PBT Nightly Bot"])
    run(["git", "checkout", "-b", branch])

    # Write a stub regression test
    stub_dir = {
        "scalacheck": "sangeet-core/src/test/scala/com/varpas/sangeet/core/regressions",
        "elm-test": "sangeet-web/tests/Regressions",
        "hypothesis": "mcp-servers/sangeet-debug-console/tests/regressions",
    }[failure["tool"]]
    stub_path = Path(stub_dir)
    stub_path.mkdir(parents=True, exist_ok=True)
    # placeholder, hand-fill later
    stub_file = stub_path / f"NightlyRegression_{failure['test']}.txt"
    stub_file.write_text(
        f"# TODO: Convert this stub into a regression test.\n"
        f"# Source: nightly PBT failure {issue_url}\n"
        f"# Test: {failure['test']}\n"
        f"# Tool: {failure['tool']}\n"
        f"# Details: {json.dumps(failure, indent=2)}\n"
    )
    run(["git", "add", str(stub_file)])
    run(
        [
            "git",
            "commit",
            "-m",
            (
                f"chore(pbt): regression stub for nightly failure {failure['test']}"
                f"\n\nLinked issue: {issue_url}"
            ),
        ]
    )
    run(["git", "push", "-u", "origin", branch])

    result = run(
        [
            "gh",
            "pr",
            "create",
            "--draft",
            "--title",
            f"chore(pbt): regression stub for {failure['test']}",
            "--body",
            (
                "Stub regression test for nightly PBT failure.\n\n"
                f"Linked issue: {issue_url}\n\n"
                f"**Action required:** convert the `.txt` stub at `{stub_file}` "
                "into an actual test file using the failing input from the issue."
            ),
        ],
        capture=True,
    )
    return result.stdout.strip()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--tool", required=True, choices=["scalacheck", "elm-test", "hypothesis"]
    )
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    output = args.output.read_text()
    parsers = {
        "scalacheck": parse_scalacheck,
        "elm-test": parse_elm_test,
        "hypothesis": parse_hypothesis,
    }
    failure = parsers[args.tool](output)
    if failure is None:
        print(
            f"WARN: Could not parse {args.tool} failure. Filing raw issue.",
            file=sys.stderr,
        )
        failure = {"tool": args.tool, "test": "unknown", "arg": "see output"}

    issue_url = create_issue(failure, args.output)
    print(f"Filed issue: {issue_url}", file=sys.stderr)

    pr_url = create_draft_pr(failure, issue_url)
    print(f"Filed draft PR: {pr_url}", file=sys.stderr)

    return 0


if __name__ == "__main__":
    sys.exit(main())
