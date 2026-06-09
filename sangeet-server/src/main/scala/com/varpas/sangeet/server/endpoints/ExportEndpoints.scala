package com.varpas.sangeet.server.endpoints

import io.circe.Json
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

object ExportEndpoints:

  private val base = endpoint.post.in("api" / "v1" / "export")

  private val errorOut: EndpointOutput[(StatusCode, Json)] =
    statusCode and jsonBody[Json]

  val html: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("html")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("exportHtml")
      .summary("Export composition to HTML string")

  val all: List[AnyEndpoint] = List(html)
