# TCP Debug Console for Sangeet Desktop App

## Context

The desktop editor sometimes freezes — keyboard input stops working, logging stops, and there's no way to inspect what went wrong. We need a debug console that lets us interact with the running app from a terminal, simulate key presses, inspect state, and get thread dumps even when the UI is frozen.

## Approach

Add a lightweight TCP server (localhost-only, port 28081) that accepts text commands over a socket. Three files touched:

1. **New**: `sangeet-desktop/.../editor/DebugConsole.scala` — TCP server + command dispatch
2. **Edit**: `EditorPane.scala` — add a few public accessor methods for debug introspection
3. **Edit**: `MainApp.scala` — wire up lifecycle (start on launch, stop on close)

## Command Protocol

- **Input**: one command per line (`\n`-terminated)
- **Output**: text response terminated by `---END---` sentinel line
- **Usage**: `nc 127.0.0.1 28081` or `rlwrap nc 127.0.0.1 28081` or `echo "get-state" | nc 127.0.0.1 28081`

### Commands

| Command | FX Thread? | Description |
|---|---|---|
| `ping` | No | Returns "pong" |
| `help` | No | Lists commands |
| `thread-dump` | No | All JVM thread states — works even during UI freeze |
| `set-debug on\|off` | No | Toggle `AppLogger.debugEnabled` |
| `type <key>` | Yes | Simulate swar input (e.g. `type m`, `type S` for komal Re) |
| `press <key>` | Yes | Simulate special key: `space`, `backspace`, `left`, `right`, `minus` |
| `get-state` | Yes | Cursor position, section, event count, read-only, edit mode |
| `get-events` | Yes | All events in current section |
| `dump-composition` | Yes | Full composition as JSON via `CompositionApi.serializeCompositionString` |
| `dump-history` | Yes | Undo past/future counts |
| `check-focus` | Yes | Which node has focus, whether ScrollPane is focused |
| `focus` | Yes | Force focus back to editor |

## Thread Model

- **Accept thread** (daemon): loops on `serverSocket.accept()`, spawns handler per client
- **Client handler thread** (daemon): reads lines, dispatches commands
- **FX thread bridge**: `Platform.runLater` + `CompletableFuture` with **5-second timeout** — if FX thread is frozen, the client gets a timeout error instead of hanging forever. Commands like `thread-dump` and `ping` skip this bridge entirely, so they always work.

## EditorPane Changes

Add these public methods (all simple accessors/delegators):

- `undoHistoryInfo: (Int, Int)` — past/future counts from UndoHistory
- `isScrollPaneFocused: Boolean` — delegates to `scrollPane.isFocused`
- `debugTypeChar(ch: Char): String` — calls `KeyHandler.handleSwarKey`, pushes editor, redraws, returns status
- `debugPressKey(keyName: String): String` — handles SPACE/BACKSPACE/MINUS via `KeyHandler.handleSpecialKey`, LEFT/RIGHT via cursor methods

These methods encapsulate all editor mutation inside EditorPane. The debug console never touches `pushEditor` or `setEditorDirect` directly.

## MainApp Changes

- After `val editorPane = new EditorPane(statusBar)` (line 57): create and start `DebugConsole`
- In `setOnCloseRequest` (line 515): call `debugConsole.stop()` before `playbackController.shutdown()`
- Log the port to both AppLogger and stderr so it's visible in the sbt console

## DebugConsole.scala Structure

```
class DebugConsole(editorPane: EditorPane, statusBar: StatusBar, port: Int = 28081):
  - start(): bind ServerSocket to 127.0.0.1, start daemon accept thread
  - stop(): set running=false, close all sockets
  - handleClient(socket): read-line loop, dispatch, write response + ---END---
  - dispatch(cmd, args): match on command name → call handler
  - runOnFxThread[T](f: => T): Either[String, T]  (Platform.runLater + CompletableFuture, 5s timeout)
  - Command methods: cmdPing, cmdHelp, cmdThreadDump, cmdSetDebug, cmdGetState, etc.
```

No new dependencies — uses `java.net.ServerSocket`, `java.io`, `java.util.concurrent` from stdlib.

## Key Design Decisions

- **Localhost only** (`InetAddress.getLoopbackAddress`) — no security risk
- **Port 28081** — follows existing pattern (web server is 28080)
- **Multiple clients OK** — all FX mutations serialize through `Platform.runLater`
- **Bind failure is non-fatal** — logs warning, app runs without debug console
- **5s FX timeout** — the whole point is diagnosing freezes; hanging forever would defeat the purpose
- **No double-tap or ornament mode** from debug console — keeps it simple. `type s` always enters a single Sa.

## Verification

1. `sbt sangeetDesktop/compile` — must compile cleanly
2. `sbt sangeetDesktop/run` — app launches, stderr shows `Debug console: nc 127.0.0.1 28081`
3. In another terminal: `nc 127.0.0.1 28081`
4. Test each command: `ping`, `help`, `get-state`, `type s`, `type m`, `press backspace`, `get-events`, `dump-history`, `check-focus`, `focus`, `thread-dump`, `set-debug on`, `dump-composition`
5. Verify app still works normally with keyboard input while debug console is connected
6. Close app — clean shutdown, no hanging threads
