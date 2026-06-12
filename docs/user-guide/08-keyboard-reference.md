# Keyboard Reference

A single page reference for every keystroke. The desktop app also shows this as a live panel on the right; toggle it with the « / » arrows or `Ctrl+Shift+K`.

## Swar (notes)

| Key | Swar | Variants (with Shift) |
|-----|------|----------------------|
| `s` | Sa | (none) |
| `r` | Re | `Shift+R` = komal Re |
| `g` | Ga | `Shift+G` = komal Ga |
| `m` | Ma | `Shift+M` = tivra Ma |
| `p` | Pa | (none) |
| `d` | Dha | `Shift+D` = komal Dha |
| `n` | Ni | `Shift+N` = komal Ni |

## Octave (saptak) — prefix the next note

| Key | Saptak |
|-----|--------|
| `.` | mandra (lower) — dot below |
| `'` | taar (upper) — dot above |
| `` ` `` | back to madhya (default) |

Hold the prefix twice for ati-mandra / ati-taar.

## Special events

| Key | Effect |
|-----|--------|
| `1` | Chikari (open strings) |
| `Space` | Rest (silence) |
| `-` | Sustain (hold previous note) |
| `Backspace` | Delete event before cursor |
| `Delete` | Delete event under cursor |

## Subdivisions

| Key | Effect |
|-----|--------|
| `Ctrl+2` … `Ctrl+8` | Set notes per beat (2–8) |
| Fast typing | Type 2–4 notes within 500 ms to auto-group on one beat |
| `ss`, `rr`, `gg`, … | Double-tap shortcut for dual swar |

## Navigation

| Key | Effect |
|-----|--------|
| `←` `→` | Move cursor one beat |
| `Tab` | Next beat |
| `Enter` | Next cycle |
| Mouse click | Place cursor on clicked cell (or switch section) |

## Selection, clipboard, undo

| Key | Effect |
|-----|--------|
| `Shift+←` `Shift+→` | Extend selection by one beat |
| `Shift+Home` | Extend selection to start of section |
| `Shift+End` | Extend selection to last cycle |
| `Ctrl+X` | Cut |
| `Ctrl+C` | Copy |
| `Ctrl+V` | Paste at cursor |
| `Ctrl+Z` | Undo |
| `Ctrl+Shift+Z` | Redo |
| `Esc` | Exit ornament mode (does not clear selection) |

## Strokes (mizrab Da / Ra)

| Key | Effect |
|-----|--------|
| `Ctrl+D` | Mark last note as Da |
| `Ctrl+R` | Mark last note as Ra |

## Ornaments — simple (apply to last note)

| Key | Ornament |
|-----|----------|
| `Ctrl+G` | Gamak (heavy oscillation) |
| `Ctrl+A` | Andolan (gentle oscillation) |
| `Ctrl+I` | Gitkari (hammer/pull trill) |

## Ornaments — one note (key + swar key)

| Key combo | Ornament |
|-----------|----------|
| `Ctrl+K` + swar | Kan swar (grace note) |
| `Ctrl+H` + swar | Sparsh (light touch) |
| `Ctrl+E` + swar | Ghaseet (heavy pull) |

## Ornaments — two notes (key + start swar + end swar)

| Key combo | Ornament |
|-----------|----------|
| `Ctrl+M` + swar + swar | Meend ↑ (ascending glide) |
| `Ctrl+Shift+M` + swar + swar | Meend ↓ (descending glide) |
| `Ctrl+J` + swar + swar | Krintan (pull-off sequence) |

## Ornaments — multi-note (key + swars + Enter)

| Key combo | Ornament |
|-----------|----------|
| `Ctrl+U`, swars, `Enter` | Murki (ornamental turn) |
| `Ctrl+W`, swars, `Enter` | Zamzama (rapid cluster) |

`Esc` cancels any ornament mode without applying.

## Tabs and panels

| Key | Effect |
|-----|--------|
| `Ctrl+Tab` / `Ctrl+Shift+Tab` | Next / previous tab |
| `Ctrl+W` | Close active tab |
| `Ctrl+B` | Toggle file browser sidebar |
| `Ctrl+Shift+O` | Open Folder |

## File operations

Every toolbar button has a keyboard shortcut. Auto-save still runs 500 ms after each keystroke, so explicit `Ctrl+S` is rarely needed.

| Key | Action |
|-----|--------|
| `Ctrl+N` | New composition |
| `Ctrl+O` | Open file |
| `Ctrl+Shift+O` | Open folder |
| `Ctrl+S` | Save |
| `Ctrl+Shift+S` | Save as |
| `Ctrl+E` | Export HTML |
| `Ctrl+W` | Close active tab |

## Sections & properties

| Key | Action |
|-----|--------|
| `Ctrl+,` | Edit composition properties |
| `Ctrl+Shift+A` | Add section |
| `F2` | Rename current section |
| `Ctrl+Shift+Backspace` | Remove current section |

## View & help

| Key | Action |
|-----|--------|
| `Ctrl+Shift+T` | Toggle light / dark theme |
| `Ctrl+Shift+L` | Cycle notation script (Devanagari → Kannada → Telugu → English) |
| `F1` | Open user guide |
| `?` | Show keyboard cheat sheet (when not typing into a text field) |
| `Ctrl+Shift+B` | Report a bug |

**macOS:** replace `Ctrl` with `⌘` (Cmd) everywhere above. The desktop app's tooltips show the right glyph for your OS.

## Quick tips

- **Shift** = komal/tivra variant (not all swar have variants)
- **`.`** and **`'`** affect only the next note, then reset to madhya
- Strokes and ornaments apply to the **last entered note**
- Use stroke edit mode (`F2`) for bulk Da/Ra annotation
- Fast typing groups notes — type 2–4 notes within 500 ms for an auto-subdivision

## What to read next

- [Taals & Raags](09-taals-raags.md) — the built-in catalog
- [Starting Beat](10-starting-beat.md) — pickup beats before sam
