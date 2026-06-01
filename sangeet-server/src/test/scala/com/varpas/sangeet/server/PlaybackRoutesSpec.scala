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
import com.varpas.sangeet.server.routes.PlaybackRoutes
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.Codecs.given

class PlaybackRoutesSpec extends AnyFlatSpec with Matchers:

  import TestFixtures.*

  val routes = Http4sServerInterpreter[IO]().toRoutes(PlaybackRoutes.all).orNotFound

  "POST /api/v1/playback/schedule" should "schedule empty composition" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson,
      "bpm" -> Json.fromDoubleOrNull(120.0)
    )
    val req = postRequest(uri"/api/v1/playback/schedule", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = json.hcursor.downField("data").as[List[Json]].getOrElse(Nil)
    data shouldBe empty
  }

  it should "schedule composition with swar" in {
    val body = Json.obj(
      "composition" -> compositionWithSwar.asJson,
      "bpm" -> Json.fromDoubleOrNull(120.0)
    )
    val req = postRequest(uri"/api/v1/playback/schedule", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = json.hcursor.downField("data").as[List[Json]].getOrElse(Nil)
    data.nonEmpty shouldBe true
    val first = data.head.hcursor
    first.downField("timeMs").succeeded shouldBe true
    first.downField("durationMs").succeeded shouldBe true
    first.downField("note").succeeded shouldBe true
  }

  it should "schedule with different BPM" in {
    val body = Json.obj(
      "composition" -> compositionWithSwar.asJson,
      "bpm" -> Json.fromDoubleOrNull(60.0)
    )
    val req = postRequest(uri"/api/v1/playback/schedule", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok
    val json = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("parse"))
    val data = json.hcursor.downField("data").as[List[Json]].getOrElse(Nil)
    data.nonEmpty shouldBe true

    val durationMs = data.head.hcursor.get[Long]("durationMs").getOrElse(0L)
    durationMs should be > 0L
  }

  it should "reject missing BPM" in {
    val body = Json.obj(
      "composition" -> minimalComposition.asJson
    )
    val req = postRequest(uri"/api/v1/playback/schedule", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }

  it should "reject missing composition" in {
    val body = Json.obj(
      "bpm" -> Json.fromDoubleOrNull(120.0)
    )
    val req = postRequest(uri"/api/v1/playback/schedule", body)
    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok
  }
