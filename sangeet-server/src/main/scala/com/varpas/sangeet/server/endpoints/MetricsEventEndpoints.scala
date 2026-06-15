package com.varpas.sangeet.server.endpoints

import io.circe.Json
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

import com.varpas.sangeet.core.api.MetricsEvent

object MetricsEventEndpoints:

  private val base = endpoint.in("api" / "v1")

  /** 400 body — { error: code, message: human-readable }. Returned when the request hits the cardinality whitelist (see
    * `AppMetrics.AllowedCounters`); also for malformed JSON since Tapir's decoder maps to BadRequest by default.
    */
  private val errorOut: EndpointOutput[(StatusCode, Json)] =
    statusCode and jsonBody[Json]

  /** `POST /api/v1/metrics/event` — accepts `{ counter, labels }`, validates against the server-side whitelist, and
    * increments the matching Micrometer counter. Returns 204 No Content on success, 400 on rejection.
    *
    * Designed for fire-and-forget client emission (web Elm cmd, desktop daemon thread); the body deliberately has no
    * timestamp / session id so clients don't have to maintain any state.
    */
  val recordEvent: Endpoint[Unit, MetricsEvent, (StatusCode, Json), Unit, Any] =
    base.post
      .in("metrics" / "event")
      .in(jsonBody[MetricsEvent])
      .errorOut(errorOut)
      .out(statusCode(StatusCode.NoContent))
      .name("recordMetricEvent")
      .summary("Increment a server-side application counter")
      .description(
        "Increments the named Micrometer counter (with the given label values) in the same registry that backs the " +
          "/metrics scrape endpoint and Cloud Monitoring push. Counter names and label values are validated against a " +
          "whitelist (see AppMetrics) — arbitrary names are rejected with 400 to keep time-series cardinality bounded."
      )

  val all: List[AnyEndpoint] = List(recordEvent)
