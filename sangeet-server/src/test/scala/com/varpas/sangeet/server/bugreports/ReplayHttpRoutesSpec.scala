package com.varpas.sangeet.server.bugreports

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReplayHttpRoutesSpec extends AnyFlatSpec with Matchers:

  private val sampleUuid = "11111111-2222-3333-4444-555555555555"

  private def routesWith(storage: ReplayStorage) = ReplayHttpRoutes(storage).orNotFound

  // Lightweight fakes — distinct impl per scenario keeps each test self-contained.
  private object NotFoundStorage extends ReplayStorage:
    def get(reportId: String) = IO.pure(Left(ReplayStorage.Error.NotFound))
  private object NotConfiguredStorage extends ReplayStorage:
    def get(reportId: String) = IO.pure(Left(ReplayStorage.Error.NotConfigured))
  private object ReadFailedStorage extends ReplayStorage:
    def get(reportId: String) = IO.pure(Left(ReplayStorage.Error.ReadFailed("simulated outage")))
  private class FoundStorage(bytes: Array[Byte]) extends ReplayStorage:
    def get(reportId: String) = IO.pure(Right(bytes))

  "GET /replay/<uuid>" should "serve the static HTML player" in {
    val routes = routesWith(NotFoundStorage) // storage not consulted for the HTML route
    val resp   = routes.run(Request[IO](Method.GET, Uri.unsafeFromString(s"/replay/$sampleUuid"))).unsafeRunSync()
    // http4s response bodies are stream-once — read once, assert against the buffered value.
    val body = resp.as[String].unsafeRunSync()
    resp.status shouldBe Status.Ok
    resp.headers.get(org.typelevel.ci.CIString("Content-Type")).map(_.head.value).getOrElse("") should include(
      "text/html"
    )
    body should include("Sangeet bug replay")
    body should include("rrweb-player")
  }

  "GET /replay/<invalid>" should "return 400 with invalid_uuid" in {
    val resp = routesWith(NotFoundStorage)
      .run(Request[IO](Method.GET, uri"/replay/not-a-uuid"))
      .unsafeRunSync()
    resp.status shouldBe Status.BadRequest
    resp.as[String].unsafeRunSync() should include("invalid_uuid")
  }

  "GET /replay/<uuid>/data" should "stream the JSON bytes on success" in {
    val payload = """{"description":"hi","replay":[]}""".getBytes("UTF-8")
    val routes  = routesWith(new FoundStorage(payload))
    val resp    = routes.run(Request[IO](Method.GET, Uri.unsafeFromString(s"/replay/$sampleUuid/data"))).unsafeRunSync()

    resp.status shouldBe Status.Ok
    resp.headers.get(org.typelevel.ci.CIString("Content-Type")).map(_.head.value).getOrElse("") should include(
      "application/json"
    )
    resp.as[String].unsafeRunSync() shouldBe new String(payload, "UTF-8")
  }

  it should "return 404 when the object is missing" in {
    val resp = routesWith(NotFoundStorage)
      .run(Request[IO](Method.GET, Uri.unsafeFromString(s"/replay/$sampleUuid/data")))
      .unsafeRunSync()
    resp.status shouldBe Status.NotFound
    resp.as[String].unsafeRunSync() should include("not_found")
  }

  it should "return 503 when storage is unconfigured" in {
    val resp = routesWith(NotConfiguredStorage)
      .run(Request[IO](Method.GET, Uri.unsafeFromString(s"/replay/$sampleUuid/data")))
      .unsafeRunSync()
    resp.status shouldBe Status.ServiceUnavailable
    resp.as[String].unsafeRunSync() should include("storage_not_configured")
  }

  it should "return 502 when storage read fails" in {
    val resp = routesWith(ReadFailedStorage)
      .run(Request[IO](Method.GET, Uri.unsafeFromString(s"/replay/$sampleUuid/data")))
      .unsafeRunSync()
    resp.status shouldBe Status.BadGateway
    resp.as[String].unsafeRunSync() should include("storage_read_failed")
  }
