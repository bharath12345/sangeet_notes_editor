# Plan 1: Desktop Rebuild with Backend/Frontend Separation

**Goal:** Restructure the Sangeet Notes Editor from a monolithic `sangeet.*` ScalaFX app into a two-module sbt build: `sangeet-core` (pure Scala 3 library, zero ScalaFX) and `sangeet-desktop` (thin ScalaFX UI layer). Feature parity with the current app. The old `sangeet.*` code stays as reference until Plan 1 is complete, then gets deleted.

**Package convention:**
- `com.varpas.sangeet.core.*` -- all backend logic
- `com.varpas.sangeet.desktop.*` -- all ScalaFX UI code

---

## Architecture Overview

```
sangeet-notes-editor/                    (root sbt project, aggregates both modules)
  build.sbt                              (multi-module: sangeet-core + sangeet-desktop)
  sangeet-core/
    src/main/scala/com/varpas/sangeet/core/
      model/       -- Note, Variant, Octave, Stroke, Laya, SwarScript, Rational, BeatPosition,
                      NoteRef, Ornament, Event, Taal, Vibhag, VibhagMarker, Raag, Section,
                      SectionType, Tihai, Metadata, Composition, CompositionType, MeendDirection
      editor/      -- CursorModel, CompositionEditor, KeyHandler, OrnamentMode, UndoHistory
      layout/      -- LayoutConfig, BeatGrouper, LineBreaker, GridLayout, LayoutModel types
                      (BeatCell, CycleAndBeat, GridLine, SectionGrid)
      render/      -- ScriptMap, NotationColors, GlyphMetrics, DotPosition (pure data, NO ScalaFX)
      format/      -- Codecs (circe), SwarFormat, PdfExport, HtmlExport
      audio/       -- TimedNote, SoundEngine trait, PlaybackScheduler, MidiEngine, PlaybackController
      raag/        -- Raags (26 built-in definitions)
      taal/        -- Taals (11 built-in definitions)
      api/         -- CompositionApi, EditorApi, CursorApi, SectionApi, OrnamentApi,
                      StrokeApi, LayoutApi, GlyphApi, ExportApi, PlaybackApi, ReferenceApi, ApiError
    src/test/scala/com/varpas/sangeet/core/...
    src/main/resources/fonts/             -- Noto Sans Devanagari, Kannada, Telugu
  sangeet-desktop/
    src/main/scala/com/varpas/sangeet/desktop/
      render/      -- SwarGlyphRenderer, GridRendererFX, OrnamentRendererFX,
                      TihaiRendererFX, CanvasRendererFX
      editor/      -- EditorPane, CompositionHeader, StatusBar, AppLogger,
                      KeyboardLegend
      dialog/      -- NewCompositionDialog, CompositionPropertiesDialog
      MainApp.scala
    src/test/scala/com/varpas/sangeet/desktop/...
  src/                                   (OLD code -- untouched reference, deleted at end)
```

**Key constraint:** `sangeet-core` must compile with ZERO ScalaFX/JavaFX dependencies. It depends only on circe, pdfbox, javax.sound.midi (part of JDK). `sangeet-desktop` depends on `sangeet-core` + ScalaFX.

---

## Task 1: Multi-Module sbt Build Setup

**Files:** `build.sbt`, `project/build.properties`, `project/plugins.sbt`, directory structure

### Steps

- [ ] **1.1** Create sub-project directories:
  ```
  mkdir -p sangeet-core/src/main/scala/com/varpas/sangeet/core
  mkdir -p sangeet-core/src/test/scala/com/varpas/sangeet/core
  mkdir -p sangeet-core/src/main/resources
  mkdir -p sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop
  mkdir -p sangeet-desktop/src/test/scala/com/varpas/sangeet/desktop
  ```

- [ ] **1.2** Create package directories inside `sangeet-core`:
  ```
  mkdir -p sangeet-core/src/main/scala/com/varpas/sangeet/core/{model,editor,layout,render,format,audio,raag,taal,api}
  mkdir -p sangeet-core/src/test/scala/com/varpas/sangeet/core/{model,editor,layout,render,format,audio,raag,taal,api}
  ```

- [ ] **1.3** Create package directories inside `sangeet-desktop`:
  ```
  mkdir -p sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/{render,editor,dialog}
  mkdir -p sangeet-desktop/src/test/scala/com/varpas/sangeet/desktop/{render,editor}
  ```

