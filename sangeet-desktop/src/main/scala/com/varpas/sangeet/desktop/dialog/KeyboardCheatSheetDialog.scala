package com.varpas.sangeet.desktop.dialog

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label, ScrollPane, Separator}
import scalafx.scene.layout.{ColumnConstraints, GridPane, HBox, Priority, VBox}
import scalafx.stage.{Modality, Stage, StageStyle}

import com.varpas.sangeet.core.strings.UiStrings
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
        UiStrings.dialogKeyboardCheatSheetSectionFileDesktop,
        Seq(
          Row(s("N"), UiStrings.dialogKeyboardCheatSheetActionNewComposition),
          Row(s("O"), UiStrings.dialogKeyboardCheatSheetActionOpenFile),
          Row(s("O", withShift = true), UiStrings.dialogKeyboardCheatSheetActionOpenFolder),
          Row(s("S"), UiStrings.dialogKeyboardCheatSheetActionSave),
          Row(s("S", withShift = true), UiStrings.dialogKeyboardCheatSheetActionSaveAs),
          Row(s("E"), UiStrings.dialogKeyboardCheatSheetActionExportHtml),
          Row(s("W"), UiStrings.dialogKeyboardCheatSheetActionCloseTab)
        )
      ),
      Group(
        UiStrings.dialogKeyboardCheatSheetSectionEditDesktop,
        Seq(
          Row(s("Z"), UiStrings.dialogKeyboardCheatSheetActionUndo),
          Row(s("Z", withShift = true), UiStrings.dialogKeyboardCheatSheetActionRedo),
          Row(s("X"), UiStrings.dialogKeyboardCheatSheetActionCut),
          Row(s("C"), UiStrings.dialogKeyboardCheatSheetActionCopy),
          Row(s("V"), UiStrings.dialogKeyboardCheatSheetActionPaste),
          Row(s(","), UiStrings.dialogKeyboardCheatSheetActionCompositionProperties)
        )
      ),
      Group(
        UiStrings.dialogKeyboardCheatSheetSectionTabsDesktop,
        Seq(
          Row(s("Tab"), UiStrings.dialogKeyboardCheatSheetActionNextTab),
          Row(s("Tab", withShift = true), UiStrings.dialogKeyboardCheatSheetActionPreviousTab)
        )
      ),
      Group(
        UiStrings.dialogKeyboardCheatSheetSectionSectionsDesktop,
        Seq(
          Row(s("A", withShift = true), UiStrings.dialogKeyboardCheatSheetActionAddSection),
          Row("F2", UiStrings.dialogKeyboardCheatSheetActionRenameSection),
          Row(s("Backspace", withShift = true), UiStrings.dialogKeyboardCheatSheetActionRemoveSection)
        )
      ),
      Group(
        UiStrings.dialogKeyboardCheatSheetSectionViewDesktop,
        Seq(
          Row(s("B"), UiStrings.dialogKeyboardCheatSheetActionToggleFileBrowser),
          Row(s("T", withShift = true), UiStrings.dialogKeyboardCheatSheetActionToggleTheme),
          Row(s("L", withShift = true), UiStrings.dialogKeyboardCheatSheetActionCycleScript)
        )
      ),
      Group(
        UiStrings.dialogKeyboardCheatSheetSectionHelpDesktop,
        Seq(
          Row("F1", UiStrings.dialogKeyboardCheatSheetActionOpenUserGuide),
          Row("?", UiStrings.dialogKeyboardCheatSheetActionShowCheatSheet),
          Row(s("B", withShift = true), UiStrings.dialogKeyboardCheatSheetActionReportBug)
        )
      )
    )

  def show(owner: javafx.stage.Stage): Unit =
    val titleLabel = new Label(UiStrings.dialogKeyboardCheatSheetTitle):
      style = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #8B1A1A;"

    val subtitle = new Label(UiStrings.dialogKeyboardCheatSheetSubtitleDesktop):
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

    val closeBtn = new Button(UiStrings.dialogKeyboardCheatSheetButtonClose):
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
      title = UiStrings.dialogKeyboardCheatSheetTitle
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
