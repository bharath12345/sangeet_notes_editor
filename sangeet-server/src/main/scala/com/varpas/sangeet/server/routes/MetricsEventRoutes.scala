package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

import com.varpas.sangeet.server.endpoints.MetricsEventEndpoints
import com.varpas.sangeet.server.metrics.AppMetrics

object MetricsEventRoutes:

  /** Wires the validation result onto an HTTP response. Bad input → 400 with a diagnostic envelope; success → 204 No
    * Content. We don't return a body on success so the client can fire-and-forget without parsing — every byte saved
    * matters because mutation rates can be high (a fast typist emits dozens per second).
    */
  val recordEvent: ServerEndpoint[Any, IO] =
    MetricsEventEndpoints.recordEvent.serverLogic { event =>
      IO.delay(AppMetrics.increment(event.counter, event.labels)).map {
        case Right(_) => Right(())
        case Left(err) =>
          Left(
            (
              StatusCode.BadRequest,
              Json.obj(
                "error"   -> Json.fromString("invalid_metric_event"),
                "message" -> Json.fromString(AppMetrics.ValidationError.message(err))
              )
            )
          )
      }
    }

  val all: List[ServerEndpoint[Any, IO]] = List(recordEvent)
