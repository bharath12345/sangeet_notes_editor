package com.varpas.sangeet.server.endpoints

import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.model.StatusCode
import io.circe.Json

object GlyphEndpoints:

  private val base = endpoint.post.in("api" / "v1" / "rendering")

  private val errorOut: EndpointOutput[(StatusCode, Json)] =
    statusCode and jsonBody[Json]

  val glyph: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("glyph")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("getGlyph")
      .summary("Get glyph rendering info for a note")

  val all: List[AnyEndpoint] = List(glyph)
