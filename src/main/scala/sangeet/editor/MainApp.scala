package sangeet.editor

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.layout.{BorderPane, VBox, HBox, Priority}
import scalafx.scene.control.SplitPane
import scalafx.geometry.Orientation
import scalafx.scene.paint.Color
import scalafx.stage.FileChooser
import sangeet.audio.{MidiEngine, PlaybackController}
import sangeet.format.SwarFormat
import sangeet.model.*
import sangeet.render.{DevanagariMap, ScriptMap}
import sangeet.taal.Taals
import java.nio.file.{Path, Files}

object MainApp extends JFXApp3:

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

  private def changeScript(script: SwarScript, editorPane: EditorPane,
                           keyboardLegend: KeyboardLegend, statusBar: StatusBar): Unit =
    DevanagariMap.setScript(script)
    keyboardLegend.updateScript(script)
    editorPane.redraw()
    statusBar.log(s"Script changed to ${ScriptMap.displayName(script)}")

  override def start(): Unit =
    val statusBar = new StatusBar()
    val editorPane = new EditorPane(statusBar)
    val keyboardLegend = new KeyboardLegend()
    val playbackToolbar = new PlaybackToolbar()

    playbackToolbar.setOnPlay { () =>
      editorPane.getComposition.foreach { comp =>
        val bpm = playbackToolbar.bpm
        val matras = comp.metadata.taal.matras
        val allEvents = comp.sections.flatMap(_.events)
        playbackController.play(allEvents, bpm, matras)
        playbackToolbar.setPlaying(true)
        statusBar.log(s"Play at ${bpm.toInt} BPM")
      }
    }

    playbackToolbar.setOnPause { () =>
      playbackController.stop()
      playbackToolbar.setPaused(true)
      statusBar.log("Paused")
    }

    playbackToolbar.setOnStop { () =>
      playbackController.stop()
      playbackToolbar.setPlaying(false)
      statusBar.log("Stopped")
    }

    val menuBar = new MenuBar:
      menus = List(
        new Menu("File"):
          items = List(
            new MenuItem("New..."):
              onAction = _ =>
                NewCompositionDialog.show().foreach { result =>
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
                  editorPane.setReadOnly(false)
                  editorPane.setEditor(editor)
                  editorPane.setFilePathAndSave(result.filePath)
                  playbackToolbar.setBpmForLaya(result.laya)
                  changeScript(result.script, editorPane, keyboardLegend, statusBar)
                  statusBar.log(s"New ${result.compositionType} created: ${result.title} → ${result.filePath}")
                }
                editorPane.requestFocus()
            ,
            new MenuItem("Open..."):
              onAction = _ =>
                val fc = new FileChooser:
                  title = "Open Composition"
                  extensionFilters.add(
                    new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
                val file = fc.showOpenDialog(stage)
                if file != null then
                  SwarFormat.readFile(file.toPath) match
                    case Right(comp) =>
                      editorPane.setReadOnly(false)
                      editorPane.setComposition(comp)
                      editorPane.setFilePath(file.toPath)
                      statusBar.log(s"Opened: ${file.getName}")
                    case Left(err) =>
                      statusBar.log(s"✗ Error opening file: ${err.getMessage}")
                editorPane.requestFocus()
            ,
            new MenuItem("Save As..."):
              onAction = _ =>
                editorPane.getComposition.foreach { comp =>
                  val fc = new FileChooser:
                    title = "Save Composition"
                    extensionFilters.add(
                      new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
                  val file = fc.showSaveDialog(stage)
                  if file != null then
                    val path = if file.getName.endsWith(".swar") then file.toPath
                               else Path.of(file.getPath + ".swar")
                    SwarFormat.writeFile(path, comp)
                    statusBar.log(s"Saved: ${file.getName}")
                }
                editorPane.requestFocus()
            ,
            new MenuItem("Export PDF..."):
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
                      sangeet.format.PdfExport.exportPdf(comp, path)
                      statusBar.log(s"Exported PDF: ${file.getName}")
                    catch case ex: Exception =>
                      statusBar.log(s"✗ PDF export failed: ${ex.getMessage}")
                      ex.printStackTrace()
                }
                editorPane.requestFocus()
            ,
            new MenuItem("Export HTML..."):
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
                    sangeet.format.HtmlExport.exportHtml(comp, path)
                    statusBar.log(s"Exported HTML: ${file.getName}")
                }
                editorPane.requestFocus()
            ,
            new SeparatorMenuItem(),
            new MenuItem("Exit"):
              onAction = _ =>
                javafx.application.Platform.exit()
          )
        ,
        new Menu("Composition"):
          items = List(
            new MenuItem("Properties..."):
              onAction = _ =>
                editorPane.getComposition.foreach { comp =>
                  CompositionPropertiesDialog.show(comp.metadata).foreach { newMeta =>
                    val newComp = comp.copy(metadata = newMeta)
                    editorPane.setComposition(newComp)
                    statusBar.log(s"Updated composition properties")
                  }
                }
                editorPane.requestFocus()
            ,
            new MenuItem("Add Section..."):
              onAction = _ =>
                editorPane.getComposition.foreach { comp =>
                  if comp.metadata.compositionType != CompositionType.Gat then
                    statusBar.log("✗ Sections can only be added to Gat compositions")
                  else
                    val choices = java.util.Arrays.asList(
                      "Gat", "Sthayi", "Antara", "Taan", "Jhala", "Jod")
                    val dialog = new javafx.scene.control.ChoiceDialog[String]("Taan", choices)
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
            ,
            new MenuItem("Rename Section..."):
              onAction = _ =>
                editorPane.getEditor.foreach { ed =>
                  val section = ed.currentSection
                  val dialog = new javafx.scene.control.TextInputDialog(section.name)
                  dialog.setTitle("Rename Section")
                  dialog.setHeaderText("Enter new section name")
                  val result = dialog.showAndWait()
                  if result.isPresent && result.get().trim.nonEmpty then
                    val newEd = ed.renameSection(ed.currentSectionIndex, result.get().trim)
                    editorPane.setEditor(newEd)
                    statusBar.log(s"Renamed section to: ${result.get().trim}")
                }
                editorPane.requestFocus()
            ,
            new MenuItem("Remove Section"):
              onAction = _ =>
                editorPane.getEditor.foreach { ed =>
                  val sectionName = ed.currentSection.name
                  ed.removeSection(ed.currentSectionIndex) match
                    case Some(newEd) =>
                      editorPane.setEditor(newEd)
                      statusBar.log(s"Removed section: $sectionName")
                    case None =>
                      statusBar.log("✗ Cannot remove the last section")
                }
                editorPane.requestFocus()
            ,
            new MenuItem("Move Section Up"):
              onAction = _ =>
                editorPane.getEditor.foreach { ed =>
                  if ed.currentSectionIndex > 0 then
                    val newEd = ed.moveSection(ed.currentSectionIndex, ed.currentSectionIndex - 1)
                    editorPane.setEditor(newEd)
                    statusBar.log(s"Moved section up")
                  else
                    statusBar.log("✗ Already at top")
                }
                editorPane.requestFocus()
            ,
            new MenuItem("Move Section Down"):
              onAction = _ =>
                editorPane.getEditor.foreach { ed =>
                  if ed.currentSectionIndex < ed.composition.sections.size - 1 then
                    val newEd = ed.moveSection(ed.currentSectionIndex, ed.currentSectionIndex + 1)
                    editorPane.setEditor(newEd)
                    statusBar.log(s"Moved section down")
                  else
                    statusBar.log("✗ Already at bottom")
                }
                editorPane.requestFocus()
            ,
            new SeparatorMenuItem(),
            new MenuItem("Change Script..."):
              onAction = _ =>
                val choices = java.util.Arrays.asList(
                  "Devanagari (Hindi)", "Kannada", "Telugu", "English")
                val current = ScriptMap.displayName(DevanagariMap.currentScript)
                val dialog = new javafx.scene.control.ChoiceDialog[String](current, choices)
                dialog.setTitle("Change Script")
                dialog.setHeaderText("Select notation script")
                dialog.setContentText("Script:")
                val result = dialog.showAndWait()
                if result.isPresent then
                  val script = result.get() match
                    case "Kannada"  => SwarScript.Kannada
                    case "Telugu"   => SwarScript.Telugu
                    case "English"  => SwarScript.English
                    case _          => SwarScript.Devanagari
                  changeScript(script, editorPane, keyboardLegend, statusBar)
                editorPane.requestFocus()
          )
        ,
        new Menu("Playback"):
          items = List(
            new MenuItem("Play"):
              onAction = _ =>
                editorPane.getComposition.foreach { comp =>
                  val bpm = playbackToolbar.bpm
                  val matras = comp.metadata.taal.matras
                  val allEvents = comp.sections.flatMap(_.events)
                  playbackController.play(allEvents, bpm, matras)
                  playbackToolbar.setPlaying(true)
                  statusBar.log(s"Play at ${bpm.toInt} BPM")
                }
                editorPane.requestFocus()
            ,
            new MenuItem("Stop"):
              onAction = _ =>
                playbackController.stop()
                playbackToolbar.setPlaying(false)
                statusBar.log("Stopped")
                editorPane.requestFocus()
          )
        ,
        new Menu("Help"):
          items = List(
            new MenuItem("Keyboard Shortcuts"):
              onAction = _ =>
                val dialog = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
                dialog.setTitle("Keyboard Shortcuts")
                dialog.setHeaderText("Sangeet Notes Editor — Shortcuts")
                dialog.setContentText(
                  """Swar Input:
                    |  s/r/g/m/p/d/n — Sa Re Ga Ma Pa Dha Ni
                    |  Shift+R/G/D/N — Komal Re/Ga/Dha/Ni
                    |  Shift+M — Tivra Ma
                    |  ss/rr/gg — Dual swar (double-tap)
                    |
                    |Octave: Ctrl+Up/Down — Taar/Mandra
                    |Subdivision: Ctrl+2..8 — Notes per beat
                    |Rest: 0 or - | Sustain: .
                    |
                    |Navigation:
                    |  Arrow keys — Move cursor
                    |  Tab/Shift+Tab — Next/Prev section
                    |  Backspace — Delete note
                    |
                    |Stroke Edit: F2 — Toggle Da/Ra editing
                    |  d/r — Set Da/Ra | Backspace — Clear
                    |
                    |Undo/Redo: Ctrl+Z / Ctrl+Shift+Z""".stripMargin)
                dialog.getDialogPane.setMinWidth(450)
                dialog.showAndWait()
                editorPane.requestFocus()
          )
        ,
        new Menu("About"):
          items = List(
            new MenuItem("About Sangeet Notes Editor"):
              onAction = _ =>
                val dialog = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
                dialog.setTitle("About")
                dialog.setHeaderText("Sangeet Notes Editor")
                dialog.setContentText(
                  """A desktop notation editor for Hindustani classical music
                    |in the Bhatkhande notation style.
                    |
                    |Designed for sitar compositions — Gat, Bandish, and Palta.
                    |
                    |Version 1.0
                    |Built with Scala 3 + ScalaFX""".stripMargin)
                dialog.showAndWait()
                editorPane.requestFocus()
          )
      )

    // Vertical split: editor on top, key log on bottom (resizable)
    val verticalSplit = new SplitPane:
      orientation = Orientation.Vertical
      items.addAll(editorPane, statusBar)
    verticalSplit.setDividerPosition(0, 0.82)

    // Horizontal split: editor+status on left, keyboard reference on right (resizable)
    val horizontalSplit = new SplitPane:
      items.addAll(verticalSplit, keyboardLegend)
    horizontalSplit.setDividerPosition(0, 0.72)

    val topBox = new VBox:
      children = List(menuBar, playbackToolbar)

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

    stage.delegate.setOnCloseRequest(_ => playbackController.shutdown())

    // Load read-only sample composition on startup
    javafx.application.Platform.runLater(() =>
      val sample = SampleComposition.build()
      editorPane.setComposition(sample)
      editorPane.setReadOnly(true)
      playbackToolbar.setBpmForLaya(sample.metadata.laya)
      statusBar.log("Uneditable sample loaded")
      statusBar.log("To start creating a new composition, go to File > New")
      editorPane.requestFocus()
    )
