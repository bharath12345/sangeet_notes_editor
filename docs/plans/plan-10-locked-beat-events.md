# LockedBeat Events — Persistent Locked Beats for Starting Beat Feature

## Context

The per-section `startingBeat` feature already exists (data field, dialog inputs, cursor skip, rendering). But locked beats are currently **visual-only** — the grid renderer checks `beat < startingBeat - 1` and draws a dot, but no actual events exist in the composition data. This causes problems:

1. **Dots don't appear automatically** — creating a composition with startingBeat=9 shows an empty grid, not 8 locked beats
2. **Locked beats are deletable** — backspace/delete removes them since there's nothing preventing it
3. **No persistence** — `.swar` files don't contain locked beat data
4. **No shift on change** — changing startingBeat in Properties dialog should shift existing notes right/left

The fix: introduce `Event.LockedBeat` as a new Event variant that gets pre-filled on creation, persisted in `.swar` files, protected from deletion, and shifted when startingBeat changes.

## Phase 1: Event Model — `Event.LockedBeat`

### Scala: `sangeet-core/.../model/Event.scala`

Add new variant alongside Swar, Rest, Sustain, Chikari:

```scala
case LockedBeat(position: BeatPosition, eventDuration: Rational) extends Event
```

Same shape as `Rest` — just position + duration. Update the pattern match methods (`Event.position`, `Event.eventDuration`) if they exist as extension methods.

### Elm: `sangeet-web/src/Model/Event.elm`

Add `LockedBeatEvent BeatPosition Rational` to the `Event` union type. Update `eventPosition` and `eventDuration` helpers.

## Phase 2: Codecs

### Scala: `sangeet-core/.../format/CompositionCodecs.scala`

**Encoder:** Add case to Event encoder:
```scala
case Event.LockedBeat(pos, dur) =>
  Json.obj("type" -> "lockedbeat".asJson, "beat" -> pos.asJson, "duration" -> dur.asJson)
```

**Decoder:** Add case to Event decoder's type match:
```scala
case "lockedbeat" =>
  for
    beat     <- c.downField("beat").as[BeatPosition]
    duration <- c.downField("duration").as[Rational]
  yield Event.LockedBeat(beat, duration)
```

### Elm: `sangeet-web/src/Model/Event.elm`

**Decoder:** Add `"lockedbeat"` branch to `eventByType`:
```elm
"lockedbeat" -> lockedBeatDecoder
```

**Encoder:** Add `LockedBeatEvent` case to `encodeEvent`.

## Phase 3: Generation Helper

### Scala: `CompositionEditor.scala`

Add pure helper method:
```scala
def generateLockedBeats(matras: Int, startingBeat: Int): List[Event] =
  if startingBeat <= 1 then Nil
  else
    (0 until (startingBeat - 1)).map { beat =>
      Event.LockedBeat(BeatPosition(0, beat, Rational.onBeat), Rational.fullBeat)
    }.toList
```

All LockedBeat events go on **cycle 0** with on-beat subdivision. The `startingBeat` is 1-indexed, so startingBeat=9 produces 8 events at beats 0–7.

### Elm: `State/Update.elm` or helper module

Equivalent Elm function for client-side generation (used when web client creates compositions directly).

## Phase 4: Pre-fill on Creation

### Scala: `CompositionEditor.create()`

Change section construction from `Nil` events to `generateLockedBeats(...)`:

```scala
case CompositionType.Gat =>
  val base = List(
    Section("Gat", SectionType.Custom("Gat"),
      generateLockedBeats(matras, gatStartingBeat), startingBeat = gatStartingBeat),
    Section("Antara", SectionType.Antara,
      generateLockedBeats(matras, antaraStartingBeat), startingBeat = antaraStartingBeat)
  )
  val taans = (1 to taanCount).map { i =>
    Section(s"Taan $i", SectionType.Taan,
      generateLockedBeats(matras, taanStartingBeat), startingBeat = taanStartingBeat)
  }.toList
  base ++ taans
```

Same pattern for Bandish (Sthayi + Antara).

### Desktop: `NewCompositionDialog` result already passes starting beats → flows through `CompositionEditor.create()` → dots appear immediately.

### Elm: `handleNewDialogSubmit` already passes starting beats to server → server calls `CompositionEditor.create()` → response has LockedBeat events.

## Phase 5: Deletion Guards

