#!/usr/bin/env python3
"""MCP server that wraps the Sangeet debug bridge (desktop TCP or web WS).

Two back ends:

* ``--transport tcp`` (default) — talks to the desktop app's DebugConsole
  on 127.0.0.1:28081. See ``transport_tcp.py``.
* ``--transport ws`` — hosts a WebSocket server the Elm web app connects to
  (load with ``?debug=ws://localhost:<port>``). See ``transport_ws.py``.

The same 31 tools are exposed in both modes. Selection happens once at
startup; switching modes means restarting the server.

Run via uvx (recommended; no install needed)::

    uvx --from . sangeet-debug-console --transport tcp
    uvx --from . sangeet-debug-console --transport ws --port 9999

Or directly with the deps installed::

    pip install "mcp[cli]>=1.2.0" "websockets>=12.0"
    python server.py --transport ws --port 9999

Wire into Claude Code by adding to ``~/.claude.json`` (global) or
``.claude/mcp_servers.json`` (project)::

    {
      "mcpServers": {
        "sangeet-debug-console": {
          "command": "uvx",
          "args": [
            "--from",
            "/abs/path/to/mcp-servers/sangeet-debug-console",
            "sangeet-debug-console",
            "--transport", "tcp"
          ]
        }
      }
    }

See README.md for the full setup walkthrough.
"""
from __future__ import annotations

import argparse
import sys
from typing import Optional

from mcp.server.fastmcp import FastMCP

from transport import Transport

mcp = FastMCP("sangeet-debug-console")

# Initialised in main(); module-level so the tool functions below can call
# transport.send(...) without threading the instance through every closure.
_transport: Transport | None = None


def send(line: str) -> str:
    """Delegate to the configured transport. Tools should call this rather
    than reaching into TCP or WS-specific code directly."""
    if _transport is None:
        return "ERROR: transport not initialised — call main() first"
    return _transport.send(line)


# ─── Diagnostics ────────────────────────────────────────────────────────────


@mcp.tool()
def ping() -> str:
    """Health check the debug bridge. Returns 'pong' if the app is up."""
    return send("ping")


@mcp.tool()
def help_text() -> str:
    """List every console command with a one-line description. Useful when an
    agent needs to discover what's available without re-reading server.py."""
    return send("help")


@mcp.tool()
def thread_dump() -> str:
    """JVM thread dump (desktop). Works even when the FX thread is frozen.
    On web the bridge returns a placeholder — browsers don't expose threads."""
    return send("thread-dump")


@mcp.tool()
def set_debug(state: str) -> str:
    """Toggle debug logging. state must be 'on' or 'off'."""
    return send(f"set-debug {state}".strip())


@mcp.tool()
def throw_crash(message: str = "MCP-triggered synthetic crash") -> str:
    """Spawn a daemon thread that throws an unchecked exception (desktop).
    CrashCapture writes a sentinel under ~/.sangeet/crash-pending/; the next
    launch surfaces the recovery dialog. On web this is not supported."""
    return send(f"throw {message}")


# ─── Tabs ───────────────────────────────────────────────────────────────────


@mcp.tool()
def list_tabs() -> str:
    """List all open editor tabs with their index, title, and file path."""
    return send("list-tabs")


@mcp.tool()
def select_tab(index: int) -> str:
    """Switch to the tab at the given 0-based index."""
    return send(f"select-tab {index}")


@mcp.tool()
def new_tab() -> str:
    """Open a fresh untitled tab with a default Yaman Gat in teentaal."""
    return send("new-tab")


@mcp.tool()
def close_tab(index: Optional[int] = None) -> str:
    """Close the tab at the given index. Omitting closes the active tab."""
    if index is None:
        return send("close-tab")
    return send(f"close-tab {index}")


@mcp.tool()
def tab_info() -> str:
    """Info about the active tab: index, title, file path, read-only flag."""
    return send("tab-info")


# ─── Editor input ───────────────────────────────────────────────────────────


@mcp.tool()
def type_char(char: str) -> str:
    """Simulate typing a swar key. Lowercase = shuddha (s r g m p d n);
    uppercase = komal (R G D N) or tivra (M); fixed: S, P."""
    return send(f"type {char}")


@mcp.tool()
def press(key: str) -> str:
    """Simulate a special key press. key ∈ {space, backspace, delete, minus, left, right}."""
    return send(f"press {key}")


@mcp.tool()
def set_octave(key: str) -> str:
    """Set octave for the next note. key ∈ {period (mandra), quote (taar), backtick (madhya)}."""
    return send(f"octave {key}")


@mcp.tool()
def set_subdivision(n: int) -> str:
    """Set beat subdivision (notes per beat). n ∈ [2, 8]."""
    return send(f"subdivision {n}")


