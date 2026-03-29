package sangeet.render

import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, TextAlignment}
import sangeet.model.*

object SwarGlyph:

  private def swarFont: Font = Font(DevanagariMap.fontName, 16)
  private def smallFont: Font = Font(DevanagariMap.fontName, 10)
  val dotRadius = 2.0

  def draw(gc: GraphicsContext, note: Note, variant: Variant, octave: Octave,
           x: Double, y: Double): Unit =
    val text = DevanagariMap.glyph(note, variant)
    gc.save()
    gc.font = swarFont
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = Color.Black
    gc.fillText(text, x, y)

    if DevanagariMap.needsKomalMark(note, variant) then
      gc.strokeLine(x - 8, y + 3, x + 8, y + 3)

    if DevanagariMap.needsTivraMark(note, variant) then
      gc.strokeLine(x - 2, y - 16, x - 2, y - 10)

    val (count, pos) = DevanagariMap.octaveDots(octave)
    if count > 0 then
      val dotY = pos match
        case DotPosition.Above => y - 14  // taar dots above swar
        case DotPosition.Below => y + 10  // mandra dots below swar (with clearance)
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
    gc.fill = Color.Black
    gc.fillText(DevanagariMap.restSymbol, x, y)
    gc.restore()

  def drawSustain(gc: GraphicsContext, x: Double, y: Double): Unit =
    gc.save()
    gc.font = swarFont
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = Color.Gray
    gc.fillText(DevanagariMap.sustainSymbol, x, y)
    gc.restore()

  def drawStroke(gc: GraphicsContext, stroke: Stroke, x: Double, y: Double): Unit =
    gc.save()
    gc.font = smallFont
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = Color.Gray
    gc.fillText(DevanagariMap.strokeText(stroke), x, y)
    gc.restore()