### Scala: `CompositionEditor.removeEventAt()` (~line 34)

Add guard at the top:
```scala
def removeEventAt(section: Section, pos: BeatPosition): Section =
  val existing = section.events.find(e => Event.position(e) == pos)
  existing match
    case Some(_: Event.LockedBeat) => section  // refuse to delete
    case _ => // existing logic
```

### Scala: `CompositionEditor.removeGroupAt()` (~line 55)

Same guard — if the event at the target position is a LockedBeat, return the section unchanged.

### Scala: `EditorApi.deleteAtCursor()` (~line 146)

Add check: if cursor is on a LockedBeat position, return current state unchanged (no-op).

### Elm: `State/Update.elm` — `handleDeleteKey` / `handleBackspace`

Add guard: check if the event at cursor position is `LockedBeatEvent`. If so, skip deletion.

### Desktop: `EditorPane` key handlers

Same guard via the `EditorApi.deleteAtCursor()` change above.

## Phase 6: Starting Beat Change with Shift Logic

### Scala: `CompositionEditor.changeStartingBeat()`

New pure method — the core of the shift logic:

```scala
def changeStartingBeat(section: Section, newStartingBeat: Int, matras: Int): Section
```

**Increasing startingBeat** (e.g., 1→9 or 5→9):
1. Remove existing LockedBeat events
2. Separate non-locked events
3. Compute how many new locked beats needed: `newStartingBeat - 1`
4. Compute shift amount: `newLockedCount - oldLockedCount` beats forward
5. Generate new LockedBeat events for beats 0..(newStartingBeat-2)
6. Shift all non-locked events forward by shift amount using `shiftEventForward`
7. Events that overflow past `matras - 1` on the current cycle wrap to the next cycle (new cycles created as needed)

**Decreasing startingBeat** (e.g., 9→5):
1. Remove existing LockedBeat events  
2. Compute shift amount: `oldLockedCount - newLockedCount` beats backward
3. Generate new LockedBeat events for beats 0..(newStartingBeat-2)
4. Shift all non-locked events backward by shift amount using `shiftEventBack`

Uses existing helpers: `flatPosition()`, `unflatPosition()`, `setEventPosition()`.

### Scala: `EditorApi` — new method

```scala
def changeStartingBeat(sectionIndex: Int, newStartingBeat: Int): EditorState
```

Calls `CompositionEditor.changeStartingBeat()` for the given section, returns updated state.

### Scala: Server endpoint

New endpoint in `EditorEndpoints` + `EditorRoutes`:
```
POST /api/editor/change-starting-beat
Body: { "sectionIndex": Int, "startingBeat": Int }
```

### Desktop: `EditorPane.applySectionStartingBeats()`

Change from simple field copy to calling `CompositionEditor.changeStartingBeat()`:
```scala
def applySectionStartingBeats(beats: Map[Int, Int]): Unit =
  editor.foreach { ed =>
    var comp = ed.composition
    beats.foreach { (idx, newBeat) =>
      if idx < comp.sections.length then
        val updated = CompositionEditor.changeStartingBeat(
          comp.sections(idx), newBeat, comp.metadata.taal.matras)
        comp = comp.copy(sections = comp.sections.updated(idx, updated))
    }
    val newEd = ed.copy(composition = comp)
    pushEditor(newEd)
    cachedGrids = None
    redraw()
  }
```

### Elm: `State/Update.elm` — `PropsDialogSubmit`

Call the new server endpoint for each section whose startingBeat changed. Server returns the updated composition with shifted events.

## Phase 7: Pattern Match Updates

Several existing methods use exhaustive pattern matching on `Event` variants. Each needs a `LockedBeat` case:

- `CompositionEditor.shiftEventBack` — handle like Rest
- `CompositionEditor.setEventPosition` — handle like Rest  
- `CompositionEditor.shiftEventForward` — handle like Rest
- Layout engine: `BeatGrouper`, `LineBreaker`, `GridLayout` — treat LockedBeat as occupying the cell (like Rest)
- Renderers: `SwarGlyphRenderer`, `GridRendererFX` (desktop), `SwarGlyph.elm`, `GridRenderer.elm` (web) — render as "●"
- `CanvasRendererFX` — LockedBeat cells show filled dot, no stroke/sahitya rows

## Phase 8: Backward Compatibility

