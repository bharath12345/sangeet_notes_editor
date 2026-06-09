package com.varpas.sangeet.server.endpoints

import io.circe.Json
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

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

  val insertChikari: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("insert-chikari")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("insertChikari")
      .summary("Insert a chikari at cursor position")

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

  val insertSwarGroup: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("insert-swar-group")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("insertSwarGroup")
      .summary("Insert 2-4 notes on a single beat with equal subdivisions")

  val deleteAtCursor: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("delete-at-cursor")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("deleteAtCursor")
      .summary("Delete events at cursor position (BACKSPACE semantics)")

  val all: List[AnyEndpoint] = List(
    insertSwar,
    insertChikari,
    insertRest,
    insertSustain,
    deleteLast,
    insertDualSwar,
    insertSwarGroup,
    deleteAtCursor
  )
