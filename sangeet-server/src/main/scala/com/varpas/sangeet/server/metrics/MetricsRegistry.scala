package com.varpas.sangeet.server.metrics

import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider
import com.google.cloud.monitoring.v3.MetricServiceSettings
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.jvm.{ClassLoaderMetrics, JvmGcMetrics, JvmMemoryMetrics, JvmThreadMetrics}
import io.micrometer.core.instrument.binder.system.{FileDescriptorMetrics, ProcessorMetrics, UptimeMetrics}
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.prometheusmetrics.{PrometheusConfig, PrometheusMeterRegistry}
import io.micrometer.stackdriver.{StackdriverConfig, StackdriverMeterRegistry}

/** Holds the meter registries used across the server.
  *
  *   - `prometheus` always exists and is scraped via `GET /metrics`. Useful for local debugging without depending on
  *     any external service.
  *   - `stackdriver` (GCP Cloud Monitoring) is created only when the env var `GCP_PROJECT_ID` is present. On Cloud Run
  *     it auto-authenticates via Application Default Credentials (the metadata server); locally it's `None` and we skip
  *     the push entirely so dev / tests don't try to talk to GCP.
  *   - `composite` is what application code instruments against; metrics published through it land in both backends
  *     simultaneously.
  *
  * JVM bindings (heap, GC, threads, classes, CPU, file descriptors, uptime) are attached on construction so we get
  * useful baseline observability without any code-side instrumentation.
  *
  * Architecture: Cloud Monitoring stores the metrics (6 weeks to 24 months retention depending on metric type); Grafana
  * Cloud Free can later be wired as a *viewer only* by adding Cloud Monitoring as a data source — no metrics data ever
  * flows through Grafana Cloud's own storage, which sidesteps its 14-day Free-tier retention limit.
  */
object MetricsRegistry:

  val prometheus: PrometheusMeterRegistry =
    new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

  val stackdriver: Option[StackdriverMeterRegistry] =
    sys.env.get("GCP_PROJECT_ID").map { gcpProject =>
      val cfg = new StackdriverConfig:
        override def projectId: String        = gcpProject
        override def step: java.time.Duration = java.time.Duration.ofSeconds(60)
        // Don't override resourceLabels — the default "global" monitored
        // resource type only accepts `project_id` (Cloud Monitoring rejects
        // `service`/`version` here with INVALID_ARGUMENT "unrecognized
        // resource label"). Service/version identification is added below as
        // commonTags on the composite, so they appear as metric.labels on
        // every meter instead.
        override def get(key: String): String = null

      // Cloud Monitoring's CreateTimeSeries responses, particularly partial-
      // failure error replies that enumerate every rejected time series in
      // `grpc-status-details-bin` (a base64-encoded protobuf), can far exceed
      // Netty's 10 KiB default HTTP/2 header limit. With 7 JVM binders each
      // emitting many series per push, a single rejection details blob can
      // run tens of KiB. We bumped to 32 KiB first — still tripped at 32768 —
      // so we raise to 1 MiB. Anything beyond that warrants real investigation,
      // not another bump.
      val channelProvider = InstantiatingGrpcChannelProvider
        .newBuilder()
        .setChannelConfigurator { builder =>
          builder match
            case ncb: NettyChannelBuilder => ncb.maxInboundMetadataSize(1024 * 1024)
            case b                        => b
        }
        .build()
      val settings: java.util.concurrent.Callable[MetricServiceSettings] =
        () =>
          MetricServiceSettings
            .newBuilder()
            .setTransportChannelProvider(channelProvider)
            .build()

      StackdriverMeterRegistry
        .builder(cfg)
        .metricServiceSettings(settings)
        .build()
    }

  val composite: CompositeMeterRegistry =
    val c = new CompositeMeterRegistry()
    c.add(prometheus)
    stackdriver.foreach(c.add)
    // Common tags applied to every meter — these become metric.labels (NOT
    // resource.labels) on Cloud Monitoring side, so Grafana/dashboards can
    // filter/group by service and version without hitting the global
    // resource type's label restrictions.
    c.config().commonTags("service", "sangeet-server", "version", "0.2.0")
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
    * Always returns the composite, so any instrumentation reaches both the local Prometheus scrape endpoint and Cloud
    * Monitoring (when `GCP_PROJECT_ID` is configured).
    */
  def registry: MeterRegistry = composite