- [ ] **1.4** Rewrite `build.sbt` as a multi-module build:
  ```scala
  val scala3Version = "3.4.2"

  ThisBuild / scalaVersion := scala3Version
  ThisBuild / version := "0.2.0"
  ThisBuild / scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
  ThisBuild / externalResolvers := Seq(Resolver.mavenLocal, Resolver.mavenCentral)

  lazy val root = project
    .in(file("."))
    .aggregate(sangeetCore, sangeetDesktop)
    .settings(
      name := "sangeet-notes-editor",
      // Root project does not compile source directly
      Compile / sources := Seq.empty,
      Test / sources := Seq.empty,
    )

  lazy val sangeetCore = project
    .in(file("sangeet-core"))
    .settings(
      name := "sangeet-core",
      libraryDependencies ++= Seq(
        "io.circe"          %% "circe-core"    % "0.14.7",
        "io.circe"          %% "circe-parser"  % "0.14.7",
        "io.circe"          %% "circe-generic" % "0.14.7",
        "org.apache.pdfbox"  % "pdfbox"        % "3.0.2",
        "org.scalatest"     %% "scalatest"     % "3.2.18" % Test,
      ),
      fork := true,
    )

  lazy val sangeetDesktop = project
    .in(file("sangeet-desktop"))
    .dependsOn(sangeetCore)
    .settings(
      name := "sangeet-desktop",
      libraryDependencies ++= Seq(
        "org.scalafx"   %% "scalafx" % "21.0.0-R32"
          excludeAll(
            ExclusionRule(organization = "org.openjfx", name = "javafx-web"),
            ExclusionRule(organization = "org.openjfx", name = "javafx-swing"),
            ExclusionRule(organization = "org.openjfx", name = "javafx-fxml"),
          ),
        "org.scalatest" %% "scalatest" % "3.2.18" % Test,
      ),
      fork := true,
      javaHome := {
        val j25 = file("/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home")
        val j21 = file("/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home")
        val j17 = file("/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home")
        if (j25.exists()) Some(j25)
        else if (j21.exists()) Some(j21)
        else if (j17.exists()) Some(j17)
        else sys.env.get("JAVA_HOME").map(file(_))
      },
      javaOptions ++= {
        if (sys.props("os.name").toLowerCase.contains("mac")) {
          val iconPath = (ThisBuild / baseDirectory).value / "packaging" / "icons" / "sangeet-icon-256.png"
          Seq("-Xms512m", "-Xmx2g",
              "-Xdock:name=Sangeet Notes Editor",
              s"-Xdock:icon=${iconPath.getAbsolutePath}",
              "-Dapple.awt.application.name=Sangeet Notes Editor")
        } else Seq("-Xms512m", "-Xmx2g")
      },
      Compile / mainClass := Some("com.varpas.sangeet.desktop.MainApp"),
      assembly / mainClass := Some("com.varpas.sangeet.desktop.MainApp"),
      assembly / assemblyJarName := "sangeet-notes-editor.jar",
      assembly / assemblyMergeStrategy := {
        case x if x.endsWith(".dll")              => MergeStrategy.discard
        case x if x.endsWith(".so")               => MergeStrategy.discard
        case PathList("META-INF", "versions", _*) => MergeStrategy.first
        case PathList("META-INF", "MANIFEST.MF")  => MergeStrategy.discard
        case PathList("META-INF", "services", _*) => MergeStrategy.concat
        case PathList("META-INF", _*)             => MergeStrategy.first
        case "module-info.class"                  => MergeStrategy.discard
        case x if x.endsWith(".class")            => MergeStrategy.first
        case x                                    => MergeStrategy.first
      },
    )
  ```

- [ ] **1.5** Copy font resources from `src/main/resources/fonts/` to `sangeet-core/src/main/resources/fonts/` (PDF export needs them).

- [ ] **1.6** Verify the multi-module build compiles:
  ```
  sbt sangeetCore/compile
  sbt sangeetDesktop/compile
  ```

**Commit point:** "chore: set up multi-module sbt build (sangeet-core + sangeet-desktop)"

---

## Task 2: sangeet-core Domain Model

**Files:** `sangeet-core/src/main/scala/com/varpas/sangeet/core/model/*.scala`

All types are rewritten from scratch under the new package, using the backend API spec as the canonical reference. The types themselves are nearly identical to the existing `sangeet.model.*` -- the change is the package path and a few naming clarifications.

### Steps

- [ ] **2.1** Create `Note.scala`:
  ```scala
  package com.varpas.sangeet.core.model

  enum Note:
    case Sa, Re, Ga, Ma, Pa, Dha, Ni

  enum Variant:
    case Shuddha, Komal, Tivra

  enum Octave:
    case AtiMandra, Mandra, Madhya, Taar, AtiTaar
  ```

- [ ] **2.2** Create `Stroke.scala`:
  ```scala
  package com.varpas.sangeet.core.model

  enum Stroke:
    case Da, Ra, Chikari, Jod
  ```

- [ ] **2.3** Create `Laya.scala`:
  ```scala
  package com.varpas.sangeet.core.model

  enum Laya:
    case AtiVilambit, Vilambit, Madhya, Drut, AtiDrut
  ```

- [ ] **2.4** Create `SwarScript.scala`:
  ```scala
  package com.varpas.sangeet.core.model

  enum SwarScript:
    case Devanagari, Kannada, Telugu, English
  ```

