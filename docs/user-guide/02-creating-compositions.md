# Creating Compositions

Click **New** in the toolbar to open the New Composition dialog.

## Composition types

| Type | Description | Default sections |
|------|-------------|------------------|
| **Gat** | Instrumental sitar composition. Use for masitkhani (vilambit) or razakhani (drut). | Gat, Antara, Taan × N |
| **Bandish** | Vocal composition. | Sthayi, Antara |
| **Palta** | Practice exercise / pattern. No laya — practiced at varying speeds. | Single section |

Gat compositions can have **multiple Taan sections** — set the count in the dialog (default 3, can be 0 to many). You can add or remove sections later from the toolbar.

## Required fields

- **Title** — name of the composition (e.g., "Yaman Vilambit Gat", "Bhairav Bandish")
- **Composition Type** — Gat / Bandish / Palta
- **Taal** — one of 11 built-in taals (see [Taals & Raags](09-taals-raags.md))
- **Raag** — one of 26 built-in raags
- **Laya** — tempo category: Ati-vilambit / Vilambit / Madhya / Drut / Ati-drut (Palta has no laya)
- **File path** — where to save the `.swar` file on disk

## Optional fields

- **Show stroke line** — display the Da/Ra row under each beat (default on for Gat)
- **Show sahitya line** — display a lyrics row under each beat (default on for Bandish)
- **Script** — the notation script for display: Devanagari (Hindi), Kannada, Telugu, or English. Keyboard input is always Roman; only the rendered output changes.
- **Starting beat** for each section (Gat / Bandish only):
  - **Gat starting beat** — first beat of the gat / sthayi section
  - **Antara starting beat** — first beat of the antara section
  - **Taan starting beat** — first beat of every taan section

A starting beat greater than 1 means the section begins partway through cycle 0 — the beats before sam are "locked" placeholders that you cannot type into or delete. See [Starting Beat](10-starting-beat.md) for the full explanation.

## After clicking Create

A new tab opens with your empty composition. The cursor is positioned at the first editable beat (the starting beat, or beat 1 if no starting beat was set). Start typing notes — see [Entering Notes](03-entering-notes.md).

The file is saved immediately to the path you chose, and auto-saved every time you make an edit (500 ms after the last keystroke, in the background).

## Editing composition metadata later

Click **Properties** in the toolbar to change:
- Title, raag, laya, script
- Taal (changes are remapped — events shift to fit the new matra count)
- Starting beat for each section (events shift to accommodate)
- Show/hide stroke and sahitya lines

## Sections

Use the toolbar buttons to manage sections:
- **Add Section** (Gat only) — pick from Gat, Sthayi, Antara, Taan, Jhala, Jod
- **Rename Section** — rename the currently-active section
- **Remove Section** — delete the currently-active section (cannot remove the last one)
- **Move Up / Move Down** — reorder sections

The currently-active section is the one your cursor is in. Click anywhere in another section's grid to switch to it.

## What to read next

- [Entering Notes](03-entering-notes.md) — keyboard input for swar, octaves, subdivisions
- [Sections & Navigation](05-sections-navigation.md) — moving around the composition
- [Starting Beat](10-starting-beat.md) — mukhda and pickup beats before sam
