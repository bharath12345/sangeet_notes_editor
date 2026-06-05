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
import com.varpas.sangeet.server.routes.ExportRoutes

class ExportRoutesSpec extends AnyFlatSpec with Matchers:

  import TestFixtures.*

  val routes = Http4sServerInterpreter[IO]().toRoutes(ExportRoutes.all).orNotFound

  // --- HTML export ---

  "POST /api/v1/export/html" should "export composition as HTML" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson,
      "script"      -> Json.fromString("devanagari")
    )
    val req  = postRequest(uri"/api/v1/export/html", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val htmlStr = json.hcursor.downField("data").as[String].getOrElse("")
    htmlStr should include("<html")
    htmlStr should include("Test Composition")
  }

  it should "export with English script" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson,
      "script"      -> Json.fromString("english")
    )
    val req  = postRequest(uri"/api/v1/export/html", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "use default Devanagari script when not specified" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson
    )
    val req  = postRequest(uri"/api/v1/export/html", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "export composition with swar content" in {
    val body = Json.obj(
      "composition" -> compositionWithSwar.asJson
    )
    val req  = postRequest(uri"/api/v1/export/html", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "reject missing composition" in {
    val body = Json.obj("bad" -> Json.fromString("data"))
    val req  = postRequest(uri"/api/v1/export/html", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- PDF export ---

  "POST /api/v1/export/pdf" should "export composition as PDF bytes" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson,
      "script"      -> Json.fromString("devanagari"),
      "landscape"   -> Json.fromBoolean(false)
    )
    val req  = postRequest(uri"/api/v1/export/pdf", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val bytes = resp.body.compile.to(Array).unsafeRunSync()
    bytes.length should be > 0
    // PDF magic bytes: %PDF
    new String(bytes.take(4)) shouldBe "%PDF"
  }

  it should "export PDF in landscape mode" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson,
      "landscape"   -> Json.fromBoolean(true)
    )
    val req  = postRequest(uri"/api/v1/export/pdf", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "reject missing composition" in {
    val body = Json.obj("bad" -> Json.fromString("data"))
    val req  = postRequest(uri"/api/v1/export/pdf", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }
