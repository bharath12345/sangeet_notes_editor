package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.*
import sttp.tapir.server.ServerEndpoint
import com.varpas.sangeet.core.api.{ApiError, CompositionApi}
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.server.endpoints.CompositionEndpoints
import com.varpas.sangeet.server.routes.JsonParsing.*
import com.varpas.sangeet.server.routes.RouteHelper.*

object CompositionRoutes:

  val create: ServerEndpoint[Any, IO] =
    CompositionEndpoints.create.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        title <- parseField[String](c, "title")
        compositionType <- parseField[CompositionType](c, "compositionType")
        taal <- parseField[Taal](c, "taal")
        raag <- parseField[Raag](c, "raag")
        laya <- Right(c.downField("laya").as[Laya].toOption)
        taanCount <- parseFieldOr(c, "taanCount", 0)
        showStrokeLine <- parseFieldOr(c, "showStrokeLine", false)
        showSahityaLine <- parseFieldOr(c, "showSahityaLine", false)
      yield CompositionApi.createComposition(
        title, compositionType, taal, raag, laya, taanCount, showStrokeLine, showSahityaLine
      ))(_.asJson)
    }

  val parse: ServerEndpoint[Any, IO] =
    CompositionEndpoints.parse.serverLogic { body =>
      val c = body.hcursor
      val jsonStr = c.downField("json").as[String]
        .getOrElse(body.noSpaces)
      handleResult(CompositionApi.parseComposition(jsonStr))(_.asJson)
    }

  val serialize: ServerEndpoint[Any, IO] =
    CompositionEndpoints.serialize.serverLogic { body =>
      handleResult(parseField[Composition](body.hcursor, "composition")) { comp =>
        CompositionApi.serializeComposition(comp)
      }
    }

  val all: List[ServerEndpoint[Any, IO]] = List(create, parse, serialize)
