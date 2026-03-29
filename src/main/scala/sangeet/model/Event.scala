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

  def position: BeatPosition = this match
    case s: Swar    => s.beat
    case r: Rest    => r.beat
    case u: Sustain => u.beat
