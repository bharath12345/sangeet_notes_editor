package sangeet.editor

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane
import scalafx.scene.paint.Color

object MainApp extends JFXApp3:

  override def start(): Unit =
    stage = new PrimaryStage:
      title = "Sangeet Notes Editor"
      width = 1200
      height = 800
      scene = new Scene:
        fill = Color.White
        root = new BorderPane:
          center = new EditorPane()
