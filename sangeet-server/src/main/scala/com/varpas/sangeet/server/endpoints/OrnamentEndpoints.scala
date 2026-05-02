package com.varpas.sangeet.server.endpoints

import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.model.StatusCode
import io.circe.Json

object OrnamentEndpoints:

  private val base = endpoint.post.in("api" / "v1" / "editor" / "ornament")

  private val errorOut: EndpointOutput[(StatusCode, Json)] =
    statusCode and jsonBody[Json]

  val simple: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("simple")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("addSimpleOrnament")
      .summary("Add a simple ornament (gamak, andolan, gitkari)")

  val singleNote: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("single-note")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("addSingleNoteOrnament")
      .summary("Add a single-note ornament (kanSwar, sparsh, ghaseet)")

  val meend: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("meend")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("addMeend")
      .summary("Add a meend ornament")

  val krintan: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("krintan")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("addKrintan")
      .summary("Add a krintan ornament")

  val murki: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("murki")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("addMurki")
      .summary("Add a murki ornament")

  val zamzama: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("zamzama")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("addZamzama")
      .summary("Add a zamzama ornament")

  val all: List[AnyEndpoint] = List(
    simple, singleNote, meend, krintan, murki, zamzama
  )
