package com.varpas.sangeet.desktop.dialog

import io.circe.Json
import javafx.concurrent.Task
import scalafx.scene.control.{Button, Label, TextArea, TextField}
import scalafx.stage.Modality

import com.varpas.sangeet.core.strings.UiStrings
import com.varpas.sangeet.desktop.diagnostics.{
  BugReportClient,
  BugReportMetadata,
  BugReportPayload,
  CrashCapture,
  DesktopEvent,
  NoopPostHogClient,
  PostHogClient
}

/** Surfaces pending crash sentinel files (written by [[CrashCapture]] when a previous run died from an uncaught
  * exception) at app startup. One modal per file; the user picks Send / Discard. Standalone Stage with no owner —
  * called before the main PrimaryStage exists.
  *
  * Send → POST via [[BugReportClient]] with `crashTrigger: true` so the server-side IssueBuilder adds the `crash`
  * label. On success the sentinel file is deleted. On send failure the file remains so the user can retry. Discard →
  * delete immediately, no send.
  */
object CrashRecoveryDialog:

  private val AppVersion = "1.0"

  /** Read every pending sentinel file and show a modal per crash. Blocks startup until the user resolves them all
    * (showAndWait). Safe to call when there are no pending crashes — returns immediately.
    */
  def processPending(
      client: BugReportClient = BugReportClient.fromEnv,
      analytics: PostHogClient = NoopPostHogClient
  ): Unit =
    CrashCapture.pending().foreach { path =>
      CrashCapture.read(path) match
        case Some(crashJson) => showOne(path, crashJson, client, analytics)
        case None            =>
          // Corrupt sentinel — delete it so we don't loop forever on every startup.
          System.err.println(s"[crash-recovery] could not parse $path; deleting")
          CrashCapture.delete(path)
    }

  private def showOne(
      path: java.nio.file.Path,
      crash: Json,
      client: BugReportClient,
      analytics: PostHogClient
  ): Unit =
    val c          = crash.hcursor
    val exception  = c.get[String]("exception").toOption.getOrElse("unknown exception")
    val message    = c.get[String]("message").toOption.getOrElse("")
    val timestamp  = c.get[String]("timestamp").toOption.getOrElse("(unknown time)")
    val stackTrace = c.get[String]("stackTrace").toOption.getOrElse("(no stack trace)")
    val threadName = c.get[String]("threadName").toOption.getOrElse("unknown")

    val titleLabel = new Label(UiStrings.dialogCrashRecoveryTitle):
      style = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #8B1A1A;"

    val explanation = new Label(UiStrings.dialogCrashRecoveryExplanation):
      style = "-fx-font-size: 12px; -fx-text-fill: #2D2926;"
      wrapText = true
      maxWidth = 580

    val summary = new Label(s"$exception: $message"):
      style = "-fx-font-size: 12px; -fx-text-fill: #5A2828; -fx-padding: 8 0 4 0;"
      wrapText = true
      maxWidth = 580

    val meta = new Label(s"Thread: $threadName · When: $timestamp"):
      style = "-fx-font-size: 11px; -fx-text-fill: #6A5A4A;"

    val traceField = new TextArea(stackTrace):
      editable = false
      prefRowCount = 8
      wrapText = false
      style = "-fx-font-family: monospace; -fx-font-size: 10px;"

    val descLabel = new Label(UiStrings.dialogCrashRecoveryDescriptionLabel):
      style = "-fx-font-size: 11px; -fx-text-fill: #2D2926; -fx-padding: 8 0 0 0;"

    val descField = new TextArea:
      promptText = UiStrings.dialogCrashRecoveryDescriptionPlaceholder
      prefRowCount = 3
      wrapText = true

    val emailLabel = new Label(UiStrings.dialogCrashRecoveryEmailLabel):
      style = "-fx-font-size: 11px; -fx-text-fill: #2D2926; -fx-padding: 8 0 0 0;"

    val emailField = new TextField:
      promptText = "you@example.com"

    val statusLabel = new Label(""):
      style = "-fx-font-size: 11px; -fx-text-fill: #5A2828;"
      wrapText = true

    val sendBtn = new Button(UiStrings.dialogCrashRecoveryButtonSend):
      style = "-fx-font-size: 12px; -fx-font-weight: bold;"
      defaultButton = true

    val discardBtn = new Button(UiStrings.dialogCrashRecoveryButtonDiscard):
      style = "-fx-font-size: 12px;"

    val stackTraceHeader = new Label(UiStrings.dialogCrashRecoveryStackTraceLabel):
      style = "-fx-font-size: 11px; -fx-padding: 8 0 0 0;"

    val dialogStage = ModalFrame.build(
      title = UiStrings.dialogCrashRecoveryWindowTitle,
      content = Seq(
        titleLabel,
        explanation,
        summary,
        meta,
        stackTraceHeader,
        traceField,
        descLabel,
        descField,
        emailLabel,
        emailField,
        statusLabel
      ),
      buttons = Seq(discardBtn, sendBtn),
      width = 620,
      buttonRowTopPadding = 12,
      // Modality.None — no owner stage exists at startup. showAndWait still
      // blocks the calling thread until the dialog is closed.
      owner = null,
      modality = Modality.None
    )

    discardBtn.onAction = _ =>
      analytics.capture(DesktopEvent.CrashRecoveryDiscarded)
      CrashCapture.delete(path)
      dialogStage.close()

    sendBtn.onAction = _ =>
      sendBtn.disable = true
      discardBtn.disable = true
      sendBtn.text = UiStrings.dialogCrashRecoveryStatusSending
      statusLabel.text = UiStrings.dialogCrashRecoveryStatusSendingReport

      val description = Some(descField.text.value.trim).filter(_.nonEmpty)
      val email       = Some(emailField.text.value.trim).filter(_.nonEmpty)

      val payload = buildPayload(crash, description, email)

      val task = new Task[Either[String, String]]:
        override def call(): Either[String, String] = client.submit(payload)

      task.setOnSucceeded { _ =>
        task.getValue match
          case Right(reportId) =>
            statusLabel.text = s"Sent. Report id: $reportId"
            analytics.capture(DesktopEvent.CrashRecoverySent)
            CrashCapture.delete(path)
            val pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1200))
            pause.setOnFinished(_ => dialogStage.close())
            pause.play()
          case Left(err) =>
            statusLabel.text = s"Send failed: $err"
            sendBtn.text = UiStrings.dialogCrashRecoveryButtonRetry
            sendBtn.disable = false
            discardBtn.disable = false
      }
      task.setOnFailed { _ =>
        statusLabel.text = s"Send threw: ${Option(task.getException).map(_.getMessage).getOrElse("unknown")}"
        sendBtn.text = UiStrings.dialogCrashRecoveryButtonRetry
        sendBtn.disable = false
        discardBtn.disable = false
      }

      val t = new Thread(task, "crash-report-submit")
      t.setDaemon(true)
      t.start()

    dialogStage.showAndWait()

  /** Build a BugReportPayload from the crash JSON. Description is composed from the exception + user-supplied note; the
    * original stack trace + threadName + eventLog all ride along as nested fields so IssueBuilder can render them.
    */
  private[dialog] def buildPayload(
      crash: Json,
      userDescription: Option[String],
      email: Option[String]
  ): BugReportPayload =
    val c          = crash.hcursor
    val exception  = c.get[String]("exception").toOption.getOrElse("unknown")
    val message    = c.get[String]("message").toOption.getOrElse("")
    val stackTrace = c.get[String]("stackTrace").toOption.getOrElse("")
    val eventLog   = c.downField("eventLogger").as[List[Json]].getOrElse(List.empty)
    val threadName = c.get[String]("threadName").toOption.getOrElse("unknown")

    val desc = userDescription.map(_ + "\n\n").getOrElse("") +
      s"[CRASH] $exception: $message\n\nThread: $threadName\n\n$stackTrace"

    BugReportPayload(
      description = desc,
      email = email,
      eventLog = eventLog,
      composition = None,         // composition state was lost in the crash; can't recover
      screenshotPngBase64 = None, // can't screenshot retroactively
      metadata = BugReportMetadata.current(
        appVersion = AppVersion,
        screenW = 0, // unknown at recovery time; the previous-session screen size isn't on disk
        screenH = 0
      )
    ).withCrashTrigger
