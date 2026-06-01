package com.varpas.sangeet.server

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.Json
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.varpas.sangeet.server.routes.EditorRoutes
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.Codecs.given

class EditorRoutesSpec extends AnyFlatSpec with Matchers:

  import TestFixtures.*

  val routes = Http4sServerInterpreter[IO]().toRoutes(EditorRoutes.all).orNotFound

  // --- insert-swar ---

  "POST /api/v1/editor/insert-swar" should "insert Sa shuddha madhya" in {
    val body = editorInputJson().deepMerge(Json.obj(
      "note" -> Json.fromString("sa"),
      "variant" -> Json.fromString("shuddha"),
      "octave" -> Json.fromString("madhya")
    ))
    val req = postRequest(uri"/api/v1/editor/insert-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    val data = json.hcursor.downField("data")
    data.downField("composition").succeeded shouldBe true
    data.downField("cursor").succeeded shouldBe true
  }

  it should "insert komal Re" in {
    val body = editorInputJson().deepMerge(Json.obj(
      "note" -> Json.fromString("re"),
      "variant" -> Json.fromString("komal"),
      "octave" -> Json.fromString("madhya")
    ))
    val req = postRequest(uri"/api/v1/editor/insert-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "insert tivra Ma in taar saptak" in {
    val body = editorInputJson().deepMerge(Json.obj(
      "note" -> Json.fromString("ma"),
      "variant" -> Json.fromString("tivra"),
      "octave" -> Json.fromString("taar")
    ))
    val req = postRequest(uri"/api/v1/editor/insert-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "reject missing note field" in {
    val body = editorInputJson().deepMerge(Json.obj(
      "variant" -> Json.fromString("shuddha"),
      "octave" -> Json.fromString("madhya")
    ))
    val req = postRequest(uri"/api/v1/editor/insert-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  it should "reject invalid input" in {
    val body = Json.obj("invalid" -> Json.fromString("data"))
    val req = postRequest(uri"/api/v1/editor/insert-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- insert-rest ---

  "POST /api/v1/editor/insert-rest" should "insert a rest and return updated composition" in {
    val body = editorInputJson()
    val req = postRequest(uri"/api/v1/editor/insert-rest", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    val data = json.hcursor.downField("data")
    data.downField("composition").succeeded shouldBe true
    data.downField("cursor").succeeded shouldBe true
  }

  it should "return error with invalid input" in {
    val body = Json.obj("invalid" -> Json.fromString("data"))
    val req = postRequest(uri"/api/v1/editor/insert-rest", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(true) shouldBe false
    json.hcursor.downField("error").downField("code").succeeded shouldBe true
  }

  // --- insert-sustain ---

  "POST /api/v1/editor/insert-sustain" should "insert a sustain" in {
    val body = editorInputJson()
    val req = postRequest(uri"/api/v1/editor/insert-sustain", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "reject invalid input" in {
    val body = Json.obj("bad" -> Json.fromString("data"))
    val req = postRequest(uri"/api/v1/editor/insert-sustain", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- delete-last ---

  "POST /api/v1/editor/delete-last" should "delete last event from section" in {
    val body = editorInputJson(compositionWithSwar)
    val req = postRequest(uri"/api/v1/editor/delete-last", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "return error for empty section" in {
    val body = editorInputJson()
    val req = postRequest(uri"/api/v1/editor/delete-last", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.BadRequest
  }

  it should "reject invalid input" in {
    val body = Json.obj("bad" -> Json.fromString("data"))
    val req = postRequest(uri"/api/v1/editor/delete-last", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- insert-dual-swar ---

  "POST /api/v1/editor/insert-dual-swar" should "insert dual Sa" in {
    val body = editorInputJson().deepMerge(Json.obj(
      "note" -> Json.fromString("sa"),
      "variant" -> Json.fromString("shuddha"),
      "octave" -> Json.fromString("madhya")
    ))
    val req = postRequest(uri"/api/v1/editor/insert-dual-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "reject missing note" in {
    val body = editorInputJson()
    val req = postRequest(uri"/api/v1/editor/insert-dual-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- insert-swar-group ---

  "POST /api/v1/editor/insert-swar-group" should "insert a group of notes" in {
    val body = editorInputJson().deepMerge(Json.obj(
      "notes" -> Json.arr(
        noteRefJson("sa"),
        noteRefJson("re"),
        noteRefJson("ga")
      )
    ))
    val req = postRequest(uri"/api/v1/editor/insert-swar-group", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "insert a pair of notes" in {
    val body = editorInputJson().deepMerge(Json.obj(
      "notes" -> Json.arr(
        noteRefJson("sa"),
        noteRefJson("re")
      )
    ))
    val req = postRequest(uri"/api/v1/editor/insert-swar-group", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "accept empty notes array" in {
    val body = editorInputJson().deepMerge(Json.obj(
      "notes" -> Json.arr()
    ))
    val req = postRequest(uri"/api/v1/editor/insert-swar-group", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "reject missing notes field" in {
    val body = editorInputJson()
    val req = postRequest(uri"/api/v1/editor/insert-swar-group", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- delete-at-cursor ---

  "POST /api/v1/editor/delete-at-cursor" should "delete event at cursor position" in {
    val body = editorInputJson(compositionWithSwar)
    val req = postRequest(uri"/api/v1/editor/delete-at-cursor", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "handle empty section gracefully" in {
    val body = editorInputJson()
    val req = postRequest(uri"/api/v1/editor/delete-at-cursor", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "reject invalid input" in {
    val body = Json.obj("bad" -> Json.fromString("data"))
    val req = postRequest(uri"/api/v1/editor/delete-at-cursor", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- chained operations ---

  "Editor operations" should "support insert-swar then delete-last" in {
    val insertBody = editorInputJson().deepMerge(Json.obj(
      "note" -> Json.fromString("sa"),
      "variant" -> Json.fromString("shuddha"),
      "octave" -> Json.fromString("madhya")
    ))
    val insertReq = postRequest(uri"/api/v1/editor/insert-swar", insertBody)
    val insertResp = routes.run(insertReq).unsafeRunSync()
    insertResp.status shouldBe Status.Ok

    val insertJson = parse(insertResp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val updatedComp = insertJson.hcursor.downField("data").downField("composition").focus.getOrElse(fail("no comp"))
    val updatedCursor = insertJson.hcursor.downField("data").downField("cursor").focus.getOrElse(fail("no cursor"))

    val deleteBody = Json.obj(
      "composition" -> updatedComp,
      "sectionIndex" -> Json.fromInt(0),
      "cursor" -> updatedCursor
    )
    val deleteReq = postRequest(uri"/api/v1/editor/delete-last", deleteBody)
    val deleteResp = routes.run(deleteReq).unsafeRunSync()

    deleteResp.status shouldBe Status.Ok
    val deleteJson = parse(deleteResp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    deleteJson.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }
