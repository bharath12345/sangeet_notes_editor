"""WebSocket transport — drives the running web app's debug bridge.

Hosts a small WS server on a loopback port. The Elm app, loaded with
``?debug=ws://localhost:<port>``, connects in. Subsequent `send` calls
parse the legacy text command into the JSON envelope the web bridge
expects (``{"id": "req-N", "cmd": {<VariantName>: {...}}}``), forward it,
and block on the matching response.

The text→JSON mapping mirrors ``DebugCommand.fromText`` in sangeet-core
(see DebugCommand.scala) so the wire vocabulary is identical to TCP. If
sangeet-core adds a new variant, we update both the dispatcher arms (Scala
+ Elm) and the corresponding arm in ``text_to_json_cmd`` here.
"""
from __future__ import annotations

import json
import threading
from queue import Empty, Queue
from typing import Any

try:
    # `websockets` is an optional runtime dep — only required when
    # --transport ws is selected.
    import websockets.sync.server as wss
except ImportError as e:  # pragma: no cover - import error surfaced lazily
    raise ImportError(
        "The 'websockets' package is required for --transport ws. "
        "Install via `pip install websockets>=12.0`."
    ) from e

from transport import Transport

DEFAULT_PORT = 9999
CONNECT_TIMEOUT_S = 30.0
REPLY_TIMEOUT_S = 5.0


def text_to_json_cmd(text: str) -> dict[str, Any]:
    """Mirror of `com.varpas.sangeet.core.debug.DebugCommand.fromText`.

    The Elm bridge's decoder accepts the circe-encoded shape, namely a
    single-key object whose value is the variant payload. Parameterless
    variants encode as ``{"Ping": {}}``.

    Raises ``ValueError`` for unknown commands so callers can surface a
    helpful error instead of silently dropping the request.
    """
    tokens = text.strip().split()
    if not tokens:
        raise ValueError("empty command")
    head, *rest = tokens

    # Parameterless variants -------------------------------------------------
    nullary = {
        "ping": "Ping",
        "help": "Help",
        "thread-dump": "ThreadDump",
        "throw-crash": "ThrowCrash",
        "list-tabs": "ListTabs",
        "new-tab": "NewTab",
        "tab-info": "TabInfo",
        "check-focus": "CheckFocus",
        "focus": "FocusEditor",
        "focus-editor": "FocusEditor",
        "finish-ornament": "FinishOrnament",
        "get-state": "GetState",
        "get-events": "GetEvents",
        "dump-composition": "DumpComposition",
        "dump-history": "DumpHistory",
        "export-html": "ExportHtml",
    }
    # `throw` (legacy alias) accepts an ignored message tail.
    if head == "throw":
        return {"ThrowCrash": {}}
    if head in nullary and not rest:
        return {nullary[head]: {}}

    # Parameterised variants -------------------------------------------------
    if head == "set-debug" and len(rest) == 1:
        arg = rest[0].lower()
        if arg in ("on", "true", "1"):
            return {"SetDebug": {"enabled": True}}
        if arg in ("off", "false", "0"):
            return {"SetDebug": {"enabled": False}}
        raise ValueError(f"set-debug: expected on/off/true/false/1/0, got '{rest[0]}'")

    if head == "select-tab" and len(rest) == 1:
        return {"SelectTab": {"id": rest[0]}}

    if head == "close-tab":
        return {"CloseTab": {"id": rest[0] if rest else ""}}

    if head == "reset":
        # reset                          → defaults
        # reset <type>                   → 1 arg
        # reset <type> <taal>            → 2 args
        # reset <type> <raag> <taal>     → 3 args (new)
        # reset <type> <taal> <int>      → 3 args (legacy: taanCount, ignored)
        if not rest:
            return {"Reset": {"compositionType": "gat", "raag": None, "taal": "teentaal"}}
        comp_type = rest[0]
        tail = rest[1:]
        if len(tail) == 1:
            return {"Reset": {"compositionType": comp_type, "raag": None, "taal": tail[0]}}
        if len(tail) == 2:
            mid, last = tail
            if last.isdigit():
                # legacy: type + taal + taanCount
                return {"Reset": {"compositionType": comp_type, "raag": None, "taal": mid}}
            return {"Reset": {"compositionType": comp_type, "raag": mid, "taal": last}}
        if len(tail) == 3:
            # legacy: type + taal + taanCount + extra → drop extras
            return {"Reset": {"compositionType": comp_type, "raag": None, "taal": tail[0]}}
        raise ValueError(f"reset: expected 0-3 args, got {len(tail)}")

    if head == "set-taal" and len(rest) == 1:
        return {"SetTaal": {"taal": rest[0]}}

    if head in ("octave", "set-octave") and len(rest) == 1:
        return {"SetOctave": {"octave": rest[0]}}

    if head in ("subdivision", "set-subdivision") and len(rest) == 1:
        try:
            n = int(rest[0])
        except ValueError as e:
            raise ValueError(f"set-subdivision: int expected, got '{rest[0]}'") from e
        return {"SetSubdivision": {"n": n}}

    if head == "type" and rest:
        # Mirror Scala: concatenate trailing tokens (e.g. "type s r g" → "srg")
        return {"TypeChar": {"ch": "".join(rest)}}

    if head == "press" and len(rest) == 1:
        return {"Press": {"key": rest[0]}}

    if head == "type-timed" and len(rest) == 2:
        try:
            delay = int(rest[1])
        except ValueError as e:
            raise ValueError(f"type-timed: int expected for delay, got '{rest[1]}'") from e
        return {"TypeTimed": {"ch": rest[0], "delayMs": delay}}

    if head == "dual":
        if len(rest) == 2:
            return {"DualSwar": {"first": rest[0], "second": rest[1]}}
        if len(rest) == 1:
            return {"DualSwar": {"first": rest[0], "second": rest[0]}}

    if head in ("group", "swar-group") and len(rest) == 1:
        return {"SwarGroup": {"notes": list(rest[0])}}

    if head == "stroke" and len(rest) == 1:
        return {"Stroke": {"stroke": rest[0]}}

    if head in ("ornament", "simple-ornament") and len(rest) == 1:
        return {"SimpleOrnament": {"name": rest[0]}}

    if head == "ornament-start" and len(rest) == 1:
        return {"OrnamentStart": {"kind": rest[0]}}

    if head == "ornament-note" and len(rest) == 1:
        return {"OrnamentNote": {"note": rest[0]}}

    if head in ("section", "switch-section") and len(rest) == 1:
        try:
            idx = int(rest[0])
        except ValueError as e:
            raise ValueError(f"switch-section: int expected, got '{rest[0]}'") from e
        return {"SwitchSection": {"idx": idx}}

    raise ValueError(f"Unsupported command for WS transport: {text!r}")


