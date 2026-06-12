---
name: hindustani-music-theory
description: Use when working on raag/taal/ornament/swar code, when designing new model types for Hindustani music concepts, or when reviewing notation rendering. Provides the domain knowledge needed to make correct decisions about scales, rhythmic cycles, ornaments, and sitar-specific notation. Activate when you see files matching sangeet-core/.../model/, sangeet-core/.../raag/, sangeet-core/.../taal/, or any file mentioning Swar/Octave/Variant/Ornament/Taal/Raag/Sahitya.
---

# Hindustani Classical Music Theory

This skill captures the domain knowledge needed to work on the notation editor without making wrong assumptions about Indian classical music. Most of these rules are not enforced by tests — they're enforced by the user noticing during review.

## Swar (Notes)

Seven base notes form the saptak:

| Position | Western | Hindustani (Bhatkhande) | Devanagari |
| -------- | ------- | ----------------------- | ---------- |
| 1        | C       | Sa                      | स          |
| 2        | D       | Re                      | रे         |
| 3        | E       | Ga                      | ग          |
| 4        | F       | Ma                      | म          |
| 5        | G       | Pa                      | प          |
| 6        | A       | Dha                     | ध          |
| 7        | B       | Ni                      | नि         |

### Variants — non-negotiable rules

- **Sa and Pa are fixed (achal).** They have NO komal or tivra variants. Any code path that lets the user toggle komal on Sa or Pa is a bug.
- **Re, Ga, Dha, Ni can be komal (flat).** Visual: underline beneath the note.
- **Ma can be tivra (sharp).** Visual: small vertical stroke above the note.
- This gives 12 chromatic notes per octave — same as Western, organised differently.

### Keyboard input convention

This codebase uses Roman → Devanagari rendering:

- Lowercase = shuddha (natural): `s r g m p d n` → Sa Re Ga Ma Pa Dha Ni
- Shift = variant: `Shift+R` → komal Re, `Shift+M` → tivra Ma, `Shift+G` → komal Ga, etc.
- Shift on `s` or `p` is a no-op (achal rule).

## Octaves (Saptak)

Three primary octaves; data model supports 5 for edge cases:

| Octave | Name       | Notation  | Range            |
| ------ | ---------- | --------- | ---------------- |
| -2     | Ati-Mandra | ँँ below  | Extended (rare)  |
| -1     | Mandra     | dot below | Lower octave     |
| 0      | Madhya     | no mark   | Middle (default) |
| +1     | Taar       | dot above | Upper octave     |
| +2     | Ati-Taar   | ँँ above  | Extended (rare)  |

UI defaults to madhya; toggles cycle through the 3-octave range unless the user explicitly extends.

## Taal (Rhythmic Cycle)

A taal is a repeating cycle of **matras** (beats) divided into **vibhags** (sections). Each vibhag has a marker on its first beat:

- **Sam (X)** — first beat of the cycle. Visual: `X`.
- **Taali** (clap, numbered 2, 3, ...) — emphasised beats. Visual: digit above beat.
- **Khali** (0, wave) — un-emphasised beat. Visual: `0` above beat.

### Standard taals shipped with the app

| Taal     | Matras | Vibhag pattern (matras per vibhag) | Markers            |
| -------- | ------ | ---------------------------------- | ------------------ |
| Teentaal | 16     | 4-4-4-4                            | X-2-0-3            |
| Ektaal   | 12     | 2-2-2-2-2-2                        | X-0-2-0-3-4        |
| Jhaptaal | 10     | 2-3-2-3                            | X-2-0-3            |
| Rupak    | 7      | 3-2-2                              | 0-2-3 (sam==khali) |
| Dadra    | 6      | 3-3                                | X-0                |
| Keherwa  | 8      | 4-4                                | X-0                |

### Rupak edge case — read carefully

Rupak's sam coincides with khali (both are beat 1). Most code paths assume "sam = strong beat, khali = weak beat" — Rupak violates that. When rendering taal markers, render both `0` and the implied X on beat 1; when generating audio, treat beat 1 as khali. The rendering layer in `sangeet-core/.../layout/` already handles this; if you add a new rhythmic feature, verify it against Rupak before claiming it works.

### Custom taals

Custom taals are first-class. They are stored as data (JSON in `sangeet-core/src/main/resources/taals/`) — never hardcoded as match cases. Any switch on taal name is a code smell; the taal definition should expose the property being switched on.

## Beat subdivision

- A single beat can hold 1 to 8 notes.
- Sub-positions: on the beat, halfway (1/2), third (1/3, 2/3), quarter (1/4, 3/4), eighth.
- The data model uses **Rational (numerator/denominator)** for precise positioning — `Rational(3, 4)` means "3/4 of the way through this beat". Never use `Double` for beat positions.
- **Dual swaras** (SaSa, ReRe, GaGa) are extremely common — typed via double-tap shortcut: `ss`, `rr`, `gg`, etc.
- **Fast-typing grouping**: when the user types 2-4 notes within 500ms, they go on one beat with equal subdivisions (1/2, 1/3, 1/4). The 500ms window is tuned to sitar pedagogy; don't change it without asking.

## Laya (Tempo)

| Laya         | BPM range | Note density   |
| ------------ | --------- | -------------- |
| Ati-vilambit | 20-30     | 6-8 notes/beat |
| Vilambit     | 30-60     | 4-6 notes/beat |
| Madhya       | 60-120    | 2-4 notes/beat |
| Drut         | 120-250   | 1-2 notes/beat |
| Ati-drut     | 250+      | 1 note/beat    |

**BPM = matras per minute.** Vilambit compositions have high note density (lots happening per beat); drut compositions are sparse (one note per beat, the speed is in the cycling).

**Paltas have NO laya** — they're practice exercises played at varying speeds. Code that requires a laya for every composition must special-case Palta (or, better, make laya `Option[Laya]`).

## Composition structure

| Type        | Description                                                                              |
| ----------- | ---------------------------------------------------------------------------------------- |
| **Bandish** | Vocal composition — sthayi (refrain), antara (second part), sometimes sanchari and abhog |
| **Gat**     | Instrumental (sitar) — masitkhani (vilambit gat) or razakhani (drut gat)                 |
| **Palta**   | Practice exercise/pattern. Has taal but no laya. Can be authored by student or guruji.   |

### Sections

- Sthayi, Antara, Sanchari, Abhog
- Taan (numbered: Taan 1, Taan 2, …)
- Toda (numbered)
- Jhala, Palta, Arohi, Avarohi, Custom

### Mukhda

Opening phrase that typically starts **before sam** and resolves on sam. The editor's per-section `startingBeat` field handles this — when a section starts on beat 14 of a 16-beat cycle, beats 14, 15, 16 are the mukhda, and beat 1 of cycle 2 is where the action lands.

Beats before the startingBeat on cycle 0 are rendered as `Event.LockedBeat` (deletion-guarded). Any new editing operation must respect locked beats.

### Tihai

Rhythmic phrase repeated **3 times**, landing on sam. Visual: bracket with `x3` marker. Tihai belongs inside `Section`, not `Composition` — a single composition can have multiple tihais in different sections, but each section has at most one. The data model already enforces this: `Section.tihai : Option[Tihai]`.

## Sitar-specific notation

### Mizrab strokes

The plectrum stroke direction. **Every note on the main string has a stroke** — if a notation lacks strokes, it's incomplete.

- **Da** — inward / down stroke
- **Ra** — outward / up stroke

Common patterns: Da-Ra-Da-Ra alternation; Da-Da-Ra triplets; Da-Diri (where Diri = Da-Ra played as a 32nd-note grace pair).

### Strings

- **Main string** — melody
- **Jod string** — drone (octave below sa)
- **Chikari strings** — high drone (taar sa and pa) — strummed in jhala patterns

### Techniques

- **Krintan** — left-hand pull-off (the hammer lifts off without re-plucking)
- **Gitkari** — hammer-on / pull-off trill
- **Ghaseet** — heavy lateral string pull (a long, slow meend)
- **Jhala** — rapid alternation between melody on main string and open chikari strings

## Ornamentations

