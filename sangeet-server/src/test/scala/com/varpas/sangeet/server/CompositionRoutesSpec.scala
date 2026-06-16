package com.varpas.sangeet.server

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.parser._
import io.circe.syntax._
import org.http4s._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.tapir.server.http4s.Http4sServerInterpreter

import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.server.routes.CompositionRoutes

class CompositionRoutesSpec extends AnyFlatSpec with Matchers:

  import TestFixtures.*

  val routes = Http4sServerInterpreter[IO]().toRoutes(CompositionRoutes.all).orNotFound

  // Plan 19 T2D: removed example tests subsumed by
  // `routes/CompositionRoutesPropSpec` properties — the create-envelope happy
  // paths (gat / bandish) and the simple serialize/parse/roundtrip tests now
  // run as N=100 properties per CI run. Kept: palta+taanCount (not in
  // generator), the version="2.0" / desktop-byte-parity golden assertions
  // (PBT verifies round-trip, not wire-format byte stability), and the
  // specific "{invalid json!!!}" → 4xx case.

  // --- create ---

  // NOTE: removed "create a Gat" + "create a Bandish" happy paths — subsumed
  // by `CompositionRoutesPropSpec::propCreateCompositionEnvelope` (every body
  // from `RequestGenerators.genCompositionRequestBody`, which iterates over
  // gat + bandish × all raags × all taals × all laya, returns 200 + success
  // envelope with metadata + sections).

  "POST /api/v1/compositions" should "create a Palta composition with taan count" in {
    val body = Json.obj(
      "title"           -> Json.fromString("Yaman Palta"),
      "compositionType" -> Json.fromString("palta"),
      "taal"            -> teentaal.asJson,
      "raag"            -> yaman.asJson,
      "taanCount"       -> Json.fromInt(3)
    )
    val req  = postRequest(uri"/api/v1/compositions", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  // NOTE: removed "reject missing required fields" — subsumed by
  // `CompositionRoutesPropSpec::propCreateMissingRequiredField4xx` (the
  // property drops each of title / compositionType / taal / raag in turn
  // and asserts 4xx, covering all four required fields rather than just
  // the single "only title present" example).

  // --- serialize ---

  // NOTE: removed "serialize a composition to JSON string" — subsumed by
  // `CompositionRoutesPropSpec::propCompositionRoundTrip` (which exercises
  // serialize→parse and asserts the round-tripped composition equals the
  // input, a strictly stronger claim than the single-field title check here).

  "POST /api/v1/compositions/serialize" should "return byte-identical output for the same composition" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson
    )
    val req1  = postRequest(uri"/api/v1/compositions/serialize", body)
    val resp1 = routes.run(req1).unsafeRunSync()
    val first = resp1.as[String].unsafeRunSync()

    val req2   = postRequest(uri"/api/v1/compositions/serialize", body)
    val resp2  = routes.run(req2).unsafeRunSync()
    val second = resp2.as[String].unsafeRunSync()

    first shouldBe second
    first should startWith("{")
    first should include("\"version\":\"2.0\"")
    first should include("\"title\":\"Test Composition\"")
  }

  it should "match desktop's SwarFormat.writeFile byte-for-byte" in {
    import com.varpas.sangeet.core.format.SwarFormat
    import com.varpas.sangeet.core.api.CompositionApi

    val desktopOutput = SwarFormat.toJson(minimalComposition).noSpaces
    val serverOutput  = CompositionApi.serializeCompositionString(minimalComposition)

    serverOutput shouldBe desktopOutput
  }

  // --- parse ---

  // NOTE: removed "parse a valid JSON string into composition" — subsumed by
  // `CompositionRoutesPropSpec::propCompositionRoundTrip`. The
  // "reject invalid JSON" example below is kept because the property only
  // generates well-formed compositions; it does NOT exercise a syntactically
  // broken JSON payload through /parse.

  "POST /api/v1/compositions/parse" should "reject invalid JSON" in {
    val body = Json.obj(
      "json" -> Json.fromString("{invalid json!!!}")
    )
    val req  = postRequest(uri"/api/v1/compositions/parse", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- roundtrip ---

  // NOTE: removed "Compositions serialize then parse should produce
  // equivalent composition" — subsumed by
  // `CompositionRoutesPropSpec::propCompositionRoundTrip`, which checks
  // equivalence by deep structural equality of the round-tripped Composition
  // (stronger than the title-only assertion this example made).
