"""Smoke tests for the WS transport.

Two layers:

1. ``text_to_json_cmd`` — pure parser; verify representative arms match
   the circe-encoded shape sangeet-core's ``DebugCommand`` produces.
2. ``WsTransport`` end-to-end — spin up a fake echo server that mimics the
   Elm bridge (reads ``{id, cmd}``, sends back ``{id, result}``) and verify
   ``send`` returns the echoed payload.

We deliberately don't cover every arm of ``text_to_json_cmd``; the
SharedIntegrationSpec / parity.spec.ts harness exercises the full surface
when the bridge is live. These tests catch regressions in the wrapper
itself.
"""
from __future__ import annotations

import json
import threading
import time
from typing import Any

import pytest
import websockets.sync.server as wss

from transport_ws import WsTransport, text_to_json_cmd


# ─── text_to_json_cmd ──────────────────────────────────────────────────────


class TestTextToJsonCmd:
    """Each case mirrors a `DebugCommand.fromText` arm in sangeet-core."""

    def test_ping_is_nullary_object(self) -> None:
        assert text_to_json_cmd("ping") == {"Ping": {}}

    def test_help_is_nullary_object(self) -> None:
        assert text_to_json_cmd("help") == {"Help": {}}

    def test_type_concatenates_trailing_tokens(self) -> None:
        # Mirrors Scala: "type s r g" → TypeChar("srg")
        assert text_to_json_cmd("type s r g") == {"TypeChar": {"ch": "srg"}}

    def test_type_single_char(self) -> None:
        assert text_to_json_cmd("type s") == {"TypeChar": {"ch": "s"}}

    def test_press_passes_key_through(self) -> None:
        assert text_to_json_cmd("press BACKSPACE") == {"Press": {"key": "BACKSPACE"}}

    def test_set_debug_on(self) -> None:
        assert text_to_json_cmd("set-debug on") == {"SetDebug": {"enabled": True}}

    def test_set_debug_off(self) -> None:
        assert text_to_json_cmd("set-debug off") == {"SetDebug": {"enabled": False}}

    def test_set_debug_rejects_garbage(self) -> None:
        with pytest.raises(ValueError, match="set-debug"):
            text_to_json_cmd("set-debug maybe")

    def test_reset_default(self) -> None:
        assert text_to_json_cmd("reset") == {
            "Reset": {"compositionType": "gat", "raag": None, "taal": "teentaal"}
        }

    def test_reset_two_args(self) -> None:
        assert text_to_json_cmd("reset palta teentaal") == {
            "Reset": {"compositionType": "palta", "raag": None, "taal": "teentaal"}
        }

    def test_reset_three_args_new_format(self) -> None:
        assert text_to_json_cmd("reset gat yaman teentaal") == {
            "Reset": {"compositionType": "gat", "raag": "yaman", "taal": "teentaal"}
        }

    def test_reset_three_args_legacy_taancount_format(self) -> None:
        # Numeric third arg = legacy taanCount; drop it, leave raag None.
        assert text_to_json_cmd("reset gat teentaal 0") == {
            "Reset": {"compositionType": "gat", "raag": None, "taal": "teentaal"}
        }

    def test_set_subdivision_parses_int(self) -> None:
        assert text_to_json_cmd("subdivision 4") == {"SetSubdivision": {"n": 4}}

    def test_set_subdivision_rejects_non_int(self) -> None:
        with pytest.raises(ValueError):
            text_to_json_cmd("subdivision four")

    def test_swar_group_splits_chars(self) -> None:
        assert text_to_json_cmd("group srgm") == {
            "SwarGroup": {"notes": ["s", "r", "g", "m"]}
        }

    def test_section_alias(self) -> None:
        assert text_to_json_cmd("section 2") == {"SwitchSection": {"idx": 2}}

    def test_focus_alias(self) -> None:
        assert text_to_json_cmd("focus") == {"FocusEditor": {}}

    def test_throw_tolerates_message_tail(self) -> None:
        # Legacy: "throw <ignored message>" — the desktop drops the tail.
        assert text_to_json_cmd("throw because reasons") == {"ThrowCrash": {}}

    def test_unknown_command_raises(self) -> None:
        with pytest.raises(ValueError, match="Unsupported"):
            text_to_json_cmd("definitely-not-a-real-command")

    def test_empty_input_raises(self) -> None:
        with pytest.raises(ValueError, match="empty"):
            text_to_json_cmd("   ")


# ─── End-to-end via fake echo server ───────────────────────────────────────


def _start_fake_bridge(port: int) -> Any:
    """Stand up a single-connection echo server that mimics the Elm bridge:
    decode each incoming envelope, respond with ``{id, result}`` where
    result is the command's JSON itself. Returns the server object so the
    test can shut it down."""

    def handler(ws: Any) -> None:
        for raw in ws:
            try:
                envelope = json.loads(raw)
            except json.JSONDecodeError:
                continue
            req_id = envelope.get("id")
            cmd = envelope.get("cmd")
            ws.send(json.dumps({"id": req_id, "result": cmd, "error": None}))

    server = wss.serve(handler, "127.0.0.1", port)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    return server


def _free_port() -> int:
    """Grab an arbitrary unused loopback port."""
    import socket

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


class TestWsTransportRoundTrip:
    def test_send_round_trips_ping(self) -> None:
        bridge_port = _free_port()
        bridge = _start_fake_bridge(bridge_port)
        try:
            # The transport hosts its own WS server. To exercise its client
            # behaviour we'd need the Elm side to connect — for the smoke
            # test we instead validate text→JSON in isolation (above) and
            # the server-side connection path via a separate transport
            # acting as the "bridge". To keep this self-contained, we use
            # a thin variant: spin up a transport, then connect a client to
            # IT (mimicking what the Elm app would do), and assert it sees
            # the JSON envelope produced by our text input.
            transport_port = _free_port()
            transport = WsTransport(port=transport_port)
            try:
                # Connect a fake "Elm" client to the transport's WS server.
                from websockets.sync.client import connect as ws_connect

                received: list[dict[str, Any]] = []
                client_thread_done = threading.Event()

                def fake_elm_client() -> None:
                    with ws_connect(f"ws://127.0.0.1:{transport_port}") as ws:
                        # Wait for the envelope sent by transport.send().
                        raw = ws.recv()
                        envelope = json.loads(raw)
                        received.append(envelope)
                        # Echo back: {id, result=cmd}
                        ws.send(
                            json.dumps(
                                {
                                    "id": envelope["id"],
                                    "result": envelope["cmd"],
                                    "error": None,
                                }
                            )
                        )
                        client_thread_done.set()

                client_thread = threading.Thread(target=fake_elm_client, daemon=True)
                client_thread.start()
                # Wait a moment for the WS handshake.
                time.sleep(0.2)

                # Drive the transport.
                reply = transport.send("ping")

                # Sanity-check the wire format we sent.
                assert client_thread_done.wait(timeout=3.0), "fake Elm client did not finish"
                assert len(received) == 1
                envelope = received[0]
                assert envelope["cmd"] == {"Ping": {}}
                assert envelope["id"].startswith("req-")

                # Reply is the JSON-encoded result.
                assert reply == json.dumps({"Ping": {}})
            finally:
                transport.close()
        finally:
            bridge.shutdown()
