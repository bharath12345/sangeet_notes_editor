package sangeet.audio

import sangeet.model.*
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

class PlaybackController(engine: SoundEngine):
  private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  private var playing = false

  def play(events: List[Event], bpm: Double, matras: Int): Unit =
    if playing then stop()
    engine.init()
    playing = true

    val timedNotes = PlaybackScheduler.scheduleWithTaal(events, bpm, matras)
    timedNotes.foreach { tn =>
      executor.schedule(
        new Runnable { def run(): Unit = if playing then engine.playNote(tn) },
        tn.timeMs,
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
