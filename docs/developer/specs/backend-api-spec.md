# Backend API Specification -- Sangeet Notes Editor

**Version:** 1.0  
**Date:** 2026-04-17  
**Status:** Draft

---

## 1. Overview

### 1.1 Purpose

This document specifies the Backend API for the Sangeet Notes Editor -- a Hindustani classical music notation editor in the Bhatkhande style. The API defines every operation needed by frontend clients (desktop, Android, web) to create, edit, render, and export compositions.

### 1.2 Architecture Context

The architecture splits the application into:

- **Backend**: A pure Scala 3 library (`sangeet-core`) containing the domain model, editor operations, layout engine, serialization, and export. Packaged as a JAR for direct JVM usage, and also exposed as an HTTP API for web clients.
- **Frontend**: Platform-specific UI layers (ScalaFX desktop, Android Compose, web SPA) that own all state and call into the backend for computation.

### 1.3 Stateless Design Philosophy

The backend is **stateless**. The client owns:

- The current `Composition` (the document)
- The `CursorModel` (editing position)
- Undo/redo history (a stack of composition snapshots)
- UI-specific state (scroll position, selection, modal state)

Every backend operation is a pure function: the client sends input data, the backend validates, transforms, and returns a result. No session, no server-side state, no side effects (except PDF byte generation for export).

### 1.4 Dual Usage Modes

| Mode | Transport | Consumer | State ownership |
|------|-----------|----------|-----------------|
| **Library** | Direct Scala function calls | Desktop (ScalaFX), Android | Client JVM process |
| **HTTP API** | JSON over HTTP | Web SPA (ScalaJS or JS) | Client browser |

The library API is the source of truth. The HTTP API is a thin REST wrapper that deserializes JSON into the same types, calls the library functions, and serializes the result back.

---

## 2. Domain Model

All types live in the `sangeet.model` package. They are pure data -- no UI, no IO dependencies.

### 2.1 Primitive Types

#### Note

The seven swaras of Indian music.

```scala
enum Note:
  case Sa, Re, Ga, Ma, Pa, Dha, Ni
```

**JSON:** lowercase string -- `"sa"`, `"re"`, `"ga"`, `"ma"`, `"pa"`, `"dha"`, `"ni"`

#### Variant

Shuddha (natural), Komal (flat), or Tivra (sharp). Sa and Pa are always Shuddha. Re, Ga, Dha, Ni can be Komal. Only Ma can be Tivra.

```scala
enum Variant:
  case Shuddha, Komal, Tivra
```

**JSON:** lowercase string -- `"shuddha"`, `"komal"`, `"tivra"`

#### Octave

Five octave registers. Madhya is default/unmarked. Mandra = dot below, Taar = dot above.

```scala
enum Octave:
  case AtiMandra, Mandra, Madhya, Taar, AtiTaar
```

**JSON:** camelCase string -- `"atiMandra"`, `"mandra"`, `"madhya"`, `"taar"`, `"atiTaar"`

#### Stroke

Sitar mizrab strokes.

```scala
enum Stroke:
  case Da, Ra, Chikari, Jod
```

**JSON:** lowercase string -- `"da"`, `"ra"`, `"chikari"`, `"jod"`

#### Laya (Tempo)

```scala
enum Laya:
  case AtiVilambit, Vilambit, Madhya, Drut, AtiDrut
```

**JSON:** camelCase string -- `"atiVilambit"`, `"vilambit"`, `"madhya"`, `"drut"`, `"atiDrut"`

#### SwarScript

The rendering script for note glyphs.

```scala
enum SwarScript:
  case Devanagari, Kannada, Telugu, English
```

**JSON:** lowercase string -- `"devanagari"`, `"kannada"`, `"telugu"`, `"english"`

#### MeendDirection

```scala
enum MeendDirection:
  case Ascending, Descending
```

**JSON:** lowercase string -- `"ascending"`, `"descending"`

#### CompositionType

```scala
enum CompositionType:
  case Bandish, Gat, Palta
  case Custom(name: String)
```

**JSON:** Simple cases as lowercase string (`"bandish"`, `"gat"`, `"palta"`). Custom as object: `{"custom": "Dhun"}`.

#### SectionType

```scala
enum SectionType:
  case Sthayi, Antara, Sanchari, Abhog
  case Taan, Toda, Jhala
  case Palta, Arohi, Avarohi
  case Custom(name: String)
```

**JSON:** Simple cases as camelCase string (`"sthayi"`, `"antara"`, `"taan"`, etc.). Custom as object: `{"custom": "Gat"}`.

### 2.2 Composite Value Types

#### Rational

Exact fractional arithmetic for sub-beat positioning and duration.

```scala
case class Rational(numerator: Int, denominator: Int)
```

**JSON:** Two-element array -- `[numerator, denominator]`

```json
[0, 1]    // 0 (on the beat)
[1, 2]    // 1/2 (halfway through beat)
[1, 4]    // 1/4 (quarter of beat)
[1, 1]    // 1 (full beat duration)
```

**Invariants:** Denominator is never zero. Values are normalized (GCD-reduced). Sign is on the numerator.

#### BeatPosition

Precise location within a composition's rhythmic structure.

```scala
case class BeatPosition(
  cycle: Int,         // taal cycle index (0-based)
  beat: Int,          // beat within cycle (0-based, 0..matras-1)
  subdivision: Rational  // sub-beat offset (0 = on beat, 1/2 = half, etc.)
)
```

**JSON:**

```json
{
  "cycle": 0,
  "beat": 3,
  "subdivision": [1, 2]
}
```

**Ordering:** Compared lexicographically by (cycle, beat, subdivision).

#### NoteRef

A reference to a specific note with its variant and octave. Used inside ornaments.

```scala
case class NoteRef(
  note: Note,
  variant: Variant,
  octave: Octave
)
```

**JSON:**

```json
{
  "note": "re",
  "variant": "komal",
  "octave": "madhya"
}
```

### 2.3 Ornaments

All ornaments extend the `Ornament` sealed trait. Serialized as discriminated unions with a `"type"` field.

```scala
sealed trait Ornament

case class Meend(startNote: NoteRef, endNote: NoteRef, direction: MeendDirection, intermediateNotes: List[NoteRef]) extends Ornament
case class KanSwar(graceNote: NoteRef) extends Ornament
case class Murki(notes: List[NoteRef]) extends Ornament
case class Gamak() extends Ornament
case class Andolan() extends Ornament
case class Krintan(notes: List[NoteRef]) extends Ornament
case class Gitkari() extends Ornament
case class Ghaseet(targetNote: NoteRef) extends Ornament
case class Sparsh(touchNote: NoteRef) extends Ornament
case class Zamzama(notes: List[NoteRef]) extends Ornament
case class CustomOrnament(name: String, parameters: Map[String, String]) extends Ornament
```

**JSON examples:**

