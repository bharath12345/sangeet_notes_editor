package sangeet.format

import org.apache.pdfbox.pdmodel.{PDDocument, PDPage, PDPageContentStream}
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.{PDType1Font, Standard14Fonts}
import sangeet.model.*
import sangeet.layout.*
import java.nio.file.Path

object PdfExport:

  def exportPdf(composition: Composition, path: Path, landscape: Boolean = false): Unit =
    val doc = new PDDocument()
    try
      val pageSize = if landscape then
        new PDRectangle(PDRectangle.A4.getHeight, PDRectangle.A4.getWidth)
      else PDRectangle.A4

      val font = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
      val boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
      val margin = 50f
      val bottomMargin = margin + 30

      var page = new PDPage(pageSize)
      doc.addPage(page)
      var cs = new PDPageContentStream(doc, page)
      var y = pageSize.getHeight - margin

      def ensureSpace(needed: Float): Unit =
        if y < bottomMargin + needed then
          // Write footer on current page
          writeFooter(cs, font, margin)
          cs.close()
          // Start new page
          page = new PDPage(pageSize)
          doc.addPage(page)
          cs = new PDPageContentStream(doc, page)
          y = pageSize.getHeight - margin

      def writeFooter(stream: PDPageContentStream, f: PDType1Font, m: Float): Unit =
        stream.setFont(f, 8)
        stream.beginText()
        stream.newLineAtOffset(m, margin - 10)
        stream.showText("Sangeet Notes Editor")
        stream.endText()

      // Title
      cs.setFont(boldFont, 16)
      cs.beginText()
      cs.newLineAtOffset(margin, y)
      cs.showText(composition.metadata.title)
      cs.endText()
      y -= 22

      // Raag info
      cs.setFont(font, 11)
      cs.beginText()
      cs.newLineAtOffset(margin, y)
      cs.showText(s"Raag: ${composition.metadata.raag.name}" +
        composition.metadata.raag.thaat.map(t => s" ($t Thaat)").getOrElse(""))
      cs.endText()
      y -= 16

      // Arohi / Avarohi
      composition.metadata.raag.arohana.foreach { ar =>
        cs.beginText()
        cs.newLineAtOffset(margin, y)
        cs.showText(s"Arohi: ${ar.mkString(" ")}")
        cs.endText()
        y -= 14
      }
      composition.metadata.raag.avarohana.foreach { av =>
        cs.beginText()
        cs.newLineAtOffset(margin, y)
        cs.showText(s"Avarohi: ${av.mkString(" ")}")
        cs.endText()
        y -= 14
      }

      // Taal and Laya
      val taalLine = s"Taal: ${composition.metadata.taal.name} (${composition.metadata.taal.matras} matras)" +
        composition.metadata.laya.map(l => s"  |  Laya: ${l.toString}").getOrElse("")
      cs.beginText()
      cs.newLineAtOffset(margin, y)
      cs.showText(taalLine)
      cs.endText()
      y -= 20

      // Instrument
      composition.metadata.instrument.foreach { inst =>
        cs.beginText()
        cs.newLineAtOffset(margin, y)
        cs.showText(s"Instrument: $inst")
        cs.endText()
        y -= 16
      }

      y -= 10 // spacing before sections

      // Sections via layout engine
      val config = LayoutConfig()
      val grids = GridLayout.layoutAll(composition, config)

      grids.foreach { grid =>
        ensureSpace(30)

        cs.setFont(boldFont, 12)
        cs.beginText()
        cs.newLineAtOffset(margin, y)
        cs.showText(grid.sectionName)
        cs.endText()
        y -= 18

        cs.setFont(font, 10)
        grid.lines.foreach { line =>
          ensureSpace(14)

          val cellTexts = line.cells.map { cell =>
            cell.events.map(renderEvent).mkString(" ")
          }
          val lineText = cellTexts.mkString(" | ")
          cs.beginText()
          cs.newLineAtOffset(margin, y)
          cs.showText(lineText)
          cs.endText()
          y -= 14
        }

        y -= 8 // spacing between sections
      }

      // Footer on last page
      writeFooter(cs, font, margin)
      cs.close()

      doc.save(path.toFile)
    finally
      doc.close()

  private def renderEvent(event: Event): String = event match
    case s: Event.Swar =>
      val variant = s.variant match
        case Variant.Komal => "(k)"
        case Variant.Tivra => "(t)"
        case _ => ""
      val octave = s.octave match
        case Octave.Mandra => "."
        case Octave.Taar => "'"
        case Octave.AtiMandra => ".."
        case Octave.AtiTaar => "''"
        case _ => ""
      s"${s.note}$variant$octave"
    case _: Event.Rest => "-"
    case _: Event.Sustain => "--"
