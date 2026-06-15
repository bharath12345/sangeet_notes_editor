package com.varpas.sangeet.server.generators

import io.circe.Json
import io.circe.syntax._
import org.scalacheck.Gen

import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.taal.Taals

/** Generators for sangeet-server REST request shapes.
  *
  * Plan 19 T2A — PBT genesis for the server module. These compose the core domain generators (T1A:
  * `com.varpas.sangeet.core.generators.Generators`) with the JSON / path-parameter shapes the Tapir endpoints actually
  * accept.
  *
  * Phase A intentionally scopes to:
  *   - `genGetRaagPath` / `genGetTaalPath` — path params for the reference endpoints (`GET /api/v1/raags/{name}`, `GET
  *     /api/v1/taals/{name}`). Drawn from the built-in registries so every value is a known-good fixture and properties
  *     exercise the "happy path" contract.
  *   - `genCompositionRequestBody` — a `POST /api/v1/compositions` JSON body matching the create endpoint's shape (a
  *     flat metadata document, not a full Composition). Once T1A's `genComposition` exists this can be swapped to
  *     encode an arbitrary Composition for the `parse` / `serialize` endpoints which DO take a full composition.
  *
  * No `CursorRequest` type exists in sangeet-server today — cursors are passed as raw circe `Json`. If a typed request
  * envelope is introduced later, add `genCursorRequest` here.
  */
object RequestGenerators:

  /** Lowercase canonical raag names registered in [[Raags.all]] (26 entries). */
  val genGetRaagPath: Gen[String] =
    Gen.oneOf(Raags.all.keys.toSeq)

  /** Lowercase canonical taal names registered in [[Taals.all]] (11 entries). */
  val genGetTaalPath: Gen[String] =
    Gen.oneOf(Taals.all.keys.toSeq)

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
