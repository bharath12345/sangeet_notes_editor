package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.*
import sttp.tapir.server.ServerEndpoint
import com.varpas.sangeet.core.api.StrokeApi
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.server.{ApiEnvelope, ErrorMapping}
import com.varpas.sangeet.server.endpoints.StrokeEndpoints
import com.varpas.sangeet.server.routes.JsonParsing.*
import com.varpas.sangeet.server.routes.EditorResultCodec.*

object StrokeRoutes:

  val set: ServerEndpoint[Any, IO] =
    StrokeEndpoints.set.serverLogic { body =>
      val c = body.hcursor
      val result = for
        input <- parseEditorInput(c)
        stroke <- parseField[Stroke](c, "stroke")
        editorResult <- StrokeApi.setStroke(input, stroke)
      yield editorResult

      result match
        case Right(r) => IO.pure(Right(ApiEnvelope.successRaw(encodeEditorResult(r))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val clear: ServerEndpoint[Any, IO] =
    StrokeEndpoints.clear.serverLogic { body =>
      val c = body.hcursor
      val result = for
        input <- parseEditorInput(c)
        editorResult <- StrokeApi.clearStroke(input)
      yield editorResult

      result match
        case Right(r) => IO.pure(Right(ApiEnvelope.successRaw(encodeEditorResult(r))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val all: List[ServerEndpoint[Any, IO]] = List(set, clear)
