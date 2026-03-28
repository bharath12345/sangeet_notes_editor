package sangeet

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.format.{SwarFormat, Codecs}
import sangeet.layout.*
import sangeet.taal.Taals
import java.nio.file.{Files, Path}

class IntegrationSpec extends AnyFlatSpec with Matchers:
  import Codecs.given

  def buildComposition(): Composition =
    Composition(
      metadata = Metadata(
        title = "Integration Test Gat",
        compositionType = CompositionType.Gat,
        raag = Raag("Yaman", Some("Kalyan"),
          Some(List("S","R","G","M+","P","D","N","S'")),
          Some(List("S'","N","D","P","M+","G","R","S")),
          Some("G"), Some("N"), None, Some(1)),
        taal = Taals.teentaal,
        laya = Some(Laya.Vilambit),
        instrument = Some("Sitar"),
        composer = Some("Traditional"),
        author = Some("Test"),
        source = None,
        createdAt = "2026-03-28T10:00:00Z",
        updatedAt = "2026-03-28T10:00:00Z"),
      sections = List(
        Section("Sthayi", SectionType.Sthayi,
          (0 until 16).toList.map { beat =>
            Event.Swar(Note.values(beat % 7), Variant.Shuddha, Octave.Madhya,
              BeatPosition(0, beat, Rational.onBeat), Rational.fullBeat,
              Some(if beat % 2 == 0 then Stroke.Da else Stroke.Ra),
              if beat == 3 then List(Gamak()) else Nil,
              None)
          }
        ),
        Section("Antara", SectionType.Antara,
          (0 until 16).toList.map { beat =>
            Event.Swar(Note.values(beat % 7), Variant.Shuddha, Octave.Taar,
              BeatPosition(0, beat, Rational.onBeat), Rational.fullBeat,
              Some(Stroke.Da), Nil, None)
          }
        )
      ),
      tihais = List(
        Tihai("Sthayi", BeatPosition(0, 10, Rational.onBeat),
              BeatPosition(1, 0, Rational.onBeat))
      )
    )

  "Full pipeline" should "roundtrip composition through .swar file" in {
    val comp = buildComposition()
    val tmpFile = Files.createTempFile("sangeet-integration-", ".swar")
    try
      SwarFormat.writeFile(tmpFile, comp)
      val loaded = SwarFormat.readFile(tmpFile)
      loaded shouldBe Right(comp)
    finally
      Files.deleteIfExists(tmpFile)
  }

  it should "layout composition into grids" in {
    val comp = buildComposition()
    val grids = GridLayout.layoutAll(comp, LayoutConfig())
    grids should have length 2
    grids.head.sectionName shouldBe "Sthayi"
    grids.head.lines should not be empty
  }

  it should "schedule playback events" in {
    val comp = buildComposition()
    val events = comp.sections.flatMap(_.events)
    val timedNotes = sangeet.audio.PlaybackScheduler.schedule(
      events, 60.0, comp.metadata.taal.matras)
    timedNotes should not be empty
    timedNotes.head.timeMs shouldBe 0L
  }

  it should "export to PDF" in {
    val comp = buildComposition()
    val tmpPdf = Files.createTempFile("sangeet-integration-", ".pdf")
    try
      sangeet.format.PdfExport.exportPdf(comp, tmpPdf)
      Files.size(tmpPdf) should be > 0L
    finally
      Files.deleteIfExists(tmpPdf)
  }

  "Palta composition" should "roundtrip with no laya" in {
    val palta = buildComposition().copy(
      metadata = buildComposition().metadata.copy(
        title = "Yaman Palta 1",
        compositionType = CompositionType.Palta,
        laya = None
      ),
      sections = List(Section("Palta", SectionType.Palta, Nil))
    )
    val tmpFile = Files.createTempFile("sangeet-palta-", ".swar")
    try
      SwarFormat.writeFile(tmpFile, palta)
      val loaded = SwarFormat.readFile(tmpFile)
      loaded.map(_.metadata.compositionType) shouldBe Right(CompositionType.Palta)
      loaded.map(_.metadata.laya) shouldBe Right(None)
    finally
      Files.deleteIfExists(tmpFile)
  }
