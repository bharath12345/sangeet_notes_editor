package com.varpas.sangeet.core.format

import java.nio.file.Files
import java.time.Instant

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.taal.Taals

class HtmlExportSpec extends AnyFunSpec with Matchers:

  def testComposition: Composition =
    val now = Instant.now().toString
    val metadata = Metadata(
      title = "Test Composition",
      compositionType = CompositionType.Gat,
      raag = Raags.yaman,
      taal = Taals.teentaal,
      laya = Some(Laya.Vilambit),
      script = None,
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
      Event.Swar(
        Note.Sa,
        Variant.Shuddha,
        Octave.Madhya,
        BeatPosition(0, 0, Rational.onBeat),
        Rational.fullBeat,
        Some(Stroke.Da),
        Nil,
        None
      ),
      Event.Swar(
        Note.Re,
        Variant.Shuddha,
        Octave.Madhya,
        BeatPosition(0, 1, Rational.onBeat),
        Rational.fullBeat,
        Some(Stroke.Ra),
        Nil,
        None
      ),
      Event.Swar(
        Note.Ga,
        Variant.Shuddha,
        Octave.Taar,
        BeatPosition(0, 2, Rational.onBeat),
        Rational.fullBeat,
        None,
        List(Gamak()),
        Some("test")
      ),
      Event.Rest(BeatPosition(0, 3, Rational.onBeat), Rational.fullBeat)
    )

    val section = Section(
      name = "Sthayi",
      sectionType = SectionType.Sthayi,
      events = events,
      tihai = None
    )

    Composition(metadata, List(section))

  describe("HtmlExport") {
    it("should generate valid HTML") {
      val composition = testComposition
      val html        = HtmlExport.render(composition, SwarScript.Devanagari)

      html should include("<!DOCTYPE html>")
      html should include("<html")
      html should include("</html>")
      html should include("<head>")
      html should include("<body>")
    }

    it("should include composition title") {
      val composition = testComposition
      val html        = HtmlExport.render(composition, SwarScript.Devanagari)

      html should include("Test Composition")
    }

    it("should include raag information") {
      val composition = testComposition
      val html        = HtmlExport.render(composition, SwarScript.Devanagari)

      html should include("Raag: Yaman")
      html should include("Kalyan")
    }

    it("should include taal information") {
      val composition = testComposition
      val html        = HtmlExport.render(composition, SwarScript.Devanagari)

      html should include("Taal: Teentaal")
      html should include("16 matras")
    }

    it("should include section names") {
      val composition = testComposition
      val html        = HtmlExport.render(composition, SwarScript.Devanagari)

      html should include("Sthayi")
    }

    it("should include notation color styles") {
      val composition = testComposition
      val html        = HtmlExport.render(composition, SwarScript.Devanagari)

      html should include(".swar-row")
      html should include(".marker-row")
      html should include(".stroke-row")
      html should include(".sahitya-row")
      html should include(".ornament-row")
    }

    it("should contain beat cells") {
      val composition = testComposition
      val html        = HtmlExport.render(composition, SwarScript.Devanagari)

      html should include("beat-cell")
      html should include("grid-line")
    }

    it("should write to file") {
      val composition = testComposition
      val tempFile    = Files.createTempFile("sangeet-test", ".html")
      try
        HtmlExport.exportHtml(composition, tempFile, SwarScript.Devanagari)
        Files.exists(tempFile) shouldBe true
        Files.size(tempFile) should be > 0L

        val content = Files.readString(tempFile)
        content should include("<!DOCTYPE html>")
      finally Files.deleteIfExists(tempFile)
    }

    it("should handle English script") {
      val composition = testComposition
      val html        = HtmlExport.render(composition, SwarScript.English)

      html should include("<!DOCTYPE html>")
      html should include("Test Composition")
    }

    it("should handle multiple sections") {
      val composition = testComposition
      val antara = Section(
        name = "Antara",
        sectionType = SectionType.Antara,
        events = List(
          Event.Swar(
            Note.Pa,
            Variant.Shuddha,
            Octave.Madhya,
            BeatPosition(0, 0, Rational.onBeat),
            Rational.fullBeat,
            None,
            Nil,
            None
          )
        ),
        tihai = None
      )
      val multiSection = composition.copy(sections = composition.sections :+ antara)

      val html = HtmlExport.render(multiSection, SwarScript.Devanagari)
      html should include("Sthayi")
      html should include("Antara")
    }

    it("should escape HTML in metadata") {
      val composition = testComposition.copy(
        metadata = testComposition.metadata.copy(title = "Test <script>alert('xss')</script> Composition")
      )
      val html = HtmlExport.render(composition, SwarScript.Devanagari)

      html should not include "<script>"
      html should include("&lt;script&gt;")
    }
  }
