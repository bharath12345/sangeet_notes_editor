package com.varpas.sangeet.desktop.dialog

import scalafx.Includes._
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Button
import scalafx.scene.layout.{HBox, Priority, VBox}
import scalafx.scene.{Node, Parent, Scene}
import scalafx.stage.{Modality, Stage, StageStyle}

/** Shared chrome for custom-Stage modal dialogs.
  *
  * Five of the desktop dialogs (About, BugReport, CrashRecovery, KeyboardCheatSheet, Support) used to hand-roll the
  * same Stage/Scene/VBox plumbing — same beige background, same `Modality.WindowModal`, same `StageStyle.Utility`, same
  * right-aligned button row. `ModalFrame` centralizes that wiring so each dialog only writes its content.
  *
  * The other five dialogs use different primitives and are not covered here:
  *   - `CompositionPropertiesDialog`, `NewCompositionDialog` use `javafx.scene.control.Dialog[T]` which brings its own
  *     chrome.
  *   - `UnsavedChangesDialog`, `DuplicateTabDialog` use `scalafx.scene.control.Alert`.
  *   - `CommandPaletteDialog` is a custom Stage too but has a different shape (no button row, transparent fill,
  *     search-field focused).
  *
  * If a sixth dialog wants the same look, it should `ModalFrame.build(...).showAndWait()`.
  */
object ModalFrame:

  /** Common parchment background used across all custom-Stage dialogs. Lives here so per-dialog files don't drift
    * apart.
    */
  val BackgroundStyle: String = "-fx-background-color: #FDF6EC;"

  /** Build a modal Stage with the standard chrome.
    *
    * @param title
    *   Window title shown in the OS title bar.
    * @param content
    *   Nodes laid out top-to-bottom in the root VBox. Buttons should be passed via `buttons` so the right-aligned row
    *   is applied consistently.
    * @param buttons
    *   Optional button row appended at the bottom. Pass `Nil` if your dialog has no buttons (e.g. one that closes
    *   itself programmatically).
    * @param width
    *   Window width in pixels. Each dialog historically picked its own; pass yours to preserve layout.
    * @param spacing
    *   Vertical spacing between content nodes inside the root VBox.
    * @param padding
    *   Root VBox padding. Defaults to 20px on every side.
    * @param owner
    *   The parent stage (for window-modal positioning + dimming). Null is permitted for dialogs shown before the main
    *   stage exists (e.g. crash recovery at startup).
    * @param modality
    *   Defaults to `WindowModal` (blocks input to the owner while open). Use `Modality.None` if there is no owner.
    * @param stageStyle
    *   Defaults to `Utility` (matches the existing dialogs).
    */
  def build(
      title: String,
      content: Seq[Node],
      buttons: Seq[Button],
      width: Double,
      spacing: Double = 6,
      padding: Insets = Insets(20),
      buttonRowTopPadding: Double = 8,
      owner: javafx.stage.Window = null,
      modality: Modality = Modality.WindowModal,
      stageStyle: StageStyle = StageStyle.Utility
  ): Stage =
    val rootChildren: Seq[Node] =
      if buttons.isEmpty then content
      else content :+ buttonRow(buttons, buttonRowTopPadding)

    val capturedSpacing = spacing
    val capturedPadding = padding
    val rootPane = new VBox:
      this.spacing = capturedSpacing
      this.padding = capturedPadding
      style = BackgroundStyle
      children = rootChildren

    buildWithRoot(
      title = title,
      root = rootPane,
      width = width,
      owner = owner,
      modality = modality,
      stageStyle = stageStyle
    )

  /** Build a modal Stage with a fully-custom root pane.
    *
    * Use this when the standard `content + buttonRow` layout does not fit — for example `KeyboardCheatSheetDialog`
    * needs a scrolling content region above a fixed button bar.
    */
  def buildWithRoot(
      title: String,
      root: Parent,
      width: Double,
      height: Option[Double] = None,
      sceneFill: Option[scalafx.scene.paint.Paint] = None,
      owner: javafx.stage.Window = null,
      modality: Modality = Modality.WindowModal,
      stageStyle: StageStyle = StageStyle.Utility
  ): Stage =
    val capturedTitle    = title
    val capturedWidth    = width
    val capturedHeight   = height
    val capturedFill     = sceneFill
    val capturedModality = modality
    val capturedRoot     = root
    val stage = new Stage:
      initStyle(stageStyle)
      initModality(capturedModality)
      this.title = capturedTitle
      this.width = capturedWidth
      capturedHeight.foreach(h => this.height = h)
      scene = new Scene:
        capturedFill.foreach(f => fill = f)
        this.root = capturedRoot
    if owner != null then stage.initOwner(owner)
    stage

  /** Build a right-aligned button row matching the standard chrome (Pos.CenterRight, 8px gap, 8/12px top padding). */
  def buttonRow(buttons: Seq[Button], topPadding: Double = 8): HBox =
    val capturedPadding = Insets(topPadding, 0, 0, 0)
    val row = new HBox:
      alignment = Pos.CenterRight
      spacing = 8
      this.padding = capturedPadding
      children = buttons.map(b => b: Node)
    HBox.setHgrow(row, Priority.Always)
    row