```json
// Simple ornament (no parameters)
{"type": "gamak"}
{"type": "andolan"}
{"type": "gitkari"}

// Single-note ornament
{"type": "kanSwar", "graceNote": {"note": "ni", "variant": "shuddha", "octave": "madhya"}}
{"type": "sparsh", "touchNote": {"note": "re", "variant": "shuddha", "octave": "madhya"}}
{"type": "ghaseet", "targetNote": {"note": "pa", "variant": "shuddha", "octave": "mandra"}}

// Two-note ornament
{"type": "meend", "startNote": {"note": "pa", "variant": "shuddha", "octave": "madhya"}, "endNote": {"note": "sa", "variant": "shuddha", "octave": "taar"}, "direction": "ascending", "intermediateNotes": []}

// Multi-note ornament
{"type": "murki", "notes": [
  {"note": "ga", "variant": "shuddha", "octave": "madhya"},
  {"note": "re", "variant": "shuddha", "octave": "madhya"},
  {"note": "sa", "variant": "shuddha", "octave": "madhya"}
]}
{"type": "krintan", "notes": [
  {"note": "ga", "variant": "shuddha", "octave": "madhya"},
  {"note": "re", "variant": "shuddha", "octave": "madhya"}
]}
{"type": "zamzama", "notes": [
  {"note": "re", "variant": "shuddha", "octave": "madhya"},
  {"note": "ga", "variant": "shuddha", "octave": "madhya"},
  {"note": "re", "variant": "shuddha", "octave": "madhya"}
]}

// Custom ornament (extensible)
{"type": "custom", "name": "Taan-Sitar", "parameters": {"technique": "pull-off", "speed": "fast"}}
```

### 2.4 Events

An event is a single notation element at a specific beat position.

```scala
enum Event:
  case Swar(
    note: Note,
    variant: Variant,
    octave: Octave,
    beat: BeatPosition,
    duration: Rational,
    stroke: Option[Stroke],
    ornaments: List[Ornament],
    sahitya: Option[String]
  )
  case Rest(beat: BeatPosition, duration: Rational)
  case Sustain(beat: BeatPosition, duration: Rational)
```

**JSON (discriminated by `"type"` field):**

```json
// Swar event
{
  "type": "swar",
  "note": "ga",
  "variant": "shuddha",
  "octave": "madhya",
  "beat": {"cycle": 0, "beat": 0, "subdivision": [0, 1]},
  "duration": [1, 1],
  "ornaments": [],
  "stroke": "da",
  "sahitya": "aa"
}

// Rest event
{
  "type": "rest",
  "beat": {"cycle": 0, "beat": 4, "subdivision": [0, 1]},
  "duration": [1, 1]
}

// Sustain event
{
  "type": "sustain",
  "beat": {"cycle": 0, "beat": 5, "subdivision": [0, 1]},
  "duration": [1, 1]
}
```

**Notes:**

- `stroke` and `sahitya` are omitted from JSON when `None` (not serialized as `null`).
- `ornaments` is always present as an array (empty list = `[]`).

### 2.5 Taal (Rhythmic Cycle)

```scala
case class Taal(
  name: String,
  matras: Int,            // total beats per cycle
  vibhags: List[Vibhag],  // sections within the cycle
  theka: Option[List[String]]  // tabla syllable pattern
)

case class Vibhag(
  beats: Int,
  marker: VibhagMarker
)

enum VibhagMarker:
  case Sam                // X -- first beat
  case Taali(number: Int) // numbered clap (2, 3, ...)
  case Khali              // 0 -- wave (empty beat)
```

**JSON:**

```json
{
  "name": "Teentaal",
  "matras": 16,
  "vibhags": [
    {"beats": 4, "marker": "sam"},
    {"beats": 4, "marker": {"taali": 2}},
    {"beats": 4, "marker": "khali"},
    {"beats": 4, "marker": {"taali": 3}}
  ],
  "theka": ["Dha","Dhin","Dhin","Dha","Dha","Dhin","Dhin","Dha",
            "Dha","Tin","Tin","Ta","Ta","Dhin","Dhin","Dha"]
}
```

**VibhagMarker JSON:** Sam and Khali are strings (`"sam"`, `"khali"`). Taali is an object: `{"taali": 2}`.

### 2.6 Raag

```scala
case class Raag(
  name: String,
  thaat: Option[String],
  arohana: Option[List[String]],
  avarohana: Option[List[String]],
  vadi: Option[String],
  samvadi: Option[String],
  pakad: Option[String],
  prahar: Option[Int]
)
```

**JSON (optional fields omitted when None):**

```json
{
  "name": "Yaman",
  "thaat": "Kalyan",
  "arohana": ["Sa", "Re", "Ga", "Ma\u266f", "Pa", "Dha", "Ni", "Sa'"],
  "avarohana": ["Sa'", "Ni", "Dha", "Pa", "Ma\u266f", "Ga", "Re", "Sa"],
  "vadi": "Ga",
  "samvadi": "Ni",
  "pakad": "Ni Re Ga, Re Sa",
  "prahar": 1
}
```

### 2.7 Section

```scala
case class Section(
  name: String,
  sectionType: SectionType,
  events: List[Event],
  tihai: Option[Tihai] = None
)

case class Tihai(
  startBeat: BeatPosition,
  landingBeat: BeatPosition
)
```

**JSON:**

```json
{
  "name": "Sthayi",
  "type": "sthayi",
  "events": [ /* ... Event objects ... */ ],
  "tihai": {
    "startBeat": {"cycle": 1, "beat": 12, "subdivision": [0, 1]},
    "landingBeat": {"cycle": 2, "beat": 0, "subdivision": [0, 1]}
  }
}
```

The `tihai` field is omitted when `None`.

### 2.8 Composition (Top-Level Document)

```scala
case class Composition(
  metadata: Metadata,
  sections: List[Section]
)

case class Metadata(
  title: String,
  compositionType: CompositionType,
  raag: Raag,
  taal: Taal,
  laya: Option[Laya],
  instrument: Option[String],
  composer: Option[String],
  author: Option[String],
  source: Option[String],
  showStrokeLine: Boolean = false,
  showSahityaLine: Boolean = false,
  createdAt: String,     // ISO-8601 instant
  updatedAt: String      // ISO-8601 instant
)
```

**JSON (.swar file format):**

```json
{
  "version": "1.0",
  "metadata": {
    "title": "Yaman Vilambit Gat",
    "compositionType": "gat",
    "raag": { /* Raag object */ },
    "taal": { /* Taal object */ },
    "laya": "vilambit",
    "instrument": "Sitar",
    "showStrokeLine": true,
    "showSahityaLine": false,
    "createdAt": "2026-03-28T10:30:00Z",
    "updatedAt": "2026-04-17T14:00:00Z"
  },
  "sections": [ /* Section objects */ ]
}
```

The `"version"` field is added at serialization time (not part of the `Composition` case class). Current version: `"1.0"`.

### 2.9 Editor State Types

These types represent client-side state that is sent to the backend with editor operations.

#### CursorModel

```scala
case class CursorModel(
  taal: Taal,
  cycle: Int = 0,
  beat: Int = 0,
  subIndex: Int = 0,
  totalSubdivisions: Int = 1,
  currentOctave: Octave = Octave.Madhya
)
```

**JSON:**

```json
{
  "taal": { /* Taal object */ },
  "cycle": 0,
  "beat": 3,
  "subIndex": 0,
  "totalSubdivisions": 2,
  "currentOctave": "madhya"
}
```

**Notes:**

- `position` is a derived property: `BeatPosition(cycle, beat, Rational(subIndex, totalSubdivisions))`
- `currentOctave` is a sticky modifier that resets to Madhya after each swar input

### 2.10 Layout Types

These types represent computed rendering data.

#### LayoutConfig

```scala
case class LayoutConfig(
  highDensityThreshold: Int = 5,
  cellWidthBase: Double = 60.0,
  cellOverflowExpand: Double = 15.0,
  lineSpacing: Double = 40.0,
  headerHeight: Double = 120.0
)
```

**JSON:**

```json
{
  "highDensityThreshold": 5,
  "cellWidthBase": 60.0,
  "cellOverflowExpand": 15.0,
  "lineSpacing": 40.0,
  "headerHeight": 120.0
}
```

#### SectionGrid / GridLine / BeatCell

```scala
case class SectionGrid(
  sectionName: String,
  sectionType: SectionType,
  lines: List[GridLine]
)

case class GridLine(
  cells: List[BeatCell],
  vibhagBreaks: List[Int],
  markers: List[(Int, VibhagMarker)]
)

case class BeatCell(
  position: CycleAndBeat,
  events: List[Event]
)

case class CycleAndBeat(cycle: Int, beat: Int)
```

**JSON:**

```json
{
  "sectionName": "Sthayi",
  "sectionType": "sthayi",
  "lines": [
    {
      "cells": [
        {
          "position": {"cycle": 0, "beat": 0},
          "events": [ /* Event objects */ ]
        }
      ],
      "vibhagBreaks": [4, 8, 12],
      "markers": [
        [0, "sam"],
        [4, {"taali": 2}],
        [8, "khali"],
        [12, {"taali": 3}]
      ]
    }
  ]
}
```

### 2.11 Audio Types

#### TimedNote

```scala
case class TimedNote(
  timeMs: Long,
  durationMs: Long,
  note: Note,
  variant: Variant,
  octave: Octave,
  stroke: Option[Stroke]
)
```

**JSON:**

```json
{
  "timeMs": 0,
  "durationMs": 1000,
  "note": "ga",
  "variant": "shuddha",
  "octave": "madhya",
  "stroke": "da"
}
```

---

## 3. API Operations

### 3.0 Common Response Envelope

All HTTP API responses use a consistent envelope.

**Success:**

```json
{
  "ok": true,
  "data": { /* operation-specific result */ }
}
```

**Error:**

```json
{
  "ok": false,
  "error": {
    "code": "INVALID_NOTE_VARIANT",
    "message": "Sa cannot have Komal variant"
  }
}
```

Library calls return `Either[ApiError, T]` where `ApiError` is defined in Section 4.

---

### 3.a Composition Operations

#### 3.a.1 Create New Composition

Creates a new empty composition with default sections based on composition type.

**Library:**

```scala
// Existing
object CompositionEditor:
  def create(
    title: String,
    compositionType: CompositionType,
    taal: Taal,
    raag: Raag,
    laya: Option[Laya],
    taanCount: Int = 0,
    showStrokeLine: Boolean = false,
    showSahityaLine: Boolean = false
  ): CompositionEditor
```

**NEW -- Library (pure, returns data without editor state):**

```scala
object CompositionApi:
  def createComposition(
    title: String,
    compositionType: CompositionType,
    taal: Taal,
    raag: Raag,
    laya: Option[Laya],
    taanCount: Int = 0,
    showStrokeLine: Boolean = false,
    showSahityaLine: Boolean = false
  ): (Composition, CursorModel)
```

**HTTP:**

```
POST /api/v1/compositions
```

**Request body:**

```json
{
  "title": "My Yaman Gat",
  "compositionType": "gat",
  "taal": { /* full Taal object, or use taalName for built-in */ },
  "taalName": "teentaal",
  "raag": { /* full Raag object, or use raagName for built-in */ },
  "raagName": "yaman",
  "laya": "vilambit",
  "taanCount": 2,
  "showStrokeLine": true,
  "showSahityaLine": false
}
```

**Notes:** Either `taal` or `taalName` must be provided (same for `raag`/`raagName`). If `taalName` is provided, the backend looks up the built-in taal.

**Response:**

```json
{
  "ok": true,
  "data": {
    "composition": { /* full Composition object */ },
    "cursor": { /* CursorModel object */ }
  }
}
```

**Default sections by composition type:**

| Type | Default sections |
|------|-----------------|
| `Gat` | "Gat" (Custom("Gat")), "Antara", plus N "Taan" sections |
| `Bandish` | "Sthayi" |
| `Palta` | "Palta" |

**Error cases:**

- `INVALID_TAAL`: taalName not found in built-in taals
- `INVALID_RAAG`: raagName not found in built-in raags
- `MISSING_FIELD`: required field missing

#### 3.a.2 Parse Composition JSON

Validates and deserializes a `.swar` JSON string into a `Composition`.

**Library:**

```scala
// Existing
object SwarFormat:
  def fromJson(jsonString: String): Either[io.circe.Error, Composition]
```

**NEW -- Library:**

```scala
object CompositionApi:
  def parseComposition(json: String): Either[ApiError, Composition]
```

**HTTP:**

```
POST /api/v1/compositions/parse
Content-Type: application/json

// Request body is the raw .swar JSON string (the entire file content)
```

**Response (success):**

```json
{
  "ok": true,
  "data": {
    "composition": { /* Composition object */ }
  }
}
```

**Error cases:**

- `PARSE_ERROR`: malformed JSON
- `VERSION_ERROR`: unsupported .swar version (currently only "1.0")
- `VALIDATION_ERROR`: structurally valid JSON but semantically invalid

#### 3.a.3 Serialize Composition to JSON

Serializes a `Composition` to the `.swar` JSON format string.

**Library:**

```scala
// Existing
object SwarFormat:
  def toJson(composition: Composition): io.circe.Json
```

**NEW -- Library:**

```scala
object CompositionApi:
  def serializeComposition(composition: Composition): String
```

**HTTP:**

```
POST /api/v1/compositions/serialize
```

**Request body:**

```json
{
  "composition": { /* Composition object */ }
}
```

**Response:**

```json
{
  "ok": true,
  "data": {
    "swarJson": "{ \"version\": \"1.0\", \"metadata\": { ... }, \"sections\": [...] }"
  }
}
```

---

### 3.b Editor Operations

These are the core operations. Every editor operation takes the current composition + cursor state as input and returns the modified composition + updated cursor + a status message.

**Common input/output pattern:**

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

**HTTP common pattern:**

```
POST /api/v1/editor/<operation>

// Request body always includes:
{
  "composition": { /* Composition */ },
  "sectionIndex": 0,
  "cursor": { /* CursorModel */ },
  // ... operation-specific fields ...
}

// Response:
{
  "ok": true,
  "data": {
    "composition": { /* modified Composition */ },
    "cursor": { /* updated CursorModel */ },
    "message": "Ga (taar)"
  }
}
```

#### 3.b.1 Insert Swar

Insert a note at the current cursor position.

**Library (existing logic in `KeyHandler.handleSwarKey`):**

```scala
// NEW
object EditorApi:
  def insertSwar(
    input: EditorInput,
    note: Note,
    shiftDown: Boolean
  ): Either[ApiError, EditorResult]
```

**HTTP:**

```
POST /api/v1/editor/insert-swar
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "sectionIndex": 0,
  "cursor": { /* ... */ },
  "note": "ga",
  "shiftDown": false
}
```

**Behavior:**

1. Resolves variant from note + shiftDown flag:
   - shiftDown + Ma = Tivra
   - shiftDown + Sa/Pa = Shuddha (no effect)
   - shiftDown + Re/Ga/Dha/Ni = Komal
   - no shift = Shuddha
2. Uses `cursor.currentOctave` for the octave
3. Creates `Event.Swar` at `cursor.position` with duration `Rational(1, cursor.totalSubdivisions)`
4. Appends event to current section's event list
5. Advances cursor via `nextSubBeat` and resets octave to Madhya

**Response message format:** `"Ga"`, `"Re komal"`, `"Ma tivra"`, `"Ga (taar)"`, `"Re komal (mandra)"`

