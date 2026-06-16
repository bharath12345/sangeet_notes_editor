package com.varpas.sangeet.server

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.parser._
import org.http4s._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.tapir.server.http4s.Http4sServerInterpreter

import com.varpas.sangeet.server.routes.CursorRoutes

class CursorRoutesSpec extends AnyFlatSpec with Matchers:

  import TestFixtures.*

  val routes = Http4sServerInterpreter[IO]().toRoutes(CursorRoutes.all).orNotFound

  // Plan 19 T2D: example tests for the generic "advances by one" / "echoes
  // requested value" behavior were removed in favor of the property-based
  // coverage in `routes/CursorRoutesPropSpec` and
  // `routes/CursorRoutesInvariantsPropSpec`. Boundary / edge-case / specific
  // 4xx tests remain here — properties don't pin the exact "beat=15 → cycle++"
  // transition or the exact malformed-input → 4xx contracts.

  // --- next-beat ---

  // NOTE: removed "should advance cursor beat" — subsumed by
  // `CursorRoutesPropSpec::propCursorNextBeatBounded` (every well-formed cursor
  // returns a 200 with beat/cycle in valid ranges).

  "POST /api/v1/cursor/next-beat" should "wrap to next cycle at end of taal" in {
    val cursor = minimalCursor.copy(beat = 15)
    val body   = Json.obj("cursor" -> cursorJson(cursor))
    val req    = postRequest(uri"/api/v1/cursor/next-beat", body)
    val resp   = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data")
    data.get[Int]("beat").getOrElse(-1) shouldBe 0
    data.get[Int]("cycle").getOrElse(-1) shouldBe 1
  }

  // --- prev-beat ---

  // NOTE: removed "should move cursor back" — subsumed by
  // `CursorRoutesPropSpec::propCursorPrevBeatBounded`.

  "POST /api/v1/cursor/prev-beat" should "not go below beat 0 cycle 0" in {
    val body = Json.obj("cursor" -> cursorJson())
    val req  = postRequest(uri"/api/v1/cursor/prev-beat", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data")
    data.get[Int]("beat").getOrElse(-1) shouldBe 0
    data.get[Int]("cycle").getOrElse(-1) shouldBe 0
  }

  // --- next-sub-beat ---

  // NOTE: removed "should advance subIndex within beat" — subsumed by
  // `CursorRoutesPropSpec::propCursorNextSubBeatBounded`.

  // --- set-subdivisions ---

  // NOTE: removed "should update totalSubdivisions" — subsumed by
  // `CursorRoutesPropSpec::propCursorSetSubdivisionsEcho`.

  "POST /api/v1/cursor/set-subdivisions" should "reject invalid subdivision count" in {
    val body = Json.obj(
      "cursor"       -> cursorJson(),
      "subdivisions" -> Json.fromInt(0)
    )
    val req  = postRequest(uri"/api/v1/cursor/set-subdivisions", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- set-octave ---

  // NOTE: removed "should change octave to taar/mandra" — both subsumed by
  // `CursorRoutesPropSpec::propCursorSetOctaveEcho` (every allowed octave
  // string is echoed back). The "only affects octave" invariant is also
  // checked in `CursorRoutesInvariantsPropSpec::propSetOctaveOnlyAffectsOctave`.

  // --- move-to ---

  // NOTE: removed "should move cursor to specific position" — subsumed by
  // `CursorRoutesPropSpec::propCursorMoveToEcho`.

  "POST /api/v1/cursor/move-to" should "reject negative beat" in {
    val body = Json.obj(
      "cursor" -> cursorJson(),
      "cycle"  -> Json.fromInt(0),
      "beat"   -> Json.fromInt(-1)
    )
    val req  = postRequest(uri"/api/v1/cursor/move-to", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  it should "accept beat beyond taal matras (no upper bound validation)" in {
    val body = Json.obj(
      "cursor" -> cursorJson(),
      "cycle"  -> Json.fromInt(0),
      "beat"   -> Json.fromInt(20)
    )
    val req  = postRequest(uri"/api/v1/cursor/move-to", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data")
    data.get[Int]("beat").getOrElse(-1) shouldBe 20
  }

  // --- invalid input ---

  "POST /api/v1/cursor/next-beat" should "reject missing cursor" in {
    val body = Json.obj("invalid" -> Json.fromString("data"))
    val req  = postRequest(uri"/api/v1/cursor/next-beat", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }
