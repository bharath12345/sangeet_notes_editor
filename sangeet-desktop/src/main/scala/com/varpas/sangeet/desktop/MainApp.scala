package com.varpas.sangeet.desktop

import java.nio.file.Path

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Orientation, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{SplitPane, _}
import scalafx.scene.layout.{BorderPane, HBox, Priority, Region, VBox}
import scalafx.scene.paint.Color
import scalafx.stage.{DirectoryChooser, FileChooser}

import com.varpas.sangeet.core.config.{AppConfig, ConfigStore}
import com.varpas.sangeet.core.editor.CompositionEditor
import com.varpas.sangeet.core.format.{HtmlExport, SwarFormat}
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.render.ScriptMap
import com.varpas.sangeet.core.taal.Taals
import com.varpas.sangeet.desktop.dialog.{CompositionPropertiesDialog, NewCompositionDialog}
import com.varpas.sangeet.desktop.editor.{
  AppLogger,
  DebugConsole,
  FileBrowserPanel,
  KeyboardLegend,
  SampleComposition,
  StatusBar,
  TabManager
}

object MainApp extends JFXApp3:

  // Single instance lock -- bind a local port; second instance fails to bind and exits
  private val lockSocket: java.net.ServerSocket =
    try
      val s = new java.net.ServerSocket(47633, 1, java.net.InetAddress.getLoopbackAddress)
      s
    catch
      case _: java.net.BindException =>
        System.err.println("Another instance of Sangeet Notes Editor is already running.")
        System.exit(1)
        null // unreachable

  // Set macOS dock name before JavaFX toolkit initializes
  if System.getProperty("os.name", "").toLowerCase.contains("mac") then
    System.setProperty("apple.awt.application.name", "Sangeet Notes Editor")
    try
      val taskbar  = java.awt.Taskbar.getTaskbar
      val iconFile = java.io.File("packaging/icons/sangeet-icon-256.png")
      if iconFile.exists then taskbar.setIconImage(javax.imageio.ImageIO.read(iconFile))
    catch case _: Exception => ()

  private def btnStyle = "-fx-font-size: 11px;"
  private def iconLabel(symbol: String) = new scalafx.scene.control.Label(symbol):
    style = "-fx-font-size: 14px;"

  override def start(): Unit =
    val logPath = AppLogger.initialize()
    System.err.println(s"Log file: $logPath")

    val statusBar        = new StatusBar()
    val tabManager       = new TabManager(statusBar)
    val keyboardLegend   = new KeyboardLegend()
    val fileBrowserPanel = new FileBrowserPanel(tabManager, statusBar)

    def withActiveEditor(f: com.varpas.sangeet.desktop.editor.EditorPane => Unit): Unit =
      tabManager.withActiveEditor(f)

    def focusActiveEditor(): Unit =
      tabManager.activeTab.foreach(_.editorPane.requestFocus())

    val initialTab   = tabManager.newTab()
    val debugConsole = new DebugConsole(tabManager, statusBar)
    debugConsole.start()

    // ── Toolbar ─────────────────────────────────────────────────────────

    val newBtn = new Button("New"):
      style = btnStyle
      graphic = iconLabel("➕")
      tooltip = new Tooltip("Create a new composition")
      onAction = _ =>
        NewCompositionDialog.show(stage).foreach { result =>
          val taal = Taals.byName(result.taalName).getOrElse(Taals.teentaal)
          val editor = CompositionEditor.create(
            title = result.title,
            compositionType = result.compositionType,
            taal = taal,
            raag = result.raag,
            laya = result.laya,
            taanCount = result.taanCount,
            showStrokeLine = result.showStrokeLine,
            showSahityaLine = result.showSahityaLine,
            gatStartingBeat = result.gatStartingBeat,
            antaraStartingBeat = result.antaraStartingBeat,
            taanStartingBeat = result.taanStartingBeat
          )
          AppLogger.info(
            s"New composition: type=${result.compositionType}, title=${result.title}, taal=${result.taalName}, file=${result.filePath}"
          )
          val et = tabManager.newTab()
          et.editorPane.setReadOnly(false)
          et.editorPane.setEditor(editor)
          et.editorPane.setFilePathAndSave(result.filePath)
          et.editorPane.changeScript(result.script)
          et.filePath = Some(result.filePath)
          tabManager.tabPane.selectionModel.value.select(et.tab)
          keyboardLegend.updateScript(result.script)
          statusBar.log(s"New ${result.compositionType} created: ${result.title} -> ${result.filePath}")
        }
        focusActiveEditor()

    val openBtn = new Button("Open File"):
      style = btnStyle
      graphic = iconLabel("📄")
      tooltip = new Tooltip("Open a .swar file")
      onAction = _ =>
        val fc = new FileChooser:
          title = "Open Composition"
          extensionFilters.add(new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
        val file = fc.showOpenDialog(stage)
        if file != null then tabManager.openFile(file.toPath)
        focusActiveEditor()

    var leftPanelExpanded   = true
    var bottomPanelExpanded = true
    var rightPanelExpanded  = true

    val openFolderBtn = new Button("Open Folder"):
      style = btnStyle
      graphic = iconLabel("📂")
      tooltip = new Tooltip("Open a folder in the file browser")

    val saveBtn = new Button("Save"):
      style = btnStyle
      graphic = iconLabel("💾")
      tooltip = new Tooltip("Save composition to current file")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getComposition.foreach { comp =>
            et.filePath match
              case Some(path) =>
                SwarFormat.writeFile(path, comp)
                AppLogger.info(s"File saved: $path")
                statusBar.log(s"Saved: ${path.getFileName}")
              case None =>
                val fc = new FileChooser:
                  title = "Save Composition"
                  extensionFilters.add(new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
                val file = fc.showSaveDialog(stage)
                if file != null then
                  val path =
                    if file.getName.endsWith(".swar") then file.toPath
                    else Path.of(file.getPath + ".swar")
                  SwarFormat.writeFile(path, comp)
                  et.editorPane.setFilePath(path)
                  et.filePath = Some(path)
                  AppLogger.info(s"File saved: $path")
                  statusBar.log(s"Saved: ${file.getName}")
          }
        }
        focusActiveEditor()

    val saveAsBtn = new Button("Save As"):
      style = btnStyle
      graphic = iconLabel("📋")
      tooltip = new Tooltip("Save composition as a new .swar file")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getComposition.foreach { comp =>
            val fc = new FileChooser:
              title = "Save Composition As"
              extensionFilters.add(new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
            val file = fc.showSaveDialog(stage)
            if file != null then
              val path =
                if file.getName.endsWith(".swar") then file.toPath
                else Path.of(file.getPath + ".swar")
              SwarFormat.writeFile(path, comp)
              et.editorPane.setFilePath(path)
              et.filePath = Some(path)
              AppLogger.info(s"File saved as: $path")
              statusBar.log(s"Saved as: ${file.getName}")
          }
        }
        focusActiveEditor()

    val cutBtn = new Button("Cut"):
      style = btnStyle
      graphic = iconLabel("✂")
      tooltip = new Tooltip("Cut selected events (Ctrl+X)")
      onAction = _ =>
        withActiveEditor(_.cutSelection())
        focusActiveEditor()

    val copyBtn = new Button("Copy"):
      style = btnStyle
      graphic = iconLabel("📋")
      tooltip = new Tooltip("Copy selected events (Ctrl+C)")
      onAction = _ =>
        withActiveEditor(_.copySelection())
        focusActiveEditor()

    val pasteBtn = new Button("Paste"):
      style = btnStyle
      graphic = iconLabel("📌")
      tooltip = new Tooltip("Paste clipboard events (Ctrl+V)")
      onAction = _ =>
        withActiveEditor(_.pasteClipboard())
        focusActiveEditor()

    val htmlBtn = new Button("HTML"):
      style = btnStyle
      graphic = iconLabel("🌐")
      tooltip = new Tooltip("Export composition as HTML")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getComposition.foreach { comp =>
            val fc = new FileChooser:
              title = "Export HTML"
              extensionFilters.add(new FileChooser.ExtensionFilter("HTML Files", "*.html"))
            val file = fc.showSaveDialog(stage)
            if file != null then
              val path =
                if file.getName.endsWith(".html") then file.toPath
                else Path.of(file.getPath + ".html")
              HtmlExport.exportHtml(comp, path, et.editorPane.currentScript)
              AppLogger.info(s"HTML exported: $path")
              statusBar.log(s"Exported HTML: ${file.getName}")
          }
        }
        focusActiveEditor()

    val propertiesBtn = new Button("Properties"):
      style = btnStyle
      graphic = iconLabel("⚙")
      tooltip = new Tooltip("Edit composition metadata")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getComposition.foreach { comp =>
            CompositionPropertiesDialog.show(comp.metadata, comp.sections, stage).foreach { result =>
              et.editorPane.applyMetadataChange(result.metadata)
              if result.sectionStartingBeats.nonEmpty then
                et.editorPane.applySectionStartingBeats(result.sectionStartingBeats)
              statusBar.log(s"Updated composition properties")
            }
          }
        }
        focusActiveEditor()

    val addSectionBtn = new Button("Add Section"):
      style = btnStyle
      graphic = iconLabel("➕")
      tooltip = new Tooltip("Add a new section to the composition")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getComposition.foreach { comp =>
            if comp.metadata.compositionType != CompositionType.Gat then
              statusBar.log("Sections can only be added to Gat compositions")
            else
              val choices = java.util.Arrays.asList("Gat", "Sthayi", "Antara", "Taan", "Jhala", "Jod")
              val dialog  = new javafx.scene.control.ChoiceDialog[String]("Taan", choices)
              dialog.initOwner(stage)
              dialog.setTitle("Add Section")
              dialog.setHeaderText("Choose section type")
              val result = dialog.showAndWait()
              if result.isPresent then
                val choice = result.get()
                val sType = choice match
                  case "Gat"    => SectionType.Custom("Gat")
                  case "Sthayi" => SectionType.Sthayi
                  case "Antara" => SectionType.Antara
                  case "Taan"   => SectionType.Taan
                  case "Jhala"  => SectionType.Jhala
                  case "Jod"    => SectionType.Custom("Jod")
                  case other    => SectionType.Custom(other)
                val newSection = Section(choice, sType, Nil)
                val newComp    = comp.copy(sections = comp.sections :+ newSection)
                et.editorPane.setComposition(newComp)
                statusBar.log(s"Added section: $choice")
          }
        }
        focusActiveEditor()

    val renameSectionBtn = new Button("Rename Section"):
      style = btnStyle
      graphic = iconLabel("✏")
      tooltip = new Tooltip("Rename the current section")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getEditor.foreach { ed =>
            val section = ed.currentSection
            val dialog  = new javafx.scene.control.TextInputDialog(section.name)
            dialog.initOwner(stage)
            dialog.setTitle("Rename Section")
            dialog.setHeaderText("Enter new section name")
            val result = dialog.showAndWait()
            if result.isPresent && result.get().trim.nonEmpty then
              val newEd = ed.renameSection(ed.currentSectionIndex, result.get().trim)
              et.editorPane.setEditor(newEd)
              statusBar.log(s"Renamed section to: ${result.get().trim}")
          }
        }
        focusActiveEditor()

    val removeSectionBtn = new Button("Remove Section"):
      style = btnStyle
      graphic = iconLabel("➖")
      tooltip = new Tooltip("Remove the current section")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getEditor.foreach { ed =>
            val sectionName = ed.currentSection.name
            ed.removeSection(ed.currentSectionIndex) match
              case Some(newEd) =>
                et.editorPane.setEditor(newEd)
                statusBar.log(s"Removed section: $sectionName")
              case None =>
                statusBar.log("Cannot remove the last section")
          }
        }
        focusActiveEditor()

    val moveUpBtn = new Button("Move Up"):
      style = btnStyle
      graphic = iconLabel("⬆")
      tooltip = new Tooltip("Move current section up")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getEditor.foreach { ed =>
            if ed.currentSectionIndex > 0 then
              val newEd = ed.moveSection(ed.currentSectionIndex, ed.currentSectionIndex - 1)
              et.editorPane.setEditor(newEd)
              statusBar.log(s"Moved section up")
            else statusBar.log("Already at top")
          }
        }
        focusActiveEditor()

    val moveDownBtn = new Button("Move Down"):
      style = btnStyle
      graphic = iconLabel("⬇")
      tooltip = new Tooltip("Move current section down")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getEditor.foreach { ed =>
            if ed.currentSectionIndex < ed.composition.sections.size - 1 then
              val newEd = ed.moveSection(ed.currentSectionIndex, ed.currentSectionIndex + 1)
              et.editorPane.setEditor(newEd)
              statusBar.log(s"Moved section down")
            else statusBar.log("Already at bottom")
          }
        }
        focusActiveEditor()

    // Script dropdown
    val scriptCombo = new ComboBox[String](
      ObservableBuffer(
        "Devanagari (Hindi)",
        "Kannada",
        "Telugu",
        "English"
      )
    ):
      style = btnStyle
      value = "Devanagari (Hindi)"
      tooltip = new Tooltip("Change notation script")
    scriptCombo.value.addListener { (_, _, newVal) =>
      if newVal != null then
        val script = newVal match
          case "Kannada" => SwarScript.Kannada
          case "Telugu"  => SwarScript.Telugu
          case "English" => SwarScript.English
          case _         => SwarScript.Devanagari
        AppLogger.info(s"Script changed: $script")
        withActiveEditor(_.changeScript(script))
        keyboardLegend.updateScript(script)
        statusBar.log(s"Script changed to ${ScriptMap.displayName(script)}")
        focusActiveEditor()
    }

    val undoBtn = new Button("Undo"):
      style = btnStyle
      graphic = iconLabel("↩")
      tooltip = new Tooltip("Undo last edit (Ctrl+Z)")
      onAction = _ =>
        statusBar.log("Use Ctrl+Z (Cmd+Z on Mac) for undo")
        focusActiveEditor()

    val redoBtn = new Button("Redo"):
      style = btnStyle
      graphic = iconLabel("↪")
      tooltip = new Tooltip("Redo (Ctrl+Shift+Z)")
      onAction = _ =>
        statusBar.log("Use Ctrl+Shift+Z (Cmd+Shift+Z on Mac) for redo")
        focusActiveEditor()

    val spacer = new Region()
    HBox.setHgrow(spacer, Priority.Always)

    val aboutBtn = new Button("About"):
      style = btnStyle
      graphic = iconLabel("ℹ")
      tooltip = new Tooltip("About Sangeet Notes Editor")
      onAction = _ =>
        val dialog = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
        dialog.initOwner(stage)
        dialog.setTitle("About")
        dialog.setHeaderText("Sangeet Notes Editor")
        dialog.setContentText("""A desktop notation editor for Hindustani classical music
            |in the Bhatkhande notation style.
            |
            |Designed for sitar compositions -- Gat, Bandish, and Palta.
            |
            |Version 1.0
            |Built with Scala 3 + ScalaFX""".stripMargin)
        dialog.showAndWait()
        focusActiveEditor()

    val toolbar = new ToolBar:
      items = List(
        newBtn,
        openBtn,
        openFolderBtn,
        saveBtn,
        saveAsBtn,
        cutBtn,
        copyBtn,
        pasteBtn,
        htmlBtn,
        new Separator(),
        propertiesBtn,
        addSectionBtn,
        renameSectionBtn,
        removeSectionBtn,
        moveUpBtn,
        moveDownBtn,
        new Separator(),
        undoBtn,
        redoBtn,
        new Separator(),
        new Label("Script:"):
          style = "-fx-font-size: 11px;"
        ,
        scriptCombo,
        spacer,
        aboutBtn
      )

    // ── Layout ─────────────────────────────────────────────────────────

    // Vertical split: tabbed editor on top, log on bottom (resizable)
    val verticalSplit = new SplitPane:
      orientation = Orientation.Vertical
      items.addAll(tabManager.editorArea, statusBar)
    verticalSplit.setDividerPosition(0, 0.82)

    // Main horizontal split: editor+status in center, keyboard reference on right
    val horizontalSplit = new SplitPane:
      items.addAll(verticalSplit, keyboardLegend)
    horizontalSplit.setDividerPosition(0, 0.72)

    // Outer split: file browser on left, rest on right
    val mainSplit = new SplitPane:
      items.addAll(fileBrowserPanel.panel, horizontalSplit)
    mainSplit.setDividerPosition(0, 0.18)

    // ── Panel Collapse State ──────────────────────────────────────────

    val collapseStripStyle =
      "-fx-background-color: #e8e8e8; -fx-border-color: #ccc; -fx-border-width: 1;"
    val collapseArrowStyle =
      "-fx-font-size: 10px; -fx-padding: 4 2 4 2; -fx-background-color: transparent; -fx-cursor: hand;"

    val leftExpandBtn = new Button("»"):
      style = collapseArrowStyle
      tooltip = new Tooltip("Show file browser (Ctrl+B)")

    val bottomExpandBtn = new Button("▲ Log"):
      style = collapseArrowStyle
      tooltip = new Tooltip("Show log panel")

    val rightExpandBtn = new Button("«"):
      style = collapseArrowStyle
      tooltip = new Tooltip("Show keyboard reference")

    val leftCollapsedStrip = new VBox:
      maxWidth = 24
      minWidth = 24
      prefWidth = 24
      style = collapseStripStyle
      alignment = Pos.TopCenter
      padding = Insets(4, 0, 0, 0)
      children = Seq(leftExpandBtn)

    val bottomCollapsedStrip = new HBox:
      maxHeight = 24
      minHeight = 24
      prefHeight = 24
      style = collapseStripStyle
      alignment = Pos.CenterLeft
      padding = Insets(0, 0, 0, 4)
      children = Seq(bottomExpandBtn)

    val rightCollapsedStrip = new VBox:
      maxWidth = 24
      minWidth = 24
      prefWidth = 24
      style = collapseStripStyle
      alignment = Pos.TopCenter
      padding = Insets(4, 0, 0, 0)
      children = Seq(rightExpandBtn)

    def collapseLeftPanel(): Unit =
      if !leftPanelExpanded then return
      leftPanelExpanded = false
      mainSplit.items.remove(fileBrowserPanel.panel)
      mainSplit.items.add(0, leftCollapsedStrip)
      mainSplit.setDividerPosition(0, 24.0 / mainSplit.width.value)
      focusActiveEditor()

    def expandLeftPanel(): Unit =
      if leftPanelExpanded then return
      leftPanelExpanded = true
      mainSplit.items.remove(leftCollapsedStrip)
      mainSplit.items.add(0, fileBrowserPanel.panel)
      mainSplit.setDividerPosition(0, 0.18)
      focusActiveEditor()

    def collapseBottomPanel(): Unit =
      if !bottomPanelExpanded then return
      bottomPanelExpanded = false
      verticalSplit.items.remove(statusBar)
      verticalSplit.items.add(bottomCollapsedStrip)
      verticalSplit.setDividerPosition(0, 1.0 - 24.0 / verticalSplit.height.value)
      focusActiveEditor()

    def expandBottomPanel(): Unit =
      if bottomPanelExpanded then return
      bottomPanelExpanded = true
      verticalSplit.items.remove(bottomCollapsedStrip)
      verticalSplit.items.add(statusBar)
      verticalSplit.setDividerPosition(0, 0.82)
      focusActiveEditor()

    def collapseRightPanel(): Unit =
      if !rightPanelExpanded then return
      rightPanelExpanded = false
      horizontalSplit.items.remove(keyboardLegend)
      horizontalSplit.items.add(rightCollapsedStrip)
      horizontalSplit.setDividerPosition(
        horizontalSplit.dividerPositions.length - 1,
        1.0 - 24.0 / horizontalSplit.width.value
      )
      focusActiveEditor()

    def expandRightPanel(): Unit =
      if rightPanelExpanded then return
      rightPanelExpanded = true
      horizontalSplit.items.remove(rightCollapsedStrip)
      horizontalSplit.items.add(keyboardLegend)
      horizontalSplit.setDividerPosition(horizontalSplit.dividerPositions.length - 1, 0.72)
      focusActiveEditor()

    // Wire button handlers (after function definitions to avoid forward references)
    leftExpandBtn.onAction = _ => expandLeftPanel()
    bottomExpandBtn.onAction = _ => expandBottomPanel()
    rightExpandBtn.onAction = _ => expandRightPanel()

    openFolderBtn.onAction = _ =>
      val dc = new DirectoryChooser:
        title = "Open Folder"
      val dir = dc.showDialog(stage)
      if dir != null then
        fileBrowserPanel.addDirectory(dir.toPath)
        if !leftPanelExpanded then expandLeftPanel()
        statusBar.log(s"Opened folder: ${dir.getName}")
      focusActiveEditor()

    // Collapse buttons injected into panel headers
    val collapseLeftBtn = new Button("«"):
      style = collapseArrowStyle
      tooltip = new Tooltip("Hide file browser")
      onAction = _ => collapseLeftPanel()
    fileBrowserPanel.setCollapseButton(collapseLeftBtn)

    val collapseBottomBtn = new Button("▼"):
      style = collapseArrowStyle
      tooltip = new Tooltip("Hide log panel")
      onAction = _ => collapseBottomPanel()
    statusBar.setCollapseButton(collapseBottomBtn)

    val collapseRightBtn = new Button("»"):
      style = collapseArrowStyle
      tooltip = new Tooltip("Hide keyboard reference")
      onAction = _ => collapseRightPanel()
    keyboardLegend.setCollapseButton(collapseRightBtn)

    stage = new PrimaryStage:
      title = "Sangeet Notes Editor"
      width = 1400
      height = 800
      scene = new Scene:
        fill = Color.White
        root = new BorderPane:
          top = toolbar
          center = mainSplit

    // Scene-level keyboard shortcuts
    stage.scene.value.addEventFilter(
      javafx.scene.input.KeyEvent.KEY_PRESSED,
      event =>
        import javafx.scene.input.{KeyCode => JKeyCode}
        val ctrl = event.isShortcutDown
        if ctrl && event.getCode == JKeyCode.B then
          if leftPanelExpanded then collapseLeftPanel() else expandLeftPanel()
          event.consume()
        else if ctrl && event.isShiftDown && event.getCode == JKeyCode.O then
          openFolderBtn.fire()
          event.consume()
        else if ctrl && event.getCode == JKeyCode.W then
          tabManager.activeTab.foreach(tabManager.closeTab)
          event.consume()
        else if ctrl && !event.isShiftDown && event.getCode == JKeyCode.TAB then
          tabManager.selectNextTab()
          event.consume()
        else if ctrl && event.isShiftDown && event.getCode == JKeyCode.TAB then
          tabManager.selectPreviousTab()
          event.consume()
    )

    // Set window/taskbar icon
    val iconPaths = List("packaging/icons/sangeet-icon-256.png", "packaging/icons/sangeet-icon-64.png")
    for path <- iconPaths do
      val file = java.io.File(path)
      if file.exists then stage.icons.add(new scalafx.scene.image.Image(file.toURI.toString))

    def buildConfig(): AppConfig =
      val panelWidth =
        if leftPanelExpanded && mainSplit.dividerPositions.nonEmpty then
          mainSplit.dividerPositions.head * stage.width.value
        else 250.0
      AppConfig(
        bookmarks = fileBrowserPanel.getBookmarks,
        openTabs = tabManager.getOpenTabs,
        activeTabPath = tabManager.activeTabPath,
        leftPanelWidth = panelWidth,
        leftPanelCollapsed = !leftPanelExpanded,
        bottomPanelCollapsed = !bottomPanelExpanded,
        rightPanelCollapsed = !rightPanelExpanded
      )

    val configSaveTimer = new java.util.Timer("config-save-timer", true)
    configSaveTimer.scheduleAtFixedRate(
      new java.util.TimerTask:
        def run(): Unit =
          javafx.application.Platform.runLater(() =>
            try ConfigStore.save(buildConfig())
            catch case _: Exception => ()
          )
      ,
      30000L,
      30000L
    )

    stage.delegate.setOnCloseRequest { _ =>
      configSaveTimer.cancel()
      tabManager.autoSaveActive()
      try
        ConfigStore.save(buildConfig())
        AppLogger.info("Config saved on exit")
      catch case ex: Exception => AppLogger.info(s"Failed to save config: ${ex.getMessage}")
      debugConsole.stop()
    }

    // Load previous session or sample composition on startup
    javafx.application.Platform.runLater(() =>
      val config = ConfigStore.load()
      if config.bookmarks.nonEmpty then fileBrowserPanel.setBookmarks(config.bookmarks)
      if config.leftPanelCollapsed then collapseLeftPanel()
      if config.bottomPanelCollapsed then collapseBottomPanel()
      if config.rightPanelCollapsed then collapseRightPanel()
      if config.openTabs.nonEmpty then
        tabManager.restoreTabs(config)
        statusBar.log(s"Restored ${config.openTabs.size} tab(s) from previous session")
      else
        val sample = SampleComposition.build()
        initialTab.editorPane.setComposition(sample)
        initialTab.editorPane.setReadOnly(true)
        statusBar.log("Uneditable sample loaded")
        statusBar.log("To start, click New to create a composition")
      focusActiveEditor()
    )