**Error cases:**

- `INVALID_SECTION_INDEX`: sectionIndex out of bounds

#### 3.b.2 Insert Rest

Insert a silence/rest at the current cursor position.

**Library:**

```scala
// NEW
object EditorApi:
  def insertRest(input: EditorInput): Either[ApiError, EditorResult]
```

**HTTP:**

```
POST /api/v1/editor/insert-rest
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "sectionIndex": 0,
  "cursor": { /* ... */ }
}
```

**Behavior:**

1. Creates `Event.Rest` at `cursor.position` with `Rational.fullBeat` duration
2. Advances cursor via `nextBeat`

**Response message:** `"Rest (silence)"`

#### 3.b.3 Insert Sustain

Insert a sustain (hold previous note) at the current cursor position.

**Library:**

```scala
// NEW
object EditorApi:
  def insertSustain(input: EditorInput): Either[ApiError, EditorResult]
```

**HTTP:**

```
POST /api/v1/editor/insert-sustain
```

**Behavior:**

1. Creates `Event.Sustain` at `cursor.position` with `Rational.fullBeat` duration
2. Advances cursor via `nextBeat`

**Response message:** `"Sustain (hold previous note)"`

#### 3.b.4 Delete Last Event

Remove the last event from the current section.

**Library:**

```scala
// NEW
object EditorApi:
  def deleteLastEvent(input: EditorInput): Either[ApiError, EditorResult]
```

**HTTP:**

```
POST /api/v1/editor/delete-last
```

**Behavior:**

1. Removes the last event from the current section's event list
2. Moves cursor back via `prevBeat`

**Error cases:**

- `EMPTY_SECTION`: no events to delete

**Response message:** `"Deleted last note"` or error `"Nothing to delete"`

#### 3.b.5 Insert Dual Swar

Insert two identical notes on the same beat, each with half duration. Used for double-tap shortcuts (ss, rr, gg, etc.).

**Library (existing logic in `KeyHandler.handleDualSwar`):**

```scala
// NEW
object EditorApi:
  def insertDualSwar(
    input: EditorInput,
    note: Note,
    shiftDown: Boolean
  ): Either[ApiError, EditorResult]
```

**HTTP:**

```
POST /api/v1/editor/insert-dual-swar
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "sectionIndex": 0,
  "cursor": { /* ... */ },
  "note": "sa",
  "shiftDown": false
}
```

**Behavior:**

1. Creates two `Event.Swar` events on the same beat:
   - Event 1: subdivision `[0, 2]`, duration `[1, 2]`
   - Event 2: subdivision `[1, 2]`, duration `[1, 2]`
2. Appends both events to the section
3. Advances cursor via `nextBeat` and resets octave to Madhya

**Response message:** `"SaSa (dual swar)"`, `"ReRe (dual swar)"`, etc.

---

### 3.c Cursor Operations

Cursor operations are pure computations on the `CursorModel`. They do not modify the composition.

**Library:**

```scala
// These already exist as methods on CursorModel
// NEW API wrapper:
object CursorApi:
  def nextBeat(cursor: CursorModel): CursorModel
  def prevBeat(cursor: CursorModel): CursorModel
  def nextSubBeat(cursor: CursorModel): CursorModel
  def setSubdivisions(cursor: CursorModel, n: Int): CursorModel
  def setOctave(cursor: CursorModel, octave: Octave): CursorModel
  def moveTo(cursor: CursorModel, cycle: Int, beat: Int): CursorModel  // NEW
```

**HTTP:**

```
POST /api/v1/cursor/next-beat
POST /api/v1/cursor/prev-beat
POST /api/v1/cursor/next-sub-beat
POST /api/v1/cursor/set-subdivisions
POST /api/v1/cursor/set-octave
POST /api/v1/cursor/move-to
```

**Example -- next-beat:**

```
POST /api/v1/cursor/next-beat

{
  "cursor": {
    "taal": { "name": "Teentaal", "matras": 16, "vibhags": [...] },
    "cycle": 0,
    "beat": 15,
    "subIndex": 0,
    "totalSubdivisions": 1,
    "currentOctave": "madhya"
  }
}
```

**Response:**

```json
{
  "ok": true,
  "data": {
    "cursor": {
      "taal": { /* same taal */ },
      "cycle": 1,
      "beat": 0,
      "subIndex": 0,
      "totalSubdivisions": 1,
      "currentOctave": "madhya"
    }
  }
}
```

**nextBeat behavior:** When beat reaches matras, cycle increments and beat resets to 0. Subdivisions reset to 1.

**prevBeat behavior:** When beat goes below 0 and cycle > 0, cycle decrements and beat goes to matras-1. At cycle 0, beat 0, cursor does not move.

**nextSubBeat behavior:** Increments subIndex. If subIndex reaches totalSubdivisions, acts as nextBeat.

**setSubdivisions(n):** Sets totalSubdivisions to n, resets subIndex to 0.

**setOctave:** Sets currentOctave for the next note input. Resets to Madhya after each swar insertion.

**moveTo(cycle, beat):** NEW -- Sets cursor to specific cycle and beat, resets subIndex to 0.

---

### 3.d Section Operations

#### 3.d.1 Add Section

**Library (existing via list manipulation; NEW explicit API):**

```scala
// NEW
object SectionApi:
  def addSection(
    composition: Composition,
    name: String,
    sectionType: SectionType,
    insertAt: Option[Int] = None  // None = append at end
  ): Composition
```

**HTTP:**

```
POST /api/v1/sections/add
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "name": "Taan 3",
  "sectionType": "taan",
  "insertAt": 4
}
```

**Response:**

```json
{
  "ok": true,
  "data": {
    "composition": { /* updated Composition with new section */ }
  }
}
```

#### 3.d.2 Remove Section

**Library (existing `CompositionEditor.removeSection`):**

```scala
// NEW
object SectionApi:
  def removeSection(
    composition: Composition,
    sectionIndex: Int
  ): Either[ApiError, Composition]
```

**HTTP:**

```
POST /api/v1/sections/remove
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "sectionIndex": 2
}
```

**Error cases:**

- `LAST_SECTION`: cannot remove the only remaining section
- `INVALID_SECTION_INDEX`: index out of bounds

#### 3.d.3 Rename Section

**Library (existing `CompositionEditor.renameSection`):**

```scala
// NEW
object SectionApi:
  def renameSection(
    composition: Composition,
    sectionIndex: Int,
    newName: String
  ): Either[ApiError, Composition]
```

**HTTP:**

```
POST /api/v1/sections/rename
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "sectionIndex": 0,
  "newName": "Masitkhani Gat"
}
```

#### 3.d.4 Reorder Sections

Move a section from one index to another.

**Library (existing `CompositionEditor.moveSection`):**

```scala
// NEW
object SectionApi:
  def moveSection(
    composition: Composition,
    fromIndex: Int,
    toIndex: Int
  ): Either[ApiError, Composition]
```

**HTTP:**

```
POST /api/v1/sections/reorder
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "fromIndex": 3,
  "toIndex": 1
}
```

**Error cases:**

- `INVALID_SECTION_INDEX`: either index out of bounds

---

### 3.e Ornament Operations

All ornament operations attach an ornament to the last `Event.Swar` in the current section.

**Common HTTP pattern:**

```
POST /api/v1/editor/ornament/<type>
```

**Common response:**

```json
{
  "ok": true,
  "data": {
    "composition": { /* modified */ },
    "message": "Gamak added"
  }
}
```

