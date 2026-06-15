# sangeet-debug-console MCP server

An [MCP](https://modelcontextprotocol.io) server that exposes the Sangeet debug bridge as a set of tools an AI agent can call. With this wired up, an agent can drive the **running** app — type swaras, inspect editor state, dump compositions, switch tabs, trigger crashes — without going through the keyboard or asking a human to verify a feature manually.

Two back ends are bundled, selected at startup via `--transport`:

- `--transport tcp` (default) — talks to the desktop app's `DebugConsole` on `127.0.0.1:28081`.
- `--transport ws` — hosts a WebSocket server the Elm web app connects to (`?debug=ws://localhost:<port>`).

The same ~31 tools are exposed in both modes. See [`docs/developer/architecture/debug-bridge.md`](../../docs/developer/architecture/debug-bridge.md) for the wire format and architecture.

## When to use this

- Building or modifying any UI feature on the desktop app, where the agent wants to write code → run the app → exercise the feature → check state → iterate.
- Reproducing a UI bug deterministically by scripting the exact keystroke sequence that triggers it.
- Smoke-testing crash-recovery, focus issues, or any edge case that's awkward to do by hand.

The corresponding desktop infrastructure lives in [`sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/DebugConsole.scala`](../../sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/DebugConsole.scala). See [`docs/developer/architecture/debug-console.md`](../../docs/developer/architecture/debug-console.md) for the protocol details and the full command catalog.

## Setup

### 1. Make sure `uv` is installed

[`uv`](https://docs.astral.sh/uv/) (and its `uvx` runner) is the easiest way — no need to manage a venv yourself.

```bash
brew install uv          # macOS
# or
curl -LsSf https://astral.sh/uv/install.sh | sh
```

### 2. Wire the server into Claude Code

Add to `~/.claude.json` (global, available across all projects) **or** to `.claude/mcp_servers.json` (project-local — won't pick up unless this project is the cwd):

```json
{
  "mcpServers": {
    "sangeet-debug-console": {
      "command": "uvx",
      "args": [
        "--from",
        "/absolute/path/to/sangeet_notes_editor/mcp-servers/sangeet-debug-console",
        "sangeet-debug-console",
        "--transport", "tcp"
      ]
    }
  }
}
```

Replace `/absolute/path/to/sangeet_notes_editor` with the real path on your machine. The first invocation downloads the `mcp` + `websockets` Python packages into a transient venv; subsequent runs reuse the cache.

For the WebSocket back end (web app), see the [WebSocket transport](#websocket-transport-web-app) section below — register a second MCP server entry with `--transport ws --port 9999`.

### 3. Restart Claude Code

The agent picks up MCP servers at session start. Open a new session; the `mcp__sangeet-debug-console__*` tools should appear in the tool list.

### 4. Start the desktop app

The MCP server is a thin pass-through to TCP `127.0.0.1:28081`. If the desktop app isn't running, every tool call returns `ERROR: cannot reach desktop debug console at 127.0.0.1:28081 — is the app running?`.

```bash
sbt sangeetDesktop/run
```

## Tools exposed

Grouped by category. The agent sees a tool name like `mcp__sangeet-debug-console__get_state` for each. Names match the underlying `DebugConsole.scala` command where reasonable.

### Diagnostics
- `ping` — health check
- `help_text` — list every command (returns the desktop console's `help` output)
- `thread_dump` — JVM thread dump; works during freezes
- `set_debug(state)` — toggle debug logging
- `throw_crash(message?)` — synthetic crash for recovery-pipeline testing

### Tabs
- `list_tabs`, `select_tab(index)`, `new_tab`, `close_tab(index?)`, `tab_info`

### Editor input
- `type_char(char)` — swar key (s/r/g/m/p/d/n + uppercase variants)
- `press(key)` — special keys (space, backspace, delete, minus, left, right)
- `set_octave(key)` — period/quote/backtick for mandra/taar/madhya
- `set_subdivision(n)` — 2–8 notes per beat
- `dual_swar(char)` — dual swar (ss, rr, gg, …)
- `swar_group(chars)` — 2–4 swaras on a single beat
- `type_timed(entries)` — explicit timing offsets, e.g. `s:0,r:100,g:200`

### Ornaments & strokes
- `stroke(name)` — da / ra / jod
- `simple_ornament(name)` — gamak / andolan / gitkari
- `ornament_start(mode)`, `ornament_note(char)`, `finish_ornament` — multi-step ornament flow

### Structure
- `switch_section(index)`, `set_taal(name)`, `reset(type?, taal?, taan_count?)`

### State inspection
- `get_state`, `get_events`, `dump_composition` (full JSON), `dump_history`
- `check_focus`, `focus_editor`

## Common recipes

### Enter a Sa-Re-Ga-Ma phrase and dump it
```
new_tab
type_char("s")
type_char("r")
type_char("g")
type_char("m")
dump_composition
```

### Reproduce a bug from a clean slate
```
reset("gat", "teentaal", 0)
type_timed("s:0,r:100,g:200,m:300")
get_events
```

### Test crash recovery
```
throw_crash("MCP regression test")
# Then quit and relaunch the app manually — the CrashRecoveryDialog
# should surface the sentinel and offer to send a bug report.
```

## Limitations

- **Single app instance.** The `MainApp` single-instance lock (port 47633) means a second `sbt sangeetDesktop/run` exits — there's only ever one TCP console to drive.
- **Stateless across calls (TCP).** Each MCP tool call opens a fresh TCP connection. The console itself doesn't multiplex; state lives in the desktop app.
- **5-second FX-thread timeout per call (TCP).** If a UI mutation takes longer than 5 s, the tool reports a timeout error. Bare diagnostics (ping, thread_dump, throw_crash) bypass the FX thread and always return.
- **WS: single client at a time.** The WS server multiplexes by `req-N` id, but only one Elm tab should connect at once — the bridge sends every reply to the most recent connection.

## WebSocket transport (web app)

The same MCP server can drive the running web app via the WebSocket debug bridge. This lets agents iterate on web features the same way they iterate on desktop features.

### Setup

1. Run the MCP server in WS mode:

   ```bash
   uvx --from . sangeet-debug-console --transport ws --port 9999
   ```

2. Open the web app with the matching `?debug=` URL param:

   ```
   http://localhost:3000/?debug=ws://localhost:9999
   ```

3. The MCP server's first command will block until the Elm app connects (max 30 s). After that, the same ~31 tools work identically to TCP mode — `ping`, `get_state`, `type_char`, `dump_composition`, etc.

### Security

The web app's `ports.js` enforces a loopback-only URL allowlist (`ws://localhost:` or `ws://127.0.0.1:` only). Any other URL is silently ignored — a hostile page can't talk the running Elm app into exfiltrating state to a remote endpoint via this bridge. For belt-and-suspenders, the WS server itself binds to `127.0.0.1` only.

### Caveats

- **No `throw_crash`.** Browsers can't simulate a JVM crash. The tool returns `crash injection not supported on web` if invoked.
- **No `thread_dump`.** Returns a placeholder string — browsers don't expose threads.
- **No file system.** Tools that touch disk on desktop (open recent file, etc.) are not available on web; the corresponding `Reset` builds a fresh composition via the `/compositions` HTTP endpoint instead.

## Adding a new tool

1. Add the corresponding command to [`DebugCommand`](../../sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/DebugCommand.scala) in sangeet-core.
2. Add a dispatch arm in `DebugCommandHandler.applyDebugCommand` (desktop) and `Debug.Interpreter.applyCmd` (web). Both compile-fail until present.
3. Add a `text_to_json_cmd` arm in `transport_ws.py` mirroring the new variant's shape.
4. Add an `@mcp.tool()` function in `server.py` that calls `send("your-command args")`.
5. Bump the version in `pyproject.toml` if you want users to pick up the new tool on next `uvx` invocation (caching).

## Tests

```bash
uv run --extra test python -m pytest tests/
```

The smoke tests cover the text→JSON mapping plus a round-trip through `WsTransport` with a fake Elm client. The full surface is exercised by the cross-platform parity harness (`tests/integration/*.json`, run by `SharedIntegrationSpec` and `e2e/integration/parity.spec.ts`).
