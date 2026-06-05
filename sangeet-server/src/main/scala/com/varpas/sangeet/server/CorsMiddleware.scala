package com.varpas.sangeet.server

import cats.effect.IO
import org.http4s._
import org.typelevel.ci.CIString

object CorsMiddleware:

  private val corsHeaders: Seq[Header.ToRaw] = Seq(
    Header.Raw(CIString("Access-Control-Allow-Origin"), "*"),
    Header.Raw(CIString("Access-Control-Allow-Methods"), "GET, POST, PUT, DELETE, OPTIONS"),
    Header.Raw(CIString("Access-Control-Allow-Headers"), "Content-Type, Authorization"),
    Header.Raw(CIString("Access-Control-Max-Age"), "86400")
  )

  def apply(routes: HttpRoutes[IO]): HttpRoutes[IO] =
    import cats.data.Kleisli
    import cats.data.OptionT

    Kleisli { request =>
      if request.method == Method.OPTIONS then
        OptionT.pure[IO](
          Response[IO](Status.NoContent).putHeaders(corsHeaders*)
        )
      else
        routes.run(request).map { response =>
          response.putHeaders(corsHeaders*)
        }
    }
