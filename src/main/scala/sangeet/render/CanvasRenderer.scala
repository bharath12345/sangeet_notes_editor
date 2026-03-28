package sangeet.render

import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, TextAlignment}
import sangeet.model.*
import sangeet.layout.*

object CanvasRenderer:

  def render(canvas: Canvas, composition: Composition, config: LayoutConfig): Unit =
    val gc = canvas.graphicsContext2D
    gc.clearRect(0, 0, canvas.width.value, canvas.height.value)

    var y = 20.0
    val x = 30.0

    y = drawHeader(gc, composition.metadata, x, y)
    y += 20

    val grids = GridLayout.layoutAll(composition, config)
    grids.foreach { grid =>
      y = GridRenderer.drawSection(gc, grid, config, x, y)
      y += 10
    }

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
