// src/test/scala/sangeet/format/CompositionCodecSpec.scala
package sangeet.format

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.syntax.*
import io.circe.parser.*
import sangeet.model.*

class CompositionCodecSpec extends AnyFlatSpec with Matchers:
  import Codecs.given

  val sampleComposition: Composition = Composition(
    metadata = Metadata(
      title = "Vilambit Gat in Yaman",
      compositionType = CompositionType.Gat,
      raag = Raag(
        name = "Yaman", thaat = Some("Kalyan"),
        arohana = Some(List("S", "R", "G", "M+", "P", "D", "N", "S'")),
        avarohana = Some(List("S'", "N", "D", "P", "M+", "G", "R", "S")),
        vadi = Some("G"), samvadi = Some("N"), pakad = None, prahar = Some(1)
      ),
      taal = Taal(
        name = "Teentaal", matras = 16,
        vibhags = List(
          Vibhag(4, VibhagMarker.Sam), Vibhag(4, VibhagMarker.Taali(2)),
          Vibhag(4, VibhagMarker.Khali), Vibhag(4, VibhagMarker.Taali(3))
        ),
        theka = Some(List("Dha","Dhin","Dhin","Dha","Dha","Dhin","Dhin","Dha",
                           "Dha","Tin","Tin","Ta","Ta","Dhin","Dhin","Dha"))
      ),
      laya = Some(Laya.Vilambit),
      instrument = Some("Sitar"),
      composer = Some("Traditional"),
      author = Some("Bharadwaj"),
      source = Some("Guruji class"),
      createdAt = "2026-03-28T10:00:00Z",
      updatedAt = "2026-03-28T10:00:00Z"
    ),
    sections = List(
      Section("Sthayi", SectionType.Sthayi, List(
        Event.Swar(Note.Pa, Variant.Shuddha, Octave.Madhya,
          BeatPosition(0, 12, Rational.onBeat), Rational.fullBeat,
          Some(Stroke.Da), Nil, None),
        Event.Swar(Note.Ma, Variant.Tivra, Octave.Madhya,
          BeatPosition(0, 13, Rational.onBeat), Rational(1, 2),
          Some(Stroke.Ra),
          List(Meend(
            NoteRef(Note.Ma, Variant.Tivra, Octave.Madhya),
            NoteRef(Note.Ga, Variant.Shuddha, Octave.Madhya),
            MeendDirection.Descending, Nil
          )),
          None),
        Event.Rest(BeatPosition(0, 14, Rational.onBeat), Rational.fullBeat),
        Event.Sustain(BeatPosition(0, 15, Rational.onBeat), Rational.fullBeat)
      ),
      tihai = Some(Tihai(BeatPosition(2, 8, Rational.onBeat), BeatPosition(3, 0, Rational.onBeat)))
    ))
  )

  "Composition codec" should "roundtrip a full composition" in {
    val json = sampleComposition.asJson
    val decoded = json.as[Composition]
    decoded shouldBe Right(sampleComposition)
  }

  it should "include version field at top level in SwarFormat" in {
    val json = SwarFormat.toJson(sampleComposition)
    json.hcursor.downField("version").as[String] shouldBe Right("1.0")
  }

  it should "roundtrip through SwarFormat" in {
    val json = SwarFormat.toJson(sampleComposition)
    val jsonString = json.spaces2
    val result = SwarFormat.fromJson(jsonString)
    result shouldBe Right(sampleComposition)
  }

  "Ornament codec" should "roundtrip Meend" in {
    val meend: Ornament = Meend(
      NoteRef(Note.Sa, Variant.Shuddha, Octave.Taar),
      NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya),
      MeendDirection.Descending,
      List(NoteRef(Note.Ni, Variant.Shuddha, Octave.Madhya))
    )
    decode[Ornament](meend.asJson.noSpaces) shouldBe Right(meend)
  }

  it should "roundtrip KanSwar" in {
    val kan: Ornament = KanSwar(NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya))
    decode[Ornament](kan.asJson.noSpaces) shouldBe Right(kan)
  }

  it should "roundtrip Gamak" in {
    val g: Ornament = Gamak()
    decode[Ornament](g.asJson.noSpaces) shouldBe Right(g)
  }

  it should "roundtrip CustomOrnament" in {
    val c: Ornament = CustomOrnament("newMove", Map("speed" -> "fast"))
    decode[Ornament](c.asJson.noSpaces) shouldBe Right(c)
  }

  "SectionType codec" should "roundtrip Custom" in {
    val st: SectionType = SectionType.Custom("Tihai Section")
    decode[SectionType](st.asJson.noSpaces) shouldBe Right(st)
  }

  "Palta composition" should "roundtrip with no laya" in {
    val palta = sampleComposition.copy(
      metadata = sampleComposition.metadata.copy(
        compositionType = CompositionType.Palta,
        laya = None
      )
    )
    val json = SwarFormat.toJson(palta)
    val result = SwarFormat.fromJson(json.spaces2)
    result.map(_.metadata.laya) shouldBe Right(None)
    result.map(_.metadata.compositionType) shouldBe Right(CompositionType.Palta)
  }
