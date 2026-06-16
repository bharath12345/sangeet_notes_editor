package com.varpas.sangeet.core.format

import io.circe.parser._
import io.circe.syntax._
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.editor.ClipboardCodecs.given
import com.varpas.sangeet.core.editor.ClipboardData
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.core.generators.Generators
import com.varpas.sangeet.core.model.Event

/** Plan 19 T1C — Phase C gap-fill for the clipboard codec.
  *
  * T1B's `CompositionEditorPropSpec` includes a single round-trip line for ClipboardData, but the codec also has two
  * other contracts worth pinning as properties:
  *   - the JSON marker (`sangeet-clipboard: true` + `version: "2.0"`) is preserved across round-trips for any event
  *     list
  *   - decoding rejects payloads whose marker is missing or set to `false`, regardless of the events alongside
  *
  * These mirror the example assertions in `ClipboardCodecsSpec` (rejection cases) but quantify over arbitrary event
  * lists rather than the single hand-built one.
  */
class ClipboardCodecsPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  private val genEvents: Gen[List[Event]] =
    Gen.choose(0, 8).flatMap(Gen.listOfN(_, Generators.genEvent))

  test("propClipboardRoundTrip: ClipboardData(events) encodes and decodes to itself"):
    forAll(genEvents) { events =>
      val cd      = ClipboardData(events)
      val decoded = cd.asJson.as[ClipboardData]
      assert(decoded == Right(cd))
    }

  test("propClipboardMarkerPreserved: every encoded payload carries the sangeet-clipboard marker + version"):
    forAll(genEvents) { events =>
      val json = ClipboardData(events).asJson.hcursor
      assert(json.downField("sangeet-clipboard").as[Boolean] == Right(true))
      assert(json.downField("version").as[String] == Right("2.0"))
    }

  test("propClipboardRoundTripViaNoSpacesString: encoded → noSpaces → parsed back to the same ClipboardData"):
    // System clipboards round-trip strings, not Json values. Pin the string-level path for any event list.
    forAll(genEvents) { events =>
      val cd       = ClipboardData(events)
      val jsonStr  = cd.asJson.noSpaces
      val reparsed = parse(jsonStr).flatMap(_.as[ClipboardData])
      assert(reparsed == Right(cd))
    }

  test("propClipboardRejectsFalseMarker: { sangeet-clipboard: false, ... } decode always fails"):
    // Quantifies the existing example "rejects JSON with false marker" over any event list.
    forAll(genEvents) { events =>
      val rejectedJson = io.circe.Json.obj(
        "sangeet-clipboard" -> io.circe.Json.False,
        "version"           -> io.circe.Json.fromString("2.0"),
        "events"            -> events.asJson
      )
      assert(rejectedJson.as[ClipboardData].isLeft)
    }
