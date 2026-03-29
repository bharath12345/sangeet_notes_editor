package sangeet.format

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals
import java.nio.file.{Files, Path}

class PdfExportSpec extends AnyFlatSpec with Matchers:

  val comp = Composition(
    metadata = Metadata(
      title = "Test Composition",
      compositionType = CompositionType.Gat,
      raag = Raag("Yaman", Some("Kalyan"),
        Some(List("S", "R", "G", "M+", "P", "D", "N", "S'")),
        Some(List("S'", "N", "D", "P", "M+", "G", "R", "S")),
        Some("G"), Some("N"), None, Some(1)),
      taal = Taals.teentaal,
      laya = Some(Laya.Vilambit),
      instrument = Some("Sitar"),
      composer = None, author = None, source = None,
      createdAt = "2026-03-28T10:00:00Z",
      updatedAt = "2026-03-28T10:00:00Z"),
    sections = List(
      Section("Sthayi", SectionType.Sthayi, List(
        Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya,
          BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat, Some(Stroke.Da), Nil, None)
      ))))

  "PdfExport" should "create a PDF file" in {
    val tmpPath = Files.createTempFile("sangeet-test-", ".pdf")
    try
      PdfExport.exportPdf(comp, tmpPath)
      Files.exists(tmpPath) shouldBe true
      Files.size(tmpPath) should be > 0L
    finally
      Files.deleteIfExists(tmpPath)
  }

  it should "export the sample composition with all ornaments and mixed text" in {
    val sample = sangeet.editor.SampleComposition.build()
    val tmpPath = Files.createTempFile("sangeet-sample-", ".pdf")
    try
      PdfExport.exportPdf(sample, tmpPath)
      Files.exists(tmpPath) shouldBe true
      Files.size(tmpPath) should be > 0L
    finally
      Files.deleteIfExists(tmpPath)
  }
