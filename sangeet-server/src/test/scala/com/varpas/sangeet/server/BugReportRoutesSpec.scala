package com.varpas.sangeet.server

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import io.circe.Json
import io.circe.parser._
import org.http4s._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIStringSyntax
import sttp.tapir.server.http4s.Http4sServerInterpreter

import com.varpas.sangeet.server.bugreports.{BugReportStorage, DisabledGitHubIssuesClient, GitHubIssuesClient}
import com.varpas.sangeet.server.routes.BugReportRoutes

class BugReportRoutesSpec extends AnyFlatSpec with Matchers:

  /** In-memory fake — captures (reportId, body) calls so the test can assert what would be sent to GCS without touching
    * the network.
    */
  final private class FakeStorage(state: Ref[IO, List[(String, Json)]], outcome: Either[String, Unit])
      extends BugReportStorage:
    def store(reportId: String, body: Json): IO[Either[String, Unit]] =
      state.update(_ :+ (reportId, body)) *> IO.pure(outcome)

  /** Records calls without hitting GitHub. */
  final private class FakeIssues(state: Ref[IO, List[(String, String, List[String])]]) extends GitHubIssuesClient:
    def createIssue(title: String, body: String, labels: List[String]): IO[Either[String, String]] =
      state.update(_ :+ (title, body, labels)) *> IO.pure(Right("https://github.com/example/issues/1"))

  private def routesWith(
      storage: BugReportStorage,
      issues: GitHubIssuesClient = DisabledGitHubIssuesClient,
      bucket: Option[String] = None,
      replayBaseUrl: Option[String] = None
  ) =
    Http4sServerInterpreter[IO]()
      .toRoutes(BugReportRoutes.createBugReport(storage, issues, bucket, replayBaseUrl))
      .orNotFound

  "POST /api/v1/bug-reports" should "store the body and return a reportId" in {
    val seen   = Ref.unsafe[IO, List[(String, Json)]](List.empty)
    val routes = routesWith(new FakeStorage(seen, Right(())))

    val body = Json.obj(
      "type"        -> Json.fromString("web"),
      "description" -> Json.fromString("Test bug — keyboard input not working"),
      "replay"      -> Json.arr()
    )

    val req = Request[IO](Method.POST, uri"/api/v1/bug-reports")
      .withEntity(body.noSpaces)
      .withHeaders(Headers(Header.Raw(ci"Content-Type", "application/json")))

    val resp = routes.run(req).unsafeRunSync()
    resp.status shouldBe Status.Ok

    val parsed   = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("response not JSON"))
    val reportId = parsed.hcursor.get[String]("reportId").getOrElse(fail("missing reportId"))
    parsed.hcursor.get[String]("status").getOrElse("") shouldBe "received"
    reportId should fullyMatch regex "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

    val recorded = seen.get.unsafeRunSync()
    recorded.length shouldBe 1
    recorded.head._1 shouldBe reportId
    recorded.head._2 shouldBe body
  }

  it should "return 503 with diagnostic JSON when storage fails" in {
    val seen   = Ref.unsafe[IO, List[(String, Json)]](List.empty)
    val routes = routesWith(new FakeStorage(seen, Left("simulated GCS outage")))

    val req = Request[IO](Method.POST, uri"/api/v1/bug-reports")
      .withEntity(Json.obj("description" -> Json.fromString("x")).noSpaces)
      .withHeaders(Headers(Header.Raw(ci"Content-Type", "application/json")))

    val resp = routes.run(req).unsafeRunSync()
    resp.status shouldBe Status.ServiceUnavailable

    val parsed = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("response not JSON"))
    parsed.hcursor.get[String]("error").getOrElse("") shouldBe "bug_report_storage_failed"
    parsed.hcursor.get[String]("message").getOrElse("") shouldBe "simulated GCS outage"
  }

  it should "file a GitHub issue after successful storage" in {
    val seen       = Ref.unsafe[IO, List[(String, Json)]](List.empty)
    val issuesSeen = Ref.unsafe[IO, List[(String, String, List[String])]](List.empty)
    val routes = routesWith(
      new FakeStorage(seen, Right(())),
      new FakeIssues(issuesSeen),
      Some("sangeet-bug-reports-test"),
      Some("https://server.example.com")
    )

    val body = Json.obj(
      "type"        -> Json.fromString("web"),
      "description" -> Json.fromString("Cursor disappears after typing fast"),
      "email"       -> Json.fromString("user@example.com"),
      "metadata" -> Json.obj(
        "url"       -> Json.fromString("https://app.example.com/edit"),
        "userAgent" -> Json.fromString("Mozilla/5.0"),
        "viewportW" -> Json.fromInt(1920),
        "viewportH" -> Json.fromInt(1080),
        "timestamp" -> Json.fromString("2026-06-11T10:00:00.000Z")
      ),
      "replay" -> Json.arr(Json.obj(), Json.obj(), Json.obj())
    )

    val req = Request[IO](Method.POST, uri"/api/v1/bug-reports")
      .withEntity(body.noSpaces)
      .withHeaders(Headers(Header.Raw(ci"Content-Type", "application/json")))

    val resp = routes.run(req).unsafeRunSync()
    resp.status shouldBe Status.Ok

    // Fiber runs concurrently. Poll briefly — Ref update lands within microseconds in practice.
    val deadline = System.currentTimeMillis() + 2000
    var calls    = List.empty[(String, String, List[String])]
    while calls.isEmpty && System.currentTimeMillis() < deadline do
      calls = issuesSeen.get.unsafeRunSync()
      if calls.isEmpty then Thread.sleep(10)

    calls.length shouldBe 1
    val (title, issueBody, labels) = calls.head
    title should startWith("Bug report — ")
    title should include("Cursor disappears")
    labels should contain allOf ("bug", "from-user", "platform-web")
    issueBody should include("user@example.com")
    issueBody should include("Mozilla/5.0")
    issueBody should include("1920×1080")
    issueBody should include("Replay events captured: 3")
    issueBody should include("storage/browser/_details/sangeet-bug-reports-test/")
    issueBody should include("https://server.example.com/replay/")
    issueBody should include("▶ Play replay")
  }

  it should "not file a GitHub issue when storage fails" in {
    val seen       = Ref.unsafe[IO, List[(String, Json)]](List.empty)
    val issuesSeen = Ref.unsafe[IO, List[(String, String, List[String])]](List.empty)
    val routes = routesWith(
      new FakeStorage(seen, Left("simulated outage")),
      new FakeIssues(issuesSeen),
      Some("any-bucket")
    )

    val req = Request[IO](Method.POST, uri"/api/v1/bug-reports")
      .withEntity(Json.obj("description" -> Json.fromString("x")).noSpaces)
      .withHeaders(Headers(Header.Raw(ci"Content-Type", "application/json")))

    routes.run(req).unsafeRunSync().status shouldBe Status.ServiceUnavailable

    // Give any spurious fiber a chance to run; assert nothing happened.
    Thread.sleep(50)
    issuesSeen.get.unsafeRunSync() shouldBe empty
  }

  /** GitHub client that always throws — exercises the `handleErrorWith` path so a regression cannot reintroduce the
    * pre-PR-3d swallow that lost the user's reportId entirely.
    */
  final private class ThrowingIssues extends GitHubIssuesClient:
    def createIssue(title: String, body: String, labels: List[String]): IO[Either[String, String]] =
      IO.raiseError(new RuntimeException("boom: simulated GitHub API exception"))

  it should "still return a successful response when the GitHub-issue fiber crashes (PR-3d E5)" in {
    val seen   = Ref.unsafe[IO, List[(String, Json)]](List.empty)
    val routes = routesWith(new FakeStorage(seen, Right(())), new ThrowingIssues)

    val req = Request[IO](Method.POST, uri"/api/v1/bug-reports")
      .withEntity(Json.obj("description" -> Json.fromString("desc that triggers a crash")).noSpaces)
      .withHeaders(Headers(Header.Raw(ci"Content-Type", "application/json")))

    val resp = routes.run(req).unsafeRunSync()
    // Storage write succeeded; the fiber crash must NOT bubble back to the user.
    resp.status shouldBe Status.Ok

    val parsed = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("response not JSON"))
    parsed.hcursor.get[String]("status").getOrElse("") shouldBe "received"

    // Give the fiber time to run handleErrorWith (and now: bump the counter + log).
    // We can't easily intercept slf4j output without extra plumbing; the structural
    // assertion is "the server didn't crash, the user got a response, storage was
    // still recorded". The counter increment is observable via /metrics in production.
    Thread.sleep(100)
    seen.get.unsafeRunSync().length shouldBe 1
  }

  /** GitHub client that returns Left — exercises the "API replied with an error" branch (vs the throwing branch). */
  final private class ApiErrorIssues extends GitHubIssuesClient:
    def createIssue(title: String, body: String, labels: List[String]): IO[Either[String, String]] =
      IO.pure(Left("HTTP 403 — rate limit exceeded"))

  it should "still return a successful response when GitHub returns an API error (PR-3d E5)" in {
    val seen   = Ref.unsafe[IO, List[(String, Json)]](List.empty)
    val routes = routesWith(new FakeStorage(seen, Right(())), new ApiErrorIssues)

    val req = Request[IO](Method.POST, uri"/api/v1/bug-reports")
      .withEntity(Json.obj("description" -> Json.fromString("rate-limit-trigger")).noSpaces)
      .withHeaders(Headers(Header.Raw(ci"Content-Type", "application/json")))

    routes.run(req).unsafeRunSync().status shouldBe Status.Ok

    Thread.sleep(100)
    seen.get.unsafeRunSync().length shouldBe 1
  }
