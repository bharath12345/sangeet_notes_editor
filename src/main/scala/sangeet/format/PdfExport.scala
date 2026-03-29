package sangeet.format

import org.apache.pdfbox.pdmodel.{PDDocument, PDPage, PDPageContentStream}
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.{PDFont, PDType0Font, PDType1Font, Standard14Fonts}
import sangeet.model.*
import sangeet.layout.*
import sangeet.render.{DevanagariMap, ScriptMap}
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
      // Fallback: no Devanagari font bundled
      val fallback = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
      val fallbackBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
      (fallback, fallbackBold)

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
        stream.beginText()
        stream.newLineAtOffset(m, margin - 10)
        stream.showText("Sangeet Notes Editor")
        stream.endText()

      def drawText(text: String, font: PDFont, size: Float, x: Float, yPos: Float): Float =
        cs.setFont(font, size)
        cs.beginText()
        cs.newLineAtOffset(x, yPos)
        cs.showText(text)
        cs.endText()
        font.getStringWidth(text) / 1000f * size

      // Title — use Devanagari font if title contains non-ASCII, otherwise Latin
      val titleFont = if containsNonLatin(composition.metadata.title) then devaBoldFont else latinBoldFont
      drawText(composition.metadata.title, titleFont, 16, margin, y)
      y -= 22

      // Raag info
      val raagText = s"Raag: ${composition.metadata.raag.name}" +
        composition.metadata.raag.thaat.map(t => s" ($t Thaat)").getOrElse("")
      val raagFont = if containsNonLatin(raagText) then devaFont else latinFont
      drawText(raagText, raagFont, 11, margin, y)
      y -= 16

      // Arohi / Avarohi
      composition.metadata.raag.arohana.foreach { ar =>
        val arText = s"Arohi: ${ar.mkString(" ")}"
        val arFont = if containsNonLatin(arText) then devaFont else latinFont
        drawText(arText, arFont, 11, margin, y)
        y -= 14
      }
      composition.metadata.raag.avarohana.foreach { av =>
        val avText = s"Avarohi: ${av.mkString(" ")}"
        val avFont = if containsNonLatin(avText) then devaFont else latinFont
        drawText(avText, avFont, 11, margin, y)
        y -= 14
      }

      // Taal and Laya
      val taalLine = s"Taal: ${composition.metadata.taal.name} (${composition.metadata.taal.matras} matras)" +
        composition.metadata.laya.map(l => s"  |  Laya: ${l.toString}").getOrElse("")
      drawText(taalLine, latinFont, 11, margin, y)
      y -= 20

      // Instrument
      composition.metadata.instrument.foreach { inst =>
        drawText(s"Instrument: $inst", latinFont, 11, margin, y)
        y -= 16
      }

      y -= 10 // spacing before sections

      // Sections via layout engine
      val config = LayoutConfig()
      val grids = GridLayout.layoutAll(composition, config)
      val script = DevanagariMap.currentScript
      val useDevanagariFont = script != SwarScript.English

      grids.foreach { grid =>
        ensureSpace(30)

        // Section name
        val sectionFont = if containsNonLatin(grid.sectionName) then devaBoldFont else latinBoldFont
        drawText(grid.sectionName, sectionFont, 12, margin, y)
        y -= 18

        val swarFontSize = 10f
        val sepText = " | "

        grid.lines.foreach { line =>
          ensureSpace(14)

          // Render each cell with proper font, separated by pipes
          var xPos = margin
          line.cells.zipWithIndex.foreach { (cell, idx) =>
            if idx > 0 then
              // Draw separator in Latin font
              val sepWidth = drawText(sepText, latinFont, swarFontSize, xPos, y)
              xPos += sepWidth

            val cellText = cell.events.map(e => renderEventForScript(e, script)).mkString(" ")
            val cellFont = if useDevanagariFont && containsNonLatin(cellText) then devaFont else latinFont
            val cellWidth = drawText(cellText, cellFont, swarFontSize, xPos, y)
            xPos += cellWidth
          }
          y -= 14
        }

        y -= 8
      }

      writeFooter(cs, latinFont, margin)
      cs.close()

      doc.save(path.toFile)
    finally
      doc.close()

  /** Check if string contains characters outside basic Latin (> 0xFF) */
  private def containsNonLatin(s: String): Boolean =
    s.exists(_.toInt > 0xFF)

  /** Render event using the current script's glyphs.
    * Uses simple text markers for variant/octave that are safe for all fonts. */
  private def renderEventForScript(event: Event, script: SwarScript): String = event match
    case s: Event.Swar =>
      val glyph = ScriptMap.glyph(s.note, script)
      val variant = s.variant match
        case Variant.Komal => "(k)"
        case Variant.Tivra => "(t)"
        case _ => ""
      val octave = s.octave match
        case Octave.Mandra    => "."
        case Octave.Taar      => "'"
        case Octave.AtiMandra => ".."
        case Octave.AtiTaar   => "''"
        case _ => ""
      s"$glyph$variant$octave"
    case _: Event.Rest    => "-"
    case _: Event.Sustain => "--"
