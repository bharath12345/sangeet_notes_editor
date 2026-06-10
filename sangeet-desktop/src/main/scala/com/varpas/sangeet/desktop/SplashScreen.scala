package com.varpas.sangeet.desktop

import scalafx.animation.{KeyFrame, Timeline}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Label
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{
  Background,
  BackgroundFill,
  Border,
  BorderStroke,
  BorderStrokeStyle,
  BorderWidths,
  CornerRadii,
  StackPane,
  VBox
}
import scalafx.scene.paint.Color
import scalafx.scene.{Node, Scene}
import scalafx.stage.{Stage, StageStyle}
import scalafx.util.Duration

/** Splash window shown at app launch.
  *
  * Full-bleed sitar photograph with grayscale text overlaid on the upper third (where the image has negative space). A
  * thin warm-gold border frames the whole splash.
  *
  * Image: Sitar2.jpg from Wikimedia Commons (Public Domain, CC0). Source page:
  * https://commons.wikimedia.org/wiki/File:Sitar2.jpg
  *
  * Falls back to a dark solid background with placeholder text if the bundled image is missing.
  */
object SplashScreen:

  private val MinDisplayMs = 4500L
  private val ResourcePath = "/images/splash-sitar.jpg"
  private val Version      = "1.0"

  // Splash dimensions match the 700x430 image aspect ratio so the photo can be full-bleed.
  private val SplashWidth  = 700
  private val SplashHeight = 430

  private def loadImage(): Option[Image] =
    Option(getClass.getResourceAsStream(ResourcePath)).map(new Image(_))

  private def backgroundNode(image: Option[Image]): Node = image match
    case Some(img) =>
      new ImageView(img):
        fitWidth = SplashWidth
        fitHeight = SplashHeight
        preserveRatio = true
        smooth = true
    case None =>
      new StackPane:
        prefWidth = SplashWidth
        prefHeight = SplashHeight
        background = new Background(
          Array(new BackgroundFill(Color.web("#1E1B18"), CornerRadii.Empty, Insets.Empty))
        )
        children = Seq(
          new Label("[ sitar image will appear here ]"):
            style = "-fx-text-fill: #C9BEB0; -fx-font-style: italic;"
        )

  /** Show the splash screen. Returns the Stage. */
  def show(): Stage =
    val bg = backgroundNode(loadImage())

    // Classical serif font fallback chain. Palatino is the preferred face — refined, vintage feel
    // that pairs well with traditional Indian aesthetics; widely available on macOS / Windows / Linux.
    val titleFont = "'Palatino', 'Palatino Linotype', 'Book Antiqua', Georgia, serif"

    // Tight, low-spread shadow gives crisp edges (not blurry like a large gaussian).
    // Only used on the title; smaller text gets no shadow at all — readability comes from
    // the dark gradient backplate behind the whole text column.
    val titleShadow = "-fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.55), 2, 0, 0, 1);"

    val titleLabel = new Label("Sangeet Notes Editor"):
      style = s"-fx-text-fill: #F4EFE7; -fx-font-size: 32px; -fx-font-family: $titleFont;" +
        " -fx-font-weight: bold;" + titleShadow

    val versionLabel = new Label(s"Version $Version"):
      style = s"-fx-text-fill: #E8C067; -fx-font-size: 13px; -fx-font-family: $titleFont;" +
        " -fx-font-style: italic;"

    val subtitleLabel = new Label("Bhatkhande Notation for Sitar"):
      style = s"-fx-text-fill: #E0D8C8; -fx-font-size: 14px; -fx-font-family: $titleFont;"

    // VBox with a translucent dark gradient backplate. The gradient fades from a strong
    // dark cover at the top to transparent at the bottom, giving the text consistent
    // contrast against any region of the photograph.
    val textColumn = new VBox:
      alignment = Pos.TopCenter
      spacing = 8
      padding = Insets(32, 20, 36, 20)
      prefWidth = SplashWidth
      style = "-fx-background-color: linear-gradient(to bottom," +
        " rgba(20,15,10,0.78) 0%, rgba(20,15,10,0.55) 55%, rgba(20,15,10,0) 100%);"
      children = Seq(titleLabel, versionLabel, subtitleLabel)

    val rootPane = new StackPane:
      prefWidth = SplashWidth
      prefHeight = SplashHeight
      alignment = Pos.TopCenter
      // Explicit background fill — without this, undecorated stages may render
      // transparent on macOS and never appear visually even though JavaFX reports
      // `showing = true`.
      background = new Background(
        Array(new BackgroundFill(Color.web("#1E1B18"), CornerRadii.Empty, Insets.Empty))
      )
      children = Seq(bg, textColumn)
      border = new Border(
        new BorderStroke(
          Color.web("#C9A961"),
          BorderStrokeStyle.Solid,
          CornerRadii.Empty,
          new BorderWidths(2)
        )
      )

    val splashScene = new Scene:
      root = rootPane
      fill = Color.web("#1E1B18")

    val splashStage = new Stage:
      initStyle(StageStyle.Undecorated)
      width = SplashWidth
      height = SplashHeight
      scene = splashScene

    val screen = javafx.stage.Screen.getPrimary.getVisualBounds
    splashStage.x = (screen.getWidth - SplashWidth) / 2
    splashStage.y = (screen.getHeight - SplashHeight) / 2

    // alwaysOnTop ensures the splash stays above the main window while it's still
    // initializing. We turn this off in hide() so it doesn't outlive the splash.
    splashStage.alwaysOnTop = true
    splashStage.show()
    splashStage.toFront()
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