#### 3.e.1 Add Simple Ornament (no parameters)

For Gamak, Andolan, Gitkari.

**Library (existing `KeyHandler.handleSimpleOrnament`):**

```scala
// NEW
object OrnamentApi:
  def addSimpleOrnament(
    composition: Composition,
    sectionIndex: Int,
    ornamentType: String  // "gamak" | "andolan" | "gitkari"
  ): Either[ApiError, (Composition, String)]
```

**HTTP:**

```
POST /api/v1/editor/ornament/simple
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "sectionIndex": 0,
  "ornamentType": "gamak"
}
```

**Error cases:**

- `NO_SWAR_TARGET`: no Swar event in the section to attach the ornament to
- `INVALID_ORNAMENT_TYPE`: unrecognized ornament type string

#### 3.e.2 Add Single-Note Ornament

For KanSwar, Sparsh, Ghaseet.

**Library:**

```scala
// NEW
object OrnamentApi:
  def addSingleNoteOrnament(
    composition: Composition,
    sectionIndex: Int,
    ornamentType: String,  // "kanSwar" | "sparsh" | "ghaseet"
    noteRef: NoteRef
  ): Either[ApiError, (Composition, String)]
```

**HTTP:**

```
POST /api/v1/editor/ornament/single-note
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "sectionIndex": 0,
  "ornamentType": "kanSwar",
  "noteRef": {"note": "ni", "variant": "shuddha", "octave": "madhya"}
}
```

#### 3.e.3 Add Two-Note Ornament

For Meend, Krintan.

**Library:**

```scala
// NEW
object OrnamentApi:
  def addMeend(
    composition: Composition,
    sectionIndex: Int,
    startNote: NoteRef,
    endNote: NoteRef,
    direction: MeendDirection,
    intermediateNotes: List[NoteRef] = Nil
  ): Either[ApiError, (Composition, String)]

  def addKrintan(
    composition: Composition,
    sectionIndex: Int,
    notes: List[NoteRef]  // at least 2 notes
  ): Either[ApiError, (Composition, String)]
```

**HTTP:**

```
POST /api/v1/editor/ornament/meend
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "sectionIndex": 0,
  "startNote": {"note": "pa", "variant": "shuddha", "octave": "madhya"},
  "endNote": {"note": "sa", "variant": "shuddha", "octave": "taar"},
  "direction": "ascending",
  "intermediateNotes": []
}
```

```
POST /api/v1/editor/ornament/krintan
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "sectionIndex": 0,
  "notes": [
    {"note": "ga", "variant": "shuddha", "octave": "madhya"},
    {"note": "re", "variant": "shuddha", "octave": "madhya"}
  ]
}
```

#### 3.e.4 Add Multi-Note Ornament

For Murki, Zamzama.

**Library:**

```scala
// NEW
object OrnamentApi:
  def addMurki(
    composition: Composition,
    sectionIndex: Int,
    notes: List[NoteRef]  // 3-5 notes typical
  ): Either[ApiError, (Composition, String)]

  def addZamzama(
    composition: Composition,
    sectionIndex: Int,
    notes: List[NoteRef]
  ): Either[ApiError, (Composition, String)]
```

**HTTP:**

```
POST /api/v1/editor/ornament/murki
POST /api/v1/editor/ornament/zamzama
```

**Request body (same pattern for both):**

```json
{
  "composition": { /* ... */ },
  "sectionIndex": 0,
  "notes": [
    {"note": "ga", "variant": "shuddha", "octave": "madhya"},
    {"note": "re", "variant": "shuddha", "octave": "madhya"},
    {"note": "sa", "variant": "shuddha", "octave": "madhya"}
  ]
}
```

**Error cases (all ornament operations):**

- `NO_SWAR_TARGET`: no Swar event in the section to attach the ornament to
- `EMPTY_NOTES`: notes list is empty (for multi-note ornaments)
- `INSUFFICIENT_NOTES`: fewer than minimum required notes

---

### 3.f Stroke Operations

#### 3.f.1 Set Stroke

Set the mizrab stroke on a swar event at the cursor position.

**Library (existing `CompositionEditor.setStrokeAt`):**

```scala
// NEW
object StrokeApi:
  def setStroke(
    composition: Composition,
    sectionIndex: Int,
    cursor: CursorModel,
    stroke: Stroke
  ): Either[ApiError, (Composition, String)]
```

**HTTP:**

```
POST /api/v1/editor/stroke/set
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "sectionIndex": 0,
  "cursor": { /* CursorModel -- used for position + subIndex */ },
  "stroke": "ra"
}
```

**Behavior:**

- Finds Swar events at the cursor's (cycle, beat) position
- Uses `cursor.subIndex` to pick which swar if multiple exist on the same beat
- Sets `stroke = Some(stroke)` on the target event

**Error cases:**

- `NO_SWAR_AT_POSITION`: no swar event at the cursor position

#### 3.f.2 Clear Stroke

Remove the explicit stroke from a swar event (reverts to auto Da/Ra alternation).

**Library (existing `CompositionEditor.clearStrokeAt`):**

```scala
// NEW
object StrokeApi:
  def clearStroke(
    composition: Composition,
    sectionIndex: Int,
    cursor: CursorModel
  ): Either[ApiError, (Composition, String)]
```

**HTTP:**

```
POST /api/v1/editor/stroke/clear
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "sectionIndex": 0,
  "cursor": { /* CursorModel */ }
}
```

---

### 3.g Layout Operations

#### 3.g.1 Compute Grid Layout

The key rendering computation. Takes a composition and layout config, returns grid data for each section.

**Library (existing `GridLayout.layoutAll`):**

```scala
// Existing
object GridLayout:
  def layoutAll(composition: Composition, config: LayoutConfig): List[SectionGrid]
  def layout(section: Section, taal: Taal, config: LayoutConfig): SectionGrid
```

**NEW -- Library:**

```scala
object LayoutApi:
  def computeLayout(
    composition: Composition,
    config: LayoutConfig = LayoutConfig()
  ): List[SectionGrid]

  def computeSectionLayout(
    section: Section,
    taal: Taal,
    config: LayoutConfig = LayoutConfig()
  ): SectionGrid
```

**HTTP:**

```
POST /api/v1/layout/compute
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "config": {
    "highDensityThreshold": 5,
    "cellWidthBase": 60.0,
    "cellOverflowExpand": 15.0,
    "lineSpacing": 40.0,
    "headerHeight": 120.0
  }
}
```

Config is optional; defaults are used if omitted.

**Response:**

```json
{
  "ok": true,
  "data": {
    "grids": [
      {
        "sectionName": "Sthayi",
        "sectionType": "sthayi",
        "lines": [
          {
            "cells": [
              {
                "position": {"cycle": 0, "beat": 0},
                "events": [
                  {
                    "type": "swar",
                    "note": "ga",
                    "variant": "shuddha",
                    "octave": "madhya",
                    "beat": {"cycle": 0, "beat": 0, "subdivision": [0, 1]},
                    "duration": [1, 1],
                    "ornaments": []
                  }
                ]
              }
            ],
            "vibhagBreaks": [4, 8, 12],
            "markers": [[0, "sam"], [4, {"taali": 2}], [8, "khali"], [12, {"taali": 3}]]
          }
        ]
      }
    ]
  }
}
```

**Layout pipeline:**

1. **BeatGrouper**: Groups events by (cycle, beat) into `BeatCell` list
2. **LineBreaker**: Decides line breaks based on density vs. highDensityThreshold
   - Low density: full taal cycle on one line
   - High density (notes/beat >= threshold): split by vibhag
