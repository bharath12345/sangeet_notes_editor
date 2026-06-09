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

  def filePath: Option[Path] = _filePath

  def filePath_=(path: Option[Path]): Unit =
    _filePath = path
    lastKnownModifiedTime = path.flatMap(readMtime)
    updateTabTitle()

  def title: String = _filePath.map(_.getFileName.toString).getOrElse("Untitled")

  def autoSave(): Unit =
    _filePath.foreach { path =>
      editorPane.setFilePathAndSave(path)
      lastKnownModifiedTime = readMtime(path)
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
    tab.text = title
