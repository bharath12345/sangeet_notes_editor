package com.varpas.sangeet.server

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.parser.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.varpas.sangeet.server.routes.ReferenceRoutes

class ReferenceRoutesSpec extends AnyFlatSpec with Matchers:

  val routes = Http4sServerInterpreter[IO]().toRoutes(ReferenceRoutes.all).orNotFound

  "GET /api/v1/taals" should "return all 11 taals" in {
    val req = Request[IO](Method.GET, uri"/api/v1/taals")
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok

    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail("Failed to parse JSON"))

    val cursor = json.hcursor
    cursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = cursor.downField("data")
    val taalNames = data.keys.getOrElse(Nil).toList
    taalNames should have length 11
    taalNames should contain allOf ("teentaal", "ektaal", "jhaptaal", "rupak", "dadra", "keherwa")
  }

  "GET /api/v1/taals/teentaal" should "return Teentaal with 16 matras" in {
    val req = Request[IO](Method.GET, uri"/api/v1/taals/teentaal")
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok

    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail("Failed to parse JSON"))

    val cursor = json.hcursor
    cursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = cursor.downField("data")
    data.get[Int]("matras").getOrElse(0) shouldBe 16
    data.get[String]("name").getOrElse("") shouldBe "Teentaal"
  }

  "GET /api/v1/taals/nonexistent" should "return error" in {
    val req = Request[IO](Method.GET, uri"/api/v1/taals/nonexistent")
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok

    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail("Failed to parse JSON"))

    val cursor = json.hcursor
    cursor.get[Boolean]("success").getOrElse(true) shouldBe false
    cursor.downField("error").downField("code").succeeded shouldBe true
  }

  "GET /api/v1/raags" should "return all 26 raags" in {
    val req = Request[IO](Method.GET, uri"/api/v1/raags")
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok

    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail("Failed to parse JSON"))

    val cursor = json.hcursor
    cursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = cursor.downField("data")
    val raagNames = data.keys.getOrElse(Nil).toList
    raagNames should have length 26
    raagNames should contain allOf ("yaman", "bhairav", "bhairavi", "bilawal")
  }

  "GET /api/v1/raags/yaman" should "return Yaman raag" in {
    val req = Request[IO](Method.GET, uri"/api/v1/raags/yaman")
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok

    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail("Failed to parse JSON"))

    val cursor = json.hcursor
    cursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = cursor.downField("data")
    data.get[String]("name").getOrElse("") shouldBe "Yaman"
    data.get[String]("thaat").getOrElse("").toLowerCase shouldBe "kalyan"
  }

  "GET /api/v1/raags/nonexistent" should "return error" in {
    val req = Request[IO](Method.GET, uri"/api/v1/raags/nonexistent")
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok

    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail("Failed to parse JSON"))

    val cursor = json.hcursor
    cursor.get[Boolean]("success").getOrElse(true) shouldBe false
    cursor.downField("error").downField("code").succeeded shouldBe true
  }
