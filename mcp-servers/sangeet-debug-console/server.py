#!/usr/bin/env python3
"""MCP server that wraps the Sangeet desktop app's TCP debug console.

The desktop app listens on 127.0.0.1:28081 (see DebugConsole.scala). This
server exposes each console command as an MCP tool so an AI agent can drive
the running desktop app — type swaras, inspect editor state, dump the
composition as JSON, switch tabs, trigger crashes for recovery testing,
etc. — without going through the keyboard.

The intended workflow: agent edits feature code → starts the desktop app
→ uses these MCP tools to exercise the new feature → reads `get_state` /
`get_events` to verify behaviour → iterates. Closes the "agent must ask
human to test" loop for UI work.

Run via uvx (recommended; no install needed):

    uvx --from . sangeet-debug-console

Or directly with the deps installed:

    pip install "mcp[cli]>=1.2.0"
    python server.py

Wire into Claude Code by adding to `~/.claude.json` (global) or
`.claude/mcp_servers.json` (project):

    {
      "mcpServers": {
        "sangeet-debug-console": {
          "command": "uvx",
          "args": ["--from", "/abs/path/to/mcp-servers/sangeet-debug-console", "sangeet-debug-console"]
        }
      }
    }

See README.md for the full setup walkthrough.
"""
from __future__ import annotations

import socket
from typing import Optional

from mcp.server.fastmcp import FastMCP

# DebugConsole.scala binds 127.0.0.1:28081 by default. The port can't be
# overridden today (would need a CLI flag added to MainApp), so we just hardcode.
HOST = "127.0.0.1"
PORT = 28081
END_MARKER = "---END---"

mcp = FastMCP("sangeet-debug-console")


def send(command: str, timeout: float = 6.0) -> str:
    """Open a fresh TCP connection, send a single command, read up to the END
    marker. Each call is independent — the console is stateless across
    connections; state lives in the desktop app itself.

    The 5-second FX-thread timeout inside DebugConsole.scala dominates, so a
    6s socket timeout here gives the handler a chance to reply even when the
    UI thread is mid-action. If the app isn't running, the connect fails fast.
    """
    try:
        with socket.create_connection((HOST, PORT), timeout=timeout) as sock:
            sock.settimeout(timeout)
            # Drain the banner ("Sangeet Debug Console..." + END_MARKER).
            read_until(sock, END_MARKER)
            sock.sendall((command + "\n").encode("utf-8"))
            reply = read_until(sock, END_MARKER)
            return reply.strip()
    except (ConnectionRefusedError, socket.timeout, OSError) as e:
        return f"ERROR: cannot reach desktop debug console at {HOST}:{PORT} — is the app running? ({e})"


def read_until(sock: socket.socket, marker: str) -> str:
    """Read from sock until a line equal to `marker` is seen. Returns the
    accumulated text excluding the marker line itself.
    """
    buf = bytearray()
    marker_bytes = (marker + "\n").encode("utf-8")
    while True:
        chunk = sock.recv(4096)
        if not chunk:
            break
        buf.extend(chunk)
        idx = buf.find(marker_bytes)
        if idx != -1:
            return buf[:idx].decode("utf-8", errors="replace")
    return buf.decode("utf-8", errors="replace")


# ─── Diagnostics ────────────────────────────────────────────────────────────


@mcp.tool()
def ping() -> str:
    """Health check the desktop debug console. Returns 'pong' if the app is up."""
    return send("ping")


@mcp.tool()
def help_text() -> str:
    """List every console command with a one-line description. Useful when an
    agent needs to discover what's available without re-reading server.py."""
    return send("help")


@mcp.tool()
def thread_dump() -> str:
    """JVM thread dump. Works even when the FX thread is frozen — runs on a
    non-FX worker. Use to diagnose UI hangs."""
    return send("thread-dump")


@mcp.tool()
def set_debug(state: str) -> str:
    """Toggle debug logging. state must be 'on' or 'off' (empty returns current state)."""
    return send(f"set-debug {state}".strip())


@mcp.tool()
def throw_crash(message: str = "MCP-triggered synthetic crash") -> str:
    """Spawn a daemon thread that throws an unchecked exception. CrashCapture
    writes a sentinel under ~/.sangeet/crash-pending/; the next launch surfaces
    the recovery dialog. Use for verifying the crash-reporting pipeline."""
    return send(f"throw {message}")


# ─── Tabs ───────────────────────────────────────────────────────────────────


