package com.varpas.sangeet.desktop.editor

import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{Label, ListView}
import scalafx.scene.layout.{Priority, VBox}

class StatusBar extends VBox:
  prefHeight = 120
  minHeight = 60
  padding = Insets(2, 5, 2, 5)
  style = "-fx-border-color: #ccc; -fx-border-width: 1 0 0 0; -fx-background-color: #f8f8f8;"

  private val logItems = ObservableBuffer[String]()

  private val headerLabel = new Label("Log"):
    style = "-fx-font-size: 10px; -fx-text-fill: #666;"

  private val logView = new ListView[String]:
    items = logItems
    style = "-fx-font-size: 11px; -fx-font-family: monospace;"
    VBox.setVgrow(this, Priority.Always)

  children = List(headerLabel, logView)

  def log(message: String): Unit =
    AppLogger.info(message)
    logItems.insert(0, message)
    if logItems.size > 100 then logItems.removeRange(100, logItems.size)
    logView.scrollTo(0)
