# Plan 15 — Playback Sandbox (standalone CLI, no editor integration)

> Status: Plan / understanding only. **No code to be written from this plan yet.** Captured 2026-06-13 from a brainstorming session about MIDI and audio playback.

## What this plan is

A standalone command-line tool that reads a `.swar` file plus a few user-provided inputs and produces audible Hindustani melodies. The point is **iteration on audio quality** in isolation — get the sound right without committing the editor UIs to any particular shape.

This is a developer playground / personal-use tool, not a feature shipped to end users.

## What this plan is NOT

- Not a feature added to the desktop or web editors. No new toolbar buttons, no new menu items, no per-section play buttons.
- Not a `.swar` schema change. The file format stays exactly as it is.
- Not MIDI in any form (export or import).
- Not a commitment to in-editor playback later. That decision is deferred until we know what "good enough" sounds like.

## Why a standalone sandbox first

The brainstorming explored two related questions a fellow student raised:

1. "Can notes from the editor be saved as MIDI?"
2. "Can MIDI files be converted to notes?"

The conclusion: the real underlying need is **the student listening to their notated composition** — MIDI is a vocabulary that got reached for, but it doesn't actually fit. Direct audio synthesis is the right tool. Specifically:

- MIDI defaults to 12-TET pitch grid; Hindustani needs microtones (shrutis). Escaping the grid requires sysex tuning standards or per-note pitch bend, and the receiving DAW has to honor it — most don't.
- The default General-MIDI Sitar (program 104) sounds wrong to Indian ears.
- The student doesn't need a `.mid` artifact; they need to hear the notes.

But before adding a Play button to the editor, the user wants to **prove the audio quality is acceptable** when synthesized from a `.swar`. A CLI script is the cheapest way to iterate: run, listen, tweak, repeat. No UI to redesign with every iteration. No commitment to a place for Play to live in the editor. No `.swar` schema rev. Once the audio is "good enough", we can revisit whether/how to integrate.

Also: a CLI that writes audio to a `.wav` file is a more useful "share with non-Sangeet friends" path than MIDI export. The receiver doesn't need a sitar VST — they just play the file. This may obviate MIDI export entirely.

## Design decisions locked in during brainstorming

These choices apply to the audio engine the script will use. They came out of the brainstorming questions and are the starting point — all can be revised as iteration reveals what works.

| Dimension | Choice | Rationale |
|---|---|---|
| **Pitch system** | Just intonation (5-limit ratios for shuddha; fixed komal/tivra ratios) | Microtone-correct. 22-shruti is more precise but adds complexity for marginal listening difference at MVP. Revisit if needed. |
| **Sound source (melody)** | Karplus-Strong plucked-string synthesis (algorithmic, ~50 LOC, no samples) | Sounds vaguely sitar-like without a 10–50 MB SoundFont. Iteration friction is low; replace with sample-based later if quality demands. |
| **Sa frequency** | User-provided per invocation. Preset shortcuts (Male vocal, Female vocal, Sitar male, Sitar female, Harmonium) + raw Hz override | Beginners can pick a preset; advanced users can specify Hz. Not stored in `.swar` (no schema change). |
| **Tempo** | Auto-derived from composition's laya (Vilambit ≈ 30 BPM, Madhya ≈ 80, Drut ≈ 160), overridable via CLI flag | Uses information already in the `.swar`. Override flag for practice-speed iteration. |
| **Drone** | Tanpura — one looped sample file (~200 KB), plays Sa-Pa-Sa under everything | Drone is what makes the melody "feel Hindustani" even without ornaments. Cheap to add (one sample, one looping AudioInputStream). |
| **Ornaments** | **Not rendered as audio in MVP**. The `.swar` carries meend, gamak, kan, krintan, etc., but the synthesizer plays the bare swar without articulation. | Ornament audio rendering is a research project of its own. Ship bare-swar playback first; layer in ornaments once that foundation is real. |
| **Tabla theka** | Not in MVP. | Independent feature with its own data and samples. Add later. |
| **Output** | Live playback (default) OR write to `.wav` file via `--out path` | Live is what you want when iterating. WAV is what you want when sharing or when the iteration cycle moves to "compare versions side by side". |

## CLI shape (sketch — not a commitment)

```
play-swar <file.swar> --sa <preset|hz> [--section <name>] [--tempo <bpm>] [--out <path>]

Examples:
  play-swar yaman.swar --sa sitar-male
  play-swar yaman.swar --sa 277 --section "Antara"
  play-swar yaman.swar --sa 240 --tempo 120 --out /tmp/yaman.wav
  play-swar bhairavi-bandish.swar --sa female-vocal --section "Sthayi" --out /tmp/sthayi.wav
```

