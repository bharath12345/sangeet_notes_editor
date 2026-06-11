package com.varpas.sangeet.server.bugreports

import java.nio.charset.StandardCharsets

import cats.effect.IO
import io.circe.Json
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.`Content-Type`

/** Raw http4s routes for the replay viewer (Phase 6). Defined outside the Tapir layer because:
  *   - one route returns an HTML page sourced from the classpath
  *   - the other returns a raw JSON byte array streamed from GCS
  *
  * Both shapes are awkward to express through Tapir's typed inputs/outputs, and we want a `HttpRoutes[IO]` value anyway
  * so that [[ReplayAuth.middleware]] can wrap the whole subtree at once.
  *
  * Note: deliberately NOT importing `org.http4s.circe.CirceEntityEncoder._` — that import would re-encode our String
  * bodies as JSON-quoted strings (turning the GCS payload `{...}` into `"{...}"`). Instead, JSON responses are built by
  * serialising the circe `Json` via `.noSpaces` and setting the content-type explicitly.
  */
object ReplayHttpRoutes:

  private val UuidPattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".r

  /** @param storage
    *   real-or-fake source of the replay JSON bytes
    */
  def apply(storage: ReplayStorage): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ GET -> Root / "replay" / uuid if isUuid(uuid) =>
        serveReplayHtml(req)

      case GET -> Root / "replay" / uuid / "data" if isUuid(uuid) =>
        serveReplayJson(storage, uuid.toLowerCase)

      // Any unrecognised path under /replay is a 400 — distinguishes "you
      // typed a bad UUID" from "the report does not exist".
      case GET -> Root / "replay" / _ =>
        IO.pure(jsonResponse(Status.BadRequest, "invalid_uuid", "Expected a v4 UUID after /replay/"))

      case GET -> Root / "replay" / _ / "data" =>
        IO.pure(jsonResponse(Status.BadRequest, "invalid_uuid", "Expected a v4 UUID after /replay/"))
    }

  private def isUuid(s: String): Boolean = UuidPattern.matches(s.toLowerCase)

  /** Stream the static HTML player. The file is on the classpath inside the assembly JAR. */
  private def serveReplayHtml(req: Request[IO]): IO[Response[IO]] =
    StaticFile
      .fromResource[IO]("/static/replay.html", Some(req))
      .getOrElseF(
        IO.pure(jsonResponse(Status.InternalServerError, "missing_static", "replay.html not found on classpath"))
      )

  /** Look the report up in GCS, map the error variants onto distinct status codes so the page UI can give the user a
    * meaningful message (its `fetch` checks 401 / 404 / 5xx separately).
    */
  private def serveReplayJson(storage: ReplayStorage, uuid: String): IO[Response[IO]] =
    storage.get(uuid).map {
      case Right(bytes) =>
        Response[IO](Status.Ok)
          .withEntity(new String(bytes, StandardCharsets.UTF_8))
          .withContentType(`Content-Type`(MediaType.application.json))
      case Left(ReplayStorage.Error.NotFound) =>
        jsonResponse(Status.NotFound, "not_found", s"No replay found for report id $uuid")
      case Left(ReplayStorage.Error.NotConfigured) =>
        jsonResponse(
          Status.ServiceUnavailable,
          "storage_not_configured",
          "Set BUG_REPORTS_BUCKET env var to enable replays"
        )
      case Left(ReplayStorage.Error.ReadFailed(msg)) =>
        jsonResponse(Status.BadGateway, "storage_read_failed", msg)
    }

  private def jsonResponse(status: Status, code: String, message: String): Response[IO] =
    Response[IO](status)
      .withEntity(Json.obj("error" -> Json.fromString(code), "message" -> Json.fromString(message)).noSpaces)
      .withContentType(`Content-Type`(MediaType.application.json))
