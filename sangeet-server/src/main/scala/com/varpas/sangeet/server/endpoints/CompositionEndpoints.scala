package com.varpas.sangeet.server.endpoints

import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.model.StatusCode
import io.circe.Json

object CompositionEndpoints:

  private val base = endpoint.in("api" / "v1" / "compositions")

  private val errorOut: EndpointOutput[(StatusCode, Json)] =
    statusCode and jsonBody[Json]

  val create: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base.post
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("createComposition")
      .summary("Create a new composition")

  val parse: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base.post
      .in("parse")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("parseComposition")
      .summary("Parse a composition from .swar JSON")

  val serialize: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base.post
      .in("serialize")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("serializeComposition")
      .summary("Serialize a composition to .swar JSON")

  val all: List[AnyEndpoint] = List(create, parse, serialize)
