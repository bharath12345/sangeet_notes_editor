# UI Strings Catalog

## Overview

The UI strings catalog is a single source of truth for all user-visible strings shared between the desktop (Scala/ScalaFX) and web (Elm) platforms. It enforces compile-time type safety and cross-platform parity.

**Location:** `sangeet-core/src/main/resources/ui-strings.json`

**Generated files:**
- `sangeet-core/src/main/scala/com/varpas/sangeet/core/strings/UiStrings.scala`
- `sangeet-web/src/UiStrings.elm`

Both generated files are checked into git so IDEs can autocomplete and reference them.

## Schema

The catalog is a JSON file with the following structure:

```json
{
  "$comment": "Optional documentation comment",
  "entries": {
    "key.name": {
      "value": "Simple string value",
      "platform": "both",
      "description": "What this string is for"
    },
    "another.key": {
      "template": "String with {param} placeholders",
      "params": [
        { "name": "param", "type": "int" }
      ],
      "platform": "both",
      "description": "Parameterized string example"
    }
  }
}
```

### Entry fields

- **`value`** (string, optional): For simple string constants. Mutually exclusive with `template`.
- **`template`** (string, optional): For parameterized strings with `{placeholder}` syntax. Mutually exclusive with `value`.
- **`params`** (array, optional): Parameter definitions for template strings. Each param has:
  - `name` (string): Parameter identifier (matches placeholder in template)
  - `type` (string): Either `"int"` or `"string"`
- **`platform`** (string, default `"both"`): One of:
  - `"both"` — Must be referenced on both desktop and web (enforced by CI)
  - `"desktop"` — Desktop-only (e.g., platform-specific menu items)
  - `"web"` — Web-only (e.g., browser-specific UI)
- **`description`** (string, required): Human-readable description of where/why this string is used

### Key naming

Keys use dot-separated semantic naming: `area.component.element`

Examples:
- `toolbar.file.new` — "New" button in File menu on toolbar
- `dialog.about.title` — Title of About dialog
- `editor.cursor.beatLabel` — Beat label in cursor position indicator

Do NOT use English text as keys (e.g., `"New"` is bad, `toolbar.file.new` is good). This makes future i18n work possible without touching call sites.

## How to add a string

1. **Edit the catalog** — Add an entry to `ui-strings.json`:
   ```json
   "toolbar.file.new": {
     "value": "New",
     "platform": "both",
     "description": "File > New menu item"
   }
   ```

2. **Regenerate code** — Run:
   ```bash
   make gen-strings
   ```
   This runs:
   - `sbt sangeetCore/genUiStrings` (generates `UiStrings.scala`)
   - `cd scripts && npm run gen` (generates `UiStrings.elm`)

3. **Use the constant** on both sides:
   - **Scala:** `UiStrings.toolbarFileNew`
   - **Elm:** `UiStrings.toolbarFileNew`

4. **Commit all three files:**
   ```bash
   git add sangeet-core/src/main/resources/ui-strings.json \
           sangeet-core/src/main/scala/com/varpas/sangeet/core/strings/UiStrings.scala \
           sangeet-web/src/UiStrings.elm
   git commit -m "feat(strings): add toolbar.file.new"
   ```

The lefthook pre-commit hook automatically regenerates the Scala and Elm files if you change the catalog.

## How to add a parameterized string

For strings with dynamic content (counts, names, etc.):

```json
"editor.beatCount": {
  "template": "Beat {current} of {total}",
  "params": [
    { "name": "current", "type": "int" },
    { "name": "total", "type": "int" }
  ],
  "platform": "both",
  "description": "Current beat indicator in status bar"
}
```

Usage:
- **Scala:** `UiStrings.editorBeatCount(current = 3, total = 16)` → `"Beat 3 of 16"`
- **Elm:** `UiStrings.editorBeatCount 3 16` → `"Beat 3 of 16"`

Both are compile-time type-safe.

## Parity check

The CI pipeline runs `scripts/check-string-parity.ts` on every push. It fails if:

1. A `platform: "both"` entry is referenced in Scala but not Elm (or vice versa)
2. A `platform: "desktop"` entry is referenced in Elm code
3. A `platform: "web"` entry is referenced in Scala code
4. Source code references a key that doesn't exist in the catalog

Run the check locally:
```bash
make check-strings
```

## Finding untracked strings

To scan the codebase for English string literals that should be in the catalog:

```bash
make find-untracked-strings
```

This is heuristic (looks for English-looking literals) and may have false positives, but it helps catch migration gaps.

## Parity report

To generate a human-readable report of all catalog entries and their usage:

```bash
make strings-report
```

This writes `docs/strings-parity-report.md` showing:
- All `both` entries and their usage on each side
- All `desktop`-only and `web`-only entries (with justification review)
- Unused entries
- Orphaned references (code references to non-existent keys)

## Troubleshooting

**"UiStrings object not found"**
- Run `make gen-strings` to regenerate the files

**"Key not found in catalog" error from parity check**
- Either add the key to `ui-strings.json` or remove the reference from code

**"Both-platform entry only referenced on one side"**
- Either change `platform` to `"desktop"` or `"web"` (and document why), or add the reference on the missing side

**Generated files have merge conflicts**
- Resolve the catalog conflict, then run `make gen-strings` to regenerate deterministically
