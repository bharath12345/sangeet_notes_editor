#!/usr/bin/env python3
"""Generate golden fixtures by replaying test definitions against the desktop app.

Reads all .json files from tests/integration/, executes their commands via TCP,
and saves the resulting .swar and .html outputs to tests/integration/golden/.

Pre-condition: sangeet-desktop must be running with TCP debug console on 127.0.0.1:28081
"""
import json
import socket
import sys
import time
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
TESTS_DIR = REPO_ROOT / 'tests' / 'integration'
GOLDEN_DIR = TESTS_DIR / 'golden'
HOST, PORT = '127.0.0.1', 28081


def tcp_send(sock, line):
    """Send a command and read response until END marker."""
    sock.sendall((line + '\n').encode('utf-8'))
    time.sleep(0.1)  # Small delay to let command execute
    chunks = []
    sock.settimeout(2.0)
    try:
        while True:
            chunk = sock.recv(65536)
            if not chunk:
                break
            decoded = chunk.decode('utf-8')
            chunks.append(decoded)
            if '---END---' in decoded:
                break
    except socket.timeout:
        pass

    response = ''.join(chunks)
    # Strip the END marker and trailing newlines
    if '---END---' in response:
        response = response[:response.rfind('---END---')]
    return response.strip()


def cmd_to_text(cmd):
    """Convert a DebugCommand JSON object to TCP text format."""
    # Handle each command variant
    if 'Reset' in cmd:
        r = cmd['Reset']
        parts = ['reset', r['compositionType']]
        if 'raag' in r and r['raag']:
            parts.append(r['raag'])
        parts.append(r['taal'])
        return ' '.join(parts)

    if 'TypeChar' in cmd:
        return f"type {cmd['TypeChar']['ch']}"

    if 'Press' in cmd:
        key = cmd['Press']['key']
        # Map special keys
        if key == ' ':
            return 'press SPACE'
        elif key == '-':
            return 'press MINUS'
        else:
            return f'press {key}'

    if 'SetOctave' in cmd:
        return f"set-octave {cmd['SetOctave']['octave']}"

    if 'SetSubdivision' in cmd:
        return f"set-subdivision {cmd['SetSubdivision']['n']}"

    if 'SwitchSection' in cmd:
        return f"switch-section {cmd['SwitchSection']['idx']}"

    if 'GetState' in cmd:
        return 'get-state'

    if 'DumpComposition' in cmd:
        return 'dump-composition'

    if 'ExportHtml' in cmd:
        return 'export-html'

    if 'Ping' in cmd:
        return 'ping'

    raise ValueError(f"Unsupported command for TCP conversion: {cmd}")


def replay_test(defn, sock):
    """Replay all commands in a test definition and return the final .swar and .html."""
    for step in defn['steps']:
        if 'Cmd' in step:
            cmd = step['Cmd']['cmd']
            # Skip dump/export commands in the test steps - we'll run them at the end
            if 'DumpComposition' in cmd or 'ExportHtml' in cmd:
                continue
            cmd_text = cmd_to_text(cmd)
            response = tcp_send(sock, cmd_text)
            if 'ERROR' in response:
                print(f"    Warning: {cmd_text} -> {response[:80]}")
        elif 'Checkpoint' in step:
            # Skip checkpoint steps (only used by test runners)
            pass

    # Get final outputs
    swar = tcp_send(sock, 'dump-composition')
    time.sleep(0.2)
    html = tcp_send(sock, 'export-html')
    return swar, html


def main():
    GOLDEN_DIR.mkdir(parents=True, exist_ok=True)

    # Find all test definition files (skip the synthetic ping test)
    test_files = sorted([f for f in TESTS_DIR.glob('*.json') if not f.name.startswith('00-')])

    print(f"Generating golden fixtures for {len(test_files)} tests...")

    for defn_path in test_files:
        defn = json.loads(defn_path.read_text())
        name = defn['name']

        try:
            with socket.create_connection((HOST, PORT), timeout=5) as sock:
                # Consume the welcome banner
                tcp_send(sock, '')

                # Reset to clean state before each test
                tcp_send(sock, 'reset gat yaman teentaal')

                # Replay the test
                swar, html = replay_test(defn, sock)

                # Write golden fixtures
                swar_path = GOLDEN_DIR / f"{name}.swar"
                html_path = GOLDEN_DIR / f"{name}.html"
                swar_path.write_text(swar)
                html_path.write_text(html)

                print(f"  ✓ {name} ({len(swar)} chars .swar, {len(html)} chars .html)")
        except Exception as e:
            print(f"  ✗ {name}: {e}", file=sys.stderr)
            import traceback
            traceback.print_exc()
            return 1

    print(f"\nGenerated {len(test_files) * 2} golden fixture files in {GOLDEN_DIR}")
    return 0


if __name__ == '__main__':
    sys.exit(main())
