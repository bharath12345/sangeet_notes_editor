package com.varpas.sangeet.server

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.parser.*
import io.circe.Json
import io.circe.syntax.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.varpas.sangeet.server.routes.OrnamentRoutes
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.Codecs.given

class OrnamentRoutesSpec extends AnyFlatSpec with Matchers:

  import TestFixtures.*

  val routes = Http4sServerInterpreter[IO]().toRoutes(OrnamentRoutes.all).orNotFound

  // --- simple ornaments ---

  "POST /api/v1/ornament/simple" should "add gamak ornament" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "ornamentType" -> Json.fromString("gamak")
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/simple", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    json.hcursor.downField("data").succeeded shouldBe true
  }

  it should "add andolan ornament" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "ornamentType" -> Json.fromString("andolan")
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/simple", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "add gitkari ornament" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "ornamentType" -> Json.fromString("gitkari")
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/simple", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "reject invalid ornament type" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "ornamentType" -> Json.fromString("invalid_type")
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/simple", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- single-note ornaments ---

  "POST /api/v1/ornament/single-note" should "add kanSwar ornament" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "ornamentType" -> Json.fromString("kanSwar"),
      "noteRef" -> noteRefJson("re")
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/single-note", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "add sparsh ornament" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "ornamentType" -> Json.fromString("sparsh"),
      "noteRef" -> noteRefJson("re")
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/single-note", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "add ghaseet ornament" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "ornamentType" -> Json.fromString("ghaseet"),
      "noteRef" -> noteRefJson("re")
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/single-note", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "reject invalid ornament type" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "ornamentType" -> Json.fromString("fakeType"),
      "noteRef" -> noteRefJson("sa")
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/single-note", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- meend ---

  "POST /api/v1/ornament/meend" should "add ascending meend" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "startNote" -> noteRefJson("sa"),
      "endNote" -> noteRefJson("ga"),
      "direction" -> Json.fromString("ascending"),
      "intermediateNotes" -> Json.arr()
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/meend", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "add descending meend" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "startNote" -> noteRefJson("pa"),
      "endNote" -> noteRefJson("sa"),
      "direction" -> Json.fromString("descending")
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/meend", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "add meend with intermediate notes" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "startNote" -> noteRefJson("sa"),
      "endNote" -> noteRefJson("pa"),
      "direction" -> Json.fromString("ascending"),
      "intermediateNotes" -> Json.arr(noteRefJson("re"), noteRefJson("ga"))
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/meend", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  // --- krintan ---

  "POST /api/v1/ornament/krintan" should "add krintan with notes" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "notes" -> Json.arr(noteRefJson("sa"), noteRefJson("re"))
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/krintan", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  // --- murki ---

  "POST /api/v1/ornament/murki" should "add murki with notes" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "notes" -> Json.arr(noteRefJson("sa"), noteRefJson("re"), noteRefJson("ga"))
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/murki", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  // --- zamzama ---

  "POST /api/v1/ornament/zamzama" should "add zamzama with notes" in {
    val body = editorInputJson(compositionWithSwar).deepMerge(Json.obj(
      "notes" -> Json.arr(noteRefJson("sa"), noteRefJson("sa"), noteRefJson("sa"))
    ))
    val req = postRequest(uri"/api/v1/editor/ornament/zamzama", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  // --- error cases ---

  "Ornament routes" should "reject missing editor input" in {
    val body = Json.obj("ornamentType" -> Json.fromString("gamak"))
    val req = postRequest(uri"/api/v1/editor/ornament/simple", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }
