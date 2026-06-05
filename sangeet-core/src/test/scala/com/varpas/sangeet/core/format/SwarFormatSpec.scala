package com.varpas.sangeet.core.format

import java.nio.file.{Files, Path}

import scala.util.Try

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.core.model._

class SwarFormatSpec extends AnyFlatSpec with Matchers:

  val minimalComposition: Composition = Composition(
    metadata = Metadata(
      title = "Test Composition",
      compositionType = CompositionType.Gat,
      raag = Raag(
        name = "Yaman",
        thaat = None,
        arohana = None,
        avarohana = None,
        vadi = None,
        samvadi = None,
        pakad = None,
        prahar = None
      ),
      taal = Taal(
        name = "Teentaal",
        matras = 16,
        vibhags = List(
          Vibhag(4, VibhagMarker.Sam),
          Vibhag(4, VibhagMarker.Taali(2)),
          Vibhag(4, VibhagMarker.Khali),
          Vibhag(4, VibhagMarker.Taali(3))
        ),
        theka = None
      ),
      laya = Some(Laya.Vilambit),
      instrument = None,
      composer = None,
      author = None,
      source = None,
      showStrokeLine = false,
      showSahityaLine = false,
      createdAt = "2026-01-01T00:00:00Z",
      updatedAt = "2026-01-01T00:00:00Z"
    ),
    sections = List(
      Section(
        "Sthayi",
        SectionType.Sthayi,
        List(
          Event.Swar(
            Note.Sa,
            Variant.Shuddha,
            Octave.Madhya,
            BeatPosition(0, 0, Rational.onBeat),
            Rational.fullBeat,
            None,
            Nil,
            None
          )
        )
      )
    )
  )

  "SwarFormat" should "include version field in toJson" in {
    val json = SwarFormat.toJson(minimalComposition)
    json.hcursor.downField("version").as[String] shouldBe Right("1.0")
  }

  it should "roundtrip composition through toJson/fromJson" in {
    val json       = SwarFormat.toJson(minimalComposition)
    val jsonString = json.spaces2
    val result     = SwarFormat.fromJson(jsonString)
    result shouldBe Right(minimalComposition)
  }

  it should "accept files with version 1.0" in {
    val jsonString =
      """{"version":"1.0","metadata":{"title":"Test","compositionType":"gat","raag":{"name":"Yaman","thaat":null,"arohana":null,"avarohana":null,"vadi":null,"samvadi":null,"pakad":null,"prahar":null},"taal":{"name":"Teentaal","matras":16,"vibhags":[{"beats":4,"marker":"sam"},{"beats":4,"marker":{"taali":2}},{"beats":4,"marker":"khali"},{"beats":4,"marker":{"taali":3}}],"theka":null},"createdAt":"2026-01-01","updatedAt":"2026-01-01"},"sections":[]}"""
    val result = SwarFormat.fromJson(jsonString)
    result.isRight shouldBe true
  }

  it should "warn and parse files with unknown version" in {
    val jsonString =
      """{"version":"2.0","metadata":{"title":"Test","compositionType":"gat","raag":{"name":"Yaman","thaat":null,"arohana":null,"avarohana":null,"vadi":null,"samvadi":null,"pakad":null,"prahar":null},"taal":{"name":"Teentaal","matras":16,"vibhags":[{"beats":4,"marker":"sam"},{"beats":4,"marker":{"taali":2}},{"beats":4,"marker":"khali"},{"beats":4,"marker":{"taali":3}}],"theka":null},"createdAt":"2026-01-01","updatedAt":"2026-01-01"},"sections":[]}"""
    val result = SwarFormat.fromJson(jsonString)
    // Should still parse (best-effort)
    result.isRight shouldBe true
  }

  it should "warn and parse files with no version field" in {
    val jsonString =
      """{"metadata":{"title":"Test","compositionType":"gat","raag":{"name":"Yaman","thaat":null,"arohana":null,"avarohana":null,"vadi":null,"samvadi":null,"pakad":null,"prahar":null},"taal":{"name":"Teentaal","matras":16,"vibhags":[{"beats":4,"marker":"sam"},{"beats":4,"marker":{"taali":2}},{"beats":4,"marker":"khali"},{"beats":4,"marker":{"taali":3}}],"theka":null},"createdAt":"2026-01-01","updatedAt":"2026-01-01"},"sections":[]}"""
    val result = SwarFormat.fromJson(jsonString)
    // Should still parse (best-effort)
    result.isRight shouldBe true
  }

  it should "write and read file successfully" in {
    val tempFile = Files.createTempFile("test-composition", ".swar")
    try
      SwarFormat.writeFile(tempFile, minimalComposition)
      val result = SwarFormat.readFile(tempFile)
      result shouldBe Right(minimalComposition)
    finally Try(Files.deleteIfExists(tempFile))
  }

  it should "fail gracefully for non-existent file" in {
    val nonExistentPath = Path.of("/nonexistent/path/file.swar")
    val result          = SwarFormat.readFile(nonExistentPath)
    result.isLeft shouldBe true
  }

  it should "fail gracefully for malformed JSON" in {
    val malformedJson = """{"metadata": invalid json}"""
    val result        = SwarFormat.fromJson(malformedJson)
    result.isLeft shouldBe true
  }
