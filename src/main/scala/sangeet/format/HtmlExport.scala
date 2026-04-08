package sangeet.format

import sangeet.model.*
import sangeet.layout.*
import sangeet.render.{DevanagariMap, ScriptMap, NotationColors, OrnamentLabels, GridLineUtil}
import java.nio.file.{Path, Files}
import java.nio.charset.StandardCharsets

object HtmlExport:

  def exportHtml(composition: Composition, path: Path): Unit =
    val html = render(composition)
    Files.writeString(path, html, StandardCharsets.UTF_8)

  def render(composition: Composition): String =
    val meta = composition.metadata
    val script = DevanagariMap.currentScript
    val config = LayoutConfig()
    val grids = GridLayout.layoutAll(composition, config)
    val fontFamily = ScriptMap.fontName(script)
    val showStroke = meta.showStrokeLine
    val showSahitya = meta.showSahityaLine

    val sb = new StringBuilder
    sb.append(s"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>${esc(meta.title)}</title>
<style>
  body {
    font-family: '$fontFamily', 'Noto Sans Devanagari', sans-serif;
    max-width: 900px;
    margin: 40px auto;
    padding: 0 20px;
    color: #222;
    background: #fff;
  }
  .header {
    background: #f0efe8;
    border: 1px solid #ccc;
    border-radius: 6px;
    padding: 16px 20px;
    margin-bottom: 24px;
  }
  .header h1 {
    margin: 0 0 6px 0;
    font-size: 22px;
  }
  .header .meta-line {
    font-size: 13px;
    color: #333;
    margin: 2px 0;
  }
  .section {
    margin-bottom: 24px;
  }
  .section h2 {
    font-size: 16px;
    margin: 0 0 8px 0;
    color: #444;
    border-bottom: 1px solid #ddd;
    padding-bottom: 4px;
  }
  .grid-line {
    display: flex;
    align-items: stretch;
    margin-bottom: 0;
  }
  .beat-cell {
    min-width: 48px;
    padding: 2px 6px;
    text-align: center;
    font-size: 16px;
    border-right: 1px solid #e0e0e0;
    box-sizing: border-box;
  }
  .beat-cell:last-child {
    border-right: none;
  }
  .vibhag-break {
    border-right: 2px solid #888;
  }
  .marker-row .beat-cell {
    font-size: 11px;
    font-weight: bold;
    color: ${NotationColors.taalMarker};
    padding: 1px 6px;
    min-height: 14px;
  }
  .marker-row .sam {
    color: ${NotationColors.taalMarkerSam};
  }
  .ornament-row .beat-cell {
    font-size: 9px;
    color: ${NotationColors.ornament};
    padding: 0 6px;
    min-height: 12px;
  }
  .swar-row .beat-cell {
    color: ${NotationColors.swar};
    font-size: 16px;
    padding: 2px 6px;
  }
  .swar-row .komal {
    text-decoration: underline;
  }
  .swar-row .tivra {
    border-top: 2px solid ${NotationColors.swar};
    padding-top: 1px;
  }
  .swar-row .rest {
    color: ${NotationColors.rest};
  }
  .swar-row .sustain {
    color: ${NotationColors.sustain};
  }
  .octave-dot {
    color: ${NotationColors.octaveDot};
    font-size: 10px;
    font-weight: bold;
  }
  .stroke-row .beat-cell {
    font-size: 10px;
    color: ${NotationColors.stroke};
    padding: 0 6px;
    min-height: 14px;
  }
  .sahitya-row .beat-cell {
    font-size: 11px;
    color: ${NotationColors.sahitya};
    padding: 0 6px;
    min-height: 14px;
  }
  .cycle-group {
    border: 1px solid #ddd;
    border-radius: 4px;
    margin-bottom: 6px;
    overflow: hidden;
  }
  .row-label {
    min-width: 40px;
    max-width: 40px;
    font-size: 8px;
    color: #aaa;
    padding: 0 4px;
    display: flex;
    align-items: center;
  }
  .footer {
    margin-top: 40px;
    padding-top: 10px;
    border-top: 1px solid #ddd;
    font-size: 11px;
    color: #999;
  }
  @media print {
    body { margin: 20px; }
    .cycle-group { break-inside: avoid; }
  }
</style>
</head>
<body>
""")

    // Header
    sb.append("<div class=\"header\">\n")
    sb.append(s"<h1>${esc(meta.title)}</h1>\n")
    sb.append(s"""<div class="meta-line">Type: ${esc(meta.compositionType.toString)}</div>\n""")

    if meta.raag.name.nonEmpty then
      sb.append(s"""<div class="meta-line">Raag: ${esc(meta.raag.name)}</div>\n""")

    meta.raag.thaat.foreach { t =>
      sb.append(s"""<div class="meta-line">Thaat: ${esc(t)}</div>\n""")
    }
    meta.raag.arohana.foreach { ar =>
      sb.append(s"""<div class="meta-line">Arohan: ${esc(ar.mkString(" "))}</div>\n""")
    }
    meta.raag.avarohana.foreach { av =>
      sb.append(s"""<div class="meta-line">Avrohan: ${esc(av.mkString(" "))}</div>\n""")
    }

    val vadiParts = List(
      meta.raag.vadi.map(v => s"Vadi: $v"),
      meta.raag.samvadi.map(s => s"Samvadi: $s")
    ).flatten
    if vadiParts.nonEmpty then
      sb.append(s"""<div class="meta-line">${esc(vadiParts.mkString("  |  "))}</div>\n""")

    sb.append(s"""<div class="meta-line">Taal: ${esc(meta.taal.name)} (${meta.taal.matras} matras)</div>\n""")

    meta.laya.foreach { l =>
      sb.append(s"""<div class="meta-line">Laya: ${esc(l.toString)}</div>\n""")
    }

    sb.append("</div>\n\n")

    // Sections
    grids.foreach { grid =>
      sb.append(s"""<div class="section">\n""")
      sb.append(s"""<h2>${esc(grid.sectionName)}</h2>\n""")

      grid.lines.foreach { line =>
        sb.append("""<div class="cycle-group">""")
        sb.append("\n")

        val vibhagSet = line.vibhagBreaks.toSet
        val markerMap = line.markers.toMap
        val numCells = line.cells.size

        def cellClass(i: Int, extra: String = ""): String =
          val vb = if vibhagSet.contains(i) then " vibhag-break" else ""
          s"""class="beat-cell$vb$extra""""

        // 1. Taal marker row
        sb.append("""<div class="grid-line marker-row">""")
        for i <- 0 until numCells do
          val markerText = markerMap.get(i).map { m =>
            val cls = if m == VibhagMarker.Sam then " sam" else ""
            DevanagariMap.vibhagMarkerText(m)
          }.getOrElse("")
          val samCls = markerMap.get(i).collect { case VibhagMarker.Sam => " sam" }.getOrElse("")
          sb.append(s"""<div ${cellClass(i, samCls)}>$markerText</div>""")
        sb.append("</div>\n")

        // 2. Ornament row (show ornament labels if any swar has ornaments)
        if GridLineUtil.hasOrnaments(line) then
          sb.append("""<div class="grid-line ornament-row">""")
          for (cell, i) <- line.cells.zipWithIndex do
            val ornText = cell.events.collect {
              case s: Event.Swar if s.ornaments.nonEmpty =>
                s.ornaments.map(OrnamentLabels.full).mkString(" ")
            }.mkString(" ")
            sb.append(s"""<div ${cellClass(i)}>${esc(ornText)}</div>""")
          sb.append("</div>\n")

        // 3. Swar row (with octave dots and variant marks)
        sb.append("""<div class="grid-line swar-row">""")
        for (cell, i) <- line.cells.zipWithIndex do
          val content = if cell.events.isEmpty then "&nbsp;"
                        else cell.events.map(renderEvent(_, script)).mkString(" ")
          sb.append(s"""<div ${cellClass(i)}>$content</div>""")
        sb.append("</div>\n")

        // 4. Da/Ra stroke row
        if showStroke then
          var swarCounter = 0
          sb.append("""<div class="grid-line stroke-row">""")
          for (cell, i) <- line.cells.zipWithIndex do
            val strokeText = cell.events.collect {
              case s: Event.Swar =>
                val st = s.stroke.getOrElse(if swarCounter % 2 == 0 then Stroke.Da else Stroke.Ra)
                swarCounter += 1
                DevanagariMap.strokeText(st)
            }.mkString(" ")
            sb.append(s"""<div ${cellClass(i)}>$strokeText</div>""")
          sb.append("</div>\n")

        // 5. Sahitya row
        if showSahitya then
          if GridLineUtil.hasSahitya(line) then
            sb.append("""<div class="grid-line sahitya-row">""")
            for (cell, i) <- line.cells.zipWithIndex do
              val text = cell.events.collect {
                case s: Event.Swar if s.sahitya.isDefined => esc(s.sahitya.get)
              }.mkString(" ")
              sb.append(s"""<div ${cellClass(i)}>$text</div>""")
            sb.append("</div>\n")

        sb.append("</div>\n")
      }

      sb.append("</div>\n\n")
    }

    // Footer
    sb.append("""<div class="footer">Sangeet Notes Editor</div>""")
    sb.append("\n</body>\n</html>\n")
    sb.toString

  private def renderEvent(event: Event, script: SwarScript): String = event match
    case s: Event.Swar =>
      val glyph = ScriptMap.glyph(s.note, script)
      val variantClass = s.variant match
        case Variant.Komal => " komal"
        case Variant.Tivra => " tivra"
        case _ => ""
      val octaveAbove = s.octave match
        case Octave.Taar    => """<span class="octave-dot">&#x2022;</span>"""
        case Octave.AtiTaar => """<span class="octave-dot">&#x2022;&#x2022;</span>"""
        case _ => ""
      val octaveBelow = s.octave match
        case Octave.Mandra    => """<span class="octave-dot">&#x2022;</span>"""
        case Octave.AtiMandra => """<span class="octave-dot">&#x2022;&#x2022;</span>"""
        case _ => ""
      val above = if octaveAbove.nonEmpty then s"""<div style="line-height:1;font-size:8px">$octaveAbove</div>""" else ""
      val below = if octaveBelow.nonEmpty then s"""<div style="line-height:1;font-size:8px">$octaveBelow</div>""" else ""
      s"""$above<span class="swar-text$variantClass">$glyph</span>$below"""
    case _: Event.Rest =>
      s"""<span class="rest">${DevanagariMap.restSymbol}</span>"""
    case _: Event.Sustain =>
      s"""<span class="sustain">${DevanagariMap.sustainSymbol}</span>"""

  private def esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
      .replace("\"", "&quot;")