- [ ] **2.5** Create `MeendDirection.scala`:
  ```scala
  package com.varpas.sangeet.core.model

  enum MeendDirection:
    case Ascending, Descending
  ```

- [ ] **2.6** Create `Rational.scala` -- copy logic from existing `sangeet.model.Rational`, update package to `com.varpas.sangeet.core.model`. Include `onBeat`, `fullBeat` companions, `+`, `compare`, `toDouble`, GCD normalization.

- [ ] **2.7** Create `BeatPosition.scala`:
  ```scala
  package com.varpas.sangeet.core.model

  case class BeatPosition(
    cycle: Int,
    beat: Int,
    subdivision: Rational
  )
  ```

- [ ] **2.8** Create `NoteRef.scala`:
  ```scala
  package com.varpas.sangeet.core.model

  case class NoteRef(
    note: Note,
    variant: Variant,
    octave: Octave
  )
  ```

- [ ] **2.9** Create `Ornament.scala` -- all 11 ornament types (Meend, KanSwar, Murki, Gamak, Andolan, Krintan, Gitkari, Ghaseet, Sparsh, Zamzama, CustomOrnament) as a sealed trait hierarchy. Same structure as existing.

- [ ] **2.10** Create `Event.scala` -- enum with `Swar`, `Rest`, `Sustain` cases. Include `position` method. Same structure as existing.

- [ ] **2.11** Create `Taal.scala` -- `Taal`, `Vibhag`, `VibhagMarker` (enum with `Sam`, `Taali(number: Int)`, `Khali`).

- [ ] **2.12** Create `Raag.scala` -- same structure as existing.

- [ ] **2.13** Create `Section.scala` -- `Section` (with `Option[Tihai]`), `SectionType` enum.

- [ ] **2.14** Create `Composition.scala` -- `Composition`, `Metadata`, `CompositionType`, `Laya`, `Tihai`.

- [ ] **2.15** Write unit tests in `sangeet-core/src/test/scala/com/varpas/sangeet/core/model/`:
  - `RationalSpec.scala` -- normalization, arithmetic, comparison, edge cases (zero, negative)
  - `NoteSpec.scala` -- enum values exist
  - `OrnamentSpec.scala` -- construction of each ornament type
  - `CompositionSpec.scala` -- Composition construction, section type defaults

- [ ] **2.16** Verify: `sbt sangeetCore/test`

**Commit point:** "feat(core): add domain model types under com.varpas.sangeet.core.model"

---

## Task 3: sangeet-core Editor Logic

**Files:** `sangeet-core/src/main/scala/com/varpas/sangeet/core/editor/*.scala`

### Steps

- [ ] **3.1** Create `CursorModel.scala` under `com.varpas.sangeet.core.editor`. Port from existing `sangeet.editor.CursorModel`. Same fields: `taal`, `cycle`, `beat`, `subIndex`, `totalSubdivisions`, `currentOctave`. Same methods: `position`, `nextBeat`, `prevBeat`, `nextSubBeat`, `withSubdivisions`, `withOctave`. Add `moveTo(cycle, beat)` from the API spec.

- [ ] **3.2** Create `OrnamentMode.scala` -- port enum from existing `sangeet.editor.OrnamentMode`. Same cases.

- [ ] **3.3** Create `CompositionEditor.scala` under `com.varpas.sangeet.core.editor`. Port the case class and companion object. Methods: `currentSection`, `updateCurrentSection`, `addEvent`, `removeLastEvent`, `maxCycle`, `removeSection`, `renameSection`, `moveSection`, `modifyLastSwar`, `setStrokeAt`, `clearStrokeAt`, `swarsAtBeat`. Companion: `empty`, `create`.

- [ ] **3.4** Create `KeyHandler.scala` under `com.varpas.sangeet.core.editor`. Port from existing. Methods: `handleSwarKey`, `handleDualSwar`, `handleSpecialKey`, `handleOctaveKey`, `handleSubdivision`, `handleStroke`, `handleSimpleOrnament`, `handleNoteOrnament`, `finishMultiNoteOrnament`, `resolveVariant`.

- [ ] **3.5** Create `UndoHistory.scala` -- port from existing. Same structure: `past`, `present`, `future`, `maxSize`, `push`, `undo`, `redo`, `canUndo`, `canRedo`.

- [ ] **3.6** Write unit tests:
  - `CursorModelSpec.scala` -- all movement methods, boundary behavior, moveTo
  - `CursorModelEdgeCaseSpec.scala` -- edge cases (cycle 0 beat 0 prevBeat, etc.)
  - `CompositionEditorSpec.scala` -- create, addEvent, removeLastEvent, removeSection, moveSection, renameSection, modifyLastSwar, setStrokeAt, clearStrokeAt
  - `CompositionEditorEdgeCaseSpec.scala` -- edge cases
  - `KeyHandlerSpec.scala` -- each handleX method
  - `UndoHistorySpec.scala` -- push/undo/redo, max size

