---
description: Scaffold a new taal in sangeet-core/src/main/scala/com/varpas/sangeet/core/taal/Taals.scala — prompts for matras + vibhag structure + theka and registers it in the `all` map.
---

You are helping add a new taal definition to the Sangeet Notes Editor's built-in taal library.

## What this command does

Appends a new `val <name> = Taal(...)` block to `sangeet-core/src/main/scala/com/varpas/sangeet/core/taal/Taals.scala` and adds its identifier to the `all: Map[String, Taal]` list. The Elm side has no work — `sangeet-web` decodes taals from `/api/v1/taals` and Tapir picks up the new entry automatically.

## Steps

1. **Load the music theory skill** (`hindustani-music-theory`) — it has the vibhag/marker rules and the Rupak edge case.

2. **Ask the user for the canonical fields** using a single `AskUserQuestion` call only if the user hasn't already given them in their `/add-taal` invocation. Otherwise extract from the prompt arg:
   - `name` — capitalized taal name (e.g., "Teentaal", "Rupak", "Dhamar")
   - `matras` — total beats in one cycle (Int)
   - `vibhags` — division pattern as a list of `(beats, marker)` pairs, where marker is one of `Sam`, `Taali(n)`, `Khali`. **The sum of vibhag beat counts must equal `matras`.**
   - `theka` — recitation syllables, one per matra (e.g., for Teentaal: "Dha Dhin Dhin Dha Dha Dhin Dhin Dha Dha Tin Tin Ta Ta Dhin Dhin Dha"). Use `"-"` for an empty matra. `None` if the user doesn't know it.

3. **Validate against the skill's rules:**
   - Sum of vibhag beats must equal `matras`.
   - The first vibhag's marker should typically be `Sam` — **unless** this is a Rupak-style taal where sam coincides with khali on beat 1; in that case the marker is `Khali` and the user should explicitly confirm that semantic.
   - Taali numbering should be sequential and skip nothing (Sam=1 implicitly, then Taali(2), Taali(3), ...). Don't enforce strictly — some taals have unusual numbering — but flag if Taali(5) appears without Taali(2)–(4).
   - If `theka` is provided, its length must equal `matras`.
   - If validation fails, surface the issue and ask for a correction.

4. **Check for duplicates.** Read `Taals.scala` and grep for the lowercase identifier. If it already exists, stop and tell the user.

5. **Append the new `val` block** just before `val all: Map[String, Taal] = List(`. Match the existing formatting exactly — `teentaal`, `ektaal`, `jhaptaal` at the top are good templates. Vibhag list is multiline, each entry on its own line. Theka list is multiline if `matras > 8`, single line otherwise — match the existing convention by looking at how `deepchandi` (14 matras) vs `dadra` (6 matras) format their theka.

6. **Add the identifier to the `all` list** in trailing position (most recent additions like `deepchandi` were appended at the end). Preserve the trailing-comma style.

7. **Run `sbt sangeetCore/compile`** to verify the file still compiles. If it doesn't, fix the error and retry.

8. **Run `sbt "sangeetCore/testOnly *TaalsSpec"`** if a spec exists. The existing 11 taals pass — your new one should too. If no spec exists, skip.

9. **Format with `sbt scalafmtOnly sangeet-core/src/main/scala/com/varpas/sangeet/core/taal/Taals.scala`** (the format-on-edit hook will also fire on Edit/Write, but running it explicitly documents the intent).

10. **Report back to the user** with: the file path, the line numbers added, a one-liner confirming the taal is reachable at `GET /api/v1/taals/<name>`, selectable in the New Composition dialog, and usable as the cycle for any composition.

## What not to do

- Do not touch the Elm side. The web app pulls taals from the server.
- Do not edit `Taal.scala` (the case class) or `VibhagMarker.scala`. New marker types require a coordinated change across model + renderer + Elm + tests.
- Do not hardcode the new taal's name anywhere outside `Taals.scala`. Lookup goes through `Taals.byName`.
- Do not invent theka syllables. If the user doesn't know the theka, leave it as `None` — the editor still works for compositions in that taal; only the bol display row is empty.
- Do not "fix" unusual vibhag patterns to match Teentaal. Some real taals have asymmetric vibhags (Rupak: 3-2-2) — that's the whole point of being a custom taal.
