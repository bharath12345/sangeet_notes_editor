package com.varpas.sangeet.desktop.editor

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import scala.collection.mutable.ListBuffer

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Alert, ButtonType, Label, Tab, TabPane}
import scalafx.scene.layout.{StackPane, VBox}
import scalafx.scene.web.WebView

import com.varpas.sangeet.core.config.{AppConfig, OpenTab}
import com.varpas.sangeet.core.editor.CompositionEditor
import com.varpas.sangeet.core.format.SwarFormat
import com.varpas.sangeet.core.model.{CompositionType, Laya}
import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.strings.UiStrings
import com.varpas.sangeet.core.taal.Taals
import com.varpas.sangeet.desktop.diagnostics.{DesktopEvent, NoopPostHogClient, PostHogClient}
import com.varpas.sangeet.desktop.dialog.{DuplicateTabDialog, UnsavedChangesDialog}
import com.varpas.sangeet.desktop.editor.TabNameResolver.DuplicateResolution

class TabManager(statusBar: StatusBar, analytics: PostHogClient = NoopPostHogClient):
  val tabPane: TabPane = new TabPane

  private val emptyPlaceholder = new VBox:
    alignment = Pos.Center
    spacing = 8
    padding = Insets(40)
    children = Seq(
      new Label("No compositions open"):
        style = "-fx-font-size: 18px; -fx-text-fill: #999;"
      ,
      new Label("Click New or Open File to begin"):
        style = "-fx-font-size: 13px; -fx-text-fill: #bbb;"
    )

  val editorArea: StackPane = new StackPane:
    children = Seq(tabPane, emptyPlaceholder)

  private val editorTabs               = ListBuffer.empty[EditorTab]
  private var previousTab: Option[Tab] = None

  tabPane.selectionModel.value.selectedItemProperty.addListener { (_, oldTab, newTab) =>
    if oldTab != null then
      editorTabs.find(_.tab.delegate eq oldTab).foreach { et =>
        et.autoSave()
      }
    if newTab != null then
      editorTabs.find(_.tab.delegate eq newTab).foreach { et =>
        checkExternalChanges(et)
      }
  }

  private def showEmptyState(): Unit =
    tabPane.visible = false
    tabPane.managed = false
    emptyPlaceholder.visible = true
    emptyPlaceholder.managed = true

  private def hideEmptyState(): Unit =
    emptyPlaceholder.visible = false
    emptyPlaceholder.managed = false
    tabPane.visible = true
    tabPane.managed = true

  def openFile(path: Path): Unit = openFile(path, "file-browser")

  /** The display title used in the tab bar — filename without the `.swar` extension. Mirrors what's rendered by the tab
    * itself so the duplicate-tab check compares apples to apples.
    */
  private def displayTitleFor(path: Path): String =
    val name = path.getFileName.toString
    if name.endsWith(".swar") then name.dropRight(5) else name

  /** Find an open tab with the given display title (filename-without-extension for files, the title field for untitled
    * compositions).
    */
  def findTabByTitle(title: String): Option[EditorTab] =
    editorTabs.find(_.displayTitle == title)

  /** Internal overload — the `source` is just a label on the CompositionOpened analytics event so we can tell apart
    * file-browser double-clicks from auto-restored tabs at startup. Behavior is otherwise identical.
    *
    * Duplicate-title handling: if a tab is already open with the SAME PATH, switch to it silently. If a different file
    * happens to share the same filename (cross-folder collision), prompt the user via [[DuplicateTabDialog]] — [Switch
    * to it] / [Open with new name "abc (2)"] / [Cancel].
    */
  def openFile(path: Path, source: String): Unit =
    hideEmptyState()
    editorTabs.find(_.filePath.contains(path)) match
      case Some(existing) =>
        tabPane.selectionModel.value.select(existing.tab)
      case None =>
        val proposedTitle = displayTitleFor(path)
        findTabByTitle(proposedTitle) match
          case Some(existing) =>
            handleDuplicateOpen(path, proposedTitle, existing, source)
          case None =>
            doOpenFile(path, source, overrideTitle = None)

  private def handleDuplicateOpen(
      path: Path,
      conflictingTitle: String,
      existing: EditorTab,
      source: String
  ): Unit =
    val owner    = if tabPane.scene.value != null then tabPane.scene.value.getWindow else null
    val newTitle = TabNameResolver.nextAvailableTitle(conflictingTitle, openTabTitles)
    val choice   = DuplicateTabDialog.show(conflictingTitle, newTitle, owner)
    choice match
      case DuplicateResolution.Switch =>
        tabPane.selectionModel.value.select(existing.tab)
      case DuplicateResolution.Rename(renamed) =>
        doOpenFile(path, source, overrideTitle = Some(renamed))
      case DuplicateResolution.Cancel =>
        ()

  private def openTabTitles: Seq[String] = editorTabs.map(_.displayTitle).toList

  private def doOpenFile(path: Path, source: String, overrideTitle: Option[String]): Unit =
    SwarFormat.readFile(path) match
      case Right(comp) =>
        val et = createTab()
        et.editorPane.setReadOnly(false)
        et.editorPane.setComposition(comp)
        et.editorPane.setFilePath(path)
        et.filePath = Some(path)
        overrideTitle.foreach { t =>
          et.displayTitleOverride = Some(t)
          et.updateTabTitle()
        }
        tabPane.selectionModel.value.select(et.tab)
        AppLogger.info(s"Tab opened: $path")
        statusBar.log(UiStrings.statusOpenedDesktop.replace("{filename}", path.getFileName.toString))
        analytics.capture(DesktopEvent.CompositionOpened(comp.metadata.taal.name, source))
      case Left(err) =>
        AppLogger.info(s"Failed to open file: $path -- ${err.getMessage}")
        statusBar.log(UiStrings.statusErrorOpeningFile.replace("{message}", err.getMessage))

  def openHtml(path: Path): Unit =
    hideEmptyState()
    editorTabs.find(_.filePath.contains(path)) match
      case Some(existing) =>
        tabPane.selectionModel.value.select(existing.tab)
      case None =>
        try
          val html    = Files.readString(path, StandardCharsets.UTF_8)
          val webView = new WebView()
          webView.engine.loadContent(html)
          val tab = new Tab:
            text = s"${path.getFileName} (preview)"
            content = webView
            closable = true
          val ep = new EditorPane(statusBar)
          ep.setReadOnly(true)
          val et = new EditorTab(ep, tab, Some(path))
          editorTabs += et
          tabPane.tabs.add(tab)
          tab.onClosed = _ =>
            editorTabs -= et
            if editorTabs.isEmpty then showEmptyState()
          tabPane.selectionModel.value.select(tab)
          AppLogger.info(s"HTML preview tab opened: $path")
          statusBar.log(UiStrings.statusPreview.replace("{filename}", path.getFileName.toString))
        catch
          case ex: Exception =>
            AppLogger.info(s"Failed to open HTML: $path -- ${ex.getMessage}")
            statusBar.log(UiStrings.statusErrorOpeningHtml.replace("{message}", ex.getMessage))

  def newTab(): EditorTab =
    analytics.capture(DesktopEvent.TabOpened)
    createTab()

  /** Outcome of `addTabUnique` — for callers that need to know whether the new tab was actually opened, the user picked
    * an existing one, or the action was cancelled.
    */
  enum AddTabOutcome:
    case Opened(tab: EditorTab)
    case Switched(tab: EditorTab)
    case Cancelled

  /** Create a new composition tab tied to the given file path. If a tab with the same path is already open, switch to
    * it. If a different file with the same display title is open, prompt the user — switch / open with new name /
    * cancel. Returns the outcome so the caller can decide whether to log success / capture analytics / etc.
    *
    * The `provision` callback receives the freshly-created tab and is expected to populate it (setEditor,
    * setFilePathAndSave, changeScript, etc.).
    */
  def addTabUnique(path: Path, proposedTitle: String)(provision: EditorTab => Unit): AddTabOutcome =
    hideEmptyState()
    editorTabs.find(_.filePath.contains(path)) match
      case Some(existing) =>
        tabPane.selectionModel.value.select(existing.tab)
        AddTabOutcome.Switched(existing)
      case None =>
        findTabByTitle(proposedTitle) match
          case Some(existing) =>
            val owner    = if tabPane.scene.value != null then tabPane.scene.value.getWindow else null
            val newTitle = TabNameResolver.nextAvailableTitle(proposedTitle, openTabTitles)
            DuplicateTabDialog.show(proposedTitle, newTitle, owner) match
              case DuplicateResolution.Switch =>
                tabPane.selectionModel.value.select(existing.tab)
                AddTabOutcome.Switched(existing)
              case DuplicateResolution.Rename(renamed) =>
                val et = createTab()
                provision(et)
                et.displayTitleOverride = Some(renamed)
                et.updateTabTitle()
                AddTabOutcome.Opened(et)
              case DuplicateResolution.Cancel =>
                AddTabOutcome.Cancelled
          case None =>
            val et = createTab()
            provision(et)
            AddTabOutcome.Opened(et)

  /** Callback set by MainApp/ToolbarBuilder that runs the Save-As FileChooser flow for the given tab and returns `true`
    * if the user picked a destination (so we can proceed with closing the tab). Returns `false` if the user cancelled
    * the file picker — in which case the tab close should be aborted too.
    */
  private var saveAsHandler: EditorTab => Boolean     = _ => false
  def setSaveAsHandler(f: EditorTab => Boolean): Unit = saveAsHandler = f

  def closeTab(et: EditorTab): Unit =
    if et.isDirty then
      val owner = if tabPane.scene.value != null then tabPane.scene.value.getWindow else null
      UnsavedChangesDialog.show(et.displayTitle, et.filePath.isDefined, owner) match
        case UnsavedChangesDialog.Outcome.Cancel  => return
        case UnsavedChangesDialog.Outcome.Discard => () // proceed to remove without saving
        case UnsavedChangesDialog.Outcome.Save    => et.autoSave()
        case UnsavedChangesDialog.Outcome.SaveAs =>
          if !saveAsHandler(et) then return
    else if et.isUntitled then
      if !promptSaveUntitled(et) then return
    else et.autoSave()
    editorTabs -= et
    tabPane.tabs.remove(et.tab)
    if editorTabs.isEmpty then showEmptyState()

  /** Close a tab without the discard-confirmation prompt. Used by callers that already know the tab's content is
    * disposable (e.g. the read-only Yaman sample). Does NOT autosave — callers must ensure it's safe to drop without
    * persistence.
    */
  def removeUntitledTabSilently(et: EditorTab): Unit =
    editorTabs -= et
    tabPane.tabs.remove(et.tab)
    if editorTabs.isEmpty then showEmptyState()

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
      if Files.exists(path) then openFile(path, "restored")
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

  def checkExternalChanges(et: EditorTab): Unit =
    if et.wasDeletedExternally then
      statusBar.log(UiStrings.statusFileWasDeleted.replace("{title}", et.title))
      et.tab.text = s"${et.title} (deleted)"
    else if et.wasModifiedExternally then
      et.filePath.foreach { path =>
        val alert = new Alert(Alert.AlertType.Confirmation):
          initOwner(tabPane.scene.value.getWindow)
          title = "File Changed"
          headerText = s"${path.getFileName} was modified externally."
          contentText = "Reload the file?"
        alert.showAndWait() match
          case Some(result) if result == ButtonType.OK =>
            SwarFormat.readFile(path) match
              case Right(comp) =>
                et.editorPane.setComposition(comp)
                et.refreshMtime()
                statusBar.log(UiStrings.statusReloaded.replace("{filename}", path.getFileName.toString))
              case Left(err) =>
                statusBar.log(UiStrings.statusErrorReloading.replace("{message}", err.getMessage))
          case _ =>
            et.refreshMtime()
      }

  def promptSaveUntitled(et: EditorTab): Boolean =
    if !et.isUntitled then return true
    val hasContent = et.editorPane.getEditor.exists { ed =>
      ed.composition.sections.exists(_.events.nonEmpty)
    }
    if !hasContent then return true
    val alert = new Alert(Alert.AlertType.Confirmation):
      initOwner(tabPane.scene.value.getWindow)
      title = "Unsaved Composition"
      headerText = "This tab has unsaved content."
      contentText = "Discard it?"
      buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
    alert.showAndWait() match
      case Some(result) if result == ButtonType.OK => true
      case _                                       => false

  private def createTab(): EditorTab =
    hideEmptyState()
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
    // Hook the dirty/clean transitions: editor-level changes mark the tab dirty;
    // successful autosaves clear the flag. Done via callbacks so EditorPane stays
    // ignorant of EditorTab (and remains testable in isolation).
    ep.setOnEditCallback(() => et.markDirty())
    ep.setOnAutoSaveSuccessCallback(() => et.markClean())
    editorTabs += et
    tabPane.tabs.add(tab)
    tab.onClosed = _ =>
      et.autoSave()
      editorTabs -= et
      if editorTabs.isEmpty then showEmptyState()
    et