@mcp.tool()
def list_tabs() -> str:
    """List all open editor tabs with their index, title, and file path. The
    active tab is marked with *."""
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
    """Close the tab at the given index. Omitting the index closes the active tab."""
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
    uppercase = komal (R G D N) or tivra (M); fixed: S, P. Operates on the
    active tab's editor."""
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
    """Enter a dual swar like ss=SaSa, rr=ReRe, gg=GaGa. Pass a single char; the
    desktop expands it to the dual."""
    return send(f"dual {char}")


@mcp.tool()
def swar_group(chars: str) -> str:
    """Enter a swar group on a single beat. e.g. 'sr' → SaRe on beat as 2-note
    subdivision, 'srgm' → 4-note subdivision. 2-4 chars."""
    return send(f"group {chars}")


@mcp.tool()
def type_timed(entries: str) -> str:
    """Type with explicit timing offsets. Format: 'c:ms,c:ms,...'. e.g.
    's:0,r:100,g:200' types Sa-Re-Ga within 500ms (auto-grouped on one beat);
    's:0,r:600' types Sa then Re on separate beats."""
    return send(f"type-timed {entries}")


# ─── Ornaments & strokes ────────────────────────────────────────────────────


@mcp.tool()
def stroke(name: str) -> str:
    """Set the mizrab stroke on the last note. name ∈ {da, ra, jod}."""
    return send(f"stroke {name}")


@mcp.tool()
def simple_ornament(name: str) -> str:
    """Apply a single-note ornament to the last note. name ∈ {gamak, andolan, gitkari}."""
    return send(f"ornament {name}")


@mcp.tool()
def ornament_start(mode: str) -> str:
    """Begin a multi-step ornament. mode ∈ {kanswar, sparsh, ghaseet, meend-asc,
    meend-desc, krintan, murki, zamzama}. Follow with ornament_note calls and
    finish_ornament for multi-note types (murki, zamzama)."""
    return send(f"ornament-start {mode}")


@mcp.tool()
def ornament_note(char: str) -> str:
    """Add a swar to the in-progress ornament. Single char."""
    return send(f"ornament-note {char}")


@mcp.tool()
def finish_ornament() -> str:
    """Finalize a multi-note ornament (murki, zamzama). No-op for single/two-note
    ornaments — they finalize on the second note."""
    return send("finish-ornament")


# ─── Structure ──────────────────────────────────────────────────────────────


@mcp.tool()
def switch_section(index: int) -> str:
    """Switch to the section at the given 0-based index in the active tab."""
    return send(f"section {index}")


@mcp.tool()
def set_taal(name: str) -> str:
    """Change the active composition's taal. name ∈ {teentaal, jhaptaal, rupak,
    ektaal, dadra, keherwa, ...}. Reflows existing events onto the new cycle."""
    return send(f"set-taal {name}")


@mcp.tool()
def reset(
    composition_type: str = "gat",
    taal: str = "teentaal",
    taan_count: int = 0,
) -> str:
    """Destructively replace the active tab's composition with a fresh empty one.
    composition_type ∈ {gat, bandish, palta}. Resets the undo history."""
    return send(f"reset {composition_type} {taal} {taan_count}")


# ─── State inspection ──────────────────────────────────────────────────────


@mcp.tool()
def get_state() -> str:
    """Snapshot of the active editor: section index/name, cursor (cycle, beat,
    subIndex, totalSubdivisions, octave), event count, read-only flag, edit
    mode, focus state."""
    return send("get-state")


@mcp.tool()
def get_events() -> str:
    """All events in the current section, in document order. Includes type
    (Swar/Rest/Sustain/Chikari/LockedBeat), note + variant + octave for swaras,
    stroke + ornament count if present."""
    return send("get-events")


@mcp.tool()
def dump_composition() -> str:
    """Full composition serialized as JSON (.swar format). Useful for diffing
    against a fixture or saving a known-good state for regression tests."""
    return send("dump-composition")


@mcp.tool()
def dump_history() -> str:
    """Undo/redo stack sizes for the active tab. Format: 'past: N\\nfuture: M'."""
    return send("dump-history")


@mcp.tool()
def check_focus() -> str:
    """Which UI node has focus. Use to diagnose 'why isn't my key press working?' issues."""
    return send("check-focus")


@mcp.tool()
def focus_editor() -> str:
    """Force focus to the editor pane in the active tab. Useful as a setup step
    before sending type/press commands."""
    return send("focus")


def main() -> None:
    """Entry point used by both `python server.py` and the `sangeet-debug-console` CLI."""
    mcp.run()


if __name__ == "__main__":
    main()
