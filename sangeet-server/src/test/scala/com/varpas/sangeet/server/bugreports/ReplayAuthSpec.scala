package com.varpas.sangeet.server.bugreports

import java.nio.charset.StandardCharsets
import java.util.Base64

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.semigroupk._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReplayAuthSpec extends AnyFlatSpec with Matchers:

  // Auth only fires on /replay/* paths (see the regression test at the bottom of
  // this file for why). Use a /replay-shaped route for the auth-positive tests.
  private val protectedRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root / "replay" / _ =>
    Ok("ok")
  }

  private def basicHeader(user: String, pass: String): Header.Raw =
    val raw = s"$user:$pass".getBytes(StandardCharsets.UTF_8)
    Header.Raw(
      org.typelevel.ci.CIString("Authorization"),
      "Basic " + Base64.getEncoder.encodeToString(raw)
    )

  "ReplayAuth.middleware" should "let through requests with the correct password" in {
    val routes = ReplayAuth.middleware(protectedRoutes, Some("hunter2"))
    val req    = Request[IO](Method.GET, uri"/replay/abc").putHeaders(basicHeader("any", "hunter2"))

    val resp = routes.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.Ok
    resp.as[String].unsafeRunSync() shouldBe "ok"
  }

  it should "return 401 with WWW-Authenticate when the header is missing" in {
    val routes = ReplayAuth.middleware(protectedRoutes, Some("hunter2"))
    val req    = Request[IO](Method.GET, uri"/replay/abc")

    val resp = routes.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.Unauthorized
    resp.headers.get(org.typelevel.ci.CIString("WWW-Authenticate")).map(_.head.value).getOrElse("") should
      include("Basic realm=")
  }

  it should "return 401 with WWW-Authenticate when the password is wrong" in {
    val routes = ReplayAuth.middleware(protectedRoutes, Some("hunter2"))
    val req    = Request[IO](Method.GET, uri"/replay/abc").putHeaders(basicHeader("any", "wrong"))

    val resp = routes.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.Unauthorized
  }

  it should "return 401 when the header decodes to malformed credentials" in {
    val routes = ReplayAuth.middleware(protectedRoutes, Some("hunter2"))
    val req = Request[IO](Method.GET, uri"/replay/abc")
      .putHeaders(Header.Raw(org.typelevel.ci.CIString("Authorization"), "Basic !!notbase64!!"))

    val resp = routes.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.Unauthorized
  }

  it should "return 503 when no password is configured (env var unset)" in {
    val routes = ReplayAuth.middleware(protectedRoutes, None)
    val req    = Request[IO](Method.GET, uri"/replay/abc").putHeaders(basicHeader("any", "anything"))

    val resp = routes.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.ServiceUnavailable
    resp.as[String].unsafeRunSync() should include("replay_viewer_disabled")
  }

  // CRITICAL regression test. The first version of this middleware short-
  // circuited on EVERY request (always returned Some). When wired into
  // Main.scala via `replayRoutes <+> tapirRoutes`, that meant every API
  // request (e.g. /api/v1/cursor/*, /metrics, /health) returned 401 or 503
  // because the middleware fired before the Tapir routes got a chance. The
  // E2E test suite caught it. The fix is to short-circuit non-/replay paths
  // to OptionT.none so `<+>` lets the next route set handle them.
  it should "pass through non-/replay paths so other route sets can handle them" in {
    val replayRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root / "replay" / _ =>
      Ok("replay-page")
    }
    val apiRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root / "api" / "v1" / "health" =>
      Ok("api-health-ok")
    }
    val combined = ReplayAuth.middleware(replayRoutes, None) <+> apiRoutes

    val healthReq = Request[IO](Method.GET, uri"/api/v1/health")
    val healthRsp = combined.orNotFound.run(healthReq).unsafeRunSync()
    healthRsp.status shouldBe Status.Ok
    healthRsp.as[String].unsafeRunSync() shouldBe "api-health-ok"

    // And the replay path is still gated by the (unconfigured) middleware.
    val replayReq = Request[IO](Method.GET, uri"/replay/abc")
    val replayRsp = combined.orNotFound.run(replayReq).unsafeRunSync()
    replayRsp.status shouldBe Status.ServiceUnavailable
  }

  it should "pass through unrelated paths even when password is configured" in {
    val replayRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root / "replay" / _ =>
      Ok("replay-page")
    }
    val metricsRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root / "metrics" =>
      Ok("prometheus-text")
    }
    val combined = ReplayAuth.middleware(replayRoutes, Some("hunter2")) <+> metricsRoutes

    val req  = Request[IO](Method.GET, uri"/metrics") // no Authorization header
    val resp = combined.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.Ok
    resp.as[String].unsafeRunSync() shouldBe "prometheus-text"
  }
