package sangeet.editor

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.layout.{BorderPane, HBox}
import scalafx.scene.paint.Color
import scalafx.geometry.{Insets, Pos}
import scalafx.stage.FileChooser
import sangeet.audio.{MidiEngine, PlaybackController}
import sangeet.format.SwarFormat
import sangeet.model.*
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

  override def start(): Unit =
    val editorPane = new EditorPane()

    val toolbar = new sangeet.editor.ToolBar(
      (taal: Taal) => (),
      () => ()
    )

    val menuBar = new MenuBar:
      menus = List(
        new Menu("File"):
          items = List(
            new MenuItem("New"):
              onAction = _ =>
                val comp = CompositionEditor.empty(Taals.teentaal,
                  Raag("", None, None, None, None, None, None, None))
                editorPane.setEditor(comp)
            ,
            new MenuItem("Open..."):
              onAction = _ =>
                val fc = new FileChooser:
                  title = "Open Composition"
                  extensionFilters.add(
                    new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
                val file = fc.showOpenDialog(stage)
                if file != null then
                  SwarFormat.readFile(file.toPath).foreach(editorPane.setComposition)
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
                }
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
                }
          )
      )

    val playButton = new Button("Play"):
      onAction = _ =>
        editorPane.getComposition.foreach { comp =>
          val bpm = bpmForLaya(comp.metadata.laya)
          val matras = comp.metadata.taal.matras
          val allEvents = comp.sections.flatMap(_.events)
          playbackController.play(allEvents, bpm, matras)
        }

    val stopButton = new Button("Stop"):
      onAction = _ =>
        playbackController.stop()

    val playbackBar = new HBox(10):
      padding = Insets(8)
      alignment = Pos.CenterLeft
      children = List(playButton, stopButton)

    stage = new PrimaryStage:
      title = "Sangeet Notes Editor"
      width = 1200
      height = 800
      scene = new Scene:
        fill = Color.White
        root = new BorderPane:
          top = new scalafx.scene.layout.VBox(menuBar, toolbar)
          center = editorPane
          bottom = playbackBar

    val samplePath = Path.of("samples/yaman-vilambit-gat.swar")
    if Files.exists(samplePath) then
      SwarFormat.readFile(samplePath).foreach(editorPane.setComposition)

    stage.delegate.setOnCloseRequest(_ => playbackController.shutdown())

    editorPane.requestFocus()
