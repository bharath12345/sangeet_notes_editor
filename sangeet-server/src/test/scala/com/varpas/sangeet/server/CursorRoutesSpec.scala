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

  // --- next-beat ---

  "POST /api/v1/cursor/next-beat" should "advance cursor beat" in {
    val body = Json.obj("cursor" -> cursorJson())
    val req  = postRequest(uri"/api/v1/cursor/next-beat", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    val data = json.hcursor.downField("data")
    data.get[Int]("beat").getOrElse(-1) shouldBe 1
  }

  it should "wrap to next cycle at end of taal" in {
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

  "POST /api/v1/cursor/prev-beat" should "move cursor back" in {
    val cursor = minimalCursor.copy(beat = 3)
    val body   = Json.obj("cursor" -> cursorJson(cursor))
    val req    = postRequest(uri"/api/v1/cursor/prev-beat", body)
    val resp   = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data")
    data.get[Int]("beat").getOrElse(-1) shouldBe 2
  }

  it should "not go below beat 0 cycle 0" in {
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

  "POST /api/v1/cursor/next-sub-beat" should "advance subIndex within beat" in {
    val cursor = minimalCursor.copy(totalSubdivisions = 4, subIndex = 1)
    val body   = Json.obj("cursor" -> cursorJson(cursor))
    val req    = postRequest(uri"/api/v1/cursor/next-sub-beat", body)
    val resp   = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data")
    data.get[Int]("subIndex").getOrElse(-1) shouldBe 2
  }

  // --- set-subdivisions ---

  "POST /api/v1/cursor/set-subdivisions" should "update totalSubdivisions" in {
    val body = Json.obj(
      "cursor"       -> cursorJson(),
      "subdivisions" -> Json.fromInt(4)
    )
    val req  = postRequest(uri"/api/v1/cursor/set-subdivisions", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data")
    data.get[Int]("totalSubdivisions").getOrElse(-1) shouldBe 4
  }

  it should "reject invalid subdivision count" in {
    val body = Json.obj(
      "cursor"       -> cursorJson(),
      "subdivisions" -> Json.fromInt(0)
    )
    val req  = postRequest(uri"/api/v1/cursor/set-subdivisions", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- set-octave ---

  "POST /api/v1/cursor/set-octave" should "change octave to taar" in {
    val body = Json.obj(
      "cursor" -> cursorJson(),
      "octave" -> Json.fromString("taar")
    )
    val req  = postRequest(uri"/api/v1/cursor/set-octave", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data")
    data.get[String]("currentOctave").getOrElse("") should (equal("taar") or equal("Taar"))
  }

  it should "change octave to mandra" in {
    val body = Json.obj(
      "cursor" -> cursorJson(),
      "octave" -> Json.fromString("mandra")
    )
    val req  = postRequest(uri"/api/v1/cursor/set-octave", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data")
    data.get[String]("currentOctave").getOrElse("") should (equal("mandra") or equal("Mandra"))
  }

  // --- move-to ---

  "POST /api/v1/cursor/move-to" should "move cursor to specific position" in {
    val body = Json.obj(
      "cursor" -> cursorJson(),
      "cycle"  -> Json.fromInt(2),
      "beat"   -> Json.fromInt(5)
    )
    val req  = postRequest(uri"/api/v1/cursor/move-to", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data")
    data.get[Int]("cycle").getOrElse(-1) shouldBe 2
    data.get[Int]("beat").getOrElse(-1) shouldBe 5
  }

  it should "reject negative beat" in {
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
