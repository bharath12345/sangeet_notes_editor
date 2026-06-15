# Debug Bridge — Architecture & Usage

Both the desktop and web apps expose a back door for programmatic control:

- **Desktop:** TCP debug console on `127.0.0.1:28081` (since plan-tcp-debug-console).
- **Web:** WebSocket bridge on a loopback port, activated by the `?debug=ws://localhost:PORT` URL param (plan-14).

Both speak the same vocabulary — the `DebugCommand` ADT defined in
[`sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/DebugCommand.scala`](../../sangeet-core/src/main/scala/com/varpas/sangeet/core/debug/DebugCommand.scala).

## What it's for

- **Agent loops.** The [`mcp-servers/sangeet-debug-console`](../../mcp-servers/sangeet-debug-console) MCP server wraps either transport. An agent writes a feature → connects via MCP → drives the running app to exercise the change → reads state → iterates. See the MCP server's README for setup.
- **Integration tests.** The desktop's `SharedIntegrationSpec` (via TCP) and the web's `parity.spec.ts` (via WS) load shared JSON test definitions from `tests/integration/` and assert against identical state checkpoints + golden `.swar`/`.html` fixtures. If a test passes on one stack but fails on the other, that's exactly the parity bug the harness is built to catch.
- **Manual debugging.** `nc 127.0.0.1 28081` lets you drive the desktop app from the command line; useful for reproducing UI freezes or inspecting state mid-flow.

## Adding a new command

1. Add a case to `enum DebugCommand` in sangeet-core.
2. Add a `fromText` arm in `DebugCommand.scala` if the legacy text protocol should accept it.
3. Add a dispatch arm in `DebugCommandHandler.applyDebugCommand` (desktop).
4. Add a dispatch arm in `Debug.Interpreter.applyCmd` (web).
5. Add a `text_to_json_cmd` arm in `mcp-servers/sangeet-debug-console/transport_ws.py` if the MCP server should support it over WS.
6. Optional: add an `@mcp.tool()` wrapper in `server.py` for ergonomic access.

Steps 3 and 4 won't compile until both dispatch arms exist — drift risk is bounded by the type system.

## Wire formats

### TCP (desktop)

Newline-delimited text. `ping`, `reset gat yaman teentaal`, `type srgmp`. Each command produces a reply terminated by a `---END---` marker line.

The parser is `DebugCommand.fromText` in sangeet-core; the desktop's `DebugConsole.scala` delegates there so the TCP vocabulary and the JSON vocabulary stay in lock-step.

### WebSocket (web)

JSON envelopes:

```json
{ "id": "req-1", "cmd": { "Ping": {} } }
```

The variant name is the top-level key inside `cmd`; its payload (if any) is the nested object. Parameterless variants encode as `{"VariantName": {}}`. This is exactly what circe produces for `DebugCommand`.

Response shape:

```json
{ "id": "req-1", "result": <value>, "error": null }
```

`result` is the variant's natural response (a string for `Ping`, an object for `GetState`, etc.). `error` is set to a string only when the bridge couldn't honour the request.

`DebugCommandSpec` in sangeet-core verifies the encoded shape, so any drift between the Scala enum and the Elm decoder is caught by Scala unit tests.

## Security

- **TCP:** bound to `127.0.0.1` (loopback only). No external network exposure.
- **WS:** the bridge in `sangeet-web/public/ports.js` rejects any URL that doesn't start with `ws://localhost:` or `ws://127.0.0.1:`. Production bundles still contain the bridge code but never connect — there's no `?debug=` query param in production URLs. For belt-and-suspenders, the WS server in `transport_ws.py` also binds to `127.0.0.1` only.

The MCP server has no separate authentication; anything that can reach `127.0.0.1:28081` (desktop) or the WS port (web) can drive the app. This is appropriate for a dev-time tool and unsafe for production deployment — neither transport is intended to ship to end users.

## Tests

- **Shared definitions:** `tests/integration/*.json` — canonical scenarios both stacks run.
- **Golden fixtures:** `tests/integration/golden/*.swar` and `*.html`. Regenerate via `./scripts/regenerate_golden_fixtures.py` when an intentional format change lands.
- **Desktop runner:** `sangeet-desktop/src/test/scala/com/varpas/sangeet/desktop/integration/SharedIntegrationSpec.scala` — auto-discovers the JSON files and runs each one through the live desktop app over TCP.
- **Web runner:** `e2e/integration/parity.spec.ts` — iterates the same JSON files, drives the Elm app via WebSocket, and compares against the same golden fixtures.

A test that passes on one stack but fails on the other is the parity bug class this harness exists to catch.

- **WS transport unit tests:** `mcp-servers/sangeet-debug-console/tests/test_transport_ws.py` — pytest suite covering the text→JSON mapping plus a smoke round-trip via a fake Elm client. Run with `uv run --extra test python -m pytest tests/` from the package directory.
