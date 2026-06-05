package com.varpas.sangeet.server.endpoints

import io.circe.Json
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

object CursorEndpoints:

  private val base = endpoint.post.in("api" / "v1" / "cursor")

  private val errorOut: EndpointOutput[(StatusCode, Json)] =
    statusCode and jsonBody[Json]

  val nextBeat: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("next-beat")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("cursorNextBeat")
      .summary("Move cursor to next beat")

  val prevBeat: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("prev-beat")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("cursorPrevBeat")
      .summary("Move cursor to previous beat")

  val nextSubBeat: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("next-sub-beat")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("cursorNextSubBeat")
      .summary("Move cursor to next sub-beat")

  val setSubdivisions: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("set-subdivisions")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("cursorSetSubdivisions")
      .summary("Set beat subdivisions")

  val setOctave: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("set-octave")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("cursorSetOctave")
      .summary("Set current octave for input")

  val moveTo: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base
      .in("move-to")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("cursorMoveTo")
      .summary("Move cursor to specific cycle and beat")

  val all: List[AnyEndpoint] = List(
    nextBeat,
    prevBeat,
    nextSubBeat,
    setSubdivisions,
    setOctave,
    moveTo
  )
