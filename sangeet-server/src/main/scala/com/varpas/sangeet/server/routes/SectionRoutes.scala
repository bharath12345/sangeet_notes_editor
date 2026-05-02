package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.*
import sttp.tapir.server.ServerEndpoint
import com.varpas.sangeet.core.api.SectionApi
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.server.{ApiEnvelope, ErrorMapping}
import com.varpas.sangeet.server.endpoints.SectionEndpoints
import com.varpas.sangeet.server.routes.JsonParsing.*

object SectionRoutes:

  val add: ServerEndpoint[Any, IO] =
    SectionEndpoints.add.serverLogic { body =>
      val c = body.hcursor
      val result = for
        composition <- parseField[Composition](c, "composition")
        name <- parseField[String](c, "name")
        sectionType <- parseField[SectionType](c, "sectionType")
        comp <- SectionApi.addSection(composition, name, sectionType)
      yield comp

      result match
        case Right(comp) =>
          IO.pure(Right(ApiEnvelope.successRaw(comp.asJson)))
        case Left(err) =>
          IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val remove: ServerEndpoint[Any, IO] =
    SectionEndpoints.remove.serverLogic { body =>
      val c = body.hcursor
      val result = for
        composition <- parseField[Composition](c, "composition")
        currentSectionIndex <- parseFieldOr(c, "currentSectionIndex", 0)
        indexToRemove <- parseField[Int](c, "indexToRemove")
        pair <- SectionApi.removeSection(composition, currentSectionIndex, indexToRemove)
      yield pair

      result match
        case Right((comp, idx)) =>
          IO.pure(Right(ApiEnvelope.successRaw(Json.obj(
            "composition" -> comp.asJson,
            "currentSectionIndex" -> Json.fromInt(idx)
          ))))
        case Left(err) =>
          IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val rename: ServerEndpoint[Any, IO] =
    SectionEndpoints.rename.serverLogic { body =>
      val c = body.hcursor
      val result = for
        composition <- parseField[Composition](c, "composition")
        index <- parseField[Int](c, "index")
        newName <- parseField[String](c, "newName")
        comp <- SectionApi.renameSection(composition, index, newName)
      yield comp

      result match
        case Right(comp) =>
          IO.pure(Right(ApiEnvelope.successRaw(comp.asJson)))
        case Left(err) =>
          IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val reorder: ServerEndpoint[Any, IO] =
    SectionEndpoints.reorder.serverLogic { body =>
      val c = body.hcursor
      val result = for
        composition <- parseField[Composition](c, "composition")
        currentSectionIndex <- parseFieldOr(c, "currentSectionIndex", 0)
        from <- parseField[Int](c, "from")
        to <- parseField[Int](c, "to")
        pair <- SectionApi.moveSection(composition, currentSectionIndex, from, to)
      yield pair

      result match
        case Right((comp, idx)) =>
          IO.pure(Right(ApiEnvelope.successRaw(Json.obj(
            "composition" -> comp.asJson,
            "currentSectionIndex" -> Json.fromInt(idx)
          ))))
        case Left(err) =>
          IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val all: List[ServerEndpoint[Any, IO]] = List(add, remove, rename, reorder)