class WsTransport(Transport):
    """WebSocket transport. Hosts the WS server; awaits a single Elm client.

    The first `send` blocks up to ``CONNECT_TIMEOUT_S`` waiting for the
    Elm app to connect; later calls go straight through. Each request gets
    a unique ``req-N`` id so responses can be matched to their request even
    if the bridge replies out of order (which it currently doesn't, but the
    contract allows for it).
    """

    def __init__(self, port: int = DEFAULT_PORT, host: str = "127.0.0.1") -> None:
        self.host = host
        self.port = port
        self._next_id = 0
        self._id_lock = threading.Lock()
        self._pending: dict[str, Queue[dict[str, Any]]] = {}
        self._socket: Any | None = None
        self._connected = threading.Event()
        self._server = wss.serve(self._handle, host, port)
        threading.Thread(target=self._server.serve_forever, daemon=True).start()
        print(f"[ws-transport] listening on ws://{host}:{port}")
        print(f"[ws-transport] load the web app with ?debug=ws://localhost:{port}")

    def _handle(self, websocket: Any) -> None:
        self._socket = websocket
        self._connected.set()
        for raw in websocket:
            try:
                data = json.loads(raw)
            except json.JSONDecodeError:
                continue
            q = self._pending.get(data.get("id"))
            if q is not None:
                q.put(data)

    def _allocate_id(self) -> str:
        with self._id_lock:
            self._next_id += 1
            return f"req-{self._next_id}"

    def send(self, command_text: str) -> str:
        if not self._connected.wait(timeout=CONNECT_TIMEOUT_S):
            return f"ERROR: no WS client connected within {CONNECT_TIMEOUT_S:.0f}s"
        assert self._socket is not None

        try:
            cmd_json = text_to_json_cmd(command_text)
        except ValueError as e:
            return f"ERROR: {e}"

        req_id = self._allocate_id()
        q: Queue[dict[str, Any]] = Queue()
        self._pending[req_id] = q
        try:
            envelope = {"id": req_id, "cmd": cmd_json}
            self._socket.send(json.dumps(envelope))
            try:
                response = q.get(timeout=REPLY_TIMEOUT_S)
            except Empty:
                return f"ERROR: WS reply timed out after {REPLY_TIMEOUT_S:.1f}s"
        finally:
            self._pending.pop(req_id, None)

        if response.get("error"):
            return f"ERROR: {response['error']}"
        result = response.get("result")
        if isinstance(result, str):
            return result
        return json.dumps(result)

    def close(self) -> None:
        try:
            if self._socket is not None:
                self._socket.close()
        finally:
            self._server.shutdown()
