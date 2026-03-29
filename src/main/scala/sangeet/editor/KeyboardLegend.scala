package sangeet.editor

import scalafx.scene.control.{Label, ScrollPane, Separator}
import scalafx.scene.layout.{VBox, Priority}
import scalafx.geometry.Insets
import sangeet.model.SwarScript
import sangeet.render.ScriptMap

class KeyboardLegend extends ScrollPane:
  prefWidth = 400
  minWidth = 180
  fitToWidth = true
  hbarPolicy = ScrollPane.ScrollBarPolicy.Never

  private def heading(text: String) = new Label(text):
    style = "-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 0 2 0;"

  private def entry(key: String, desc: String) = new Label(s"$key  $desc"):
    style = "-fx-font-size: 11px; -fx-font-family: monospace; -fx-padding: 1 0 1 0;"
    wrapText = true

  private val legendBox = new VBox:
    spacing = 1
    padding = Insets(8, 10, 8, 10)
    style = "-fx-background-color: #f5f5f0; -fx-border-color: #ccc; -fx-border-width: 0 0 0 1;"

  content = legendBox

  // Initialize with default
  updateScript(SwarScript.Devanagari)

  def updateScript(script: SwarScript): Unit =
    val entries = ScriptMap.legendEntries(script)
    val swarEntries = entries.map { (key, desc, variant) =>
      val label = if variant.nonEmpty then s"$desc $variant" else desc
      entry(key, label)
    }

    legendBox.children = List(
      new Label(s"Keyboard Reference"):
        style = "-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 0 0 2 0;"
      ,
      new Label(s"Script: ${ScriptMap.displayName(script)}"):
        style = "-fx-font-size: 10px; -fx-text-fill: #555; -fx-padding: 0 0 4 0;"
      ,
      new Separator()
    ) ++ (heading("Swar (Notes)") :: swarEntries) ++ List(
      new Separator(),
      heading("Octave"),
      entry(".", "Next note in mandra"),
      entry("'", "Next note in taar"),
      entry("`", "Back to madhya"),

      new Separator(),
      heading("Special"),
      entry("Space", "Rest (silence)"),
      entry("-", "Sustain (hold)"),
      entry("Del", "Delete last note"),

      new Separator(),
      heading("Navigation"),
      entry("\u2190 \u2192", "Move cursor"),

      new Separator(),
      heading("Strokes (Mizrab)"),
      entry("Ctrl+D", "Da (inward stroke)"),
      entry("Ctrl+R", "Ra (outward stroke)"),

      new Separator(),
      heading("Ornaments — Simple"),
      entry("Ctrl+G", "Gamak (heavy oscillation)"),
      entry("Ctrl+A", "Andolan (gentle oscillation)"),
      entry("Ctrl+I", "Gitkari (hammer/pull trill)"),

      new Separator(),
      heading("Ornaments — One Note"),
      entry("Ctrl+K ♪", "Kan Swar (grace note)"),
      entry("Ctrl+H ♪", "Sparsh (light touch)"),
      entry("Ctrl+E ♪", "Ghaseet (heavy pull)"),

      new Separator(),
      heading("Ornaments — Two Notes"),
      entry("Ctrl+M ♪♪", "Meend ↑ (ascending glide)"),
      entry("Ctrl+Shift+M ♪♪", "Meend ↓ (descending glide)"),
      entry("Ctrl+J ♪♪", "Krintan (pull-off seq.)"),

      new Separator(),
      heading("Ornaments — Multi-Note"),
      entry("Ctrl+U ..↵", "Murki (ornamental turn)"),
      entry("Ctrl+Z ..↵", "Zamzama (rapid cluster)"),

      new Separator(),
      heading("Ornament Keys"),
      new Label("♪  = type one swar key"):
        style = "-fx-font-size: 10px; -fx-text-fill: #555; -fx-padding: 2 0 0 0;"
        wrapText = true
      ,
      new Label("♪♪ = type start, then end note"):
        style = "-fx-font-size: 10px; -fx-text-fill: #555; -fx-padding: 2 0 0 0;"
        wrapText = true
      ,
      new Label("..↵ = type notes, press Enter"):
        style = "-fx-font-size: 10px; -fx-text-fill: #555; -fx-padding: 2 0 0 0;"
        wrapText = true
      ,
      entry("Esc", "Cancel ornament mode"),

      new Separator(),
      heading("Tips"),
      new Label("Shift = komal/tivra variant"):
        style = "-fx-font-size: 10px; -fx-text-fill: #555; -fx-padding: 2 0 0 0;"
        wrapText = true
      ,
      new Label(". and ' affect only the next note, then reset to madhya"):
        style = "-fx-font-size: 10px; -fx-text-fill: #555; -fx-padding: 2 0 0 0;"
        wrapText = true
      ,
      new Label("Strokes & ornaments apply to the last entered note"):
        style = "-fx-font-size: 10px; -fx-text-fill: #555; -fx-padding: 2 0 0 0;"
        wrapText = true
    )
