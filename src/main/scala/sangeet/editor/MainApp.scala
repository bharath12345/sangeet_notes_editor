package sangeet.editor

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.layout.BorderPane
import scalafx.scene.paint.Color
import scalafx.stage.FileChooser
import sangeet.format.SwarFormat
import sangeet.model.*
import sangeet.taal.Taals
import java.nio.file.{Path, Files}

object MainApp extends JFXApp3:

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
          )
      )

    stage = new PrimaryStage:
      title = "Sangeet Notes Editor"
      width = 1200
      height = 800
      scene = new Scene:
        fill = Color.White
        root = new BorderPane:
          top = new scalafx.scene.layout.VBox(menuBar, toolbar)
          center = editorPane

    val samplePath = Path.of("samples/yaman-vilambit-gat.swar")
    if Files.exists(samplePath) then
      SwarFormat.readFile(samplePath).foreach(editorPane.setComposition)

    editorPane.requestFocus()
