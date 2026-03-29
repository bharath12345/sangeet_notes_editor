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
                  showName: Boolean = true,
                  isActive: Boolean = false,
                  cursorVisible: Boolean = true,
                  showStrokeLine: Boolean = false,
                  showSahityaLine: Boolean = false): Double =
    var y = startY

    if showName then
      gc.save()
      if isActive then
        gc.font = Font("System Bold", 15)
        gc.fill = Color.rgb(25, 118, 210)
        gc.fillText(s"▸ ${grid.sectionName}", startX, y)
        gc.stroke = Color.rgb(25, 118, 210)
        gc.lineWidth = 2.0
        gc.strokeLine(startX, y + 4, startX + 600, y + 4)
      else
        gc.font = sectionFont
        gc.fill = Color.Gray
        gc.fillText(s"── ${grid.sectionName} ", startX, y)
        gc.stroke = Color.LightGray
        gc.lineWidth = 1.0
        gc.strokeLine(startX + 80, y - 5, startX + 600, y - 5)
      gc.restore()
      y += 25
    else if isActive then
      gc.save()
      gc.stroke = Color.rgb(25, 118, 210)
      gc.lineWidth = 2.0
      gc.strokeLine(startX - 10, startY - 5, startX - 10, startY + 50)
      gc.restore()

    val sectionStartY = y

    if grid.lines.isEmpty then
      gc.save()
      if isActive then
        gc.stroke = Color.rgb(25, 118, 210, 0.4)
        gc.setLineDashes(4.0, 4.0)
        gc.strokeRect(startX, y, 600, 20)
        gc.font = Font("System", 11)
        gc.fill = Color.rgb(25, 118, 210)
        gc.fillText("(empty — start typing to add notes)", startX + 8, y + 14)
        if cursorVisible then
          gc.setLineDashes()
          gc.stroke = Color.rgb(25, 118, 210)
          gc.lineWidth = 2.5
          gc.strokeLine(startX + 4, y + 2, startX + 4, y + 18)
      else
        gc.stroke = Color.LightGray
        gc.setLineDashes(4.0, 4.0)
        gc.strokeRect(startX, y, 600, 20)
        gc.font = Font("System", 11)
        gc.fill = Color.Gray
        gc.fillText("(empty)", startX + 8, y + 14)
      gc.restore()
      y += 20 + config.lineSpacing
    else
      // Track whether cursor was drawn inside any line
      var cursorDrawn = false
      grid.lines.foreach { line =>
        val drewCursor = drawGridLine(gc, line, config, startX, y, cursorPos, cursorVisible,
          showStrokeLine, showSahityaLine)
        if drewCursor then cursorDrawn = true
        y += lineHeight(showStrokeLine, showSahityaLine) + config.lineSpacing
      }

      // If cursor cycle matches a line but cursor beat has no cell,
      // draw the cursor after the last cell of the matching line
      cursorPos.foreach { (cursorCycle, cursorBeat) =>
        if !cursorDrawn then
          // Find the line whose cells match the cursor cycle
          var lineY = sectionStartY
          var targetLineY: Option[Double] = None
          var targetCellCount = 0
          grid.lines.foreach { line =>
            val lineCycle = line.cells.headOption.map(_.position.cycle)
            if lineCycle.contains(cursorCycle) then
              targetLineY = Some(lineY)
              targetCellCount = line.cells.size
            lineY += lineHeight(showStrokeLine, showSahityaLine) + config.lineSpacing
          }

          targetLineY match
            case Some(ly) =>
              // Draw cursor right after the last cell on the same line
              val cursorX = startX + targetCellCount * config.cellWidthBase
              if cursorVisible then
                drawBlinkingCursor(gc, cursorX, ly)
            case None =>
              // Cursor is on a cycle with no events at all — draw after all lines
              val cursorX = startX
              if cursorVisible then
                drawBlinkingCursor(gc, cursorX, y - config.lineSpacing)
      }

    // Draw left accent bar for active section content area
    if isActive && showName then
      gc.save()
      gc.stroke = Color.rgb(25, 118, 210)
      gc.lineWidth = 3.0
      gc.strokeLine(startX - 12, sectionStartY - 8, startX - 12, y - config.lineSpacing + 5)
      gc.restore()

    y

  /** Line height varies based on whether stroke/sahitya lines are shown */
  def lineHeight(showStroke: Boolean, showSahitya: Boolean): Double =
    var h = 36.0 // marker + swar area
    if showStroke then h += 16.0
    if showSahitya then h += 14.0
    h

  private def drawBlinkingCursor(gc: GraphicsContext, x: Double, lineStartY: Double): Unit =
    val top = lineStartY + 4   // a bit below marker area
    val bottom = lineStartY + 48
    gc.save()
    gc.stroke = Color.rgb(25, 118, 210)
    gc.lineWidth = 2.5
    gc.strokeLine(x + 3, top, x + 3, bottom)
    gc.restore()

  /** Draw a grid line. Returns true if the cursor was drawn inside a cell. */
  def drawGridLine(gc: GraphicsContext, line: GridLine, config: LayoutConfig,
                   startX: Double, startY: Double,
                   cursorPos: Option[(Int, Int)] = None,
                   cursorVisible: Boolean = true,
                   showStrokeLine: Boolean = false,
                   showSahityaLine: Boolean = false): Boolean =
    val markerY = startY
    val swarY = startY + 22
    val strokeY = if showStrokeLine then swarY + 16 else swarY
    val sahityaY = if showSahityaLine then strokeY + 14 else strokeY
    val bottomY = sahityaY
    var cursorDrawn = false

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

      // Draw cursor on matching cell
      cursorPos.foreach { (cursorCycle, cursorBeat) =>
        if cell.position.cycle == cursorCycle && cell.position.beat == cursorBeat then
          cursorDrawn = true
          // Draw blinking | cursor line at right edge of cell
          if cursorVisible then
            gc.save()
            gc.stroke = Color.rgb(25, 118, 210)
            gc.lineWidth = 2.5
            val cursorLineX = cellX + config.cellWidthBase - 4
            gc.strokeLine(cursorLineX, markerY + 4, cursorLineX, bottomY + 6)
            gc.restore()
      }

      val eventCount = cell.events.size
      cell.events.zipWithIndex.foreach { (event, evtIdx) =>
        val evtX = if eventCount == 1 then cellCenterX
                   else cellX + (evtIdx + 0.5) * (config.cellWidthBase / eventCount)

        event match
          case s: Event.Swar =>
            SwarGlyph.draw(gc, s.note, s.variant, s.octave, evtX, swarY)
            if showStrokeLine then
              s.stroke.foreach(st => SwarGlyph.drawStroke(gc, st, evtX, strokeY))
            if showSahityaLine then
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

    // Draw stroke line separator and label if enabled
    if showStrokeLine then
      val lineEndX = startX + line.cells.size * config.cellWidthBase
      gc.save()
      gc.stroke = Color.rgb(180, 180, 180)
      gc.lineWidth = 0.5
      gc.strokeLine(startX, strokeY - 10, lineEndX, strokeY - 10)
      // Draw "Da/Ra" label at left margin
      gc.font = Font("System", 9)
      gc.fill = Color.rgb(160, 160, 160)
      gc.setTextAlign(TextAlignment.Left)
      gc.fillText("Da/Ra", startX - 38, strokeY)
      gc.restore()

    // Draw sahitya line separator and label if enabled
    if showSahityaLine then
      val lineEndX = startX + line.cells.size * config.cellWidthBase
      gc.save()
      gc.stroke = Color.rgb(180, 180, 180)
      gc.lineWidth = 0.5
      gc.strokeLine(startX, sahityaY - 10, lineEndX, sahityaY - 10)
      // Draw "Sahitya" label at left margin
      gc.font = Font("System", 9)
      gc.fill = Color.rgb(160, 160, 160)
      gc.setTextAlign(TextAlignment.Left)
      gc.fillText("Sahitya", startX - 45, sahityaY)
      gc.restore()

    line.vibhagBreaks.foreach { breakIdx =>
      val lineX = startX + breakIdx * config.cellWidthBase
      gc.save()
      gc.stroke = Color.Gray
      gc.strokeLine(lineX, markerY - 5, lineX, bottomY + 5)
      gc.restore()
    }

    cursorDrawn
