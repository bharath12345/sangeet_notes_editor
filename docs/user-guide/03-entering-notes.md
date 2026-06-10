# Entering Notes

The editor uses **Roman keyboard input** that renders as Devanagari (or Kannada / Telugu / English) glyphs on screen.

## The seven swar

| Key | Swar | Devanagari |
|-----|------|------------|
| `s` | Sa | स |
| `r` | Re | रे |
| `g` | Ga | ग |
| `m` | Ma | म |
| `p` | Pa | प |
| `d` | Dha | ध |
| `n` | Ni | नि |

Type the key and the note appears at the cursor position. The cursor advances one beat (or one sub-position within a beat — see Subdivisions below).

## Komal and tivra variants

Hold **Shift** for the altered variant:

| Key combo | Result |
|-----------|--------|
| `Shift+R` | Komal Re (र — underlined) |
| `Shift+G` | Komal Ga (ग — underlined) |
| `Shift+M` | Tivra Ma (म — with vertical stroke above) |
| `Shift+D` | Komal Dha (ध — underlined) |
| `Shift+N` | Komal Ni (नि — underlined) |

Sa and Pa are achal (fixed) — they have no komal or tivra variant. Shift+S and Shift+P do nothing.

## Octaves (saptak)

By default, notes are in **madhya** (middle) saptak. Modify the next note with these prefixes:

| Key | Effect |
|-----|--------|
| `.` | Next note is in **mandra** (lower octave) — dot below the glyph |
| `'` | Next note is in **taar** (upper octave) — dot above the glyph |
| `` ` `` | Back to **madhya** explicitly (cancels a pending `.` or `'`) |

These prefixes affect **only the next note**, then reset. Example: `.s s 's` produces low-Sa, mid-Sa, high-Sa.

For extended range (rare): hold the prefix twice for ati-mandra / ati-taar (two dots).

## Subdivisions — multiple notes per beat

By default, one note occupies one beat. To fit multiple notes in a beat:

- **`Ctrl+2` through `Ctrl+8`** — set subdivisions per beat (2 to 8). The next notes typed will share the current beat at equal sub-positions.
- The cursor's sub-position indicator shows where in the beat you are.
- After filling the beat, the cursor moves to the next beat at subdivision 1.

### Fast-typing shortcut

Type 2 to 4 notes within **500 milliseconds** of each other and they automatically group on the same beat with equal subdivisions. So typing `srg` quickly puts S R G on one beat as a triplet. Wait longer than 500 ms and the next note starts a new beat.

### Dual swar shortcut

Type the same swar key **twice rapidly** (e.g., `ss`, `rr`, `gg`) to insert it as a dual swar — two of that note on one beat. This is a very common pattern in Hindustani music.

## Special events

| Key | Result |
|-----|--------|
| `1` | **Chikari** — the open sympathetic strings (a non-melodic stroke) |
| `Space` | **Rest** — silence for one beat (or sub-position) |
| `-` | **Sustain** — hold the previous note across this beat |

## Deleting

| Key | Effect |
|-----|--------|
| `Backspace` | Delete the event before the cursor and move cursor back |
| `Delete` | Delete the event under the cursor |

If the deleted event was part of a fast-typed group, the entire group is removed and the beat is restored to subdivision 1.

**Locked beats** (the dot placeholders before a starting beat) cannot be deleted. See [Starting Beat](10-starting-beat.md).

## Changing the script

Use the **Script** dropdown in the toolbar to switch the rendered output between:
- Devanagari (Hindi) — स रे ग म प ध नि
- Kannada — ಸ ರಿ ಗ ಮ ಪ ಧ ನಿ
- Telugu — స రి గ మ ప ధ ని
- English — S R G M P D N

The change is purely visual — your keyboard input is always Roman, and the file format is script-independent.

## What to read next

- [Ornaments & Strokes](04-ornaments-strokes.md) — meend, gamak, kan, Da/Ra
- [Sections & Navigation](05-sections-navigation.md) — moving the cursor across cycles
- [Keyboard Reference](08-keyboard-reference.md) — full keymap