- [ ] **3.7** Verify: `sbt sangeetCore/test`

**Commit point:** "feat(core): add editor logic (CursorModel, CompositionEditor, KeyHandler, UndoHistory)"

---

## Task 4: sangeet-core Layout Engine

**Files:** `sangeet-core/src/main/scala/com/varpas/sangeet/core/layout/*.scala`

### Steps

- [ ] **4.1** Create `LayoutModel.scala` -- `BeatCell`, `CycleAndBeat`, `GridLine`, `SectionGrid`. Same types as existing `sangeet.layout.*`.

- [ ] **4.2** Create `LayoutConfig.scala` -- port from existing. Same fields and defaults.

- [ ] **4.3** Create `BeatGrouper.scala` -- port from existing `sangeet.layout.BeatGrouper`. Groups events by (cycle, beat) into `BeatCell` list.

- [ ] **4.4** Create `LineBreaker.scala` -- port from existing `sangeet.layout.LineBreaker`. Density-aware line breaking: full cycle per line (drut) vs split by vibhag (vilambit).

- [ ] **4.5** Create `GridLayout.scala` -- port from existing. `layout(section, taal, config)` and `layoutAll(composition, config)`.

- [ ] **4.6** Write unit tests:
  - `BeatGrouperSpec.scala` -- grouping events by beat
  - `LineBreakerSpec.scala` -- line breaking at different densities
  - `LineBreakerMultiTaalSpec.scala` -- multi-taal edge cases
  - `LayoutConfigSpec.scala` -- default values
  - `GridLayoutSpec.scala` -- full pipeline

- [ ] **4.7** Verify: `sbt sangeetCore/test`

**Commit point:** "feat(core): add layout engine (BeatGrouper, LineBreaker, GridLayout)"

---

## Task 5: sangeet-core Glyph/Rendering Data

**Files:** `sangeet-core/src/main/scala/com/varpas/sangeet/core/render/*.scala`

These are pure data objects -- no ScalaFX types. The desktop renderer will consume this data to draw on a ScalaFX Canvas.

### Steps

- [ ] **5.1** Create `DotPosition.scala`:
  ```scala
  package com.varpas.sangeet.core.render

  enum DotPosition:
    case Above, Below, None
  ```

- [ ] **5.2** Create `NotationColors.scala` -- port from existing `sangeet.render.NotationColors`. Same hex strings and `hexToRgb` helper.

- [ ] **5.3** Create `ScriptMap.scala` -- port from existing `sangeet.render.ScriptMap`. All glyph maps (Devanagari, Kannada, Telugu, English), `glyph`, `fontName`, `legendEntries`, `displayName`. Pure data, no ScalaFX Font type.

- [ ] **5.4** Create `GlyphMetrics.scala` -- extract rendering logic that was in `DevanagariMap`:
  ```scala
  package com.varpas.sangeet.core.render

  import com.varpas.sangeet.core.model.*

  object GlyphMetrics:
    def glyph(note: Note, variant: Variant, script: SwarScript): String =
      ScriptMap.glyph(note, script)

    def needsKomalMark(note: Note, variant: Variant): Boolean =
      variant == Variant.Komal && (note == Note.Re || note == Note.Ga ||
        note == Note.Dha || note == Note.Ni)

    def needsTivraMark(note: Note, variant: Variant): Boolean =
      variant == Variant.Tivra && note == Note.Ma

    def octaveDots(octave: Octave): (Int, DotPosition) = octave match
      case Octave.AtiMandra => (2, DotPosition.Below)
      case Octave.Mandra    => (1, DotPosition.Below)
      case Octave.Madhya    => (0, DotPosition.None)
      case Octave.Taar      => (1, DotPosition.Above)
      case Octave.AtiTaar   => (2, DotPosition.Above)

    val restSymbol: String = "-"
    val sustainSymbol: String = "\u2014"

    def vibhagMarkerText(marker: VibhagMarker): String = marker match
      case VibhagMarker.Sam      => "X"
      case VibhagMarker.Taali(n) => n.toString
      case VibhagMarker.Khali    => "0"

    def strokeText(stroke: Stroke, script: SwarScript): String =
      // ... port from DevanagariMap.strokeText, takes script as parameter
  ```
  **Key change:** `strokeText` takes `script` as an explicit parameter instead of reading mutable global state.

- [ ] **5.5** Write unit tests:
  - `ScriptMapSpec.scala` -- glyph lookups for all scripts
  - `GlyphMetricsSpec.scala` -- komal/tivra marks, octave dots, stroke text
  - `NotationColorsSpec.scala` -- hex parsing

- [ ] **5.6** Verify: `sbt sangeetCore/test`

**Commit point:** "feat(core): add pure rendering data (ScriptMap, GlyphMetrics, NotationColors)"

---

## Task 6: sangeet-core Format (JSON Serialization)

