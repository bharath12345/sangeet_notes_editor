package com.varpas.sangeet.desktop.dialog

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Hyperlink, Label, Separator}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{HBox, Priority, VBox}
import scalafx.stage.{Modality, Stage, StageStyle}

object SupportDialog:

  private val UpiHandle         = "bharath12345-1@oksbi"
  private val UpiQrResourcePath = "/images/upi-qr.png"

  // International section is hidden until a working payment platform is set up.
  // PayPal account is pending activation; once active, set this to true and fill in the URL.
  private val ShowInternational   = false
  private val SupportPlatformName = "PayPal"
  private val SupportPlatformUrl  = ""

  private def openInBrowser(url: String): Unit =
    try java.awt.Desktop.getDesktop.browse(java.net.URI.create(url))
    catch case _: Exception => ()

  private def loadQrImage(): Option[Image] =
    Option(getClass.getResourceAsStream(UpiQrResourcePath)).map(new Image(_))

  def show(owner: javafx.stage.Stage): Unit =
    val header = new Label("Support the Project"):
      style = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #8B1A1A;"

    val intro = new Label(
      "Sangeet Notes Editor is free and always will be — all features, no restrictions. " +
        "If it has helped you preserve or share music, you can support continued development:"
    ):
      style = "-fx-font-size: 13px; -fx-text-fill: #2D2926;"
      wrapText = true
      maxWidth = 460

    // India / UPI section
    val upiHeader = new Label("For users in India — UPI"):
      style = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #5A2828;"

    val upiHandleLabel = new Label(s"UPI handle: $UpiHandle"):
      style = "-fx-font-size: 13px; -fx-font-family: monospace; -fx-text-fill: #2D2926;"

    val upiQrView = loadQrImage() match
      case Some(img) =>
        new ImageView(img):
          fitWidth = 240
          fitHeight = 260
          preserveRatio = true
      case None =>
        new ImageView:
          fitWidth = 240
          fitHeight = 260

    val upiQrPlaceholder = new Label("(QR code image will appear here)"):
      style = "-fx-font-size: 11px; -fx-text-fill: #6A5A4A; -fx-font-style: italic;"
      visible = loadQrImage().isEmpty
      managed = loadQrImage().isEmpty

    val upiBox = new VBox:
      spacing = 6
      alignment = Pos.CenterLeft
      children = Seq(upiHeader, upiHandleLabel, upiQrView, upiQrPlaceholder)

    val intlBoxOpt: Option[VBox] =
      if !ShowInternational then None
      else
        val intlHeader = new Label("For international users"):
          style = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #5A2828;"
        val intlLink = new Hyperlink(s"Support via $SupportPlatformName"):
          style = "-fx-font-size: 13px;"
          onAction = _ => openInBrowser(SupportPlatformUrl)
        Some(new VBox:
          spacing = 6
          alignment = Pos.CenterLeft
          children = Seq(intlHeader, intlLink)
        )

    val thankYou = new Label("🙏 Thank you for your support."):
      style = "-fx-font-size: 12px; -fx-text-fill: #5A2828; -fx-font-style: italic;"

    val closeBtn = new Button("Close"):
      style = "-fx-font-size: 12px;"
      defaultButton = true

    val buttonRow = new HBox:
      alignment = Pos.CenterRight
      spacing = 8
      padding = Insets(8, 0, 0, 0)
      children = Seq(closeBtn)
    HBox.setHgrow(buttonRow, Priority.Always)

    val baseChildren = Seq(header, intro, new Separator(), upiBox)
    val tailChildren = intlBoxOpt match
      case Some(box) => Seq(new Separator(), box, thankYou, buttonRow)
      case None      => Seq(thankYou, buttonRow)

    val rootPane = new VBox:
      spacing = 10
      padding = Insets(20)
      style = "-fx-background-color: #FDF6EC;"
      children = baseChildren ++ tailChildren

    val dialogStage = new Stage:
      initStyle(StageStyle.Utility)
      initModality(Modality.WindowModal)
      width = 520
      scene = new Scene:
        root = rootPane
    dialogStage.title = "Support — Sangeet Notes Editor"
    dialogStage.initOwner(owner)
    closeBtn.onAction = _ => dialogStage.close()
    dialogStage.showAndWait()
