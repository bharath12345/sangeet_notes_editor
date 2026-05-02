package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.*
import sttp.tapir.server.ServerEndpoint
import com.varpas.sangeet.core.api.{ApiError, CompositionApi}
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.server.{ApiEnvelope, ErrorMapping}
import com.varpas.sangeet.server.endpoints.CompositionEndpoints
import com.varpas.sangeet.server.routes.JsonParsing.*

object CompositionRoutes:

  val create: ServerEndpoint[Any, IO] =
    CompositionEndpoints.create.serverLogic { body =>
      val c = body.hcursor
      val result = for
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
      )

      result match
        case Right(comp) =>
          IO.pure(Right(ApiEnvelope.successRaw(comp.asJson)))
        case Left(err) =>
          IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val parse: ServerEndpoint[Any, IO] =
    CompositionEndpoints.parse.serverLogic { body =>
      val c = body.hcursor
      val jsonStr = c.downField("json").as[String]
        .getOrElse(body.noSpaces)

      CompositionApi.parseComposition(jsonStr) match
        case Right(comp) =>
          IO.pure(Right(ApiEnvelope.successRaw(comp.asJson)))
        case Left(err) =>
          IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val serialize: ServerEndpoint[Any, IO] =
    CompositionEndpoints.serialize.serverLogic { body =>
      val c = body.hcursor
      parseField[Composition](c, "composition") match
        case Right(comp) =>
          val json = CompositionApi.serializeComposition(comp)
          IO.pure(Right(ApiEnvelope.successRaw(json)))
        case Left(err) =>
          IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val all: List[ServerEndpoint[Any, IO]] = List(create, parse, serialize)
