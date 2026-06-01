package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.*
import sttp.tapir.server.ServerEndpoint
import com.varpas.sangeet.core.api.PlaybackApi
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.audio.TimedNote
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.server.endpoints.PlaybackEndpoints
import com.varpas.sangeet.server.routes.JsonParsing.*
import com.varpas.sangeet.server.routes.RouteHelper.*

object PlaybackRoutes:

  private def encodeTimedNote(tn: TimedNote): Json =
    Json.obj(
      "timeMs" -> Json.fromLong(tn.timeMs),
      "durationMs" -> Json.fromLong(tn.durationMs),
      "note" -> tn.note.asJson,
      "variant" -> tn.variant.asJson,
      "octave" -> tn.octave.asJson
    )

  val schedule: ServerEndpoint[Any, IO] =
    PlaybackEndpoints.schedule.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        composition <- parseField[Composition](c, "composition")
        bpm <- parseField[Double](c, "bpm")
        notes <- PlaybackApi.scheduleCompositionPlayback(composition, bpm)
      yield Json.arr(notes.map(encodeTimedNote)*))(identity)
    }

  val all: List[ServerEndpoint[Any, IO]] = List(schedule)
