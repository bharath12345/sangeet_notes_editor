package sangeet.audio

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*

class MidiEngineSpec extends AnyFlatSpec with Matchers:

  private val engine = new MidiEngine()

  "toMidiNote" should "map Sa Madhya to MIDI 60 (middle C)" in {
    engine.toMidiNote(Note.Sa, Variant.Shuddha, Octave.Madhya) shouldBe 60
  }

  it should "map Re Shuddha Madhya to MIDI 62" in {
    engine.toMidiNote(Note.Re, Variant.Shuddha, Octave.Madhya) shouldBe 62
  }

  it should "map Re Komal Madhya to MIDI 61" in {
    engine.toMidiNote(Note.Re, Variant.Komal, Octave.Madhya) shouldBe 61
  }

  it should "map Ma Tivra Madhya to MIDI 66" in {
    engine.toMidiNote(Note.Ma, Variant.Tivra, Octave.Madhya) shouldBe 66
  }

  it should "map Pa Madhya to MIDI 67" in {
    engine.toMidiNote(Note.Pa, Variant.Shuddha, Octave.Madhya) shouldBe 67
  }

  it should "map Ni Shuddha Madhya to MIDI 71" in {
    engine.toMidiNote(Note.Ni, Variant.Shuddha, Octave.Madhya) shouldBe 71
  }

  it should "shift down 12 for Mandra octave" in {
    engine.toMidiNote(Note.Sa, Variant.Shuddha, Octave.Mandra) shouldBe 48
  }

  it should "shift up 12 for Taar octave" in {
    engine.toMidiNote(Note.Sa, Variant.Shuddha, Octave.Taar) shouldBe 72
  }

  it should "shift down 24 for AtiMandra octave" in {
    engine.toMidiNote(Note.Sa, Variant.Shuddha, Octave.AtiMandra) shouldBe 36
  }

  it should "shift up 24 for AtiTaar octave" in {
    engine.toMidiNote(Note.Sa, Variant.Shuddha, Octave.AtiTaar) shouldBe 84
  }

  it should "handle Ga Komal correctly" in {
    // Ga base = 4, komal = -1 → 3 semitones above Sa
    engine.toMidiNote(Note.Ga, Variant.Komal, Octave.Madhya) shouldBe 63
  }

  it should "handle Dha Komal correctly" in {
    // Dha base = 9, komal = -1 → 8 semitones above Sa
    engine.toMidiNote(Note.Dha, Variant.Komal, Octave.Madhya) shouldBe 68
  }

  it should "handle Ni Komal correctly" in {
    // Ni base = 11, komal = -1 → 10 semitones above Sa
    engine.toMidiNote(Note.Ni, Variant.Komal, Octave.Madhya) shouldBe 70
  }

  it should "map all 12 chromatic notes correctly" in {
    // Sa=0, Re(k)=1, Re=2, Ga(k)=3, Ga=4, Ma=5, Ma(t)=6, Pa=7, Dha(k)=8, Dha=9, Ni(k)=10, Ni=11
    val expected = List(
      (Note.Sa, Variant.Shuddha, 60),
      (Note.Re, Variant.Komal, 61),
      (Note.Re, Variant.Shuddha, 62),
      (Note.Ga, Variant.Komal, 63),
      (Note.Ga, Variant.Shuddha, 64),
      (Note.Ma, Variant.Shuddha, 65),
      (Note.Ma, Variant.Tivra, 66),
      (Note.Pa, Variant.Shuddha, 67),
      (Note.Dha, Variant.Komal, 68),
      (Note.Dha, Variant.Shuddha, 69),
      (Note.Ni, Variant.Komal, 70),
      (Note.Ni, Variant.Shuddha, 71),
    )
    expected.foreach { (note, variant, midi) =>
      engine.toMidiNote(note, variant, Octave.Madhya) shouldBe midi
    }
  }
