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
import com.varpas.sangeet.server.routes.LayoutRoutes
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.Codecs.given

class LayoutRoutesSpec extends AnyFlatSpec with Matchers:

  import TestFixtures.*

  val routes = Http4sServerInterpreter[IO]().toRoutes(LayoutRoutes.all).orNotFound

  "POST /api/v1/layout/compute" should "compute layout for empty composition" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson
    )
    val req = postRequest(uri"/api/v1/layout/compute", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = json.hcursor.downField("data").as[List[Json]].getOrElse(Nil)
    data should have length 1
    data.head.hcursor.get[String]("sectionName").getOrElse("") shouldBe "Sthayi"
  }

  it should "compute layout for composition with swar" in {
    val body = Json.obj(
      "composition" -> compositionWithSwar.asJson
    )
    val req = postRequest(uri"/api/v1/layout/compute", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "compute layout with custom config" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson,
      "highDensityThreshold" -> Json.fromInt(3),
      "cellWidthBase" -> Json.fromDoubleOrNull(80.0),
      "cellOverflowExpand" -> Json.fromDoubleOrNull(20.0),
      "lineSpacing" -> Json.fromDoubleOrNull(50.0),
      "headerHeight" -> Json.fromDoubleOrNull(100.0)
    )
    val req = postRequest(uri"/api/v1/layout/compute", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
  }

  it should "compute layout for multi-section composition" in {
    val comp = {
      val c = minimalComposition
      val antara = Section("Antara", SectionType.Antara, Nil, None)
      c.copy(sections = c.sections :+ antara)
    }
    val body = Json.obj(
      "composition" -> comp.asJson
    )
    val req = postRequest(uri"/api/v1/layout/compute", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data").as[List[Json]].getOrElse(Nil)
    data should have length 2
  }

  it should "return grid lines with correct structure" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson
    )
    val req = postRequest(uri"/api/v1/layout/compute", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data").as[List[Json]].getOrElse(Nil)
    val firstSection = data.head.hcursor
    firstSection.downField("sectionName").succeeded shouldBe true
    firstSection.downField("sectionType").succeeded shouldBe true
    firstSection.downField("lines").succeeded shouldBe true
  }

  it should "reject missing composition" in {
    val body = Json.obj("bad" -> Json.fromString("data"))
    val req = postRequest(uri"/api/v1/layout/compute", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }
