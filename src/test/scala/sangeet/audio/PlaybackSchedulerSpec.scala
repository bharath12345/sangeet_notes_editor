package sangeet.audio

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*

class PlaybackSchedulerSpec extends AnyFlatSpec with Matchers:

  def swar(beat: Int, note: Note = Note.Sa): Event.Swar =
    Event.Swar(note, Variant.Shuddha, Octave.Madhya,
      BeatPosition(0, beat, Rational.onBeat), Rational.fullBeat, None, Nil, None)

  "PlaybackScheduler.schedule" should "convert events to timed notes" in {
    val events = List(swar(0, Note.Sa), swar(1, Note.Re), swar(2, Note.Ga))
    val bpm = 60.0
    val timedNotes = PlaybackScheduler.schedule(events, bpm, 16)
    timedNotes should have length 3
    timedNotes(0).timeMs shouldBe 0L
    timedNotes(1).timeMs shouldBe 1000L
    timedNotes(2).timeMs shouldBe 2000L
  }

  it should "handle sub-beat events" in {
    val events = List(
      swar(0, Note.Sa).copy(beat = BeatPosition(0, 0, Rational.onBeat)),
      swar(0, Note.Re).copy(beat = BeatPosition(0, 0, Rational(1, 2)))
    )
    val timedNotes = PlaybackScheduler.schedule(events, 60.0, 16)
    timedNotes(0).timeMs shouldBe 0L
    timedNotes(1).timeMs shouldBe 500L
  }

  it should "skip rest events (no sound)" in {
    val events = List(
      swar(0),
      Event.Rest(BeatPosition(0, 1, Rational.onBeat), Rational.fullBeat),
      swar(2)
    )
    val timedNotes = PlaybackScheduler.schedule(events, 60.0, 16)
    timedNotes should have length 2
  }

  it should "use matras for cycle offset" in {
    val events = List(
      swar(0, Note.Sa).copy(beat = BeatPosition(1, 0, Rational.onBeat))
    )
    val timedNotes = PlaybackScheduler.schedule(events, 60.0, 7)
    timedNotes(0).timeMs shouldBe 7000L // cycle 1 * 7 matras * 1000ms
  }

  it should "produce empty list for empty events" in {
    PlaybackScheduler.schedule(Nil, 60.0, 16) shouldBe empty
  }

  it should "skip sustain events" in {
    val events = List(
      swar(0),
      Event.Sustain(BeatPosition(0, 1, Rational.onBeat), Rational.fullBeat)
    )
    val timedNotes = PlaybackScheduler.schedule(events, 60.0, 16)
    timedNotes should have length 1
  }

  it should "compute duration from event duration" in {
    val event = swar(0).copy(duration = Rational(1, 2))
    val timedNotes = PlaybackScheduler.schedule(List(event), 60.0, 16)
    timedNotes.head.durationMs shouldBe 500L
  }

  it should "preserve stroke information" in {
    val event = swar(0).copy(stroke = Some(Stroke.Da))
    val timedNotes = PlaybackScheduler.schedule(List(event), 60.0, 16)
    timedNotes.head.stroke shouldBe Some(Stroke.Da)
  }

  it should "preserve variant and octave" in {
    val event = Event.Swar(Note.Re, Variant.Komal, Octave.Taar,
      BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat, None, Nil, None)
    val timedNotes = PlaybackScheduler.schedule(List(event), 60.0, 16)
    timedNotes.head.variant shouldBe Variant.Komal
    timedNotes.head.octave shouldBe Octave.Taar
  }

  it should "compute correct timing at 120 BPM" in {
    val events = List(swar(0), swar(1))
    val timedNotes = PlaybackScheduler.schedule(events, 120.0, 16)
    timedNotes(0).timeMs shouldBe 0L
    timedNotes(1).timeMs shouldBe 500L
  }
