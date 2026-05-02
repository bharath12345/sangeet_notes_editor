package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.*
import sttp.tapir.server.ServerEndpoint
import com.varpas.sangeet.core.api.GlyphApi
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.render.DotPosition
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.server.{ApiEnvelope, ErrorMapping}
import com.varpas.sangeet.server.endpoints.GlyphEndpoints
import com.varpas.sangeet.server.routes.JsonParsing.*

object RenderingRoutes:

  val glyph: ServerEndpoint[Any, IO] =
    GlyphEndpoints.glyph.serverLogic { body =>
      val c = body.hcursor
      val result = for
        note <- parseField[Note](c, "note")
        variant <- parseField[Variant](c, "variant")
        octave <- parseField[Octave](c, "octave")
        script <- parseFieldOr[SwarScript](c, "script", SwarScript.Devanagari)
      yield
        val glyphStr = GlyphApi.noteGlyph(note, variant, script)
        val needsKomal = GlyphApi.needsKomalMark(note, variant)
        val needsTivra = GlyphApi.needsTivraMark(note, variant)
        val (dotCount, dotPos) = GlyphApi.octaveDots(octave)
        Json.obj(
          "glyph" -> Json.fromString(glyphStr),
          "needsKomalMark" -> Json.fromBoolean(needsKomal),
          "needsTivraMark" -> Json.fromBoolean(needsTivra),
          "octaveDots" -> Json.fromInt(dotCount),
          "dotPosition" -> Json.fromString(dotPos.toString.toLowerCase),
          "allScripts" -> Json.obj(
            GlyphApi.allScriptMappings(note).map { case (s, g) =>
              s.toString.toLowerCase -> Json.fromString(g)
            }.toSeq*
          )
        )

      result match
        case Right(json) =>
          IO.pure(Right(ApiEnvelope.successRaw(json)))
        case Left(err) =>
          IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val all: List[ServerEndpoint[Any, IO]] = List(glyph)
