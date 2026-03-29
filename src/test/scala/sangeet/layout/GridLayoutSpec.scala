package sangeet.layout

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals

class GridLayoutSpec extends AnyFlatSpec with Matchers:

  val taal = Taals.teentaal

  def swar(beat: Int, note: Note = Note.Sa, cycle: Int = 0): Event.Swar =
    Event.Swar(note, Variant.Shuddha, Octave.Madhya,
      BeatPosition(cycle, beat, Rational.onBeat), Rational.fullBeat,
      Some(Stroke.Da), Nil, None)

  "GridLayout.layout" should "produce a SectionGrid from a Section" in {
    val section = Section("Sthayi", SectionType.Sthayi,
      (0 until 16).toList.map(b => swar(b)))
    val grid = GridLayout.layout(section, taal, LayoutConfig())
    grid.sectionName shouldBe "Sthayi"
    grid.lines should not be empty
  }

  it should "handle an empty section" in {
    val section = Section("Empty", SectionType.Sthayi, Nil)
    val grid = GridLayout.layout(section, taal, LayoutConfig())
    grid.lines shouldBe empty
  }

  it should "handle multi-cycle sections" in {
    val events = (0 to 1).flatMap(c => (0 until 16).map(b => swar(b, cycle = c))).toList
    val section = Section("Sthayi", SectionType.Sthayi, events)
    val grid = GridLayout.layout(section, taal, LayoutConfig())
    grid.lines should have length 2
  }

  it should "preserve section type in the grid" in {
    val section = Section("Antara", SectionType.Antara,
      (0 until 16).toList.map(b => swar(b)))
    val grid = GridLayout.layout(section, taal, LayoutConfig())
    grid.sectionType shouldBe SectionType.Antara
  }

  "GridLayout.layoutAll" should "layout all sections of a composition" in {
    val sections = List(
      Section("Sthayi", SectionType.Sthayi, (0 until 16).toList.map(b => swar(b))),
      Section("Antara", SectionType.Antara, (0 until 16).toList.map(b => swar(b)))
    )
    val raag = Raag("Yaman", Some("Kalyan"),
      Some(List("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni")),
      Some(List("Sa", "Ni", "Dha", "Pa", "Ma", "Ga", "Re")),
      Some("Ga"), Some("Ni"), None, None)
    val metadata = Metadata(
      title = "Test Gat",
      compositionType = CompositionType.Gat,
      raag = raag,
      taal = taal,
      laya = Some(Laya.Madhya),
      instrument = Some("Sitar"),
      composer = None,
      author = None,
      source = None,
      createdAt = "2026-03-28",
      updatedAt = "2026-03-28"
    )
    val composition = Composition(metadata, sections)
    val grids = GridLayout.layoutAll(composition, LayoutConfig())
    grids should have length 2
    grids.head.sectionName shouldBe "Sthayi"
    grids(1).sectionName shouldBe "Antara"
  }
