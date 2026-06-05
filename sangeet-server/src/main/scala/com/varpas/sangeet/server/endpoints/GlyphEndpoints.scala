package com.varpas.sangeet.server.endpoints

import io.circe.Json
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

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
