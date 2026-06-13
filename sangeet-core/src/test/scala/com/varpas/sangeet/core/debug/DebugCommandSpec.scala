package com.varpas.sangeet.core.debug

import io.circe.parser.decode
import io.circe.syntax._
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

  "DebugCommand.fromText" should "parse ping" in {
    DebugCommand.fromText("ping") shouldBe Right(DebugCommand.Ping)
  }
  it should "parse reset with raag" in {
    DebugCommand.fromText("reset gat yaman teentaal") shouldBe
      Right(DebugCommand.Reset("gat", Some("yaman"), "teentaal"))
  }
  it should "parse reset without raag (palta)" in {
    DebugCommand.fromText("reset palta teentaal") shouldBe
      Right(DebugCommand.Reset("palta", None, "teentaal"))
  }
  it should "parse type with multi-char arg" in {
    DebugCommand.fromText("type srgmp") shouldBe Right(DebugCommand.TypeChar("srgmp"))
  }
  it should "parse type-timed" in {
    DebugCommand.fromText("type-timed s 250") shouldBe Right(DebugCommand.TypeTimed("s", 250))
  }
  it should "parse swar-group" in {
    DebugCommand.fromText("swar-group srgm") shouldBe
      Right(DebugCommand.SwarGroup(List("s", "r", "g", "m")))
  }
  it should "parse 'group srgm' as SwarGroup alias" in {
    DebugCommand.fromText("group srgm") shouldBe
      Right(DebugCommand.SwarGroup(List("s", "r", "g", "m")))
  }
  it should "reject unknown commands" in {
    DebugCommand.fromText("not-a-real-command").isLeft shouldBe true
  }
  it should "reject empty input" in {
    DebugCommand.fromText("").isLeft shouldBe true
    DebugCommand.fromText("   ").isLeft shouldBe true
  }
