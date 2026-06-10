# Starting Beat (Mukhda Before Sam)

Many Hindustani compositions begin their melodic phrase **before** sam (the first beat of the taal cycle). The opening phrase that resolves on sam is called the **mukhda**.

For example, a Yaman Vilambit Gat in Teentaal (16 matras) typically starts on **beat 9**, with the gat phrase landing on sam (beat 1) of the next cycle. The first 8 beats of the cycle (beats 1–8) come "before" the gat begins — they are placeholders, not editable.

## How the editor represents this

When you set a section's **starting beat** to 9, the editor:
1. **Pre-fills** beats 1–8 of cycle 0 with **LockedBeat** placeholder events (shown as dots: `•`)
2. **Prevents** the cursor from typing or deleting in those locked positions
3. **Persists** the locked beats in the `.swar` file so they don't have to be regenerated
4. **Restores** them on load even if the file is old or hand-edited

Locked beats appear on **cycle 0 only** — subsequent cycles use the full taal length.

## Setting a starting beat

### When creating a new composition

The New Composition dialog has starting-beat inputs:
- **Gat starting beat** — for the Gat (or Sthayi) section
- **Antara starting beat** — for the Antara section
- **Taan starting beat** — for every Taan section

Set any of these to a value greater than 1 to begin that section partway through cycle 0.

### Changing an existing composition

Open **Properties** from the toolbar. The dialog includes a starting-beat input for each section. When you change a value:

- **Increasing** the starting beat (e.g., 1 → 9): the editor inserts more locked beats at the start of cycle 0 and **shifts all existing events forward** by the difference. Events that overflow past the end of cycle 0 wrap into cycle 1 (or new cycles are created).
- **Decreasing** the starting beat (e.g., 9 → 5): the editor removes locked beats and **shifts events backward**.

Your existing music is never lost — it just slides in the appropriate direction.

## Why this matters

Without LockedBeat events, three things go wrong:
1. The cursor could land on beats 1–8 of cycle 0, and you might accidentally type there
2. Backspace/Delete could erase the visual structure
3. The visual dots wouldn't survive a save/reload cycle

By making locked beats first-class events:
- The cursor automatically skips over them when navigating
- Backspace stops at the first editable position (the starting beat)
- The `.swar` file is self-describing — no special-case rendering logic

## Composition types that support starting beat

| Type | Starting beat applies to |
|------|--------------------------|
| **Gat** | Gat section, Antara section, every Taan section (independent values) |
| **Bandish** | Sthayi section, Antara section |
| **Palta** | Not applicable — Palta sections start at beat 1 |

## Editing tips

- The cursor automatically moves to the starting beat when you switch sections or create a new composition
- Pressing `Home` jumps to the starting beat (not beat 1) for sections with locked beats
- If you want a section to truly start at beat 1, set the starting beat to 1 (the default)
- The Properties dialog shows all sections at once, so you can adjust starting beats across the whole composition in one go

## What if the file format is older?

`.swar` files created before LockedBeat events existed don't contain the locked-beat placeholders — only the `startingBeat` field on each section. The editor **migrates on load**: if it sees a section with `startingBeat > 1` and no LockedBeat events, it injects them automatically. The migration is idempotent (running it again is a no-op).

## What to read next

- [Sections & Navigation](05-sections-navigation.md) — moving around the grid
- [Creating Compositions](02-creating-compositions.md) — setting starting beats from the start
- [File Operations](07-file-operations.md) — auto-save and the file format
