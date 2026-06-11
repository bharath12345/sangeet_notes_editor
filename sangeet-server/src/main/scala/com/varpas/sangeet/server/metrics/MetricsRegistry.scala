package com.varpas.sangeet.server.metrics

import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.jvm.{ClassLoaderMetrics, JvmGcMetrics, JvmMemoryMetrics, JvmThreadMetrics}
import io.micrometer.core.instrument.binder.system.{FileDescriptorMetrics, ProcessorMetrics, UptimeMetrics}
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.core.instrument.{Clock, MeterRegistry}
import io.micrometer.prometheusmetrics.{PrometheusConfig, PrometheusMeterRegistry}
import io.micrometer.registry.otlp.{OtlpConfig, OtlpMeterRegistry}

/** Holds the meter registries used across the server.
  *
  *   - `prometheus` always exists and is scraped via `GET /metrics`. Useful for local debugging without depending on
  *     any external service.
  *   - `otlp` is created only when the env vars `OTLP_ENDPOINT` and `OTLP_AUTH` are present. In production it pushes to
  *     Grafana Cloud every 30s; locally it's `None` and we skip OTLP entirely.
  *   - `composite` is what application code instruments against; metrics published through it land in both backends
  *     simultaneously.
  *
  * JVM bindings (heap, GC, threads, classes, CPU, file descriptors, uptime) are attached on construction so we get
  * useful baseline observability without any code-side instrumentation.
  */
object MetricsRegistry:

  val prometheus: PrometheusMeterRegistry =
    new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

  val otlp: Option[OtlpMeterRegistry] =
    (sys.env.get("OTLP_ENDPOINT"), sys.env.get("OTLP_AUTH")) match
      case (Some(endpoint), Some(auth)) =>
        val cfg = new OtlpConfig:
          override def url: String              = endpoint
          override def step: java.time.Duration = java.time.Duration.ofSeconds(30)
          override def resourceAttributes: java.util.Map[String, String] =
            java.util.Map.of("service.name", "sangeet-server", "service.version", "0.2.0")
          override def headers: java.util.Map[String, String] =
            java.util.Map.of("Authorization", auth)
          override def get(key: String): String = null
        Some(new OtlpMeterRegistry(cfg, Clock.SYSTEM))
      case _ =>
        None

  val composite: CompositeMeterRegistry =
    val c = new CompositeMeterRegistry()
    c.add(prometheus)
    otlp.foreach(c.add)
    c

  private val jvmBindings: Seq[MeterBinder] = Seq(
    new ClassLoaderMetrics(),
    new JvmMemoryMetrics(),
    new JvmGcMetrics(),
    new JvmThreadMetrics(),
    new ProcessorMetrics(),
    new UptimeMetrics(),
    new FileDescriptorMetrics()
  )

  jvmBindings.foreach(_.bindTo(composite))

  /** Returns the Prometheus text exposition format snapshot. Called by the `/metrics` endpoint. */
  def scrape(): String = prometheus.scrape()

  /** Surface for application code to register custom meters (counters/timers/gauges).
    *
    * Always returns the composite, so any instrumentation reaches both the local Prometheus scrape endpoint and Grafana
    * Cloud (when OTLP is configured).
    */
  def registry: MeterRegistry = composite
