package com.varpas.sangeet.desktop

import scalafx.animation.{KeyFrame, Timeline}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.Label
import scalafx.scene.effect.DropShadow
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{Background, BackgroundFill, CornerRadii, StackPane, VBox}
import scalafx.scene.paint.Color
import scalafx.stage.{Modality, Stage, StageStyle}
import scalafx.util.Duration

/** Splash window shown at app launch.
  *
  * Layout: option C — minimal silhouette. Solid warm background. Sitar image (if present) centered inside a card. App
  * name + version below.
  *
  * If `/images/splash-sitar.jpg` is bundled in resources, it's shown. Otherwise a placeholder note appears so the
  * splash still renders.
  */
object SplashScreen:

  private val MinDisplayMs = 2500L
  private val ResourcePath = "/images/splash-sitar.jpg"
  private val Version      = "1.0"

  private def loadImage(): Option[Image] =
    Option(getClass.getResourceAsStream(ResourcePath)).map(new Image(_))

  /** Show the splash screen. Returns the Stage. */
  def show(): Stage =
    val image = loadImage()

    val imageCard = image match
      case Some(img) =>
        new ImageView(img):
          fitWidth = 480
          fitHeight = 270
          preserveRatio = true
          smooth = true
          effect = new DropShadow:
            radius = 18
            offsetY = 6
            color = Color.web("#000000", 0.40)
      case None =>
        // No bundled image yet — keep the splash usable.
        new StackPane:
          prefWidth = 480
          prefHeight = 270
          background = new Background(
            Array(
              new BackgroundFill(Color.web("#2A2520"), new CornerRadii(8), Insets.Empty)
            )
          )
          children = Seq(
            new Label("[ sitar image will appear here ]"):
              style = "-fx-text-fill: #C9BEB0; -fx-font-style: italic;"
          )

    val titleLabel = new Label("Sangeet Notes Editor"):
      style = "-fx-text-fill: #FDF6EC; -fx-font-size: 28px; -fx-font-weight: bold;" +
        " -fx-font-family: 'Helvetica Neue', sans-serif;"

    val versionLabel = new Label(s"Version $Version"):
      style = "-fx-text-fill: #E8A317; -fx-font-size: 13px;"

    val subtitleLabel = new Label("Bhatkhande Notation for Sitar"):
      style = "-fx-text-fill: #C9BEB0; -fx-font-size: 12px; -fx-font-style: italic;"

    val content = new VBox:
      alignment = Pos.Center
      spacing = 14
      padding = Insets(32, 32, 32, 32)
      children = Seq(imageCard, titleLabel, versionLabel, subtitleLabel)

    val rootPane = new StackPane:
      background = new Background(
        Array(
          new BackgroundFill(Color.web("#1E1B18"), CornerRadii.Empty, Insets.Empty)
        )
      )
      children = Seq(content)

    val splashScene = new Scene:
      root = rootPane
      fill = Color.web("#1E1B18")

    val splashStage = new Stage:
      initStyle(StageStyle.Undecorated)
      initModality(Modality.None)
      width = 560
      height = 460
      scene = splashScene

    // Center on screen
    val screen = javafx.stage.Screen.getPrimary.getVisualBounds
    splashStage.x = (screen.getWidth - 560) / 2
    splashStage.y = (screen.getHeight - 460) / 2

    splashStage.show()
    splashStage

  /** Hide the splash after at least `MinDisplayMs` has elapsed since `startMs`. */
  def hide(splash: Stage, startMs: Long): Unit =
    val elapsed   = System.currentTimeMillis - startMs
    val remaining = MinDisplayMs - elapsed
    if remaining <= 0 then splash.close()
    else
      val timeline = new Timeline:
        keyFrames = Seq(KeyFrame(Duration(remaining.toDouble), onFinished = _ => splash.close()))
      timeline.play()
