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

    val timedNotes = PlaybackScheduler.schedule(events, bpm, matras).sortBy(_.timeMs)
    if timedNotes.isEmpty then return

    // Single tick thread processes notes in time order instead of
    // scheduling 2 tasks per note (noteOn + noteOff) all at once
    val noteArray = timedNotes.toArray
    val startTime = System.currentTimeMillis()

    executor.submit(new Runnable {
      def run(): Unit = {
        var noteIdx = 0
        while playing && noteIdx < noteArray.length do {
          val now = System.currentTimeMillis() - startTime
          while noteIdx < noteArray.length && noteArray(noteIdx).timeMs <= now do {
            val tn = noteArray(noteIdx)
            if playing then {
              engine.playNote(tn)
              executor.schedule(
                new Runnable { def run(): Unit =
                  if playing then
                    val midi = engine match
                      case m: MidiEngine => m.toMidiNote(tn.note, tn.variant, tn.octave)
                      case _ => 60
                    engine.noteOff(midi)
                },
                tn.durationMs,
                TimeUnit.MILLISECONDS
              )
            }
            noteIdx += 1
          }
          if playing && noteIdx < noteArray.length then {
            val nextTime = noteArray(noteIdx).timeMs
            val sleepMs = math.max(1, nextTime - (System.currentTimeMillis() - startTime))
            Thread.sleep(math.min(sleepMs, 10))
          }
        }
      }
    })

  def stop(): Unit =
    playing = false
    engine.stop()

  def shutdown(): Unit =
    playing = false
    executor.shutdownNow()
    engine.shutdown()

  def isPlaying: Boolean = playing