**Files:** `sangeet-core/src/main/scala/com/varpas/sangeet/core/format/*.scala`

### Steps

- [ ] **6.1** Create `Codecs.scala` -- circe encoders/decoders for ALL model types. Port from existing `sangeet.format.Codecs`. Must match the JSON serialization format documented in the backend API spec exactly:
  - Rational as `[numerator, denominator]` array
  - Enums as lowercase strings (Note, Variant, Stroke, Laya, SwarScript, MeendDirection)
  - Octave as camelCase strings
  - VibhagMarker: `"sam"`, `"khali"`, `{"taali": N}`
  - CompositionType: `"bandish"`, `"gat"`, `"palta"`, `{"custom": "name"}`
  - SectionType: simple cases as camelCase, Custom as `{"custom": "name"}`
  - Event: discriminated by `"type"` field
  - Ornament: discriminated by `"type"` field
  - Optional fields omitted when None

- [ ] **6.2** Create `SwarFormat.scala` -- port from existing. Methods: `toJson`, `fromJson`, `writeFile`, `readFile`. Same version handling (`"1.0"`).

- [ ] **6.3** Write unit tests:
  - `CodecsSpec.scala` -- round-trip encoding/decoding for every type
  - `CodecsEdgeCaseSpec.scala` -- edge cases (empty lists, None fields, custom types)
  - `CompositionCodecSpec.scala` -- full composition round-trip
  - `SwarFormatSpec.scala` -- file read/write, version validation

- [ ] **6.4** **Backward compatibility test:** Load an existing `.swar` file created by the old code using the new `SwarFormat.readFile`. Verify it parses correctly. Then re-serialize and verify the output matches (modulo field ordering).

- [ ] **6.5** Verify: `sbt sangeetCore/test`

**Commit point:** "feat(core): add JSON serialization (circe codecs, SwarFormat)"

---

## Task 7: sangeet-core Format (PDF and HTML Export)

**Files:** `sangeet-core/src/main/scala/com/varpas/sangeet/core/format/{PdfExport.scala, HtmlExport.scala}`

### Steps

- [ ] **7.1** Create `PdfExport.scala` -- port from existing `sangeet.format.PdfExport`. Key change: accept `script: SwarScript` as an explicit parameter instead of reading from `DevanagariMap._script`. Use `GlyphMetrics` and `ScriptMap` from `com.varpas.sangeet.core.render` for glyph lookups. Font loading from resources (Noto Sans Devanagari embedded).

- [ ] **7.2** Create `HtmlExport.scala` -- port from existing `sangeet.format.HtmlExport`. Same change: accept `script: SwarScript` as explicit parameter. Use `NotationColors` from core for color values.

- [ ] **7.3** Write unit tests:
  - `PdfExportSpec.scala` -- generates a PDF without crashing, output is non-empty bytes
  - `HtmlExportSpec.scala` -- generates valid HTML string, contains expected elements

- [ ] **7.4** Verify: `sbt sangeetCore/test`

**Commit point:** "feat(core): add PDF and HTML export"

---

## Task 8: sangeet-core Audio

**Files:** `sangeet-core/src/main/scala/com/varpas/sangeet/core/audio/*.scala`

### Steps

- [ ] **8.1** Create `TimedNote.scala`:
  ```scala
  package com.varpas.sangeet.core.audio

  import com.varpas.sangeet.core.model.*

  case class TimedNote(
    timeMs: Long,
    durationMs: Long,
    note: Note,
    variant: Variant,
    octave: Octave,
    stroke: Option[Stroke]
  )
  ```

- [ ] **8.2** Create `SoundEngine.scala` -- trait with `init`, `playNote`, `noteOff`, `stop`, `shutdown`. Port from existing `sangeet.audio.SoundEngine`.

- [ ] **8.3** Create `PlaybackScheduler.scala` -- port from existing. Pure function: `schedule(events, bpm, matras): List[TimedNote]`.

- [ ] **8.4** Create `MidiEngine.scala` -- port from existing. Uses `javax.sound.midi` (JDK, not ScalaFX). Includes `toMidiNote` helper.

- [ ] **8.5** Create `PlaybackController.scala` -- port from existing. Uses `ScheduledExecutorService` for timing. Batch tick thread approach (single thread processes notes in time order).

- [ ] **8.6** Write unit tests:
  - `PlaybackSchedulerSpec.scala` -- scheduling logic, timing calculations
  - `MidiEngineSpec.scala` -- note mapping (MIDI note numbers)
  - `PlaybackControllerSpec.scala` -- play/stop lifecycle

- [ ] **8.7** Verify: `sbt sangeetCore/test`

**Commit point:** "feat(core): add audio playback (PlaybackScheduler, MidiEngine, PlaybackController)"

---

## Task 9: sangeet-core Reference Data

**Files:** `sangeet-core/src/main/scala/com/varpas/sangeet/core/{raag,taal}/*.scala`

