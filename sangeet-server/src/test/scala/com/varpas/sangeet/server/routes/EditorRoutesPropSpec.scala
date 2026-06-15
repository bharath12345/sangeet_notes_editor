package com.varpas.sangeet.server.routes

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.parser._
import org.http4s._
import org.http4s.implicits._
import org.scalacheck.Shrink
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sttp.tapir.server.http4s.Http4sServerInterpreter

import com.varpas.sangeet.server.generators.RequestGenerators

/** Plan 19 T2B — editor route contract properties.
  *
  * The editor routes are the broadest API surface (13 endpoints, each accepting an EditorInput envelope plus
  * endpoint-specific payload). Rather than spec each one separately the property here asserts the universal envelope
  * contract:
  *
  *   - any well-formed request returns 200 OK with `{success: true, data: {...}}` containing both `composition` and
  *     `cursor` keys
  *   - any malformed request (random JSON object missing the required envelope fields) returns 4xx — never 5xx
  *
  * Per-endpoint correctness (insert-swar actually inserts a Sa, change-starting-beat synthesises locked beats, etc.) is
  * covered by the existing `EditorRoutesSpec` example tests; this spec covers the cross-cutting envelope contract that
  * an example-by-example test cannot.
  */
class EditorRoutesPropSpec extends AnyFunSuite with Matchers with ScalaCheckPropertyChecks:

  private val routes: HttpApp[IO] =
    Http4sServerInterpreter[IO]().toRoutes(EditorRoutes.all).orNotFound

  private given Shrink[String] = Shrink.shrinkAny[String]
  private given Shrink[Json]   = Shrink.shrinkAny[Json]

  private def post(path: String, body: Json): Response[IO] =
    val req = Request[IO](Method.POST, Uri.unsafeFromString(path))
      .withEntity(body.noSpaces)
      .withContentType(headers.`Content-Type`(MediaType.application.json))
    routes.run(req).unsafeRunSync()

  // Endpoints that take only an EditorInput envelope (no extra fields required).
  // `insert-swar-group` is included with the `notes` field set to an empty array
  // by the generator (it accepts `[]` as a no-op — covered by an example test).
  private val envelopeOnlyEndpoints = List(
    "/api/v1/editor/insert-chikari",
    "/api/v1/editor/insert-rest",
    "/api/v1/editor/insert-sustain",
    "/api/v1/editor/delete-at-cursor"
  )

  test("propEditorEnvelopeSuccess: well-formed EditorInput returns 200 with success envelope") {
    forAll(RequestGenerators.genEditorInputBody) { body =>
      envelopeOnlyEndpoints.foreach { path =>
        val resp = post(path, body)
        resp.status shouldBe Status.Ok
        val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail(s"$path: not JSON"))
        json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
        val data = json.hcursor.downField("data")
        data.downField("composition").succeeded shouldBe true
        data.downField("cursor").succeeded shouldBe true
      }
    }
  }

  test("propInsertSwarEnvelope: well-formed insert-swar body returns 200 with success envelope") {
    // insert-swar additionally requires note/variant/octave — covered by genInsertSwarBody.
    forAll(RequestGenerators.genInsertSwarBody) { body =>
      val resp = post("/api/v1/editor/insert-swar", body)
      resp.status shouldBe Status.Ok
      val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("not JSON"))
      json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
      val data = json.hcursor.downField("data")
      data.downField("composition").succeeded shouldBe true
      data.downField("cursor").succeeded shouldBe true
    }
  }

  test("propInsertDualSwarEnvelope: well-formed insert-dual-swar body returns 200 with success envelope") {
    forAll(RequestGenerators.genInsertSwarBody) { body =>
      val resp = post("/api/v1/editor/insert-dual-swar", body)
      resp.status shouldBe Status.Ok
      val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("not JSON"))
      json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    }
  }

  test("propEditorMalformedNeverServerError: random non-EditorInput JSON returns 4xx (never 5xx)") {
    // Build a body that does NOT match the EditorInput envelope — a single key
    // with a primitive value. The route must reject this with a 4xx, never 5xx.
    val genGarbage: org.scalacheck.Gen[Json] =
      for
        key <- org.scalacheck.Gen.alphaStr.suchThat(_.nonEmpty).map(_.take(8))
        v   <- org.scalacheck.Gen.alphaStr.suchThat(_.nonEmpty).map(_.take(8))
      yield Json.obj(key -> Json.fromString(v))

    val allEnvelopeEndpoints = envelopeOnlyEndpoints :+ "/api/v1/editor/insert-swar"
    forAll(genGarbage) { body =>
      allEnvelopeEndpoints.foreach { path =>
        val resp = post(path, body)
        resp.status.code should (be >= 400 and be < 500)
        // The error envelope is always present for our 4xx responses (ApiEnvelope.failure).
        val parsed = parse(resp.as[String].unsafeRunSync())
        parsed.toOption.foreach { json =>
          json.hcursor.get[Boolean]("success").getOrElse(true) shouldBe false
        }
      }
    }
  }
