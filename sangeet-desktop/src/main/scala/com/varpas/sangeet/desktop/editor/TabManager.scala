package com.varpas.sangeet.desktop.editor

import java.nio.file.{Files, Path}

import scala.collection.mutable.ListBuffer

import scalafx.scene.control.{Tab, TabPane}

import com.varpas.sangeet.core.config.{AppConfig, OpenTab}
import com.varpas.sangeet.core.editor.CompositionEditor
import com.varpas.sangeet.core.format.SwarFormat
import com.varpas.sangeet.core.model.{CompositionType, Laya}
import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.taal.Taals

class TabManager(statusBar: StatusBar):
  val tabPane: TabPane = new TabPane

  private val editorTabs               = ListBuffer.empty[EditorTab]
  private var previousTab: Option[Tab] = None

  tabPane.selectionModel.value.selectedItemProperty.addListener { (_, oldTab, _) =>
    if oldTab != null then
      editorTabs.find(_.tab.delegate eq oldTab).foreach { et =>
        et.autoSave()
      }
  }

  def openFile(path: Path): Unit =
    editorTabs.find(_.filePath.contains(path)) match
      case Some(existing) =>
        tabPane.selectionModel.value.select(existing.tab)
      case None =>
        SwarFormat.readFile(path) match
          case Right(comp) =>
            val et = createTab()
            et.editorPane.setReadOnly(false)
            et.editorPane.setComposition(comp)
            et.editorPane.setFilePath(path)
            et.filePath = Some(path)
            tabPane.selectionModel.value.select(et.tab)
            AppLogger.info(s"Tab opened: $path")
            statusBar.log(s"Opened: ${path.getFileName}")
          case Left(err) =>
            AppLogger.info(s"Failed to open file: $path -- ${err.getMessage}")
            statusBar.log(s"Error opening file: ${err.getMessage}")

  def newTab(): EditorTab =
    createTab()

  def closeTab(et: EditorTab): Unit =
    et.autoSave()
    editorTabs -= et
    tabPane.tabs.remove(et.tab)
    if editorTabs.isEmpty then createTab()

  def activeTab: Option[EditorTab] =
    val sel = tabPane.selectionModel.value.getSelectedItem
    if sel == null then None
    else editorTabs.find(_.tab.delegate eq sel)

  def switchTo(path: Path): Unit =
    editorTabs.find(_.filePath.contains(path)).foreach { et =>
      tabPane.selectionModel.value.select(et.tab)
    }

  def autoSaveActive(): Unit =
    activeTab.foreach(_.autoSave())

  def restoreTabs(config: AppConfig): Unit =
    config.openTabs.foreach { ot =>
      val path = Path.of(ot.filePath)
      if Files.exists(path) then openFile(path)
    }
    config.activeTabPath.foreach { ap =>
      switchTo(Path.of(ap))
    }

  def getOpenTabs: List[OpenTab] =
    editorTabs.toList.flatMap { et =>
      et.filePath.map { fp =>
        val sectionIdx = et.editorPane.getEditor.map(_.currentSectionIndex).getOrElse(0)
        OpenTab(fp.toString, sectionIdx)
      }
    }

  def activeTabPath: Option[String] =
    activeTab.flatMap(_.filePath).map(_.toString)

  def selectNextTab(): Unit =
    val sel = tabPane.selectionModel.value
    if sel.getSelectedIndex < tabPane.tabs.size - 1 then sel.selectNext()
    else sel.selectFirst()

  def selectPreviousTab(): Unit =
    val sel = tabPane.selectionModel.value
    if sel.getSelectedIndex > 0 then sel.selectPrevious()
    else sel.selectLast()

  def allTabs: List[EditorTab] = editorTabs.toList

  def activeTabIndex: Int =
    val sel = tabPane.selectionModel.value.getSelectedItem
    if sel == null then -1
    else editorTabs.indexWhere(_.tab.delegate eq sel)

  def selectTabByIndex(index: Int): Unit =
    if index >= 0 && index < editorTabs.size then tabPane.selectionModel.value.select(editorTabs(index).tab)

  def withActiveEditor(f: EditorPane => Unit): Unit =
    activeTab.foreach(t => f(t.editorPane))

  def withActiveEditorResult[A](f: EditorPane => A): Option[A] =
    activeTab.map(t => f(t.editorPane))

  private def createTab(): EditorTab =
    val ep = new EditorPane(statusBar)
    val defaultEditor = CompositionEditor.create(
      title = "Untitled",
      compositionType = CompositionType.Gat,
      taal = Taals.teentaal,
      raag = Raags.yaman,
      laya = Some(Laya.Madhya),
      showStrokeLine = true
    )
    ep.setEditor(defaultEditor)
    ep.setReadOnly(false)
    val tab = new Tab:
      text = "Untitled"
      content = ep
      closable = true
    val et = new EditorTab(ep, tab)
    editorTabs += et
    tabPane.tabs.add(tab)
    tab.onClosed = _ =>
      et.autoSave()
      editorTabs -= et
      if editorTabs.isEmpty then createTab()
    et
