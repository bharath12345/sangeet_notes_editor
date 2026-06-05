package com.varpas.sangeet.server

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.parser._
import org.http4s._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerInterpreter

class HealthCheckSpec extends AnyFlatSpec with Matchers:

  // Recreate the health endpoint from Main
  private val healthEndpoint: sttp.tapir.server.ServerEndpoint[Any, IO] =
    endpoint.get
      .in("health")
      .out(jsonBody[Json])
      .serverLogicSuccess { _ =>
        IO.pure(
          Json.obj(
            "status"  -> Json.fromString("ok"),
            "service" -> Json.fromString("sangeet-server"),
            "version" -> Json.fromString("0.2.0")
          )
        )
      }

  val routes = Http4sServerInterpreter[IO]().toRoutes(List(healthEndpoint)).orNotFound

  "GET /health" should "return status ok" in {
    val req  = Request[IO](Method.GET, uri"/health")
    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok

    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail("Failed to parse JSON"))

    val cursor = json.hcursor
    cursor.get[String]("status").getOrElse("") shouldBe "ok"
    cursor.get[String]("service").getOrElse("") shouldBe "sangeet-server"
    cursor.get[String]("version").getOrElse("") shouldBe "0.2.0"
  }
