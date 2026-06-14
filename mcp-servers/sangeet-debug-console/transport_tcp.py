"""TCP transport — talks to the desktop app's DebugConsole on 127.0.0.1:28081.

Each `send` opens a fresh connection, drains the banner, writes the command
line, then reads up to the END_MARKER. The desktop console is stateless
across connections — state lives in the running app — so this connection
model matches its semantics exactly.
"""
from __future__ import annotations

import socket

from transport import Transport

DEFAULT_HOST = "127.0.0.1"
DEFAULT_PORT = 28081
END_MARKER = "---END---"


class TcpTransport(Transport):
    """Synchronous TCP transport.

    `timeout` covers connect + read together. The desktop's FX-thread
    handler has its own 5 s ceiling, so 6 s here gives the reply a chance
    to land even when the UI thread is mid-action.
    """

    def __init__(
        self,
        host: str = DEFAULT_HOST,
        port: int = DEFAULT_PORT,
        timeout: float = 6.0,
    ) -> None:
        self.host = host
        self.port = port
        self.timeout = timeout

    def send(self, command_text: str) -> str:
        try:
            with socket.create_connection((self.host, self.port), timeout=self.timeout) as sock:
                sock.settimeout(self.timeout)
                # Drain the banner ("Sangeet Debug Console..." + END_MARKER).
                _read_until(sock, END_MARKER)
                sock.sendall((command_text + "\n").encode("utf-8"))
                reply = _read_until(sock, END_MARKER)
                return reply.strip()
        except (ConnectionRefusedError, socket.timeout, OSError) as e:
            return (
                f"ERROR: cannot reach desktop debug console at "
                f"{self.host}:{self.port} — is the app running? ({e})"
            )

    def close(self) -> None:
        # Each send() opens and closes its own socket; nothing to release.
        pass


def _read_until(sock: socket.socket, marker: str) -> str:
    """Read from `sock` until a line equal to `marker` is observed. Returns
    the accumulated text excluding the marker line."""
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
