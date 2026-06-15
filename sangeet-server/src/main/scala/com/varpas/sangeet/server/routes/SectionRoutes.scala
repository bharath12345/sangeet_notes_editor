package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax._
import sttp.tapir.server.ServerEndpoint

import com.varpas.sangeet.core.api.SectionApi
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.server.endpoints.SectionEndpoints
import com.varpas.sangeet.server.routes.JsonParsing._
import com.varpas.sangeet.server.routes.RouteHelper._

object SectionRoutes:

  val add: ServerEndpoint[Any, IO] =
    SectionEndpoints.add.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        composition <- parseField[Composition](c, "composition")
        name        <- parseField[String](c, "name")
        sectionType <- parseField[SectionType](c, "sectionType")
        comp        <- SectionApi.addSection(composition, name, sectionType)
      yield comp)(_.asJson)
    }

  val remove: ServerEndpoint[Any, IO] =
    SectionEndpoints.remove.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        composition         <- parseField[Composition](c, "composition")
        currentSectionIndex <- parseFieldOr(c, "currentSectionIndex", 0)
        indexToRemove       <- parseField[Int](c, "indexToRemove")
        pair                <- SectionApi.removeSection(composition, currentSectionIndex, indexToRemove)
      yield pair) { case (comp, idx) =>
        Json.obj(
          "composition"         -> comp.asJson,
          "currentSectionIndex" -> Json.fromInt(idx)
        )
      }
    }

  val clear: ServerEndpoint[Any, IO] =
    SectionEndpoints.clear.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        composition <- parseField[Composition](c, "composition")
        index       <- parseField[Int](c, "index")
        comp        <- SectionApi.clearSection(composition, index)
      yield comp)(_.asJson)
    }

  val reorder: ServerEndpoint[Any, IO] =
    SectionEndpoints.reorder.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        composition         <- parseField[Composition](c, "composition")
        currentSectionIndex <- parseFieldOr(c, "currentSectionIndex", 0)
        from                <- parseField[Int](c, "from")
        to                  <- parseField[Int](c, "to")
        pair                <- SectionApi.moveSection(composition, currentSectionIndex, from, to)
      yield pair) { case (comp, idx) =>
        Json.obj(
          "composition"         -> comp.asJson,
          "currentSectionIndex" -> Json.fromInt(idx)
        )
      }
    }

  val all: List[ServerEndpoint[Any, IO]] = List(add, remove, clear, reorder)
