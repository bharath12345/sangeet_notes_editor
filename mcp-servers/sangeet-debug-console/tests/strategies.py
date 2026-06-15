"""Hypothesis strategies for sangeet-debug-console properties.

These strategies generate values in the shapes the MCP server's text→JSON
parser (``transport_ws.text_to_json_cmd``) produces. The parser mirrors
``com.varpas.sangeet.core.debug.DebugCommand.fromText`` — every variant
here corresponds to an arm of that dispatcher.

The ``swar`` strategy respects the Sa/Pa achal rule (Sa and Pa never carry
komal/tivra variants). It is exposed for use by future properties that
need swar-shaped values even though the current text parser does not
embed a full Swar dict in any of its outputs.

Each ``debug_command`` member is a ``(text, expected_json)`` pair so a
single property can drive ``text_to_json_cmd`` and assert the returned
dict matches the canonical encoding.
"""
from __future__ import annotations

from typing import Any

from hypothesis import strategies as st

# ─── Swar primitives ───────────────────────────────────────────────────────

# Match the seven canonical Hindustani swar names used in sangeet-core's
# Swar.Note enum. Komal/Tivra are forbidden on Sa and Pa (achal swar).
NOTES: list[str] = ["Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni"]
VARIANTS: list[str] = ["Shuddha", "Komal", "Tivra"]
OCTAVES: list[str] = ["AtiMandra", "Mandra", "Madhya", "Taar", "AtiTaar"]


def swar() -> st.SearchStrategy[dict[str, str]]:
    """Generate a Swar dict respecting the Sa/Pa achal rule.

    Sa and Pa are achal (immovable) — they only ever take the Shuddha
    variant. The remaining five swar (Re, Ga, Ma, Dha, Ni) are vikrit and
    may be Shuddha, Komal, or Tivra (though Ma is the only swar that
    actually takes Tivra in classical theory, all five accept the variant
    at the model level).
    """
    achal = st.fixed_dictionaries(
        {
            "note": st.sampled_from(["Sa", "Pa"]),
            "variant": st.just("Shuddha"),
            "octave": st.sampled_from(OCTAVES),
        }
    )
    vikrit = st.fixed_dictionaries(
        {
            "note": st.sampled_from([n for n in NOTES if n not in ("Sa", "Pa")]),
            "variant": st.sampled_from(VARIANTS),
            "octave": st.sampled_from(OCTAVES),
        }
    )
    return st.one_of(achal, vikrit)


# ─── DebugCommand text/JSON pairs ──────────────────────────────────────────


# Tokens the parser accepts for ``set-debug``: "on", "true", "1" → enabled=True;
# "off", "false", "0" → enabled=False. Each variant in the source mapping is
# explicitly enumerated rather than fuzzed to keep the round-trip exact.
_SET_DEBUG_ON_TOKENS = ["on", "true", "1"]
_SET_DEBUG_OFF_TOKENS = ["off", "false", "0"]

# Aliases for the focus-editor command — both map to the same JSON.
_FOCUS_ALIASES = ["focus", "focus-editor"]

# Nullary command names (no arguments) and their JSON variant names. Mirrors
# the ``nullary`` dict inside ``text_to_json_cmd``.
_NULLARY = {
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


def _nullary_command() -> st.SearchStrategy[tuple[str, dict[str, Any]]]:
    """Draw a (text, json) pair for a parameterless command."""
    return st.sampled_from(
        [(text, {variant: {}}) for text, variant in _NULLARY.items()]
    )


def _set_debug_command() -> st.SearchStrategy[tuple[str, dict[str, Any]]]:
    """``set-debug on/off`` (with all accepted boolean aliases)."""
    on = st.sampled_from(_SET_DEBUG_ON_TOKENS).map(
        lambda tok: (f"set-debug {tok}", {"SetDebug": {"enabled": True}})
    )
    off = st.sampled_from(_SET_DEBUG_OFF_TOKENS).map(
        lambda tok: (f"set-debug {tok}", {"SetDebug": {"enabled": False}})
    )
    return st.one_of(on, off)


# Identifier-like tokens — letters, digits, dashes; never empty; never
# whitespace; never start with a digit (to avoid colliding with the legacy
# numeric tail in ``reset``).
_IDENT = st.from_regex(r"\A[a-z][a-z0-9-]{0,15}\Z", fullmatch=True)
# A short ASCII letter run used for the ``type`` and ``swar-group`` commands.
_SWAR_CHARS = st.text(alphabet="srgmpdnSRGMPDN", min_size=1, max_size=6)


def _type_command() -> st.SearchStrategy[tuple[str, dict[str, Any]]]:
    """``type <chars...>`` — trailing tokens are concatenated."""
    return _SWAR_CHARS.map(lambda chars: (f"type {chars}", {"TypeChar": {"ch": chars}}))


def _press_command() -> st.SearchStrategy[tuple[str, dict[str, Any]]]:
    """``press <KEY>`` — passes the key through verbatim."""
    keys = st.sampled_from(
        ["BACKSPACE", "ENTER", "ESC", "TAB", "ARROW_LEFT", "ARROW_RIGHT", "DELETE"]
    )
    return keys.map(lambda k: (f"press {k}", {"Press": {"key": k}}))


def _subdivision_command() -> st.SearchStrategy[tuple[str, dict[str, Any]]]:
    """``subdivision <int>`` / ``set-subdivision <int>``."""
    head = st.sampled_from(["subdivision", "set-subdivision"])
    n = st.integers(min_value=1, max_value=32)
    return st.tuples(head, n).map(
        lambda hn: (f"{hn[0]} {hn[1]}", {"SetSubdivision": {"n": hn[1]}})
    )


def _section_command() -> st.SearchStrategy[tuple[str, dict[str, Any]]]:
    """``section <int>`` / ``switch-section <int>``."""
    head = st.sampled_from(["section", "switch-section"])
    idx = st.integers(min_value=0, max_value=99)
    return st.tuples(head, idx).map(
        lambda hi: (f"{hi[0]} {hi[1]}", {"SwitchSection": {"idx": hi[1]}})
    )


def _swar_group_command() -> st.SearchStrategy[tuple[str, dict[str, Any]]]:
    """``group <chars>`` / ``swar-group <chars>`` — splits chars to list."""
    head = st.sampled_from(["group", "swar-group"])
    chars = _SWAR_CHARS
    return st.tuples(head, chars).map(
        lambda hc: (f"{hc[0]} {hc[1]}", {"SwarGroup": {"notes": list(hc[1])}})
    )


def _focus_command() -> st.SearchStrategy[tuple[str, dict[str, Any]]]:
    """``focus`` / ``focus-editor`` — both map to FocusEditor."""
    return st.sampled_from(
        [(alias, {"FocusEditor": {}}) for alias in _FOCUS_ALIASES]
    )


def debug_command() -> st.SearchStrategy[tuple[str, dict[str, Any]]]:
    """Generate a (text, expected_json) pair for an arm of ``text_to_json_cmd``.

    Each draw covers one DebugCommand variant the parser handles. The
    accompanying JSON is the canonical dict the parser should produce.
    """
    return st.one_of(
        _nullary_command(),
        _set_debug_command(),
        _type_command(),
        _press_command(),
        _subdivision_command(),
        _section_command(),
        _swar_group_command(),
        _focus_command(),
    )