@mcp.tool()
def dual_swar(char: str) -> str:
    """Enter a dual swar like ss=SaSa, rr=ReRe, gg=GaGa."""
    return send(f"dual {char}")


@mcp.tool()
def swar_group(chars: str) -> str:
    """Enter a swar group on a single beat. e.g. 'sr' → SaRe, 'srgm' → 4-note subdivision."""
    return send(f"group {chars}")


@mcp.tool()
def type_timed(entries: str) -> str:
    """Type with explicit timing offsets. Format: 'c:ms,c:ms,...'."""
    return send(f"type-timed {entries}")


# ─── Ornaments & strokes ────────────────────────────────────────────────────


@mcp.tool()
def stroke(name: str) -> str:
    """Set the mizrab stroke on the last note. name ∈ {da, ra, jod}."""
    return send(f"stroke {name}")


@mcp.tool()
def simple_ornament(name: str) -> str:
    """Apply a single-note ornament. name ∈ {gamak, andolan, gitkari}."""
    return send(f"ornament {name}")


@mcp.tool()
def ornament_start(mode: str) -> str:
    """Begin a multi-step ornament. mode ∈ {kanswar, sparsh, ghaseet, meend-asc,
    meend-desc, krintan, murki, zamzama}."""
    return send(f"ornament-start {mode}")


@mcp.tool()
def ornament_note(char: str) -> str:
    """Add a swar to the in-progress ornament. Single char."""
    return send(f"ornament-note {char}")


@mcp.tool()
def finish_ornament() -> str:
    """Finalize a multi-note ornament (murki, zamzama)."""
    return send("finish-ornament")


# ─── Structure ──────────────────────────────────────────────────────────────


@mcp.tool()
def switch_section(index: int) -> str:
    """Switch to the section at the given 0-based index in the active tab."""
    return send(f"section {index}")


@mcp.tool()
def set_taal(name: str) -> str:
    """Change the active composition's taal. name ∈ {teentaal, jhaptaal, rupak, ektaal, dadra, keherwa, ...}."""
    return send(f"set-taal {name}")


@mcp.tool()
def reset(
    composition_type: str = "gat",
    taal: str = "teentaal",
    taan_count: int = 0,
) -> str:
    """Destructively replace the active tab's composition with a fresh empty one."""
    return send(f"reset {composition_type} {taal} {taan_count}")


# ─── State inspection ──────────────────────────────────────────────────────


@mcp.tool()
def get_state() -> str:
    """Snapshot of the active editor: section, cursor, event count, flags."""
    return send("get-state")


@mcp.tool()
def get_events() -> str:
    """All events in the current section, in document order."""
    return send("get-events")


@mcp.tool()
def dump_composition() -> str:
    """Full composition serialized as JSON (.swar format)."""
    return send("dump-composition")


@mcp.tool()
def dump_history() -> str:
    """Undo/redo stack sizes for the active tab."""
    return send("dump-history")


@mcp.tool()
def check_focus() -> str:
    """Which UI node has focus."""
    return send("check-focus")


@mcp.tool()
def focus_editor() -> str:
    """Force focus to the editor pane in the active tab."""
    return send("focus")


# ─── Entry point ────────────────────────────────────────────────────────────


def _build_transport(args: argparse.Namespace) -> Transport:
    """Instantiate the chosen transport. Imported lazily so users of TCP
    don't need the optional `websockets` dependency installed."""
    if args.transport == "tcp":
        from transport_tcp import TcpTransport

        port = args.port if args.port is not None else 28081
        return TcpTransport(port=port)
    if args.transport == "ws":
        from transport_ws import WsTransport

        port = args.port if args.port is not None else 9999
        return WsTransport(port=port)
    raise ValueError(f"unknown transport: {args.transport}")


def _parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="sangeet-debug-console",
        description="MCP server wrapping the Sangeet debug bridge (TCP or WS).",
    )
    parser.add_argument(
        "--transport",
        choices=["tcp", "ws"],
        default="tcp",
        help="Which back end to use. tcp = desktop app, ws = web app (default: tcp).",
    )
    parser.add_argument(
        "--port",
        type=int,
        default=None,
        help="Override transport port (TCP default: 28081, WS default: 9999).",
    )
    # parse_known_args so the MCP CLI's own flags (if any) pass through unchanged.
    args, _ = parser.parse_known_args(argv)
    return args


def main() -> None:
    """Entry point used by both ``python server.py`` and the
    ``sangeet-debug-console`` CLI script."""
    global _transport
    args = _parse_args(sys.argv[1:])
    _transport = _build_transport(args)
    try:
        mcp.run()
    finally:
        _transport.close()


if __name__ == "__main__":
    main()
