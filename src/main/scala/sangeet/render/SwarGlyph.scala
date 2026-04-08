package sangeet.render

import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, TextAlignment}
import sangeet.model.*

object SwarGlyph:

  private var _cachedScript: String = ""
  private var _swarFont: Font = _
  private var _smallFont: Font = _

  private def swarFont: Font =
    val name = DevanagariMap.fontName
    if name != _cachedScript then
      _cachedScript = name
      _swarFont = Font(name, 16)
      _smallFont = Font(name, 10)
    _swarFont

  private def smallFont: Font =
    val name = DevanagariMap.fontName
    if name != _cachedScript then
      _cachedScript = name
      _swarFont = Font(name, 16)
      _smallFont = Font(name, 10)
    _smallFont
  val dotRadius = 2.0

  private val swarColor = Color.web(NotationColors.swar)
  private val dotColor = Color.web(NotationColors.octaveDot)
  private val restColor = Color.web(NotationColors.rest)
  private val sustainColor = Color.web(NotationColors.sustain)
  private val strokeColor = Color.web(NotationColors.stroke)

  def draw(gc: GraphicsContext, note: Note, variant: Variant, octave: Octave,
           x: Double, y: Double): Unit =
    val text = DevanagariMap.glyph(note, variant)
    gc.save()
    gc.font = swarFont
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = swarColor
    gc.fillText(text, x, y)

    gc.stroke = swarColor
    if DevanagariMap.needsKomalMark(note, variant) then
      gc.strokeLine(x - 8, y + 3, x + 8, y + 3)

    if DevanagariMap.needsTivraMark(note, variant) then
      gc.strokeLine(x - 2, y - 16, x - 2, y - 10)

    val (count, pos) = DevanagariMap.octaveDots(octave)
    if count > 0 then
      gc.fill = dotColor
      val dotY = pos match
        case DotPosition.Above => y - 14
        case DotPosition.Below => y + 10
        case DotPosition.None  => y
      for i <- 0 until count do
        val offsetX = if count == 2 then (i - 0.5) * 5 else 0.0
        gc.fillOval(x + offsetX - dotRadius, dotY + i * 5 - dotRadius,
                    dotRadius * 2, dotRadius * 2)

    gc.restore()

  def drawRest(gc: GraphicsContext, x: Double, y: Double): Unit =
    gc.save()
    gc.font = swarFont
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = restColor
    gc.fillText(DevanagariMap.restSymbol, x, y)
    gc.restore()

  def drawSustain(gc: GraphicsContext, x: Double, y: Double): Unit =
    gc.save()
    gc.font = swarFont
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = sustainColor
    gc.fillText(DevanagariMap.sustainSymbol, x, y)
    gc.restore()

  def drawStroke(gc: GraphicsContext, stroke: Stroke, x: Double, y: Double): Unit =
    gc.save()
    gc.font = smallFont
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = strokeColor
    gc.fillText(DevanagariMap.strokeText(stroke), x, y)
    gc.restore()
