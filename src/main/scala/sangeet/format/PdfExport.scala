package sangeet.format

import org.apache.pdfbox.pdmodel.{PDDocument, PDPage, PDPageContentStream}
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.{PDFont, PDType0Font, PDType1Font, Standard14Fonts}
import sangeet.model.*
import sangeet.layout.*
import sangeet.render.{DevanagariMap, ScriptMap, NotationColors, ScriptUtil, OrnamentLabels, GridLineUtil}
import java.nio.file.Path

object PdfExport:

  /** Load the bundled Noto Sans Devanagari font, returning (regular, bold).
    * Falls back to Helvetica if the resource is missing. */
  private def loadDevanagariFont(doc: PDDocument): (PDFont, PDFont) =
    val regularStream = getClass.getResourceAsStream("/fonts/NotoSansDevanagari-Regular.ttf")
    val boldStream = getClass.getResourceAsStream("/fonts/NotoSansDevanagari-Bold.ttf")
    if regularStream != null && boldStream != null then
      try
        val regular = PDType0Font.load(doc, regularStream)
        val bold = PDType0Font.load(doc, boldStream)
        (regular, bold)
      finally
        regularStream.close()
        boldStream.close()
    else
      val fallback = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
      val fallbackBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
      (fallback, fallbackBold)

  import NotationColors.hexToRgb

  def exportPdf(composition: Composition, path: Path, landscape: Boolean = false): Unit =
    val doc = new PDDocument()
    try
      val pageSize = if landscape then
        new PDRectangle(PDRectangle.A4.getHeight, PDRectangle.A4.getWidth)
      else PDRectangle.A4

      val latinFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
      val latinBoldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
      val (devaFont, devaBoldFont) = loadDevanagariFont(doc)
      val margin = 50f
      val bottomMargin = margin + 30
      val showStroke = composition.metadata.showStrokeLine
      val showSahitya = composition.metadata.showSahityaLine

      var page = new PDPage(pageSize)
      doc.addPage(page)
      var cs = new PDPageContentStream(doc, page)
      var y = pageSize.getHeight - margin

      def ensureSpace(needed: Float): Unit =
        if y < bottomMargin + needed then
          writeFooter(cs, latinFont, margin)
          cs.close()
          page = new PDPage(pageSize)
          doc.addPage(page)
          cs = new PDPageContentStream(doc, page)
          y = pageSize.getHeight - margin

      def writeFooter(stream: PDPageContentStream, f: PDFont, m: Float): Unit =
        stream.setFont(f, 8)
        stream.setNonStrokingColor(0.5f, 0.5f, 0.5f)
        stream.beginText()
        stream.newLineAtOffset(m, margin - 10)
        stream.showText("Sangeet Notes Editor")
        stream.endText()

      def setColor(hex: String): Unit =
        val (r, g, b) = hexToRgb(hex)
        cs.setNonStrokingColor(r, g, b)

      def drawText(text: String, font: PDFont, size: Float, x: Float, yPos: Float): Float =
        val safe = ScriptUtil.sanitizeForFont(text)
        cs.setFont(font, size)
        cs.beginText()
        cs.newLineAtOffset(x, yPos)
        cs.showText(safe)
        cs.endText()
        font.getStringWidth(safe) / 1000f * size

      def drawMixedText(text: String, latFont: PDFont, nlFont: PDFont,
                        size: Float, x: Float, yPos: Float): Float =
        if !ScriptUtil.containsNonLatin(text) then
          drawText(text, latFont, size, x, yPos)
        else
          var xPos = x
          val runs = ScriptUtil.splitByScript(text)
          runs.foreach { (segment, isNL) =>
            val font = if isNL then nlFont else latFont
            val w = drawText(segment, font, size, xPos, yPos)
            xPos += w
          }
          xPos - x

      /** Draw a row of cell texts at given y position with cell spacing.
        * Returns the y after this row. */
      def drawCellRow(line: GridLine, cellTexts: IndexedSeq[String],
                      font: PDFont, nlFont: PDFont, size: Float,
                      colorHex: String, rowY: Float, cellWidth: Float): Unit =
        setColor(colorHex)
        for (text, i) <- cellTexts.zipWithIndex do
          if text.nonEmpty then
            val x = margin + i * cellWidth + cellWidth / 2
            // Center text in cell
            val textW = if ScriptUtil.containsNonLatin(text) then
              // Approximate - just render at center
              drawMixedText(text, font, nlFont, size, x - textWidth(text, font, size) / 2, rowY)
              0f // already drawn
            else
              val tw = textWidth(text, font, size)
              drawText(text, font, size, x - tw / 2, rowY)

      def textWidth(text: String, font: PDFont, size: Float): Float =
        val safe = ScriptUtil.sanitizeForFont(text)
        try font.getStringWidth(safe) / 1000f * size
        catch case _: Exception => size * text.length * 0.5f

      // --- Header ---
      setColor("#000000")
      drawMixedText(composition.metadata.title, latinBoldFont, devaBoldFont, 16, margin, y)
      y -= 22

      setColor("#333333")
      val raagText = s"Raag: ${composition.metadata.raag.name}" +
        composition.metadata.raag.thaat.map(t => s" ($t Thaat)").getOrElse("")
      drawMixedText(raagText, latinFont, devaFont, 11, margin, y)
      y -= 16

      composition.metadata.raag.arohana.foreach { ar =>
        val arText = s"Arohi: ${ar.mkString(" ")}"
        drawMixedText(arText, latinFont, devaFont, 11, margin, y)
        y -= 14
      }
      composition.metadata.raag.avarohana.foreach { av =>
        val avText = s"Avarohi: ${av.mkString(" ")}"
        drawMixedText(avText, latinFont, devaFont, 11, margin, y)
        y -= 14
      }

      val taalLine = s"Taal: ${composition.metadata.taal.name} (${composition.metadata.taal.matras} matras)" +
        composition.metadata.laya.map(l => s"  |  Laya: ${l.toString}").getOrElse("")
      drawMixedText(taalLine, latinFont, devaFont, 11, margin, y)
      y -= 20

      composition.metadata.instrument.foreach { inst =>
        drawMixedText(s"Instrument: $inst", latinFont, devaFont, 11, margin, y)
        y -= 16
      }

      y -= 10

      // --- Sections ---
      val config = LayoutConfig()
      val grids = GridLayout.layoutAll(composition, config)
      val script = DevanagariMap.currentScript
      val useDevanagariFont = script != SwarScript.English
      val cellWidth = 32f

      grids.foreach { grid =>
        ensureSpace(30)

        setColor("#444444")
        drawMixedText(grid.sectionName, latinBoldFont, devaBoldFont, 12, margin, y)
        y -= 16

        grid.lines.foreach { line =>
          // Calculate height needed for this line
          val lineHeight = 12f + 12f + // marker + swar rows
            (if showStroke then 10f else 0f) +
            (if showSahitya then 10f else 0f) +
            12f // ornament row
          ensureSpace(lineHeight + 8)

          val numCells = line.cells.size
          val markerMap = line.markers.toMap

          // Draw vibhag separator lines
          val (sr, sg, sb_) = hexToRgb("#888888")
          cs.setStrokingColor(sr, sg, sb_)
          cs.setLineWidth(0.5f)
          line.vibhagBreaks.foreach { breakIdx =>
            val lx = margin + breakIdx * cellWidth
            cs.moveTo(lx, y + 4)
            cs.lineTo(lx, y - lineHeight + 4)
            cs.stroke()
          }

          // 1. Taal marker row
          for i <- 0 until numCells do
            markerMap.get(i).foreach { marker =>
              val color = if marker == VibhagMarker.Sam then NotationColors.taalMarkerSam
                          else NotationColors.taalMarker
              setColor(color)
              val text = DevanagariMap.vibhagMarkerText(marker)
              val x = margin + i * cellWidth + cellWidth / 2
              val tw = textWidth(text, latinFont, 8f)
              drawText(text, latinFont, 8f, x - tw / 2, y)
            }
          y -= 12

          // 2. Ornament row
          if GridLineUtil.hasOrnaments(line) then
            setColor(NotationColors.ornament)
            for (cell, i) <- line.cells.zipWithIndex do
              val ornText = cell.events.collect {
                case s: Event.Swar if s.ornaments.nonEmpty =>
                  s.ornaments.map(OrnamentLabels.abbreviated).mkString(" ")
              }.mkString
              if ornText.nonEmpty then
                val x = margin + i * cellWidth + cellWidth / 2
                val tw = textWidth(ornText, latinFont, 6f)
                drawText(ornText, latinFont, 6f, x - tw / 2, y)
            y -= 10

          // 3. Swar row
          for (cell, i) <- line.cells.zipWithIndex do
            val x = margin + i * cellWidth + cellWidth / 2
            cell.events.zipWithIndex.foreach { (event, evtIdx) =>
              val evtX = if cell.events.size == 1 then x
                         else x - cellWidth / 4 + evtIdx * cellWidth / cell.events.size
              event match
                case s: Event.Swar =>
                  val glyph = ScriptMap.glyph(s.note, script)

                  // Octave dot above (taar)
                  val dotAbove = s.octave match
                    case Octave.Taar    => "."
                    case Octave.AtiTaar => ".."
                    case _ => ""
                  if dotAbove.nonEmpty then
                    setColor(NotationColors.octaveDot)
                    val tw = textWidth(dotAbove, latinFont, 8f)
                    drawText(dotAbove, latinFont, 8f, evtX - tw / 2, y + 8)

                  // Swar glyph
                  setColor(NotationColors.swar)
                  val glyphFont = if useDevanagariFont && glyph.exists(ScriptUtil.isIndicChar) then devaFont else latinFont
                  val tw = textWidth(glyph, glyphFont, 10f)
                  drawText(glyph, glyphFont, 10f, evtX - tw / 2, y)

                  // Variant mark
                  if s.variant == Variant.Komal then
                    val (kr, kg, kb) = hexToRgb(NotationColors.swar)
                    cs.setStrokingColor(kr, kg, kb)
                    cs.setLineWidth(0.6f)
                    cs.moveTo(evtX - 5, y - 1.5f)
                    cs.lineTo(evtX + 5, y - 1.5f)
                    cs.stroke()
                  else if s.variant == Variant.Tivra then
                    val (kr, kg, kb) = hexToRgb(NotationColors.swar)
                    cs.setStrokingColor(kr, kg, kb)
                    cs.setLineWidth(0.6f)
                    cs.moveTo(evtX - 1, y + 10)
                    cs.lineTo(evtX - 1, y + 5)
                    cs.stroke()

                  // Octave dot below (mandra)
                  val dotBelow = s.octave match
                    case Octave.Mandra    => "."
                    case Octave.AtiMandra => ".."
                    case _ => ""
                  if dotBelow.nonEmpty then
                    setColor(NotationColors.octaveDot)
                    val dtw = textWidth(dotBelow, latinFont, 8f)
                    drawText(dotBelow, latinFont, 8f, evtX - dtw / 2, y - 8)

                case _: Event.Rest =>
                  setColor(NotationColors.rest)
                  drawText("-", latinFont, 10f, evtX - 2, y)

                case _: Event.Sustain =>
                  setColor(NotationColors.sustain)
                  drawText("--", latinFont, 10f, evtX - 4, y)
            }
          y -= 14

          // 4. Da/Ra stroke row
          if showStroke then
            var swarCounter = 0
            setColor(NotationColors.stroke)
            for (cell, i) <- line.cells.zipWithIndex do
              val strokeTexts = cell.events.collect {
                case s: Event.Swar =>
                  val st = s.stroke.getOrElse(if swarCounter % 2 == 0 then Stroke.Da else Stroke.Ra)
                  swarCounter += 1
                  DevanagariMap.strokeText(st)
              }
              if strokeTexts.nonEmpty then
                val text = strokeTexts.mkString(" ")
                val x = margin + i * cellWidth + cellWidth / 2
                val stFont = if useDevanagariFont && text.exists(ScriptUtil.isIndicChar) then devaFont else latinFont
                val tw = textWidth(text, stFont, 7f)
                drawText(text, stFont, 7f, x - tw / 2, y)
            y -= 10

          // 5. Sahitya row
          if showSahitya then
            if GridLineUtil.hasSahitya(line) then
              setColor(NotationColors.sahitya)
              for (cell, i) <- line.cells.zipWithIndex do
                val text = cell.events.collect {
                  case s: Event.Swar if s.sahitya.isDefined => s.sahitya.get
                }.mkString
                if text.nonEmpty then
                  val x = margin + i * cellWidth + cellWidth / 2
                  val sahFont = if text.exists(ScriptUtil.isIndicChar) then devaFont else latinFont
                  val tw = textWidth(text, sahFont, 7f)
                  drawText(text, sahFont, 7f, x - tw / 2, y)
              y -= 10

          y -= 4 // line spacing
        }

        y -= 8
      }

      writeFooter(cs, latinFont, margin)
      cs.close()

      doc.save(path.toFile)
    finally
      doc.close()

