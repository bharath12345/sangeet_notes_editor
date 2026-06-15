package com.varpas.sangeet.server.routes

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.parser._
import org.http4s._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIStringSyntax
import sttp.tapir.server.http4s.Http4sServerInterpreter

import com.varpas.sangeet.server.metrics.MetricsRegistry

/** Covers the happy path (204 + counter visible in the Prometheus scrape) and the cardinality guard rails (400 for
  * unknown counter, unknown label key, unknown label value). The whitelist itself is data in [[AppMetrics]] — the
  * route's only job is to delegate validation + serialise the error reason — so we don't repeat every allowed
  * counter/label combo here.
  */
class MetricsEventRoutesSpec extends AnyFlatSpec with Matchers:

  private val routes = Http4sServerInterpreter[IO]().toRoutes(MetricsEventRoutes.all).orNotFound

  private def post(body: String): Response[IO] =
    val req = Request[IO](Method.POST, uri"/api/v1/metrics/event")
      .withEntity(body)
      .withHeaders(Headers(Header.Raw(ci"Content-Type", "application/json")))
    routes.run(req).unsafeRunSync()

  "POST /api/v1/metrics/event" should "return 204 and increment the counter for a valid request" in {
    // Prometheus scrape is the source of truth for "did the meter land?" — the same path Cloud Monitoring would also
    // see, since both backends share the composite registry.
    val before = MetricsRegistry.scrape()

    val body = """{"counter":"sangeet_editor_mutation_total","labels":{"kind":"swar_insert"}}"""
    val resp = post(body)

    resp.status shouldBe Status.NoContent
    resp.as[String].unsafeRunSync() shouldBe ""

    val after = MetricsRegistry.scrape()
    after should include("sangeet_editor_mutation_total")
    after should include("""kind="swar_insert"""")

    // Confirm the counter actually went up (not just that the line exists from a previous test in the suite).
    val beforeCount = countOf(before, "sangeet_editor_mutation_total", "swar_insert")
    val afterCount  = countOf(after, "sangeet_editor_mutation_total", "swar_insert")
    afterCount shouldBe (beforeCount + 1.0) +- 0.0001
  }

  it should "return 204 for a counter with no labels (sangeet_section_switch_total)" in {
    val resp = post("""{"counter":"sangeet_section_switch_total","labels":{}}""")
    resp.status shouldBe Status.NoContent
  }

  it should "reject an unknown counter name with 400 and a diagnostic body" in {
    val resp = post("""{"counter":"evil","labels":{}}""")
    resp.status shouldBe Status.BadRequest

    val body = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("response not JSON"))
    body.hcursor.get[String]("error").getOrElse("") shouldBe "invalid_metric_event"
    body.hcursor.get[String]("message").getOrElse("") should include("evil")
  }

  it should "reject an unknown label key with 400" in {
    val resp = post(
      """{"counter":"sangeet_editor_mutation_total","labels":{"bogus":"x"}}"""
    )
    resp.status shouldBe Status.BadRequest
    val body = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("not JSON"))
    body.hcursor.get[String]("message").getOrElse("") should include("bogus")
  }

  it should "reject an unknown label value with 400" in {
    val resp = post(
      """{"counter":"sangeet_editor_mutation_total","labels":{"kind":"format_disk"}}"""
    )
    resp.status shouldBe Status.BadRequest
    val body = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("not JSON"))
    body.hcursor.get[String]("message").getOrElse("") should include("format_disk")
  }

  it should "reject a request that omits a required label key with 400" in {
    val resp = post(
      """{"counter":"sangeet_file_op_total","labels":{"op":"save"}}"""
    )
    resp.status shouldBe Status.BadRequest
    val body = parse(resp.as[String].unsafeRunSync()).getOrElse(fail("not JSON"))
    body.hcursor.get[String]("message").getOrElse("") should include("result")
  }

  it should "accept all 5 whitelisted counters with one valid label combo each" in {
    val good = List(
      """{"counter":"sangeet_editor_mutation_total","labels":{"kind":"undo"}}""",
      """{"counter":"sangeet_file_op_total","labels":{"op":"open","result":"success"}}""",
      """{"counter":"sangeet_section_switch_total","labels":{}}""",
      """{"counter":"sangeet_clipboard_op_total","labels":{"op":"copy"}}""",
      """{"counter":"sangeet_ornament_finish_total","labels":{"type":"meend"}}"""
    )
    good.foreach { body =>
      post(body).status shouldBe Status.NoContent
    }
  }

  // Parse the Prometheus exposition format for a specific counter + label-value combo and return the current sample.
  // Defensive — the scrape contains comment lines and other counters; we want exactly the row for this label.
  private def countOf(scrape: String, counter: String, kindValue: String): Double =
    scrape.linesIterator
      .filter(l => l.startsWith(counter) && l.contains(s"""kind="$kindValue""""))
      .map(_.trim.split("\\s+").last.toDouble)
      .nextOption()
      .getOrElse(0.0)
