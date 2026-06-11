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

import com.varpas.sangeet.server.bugreports.BugReportStorage
import com.varpas.sangeet.server.routes.BugReportRoutes

class BugReportRoutesSpec extends AnyFlatSpec with Matchers:

  /** In-memory fake — captures (reportId, body) calls so the test can assert what would be sent to GCS without touching
    * the network.
    */
  final private class FakeStorage(state: Ref[IO, List[(String, Json)]], outcome: Either[String, Unit])
      extends BugReportStorage:
    def store(reportId: String, body: Json): IO[Either[String, Unit]] =
      state.update(_ :+ (reportId, body)) *> IO.pure(outcome)

  private def routesWith(storage: BugReportStorage) =
    Http4sServerInterpreter[IO]().toRoutes(BugReportRoutes.createBugReport(storage)).orNotFound

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
