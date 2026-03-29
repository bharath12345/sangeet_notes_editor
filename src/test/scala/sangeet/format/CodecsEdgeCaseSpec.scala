package sangeet.format

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import sangeet.model.*
import Codecs.given

class CodecsEdgeCaseSpec extends AnyFlatSpec with Matchers:

  "Note codec" should "reject invalid values" in {
    val result = decode[Note]("\"invalid\"")
    result.isLeft shouldBe true
  }

  "Variant codec" should "reject invalid values" in {
    val result = decode[Variant]("\"sharp\"")
    result.isLeft shouldBe true
  }

  "Octave codec" should "reject invalid values" in {
    val result = decode[Octave]("\"super_high\"")
    result.isLeft shouldBe true
  }

  "Rational codec" should "handle zero numerator" in {
    val r = Rational(0, 1)
    val json = r.asJson
    decode[Rational](json.noSpaces) shouldBe Right(r)
  }

  it should "reject non-array JSON" in {
    val result = decode[Rational]("42")
    result.isLeft shouldBe true
  }

  it should "reject array with wrong size" in {
    val result = decode[Rational]("[1]")
    result.isLeft shouldBe true
  }

  "BeatPosition codec" should "roundtrip with subdivision" in {
    val bp = BeatPosition(3, 14, Rational(3, 8))
    val json = bp.asJson
    decode[BeatPosition](json.noSpaces) shouldBe Right(bp)
  }

  "Event codec" should "roundtrip Swar with all fields" in {
    val swar = Event.Swar(
      Note.Ma, Variant.Tivra, Octave.Taar,
      BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat,
      Some(Stroke.Da),
      List(Gamak(), KanSwar(NoteRef(Note.Pa, Variant.Shuddha, Octave.Madhya))),
      Some("test lyrics")
    )
    val json = swar.asJson
    decode[Event](json.noSpaces) shouldBe Right(swar)
  }

  it should "roundtrip Swar with no optional fields" in {
    val swar = Event.Swar(
      Note.Sa, Variant.Shuddha, Octave.Madhya,
      BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat,
      None, Nil, None
    )
    val json = swar.asJson
    decode[Event](json.noSpaces) shouldBe Right(swar)
  }

  it should "roundtrip Rest" in {
    val rest = Event.Rest(BeatPosition(1, 5, Rational.onBeat), Rational.fullBeat)
    val json = rest.asJson
    decode[Event](json.noSpaces) shouldBe Right(rest)
  }

  it should "roundtrip Sustain" in {
    val sustain = Event.Sustain(BeatPosition(0, 3, Rational(1, 2)), Rational(1, 2))
    val json = sustain.asJson
    decode[Event](json.noSpaces) shouldBe Right(sustain)
  }

  "Ornament codec" should "roundtrip all ornament types" in {
    val noteRef = NoteRef(Note.Re, Variant.Komal, Octave.Madhya)
    val ornaments: List[Ornament] = List(
      Gamak(),
      Andolan(),
      Gitkari(),
      KanSwar(noteRef),
      Sparsh(noteRef),
      Ghaseet(noteRef),
      Meend(noteRef, NoteRef(Note.Pa, Variant.Shuddha, Octave.Madhya), MeendDirection.Ascending, Nil),
      Meend(noteRef, NoteRef(Note.Sa, Variant.Shuddha, Octave.Mandra), MeendDirection.Descending,
        List(NoteRef(Note.Ga, Variant.Shuddha, Octave.Madhya))),
      Krintan(List(noteRef, NoteRef(Note.Sa, Variant.Shuddha, Octave.Madhya))),
      Murki(List(noteRef, NoteRef(Note.Ga, Variant.Shuddha, Octave.Madhya), noteRef)),
      Zamzama(List(noteRef, noteRef, noteRef)),
      CustomOrnament("test", Map("speed" -> "fast", "intensity" -> "3"))
    )
    ornaments.foreach { orn =>
      val json = orn.asJson
      val decoded = decode[Ornament](json.noSpaces)
      decoded shouldBe Right(orn)
    }
  }

  "Metadata decoder" should "handle missing optional showStrokeLine/showSahityaLine" in {
    // Simulate a v1.0 file that doesn't have these fields
    val json = """{"title":"Test","compositionType":"gat","raag":{"name":"Yaman"},"taal":{"name":"Teentaal","matras":16,"vibhags":[{"beats":4,"marker":"sam"},{"beats":4,"marker":{"taali":2}},{"beats":4,"marker":"khali"},{"beats":4,"marker":{"taali":3}}]},"createdAt":"2026-01-01","updatedAt":"2026-01-01"}"""
    val result = decode[Metadata](json)
    result.isRight shouldBe true
    result.toOption.get.showStrokeLine shouldBe false
    result.toOption.get.showSahityaLine shouldBe false
  }

  "Laya codec" should "roundtrip all values" in {
    Laya.values.foreach { laya =>
      val json = laya.asJson
      decode[Laya](json.noSpaces) shouldBe Right(laya)
    }
  }

  "SectionType codec" should "roundtrip all standard types" in {
    val types = List(SectionType.Sthayi, SectionType.Antara, SectionType.Sanchari,
      SectionType.Abhog, SectionType.Taan, SectionType.Toda, SectionType.Jhala,
      SectionType.Palta, SectionType.Arohi, SectionType.Avarohi)
    types.foreach { st =>
      val json = st.asJson
      decode[SectionType](json.noSpaces) shouldBe Right(st)
    }
  }

  it should "roundtrip Custom section type" in {
    val custom = SectionType.Custom("My Section")
    val json = custom.asJson
    decode[SectionType](json.noSpaces) shouldBe Right(custom)
  }
