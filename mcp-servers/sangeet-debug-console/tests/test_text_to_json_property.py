"""Property: ``text_to_json_cmd`` maps every canonical text form to its
canonical JSON dict.

This is the genesis Hypothesis property for the MCP server. The strategy
in ``strategies.debug_command`` enumerates the canonical text shapes the
parser accepts (one per ``DebugCommand`` variant) paired with the dict
the parser is contracted to return. The property drives the parser with
hundreds of generated inputs across all variants and asserts equality —
catching any future regression where an arm starts returning a different
shape than its spec.

We intentionally keep this to a single property at T5A scope; T5B will
add more (e.g. error-path properties for malformed inputs, idempotence
of the JSON envelope).
"""
from __future__ import annotations

from typing import Any

from hypothesis import given

from transport_ws import text_to_json_cmd

from .strategies import debug_command


@given(debug_command())
def test_prop_text_parses_to_canonical_json(pair: tuple[str, dict[str, Any]]) -> None:
    """Every (text, expected_json) pair drawn from ``debug_command`` must
    round-trip through ``text_to_json_cmd`` to its declared shape.

    Asserts:
      - The parser does not raise for canonical inputs.
      - The returned dict equals the canonical JSON encoding.
    """
    text, expected = pair
    assert text_to_json_cmd(text) == expected
