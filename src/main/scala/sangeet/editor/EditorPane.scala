package sangeet.editor

import scalafx.scene.layout.StackPane
import scalafx.scene.canvas.Canvas
import sangeet.model.*
import sangeet.layout.LayoutConfig
import sangeet.render.CanvasRenderer

class EditorPane extends StackPane:
  private val canvas = new Canvas(1100, 700)
  children = List(canvas)

  private var currentComposition: Option[Composition] = None
  private val config = LayoutConfig()

  def setComposition(comp: Composition): Unit =
    currentComposition = Some(comp)
    redraw()

  def redraw(): Unit =
    currentComposition.foreach { comp =>
      CanvasRenderer.render(canvas, comp, config)
    }
