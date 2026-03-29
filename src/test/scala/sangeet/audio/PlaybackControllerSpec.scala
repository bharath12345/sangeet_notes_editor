package sangeet.audio

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*

class PlaybackControllerSpec extends AnyFlatSpec with Matchers:

  /** A mock SoundEngine that records calls for verification */
  private class MockSoundEngine extends SoundEngine:
    var inited = false
    var stopped = false
    var shutdownCalled = false
    var notesPlayed: List[TimedNote] = Nil
    var notesOff: List[Int] = Nil

    override def init(): Unit =
      inited = true
      stopped = false

    override def playNote(note: TimedNote): Unit =
      notesPlayed = notesPlayed :+ note

    override def noteOff(midiNote: Int): Unit =
      notesOff = notesOff :+ midiNote

    override def stop(): Unit =
      stopped = true

    override def shutdown(): Unit =
      shutdownCalled = true

  private def sampleEvents: List[Event] =
    List(
      Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya,
        BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat, None, Nil, None),
      Event.Swar(Note.Re, Variant.Shuddha, Octave.Madhya,
        BeatPosition(0, 1, Rational.onBeat), Rational.fullBeat, None, Nil, None),
      Event.Rest(BeatPosition(0, 2, Rational.onBeat), Rational.fullBeat),
      Event.Swar(Note.Ga, Variant.Shuddha, Octave.Madhya,
        BeatPosition(0, 3, Rational.onBeat), Rational.fullBeat, None, Nil, None),
    )

  "PlaybackController" should "not be playing initially" in {
    val engine = new MockSoundEngine()
    val controller = new PlaybackController(engine)
    controller.isPlaying shouldBe false
  }

  it should "init engine and set playing on play" in {
    val engine = new MockSoundEngine()
    val controller = new PlaybackController(engine)
    controller.play(sampleEvents, 60.0, 16)
    engine.inited shouldBe true
    controller.isPlaying shouldBe true
    controller.shutdown()
  }

  it should "stop playback" in {
    val engine = new MockSoundEngine()
    val controller = new PlaybackController(engine)
    controller.play(sampleEvents, 60.0, 16)
    controller.stop()
    controller.isPlaying shouldBe false
    engine.stopped shouldBe true
    controller.shutdown()
  }

  it should "shutdown engine" in {
    val engine = new MockSoundEngine()
    val controller = new PlaybackController(engine)
    controller.shutdown()
    engine.shutdownCalled shouldBe true
  }

  it should "reinit engine on subsequent play calls" in {
    val engine = new MockSoundEngine()
    val controller = new PlaybackController(engine)
    controller.play(sampleEvents, 60.0, 16)
    controller.stop()
    engine.inited = false
    controller.play(sampleEvents, 120.0, 16)
    engine.inited shouldBe true
    controller.shutdown()
  }

  it should "stop previous playback before starting new" in {
    val engine = new MockSoundEngine()
    var stopCount = 0
    val trackingEngine = new MockSoundEngine():
      override def stop(): Unit =
        super.stop()
        stopCount += 1
    val controller = new PlaybackController(trackingEngine)
    controller.play(sampleEvents, 60.0, 16)
    controller.play(sampleEvents, 120.0, 16)
    // stop() called once when restarting (from first play being stopped)
    stopCount shouldBe 1
    controller.isPlaying shouldBe true
    controller.shutdown()
  }
