package sangeet.editor

import scalafx.scene.control.{ListView, Label}
import scalafx.scene.layout.{VBox, Priority, HBox}
import scalafx.geometry.Insets
import scalafx.collections.ObservableBuffer

class StatusBar extends VBox:
  prefHeight = 120
  minHeight = 60
  padding = Insets(2, 5, 2, 5)
  style = "-fx-border-color: #ccc; -fx-border-width: 1 0 0 0; -fx-background-color: #f8f8f8;"

  private val logItems = ObservableBuffer[String]()

  private val headerLabel = new Label("Key Log"):
    style = "-fx-font-size: 10px; -fx-text-fill: #666;"

  private val logView = new ListView[String]:
    items = logItems
    style = "-fx-font-size: 11px; -fx-font-family: monospace;"
    VBox.setVgrow(this, Priority.Always)

  children = List(headerLabel, logView)

  def log(message: String): Unit =
    logItems.insert(0, message)
    if logItems.size > 100 then
      logItems.remove(100, logItems.size)
    logView.scrollTo(0)
