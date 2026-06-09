package com.varpas.sangeet.core.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import io.circe.parser._
import io.circe.syntax._

import com.varpas.sangeet.core.editor.ClipboardCodecs.given
import com.varpas.sangeet.core.model._

class ClipboardCodecsSpec extends AnyFlatSpec with Matchers:

  val sampleEvents = List(
    Event.Swar(
      Note.Sa,
      Variant.Shuddha,
      Octave.Madhya,
      BeatPosition(0, 0, Rational.onBeat),
      Rational(1, 1),
      None,
      Nil,
      None
    ),
    Event.Swar(
      Note.Re,
      Variant.Shuddha,
      Octave.Madhya,
      BeatPosition(0, 1, Rational.onBeat),
      Rational(1, 1),
      None,
      Nil,
      None
    ),
    Event.Rest(BeatPosition(0, 2, Rational.onBeat), Rational(1, 1))
  )

  "ClipboardCodecs" should "roundtrip encode and decode ClipboardData" in {
    val original = ClipboardData(sampleEvents)
    val json     = original.asJson
    val decoded  = json.as[ClipboardData]
    decoded shouldBe Right(original)
  }

  it should "include sangeet-clipboard marker in JSON" in {
    val json = ClipboardData(Nil).asJson
    json.hcursor.downField("sangeet-clipboard").as[Boolean] shouldBe Right(true)
    json.hcursor.downField("version").as[String] shouldBe Right("2.0")
  }

  it should "reject JSON without sangeet-clipboard marker" in {
    val json = parse("""{"events": []}""").getOrElse(io.circe.Json.Null)
    json.as[ClipboardData].isLeft shouldBe true
  }

  it should "reject JSON with false marker" in {
    val json = parse("""{"sangeet-clipboard": false, "version": "2.0", "events": []}""").getOrElse(io.circe.Json.Null)
    json.as[ClipboardData].isLeft shouldBe true
  }

  it should "handle empty events list" in {
    val original = ClipboardData(Nil)
    val decoded  = original.asJson.as[ClipboardData]
    decoded shouldBe Right(ClipboardData(Nil))
  }

  it should "roundtrip events with ornaments and strokes" in {
    val events = List(
      Event.Swar(
        Note.Ga,
        Variant.Shuddha,
        Octave.Taar,
        BeatPosition(0, 0, Rational.onBeat),
        Rational(1, 2),
        Some(Stroke.Da),
        List(KanSwar(NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya))),
        Some("la")
      ),
      Event.Chikari(BeatPosition(0, 1, Rational.onBeat), Rational(1, 1))
    )
    val original = ClipboardData(events)
    val decoded  = original.asJson.as[ClipboardData]
    decoded shouldBe Right(original)
  }

  it should "produce valid JSON string for system clipboard" in {
    val cd       = ClipboardData(sampleEvents)
    val jsonStr  = cd.asJson.noSpaces
    val reparsed = parse(jsonStr).flatMap(_.as[ClipboardData])
    reparsed shouldBe Right(cd)
  }
