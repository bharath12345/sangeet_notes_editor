# Starting Beat Feature — Per-Section Starting Beat for Gat/Bandish

## Context

In Hindustani classical music, compositions (Gat/Bandish) don't always start at sam (beat 1). A vilambit gat in Teentaal might start on the 12th matra. The beats before the starting beat are permanently locked — no notes can be placed there. They render as a distinct "big dot" symbol (different from the "-" rest dash).

Key domain rules (confirmed by user's guruji):
- **Gat (Sthayi) and Antara can start on different beats**
- **All Taans of a composition share one starting beat** (but it's not necessarily sam)
- Starting beat is **per-section**, stored on each `Section`
- Locked beats are **permanent** — cursor skips them entirely
- When taal changes in Properties dialog, starting beats that exceed the new taal's matra count show a validation error
- User must always explicitly pick the starting beat (no default)
- Applies to **Gat and Bandish** compositions only (not Palta/Sargam)

## Data Model Changes

### Scala: `sangeet-core/src/main/scala/com/varpas/sangeet/core/model/Section.scala`

Add `startingBeat` field (1-indexed, where 1 = sam):

```scala
case class Section(
    name: String,
    sectionType: SectionType,
    events: List[Event],
    tihai: Option[Tihai] = None,
    startingBeat: Int = 1          // NEW — 1-indexed, 1 = sam (no locked beats)
)
```

### Elm: `sangeet-web/src/Model/Composition.elm`

Add `startingBeat` to Section type alias:

```elm
type alias Section =
    { name : String
    , sectionType : SectionType
    , events : List Event
    , tihai : Maybe Tihai
    , startingBeat : Int           -- NEW — 1-indexed
    }
```

Update `sectionDecoder` (use `optionalFieldWithDefault "startingBeat" 1 Decode.int` for backward compat with old .swar files).
Update `encodeSection` to include `startingBeat`.

## Codec Changes

### Scala: `sangeet-core/src/main/scala/com/varpas/sangeet/core/format/CompositionCodecs.scala`

Section encoder (line ~149): add `"startingBeat" -> s.startingBeat.asJson`
Section decoder (line ~157): add `startingBeat <- c.downField("startingBeat").as[Option[Int]].map(_.getOrElse(1))`
Pass `startingBeat` to `Section(name, stype, events, tihai, startingBeat)`

## Creation Flow Changes

### Scala: `CompositionEditor.create()` in `sangeet-core/.../editor/CompositionEditor.scala`

Add three new parameters: `gatStartingBeat: Int = 1`, `antaraStartingBeat: Int = 1`, `taanStartingBeat: Int = 1`

In the `CompositionType.Gat` match arm (line ~339), pass starting beats to sections:
```scala
case CompositionType.Gat =>
  val base = List(
    Section("Gat", SectionType.Custom("Gat"), Nil, startingBeat = gatStartingBeat),
    Section("Antara", SectionType.Antara, Nil, startingBeat = antaraStartingBeat)
  )
  val taans = (1 to taanCount).map { i =>
    Section(s"Taan $i", SectionType.Taan, Nil, startingBeat = taanStartingBeat)
  }.toList
  base ++ taans
```

Similar for `Bandish` — Sthayi and Antara get their respective starting beats.

### Scala: `CompositionApi.createComposition()` in `sangeet-core/.../api/CompositionApi.scala`

Add `gatStartingBeat`, `antaraStartingBeat`, `taanStartingBeat` parameters, pass through to `CompositionEditor.create()`.

### Scala: `CompositionRoutes.create` in `sangeet-server/.../routes/CompositionRoutes.scala`

Parse three new fields from request JSON:
```scala
gatStartingBeat     <- parseFieldOr(c, "gatStartingBeat", 1)
antaraStartingBeat  <- parseFieldOr(c, "antaraStartingBeat", 1)
taanStartingBeat    <- parseFieldOr(c, "taanStartingBeat", 1)
```

## New Dialog Changes

### Desktop: `NewCompositionDialog.scala`

Add three `Spinner[Integer]` fields after the Taal row (row 9):
- "Gat Starting Beat:" — spinner 1..matras, mandatory for Gat/Bandish
- "Antara Starting Beat:" — spinner 1..matras
- "Taan Starting Beat:" — spinner 1..matras (visible only when taanCount > 0 for Gat)

Behavior:
- All three visible only for Gat and Bandish types (hide via `updateVisibility()`)
- When taal combo changes, update spinner max to new taal's matras; clamp current value if > new max
- Add to `Result` case class: `gatStartingBeat`, `antaraStartingBeat`, `taanStartingBeat`
- Shift subsequent grid rows down by 3 (thaat row 10 → 13, etc.)

### Elm: `State/Model.elm` — `NewDialogForm`

Add three fields:
```elm
type alias NewDialogForm =
    { ...existing fields...
    , gatStartingBeat : Int
    , antaraStartingBeat : Int
    , taanStartingBeat : Int
    }
```

No default value — but the form is initialized when ShowNewDialog is dispatched. Initialize all three to 1. The dialog enforces the user must pick.

### Elm: `State/Msg.elm`

Add messages:
```elm
| NewDialogSetGatStartingBeat String
| NewDialogSetAntaraStartingBeat String
| NewDialogSetTaanStartingBeat String
```

### Elm: `View/Dialogs/NewComposition.elm`

Add three number input fields after the Taal selector, visible only when compositionType is "gat" or "bandish". Each shows label like "Gat Starting Beat (1-16)" where 16 comes from the selected taal's matras. The taan field is only visible when compositionType is "gat" (since Bandish doesn't have taans in the current model).

### Elm: `State/Update.elm` — `handleNewDialogSubmit`

Pass `gatStartingBeat`, `antaraStartingBeat`, `taanStartingBeat` to `ApiComposition.createComposition`.

### Elm: `Api/Composition.elm` — `createComposition`

Add the three starting beat fields to params record and JSON encoder.

## Properties Dialog Changes

### Desktop: `CompositionPropertiesDialog.scala`

Currently shows: Title, Type (read-only), Raag (read-only), Taal.

Add starting beat spinners for each section that supports it. Since sections are known from the composition, show:
- For Gat: "Gat Starting Beat:", "Antara Starting Beat:", and if taans exist "Taan Starting Beat:" (one spinner, applies to all taans)
- For Bandish: "Sthayi Starting Beat:", "Antara Starting Beat:" (if present)

Change the dialog signature: `show(meta: Metadata, sections: List[Section], ...)` → returns a result that includes updated section starting beats.

When taal changes: validate that all starting beats are within range, show error if not.

The return type needs to carry both metadata changes AND per-section starting beat changes. Simplest: return `Option[(Metadata, Map[Int, Int])]` where the Map is `sectionIndex → newStartingBeat`.

### Elm: `State/Model.elm` — `PropsDialogForm`

Expand to include starting beats:
```elm
type alias PropsDialogForm =
    { title : String
    , taalName : String
    , sectionStartingBeats : List { sectionIndex : Int, name : String, startingBeat : Int }
    }
```

### Elm: `State/Msg.elm`

Add: `| PropsDialogSetStartingBeat Int String` (sectionIndex, value as string)

### Elm: `View/Dialogs/Properties.elm`

Add starting beat fields below the taal selector — one for each section that supports it. Group taans together with one field. Show validation error if any beat exceeds taal matras.

### Elm: `State/Update.elm` — `PropsDialogSubmit`

When applying properties: update each section's `startingBeat` from the form. Validate all are within 1..taal.matras before applying.

## Rendering Changes

### What to render

Beats before `startingBeat` (beats 1 through startingBeat-1, 0-indexed: beats 0 through startingBeat-2) show a filled circle "●" instead of the normal empty cell or rest dash.

### Elm: `View/SwarGlyph.elm`

Add a new function:
```elm
drawLockedBeat : NotationColors -> Html msg
drawLockedBeat colors =
    div [ class "swar-glyph swar-locked" ]
        [ span [ class "swar-text", style "color" colors.rest, style "opacity" "0.4" ]
            [ text "●" ]
        ]
```

### Elm: `View/GridRenderer.elm`

When rendering a beat cell, check if the beat index is before the section's `startingBeat`. If so, render `drawLockedBeat` instead of the normal cell content. The section's `startingBeat` needs to be threaded through to the grid renderer.

### Desktop: `SwarGlyphRenderer.scala`

Add `drawLockedBeat` method — draws "●" in a lighter/grey color.

### Desktop: `GridRendererFX.scala`

Check beat position against section's `startingBeat` when rendering cells.

## Cursor Behavior

### Cursor skips locked beats

When the cursor would land on a locked beat (beat < startingBeat), skip to startingBeat instead. This applies to:
- Initial cursor position after creating/switching to a section
- Arrow key navigation (left arrow from startingBeat wraps to previous cycle's last beat, not to locked beats)
- Home key should go to startingBeat, not beat 0

### Elm: cursor navigation in `Input/KeyHandler.elm` or `State/Update.elm`

Add a guard: after computing the new beat position, if `beat < startingBeat - 1` (0-indexed), clamp to `startingBeat - 1`.

### Desktop: `CursorModel` / `EditorPane` cursor movement

Same guard — clamp beat to `startingBeat - 1` (0-indexed) for the current section.

## File Format Backward Compatibility

- Old `.swar` files without `startingBeat` in sections decode with default `1` (start at sam)
- The `Option[Int].getOrElse(1)` pattern in the Scala decoder handles this
- The `optionalFieldWithDefault` pattern in Elm handles this

## Verification

1. `sbt sangeet-core/compile` — model + codec changes compile
2. `sbt sangeet-core/test` — existing tests pass with new default field; add new codec roundtrip test for Section with startingBeat
3. `sbt sangeet-server/compile` — route changes compile  
4. `sbt sangeet-server/test` — existing integration tests pass
5. `cd sangeet-web && npx elm make src/Main.elm` — Elm compiles
6. `cd sangeet-web && npx elm-test` — Elm tests pass
7. Desktop: launch app → New Gat dialog shows 3 starting beat fields → create → locked beats render as dots → cursor skips them
8. Web: same flow via browser
9. Properties dialog: change taal → validation error if beats out of range → fix → save → grid updates
10. Open old .swar file → loads fine with startingBeat=1 default

## Implementation Order

1. **Model + Codecs** — Section.scala, CompositionCodecs.scala, Composition.elm (data layer)
2. **Creation flow** — CompositionEditor, CompositionApi, CompositionRoutes (server)
3. **Desktop New dialog** — NewCompositionDialog.scala + MainApp.scala
4. **Desktop Properties dialog** — CompositionPropertiesDialog.scala + MainApp.scala
5. **Elm New dialog** — NewDialogForm, Msg, NewComposition.elm view, Update.elm, Api/Composition.elm
6. **Elm Properties dialog** — PropsDialogForm, Msg, Properties.elm view, Update.elm
7. **Rendering** — SwarGlyph (Elm + desktop), GridRenderer (Elm + desktop)
8. **Cursor skip** — KeyHandler / cursor navigation (Elm + desktop)
9. **Tests** — codec roundtrip, existing test fixes
