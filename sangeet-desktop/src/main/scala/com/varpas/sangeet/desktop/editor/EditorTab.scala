package com.varpas.sangeet.desktop.editor

import java.nio.file.Path

import scalafx.scene.control.Tab

class EditorTab(
    val editorPane: EditorPane,
    val tab: Tab,
    private var _filePath: Option[Path] = None
):
  def filePath: Option[Path] = _filePath

  def filePath_=(path: Option[Path]): Unit =
    _filePath = path
    updateTabTitle()

  def title: String = _filePath.map(_.getFileName.toString).getOrElse("Untitled")

  def autoSave(): Unit =
    _filePath.foreach { path =>
      editorPane.setFilePathAndSave(path)
    }

  private[editor] def updateTabTitle(): Unit =
    tab.text = title
