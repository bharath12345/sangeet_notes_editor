package com.varpas.sangeet.desktop.editor

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, Label, ScrollPane, Separator}
import scalafx.scene.layout.{HBox, Priority, Region, VBox}

import com.varpas.sangeet.core.model.SwarScript
import com.varpas.sangeet.core.render.ScriptMap
import com.varpas.sangeet.core.strings.UiStrings

class KeyboardLegend extends ScrollPane:
  prefWidth = 400
  minWidth = 180
  fitToWidth = true
  hbarPolicy = ScrollPane.ScrollBarPolicy.Never

  private def heading(text: String) = new Label(text):
    style = "-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 6 0 2 0;"

  private def entry(key: String, desc: String) = new Label(s"$key  $desc"):
    style = "-fx-font-size: 13px; -fx-font-family: monospace; -fx-padding: 1 0 1 0;"
    wrapText = true

  private val legendBox = new VBox:
    spacing = 1
    padding = Insets(8, 10, 8, 10)
    style = "-fx-background-color: #f5f5f0; -fx-border-color: #ccc; -fx-border-width: 0 0 0 1;"

  private val headerSpacer = new Region()
  HBox.setHgrow(headerSpacer, Priority.Always)

  private val headerBox = new HBox:
    alignment = Pos.CenterLeft
    padding = Insets(2, 4, 2, 4)
    visible = false
    managed = false

  private val wrapper = new VBox:
    children = Seq(headerBox, legendBox)
    style = "-fx-background-color: #f5f5f0;"

  content = wrapper

  def setCollapseButton(btn: Button): Unit =
    headerBox.children = Seq(headerSpacer, btn)
    headerBox.visible = true
    headerBox.managed = true

  // Initialize with default
  updateScript(SwarScript.Devanagari)

  def updateScript(script: SwarScript): Unit =
    val entries = ScriptMap.legendEntries(script)
    val swarEntries = entries.map { (key, desc, variant) =>
      val label = if variant.nonEmpty then s"$desc $variant" else desc
      entry(key, label)
    }

    val scriptName = ScriptMap.displayName(script)
    legendBox.children = List(
      new Label(UiStrings.keyboardLegendTitleDesktop):
        style = "-fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 0 0 2 0;"
      ,
      new Label(UiStrings.keyboardLegendScriptLabel.replace("{scriptName}", scriptName)):
        style = "-fx-font-size: 12px; -fx-text-fill: #555; -fx-padding: 0 0 4 0;"
      ,
      new Separator()
    ) ++ (heading(UiStrings.keyboardLegendSectionSwarNotes) :: swarEntries) ++ List(
      new Separator(),
      heading(UiStrings.keyboardLegendSectionOctave),
      entry(".", UiStrings.keyboardLegendOctaveMandraDesktop),
      entry("'", UiStrings.keyboardLegendOctaveTaarDesktop),
      entry("`", UiStrings.keyboardLegendOctaveBackToMadhya),
      new Separator(),
      heading(UiStrings.keyboardLegendSectionSpecial),
      entry("1", UiStrings.keyboardLegendSpecialChikari),
      entry("Space", UiStrings.keyboardLegendSpecialRest),
      entry("-", UiStrings.keyboardLegendSpecialSustain),
      entry("Del", UiStrings.keyboardLegendSpecialDeleteLast),
      new Separator(),
      heading(UiStrings.keyboardLegendSectionNavigation),
      entry("\u2190 \u2192", UiStrings.keyboardLegendNavMoveCursor),
      entry("Tab", UiStrings.keyboardLegendNavTabDesktop),
      entry("Enter", UiStrings.keyboardLegendNavEnter),
      new Separator(),
      heading(UiStrings.keyboardLegendSectionUndoRedoDesktop),
      entry("Ctrl+Z", UiStrings.keyboardLegendUndo),
      entry("Ctrl+Shift+Z", UiStrings.keyboardLegendRedoDesktop),
      new Separator(),
      heading(UiStrings.keyboardLegendSectionSubdivisions),
      entry("Ctrl+2-8", UiStrings.keyboardLegendSubdivisionsSetPerBeat),
      entry("ss/rr/gg..", UiStrings.keyboardLegendSubdivisionsDoubleTap),
      new Separator(),
      heading(UiStrings.keyboardLegendSectionStrokesMizrab),
      entry("Ctrl+D", UiStrings.keyboardLegendStrokesDa),
      entry("Ctrl+R", UiStrings.keyboardLegendStrokesRa),
      new Separator(),
      heading(UiStrings.keyboardLegendSectionOrnamentsSimple),
      entry("Ctrl+G", UiStrings.keyboardLegendOrnamentsGamakDesktop),
      entry("Ctrl+A", UiStrings.keyboardLegendOrnamentsAndolanDesktop),
      entry("Ctrl+I", UiStrings.keyboardLegendOrnamentsGitkariDesktop),
      new Separator(),
      heading(UiStrings.keyboardLegendSectionOrnamentsOneNote),
      entry("Ctrl+K \u266a", UiStrings.keyboardLegendOrnamentsKanDesktop),
      entry("Ctrl+H \u266a", UiStrings.keyboardLegendOrnamentsSparshDesktop),
      entry("Ctrl+E \u266a", UiStrings.keyboardLegendOrnamentsGhaseetDesktop),
      new Separator(),
      heading(UiStrings.keyboardLegendSectionOrnamentsTwoNotes),
      entry("Ctrl+M \u266a\u266a", UiStrings.keyboardLegendOrnamentsMeendAscDesktop),
      entry("Ctrl+Shift+M \u266a\u266a", UiStrings.keyboardLegendOrnamentsMeendDescDesktop),
      entry("Ctrl+J \u266a\u266a", UiStrings.keyboardLegendOrnamentsKrintanDesktop),
      new Separator(),
      heading(UiStrings.keyboardLegendSectionOrnamentsMultiNote),
      entry("Ctrl+U ..\u21b5", UiStrings.keyboardLegendOrnamentsMurkiDesktop),
      entry("Ctrl+W ..\u21b5", UiStrings.keyboardLegendOrnamentsZamzamaDesktop),
      new Separator(),
      heading(UiStrings.keyboardLegendSectionOrnamentKeys),
      new Label(UiStrings.keyboardLegendOrnamentKeysOneNote):
        style = "-fx-font-size: 12px; -fx-text-fill: #555; -fx-padding: 2 0 0 0;"
        wrapText = true
      ,
      new Label(UiStrings.keyboardLegendOrnamentKeysTwoNotes):
        style = "-fx-font-size: 12px; -fx-text-fill: #555; -fx-padding: 2 0 0 0;"
        wrapText = true
      ,
      new Label(UiStrings.keyboardLegendOrnamentKeysMultiNote):
        style = "-fx-font-size: 12px; -fx-text-fill: #555; -fx-padding: 2 0 0 0;"
        wrapText = true
      ,
      entry("Esc", UiStrings.keyboardLegendOrnamentsCancel),
      new Separator(),
      heading(UiStrings.keyboardLegendSectionTips),
      new Label(UiStrings.keyboardLegendTipsShiftVariant):
        style = "-fx-font-size: 12px; -fx-text-fill: #555; -fx-padding: 2 0 0 0;"
        wrapText = true
      ,
      new Label(UiStrings.keyboardLegendTipsOctaveReset):
        style = "-fx-font-size: 12px; -fx-text-fill: #555; -fx-padding: 2 0 0 0;"
        wrapText = true
      ,
      new Label(UiStrings.keyboardLegendTipsApplyToLast):
        style = "-fx-font-size: 12px; -fx-text-fill: #555; -fx-padding: 2 0 0 0;"
        wrapText = true
    )
