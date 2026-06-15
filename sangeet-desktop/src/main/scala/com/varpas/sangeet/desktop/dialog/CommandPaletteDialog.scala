package com.varpas.sangeet.desktop.dialog

import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Pos
import scalafx.scene.control._
import scalafx.scene.layout.{HBox, Priority, VBox}

import com.varpas.sangeet.core.strings.UiStrings
import com.varpas.sangeet.desktop.action.AppAction

/** Cmd+K / Ctrl+K command palette. Modal stage with a search TextField on top, a filtered ListView below. Substring
  * match on title or group. Arrow keys navigate, Enter fires the highlighted action, Esc closes.
  */
object CommandPaletteDialog:

  def show(owner: javafx.stage.Stage, actions: List[AppAction]): Unit =
    val queryProp    = StringProperty("")
    val initialItems = ObservableBuffer.from(actions)

    val searchField = new TextField:
      promptText = UiStrings.dialogCommandPaletteSearchPlaceholder
      style = "-fx-font-size: 14px; -fx-padding: 8 12 8 12; -fx-background-radius: 0;" +
        " -fx-border-color: transparent transparent #D4C8B8 transparent; -fx-border-width: 0 0 1 0;"
    queryProp <==> searchField.text

    val listView = new ListView[AppAction]:
      items = ObservableBuffer.from(actions)
      style = "-fx-font-size: 13px; -fx-background-color: transparent;"
      cellFactory = (lv: ListView[AppAction]) =>
        new ListCell[AppAction]:
          item.onChange { (_, _, newItem) =>
            if newItem == null then
              text = ""
              graphic = null
            else
              val titleLabel = new Label(newItem.title):
                style = "-fx-font-size: 13px; -fx-text-fill: #2D2926;"
                maxWidth = Double.MaxValue
              HBox.setHgrow(titleLabel, Priority.Always)
              val groupLabel = new Label(newItem.group):
                style = "-fx-font-size: 10px; -fx-text-fill: #8A7964; -fx-font-style: italic;"
              val shortcutLabel = new Label(newItem.shortcut.getOrElse("")):
                style = "-fx-font-family: 'SF Mono', Menlo, Consolas, monospace;" +
                  " -fx-font-size: 11px; -fx-text-fill: #6A5A4A;" +
                  " -fx-background-color: rgba(176,122,62,0.12); -fx-padding: 1 6 1 6;" +
                  " -fx-background-radius: 3;"
              val row = new HBox:
                spacing = 10
                alignment = Pos.CenterLeft
                children = Seq(titleLabel, groupLabel, shortcutLabel)
              graphic = row
          }

    if initialItems.nonEmpty then listView.selectionModel.value.select(0)

    // Re-filter as the user types. Keep selection on the first row so Enter always
    // fires *something* without an extra ↓ press.
    queryProp.onChange { (_, _, newVal) =>
      val filtered = AppAction.filter(actions, newVal)
      listView.items = ObservableBuffer.from(filtered)
      if filtered.nonEmpty then listView.selectionModel.value.select(0)
    }

    val container = new VBox:
      style = "-fx-background-color: #FDF6EC; -fx-background-radius: 6;" +
        " -fx-border-color: #B07A3E; -fx-border-radius: 6; -fx-border-width: 1;"
      children = Seq(searchField, listView)
    VBox.setVgrow(listView, Priority.Always)
    listView.prefHeight = 360
    listView.maxHeight = 360

    val dialogStage = ModalFrame.buildWithRoot(
      title = UiStrings.dialogCommandPaletteTitle,
      root = container,
      width = 540,
      height = Some(420),
      sceneFill = Some(scalafx.scene.paint.Color.Transparent),
      owner = owner
    )

    def runSelected(): Unit =
      val sel = listView.selectionModel.value.getSelectedItem
      if sel != null then
        dialogStage.close()
        sel.run()

    // The search field swallows arrow keys and Enter, so install an event filter
    // at the scene level instead. Esc closes; ↑/↓ moves selection; Enter fires.
    dialogStage.scene.value.addEventFilter(
      javafx.scene.input.KeyEvent.KEY_PRESSED,
      (ev: javafx.scene.input.KeyEvent) =>
        import javafx.scene.input.{KeyCode => JKeyCode}
        ev.getCode match
          case JKeyCode.ESCAPE => dialogStage.close(); ev.consume()
          case JKeyCode.ENTER  => runSelected(); ev.consume()
          case JKeyCode.DOWN =>
            val sm  = listView.selectionModel.value
            val idx = sm.getSelectedIndex
            if idx < listView.items.value.size - 1 then sm.select(idx + 1)
            ev.consume()
          case JKeyCode.UP =>
            val sm  = listView.selectionModel.value
            val idx = sm.getSelectedIndex
            if idx > 0 then sm.select(idx - 1)
            ev.consume()
          case _ => ()
    )

    // Click-to-execute as well as arrow-Enter.
    listView.onMouseClicked = ev => if ev.getClickCount == 1 then runSelected()

    // Autofocus the search field once the scene is shown.
    dialogStage.setOnShown(_ => searchField.requestFocus())
    dialogStage.showAndWait()
