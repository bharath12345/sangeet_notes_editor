# Plan 2: Web Application (Tapir HTTP Server + Elm Frontend)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a web-based notation editor that achieves feature parity with the desktop app (minus voice recognition and direct filesystem access). A stateless Scala 3 HTTP server (`sangeet-server`) exposes `sangeet-core` operations as REST endpoints with auto-generated Swagger docs. An Elm frontend (`sangeet-web`) renders notation on HTML5 Canvas and communicates with the server via JSON.

**Architecture:**

```
Browser (Elm SPA)                  JVM (Tapir + http4s)
+-------------------+             +--------------------+
| sangeet-web/      |  REST/JSON  | sangeet-server/    |
|                   | <---------> |                    |
| Elm Model/Update  |             | Tapir endpoints    |
| Canvas rendering  |             |   |                |
| Keyboard handling |             |   v                |
| Web Audio (ports) |             | sangeet-core (JAR) |
| File download/    |             |   - model          |
|   upload          |             |   - layout          |
+-------------------+             |   - format          |
                                  |   - export          |
                                  +--------------------+
```

- `sangeet-server` is an sbt sub-module depending on `sangeet-core`
- `sangeet-web` is an Elm project at `sangeet-web/` in the same monorepo
- The server is stateless -- all state lives in the Elm frontend
- Package: `com.varpas.sangeet.server.*`

**Tech Stack:**

| Component | Technology |
|-----------|-----------|
| Server | Scala 3, Tapir (tapir-core, tapir-json-circe, tapir-swagger-ui, tapir-http4s-server), http4s-ember-server, cats-effect 3 |
| Frontend | Elm 0.19, elm/browser, elm/html, elm/json, elm/http, elm/time, joakin/elm-canvas |
| Audio | Web Audio API via Elm ports |
| Build | sbt (server), elm make (frontend), Makefile orchestration |

**Prerequisite:** Plan 1 (desktop rebuild) is complete. `sangeet-core` exists as a separate sbt sub-module with all domain types, API objects, and circe codecs.

---

### Task 1: Add sangeet-server to sbt build

**Files:**
- Modify: `build.sbt`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/Main.scala`

Add the `sangeet-server` sub-project to the existing multi-module sbt build with all required dependencies.

- [ ] **Step 1: Add sangeet-server sub-project to build.sbt**

After the existing `sangeet-core` and `sangeet-desktop` project definitions (from Plan 1), add:

```scala
val tapirVersion = "1.10.0"
val http4sVersion = "0.23.27"
val catsEffectVersion = "3.5.4"

lazy val `sangeet-server` = project
  .in(file("sangeet-server"))
  .dependsOn(`sangeet-core`)
  .settings(
    name := "sangeet-server",
    scalaVersion := scala3Version,
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
    libraryDependencies ++= Seq(
      // Tapir
      "com.softwaremill.sttp.tapir" %% "tapir-core"              % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % tapirVersion,
      // http4s
      "org.http4s"                  %% "http4s-ember-server"      % http4sVersion,
      // Cats Effect
      "org.typelevel"               %% "cats-effect"              % catsEffectVersion,
      // circe (transitive from sangeet-core, but explicit for server-specific codecs)
      "io.circe"                    %% "circe-core"               % "0.14.7",
      "io.circe"                    %% "circe-generic"            % "0.14.7",
      // Testing
      "org.scalatest"               %% "scalatest"                % "3.2.18" % Test,
    ),
    fork := true,
    Compile / mainClass := Some("com.varpas.sangeet.server.Main"),
  )
```

- [ ] **Step 2: Create directory structure**

```bash
mkdir -p sangeet-server/src/main/scala/com/varpas/sangeet/server
mkdir -p sangeet-server/src/test/scala/com/varpas/sangeet/server
```

- [ ] **Step 3: Create minimal Main.scala**

Create `sangeet-server/src/main/scala/com/varpas/sangeet/server/Main.scala`:

```scala
package com.varpas.sangeet.server

import cats.effect.{IO, IOApp, ExitCode}

object Main extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    IO.println("Sangeet Server starting...").as(ExitCode.Success)
```

- [ ] **Step 4: Verify compilation**

```bash
sbt "sangeet-server / compile"
```

Expected: Compiles successfully. The server sub-module can resolve `sangeet-core` types.

- [ ] **Step 5: Commit**

```bash
git add build.sbt sangeet-server/
git commit -m "feat: add sangeet-server sbt sub-module with Tapir and http4s dependencies"
```

---

### Task 2: Define response envelope and error mapping

**Files:**
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/ApiEnvelope.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/ErrorMapping.scala`

Define the JSON response envelope (`ok`/`data`/`error`) and map `ApiError` to HTTP status codes.

- [ ] **Step 1: Create ApiEnvelope.scala**

```scala
package com.varpas.sangeet.server

import io.circe.*
import io.circe.syntax.*
import io.circe.generic.semiauto.*

case class ApiSuccess[A](ok: Boolean = true, data: A)
case class ApiFailure(ok: Boolean = false, error: ErrorBody)
case class ErrorBody(code: String, message: String)

object ApiEnvelope:
  given [A: Encoder]: Encoder[ApiSuccess[A]] = Encoder.instance { s =>
    Json.obj("ok" -> Json.True, "data" -> s.data.asJson)
  }

  given Encoder[ApiFailure] = Encoder.instance { f =>
    Json.obj(
      "ok" -> Json.False,
      "error" -> Json.obj(
        "code" -> f.error.code.asJson,
        "message" -> f.error.message.asJson
      )
    )
  }

  def success[A: Encoder](data: A): Json = ApiSuccess(data = data).asJson
  def failure(code: String, message: String): Json =
    ApiFailure(error = ErrorBody(code, message)).asJson
```

- [ ] **Step 2: Create ErrorMapping.scala**

Map `ApiError` codes to HTTP status codes:

```scala
package com.varpas.sangeet.server

import sangeet.core.ApiError
import sttp.model.StatusCode

object ErrorMapping:
  def statusCode(error: ApiError): StatusCode = error.code match
    case "VALIDATION_ERROR" => StatusCode.UnprocessableEntity  // 422
    case "NOT_FOUND"        => StatusCode.NotFound             // 404
    case "EXPORT_ERROR"     => StatusCode.InternalServerError  // 500
    case _                  => StatusCode.BadRequest           // 400

  def toFailureJson(error: ApiError): io.circe.Json =
    ApiEnvelope.failure(error.code, error.message)
```

- [ ] **Step 3: Verify compilation**

```bash
sbt "sangeet-server / compile"
```

- [ ] **Step 4: Commit**

```bash
git add sangeet-server/src/main/scala/com/varpas/sangeet/server/
git commit -m "feat: add API response envelope and error-to-HTTP status mapping"
```

---

### Task 3: Define Tapir endpoint definitions -- Reference Data (GET endpoints)

**Files:**
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/ReferenceEndpoints.scala`

Start with the simplest endpoints (GET, no request body) to establish the Tapir pattern.

- [ ] **Step 1: Create ReferenceEndpoints.scala**

```scala
package com.varpas.sangeet.server.endpoints

import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import io.circe.*
import io.circe.generic.semiauto.*
import sangeet.model.{Taal, Raag}

object ReferenceEndpoints:
  private val baseEndpoint = endpoint.in("api" / "v1")

  // GET /api/v1/taals
  val listTaals = baseEndpoint
    .get
    .in("taals")
    .out(jsonBody[Json])
    .summary("List all built-in taals")
    .tag("Reference Data")

  // GET /api/v1/taals/:name
  val getTaalByName = baseEndpoint
    .get
    .in("taals" / path[String]("name"))
    .out(jsonBody[Json])
    .errorOut(statusCode and jsonBody[Json])
    .summary("Get a taal by name")
    .tag("Reference Data")

  // GET /api/v1/raags
  val listRaags = baseEndpoint
    .get
    .in("raags")
    .out(jsonBody[Json])
    .summary("List all built-in raags")
    .tag("Reference Data")

  // GET /api/v1/raags/:name
  val getRaagByName = baseEndpoint
    .get
    .in("raags" / path[String]("name"))
    .out(jsonBody[Json])
    .errorOut(statusCode and jsonBody[Json])
    .summary("Get a raag by name")
    .tag("Reference Data")

  // GET /api/v1/rendering/colors
  val getColors = baseEndpoint
    .get
    .in("rendering" / "colors")
    .out(jsonBody[Json])
    .summary("Get notation color palette")
    .tag("Rendering")

  // GET /api/v1/rendering/scripts
  val getScripts = baseEndpoint
    .get
    .in("rendering" / "scripts")
    .out(jsonBody[Json])
    .summary("Get all script glyph mappings")
    .tag("Rendering")

  val all = List(listTaals, getTaalByName, listRaags, getRaagByName, getColors, getScripts)
```

- [ ] **Step 2: Verify compilation**

```bash
sbt "sangeet-server / compile"
```

- [ ] **Step 3: Commit**

```bash
git add sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/
git commit -m "feat: define Tapir endpoint definitions for reference data and rendering"
```

---

### Task 4: Define Tapir endpoint definitions -- Composition, Editor, Cursor

**Files:**
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/CompositionEndpoints.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/EditorEndpoints.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/CursorEndpoints.scala`

- [ ] **Step 1: Create CompositionEndpoints.scala**

Define endpoints for: `POST /compositions`, `POST /compositions/parse`, `POST /compositions/serialize`.

Each endpoint takes a `jsonBody[Json]` input and returns `jsonBody[Json]` output (using raw circe Json for flexibility with the envelope). Error output uses `statusCode and jsonBody[Json]`.

```scala
package com.varpas.sangeet.server.endpoints

import sttp.tapir.*
import sttp.tapir.json.circe.*
import io.circe.Json

object CompositionEndpoints:
  private val base = endpoint.in("api" / "v1" / "compositions")

  val create = base.post
    .in(jsonBody[Json])
    .out(jsonBody[Json])
    .errorOut(statusCode and jsonBody[Json])
    .summary("Create new composition")
    .tag("Composition")

  val parse = base.post
    .in("parse")
    .in(jsonBody[Json])
    .out(jsonBody[Json])
    .errorOut(statusCode and jsonBody[Json])
    .summary("Parse .swar JSON into Composition")
    .tag("Composition")

  val serialize = base.post
    .in("serialize")
    .in(jsonBody[Json])
    .out(jsonBody[Json])
    .errorOut(statusCode and jsonBody[Json])
    .summary("Serialize Composition to .swar JSON")
    .tag("Composition")

  val all = List(create, parse, serialize)
```

