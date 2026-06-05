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

import com.varpas.sangeet.server.routes.StrokeRoutes

class StrokeRoutesSpec extends AnyFlatSpec with Matchers:

  import TestFixtures.*

  val routes = Http4sServerInterpreter[IO]().toRoutes(StrokeRoutes.all).orNotFound

  // --- set stroke ---

  "POST /api/v1/stroke/set" should "set Da stroke on a beat with a swar" in {
    val input = editorInputJson(compositionWithSwar)
    val body = input.deepMerge(
      Json.obj(
        "stroke" -> Json.fromString("da")
      )
    )
    val req  = postRequest(uri"/api/v1/editor/stroke/set", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    json.hcursor.downField("data").succeeded shouldBe true
  }

  it should "set Ra stroke" in {
    val input = editorInputJson(compositionWithSwar)
    val body = input.deepMerge(
      Json.obj(
        "stroke" -> Json.fromString("ra")
      )
    )
    val req  = postRequest(uri"/api/v1/editor/stroke/set", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "set Chikari stroke" in {
    val input = editorInputJson(compositionWithSwar)
    val body = input.deepMerge(
      Json.obj(
        "stroke" -> Json.fromString("chikari")
      )
    )
    val req  = postRequest(uri"/api/v1/editor/stroke/set", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "reject invalid input" in {
    val body = Json.obj("invalid" -> Json.fromString("data"))
    val req  = postRequest(uri"/api/v1/editor/stroke/set", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- clear stroke ---

  "POST /api/v1/stroke/clear" should "clear stroke on a beat" in {
    val input = editorInputJson(compositionWithSwar)
    val req   = postRequest(uri"/api/v1/editor/stroke/clear", input)
    val resp  = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "handle empty composition gracefully" in {
    val input = editorInputJson()
    val req   = postRequest(uri"/api/v1/editor/stroke/clear", input)
    val resp  = routes.run(req).unsafeRunSync()

    // Should either succeed (no-op) or return specific error
    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail("parse"))
    json.hcursor.downField("success").succeeded shouldBe true
  }

  it should "reject missing input" in {
    val body = Json.obj("bad" -> Json.fromString("input"))
    val req  = postRequest(uri"/api/v1/editor/stroke/clear", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }
