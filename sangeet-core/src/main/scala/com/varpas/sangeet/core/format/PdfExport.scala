package com.varpas.sangeet.core.format

import java.nio.file.Path

import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.{PDFont, PDType0Font, PDType1Font, Standard14Fonts}
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage, PDPageContentStream}

import com.varpas.sangeet.core.layout._
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.render.{
  GlyphMetrics,
  GridLineUtil,
  NotationColors,
  OrnamentLabels,
  ScriptMap,
  ScriptUtil
}

object PdfExport:

  private def loadDevanagariFont(doc: PDDocument): (PDFont, PDFont) =
    val regularStream = getClass.getResourceAsStream("/fonts/NotoSansDevanagari-Regular.ttf")
    val boldStream    = getClass.getResourceAsStream("/fonts/NotoSansDevanagari-Bold.ttf")
    if regularStream != null && boldStream != null then
      try
        val regular = PDType0Font.load(doc, regularStream)
        val bold    = PDType0Font.load(doc, boldStream)
        (regular, bold)
      finally
        regularStream.close()
        boldStream.close()
    else
      val fallback     = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
      val fallbackBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
      (fallback, fallbackBold)

  import NotationColors.hexToRgb

  private class PdfCtx(
      val doc: PDDocument,
      val pageSize: PDRectangle,
      val latinFont: PDFont,
      val latinBoldFont: PDFont,
      val devaFont: PDFont,
      val devaBoldFont: PDFont,
      val margin: Float,
      val useDevanagari: Boolean
  ):
    private val bottomMargin    = margin + 30
    var cs: PDPageContentStream = _
    var y: Float                = _
    var page: PDPage            = _

    def initFirstPage(): Unit =
      page = new PDPage(pageSize)
      doc.addPage(page)
      cs = new PDPageContentStream(doc, page)
      y = pageSize.getHeight - margin

    def ensureSpace(needed: Float): Unit =
      if y < bottomMargin + needed then
        writeFooter()
        cs.close()
        page = new PDPage(pageSize)
        doc.addPage(page)
        cs = new PDPageContentStream(doc, page)
        y = pageSize.getHeight - margin

    def writeFooter(): Unit =
      cs.setFont(latinFont, 8)
      cs.setNonStrokingColor(0.5f, 0.5f, 0.5f)
      cs.beginText()
      cs.newLineAtOffset(margin, margin - 10)
      cs.showText("Sangeet Notes Editor")
      cs.endText()

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

    def drawMixedText(text: String, latFont: PDFont, nlFont: PDFont, size: Float, x: Float, yPos: Float): Float =
      if !ScriptUtil.containsNonLatin(text) then drawText(text, latFont, size, x, yPos)
      else
        var xPos = x
        val runs = ScriptUtil.splitByScript(text)
        runs.foreach { (segment, isNL) =>
          val font = if isNL then nlFont else latFont
          val w    = drawText(segment, font, size, xPos, yPos)
          xPos += w
        }
        xPos - x

    def textWidth(text: String, font: PDFont, size: Float): Float =
      val safe = ScriptUtil.sanitizeForFont(text)
      try font.getStringWidth(safe) / 1000f * size
      catch case _: Exception => size * text.length * 0.5f

    def glyphFont(text: String): PDFont =
      if useDevanagari && text.exists(ScriptUtil.isIndicChar) then devaFont else latinFont

  def exportPdf(composition: Composition, path: Path, script: SwarScript, landscape: Boolean = false): Unit =
    val doc = new PDDocument()
    try
      val pageSize =
        if landscape then new PDRectangle(PDRectangle.A4.getHeight, PDRectangle.A4.getWidth)
        else PDRectangle.A4

      val latinFont                = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
      val latinBoldFont            = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
      val (devaFont, devaBoldFont) = loadDevanagariFont(doc)

      val ctx = new PdfCtx(
        doc,
        pageSize,
        latinFont,
        latinBoldFont,
        devaFont,
        devaBoldFont,
        margin = 50f,
        useDevanagari = script != SwarScript.English
      )
      ctx.initFirstPage()

      renderHeader(ctx, composition.metadata)

      val config      = LayoutConfig()
      val grids       = GridLayout.layoutAll(composition, config)
      val showStroke  = composition.metadata.showStrokeLine
      val showSahitya = composition.metadata.showSahityaLine
      val cellWidth   = 32f

      grids.foreach { grid =>
        ctx.ensureSpace(30)
        ctx.setColor("#444444")
        ctx.drawMixedText(grid.sectionName, latinBoldFont, devaBoldFont, 12, ctx.margin, ctx.y)
        ctx.y -= 16

        grid.lines.foreach { line =>
          renderGridLine(ctx, line, script, cellWidth, showStroke, showSahitya)
        }
        ctx.y -= 8
      }

      ctx.writeFooter()
      ctx.cs.close()
      doc.save(path.toFile)
    finally doc.close()

  private def renderHeader(ctx: PdfCtx, meta: Metadata): Unit =
    ctx.setColor("#000000")
    ctx.drawMixedText(meta.title, ctx.latinBoldFont, ctx.devaBoldFont, 16, ctx.margin, ctx.y)
    ctx.y -= 22

    ctx.setColor("#333333")
    val raagText = s"Raag: ${meta.raag.name}" +
      meta.raag.thaat.map(t => s" ($t Thaat)").getOrElse("")
    ctx.drawMixedText(raagText, ctx.latinFont, ctx.devaFont, 11, ctx.margin, ctx.y)
    ctx.y -= 16

    meta.raag.arohana.foreach { ar =>
      ctx.drawMixedText(s"Arohi: ${ar.mkString(" ")}", ctx.latinFont, ctx.devaFont, 11, ctx.margin, ctx.y)
      ctx.y -= 14
    }
    meta.raag.avarohana.foreach { av =>
      ctx.drawMixedText(s"Avarohi: ${av.mkString(" ")}", ctx.latinFont, ctx.devaFont, 11, ctx.margin, ctx.y)
      ctx.y -= 14
    }

    val taalLine = s"Taal: ${meta.taal.name} (${meta.taal.matras} matras)" +
      meta.laya.map(l => s"  |  Laya: ${l.toString}").getOrElse("")
    ctx.drawMixedText(taalLine, ctx.latinFont, ctx.devaFont, 11, ctx.margin, ctx.y)
    ctx.y -= 20

    meta.instrument.foreach { inst =>
      ctx.drawMixedText(s"Instrument: $inst", ctx.latinFont, ctx.devaFont, 11, ctx.margin, ctx.y)
      ctx.y -= 16
    }
    ctx.y -= 10

  private def renderGridLine(
      ctx: PdfCtx,
      line: GridLine,
      script: SwarScript,
      cellWidth: Float,
      showStroke: Boolean,
      showSahitya: Boolean
  ): Unit =
    val lineHeight = 12f + 12f +
      (if showStroke then 10f else 0f) +
      (if showSahitya then 10f else 0f) +
      12f
    ctx.ensureSpace(lineHeight + 8)

    val numCells  = line.cells.size
    val markerMap = line.markers.toMap

    val (sr, sg, sb_) = hexToRgb("#888888")
    ctx.cs.setStrokingColor(sr, sg, sb_)
    ctx.cs.setLineWidth(0.5f)
    line.vibhagBreaks.foreach { breakIdx =>
      val lx = ctx.margin + breakIdx * cellWidth
      ctx.cs.moveTo(lx, ctx.y + 4)
      ctx.cs.lineTo(lx, ctx.y - lineHeight + 4)
      ctx.cs.stroke()
    }

    // 1. Taal marker row
    for i <- 0 until numCells do
      markerMap.get(i).foreach { marker =>
        val color =
          if marker == VibhagMarker.Sam then NotationColors.taalMarkerSam
          else NotationColors.taalMarker
        ctx.setColor(color)
        val text = GlyphMetrics.vibhagMarkerText(marker)
        val x    = ctx.margin + i * cellWidth + cellWidth / 2
        val tw   = ctx.textWidth(text, ctx.latinFont, 8f)
        ctx.drawText(text, ctx.latinFont, 8f, x - tw / 2, ctx.y)
      }
    ctx.y -= 12

    // 2. Ornament row
    if GridLineUtil.hasOrnaments(line) then
      ctx.setColor(NotationColors.ornament)
      for (cell, i) <- line.cells.zipWithIndex do
        val ornText = cell.events.collect {
          case s: Event.Swar if s.ornaments.nonEmpty =>
            s.ornaments.map(OrnamentLabels.abbreviated).mkString(" ")
        }.mkString
        if ornText.nonEmpty then
          val x  = ctx.margin + i * cellWidth + cellWidth / 2
          val tw = ctx.textWidth(ornText, ctx.latinFont, 6f)
          ctx.drawText(ornText, ctx.latinFont, 6f, x - tw / 2, ctx.y)
      ctx.y -= 10

    // 3. Swar row
    for (cell, i) <- line.cells.zipWithIndex do
      val x = ctx.margin + i * cellWidth + cellWidth / 2
      cell.events.zipWithIndex.foreach { (event, evtIdx) =>
        val evtX =
          if cell.events.size == 1 then x
          else x - cellWidth / 4 + evtIdx * cellWidth / cell.events.size
        renderEvent(ctx, event, script, evtX)
      }
    ctx.y -= 14

    // 4. Da/Ra stroke row
    if showStroke then
      var swarCounter = 0
      ctx.setColor(NotationColors.stroke)
      for (cell, i) <- line.cells.zipWithIndex do
        val strokeTexts = cell.events.collect { case s: Event.Swar =>
          val st = s.stroke.getOrElse(if swarCounter % 2 == 0 then Stroke.Da else Stroke.Ra)
          swarCounter += 1
          GlyphMetrics.strokeText(st, script)
        }
        if strokeTexts.nonEmpty then
          val text   = strokeTexts.mkString(" ")
          val x      = ctx.margin + i * cellWidth + cellWidth / 2
          val stFont = ctx.glyphFont(text)
          val tw     = ctx.textWidth(text, stFont, 7f)
          ctx.drawText(text, stFont, 7f, x - tw / 2, ctx.y)
      ctx.y -= 10

    // 5. Sahitya row
    if showSahitya then
      if GridLineUtil.hasSahitya(line) then
        ctx.setColor(NotationColors.sahitya)
        for (cell, i) <- line.cells.zipWithIndex do
          val text = cell.events.collect {
            case s: Event.Swar if s.sahitya.isDefined => s.sahitya.get
          }.mkString
          if text.nonEmpty then
            val x       = ctx.margin + i * cellWidth + cellWidth / 2
            val sahFont = ctx.glyphFont(text)
            val tw      = ctx.textWidth(text, sahFont, 7f)
            ctx.drawText(text, sahFont, 7f, x - tw / 2, ctx.y)
        ctx.y -= 10

    ctx.y -= 4

  private def renderEvent(ctx: PdfCtx, event: Event, script: SwarScript, evtX: Float): Unit =
    event match
      case s: Event.Swar =>
        val glyph = ScriptMap.glyph(s.note, script)

        val dotAbove = s.octave match
          case Octave.Taar    => "."
          case Octave.AtiTaar => ".."
          case _              => ""
        if dotAbove.nonEmpty then
          ctx.setColor(NotationColors.octaveDot)
          val tw = ctx.textWidth(dotAbove, ctx.latinFont, 8f)
          ctx.drawText(dotAbove, ctx.latinFont, 8f, evtX - tw / 2, ctx.y + 8)

        ctx.setColor(NotationColors.swar)
        val font = ctx.glyphFont(glyph)
        val tw   = ctx.textWidth(glyph, font, 10f)
        ctx.drawText(glyph, font, 10f, evtX - tw / 2, ctx.y)

        if s.variant == Variant.Komal then
          val (kr, kg, kb) = hexToRgb(NotationColors.swar)
          ctx.cs.setStrokingColor(kr, kg, kb)
          ctx.cs.setLineWidth(0.6f)
          ctx.cs.moveTo(evtX - 5, ctx.y - 1.5f)
          ctx.cs.lineTo(evtX + 5, ctx.y - 1.5f)
          ctx.cs.stroke()
        else if s.variant == Variant.Tivra then
          val (kr, kg, kb) = hexToRgb(NotationColors.swar)
          ctx.cs.setStrokingColor(kr, kg, kb)
          ctx.cs.setLineWidth(0.6f)
          ctx.cs.moveTo(evtX - 1, ctx.y + 10)
          ctx.cs.lineTo(evtX - 1, ctx.y + 5)
          ctx.cs.stroke()

        val dotBelow = s.octave match
          case Octave.Mandra    => "."
          case Octave.AtiMandra => ".."
          case _                => ""
        if dotBelow.nonEmpty then
          ctx.setColor(NotationColors.octaveDot)
          val dtw = ctx.textWidth(dotBelow, ctx.latinFont, 8f)
          ctx.drawText(dotBelow, ctx.latinFont, 8f, evtX - dtw / 2, ctx.y - 8)

      case _: Event.Rest =>
        ctx.setColor(NotationColors.rest)
        ctx.drawText(GlyphMetrics.restSymbol, ctx.latinFont, 10f, evtX - 2, ctx.y)

      case _: Event.Sustain =>
        ctx.setColor(NotationColors.sustain)
        ctx.drawText("--", ctx.latinFont, 10f, evtX - 4, ctx.y)
