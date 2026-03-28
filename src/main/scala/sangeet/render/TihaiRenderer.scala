package sangeet.render

import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, TextAlignment}

object TihaiRenderer:

  def draw(gc: GraphicsContext, startX: Double, endX: Double, y: Double): Unit =
    gc.save()
    gc.stroke = Color.DarkOrange
    gc.lineWidth = 1.5

    val bracketY = y - 35
    val tickHeight = 5

    gc.strokeLine(startX, bracketY, endX, bracketY)
    gc.strokeLine(startX, bracketY - tickHeight, startX, bracketY + tickHeight)
    gc.strokeLine(endX, bracketY - tickHeight, endX, bracketY + tickHeight)

    gc.font = Font("System Bold", 10)
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = Color.DarkOrange
    gc.fillText("x3", (startX + endX) / 2, bracketY - 5)

    gc.restore()
