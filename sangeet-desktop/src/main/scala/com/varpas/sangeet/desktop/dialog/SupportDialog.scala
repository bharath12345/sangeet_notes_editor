package com.varpas.sangeet.desktop.dialog

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Hyperlink, Label, Separator}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{HBox, Priority, VBox}
import scalafx.stage.{Modality, Stage, StageStyle}

import com.varpas.sangeet.core.strings.UiStrings

object SupportDialog:

  private val UpiHandle         = "bharath12345-1@oksbi"
  private val UpiQrResourcePath = "/images/upi-qr.png"

  private val ShowInternational   = true
  private val SupportPlatformName = "PayPal"
  private val SupportPlatformUrl  = "https://www.paypal.com/ncp/payment/4NZ6FZZFVQMR6"

  private def openInBrowser(url: String): Unit =
    try java.awt.Desktop.getDesktop.browse(java.net.URI.create(url))
    catch case _: Exception => ()

  private def loadQrImage(): Option[Image] =
    Option(getClass.getResourceAsStream(UpiQrResourcePath)).map(new Image(_))

  def show(owner: javafx.stage.Stage): Unit =
    val header = new Label(UiStrings.dialogSupportTitle):
      style = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #8B1A1A;"

    val intro = new Label(UiStrings.dialogSupportIntro):
      style = "-fx-font-size: 13px; -fx-text-fill: #2D2926;"
      wrapText = true
      maxWidth = 460

    // India / UPI section
    val upiHeader = new Label(UiStrings.dialogSupportUpiHeader):
      style = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #5A2828;"

    val upiHandleLabel = new Label(UiStrings.dialogSupportUpiHandleLabelWithValue(UpiHandle)):
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

    val upiQrPlaceholder = new Label(UiStrings.dialogSupportUpiQrPlaceholder):
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
        val intlHeader = new Label(UiStrings.dialogSupportInternationalHeader):
          style = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #5A2828;"
        val intlLink = new Hyperlink(UiStrings.dialogSupportInternationalPlatformLink(SupportPlatformName)):
          style = "-fx-font-size: 13px;"
          onAction = _ => openInBrowser(SupportPlatformUrl)
        Some(new VBox:
          spacing = 6
          alignment = Pos.CenterLeft
          children = Seq(intlHeader, intlLink)
        )

    val thankYou = new Label(UiStrings.dialogSupportThankYou):
      style = "-fx-font-size: 12px; -fx-text-fill: #5A2828; -fx-font-style: italic;"

    val closeBtn = new Button(UiStrings.dialogSupportClose):
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
    dialogStage.title = UiStrings.dialogSupportWindowTitle
    dialogStage.initOwner(owner)
    closeBtn.onAction = _ => dialogStage.close()
    dialogStage.showAndWait()
