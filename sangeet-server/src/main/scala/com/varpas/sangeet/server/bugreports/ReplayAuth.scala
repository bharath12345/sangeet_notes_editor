package com.varpas.sangeet.server.bugreports

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import io.circe.Json
import org.http4s.headers.{Authorization, `WWW-Authenticate`}
import org.http4s.implicits._
import org.http4s.{AuthScheme, Challenge, Credentials, HttpRoutes, MediaType, Request, Response, Status}

/** HTTP Basic Auth middleware for the replay viewer endpoints. Single shared password sourced from
  * `REPLAY_VIEWER_PASSWORD` env var (mounted from Secret Manager `replay-viewer-password` on Cloud Run).
  *
  * Constant-time string compare via `MessageDigest.isEqual` so the response time doesn't reveal how many leading
  * characters of the password were correct. Empty / unset env var → 503 with diagnostic JSON instead of silently
  * letting unauthenticated requests through (auth misconfiguration must be visible, not a hidden open door).
  *
  * The realm string `"Sangeet replay viewer"` is what the browser shows in its native auth prompt — keep it
  * recognisable so visitors know what they're being asked for.
  */
object ReplayAuth:

  private val Realm = "Sangeet replay viewer"

  /** Wrap a route set with Basic Auth. Reads the expected password from env at construction time; if absent, every
    * request short-circuits to 503 so misconfiguration is immediately visible.
    */
  def middleware(routes: HttpRoutes[IO]): HttpRoutes[IO] =
    middleware(routes, sys.env.get("REPLAY_VIEWER_PASSWORD").filter(_.nonEmpty))

  /** Explicit-password overload for tests. */
  def middleware(routes: HttpRoutes[IO], expectedPassword: Option[String]): HttpRoutes[IO] =
    Kleisli { (req: Request[IO]) =>
      // CRITICAL: short-circuit on path before doing anything else, otherwise
      // this middleware swallows every request in the app (each one returning
      // 401/503), not just the replay viewer routes. The Kleisli must return
      // OptionT.none for non-/replay paths so that `<+>` lets the next route
      // set (e.g. the Tapir API routes) handle them.
      if !isReplayPath(req) then OptionT.none[IO, Response[IO]]
      else
        expectedPassword match
          case None =>
            OptionT.pure[IO](unconfiguredResponse)
          case Some(password) =>
            providedPassword(req) match
              case Some(provided) if constantTimeEquals(provided, password) =>
                routes(req)
              case _ =>
                OptionT.pure[IO](unauthorizedResponse)
    }

  private def isReplayPath(req: Request[IO]): Boolean =
    req.uri.path.segments.headOption.exists(_.encoded == "replay")

  private def providedPassword(req: Request[IO]): Option[String] =
    req.headers
      .get[Authorization]
      .collect {
        case Authorization(Credentials.Token(scheme, token)) if scheme == AuthScheme.Basic =>
          token
      }
      .flatMap(decodeBasic)
      .map(_._2)

  private def decodeBasic(token: String): Option[(String, String)] =
    try
      val decoded = new String(Base64.getDecoder.decode(token), StandardCharsets.UTF_8)
      val idx     = decoded.indexOf(':')
      if idx < 0 then None
      else Some((decoded.substring(0, idx), decoded.substring(idx + 1)))
    catch case _: IllegalArgumentException => None

  private def constantTimeEquals(a: String, b: String): Boolean =
    MessageDigest.isEqual(
      a.getBytes(StandardCharsets.UTF_8),
      b.getBytes(StandardCharsets.UTF_8)
    )

  private val unauthorizedResponse: Response[IO] =
    Response[IO](Status.Unauthorized)
      .putHeaders(`WWW-Authenticate`(Challenge("Basic", Realm)))
      .withEntity(
        Json
          .obj("error" -> Json.fromString("authentication_required"))
          .noSpaces
      )
      .withContentType(org.http4s.headers.`Content-Type`(MediaType.application.json))

  private val unconfiguredResponse: Response[IO] =
    Response[IO](Status.ServiceUnavailable)
      .withEntity(
        Json
          .obj(
            "error"   -> Json.fromString("replay_viewer_disabled"),
            "message" -> Json.fromString("Set REPLAY_VIEWER_PASSWORD env var to enable.")
          )
          .noSpaces
      )
      .withContentType(org.http4s.headers.`Content-Type`(MediaType.application.json))
