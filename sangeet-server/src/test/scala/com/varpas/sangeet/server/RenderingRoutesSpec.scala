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

import com.varpas.sangeet.server.routes.{ReferenceRoutes, RenderingRoutes}

class RenderingRoutesSpec extends AnyFlatSpec with Matchers:

  import TestFixtures.*

  val glyphRoutes = Http4sServerInterpreter[IO]().toRoutes(RenderingRoutes.all).orNotFound
  val refRoutes   = Http4sServerInterpreter[IO]().toRoutes(ReferenceRoutes.all).orNotFound

  // --- glyph ---

  "POST /api/v1/rendering/glyph" should "render Sa in Devanagari" in {
    val body = Json.obj(
      "note"    -> Json.fromString("sa"),
      "variant" -> Json.fromString("shuddha"),
      "octave"  -> Json.fromString("madhya"),
      "script"  -> Json.fromString("devanagari")
    )
    val req  = postRequest(uri"/api/v1/rendering/glyph", body)
    val resp = glyphRoutes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = json.hcursor.downField("data")
    data.get[String]("glyph").getOrElse("") should not be empty
    data.get[Boolean]("needsKomalMark").getOrElse(true) shouldBe false
    data.get[Boolean]("needsTivraMark").getOrElse(true) shouldBe false
    data.get[Int]("octaveDots").getOrElse(-1) shouldBe 0
  }

  it should "render komal Re with komal mark" in {
    val body = Json.obj(
      "note"    -> Json.fromString("re"),
      "variant" -> Json.fromString("komal"),
      "octave"  -> Json.fromString("madhya")
    )
    val req  = postRequest(uri"/api/v1/rendering/glyph", body)
    val resp = glyphRoutes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data")
    data.get[Boolean]("needsKomalMark").getOrElse(false) shouldBe true
  }

  it should "render tivra Ma with tivra mark" in {
    val body = Json.obj(
      "note"    -> Json.fromString("ma"),
      "variant" -> Json.fromString("tivra"),
      "octave"  -> Json.fromString("madhya")
    )
    val req  = postRequest(uri"/api/v1/rendering/glyph", body)
    val resp = glyphRoutes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data")
    data.get[Boolean]("needsTivraMark").getOrElse(false) shouldBe true
  }

  it should "include octave dots for mandra" in {
    val body = Json.obj(
      "note"    -> Json.fromString("sa"),
      "variant" -> Json.fromString("shuddha"),
      "octave"  -> Json.fromString("mandra")
    )
    val req  = postRequest(uri"/api/v1/rendering/glyph", body)
    val resp = glyphRoutes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data")
    data.get[Int]("octaveDots").getOrElse(0) should be > 0
    data.get[String]("dotPosition").getOrElse("") shouldBe "below"
  }

  it should "include octave dots for taar" in {
    val body = Json.obj(
      "note"    -> Json.fromString("sa"),
      "variant" -> Json.fromString("shuddha"),
      "octave"  -> Json.fromString("taar")
    )
    val req  = postRequest(uri"/api/v1/rendering/glyph", body)
    val resp = glyphRoutes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data")
    data.get[Int]("octaveDots").getOrElse(0) should be > 0
    data.get[String]("dotPosition").getOrElse("") shouldBe "above"
  }

  it should "include all script mappings" in {
    val body = Json.obj(
      "note"    -> Json.fromString("sa"),
      "variant" -> Json.fromString("shuddha"),
      "octave"  -> Json.fromString("madhya")
    )
    val req  = postRequest(uri"/api/v1/rendering/glyph", body)
    val resp = glyphRoutes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json       = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val allScripts = json.hcursor.downField("data").downField("allScripts")
    allScripts.downField("devanagari").succeeded shouldBe true
    allScripts.downField("english").succeeded shouldBe true
  }

  it should "render all 7 notes" in {
    val notes = List("sa", "re", "ga", "ma", "pa", "dha", "ni")
    notes.foreach { n =>
      val body = Json.obj(
        "note"    -> Json.fromString(n),
        "variant" -> Json.fromString("shuddha"),
        "octave"  -> Json.fromString("madhya")
      )
      val req  = postRequest(uri"/api/v1/rendering/glyph", body)
      val resp = glyphRoutes.run(req).unsafeRunSync()
      resp.status shouldBe Status.Ok
    }
  }

  // --- colors ---

  "GET /api/v1/rendering/colors" should "return all notation colors" in {
    val req  = Request[IO](Method.GET, uri"/api/v1/rendering/colors")
    val resp = refRoutes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = json.hcursor.downField("data")
    data.downField("swar").succeeded shouldBe true
    data.downField("ornament").succeeded shouldBe true
    data.downField("stroke").succeeded shouldBe true
    data.downField("sahitya").succeeded shouldBe true
    data.downField("rest").succeeded shouldBe true
    data.downField("komalMark").succeeded shouldBe true
    data.downField("tivraMark").succeeded shouldBe true
    data.downField("octaveDot").succeeded shouldBe true
    data.downField("taalMarker").succeeded shouldBe true
  }

  it should "return color values as hex strings" in {
    val req  = Request[IO](Method.GET, uri"/api/v1/rendering/colors")
    val resp = refRoutes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json      = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val swarColor = json.hcursor.downField("data").get[String]("swar").getOrElse("")
    swarColor should startWith("#")
  }

  // --- scripts ---

  "GET /api/v1/rendering/scripts" should "return all available scripts" in {
    val req  = Request[IO](Method.GET, uri"/api/v1/rendering/scripts")
    val resp = refRoutes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = json.hcursor.downField("data")
    data.downField("devanagari").succeeded shouldBe true
    data.downField("english").succeeded shouldBe true
  }

  it should "include note mappings for each script" in {
    val req  = Request[IO](Method.GET, uri"/api/v1/rendering/scripts")
    val resp = refRoutes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json       = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val devanagari = json.hcursor.downField("data").downField("devanagari")
    devanagari.downField("displayName").succeeded shouldBe true
    devanagari.downField("fontName").succeeded shouldBe true
    devanagari.downField("notes").downField("sa").succeeded shouldBe true
  }
