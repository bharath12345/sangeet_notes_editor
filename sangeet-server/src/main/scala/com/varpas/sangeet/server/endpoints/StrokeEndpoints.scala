package com.varpas.sangeet.server.endpoints

import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.model.StatusCode
import io.circe.Json

object StrokeEndpoints:

  private val base = endpoint.post.in("api" / "v1" / "editor" / "stroke")

  private val errorOut: EndpointOutput[(StatusCode, Json)] =
    statusCode and jsonBody[Json]

  val set: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("set")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("setStroke")
      .summary("Set a stroke on the swar at cursor position")

  val clear: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("clear")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("clearStroke")
      .summary("Clear the stroke at cursor position")

  val all: List[AnyEndpoint] = List(set, clear)
