package com.varpas.sangeet.core.api

import io.circe.parser._
import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/** Wire-format round-trip for the Plan 18 PR-3b client→server metrics event payload. Both the web (Elm) and desktop
  * (`DesktopMetrics`) build JSON by hand against the shape codified by [[MetricsEvent]]; this spec guards against
  * accidental shape drift.
  */
class MetricsEventApiTest extends AnyFlatSpec with Matchers:

  "MetricsEvent" should "round-trip through circe encode/decode" in {
    val event = MetricsEvent(
      counter = "sangeet_editor_mutation_total",
      labels = Map("kind" -> "swar_insert")
    )

    val json    = event.asJson.noSpaces
    val decoded = parse(json).flatMap(_.as[MetricsEvent])

    decoded shouldBe Right(event)
  }

  it should "encode an empty labels map as a JSON object (not omitted)" in {
    val event = MetricsEvent(counter = "sangeet_section_switch_total", labels = Map.empty)
    val json  = event.asJson.noSpaces

    // Section switch carries no labels — but circe should still serialize the field as `{}` so
    // the server's decoder doesn't need a special "missing labels" branch.
    json shouldBe """{"counter":"sangeet_section_switch_total","labels":{}}"""
  }

  it should "decode the documented payload shape used by web + desktop clients" in {
    val json =
      """{"counter":"sangeet_file_op_total","labels":{"op":"save","result":"success"}}"""
    val decoded = parse(json).flatMap(_.as[MetricsEvent])
    decoded.map(_.counter) shouldBe Right("sangeet_file_op_total")
    decoded.map(_.labels) shouldBe Right(Map("op" -> "save", "result" -> "success"))
  }
