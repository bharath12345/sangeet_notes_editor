package sangeet.audio

import sangeet.model.*

case class TimedNote(
  timeMs: Long,
  durationMs: Long,
  note: Note,
  variant: Variant,
  octave: Octave,
  stroke: Option[Stroke]
)

trait SoundEngine:
  def init(): Unit
  def playNote(note: TimedNote): Unit
  def stop(): Unit
  def shutdown(): Unit
