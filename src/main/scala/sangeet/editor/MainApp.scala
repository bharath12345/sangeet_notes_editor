package sangeet.editor

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.layout.{BorderPane, VBox, HBox, Priority}
import scalafx.scene.paint.Color
import scalafx.stage.FileChooser
import sangeet.audio.{MidiEngine, PlaybackController}
import sangeet.format.SwarFormat
import sangeet.model.*
import sangeet.render.{DevanagariMap, ScriptMap}
import sangeet.taal.Taals
import java.nio.file.{Path, Files}

object MainApp extends JFXApp3:

  private val playbackController = new PlaybackController(new MidiEngine())

  private def bpmForLaya(laya: Option[Laya]): Double =
    laya match
      case Some(Laya.AtiVilambit) => 30.0
      case Some(Laya.Vilambit)    => 40.0
      case Some(Laya.Madhya)      => 80.0
      case Some(Laya.Drut)        => 160.0
      case Some(Laya.AtiDrut)     => 250.0
      case None                   => 60.0

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
                    laya = result.laya
                  )
                  editorPane.setEditor(editor)
                  changeScript(result.script, editorPane, keyboardLegend, statusBar)
                  statusBar.log(s"New ${result.compositionType} created: ${result.title}")
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
                      editorPane.setComposition(comp)
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
                    val path = if file.getName.endsWith(".pdf") then file.toPath
                               else Path.of(file.getPath + ".pdf")
                    sangeet.format.PdfExport.exportPdf(comp, path)
                    statusBar.log(s"Exported PDF: ${file.getName}")
                }
                editorPane.requestFocus()
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
                  val choices = java.util.Arrays.asList(
                    "Sthayi", "Antara", "Sanchari", "Abhog",
                    "Taan", "Toda", "Jhala", "Palta", "Arohi", "Avarohi")
                  val dialog = new javafx.scene.control.ChoiceDialog[String]("Antara", choices)
                  dialog.setTitle("Add Section")
                  dialog.setHeaderText("Choose section type")
                  val result = dialog.showAndWait()
                  if result.isPresent then
                    val choice = result.get()
                    val sType = choice match
                      case "Sthayi"   => SectionType.Sthayi
                      case "Antara"   => SectionType.Antara
                      case "Sanchari" => SectionType.Sanchari
                      case "Abhog"    => SectionType.Abhog
                      case "Taan"     => SectionType.Taan
                      case "Toda"     => SectionType.Toda
                      case "Jhala"    => SectionType.Jhala
                      case "Palta"    => SectionType.Palta
                      case "Arohi"    => SectionType.Arohi
                      case "Avarohi"  => SectionType.Avarohi
                      case other      => SectionType.Custom(other)
                    val newSection = Section(choice, sType, Nil)
                    val newComp = comp.copy(sections = comp.sections :+ newSection)
                    editorPane.setComposition(newComp)
                    statusBar.log(s"Added section: $choice")
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
            new MenuItem("▶ Play"):
              onAction = _ =>
                editorPane.getComposition.foreach { comp =>
                  val bpm = bpmForLaya(comp.metadata.laya)
                  val matras = comp.metadata.taal.matras
                  val allEvents = comp.sections.flatMap(_.events)
                  playbackController.play(allEvents, bpm, matras)
                  statusBar.log("▶ Playback started")
                }
                editorPane.requestFocus()
            ,
            new MenuItem("■ Stop"):
              onAction = _ =>
                playbackController.stop()
                statusBar.log("■ Playback stopped")
                editorPane.requestFocus()
          )
      )

    // Center area: editor + keyboard legend side by side
    val centerArea = new HBox:
      HBox.setHgrow(editorPane, Priority.Always)
      children = List(editorPane, keyboardLegend)

    stage = new PrimaryStage:
      title = "Sangeet Notes Editor"
      width = 1400
      height = 800
      scene = new Scene:
        fill = Color.White
        root = new BorderPane:
          top = menuBar
          center = centerArea
          bottom = statusBar

    stage.delegate.setOnCloseRequest(_ => playbackController.shutdown())

    // Show New Composition dialog on startup
    javafx.application.Platform.runLater(() =>
      NewCompositionDialog.show() match
        case Some(result) =>
          val taal = Taals.byName(result.taalName).getOrElse(Taals.teentaal)
          val editor = CompositionEditor.create(
            title = result.title,
            compositionType = result.compositionType,
            taal = taal,
            raag = result.raag,
            laya = result.laya
          )
          editorPane.setEditor(editor)
          changeScript(result.script, editorPane, keyboardLegend, statusBar)
          statusBar.log(s"New ${result.compositionType} created: ${result.title}")
        case None =>
          // User cancelled — create a default empty composition
          val editor = CompositionEditor.empty(Taals.teentaal,
            Raag("", None, None, None, None, None, None, None))
          editorPane.setEditor(editor)
          statusBar.log("Ready — Use File > New to create a composition")
      editorPane.requestFocus()
    )