### Steps

- [ ] **9.1** Create `Taals.scala` under `com.varpas.sangeet.core.taal`. Port all 11 built-in taal definitions from existing `sangeet.taal.Taals`. Include `all: Map[String, Taal]`, `byName(name: String): Option[Taal]`, and convenience vals (`teentaal`, `ektaal`, etc.).

- [ ] **9.2** Create `Raags.scala` under `com.varpas.sangeet.core.raag`. Port all 26 built-in raag definitions from existing `sangeet.raag.Raags`.

- [ ] **9.3** Write unit tests:
  - `TaalsSpec.scala` -- all 11 taals exist, matras match, vibhag structure correct
  - `RaagsSpec.scala` -- all 26 raags exist, required fields populated

- [ ] **9.4** Verify: `sbt sangeetCore/test`

**Commit point:** "feat(core): add built-in raag and taal reference data"

---

## Task 10: sangeet-core API Layer

**Files:** `sangeet-core/src/main/scala/com/varpas/sangeet/core/api/*.scala`

This layer provides the clean public API defined in the backend API spec. Each API object is a thin facade over the internal modules.

### Steps

- [ ] **10.1** Create `ApiError.scala` -- the error ADT from the backend API spec (Section 4). All cases: `InvalidNoteVariant`, `InvalidSectionIndex`, `LastSection`, `EmptySection`, `NoSwarTarget`, `NoSwarAtPosition`, `EmptyNotes`, `InsufficientNotes`, `InvalidOrnamentType`, `ParseError`, `VersionError`, `ValidationError`, `NotFound`, `ExportError`, `MissingField`.

- [ ] **10.2** Create `EditorInput.scala` and `EditorResult.scala`:
  ```scala
  case class EditorInput(
    composition: Composition,
    sectionIndex: Int,
    cursor: CursorModel
  )
  case class EditorResult(
    composition: Composition,
    cursor: CursorModel,
    message: String
  )
  ```

- [ ] **10.3** Create `CompositionApi.scala` -- `createComposition`, `parseComposition`, `serializeComposition`. Delegates to `CompositionEditor.create` and `SwarFormat`.

- [ ] **10.4** Create `EditorApi.scala` -- `insertSwar`, `insertRest`, `insertSustain`, `deleteLastEvent`, `insertDualSwar`. Delegates to `KeyHandler`.

- [ ] **10.5** Create `CursorApi.scala` -- `nextBeat`, `prevBeat`, `nextSubBeat`, `setSubdivisions`, `setOctave`, `moveTo`. Pure cursor operations.

- [ ] **10.6** Create `SectionApi.scala` -- `addSection`, `removeSection`, `renameSection`, `moveSection`.

- [ ] **10.7** Create `OrnamentApi.scala` -- `addSimpleOrnament`, `addSingleNoteOrnament`, `addMeend`, `addKrintan`, `addMurki`, `addZamzama`.

- [ ] **10.8** Create `StrokeApi.scala` -- `setStroke`, `clearStroke`.

- [ ] **10.9** Create `LayoutApi.scala` -- `computeLayout`, `computeSectionLayout`.

- [ ] **10.10** Create `GlyphApi.scala` -- `noteGlyph`, `notationColors`, `allScriptMappings`. Returns pure data types (`GlyphInfo`, `ColorPalette`, `ScriptMapping`).

- [ ] **10.11** Create `ExportApi.scala` -- `exportPdf`, `exportHtml`. Takes explicit `SwarScript` parameter.

- [ ] **10.12** Create `PlaybackApi.scala` -- `schedulePlayback`.

- [ ] **10.13** Create `ReferenceApi.scala` -- `allTaals`, `taalByName`, `allRaags`, `raagByName`.

- [ ] **10.14** Write unit tests for each API object -- at minimum one happy path and one error case per method.

- [ ] **10.15** Verify: `sbt sangeetCore/test`

**Commit point:** "feat(core): add public API layer (CompositionApi, EditorApi, etc.)"

---

## Task 11: sangeet-desktop Canvas Rendering

**Files:** `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/render/*.scala`

These are thin ScalaFX wrappers that use layout data and glyph metrics from `sangeet-core` to draw on a JavaFX Canvas.

### Steps

- [ ] **11.1** Create `SwarGlyphRenderer.scala` -- port drawing logic from existing `SwarGlyph`. Uses `GlyphMetrics` from core for glyph text, komal/tivra marks, octave dots. Uses ScalaFX `GraphicsContext`, `Font`, `Color`. Takes `script: SwarScript` as parameter (no mutable global state). Caches `Font` instances keyed by script.

- [ ] **11.2** Create `OrnamentRendererFX.scala` -- port from existing `OrnamentRenderer`. Uses ScalaFX drawing primitives for meend arcs, gamak zigzags, kan swar glyphs, etc.

- [ ] **11.3** Create `TihaiRendererFX.scala` -- port tihai bracket rendering (if present in existing code). Draws `x3` bracket markers.

- [ ] **11.4** Create `GridRendererFX.scala` -- port from existing `GridRenderer`. Key changes:
  - Uses `GlyphMetrics.strokeText(stroke, script)` instead of `DevanagariMap.strokeText(stroke)`
  - Uses `GlyphMetrics.vibhagMarkerText(marker)` instead of `DevanagariMap.vibhagMarkerText(marker)`
  - Uses `SwarGlyphRenderer` instead of `SwarGlyph`
  - Receives `script` as parameter
  - Same `LineLayout` constants, same `lineHeight` calculation
  - `drawSection`, `drawGridLine` methods

- [ ] **11.5** Create `CanvasRendererFX.scala` -- port from existing `CanvasRenderer`. Orchestrates `GridRendererFX` for each section. Returns `List[SectionBounds]` for click-to-beat mapping. Same `SectionBounds` and `LineBounds` types defined locally (or in a shared types file).

- [ ] **11.6** Write unit tests (limited -- rendering is visual):
  - Verify `SwarGlyphRenderer` font cache behavior
  - Verify `GridRendererFX.lineHeight` returns expected values

- [ ] **11.7** Verify: `sbt sangeetDesktop/compile`

**Commit point:** "feat(desktop): add canvas rendering layer"

---

## Task 12: sangeet-desktop Editor UI

**Files:** `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/editor/*.scala`

### Steps

- [ ] **12.1** Create `AppLogger.scala` -- port from existing `sangeet.editor.AppLogger`.

- [ ] **12.2** Create `StatusBar.scala` -- port from existing `sangeet.editor.StatusBar`. ScalaFX `ListView` with log messages.

- [ ] **12.3** Create `CompositionHeader.scala` -- port from existing `sangeet.editor.CompositionHeader`. ScalaFX `VBox` panel. Uses core model types.

- [ ] **12.4** Create `KeyboardLegend.scala` -- port from existing `sangeet.editor.KeyboardLegend`. Uses `ScriptMap` from core.

- [ ] **12.5** Create `EditorPane.scala` -- port from existing `sangeet.editor.EditorPane`. This is the most complex file. Key changes:
  - Import types from `com.varpas.sangeet.core.model.*` and `com.varpas.sangeet.core.editor.*`
  - Import layout from `com.varpas.sangeet.core.layout.*`
  - Import renderer from `com.varpas.sangeet.desktop.render.*`
  - Store `script: SwarScript` as local mutable state (replaces `DevanagariMap._script` global)
  - Pass `script` to all render calls
  - No voice recognition (omit all whisper-jni/MicCapture code)
  - Same keyboard handling logic (delegates to core `KeyHandler`)
  - Same mouse click handling (delegates to `SectionBounds` hit testing)
  - Same blink timer, double-tap detection, auto-save, layout cache
  - `changeScript(script)` method updates local state and redraws

- [ ] **12.6** Verify: `sbt sangeetDesktop/compile`

**Commit point:** "feat(desktop): add editor UI (EditorPane, CompositionHeader, StatusBar)"

---

## Task 13: sangeet-desktop Dialogs and Main App

**Files:** `sangeet-desktop/src/main/scala/com/varpas/sangeet/desktop/*.scala`, `.../dialog/*.scala`

### Steps

- [ ] **13.1** Create `NewCompositionDialog.scala` under `com.varpas.sangeet.desktop.dialog`. Port from existing. Uses core `Raags`, `Taals`, model types.

- [ ] **13.2** Create `CompositionPropertiesDialog.scala` under `com.varpas.sangeet.desktop.dialog`. Port from existing.

- [ ] **13.3** Create `SampleComposition.scala` under `com.varpas.sangeet.desktop.editor`. Port from existing `sangeet.editor.SampleComposition`. Uses core model types and raag/taal reference data.

- [ ] **13.4** Create `MainApp.scala` under `com.varpas.sangeet.desktop`. Port from existing `sangeet.editor.MainApp`. Key changes:
  - Extends `JFXApp3`, same window layout (1400x800, split panes, toolbars)
  - Uses core `PlaybackController`, `MidiEngine`, `SwarFormat`
  - Uses desktop `EditorPane`, `StatusBar`, `KeyboardLegend`
  - Script combo box calls `editorPane.changeScript(script)` instead of `DevanagariMap.setScript`
  - Voice button remains disabled (no whisper-jni dependency in desktop module for now)
  - Single-instance lock on port 47633

- [ ] **13.5** Verify desktop app launches:
  ```
  sbt sangeetDesktop/run
  ```

- [ ] **13.6** Verify: create a new composition, enter notes, save as `.swar`, export PDF, export HTML, play audio.

**Commit point:** "feat(desktop): add MainApp, dialogs, sample composition"

---

## Task 14: Migration Verification

### Steps

