package com.varpas.sangeet.desktop.dialog

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label, ScrollPane, Separator}
import scalafx.scene.layout.{ColumnConstraints, GridPane, HBox, Priority, VBox}
import scalafx.stage.{Modality, Stage, StageStyle}

import com.varpas.sangeet.desktop.ShortcutText

/** In-app keyboard cheat sheet. Opens on `?` or `F1`. Two-column grid of (shortcut, action) grouped by category. Full
  * reference lives in the bundled user guide; this is the quick reminder.
  */
object KeyboardCheatSheetDialog:

  private case class Row(keys: String, action: String)
  private case class Group(title: String, rows: Seq[Row])

  // Display order matters — top-of-page is most-needed.
  private def groups: Seq[Group] =
    import ShortcutText.{shortcut => s}
    Seq(
      Group(
        "File",
        Seq(
          Row(s("N"), "New composition"),
          Row(s("O"), "Open file"),
          Row(s("O", withShift = true), "Open folder"),
          Row(s("S"), "Save"),
          Row(s("S", withShift = true), "Save as"),
          Row(s("E"), "Export HTML"),
          Row(s("W"), "Close tab")
        )
      ),
      Group(
        "Edit",
        Seq(
          Row(s("Z"), "Undo"),
          Row(s("Z", withShift = true), "Redo"),
          Row(s("X"), "Cut"),
          Row(s("C"), "Copy"),
          Row(s("V"), "Paste"),
          Row(s(","), "Composition properties")
        )
      ),
      Group(
        "Tabs",
        Seq(
          Row(s("Tab"), "Next tab"),
          Row(s("Tab", withShift = true), "Previous tab")
        )
      ),
      Group(
        "Sections",
        Seq(
          Row(s("A", withShift = true), "Add section"),
          Row("F2", "Rename current section"),
          Row(s("Backspace", withShift = true), "Remove current section")
        )
      ),
      Group(
        "View",
        Seq(
          Row(s("B"), "Toggle file browser"),
          Row(s("T", withShift = true), "Toggle theme"),
          Row(s("L", withShift = true), "Cycle notation script")
        )
      ),
      Group(
        "Help",
        Seq(
          Row("F1", "Open user guide"),
          Row("?", "Show this cheat sheet"),
          Row(s("B", withShift = true), "Report a bug")
        )
      )
    )

  def show(owner: javafx.stage.Stage): Unit =
    val titleLabel = new Label("Keyboard Shortcuts"):
      style = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #8B1A1A;"

    val subtitle = new Label(
      "Full reference: Help → User Guide → Keyboard Reference"
    ):
      style = "-fx-font-size: 11px; -fx-text-fill: #6A5A4A; -fx-font-style: italic;"

    val contentPane = new VBox:
      spacing = 14
      padding = Insets(20)
      style = "-fx-background-color: #FDF6EC;"
      children = Seq[scalafx.scene.Node](titleLabel, subtitle, new Separator()) ++
        groups.flatMap(g => Seq[scalafx.scene.Node](buildGroup(g), new Separator())).dropRight(1)

    val scroll = new ScrollPane:
      content = contentPane
      fitToWidth = true
      style = "-fx-background-color: transparent; -fx-background: transparent;"
      prefViewportHeight = 500

    val closeBtn = new Button("Close"):
      style = "-fx-font-size: 12px;"
      defaultButton = true

    val buttonRow = new HBox:
      alignment = Pos.CenterRight
      padding = Insets(8, 16, 12, 16)
      style = "-fx-background-color: #FDF6EC;"
      children = Seq(closeBtn)

    val rootPane = new VBox:
      children = Seq(scroll, buttonRow)
      style = "-fx-background-color: #FDF6EC;"
    VBox.setVgrow(scroll, Priority.Always)

    val dialogStage = new Stage:
      initStyle(StageStyle.Utility)
      initModality(Modality.WindowModal)
      title = "Keyboard Shortcuts"
      width = 480
      scene = new Scene:
        root = rootPane
    dialogStage.initOwner(owner)
    closeBtn.onAction = _ => dialogStage.close()
    dialogStage.showAndWait()

  private def buildGroup(g: Group): VBox =
    val header = new Label(g.title):
      style = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #5A2828;"

    val grid = new GridPane:
      hgap = 16
      vgap = 4
      padding = Insets(4, 0, 0, 8)
    val keyCol = new ColumnConstraints:
      minWidth = 130
    val labelCol = new ColumnConstraints:
      hgrow = Priority.Always
    grid.columnConstraints.addAll(keyCol, labelCol)

    g.rows.zipWithIndex.foreach { (row, i) =>
      val keyLabel = new Label(row.keys):
        style = "-fx-font-family: 'SF Mono', Menlo, Consolas, monospace;" +
          " -fx-background-color: rgba(176, 122, 62, 0.12); -fx-padding: 1 6 1 6;" +
          " -fx-background-radius: 3; -fx-font-size: 11px; -fx-text-fill: #4A2F12;"
      val actionLabel = new Label(row.action):
        style = "-fx-font-size: 12px; -fx-text-fill: #2D2926;"
      grid.add(keyLabel, 0, i)
      grid.add(actionLabel, 1, i)
    }

    new VBox:
      spacing = 4
      children = Seq(header, grid)
