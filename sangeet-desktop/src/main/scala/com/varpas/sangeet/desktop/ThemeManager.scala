package com.varpas.sangeet.desktop

import javafx.scene.Scene

import scala.jdk.CollectionConverters._

object ThemeManager:

  enum Theme:
    case Light, Dark

  private var current: Theme = Theme.Light

  def get: Theme = current

  private def resourcePath(theme: Theme): Option[String] =
    val name = theme match
      case Theme.Light => "/themes/sangeet-light.css"
      case Theme.Dark  => "/themes/sangeet-dark.css"
    Option(getClass.getResource(name)).map(_.toExternalForm)

  /** Apply the given theme to the scene. Removes other theme stylesheets first. */
  def apply(scene: Scene, theme: Theme): Unit =
    current = theme
    val sheets = scene.getStylesheets
    val toRemove = sheets.asScala.filter { s =>
      s.endsWith("sangeet-light.css") || s.endsWith("sangeet-dark.css")
    }.toList
    toRemove.foreach(sheets.remove)
    resourcePath(theme).foreach(sheets.add)

  def toggle(scene: Scene): Theme =
    val next = current match
      case Theme.Light => Theme.Dark
      case Theme.Dark  => Theme.Light
    apply(scene, next)
    next

  def fromName(s: String): Theme = s.toLowerCase match
    case "dark" => Theme.Dark
    case _      => Theme.Light

  def name(t: Theme): String = t match
    case Theme.Light => "light"
    case Theme.Dark  => "dark"
