package com.varpas.sangeet.server.codegen

import java.nio.file.{Files, Paths, StandardOpenOption}

import io.circe.Json
import sttp.apispec.openapi.circe.yaml._
import sttp.tapir._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.json.circe._

import com.varpas.sangeet.server.endpoints.AllEndpoints

/** PR-1c (plan 18): exports the Tapir endpoint catalog as an OpenAPI 3.0 YAML document under
  * `docs/developer/specs/openapi.yaml`. Run via `make gen-specs` (which invokes the sbt `generateOpenApi` task); CI's
  * `check-specs` job runs the same task and fails if the committed file drifts from generation.
  *
  * The output is byte-for-byte deterministic — OpenAPIDocsInterpreter walks the endpoint ADT in source order, and
  * circe-yaml's emitter is stable — so the diff check is meaningful. If you change an endpoint's name, summary, path,
  * params, or schemas, regenerate.
  */
object OpenApiExporter:

  /** Title and version shown at the top of the OpenAPI doc. Kept in sync with the values passed to SwaggerInterpreter
    * in Main.scala — the two surfaces should describe the same API.
    */
  private val ApiTitle   = "Sangeet Notes Editor API"
  private val ApiVersion = "0.2.0"

  /** Default path when no argument is given. Resolved against cwd, so it only works when launched from the build root.
    * The sbt `generateOpenApi` task passes an absolute path explicitly to avoid the cwd-confusion that `fork := true`
    * introduces (each sub-project's forked JVM inherits its own baseDirectory as cwd).
    */
  private val DefaultOutputPath = Paths.get("docs", "developer", "specs", "openapi.yaml")

  /** Infra endpoints (`/health`, `/metrics`) live in `Main.scala`, not in `AllEndpoints`, because their server logic
    * needs IO context and is wired alongside the http4s server. Re-declare just the endpoint shapes here so the
    * generated spec covers them — these duplicate the wire format only, and Main remains the single source of truth for
    * the runtime behavior.
    */
  private val healthEndpoint: AnyEndpoint =
    endpoint.get
      .in("health")
      .out(jsonBody[Json])
      .name("health")
      .summary("Liveness probe")
      .description("Returns 200 with service identification once the server is bound.")

  private val metricsEndpoint: AnyEndpoint =
    endpoint.get
      .in("metrics")
      .out(stringBody)
      .name("metrics")
      .summary("Prometheus scrape endpoint")
      .description("Returns process + HTTP metrics in Prometheus text exposition format (text/plain; version=0.0.4).")

  def main(args: Array[String]): Unit =
    val outputPath = args.headOption.map(Paths.get(_)).getOrElse(DefaultOutputPath)

    val allEndpoints = List(healthEndpoint, metricsEndpoint) ++ AllEndpoints.all

    val openApi = OpenAPIDocsInterpreter().toOpenAPI(allEndpoints, ApiTitle, ApiVersion)
    val yaml    = openApi.toYaml

    Files.createDirectories(outputPath.getParent)
    Files.writeString(
      outputPath,
      yaml,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    )
    println(
      s"Wrote ${outputPath.toAbsolutePath} (${yaml.getBytes("UTF-8").length} bytes, ${allEndpoints.size} endpoints)"
    )