Old `.swar` files have `startingBeat` on sections but no LockedBeat events in the event list. Two approaches:

**Option A — Migration on load:** When loading a file, if a section has `startingBeat > 1` but no LockedBeat events, inject them. Do this in `SwarFormat.fromJson()` post-processing.

**Option B — Dual rendering:** Keep the visual-only check as fallback. If no LockedBeat events exist but `startingBeat > 1`, render dots visually.

**Recommendation: Option A** — migrate on load. This is cleaner and means the rest of the code only needs to handle one path. The migration is idempotent — running it on an already-migrated file is a no-op.

```scala
// In SwarFormat.fromJson, after decoding:
def migrateLockedBeats(comp: Composition): Composition =
  val matras = comp.metadata.taal.matras
  comp.copy(sections = comp.sections.map { section =>
    if section.startingBeat > 1 && !section.events.exists(_.isInstanceOf[Event.LockedBeat]) then
      section.copy(events = CompositionEditor.generateLockedBeats(matras, section.startingBeat) ++ section.events)
    else section
  })
```

Same migration in Elm's composition decoder or in the API response handler.

## Phase 9: Tests

### Scala unit tests (`sangeet-core`)

1. **Codec roundtrip** — LockedBeat event encodes/decodes correctly
2. **generateLockedBeats** — startingBeat=1 → empty, startingBeat=9 → 8 events at beats 0-7
3. **Deletion guard** — `removeEventAt` on a LockedBeat returns section unchanged
4. **changeStartingBeat increase** — shifting 1→9 inserts 8 locked beats, shifts existing events forward
5. **changeStartingBeat decrease** — shifting 9→5 removes 4 locked beats, shifts events backward
6. **changeStartingBeat overflow** — events pushed past taal boundary wrap to next cycle
7. **Migration** — section with startingBeat=5 but no LockedBeat events gets them injected

### Elm tests (`sangeet-web/tests`)

1. **Event codec** — LockedBeatEvent roundtrip
2. **Deletion guard** — backspace on LockedBeat is no-op
3. **Grid rendering** — LockedBeat renders as dot glyph

### Existing test updates

- `CompositionCodecSpec.scala` sample composition may need LockedBeat events if its sections have non-1 startingBeat
- Pattern match exhaustiveness will cause compile errors in any missed spots — the compiler enforces coverage

## Verification

1. `sbt sangeet-core/compile` — new Event variant compiles, all pattern matches covered
2. `sbt sangeet-core/test` — existing + new tests pass
3. `sbt sangeet-server/compile` — new endpoint compiles
4. `sbt sangeet-server/test` — integration tests pass
5. `cd sangeet-web && npx elm make src/Main.elm` — compiles with new Event variant
6. `cd sangeet-web && npx elm-test` — tests pass
7. **Manual: New dialog** — create Gat with startingBeat=9 → 8 dots appear immediately, not deletable
8. **Manual: Properties dialog** — change startingBeat 1→9 → existing notes shift right, dots appear
9. **Manual: Properties dialog** — change startingBeat 9→5 → 4 dots removed, notes shift left
10. **Manual: Save/reload** — `.swar` file contains LockedBeat events, reloads correctly
11. **Manual: Old file** — open pre-LockedBeat `.swar` file → migrated on load, dots appear

## Implementation Order

1. **Event.LockedBeat model** — Event.scala, Event.elm (compile will show all pattern match gaps)
2. **Codecs** — CompositionCodecs.scala, Event.elm encoder/decoder
3. **Pattern match fixes** — fix all compile errors from new variant across core/desktop/web
4. **generateLockedBeats helper** — CompositionEditor.scala
5. **Pre-fill on creation** — CompositionEditor.create() uses generateLockedBeats
6. **Deletion guards** — removeEventAt, removeGroupAt, deleteAtCursor, Elm key handlers
7. **changeStartingBeat** — CompositionEditor.changeStartingBeat() + EditorApi + server endpoint
8. **Desktop Properties integration** — EditorPane.applySectionStartingBeats calls changeStartingBeat
9. **Elm Properties integration** — Update.elm PropsDialogSubmit calls new endpoint
10. **Backward compatibility** — migration on load in SwarFormat.fromJson + Elm decoder
11. **Tests** — codec, generation, deletion guard, shift logic, migration
12. **Rendering** — LockedBeat cases in SwarGlyph/GridRenderer (desktop + web)