The model has a closed set of named ornaments plus an open `CustomOrnament` for extensibility. Guruji may teach new techniques — the system must accommodate them without code changes.

| Ornament           | What it is                                                                                                                                                                                                                      |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Meend**          | Glide between notes. Has direction (Ascending = pulling string up in pitch, Descending = releasing). Start note, end note, optional intermediate notes. Does NOT store fret position — that's physical technique, not notation. |
| **Kan Swar**       | Grace note immediately before the main note                                                                                                                                                                                     |
| **Murki**          | Rapid ornamental turn, 3-5 notes                                                                                                                                                                                                |
| **Gamak**          | Heavy oscillation around a note                                                                                                                                                                                                 |
| **Andolan**        | Slow, gentle oscillation                                                                                                                                                                                                        |
| **Krintan**        | Sitar pull-off sequence                                                                                                                                                                                                         |
| **Gitkari**        | Sitar hammer/pull trill                                                                                                                                                                                                         |
| **Ghaseet**        | Sitar heavy lateral pull                                                                                                                                                                                                        |
| **Sparsh**         | Light touch of adjacent note                                                                                                                                                                                                    |
| **Zamzama**        | Rapid repeated note cluster                                                                                                                                                                                                     |
| **CustomOrnament** | Free-form. Has a name + `Map[String, String]` parameters.                                                                                                                                                                       |

### Meend direction — read carefully

For a sitar player, "ascending" means pulling the string sideways to raise the pitch — physically harder, sounds more strained. "Descending" means releasing the pull — physically easier, sounds smoother. Confusing the two in rendering / UI labels is a real bug, not cosmetic.

## Sahitya (Lyrics)

- Aligned per beat: each syllable sits under one note.
- Multi-script support is **mandatory**. Guruji teaches compositions in Hindi, Kannada, Telugu, Sanskrit, Braj Bhasha — sometimes mixed within one composition.
- Sahitya is per-event, not per-beat. A single beat with 4 notes can have 4 syllables, one syllable spanning all 4, or any combination.
- The renderer uses the same script-aware glyph layer as the swar row — see `sangeet-core/.../render/ScriptMap.scala`.

## Rendering — Bhatkhande style

The 5-row layout per cycle line, top-to-bottom:

1. **Taal markers** — Sam (X), Taali (digits), Khali (0). Color: dark red.
2. **Ornaments** — meend brackets, kan notation, gamak squiggles. Color: deep purple.
3. **Swar** — the notes themselves with octave dots and komal/tivra marks. Color: dark indigo (dots in orange).
4. **Da/Ra strokes** — mizrab stroke indicators. Color: teal.
5. **Sahitya** — lyrics. Color: dark green.

Cell width is **dynamic** — cells scale to fill the available canvas width for any taal. Don't hardcode pixel widths.

**Density-aware line breaking**: drut compositions can fit a full cycle on one line; vilambit compositions split by vibhag. User can override per section.

## Cross-cutting rules to remember

- **Model is pure.** `sangeet-core.model` has zero UI / IO dependencies. New types added here must not import circe, ScalaFX, http4s, anything beyond the standard library.
- **Layout is data, not pixels.** The layout engine produces `RenderedGrid` (positions, beats, line breaks) — renderers consume it. ScalaFX rendering and HTML export both consume the same layout.
- **Taals are data, ornaments are extensible.** Don't add hardcoded match cases on taal name or ornament name in a place that should be data-driven.
- **`.swar` file format is versioned.** Files include `"version": "1.0"`. New required fields must come with a backward-compatibility decoder (`getOrElse(...)`) or a version bump.

## When to consult the user

Domain decisions where you should ask Bharadwaj before guessing:

- Whether a new ornament is a variant of an existing one or genuinely new
- Whether a new taal's vibhag pattern is canonical or a regional variation
- Whether a feature should apply to Palta (no laya) the same way it applies to Bandish/Gat
- Whether a Carnatic-specific concept should be modelled (the system is Hindustani-first but Carnatic-aware)
- Anything involving sahitya transliteration across scripts

He has a physical notebook full of compositions and an active guruji — when in doubt, the right answer is "ask him, don't synthesise from web sources".
