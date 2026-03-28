package sangeet.editor

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane
import scalafx.scene.paint.Color
import sangeet.format.SwarFormat
import java.nio.file.{Path, Files}

object MainApp extends JFXApp3:

  override def start(): Unit =
    val editorPane = new EditorPane()

    stage = new PrimaryStage:
      title = "Sangeet Notes Editor"
      width = 1200
      height = 800
      scene = new Scene:
        fill = Color.White
        root = new BorderPane:
          center = editorPane

    val samplePath = Path.of("samples/yaman-vilambit-gat.swar")
    if Files.exists(samplePath) then
      SwarFormat.readFile(samplePath).foreach(editorPane.setComposition)
