package com.varpas.sangeet.server.metrics

import java.util.concurrent.atomic.AtomicLong

import cats.effect.IO
import io.micrometer.core.instrument.{Tags, Timer}
import sttp.tapir.server.interceptor.metrics.MetricsRequestInterceptor
import sttp.tapir.server.metrics.{EndpointMetric, Metric}

/** HTTP request metrics for the Tapir server, backed by the shared Micrometer composite registry.
  *
  * No off-the-shelf Tapir module wires Micrometer (Tapir ships Prometheus/OpenTelemetry/Datadog/zio variants but not
  * Micrometer), so we implement Tapir's `Metric` SPI directly here. The benefit over `tapir-prometheus-metrics`: meters
  * land in [[MetricsRegistry.registry]] alongside the JVM bindings, so they reach both the Prometheus scrape endpoint
  * *and* Cloud Monitoring push without needing to bridge two separate registries.
  *
  * Three meters per request:
  *   - `tapir.request.active` — single global gauge of in-flight requests (no labels — keeps cardinality flat;
  *     per-route active count is rarely useful enough to justify the explosion)
  *   - `tapir.request.total` — counter labeled `method`/`path`/`status_code`
  *   - `tapir.request.duration` — timer labeled the same way
  *
  * `path` is Tapir's route *template* (e.g. `/api/v1/raags/{name}`), not the literal URL —
  * `AnyEndpoint.showPathTemplate()` extracts it for free from the endpoint declaration.
  */
object HttpMetrics:

  private val registry = MetricsRegistry.registry

  private val activeRequests: AtomicLong =
    registry.gauge("tapir.request.active", Tags.empty, new AtomicLong(0L))

  private val requestActive: Metric[IO, AtomicLong] =
    Metric[IO, AtomicLong](
      activeRequests,
      onRequest = (_, counter, _) =>
        IO.pure(
          EndpointMetric[IO]()
            .onEndpointRequest(_ => IO.delay { counter.incrementAndGet(); () })
            .onResponseHeaders((_, _) => IO.delay { counter.decrementAndGet(); () })
            .onException((_, _) => IO.delay { counter.decrementAndGet(); () })
        )
    )

  private val requestTotal: Metric[IO, Unit] =
    Metric[IO, Unit](
      (),
      onRequest = (req, _, _) =>
        IO.pure(
          EndpointMetric[IO]()
            .onResponseBody { (ep, res) =>
              IO.delay {
                registry
                  .counter(
                    "tapir.request.total",
                    Tags.of(
                      "method",
                      req.method.method,
                      "path",
                      ep.showPathTemplate(),
                      "status_code",
                      res.code.code.toString
                    )
                  )
                  .increment()
              }
            }
        )
    )

  private val requestDuration: Metric[IO, Unit] =
    Metric[IO, Unit](
      (),
      onRequest = (req, _, _) =>
        IO.delay(Timer.start(registry)).flatMap { sample =>
          IO.pure(
            EndpointMetric[IO]()
              .onResponseBody { (ep, res) =>
                IO.delay {
                  val timer = registry.timer(
                    "tapir.request.duration",
                    Tags.of(
                      "method",
                      req.method.method,
                      "path",
                      ep.showPathTemplate(),
                      "status_code",
                      res.code.code.toString
                    )
                  )
                  sample.stop(timer)
                  ()
                }
              }
          )
        }
    )

  val requestInterceptor: MetricsRequestInterceptor[IO] =
    new MetricsRequestInterceptor[IO](
      List(requestActive, requestTotal, requestDuration),
      Seq.empty
    )
