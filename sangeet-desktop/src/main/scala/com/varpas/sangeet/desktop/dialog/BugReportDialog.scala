package com.varpas.sangeet.desktop.dialog

import io.circe.Json
import javafx.application.Platform
import javafx.concurrent.Task
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label, TextArea, TextField}
import scalafx.scene.layout.{HBox, VBox}
import scalafx.stage.{Modality, Screen, Stage, StageStyle}

import com.varpas.sangeet.core.strings.UiStrings
import com.varpas.sangeet.desktop.diagnostics.{
  BugReportClient,
  BugReportMetadata,
  BugReportPayload,
  DesktopEvent,
  EventLogger,
  NoopPostHogClient,
  PostHogClient,
  ScreenshotCapture
}

/** "Report a bug" modal. Mirrors the web BugReport dialog (textarea for description, optional email field, disclosure
  * paragraph, Send/Cancel buttons). Send gathers the EventLogger snapshot + active composition + a screenshot of the
  * current window, then POSTs via BugReportClient.
  *
  * Designed to be opened from both the toolbar 🐞 button and the Help → Report a Bug menu item. The caller passes the
  * owner Stage (for modality + screenshot source) and a lazy `() => Option[Json]` that returns the active tab's
  * composition JSON — kept lazy because the dialog might outlive a tab swap and we want the snapshot at Send time, not
  * dialog-open time.
  */
object BugReportDialog:

  private val AppVersion = "1.0"

  def show(
      owner: javafx.stage.Stage,
      activeComposition: () => Option[Json],
      client: BugReportClient = BugReportClient.fromEnv,
      analytics: PostHogClient = NoopPostHogClient
  ): Unit =
    val titleLabel = new Label(UiStrings.dialogBugReportTitle):
      style = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #8B1A1A;"

    val descLabel = new Label(UiStrings.dialogBugReportDescriptionLabel):
      style = "-fx-font-size: 12px; -fx-text-fill: #2D2926;"

    val descField = new TextArea:
      promptText = UiStrings.dialogBugReportDescriptionPlaceholder
      prefRowCount = 6
      wrapText = true

    val emailLabel = new Label(UiStrings.dialogBugReportEmailLabel):
      style = "-fx-font-size: 12px; -fx-text-fill: #2D2926; -fx-padding: 8 0 0 0;"

    val emailField = new TextField:
      promptText = UiStrings.dialogBugReportEmailPlaceholder

    val disclosure = new Label(UiStrings.dialogBugReportDisclosureDesktop):
      style = "-fx-font-size: 11px; -fx-text-fill: #6A5A4A; -fx-padding: 12 0 4 0;"
      wrapText = true
      maxWidth = 520

    val statusLabel = new Label(""):
      style = "-fx-font-size: 11px; -fx-text-fill: #5A2828;"
      wrapText = true
      maxWidth = 520

    val sendBtn = new Button(UiStrings.dialogBugReportButtonSend):
      style = "-fx-font-size: 12px; -fx-font-weight: bold;"
      defaultButton = true
      disable = true

    val cancelBtn = new Button(UiStrings.dialogBugReportButtonCancel):
      style = "-fx-font-size: 12px;"

    val buttonRow = new HBox:
      alignment = Pos.CenterRight
      spacing = 8
      padding = Insets(12, 0, 0, 0)
      children = Seq(cancelBtn, sendBtn)

    val rootPane = new VBox:
      spacing = 6
      padding = Insets(20)
      style = "-fx-background-color: #FDF6EC;"
      children = Seq(titleLabel, descLabel, descField, emailLabel, emailField, disclosure, statusLabel, buttonRow)

    val dialogStage = new Stage:
      initStyle(StageStyle.Utility)
      initModality(Modality.WindowModal)
      width = 560
      scene = new Scene:
        root = rootPane
    dialogStage.title = UiStrings.dialogBugReportTitle
    dialogStage.initOwner(owner)

    // Enable Send only when the description is non-empty.
    descField.text.onChange { (_, _, newVal) =>
      sendBtn.disable = Option(newVal).map(_.trim).forall(_.isEmpty)
    }

    cancelBtn.onAction = _ => dialogStage.close()

    sendBtn.onAction = _ =>
      val description = descField.text.value.trim
      val email       = Some(emailField.text.value.trim).filter(_.nonEmpty)

      // Capture the screenshot on the FX thread BEFORE the background submit
      // — Scene.snapshot must run on the FX thread, and we want the visible
      // state at Send time (not 5 seconds later when the network completes).
      val screenshot = ScreenshotCapture.capturePngBase64(owner.getScene) match
        case Right(b) => Some(b)
        case Left(err) =>
          statusLabel.text = UiStrings.dialogBugReportStatusScreenshotFailed(err)
          None

      val screen = Screen.primary
      val payload = BugReportPayload(
        description = description,
        email = email,
        eventLog = EventLogger.snapshot(),
        composition = activeComposition(),
        screenshotPngBase64 = screenshot,
        metadata = BugReportMetadata.current(
          appVersion = AppVersion,
          screenW = screen.bounds.width.toInt,
          screenH = screen.bounds.height.toInt
        )
      )

      sendBtn.disable = true
      cancelBtn.disable = true
      sendBtn.text = UiStrings.dialogBugReportButtonSending
      statusLabel.text = UiStrings.dialogBugReportStatusSending

      // Run the blocking network send off the FX thread so the UI doesn't
      // freeze on slow connections (Cloud Run cold-start can be 10–20s).
      val task = new Task[Either[String, String]]:
        override def call(): Either[String, String] = client.submit(payload)

      task.setOnSucceeded { _ =>
        task.getValue match
          case Right(reportId) =>
            statusLabel.text = UiStrings.dialogBugReportStatusSent(reportId)
            sendBtn.text = UiStrings.dialogBugReportButtonSentSuccess
            analytics.capture(DesktopEvent.BugReportSent)
            // Auto-dismiss after a short delay so the user can see the confirmation.
            val pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1200))
            pause.setOnFinished(_ => dialogStage.close())
            pause.play()
          case Left(err) =>
            statusLabel.text = UiStrings.dialogBugReportStatusSendFailed(err)
            sendBtn.text = UiStrings.dialogBugReportButtonSend
            sendBtn.disable = false
            cancelBtn.disable = false
      }
      task.setOnFailed { _ =>
        val msg = Option(task.getException).map(_.getMessage).getOrElse("unknown")
        statusLabel.text = UiStrings.dialogBugReportStatusSendThrew(msg)
        sendBtn.text = UiStrings.dialogBugReportButtonSend
        sendBtn.disable = false
        cancelBtn.disable = false
      }

      val thread = new Thread(task, "bug-report-submit")
      thread.setDaemon(true)
      thread.start()

    Platform.runLater(() => descField.requestFocus())
    dialogStage.show()
