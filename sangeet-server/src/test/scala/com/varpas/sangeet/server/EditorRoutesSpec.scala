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

  // Plan 19 T2D: the "any valid envelope returns 200 with success body"
  // happy paths for the simple env-only endpoints (insert-rest, insert-sustain,
  // insert-chikari, delete-at-cursor) and the basic insert-swar/insert-dual-swar
  // shapes were removed in favor of the property-based coverage in
  // `routes/EditorRoutesPropSpec`. Per-endpoint behavioral specifics
  // (change-starting-beat synthesis, copy/cut/paste selection, group inserts,
  // chained operations, lockedCount==8) remain — the properties only assert
  // the envelope contract, not the editor semantics.

  // --- insert-swar ---

  // NOTE: removed three "insert Sa/Re/Ma" happy-path tests — subsumed by
  // `EditorRoutesPropSpec::propInsertSwarEnvelope` (every (note, variant,
  // octave) from achal-aware generators returns a success envelope).

  "POST /api/v1/editor/insert-swar" should "reject missing note field" in {
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

  // --- insert-rest / insert-sustain / insert-chikari ---

  // NOTE: removed the happy-path "should insert a rest / sustain / chikari"
  // tests and the per-endpoint "reject invalid input" tests — subsumed by
  // `EditorRoutesPropSpec::propEditorEnvelopeSuccess` and
  // `propEditorMalformedNeverServerError` respectively. The property loops
  // over all four envelope-only endpoints (insert-chikari, insert-rest,
  // insert-sustain, delete-at-cursor) with both well-formed and garbage
  // bodies, so each "{200 → success, garbage → 4xx with error envelope}"
  // assertion is exercised 100× per CI run.

  // The "return error with invalid input" example for insert-rest also
  // pinned that the failure envelope contains an `error.code` field —
  // that exact assertion now lives in `ApiEnvelopePropSpec::propFailureShape`,
  // applied universally across every failure envelope.

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

  // NOTE: removed "insert dual Sa" happy path — subsumed by
  // `EditorRoutesPropSpec::propInsertDualSwarEnvelope`.

  "POST /api/v1/editor/insert-dual-swar" should "reject missing note" in {
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

  // NOTE: removed "reject invalid input" — subsumed by
  // `EditorRoutesPropSpec::propEditorMalformedNeverServerError`. The
  // "delete event at cursor position" + "handle empty section gracefully"
  // tests are retained because the property's generator only produces
  // empty compositions; the non-empty delete behavior here is not exercised
  // by the property.

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
