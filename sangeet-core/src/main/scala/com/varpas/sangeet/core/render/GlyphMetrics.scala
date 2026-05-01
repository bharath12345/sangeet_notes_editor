package com.varpas.sangeet.core.render

import com.varpas.sangeet.core.model.*

object GlyphMetrics:
  def glyph(note: Note, variant: Variant, script: SwarScript): String =
    ScriptMap.glyph(note, script)

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
  val sustainSymbol: String = "\u2014"

  def vibhagMarkerText(marker: VibhagMarker): String = marker match
    case VibhagMarker.Sam      => "X"
    case VibhagMarker.Taali(n) => n.toString
    case VibhagMarker.Khali    => "0"

  def strokeText(stroke: Stroke, script: SwarScript): String =
    if script == SwarScript.English then
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
