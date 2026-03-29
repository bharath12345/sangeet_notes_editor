package sangeet.render

import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, TextAlignment}
import sangeet.model.*
import sangeet.layout.*

  /** Per-line info for click-to-beat mapping */
  case class LineBounds(startY: Double, endY: Double, startX: Double, cellWidth: Double,
                        firstBeat: Int, cellCount: Int, cycle: Int)

  /** Y-range for each section with per-line detail */
  case class SectionBounds(sectionIndex: Int, startY: Double, endY: Double,
                           lines: List[LineBounds] = Nil)

object CanvasRenderer:

  /** Render and return section bounds for click handling.
    * @param strokeEditMode if true, cursor draws on the stroke line instead of swar line */
  def render(canvas: Canvas, composition: Composition, config: LayoutConfig,
             cursorPos: Option[(Int, Int, Int)] = None,
             cursorVisible: Boolean = true,
             strokeEditMode: Boolean = false): List[SectionBounds] =
    val gc = canvas.graphicsContext2D
    gc.clearRect(0, 0, canvas.width.value, canvas.height.value)

    var y = 20.0
    val x = 30.0
    val boundsBuilder = List.newBuilder[SectionBounds]

    // Header is rendered by CompositionHeader panel, not on canvas
    val grids = GridLayout.layoutAll(composition, config)
    val showSectionNames = grids.size > 1
    grids.zipWithIndex.foreach { (grid, sectionIdx) =>
      val sectionCursor = cursorPos.collect {
        case (si, cycle, beat) if si == sectionIdx => (cycle, beat)
      }
      val isActive = cursorPos.exists(_._1 == sectionIdx)
      val sectionStartY = y

      // Compute per-line bounds for click-to-beat mapping
      val showStroke = composition.metadata.showStrokeLine
      val showSahitya = composition.metadata.showSahityaLine
      val lh = GridRenderer.lineHeight(showStroke, showSahitya)
      var lineY = sectionStartY + (if showSectionNames then 25 else 0)
      val linesBounds = grid.lines.map { line =>
        val cycle = line.cells.headOption.map(_.position.cycle).getOrElse(0)
        val firstBeat = line.cells.headOption.map(_.position.beat).getOrElse(0)
        val lb = LineBounds(lineY, lineY + lh, x, config.cellWidthBase, firstBeat, line.cells.size, cycle)
        lineY += lh + config.lineSpacing
        lb
      }

      y = GridRenderer.drawSection(gc, grid, config, x, y, sectionCursor, showSectionNames, isActive, cursorVisible,
        showStroke, showSahitya, strokeEditMode)
      boundsBuilder += SectionBounds(sectionIdx, sectionStartY, y, linesBounds)
      y += 10
    }
    boundsBuilder.result()

  def drawHeader(gc: GraphicsContext, meta: Metadata, x: Double, startY: Double): Double =
    var y = startY
    gc.save()
    gc.font = Font("System Bold", 16)
    gc.fill = Color.Black
    gc.fillText(meta.title, x, y)
    y += 22

    gc.font = Font("System", 13)
    gc.fillText(s"Raag: ${meta.raag.name}" +
      meta.raag.thaat.map(t => s" ($t Thaat)").getOrElse(""), x, y)
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
