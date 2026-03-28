package sangeet.render

import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.shape.ArcType
import scalafx.scene.text.{Font, TextAlignment}
import sangeet.model.*

object OrnamentRenderer:

  def draw(gc: GraphicsContext, ornaments: List[Ornament],
           x: Double, y: Double, cellWidth: Double): Unit =
    ornaments.foreach {
      case m: Meend          => drawMeendStart(gc, m, x, y)
      case k: KanSwar        => drawKanSwar(gc, k, x, y)
      case _: Gamak          => drawWavyLine(gc, x, y - 22, 16, heavy = true)
      case _: Andolan        => drawWavyLine(gc, x, y - 22, 16, heavy = false)
      case _: Gitkari        => drawWavyLine(gc, x, y - 22, 12, heavy = true)
      case m: Murki          => drawMurki(gc, m, x, y)
      case k: Krintan        => drawKrintanMark(gc, x, y)
      case g: Ghaseet        => drawGhaseetMark(gc, x, y)
      case s: Sparsh         => drawSparshMark(gc, s, x, y)
      case z: Zamzama        => drawZamzamaMark(gc, z, x, y)
      case c: CustomOrnament => drawCustomMark(gc, c, x, y)
    }

  private def drawMeendStart(gc: GraphicsContext, meend: Meend,
                              x: Double, y: Double): Unit =
    gc.save()
    gc.stroke = Color.DarkBlue
    gc.lineWidth = 1.5
    val arcY = y - 25
    gc.strokeArc(x - 15, arcY, 30, 10, 0, 180, ArcType.Open)
    gc.restore()

  private def drawKanSwar(gc: GraphicsContext, kan: KanSwar,
                           x: Double, y: Double): Unit =
    gc.save()
    gc.font = Font("Noto Sans Devanagari", 9)
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = Color.DarkRed
    val text = DevanagariMap.glyph(kan.graceNote.note, kan.graceNote.variant)
    gc.fillText(text, x - 12, y - 10)
    gc.restore()

  private def drawWavyLine(gc: GraphicsContext, x: Double, y: Double,
                            width: Double, heavy: Boolean): Unit =
    gc.save()
    gc.stroke = Color.DarkBlue
    gc.lineWidth = if heavy then 1.5 else 0.8
    val steps = 6
    val dx = width / steps
    for i <- 0 until steps do
      val x1 = x - width / 2 + i * dx
      val x2 = x1 + dx
      val yOff = if i % 2 == 0 then -2 else 2
      gc.strokeLine(x1, y + yOff, x2, y - yOff)
    gc.restore()

  private def drawMurki(gc: GraphicsContext, murki: Murki,
                         x: Double, y: Double): Unit =
    gc.save()
    gc.font = Font("Noto Sans Devanagari", 8)
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = Color.Purple
    val text = "(" + murki.notes.map(n => DevanagariMap.glyph(n.note, n.variant)).mkString("") + ")"
    gc.fillText(text, x, y - 18)
    gc.restore()

  private def drawKrintanMark(gc: GraphicsContext, x: Double, y: Double): Unit =
    gc.save()
    gc.font = Font("System", 8)
    gc.fill = Color.Brown
    gc.fillText("kr", x - 6, y - 18)
    gc.restore()

  private def drawGhaseetMark(gc: GraphicsContext, x: Double, y: Double): Unit =
    gc.save()
    gc.stroke = Color.DarkRed
    gc.lineWidth = 2.0
    gc.strokeArc(x - 12, y - 28, 24, 8, 0, 180, ArcType.Open)
    gc.restore()

  private def drawSparshMark(gc: GraphicsContext, sparsh: Sparsh,
                              x: Double, y: Double): Unit =
    gc.save()
    gc.font = Font("Noto Sans Devanagari", 7)
    gc.fill = Color.Gray
    gc.fillText(DevanagariMap.glyph(sparsh.touchNote.note, sparsh.touchNote.variant),
                x + 10, y - 8)
    gc.restore()

  private def drawZamzamaMark(gc: GraphicsContext, z: Zamzama,
                               x: Double, y: Double): Unit =
    gc.save()
    gc.font = Font("Noto Sans Devanagari", 8)
    gc.fill = Color.DarkCyan
    val text = "[" + z.notes.map(n => DevanagariMap.glyph(n.note, n.variant)).mkString("") + "]"
    gc.fillText(text, x, y - 18)
    gc.restore()

  private def drawCustomMark(gc: GraphicsContext, c: CustomOrnament,
                              x: Double, y: Double): Unit =
    gc.save()
    gc.font = Font("System Italic", 8)
    gc.fill = Color.DarkGray
    gc.fillText(c.name, x - 8, y - 18)
    gc.restore()
