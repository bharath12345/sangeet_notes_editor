"""Make the package root importable so tests can `import transport_ws`.

The package is structured as a flat directory (server.py + transport_*.py
at the top level), not as a `src/` layout, so we prepend the project root
to sys.path explicitly.
"""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
