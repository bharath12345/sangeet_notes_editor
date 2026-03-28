package sangeet.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NoteSpec extends AnyFlatSpec with Matchers:

  "Note" should "have 7 values" in {
    Note.values should have length 7
  }

  "Variant" should "have Shuddha, Komal, Tivra" in {
    Variant.values should contain allOf (Variant.Shuddha, Variant.Komal, Variant.Tivra)
  }

  "Octave" should "have 5 values in order" in {
    Octave.values.toList shouldBe List(
      Octave.AtiMandra, Octave.Mandra, Octave.Madhya, Octave.Taar, Octave.AtiTaar
    )
  }

  "Stroke" should "have Da, Ra, Chikari, Jod" in {
    Stroke.values should have length 4
  }

  "BeatPosition" should "represent a position in a taal cycle" in {
    val pos = BeatPosition(cycle = 0, beat = 3, subdivision = Rational(1, 2))
    pos.cycle shouldBe 0
    pos.beat shouldBe 3
    pos.subdivision shouldBe Rational(1, 2)
  }

  it should "order positions correctly" in {
    val pos1 = BeatPosition(0, 0, Rational.onBeat)
    val pos2 = BeatPosition(0, 0, Rational(1, 2))
    val pos3 = BeatPosition(0, 1, Rational.onBeat)
    val pos4 = BeatPosition(1, 0, Rational.onBeat)

    List(pos3, pos1, pos4, pos2).sorted shouldBe List(pos1, pos2, pos3, pos4)
  }