3. **GridLayout**: Orchestrates the above, produces `SectionGrid`

---

### 3.h Glyph/Rendering Data

#### 3.h.1 Get Note Glyph

Returns the display text and decoration information for a note in a given script.

**Library (existing `ScriptMap.glyph`, `DevanagariMap`):**

```scala
// NEW
object GlyphApi:
  case class GlyphInfo(
    text: String,              // e.g., "रे" for Devanagari Re
    needsKomalMark: Boolean,   // underline decoration
    needsTivraMark: Boolean,   // vertical stroke above
    dotCount: Int,             // 0, 1, or 2 octave dots
    dotPosition: DotPosition   // Above, Below, or None
  )

  def noteGlyph(
    note: Note,
    variant: Variant,
    octave: Octave,
    script: SwarScript
  ): GlyphInfo
```

**HTTP:**

```
POST /api/v1/rendering/glyph
```

**Request body:**

```json
{
  "note": "re",
  "variant": "komal",
  "octave": "mandra",
  "script": "devanagari"
}
```

**Response:**

```json
{
  "ok": true,
  "data": {
    "text": "\u0930\u0947",
    "needsKomalMark": true,
    "needsTivraMark": false,
    "dotCount": 1,
    "dotPosition": "below"
  }
}
```

#### 3.h.2 Get Notation Colors

Returns the full color palette used for notation rendering.

**Library (existing `NotationColors` object):**

```scala
// NEW
object GlyphApi:
  case class ColorPalette(
    taalMarker: String,       // "#B71C1C"
    taalMarkerSam: String,    // "#D32F2F"
    swar: String,             // "#1A237E"
    octaveDot: String,        // "#E65100"
    ornament: String,         // "#4A148C"
    stroke: String,           // "#00695C"
    sahitya: String,          // "#2E7D32"
    rest: String,             // "#616161"
    sustain: String,          // "#9E9E9E"
    komalMark: String,        // "#1A237E"
    tivraMark: String         // "#1A237E"
  )

  def notationColors: ColorPalette
```

**HTTP:**

```
GET /api/v1/rendering/colors
```

**Response:**

```json
{
  "ok": true,
  "data": {
    "taalMarker": "#B71C1C",
    "taalMarkerSam": "#D32F2F",
    "swar": "#1A237E",
    "octaveDot": "#E65100",
    "ornament": "#4A148C",
    "stroke": "#00695C",
    "sahitya": "#2E7D32",
    "rest": "#616161",
    "sustain": "#9E9E9E",
    "komalMark": "#1A237E",
    "tivraMark": "#1A237E"
  }
}
```

#### 3.h.3 Get Script Mappings

Returns note-to-glyph mappings for all scripts.

**Library:**

```scala
// NEW
object GlyphApi:
  case class ScriptMapping(
    script: SwarScript,
    displayName: String,         // "Devanagari (Hindi)"
    fontName: String,            // "Noto Sans Devanagari"
    glyphs: Map[Note, String],   // Sa -> "सा", Re -> "रे", ...
    restSymbol: String,          // "-"
    sustainSymbol: String,       // "\u2014"
    strokeTexts: Map[Stroke, String]  // Da -> "दा", Ra -> "रा", ...
  )

  def allScriptMappings: List[ScriptMapping]
```

**HTTP:**

```
GET /api/v1/rendering/scripts
```

**Response:**

```json
{
  "ok": true,
  "data": {
    "scripts": [
      {
        "script": "devanagari",
        "displayName": "Devanagari (Hindi)",
        "fontName": "Noto Sans Devanagari",
        "glyphs": {
          "sa": "\u0938\u093e",
          "re": "\u0930\u0947",
          "ga": "\u0917",
          "ma": "\u092e",
          "pa": "\u092a",
          "dha": "\u0927",
          "ni": "\u0928\u093f"
        },
        "restSymbol": "-",
        "sustainSymbol": "\u2014",
        "strokeTexts": {
          "da": "\u0926\u093e",
          "ra": "\u0930\u093e",
          "chikari": "\u091a\u0940",
          "jod": "\u091c\u094b"
        }
      },
      {
        "script": "kannada",
        "displayName": "Kannada",
        "fontName": "Noto Sans Kannada",
        "glyphs": { /* ... */ },
        "restSymbol": "-",
        "sustainSymbol": "\u2014",
        "strokeTexts": { /* ... */ }
      }
    ]
  }
}
```

---

### 3.i Export Operations

#### 3.i.1 Export to PDF

Generates a PDF document from a composition.

**Library (existing `PdfExport`):**

```scala
// Existing -- writes to file
object PdfExport:
  // (internal, writes to disk)

// NEW -- returns bytes
object ExportApi:
  def exportPdf(
    composition: Composition,
    script: SwarScript = SwarScript.Devanagari
  ): Either[ApiError, Array[Byte]]
```

**HTTP:**

```
POST /api/v1/export/pdf
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "script": "devanagari"
}
```

**Response:** Binary PDF data with `Content-Type: application/pdf`. The response is NOT JSON-wrapped -- it is raw bytes.

**Error cases:**

- `EXPORT_ERROR`: font loading failure or rendering error

#### 3.i.2 Export to HTML

Generates a standalone HTML document from a composition.

**Library (existing `HtmlExport.render`):**

```scala
// Existing
object HtmlExport:
  def render(composition: Composition): String

// NEW
object ExportApi:
  def exportHtml(
    composition: Composition,
    script: SwarScript = SwarScript.Devanagari
  ): String
```

**HTTP:**

```
POST /api/v1/export/html
```

**Request body:**

```json
{
  "composition": { /* ... */ },
  "script": "devanagari"
}
```

**Response:**

```json
{
  "ok": true,
  "data": {
    "html": "<!DOCTYPE html><html>..."
  }
}
```

---

### 3.j Playback Operations

#### 3.j.1 Schedule Playback

Converts a list of events into timed notes for audio playback.

**Library (existing `PlaybackScheduler.schedule`):**

```scala
// Existing
object PlaybackScheduler:
  def schedule(events: List[Event], bpm: Double, matras: Int): List[TimedNote]

// NEW
object PlaybackApi:
  def schedulePlayback(
    events: List[Event],
    bpm: Double,
    matras: Int
  ): List[TimedNote]
```

**HTTP:**

```
POST /api/v1/playback/schedule
```

**Request body:**

```json
{
  "events": [ /* Event objects */ ],
  "bpm": 60.0,
  "matras": 16
}
```

**Response:**

```json
{
  "ok": true,
  "data": {
    "timedNotes": [
      {"timeMs": 0, "durationMs": 1000, "note": "ga", "variant": "shuddha", "octave": "madhya", "stroke": "da"},
      {"timeMs": 1000, "durationMs": 1000, "note": "ma", "variant": "shuddha", "octave": "madhya"},
      {"timeMs": 2000, "durationMs": 500, "note": "pa", "variant": "shuddha", "octave": "madhya", "stroke": "ra"}
    ]
  }
}
```

**Timing calculation:**

- `msPerBeat = 60000.0 / bpm`
- `beatOffset = event.beat.cycle * matras + event.beat.beat`
- `subOffset = event.beat.subdivision.toDouble`
- `timeMs = (beatOffset + subOffset) * msPerBeat`
- `durationMs = event.duration.toDouble * msPerBeat`

Only `Event.Swar` events produce `TimedNote` entries. Rest and Sustain are filtered out.

---

### 3.k Reference Data

#### 3.k.1 List All Taals

**Library (existing `Taals.all`):**

```scala
// Existing
object Taals:
  val all: Map[String, Taal]
  def byName(name: String): Option[Taal]

// NEW
object ReferenceApi:
  def allTaals: List[Taal]
  def taalByName(name: String): Either[ApiError, Taal]
```

**HTTP:**

```
GET /api/v1/taals
```

**Response:**

```json
{
  "ok": true,
  "data": {
    "taals": [
      {
        "name": "Teentaal",
        "matras": 16,
        "vibhags": [
          {"beats": 4, "marker": "sam"},
          {"beats": 4, "marker": {"taali": 2}},
          {"beats": 4, "marker": "khali"},
          {"beats": 4, "marker": {"taali": 3}}
        ],
        "theka": ["Dha","Dhin","Dhin","Dha","Dha","Dhin","Dhin","Dha",
                  "Dha","Tin","Tin","Ta","Ta","Dhin","Dhin","Dha"]
      }
    ]
  }
}
```

**Built-in taals (11):** Teentaal (16), Ektaal (12), Jhaptaal (10), Rupak (7), Dadra (6), Keherwa (8), Chautaal (12), Dhamar (14), Tilwada (16), Jhoomra (14), Deepchandi (14)

#### 3.k.2 Get Taal by Name

**HTTP:**

```
GET /api/v1/taals/:name
```

**Example:** `GET /api/v1/taals/teentaal`

**Error cases:**

- `NOT_FOUND`: no taal with that name (case-insensitive lookup)

#### 3.k.3 List All Raags

**Library (existing `Raags`):**

```scala
// NEW
object ReferenceApi:
  def allRaags: List[Raag]
  def raagByName(name: String): Either[ApiError, Raag]
```

**HTTP:**

```
GET /api/v1/raags
```

**Response:**

```json
{
  "ok": true,
  "data": {
    "raags": [
      {
        "name": "Yaman",
        "thaat": "Kalyan",
        "arohana": ["Sa", "Re", "Ga", "Ma\u266f", "Pa", "Dha", "Ni", "Sa'"],
        "avarohana": ["Sa'", "Ni", "Dha", "Pa", "Ma\u266f", "Ga", "Re", "Sa"],
        "vadi": "Ga",
        "samvadi": "Ni",
        "pakad": "Ni Re Ga, Re Sa",
        "prahar": 1
      }
    ]
  }
}
```

**Built-in raags (26):** Yaman, Bhairav, Durga, Bhupali, Malkauns, Bageshree, Desh, Kafi, Bihag, and 17 others.

#### 3.k.4 Get Raag by Name

**HTTP:**

```
GET /api/v1/raags/:name
```

**Example:** `GET /api/v1/raags/yaman`

**Error cases:**

- `NOT_FOUND`: no raag with that name (case-insensitive lookup)

---

## 4. Error Model

### 4.1 ApiError Type

```scala
// NEW
enum ApiError(val code: String, val message: String):
  // Validation errors
  case InvalidNoteVariant(note: Note, variant: Variant)
    extends ApiError("INVALID_NOTE_VARIANT", s"$note cannot have $variant variant")
  case InvalidSectionIndex(index: Int, size: Int)
    extends ApiError("INVALID_SECTION_INDEX", s"Section index $index out of bounds (0..${size - 1})")
  case LastSection
    extends ApiError("LAST_SECTION", "Cannot remove the only remaining section")
  case EmptySection
    extends ApiError("EMPTY_SECTION", "Section has no events")
  case NoSwarTarget
    extends ApiError("NO_SWAR_TARGET", "No swar event found to attach to")
  case NoSwarAtPosition(cycle: Int, beat: Int)
    extends ApiError("NO_SWAR_AT_POSITION", s"No swar at cycle $cycle, beat $beat")
  case EmptyNotes
    extends ApiError("EMPTY_NOTES", "Notes list cannot be empty")
  case InsufficientNotes(required: Int, actual: Int)
    extends ApiError("INSUFFICIENT_NOTES", s"At least $required notes required, got $actual")
  case InvalidOrnamentType(typeName: String)
    extends ApiError("INVALID_ORNAMENT_TYPE", s"Unknown ornament type: $typeName")

  // Format errors
  case ParseError(detail: String)
    extends ApiError("PARSE_ERROR", s"JSON parse error: $detail")
  case VersionError(version: String)
    extends ApiError("VERSION_ERROR", s"Unsupported .swar version: $version")
  case ValidationError(detail: String)
    extends ApiError("VALIDATION_ERROR", detail)

  // Reference data errors
  case NotFound(entity: String, name: String)
    extends ApiError("NOT_FOUND", s"$entity '$name' not found")

  // Export errors
  case ExportError(detail: String)
    extends ApiError("EXPORT_ERROR", detail)

  // Generic
  case MissingField(field: String)
    extends ApiError("MISSING_FIELD", s"Required field '$field' is missing")
```

### 4.2 HTTP Error Codes

| ApiError code | HTTP status |
|---------------|-------------|
| `INVALID_NOTE_VARIANT` | 400 |
| `INVALID_SECTION_INDEX` | 400 |
| `LAST_SECTION` | 400 |
| `EMPTY_SECTION` | 400 |
| `NO_SWAR_TARGET` | 400 |
| `NO_SWAR_AT_POSITION` | 400 |
| `EMPTY_NOTES` | 400 |
| `INSUFFICIENT_NOTES` | 400 |
| `INVALID_ORNAMENT_TYPE` | 400 |
| `PARSE_ERROR` | 400 |
| `VERSION_ERROR` | 400 |
| `VALIDATION_ERROR` | 422 |
| `NOT_FOUND` | 404 |
| `EXPORT_ERROR` | 500 |
| `MISSING_FIELD` | 400 |

### 4.3 HTTP Error Response Format

```json
{
  "ok": false,
  "error": {
    "code": "INVALID_SECTION_INDEX",
    "message": "Section index 5 out of bounds (0..2)"
  }
}
```

---

## 5. HTTP API Details

### 5.1 Base Configuration

| Setting | Value |
|---------|-------|
| Base path | `/api/v1` |
| Content-Type (request) | `application/json` (except noted) |
| Content-Type (response) | `application/json` (except PDF export) |
| Character encoding | UTF-8 |
| Max request body | 10 MB |

### 5.2 CORS Headers

For web deployment, the server should return:

```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, OPTIONS
Access-Control-Allow-Headers: Content-Type
Access-Control-Max-Age: 86400
```

Production deployments should restrict `Access-Control-Allow-Origin` to the specific frontend domain.

### 5.3 Endpoint Summary

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/compositions` | Create new composition |
| `POST` | `/compositions/parse` | Parse .swar JSON |
| `POST` | `/compositions/serialize` | Serialize to .swar JSON |
| `POST` | `/editor/insert-swar` | Insert a note |
| `POST` | `/editor/insert-rest` | Insert rest |
| `POST` | `/editor/insert-sustain` | Insert sustain |
| `POST` | `/editor/delete-last` | Delete last event |
| `POST` | `/editor/insert-dual-swar` | Insert dual swar |
| `POST` | `/cursor/next-beat` | Move cursor forward |
| `POST` | `/cursor/prev-beat` | Move cursor backward |
| `POST` | `/cursor/next-sub-beat` | Move to next sub-beat |
| `POST` | `/cursor/set-subdivisions` | Set subdivision count |
| `POST` | `/cursor/set-octave` | Set current octave |
| `POST` | `/cursor/move-to` | Move to specific position |
| `POST` | `/sections/add` | Add section |
| `POST` | `/sections/remove` | Remove section |
| `POST` | `/sections/rename` | Rename section |
| `POST` | `/sections/reorder` | Reorder sections |
| `POST` | `/editor/ornament/simple` | Add simple ornament |
| `POST` | `/editor/ornament/single-note` | Add single-note ornament |
| `POST` | `/editor/ornament/meend` | Add meend |
| `POST` | `/editor/ornament/krintan` | Add krintan |
| `POST` | `/editor/ornament/murki` | Add murki |
| `POST` | `/editor/ornament/zamzama` | Add zamzama |
| `POST` | `/editor/stroke/set` | Set stroke |
| `POST` | `/editor/stroke/clear` | Clear stroke |
| `POST` | `/layout/compute` | Compute grid layout |
| `POST` | `/rendering/glyph` | Get note glyph info |
| `GET` | `/rendering/colors` | Get color palette |
| `GET` | `/rendering/scripts` | Get all script mappings |
| `POST` | `/export/pdf` | Export to PDF |
| `POST` | `/export/html` | Export to HTML |
| `POST` | `/playback/schedule` | Schedule playback |
| `GET` | `/taals` | List all taals |
| `GET` | `/taals/:name` | Get taal by name |
| `GET` | `/raags` | List all raags |
| `GET` | `/raags/:name` | Get raag by name |

### 5.4 OpenAPI Compatibility

The API is designed to be described by an OpenAPI 3.0 specification. Key notes:

- All POST bodies are `application/json` (except raw .swar content in `/compositions/parse`)
- All responses are `application/json` (except `/export/pdf` which returns `application/pdf`)
- Discriminated unions (Event, Ornament, CompositionType, SectionType, VibhagMarker) use the `"type"` or key-based discrimination documented in Section 2
- Enum types map to `string` with `enum` constraints in OpenAPI

### 5.5 Versioning

The HTTP API is versioned in the URL path (`/api/v1`). Breaking changes require a new version (`/api/v2`). Non-breaking additions (new fields with defaults, new endpoints) can be added to the existing version.

---

## 6. Library API Details

### 6.1 Package Structure

```
sangeet.core/
  CompositionApi    -- create, parse, serialize
  EditorApi         -- insert swar, rest, sustain, delete, dual swar
  CursorApi         -- cursor movement operations
  SectionApi        -- add, remove, rename, reorder sections
  OrnamentApi       -- attach ornaments to swar events
  StrokeApi         -- set/clear mizrab strokes
  LayoutApi         -- compute grid layout
  GlyphApi          -- note glyph rendering data, colors, scripts
  ExportApi         -- PDF and HTML export
  PlaybackApi       -- playback scheduling
  ReferenceApi      -- built-in taals and raags
  ApiError          -- error ADT
```

All API objects reside in `sangeet.core`. They depend on `sangeet.model` types and delegate to existing implementation in `sangeet.format`, `sangeet.layout`, `sangeet.render`, and `sangeet.audio`.

### 6.2 Thread Safety

- All API functions are **pure** (no shared mutable state, no side effects)
- The `DevanagariMap._script` mutable variable is the one exception in the existing codebase. For the library API, script is passed as an explicit parameter rather than relying on global mutable state
- Multiple threads can call any API function concurrently without synchronization
- `ExportApi.exportPdf` may allocate and close a `PDDocument` internally but does not leak resources

### 6.3 No Mutable Shared State

The library API enforces the stateless contract:

- No singleton state, no caches, no registries
- Every operation takes all required data as parameters and returns the complete result
- The client is responsible for threading the `Composition` and `CursorModel` through successive operations
- Undo/redo is entirely client-side (snapshot the `Composition` before each mutation)

### 6.4 Usage Example (Desktop / Android)

```scala
import sangeet.core.*
import sangeet.model.*
import sangeet.taal.Taals
import sangeet.raag.Raags

// Create a new composition
val (composition, cursor) = CompositionApi.createComposition(
  title = "My Yaman Gat",
  compositionType = CompositionType.Gat,
  taal = Taals.teentaal,
  raag = Raags.yaman,
  laya = Some(Laya.Vilambit),
  taanCount = 2,
  showStrokeLine = true
)

// Insert a note
val input = EditorInput(composition, sectionIndex = 0, cursor)
val Right(result) = EditorApi.insertSwar(input, Note.Ga, shiftDown = false)
// result.composition -- updated composition
// result.cursor      -- cursor moved to next position
// result.message     -- "Ga"

// Compute layout for rendering
val grids = LayoutApi.computeLayout(result.composition)

// Export to PDF
val Right(pdfBytes) = ExportApi.exportPdf(result.composition, SwarScript.Devanagari)

// Schedule playback
val section = result.composition.sections.head
val timedNotes = PlaybackApi.schedulePlayback(section.events, bpm = 60.0, matras = 16)
```

---

## Appendix A: Variant Resolution Rules

The variant (shuddha/komal/tivra) is resolved from the note + shiftDown flag:

| Note | shiftDown = false | shiftDown = true |
|------|-------------------|------------------|
| Sa | Shuddha | Shuddha (no variant) |
| Re | Shuddha | Komal |
| Ga | Shuddha | Komal |
| Ma | Shuddha | Tivra |
| Pa | Shuddha | Shuddha (no variant) |
| Dha | Shuddha | Komal |
| Ni | Shuddha | Komal |

## Appendix B: Keyboard Input Mapping

| Key | Note | Shift+Key | Variant |
|-----|------|-----------|---------|
| `s` | Sa | `S` | (no effect) |
| `r` | Re | `R` | Komal Re |
| `g` | Ga | `G` | Komal Ga |
| `m` | Ma | `M` | Tivra Ma |
| `p` | Pa | `P` | (no effect) |
| `d` | Dha | `D` | Komal Dha |
| `n` | Ni | `N` | Komal Ni |
| `Space` | Rest | | |
| `-` | Sustain | | |
| `Backspace` | Delete last | | |
| `.` | Set Mandra octave (next note) | | |
| `'` | Set Taar octave (next note) | | |
| `` ` `` | Reset to Madhya octave | | |
| `ss`, `rr`, etc. | Dual swar (double-tap) | | |

## Appendix C: Built-in Taals Summary

| Name | Matras | Vibhags | Structure |
|------|--------|---------|-----------|
| Teentaal | 16 | 4 | X 4 + 2 4 + 0 4 + 3 4 |
| Ektaal | 12 | 6 | X 2 + 0 2 + 2 2 + 0 2 + 3 2 + 4 2 |
| Jhaptaal | 10 | 4 | X 2 + 2 3 + 0 2 + 3 3 |
| Rupak | 7 | 3 | 0 3 + 1 2 + 2 2 (sam = khali) |
| Dadra | 6 | 2 | X 3 + 0 3 |
| Keherwa | 8 | 2 | X 4 + 0 4 |
| Chautaal | 12 | 6 | X 2 + 0 2 + 2 2 + 0 2 + 3 2 + 4 2 |
| Dhamar | 14 | 4 | X 5 + 2 2 + 0 3 + 3 4 |
| Tilwada | 16 | 4 | X 4 + 2 4 + 0 4 + 3 4 |
| Jhoomra | 14 | 4 | X 3 + 2 4 + 0 3 + 3 4 |
| Deepchandi | 14 | 4 | X 3 + 2 4 + 0 3 + 3 4 |

**Note:** Rupak is unusual -- its sam (beat 0) coincides with khali.
