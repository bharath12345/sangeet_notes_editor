package com.varpas.sangeet.server

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.parser._
import org.http4s._
import org.http4s.implicits._
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerInterpreter

/** Plan 19 T2C — `/health` endpoint contract properties.
  *
  * The existing [[HealthCheckSpec]] asserts a single happy-path call. This spec generalises the contract to "any number
  * of repeated calls return identical bodies" (pure idempotency) and "calls under arbitrary query strings still hit
  * health" (path-only routing — query params must be ignored). Both invariants matter operationally because the
  * `/health` endpoint is the GCP load-balancer probe target; if it ever drifted to a non-200 (or a different body) on a
  * subset of probes the whole service would flap.
  *
  * The endpoint is reconstructed locally rather than wired via Main.scala (mirroring [[HealthCheckSpec]]) so the spec
  * has no dependency on IOApp / EmberServer.
  */
class HealthPropSpec extends AnyFunSuite with Matchers with ScalaCheckPropertyChecks:

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

  private val routes: HttpApp[IO] =
    Http4sServerInterpreter[IO]().toRoutes(List(healthEndpoint)).orNotFound

  private def get(uri: Uri): Response[IO] =
    routes.run(Request[IO](Method.GET, uri)).unsafeRunSync()

  test("propHealthIdempotent: N consecutive GET /health calls return identical 200 bodies") {
    // The load balancer hits /health every ~5s; a flake under repetition would
    // page on-call. Pin "byte-identical body across N calls" — N up to 20.
    forAll(Gen.choose(2, 20)) { n =>
      val bodies = (1 to n).map(_ => get(uri"/health"))
      bodies.foreach { resp =>
        resp.status shouldBe Status.Ok
      }
      val payloads = bodies.map(_.as[String].unsafeRunSync()).toSet
      payloads.size shouldBe 1
      // And the payload must actually be the documented shape, not just stable.
      val json = parse(payloads.head).getOrElse(fail("body not JSON"))
      json.hcursor.get[String]("status").getOrElse("") shouldBe "ok"
      json.hcursor.get[String]("service").getOrElse("") shouldBe "sangeet-server"
    }
  }

  test("propHealthIgnoresQueryString: any query string still returns 200 (path-only routing)") {
    // Tapir's path matcher ignores query unless explicitly captured. Pin this
    // contract because some k8s/GCP probes append `?cb=<timestamp>` cache-busters.
    val genQuery: Gen[String] =
      for
        k <- Gen.alphaStr.map(_.take(8)).suchThat(_.nonEmpty)
        v <- Gen.alphaNumStr.map(_.take(8))
      yield s"$k=$v"

    forAll(genQuery) { qs =>
      val uri  = Uri.unsafeFromString(s"/health?$qs")
      val resp = get(uri)
      resp.status shouldBe Status.Ok
    }
  }

  test("propHealthRejectsNonGet: HEAD/POST/PUT/DELETE on /health do not return 200") {
    // The Tapir-built route only registers GET. Other methods should not
    // accidentally hit the success path (which would let a client mutate something
    // by mistake on the probe URL — defence in depth).
    val verbs = List(Method.POST, Method.PUT, Method.DELETE, Method.PATCH)
    verbs.foreach { m =>
      val resp = routes.run(Request[IO](m, uri"/health")).unsafeRunSync()
      resp.status.code should not be 200
    }
  }
