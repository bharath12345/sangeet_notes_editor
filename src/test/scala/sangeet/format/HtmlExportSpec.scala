package sangeet.format

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals
import sangeet.render.DevanagariMap
import java.nio.file.{Files, Path}

class HtmlExportSpec extends AnyFlatSpec with Matchers:

  val comp = Composition(
    metadata = Metadata(
      title = "Test Composition",
      compositionType = CompositionType.Gat,
      raag = Raag("Yaman", Some("Kalyan"),
        Some(List("Sa", "Re", "Ga", "Ma+", "Pa", "Dha", "Ni", "Sa'")),
        Some(List("Sa'", "Ni", "Dha", "Pa", "Ma+", "Ga", "Re", "Sa")),
        Some("Ga"), Some("Ni"), None, Some(1)),
      taal = Taals.teentaal,
      laya = Some(Laya.Vilambit),
      instrument = Some("Sitar"),
      composer = None, author = None, source = None,
      createdAt = "2026-03-28T10:00:00Z",
      updatedAt = "2026-03-28T10:00:00Z",
      showStrokeLine = true),
    sections = List(
      Section("Sthayi", SectionType.Sthayi, List(
        Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya,
          BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat, Some(Stroke.Da), Nil, None),
        Event.Rest(BeatPosition(0, 1, Rational.onBeat), Rational.fullBeat),
        Event.Swar(Note.Ga, Variant.Shuddha, Octave.Madhya,
          BeatPosition(0, 2, Rational.onBeat), Rational.fullBeat, None, Nil, None),
        Event.Sustain(BeatPosition(0, 3, Rational.onBeat), Rational.fullBeat)
      ))))

  "HtmlExport" should "create an HTML file" in {
    val tmpPath = Files.createTempFile("sangeet-test-", ".html")
    try
      HtmlExport.exportHtml(comp, tmpPath)
      Files.exists(tmpPath) shouldBe true
      Files.size(tmpPath) should be > 0L
    finally
      Files.deleteIfExists(tmpPath)
  }

  "HtmlExport.render" should "produce valid HTML with doctype" in {
    val html = HtmlExport.render(comp)
    html should startWith ("<!DOCTYPE html>")
    html should include ("</html>")
  }

  it should "include the composition title" in {
    val html = HtmlExport.render(comp)
    html should include ("Test Composition")
  }

  it should "include raag info" in {
    val html = HtmlExport.render(comp)
    html should include ("Yaman")
    html should include ("Kalyan")
  }

  it should "include arohan and avrohan" in {
    val html = HtmlExport.render(comp)
    html should include ("Arohan:")
    html should include ("Avrohan:")
  }

  it should "include vadi and samvadi" in {
    val html = HtmlExport.render(comp)
    html should include ("Vadi: Ga")
    html should include ("Samvadi: Ni")
  }

  it should "include taal info" in {
    val html = HtmlExport.render(comp)
    html should include ("Taal: Teentaal")
    html should include ("16 matras")
  }

  it should "include laya" in {
    val html = HtmlExport.render(comp)
    html should include ("Laya:")
    html should include ("Vilambit")
  }

  it should "include composition type" in {
    val html = HtmlExport.render(comp)
    html should include ("Type: Gat")
  }

  it should "include section name" in {
    val html = HtmlExport.render(comp)
    html should include ("Sthayi")
  }

  it should "include rest symbol" in {
    val html = HtmlExport.render(comp)
    html should include ("rest")
    html should include ("-")
  }

  it should "include sustain symbol" in {
    val html = HtmlExport.render(comp)
    html should include ("sustain")
  }

  it should "include stroke info" in {
    val html = HtmlExport.render(comp)
    // Stroke text depends on current script — check for the Devanagari or English version
    val daText = sangeet.render.DevanagariMap.strokeText(sangeet.model.Stroke.Da)
    html should include (daText)
  }

  it should "include footer" in {
    val html = HtmlExport.render(comp)
    html should include ("Sangeet Notes Editor")
  }

  it should "escape HTML special characters in title" in {
    val compWithSpecial = comp.copy(
      metadata = comp.metadata.copy(title = "Test <script>alert('xss')</script>"))
    val html = HtmlExport.render(compWithSpecial)
    html should not include "<script>"
    html should include ("&lt;script&gt;")
  }

  it should "render swar with komal class" in {
    val compWithKomal = comp.copy(sections = List(
      Section("Sthayi", SectionType.Sthayi, List(
        Event.Swar(Note.Re, Variant.Komal, Octave.Madhya,
          BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat, None, Nil, None)
      ))))
    val html = HtmlExport.render(compWithKomal)
    html should include ("komal")
  }

  it should "render swar with tivra class" in {
    val compWithTivra = comp.copy(sections = List(
      Section("Sthayi", SectionType.Sthayi, List(
        Event.Swar(Note.Ma, Variant.Tivra, Octave.Madhya,
          BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat, None, Nil, None)
      ))))
    val html = HtmlExport.render(compWithTivra)
    html should include ("tivra")
  }

  it should "handle empty sections" in {
    val emptyComp = comp.copy(sections = List(
      Section("Antara", SectionType.Antara, Nil)))
    val html = HtmlExport.render(emptyComp)
    html should include ("Antara")
  }

  it should "handle composition with no laya" in {
    val noLaya = comp.copy(metadata = comp.metadata.copy(laya = None))
    val html = HtmlExport.render(noLaya)
    html should not include "Laya:"
  }

  it should "include print-friendly CSS" in {
    val html = HtmlExport.render(comp)
    html should include ("@media print")
  }
