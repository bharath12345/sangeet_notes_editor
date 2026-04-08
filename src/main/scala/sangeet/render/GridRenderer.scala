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
  val sahityaFont = Font("Noto Sans Devanagari", 11)

  def drawSection(gc: GraphicsContext, grid: SectionGrid, config: LayoutConfig,
                  startX: Double, startY: Double,
                  cursorPos: Option[(Int, Int)] = None,
                  showName: Boolean = true,
                  isActive: Boolean = false,
                  cursorVisible: Boolean = true,
                  showStrokeLine: Boolean = false,
                  showSahityaLine: Boolean = false,
                  strokeEditMode: Boolean = false): Double =
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
          showStrokeLine, showSahityaLine, strokeEditMode)
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

  /** Vertical layout offsets within a grid line, relative to startY.
    * Rows from top: marker → bracket → ornament/taar → swar → mandra → stroke → sahitya */
  object LineLayout:
    val markerH      = 14.0   // taal markers (X, 0, 1, 2)
    val bracketH     = 10.0   // grouping bracket for subdivisions
    val ornamentH    = 18.0   // ornaments + taar saptak dots
    val swarH        = 18.0   // swar glyph
    val mandraH      = 12.0   // mandra saptak dots (clearance below swar)
    val strokeH      = 16.0   // Da/Ra stroke indicators
    val sahityaH     = 14.0   // lyrics

    // Y offsets relative to startY
    val markerY      = 0.0
    val bracketY     = markerH
    val ornamentY    = bracketY + bracketH       // where ornaments/taar dots go
    val swarY        = ornamentY + ornamentH     // where swar text baseline is
    val mandraY      = swarY + mandraH           // bottom of mandra dot area
    val strokeY      = mandraY + 4               // stroke row (with 4px gap)
    val sahityaY     = strokeY + strokeH         // sahitya row

  /** Line height varies based on whether stroke/sahitya lines are shown */
  def lineHeight(showStroke: Boolean, showSahitya: Boolean): Double =
    if showSahitya then LineLayout.sahityaY + LineLayout.sahityaH
    else if showStroke then LineLayout.strokeY + LineLayout.strokeH
    else LineLayout.mandraY

  private def drawBlinkingCursor(gc: GraphicsContext, x: Double, lineStartY: Double): Unit =
    val top = lineStartY + LineLayout.bracketY
    val bottom = lineStartY + LineLayout.mandraY
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
                   showSahityaLine: Boolean = false,
                   strokeEditMode: Boolean = false): Boolean =
    val markerY = startY + LineLayout.markerY
    val bracketY = startY + LineLayout.bracketY
    val swarY = startY + LineLayout.swarY
    val strokeY = startY + LineLayout.strokeY
    val sahityaY = startY + LineLayout.sahityaY
    val bottomY = startY + lineHeight(showStrokeLine, showSahityaLine)
    var cursorDrawn = false

    line.markers.foreach { (cellIdx, marker) =>
      val markerX = startX + cellIdx * config.cellWidthBase + config.cellWidthBase / 2
      gc.save()
      gc.font = markerFont
      gc.setTextAlign(TextAlignment.Center)
      gc.fill = if marker == VibhagMarker.Sam then Color.web(NotationColors.taalMarkerSam)
                else Color.web(NotationColors.taalMarker)
      gc.fillText(DevanagariMap.vibhagMarkerText(marker), markerX, markerY)
      gc.restore()
    }

    // Draw grouping brackets for cells with multiple events (subdivisions)
    line.cells.zipWithIndex.foreach { (cell, idx) =>
      val eventCount = cell.events.size
      if eventCount > 1 then
        val cellX = startX + idx * config.cellWidthBase
        gc.save()
        gc.stroke = Color.rgb(120, 120, 120)
        gc.lineWidth = 1.0
        val bLeft = cellX + 2
        val bRight = cellX + config.cellWidthBase - 2
        val bTop = bracketY
        val bBot = bracketY + 6
        // Draw ⌐...⌐ bracket: left tick down, horizontal line, right tick down
        gc.strokeLine(bLeft, bBot, bLeft, bTop)       // left tick
        gc.strokeLine(bLeft, bTop, bRight, bTop)      // horizontal
        gc.strokeLine(bRight, bTop, bRight, bBot)     // right tick
        gc.restore()
    }

    // Pre-compute alternating Da/Ra stroke index across all swar events in the line
    // (used when showStrokeLine is true to auto-fill default strokes)
    var swarCounter = 0

    line.cells.zipWithIndex.foreach { (cell, idx) =>
      val cellX = startX + idx * config.cellWidthBase
      val cellCenterX = cellX + config.cellWidthBase / 2

      // Draw cursor on matching cell
      cursorPos.foreach { (cursorCycle, cursorBeat) =>
        if cell.position.cycle == cursorCycle && cell.position.beat == cursorBeat then
          cursorDrawn = true
          if cursorVisible then
            gc.save()
            if strokeEditMode && showStrokeLine then
              // Stroke edit cursor: smaller, on the stroke line, orange color
              gc.stroke = Color.rgb(230, 120, 0)
              gc.lineWidth = 2.0
              val cursorLineX = cellX + config.cellWidthBase - 4
              gc.strokeLine(cursorLineX, strokeY - 10, cursorLineX, strokeY + 6)
            else
              // Normal swar cursor: full height, blue
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
              // Use explicit stroke if set, otherwise auto-alternate Da/Ra
              val stroke = s.stroke.getOrElse(if swarCounter % 2 == 0 then Stroke.Da else Stroke.Ra)
              SwarGlyph.drawStroke(gc, stroke, evtX, strokeY)
              swarCounter += 1
            if showSahityaLine then
              s.sahitya.foreach { text =>
                gc.save()
                gc.font = sahityaFont
                gc.setTextAlign(TextAlignment.Center)
                gc.fill = Color.web(NotationColors.sahitya)
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
      // Draw stroke label at left margin (script-aware)
      val strokeLabel = s"${DevanagariMap.strokeText(Stroke.Da)}/${DevanagariMap.strokeText(Stroke.Ra)}"
      gc.font = Font(DevanagariMap.fontName, 9)
      gc.fill = Color.rgb(160, 160, 160)
      gc.setTextAlign(TextAlignment.Left)
      gc.fillText(strokeLabel, startX - 38, strokeY)
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
