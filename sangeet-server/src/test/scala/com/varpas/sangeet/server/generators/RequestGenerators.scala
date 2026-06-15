package com.varpas.sangeet.server.generators

import io.circe.Json
import io.circe.syntax._
import org.scalacheck.Gen

import com.varpas.sangeet.core.editor.CursorModel
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.core.generators.Generators
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.taal.Taals

/** Generators for sangeet-server REST request shapes.
  *
  * Plan 19 T2A introduced this module. T2B extends it with cursor / editor-input / bug-report request-body shapes used
  * by the bulk endpoint-contract properties.
  *
  * These compose the core domain generators (T1A: `com.varpas.sangeet.core.generators.Generators`) with the JSON /
  * path-parameter shapes the Tapir endpoints actually accept. They are intentionally API-shape only — anything that
  * generates a domain type belongs in core's `Generators`, not here.
  *
  * The cursor / editor-input bodies build their JSON envelope the same way [[TestFixtures]] does, so the wire-shape
  * stays identical to the example tests while letting properties exercise hundreds of distinct (cursor, composition,
  * note) tuples per run.
  */
object RequestGenerators:

  // ---- Path params --------------------------------------------------------------------------

  /** Lowercase canonical raag names registered in [[Raags.all]] (26 entries). */
  val genGetRaagPath: Gen[String] =
    Gen.oneOf(Raags.all.keys.toSeq)

  /** Lowercase canonical taal names registered in [[Taals.all]] (11 entries). */
  val genGetTaalPath: Gen[String] =
    Gen.oneOf(Taals.all.keys.toSeq)

  /** Names that are NOT in the registries. Used to assert the "not found → 404 (never 500)" contract for the `GET
    * /api/v1/raags/{name}` and `GET /api/v1/taals/{name}` endpoints.
    *
    * Built by combining a short ascii-only suffix with a known-bad prefix; rejecting any accidental hits keeps the
    * generator total without coupling to a hand-maintained blocklist.
    */
  private val genUnknownPath: Gen[String] =
    for
      prefix <- Gen.oneOf("nonexistent", "made-up", "bogus", "xyz")
      n      <- Gen.choose(0, 6)
      suffix <- Gen.listOfN(n, Gen.alphaNumChar).map(_.mkString)
    yield s"$prefix-$suffix"

  val genUnknownRaagPath: Gen[String] =
    genUnknownPath.suchThat(s => !Raags.all.contains(s.toLowerCase))

  val genUnknownTaalPath: Gen[String] =
    genUnknownPath.suchThat(s => !Taals.all.contains(s.toLowerCase))

  // ---- Composition create body --------------------------------------------------------------

  /** Laya values the create endpoint accepts. Matches the JSON encoding of [[com.varpas.sangeet.core.model.Laya]]
    * (lowercase enum name).
    */
  private val genLaya: Gen[String] =
    Gen.oneOf("vilambit", "madhya", "drut")

  /** Composition type values the create endpoint accepts. Matches the JSON encoding of
    * [[com.varpas.sangeet.core.model.CompositionType]].
    */
  private val genCompositionType: Gen[String] =
    Gen.oneOf("gat", "bandish")

  /** A short, ASCII-safe composition title. Property-based tests stress the endpoint with strings of varying length but
    * bounded character set to avoid accidentally exercising Unicode-handling concerns unrelated to the request-shape
    * contract.
    */
  private val genTitle: Gen[String] =
    Gen
      .chooseNum(1, 40)
      .flatMap(n => Gen.listOfN(n, Gen.alphaNumChar))
      .map(_.mkString.trim)
      .suchThat(_.nonEmpty)

  /** JSON body for `POST /api/v1/compositions`.
    *
    * Mirrors the body shape exercised by `CompositionRoutesSpec` — the endpoint constructs a Metadata + empty Section
    * under the hood from these fields, so we don't need a full Composition here.
    *
    * Uses the same circe `Codecs.given` instances that the route uses, so the bodies are guaranteed wire-compatible
    * with the server's decoder.
    */
  val genCompositionRequestBody: Gen[String] =
    for
      title    <- genTitle
      compType <- genCompositionType
      raagKey  <- genGetRaagPath
      taalKey  <- genGetTaalPath
      laya     <- genLaya
    yield
      val raag = Raags.byName(raagKey).get
      val taal = Taals.byName(taalKey).get
      Json
        .obj(
          "title"           -> Json.fromString(title),
          "compositionType" -> Json.fromString(compType),
          "taal"            -> taal.asJson,
          "raag"            -> raag.asJson,
          "laya"            -> Json.fromString(laya),
          "showStrokeLine"  -> Json.fromBoolean(false),
          "showSahityaLine" -> Json.fromBoolean(false)
        )
        .noSpaces

  // ---- Cursor envelope generators ----------------------------------------------------------

  /** A cursor positioned within a valid range for some built-in taal. The beat is in `[0, matras-1]`; cycle in `[0,
    * 4]`; subdivisions in `[1, 4]`. Keeping the bounds tight matches the example tests' expectations and avoids
    * exercising the "no upper bound validation" tail of [[CursorApi.moveTo]] which already has a dedicated example
    * test.
    */
  val genCursorModel: Gen[CursorModel] =
    for
      taal              <- Generators.genTaal
      cycle             <- Gen.choose(0, 4)
      beat              <- Gen.choose(0, taal.matras - 1)
      totalSubdivisions <- Gen.choose(1, 4)
      subIndex          <- Gen.choose(0, totalSubdivisions - 1)
      octave            <- Generators.genOctave
    yield CursorModel(taal, cycle, beat, subIndex, totalSubdivisions, octave)

  /** Serialise a cursor to the JSON envelope the cursor routes accept. Lower-case octave to match the route's tolerant
    * decoder ([[JsonParsing.parseOctaveString]]).
    */
  def cursorJson(cursor: CursorModel): Json =
    Json.obj(
      "taal"              -> cursor.taal.asJson,
      "cycle"             -> Json.fromInt(cursor.cycle),
      "beat"              -> Json.fromInt(cursor.beat),
      "subIndex"          -> Json.fromInt(cursor.subIndex),
      "totalSubdivisions" -> Json.fromInt(cursor.totalSubdivisions),
      "currentOctave"     -> Json.fromString(cursor.currentOctave.toString.toLowerCase)
    )

  /** Top-level JSON body wrapping a cursor under the `"cursor"` key — the shape every cursor route (POST
    * `/api/v1/cursor/next-beat`, `prev-beat`, etc.) expects.
    */
  val genCursorRequestBody: Gen[Json] =
    genCursorModel.map(cur => Json.obj("cursor" -> cursorJson(cur)))

  // ---- Editor envelope generators -----------------------------------------------------------

  /** A minimal composition tailored for editor-route properties.
    *
    * We deliberately use an empty-section composition with the cursor's own taal as the metadata taal — this keeps the
    * (composition, sectionIndex, cursor) triple internally consistent so the route's [[parseEditorInput]] decoder
    * succeeds. Tests for the "invalid input" 4xx branch construct broken bodies by hand; the property here covers the
    * "well-formed input → success envelope" half of the contract.
    */
  private def emptyCompositionFor(cursor: CursorModel): Composition =
    val metadata = Metadata(
      title = "PropTest",
      compositionType = CompositionType.Gat,
      raag = Raags.byName("yaman").get,
      taal = cursor.taal,
      laya = Some(Laya.Madhya),
      instrument = None,
      composer = None,
      author = None,
      source = None,
      showStrokeLine = false,
      showSahityaLine = false,
      createdAt = "2026-06-15T00:00:00Z",
      updatedAt = "2026-06-15T00:00:00Z"
    )
    val section = Section(
      name = "Sthayi",
      sectionType = SectionType.Sthayi,
      events = Nil,
      tihai = None,
      startingBeat = 1
    )
    Composition(metadata, List(section))

  /** Editor-input JSON wrapping a composition + sectionIndex=0 + cursor. The contents satisfy [[parseEditorInput]]'s
    * required fields.
    */
  val genEditorInputBody: Gen[Json] =
    genCursorModel.map { cursor =>
      val comp = emptyCompositionFor(cursor)
      Json.obj(
        "composition"  -> comp.asJson,
        "sectionIndex" -> Json.fromInt(0),
        "cursor"       -> cursorJson(cursor)
      )
    }

  /** Editor-input JSON for `insert-swar` / `insert-dual-swar` — extends the editor input with `note`/`variant`/`octave`
    * fields, drawn from the achal-aware variant picker so generated swars are musically valid by construction.
    */
  val genInsertSwarBody: Gen[Json] =
    for
      base    <- genEditorInputBody
      note    <- Generators.genNote
      variant <- Generators.variantFor(note)
      octave  <- Generators.genOctave
    yield base.deepMerge(
      Json.obj(
        "note"    -> Json.fromString(note.toString.toLowerCase),
        "variant" -> Json.fromString(variant.toString.toLowerCase),
        "octave"  -> Json.fromString(octave.toString.toLowerCase)
      )
    )

  // ---- Bug-report body generators ----------------------------------------------------------

  /** Generates arbitrary JSON bodies for `POST /api/v1/bug-reports`. The endpoint accepts any JSON (see
    * `BugReportEndpoints.createBugReport` — schema is intentionally open in the MVP). Properties using this generator
    * assert the "never 5xx on a well-formed JSON body" contract — the response must be either 200/202 (accepted) or
    * 4xx/503 (rejected with a diagnostic), never an unhandled 500.
    *
    * Builds a mix of shapes: empty objects, descriptions-only, full web-rrweb-style payloads with a `replay` array, and
    * desktop-style payloads with `metadata`. Excludes raw `null` / `1` / `"x"` at the top level since the endpoint's
    * `jsonBody[Json]` decoder requires a JSON object — those would fail decoding before reaching our logic, and the 400
    * in that case is Tapir's contract, not ours.
    */
  val genBugReportBody: Gen[Json] =
    val genShortStr: Gen[String] =
      Gen.choose(1, 32).flatMap(Gen.listOfN(_, Gen.alphaNumChar)).map(_.mkString)

    val genReplayEvent: Gen[Json] =
      for
        ty <- Gen.choose(0, 5)
        ts <- Gen.choose(0L, 1000000L)
      yield Json.obj("type" -> Json.fromInt(ty), "timestamp" -> Json.fromLong(ts))

    val genMetadata: Gen[Json] =
      for
        url       <- genShortStr
        userAgent <- genShortStr
        w         <- Gen.choose(320, 4000)
        h         <- Gen.choose(240, 4000)
      yield Json.obj(
        "url"       -> Json.fromString(url),
        "userAgent" -> Json.fromString(userAgent),
        "viewportW" -> Json.fromInt(w),
        "viewportH" -> Json.fromInt(h)
      )

    Gen.frequency(
      // Minimal — empty object.
      1 -> Gen.const(Json.obj()),
      // Description only.
      3 -> genShortStr.map(d => Json.obj("description" -> Json.fromString(d))),
      // Web-style with replay events.
      3 -> (for
        d  <- genShortStr
        n  <- Gen.choose(0, 5)
        ev <- Gen.listOfN(n, genReplayEvent)
      yield Json.obj(
        "type"        -> Json.fromString("web"),
        "description" -> Json.fromString(d),
        "replay"      -> Json.arr(ev*)
      )),
      // Desktop-style with metadata.
      3 -> (for
        d <- genShortStr
        m <- genMetadata
      yield Json.obj(
        "type"        -> Json.fromString("desktop"),
        "description" -> Json.fromString(d),
        "metadata"    -> m
      )),
      // Unknown extra fields — the route must tolerate them.
      2 -> (for
        d <- genShortStr
        k <- genShortStr
        v <- genShortStr
      yield Json.obj(
        "description" -> Json.fromString(d),
        k             -> Json.fromString(v)
      ))
    )
