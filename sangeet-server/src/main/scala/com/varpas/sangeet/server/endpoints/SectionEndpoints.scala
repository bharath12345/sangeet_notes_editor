package com.varpas.sangeet.server.endpoints

import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.model.StatusCode
import io.circe.Json

object SectionEndpoints:

  private val base = endpoint.post.in("api" / "v1" / "sections")

  private val errorOut: EndpointOutput[(StatusCode, Json)] =
    statusCode and jsonBody[Json]

  val add: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("add")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("addSection")
      .summary("Add a new section to the composition")

  val remove: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("remove")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("removeSection")
      .summary("Remove a section by index")

  val rename: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("rename")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("renameSection")
      .summary("Rename a section by index")

  val reorder: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("reorder")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("reorderSection")
      .summary("Move a section from one index to another")

  val all: List[AnyEndpoint] = List(add, remove, rename, reorder)
