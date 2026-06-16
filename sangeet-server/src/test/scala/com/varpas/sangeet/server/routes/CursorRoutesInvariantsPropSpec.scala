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

/** Plan 19 T2C — cursor-route invariants T2B didn't cover.
  *
  * T2B's [[CursorRoutesPropSpec]] established bounded-range + echo contracts. This spec adds the orthogonal axis:
  *
  *   - **Idempotency / pure-function contract.** POST routes that are pure functions of the request body (no
  *     server-side state) must return byte-identical responses across N calls with the same body. Cursor routes match
  *     this profile — they have no side effects.
  *   - **Local inverse.** `next-beat` followed by `prev-beat` returns to the same (cycle, beat) cell when the starting
  *     position is in the interior of a taal cycle (not at the cycle boundary, where `prev-beat` clamps).
  *   - **`set-octave` is reset-only on the octave.** No other cursor field should drift (beat / cycle / sub-index /
  *     totalSubdivisions are preserved). This catches a class of regression where `set-octave` accidentally also resets
  *     sub-divisions.
  *   - **`set-subdivisions` resets `subIndex`.** Already covered in T2B as an echo invariant; here we add the
  *     companion: `taal` is preserved (no implicit re-binding to a default taal on subdivision change).
  *
  * These aren't covered by T2B because they are *cross-call* invariants — they need two HTTP calls to express, not one.
  * Keeping them in a separate spec keeps each spec's reading order short.
  */
