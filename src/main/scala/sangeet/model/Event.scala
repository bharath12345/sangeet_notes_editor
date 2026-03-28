package sangeet.model

enum Event:
  case Swar(
    note: Note,
    variant: Variant,
    octave: Octave,
    beat: BeatPosition,
    duration: Rational,
    stroke: Option[Stroke],
    ornaments: List[Ornament],
    sahitya: Option[String]
  )

  case Rest(
    beat: BeatPosition,
    duration: Rational
  )

  case Sustain(
    beat: BeatPosition,
    duration: Rational
  )
