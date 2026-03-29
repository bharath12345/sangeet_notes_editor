package sangeet.render

import sangeet.model.*

enum DotPosition:
  case Above, Below, None

object DevanagariMap:

  // Current script — mutable to allow runtime switching
  @volatile private var _script: SwarScript = SwarScript.Devanagari

  def currentScript: SwarScript = _script
  def setScript(s: SwarScript): Unit = _script = s

  def glyph(note: Note, variant: Variant): String =
    ScriptMap.glyph(note, _script)

  def fontName: String = ScriptMap.fontName(_script)

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

  def strokeText(stroke: Stroke): String =
    if _script == SwarScript.English then
      stroke match
        case Stroke.Da      => "Da"
        case Stroke.Ra      => "Ra"
        case Stroke.Chikari => "Ch"
        case Stroke.Jod     => "Jo"
    else
      stroke match
        case Stroke.Da      => "दा"
        case Stroke.Ra      => "रा"
        case Stroke.Chikari => "ची"
        case Stroke.Jod     => "जो"
