package sangeet.render

import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, TextAlignment}
import sangeet.model.*
import sangeet.layout.*

object GridRenderer:

  val markerFont = Font("System", 12)
  val sectionFont = Font("System Bold", 14)
  val headerFont = Font("System", 12)

  def drawSection(gc: GraphicsContext, grid: SectionGrid, config: LayoutConfig,
                  startX: Double, startY: Double,
                  cursorPos: Option[(Int, Int)] = None,
                  showName: Boolean = true): Double =
    var y = startY

    if showName then
      gc.save()
      gc.font = sectionFont
      gc.fill = Color.DarkBlue
      gc.fillText(s"── ${grid.sectionName} ", startX, y)
      gc.strokeLine(startX + 80, y - 5, startX + 600, y - 5)
      gc.restore()
      y += 25

    if grid.lines.isEmpty then
      // Draw an empty placeholder row for sections with no events yet
      gc.save()
      gc.stroke = Color.LightGray
      gc.setLineDashes(4.0, 4.0)
      gc.strokeRect(startX, y, 600, 20)
      gc.font = Font("System", 11)
      gc.fill = Color.Gray
      gc.fillText("(empty)", startX + 8, y + 14)
      gc.restore()
      y += 20 + config.lineSpacing
    else
      grid.lines.foreach { line =>
        y = drawGridLine(gc, line, config, startX, y, cursorPos)
        y += config.lineSpacing
      }
    y

  def drawGridLine(gc: GraphicsContext, line: GridLine, config: LayoutConfig,
                   startX: Double, startY: Double,
                   cursorPos: Option[(Int, Int)] = None): Double =
    val markerY = startY
    val swarY = startY + 22
    val strokeY = swarY + 16
    val sahityaY = strokeY + 14

    line.markers.foreach { (cellIdx, marker) =>
      val markerX = startX + cellIdx * config.cellWidthBase + config.cellWidthBase / 2
      gc.save()
      gc.font = markerFont
      gc.setTextAlign(TextAlignment.Center)
      gc.fill = if marker == VibhagMarker.Sam then Color.Red else Color.Black
      gc.fillText(DevanagariMap.vibhagMarkerText(marker), markerX, markerY)
      gc.restore()
    }

    line.cells.zipWithIndex.foreach { (cell, idx) =>
      val cellX = startX + idx * config.cellWidthBase
      val cellCenterX = cellX + config.cellWidthBase / 2

      // Draw cursor highlight
      cursorPos.foreach { (cursorCycle, cursorBeat) =>
        if cell.position.cycle == cursorCycle && cell.position.beat == cursorBeat then
          gc.save()
          gc.fill = Color.rgb(65, 105, 225, 0.15) // light blue highlight
          gc.fillRect(cellX + 2, markerY - 8, config.cellWidthBase - 4, sahityaY - markerY + 16)
          gc.stroke = Color.RoyalBlue
          gc.lineWidth = 2.0
          gc.strokeRect(cellX + 2, markerY - 8, config.cellWidthBase - 4, sahityaY - markerY + 16)
          gc.restore()
      }

      val eventCount = cell.events.size
      cell.events.zipWithIndex.foreach { (event, evtIdx) =>
        val evtX = if eventCount == 1 then cellCenterX
                   else cellX + (evtIdx + 0.5) * (config.cellWidthBase / eventCount)

        event match
          case s: Event.Swar =>
            SwarGlyph.draw(gc, s.note, s.variant, s.octave, evtX, swarY)
            s.stroke.foreach(st => SwarGlyph.drawStroke(gc, st, evtX, strokeY))
            s.sahitya.foreach { text =>
              gc.save()
              gc.font = Font("Noto Sans Devanagari", 11)
              gc.setTextAlign(TextAlignment.Center)
              gc.fill = Color.DarkGreen
              gc.fillText(text, evtX, sahityaY)
              gc.restore()
            }
            if s.ornaments.nonEmpty then
              OrnamentRenderer.draw(gc, s.ornaments, evtX, swarY, config.cellWidthBase)
          case _: Event.Rest =>
            SwarGlyph.drawRest(gc, evtX, swarY)
          case _: Event.Sustain =>
            SwarGlyph.drawSustain(gc, evtX, swarY)
      }
    }

    line.vibhagBreaks.foreach { breakIdx =>
      val lineX = startX + breakIdx * config.cellWidthBase
      gc.save()
      gc.stroke = Color.Gray
      gc.strokeLine(lineX, markerY - 5, lineX, sahityaY + 5)
      gc.restore()
    }

    sahityaY
