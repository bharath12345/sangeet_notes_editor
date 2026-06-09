package com.varpas.sangeet.desktop.render

import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.text.TextAlignment

import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.render.{DotPosition, GlyphMetrics, NotationColors}

/** Renders individual swar glyphs on a ScalaFX canvas. Uses GlyphMetrics from core for glyph text, komal/tivra marks,
  * octave dots. Takes script as parameter (no mutable global state).
  */
object SwarGlyphRenderer:

  private def swarFont(script: SwarScript)  = FontCache.scriptFont(script, 16)
  private def smallFont(script: SwarScript) = FontCache.scriptFont(script, 10)

  val dotRadius = 2.0

  private val swarColor    = Color.web(NotationColors.swar)
  private val dotColor     = Color.web(NotationColors.octaveDot)
  private val restColor    = Color.web(NotationColors.rest)
  private val sustainColor = Color.web(NotationColors.sustain)
  private val strokeColor  = Color.web(NotationColors.stroke)

  def draw(
      gc: GraphicsContext,
      note: Note,
      variant: Variant,
      octave: Octave,
      x: Double,
      y: Double,
      script: SwarScript
  ): Unit =
    val text = GlyphMetrics.glyph(note, variant, script)
    gc.save()
    gc.font = swarFont(script)
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = swarColor
    gc.fillText(text, x, y)

    gc.stroke = swarColor
    if GlyphMetrics.needsKomalMark(note, variant) then gc.strokeLine(x - 8, y + 3, x + 8, y + 3)

    if GlyphMetrics.needsTivraMark(note, variant) then gc.strokeLine(x - 2, y - 16, x - 2, y - 10)

    val (count, pos) = GlyphMetrics.octaveDots(octave)
    if count > 0 then
      gc.fill = dotColor
      val dotY = pos match
        case DotPosition.Above => y - 14
        case DotPosition.Below => y + 10
        case DotPosition.None  => y
      for i <- 0 until count do
        val offsetX = if count == 2 then (i - 0.5) * 5 else 0.0
        gc.fillOval(x + offsetX - dotRadius, dotY + i * 5 - dotRadius, dotRadius * 2, dotRadius * 2)

    gc.restore()

  def drawRest(gc: GraphicsContext, x: Double, y: Double, script: SwarScript): Unit =
    gc.save()
    gc.font = swarFont(script)
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = restColor
    gc.fillText(GlyphMetrics.restSymbol, x, y)
    gc.restore()

  def drawSustain(gc: GraphicsContext, x: Double, y: Double, script: SwarScript): Unit =
    gc.save()
    gc.font = swarFont(script)
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = sustainColor
    gc.fillText(GlyphMetrics.sustainSymbol, x, y)
    gc.restore()

  def drawStroke(gc: GraphicsContext, stroke: Stroke, x: Double, y: Double, script: SwarScript): Unit =
    gc.save()
    gc.font = smallFont(script)
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = strokeColor
    gc.fillText(GlyphMetrics.strokeText(stroke, script), x, y)
    gc.restore()

  def drawChikari(gc: GraphicsContext, x: Double, y: Double, script: SwarScript): Unit =
    gc.save()
    gc.font = swarFont(script)
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = swarColor
    gc.fillText(GlyphMetrics.chikariSwarText, x, y)
    gc.restore()

  def drawChikariStroke(gc: GraphicsContext, x: Double, y: Double, script: SwarScript): Unit =
    gc.save()
    gc.font = smallFont(script)
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = strokeColor
    gc.fillText(GlyphMetrics.chikariStrokeText(script), x, y)
    gc.restore()
