package com.varpas.sangeet.server.bugreports

import java.nio.charset.StandardCharsets
import java.util.Base64

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReplayAuthSpec extends AnyFlatSpec with Matchers:

  private val protectedRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root / "secret" =>
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
    val req    = Request[IO](Method.GET, uri"/secret").putHeaders(basicHeader("any", "hunter2"))

    val resp = routes.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.Ok
    resp.as[String].unsafeRunSync() shouldBe "ok"
  }

  it should "return 401 with WWW-Authenticate when the header is missing" in {
    val routes = ReplayAuth.middleware(protectedRoutes, Some("hunter2"))
    val req    = Request[IO](Method.GET, uri"/secret")

    val resp = routes.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.Unauthorized
    resp.headers.get(org.typelevel.ci.CIString("WWW-Authenticate")).map(_.head.value).getOrElse("") should
      include("Basic realm=")
  }

  it should "return 401 with WWW-Authenticate when the password is wrong" in {
    val routes = ReplayAuth.middleware(protectedRoutes, Some("hunter2"))
    val req    = Request[IO](Method.GET, uri"/secret").putHeaders(basicHeader("any", "wrong"))

    val resp = routes.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.Unauthorized
  }

  it should "return 401 when the header decodes to malformed credentials" in {
    val routes = ReplayAuth.middleware(protectedRoutes, Some("hunter2"))
    val req = Request[IO](Method.GET, uri"/secret")
      .putHeaders(Header.Raw(org.typelevel.ci.CIString("Authorization"), "Basic !!notbase64!!"))

    val resp = routes.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.Unauthorized
  }

  it should "return 503 when no password is configured (env var unset)" in {
    val routes = ReplayAuth.middleware(protectedRoutes, None)
    val req    = Request[IO](Method.GET, uri"/secret").putHeaders(basicHeader("any", "anything"))

    val resp = routes.orNotFound.run(req).unsafeRunSync()
    resp.status shouldBe Status.ServiceUnavailable
    resp.as[String].unsafeRunSync() should include("replay_viewer_disabled")
  }
