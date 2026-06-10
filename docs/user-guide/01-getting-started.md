# Getting Started

Sangeet Notes Editor is a notation editor for Hindustani classical music in the Bhatkhande style, designed primarily for sitar compositions. It runs on desktop (Mac, Windows, Linux) and in your browser.

## What you can do

- Write **gat**, **bandish**, and **palta** compositions
- Use **Bhatkhande notation** — Roman keyboard input, Devanagari (or Kannada / Telugu / English) output
- Pick from **11 built-in taals** and **26 raags**
- Mark sitar-specific elements: **mizrab strokes** (Da/Ra), **meend**, **kan swar**, **gamak**, **andolan**, **gitkari**, **murki**, **krintan**, **zamzama**, **ghaseet**, **sparsh**
- Set **per-section starting beats** (mukhda before sam) for gat / bandish sections
- Add **sahitya** (lyrics) aligned beat-by-beat
- Save as **`.swar`** files (UTF-8 JSON) on your computer
- Export to **HTML** for printing or sharing

## First launch

> **Desktop:** Download the installer for your platform from the [releases page](https://github.com/bharath12345/sangeet_notes_editor/releases). On Mac, drag the `.dmg` contents into Applications. On Windows, run the `.msi`. On Linux, install the `.deb`. Launch from your applications menu.
>
> **Web:** Open [sangeet-editor.in](https://sangeet-editor.in) in any modern browser.

On first launch you'll see a sample **Yaman Vilambit Gat** loaded read-only — it shows what a fully-marked composition looks like. The red "Read-only sample" notice indicates you can't edit it. Click **New** in the toolbar to start your own composition.

## The window at a glance

```
┌──────────────────────────────────────────────────────────────┐
│  Toolbar: New, Open, Save, Cut/Copy/Paste, Properties, ...   │
├────────┬────────────────────────────────────────────┬────────┤
│        │                                            │        │
│  File  │      Composition canvas (grid)             │ Keyb.  │
│ browser│                                            │ legend │
│        │      (cycles × beats with notation rows)   │        │
│        │                                            │        │
├────────┴────────────────────────────────────────────┴────────┤
│  Status / log panel                                          │
└──────────────────────────────────────────────────────────────┘
```

All three side panels (file browser, keyboard legend, log) can be **collapsed** by clicking the arrows at their edges, or with `Ctrl+B` for the file browser.

## What's in each row of the grid

Each line of a taal cycle has up to five notation rows, top to bottom:

1. **Taal markers** — Sam (X), Taali numbers, Khali (0)
2. **Ornaments** — meend curves, gamak waves, kan grace notes, etc.
3. **Swar** — note glyphs with octave dots and komal/tivra marks
4. **Strokes** — Da / Ra mizrab indicators
5. **Sahitya** — lyrics aligned to each beat

The stroke and sahitya rows are optional per composition and can be toggled in Properties.

## What to read next

- [Creating Compositions](02-creating-compositions.md) — the New dialog and composition types
- [Entering Notes](03-entering-notes.md) — keyboard input, octaves, subdivisions
- [Keyboard Reference](08-keyboard-reference.md) — every shortcut at a glance
