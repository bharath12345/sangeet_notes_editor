package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import sttp.tapir.server.ServerEndpoint

import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.core.format.HtmlExport
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.server.endpoints.ExportEndpoints
import com.varpas.sangeet.server.routes.JsonParsing._
import com.varpas.sangeet.server.{ApiEnvelope, ErrorMapping}

object ExportRoutes:

  val html: ServerEndpoint[Any, IO] =
    ExportEndpoints.html.serverLogic { body =>
      val c = body.hcursor
      val result = for
        composition <- parseField[Composition](c, "composition")
        script      <- parseFieldOr[SwarScript](c, "script", SwarScript.Devanagari)
      yield (composition, script)

      result match
        case Right((composition, script)) =>
          IO.blocking {
            val htmlString = HtmlExport.render(composition, script)
            Right(ApiEnvelope.successRaw(Json.fromString(htmlString)))
          }
        case Left(err) =>
          IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val all: List[ServerEndpoint[Any, IO]] = List(html)
