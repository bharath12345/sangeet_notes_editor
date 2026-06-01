package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.*
import sttp.tapir.server.ServerEndpoint
import com.varpas.sangeet.core.api.StrokeApi
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.server.endpoints.StrokeEndpoints
import com.varpas.sangeet.server.routes.JsonParsing.*
import com.varpas.sangeet.server.routes.EditorResultCodec.*
import com.varpas.sangeet.server.routes.RouteHelper.*

object StrokeRoutes:

  val set: ServerEndpoint[Any, IO] =
    StrokeEndpoints.set.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        input <- parseEditorInput(c)
        stroke <- parseField[Stroke](c, "stroke")
        editorResult <- StrokeApi.setStroke(input, stroke)
      yield editorResult)(encodeEditorResult)
    }

  val clear: ServerEndpoint[Any, IO] =
    StrokeEndpoints.clear.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        input <- parseEditorInput(c)
        editorResult <- StrokeApi.clearStroke(input)
      yield editorResult)(encodeEditorResult)
    }

  val all: List[ServerEndpoint[Any, IO]] = List(set, clear)
