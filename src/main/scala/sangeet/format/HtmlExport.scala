package sangeet.format

import sangeet.model.*
import sangeet.layout.*
import sangeet.render.{DevanagariMap, ScriptMap}
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
    margin-bottom: 2px;
  }
  .beat-cell {
    min-width: 48px;
    padding: 4px 6px;
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
  .marker-row {
    display: flex;
    margin-bottom: 0;
  }
  .marker-cell {
    min-width: 48px;
    padding: 1px 6px;
    text-align: center;
    font-size: 11px;
    font-weight: bold;
    color: #666;
    box-sizing: border-box;
  }
  .cycle-group {
    border: 1px solid #ddd;
    border-radius: 4px;
    margin-bottom: 6px;
    overflow: hidden;
  }
  .swar-text {
    display: inline;
  }
  .komal {
    text-decoration: underline;
  }
  .tivra {
    border-top: 2px solid #222;
    padding-top: 1px;
  }
  .octave-dot {
    font-size: 8px;
    line-height: 1;
  }
  .rest { color: #888; }
  .sustain { color: #aaa; }
  .stroke {
    font-size: 10px;
    color: #666;
    display: block;
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

        // Marker row
        if line.markers.nonEmpty then
          val markerMap = line.markers.toMap
          sb.append("""<div class="marker-row">""")
          for i <- line.cells.indices do
            val markerText = markerMap.get(i).map(DevanagariMap.vibhagMarkerText).getOrElse("")
            sb.append(s"""<div class="marker-cell">$markerText</div>""")
          sb.append("</div>\n")

        // Beat cells
        val vibhagSet = line.vibhagBreaks.toSet
        sb.append("""<div class="grid-line">""")
        for (cell, i) <- line.cells.zipWithIndex do
          val extraClass = if vibhagSet.contains(i) then " vibhag-break" else ""
          val content = if cell.events.isEmpty then "&nbsp;"
                        else cell.events.map(renderEvent(_, script)).mkString(" ")
          sb.append(s"""<div class="beat-cell$extraClass">$content</div>""")
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
      val octaveDot = s.octave match
        case Octave.AtiMandra => """<span class="octave-dot">..</span>"""
        case Octave.Mandra    => """<span class="octave-dot">.</span>"""
        case Octave.Taar      => """<span class="octave-dot">'</span>"""
        case Octave.AtiTaar   => """<span class="octave-dot">''</span>"""
        case _ => ""
      val strokeHtml = s.stroke.map { st =>
        s"""<span class="stroke">${DevanagariMap.strokeText(st)}</span>"""
      }.getOrElse("")
      s"""<span class="swar-text$variantClass">$glyph$octaveDot</span>$strokeHtml"""
    case _: Event.Rest =>
      s"""<span class="rest">${DevanagariMap.restSymbol}</span>"""
    case _: Event.Sustain =>
      s"""<span class="sustain">${DevanagariMap.sustainSymbol}</span>"""

  private def esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
      .replace("\"", "&quot;")
