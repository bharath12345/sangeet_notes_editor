package com.varpas.sangeet.server.endpoints

import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.model.StatusCode
import io.circe.Json

object EditorEndpoints:

  private val base = endpoint.post.in("api" / "v1" / "editor")

  private val errorOut: EndpointOutput[(StatusCode, Json)] =
    statusCode and jsonBody[Json]

  val insertSwar: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("insert-swar")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("insertSwar")
      .summary("Insert a swar note at cursor position")

  val insertRest: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("insert-rest")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("insertRest")
      .summary("Insert a rest at cursor position")

  val insertSustain: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("insert-sustain")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("insertSustain")
      .summary("Insert a sustain at cursor position")

  val deleteLast: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("delete-last")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("deleteLastEvent")
      .summary("Delete the last event in the current section")

  val insertDualSwar: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("insert-dual-swar")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("insertDualSwar")
      .summary("Insert dual swar (two identical notes)")

  val all: List[AnyEndpoint] = List(
    insertSwar, insertRest, insertSustain, deleteLast, insertDualSwar
  )