**Flag semantics:**

- `--sa` — accepts either a preset name (`male-vocal`, `female-vocal`, `sitar-male`, `sitar-female`, `harmonium`) or a raw frequency in Hz. Required.
- `--section <name>` — play only the named section, looping until interrupted. Default: play the whole composition top-to-bottom once.
- `--tempo <bpm>` — override the laya-derived tempo. Useful for slowing things down to listen carefully.
- `--out <path>` — write to a WAV file instead of playing through speakers. When set, the script exits when rendering is done; no looping (one pass).
- Stop live playback with Ctrl+C.

## Architectural sketch

**Where the script lives.** Two reasonable options; pick during implementation:

1. **`scripts/play-swar.scala`** as a `scala-cli` script. Pulls `sangeet-core` as a dependency (for `SwarFormat` and the domain model). Zero new sbt modules. Cheapest to set up; runs ad-hoc.
2. **`sangeet-cli/`** as a new sbt sub-project. Same dependency on `sangeet-core`. Heavier scaffolding, but if more CLI tools accumulate over time, this becomes the natural home.

Default to (1) for the playground phase. Graduate to (2) only if a second CLI tool shows up.

**Language.** Scala. The `.swar` parser already exists in `sangeet-core` (`SwarFormat`); reusing it saves writing a parser elsewhere. The audio synthesis itself is straightforward in pure Scala — Karplus-Strong is a single-purpose ring buffer with averaging, `javax.sound.sampled` ships in the JVM for both live playback and WAV writing. Python would have a faster audio-lib ecosystem (numpy, scipy, sounddevice), but the JVM tooling is enough for MVP and the language match avoids a second parser.

**Module layout.** Pure-logic pieces in `sangeet-core/audio/` so they're reusable later if the editor ever adopts playback. Side-effecting pieces (speaker output, file writing) stay in the script.

```
sangeet-core/src/main/scala/com/varpas/sangeet/core/audio/
  ShrutiTable.scala       — Sa freq + raag → Hz per swar (just intonation)
  PlaybackScheduler.scala — composition + tempo → sequence of (Hz, startTime, duration)
  KarplusStrong.scala     — algorithmic plucked-string synthesizer (generates Array[Double] samples)
  Drone.scala             — tanpura drone generator (loops the sample at the right pitch)
  Mixer.scala             — sum multiple synthesized voices into one output buffer

scripts/play-swar.scala   — CLI: parse args, call SwarFormat.read, drive the engine, output
sangeet-core/src/main/resources/audio/
  tanpura-sa.wav          — one tanpura sample (Sa drone, ~5 sec, loopable)
```

The `core/audio/` pieces have zero JVM-only or ScalaFX dependencies — same constraint as `core/model/`. They produce sample buffers (`Array[Double]` or `Array[Short]`); the script wraps those in `javax.sound.sampled` calls.

## Phased iteration roadmap

Each phase is a listening test. The user is the oracle for "is this good enough yet?". Move to the next phase when the current one sounds acceptable.

### Phase A — Pitch + timing (sine waves)

**Goal:** Verify that the .swar → frequency mapping is correct and that beats land where they should.

**What works:**
- `ShrutiTable` computes Hz for each swar given Sa freq + raag's komal/shuddha/tivra rules
- `PlaybackScheduler` turns the composition's events into a timeline `(Hz, startTimeMs, durationMs)` honoring subdivisions and section structure
- Each event renders as a pure sine wave at the correct Hz
- Live playback through speakers via `SourceDataLine`

**Listening test:** Play a known composition (e.g., `02-simple-gat-yaman-teentaal.swar`). Can you hear "Sa Re Ga Ma Pa Dha Ni Sa'" rising? Are the durations right? Does Antara start where expected?

**Done when:** You can hum along correctly.

### Phase B — Sitar-ish timbre (Karplus-Strong)

**Goal:** Replace the boring sine wave with something closer to a plucked string.

**What works:**
- `KarplusStrong` synthesizer: initialize a ring buffer of length `sampleRate/freq` with white noise, then iterate `buffer[i] = 0.5 * (buffer[i-1] + buffer[i])` (with damping factor) to produce a plucked tone that decays naturally
- Each scheduled event triggers a fresh KS pluck instead of a sine wave
- Tweakable parameters: damping factor (how fast the pluck dies), initial noise level (attack character)

