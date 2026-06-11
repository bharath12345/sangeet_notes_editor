package com.varpas.sangeet.server

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.semigroupk._
import com.comcast.ip4s._
import io.circe.Json
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Location
import org.http4s.server.Router
import org.http4s.{HttpRoutes, Method, Response, Status, Uri}
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import com.varpas.sangeet.server.endpoints.AllEndpoints
import com.varpas.sangeet.server.metrics.MetricsRegistry
import com.varpas.sangeet.server.routes.AllRoutes

object Main extends IOApp:

  private val healthEndpoint: sttp.tapir.server.ServerEndpoint[Any, IO] =
    endpoint.get
      .in("health")
      .out(jsonBody[Json])
      .serverLogicSuccess { _ =>
        IO.pure(
          Json.obj(
            "status"  -> Json.fromString("ok"),
            "service" -> Json.fromString("sangeet-server"),
            "version" -> Json.fromString("0.2.0")
          )
        )
      }

  // Prometheus scrape endpoint. Returns plain-text exposition format; the
  // Prometheus content type version param (`; version=0.0.4`) is recognised
  // by all scrapers and tooling we care about (including Grafana Cloud).
  private val metricsEndpoint: sttp.tapir.server.ServerEndpoint[Any, IO] =
    endpoint.get
      .in("metrics")
      .out(stringBody)
      .out(header("Content-Type", "text/plain; version=0.0.4; charset=utf-8"))
      .serverLogicSuccess(_ => IO.delay(MetricsRegistry.scrape()))

  // The root path of an API server isn't very discoverable on its own. Send
  // bare visitors to the Swagger UI so the URL self-describes when pasted.
  // Defined as a plain http4s route (not Tapir) because a Tapir endpoint
  // with no path inputs matches every GET, not just `/`.
  private val rootRedirectRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req if req.method == Method.GET && req.uri.path.segments.isEmpty =>
      IO.pure(
        Response[IO](Status.Found)
          .withHeaders(Location(Uri.unsafeFromString("/docs/")))
      )
  }

  override def run(args: List[String]): IO[ExitCode] =
    val portNum = sys.env.getOrElse("PORT", "28080").toInt

    val swaggerEndpoints = SwaggerInterpreter()
      .fromEndpoints[IO](AllEndpoints.all, "Sangeet Notes Editor API", "0.2.0")

    val allServerEndpoints =
      List(healthEndpoint, metricsEndpoint) ++ AllRoutes.all ++ swaggerEndpoints

    val tapirRoutes = Http4sServerInterpreter[IO]().toRoutes(allServerEndpoints)
    val combined    = rootRedirectRoute <+> tapirRoutes
    val corsRoutes  = CorsMiddleware(combined)
    val httpApp     = Router("/" -> corsRoutes).orNotFound

    // Force MetricsRegistry init at startup so JVM bindings are attached
    // before the first request, and OTLP push (if configured) begins ticking.
    val otlpStatus =
      if MetricsRegistry.otlp.isDefined then "OTLP push: enabled (pushing to OTLP_ENDPOINT every 30s)"
      else "OTLP push: disabled (set OTLP_ENDPOINT and OTLP_AUTH env vars to enable)"

    for
      _ <- IO.println(s"Sangeet Server starting on port $portNum...")
      _ <- IO.println(s"Swagger UI: http://localhost:$portNum/docs")
      _ <- IO.println(s"Health check: http://localhost:$portNum/health")
      _ <- IO.println(s"Metrics (Prometheus): http://localhost:$portNum/metrics")
      _ <- IO.println(otlpStatus)
      exitCode <- EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withPort(Port.fromInt(portNum).getOrElse(port"28080"))
        .withHttpApp(httpApp)
        .build
        .useForever
        .as(ExitCode.Success)
    yield exitCode
