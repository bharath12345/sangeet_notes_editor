package com.varpas.sangeet.desktop

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.geometry.{Insets, Orientation, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{SplitPane, _}
import scalafx.scene.layout.{BorderPane, HBox, VBox}
import scalafx.scene.paint.Color
import scalafx.stage.DirectoryChooser

import com.varpas.sangeet.core.config.{AppConfig, ConfigStore}
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

  override def start(): Unit =
    val logPath = AppLogger.initialize()
    System.err.println(s"Log file: $logPath")

    val statusBar        = new StatusBar()
    val tabManager       = new TabManager(statusBar)
    val keyboardLegend   = new KeyboardLegend()
    val fileBrowserPanel = new FileBrowserPanel(tabManager, statusBar)

    val initialTab   = tabManager.newTab()
    val debugConsole = new DebugConsole(tabManager, statusBar)
    debugConsole.start()

    // ── Toolbar ─────────────────────────────────────────────────────────

    val toolbarBuilder = new ToolbarBuilder(() => stage, tabManager, statusBar, keyboardLegend)
    val toolbar        = toolbarBuilder.build()

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

    var leftPanelExpanded   = true
    var bottomPanelExpanded = true
    var rightPanelExpanded  = true

    def focusActiveEditor(): Unit =
      tabManager.activeTab.foreach(_.editorPane.requestFocus())

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

    toolbarBuilder.openFolderBtn.onAction = _ =>
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
          toolbarBuilder.openFolderBtn.fire()
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
