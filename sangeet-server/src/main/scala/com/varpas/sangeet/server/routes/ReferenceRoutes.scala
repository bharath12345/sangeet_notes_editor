package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.*
import sttp.tapir.server.ServerEndpoint
import com.varpas.sangeet.core.api.ReferenceApi
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.server.ApiEnvelope
import com.varpas.sangeet.server.endpoints.ReferenceEndpoints
import com.varpas.sangeet.server.routes.RouteHelper.*

object ReferenceRoutes:

  val listTaals: ServerEndpoint[Any, IO] =
    ReferenceEndpoints.listTaals.serverLogic { _ =>
      val taals = ReferenceApi.allTaals
      val json = ApiEnvelope.successRaw(Json.obj(
        taals.map { case (name, taal) => name -> taal.asJson }.toSeq*
      ))
      IO.pure(Right(json))
    }

  val getTaal: ServerEndpoint[Any, IO] =
    ReferenceEndpoints.getTaal.serverLogic { name =>
      handleResult(ReferenceApi.taalByName(name))(_.asJson)
    }

  val listRaags: ServerEndpoint[Any, IO] =
    ReferenceEndpoints.listRaags.serverLogic { _ =>
      val raags = ReferenceApi.allRaags
      val json = ApiEnvelope.successRaw(Json.obj(
        raags.map { case (name, raag) => name -> raag.asJson }.toSeq*
      ))
      IO.pure(Right(json))
    }

  val getRaag: ServerEndpoint[Any, IO] =
    ReferenceEndpoints.getRaag.serverLogic { name =>
      handleResult(ReferenceApi.raagByName(name))(_.asJson)
    }

  val getColors: ServerEndpoint[Any, IO] =
    ReferenceEndpoints.getColors.serverLogic { _ =>
      import com.varpas.sangeet.core.render.NotationColors
      val json = ApiEnvelope.successRaw(Json.obj(
        "taalMarker"    -> Json.fromString(NotationColors.taalMarker),
        "taalMarkerSam" -> Json.fromString(NotationColors.taalMarkerSam),
        "swar"          -> Json.fromString(NotationColors.swar),
        "octaveDot"     -> Json.fromString(NotationColors.octaveDot),
        "ornament"      -> Json.fromString(NotationColors.ornament),
        "stroke"        -> Json.fromString(NotationColors.stroke),
        "sahitya"       -> Json.fromString(NotationColors.sahitya),
        "rest"          -> Json.fromString(NotationColors.rest),
        "sustain"       -> Json.fromString(NotationColors.sustain),
        "komalMark"     -> Json.fromString(NotationColors.komalMark),
        "tivraMark"     -> Json.fromString(NotationColors.tivraMark)
      ))
      IO.pure(Right(json))
    }

  val getScripts: ServerEndpoint[Any, IO] =
    ReferenceEndpoints.getScripts.serverLogic { _ =>
      import com.varpas.sangeet.core.model.{Note, SwarScript}
      import com.varpas.sangeet.core.render.ScriptMap
      val scripts = SwarScript.values.toList.map { script =>
        script.toString.toLowerCase -> Json.obj(
          "displayName" -> Json.fromString(ScriptMap.displayName(script)),
          "fontName" -> Json.fromString(ScriptMap.fontName(script)),
          "notes" -> Json.obj(
            Note.values.toList.map(n => n.toString.toLowerCase -> Json.fromString(ScriptMap.glyph(n, script)))*
          )
        )
      }
      val json = ApiEnvelope.successRaw(Json.obj(scripts*))
      IO.pure(Right(json))
    }

  val all: List[ServerEndpoint[Any, IO]] = List(
    listTaals, getTaal, listRaags, getRaag, getColors, getScripts
  )
