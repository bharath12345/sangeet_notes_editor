package sangeet.audio

import sangeet.model.*
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

class PlaybackController(engine: SoundEngine):
  private var executor: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
  private var playing = false

  def play(events: List[Event], bpm: Double, matras: Int): Unit =
    if playing then stop()
    engine.init()
    playing = true

    val timedNotes = PlaybackScheduler.schedule(events, bpm, matras)
    timedNotes.foreach { tn =>
      executor.schedule(
        new Runnable { def run(): Unit =
          if playing then engine.playNote(tn)
        },
        tn.timeMs,
        TimeUnit.MILLISECONDS
      )
      executor.schedule(
        new Runnable { def run(): Unit =
          if playing then
            val midi = engine match
              case m: MidiEngine => m.toMidiNote(tn.note, tn.variant, tn.octave)
              case _ => 60
            engine.noteOff(midi)
        },
        tn.timeMs + tn.durationMs,
        TimeUnit.MILLISECONDS
      )
    }

  def stop(): Unit =
    playing = false
    engine.stop()

  def shutdown(): Unit =
    playing = false
    executor.shutdownNow()
    engine.shutdown()

  def isPlaying: Boolean = playing
