package com.varpas.sangeet.core.format

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.taal.Taals
import com.varpas.sangeet.core.raag.Raags
import java.nio.file.{Files, Path}
import java.time.Instant

class PdfExportSpec extends AnyFunSpec with Matchers:

  def testComposition: Composition =
    val now = Instant.now().toString
    val metadata = Metadata(
      title = "Test Composition",
      compositionType = CompositionType.Gat,
      raag = Raags.yaman,
      taal = Taals.teentaal,
      laya = Some(Laya.Vilambit),
      instrument = Some("Sitar"),
      composer = None,
      author = None,
      source = None,
      showStrokeLine = true,
      showSahityaLine = true,
      createdAt = now,
      updatedAt = now
    )

    val events = List(
      Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya,
        BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat, Some(Stroke.Da), Nil, None),
      Event.Swar(Note.Re, Variant.Shuddha, Octave.Madhya,
        BeatPosition(0, 1, Rational.onBeat), Rational.fullBeat, Some(Stroke.Ra), Nil, None),
      Event.Swar(Note.Ga, Variant.Shuddha, Octave.Taar,
        BeatPosition(0, 2, Rational.onBeat), Rational.fullBeat, None, List(Gamak()), Some("test")),
      Event.Rest(BeatPosition(0, 3, Rational.onBeat), Rational.fullBeat)
    )

    val section = Section(
      name = "Sthayi",
      sectionType = SectionType.Sthayi,
      events = events,
      tihai = None
    )

    Composition(metadata, List(section))

  describe("PdfExport") {
    it("should generate a PDF without crashing") {
      val composition = testComposition
      val tempFile = Files.createTempFile("sangeet-test", ".pdf")
      try {
        PdfExport.exportPdf(composition, tempFile, SwarScript.Devanagari, landscape = false)
        Files.exists(tempFile) shouldBe true
        Files.size(tempFile) should be > 0L
      } finally {
        Files.deleteIfExists(tempFile)
      }
    }

    it("should generate a PDF in landscape mode") {
      val composition = testComposition
      val tempFile = Files.createTempFile("sangeet-test-landscape", ".pdf")
      try {
        PdfExport.exportPdf(composition, tempFile, SwarScript.Devanagari, landscape = true)
        Files.exists(tempFile) shouldBe true
        Files.size(tempFile) should be > 0L
      } finally {
        Files.deleteIfExists(tempFile)
      }
    }

    it("should generate a PDF with English script") {
      val composition = testComposition
      val tempFile = Files.createTempFile("sangeet-test-english", ".pdf")
      try {
        PdfExport.exportPdf(composition, tempFile, SwarScript.English, landscape = false)
        Files.exists(tempFile) shouldBe true
        Files.size(tempFile) should be > 0L
      } finally {
        Files.deleteIfExists(tempFile)
      }
    }

    it("should handle compositions with multiple sections") {
      val composition = testComposition
      val antara = Section(
        name = "Antara",
        sectionType = SectionType.Antara,
        events = List(
          Event.Swar(Note.Pa, Variant.Shuddha, Octave.Madhya,
            BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat, None, Nil, None)
        ),
        tihai = None
      )
      val multiSection = composition.copy(sections = composition.sections :+ antara)

      val tempFile = Files.createTempFile("sangeet-test-multi", ".pdf")
      try {
        PdfExport.exportPdf(multiSection, tempFile, SwarScript.Devanagari, landscape = false)
        Files.exists(tempFile) shouldBe true
        Files.size(tempFile) should be > 0L
      } finally {
        Files.deleteIfExists(tempFile)
      }
    }
  }
