package com.varpas.sangeet.core.debug

import io.circe.parser.decode
import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TestDefinitionSpec extends AnyFlatSpec with Matchers:

  "TestDefinition" should "round-trip a small test" in {
    val defn = TestDefinition(
      name = "sample",
      description = Some("Smoke test"),
      steps = List(
        TestStep.Cmd(DebugCommand.Reset("gat", Some("yaman"), "teentaal")),
        TestStep.Cmd(DebugCommand.TypeChar("s")),
        TestStep.Checkpoint(ExpectedState(eventCount = Some(1), cursorBeat = Some(2))),
        TestStep.AssertGoldenSwar("golden/sample.swar")
      )
    )
    val json = defn.asJson.noSpaces
    decode[TestDefinition](json) shouldBe Right(defn)
  }

  it should "round-trip with no description" in {
    val defn = TestDefinition(
      name = "minimal",
      description = None,
      steps = List(TestStep.Cmd(DebugCommand.Ping))
    )
    val json = defn.asJson.noSpaces
    decode[TestDefinition](json) shouldBe Right(defn)
  }

  it should "round-trip with all TestStep variants" in {
    val defn = TestDefinition(
      name = "all_step_types",
      description = Some("Test all step variants"),
      steps = List(
        TestStep.Cmd(DebugCommand.Reset("gat", Some("yaman"), "teentaal")),
        TestStep.Checkpoint(ExpectedState(eventCount = Some(10), sectionName = Some("Sthayi"))),
        TestStep.AssertGoldenSwar("golden/test.swar"),
        TestStep.AssertGoldenHtml("golden/test.html")
      )
    )
    val json = defn.asJson.noSpaces
    decode[TestDefinition](json) shouldBe Right(defn)
  }

  "TestStep.Cmd" should "encode with DebugCommand nested under cmd field" in {
    val step = TestStep.Cmd(DebugCommand.TypeChar("s"))
    val json = step.asJson.noSpaces
    json should include(""""Cmd"""")
    json should include(""""cmd"""")
    json should include(""""TypeChar"""")
  }

  "TestStep.Checkpoint" should "encode with ExpectedState nested under expect field" in {
    val step = TestStep.Checkpoint(ExpectedState(eventCount = Some(5)))
    val json = step.asJson.noSpaces
    json should include(""""Checkpoint"""")
    json should include(""""expect"""")
    json should include(""""eventCount"""")
  }

  "TestStep.AssertGoldenSwar" should "round-trip" in {
    val step = TestStep.AssertGoldenSwar("golden/test.swar")
    val json = step.asJson.noSpaces
    decode[TestStep](json) shouldBe Right(step)
  }

  "TestStep.AssertGoldenHtml" should "round-trip" in {
    val step = TestStep.AssertGoldenHtml("golden/test.html")
    val json = step.asJson.noSpaces
    decode[TestStep](json) shouldBe Right(step)
  }

  "ExpectedState" should "encode only present fields" in {
    val state = ExpectedState(eventCount = Some(5), cursorBeat = Some(2))
    val json  = state.asJson.noSpaces
    json should include(""""eventCount":5""")
    json should include(""""cursorBeat":2""")
    // Should NOT include fields that are None
    json should not include ("cursorCycle")
    json should not include ("sectionName")
  }

  it should "round-trip with all fields populated" in {
    val state = ExpectedState(
      eventCount = Some(10),
      cursorBeat = Some(3),
      cursorCycle = Some(2),
      sectionName = Some("Sthayi"),
      taalName = Some("teentaal"),
      raagName = Some("yaman"),
      sectionCount = Some(3)
    )
    val json = state.asJson.noSpaces
    decode[ExpectedState](json) shouldBe Right(state)
  }

  it should "round-trip with no fields populated" in {
    val state = ExpectedState()
    val json  = state.asJson.noSpaces
    decode[ExpectedState](json) shouldBe Right(state)
  }

  it should "reject unknown TestStep variants" in {
    val badJson = """{"UnknownVariant":{"foo":"bar"}}"""
    decode[TestStep](badJson).isLeft shouldBe true
  }

  it should "handle a realistic full test definition" in {
    val defn = TestDefinition(
      name = "build_gat_with_antara",
      description = Some("Build a Gat with Antara section, assert event count + golden swar"),
      steps = List(
        TestStep.Cmd(DebugCommand.Reset("gat", Some("yaman"), "teentaal")),
        TestStep.Cmd(DebugCommand.TypeChar("s")),
        TestStep.Cmd(DebugCommand.TypeChar("r")),
        TestStep.Checkpoint(ExpectedState(eventCount = Some(2), cursorBeat = Some(3))),
        TestStep.AssertGoldenSwar("golden/build-gat-with-antara.swar"),
        TestStep.AssertGoldenHtml("golden/build-gat-with-antara.html")
      )
    )
    val json    = defn.asJson.spaces2
    val decoded = decode[TestDefinition](json)
    decoded shouldBe Right(defn)
  }
