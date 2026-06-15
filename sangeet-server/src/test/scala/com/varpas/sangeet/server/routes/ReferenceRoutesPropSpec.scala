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

import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.taal.Taals
import com.varpas.sangeet.server.generators.RequestGenerators

/** Plan 19 T2A introduced the first end-to-end property here (every known raag returns 200 with `success: true`). T2B
  * extends this spec with sibling contracts for the rest of the reference route family:
  *
  *   - every known taal returns 200 with a valid body shape
  *   - listing endpoints (raags / taals) always return the full registry
  *   - unknown raag / taal names return 404 (and never 500)
  *
  * The properties are universally quantified over the registry maps so any future addition to `Raags.all` or
  * `Taals.all` is exercised automatically — there's no per-entry test to maintain.
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

  test("propGetTaalByName: every known taal returns 200 with success=true and matras>0") {
    forAll(RequestGenerators.genGetTaalPath) { taalName =>
      val uri  = uri"/api/v1/taals" / taalName
      val req  = Request[IO](Method.GET, uri)
      val resp = routes.run(req).unsafeRunSync()

      resp.status shouldBe Status.Ok

      val body = resp.as[String].unsafeRunSync()
      val json = parse(body).getOrElse(fail(s"Failed to parse response body for taal '$taalName': $body"))
      json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

      // Spot-check the body shape — the route hands back the full Taal codec
      // output. `matras` is the most universally meaningful field and must be a
      // positive int for every catalog entry (sanity guard against a future
      // accidentally-zero taal in the registry).
      val matras = json.hcursor.downField("data").get[Int]("matras").getOrElse(0)
      matras should be > 0
    }
  }

  test("propGetUnknownRaag: unknown raag name returns 404 with success=false (never 5xx)") {
    forAll(RequestGenerators.genUnknownRaagPath) { raagName =>
      val uri  = uri"/api/v1/raags" / raagName
      val req  = Request[IO](Method.GET, uri)
      val resp = routes.run(req).unsafeRunSync()

      resp.status shouldBe Status.NotFound

      val body = resp.as[String].unsafeRunSync()
      val json = parse(body).getOrElse(fail(s"Failed to parse error response: $body"))
      json.hcursor.get[Boolean]("success").getOrElse(true) shouldBe false
      // The error envelope must include a structured `error.code` field —
      // clients dispatch on this rather than HTTP status alone.
      json.hcursor.downField("error").downField("code").succeeded shouldBe true
    }
  }

  test("propGetUnknownTaal: unknown taal name returns 404 with success=false (never 5xx)") {
    forAll(RequestGenerators.genUnknownTaalPath) { taalName =>
      val uri  = uri"/api/v1/taals" / taalName
      val req  = Request[IO](Method.GET, uri)
      val resp = routes.run(req).unsafeRunSync()

      resp.status shouldBe Status.NotFound

      val body = resp.as[String].unsafeRunSync()
      val json = parse(body).getOrElse(fail(s"Failed to parse error response: $body"))
      json.hcursor.get[Boolean]("success").getOrElse(true) shouldBe false
      json.hcursor.downField("error").downField("code").succeeded shouldBe true
    }
  }

  // ---- List-endpoint contract -----------------------------------------------------
  //
  // These two assertions aren't `forAll` properties — there's nothing to quantify
  // — but they live here next to the get-by-name properties so the registry
  // round-trip ("each name from the list endpoint is reachable via get") is
  // self-contained in one spec. Without this pairing, a regression that drops
  // entries from the list endpoint (e.g. silently filters by a flag) would still
  // pass the per-name property and only fail much later in integration.

  test("propListRaagsContainsRegistry: GET /api/v1/raags lists every entry in Raags.all") {
    val req  = Request[IO](Method.GET, uri"/api/v1/raags")
    val resp = routes.run(req).unsafeRunSync()
    resp.status shouldBe Status.Ok

    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail(s"Failed to parse: $body"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val keys = json.hcursor.downField("data").keys.getOrElse(Nil).toSet
    keys shouldBe Raags.all.keys.toSet
  }

  test("propListTaalsContainsRegistry: GET /api/v1/taals lists every entry in Taals.all") {
    val req  = Request[IO](Method.GET, uri"/api/v1/taals")
    val resp = routes.run(req).unsafeRunSync()
    resp.status shouldBe Status.Ok

    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail(s"Failed to parse: $body"))
    json.hcursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val keys = json.hcursor.downField("data").keys.getOrElse(Nil).toSet
    keys shouldBe Taals.all.keys.toSet
  }
