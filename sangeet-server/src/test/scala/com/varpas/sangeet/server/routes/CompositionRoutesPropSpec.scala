package com.varpas.sangeet.server.routes

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.parser._
import io.circe.syntax._
import org.http4s._
import org.http4s.implicits._
import org.scalacheck.Shrink
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sttp.tapir.server.http4s.Http4sServerInterpreter

import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.core.generators.Generators
import com.varpas.sangeet.core.model.Composition
import com.varpas.sangeet.server.generators.RequestGenerators

/** Plan 19 T2B — composition route contracts.
  *
  * The headline property is the serialize→parse round-trip: for every Composition the core domain generators can build,
  * `POST /api/v1/compositions/serialize` followed by `POST /api/v1/compositions/parse` reproduces the same composition.
  * This generalises the example test "Compositions serialize then parse → equivalent" (which used a single
  * `minimalComposition` fixture) to all 100+ generated compositions per CI run, and covers the round-trip claim
  * documented in the OpenAPI spec.
  *
  * Properties also cover:
  *   - create-endpoint: any well-formed body (raag/taal from registry + valid laya) returns 200 with a Composition
  *     under `data`
  *   - missing required fields → 4xx (universal — generates a body with one required field stripped)
  *   - serialize is deterministic — calling it twice returns byte-identical output
  */
class CompositionRoutesPropSpec extends AnyFunSuite with Matchers with ScalaCheckPropertyChecks:

  private val routes: HttpApp[IO] =
    Http4sServerInterpreter[IO]().toRoutes(CompositionRoutes.all).orNotFound

  private given Shrink[String]      = Shrink.shrinkAny[String]
  private given Shrink[Json]        = Shrink.shrinkAny[Json]
  private given Shrink[Composition] = Shrink.shrinkAny[Composition]

  private def post(path: String, body: String): Response[IO] =
    val req = Request[IO](Method.POST, Uri.unsafeFromString(path))
      .withEntity(body)
      .withContentType(headers.`Content-Type`(MediaType.application.json))
    routes.run(req).unsafeRunSync()

  private def postJson(path: String, body: Json): Response[IO] = post(path, body.noSpaces)

  test("propCreateCompositionEnvelope: every well-formed create body returns 200 with success envelope") {
    forAll(RequestGenerators.genCompositionRequestBody) { body =>
      val resp = post("/api/v1/compositions", body)
      resp.status shouldBe Status.Ok
      val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("response not JSON"))
      json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
      // The created composition must include a Metadata block (title round-trip checked
      // in the example tests; here we just assert the structural envelope is intact).
      json.hcursor.downField("data").downField("metadata").succeeded shouldBe true
      json.hcursor.downField("data").downField("sections").succeeded shouldBe true
    }
  }

  test("propCompositionRoundTrip: serialize→parse reproduces the composition") {
    // The wire format itself is byte-stable (golden tests already pin that). The
    // server-side property here is endpoint-shape: post the composition to /serialize,
    // take the body back, post it to /parse, and check the round-tripped composition
    // is structurally equal to the original.
    forAll(Generators.genComposition) { comp =>
      val serBody = Json.obj("composition" -> comp.asJson).noSpaces
      val serResp = post("/api/v1/compositions/serialize", serBody)
      serResp.status shouldBe Status.Ok

      val serializedStr = serResp.as[String].unsafeRunSync()

      val parseBody = Json.obj("json" -> Json.fromString(serializedStr)).noSpaces
      val parseResp = post("/api/v1/compositions/parse", parseBody)
      parseResp.status shouldBe Status.Ok

      val parsedJson = parse(parseResp.as[String].unsafeRunSync()).getOrElse(fail("parse response not JSON"))
      parsedJson.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

      val parsedComp = parsedJson.hcursor.downField("data").as[Composition] match
        case Right(c)  => c
        case Left(err) => fail(s"Failed to decode round-tripped composition: $err")

      parsedComp shouldBe comp
    }
  }

  test("propSerializeDeterministic: serialise twice → byte-identical output") {
    forAll(Generators.genComposition) { comp =>
      val body   = Json.obj("composition" -> comp.asJson).noSpaces
      val first  = post("/api/v1/compositions/serialize", body).as[String].unsafeRunSync()
      val second = post("/api/v1/compositions/serialize", body).as[String].unsafeRunSync()
      first shouldBe second
    }
  }

  test("propCreateMissingRequiredField4xx: dropping a required field never returns 5xx") {
    // The endpoint requires title + compositionType + taal + raag at minimum.
    // For each one, take a valid body, drop the field, post, and assert the
    // response is a 4xx (never a 500). This generalises the single
    // "reject missing required fields" example to all four required fields.
    val requiredFields = List("title", "compositionType", "taal", "raag")
    val gen = for
      bodyStr <- RequestGenerators.genCompositionRequestBody
      field   <- org.scalacheck.Gen.oneOf(requiredFields)
    yield (bodyStr, field)

    forAll(gen) { case (bodyStr, fieldToDrop) =>
      val json    = parse(bodyStr).getOrElse(fail("seed body not JSON")).asObject.getOrElse(fail("seed not object"))
      val mangled = Json.fromJsonObject(json.remove(fieldToDrop)).noSpaces
      val resp    = postJson("/api/v1/compositions", parse(mangled).getOrElse(fail("re-parse")))
      resp.status.code should (be >= 400 and be < 500)
    }
  }
