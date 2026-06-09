package com.varpas.sangeet.desktop.render

import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.paint.Color

import com.varpas.sangeet.core.layout.{LayoutConfig, SectionGrid}
import com.varpas.sangeet.core.model._

/** Per-line info for click-to-beat mapping */
case class LineBounds(
    startY: Double,
    endY: Double,
    startX: Double,
    cellWidth: Double,
    firstBeat: Int,
    cellCount: Int,
    cycle: Int
)

/** Y-range for each section with per-line detail */
case class SectionBounds(
    sectionIndex: Int,
    startY: Double,
    endY: Double,
    lines: List[LineBounds] = Nil
)

/** Orchestrates GridRendererFX for each section in a composition. Returns List[SectionBounds] for click-to-beat
  * mapping.
  */
object CanvasRendererFX:

  /** Render all sections and return section bounds for click handling.
    * @param strokeEditMode
    *   if true, cursor draws on the stroke line instead of swar line
    */
  def render(
      canvas: Canvas,
      composition: Composition,
      grids: List[SectionGrid],
      config: LayoutConfig,
      cursorPos: Option[(Int, Int, Int)] = None,
      cursorVisible: Boolean = true,
      strokeEditMode: Boolean = false,
      script: SwarScript = SwarScript.Devanagari,
      readOnly: Boolean = false,
      selectionRange: Option[((Int, Int), (Int, Int))] = None
  ): List[SectionBounds] =
    val gc = canvas.graphicsContext2D
    gc.clearRect(0, 0, canvas.width.value, canvas.height.value)

    var y = 20.0

    if readOnly then
      gc.save()
      gc.font = FontCache.font("System", 13)
      gc.fill = Color.rgb(200, 40, 40)
      gc.fillText("Read-only sample.  To start editing, use File → New to create a composition.", 60, y)
      gc.restore()
      y += 24
    val leftMargin       = 60.0
    val maxCells         = grids.flatMap(_.lines.map(_.cells.size)).maxOption.getOrElse(1)
    val dynamicCellWidth = (canvas.width.value - leftMargin - 10) / maxCells
    val effectiveConfig  = config.copy(cellWidthBase = dynamicCellWidth)
    val x                = leftMargin
    val boundsBuilder    = List.newBuilder[SectionBounds]

    val showSectionNames = grids.size > 1
    grids.zipWithIndex.foreach { (grid, sectionIdx) =>
      val sectionCursor = cursorPos.collect {
        case (si, cycle, beat) if si == sectionIdx => (cycle, beat)
      }
      val isActive      = cursorPos.exists(_._1 == sectionIdx)
      val sectionStartY = y

      // Compute per-line bounds for click-to-beat mapping
      val showStroke  = composition.metadata.showStrokeLine
      val showSahitya = composition.metadata.showSahityaLine
      val lh          = GridRendererFX.lineHeight(showStroke, showSahitya)
      var lineY       = sectionStartY + (if showSectionNames then 25 else 0)
      val linesBounds = grid.lines.map { line =>
        val cycle     = line.cells.headOption.map(_.position.cycle).getOrElse(0)
        val firstBeat = line.cells.headOption.map(_.position.beat).getOrElse(0)
        val lb = LineBounds(lineY, lineY + lh, x, effectiveConfig.cellWidthBase, firstBeat, line.cells.size, cycle)
        lineY += lh + effectiveConfig.lineSpacing
        lb
      }

      val sectionSelection = if isActive then selectionRange else None
      y = GridRendererFX.drawSection(
        gc,
        grid,
        effectiveConfig,
        x,
        y,
        sectionCursor,
        showSectionNames,
        isActive,
        cursorVisible,
        showStroke,
        showSahitya,
        strokeEditMode,
        script,
        sectionSelection
      )
      boundsBuilder += SectionBounds(sectionIdx, sectionStartY, y, linesBounds)
      y += 10
    }
    boundsBuilder.result()

  /** Draw composition header (title, raag, taal info) */
  def drawHeader(gc: GraphicsContext, meta: Metadata, x: Double, startY: Double): Double =
    var y = startY
    gc.save()
    gc.font = FontCache.font("System Bold", 16)
    gc.fill = Color.Black
    gc.fillText(meta.title, x, y)
    y += 22

    gc.font = FontCache.font("System", 13)
    gc.fillText(
      s"Raag: ${meta.raag.name}" +
        meta.raag.thaat.map(t => s" ($t Thaat)").getOrElse(""),
      x,
      y
    )
    y += 18

    meta.raag.arohana.foreach { ar =>
      gc.fillText(s"Arohi:   ${ar.mkString(" ")}", x, y)
      y += 16
    }
    meta.raag.avarohana.foreach { av =>
      gc.fillText(s"Avarohi: ${av.mkString(" ")}", x, y)
      y += 16
    }

    val vadiLine = List(
      meta.raag.vadi.map(v => s"Vadi: $v"),
      meta.raag.samvadi.map(s => s"Samvadi: $s")
    ).flatten.mkString("  |  ")
    if vadiLine.nonEmpty then
      gc.fillText(vadiLine, x, y)
      y += 16

    val taalLine = s"Taal: ${meta.taal.name} (${meta.taal.matras} matras)" +
      meta.laya.map(l => s"  |  Laya: ${l.toString}").getOrElse("")
    gc.fillText(taalLine, x, y)
    y += 16

    val composerLine = List(
      meta.composer.map(c => s"Composer: $c"),
      meta.source.map(s => s"Source: $s")
    ).flatten.mkString("  |  ")
    if composerLine.nonEmpty then
      gc.fillText(composerLine, x, y)
      y += 16

    gc.restore()
    y
