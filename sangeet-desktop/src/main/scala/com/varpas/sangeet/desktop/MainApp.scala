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

  /** Single source of truth for the desktop app version. Mirrored from `build.sbt`'s `ThisBuild / version`. Used in
    * crash reports, bug reports, and PostHog event properties.
    */
  val AppVersion: String = "0.2.0"

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
    val splashStart = System.currentTimeMillis
    val splash      = SplashScreen.show()

    val logPath = AppLogger.initialize()
    System.err.println(s"Log file: $logPath")

    // Crash capture must be installed BEFORE the EventLogger startup call so
    // that even a crash during EventLogger init still gets written to disk.
    // Default handler covers most threads; the FX Application Thread has its
    // own handler chain installed below.
    com.varpas.sangeet.desktop.diagnostics.CrashCapture.install()
    com.varpas.sangeet.desktop.diagnostics.CrashCapture.installOnCurrentThread()

    com.varpas.sangeet.desktop.diagnostics.EventLogger.recordLifecycle(
      "startup",
      Some(s"javaVersion=${sys.props.getOrElse("java.version", "?")} os=${sys.props.getOrElse("os.name", "?")}")
    )

    // Phase 10: anonymous usage analytics. distinctId is a stable UUID per install at ~/.sangeet/distinct_id.
    // PostHogClient.fromEnv returns a no-op if SANGEET_POSTHOG_API_KEY is unset (and the build-time key is empty)
    // or if SANGEET_ANALYTICS_DISABLED=1 — so the rest of the app can call capture() unconditionally.
    val distinctId     = com.varpas.sangeet.core.config.DistinctIdStore.loadOrCreate()
    val analytics      = com.varpas.sangeet.desktop.diagnostics.PostHogClient.fromEnv(distinctId, AppVersion)
    val sessionStartMs = System.currentTimeMillis()
    val screenBounds   = scalafx.stage.Screen.primary.bounds
    analytics.capture(
      com.varpas.sangeet.desktop.diagnostics.DesktopEvent.AppStarted(
        appVersion = AppVersion,
        os = sys.props.getOrElse("os.name", "?"),
        osVersion = sys.props.getOrElse("os.version", "?"),
        javaVersion = sys.props.getOrElse("java.version", "?"),
        screenW = screenBounds.width.toInt,
        screenH = screenBounds.height.toInt
      )
    )

    // Pending crashes from previous runs surface BEFORE the main window so
    // the user can choose Send/Discard before getting back into editing.
    // Standalone Stage — the main PrimaryStage doesn't exist yet at this
    // point in startup.
    com.varpas.sangeet.desktop.dialog.CrashRecoveryDialog.processPending(analytics = analytics)

    val statusBar        = new StatusBar()
    val tabManager       = new TabManager(statusBar, analytics)
    val keyboardLegend   = new KeyboardLegend()
    val fileBrowserPanel = new FileBrowserPanel(tabManager, statusBar)

    val initialTab   = tabManager.newTab()
    val debugConsole = new DebugConsole(tabManager, statusBar)
    debugConsole.start()

    // ── Toolbar ─────────────────────────────────────────────────────────

    val toolbarBuilder         = new ToolbarBuilder(() => stage, tabManager, statusBar, keyboardLegend, analytics)
    val (toolbar, toolbarActs) = toolbarBuilder.build()

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

    // Task 5: track whether the read-only Yaman sample should auto-load on startup.
    // Initialized from AppConfig in the config-loading block below; mutated when the
    // user clicks the sample-tab's "Don't show on startup" button. Persisted via
    // buildConfig() on every save (timer + onCloseRequest).
    var showSampleOnStartup = true

    def focusActiveEditor(): Unit =
      tabManager.activeTab.foreach(_.editorPane.requestFocus())

    val collapseStripStyle =
      "-fx-background-color: #F5EDE3; -fx-border-color: #D4C8B8; -fx-border-width: 1;"
    val panelBtnStyle =
      "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6 4 6;" +
        " -fx-min-width: 32; -fx-pref-width: 32;"

    val leftExpandBtn = new Button():
      style = panelBtnStyle
      graphic = Icons.make("mdi2c-chevron-double-right", 18)
      tooltip = new Tooltip("Show file browser (Ctrl+B)")

    val bottomExpandBtn = new Button():
      style = panelBtnStyle
      graphic = Icons.make("mdi2c-chevron-double-up", 18)
      tooltip = new Tooltip("Show log panel")

    val rightExpandBtn = new Button():
      style = panelBtnStyle
      graphic = Icons.make("mdi2c-chevron-double-left", 18)
      tooltip = new Tooltip("Show keyboard reference")

    val leftCollapsedStrip = new VBox:
      maxWidth = 32
      minWidth = 32
      prefWidth = 32
      style = collapseStripStyle
      alignment = Pos.TopCenter
      padding = Insets(4, 0, 0, 0)
      children = Seq(leftExpandBtn)

    val bottomCollapsedStrip = new HBox:
      maxHeight = 32
      minHeight = 32
      prefHeight = 32
      style = collapseStripStyle
      alignment = Pos.CenterLeft
      padding = Insets(0, 0, 0, 4)
      children = Seq(bottomExpandBtn)

    val rightCollapsedStrip = new VBox:
      maxWidth = 32
      minWidth = 32
      prefWidth = 32
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
    val collapseLeftBtn = new Button():
      style = panelBtnStyle
      graphic = Icons.make("mdi2c-chevron-double-left", 18)
      tooltip = new Tooltip("Hide file browser")
      onAction = _ => collapseLeftPanel()
    fileBrowserPanel.setCollapseButton(collapseLeftBtn)

    val collapseBottomBtn = new Button():
      style = panelBtnStyle
      graphic = Icons.make("mdi2c-chevron-double-down", 18)
      tooltip = new Tooltip("Hide log panel")
      onAction = _ => collapseBottomPanel()
    statusBar.setCollapseButton(collapseBottomBtn)

    val collapseRightBtn = new Button():
      style = panelBtnStyle
      graphic = Icons.make("mdi2c-chevron-double-right", 18)
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

    // Apply default theme (will be replaced by saved-config theme below if present)
    ThemeManager.apply(stage.scene.value, ThemeManager.Theme.Light)

    toolbarBuilder.themeToggleBtn.onAction = _ =>
      val newTheme = ThemeManager.toggle(stage.scene.value)
      statusBar.log(s"Theme: ${ThemeManager.name(newTheme)}")
      focusActiveEditor()

    // Scene-level keyboard shortcuts. Every action delegates to a toolbar button's
    // `.fire()` so the action path is identical to a click (same analytics, same
    // status-bar log). Handlers that don't map to a single button (panel toggles,
    // tab nav, cheat sheet) call directly. The `?` handler skips if focus is on a
    // text input so it doesn't fight the editor's swar-input area.
    stage.scene.value.addEventFilter(
      javafx.scene.input.KeyEvent.KEY_PRESSED,
      event =>
        import javafx.scene.input.{KeyCode => JKeyCode}
        val ctrl  = event.isShortcutDown
        val shift = event.isShiftDown

        if ctrl && !shift && event.getCode == JKeyCode.B then
          if leftPanelExpanded then collapseLeftPanel() else expandLeftPanel()
          event.consume()
        else if ctrl && shift && event.getCode == JKeyCode.O then
          toolbarBuilder.openFolderBtn.fire()
          event.consume()
        else if ctrl && event.getCode == JKeyCode.W then
          tabManager.activeTab.foreach(tabManager.closeTab)
          event.consume()
        else if ctrl && !shift && event.getCode == JKeyCode.TAB then
          tabManager.selectNextTab()
          event.consume()
        else if ctrl && shift && event.getCode == JKeyCode.TAB then
          tabManager.selectPreviousTab()
          event.consume()

        // ── File ──────────────────────────────────────────────────────────
        else if ctrl && !shift && event.getCode == JKeyCode.N then
          toolbarActs.newBtn.fire(); event.consume()
        else if ctrl && !shift && event.getCode == JKeyCode.O then
          toolbarActs.openBtn.fire(); event.consume()
        else if ctrl && !shift && event.getCode == JKeyCode.S then
          toolbarActs.saveBtn.fire(); event.consume()
        else if ctrl && shift && event.getCode == JKeyCode.S then
          toolbarActs.saveAsBtn.fire(); event.consume()
        else if ctrl && !shift && event.getCode == JKeyCode.E then
          toolbarActs.htmlBtn.fire(); event.consume()

        // ── Edit / sections ───────────────────────────────────────────────
        else if ctrl && !shift && event.getCode == JKeyCode.COMMA then
          toolbarActs.propertiesBtn.fire(); event.consume()
        else if ctrl && shift && event.getCode == JKeyCode.A then
          toolbarActs.addSectionBtn.fire(); event.consume()
        else if !ctrl && !shift && event.getCode == JKeyCode.F2 then
          toolbarActs.renameSectionBtn.fire(); event.consume()
        else if ctrl && shift && event.getCode == JKeyCode.BACK_SPACE then
          toolbarActs.removeSectionBtn.fire(); event.consume()

        // ── View ──────────────────────────────────────────────────────────
        else if ctrl && shift && event.getCode == JKeyCode.T then
          toolbarBuilder.themeToggleBtn.fire(); event.consume()
        else if ctrl && shift && event.getCode == JKeyCode.L then
          cycleScript(toolbarActs.scriptCombo); event.consume()

        // ── Help ──────────────────────────────────────────────────────────
        else if !ctrl && !shift && event.getCode == JKeyCode.F1 then
          toolbarActs.helpBtn.fire(); event.consume()
        else if ctrl && shift && event.getCode == JKeyCode.B then
          toolbarActs.reportBugBtn.fire(); event.consume()
        else if cheatSheetTrigger(event) then
          toolbarBuilder.cheatSheetBtn.fire(); event.consume()
    )

    // `?` opens the cheat sheet, but only if the user isn't currently typing into
    // a TextField/TextArea. The swar-input editor canvas is not a text input, so
    // its swar-typing flow is unaffected.
    def cheatSheetTrigger(event: javafx.scene.input.KeyEvent): Boolean =
      if event.getCharacter == null || event.getCharacter != "?" then false
      else
        val focus = Option(stage.scene.value.getFocusOwner)
        !focus.exists(n => n.isInstanceOf[javafx.scene.control.TextInputControl])

    // Cycle Devanagari → Kannada → Telugu → English → Devanagari. Triggers the
    // combo's change listener which propagates the script change to the editor.
    def cycleScript(combo: javafx.scene.control.ComboBox[String]): Unit =
      val items = combo.getItems
      if items.isEmpty then ()
      else
        val current = combo.getValue
        val idx     = items.indexOf(current)
        val next    = items.get((idx + 1) % items.size)
        combo.setValue(next)

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
        rightPanelCollapsed = !rightPanelExpanded,
        theme = ThemeManager.name(ThemeManager.get),
        showSampleOnStartup = showSampleOnStartup
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
      // Fire + flush AppQuit FIRST so the SDK's background queue has time to drain before close() blocks.
      try
        analytics.capture(
          com.varpas.sangeet.desktop.diagnostics.DesktopEvent.AppQuit(
            sessionDurationMs = System.currentTimeMillis() - sessionStartMs,
            swarInputCount = com.varpas.sangeet.desktop.diagnostics.SessionStats.swarInputCount
          )
        )
        analytics.flush()
        analytics.close()
      catch case _: Throwable => () // analytics must never block exit
      configSaveTimer.cancel()
      tabManager.autoSaveActive()
      try
        ConfigStore.save(buildConfig())
        AppLogger.info("Config saved on exit")
      catch case ex: Exception => AppLogger.info(s"Failed to save config: ${ex.getMessage}")
      debugConsole.stop()
    }

    // Dismiss splash after minimum display interval, on the JavaFX thread
    javafx.application.Platform.runLater(() => SplashScreen.hide(splash, splashStart))

    // Load previous session or sample composition on startup
    javafx.application.Platform.runLater(() =>
      val config = ConfigStore.load()
      if config.bookmarks.nonEmpty then fileBrowserPanel.setBookmarks(config.bookmarks)
      if config.leftPanelCollapsed then collapseLeftPanel()
      if config.bottomPanelCollapsed then collapseBottomPanel()
      if config.rightPanelCollapsed then collapseRightPanel()
      ThemeManager.apply(stage.scene.value, ThemeManager.fromName(config.theme))
      showSampleOnStartup = config.showSampleOnStartup
      if config.openTabs.nonEmpty then
        tabManager.restoreTabs(config)
        statusBar.log(s"Restored ${config.openTabs.size} tab(s) from previous session")
      else if showSampleOnStartup then
        val sample = SampleComposition.build()
        initialTab.editorPane.setComposition(sample)
        initialTab.editorPane.setReadOnly(true)
        // Task 5: dismiss-in-place banner above the sample. Clicking "Don't show on
        // startup" flips the config flag, persists, and closes the tab. Banner shows
        // only above the sample tab; other tabs created later are unaffected because
        // we mutate THIS tab's content directly.
        val dismissBtn = new scalafx.scene.control.Button("Don't show on startup"):
          style = "-fx-background-color: transparent; -fx-text-fill: #6A3E1A;" +
            " -fx-border-color: #B07A3E; -fx-border-radius: 3; -fx-background-radius: 3;" +
            " -fx-padding: 2 8 2 8; -fx-font-size: 11px; -fx-cursor: hand;"
        val bannerLabel = new scalafx.scene.control.Label(
          "This is a read-only sample showing Yaman Vilambit Gat."
        ):
          style = "-fx-font-size: 12px; -fx-text-fill: #4A2F12;"
        val banner = new scalafx.scene.layout.HBox:
          spacing = 12
          padding = scalafx.geometry.Insets(6, 12, 6, 12)
          alignment = scalafx.geometry.Pos.CenterLeft
          style = "-fx-background-color: #FDEFD6; -fx-border-color: #E5C586; -fx-border-width: 0 0 1 0;"
          children = Seq(bannerLabel, dismissBtn)
        val originalContent = initialTab.tab.content.value
        // Drop down to the javafx VBox to mix the scalafx banner with the existing
        // javafx editor node — saves the scalafx<->javafx wrapping dance.
        val wrapped = new javafx.scene.layout.VBox(banner.delegate, originalContent)
        initialTab.tab.delegate.setContent(wrapped)
        // Bypass tabManager.closeTab below — its untitled-with-content prompt would ask
        // "discard?" for the sample. The sample IS disposable by design (read-only,
        // regenerable on next launch if the user re-enables it from About).
        dismissBtn.onAction = _ =>
          showSampleOnStartup = false
          try ConfigStore.save(buildConfig())
          catch case _: Exception => ()
          tabManager.removeUntitledTabSilently(initialTab)
          statusBar.log("Sample dismissed — won't appear on next launch")
        statusBar.log("Uneditable sample loaded")
        statusBar.log("To start, click New to create a composition")
      focusActiveEditor()
    )
