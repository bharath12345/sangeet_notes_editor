---
description: Scaffold a new raag in sangeet-core/src/main/scala/com/varpas/sangeet/core/raag/Raags.scala — prompts for canonical fields and registers it in the `all` map.
---

You are helping add a new raag definition to the Sangeet Notes Editor's built-in raag library.

## What this command does

Appends a new `val <name> = Raag(...)` block to `sangeet-core/src/main/scala/com/varpas/sangeet/core/raag/Raags.scala` and adds its identifier to the `all: Map[String, Raag]` list at the bottom of that file. The Elm side has no work — `sangeet-web` decodes raags from the server's `/api/v1/raags` endpoint and Tapir picks up the new entry automatically.

## Steps

1. **Load the music theory skill** (`hindustani-music-theory`) — it has the variant rules you'll need to validate the user's input.

2. **Ask the user for the raag's canonical fields** using a single `AskUserQuestion` call only if the user hasn't already given them in their `/add-raag` invocation. Otherwise extract them from the prompt arg. Required fields (use `Option` semantics — empty string from user → `None`):
   - `name` — capitalized raag name (e.g., "Yaman", "Bageshree")
   - `thaat` — parent scale, one of the 10 thaats (Bilawal, Kalyan, Khamaj, Bhairav, Bhairavi, Asavari, Kafi, Marwa, Purvi, Todi)
   - `arohana` — ascending scale as a `List[String]` (e.g., `List("Sa", "Re", "Ga", "Ma♯", "Pa", "Dha", "Ni", "Sa'")`). Use `♯` for tivra, `♭` for komal. End with `Sa'` (taar sa).
   - `avarohana` — descending scale, ending in `Sa` (madhya). Note: not always the reverse of arohana — some raags use different notes ascending vs descending.
   - `vadi` — most prominent note
   - `samvadi` — second most prominent (typically a fourth or fifth away from vadi)
   - `pakad` — characteristic phrase as a single string (e.g., "Ni Re Ga, Re Sa")
   - `prahar` — time of day, Int 1-8 (1 = early morning, 4 = noon, 8 = midnight). `None` if not strongly associated with a specific time.

3. **Validate against the skill's rules:**
   - Sa and Pa must appear without variants (no `Sa♭`, no `Pa♯`).
   - Re/Ga/Dha/Ni may have `♭` (komal) but not `♯`.
   - Ma may have `♯` (tivra) but not `♭`.
   - vadi and samvadi must each appear somewhere in arohana or avarohana.
   - If the raag omits a note (e.g., Bhupali skips Ma and Ni), arohana/avarohana lengths can be <8 — that's pentatonic, fine.
   - If validation fails, surface the issue to the user and ask for a correction before writing.

4. **Check for duplicates.** Read `Raags.scala` and grep for the lowercase identifier. If it already exists, stop and tell the user.

5. **Append the new `val` block** just before the `val all: Map[String, Raag] = List(` line. Match the existing formatting exactly — see `yaman`, `bhairav`, `durga` at the top of the file as templates. Use single-line `Some("...")` wrappers, two-space indentation matching neighbors.

6. **Add the identifier to the `all` list** in alphabetical-ish order (or trailing, matching how recent additions like `madmadSarang` were appended). Place it on its own line, preserving the trailing-comma style of the surrounding entries.

7. **Run `sbt sangeetCore/compile`** to verify the file still compiles. If it doesn't, fix the error and retry.

8. **Run `sbt "sangeetCore/testOnly *RaagsSpec"`** if a spec exists for raags. The existing 26 raags pass — your new one should too. If there's no RaagsSpec, skip this step.

9. **Format with `sbt scalafmtOnly sangeet-core/src/main/scala/com/varpas/sangeet/core/raag/Raags.scala`** (the format-on-edit hook will also fire on Edit/Write, but running it explicitly here documents the intent).

10. **Report back to the user** with: the file path, the line numbers added, and a one-liner confirming the raag is reachable at `GET /api/v1/raags/<name>` and selectable in the New Composition dialog.

## What not to do

- Do not touch the Elm side. The web app pulls raags from the server.
- Do not edit `Raag.scala` (the case class). New fields require a coordinated change across model + Elm + tests; that's a separate task, not `/add-raag`.
- Do not hardcode the new raag's name anywhere outside `Raags.scala`. Lookup goes through `Raags.byName`.
- Do not invent values. If the user can't supply vadi/samvadi/pakad, leave them as `None` and tell them they can fill these in later.
