package com.varpas.sangeet.desktop.editor

import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Path}

import scalafx.scene.control.Tab

class EditorTab(
    val editorPane: EditorPane,
    val tab: Tab,
    private var _filePath: Option[Path] = None
):
  private var lastKnownModifiedTime: Option[FileTime] = _filePath.flatMap(readMtime)

  /** Dirty flag — true when the in-memory composition has edits not yet written to disk. Drives the trailing `*` in the
    * tab title and the "close-with-unsaved-changes" prompt. Set by `EditorPane.pushEditorState` (any edit), cleared by
    * `autoSave()` after a successful write.
    */
  private var _isDirty: Boolean = false

  /** Override for the display title — used when the auto-rename flow (TabManager.handleDuplicateOpen) gave this tab a
    * unique name like `"abc (2)"`. When set, takes precedence over the filename derived from `_filePath`.
    */
  private[editor] var displayTitleOverride: Option[String] = None

  def filePath: Option[Path] = _filePath

  def filePath_=(path: Option[Path]): Unit =
    _filePath = path
    lastKnownModifiedTime = path.flatMap(readMtime)
    updateTabTitle()

  /** Filename including the `.swar` extension — used for status messages and friendly display in alerts. */
  def title: String = _filePath.map(_.getFileName.toString).getOrElse("Untitled")

  /** Title as shown in the tab bar — filename WITHOUT `.swar`, the override (if set), or "Untitled". Used for duplicate
    * detection so the comparison key matches the rendered tab text.
    */
  def displayTitle: String =
    displayTitleOverride.getOrElse {
      _filePath
        .map(p => stripSwarExt(p.getFileName.toString))
        .getOrElse("Untitled")
    }

  private def stripSwarExt(name: String): String =
    if name.endsWith(".swar") then name.dropRight(5) else name

  def isDirty: Boolean = _isDirty

  private[editor] def markDirty(): Unit =
    if !_isDirty then
      _isDirty = true
      updateTabTitle()

  private[editor] def markClean(): Unit =
    if _isDirty then
      _isDirty = false
      updateTabTitle()

  def autoSave(): Unit =
    _filePath.foreach { path =>
      editorPane.setFilePathAndSave(path)
      lastKnownModifiedTime = readMtime(path)
      markClean()
    }

  def isUntitled: Boolean = _filePath.isEmpty

  def wasModifiedExternally: Boolean =
    _filePath.exists { path =>
      if !Files.exists(path) then true
      else
        val currentMtime = readMtime(path)
        lastKnownModifiedTime match
          case None       => false
          case Some(last) => currentMtime.exists(_ != last)
    }

  def wasDeletedExternally: Boolean =
    _filePath.exists(p => !Files.exists(p))

  def refreshMtime(): Unit =
    lastKnownModifiedTime = _filePath.flatMap(readMtime)

  private def readMtime(path: Path): Option[FileTime] =
    try Some(Files.getLastModifiedTime(path))
    catch case _: Exception => None

  private[editor] def updateTabTitle(): Unit =
    val base = displayTitle
    tab.text = if _isDirty then s"$base *" else base
