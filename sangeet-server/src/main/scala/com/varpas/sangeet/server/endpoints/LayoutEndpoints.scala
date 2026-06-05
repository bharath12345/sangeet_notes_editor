package com.varpas.sangeet.server.endpoints

import io.circe.Json
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

object LayoutEndpoints:

  private val base = endpoint.post.in("api" / "v1" / "layout")

  private val errorOut: EndpointOutput[(StatusCode, Json)] =
    statusCode and jsonBody[Json]

  val compute: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("compute")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("computeLayout")
      .summary("Compute grid layout for a composition")

  val all: List[AnyEndpoint] = List(compute)
