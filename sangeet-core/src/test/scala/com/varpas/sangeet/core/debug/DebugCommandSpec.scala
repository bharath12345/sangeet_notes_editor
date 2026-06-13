package com.varpas.sangeet.core.debug

import io.circe.parser.decode
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import DebugCommand.given

class DebugCommandSpec extends AnyFlatSpec with Matchers:

  private def roundTrip(cmd: DebugCommand): Unit =
    val encoded = cmd.asJson.noSpaces
    val decoded = decode[DebugCommand](encoded)
    decoded shouldBe Right(cmd)

  "DebugCommand" should "round-trip Ping" in roundTrip(DebugCommand.Ping)
  it should "round-trip Reset with raag" in {
    roundTrip(DebugCommand.Reset("gat", Some("yaman"), "teentaal"))
  }
  it should "round-trip Reset without raag (Palta)" in {
    roundTrip(DebugCommand.Reset("palta", None, "teentaal"))
  }
  it should "round-trip TypeChar" in roundTrip(DebugCommand.TypeChar("s"))
  it should "round-trip TypeTimed" in roundTrip(DebugCommand.TypeTimed("s", 250))
  it should "round-trip SwarGroup with 4 notes" in {
    roundTrip(DebugCommand.SwarGroup(List("s", "r", "g", "m")))
  }
  it should "round-trip SimpleOrnament" in {
    roundTrip(DebugCommand.SimpleOrnament("gamak"))
  }
  it should "round-trip GetState" in roundTrip(DebugCommand.GetState)

  it should "encode the discriminator as a top-level field" in {
    val json = (DebugCommand.TypeChar("s"): DebugCommand).asJson.noSpaces
    json should include(""""TypeChar"""")
  }

  it should "reject unknown discriminator values" in {
    val bad = """{"NotARealCommand":{}}"""
    decode[DebugCommand](bad).isLeft shouldBe true
  }
