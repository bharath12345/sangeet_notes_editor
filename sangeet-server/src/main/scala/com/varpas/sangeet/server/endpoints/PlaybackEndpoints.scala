package com.varpas.sangeet.server.endpoints

import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.model.StatusCode
import io.circe.Json

object PlaybackEndpoints:

  private val base = endpoint.post.in("api" / "v1" / "playback")

  private val errorOut: EndpointOutput[(StatusCode, Json)] =
    statusCode and jsonBody[Json]

  val schedule: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("schedule")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("schedulePlayback")
      .summary("Schedule playback events for a composition")

  val all: List[AnyEndpoint] = List(schedule)
