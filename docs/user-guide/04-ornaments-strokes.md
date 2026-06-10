# Ornaments & Strokes

## Mizrab strokes (Da and Ra)

The sitar's mizrab (plectrum) produces two basic stroke directions:

- **Da** — inward / downward stroke
- **Ra** — outward / upward stroke

Strokes are displayed in the row below the swar row. The editor can show them either automatically (alternating Da-Ra by default) or marked explicitly.

### Quick stroke insertion

| Key | Effect (applies to last entered note) |
|-----|----------------------------------------|
| `Ctrl+D` | Mark the previous note as Da |
| `Ctrl+R` | Mark the previous note as Ra |

### Stroke edit mode

For marking many strokes in a row, enter **stroke edit mode**:

1. Press `F2` to toggle stroke mode (status bar confirms)
2. In stroke mode, lowercase `d` = Da, `r` = Ra on the cell under the cursor
3. `Delete` clears the stroke (reverts to auto)
4. `Esc` exits stroke mode

Outside stroke mode, `d` and `r` type the swar Dha and Re respectively.

## Ornaments

All ornaments apply to the **last entered note**, except for multi-note ornaments which capture the next notes you type. Press `Esc` at any time to cancel ornament entry.

### Simple ornaments (one keystroke, applied to last note)

| Key | Ornament | Description |
|-----|----------|-------------|
| `Ctrl+G` | **Gamak** | Heavy oscillation |
| `Ctrl+A` | **Andolan** | Slow gentle oscillation |
| `Ctrl+I` | **Gitkari** | Hammer-on / pull-off trill |

### One-note ornaments (key + one swar)

These need one additional note key after the Ctrl combo:

| Key combo | Ornament | What to type next |
|-----------|----------|-------------------|
| `Ctrl+K` + `swar` | **Kan Swar** | The grace note before the main note |
| `Ctrl+H` + `swar` | **Sparsh** | Light touch of an adjacent note |
| `Ctrl+E` + `swar` | **Ghaseet** | Heavy lateral pull to the target note |

### Two-note ornaments (key + start + end)

These need two additional note keys: a start and an end:

| Key combo | Ornament | Notes |
|-----------|----------|-------|
| `Ctrl+M` + `swar` + `swar` | **Meend ↑** | Ascending glide |
| `Ctrl+Shift+M` + `swar` + `swar` | **Meend ↓** | Descending glide |
| `Ctrl+J` + `swar` + `swar` | **Krintan** | Pull-off sequence |

### Multi-note ornaments (key + several swars + Enter)

| Key combo | Ornament | How |
|-----------|----------|-----|
| `Ctrl+U` then swars then `Enter` | **Murki** | Type a rapid ornamental turn (3–5 notes), then Enter |
| `Ctrl+W` then swars then `Enter` | **Zamzama** | Rapid repeated cluster |

### How ornaments render

Ornaments appear in the **ornament row** above the swar row:
- Meend — curved line spanning from start to end note
- Kan / Sparsh — small grace-note glyph before the main note
- Gamak — wavy line above the note
- Andolan — gentle wave
- Gitkari / Krintan / Ghaseet — sitar-specific marks
- Murki / Zamzama — bracket spanning the affected notes

Ornaments use a **deep purple** color so they're visually distinct from the indigo swar row.

## Combining ornaments with strokes

A single note can carry both a stroke (Da/Ra) and one ornament. Type the swar, then apply the stroke and ornament in any order. Each replaces the previous of its kind on that note.

## Tihai

Tihais (rhythmic phrases repeated three times) are a section-level annotation rather than per-note. They render with a visual bracket and an "×3" marker. This feature is part of the data model; the UI for entering tihai brackets is documented in [Sections & Navigation](05-sections-navigation.md).

## What to read next

- [Sections & Navigation](05-sections-navigation.md) — moving across cycles and sections
- [Keyboard Reference](08-keyboard-reference.md) — complete keymap