class CursorRoutesInvariantsPropSpec extends AnyFunSuite with Matchers with ScalaCheckPropertyChecks:

  private val routes: HttpApp[IO] =
    Http4sServerInterpreter[IO]().toRoutes(CursorRoutes.all).orNotFound

  private given Shrink[String] = Shrink.shrinkAny[String]
  private given Shrink[Json]   = Shrink.shrinkAny[Json]

  private def post(path: String, body: Json): Response[IO] =
    val req = Request[IO](Method.POST, Uri.unsafeFromString(path))
      .withEntity(body.noSpaces)
      .withContentType(headers.`Content-Type`(MediaType.application.json))
    routes.run(req).unsafeRunSync()

  private def postBody(path: String, body: Json): String =
    val resp = post(path, body)
    resp.status shouldBe Status.Ok
    resp.as[String].unsafeRunSync()

  private def assertSuccessData(bodyStr: String): io.circe.ACursor =
    val json = parse(bodyStr).getOrElse(fail(s"body not JSON: $bodyStr"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    json.hcursor.downField("data")

  test("propCursorPostsAreIdempotent: posting the same body to the same route twice returns identical bodies") {
    // Cursor routes are pure functions of the request body — no server-side
    // state, no logging side effects in the response, no timestamps. Two
    // back-to-back calls with the same body must produce byte-identical
    // response bodies. A regression that injected a `requestId` or
    // `serverTimestamp` into the response would trip this property.
    val paths = List(
      "/api/v1/cursor/next-beat",
      "/api/v1/cursor/prev-beat",
      "/api/v1/cursor/next-sub-beat"
    )
    forAll(RequestGenerators.genCursorRequestBody) { body =>
      paths.foreach { path =>
        val a = postBody(path, body)
        val b = postBody(path, body)
        b shouldBe a
      }
    }
  }

  test("propNextThenPrev: next-beat followed by prev-beat returns to the original (cycle, beat) in the interior") {
    // In the interior of a taal cycle (beat ∈ [1, matras-1]), nextBeat advances
    // to (cycle, beat+1) and prevBeat retreats from there to (cycle, beat).
    // Exclude beat = matras - 1 (cycle boundary — next rolls over to next cycle
    // and prev clamps to the previous cycle's last beat, breaking the simple
    // inverse).
    //
    // The composition of (next, prev) on interior positions is the identity
    // on (cycle, beat). Subdivision state may or may not be preserved
    // depending on the cursor's totalSubdivisions; we restrict to
    // totalSubdivisions = 1 so the inverse is exact.
    //
    // Construct interior cursors directly rather than filtering — `suchThat`
    // on the generic body generator discards ~80% (subdivisions ≠ 1 plus the
    // cycle-boundary beat), causing ScalaCheck to give up.
    val gen: org.scalacheck.Gen[io.circe.Json] =
      for
        taal   <- com.varpas.sangeet.core.generators.Generators.genTaal
        cycle  <- org.scalacheck.Gen.choose(0, 4)
        beat   <- org.scalacheck.Gen.choose(0, taal.matras - 2)
        octave <- com.varpas.sangeet.core.generators.Generators.genOctave
      yield
        val cur = com.varpas.sangeet.core.editor.CursorModel(
          taal = taal,
          cycle = cycle,
          beat = beat,
          subIndex = 0,
          totalSubdivisions = 1,
          currentOctave = octave
        )
        Json.obj("cursor" -> RequestGenerators.cursorJson(cur))

    forAll(gen) { body =>
      // Step 1: next-beat.
      val afterNext    = assertSuccessData(postBody("/api/v1/cursor/next-beat", body))
      val nextCycle    = afterNext.get[Int]("cycle").getOrElse(fail("missing cycle"))
      val nextBeat     = afterNext.get[Int]("beat").getOrElse(fail("missing beat"))
      val nextSubdivs  = afterNext.get[Int]("totalSubdivisions").getOrElse(fail("missing subdivisions"))
      val nextOctave   = afterNext.get[String]("currentOctave").getOrElse("madhya")
      val nextTaalJson = afterNext.downField("taal").as[Json].getOrElse(fail("missing taal"))

      // Step 2: feed that back into prev-beat as a fresh request body.
      val nextCursorJson = Json.obj(
        "taal"              -> nextTaalJson,
        "cycle"             -> Json.fromInt(nextCycle),
        "beat"              -> Json.fromInt(nextBeat),
        "subIndex"          -> Json.fromInt(0),
        "totalSubdivisions" -> Json.fromInt(nextSubdivs),
        "currentOctave"     -> Json.fromString(nextOctave)
      )
      val nextBody = Json.obj("cursor" -> nextCursorJson)

      val afterPrev = assertSuccessData(postBody("/api/v1/cursor/prev-beat", nextBody))
      val prevCycle = afterPrev.get[Int]("cycle").getOrElse(fail("missing cycle"))
      val prevBeat  = afterPrev.get[Int]("beat").getOrElse(fail("missing beat"))

      val origCursor = body.hcursor.downField("cursor")
      val origCycle  = origCursor.get[Int]("cycle").getOrElse(fail("orig cycle"))
      val origBeat   = origCursor.get[Int]("beat").getOrElse(fail("orig beat"))

      prevCycle shouldBe origCycle
      prevBeat shouldBe origBeat
    }
  }

  test("propSetOctaveOnlyAffectsOctave: set-octave preserves beat/cycle/subIndex/totalSubdivisions/taal") {
    // The endpoint is a one-field mutator; everything else must be byte-stable.
    // A regression that reset sub-divisions on octave change (a plausible bug
    // since both fields are in the cursor envelope) would be caught here.
    val gen = for
      base   <- RequestGenerators.genCursorRequestBody
      octave <- org.scalacheck.Gen.oneOf("atimandra", "mandra", "madhya", "taar", "atitaar")
    yield base.deepMerge(Json.obj("octave" -> Json.fromString(octave)))

    forAll(gen) { body =>
      val before = body.hcursor.downField("cursor")
      val data   = assertSuccessData(postBody("/api/v1/cursor/set-octave", body))

      // Preserved fields.
      data.get[Int]("beat").getOrElse(-1) shouldBe before.get[Int]("beat").getOrElse(-1)
      data.get[Int]("cycle").getOrElse(-1) shouldBe before.get[Int]("cycle").getOrElse(-1)
      data.get[Int]("subIndex").getOrElse(-1) shouldBe before.get[Int]("subIndex").getOrElse(-1)
      data.get[Int]("totalSubdivisions").getOrElse(-1) shouldBe before.get[Int]("totalSubdivisions").getOrElse(-1)
      // Taal must be untouched (deep equality on the JSON sub-object).
      data.downField("taal").as[Json].toOption shouldBe before.downField("taal").as[Json].toOption
    }
  }

  test("propSetSubdivisionsPreservesTaalAndCycle: set-subdivisions preserves taal/cycle/beat") {
    // Companion of T2B's `propCursorSetSubdivisionsEcho` — that one checked
    // the new value is echoed and subIndex resets. This one checks the *other*
    // fields aren't disturbed. Splitting "what changes" and "what is preserved"
    // into two complementary properties keeps each readable.
    val gen = for
      base <- RequestGenerators.genCursorRequestBody
      n    <- org.scalacheck.Gen.choose(1, 8)
    yield base.deepMerge(Json.obj("subdivisions" -> Json.fromInt(n)))

    forAll(gen) { body =>
      val before = body.hcursor.downField("cursor")
      val data   = assertSuccessData(postBody("/api/v1/cursor/set-subdivisions", body))

      data.get[Int]("beat").getOrElse(-1) shouldBe before.get[Int]("beat").getOrElse(-1)
      data.get[Int]("cycle").getOrElse(-1) shouldBe before.get[Int]("cycle").getOrElse(-1)
      data.downField("taal").as[Json].toOption shouldBe before.downField("taal").as[Json].toOption
    }
  }
