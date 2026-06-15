# TCP Debug Console

The desktop app exposes a line-oriented TCP server on **`127.0.0.1:28081`** that mirrors most of what the keyboard does, plus diagnostics that work even when the UI is frozen. It's our primary tool for:

- Reproducing bugs deterministically (paste a script, watch the state)
- Inspecting state during a freeze (`thread-dump` runs from a non-FX thread, so it returns even if the FX thread is stuck)
- Driving the 129 desktop integration tests in [`DebugConsoleTcpSpec.scala`](../../sangeet-desktop/src/test/scala/com/varpas/sangeet/desktop/editor/DebugConsoleTcpSpec.scala)
- Crash-recovery verification (the `throw` command, see [Recipe: trigger the crash recovery dialog](#recipe-trigger-the-crash-recovery-dialog))

> Source: [`DebugConsole.scala`](../../sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/DebugConsole.scala). User-guide summary lives at [user-guide/07-file-operations.md#debug-console](../user-guide/07-file-operations.md#debug-console).

---

## Connecting

```bash
nc 127.0.0.1 28081           # basic
rlwrap nc 127.0.0.1 28081    # with line-editing + history (recommended)
echo "get-state" | nc 127.0.0.1 28081   # one-shot, scriptable
```

On launch the app prints the exact `nc` command to stderr, so you can copy it from the terminal that started the JVM.

The console binds to the **loopback interface only** — it is not reachable from other machines. Bind failure (port already taken) is logged but does **not** prevent the app from starting; the rest of the editor works normally without the console.

## Protocol

Line-based, one command per line, replies framed by `---END---`:

```
$ nc 127.0.0.1 28081
Sangeet Debug Console. Type 'help' for commands.
---END---
ping
pong
---END---
get-state
section: 0 (Sthayi)
cursor.cycle: 0
cursor.beat: 0
cursor.subIndex: 0
cursor.totalSubdivisions: 1
cursor.octave: madhya
events: 0
readOnly: false
editMode: Normal
scrollPaneFocused: true
---END---
```

- Empty lines are ignored.
- Replies prefixed `ERROR:` are failures (unknown command, bad arg, FX thread timeout, etc.).
- Commands that mutate UI state are dispatched onto the FX thread via `Platform.runLater` with a **5-second timeout** — if the FX thread is frozen for longer, the console replies `ERROR: FX thread did not respond in 5 seconds (possible freeze)` instead of hanging.

## Command catalog

Get the full list with `help`. Categories:

| Category | Commands |
|----------|----------|
| **Health/diagnostics** | `ping`, `help`, `thread-dump`, `set-debug on\|off`, `throw [msg]` |
| **Tabs** | `list-tabs`, `select-tab <i>`, `new-tab`, `close-tab [i]`, `tab-info` |
| **Editor (input)** | `type <char>`, `press <key>`, `octave <key>`, `subdivision <n>`, `dual <c>`, `group <chars>`, `type-timed <c:ms,...>`, `stroke <da\|ra\|jod>` |
| **Editor (ornaments)** | `ornament <name>`, `ornament-start <mode>`, `ornament-note <c>`, `finish-ornament` |
| **Editor (structure)** | `section <i>`, `set-taal <name>`, `reset [type] [taal] [taanCount]` |
| **State inspection** | `get-state`, `get-events`, `dump-composition`, `dump-history`, `check-focus`, `focus` |

`type-timed` is the only timing-aware command — entries are `char:ms` pairs giving an offset from t=0, used to exercise the 500ms fast-typing grouping window without sleeping the test thread.

## Common recipes

### Recipe: capture state when the UI is frozen

```bash
echo thread-dump | nc 127.0.0.1 28081 > /tmp/dump.txt
echo get-state   | nc 127.0.0.1 28081 >> /tmp/dump.txt
echo dump-history | nc 127.0.0.1 28081 >> /tmp/dump.txt
```

`thread-dump` is the only command that runs **off** the FX thread, so it always returns. If it shows the FX thread stuck on a particular lock or stack frame, that's the freeze. Attach the dump to the bug report.

### Recipe: reproduce a bug from scratch

```bash
rlwrap nc 127.0.0.1 28081
# session:
reset gat teentaal 0     # clean Yaman Gat composition, teentaal
type s
type r
type g
type m
get-events               # confirm 4 events landed where expected
dump-composition         # full JSON for diffing or saving to a fixture
```

`reset` accepts `gat`, `bandish`, `palta` as the first arg; defaults are `gat teentaal 0` (no taans).

### Recipe: exercise fast-typing grouping

```bash
type-timed s:0,r:100,g:200,m:300
get-events
# All 4 swaras land on beat 0 with subdivisions 0-3 (within the 500ms window).

type-timed s:0,r:600
# Sa on beat 0, Re on beat 1 (gap > 500ms threshold).
```

### Recipe: trigger the crash recovery dialog

```bash
echo throw | nc 127.0.0.1 28081
# Spawns a daemon thread that immediately throws. CrashCapture writes
# ~/.sangeet/crash-pending/crash-<timestamp>.json — see
# docs/developer/operations/observability-and-bug-reporting.md for the schema.
# The JVM stays alive (only the daemon thread dies), so close the app
# normally, then relaunch — the CrashRecoveryDialog should surface the
# sentinel and offer to send a bug report.
```

This is exactly how Phase 9 verification works.

### Recipe: switch composition type / taal mid-session

```bash
set-taal jhaptaal        # current section gets reflowed onto the new cycle
section 1                # switch to section index 1
reset palta keherwa 0    # blow away the active composition with a fresh Palta in Keherwa
```

`reset` is destructive — it replaces the active tab's composition with an empty one. The undo history for that tab is reset too.

## Using the console from tests

[`DebugConsoleTcpSpec.scala`](../../sangeet-desktop/src/test/scala/com/varpas/sangeet/desktop/editor/DebugConsoleTcpSpec.scala) is the integration-test layer. The pattern:

1. Boot the FX toolkit + `MainApp` in a test fixture once per suite
2. Open a socket to `127.0.0.1:28081`
3. Send commands, read responses up to `---END---`
4. Assert against the response text

129 tests live there today, covering tab management, swar input, ornament workflows, stroke editing, section switching, and undo/redo. They are the highest-fidelity tests for desktop behavior — they exercise the real ScalaFX toolkit, not mocks.

If you're adding a new editor feature, **add a debug-console command first**, then write the integration test against it, then build the UI. That ordering keeps the feature scriptable from day one and gives you a regression test for free.

## Limitations and safety

- **Single app instance.** The `MainApp` single-instance lock binds port 47633 before the console binds 28081. A second launch exits early and never starts a second console.
- **No authentication.** Loopback-only mitigates this; do not expose to non-trusted local users.
- **`throw` does not kill the JVM.** It only kills the daemon thread it spawns. To test the second half of crash recovery, you still have to quit and relaunch the app.
- **Web app has no equivalent.** This is desktop-only. The Tapir REST API on `:28080` is the corresponding interface for the web app (see [Swagger UI](http://localhost:28080/docs)).
- **Frozen FX thread → 5s timeout per mutating command.** Diagnostics (`thread-dump`, `ping`, `set-debug`, `throw`) bypass the FX thread and always reply.

## Adding a new command

1. Add a case in `dispatch()` in [`DebugConsole.scala`](../../sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/DebugConsole.scala).
2. Wrap any UI-mutating call in `runOnFx { ... }` — this enforces the FX-thread + 5s timeout contract.
3. Extend `cmdHelp()` so `help` lists it.
4. Add a `DebugConsoleTcpSpec` test sending the command and asserting the reply.
5. If the command is non-obvious (e.g. multi-step workflows), add a recipe to this doc.

The console is intentionally additive: never remove commands without checking [`DebugConsoleTcpSpec`](../../sangeet-desktop/src/test/scala/com/varpas/sangeet/desktop/editor/DebugConsoleTcpSpec.scala) and any scripts that may depend on them.
