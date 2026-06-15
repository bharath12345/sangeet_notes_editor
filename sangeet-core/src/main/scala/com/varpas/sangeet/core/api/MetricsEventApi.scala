package com.varpas.sangeet.core.api

import io.circe._
import io.circe.generic.semiauto._

/** Wire format for client-emitted application metric increments.
  *
  * Used by both the web (Elm `Api.Metrics.incrementCounter`) and the desktop (`DesktopMetrics`) to record mutation /
  * file-op / section-switch / clipboard / ornament events server-side via `POST /api/v1/metrics/event`. The server
  * validates `counter` + `labels` against a whitelist (see
  * `sangeet-server/src/main/scala/com/varpas/sangeet/server/metrics/AppMetrics.scala`) so clients cannot create
  * arbitrary metrics or blow the time-series cardinality budget.
  *
  * The shape is deliberately flat — one counter name + a string→string map of labels — so we never need a schema change
  * to add a new counter or label value (just an allowlist update in `AppMetrics`).
  */
final case class MetricsEvent(counter: String, labels: Map[String, String])

object MetricsEvent:
  given Codec[MetricsEvent] = deriveCodec
