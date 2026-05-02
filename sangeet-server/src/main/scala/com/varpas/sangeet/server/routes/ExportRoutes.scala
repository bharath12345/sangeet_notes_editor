package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.*
import sttp.tapir.server.ServerEndpoint
import com.varpas.sangeet.core.api.{ApiError, ExportApi}
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.{Codecs, HtmlExport}
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.server.{ApiEnvelope, ErrorMapping}
import com.varpas.sangeet.server.endpoints.ExportEndpoints
import com.varpas.sangeet.server.routes.JsonParsing.*
import java.nio.file.{Files, Path}

object ExportRoutes:

  val pdf: ServerEndpoint[Any, IO] =
    ExportEndpoints.pdf.serverLogic { body =>
      val c = body.hcursor
      val result = for
        composition <- parseField[Composition](c, "composition")
        script <- parseFieldOr[SwarScript](c, "script", SwarScript.Devanagari)
        landscape <- parseFieldOr(c, "landscape", false)
      yield (composition, script, landscape)

      result match
        case Right((composition, script, landscape)) =>
          IO.blocking {
            val tempFile = Files.createTempFile("sangeet-export-", ".pdf")
            try
              ExportApi.exportPdf(composition, tempFile, script, landscape) match
                case Right(_) =>
                  val bytes = Files.readAllBytes(tempFile)
                  Right(bytes)
                case Left(err) =>
                  Left(ErrorMapping.toResponse(err))
            finally
              Files.deleteIfExists(tempFile)
          }
        case Left(err) =>
          IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val html: ServerEndpoint[Any, IO] =
    ExportEndpoints.html.serverLogic { body =>
      val c = body.hcursor
      val result = for
        composition <- parseField[Composition](c, "composition")
        script <- parseFieldOr[SwarScript](c, "script", SwarScript.Devanagari)
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

  val all: List[ServerEndpoint[Any, IO]] = List(pdf, html)
