package com.varpas.sangeet.desktop.dialog

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, CheckBox, Hyperlink, Label}
import scalafx.scene.layout.{HBox, VBox}

import com.varpas.sangeet.core.config.ConfigStore
import com.varpas.sangeet.core.strings.UiStrings

object AboutDialog:

  private val Version      = "1.0"
  private val RepoUrl      = "https://github.com/bharath12345/sangeet_notes_editor"
  private val WebUrl       = "https://sangeet-editor.in"
  private val ReleasesUrl  = s"$RepoUrl/releases"
  private val LicenseUrl   = s"$RepoUrl/blob/main/LICENSE"
  private val UserGuideUrl = s"$RepoUrl/tree/main/docs/user-guide"

  private def openInBrowser(url: String): Unit =
    try java.awt.Desktop.getDesktop.browse(java.net.URI.create(url))
    catch case _: Exception => ()

  private def link(text: String, url: String): Hyperlink =
    new Hyperlink(text):
      onAction = _ => openInBrowser(url)
      style = "-fx-font-size: 12px;"

  def show(owner: javafx.stage.Stage): Unit =
    val titleLabel = new Label(UiStrings.dialogAboutTitle):
      style = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #8B1A1A;"

    val betaBadge = new Label(UiStrings.toolbarBetaBadge):
      style = "-fx-background-color: #C75A1E; -fx-text-fill: white;" +
        " -fx-padding: 2 8 2 8; -fx-background-radius: 3; -fx-font-size: 10px;" +
        " -fx-font-weight: bold;"

    val versionLabel = new Label(UiStrings.dialogAboutVersion(Version)):
      style = "-fx-font-size: 12px; -fx-text-fill: #5A2828;"

    val subtitle: HBox = new HBox:
      spacing = 8
      alignment = Pos.CenterLeft
      children = Seq(versionLabel, betaBadge)

    val betaNote = new Label(UiStrings.dialogAboutBetaNoteDesktop):
      style = "-fx-font-size: 11px; -fx-text-fill: #6A3E1A; -fx-font-style: italic;"
      wrapText = true
      maxWidth = 420

    val description = new Label(
      UiStrings.dialogAboutDescriptionDesktopLine1 + "\n" +
        UiStrings.dialogAboutDescriptionDesktopLine2
    ):
      style = "-fx-font-size: 13px; -fx-text-fill: #2D2926;"
      wrapText = true
      maxWidth = 420

    val techNote = new Label(UiStrings.dialogAboutTechDesktop):
      style = "-fx-font-size: 11px; -fx-text-fill: #6A5A4A; -fx-font-style: italic;"

    val linksBox = new VBox:
      spacing = 4
      padding = Insets(8, 0, 8, 0)
      children = Seq(
        link(UiStrings.dialogAboutLinksWebVersion("sangeet-editor.in"), WebUrl),
        link(UiStrings.dialogAboutLinksDownloadDesktop, ReleasesUrl),
        link(UiStrings.dialogAboutLinksUserGuideDesktop, UserGuideUrl),
        link(UiStrings.dialogAboutLinksGithub, RepoUrl),
        link(UiStrings.dialogAboutLinksLicense, LicenseUrl)
      )

    val licenseNote = new Label(UiStrings.dialogAboutLicenseDesktop):
      style = "-fx-font-size: 11px; -fx-text-fill: #6A5A4A;"

    // Task 5: re-enable / disable the read-only Yaman sample that loads on startup.
    // Reads the latest persisted value each time the dialog opens so it always reflects
    // the current state. Mutating the checkbox writes back to disk synchronously — small
    // file, no perceptible cost.
    val currentConfig = ConfigStore.load()
    val sampleToggle: CheckBox = new CheckBox(UiStrings.dialogAboutSampleToggle):
      selected = currentConfig.showSampleOnStartup
      style = "-fx-font-size: 11px; -fx-text-fill: #4A3F32; -fx-padding: 4 0 0 0;"
    sampleToggle.selected.onChange { (_, _, newVal) =>
      try ConfigStore.save(currentConfig.copy(showSampleOnStartup = newVal))
      catch case _: Exception => ()
    }

    val privacyNote = new Label(UiStrings.dialogAboutPrivacyDesktop):
      style = "-fx-font-size: 10px; -fx-text-fill: #6A5A4A; -fx-font-style: italic;"
      wrapText = true
      maxWidth = 420

    val closeBtn = new Button(UiStrings.dialogAboutClose):
      style = "-fx-font-size: 12px;"
      defaultButton = true

    val dialogStage = ModalFrame.build(
      title = s"About ${UiStrings.dialogAboutTitle}",
      content = Seq(
        titleLabel,
        subtitle,
        betaNote,
        description,
        techNote,
        linksBox,
        licenseNote,
        privacyNote,
        sampleToggle
      ),
      buttons = Seq(closeBtn),
      width = 470,
      spacing = 4,
      owner = owner
    )
    closeBtn.onAction = _ => dialogStage.close()
    dialogStage.showAndWait()
