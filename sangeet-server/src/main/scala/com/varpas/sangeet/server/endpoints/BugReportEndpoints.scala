package com.varpas.sangeet.server.endpoints

import io.circe.Json
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

object BugReportEndpoints:

  private val base = endpoint.in("api" / "v1")

  private val errorOut: EndpointOutput[(StatusCode, Json)] =
    statusCode and jsonBody[Json]

  /** Accept any JSON body for the MVP — schema is intentionally open while the web (rrweb shape) and desktop (action
    * log + screenshot shape) clients are still being designed. We'll lock down the schema once both senders exist and
    * we know what we actually need to enforce.
    */
  val createBugReport: Endpoint[Unit, Json, (StatusCode, Json), Json, Any] =
    base.post
      .in("bug-reports")
      .in(jsonBody[Json])
      .errorOut(errorOut)
      .out(jsonBody[Json])
      .name("createBugReport")
      .summary("Submit a bug report")
      .description(
        "Accepts an arbitrary JSON payload (web rrweb capture or desktop action log + screenshot)" +
          " and stores it in GCS under a generated UUID. Returns the reportId for later lookup."
      )

  val all: List[AnyEndpoint] = List(createBugReport)
