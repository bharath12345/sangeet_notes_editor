package com.varpas.sangeet.desktop.dialog

import scalafx.Includes.jfxButtonData2sfx
import scalafx.scene.control.{Alert, ButtonType}

import com.varpas.sangeet.core.strings.UiStrings

/** 3-button modal asking the user how to close a tab with unsaved changes. Maps the user's choice into a sealed outcome
  * enum so callers (TabManager, MainApp.onCloseRequest) can sequence the right next action.
  */
object UnsavedChangesDialog:

  /** What the user picked. `SaveAs` only appears when the tab has never been saved to disk (no filePath yet) — the
    * caller is expected to surface a Save-As file picker afterwards. The plain `Save` arm uses the previously-picked
    * file path silently.
    */
  enum Outcome:
    case Save
    case SaveAs
    case Discard
    case Cancel

  def show(tabTitle: String, hasFilePath: Boolean, owner: javafx.stage.Window = null): Outcome =
    val saveButtonLabel =
      if hasFilePath then UiStrings.dialogUnsavedChangesButtonSave
      else UiStrings.dialogUnsavedChangesButtonSaveAs
    val saveBtn    = new ButtonType(saveButtonLabel)
    val discardBtn = new ButtonType(UiStrings.dialogUnsavedChangesButtonDiscard)
    val cancelBtn = new ButtonType(
      UiStrings.dialogUnsavedChangesButtonCancel,
      javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE
    )
    val dialogOwner = owner

    val alert = new Alert(Alert.AlertType.Confirmation):
      if dialogOwner != null then initOwner(dialogOwner)
      title = UiStrings.dialogUnsavedChangesTitle
      headerText = UiStrings.dialogUnsavedChangesHeader(tabTitle)
      contentText = UiStrings.dialogUnsavedChangesBody
      buttonTypes = Seq(saveBtn, discardBtn, cancelBtn)

    alert.showAndWait() match
      case Some(b) if b == saveBtn    => if hasFilePath then Outcome.Save else Outcome.SaveAs
      case Some(b) if b == discardBtn => Outcome.Discard
      case _                          => Outcome.Cancel
