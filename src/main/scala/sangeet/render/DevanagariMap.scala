package sangeet.render

import sangeet.model.*

enum DotPosition:
  case Above, Below, None

object DevanagariMap:

  private val glyphs: Map[Note, String] = Map(
    Note.Sa  -> "सा",
    Note.Re  -> "रे",
    Note.Ga  -> "ग",
    Note.Ma  -> "म",
    Note.Pa  -> "प",
    Note.Dha -> "ध",
    Note.Ni  -> "नि"
  )

  def glyph(note: Note, variant: Variant): String = glyphs(note)

  def needsKomalMark(note: Note, variant: Variant): Boolean =
    variant == Variant.Komal && (note == Note.Re || note == Note.Ga ||
      note == Note.Dha || note == Note.Ni)

  def needsTivraMark(note: Note, variant: Variant): Boolean =
    variant == Variant.Tivra && note == Note.Ma

  def octaveDots(octave: Octave): (Int, DotPosition) = octave match
    case Octave.AtiMandra => (2, DotPosition.Below)
    case Octave.Mandra    => (1, DotPosition.Below)
    case Octave.Madhya    => (0, DotPosition.None)
    case Octave.Taar      => (1, DotPosition.Above)
    case Octave.AtiTaar   => (2, DotPosition.Above)

  val restSymbol: String = "-"
  val sustainSymbol: String = "\u2014" // em-dash for sustain (hold)

  val vibhagMarkerText: VibhagMarker => String =
    case VibhagMarker.Sam      => "X"
    case VibhagMarker.Taali(n) => n.toString
    case VibhagMarker.Khali    => "0"

  val strokeText: Stroke => String =
    case Stroke.Da      => "Da"
    case Stroke.Ra      => "Ra"
    case Stroke.Chikari => "Ch"
    case Stroke.Jod     => "Jo"
