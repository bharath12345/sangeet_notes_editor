package com.varpas.sangeet.server.routes

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import io.circe.Json
import io.circe.parser._
import org.http4s._
import org.http4s.implicits._
import org.scalacheck.Shrink
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.typelevel.ci.CIStringSyntax
import sttp.tapir.server.http4s.Http4sServerInterpreter

import com.varpas.sangeet.server.bugreports.{BugReportStorage, DisabledGitHubIssuesClient, GitHubIssuesClient}
import com.varpas.sangeet.server.generators.RequestGenerators

/** Plan 19 T2B — bug-report endpoint contract properties.
  *
  * The bug-report endpoint accepts arbitrary JSON (the schema is intentionally open — see
  * `BugReportEndpoints.createBugReport`'s doc). That openness is exactly what we want to test as a property: no matter
  * what JSON body the user sends, the response must be one of:
  *
  *   - 200 OK with a `reportId` and `status: "received"` (storage accepted the write)
  *   - 503 ServiceUnavailable with a diagnostic JSON body (storage refused)
  *
  * It must NEVER be a 5xx that isn't 503 (e.g. an uncaught exception leaking out of the IO chain) and it must NEVER be
  * a 2xx with a malformed body. This property catches a whole class of "any JSON body that decodes" injection bugs.
  *
  * GitHub side-effect: routed to the disabled client so the property doesn't hit the network.
  */
class BugReportRoutesPropSpec extends AnyFunSuite with Matchers with ScalaCheckPropertyChecks:

  /** Always-succeeding storage — the happy path. */
  final private class OkStorage(state: Ref[IO, List[(String, Json)]]) extends BugReportStorage:
    def store(reportId: String, body: Json): IO[Either[String, Unit]] =
      state.update(_ :+ (reportId, body)) *> IO.pure(Right(()))

  /** Always-failing storage — the contract is "503 with diagnostic body". */
  final private class FailStorage extends BugReportStorage:
    def store(reportId: String, body: Json): IO[Either[String, Unit]] =
      IO.pure(Left("simulated GCS outage"))

  private def routesWith(storage: BugReportStorage, issues: GitHubIssuesClient = DisabledGitHubIssuesClient) =
    Http4sServerInterpreter[IO]()
      .toRoutes(BugReportRoutes.createBugReport(storage, issues, gcsBucket = None, replayBaseUrl = None))
      .orNotFound

  private given Shrink[Json] = Shrink.shrinkAny[Json]

  private def post(routes: HttpApp[IO], body: Json): Response[IO] =
    val req = Request[IO](Method.POST, uri"/api/v1/bug-reports")
      .withEntity(body.noSpaces)
      .withHeaders(Headers(Header.Raw(ci"Content-Type", "application/json")))
    routes.run(req).unsafeRunSync()

  test("propBugReportAcceptsArbitraryJson: any JSON body → 200 with reportId + status=received") {
    val seen   = Ref.unsafe[IO, List[(String, Json)]](List.empty)
    val routes = routesWith(new OkStorage(seen))

    forAll(RequestGenerators.genBugReportBody) { body =>
      val resp = post(routes, body)
      resp.status shouldBe Status.Ok

      val parsed = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("response not JSON"))
      // Status is always "received" on the happy path.
      parsed.hcursor.get[String]("status").getOrElse("") shouldBe "received"
      // reportId is a UUIDv4 — same regex the example test uses.
      val reportId = parsed.hcursor.get[String]("reportId").getOrElse("")
      reportId should fullyMatch regex "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
    }
  }

  test("propBugReportStorageFailure: any JSON body → 503 with diagnostic envelope when storage fails") {
    val routes = routesWith(new FailStorage)

    forAll(RequestGenerators.genBugReportBody) { body =>
      val resp = post(routes, body)
      resp.status shouldBe Status.ServiceUnavailable

      val parsed = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("response not JSON"))
      parsed.hcursor.get[String]("error").getOrElse("") shouldBe "bug_report_storage_failed"
      // Message must be non-empty so the client / on-call has something to grep for.
      parsed.hcursor.get[String]("message").getOrElse("") should not be empty
    }
  }

  test("propBugReportNeverUnhandled500: response is always {200, 503} (never an unhandled 5xx)") {
    // Two universes: storage-ok and storage-fail. Together they cover both
    // observable branches of the route. The combined property is "no body in
    // either universe produces a 500 / 502 / 504".
    val okSeen     = Ref.unsafe[IO, List[(String, Json)]](List.empty)
    val okRoutes   = routesWith(new OkStorage(okSeen))
    val failRoutes = routesWith(new FailStorage)

    forAll(RequestGenerators.genBugReportBody) { body =>
      val okStatus = post(okRoutes, body).status.code
      okStatus shouldBe 200

      val failStatus = post(failRoutes, body).status.code
      failStatus shouldBe 503
    }
  }
