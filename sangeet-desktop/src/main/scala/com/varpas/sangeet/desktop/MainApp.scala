package com.varpas.sangeet.desktop

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.layout.{BorderPane, VBox, HBox, Priority, Region}
import scalafx.scene.control.SplitPane
import scalafx.geometry.{Insets, Orientation, Pos}
import scalafx.scene.paint.Color
import scalafx.stage.FileChooser
import scalafx.collections.ObservableBuffer
import com.varpas.sangeet.core.audio.{MidiEngine, PlaybackController}
import com.varpas.sangeet.core.format.{SwarFormat, PdfExport, HtmlExport}
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.editor.CompositionEditor
import com.varpas.sangeet.core.render.ScriptMap
import com.varpas.sangeet.core.taal.Taals
import com.varpas.sangeet.desktop.dialog.{NewCompositionDialog, CompositionPropertiesDialog}
import com.varpas.sangeet.desktop.editor.{EditorPane, StatusBar, KeyboardLegend, AppLogger, SampleComposition, DebugConsole}
import java.nio.file.{Path, Files}

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
      val taskbar = java.awt.Taskbar.getTaskbar
      val iconFile = java.io.File("packaging/icons/sangeet-icon-256.png")
      if iconFile.exists then
        taskbar.setIconImage(javax.imageio.ImageIO.read(iconFile))
    catch case _: Exception => ()

  private val playbackController = new PlaybackController(new MidiEngine())

  private def btnStyle = "-fx-font-size: 11px;"
  private def iconLabel(symbol: String) = new scalafx.scene.control.Label(symbol):
    style = "-fx-font-size: 14px;"

  override def start(): Unit =
    val logPath = AppLogger.initialize()
    System.err.println(s"Log file: $logPath")

    val statusBar = new StatusBar()
    val editorPane = new EditorPane(statusBar)
    val keyboardLegend = new KeyboardLegend()

    val debugConsole = new DebugConsole(editorPane, statusBar)
    debugConsole.start()

    // ── Row 1: File + Composition actions ──────────────────────────────

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
            showSahityaLine = result.showSahityaLine
          )
          AppLogger.info(s"New composition: type=${result.compositionType}, title=${result.title}, taal=${result.taalName}, file=${result.filePath}")
          editorPane.setReadOnly(false)
          editorPane.setEditor(editor)
          editorPane.setFilePathAndSave(result.filePath)
          editorPane.changeScript(result.script)
          keyboardLegend.updateScript(result.script)
          statusBar.log(s"New ${result.compositionType} created: ${result.title} -> ${result.filePath}")
        }
        editorPane.requestFocus()

    val openBtn = new Button("Open"):
      style = btnStyle
      graphic = iconLabel("📂")
      tooltip = new Tooltip("Open a .swar file")
      onAction = _ =>
        val fc = new FileChooser:
          title = "Open Composition"
          extensionFilters.add(
            new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
        val file = fc.showOpenDialog(stage)
        if file != null then
          SwarFormat.readFile(file.toPath) match
            case Right(comp) =>
              AppLogger.info(s"File opened: ${file.getAbsolutePath}")
              editorPane.setReadOnly(false)
              editorPane.setComposition(comp)
              editorPane.setFilePath(file.toPath)
              statusBar.log(s"Opened: ${file.getName}")
            case Left(err) =>
              AppLogger.info(s"File open failed: ${file.getAbsolutePath} -- ${err.getMessage}")
              statusBar.log(s"Error opening file: ${err.getMessage}")
        editorPane.requestFocus()

    val saveBtn = new Button("Save"):
      style = btnStyle
      graphic = iconLabel("💾")
      tooltip = new Tooltip("Save composition to current file")
      onAction = _ =>
        editorPane.getComposition.foreach { comp =>
          editorPane.getFilePath match
            case Some(path) =>
              SwarFormat.writeFile(path, comp)
              AppLogger.info(s"File saved: $path")
              statusBar.log(s"Saved: ${path.getFileName}")
            case None =>
              // No file path yet, prompt Save As
              val fc = new FileChooser:
                title = "Save Composition"
                extensionFilters.add(
                  new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
              val file = fc.showSaveDialog(stage)
              if file != null then
                val path = if file.getName.endsWith(".swar") then file.toPath
                           else Path.of(file.getPath + ".swar")
                SwarFormat.writeFile(path, comp)
                editorPane.setFilePath(path)
                AppLogger.info(s"File saved: $path")
                statusBar.log(s"Saved: ${file.getName}")
        }
        editorPane.requestFocus()

    val saveAsBtn = new Button("Save As"):
      style = btnStyle
      graphic = iconLabel("📋")
      tooltip = new Tooltip("Save composition as a new .swar file")
      onAction = _ =>
        editorPane.getComposition.foreach { comp =>
          val fc = new FileChooser:
            title = "Save Composition As"
            extensionFilters.add(
              new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
          val file = fc.showSaveDialog(stage)
          if file != null then
            val path = if file.getName.endsWith(".swar") then file.toPath
                       else Path.of(file.getPath + ".swar")
            SwarFormat.writeFile(path, comp)
            editorPane.setFilePath(path)
            AppLogger.info(s"File saved as: $path")
            statusBar.log(s"Saved as: ${file.getName}")
        }
        editorPane.requestFocus()

    val pdfBtn = new Button("PDF"):
      style = btnStyle
      graphic = iconLabel("📄")
      tooltip = new Tooltip("Export composition as PDF")
      onAction = _ =>
        editorPane.getComposition.foreach { comp =>
          val fc = new FileChooser:
            title = "Export PDF"
            extensionFilters.add(
              new FileChooser.ExtensionFilter("PDF Files", "*.pdf"))
          val file = fc.showSaveDialog(stage)
          if file != null then
            try
              val path = if file.getName.endsWith(".pdf") then file.toPath
                         else Path.of(file.getPath + ".pdf")
              PdfExport.exportPdf(comp, path, editorPane.currentScript)
              AppLogger.info(s"PDF exported: $path")
              statusBar.log(s"Exported PDF: ${file.getName}")
            catch case ex: Exception =>
              AppLogger.info(s"PDF export failed: ${ex.getMessage}")
              statusBar.log(s"PDF export failed: ${ex.getMessage}")
              ex.printStackTrace()
        }
        editorPane.requestFocus()

    val htmlBtn = new Button("HTML"):
      style = btnStyle
      graphic = iconLabel("🌐")
      tooltip = new Tooltip("Export composition as HTML")
      onAction = _ =>
        editorPane.getComposition.foreach { comp =>
          val fc = new FileChooser:
            title = "Export HTML"
            extensionFilters.add(
              new FileChooser.ExtensionFilter("HTML Files", "*.html"))
          val file = fc.showSaveDialog(stage)
          if file != null then
            val path = if file.getName.endsWith(".html") then file.toPath
                       else Path.of(file.getPath + ".html")
            HtmlExport.exportHtml(comp, path, editorPane.currentScript)
            AppLogger.info(s"HTML exported: $path")
            statusBar.log(s"Exported HTML: ${file.getName}")
        }
        editorPane.requestFocus()

    val propertiesBtn = new Button("Properties"):
      style = btnStyle
      graphic = iconLabel("⚙")
      tooltip = new Tooltip("Edit composition metadata")
      onAction = _ =>
        editorPane.getComposition.foreach { comp =>
          CompositionPropertiesDialog.show(comp.metadata, stage).foreach { newMeta =>
            val newComp = comp.copy(metadata = newMeta)
            editorPane.setComposition(newComp)
            statusBar.log(s"Updated composition properties")
          }
        }
        editorPane.requestFocus()

    val addSectionBtn = new Button("Add Section"):
      style = btnStyle
      graphic = iconLabel("➕")
      tooltip = new Tooltip("Add a new section to the composition")
      onAction = _ =>
        editorPane.getComposition.foreach { comp =>
          if comp.metadata.compositionType != CompositionType.Gat then
            statusBar.log("Sections can only be added to Gat compositions")
          else
            val choices = java.util.Arrays.asList(
              "Gat", "Sthayi", "Antara", "Taan", "Jhala", "Jod")
            val dialog = new javafx.scene.control.ChoiceDialog[String]("Taan", choices)
            dialog.initOwner(stage)
            dialog.setTitle("Add Section")
            dialog.setHeaderText("Choose section type")
            val result = dialog.showAndWait()
            if result.isPresent then
              val choice = result.get()
              val sType = choice match
                case "Gat"      => SectionType.Custom("Gat")
                case "Sthayi"   => SectionType.Sthayi
                case "Antara"   => SectionType.Antara
                case "Taan"     => SectionType.Taan
                case "Jhala"    => SectionType.Jhala
                case "Jod"      => SectionType.Custom("Jod")
                case other      => SectionType.Custom(other)
              val newSection = Section(choice, sType, Nil)
              val newComp = comp.copy(sections = comp.sections :+ newSection)
              editorPane.setComposition(newComp)
              statusBar.log(s"Added section: $choice")
        }
        editorPane.requestFocus()

    val renameSectionBtn = new Button("Rename Section"):
      style = btnStyle
      graphic = iconLabel("✏")
      tooltip = new Tooltip("Rename the current section")
      onAction = _ =>
        editorPane.getEditor.foreach { ed =>
          val section = ed.currentSection
          val dialog = new javafx.scene.control.TextInputDialog(section.name)
          dialog.initOwner(stage)
          dialog.setTitle("Rename Section")
          dialog.setHeaderText("Enter new section name")
          val result = dialog.showAndWait()
          if result.isPresent && result.get().trim.nonEmpty then
            val newEd = ed.renameSection(ed.currentSectionIndex, result.get().trim)
            editorPane.setEditor(newEd)
            statusBar.log(s"Renamed section to: ${result.get().trim}")
        }
        editorPane.requestFocus()

    val removeSectionBtn = new Button("Remove Section"):
      style = btnStyle
      graphic = iconLabel("➖")
      tooltip = new Tooltip("Remove the current section")
      onAction = _ =>
        editorPane.getEditor.foreach { ed =>
          val sectionName = ed.currentSection.name
          ed.removeSection(ed.currentSectionIndex) match
            case Some(newEd) =>
              editorPane.setEditor(newEd)
              statusBar.log(s"Removed section: $sectionName")
            case None =>
              statusBar.log("Cannot remove the last section")
        }
        editorPane.requestFocus()

    val moveUpBtn = new Button("Move Up"):
      style = btnStyle
      graphic = iconLabel("⬆")
      tooltip = new Tooltip("Move current section up")
      onAction = _ =>
        editorPane.getEditor.foreach { ed =>
          if ed.currentSectionIndex > 0 then
            val newEd = ed.moveSection(ed.currentSectionIndex, ed.currentSectionIndex - 1)
            editorPane.setEditor(newEd)
            statusBar.log(s"Moved section up")
          else
            statusBar.log("Already at top")
        }
        editorPane.requestFocus()

    val moveDownBtn = new Button("Move Down"):
      style = btnStyle
      graphic = iconLabel("⬇")
      tooltip = new Tooltip("Move current section down")
      onAction = _ =>
        editorPane.getEditor.foreach { ed =>
          if ed.currentSectionIndex < ed.composition.sections.size - 1 then
            val newEd = ed.moveSection(ed.currentSectionIndex, ed.currentSectionIndex + 1)
            editorPane.setEditor(newEd)
            statusBar.log(s"Moved section down")
          else
            statusBar.log("Already at bottom")
        }
        editorPane.requestFocus()

    // Script dropdown
    val scriptCombo = new ComboBox[String](ObservableBuffer(
      "Devanagari (Hindi)", "Kannada", "Telugu", "English"
    )):
      style = btnStyle
      value = "Devanagari (Hindi)"
      tooltip = new Tooltip("Change notation script")
    scriptCombo.value.addListener { (_, _, newVal) =>
      if newVal != null then
        val script = newVal match
          case "Kannada"  => SwarScript.Kannada
          case "Telugu"   => SwarScript.Telugu
          case "English"  => SwarScript.English
          case _          => SwarScript.Devanagari
        AppLogger.info(s"Script changed: $script")
        editorPane.changeScript(script)
        keyboardLegend.updateScript(script)
        statusBar.log(s"Script changed to ${ScriptMap.displayName(script)}")
        editorPane.requestFocus()
    }

    // Voice toggle button (disabled -- coming soon)
    val voiceBtn = new ToggleButton("Voice"):
      style = btnStyle
      graphic = iconLabel("🎙")
      tooltip = new Tooltip("Voice input (coming soon)")
      disable = true

    val undoBtn = new Button("Undo"):
      style = btnStyle
      graphic = iconLabel("↩")
      tooltip = new Tooltip("Undo last edit (Ctrl+Z)")
      onAction = _ =>
        // Undo is handled by keyboard in EditorPane; this button is a convenience
        statusBar.log("Use Ctrl+Z (Cmd+Z on Mac) for undo")
        editorPane.requestFocus()

    val redoBtn = new Button("Redo"):
      style = btnStyle
      graphic = iconLabel("↪")
      tooltip = new Tooltip("Redo (Ctrl+Shift+Z)")
      onAction = _ =>
        statusBar.log("Use Ctrl+Shift+Z (Cmd+Shift+Z on Mac) for redo")
        editorPane.requestFocus()

    val row1 = new ToolBar:
      items = List(
        newBtn, openBtn, saveBtn, saveAsBtn, pdfBtn, htmlBtn,
        new Separator(),
        propertiesBtn, addSectionBtn, renameSectionBtn, removeSectionBtn, moveUpBtn, moveDownBtn,
        new Separator(),
        undoBtn, redoBtn,
        new Separator(),
        new Label("Script:") { style = "-fx-font-size: 11px;" }, scriptCombo,
        voiceBtn
      )

    // ── Row 2: Playback + About ────────────────────────────────────────

    val playBtn = new Button("Play"):
      style = btnStyle
      graphic = iconLabel("▶")
    val pauseBtn = new Button("Pause"):
      style = btnStyle
      graphic = iconLabel("⏸")
      disable = true
    val stopBtn = new Button("Stop"):
      style = btnStyle
      graphic = iconLabel("⏹")
      disable = true

    val loopCheck = new CheckBox("Loop"):
      style = "-fx-font-size: 11px;"

    val bpmLabel = new Label("BPM:"):
      style = "-fx-font-size: 11px;"
    val bpmSlider = new Slider(10, 300, 60):
      prefWidth = 150
      showTickMarks = true
      showTickLabels = true
      majorTickUnit = 50
      blockIncrement = 5
    val bpmValue = new Label("60"):
      style = "-fx-font-size: 11px; -fx-min-width: 30;"

    bpmSlider.value.addListener { (_, _, newVal) =>
      bpmValue.text = newVal.intValue.toString
    }

    def setBpmForLaya(laya: Option[Laya]): Unit =
      val bpmVal = laya match
        case Some(Laya.AtiVilambit) => 30.0
        case Some(Laya.Vilambit)    => 40.0
        case Some(Laya.Madhya)      => 80.0
        case Some(Laya.Drut)        => 160.0
        case Some(Laya.AtiDrut)     => 250.0
        case None                   => 60.0
      bpmSlider.value = bpmVal
      bpmValue.text = bpmVal.toInt.toString

    def setPlaying(playing: Boolean): Unit =
      playBtn.disable = playing
      pauseBtn.disable = !playing
      stopBtn.disable = !playing

    def setPaused(paused: Boolean): Unit =
      playBtn.disable = !paused
      pauseBtn.disable = paused

    playBtn.onAction = _ =>
      editorPane.getComposition.foreach { comp =>
        val bpm = bpmSlider.value.value
        val matras = comp.metadata.taal.matras
        val allEvents = comp.sections.flatMap(_.events)
        playbackController.play(allEvents, bpm, matras)
        setPlaying(true)
        AppLogger.info(s"Playback started: bpm=${bpm.toInt}, matras=$matras, events=${allEvents.size}")
        statusBar.log(s"Play at ${bpm.toInt} BPM")
      }
      editorPane.requestFocus()

    pauseBtn.onAction = _ =>
      playbackController.stop()
      setPaused(true)
      AppLogger.info("Playback paused")
      statusBar.log("Paused")
      editorPane.requestFocus()

    stopBtn.onAction = _ =>
      playbackController.stop()
      setPlaying(false)
      AppLogger.info("Playback stopped")
      statusBar.log("Stopped")
      editorPane.requestFocus()

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
        dialog.setContentText(
          """A desktop notation editor for Hindustani classical music
            |in the Bhatkhande notation style.
            |
            |Designed for sitar compositions -- Gat, Bandish, and Palta.
            |
            |Version 1.0
            |Built with Scala 3 + ScalaFX""".stripMargin)
        dialog.showAndWait()
        editorPane.requestFocus()

    val row2 = new ToolBar:
      items = List(
        playBtn, pauseBtn, stopBtn, new Separator(),
        loopCheck, new Separator(),
        bpmLabel, bpmSlider, bpmValue,
        spacer,
        aboutBtn
      )

    // ── Layout ─────────────────────────────────────────────────────────

    // Vertical split: editor on top, log on bottom (resizable)
    val verticalSplit = new SplitPane:
      orientation = Orientation.Vertical
      items.addAll(editorPane, statusBar)
    verticalSplit.setDividerPosition(0, 0.82)

    // Horizontal split: editor+status on left, keyboard reference on right (resizable)
    val horizontalSplit = new SplitPane:
      items.addAll(verticalSplit, keyboardLegend)
    horizontalSplit.setDividerPosition(0, 0.72)

    val topBox = new VBox:
      children = List(row1, row2)

    stage = new PrimaryStage:
      title = "Sangeet Notes Editor"
      width = 1400
      height = 800
      scene = new Scene:
        fill = Color.White
        root = new BorderPane:
          top = topBox
          center = horizontalSplit

    // Set window/taskbar icon
    val iconPaths = List("packaging/icons/sangeet-icon-256.png", "packaging/icons/sangeet-icon-64.png")
    for path <- iconPaths do
      val file = java.io.File(path)
      if file.exists then
        stage.icons.add(new scalafx.scene.image.Image(file.toURI.toString))

    stage.delegate.setOnCloseRequest { _ =>
      debugConsole.stop()
      playbackController.shutdown()
    }

    // Load read-only sample composition on startup
    javafx.application.Platform.runLater(() =>
      val sample = SampleComposition.build()
      editorPane.setComposition(sample)
      editorPane.setReadOnly(true)
      setBpmForLaya(sample.metadata.laya)
      statusBar.log("Uneditable sample loaded")
      statusBar.log("To start, click New to create a composition")
      editorPane.requestFocus()
    )
