package com.varpas.sangeet.server.endpoints

import io.circe.Json
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

object ReferenceEndpoints:

  private val base = endpoint.in("api" / "v1")

  private val errorOut: EndpointOutput[(StatusCode, Json)] =
    statusCode and jsonBody[Json]

  val listTaals: Endpoint[Unit, Unit, (StatusCode, Json), Json, Any] =
    base.get
      .in("taals")
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("listTaals")
      .summary("List all built-in taals")

  val getTaal: Endpoint[Unit, String, (StatusCode, Json), Json, Any] =
    base.get
      .in("taals" / path[String]("name"))
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("getTaal")
      .summary("Get a taal by name")

  val listRaags: Endpoint[Unit, Unit, (StatusCode, Json), Json, Any] =
    base.get
      .in("raags")
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("listRaags")
      .summary("List all built-in raags")

  val getRaag: Endpoint[Unit, String, (StatusCode, Json), Json, Any] =
    base.get
      .in("raags" / path[String]("name"))
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("getRaag")
      .summary("Get a raag by name")

  val getColors: Endpoint[Unit, Unit, (StatusCode, Json), Json, Any] =
    base.get
      .in("rendering" / "colors")
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("getColors")
      .summary("Get notation color palette")

  val getScripts: Endpoint[Unit, Unit, (StatusCode, Json), Json, Any] =
    base.get
      .in("rendering" / "scripts")
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("getScripts")
      .summary("Get available script mappings")

  val all: List[AnyEndpoint] = List(
    listTaals,
    getTaal,
    listRaags,
    getRaag,
    getColors,
    getScripts
  )
