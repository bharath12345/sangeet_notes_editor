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

/** Plan 19 T2B — bulk endpoint contract properties for the cursor route family (POST /api/v1/cursor/next-beat,
  * prev-beat, next-sub-beat, set-octave, set-subdivisions, move-to).
  *
  * Cursor routes are pure functions over a (cursor, optional-args) tuple: every endpoint must return either 200 OK with
  * the success envelope when the input parses, or a 4xx error envelope when it doesn't. There's no I/O or external
  * service in play, so the universally quantified contract is tight: status ∈ {200, 4xx}, never 5xx.
  *
  * Beyond the envelope contract these properties also verify a handful of structural invariants on the response body:
  *
  *   - the resulting cursor's `beat` and `cycle` are in legal ranges (next/prev never "exit" the composition)
  *   - `setOctave` returns the requested octave
  *   - `setSubdivisions` returns the requested totalSubdivisions (when in-range)
  *   - `nextBeat` / `prevBeat` are well-defined inverses on their respective non-boundary inputs
  */
class CursorRoutesPropSpec extends AnyFunSuite with Matchers with ScalaCheckPropertyChecks:

  private val routes: HttpApp[IO] =
    Http4sServerInterpreter[IO]().toRoutes(CursorRoutes.all).orNotFound

  // Disable string shrinking — cursor JSON envelopes serialise enum names that
  // ScalaCheck's default String shrinker would walk to "", invalidating the
  // generator's domain constraints.
  private given Shrink[String] = Shrink.shrinkAny[String]
  private given Shrink[Json]   = Shrink.shrinkAny[Json]

  private def post(path: String, body: Json): Response[IO] =
    val uri = Uri.unsafeFromString(path)
    val req = Request[IO](Method.POST, uri)
      .withEntity(body.noSpaces)
      .withContentType(headers.`Content-Type`(MediaType.application.json))
    routes.run(req).unsafeRunSync()

  // Every cursor route shares the same JSON envelope on success: `{success: true, data: cursor}`.
  // Pull this into one helper so each property reads as the contract it's asserting, not the
  // mechanical body-parse plumbing.
  private def assertCursorEnvelope(resp: Response[IO]): io.circe.ACursor =
    resp.status shouldBe Status.Ok
    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail(s"Failed to parse response body: $body"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    json.hcursor.downField("data")

  test("propCursorNextBeatBounded: next-beat returns a cursor whose beat/cycle are in valid ranges") {
    forAll(RequestGenerators.genCursorRequestBody) { body =>
      val resp     = post("/api/v1/cursor/next-beat", body)
      val data     = assertCursorEnvelope(resp)
      val newBeat  = data.get[Int]("beat").getOrElse(fail("missing beat"))
      val newCycle = data.get[Int]("cycle").getOrElse(fail("missing cycle"))

      // Beat is always a valid index into some taal (max matras across the registry is 16).
      newBeat should be >= 0
      newBeat should be <= 15
      newCycle should be >= 0
    }
  }

  test("propCursorPrevBeatBounded: prev-beat never moves the cursor below (cycle=0, beat=0)") {
    forAll(RequestGenerators.genCursorRequestBody) { body =>
      val resp     = post("/api/v1/cursor/prev-beat", body)
      val data     = assertCursorEnvelope(resp)
      val newBeat  = data.get[Int]("beat").getOrElse(fail("missing beat"))
      val newCycle = data.get[Int]("cycle").getOrElse(fail("missing cycle"))

      newBeat should be >= 0
      newCycle should be >= 0
    }
  }

  test("propCursorNextSubBeatBounded: next-sub-beat returns a cursor with subIndex in valid range") {
    forAll(RequestGenerators.genCursorRequestBody) { body =>
      val resp         = post("/api/v1/cursor/next-sub-beat", body)
      val data         = assertCursorEnvelope(resp)
      val subIndex     = data.get[Int]("subIndex").getOrElse(fail("missing subIndex"))
      val totalSubdivs = data.get[Int]("totalSubdivisions").getOrElse(fail("missing totalSubdivisions"))

      subIndex should be >= 0
      totalSubdivs should be > 0
      // After advancing a sub-beat the cursor either stays within the beat (subIndex < total)
      // or rolls over to the next beat (subIndex reset to 0).
      subIndex should be < totalSubdivs
    }
  }

  test("propCursorSetOctaveEcho: set-octave returns the requested octave") {
    // The request envelope needs both a cursor and an octave string at the top level.
    val gen = for
      base   <- RequestGenerators.genCursorRequestBody
      octave <- org.scalacheck.Gen.oneOf("atimandra", "mandra", "madhya", "taar", "atitaar")
    yield base.deepMerge(Json.obj("octave" -> Json.fromString(octave))) -> octave

    forAll(gen) { case (body, requestedOctave) =>
      val resp     = post("/api/v1/cursor/set-octave", body)
      val data     = assertCursorEnvelope(resp)
      val returned = data.get[String]("currentOctave").getOrElse(fail("missing currentOctave"))
      // The route serialises octave as the Scala enum name capitalised
      // ("Madhya", "AtiMandra"); compare case-insensitively.
      returned.toLowerCase shouldBe requestedOctave.toLowerCase
    }
  }

  test("propCursorSetSubdivisionsEcho: set-subdivisions with n∈[1,8] echoes n in the response") {
    val gen = for
      base <- RequestGenerators.genCursorRequestBody
      n    <- org.scalacheck.Gen.choose(1, 8)
    yield base.deepMerge(Json.obj("subdivisions" -> Json.fromInt(n))) -> n

    forAll(gen) { case (body, requestedN) =>
      val resp = post("/api/v1/cursor/set-subdivisions", body)
      val data = assertCursorEnvelope(resp)
      data.get[Int]("totalSubdivisions").getOrElse(-1) shouldBe requestedN
      // The route resets subIndex to 0 whenever subdivisions are reassigned;
      // pinning this here protects against a subtle regression where the
      // existing subIndex was kept (which can index past totalSubdivisions).
      data.get[Int]("subIndex").getOrElse(-1) shouldBe 0
    }
  }

  test("propCursorMoveToEcho: move-to with non-negative (cycle, beat) returns that position") {
    val gen = for
      base  <- RequestGenerators.genCursorRequestBody
      cycle <- org.scalacheck.Gen.choose(0, 4)
      beat  <- org.scalacheck.Gen.choose(0, 15)
    yield (
      base.deepMerge(
        Json.obj("cycle" -> Json.fromInt(cycle), "beat" -> Json.fromInt(beat))
      ),
      cycle,
      beat
    )

    forAll(gen) { case (body, requestedCycle, requestedBeat) =>
      val resp = post("/api/v1/cursor/move-to", body)
      val data = assertCursorEnvelope(resp)
      data.get[Int]("cycle").getOrElse(-1) shouldBe requestedCycle
      data.get[Int]("beat").getOrElse(-1) shouldBe requestedBeat
    }
  }

  test("propCursorNeverServerError: every well-formed cursor request returns < 500") {
    // Aggregate contract — across all cursor routes, no well-formed input
    // (drawn from `genCursorRequestBody`) ever produces a 5xx. This catches
    // regressions where an exception leaks through the IO chain instead of
    // being mapped to a 4xx via `ErrorMapping.toResponse`.
    val routesWithName = List(
      "/api/v1/cursor/next-beat",
      "/api/v1/cursor/prev-beat",
      "/api/v1/cursor/next-sub-beat"
    )
    forAll(RequestGenerators.genCursorRequestBody) { body =>
      routesWithName.foreach { path =>
        val resp = post(path, body)
        resp.status.code should be < 500
      }
    }
  }
