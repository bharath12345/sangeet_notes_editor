package sangeet.editor

import scalafx.scene.layout.StackPane
import scalafx.scene.canvas.Canvas
import scalafx.scene.paint.Color

class EditorPane extends StackPane:
  private val canvas = new Canvas(1100, 700)
  children = List(canvas)

  // Draw a placeholder message
  private val gc = canvas.graphicsContext2D
  gc.fill = Color.Black
  gc.font = new scalafx.scene.text.Font("System", 18)
  gc.fillText("Sangeet Notes Editor — canvas ready", 50, 50)