**Listening test:** Does it sound like a plucked instrument, or still mechanical?

**Done when:** "Sounds like a sitar to a casual ear" — full disclosure, it won't sound exactly like a real sitar without samples, but Karplus-Strong gets surprisingly far.

### Phase C — Tanpura drone

**Goal:** Add the harmonic context that makes Hindustani melodies "feel right".

**What works:**
- Bundle a 5–10 second `tanpura-sa.wav` sample in resources
- `Drone` plays the sample on loop, pitch-shifted so its Sa matches the user-set Sa frequency
- `Mixer` sums drone + melody into one output buffer
- Drone volume is configurable (CLI flag or constant; default low so melody is dominant)

**Pitch-shifting:** Resample the WAV at a different playback rate. Simple, slight pitch artifacts, fine for the drone purpose.

**Listening test:** Does the melody feel anchored? Do you naturally hear the Sa as the tonic?

**Done when:** Removing the drone makes the melody feel rootless.

### Phase D — WAV output

**Goal:** Make the script useful for comparing iterations and for sharing.

**What works:**
- `--out path/to/file.wav` flag
- Same scheduling + synthesis, but accumulate samples into a buffer instead of streaming to speakers
- Write the buffer as a 16-bit PCM WAV via `AudioSystem.write(...)`
- Exits when rendering is done

**Done when:** You can render a composition to WAV, play it back in any audio player, and it sounds the same as live playback.

### Phase E — First ornament: meend (linear pitch glide)

**Goal:** Start rendering articulation. Meend is the simplest — a linear pitch glide from one swar to another.

**What works:**
- When the scheduler encounters a Meend ornament with start swar A and end swar B over duration D
- Instead of two discrete plucks, render one continuous tone whose frequency varies linearly from `freq(A)` to `freq(B)` over D
- Karplus-Strong with a varying buffer length, OR a sine/oscillator with frequency modulation — either works

**Listening test:** Does the slide feel natural? Compare to a recording of a real sitar meend.

**Done when:** Meend doesn't sound like two distinct notes.

### Phase F — More ornaments: gamak, kan

**Goal:** Add the remaining commonly-used ornaments.

**What works:**
- **Gamak**: rapid pitch oscillation around a center swar. Render as a sine LFO modulating the carrier frequency (depth = a half-step or so, rate = a few Hz).
- **Kan swar**: a quick grace note immediately before the main note. Schedule the grace note as a very short, lower-velocity pluck preceding the main pluck.

**Listening test:** Compare to known recordings of compositions that use these ornaments heavily.

**Done when:** Removing the ornaments makes the music feel notably less expressive.

### Phase G (optional) — Tabla theka under the melody

**Goal:** Add rhythmic accompaniment. This is its own substantial chunk and probably has its own future plan; mentioning here for completeness.

Out of scope for this plan as written. Defer.

## What success looks like

- I can run `play-swar yaman-bandish.swar --sa sitar-male` and hear a passable rendition of a Hindustani composition.
- The audio is microtone-correct (not 12-TET).
- The tanpura drone makes it feel grounded.
- I can render to WAV and share with a friend who doesn't have Sangeet — they hear something that sounds like Hindustani music, not a Western piano line.
- Quality is good enough that the question "should we put Play in the editor?" becomes worth answering.

## What I will not know until I listen

- Whether Karplus-Strong is enough or if we need samples. Plausible failure mode: it sounds too thin / too synthetic; community feedback says "I want something more like a real sitar". Mitigation: Phase B's done-bar is subjective; if "vaguely plucked" isn't enough, the next move is a small pre-recorded swar bank (one sample per shuddha swar per octave) before going to a full SoundFont.
- Whether just intonation is enough or if we need 22-shruti. Plausible failure mode: certain raags (Bhairavi, Marwa) have shrutis that just-intonation gets noticeably wrong, and trained ears immediately hear it. Mitigation: ShrutiTable's structure is per-raag — adding a 22-shruti override per raag is additive, doesn't require rewriting the engine.
- Whether bare-swar playback (no ornaments) is interesting enough to bother with. Plausible failure mode: it sounds mechanical even with drone + Karplus-Strong, and the only reason to bother is the ornaments — meaning Phase E becomes blocking, not optional. We won't know until we hear Phase C.

## Decision points the iteration will surface

