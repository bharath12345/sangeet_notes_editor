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
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.server.routes.SectionRoutes

class SectionRoutesSpec extends AnyFlatSpec with Matchers:

  import TestFixtures.*

  val routes = Http4sServerInterpreter[IO]().toRoutes(SectionRoutes.all).orNotFound

  // --- add ---

  "POST /api/v1/sections/add" should "add a new section" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson,
      "name"        -> Json.fromString("Antara"),
      "sectionType" -> Json.fromString("antara")
    )
    val req  = postRequest(uri"/api/v1/sections/add", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data     = json.hcursor.downField("data")
    val sections = data.downField("sections").as[List[Json]].getOrElse(Nil)
    sections should have length 2
  }

  it should "add a taan section" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson,
      "name"        -> Json.fromString("Taan 1"),
      "sectionType" -> Json.fromString("taan")
    )
    val req  = postRequest(uri"/api/v1/sections/add", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json     = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data     = json.hcursor.downField("data")
    val sections = data.downField("sections").as[List[Json]].getOrElse(Nil)
    sections should have length 2
  }

  it should "reject missing fields" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson
    )
    val req  = postRequest(uri"/api/v1/sections/add", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- remove ---

  "POST /api/v1/sections/remove" should "remove a section" in {
    val twoSections =
      val comp   = minimalComposition
      val antara = Section("Antara", SectionType.Antara, Nil, None)
      comp.copy(sections = comp.sections :+ antara)
    val body = Json.obj(
      "composition"         -> twoSections.asJson,
      "currentSectionIndex" -> Json.fromInt(0),
      "indexToRemove"       -> Json.fromInt(1)
    )
    val req  = postRequest(uri"/api/v1/sections/remove", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data     = json.hcursor.downField("data")
    val sections = data.downField("composition").downField("sections").as[List[Json]].getOrElse(Nil)
    sections should have length 1
  }

  it should "reject removing the last section" in {
    val body = Json.obj(
      "composition"         -> minimalComposition.asJson,
      "currentSectionIndex" -> Json.fromInt(0),
      "indexToRemove"       -> Json.fromInt(0)
    )
    val req  = postRequest(uri"/api/v1/sections/remove", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  it should "reject out-of-bounds index" in {
    val body = Json.obj(
      "composition"         -> minimalComposition.asJson,
      "currentSectionIndex" -> Json.fromInt(0),
      "indexToRemove"       -> Json.fromInt(5)
    )
    val req  = postRequest(uri"/api/v1/sections/remove", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- clear ---

  "POST /api/v1/sections/clear" should "clear a section" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson,
      "index"       -> Json.fromInt(0)
    )
    val req  = postRequest(uri"/api/v1/sections/clear", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data     = json.hcursor.downField("data")
    val sections = data.downField("sections").as[List[Json]].getOrElse(Nil)
    sections should have length 1
    val events = sections.head.hcursor.downField("events").as[List[Json]].getOrElse(Nil)
    events shouldBe empty
  }

  it should "reject out-of-bounds index" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson,
      "index"       -> Json.fromInt(5)
    )
    val req  = postRequest(uri"/api/v1/sections/clear", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- reorder ---

  "POST /api/v1/sections/reorder" should "reorder sections" in {
    val twoSections =
      val comp   = minimalComposition
      val antara = Section("Antara", SectionType.Antara, Nil, None)
      comp.copy(sections = comp.sections :+ antara)
    val body = Json.obj(
      "composition"         -> twoSections.asJson,
      "currentSectionIndex" -> Json.fromInt(0),
      "from"                -> Json.fromInt(0),
      "to"                  -> Json.fromInt(1)
    )
    val req  = postRequest(uri"/api/v1/sections/reorder", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "reject invalid reorder indices" in {
    val body = Json.obj(
      "composition"         -> minimalComposition.asJson,
      "currentSectionIndex" -> Json.fromInt(0),
      "from"                -> Json.fromInt(0),
      "to"                  -> Json.fromInt(5)
    )
    val req  = postRequest(uri"/api/v1/sections/reorder", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }
