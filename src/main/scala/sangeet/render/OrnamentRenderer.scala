package sangeet.render

import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.shape.ArcType
import scalafx.scene.text.{Font, TextAlignment}
import sangeet.model.*

object OrnamentRenderer:

  private val ornColor = Color.web(NotationColors.ornament)
  private val devaFont9 = Font("Noto Sans Devanagari", 9)
  private val devaFont8 = Font("Noto Sans Devanagari", 8)
  private val devaFont7 = Font("Noto Sans Devanagari", 7)
  private val italicFont8 = Font("System Italic", 8)

  def draw(gc: GraphicsContext, ornaments: List[Ornament],
           x: Double, y: Double, cellWidth: Double): Unit =
    ornaments.foreach {
      case m: Meend          => drawMeend(gc, m, x, y)
      case k: KanSwar        => drawKanSwar(gc, k, x, y)
      case _: Gamak          => drawGamak(gc, x, y)
      case _: Andolan        => drawAndolan(gc, x, y)
      case _: Gitkari        => drawGitkari(gc, x, y)
      case m: Murki          => drawMurki(gc, m, x, y)
      case k: Krintan        => drawKrintan(gc, k, x, y)
      case g: Ghaseet        => drawGhaseet(gc, g, x, y)
      case s: Sparsh         => drawSparsh(gc, s, x, y)
      case z: Zamzama        => drawZamzama(gc, z, x, y)
      case c: CustomOrnament => drawCustom(gc, c, x, y)
    }

  /** Meend: arc above the note. Upward curve = ascending, downward = descending. */
  private def drawMeend(gc: GraphicsContext, meend: Meend,
                        x: Double, y: Double): Unit =
    gc.save()
    gc.stroke = ornColor
    gc.lineWidth = 1.8
    val arcY = y - 26
    meend.direction match
      case MeendDirection.Ascending =>
        // Upward-curving arc
        gc.strokeArc(x - 15, arcY - 6, 30, 12, 0, 180, ArcType.Open)
      case MeendDirection.Descending =>
        // Downward-curving arc
        gc.strokeArc(x - 15, arcY, 30, 12, 180, 180, ArcType.Open)
    // Small arrow at end to show direction
    val arrowX = x + 13
    val arrowY = arcY + 6
    gc.lineWidth = 1.2
    gc.strokeLine(arrowX, arrowY, arrowX - 3, arrowY - 3)
    gc.strokeLine(arrowX, arrowY, arrowX - 3, arrowY + 3)
    gc.restore()

  /** Kan Swar: small superscript Devanagari glyph before main note */
  private def drawKanSwar(gc: GraphicsContext, kan: KanSwar,
                           x: Double, y: Double): Unit =
    gc.save()
    gc.font = devaFont9
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = ornColor
    val text = DevanagariMap.glyph(kan.graceNote.note, kan.graceNote.variant)
    gc.fillText(text, x - 12, y - 10)
    gc.restore()

  /** Gamak: heavy wavy line with large amplitude above the note */
  private def drawGamak(gc: GraphicsContext, x: Double, y: Double): Unit =
    gc.save()
    gc.stroke = ornColor
    gc.lineWidth = 1.8
    val baseY = y - 22
    val width = 18.0
    val steps = 4
    val dx = width / steps
    val amp = 3.5
    for i <- 0 until steps do
      val x1 = x - width / 2 + i * dx
      val x2 = x1 + dx
      val yOff = if i % 2 == 0 then -amp else amp
      gc.strokeLine(x1, baseY + yOff, x2, baseY - yOff)
    gc.restore()

  /** Andolan: gentle, thin wavy line above the note (smaller than Gamak) */
  private def drawAndolan(gc: GraphicsContext, x: Double, y: Double): Unit =
    gc.save()
    gc.stroke = ornColor
    gc.lineWidth = 0.9
    val baseY = y - 21
    val width = 14.0
    val steps = 6
    val dx = width / steps
    val amp = 1.5
    for i <- 0 until steps do
      val x1 = x - width / 2 + i * dx
      val x2 = x1 + dx
      val yOff = if i % 2 == 0 then -amp else amp
      gc.strokeLine(x1, baseY + yOff, x2, baseY - yOff)
    gc.restore()

  /** Gitkari: trill mark — "tr" text with wavy tail */
  private def drawGitkari(gc: GraphicsContext, x: Double, y: Double): Unit =
    gc.save()
    val baseY = y - 20
    // "tr" label
    gc.font = italicFont8
    gc.fill = ornColor
    gc.fillText("tr", x - 10, baseY)
    // Short wavy tail after "tr"
    gc.stroke = ornColor
    gc.lineWidth = 1.0
    val tailStart = x - 2
    for i <- 0 until 3 do
      val x1 = tailStart + i * 3
      val x2 = x1 + 3
      val yOff = if i % 2 == 0 then -1.5 else 1.5
      gc.strokeLine(x1, baseY - 3 + yOff, x2, baseY - 3 - yOff)
    gc.restore()

  /** Murki: small notes in parentheses above the main note */
  private def drawMurki(gc: GraphicsContext, murki: Murki,
                         x: Double, y: Double): Unit =
    gc.save()
    gc.font = devaFont8
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = ornColor
    val text = "(" + murki.notes.map(n => DevanagariMap.glyph(n.note, n.variant)).mkString("") + ")"
    gc.fillText(text, x, y - 18)
    gc.restore()

  /** Krintan: downward curve with pull-off notes */
  private def drawKrintan(gc: GraphicsContext, krintan: Krintan,
                           x: Double, y: Double): Unit =
    gc.save()
    val baseY = y - 24
    // Downward curve
    gc.stroke = ornColor
    gc.lineWidth = 1.5
    gc.strokeArc(x - 10, baseY, 20, 8, 180, 180, ArcType.Open)
    // Show notes inside/below the curve
    if krintan.notes.nonEmpty then
      gc.font = devaFont7
      gc.fill = ornColor
      gc.setTextAlign(TextAlignment.Center)
      val noteText = krintan.notes.map(n => DevanagariMap.glyph(n.note, n.variant)).mkString("")
      gc.fillText(noteText, x, baseY - 2)
    gc.restore()

  /** Ghaseet: heavy arc with directional arrow */
  private def drawGhaseet(gc: GraphicsContext, ghaseet: Ghaseet,
                           x: Double, y: Double): Unit =
    gc.save()
    val baseY = y - 26
    // Heavy arc
    gc.stroke = ornColor
    gc.lineWidth = 2.5
    gc.strokeArc(x - 14, baseY, 28, 10, 0, 180, ArcType.Open)
    // Arrow at end
    gc.lineWidth = 1.5
    val arrowX = x + 12
    gc.strokeLine(arrowX, baseY + 5, arrowX - 4, baseY + 2)
    gc.strokeLine(arrowX, baseY + 5, arrowX - 4, baseY + 8)
    // Target note label
    gc.font = devaFont7
    gc.fill = ornColor
    gc.fillText(DevanagariMap.glyph(ghaseet.targetNote.note, ghaseet.targetNote.variant),
                x + 14, baseY + 4)
    gc.restore()

  /** Sparsh: tiny superscript dot and note */
  private def drawSparsh(gc: GraphicsContext, sparsh: Sparsh,
                          x: Double, y: Double): Unit =
    gc.save()
    // Small dot
    gc.fill = ornColor
    gc.fillOval(x + 8, y - 14, 3, 3)
    // Tiny note glyph
    gc.font = devaFont7
    gc.fill = ornColor
    gc.fillText(DevanagariMap.glyph(sparsh.touchNote.note, sparsh.touchNote.variant),
                x + 12, y - 8)
    gc.restore()

  /** Zamzama: rapid note cluster in square brackets */
  private def drawZamzama(gc: GraphicsContext, z: Zamzama,
                           x: Double, y: Double): Unit =
    gc.save()
    gc.font = devaFont8
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = ornColor
    val text = "[" + z.notes.map(n => DevanagariMap.glyph(n.note, n.variant)).mkString("") + "]"
    gc.fillText(text, x, y - 18)
    gc.restore()

  /** Custom ornament: italic name label */
  private def drawCustom(gc: GraphicsContext, c: CustomOrnament,
                          x: Double, y: Double): Unit =
    gc.save()
    gc.font = italicFont8
    gc.fill = ornColor
    gc.fillText(c.name, x - 8, y - 18)
    gc.restore()
