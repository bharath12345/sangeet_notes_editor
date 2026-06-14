package com.varpas.sangeet.desktop.dialog

import scalafx.scene.control.{Alert, ButtonType}

import com.varpas.sangeet.core.strings.UiStrings
import com.varpas.sangeet.desktop.editor.TabNameResolver.DuplicateResolution

/** Modal that asks the user how to handle a tab-title collision when opening or creating a tab. Three choices:
  *   - Switch to the already-open tab
  *   - Open a copy under an auto-renamed title (e.g. "abc (2)")
  *   - Cancel
  */
object DuplicateTabDialog:

  def show(
      existingTitle: String,
      proposedNewTitle: String,
      owner: javafx.stage.Window = null
  ): DuplicateResolution =
    val switchBtn   = new ButtonType(UiStrings.dialogDuplicateTabButtonSwitch)
    val renameBtn   = new ButtonType(UiStrings.dialogDuplicateTabButtonRename(proposedNewTitle))
    val cancelBtn   = ButtonType.Cancel
    val dialogOwner = owner

    val alert = new Alert(Alert.AlertType.Confirmation):
      if dialogOwner != null then initOwner(dialogOwner)
      title = UiStrings.dialogDuplicateTabTitle
      headerText = UiStrings.dialogDuplicateTabHeader(existingTitle)
      contentText = UiStrings.dialogDuplicateTabBody
      buttonTypes = Seq(switchBtn, renameBtn, cancelBtn)

    alert.showAndWait() match
      case Some(b) if b == switchBtn => DuplicateResolution.Switch
      case Some(b) if b == renameBtn => DuplicateResolution.Rename(proposedNewTitle)
      case _                         => DuplicateResolution.Cancel
