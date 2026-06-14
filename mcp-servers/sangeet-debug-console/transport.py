"""Abstract Transport interface shared by TCP and WebSocket back ends.

The MCP server speaks a legacy "command line" text format ("ping",
"type srgmp", "reset gat yaman teentaal", ...). The TCP transport delivers
those lines verbatim to the desktop's DebugConsole on 127.0.0.1:28081. The
WS transport parses them into the JSON envelope the web debug bridge expects
({"id": "req-N", "cmd": {"VariantName": {...}}}) before sending.

Both back ends return a single text reply per call. From the MCP tools' point
of view the two are interchangeable.
"""
from __future__ import annotations

from abc import ABC, abstractmethod


class Transport(ABC):
    """Minimal contract every transport must satisfy.

    Subclasses are expected to be short-lived (one per MCP server process)
    but `send` may be called repeatedly. Implementations should be safe for
    sequential use; concurrent use is not required by the MCP tool layer
    (MCP tool calls are serialized by the host).
    """

    @abstractmethod
    def send(self, command_text: str) -> str:
        """Send a single command in the legacy text format and return the
        text response. WS implementations parse the text to JSON internally."""

    @abstractmethod
    def close(self) -> None:
        """Release any held resources (open sockets, background threads)."""
