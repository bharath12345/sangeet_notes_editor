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
import com.varpas.sangeet.server.routes.CompositionRoutes

class CompositionRoutesSpec extends AnyFlatSpec with Matchers:

  import TestFixtures.*

  val routes = Http4sServerInterpreter[IO]().toRoutes(CompositionRoutes.all).orNotFound

  // --- create ---

  "POST /api/v1/compositions" should "create a Gat composition" in {
    val body = Json.obj(
      "title"           -> Json.fromString("My Gat"),
      "compositionType" -> Json.fromString("gat"),
      "taal"            -> teentaal.asJson,
      "raag"            -> yaman.asJson,
      "laya"            -> Json.fromString("vilambit"),
      "showStrokeLine"  -> Json.fromBoolean(true),
      "showSahityaLine" -> Json.fromBoolean(false)
    )
    val req  = postRequest(uri"/api/v1/compositions", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = json.hcursor.downField("data")
    data.downField("metadata").downField("title").as[String].getOrElse("") shouldBe "My Gat"
    val sections = data.downField("sections").as[List[Json]].getOrElse(Nil)
    sections.nonEmpty shouldBe true
  }

  it should "create a Bandish composition" in {
    val body = Json.obj(
      "title"           -> Json.fromString("My Bandish"),
      "compositionType" -> Json.fromString("bandish"),
      "taal"            -> teentaal.asJson,
      "raag"            -> yaman.asJson,
      "laya"            -> Json.fromString("madhya")
    )
    val req  = postRequest(uri"/api/v1/compositions", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "create a Palta composition with taan count" in {
    val body = Json.obj(
      "title"           -> Json.fromString("Yaman Palta"),
      "compositionType" -> Json.fromString("palta"),
      "taal"            -> teentaal.asJson,
      "raag"            -> yaman.asJson,
      "taanCount"       -> Json.fromInt(3)
    )
    val req  = postRequest(uri"/api/v1/compositions", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
  }

  it should "reject missing required fields" in {
    val body = Json.obj(
      "title" -> Json.fromString("Bad")
    )
    val req  = postRequest(uri"/api/v1/compositions", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- serialize ---

  "POST /api/v1/compositions/serialize" should "serialize a composition to JSON string" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson
    )
    val req  = postRequest(uri"/api/v1/compositions/serialize", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = json.hcursor.downField("data")
    data.downField("metadata").downField("title").as[String].getOrElse("") shouldBe "Test Composition"
  }

  // --- parse ---

  "POST /api/v1/compositions/parse" should "parse a valid JSON string into composition" in {
    val serialized = minimalComposition.asJson.noSpaces
    val body = Json.obj(
      "json" -> Json.fromString(serialized)
    )
    val req  = postRequest(uri"/api/v1/compositions/parse", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = json.hcursor.downField("data")
    data.downField("metadata").downField("title").as[String].getOrElse("") shouldBe "Test Composition"
  }

  it should "reject invalid JSON" in {
    val body = Json.obj(
      "json" -> Json.fromString("{invalid json!!!}")
    )
    val req  = postRequest(uri"/api/v1/compositions/parse", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  // --- roundtrip ---

  "Compositions serialize then parse" should "produce equivalent composition" in {
    val serBody = Json.obj("composition" -> minimalComposition.asJson)
    val serReq  = postRequest(uri"/api/v1/compositions/serialize", serBody)
    val serResp = routes.run(serReq).unsafeRunSync()

    serResp.status shouldBe Status.Ok
    val serJson        = parse(serResp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val serializedJson = serJson.hcursor.downField("data").focus.getOrElse(fail("no data"))
    val serializedStr  = serializedJson.noSpaces

    val parseBody = Json.obj("json" -> Json.fromString(serializedStr))
    val parseReq  = postRequest(uri"/api/v1/compositions/parse", parseBody)
    val parseResp = routes.run(parseReq).unsafeRunSync()

    parseResp.status shouldBe Status.Ok
    val parseJson = parse(parseResp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    parseJson.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    val data = parseJson.hcursor.downField("data")
    data.downField("metadata").downField("title").as[String].getOrElse("") shouldBe "Test Composition"
  }