- [ ] **Step 2: Create EditorEndpoints.scala**

Define endpoints for: `insert-swar`, `insert-rest`, `insert-sustain`, `delete-last`, `insert-dual-swar`. All follow the `EditorInput + operation-specific fields -> EditorResult` pattern.

```scala
package com.varpas.sangeet.server.endpoints

import sttp.tapir.*
import sttp.tapir.json.circe.*
import io.circe.Json

object EditorEndpoints:
  private val base = endpoint.in("api" / "v1" / "editor").post

  val insertSwar = base.in("insert-swar")
    .in(jsonBody[Json]).out(jsonBody[Json])
    .errorOut(statusCode and jsonBody[Json])
    .summary("Insert a swar note").tag("Editor")

  val insertRest = base.in("insert-rest")
    .in(jsonBody[Json]).out(jsonBody[Json])
    .errorOut(statusCode and jsonBody[Json])
    .summary("Insert a rest").tag("Editor")

  val insertSustain = base.in("insert-sustain")
    .in(jsonBody[Json]).out(jsonBody[Json])
    .errorOut(statusCode and jsonBody[Json])
    .summary("Insert a sustain").tag("Editor")

  val deleteLast = base.in("delete-last")
    .in(jsonBody[Json]).out(jsonBody[Json])
    .errorOut(statusCode and jsonBody[Json])
    .summary("Delete last event").tag("Editor")

  val insertDualSwar = base.in("insert-dual-swar")
    .in(jsonBody[Json]).out(jsonBody[Json])
    .errorOut(statusCode and jsonBody[Json])
    .summary("Insert dual swar").tag("Editor")

  val all = List(insertSwar, insertRest, insertSustain, deleteLast, insertDualSwar)
```

- [ ] **Step 3: Create CursorEndpoints.scala**

Define endpoints for: `next-beat`, `prev-beat`, `next-sub-beat`, `set-subdivisions`, `set-octave`, `move-to`.

```scala
package com.varpas.sangeet.server.endpoints

import sttp.tapir.*
import sttp.tapir.json.circe.*
import io.circe.Json

object CursorEndpoints:
  private val base = endpoint.in("api" / "v1" / "cursor").post

  val nextBeat = base.in("next-beat")
    .in(jsonBody[Json]).out(jsonBody[Json])
    .summary("Move cursor to next beat").tag("Cursor")

  val prevBeat = base.in("prev-beat")
    .in(jsonBody[Json]).out(jsonBody[Json])
    .summary("Move cursor to previous beat").tag("Cursor")

  val nextSubBeat = base.in("next-sub-beat")
    .in(jsonBody[Json]).out(jsonBody[Json])
    .summary("Move cursor to next sub-beat").tag("Cursor")

  val setSubdivisions = base.in("set-subdivisions")
    .in(jsonBody[Json]).out(jsonBody[Json])
    .summary("Set beat subdivision count").tag("Cursor")

  val setOctave = base.in("set-octave")
    .in(jsonBody[Json]).out(jsonBody[Json])
    .summary("Set current octave modifier").tag("Cursor")

  val moveTo = base.in("move-to")
    .in(jsonBody[Json]).out(jsonBody[Json])
    .summary("Move cursor to specific position").tag("Cursor")

  val all = List(nextBeat, prevBeat, nextSubBeat, setSubdivisions, setOctave, moveTo)
```

- [ ] **Step 4: Verify compilation**

```bash
sbt "sangeet-server / compile"
```

- [ ] **Step 5: Commit**

```bash
git add sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/
git commit -m "feat: define Tapir endpoints for composition, editor, and cursor operations"
```

---

### Task 5: Define Tapir endpoint definitions -- Section, Ornament, Stroke, Layout, Export, Playback

**Files:**
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/SectionEndpoints.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/OrnamentEndpoints.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/StrokeEndpoints.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/LayoutEndpoints.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/ExportEndpoints.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/PlaybackEndpoints.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/GlyphEndpoints.scala`

- [ ] **Step 1: Create SectionEndpoints.scala**

Endpoints: `POST /sections/add`, `/sections/remove`, `/sections/rename`, `/sections/reorder`. All take JSON body, return JSON. Error output on remove/rename/reorder.

- [ ] **Step 2: Create OrnamentEndpoints.scala**

Endpoints: `POST /editor/ornament/simple`, `/editor/ornament/single-note`, `/editor/ornament/meend`, `/editor/ornament/krintan`, `/editor/ornament/murki`, `/editor/ornament/zamzama`. All POST with JSON body + error output.

- [ ] **Step 3: Create StrokeEndpoints.scala**

Endpoints: `POST /editor/stroke/set`, `/editor/stroke/clear`.

- [ ] **Step 4: Create LayoutEndpoints.scala**

Endpoint: `POST /layout/compute`. Takes composition + optional config, returns grid data.

- [ ] **Step 5: Create ExportEndpoints.scala**

Endpoints:
- `POST /export/pdf` -- returns `application/pdf` bytes (use `byteArrayBody` for output, not JSON)
- `POST /export/html` -- returns JSON-wrapped HTML string

- [ ] **Step 6: Create PlaybackEndpoints.scala**

Endpoint: `POST /playback/schedule`. Takes events + BPM + matras, returns timed notes.

- [ ] **Step 7: Create GlyphEndpoints.scala**

Endpoint: `POST /rendering/glyph`. Takes note + variant + octave + script, returns glyph info.

- [ ] **Step 8: Create AllEndpoints.scala**

Aggregate all endpoint lists into one object for Swagger doc generation:

```scala
package com.varpas.sangeet.server.endpoints

object AllEndpoints:
  val all =
    ReferenceEndpoints.all ++
    CompositionEndpoints.all ++
    EditorEndpoints.all ++
    CursorEndpoints.all ++
    SectionEndpoints.all ++
    OrnamentEndpoints.all ++
    StrokeEndpoints.all ++
    LayoutEndpoints.all ++
    ExportEndpoints.all ++
    PlaybackEndpoints.all ++
    GlyphEndpoints.all
```

- [ ] **Step 9: Verify compilation**

```bash
sbt "sangeet-server / compile"
```

- [ ] **Step 10: Commit**

```bash
git add sangeet-server/src/main/scala/com/varpas/sangeet/server/endpoints/
git commit -m "feat: define all remaining Tapir endpoint definitions (36 endpoints total)"
```

---

### Task 6: Implement server routes -- Reference Data and Rendering

**Files:**
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/ReferenceRoutes.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/RenderingRoutes.scala`

Wire the GET endpoints to `sangeet-core` functions. These are the simplest routes (no request body parsing).

- [ ] **Step 1: Create ReferenceRoutes.scala**

```scala
package com.varpas.sangeet.server.routes

import cats.effect.IO
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes
import io.circe.syntax.*
import sangeet.core.ReferenceApi
import com.varpas.sangeet.server.{ApiEnvelope, ErrorMapping}
import com.varpas.sangeet.server.endpoints.ReferenceEndpoints

object ReferenceRoutes:
  val routes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(
      List(
        ReferenceEndpoints.listTaals.serverLogicSuccess[IO] { _ =>
          IO.pure(ApiEnvelope.success(
            io.circe.Json.obj("taals" -> ReferenceApi.allTaals.asJson)
          ))
        },
        ReferenceEndpoints.getTaalByName.serverLogic[IO] { name =>
          IO.pure(
            ReferenceApi.taalByName(name) match
              case Right(taal) => Right(ApiEnvelope.success(taal.asJson))
              case Left(err)   => Left((ErrorMapping.statusCode(err), ErrorMapping.toFailureJson(err)))
          )
        },
        ReferenceEndpoints.listRaags.serverLogicSuccess[IO] { _ =>
          IO.pure(ApiEnvelope.success(
            io.circe.Json.obj("raags" -> ReferenceApi.allRaags.asJson)
          ))
        },
        ReferenceEndpoints.getRaagByName.serverLogic[IO] { name =>
          IO.pure(
            ReferenceApi.raagByName(name) match
              case Right(raag) => Right(ApiEnvelope.success(raag.asJson))
              case Left(err)   => Left((ErrorMapping.statusCode(err), ErrorMapping.toFailureJson(err)))
          )
        },
      )
    )
```

- [ ] **Step 2: Create RenderingRoutes.scala**

Wire `GET /rendering/colors` to `GlyphApi.notationColors` and `GET /rendering/scripts` to `GlyphApi.allScriptMappings`. Wire `POST /rendering/glyph` to `GlyphApi.noteGlyph`.

- [ ] **Step 3: Verify compilation**

```bash
sbt "sangeet-server / compile"
```

- [ ] **Step 4: Commit**

```bash
git add sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/
git commit -m "feat: implement reference data and rendering server routes"
```

---

### Task 7: Implement server routes -- Composition, Editor, Cursor

**Files:**
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/CompositionRoutes.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/EditorRoutes.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/CursorRoutes.scala`

These routes parse JSON request bodies, extract fields, call `sangeet-core` API functions, and wrap results in the response envelope.

- [ ] **Step 1: Create CompositionRoutes.scala**

For each composition endpoint:
1. Parse the incoming `Json` body to extract fields (using circe cursor/HCursor)
2. Resolve `taalName`/`raagName` to built-in taals/raags if provided
3. Call `CompositionApi.createComposition(...)` / `parseComposition(...)` / `serializeComposition(...)`
4. Wrap result in `ApiEnvelope.success(...)` or map error to `Left(...)`

Example pattern for the create endpoint:

```scala
CompositionEndpoints.create.serverLogic[IO] { json =>
  IO.pure {
    val cursor = json.hcursor
    for
      title <- cursor.get[String]("title").left.map(e => parseError(e))
      compType <- cursor.get[CompositionType]("compositionType").left.map(e => parseError(e))
      taal <- resolveTaal(cursor)
      raag <- resolveRaag(cursor)
      laya = cursor.get[Option[Laya]]("laya").getOrElse(None)
      taanCount = cursor.get[Int]("taanCount").getOrElse(0)
      showStroke = cursor.get[Boolean]("showStrokeLine").getOrElse(false)
      showSahitya = cursor.get[Boolean]("showSahityaLine").getOrElse(false)
      (composition, cursorModel) = CompositionApi.createComposition(
        title, compType, taal, raag, laya, taanCount, showStroke, showSahitya
      )
    yield ApiEnvelope.success(Json.obj(
      "composition" -> composition.asJson,
      "cursor" -> cursorModel.asJson
    ))
  }
}
```

- [ ] **Step 2: Create EditorRoutes.scala**

Each editor endpoint:
1. Parse `composition`, `sectionIndex`, `cursor` from body (these are common)
2. Parse operation-specific fields (`note`, `shiftDown` for insert-swar)
3. Build `EditorInput`, call `EditorApi.insertSwar(...)` etc.
4. Return `EditorResult` (composition + cursor + message) in envelope

- [ ] **Step 3: Create CursorRoutes.scala**

Cursor endpoints are simpler -- they only take a `CursorModel` and return a modified one. Parse `cursor` from body, call `CursorApi.nextBeat(cursor)` etc., return wrapped result.

For `set-subdivisions`: also parse `n: Int` from body.
For `set-octave`: also parse `octave: Octave` from body.
For `move-to`: also parse `cycle: Int` and `beat: Int` from body.

- [ ] **Step 4: Verify compilation**

```bash
sbt "sangeet-server / compile"
```

- [ ] **Step 5: Commit**

```bash
git add sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/
git commit -m "feat: implement composition, editor, and cursor server routes"
```

---

### Task 8: Implement server routes -- Section, Ornament, Stroke, Layout, Export, Playback

**Files:**
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/SectionRoutes.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/OrnamentRoutes.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/StrokeRoutes.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/LayoutRoutes.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/ExportRoutes.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/PlaybackRoutes.scala`

- [ ] **Step 1: Create SectionRoutes.scala**

Wire `SectionApi.addSection`, `removeSection`, `renameSection`, `moveSection`. Parse `composition`, `sectionIndex`/`name`/`sectionType`/`insertAt`/`fromIndex`/`toIndex` from body.

- [ ] **Step 2: Create OrnamentRoutes.scala**

Wire all 6 ornament endpoints. Parse `composition`, `sectionIndex`, plus ornament-specific fields (`ornamentType`, `noteRef`, `startNote`/`endNote`/`direction`/`intermediateNotes`, `notes`).

- [ ] **Step 3: Create StrokeRoutes.scala**

Wire `StrokeApi.setStroke` and `clearStroke`. Parse `composition`, `sectionIndex`, `cursor`, `stroke`.

- [ ] **Step 4: Create LayoutRoutes.scala**

Wire `LayoutApi.computeLayout`. Parse `composition` and optional `config`. Return list of `SectionGrid` in envelope.

- [ ] **Step 5: Create ExportRoutes.scala**

PDF endpoint: Parse `composition` and `script`, call `ExportApi.exportPdf(...)`, return raw bytes with `Content-Type: application/pdf` and `Content-Disposition: attachment; filename="composition.pdf"`.

HTML endpoint: Parse same, call `ExportApi.exportHtml(...)`, return HTML string wrapped in envelope.

- [ ] **Step 6: Create PlaybackRoutes.scala**

Wire `PlaybackApi.schedulePlayback`. Parse `events`, `bpm`, `matras`. Return list of `TimedNote` in envelope.

- [ ] **Step 7: Verify compilation**

```bash
sbt "sangeet-server / compile"
```

- [ ] **Step 8: Commit**

```bash
git add sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/
git commit -m "feat: implement remaining server routes (section, ornament, stroke, layout, export, playback)"
```

---

### Task 9: Swagger UI, CORS, health check, and server startup

**Files:**
- Modify: `sangeet-server/src/main/scala/com/varpas/sangeet/server/Main.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/CorsMiddleware.scala`
- Create: `sangeet-server/src/main/scala/com/varpas/sangeet/server/routes/AllRoutes.scala`

- [ ] **Step 1: Create AllRoutes.scala**

Aggregate all route modules into a single `HttpRoutes[IO]`:

```scala
package com.varpas.sangeet.server.routes

import cats.effect.IO
import cats.implicits.*
import org.http4s.HttpRoutes

object AllRoutes:
  val routes: HttpRoutes[IO] =
    ReferenceRoutes.routes <+>
    RenderingRoutes.routes <+>
    CompositionRoutes.routes <+>
    EditorRoutes.routes <+>
    CursorRoutes.routes <+>
    SectionRoutes.routes <+>
    OrnamentRoutes.routes <+>
    StrokeRoutes.routes <+>
    LayoutRoutes.routes <+>
    ExportRoutes.routes <+>
    PlaybackRoutes.routes
```

- [ ] **Step 2: Create CorsMiddleware.scala**

```scala
package com.varpas.sangeet.server

import cats.effect.IO
import org.http4s.*
import org.http4s.headers.*
import org.http4s.Method

object CorsMiddleware:
  def apply(routes: HttpRoutes[IO]): HttpRoutes[IO] =
    val corsHeaders = Headers(
      Header.Raw(ci"Access-Control-Allow-Origin", "*"),
      Header.Raw(ci"Access-Control-Allow-Methods", "GET, POST, OPTIONS"),
      Header.Raw(ci"Access-Control-Allow-Headers", "Content-Type"),
      Header.Raw(ci"Access-Control-Max-Age", "86400"),
    )

    HttpRoutes.of[IO] {
      case req if req.method == Method.OPTIONS =>
        IO.pure(Response[IO](Status.NoContent).withHeaders(corsHeaders))
      case req =>
        routes.run(req).map(_.map(_.putHeaders(corsHeaders.headers*)))
          .getOrElse(Response[IO](Status.NotFound))
    }
```

- [ ] **Step 3: Update Main.scala with full server startup**

```scala
package com.varpas.sangeet.server

import cats.effect.{IO, IOApp, ExitCode}
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.varpas.sangeet.server.routes.AllRoutes
import com.varpas.sangeet.server.endpoints.AllEndpoints

object Main extends IOApp:
  private val port = sys.env.getOrElse("PORT", "8080").toInt

  // Health check
  private val healthRoute = HttpRoutes.of[IO] {
    case GET -> Root / "health" => Ok("""{"status":"ok"}""")
  }

  // Swagger UI from all endpoint definitions
  private val swaggerEndpoints =
    SwaggerInterpreter().fromEndpoints[IO](AllEndpoints.all, "Sangeet Notes Editor API", "1.0")
  private val swaggerRoutes =
    Http4sServerInterpreter[IO]().toRoutes(swaggerEndpoints)

  override def run(args: List[String]): IO[ExitCode] =
    val allRoutes = CorsMiddleware(
      healthRoute <+> AllRoutes.routes <+> swaggerRoutes
    )

    EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(Port.fromInt(port).getOrElse(port"8080"))
      .withHttpApp(allRoutes.orNotFound)
      .build
      .use { server =>
        IO.println(s"Sangeet Server started on http://localhost:$port") *>
        IO.println(s"Swagger UI: http://localhost:$port/docs") *>
        IO.never
      }
      .as(ExitCode.Success)
```

- [ ] **Step 4: Test server startup and Swagger UI**

```bash
sbt "sangeet-server / run" &
sleep 3
curl -s http://localhost:8080/health | jq .
curl -s http://localhost:8080/api/v1/taals | jq .status
curl -s http://localhost:8080/docs  # Should return Swagger UI HTML
kill %1
```

- [ ] **Step 5: Commit**

```bash
git add sangeet-server/src/main/scala/com/varpas/sangeet/server/
git commit -m "feat: add Swagger UI, CORS middleware, health check, and server startup"
```

---

### Task 10: Server integration tests

**Files:**
- Create: `sangeet-server/src/test/scala/com/varpas/sangeet/server/ReferenceRoutesSpec.scala`
- Create: `sangeet-server/src/test/scala/com/varpas/sangeet/server/EditorRoutesSpec.scala`
- Create: `sangeet-server/src/test/scala/com/varpas/sangeet/server/ExportRoutesSpec.scala`

Test a representative subset of endpoints using Tapir's `serverLogic` testing support or direct http4s route testing.

- [ ] **Step 1: Create ReferenceRoutesSpec.scala**

Test GET endpoints: list taals returns 11, get taal by name "teentaal" returns Teentaal with 16 matras, get unknown taal returns 404.

```scala
package com.varpas.sangeet.server

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import io.circe.parser.*
import com.varpas.sangeet.server.routes.ReferenceRoutes

class ReferenceRoutesSpec extends AnyFlatSpec with Matchers:

  "GET /api/v1/taals" should "return all 11 built-in taals" in {
    val req = Request[IO](Method.GET, uri"/api/v1/taals")
    val resp = ReferenceRoutes.routes.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.Ok
    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).toOption.get
    val taals = json.hcursor.downField("data").downField("taals").as[List[io.circe.Json]]
    taals.toOption.get should have size 11
  }

  "GET /api/v1/taals/teentaal" should "return Teentaal" in {
    val req = Request[IO](Method.GET, uri"/api/v1/taals/teentaal")
    val resp = ReferenceRoutes.routes.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.Ok
    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).toOption.get
    json.hcursor.downField("data").downField("matras").as[Int].toOption.get shouldBe 16
  }

  "GET /api/v1/taals/nonexistent" should "return 404" in {
    val req = Request[IO](Method.GET, uri"/api/v1/taals/nonexistent")
    val resp = ReferenceRoutes.routes.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.NotFound
  }
```

- [ ] **Step 2: Create EditorRoutesSpec.scala**

Test the create composition and insert swar roundtrip:
1. POST to `/compositions` with Yaman + Teentaal + Gat
2. Verify response has composition and cursor
3. POST to `/editor/insert-swar` with the returned composition/cursor + note "ga"
4. Verify response has updated composition with one event and cursor advanced

- [ ] **Step 3: Create ExportRoutesSpec.scala**

Test PDF export: create a composition, insert a few notes, POST to `/export/pdf`, verify response has `Content-Type: application/pdf` and non-empty body.

- [ ] **Step 4: Run tests**

```bash
sbt "sangeet-server / test"
```

- [ ] **Step 5: Commit**

```bash
git add sangeet-server/src/test/
git commit -m "test: add integration tests for reference, editor, and export routes"
```

---

### Task 11: Set up Elm project

**Files:**
- Create: `sangeet-web/elm.json`
- Create: `sangeet-web/src/Main.elm`
- Create: `sangeet-web/public/index.html`
- Create: `sangeet-web/.gitignore`

Initialize the Elm project structure with all needed dependencies.

- [ ] **Step 1: Create elm.json**

```bash
mkdir -p sangeet-web/src sangeet-web/public
```

Create `sangeet-web/elm.json`:

```json
{
    "type": "application",
    "source-directories": [
        "src"
    ],
    "elm-version": "0.19.1",
    "dependencies": {
        "direct": {
            "elm/browser": "1.0.2",
            "elm/core": "1.0.5",
            "elm/html": "1.0.0",
            "elm/http": "2.0.0",
            "elm/json": "1.1.3",
            "elm/time": "1.0.0",
            "elm/file": "1.0.5",
            "elm/bytes": "1.0.8",
            "joakin/elm-canvas": "5.0.0"
        },
        "indirect": {
            "elm/url": "1.0.0",
            "elm/virtual-dom": "1.0.3"
        }
    },
    "test-dependencies": {
        "direct": {},
        "indirect": {}
    }
}
```

- [ ] **Step 2: Create public/index.html**

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Sangeet Notes Editor</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: system-ui, -apple-system, sans-serif; overflow: hidden; }
    @import url('https://fonts.googleapis.com/css2?family=Noto+Sans+Devanagari:wght@400;700&family=Noto+Sans+Kannada&family=Noto+Sans+Telugu&display=swap');
  </style>
  <link rel="preconnect" href="https://fonts.googleapis.com">
</head>
<body>
  <div id="app"></div>
  <script src="elm.js"></script>
  <script src="ports.js"></script>
  <script>
    var app = Elm.Main.init({
      node: document.getElementById('app'),
      flags: { apiBaseUrl: 'http://localhost:8080/api/v1' }
    });

    // Wire ports (audio, file download)
    if (app.ports) {
      initPorts(app);
    }
  </script>
</body>
</html>
```

- [ ] **Step 3: Create minimal Main.elm**

```elm
module Main exposing (main)

import Browser
import Html exposing (Html, div, text)

type alias Flags =
    { apiBaseUrl : String }

type alias Model =
    { apiBaseUrl : String
    }

type Msg
    = NoOp

main : Program Flags Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , subscriptions = \_ -> Sub.none
        , view = view
        }

init : Flags -> ( Model, Cmd Msg )
init flags =
    ( { apiBaseUrl = flags.apiBaseUrl }, Cmd.none )

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        NoOp ->
            ( model, Cmd.none )

view : Model -> Html Msg
view model =
    div []
        [ text "Sangeet Notes Editor (Web)" ]
```

- [ ] **Step 4: Create .gitignore**

Create `sangeet-web/.gitignore`:

```
elm-stuff/
public/elm.js
node_modules/
```

- [ ] **Step 5: Create public/ports.js (stub)**

```javascript
function initPorts(app) {
  // Audio playback port (Task 13)
  // File download port (Task 14)
}
```

- [ ] **Step 6: Install dependencies and verify build**

```bash
cd sangeet-web
npx elm make src/Main.elm --output=public/elm.js
```

Expected: Compiles successfully, produces `public/elm.js`.

- [ ] **Step 7: Commit**

```bash
git add sangeet-web/
git commit -m "feat: initialize Elm project with skeleton Main, index.html, and ports stub"
```

---

### Task 12: Elm domain model types

**Files:**
- Create: `sangeet-web/src/Model/Types.elm`
- Create: `sangeet-web/src/Model/Composition.elm`
- Create: `sangeet-web/src/Model/Event.elm`
- Create: `sangeet-web/src/Model/Ornament.elm`
- Create: `sangeet-web/src/Model/Taal.elm`
- Create: `sangeet-web/src/Model/Raag.elm`
- Create: `sangeet-web/src/Model/Cursor.elm`
- Create: `sangeet-web/src/Model/Layout.elm`

Define ALL domain types in Elm matching the backend API spec (Section 2). Include JSON decoders and encoders for each type.

- [ ] **Step 1: Create Model/Types.elm**

Primitive types and their JSON codecs:

```elm
module Model.Types exposing (..)

import Json.Decode as D
import Json.Encode as E

type Note = Sa | Re | Ga | Ma | Pa | Dha | Ni

noteDecoder : D.Decoder Note
noteDecoder =
    D.string |> D.andThen (\s ->
        case s of
            "sa" -> D.succeed Sa
            "re" -> D.succeed Re
            "ga" -> D.succeed Ga
            "ma" -> D.succeed Ma
            "pa" -> D.succeed Pa
            "dha" -> D.succeed Dha
            "ni" -> D.succeed Ni
            _ -> D.fail ("Unknown note: " ++ s)
    )

encodeNote : Note -> E.Value
encodeNote note =
    E.string (case note of
        Sa -> "sa"
        Re -> "re"
        Ga -> "ga"
        Ma -> "ma"
        Pa -> "pa"
        Dha -> "dha"
        Ni -> "ni"
    )

type Variant = Shuddha | Komal | Tivra
type Octave = AtiMandra | Mandra | Madhya | Taar | AtiTaar
type Stroke = Da | Ra | Chikari | Jod
type Laya = AtiVilambit | Vilambit | MadhyaLaya | Drut | AtiDrut
type SwarScript = Devanagari | Kannada | Telugu | English
type MeendDirection = Ascending | Descending

type alias Rational = { numerator : Int, denominator : Int }

rationalDecoder : D.Decoder Rational
rationalDecoder =
    D.map2 Rational (D.index 0 D.int) (D.index 1 D.int)

encodeRational : Rational -> E.Value
encodeRational r =
    E.list E.int [ r.numerator, r.denominator ]

type alias BeatPosition =
    { cycle : Int, beat : Int, subdivision : Rational }

type alias NoteRef =
    { note : Note, variant : Variant, octave : Octave }
```

Include decoders/encoders for Variant, Octave, Stroke, Laya, SwarScript, MeendDirection, BeatPosition, NoteRef following the same pattern (string-based for enums, object-based for records).

- [ ] **Step 2: Create Model/Ornament.elm**

Define all ornament types as a custom type with JSON codecs using the `"type"` discriminator:

```elm
type Ornament
    = MeendOrnament { startNote : NoteRef, endNote : NoteRef, direction : MeendDirection, intermediateNotes : List NoteRef }
    | KanSwarOrnament { graceNote : NoteRef }
    | MurkiOrnament { notes : List NoteRef }
    | GamakOrnament
    | AndolanOrnament
    | KrintanOrnament { notes : List NoteRef }
    | GitkariOrnament
    | GhaseetOrnament { targetNote : NoteRef }
    | SparshOrnament { touchNote : NoteRef }
    | ZamzamaOrnament { notes : List NoteRef }
    | CustomOrnament { name : String, parameters : Dict String String }
```

Decoder dispatches on `D.field "type" D.string`.

- [ ] **Step 3: Create Model/Event.elm**

```elm
type Event
    = SwarEvent
        { note : Note, variant : Variant, octave : Octave
        , beat : BeatPosition, duration : Rational
        , stroke : Maybe Stroke, ornaments : List Ornament
        , sahitya : Maybe String
        }
    | RestEvent { beat : BeatPosition, duration : Rational }
    | SustainEvent { beat : BeatPosition, duration : Rational }
```

- [ ] **Step 4: Create Model/Taal.elm**

```elm
type VibhagMarker = Sam | TaaliMarker Int | KhaliMarker

type alias Vibhag = { beats : Int, marker : VibhagMarker }

type alias Taal =
    { name : String, matras : Int, vibhags : List Vibhag, theka : Maybe (List String) }
```

VibhagMarker decoder: `"sam"` -> Sam, `"khali"` -> Khali, `{"taali": n}` -> TaaliMarker n.

- [ ] **Step 5: Create Model/Raag.elm, Model/Composition.elm**

Define `Raag`, `Metadata`, `Section`, `Tihai`, `Composition`, `CompositionType`, `SectionType` with full codecs.

- [ ] **Step 6: Create Model/Cursor.elm**

```elm
type alias CursorModel =
    { taal : Taal
    , cycle : Int
    , beat : Int
    , subIndex : Int
    , totalSubdivisions : Int
    , currentOctave : Octave
    }
```

- [ ] **Step 7: Create Model/Layout.elm**

```elm
type alias LayoutConfig =
    { highDensityThreshold : Int, cellWidthBase : Float
    , cellOverflowExpand : Float, lineSpacing : Float, headerHeight : Float }

type alias CycleAndBeat = { cycle : Int, beat : Int }
type alias BeatCell = { position : CycleAndBeat, events : List Event }
type alias GridLine = { cells : List BeatCell, vibhagBreaks : List Int, markers : List ( Int, VibhagMarker ) }
type alias SectionGrid = { sectionName : String, sectionType : SectionType, lines : List GridLine }

type alias GlyphInfo =
    { text : String, needsKomalMark : Bool, needsTivraMark : Bool
    , dotCount : Int, dotPosition : String }

type alias TimedNote =
    { timeMs : Int, durationMs : Int, note : Note, variant : Variant
    , octave : Octave, stroke : Maybe Stroke }
```

- [ ] **Step 8: Verify build**

```bash
cd sangeet-web && npx elm make src/Main.elm --output=public/elm.js
```

- [ ] **Step 9: Commit**

```bash
git add sangeet-web/src/Model/
git commit -m "feat: define all Elm domain model types with JSON decoders/encoders"
```

---

### Task 13: Elm API client

**Files:**
- Create: `sangeet-web/src/Api/Client.elm`
- Create: `sangeet-web/src/Api/Composition.elm`
- Create: `sangeet-web/src/Api/Editor.elm`
- Create: `sangeet-web/src/Api/Cursor.elm`
- Create: `sangeet-web/src/Api/Section.elm`
- Create: `sangeet-web/src/Api/Ornament.elm`
- Create: `sangeet-web/src/Api/Stroke.elm`
- Create: `sangeet-web/src/Api/Layout.elm`
- Create: `sangeet-web/src/Api/Export.elm`
- Create: `sangeet-web/src/Api/Playback.elm`
- Create: `sangeet-web/src/Api/Reference.elm`

HTTP functions for all backend endpoints. Each function takes input data, builds an HTTP request, and returns a `Cmd Msg`.

- [ ] **Step 1: Create Api/Client.elm**

Common HTTP helpers:

```elm
module Api.Client exposing (postJson, getJson, postBytes, ApiResult(..))

import Http
import Json.Decode as D
import Json.Encode as E

type ApiResult a
    = ApiOk a
    | ApiErr { code : String, message : String }

apiResultDecoder : D.Decoder a -> D.Decoder (ApiResult a)
apiResultDecoder dataDecoder =
    D.field "ok" D.bool |> D.andThen (\ok ->
        if ok then
            D.field "data" dataDecoder |> D.map ApiOk
        else
            D.field "error"
                (D.map2 (\c m -> ApiErr { code = c, message = m })
                    (D.field "code" D.string)
                    (D.field "message" D.string)
                )
    )

postJson : String -> String -> E.Value -> D.Decoder a -> (Result Http.Error (ApiResult a) -> msg) -> Cmd msg
postJson baseUrl path body decoder toMsg =
    Http.post
        { url = baseUrl ++ path
        , body = Http.jsonBody body
        , expect = Http.expectJson toMsg (apiResultDecoder decoder)
        }

getJson : String -> String -> D.Decoder a -> (Result Http.Error (ApiResult a) -> msg) -> Cmd msg
getJson baseUrl path decoder toMsg =
    Http.get
        { url = baseUrl ++ path
        , expect = Http.expectJson toMsg (apiResultDecoder decoder)
        }

postBytes : String -> String -> E.Value -> (Result Http.Error Bytes.Bytes -> msg) -> Cmd msg
postBytes baseUrl path body toMsg =
    Http.post
        { url = baseUrl ++ path
        , body = Http.jsonBody body
        , expect = Http.expectBytes toMsg identity
        }
```

- [ ] **Step 2: Create Api/Reference.elm**

```elm
module Api.Reference exposing (fetchTaals, fetchRaags, fetchColors, fetchScripts)

fetchTaals : String -> (Result Http.Error (ApiResult (List Taal)) -> msg) -> Cmd msg
fetchTaals baseUrl toMsg =
    getJson baseUrl "/taals"
        (D.field "taals" (D.list taalDecoder))
        toMsg

fetchRaags : String -> (Result Http.Error (ApiResult (List Raag)) -> msg) -> Cmd msg
fetchRaags baseUrl toMsg =
    getJson baseUrl "/raags"
        (D.field "raags" (D.list raagDecoder))
        toMsg
-- etc.
```

- [ ] **Step 3: Create Api/Composition.elm**

Functions: `createComposition`, `parseComposition`, `serializeComposition`. Each builds the request JSON from input parameters and decodes the response.

- [ ] **Step 4: Create Api/Editor.elm**

Functions: `insertSwar`, `insertRest`, `insertSustain`, `deleteLast`, `insertDualSwar`. Each encodes `EditorInput` (composition + sectionIndex + cursor) plus operation-specific fields, decodes `EditorResult`.

```elm
insertSwar : String -> Composition -> Int -> CursorModel -> Note -> Bool -> (Result Http.Error (ApiResult EditorResult) -> msg) -> Cmd msg
insertSwar baseUrl composition sectionIndex cursor note shiftDown toMsg =
    postJson baseUrl "/editor/insert-swar"
        (E.object
            [ ("composition", encodeComposition composition)
            , ("sectionIndex", E.int sectionIndex)
            , ("cursor", encodeCursor cursor)
            , ("note", encodeNote note)
            , ("shiftDown", E.bool shiftDown)
            ]
        )
        editorResultDecoder
        toMsg
```

- [ ] **Step 5: Create Api/Cursor.elm**

Functions: `nextBeat`, `prevBeat`, `nextSubBeat`, `setSubdivisions`, `setOctave`, `moveTo`. Each takes a `CursorModel` and returns an updated one.

- [ ] **Step 6: Create remaining API modules**

`Api/Section.elm`: `addSection`, `removeSection`, `renameSection`, `reorderSections`
`Api/Ornament.elm`: `addSimple`, `addSingleNote`, `addMeend`, `addKrintan`, `addMurki`, `addZamzama`
`Api/Stroke.elm`: `setStroke`, `clearStroke`
`Api/Layout.elm`: `computeLayout`
`Api/Export.elm`: `exportPdf`, `exportHtml`
`Api/Playback.elm`: `schedulePlayback`

- [ ] **Step 7: Verify build**

```bash
cd sangeet-web && npx elm make src/Main.elm --output=public/elm.js
```

- [ ] **Step 8: Commit**

```bash
git add sangeet-web/src/Api/
git commit -m "feat: implement Elm API client for all 36 backend endpoints"
```

---

### Task 14: Elm application state (Model, Msg, Update)

**Files:**
- Modify: `sangeet-web/src/Main.elm`
- Create: `sangeet-web/src/State/Model.elm`
- Create: `sangeet-web/src/State/Msg.elm`
- Create: `sangeet-web/src/State/Update.elm`
- Create: `sangeet-web/src/State/UndoHistory.elm`

Define the full application state, message types, and update function following The Elm Architecture.

- [ ] **Step 1: Create State/UndoHistory.elm**

Immutable undo/redo stack, max 50 entries:

```elm
module State.UndoHistory exposing (UndoHistory, init, push, undo, redo, present)

type alias Snapshot =
    { composition : Composition
    , cursor : CursorModel
    , sectionIndex : Int
    }

type UndoHistory =
    UndoHistory
        { past : List Snapshot
        , present : Snapshot
        , future : List Snapshot
        , maxSize : Int
        }

init : Snapshot -> UndoHistory
push : Snapshot -> UndoHistory -> UndoHistory
undo : UndoHistory -> Maybe UndoHistory
redo : UndoHistory -> Maybe UndoHistory
present : UndoHistory -> Snapshot
```

- [ ] **Step 2: Create State/Model.elm**

```elm
module State.Model exposing (Model, EditMode(..), OrnamentMode(..), init)

type EditMode = SwarEdit | StrokeEdit

type OrnamentMode
    = NoOrnament
    | SingleNoteMode String  -- ornamentType
    | MeendStartMode MeendDirection
    | MeendEndMode NoteRef MeendDirection
    | KrintanStartMode
    | KrintanEndMode NoteRef
    | MurkiCollectMode (List NoteRef)
    | ZamzamaCollectMode (List NoteRef)

type alias Model =
    { apiBaseUrl : String
    , history : UndoHistory
    , editMode : EditMode
    , ornamentMode : OrnamentMode
    , currentScript : SwarScript
    , playbackState : PlaybackState
    , bpm : Float
    , loopEnabled : Bool
    , lastTypedChar : Maybe Char
    , lastTypedTime : Int  -- millis
    , cursorVisible : Bool
    , statusLog : List String
    , availableTaals : List Taal
    , availableRaags : List Raag
    , colorPalette : Maybe ColorPalette
    , scriptMappings : List ScriptMapping
    , layoutGrids : Maybe (List SectionGrid)
    , showNewDialog : Bool
    , showPropsDialog : Bool
    , showAboutDialog : Bool
    , pendingApiCall : Bool  -- loading indicator
    }

type PlaybackState = Stopped | Playing | Paused
```

- [ ] **Step 3: Create State/Msg.elm**

```elm
module State.Msg exposing (Msg(..))

type Msg
    -- Keyboard input
    = KeyPressed String Bool Bool  -- key, shiftDown, ctrlDown
    | KeyTyped Char Bool  -- char, shiftDown
    -- Mouse
    | CanvasClicked Float Float  -- x, y
    -- Toolbar actions
    | NewComposition
    | OpenFile
    | SaveFile
    | ExportPdf
    | ExportHtml
    | ChangeScript SwarScript
    | ShowProperties
    | AddSection
    | RenameSection
    | RemoveSection
    | MoveSectionUp
    | MoveSectionDown
    -- Playback
    | Play | Pause | Stop
    | SetBpm Float
    | ToggleLoop
    -- Dialogs
    | NewDialogSubmit NewCompositionForm
    | NewDialogCancel
    | PropsDialogSubmit PropsForm
    | PropsDialogCancel
    | ShowAbout | HideAbout
    -- Undo/Redo
    | Undo | Redo
    -- Subdivisions
    | SetSubdivisions Int
    -- Octave
    | SetOctave Octave
    -- Stroke mode
    | ToggleStrokeMode
    -- API responses
    | GotCreateComposition (Result Http.Error (ApiResult { composition : Composition, cursor : CursorModel }))
    | GotEditorResult (Result Http.Error (ApiResult EditorResult))
    | GotCursorResult (Result Http.Error (ApiResult CursorModel))
    | GotLayoutResult (Result Http.Error (ApiResult (List SectionGrid)))
    | GotTaals (Result Http.Error (ApiResult (List Taal)))
    | GotRaags (Result Http.Error (ApiResult (List Raag)))
    | GotColors (Result Http.Error (ApiResult ColorPalette))
    | GotScripts (Result Http.Error (ApiResult (List ScriptMapping)))
    | GotPdfBytes (Result Http.Error Bytes.Bytes)
    | GotHtml (Result Http.Error (ApiResult String))
    | GotTimedNotes (Result Http.Error (ApiResult (List TimedNote)))
    | GotParseResult (Result Http.Error (ApiResult Composition))
    | GotSerializeResult (Result Http.Error (ApiResult String))
    -- File operations
    | FileSelected File.File
    | FileLoaded String
    -- Timer
    | CursorBlink Time.Posix
    | Tick Time.Posix  -- for double-tap timing
    -- No-op
    | NoOp
```

- [ ] **Step 4: Create State/Update.elm**

The main update function. For each Msg variant, compute the new Model and any Cmd.

Key patterns:

1. **Keyboard input** -> decide action (swar entry, navigation, ornament, etc.) -> call appropriate API endpoint -> on response, update model
2. **API responses** -> unwrap result, update composition/cursor in undo history, request new layout
3. **Layout response** -> store grids, trigger canvas re-render
4. **Undo/Redo** -> purely local, no API call needed, request layout for the restored state

The update function should be split into helper functions for readability:
- `handleKeyPress`, `handleKeyTyped`, `handleSwarKey`, `handleNavigationKey`, `handleOrnamentKey`
- `handleApiResponse`, `handleEditorResponse`
- `requestLayout` (fires layout API call after every composition change)

Double-tap detection:
```elm
handleKeyTyped char shiftDown model =
    let
        now = model.currentTimeMs
        isDualSwar = model.lastTypedChar == Just char && (now - model.lastTypedTime) < 350
    in
    if isDualSwar then
        -- Undo last entry, insert dual swar
        ( { model | lastTypedChar = Nothing }, Api.Editor.insertDualSwar ... )
    else
        -- Normal swar entry
        ( { model | lastTypedChar = Just char, lastTypedTime = now }
        , Api.Editor.insertSwar ...
        )
```

- [ ] **Step 5: Update Main.elm**

Wire Model, Msg, Update together. Add subscriptions for keyboard events and cursor blink timer:

```elm
subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ Browser.Events.onKeyDown (keyDecoder KeyPressed)
        , Time.every 530 CursorBlink
        , Time.every 50 Tick  -- for double-tap timing
        ]
```

- [ ] **Step 6: Add init logic**

On init, fetch reference data (taals, raags, colors, scripts) from the server:

```elm
init flags =
    ( initialModel flags.apiBaseUrl
    , Cmd.batch
        [ Api.Reference.fetchTaals flags.apiBaseUrl GotTaals
        , Api.Reference.fetchRaags flags.apiBaseUrl GotRaags
        , Api.Reference.fetchColors flags.apiBaseUrl GotColors
        , Api.Reference.fetchScripts flags.apiBaseUrl GotScripts
        ]
    )
```

- [ ] **Step 7: Verify build**

```bash
cd sangeet-web && npx elm make src/Main.elm --output=public/elm.js
```

- [ ] **Step 8: Commit**

```bash
git add sangeet-web/src/State/ sangeet-web/src/Main.elm
git commit -m "feat: implement Elm application state with Model, Msg, Update, and UndoHistory"
```

---

### Task 15: Elm notation canvas rendering

**Files:**
- Create: `sangeet-web/src/View/Canvas.elm`
- Create: `sangeet-web/src/View/SwarGlyph.elm`
- Create: `sangeet-web/src/View/OrnamentRenderer.elm`
- Create: `sangeet-web/src/View/GridRenderer.elm`
- Create: `sangeet-web/src/View/Colors.elm`

Render the notation grid on an HTML5 Canvas using `joakin/elm-canvas`. The rendering logic mirrors the desktop `GridRenderer`/`SwarGlyph`/`OrnamentRenderer` architecture.

- [ ] **Step 1: Create View/Colors.elm**

Map the `ColorPalette` from the server to Elm `Canvas.Color` values. Fallback to hardcoded defaults if the palette hasn't loaded yet.

```elm
module View.Colors exposing (notationColors, NotationColors)

type alias NotationColors =
    { taalMarker : Color, taalMarkerSam : Color, swar : Color
    , octaveDot : Color, ornament : Color, stroke : Color
    , sahitya : Color, rest : Color, sustain : Color
    , cursor : Color, cursorStroke : Color
    }

defaults : NotationColors
defaults =
    { taalMarker = Color.rgb255 183 28 28
    , taalMarkerSam = Color.rgb255 211 47 47
    , swar = Color.rgb255 26 35 126
    , octaveDot = Color.rgb255 230 81 0
    , ornament = Color.rgb255 74 20 140
    , stroke = Color.rgb255 0 105 92
    , sahitya = Color.rgb255 46 125 50
    , rest = Color.rgb255 97 97 97
    , sustain = Color.rgb255 158 158 158
    , cursor = Color.rgb255 25 118 210
    , cursorStroke = Color.rgb255 230 120 0
    }
```

- [ ] **Step 2: Create View/SwarGlyph.elm**

Render a single swar glyph on the canvas with:
- Note text (from script mapping)
- Komal underline
- Tivra overbar
- Octave dots above/below
- Rest and sustain symbols

```elm
module View.SwarGlyph exposing (drawSwar, drawRest, drawSustain)

drawSwar : Canvas.Renderable -> ...
```

Use `Canvas.text` for the glyph text, `Canvas.shapes` for dots and lines.

- [ ] **Step 3: Create View/OrnamentRenderer.elm**

Render each ornament type above the swar row:
- Meend: arc curve with arrow
- Kan Swar: small glyph positioned left and above
- Gamak: heavy zigzag (4 segments)
- Andolan: gentle zigzag (6 segments)
- Gitkari: italic "tr" + wavy tail
- Murki: notes in parentheses
- Krintan: downward arc with note text
- Ghaseet: heavy arc with arrow
- Sparsh: dot + tiny glyph
- Zamzama: notes in square brackets
- Custom: italic name label

All use the ornament color from the palette.

- [ ] **Step 4: Create View/GridRenderer.elm**

Render a complete section grid. For each `GridLine`:
1. Draw taal markers row (14px height)
2. Draw subdivision brackets (10px)
3. Draw ornament row (18px) using OrnamentRenderer
4. Draw swar row (18px) using SwarGlyph
5. Draw mandra dot area (12px)
6. Draw stroke row if enabled (16px)
7. Draw sahitya row if enabled (14px)
8. Draw vibhag separator lines

Also render:
- Section headers (active: blue + underline, inactive: gray)
- Cursor (blinking vertical line at cursor position)

- [ ] **Step 5: Create View/Canvas.elm**

Top-level canvas view that combines composition header (HTML) + canvas element:

```elm
module View.Canvas exposing (view)

view : Model -> Html Msg
view model =
    div [ class "editor-area" ]
        [ compositionHeader model
        , Canvas.toHtml ( canvasWidth, canvasHeight )
            [ Mouse.onClick (\event -> CanvasClicked (Tuple.first event.offsetPos) (Tuple.second event.offsetPos))
            ]
            (renderAllSections model)
        ]
```

The canvas height is computed dynamically based on the number of sections and lines.

- [ ] **Step 6: Verify build**

```bash
cd sangeet-web && npx elm make src/Main.elm --output=public/elm.js
```

- [ ] **Step 7: Commit**

```bash
git add sangeet-web/src/View/
git commit -m "feat: implement Elm canvas rendering for notation grid (5 rows per line)"
```

---

### Task 16: Elm keyboard input handling

**Files:**
- Create: `sangeet-web/src/Input/KeyHandler.elm`
- Create: `sangeet-web/src/Input/OrnamentMode.elm`
- Modify: `sangeet-web/src/State/Update.elm`

Map browser keyboard events to application Msg types and handle the ornament mode state machine.

- [ ] **Step 1: Create Input/KeyHandler.elm**

Map raw key events to semantic actions:

```elm
module Input.KeyHandler exposing (handleKeyDown, handleKeyPress)

type KeyAction
    = SwarInput Note Bool  -- note, shiftDown
    | InsertRest
    | InsertSustain
    | DeleteLast
    | NavRight | NavLeft | NavNextCycle
    | UndoAction | RedoAction
    | SubdivisionSet Int
    | OctaveSet Octave
    | StrokeShortcut Stroke
    | ToggleStroke
    | OrnamentShortcut OrnamentTrigger
    | EscapeAction
    | NoAction

handleKeyDown : String -> Bool -> Bool -> KeyAction
handleKeyDown key shiftDown ctrlDown =
    if ctrlDown then
        case key of
            "z" -> if shiftDown then RedoAction else UndoAction
            "d" -> StrokeShortcut Da
            "r" -> StrokeShortcut Ra
            "c" -> StrokeShortcut Chikari
            "g" -> OrnamentShortcut GamakTrigger
            "a" -> OrnamentShortcut AndolanTrigger
            "i" -> OrnamentShortcut GitkariTrigger
            "k" -> OrnamentShortcut KanSwarTrigger
            "h" -> OrnamentShortcut SparshTrigger
            "e" -> OrnamentShortcut GhaseetTrigger
            "m" -> OrnamentShortcut (if shiftDown then MeendDescTrigger else MeendAscTrigger)
            "j" -> OrnamentShortcut KrintanTrigger
            "u" -> OrnamentShortcut MurkiTrigger
            "w" -> OrnamentShortcut ZamzamaTrigger
            _ ->
                case String.toInt (String.right 1 key) of
                    Just n -> if n >= 2 && n <= 8 then SubdivisionSet n else NoAction
                    Nothing -> NoAction
    else
        case key of
            "ArrowRight" -> NavRight
            "ArrowLeft" -> NavLeft
            "Tab" -> NavRight
            "Enter" -> NavNextCycle
            "Escape" -> EscapeAction
            "F2" -> ToggleStroke
            " " -> InsertRest
            "-" -> InsertSustain
            "Backspace" -> DeleteLast
            "Delete" -> DeleteLast
            "." -> OctaveSet Mandra
            "'" -> OctaveSet Taar
            "`" -> OctaveSet Madhya
            _ -> mapSwarKey key shiftDown

mapSwarKey : String -> Bool -> KeyAction
mapSwarKey key shiftDown =
    case String.toLower key of
        "s" -> SwarInput Sa shiftDown
        "r" -> SwarInput Re shiftDown
        "g" -> SwarInput Ga shiftDown
        "m" -> SwarInput Ma shiftDown
        "p" -> SwarInput Pa shiftDown
        "d" -> SwarInput Dha shiftDown
        "n" -> SwarInput Ni shiftDown
        _ -> NoAction
```

Important: Prevent default on Space, Tab, arrow keys to avoid browser scrolling/focus changes. This is done via the `preventDefaultOn` attribute on the keyboard event subscription.

- [ ] **Step 2: Create Input/OrnamentMode.elm**

Ornament mode state machine:

```elm
module Input.OrnamentMode exposing (transition, OrnamentAction(..))

type OrnamentAction
    = ApplySimple String  -- ornamentType
    | ApplySingleNote String NoteRef
    | ApplyMeend NoteRef NoteRef MeendDirection
    | ApplyKrintan NoteRef NoteRef
    | ApplyMurki (List NoteRef)
    | ApplyZamzama (List NoteRef)
    | StillCollecting OrnamentMode
    | Cancelled

transition : OrnamentMode -> Char -> Bool -> OrnamentAction
```

The function takes the current ornament mode and a note key press, and returns either an `ApplyX` action (ornament is complete, attach to last swar) or `StillCollecting` (update the mode, wait for more input).

- [ ] **Step 3: Wire into Update.elm**

Connect `handleKeyDown` output to API calls in the update function. When an API response arrives with a new composition, also fire a layout compute request.

Handle browser key conflict for Ctrl+D (browser bookmark). Use `preventDefaultOn` to intercept. If Ctrl+D still conflicts, use an alternative binding (document this).

- [ ] **Step 4: Verify build**

```bash
cd sangeet-web && npx elm make src/Main.elm --output=public/elm.js
```

- [ ] **Step 5: Commit**

```bash
git add sangeet-web/src/Input/ sangeet-web/src/State/Update.elm
git commit -m "feat: implement keyboard input handling with ornament mode state machine"
```

---

### Task 17: Elm toolbar and controls

**Files:**
- Create: `sangeet-web/src/View/Toolbar.elm`
- Create: `sangeet-web/src/View/Header.elm`
- Create: `sangeet-web/src/View/StatusBar.elm`
- Create: `sangeet-web/src/View/KeyboardLegend.elm`
- Create: `sangeet-web/src/View/Layout.elm`

Build the complete application UI shell around the canvas.

- [ ] **Step 1: Create View/Toolbar.elm**

Two toolbar rows as HTML divs:

Row 1: New, Open, Save, PDF, HTML | Properties, Add Section, Rename, Remove, Move Up/Down | Script dropdown
Row 2: Play, Pause, Stop | Loop checkbox | BPM slider + label | About

```elm
module View.Toolbar exposing (view)

view : Model -> Html Msg
view model =
    div [ class "toolbar" ]
        [ toolbarRow1 model
        , toolbarRow2 model
        ]

toolbarRow1 model =
    div [ class "toolbar-row" ]
        [ button [ onClick NewComposition ] [ text "New" ]
        , button [ onClick OpenFile ] [ text "Open" ]
        , button [ onClick SaveFile ] [ text "Save" ]
        , button [ onClick ExportPdf ] [ text "PDF" ]
        , button [ onClick ExportHtml ] [ text "HTML" ]
        , span [ class "separator" ] []
        , button [ onClick ShowProperties ] [ text "Properties" ]
        , button [ onClick AddSection ] [ text "+Section" ]
        -- ... more buttons ...
        , select [ onInput (ChangeScript << parseScript) ]
            [ option [ value "devanagari" ] [ text "Devanagari" ]
            , option [ value "kannada" ] [ text "Kannada" ]
            , option [ value "telugu" ] [ text "Telugu" ]
            , option [ value "english" ] [ text "English" ]
            ]
        ]

toolbarRow2 model =
    div [ class "toolbar-row" ]
        [ button [ onClick Play, disabled (model.playbackState == Playing) ] [ text "Play" ]
        , button [ onClick Pause, disabled (model.playbackState /= Playing) ] [ text "Pause" ]
        , button [ onClick Stop, disabled (model.playbackState == Stopped) ] [ text "Stop" ]
        , span [ class "separator" ] []
        , label [] [ input [ type_ "checkbox", checked model.loopEnabled, onCheck (\_ -> ToggleLoop) ] [], text "Loop" ]
        , span [ class "separator" ] []
        , label [] [ text "BPM:" ]
        , input [ type_ "range", Attr.min "10", Attr.max "300", value (String.fromFloat model.bpm)
                , onInput (\s -> SetBpm (String.toFloat s |> Maybe.withDefault 60)) ] []
        , span [] [ text (String.fromFloat model.bpm) ]
        , span [ class "spacer" ] []
        , button [ onClick ShowAbout ] [ text "About" ]
        ]
```

- [ ] **Step 2: Create View/Header.elm**

Composition header panel (HTML, not canvas):

```elm
module View.Header exposing (view)

view : Model -> Html Msg
view model =
    let
        meta = (present model.history).composition.metadata
    in
    div [ class "composition-header" ]
        [ div [ class "header-title-row" ]
            [ span [ class "title" ] [ text meta.title ]
            , span [ class "type-badge" ] [ text (compositionTypeLabel meta.compositionType) ]
            ]
        , div [ class "header-detail-row" ] (detailChips meta)
        , arohanRow meta
        , avarohanRow meta
        ]
```

- [ ] **Step 3: Create View/StatusBar.elm**

Scrollable log panel at bottom:

```elm
module View.StatusBar exposing (view)

view : Model -> Html Msg
view model =
    div [ class "status-bar" ]
        [ div [ class "status-header" ] [ text "Log" ]
        , div [ class "status-entries" ]
            (List.map (\msg -> div [ class "log-entry" ] [ text ("> " ++ msg) ]) model.statusLog)
        ]
```

- [ ] **Step 4: Create View/KeyboardLegend.elm**

Collapsible right sidebar with keyboard shortcut reference. Updates swar names based on current script.

- [ ] **Step 5: Create View/Layout.elm**

Top-level layout that assembles all components:

```elm
module View.Layout exposing (view)

view : Model -> Html Msg
view model =
    div [ class "app-container" ]
        [ View.Toolbar.view model
        , div [ class "main-area" ]
            [ div [ class "editor-panel" ]
                [ View.Header.view model
                , View.Canvas.view model
                ]
            , View.KeyboardLegend.view model
            ]
        , View.StatusBar.view model
        , dialogOverlays model
        ]
```

- [ ] **Step 6: Add CSS**

Create `sangeet-web/public/styles.css` with styles for:
- App container (full viewport, flex column)
- Toolbar rows (flex, gap, button styles)
- Main area (flex row, 72/28 split)
- Editor panel (flex column, scroll)
- Composition header (off-white background, border bottom)
- Status bar (120px preferred height, scroll)
- Keyboard legend (400px preferred width, left border, light background)
- Responsive: hide keyboard legend under 900px viewport width

Update `index.html` to include `<link rel="stylesheet" href="styles.css">`.

- [ ] **Step 7: Verify build**

```bash
cd sangeet-web && npx elm make src/Main.elm --output=public/elm.js
```

- [ ] **Step 8: Commit**

```bash
git add sangeet-web/src/View/ sangeet-web/public/
git commit -m "feat: implement Elm toolbar, composition header, status bar, keyboard legend, and layout"
```

---

### Task 18: Elm dialogs

**Files:**
- Create: `sangeet-web/src/View/Dialogs/NewComposition.elm`
- Create: `sangeet-web/src/View/Dialogs/Properties.elm`
- Create: `sangeet-web/src/View/Dialogs/About.elm`

- [ ] **Step 1: Create Dialogs/NewComposition.elm**

Modal form with all fields from the frontend spec Section 3.g:
- Title (text input, required)
- Type (select: Gat/Bandish/Palta)
- Raag (searchable select from loaded raags list, or custom text)
- Raag detection feedback label
- Laya (select, conditional on type)
- Taan count (number input, conditional on Gat)
- Stroke line checkbox
- Sahitya line checkbox (conditional, hidden for Palta)
- Taal (select from loaded taals list)
- Thaat, Arohan, Avrohan, Vadi, Samvadi (auto-filled from raag, editable)
- Script (select)
- Validation error label

Conditional visibility by composition type (Gat/Bandish/Palta) as specified.

On submit: validate fields, call `Api.Composition.createComposition(...)`, on success update model with new composition + cursor.

- [ ] **Step 2: Create Dialogs/Properties.elm**

Simpler form: Title (editable), Type (read-only), Raag (read-only), Taal (editable select).

- [ ] **Step 3: Create Dialogs/About.elm**

Simple modal with application info text. "Sangeet Notes Editor -- A web notation editor for Hindustani classical music..."

- [ ] **Step 4: Wire dialogs into View/Layout.elm**

Render dialog overlays based on `model.showNewDialog`, `model.showPropsDialog`, `model.showAboutDialog`.

- [ ] **Step 5: Verify build**

```bash
cd sangeet-web && npx elm make src/Main.elm --output=public/elm.js
```

- [ ] **Step 6: Commit**

```bash
git add sangeet-web/src/View/Dialogs/
git commit -m "feat: implement New Composition, Properties, and About dialogs"
```

---

### Task 19: Elm playback via Web Audio ports

**Files:**
- Modify: `sangeet-web/public/ports.js`
- Create: `sangeet-web/src/Ports.elm`
- Modify: `sangeet-web/src/State/Update.elm`

Implement audio playback using Elm ports to JavaScript Web Audio API.

- [ ] **Step 1: Create Ports.elm**

```elm
port module Ports exposing (playNotes, stopPlayback, downloadFile)

import Json.Encode as E

-- Send timed notes to JavaScript for Web Audio scheduling
port playNotes : E.Value -> Cmd msg

-- Stop all scheduled audio
port stopPlayback : () -> Cmd msg

-- Trigger file download (for save/export)
port downloadFile : { filename : String, mimeType : String, content : String } -> Cmd msg

-- Trigger binary file download (for PDF)
port downloadBinaryFile : { filename : String, mimeType : String, bytes : E.Value } -> Cmd msg
```

- [ ] **Step 2: Implement ports.js**

```javascript
function initPorts(app) {
  const audioCtx = new (window.AudioContext || window.webkitAudioContext)();

  // Simple sine wave oscillator for each note
  // Maps Note + Octave to frequency
  const noteFreqs = {
    sa: { mandra: 130.81, madhya: 261.63, taar: 523.25 },
    re: { mandra: 146.83, madhya: 293.66, taar: 587.33 },
    // ... all 12 chromatic notes (shuddha + komal/tivra)
  };

  let scheduledNodes = [];

  app.ports.playNotes.subscribe(function(timedNotesJson) {
    // Resume audio context (browser autoplay policy)
    audioCtx.resume();

    const notes = timedNotesJson;
    const startTime = audioCtx.currentTime + 0.1;

    notes.forEach(function(n) {
      const freq = getFrequency(n.note, n.variant, n.octave);
      if (!freq) return;

      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'triangle';  // Sitar-like timbre approximation
      osc.frequency.value = freq;
      gain.gain.value = 0.3;

      osc.connect(gain);
      gain.connect(audioCtx.destination);

      const noteStart = startTime + n.timeMs / 1000;
      const noteEnd = noteStart + n.durationMs / 1000;

      osc.start(noteStart);
      gain.gain.exponentialRampToValueAtTime(0.001, noteEnd);
      osc.stop(noteEnd + 0.05);

      scheduledNodes.push(osc);
    });
  });

  app.ports.stopPlayback.subscribe(function() {
    scheduledNodes.forEach(function(node) {
      try { node.stop(); } catch(e) {}
    });
    scheduledNodes = [];
  });

  app.ports.downloadFile.subscribe(function(data) {
    const blob = new Blob([data.content], { type: data.mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = data.filename;
    a.click();
    URL.revokeObjectURL(url);
  });

  app.ports.downloadBinaryFile.subscribe(function(data) {
    // data.bytes is a Uint8Array
    const blob = new Blob([new Uint8Array(data.bytes)], { type: data.mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = data.filename;
    a.click();
    URL.revokeObjectURL(url);
  });
}
```

- [ ] **Step 3: Wire playback in Update.elm**

On `Play`:
1. Get all events from all sections
2. Call `Api.Playback.schedulePlayback(events, bpm, matras)`
3. On `GotTimedNotes` response: encode timed notes as JSON, send to `Ports.playNotes`
4. Set `playbackState = Playing`

On `Stop`: call `Ports.stopPlayback()`, set `playbackState = Stopped`.
On `Pause`: same as Stop (MIDI pause is effectively stop for now).

- [ ] **Step 4: Verify build**

```bash
cd sangeet-web && npx elm make src/Main.elm --output=public/elm.js
```

- [ ] **Step 5: Commit**

```bash
git add sangeet-web/src/Ports.elm sangeet-web/public/ports.js sangeet-web/src/State/Update.elm
git commit -m "feat: implement Web Audio playback and file download via Elm ports"
```

---

### Task 20: Elm file operations

**Files:**
- Modify: `sangeet-web/src/State/Update.elm`
- Modify: `sangeet-web/src/State/Msg.elm`

Implement file open, save, and export operations using browser File API.

- [ ] **Step 1: Implement Open File**

On `OpenFile` msg:
1. Use `File.Select.file [".swar"]` to open a file picker
2. On `FileSelected file`: read file contents with `File.toString`
3. On `FileLoaded contents`: call `Api.Composition.parseComposition(contents)`
4. On success: set composition in undo history, request layout, log "Opened: filename"

- [ ] **Step 2: Implement Save File**

On `SaveFile` msg:
1. Call `Api.Composition.serializeComposition(composition)`
2. On `GotSerializeResult (Ok (ApiOk swarJson))`: call `Ports.downloadFile { filename = title ++ ".swar", mimeType = "application/json", content = swarJson }`

- [ ] **Step 3: Implement Export PDF**

On `ExportPdf` msg:
1. Call `Api.Export.exportPdf(composition, script)` (expects raw bytes)
2. On `GotPdfBytes (Ok bytes)`: call `Ports.downloadBinaryFile { filename = title ++ ".pdf", mimeType = "application/pdf", bytes = encodeBytes bytes }`

- [ ] **Step 4: Implement Export HTML**

On `ExportHtml` msg:
1. Call `Api.Export.exportHtml(composition, script)`
2. On `GotHtml (Ok (ApiOk html))`: call `Ports.downloadFile { filename = title ++ ".html", mimeType = "text/html", content = html }`

- [ ] **Step 5: Verify build**

```bash
cd sangeet-web && npx elm make src/Main.elm --output=public/elm.js
```

- [ ] **Step 6: Commit**

```bash
git add sangeet-web/src/
git commit -m "feat: implement file open, save, PDF export, and HTML export operations"
```

---

### Task 21: Makefile integration

**Files:**
- Modify: `Makefile` (or create if not present)

Add build commands for the web application.

- [ ] **Step 1: Add web targets to Makefile**

```makefile
# Web application targets

.PHONY: web-dev web-build server server-jar web-clean

# Start Elm dev server with live reload
web-dev:
	cd sangeet-web && npx elm-live src/Main.elm --open --dir=public -- --output=public/elm.js

# Production Elm build (optimized + minified)
web-build:
	cd sangeet-web && npx elm make src/Main.elm --optimize --output=public/elm.js
	cd sangeet-web && npx uglifyjs public/elm.js --compress "pure_funcs=[F2,F3,F4,F5,F6,F7,F8,F9,A2,A3,A4,A5,A6,A7,A8,A9],pure_getters,keep_fargs=false,unsafe_comps,unsafe" | npx uglifyjs --mangle --output public/elm.min.js

# Start Tapir server (development)
server:
	sbt "sangeet-server / run"

# Build server fat JAR
server-jar:
	sbt "sangeet-server / assembly"

# Clean web build artifacts
web-clean:
	rm -f sangeet-web/public/elm.js sangeet-web/public/elm.min.js
	rm -rf sangeet-web/elm-stuff

# Install Elm and Node tooling
web-setup:
	cd sangeet-web && npm init -y && npm install --save-dev elm elm-live uglify-js

# Build everything
all: web-build server-jar
```

- [ ] **Step 2: Add npm setup for sangeet-web**

Create `sangeet-web/package.json`:

```json
{
  "name": "sangeet-web",
  "private": true,
  "scripts": {
    "dev": "elm-live src/Main.elm --open --dir=public -- --output=public/elm.js",
    "build": "elm make src/Main.elm --optimize --output=public/elm.js",
    "format": "elm-format src/ --yes"
  },
  "devDependencies": {
    "elm": "^0.19.1-6",
    "elm-live": "^4.0.2",
    "elm-format": "^0.8.7",
    "uglify-js": "^3.17.4"
  }
}
```

- [ ] **Step 3: Update sangeet-web/.gitignore**

Add `node_modules/` if not already present.

- [ ] **Step 4: Verify**

```bash
make web-build
make server &  # in background
curl http://localhost:8080/health
kill %1
```

- [ ] **Step 5: Commit**

```bash
git add Makefile sangeet-web/package.json sangeet-web/.gitignore
git commit -m "build: add Makefile targets for web dev, build, and server"
```

---

### Task 22: Integration testing

**Files:**
- Create: `sangeet-web/tests/integration.sh`

End-to-end test: start the server, serve the Elm app, verify key flows.

- [ ] **Step 1: Create integration test script**

```bash
#!/bin/bash
set -e

echo "=== Sangeet Web Integration Test ==="

# Build
echo "Building Elm app..."
cd sangeet-web && npx elm make src/Main.elm --output=public/elm.js && cd ..

echo "Starting server..."
sbt "sangeet-server / run" &
SERVER_PID=$!
sleep 5

# Health check
echo "Health check..."
curl -sf http://localhost:8080/health | grep -q '"status":"ok"'
echo "  OK"

# Swagger UI
echo "Swagger UI..."
curl -sf http://localhost:8080/docs | grep -q "swagger"
echo "  OK"

# List taals
echo "List taals..."
TAAL_COUNT=$(curl -sf http://localhost:8080/api/v1/taals | python3 -c "import sys,json; print(len(json.load(sys.stdin)['data']['taals']))")
[ "$TAAL_COUNT" -eq 11 ] && echo "  OK (11 taals)" || echo "  FAIL (expected 11, got $TAAL_COUNT)"

# List raags
echo "List raags..."
RAAG_COUNT=$(curl -sf http://localhost:8080/api/v1/raags | python3 -c "import sys,json; print(len(json.load(sys.stdin)['data']['raags']))")
[ "$RAAG_COUNT" -eq 26 ] && echo "  OK (26 raags)" || echo "  FAIL (expected 26, got $RAAG_COUNT)"

# Create composition
echo "Create composition..."
COMP_RESPONSE=$(curl -sf -X POST http://localhost:8080/api/v1/compositions \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Gat","compositionType":"gat","taalName":"teentaal","raagName":"yaman","laya":"vilambit","taanCount":2,"showStrokeLine":true,"showSahityaLine":false}')
echo "$COMP_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); assert d['ok']==True; print('  OK')"

# Insert swar
echo "Insert swar..."
COMPOSITION=$(echo "$COMP_RESPONSE" | python3 -c "import sys,json; print(json.dumps(json.load(sys.stdin)['data']['composition']))")
CURSOR=$(echo "$COMP_RESPONSE" | python3 -c "import sys,json; print(json.dumps(json.load(sys.stdin)['data']['cursor']))")
SWAR_RESPONSE=$(curl -sf -X POST http://localhost:8080/api/v1/editor/insert-swar \
  -H "Content-Type: application/json" \
  -d "{\"composition\":$COMPOSITION,\"sectionIndex\":0,\"cursor\":$CURSOR,\"note\":\"ga\",\"shiftDown\":false}")
echo "$SWAR_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); assert d['ok']==True; assert d['data']['message']=='Ga'; print('  OK')"

# Compute layout
echo "Compute layout..."
UPDATED_COMP=$(echo "$SWAR_RESPONSE" | python3 -c "import sys,json; print(json.dumps(json.load(sys.stdin)['data']['composition']))")
LAYOUT_RESPONSE=$(curl -sf -X POST http://localhost:8080/api/v1/layout/compute \
  -H "Content-Type: application/json" \
  -d "{\"composition\":$UPDATED_COMP}")
echo "$LAYOUT_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); assert d['ok']==True; assert len(d['data']['grids'])>0; print('  OK')"

# Export PDF
echo "Export PDF..."
PDF_STATUS=$(curl -sf -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/v1/export/pdf \
  -H "Content-Type: application/json" \
  -d "{\"composition\":$UPDATED_COMP,\"script\":\"devanagari\"}")
[ "$PDF_STATUS" -eq 200 ] && echo "  OK" || echo "  FAIL (HTTP $PDF_STATUS)"

# Colors
echo "Get colors..."
curl -sf http://localhost:8080/api/v1/rendering/colors | python3 -c "import sys,json; d=json.load(sys.stdin); assert d['ok']==True; assert 'taalMarker' in d['data']; print('  OK')"

# Cleanup
kill $SERVER_PID 2>/dev/null
echo ""
echo "=== All integration tests passed ==="
```

- [ ] **Step 2: Make executable and run**

```bash
chmod +x sangeet-web/tests/integration.sh
./sangeet-web/tests/integration.sh
```

- [ ] **Step 3: Add Makefile target**

```makefile
integration-test:
	./sangeet-web/tests/integration.sh
```

- [ ] **Step 4: Commit**

```bash
git add sangeet-web/tests/ Makefile
git commit -m "test: add end-to-end integration test script for web app"
```

---

## Summary

| Task | Description | Estimated Time |
|------|-------------|---------------|
| 1 | Add sangeet-server to sbt build | 5 min |
| 2 | Response envelope and error mapping | 5 min |
| 3 | Tapir endpoints -- Reference/Rendering (GET) | 5 min |
| 4 | Tapir endpoints -- Composition/Editor/Cursor | 5 min |
| 5 | Tapir endpoints -- remaining 6 categories | 10 min |
| 6 | Server routes -- Reference/Rendering | 10 min |
| 7 | Server routes -- Composition/Editor/Cursor | 15 min |
| 8 | Server routes -- remaining 6 categories | 15 min |
| 9 | Swagger UI, CORS, health check, server startup | 10 min |
| 10 | Server integration tests | 10 min |
| 11 | Elm project setup | 5 min |
| 12 | Elm domain model types + JSON codecs | 20 min |
| 13 | Elm API client (all 36 endpoints) | 15 min |
| 14 | Elm application state (Model/Msg/Update) | 20 min |
| 15 | Elm canvas rendering (5 rows) | 25 min |
| 16 | Elm keyboard input handling | 10 min |
| 17 | Elm toolbar and controls | 15 min |
| 18 | Elm dialogs | 10 min |
| 19 | Elm playback via Web Audio ports | 10 min |
| 20 | Elm file operations | 10 min |
| 21 | Makefile integration | 5 min |
| 22 | Integration testing | 10 min |
| **Total** | | **~4 hours** |

**Dependency order:** Tasks 1-10 (server) are independent of Tasks 11-20 (frontend) and can be parallelized. Task 21-22 require both sides complete.

**Feature parity exclusions (web vs desktop):**
- Voice recognition (no Whisper in browser -- would need Whisper WASM, deferred)
- Direct filesystem access (replaced by File API download/upload)
- Auto-save to disk (replaced by localStorage or manual save)
- Single-instance detection (not applicable to web)
