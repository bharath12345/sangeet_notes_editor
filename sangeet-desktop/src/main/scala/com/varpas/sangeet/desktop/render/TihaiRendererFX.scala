package com.varpas.sangeet.desktop.render

import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.text.TextAlignment

/** Renders tihai bracket markers ("x3") on a ScalaFX canvas.
  * A tihai is a rhythmic phrase repeated 3 times, landing on sam. */
object TihaiRendererFX:

  def draw(gc: GraphicsContext, startX: Double, endX: Double, y: Double): Unit =
    gc.save()
    gc.stroke = Color.DarkOrange
    gc.lineWidth = 1.5

    val bracketY = y - 35
    val tickHeight = 5

    // Horizontal bracket line with vertical ticks at ends
    gc.strokeLine(startX, bracketY, endX, bracketY)
    gc.strokeLine(startX, bracketY - tickHeight, startX, bracketY + tickHeight)
    gc.strokeLine(endX, bracketY - tickHeight, endX, bracketY + tickHeight)

    // "x3" label centered above the bracket
    gc.font = FontCache.font("System Bold", 10)
    gc.setTextAlign(TextAlignment.Center)
    gc.fill = Color.DarkOrange
    gc.fillText("x3", (startX + endX) / 2, bracketY - 5)

    gc.restore()