- [ ] **14.1** **Run full test suite:**
  ```
  sbt test
  ```
  All tests in both `sangeetCore/test` and `sangeetDesktop/test` must pass.

- [ ] **14.2** **Backward compatibility:** Open an existing `.swar` file created by the old monolithic app using the new desktop app. Verify:
  - File opens without errors
  - All sections, events, ornaments render correctly
  - Re-save and diff the JSON -- should be semantically identical

- [ ] **14.3** **PDF export comparison:** Export the same composition to PDF using both old and new code. Visually compare the output for layout, glyph rendering, colors, ornaments, stroke/sahitya rows.

- [ ] **14.4** **HTML export comparison:** Same as PDF -- compare old vs new HTML output.

- [ ] **14.5** **Feature parity checklist:**
  - [ ] Note entry (all 12 chromatic notes via keyboard)
  - [ ] Octave modifiers (mandra, taar, madhya)
  - [ ] Double-tap dual swar (ss, rr, gg, etc.)
  - [ ] Rest and sustain entry
  - [ ] Backspace delete
  - [ ] Cursor navigation (arrows, Tab, Enter)
  - [ ] Subdivisions (Ctrl+2 through Ctrl+8)
  - [ ] Undo / Redo (Ctrl+Z, Ctrl+Shift+Z)
  - [ ] Stroke entry (Ctrl+D/R/C)
  - [ ] Stroke edit mode (F2 toggle)
  - [ ] All ornaments (simple, single-note, two-note, multi-note)
  - [ ] Ornament mode cancellation (Escape)
  - [ ] Mouse click cursor placement
  - [ ] Section switching (click on inactive section)
  - [ ] New composition dialog (all fields, conditional visibility)
  - [ ] Open/Save .swar files
  - [ ] PDF export
  - [ ] HTML export
  - [ ] Script switching (Devanagari, Kannada, Telugu, English)
  - [ ] MIDI playback (play, pause, stop)
  - [ ] BPM slider and laya-to-BPM defaults
  - [ ] Composition header rendering
  - [ ] Keyboard legend sidebar
  - [ ] Status bar / log panel
  - [ ] Auto-save (debounced, background thread)
  - [ ] Read-only sample on startup
  - [ ] Single-instance lock
  - [ ] Add/Remove/Rename/Move sections
  - [ ] Composition properties dialog
  - [ ] About dialog

- [ ] **14.6** **Test count verification:** New test suite should have >= 299 tests (matching old suite). Run `sbt testOnly` with verbose output and count.

**Commit point:** "test: migration verification -- feature parity confirmed"

---

## Task 15: Cleanup

### Steps

- [ ] **15.1** Delete old source code:
  ```
  rm -rf src/main/scala/sangeet/
  rm -rf src/test/scala/sangeet/
  ```

- [ ] **15.2** Remove old root-level source/test settings from `build.sbt` if any remain (the root project should have `Compile / sources := Seq.empty`).

- [ ] **15.3** Update `CLAUDE.md`:
  - Change module layout section to reflect new `sangeet-core` / `sangeet-desktop` structure
  - Update package references from `sangeet.*` to `com.varpas.sangeet.core.*` / `com.varpas.sangeet.desktop.*`
  - Update main class path
  - Note that voice recognition is excluded (will be revisited separately)

- [ ] **15.4** Final full build:
  ```
  sbt clean compile test
  sbt sangeetDesktop/run
  ```

- [ ] **15.5** Update CI/CD configuration (`.github/workflows/`) if it references old paths or main class.

**Commit point:** "chore: remove old monolithic code, update docs and CI"

---

## Summary

| Task | Description | Est. Steps | Dependencies |
|------|-------------|------------|--------------|
| 1 | Multi-module sbt build | 6 | None |
| 2 | Core domain model | 16 | Task 1 |
| 3 | Core editor logic | 7 | Task 2 |
| 4 | Core layout engine | 7 | Task 2 |
| 5 | Core glyph/rendering data | 6 | Task 2 |
| 6 | Core JSON serialization | 5 | Task 2 |
| 7 | Core PDF/HTML export | 4 | Tasks 5, 6 |
| 8 | Core audio | 7 | Task 2 |
| 9 | Core reference data | 4 | Task 2 |
| 10 | Core API layer | 15 | Tasks 3-9 |
| 11 | Desktop canvas rendering | 7 | Tasks 4, 5 |
| 12 | Desktop editor UI | 6 | Tasks 3, 5, 11 |
| 13 | Desktop dialogs & MainApp | 6 | Tasks 9, 12 |
| 14 | Migration verification | 6 | Task 13 |
| 15 | Cleanup | 5 | Task 14 |

**Total:** ~107 steps across 15 tasks.

**Exclusions from this plan:**
- Voice recognition / whisper-jni (will be revisited in a separate plan)
- HTTP API server (Plan 2 -- web backend)
- Web frontend / Android frontend (Plan 3)
- New features beyond current parity
