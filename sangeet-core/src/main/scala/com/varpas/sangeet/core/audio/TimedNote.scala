package com.varpas.sangeet.core.audio

import com.varpas.sangeet.core.model.*

case class TimedNote(
  timeMs: Long,
  durationMs: Long,
  note: Note,
  variant: Variant,
  octave: Octave,
  stroke: Option[Stroke]
)
