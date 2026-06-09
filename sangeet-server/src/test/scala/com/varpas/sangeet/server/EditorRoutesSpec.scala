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
import com.varpas.sangeet.server.routes.EditorRoutes

class EditorRoutesSpec extends AnyFlatSpec with Matchers:

  import TestFixtures.*

  val routes = Http4sServerInterpreter[IO]().toRoutes(EditorRoutes.all).orNotFound

  // --- insert-swar ---

  "POST /api/v1/editor/insert-swar" should "insert Sa shuddha madhya" in {
    val body = editorInputJson().deepMerge(
      Json.obj(
        "note"    -> Json.fromString("sa"),
        "variant" -> Json.fromString("shuddha"),
        "octave"  -> Json.fromString("madhya")
      )
    )
    val req  = postRequest(uri"/api/v1/editor/insert-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    val data = json.hcursor.downField("data")
    data.downField("composition").succeeded shouldBe true
    data.downField("cursor").succeeded shouldBe true
  }

  it should "insert komal Re" in {
    val body = editorInputJson().deepMerge(
      Json.obj(
        "note"    -> Json.fromString("re"),
        "variant" -> Json.fromString("komal"),
        "octave"  -> Json.fromString("madhya")
      )
    )
    val req  = postRequest(uri"/api/v1/editor/insert-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "insert tivra Ma in taar saptak" in {
    val body = editorInputJson().deepMerge(
      Json.obj(
        "note"    -> Json.fromString("ma"),
        "variant" -> Json.fromString("tivra"),
        "octave"  -> Json.fromString("taar")
      )
    )
    val req  = postRequest(uri"/api/v1/editor/insert-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "reject missing note field" in {
    val body = editorInputJson().deepMerge(
      Json.obj(
        "variant" -> Json.fromString("shuddha"),
        "octave"  -> Json.fromString("madhya")
      )
    )
    val req  = postRequest(uri"/api/v1/editor/insert-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  it should "reject invalid input" in {
    val body = Json.obj("invalid" -> Json.fromString("data"))
    val req  = postRequest(uri"/api/v1/editor/insert-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- insert-rest ---

  "POST /api/v1/editor/insert-rest" should "insert a rest and return updated composition" in {
    val body = editorInputJson()
    val req  = postRequest(uri"/api/v1/editor/insert-rest", body)
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
    val req  = postRequest(uri"/api/v1/editor/insert-rest", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(true) shouldBe false
    json.hcursor.downField("error").downField("code").succeeded shouldBe true
  }

  // --- insert-sustain ---

  "POST /api/v1/editor/insert-sustain" should "insert a sustain" in {
    val body = editorInputJson()
    val req  = postRequest(uri"/api/v1/editor/insert-sustain", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "reject invalid input" in {
    val body = Json.obj("bad" -> Json.fromString("data"))
    val req  = postRequest(uri"/api/v1/editor/insert-sustain", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- insert-chikari ---

  "POST /api/v1/editor/insert-chikari" should "insert a chikari event" in {
    val body = editorInputJson()
    val req  = postRequest(uri"/api/v1/editor/insert-chikari", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "reject invalid input" in {
    val body = Json.obj("bad" -> Json.fromString("data"))
    val req  = postRequest(uri"/api/v1/editor/insert-chikari", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- delete-last ---

  "POST /api/v1/editor/delete-last" should "delete last event from section" in {
    val body = editorInputJson(compositionWithSwar)
    val req  = postRequest(uri"/api/v1/editor/delete-last", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "return error for empty section" in {
    val body = editorInputJson()
    val req  = postRequest(uri"/api/v1/editor/delete-last", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.BadRequest
  }

  it should "reject invalid input" in {
    val body = Json.obj("bad" -> Json.fromString("data"))
    val req  = postRequest(uri"/api/v1/editor/delete-last", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- insert-dual-swar ---

  "POST /api/v1/editor/insert-dual-swar" should "insert dual Sa" in {
    val body = editorInputJson().deepMerge(
      Json.obj(
        "note"    -> Json.fromString("sa"),
        "variant" -> Json.fromString("shuddha"),
        "octave"  -> Json.fromString("madhya")
      )
    )
    val req  = postRequest(uri"/api/v1/editor/insert-dual-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "reject missing note" in {
    val body = editorInputJson()
    val req  = postRequest(uri"/api/v1/editor/insert-dual-swar", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- insert-swar-group ---

  "POST /api/v1/editor/insert-swar-group" should "insert a group of notes" in {
    val body = editorInputJson().deepMerge(
      Json.obj(
        "notes" -> Json.arr(
          noteRefJson("sa"),
          noteRefJson("re"),
          noteRefJson("ga")
        )
      )
    )
    val req  = postRequest(uri"/api/v1/editor/insert-swar-group", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "insert a pair of notes" in {
    val body = editorInputJson().deepMerge(
      Json.obj(
        "notes" -> Json.arr(
          noteRefJson("sa"),
          noteRefJson("re")
        )
      )
    )
    val req  = postRequest(uri"/api/v1/editor/insert-swar-group", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "accept empty notes array" in {
    val body = editorInputJson().deepMerge(
      Json.obj(
        "notes" -> Json.arr()
      )
    )
    val req  = postRequest(uri"/api/v1/editor/insert-swar-group", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "reject missing notes field" in {
    val body = editorInputJson()
    val req  = postRequest(uri"/api/v1/editor/insert-swar-group", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- delete-at-cursor ---

  "POST /api/v1/editor/delete-at-cursor" should "delete event at cursor position" in {
    val body = editorInputJson(compositionWithSwar)
    val req  = postRequest(uri"/api/v1/editor/delete-at-cursor", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "handle empty section gracefully" in {
    val body = editorInputJson()
    val req  = postRequest(uri"/api/v1/editor/delete-at-cursor", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "reject invalid input" in {
    val body = Json.obj("bad" -> Json.fromString("data"))
    val req  = postRequest(uri"/api/v1/editor/delete-at-cursor", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- copy-selection ---

  "POST /api/v1/editor/copy-selection" should "copy selected events" in {
    val cursor = cursorWithSelection(anchorBeat = 0, endBeat = 1)
    val body = Json.obj(
      "composition"  -> compositionWithMultipleSwar.asJson,
      "sectionIndex" -> Json.fromInt(0),
      "cursor"       -> cursorJsonWithSelection(cursor)
    )
    val req  = postRequest(uri"/api/v1/editor/copy-selection", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    val data = json.hcursor.downField("data")
    data.downField("clipboardJson").succeeded shouldBe true
    data.downField("composition").succeeded shouldBe true
    data.downField("cursor").succeeded shouldBe true
  }

  it should "return error when no selection" in {
    val body = editorInputJson(compositionWithMultipleSwar)
    val req  = postRequest(uri"/api/v1/editor/copy-selection", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.BadRequest
  }

  it should "reject invalid input" in {
    val body = Json.obj("bad" -> Json.fromString("data"))
    val req  = postRequest(uri"/api/v1/editor/copy-selection", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- cut-selection ---

  "POST /api/v1/editor/cut-selection" should "cut selected events" in {
    val cursor = cursorWithSelection(anchorBeat = 0, endBeat = 1)
    val body = Json.obj(
      "composition"  -> compositionWithMultipleSwar.asJson,
      "sectionIndex" -> Json.fromInt(0),
      "cursor"       -> cursorJsonWithSelection(cursor)
    )
    val req  = postRequest(uri"/api/v1/editor/cut-selection", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    val data = json.hcursor.downField("data")
    data.downField("clipboardJson").succeeded shouldBe true
    data.downField("composition").succeeded shouldBe true
  }

  it should "return error when no selection" in {
    val body = editorInputJson(compositionWithMultipleSwar)
    val req  = postRequest(uri"/api/v1/editor/cut-selection", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.BadRequest
  }

  // --- paste-clipboard ---

  "POST /api/v1/editor/paste-clipboard" should "paste events at cursor" in {
    val clipboardJson =
      """{"sangeet-clipboard":true,"version":"2.0","events":[{"type":"swar","note":"sa","variant":"shuddha","octave":"madhya","beat":{"cycle":0,"beat":0,"subdivision":[0,1]},"duration":[1,1],"ornaments":[]}]}"""
    val body = editorInputJson().deepMerge(
      Json.obj("clipboardJson" -> Json.fromString(clipboardJson))
    )
    val req  = postRequest(uri"/api/v1/editor/paste-clipboard", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    val data = json.hcursor.downField("data")
    data.downField("composition").succeeded shouldBe true
    data.downField("cursor").succeeded shouldBe true
  }

  it should "return error for invalid clipboard JSON" in {
    val body = editorInputJson().deepMerge(
      Json.obj("clipboardJson" -> Json.fromString("{\"invalid\": true}"))
    )
    val req  = postRequest(uri"/api/v1/editor/paste-clipboard", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.BadRequest
  }

  it should "return error when clipboardJson field is missing" in {
    val body = editorInputJson()
    val req  = postRequest(uri"/api/v1/editor/paste-clipboard", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  it should "handle empty clipboard events" in {
    val clipboardJson = """{"sangeet-clipboard":true,"version":"2.0","events":[]}"""
    val body = editorInputJson().deepMerge(
      Json.obj("clipboardJson" -> Json.fromString(clipboardJson))
    )
    val req  = postRequest(uri"/api/v1/editor/paste-clipboard", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  // --- chained operations ---

  "Editor operations" should "support insert-swar then delete-last" in {
    val insertBody = editorInputJson().deepMerge(
      Json.obj(
        "note"    -> Json.fromString("sa"),
        "variant" -> Json.fromString("shuddha"),
        "octave"  -> Json.fromString("madhya")
      )
    )
    val insertReq  = postRequest(uri"/api/v1/editor/insert-swar", insertBody)
    val insertResp = routes.run(insertReq).unsafeRunSync()
    insertResp.status shouldBe Status.Ok

    val insertJson    = parse(insertResp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val updatedComp   = insertJson.hcursor.downField("data").downField("composition").focus.getOrElse(fail("no comp"))
    val updatedCursor = insertJson.hcursor.downField("data").downField("cursor").focus.getOrElse(fail("no cursor"))

    val deleteBody = Json.obj(
      "composition"  -> updatedComp,
      "sectionIndex" -> Json.fromInt(0),
      "cursor"       -> updatedCursor
    )
    val deleteReq  = postRequest(uri"/api/v1/editor/delete-last", deleteBody)
    val deleteResp = routes.run(deleteReq).unsafeRunSync()

    deleteResp.status shouldBe Status.Ok
    val deleteJson = parse(deleteResp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    deleteJson.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  // --- change-starting-beat ---

  "POST /api/v1/editor/change-starting-beat" should "change starting beat and return updated composition" in {
    val body = Json.obj(
      "composition"  -> minimalComposition.asJson,
      "sectionIndex" -> Json.fromInt(0),
      "startingBeat" -> Json.fromInt(5)
    )
    val req  = postRequest(uri"/api/v1/editor/change-starting-beat", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    json.hcursor.downField("data").succeeded shouldBe true
  }

  it should "insert locked beat events when increasing from 1" in {
    val body = Json.obj(
      "composition"  -> minimalComposition.asJson,
      "sectionIndex" -> Json.fromInt(0),
      "startingBeat" -> Json.fromInt(9)
    )
    val req  = postRequest(uri"/api/v1/editor/change-starting-beat", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json     = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val compJson = json.hcursor.downField("data")
    val events   = compJson.downField("sections").downN(0).downField("events")
    val eventArr = events.focus.flatMap(_.asArray).getOrElse(fail("no events array"))
    val lockedCount = eventArr.count { e =>
      e.hcursor.downField("type").as[String].getOrElse("") == "lockedbeat"
    }
    lockedCount shouldBe 8
  }

  it should "update the section startingBeat field" in {
    val body = Json.obj(
      "composition"  -> minimalComposition.asJson,
      "sectionIndex" -> Json.fromInt(0),
      "startingBeat" -> Json.fromInt(5)
    )
    val req  = postRequest(uri"/api/v1/editor/change-starting-beat", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val sb = json.hcursor
      .downField("data")
      .downField("sections")
      .downN(0)
      .downField("startingBeat")
      .as[Int]
      .getOrElse(fail("no startingBeat"))
    sb shouldBe 5
  }

  it should "reject invalid section index" in {
    val body = Json.obj(
      "composition"  -> minimalComposition.asJson,
      "sectionIndex" -> Json.fromInt(99),
      "startingBeat" -> Json.fromInt(5)
    )
    val req  = postRequest(uri"/api/v1/editor/change-starting-beat", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.BadRequest
  }

  it should "reject startingBeat out of range" in {
    val body = Json.obj(
      "composition"  -> minimalComposition.asJson,
      "sectionIndex" -> Json.fromInt(0),
      "startingBeat" -> Json.fromInt(0)
    )
    val req  = postRequest(uri"/api/v1/editor/change-starting-beat", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.BadRequest
  }

  it should "reject missing startingBeat field" in {
    val body = Json.obj(
      "composition"  -> minimalComposition.asJson,
      "sectionIndex" -> Json.fromInt(0)
    )
    val req  = postRequest(uri"/api/v1/editor/change-starting-beat", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  it should "reject invalid input" in {
    val body = Json.obj("bad" -> Json.fromString("data"))
    val req  = postRequest(uri"/api/v1/editor/change-starting-beat", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }
