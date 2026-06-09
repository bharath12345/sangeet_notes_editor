package com.varpas.sangeet.desktop.render

import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.text.TextAlignment

import com.varpas.sangeet.core.layout.{GridLine, LayoutConfig, SectionGrid}
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.render.{GlyphMetrics, NotationColors}

/** Renders grid-based notation sections on a ScalaFX canvas. Receives script as parameter, delegates to
  * SwarGlyphRenderer and OrnamentRendererFX.
  */
object GridRendererFX:

  val markerFont  = FontCache.font("System", 12)
  val sectionFont = FontCache.font("System Bold", 14)
  val headerFont  = FontCache.font("System", 12)

  private def sahityaFont(script: SwarScript) = FontCache.scriptFont(script, 11)

  /** Vertical layout offsets within a grid line, relative to startY. Rows from top: marker -> bracket -> ornament/taar
    * -> swar -> mandra -> stroke -> sahitya
    */
  object LineLayout:
    val markerH   = 14.0
    val bracketH  = 10.0
    val ornamentH = 18.0
    val swarH     = 18.0
    val mandraH   = 12.0
    val strokeH   = 16.0
    val sahityaH  = 14.0

    val markerY   = 0.0
    val bracketY  = markerH
    val ornamentY = bracketY + bracketH
    val swarY     = ornamentY + ornamentH
    val mandraY   = swarY + mandraH
    val strokeY   = mandraY + 4
    val sahityaY  = strokeY + strokeH

  /** Line height varies based on whether stroke/sahitya lines are shown */
  def lineHeight(showStroke: Boolean, showSahitya: Boolean): Double =
    if showSahitya then LineLayout.sahityaY + LineLayout.sahityaH
    else if showStroke then LineLayout.strokeY + LineLayout.strokeH
    else LineLayout.mandraY

  def drawSection(
      gc: GraphicsContext,
      grid: SectionGrid,
      config: LayoutConfig,
      startX: Double,
      startY: Double,
      cursorPos: Option[(Int, Int)] = None,
      showName: Boolean = true,
      isActive: Boolean = false,
      cursorVisible: Boolean = true,
      showStrokeLine: Boolean = false,
      showSahityaLine: Boolean = false,
      strokeEditMode: Boolean = false,
      script: SwarScript = SwarScript.Devanagari,
      selectionRange: Option[((Int, Int), (Int, Int))] = None,
      startingBeat: Int = 1
  ): Double =
    var y = startY

    val gridWidth = grid.lines.map(_.cells.size).maxOption.getOrElse(10).toDouble * config.cellWidthBase

    if showName then
      gc.save()
      if isActive then
        gc.font = FontCache.font("System Bold", 15)
        gc.fill = Color.rgb(25, 118, 210)
        gc.fillText(s"\u25b8 ${grid.sectionName}", startX, y)
        gc.stroke = Color.rgb(25, 118, 210)
        gc.lineWidth = 2.0
        gc.strokeLine(startX, y + 4, startX + gridWidth, y + 4)
      else
        gc.font = sectionFont
        gc.fill = Color.Gray
        gc.fillText(s"\u2500\u2500 ${grid.sectionName} ", startX, y)
        gc.stroke = Color.LightGray
        gc.lineWidth = 1.0
        gc.strokeLine(startX + 80, y - 5, startX + gridWidth, y - 5)
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
        gc.strokeRect(startX, y, gridWidth, 20)
        gc.font = FontCache.font("System", 11)
        gc.fill = Color.rgb(25, 118, 210)
        gc.fillText("(empty \u2014 start typing to add notes)", startX + 8, y + 14)
        if cursorVisible then
          gc.setLineDashes()
          gc.stroke = Color.rgb(25, 118, 210)
          gc.lineWidth = 2.5
          gc.strokeLine(startX + 4, y + 2, startX + 4, y + 18)
      else
        gc.stroke = Color.LightGray
        gc.setLineDashes(4.0, 4.0)
        gc.strokeRect(startX, y, gridWidth, 20)
        gc.font = FontCache.font("System", 11)
        gc.fill = Color.Gray
        gc.fillText("(empty)", startX + 8, y + 14)
      gc.restore()
      y += 20 + config.lineSpacing
    else
      var cursorDrawn = false
      grid.lines.foreach { line =>
        val drewCursor = drawGridLine(
          gc,
          line,
          config,
          startX,
          y,
          cursorPos,
          cursorVisible,
          showStrokeLine,
          showSahityaLine,
          strokeEditMode,
          script,
          selectionRange,
          startingBeat
        )
        if drewCursor then cursorDrawn = true
        y += lineHeight(showStrokeLine, showSahityaLine) + config.lineSpacing
      }

      cursorPos.foreach { (cursorCycle, cursorBeat) =>
        if !cursorDrawn then
          var lineY                       = sectionStartY
          var targetLineY: Option[Double] = None
          var targetCellCount             = 0
          grid.lines.foreach { line =>
            val lineCycle = line.cells.headOption.map(_.position.cycle)
            if lineCycle.contains(cursorCycle) then
              targetLineY = Some(lineY)
              targetCellCount = line.cells.size
            lineY += lineHeight(showStrokeLine, showSahityaLine) + config.lineSpacing
          }

          targetLineY match
            case Some(ly) =>
              val cursorX = startX + targetCellCount * config.cellWidthBase
              if cursorVisible then drawBlinkingCursor(gc, cursorX, ly)
            case None =>
              val cursorX = startX
              if cursorVisible then drawBlinkingCursor(gc, cursorX, y - config.lineSpacing)
      }

    // Draw left accent bar for active section content area
    if isActive && showName then
      gc.save()
      gc.stroke = Color.rgb(25, 118, 210)
      gc.lineWidth = 3.0
      gc.strokeLine(startX - 12, sectionStartY - 8, startX - 12, y - config.lineSpacing + 5)
      gc.restore()

    y

  private def drawBlinkingCursor(gc: GraphicsContext, x: Double, lineStartY: Double): Unit =
    val top    = lineStartY + LineLayout.bracketY
    val bottom = lineStartY + LineLayout.mandraY
    gc.save()
    gc.stroke = Color.rgb(25, 118, 210)
    gc.lineWidth = 2.5
    gc.strokeLine(x + 3, top, x + 3, bottom)
    gc.restore()

  /** Draw a grid line. Returns true if the cursor was drawn inside a cell. */
  private def isCellSelected(
      cycle: Int,
      beat: Int,
      selectionRange: Option[((Int, Int), (Int, Int))]
  ): Boolean =
    selectionRange.exists { case ((startCycle, startBeat), (endCycle, endBeat)) =>
      val afterStart = cycle > startCycle || (cycle == startCycle && beat >= startBeat)
      val beforeEnd  = cycle < endCycle || (cycle == endCycle && beat <= endBeat)
      afterStart && beforeEnd
    }

  def drawGridLine(
      gc: GraphicsContext,
      line: GridLine,
      config: LayoutConfig,
      startX: Double,
      startY: Double,
      cursorPos: Option[(Int, Int)] = None,
      cursorVisible: Boolean = true,
      showStrokeLine: Boolean = false,
      showSahityaLine: Boolean = false,
      strokeEditMode: Boolean = false,
      script: SwarScript = SwarScript.Devanagari,
      selectionRange: Option[((Int, Int), (Int, Int))] = None,
      startingBeat: Int = 1
  ): Boolean =
    val markerY     = startY + LineLayout.markerY
    val bracketY    = startY + LineLayout.bracketY
    val swarY       = startY + LineLayout.swarY
    val strokeY     = startY + LineLayout.strokeY
    val sahityaY    = startY + LineLayout.sahityaY
    val bottomY     = startY + lineHeight(showStrokeLine, showSahityaLine)
    var cursorDrawn = false

    // Draw selection highlight background
    line.cells.zipWithIndex.foreach { (cell, idx) =>
      if isCellSelected(cell.position.cycle, cell.position.beat, selectionRange) then
        val cellX = startX + idx * config.cellWidthBase
        gc.save()
        gc.fill = Color.rgb(25, 118, 210, 0.15)
        gc.fillRect(cellX, bracketY - 2, config.cellWidthBase, bottomY - bracketY + 4)
        gc.restore()
    }

    // Draw taal markers (X, 0, 2, 3, ...)
    line.markers.foreach { (cellIdx, marker) =>
      val markerX = startX + cellIdx * config.cellWidthBase + config.cellWidthBase / 2
      gc.save()
      gc.font = markerFont
      gc.setTextAlign(TextAlignment.Center)
      gc.fill =
        if marker == VibhagMarker.Sam then Color.web(NotationColors.taalMarkerSam)
        else Color.web(NotationColors.taalMarker)
      gc.fillText(GlyphMetrics.vibhagMarkerText(marker), markerX, markerY)
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
        val bLeft  = cellX + 2
        val bRight = cellX + config.cellWidthBase - 2
        val bTop   = bracketY
        val bBot   = bracketY + 6
        gc.strokeLine(bLeft, bBot, bLeft, bTop)
        gc.strokeLine(bLeft, bTop, bRight, bTop)
        gc.strokeLine(bRight, bTop, bRight, bBot)
        gc.restore()
    }

    // Pre-compute alternating Da/Ra stroke index
    var swarCounter = 0

    line.cells.zipWithIndex.foreach { (cell, idx) =>
      val cellX       = startX + idx * config.cellWidthBase
      val cellCenterX = cellX + config.cellWidthBase / 2
      val isLocked    = cell.position.beat < startingBeat - 1

      if isLocked then SwarGlyphRenderer.drawLockedBeat(gc, cellCenterX, swarY, script)
      else
        // Draw cursor on matching cell
        cursorPos.foreach { (cursorCycle, cursorBeat) =>
          if cell.position.cycle == cursorCycle && cell.position.beat == cursorBeat then
            cursorDrawn = true
            if cursorVisible then
              gc.save()
              if strokeEditMode && showStrokeLine then
                gc.stroke = Color.rgb(230, 120, 0)
                gc.lineWidth = 2.0
                val cursorLineX = cellX + config.cellWidthBase - 4
                gc.strokeLine(cursorLineX, strokeY - 10, cursorLineX, strokeY + 6)
              else
                gc.stroke = Color.rgb(25, 118, 210)
                gc.lineWidth = 2.5
                val cursorLineX = cellX + config.cellWidthBase - 4
                gc.strokeLine(cursorLineX, markerY + 4, cursorLineX, bottomY + 6)
              gc.restore()
        }

        val eventCount = cell.events.size
        cell.events.zipWithIndex.foreach { (event, evtIdx) =>
          val evtX =
            if eventCount == 1 then cellCenterX
            else cellX + (evtIdx + 0.5) * (config.cellWidthBase / eventCount)

          event match
            case s: Event.Swar =>
              SwarGlyphRenderer.draw(gc, s.note, s.variant, s.octave, evtX, swarY, script)
              if showStrokeLine then
                val stroke = s.stroke.getOrElse(if swarCounter % 2 == 0 then Stroke.Da else Stroke.Ra)
                SwarGlyphRenderer.drawStroke(gc, stroke, evtX, strokeY, script)
                swarCounter += 1
              if showSahityaLine then
                s.sahitya.foreach { text =>
                  gc.save()
                  gc.font = sahityaFont(script)
                  gc.setTextAlign(TextAlignment.Center)
                  gc.fill = Color.web(NotationColors.sahitya)
                  gc.fillText(text, evtX, sahityaY)
                  gc.restore()
                }
              if s.ornaments.nonEmpty then
                OrnamentRendererFX.draw(gc, s.ornaments, evtX, swarY, config.cellWidthBase, script)
            case _: Event.Rest =>
              SwarGlyphRenderer.drawRest(gc, evtX, swarY, script)
            case _: Event.Sustain =>
              SwarGlyphRenderer.drawSustain(gc, evtX, swarY, script)
            case _: Event.Chikari =>
              SwarGlyphRenderer.drawChikari(gc, evtX, swarY, script)
              if showStrokeLine then SwarGlyphRenderer.drawChikariStroke(gc, evtX, strokeY, script)
        }
    }

    // Draw swar row label
    gc.save()
    gc.font = FontCache.font("System", 9)
    gc.fill = Color.rgb(160, 160, 160)
    gc.setTextAlign(TextAlignment.Left)
    gc.fillText("Swar", startX - 30, swarY)
    gc.restore()

    // Draw stroke line separator and label if enabled
    if showStrokeLine then
      val lineEndX = startX + line.cells.size * config.cellWidthBase
      gc.save()
      gc.stroke = Color.rgb(180, 180, 180)
      gc.lineWidth = 0.5
      gc.strokeLine(startX, strokeY - 10, lineEndX, strokeY - 10)
      val strokeLabel = s"${GlyphMetrics.strokeText(Stroke.Da, script)}/${GlyphMetrics.strokeText(Stroke.Ra, script)}"
      gc.font = FontCache.scriptFont(script, 9)
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
      gc.font = FontCache.font("System", 9)
      gc.fill = Color.rgb(160, 160, 160)
      gc.setTextAlign(TextAlignment.Left)
      gc.fillText("Sahitya", startX - 45, sahityaY)
      gc.restore()

    // Draw vibhag break lines
    line.vibhagBreaks.foreach { breakIdx =>
      val lineX = startX + breakIdx * config.cellWidthBase
      gc.save()
      gc.stroke = Color.Gray
      gc.strokeLine(lineX, markerY - 5, lineX, bottomY + 5)
      gc.restore()
    }

    cursorDrawn
