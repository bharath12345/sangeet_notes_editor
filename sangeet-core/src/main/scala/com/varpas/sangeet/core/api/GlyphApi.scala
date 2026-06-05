package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.render.{DotPosition, GlyphMetrics, NotationColors, ScriptMap}

object GlyphApi:

  /** Get the glyph string for a note in the given script. */
  def noteGlyph(note: Note, variant: Variant, script: SwarScript): String =
    GlyphMetrics.glyph(note, variant, script)

  /** Check if a note/variant needs a komal underline mark. */
  def needsKomalMark(note: Note, variant: Variant): Boolean =
    GlyphMetrics.needsKomalMark(note, variant)

  /** Check if a note/variant needs a tivra vertical mark. */
  def needsTivraMark(note: Note, variant: Variant): Boolean =
    GlyphMetrics.needsTivraMark(note, variant)

  /** Get the number of octave dots and their position for an octave. */
  def octaveDots(octave: Octave): (Int, DotPosition) =
    GlyphMetrics.octaveDots(octave)

  /** Get the rest symbol. */
  def restSymbol: String =
    GlyphMetrics.restSymbol

  /** Get the sustain symbol. */
  def sustainSymbol: String =
    GlyphMetrics.sustainSymbol

  /** Get the vibhag marker text. */
  def vibhagMarkerText(marker: VibhagMarker): String =
    GlyphMetrics.vibhagMarkerText(marker)

  /** Get the stroke text for rendering. */
  def strokeText(stroke: Stroke, script: SwarScript): String =
    GlyphMetrics.strokeText(stroke, script)

  /** Get the notation colors palette. */
  def notationColors: NotationColors.type =
    NotationColors

  /** Get all script mappings for a note. */
  def allScriptMappings(note: Note): Map[SwarScript, String] =
    Map(
      SwarScript.Devanagari -> ScriptMap.glyph(note, SwarScript.Devanagari),
      SwarScript.English    -> ScriptMap.glyph(note, SwarScript.English),
      SwarScript.Kannada    -> ScriptMap.glyph(note, SwarScript.Kannada),
      SwarScript.Telugu     -> ScriptMap.glyph(note, SwarScript.Telugu)
    )
