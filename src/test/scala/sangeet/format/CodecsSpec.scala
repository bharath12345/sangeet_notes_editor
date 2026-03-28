package sangeet.format

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.syntax.*
import io.circe.parser.*
import sangeet.model.*

class CodecsSpec extends AnyFlatSpec with Matchers:
  import Codecs.given

  // --- Note enum ---
  "Note codec" should "serialize to lowercase string" in {
    Note.Sa.asJson.noSpaces shouldBe "\"sa\""
    Note.Dha.asJson.noSpaces shouldBe "\"dha\""
  }

  it should "deserialize from lowercase string" in {
    decode[Note]("\"sa\"") shouldBe Right(Note.Sa)
    decode[Note]("\"ni\"") shouldBe Right(Note.Ni)
  }

  // --- Variant enum ---
  "Variant codec" should "roundtrip" in {
    val v = Variant.Komal
    decode[Variant](v.asJson.noSpaces) shouldBe Right(v)
  }

  // --- Octave enum ---
  "Octave codec" should "serialize to lowercase" in {
    Octave.Madhya.asJson.noSpaces shouldBe "\"madhya\""
    Octave.Taar.asJson.noSpaces shouldBe "\"taar\""
  }

  // --- Stroke enum ---
  "Stroke codec" should "roundtrip" in {
    val s = Stroke.Da
    decode[Stroke](s.asJson.noSpaces) shouldBe Right(s)
  }

  // --- Rational ---
  "Rational codec" should "serialize as [num, den] array" in {
    Rational(1, 2).asJson.noSpaces shouldBe "[1,2]"
    Rational(0, 1).asJson.noSpaces shouldBe "[0,1]"
  }

  it should "deserialize from array" in {
    decode[Rational]("[1,4]") shouldBe Right(Rational(1, 4))
  }

  // --- BeatPosition ---
  "BeatPosition codec" should "roundtrip" in {
    val bp = BeatPosition(2, 5, Rational(1, 3))
    decode[BeatPosition](bp.asJson.noSpaces) shouldBe Right(bp)
  }

  // --- NoteRef ---
  "NoteRef codec" should "roundtrip" in {
    val nr = NoteRef(Note.Ga, Variant.Komal, Octave.Mandra)
    decode[NoteRef](nr.asJson.noSpaces) shouldBe Right(nr)
  }

  // --- Laya ---
  "Laya codec" should "serialize to lowercase" in {
    Laya.Vilambit.asJson.noSpaces shouldBe "\"vilambit\""
    Laya.AtiVilambit.asJson.noSpaces shouldBe "\"atiVilambit\""
  }

  // --- MeendDirection ---
  "MeendDirection codec" should "roundtrip" in {
    val d = MeendDirection.Descending
    decode[MeendDirection](d.asJson.noSpaces) shouldBe Right(d)
  }

  // --- VibhagMarker ---
  "VibhagMarker codec" should "serialize Sam as string" in {
    val sam: VibhagMarker = VibhagMarker.Sam
    sam.asJson.noSpaces shouldBe "\"sam\""
  }

  it should "serialize Khali as string" in {
    val khali: VibhagMarker = VibhagMarker.Khali
    khali.asJson.noSpaces shouldBe "\"khali\""
  }

  it should "serialize Taali as object" in {
    val taali: VibhagMarker = VibhagMarker.Taali(2)
    taali.asJson.noSpaces shouldBe """{"taali":2}"""
  }

  it should "roundtrip all variants" in {
    val markers = List(VibhagMarker.Sam, VibhagMarker.Khali, VibhagMarker.Taali(3))
    markers.foreach { m =>
      decode[VibhagMarker](m.asJson.noSpaces) shouldBe Right(m)
    }
  }

  // --- CompositionType ---
  "CompositionType codec" should "serialize simple variants as strings" in {
    (CompositionType.Gat: CompositionType).asJson.noSpaces shouldBe "\"gat\""
  }

  it should "serialize Custom with name" in {
    val ct: CompositionType = CompositionType.Custom("Alap")
    ct.asJson.noSpaces shouldBe """{"custom":"Alap"}"""
  }

  it should "roundtrip" in {
    val types: List[CompositionType] = List(
      CompositionType.Bandish, CompositionType.Gat,
      CompositionType.Palta, CompositionType.Custom("Alap")
    )
    types.foreach { t =>
      decode[CompositionType](t.asJson.noSpaces) shouldBe Right(t)
    }
  }
