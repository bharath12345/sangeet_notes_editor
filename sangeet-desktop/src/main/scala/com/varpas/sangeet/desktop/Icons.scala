package com.varpas.sangeet.desktop

import scalafx.scene.Node
import scalafx.scene.control.Label

import org.kordamp.ikonli.javafx.FontIcon

/** Shared helper for Material Design icons rendered via Ikonli. */
object Icons:

  // Warm-brown accent matching the app palette.
  private val DefaultColor = javafx.scene.paint.Color.web("#5A2828")

  /** Build an icon as a Label-wrapped FontIcon (ScalaFX-compatible Node). `code` is an Ikonli string code, e.g.
    * "mdi2f-folder-outline".
    */
  def make(code: String, size: Int = 16, color: javafx.scene.paint.Color = DefaultColor): Node =
    val fi = new FontIcon(code)
    fi.setIconSize(size)
    fi.setIconColor(color)
    val label = new Label
    label.delegate.setGraphic(fi)
    label

  /** Raw FontIcon — use when you need a JavaFX Node directly (e.g., TreeCell graphic). The Label wrapper isn't always
    * desirable: it adds tiny padding.
    */
  def jfx(code: String, size: Int = 14, color: javafx.scene.paint.Color = DefaultColor): FontIcon =
    val fi = new FontIcon(code)
    fi.setIconSize(size)
    fi.setIconColor(color)
    fi