| Trigger | Decision to make |
|---|---|
| "Karplus-Strong doesn't sound enough like a sitar" after Phase B | Switch to a small pre-recorded swar bank, OR adopt a full SoundFont, OR accept the synthetic sound. |
| "Just intonation sounds off for raag X" after Phase A or C | Add a 22-shruti table for that raag, OR accept the approximation. |
| "Bare swar playback is boring without ornaments" after Phase C | Either prioritize Phase E (ornaments) immediately, or pause iteration until we have a sample-based engine where ornaments naturally fall out. |
| "This sounds good enough — students will use it" after any phase | Revisit the question: should this become a feature in the editor? Where would Play live? Does the `.swar` need a `preferredSa` field? |
| "This is great — I want to share it widely" | Consider promoting `--out wav` into a button in the editor that exports the rendered audio, as a richer alternative to MIDI. |

## What this plan deliberately leaves out

The brainstorming session also discussed several things that are NOT part of this plan:

- **In-editor playback UI** — toolbar Play button, section ▶ buttons, Esc-to-stop, Play-toggles-pause, cursor-follows-playback. All deferred until the standalone script proves the audio is worth integrating.
- **`.swar` schema change** for `preferredSa` — deferred for the same reason. The script accepts Sa via CLI flag; no file format change needed.
- **MIDI export** — Phase D (WAV output) almost certainly removes the need, because WAV is more shareable and preserves the Hindustani character. Revisit only if a real user need surfaces.
- **MIDI import** — explicitly out of scope. Arbitrary MIDI files don't carry the raag/taal/komal/tivra/ornament information needed to round-trip into Sangeet's domain. Importing MIDI that Sangeet itself exported (with embedded metadata) might be doable, but only matters if MIDI export becomes a thing.
- **Audio export from the editor** — once the script can produce WAVs and quality is good, a "Render to WAV" button in the editor is a single-step add. Capturing as future possibility, not committing.
- **Tabla theka** — independent feature with its own data, sample bank, and design questions. Future plan.

## Cross-platform considerations

The script is JVM-only (desktop side). It does not run in the browser. If we ever decide the script's audio engine should ship inside the web editor too, the synthesis code would need a parallel Web Audio API implementation in JS. The pure-logic pieces (`ShrutiTable`, `PlaybackScheduler`) compile to Scala.js or could be ported by hand to Elm; the synthesis (Karplus-Strong loop, drone playback) has to be re-implemented per platform. This is the same constraint the cross-platform parity harness (Plan-14) operates under and is well-trodden ground.

For the standalone sandbox phase, none of this matters — it's a developer tool, runs on JVM, done.

## Effort estimate (rough)

| Phase | Effort | Cumulative |
|---|---|---|
| A (sine + timing) | 1–2 days | 1–2 days |
| B (Karplus-Strong) | 1–2 days | 2–4 days |
| C (drone) | 1 day | 3–5 days |
| D (WAV output) | 0.5 day | 3.5–5.5 days |
| E (meend) | 2–3 days | 5.5–8.5 days |
| F (gamak + kan) | 3–5 days | 8.5–13.5 days |

"Done enough to evaluate" is realistically Phase C (3–5 days of part-time work). Everything after that is "is this nice enough that we want it in the editor?".

## Open questions for a future session

These don't block writing the plan, but will need answers before any code is written:

- **Script home**: `scripts/play-swar.scala` (scala-cli) vs `sangeet-cli/` (new sbt module)? I lean toward scala-cli for the playground phase.
- **Tanpura sample source**: record one ourselves (~5 seconds, single take), find a CC-licensed one online, or generate algorithmically? Recording is cheapest if a clean Sa from a real tanpura is available.
- **Default volumes / mix balance**: drone vs melody. Will need tweaking by ear during Phase C.
- **Sample rate / bit depth**: 44.1 kHz / 16-bit PCM is the obvious default; no reason to deviate unless a phase reveals a need.
- **CLI library**: scala-cli's built-in arg parsing, scopt, decline, or just `args(0)`-style? Probably scopt or decline for sane `--help`; not worth bikeshedding.

## Summary in one paragraph

Don't add anything to the editors. Write a standalone Scala CLI script that reads a `.swar` file, takes a Sa frequency + optional section/tempo/output flags, and produces audible Hindustani playback. Use Karplus-Strong synthesis for plucked-string-like timbre, just-intonation pitch tied to the user-set Sa, and a looped tanpura drone for harmonic context. Ship in phases (sine waves → Karplus-Strong → drone → WAV output → meend → gamak/kan), listening after each phase. Use what we learn to decide whether and how to ever integrate playback into the editors themselves; that decision is explicitly deferred.
