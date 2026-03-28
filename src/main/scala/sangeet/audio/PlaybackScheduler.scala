package sangeet.audio

import sangeet.model.*

object PlaybackScheduler:

  def schedule(events: List[Event], bpm: Double, matras: Int): List[TimedNote] =
    val msPerBeat = 60000.0 / bpm
    events.collect {
      case s: Event.Swar =>
        val beatOffset = s.beat.cycle * matras + s.beat.beat
        val subOffset = s.beat.subdivision.toDouble
        val timeMs = ((beatOffset + subOffset) * msPerBeat).toLong
        val durationMs = (s.duration.toDouble * msPerBeat).toLong
        TimedNote(timeMs, durationMs, s.note, s.variant, s.octave, s.stroke)
    }
