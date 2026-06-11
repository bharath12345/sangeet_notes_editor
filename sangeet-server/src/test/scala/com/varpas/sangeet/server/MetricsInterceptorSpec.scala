package com.varpas.sangeet.server

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

import com.varpas.sangeet.server.metrics.{HttpMetrics, MetricsRegistry}
import com.varpas.sangeet.server.routes.ReferenceRoutes

/** Smoke test that the [[HttpMetrics]] interceptor is correctly wired into a [[Http4sServerInterpreter]] and that
  * completed requests emit meters into the shared [[MetricsRegistry]] composite. Verifies the path *template* (not the
  * literal URL value) is what lands in the label, which is the critical cardinality-safety property.
  */
class MetricsInterceptorSpec extends AnyFlatSpec with Matchers:

  private val options = Http4sServerOptions
    .customiseInterceptors[IO]
    .metricsInterceptor(HttpMetrics.requestInterceptor)
    .options

  private val routes = Http4sServerInterpreter[IO](options).toRoutes(ReferenceRoutes.all).orNotFound

  "HttpMetrics interceptor" should "record request count + duration labeled with the route template" in {
    val req  = Request[IO](Method.GET, uri"/api/v1/raags/Yaman")
    val resp = routes.run(req).unsafeRunSync()
    resp.status shouldBe Status.Ok
    // Body must be consumed so Tapir's onResponseBody callback fires.
    val _ = resp.as[String].unsafeRunSync()

    val scrape = MetricsRegistry.scrape()
    scrape should include("tapir_request_total")
    scrape should include("tapir_request_duration")
    // Path template — `{name}` placeholder — must be in the label, NOT the literal "Yaman"
    scrape should include("""path="/api/v1/raags/{name}"""")
    scrape should not include """path="/api/v1/raags/Yaman""""
  }
