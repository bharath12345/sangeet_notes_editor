package com.varpas.sangeet.server.routes

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.parser._
import org.http4s._
import org.http4s.implicits._
import org.scalacheck.Shrink
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sttp.tapir.server.http4s.Http4sServerInterpreter

import com.varpas.sangeet.server.generators.RequestGenerators

/** Plan 19 T2A — the one and only sample property for sangeet-server.
  *
  * Phase A scope: introduce ScalaCheck to the server module via a single end-to-end property against an existing
  * Reference endpoint with a simple contract. Additional properties land in later phases.
  *
  * The property exercises `GET /api/v1/raags/{name}` for every name drawn from the built-in [[Raags.all]] registry — a
  * universal-quantification over the 26 known raags asserting the route returns 200 OK and `success: true`. This is
  * stronger than `ReferenceRoutesSpec`'s single-fixture test (`yaman` only), which only covered one of the 26.
  */
class ReferenceRoutesPropSpec extends AnyFunSuite with Matchers with ScalaCheckPropertyChecks:

  private val routes: HttpApp[IO] =
    Http4sServerInterpreter[IO]().toRoutes(ReferenceRoutes.all).orNotFound

  // Disable ScalaCheck's default String shrinking. The default shrinker walks
  // String values toward "" / " ", which is wrong for our enumerated path-param
  // generator (every value in `Raags.all.keys` is a valid raag name; shrinking
  // produces inputs the generator would never emit). `Shrink.shrinkAny` is the
  // canonical "no shrink" stand-in.
  private given Shrink[String] = Shrink.shrinkAny[String]

  test("propGetRaagByName: every known raag returns 200 with success=true") {
    forAll(RequestGenerators.genGetRaagPath) { raagName =>
      // Build the URI via path-segment append rather than string interpolation
      // so http4s percent-encodes spaces and other reserved chars correctly —
      // a handful of registry keys like "miyan ki malhar" contain spaces.
      val uri  = uri"/api/v1/raags" / raagName
      val req  = Request[IO](Method.GET, uri)
      val resp = routes.run(req).unsafeRunSync()

      resp.status shouldBe Status.Ok

      val body = resp.as[String].unsafeRunSync()
      val json = parse(body).getOrElse(fail(s"Failed to parse response body for raag '$raagName': $body"))
      json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true
    }
  }
