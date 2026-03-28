package sangeet.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CompositionSpec extends AnyFlatSpec with Matchers:

  val teentaal = Taal(
    name = "Teentaal",
    matras = 16,
    vibhags = List(
      Vibhag(4, VibhagMarker.Sam),
      Vibhag(4, VibhagMarker.Taali(2)),
      Vibhag(4, VibhagMarker.Khali),
      Vibhag(4, VibhagMarker.Taali(3))
    ),
    theka = Some(List("Dha","Dhin","Dhin","Dha","Dha","Dhin","Dhin","Dha",
                       "Dha","Tin","Tin","Ta","Ta","Dhin","Dhin","Dha"))
  )

  val yaman = Raag(
    name = "Yaman",
    thaat = Some("Kalyan"),
    arohana = Some(List("S", "R", "G", "M+", "P", "D", "N", "S'")),
    avarohana = Some(List("S'", "N", "D", "P", "M+", "G", "R", "S")),
    vadi = Some("G"),
    samvadi = Some("N"),
    pakad = None,
    prahar = Some(1)
  )

  "Taal" should "compute total matras from vibhags" in {
    teentaal.matras shouldBe 16
    teentaal.vibhags.map(_.beats).sum shouldBe 16
  }

  "Event.Swar" should "hold all note properties" in {
    val swar: Event.Swar = Event.Swar(
      note = Note.Sa,
      variant = Variant.Shuddha,
      octave = Octave.Madhya,
      beat = BeatPosition(0, 0, Rational.onBeat),
      duration = Rational.fullBeat,
      stroke = Some(Stroke.Da),
      ornaments = Nil,
      sahitya = None
    )
    swar.note shouldBe Note.Sa
    swar.stroke shouldBe Some(Stroke.Da)
  }

  "Event.Rest" should "represent silence" in {
    val rest: Event.Rest = Event.Rest(
      beat = BeatPosition(0, 1, Rational.onBeat),
      duration = Rational.fullBeat
    )
    rest.beat.beat shouldBe 1
  }

  "Section" should "hold events with a section type" in {
    val section = Section(
      name = "Sthayi",
      sectionType = SectionType.Sthayi,
      events = List(
        Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya,
          BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat, None, Nil, None)
      )
    )
    section.events should have length 1
    section.sectionType shouldBe SectionType.Sthayi
  }

  "Composition" should "hold metadata, sections, and tihais" in {
    val comp = Composition(
      metadata = Metadata(
        title = "Vilambit Gat in Yaman",
        compositionType = CompositionType.Gat,
        raag = yaman,
        taal = teentaal,
        laya = Some(Laya.Vilambit),
        instrument = Some("Sitar"),
        composer = Some("Traditional"),
        author = Some("Bharadwaj"),
        source = Some("Guruji class"),
        createdAt = "2026-03-28T10:00:00Z",
        updatedAt = "2026-03-28T10:00:00Z"
      ),
      sections = List(
        Section("Sthayi", SectionType.Sthayi, Nil)
      ),
      tihais = Nil
    )
    comp.metadata.title shouldBe "Vilambit Gat in Yaman"
    comp.metadata.laya shouldBe Some(Laya.Vilambit)
    comp.sections should have length 1
  }

  "CompositionType.Palta" should "exist" in {
    CompositionType.Palta shouldBe CompositionType.Palta
  }

  "CompositionType.Custom" should "hold a name" in {
    val ct: CompositionType.Custom = CompositionType.Custom("Alap")
    ct.name shouldBe "Alap"
  }

  "SectionType" should "include Palta, Arohi, Avarohi" in {
    SectionType.Palta shouldBe SectionType.Palta
    SectionType.Arohi shouldBe SectionType.Arohi
    SectionType.Avarohi shouldBe SectionType.Avarohi
  }
