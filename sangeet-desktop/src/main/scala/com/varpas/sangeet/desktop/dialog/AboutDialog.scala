package com.varpas.sangeet.desktop.dialog

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, CheckBox, Hyperlink, Label}
import scalafx.scene.layout.{HBox, VBox}
import scalafx.stage.{Modality, Stage, StageStyle}

import com.varpas.sangeet.core.config.ConfigStore

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
    val titleLabel = new Label("Sangeet Notes Editor"):
      style = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #8B1A1A;"

    val betaBadge = new Label("BETA"):
      style = "-fx-background-color: #C75A1E; -fx-text-fill: white;" +
        " -fx-padding: 2 8 2 8; -fx-background-radius: 3; -fx-font-size: 10px;" +
        " -fx-font-weight: bold;"

    val versionLabel = new Label(s"Version $Version"):
      style = "-fx-font-size: 12px; -fx-text-fill: #5A2828;"

    val subtitle: HBox = new HBox:
      spacing = 8
      alignment = Pos.CenterLeft
      children = Seq(versionLabel, betaBadge)

    val betaNote = new Label(
      "Beta release — actively iterating toward v1.0. Expect rough edges; " +
        "please file bugs via the 🐞 button in the toolbar."
    ):
      style = "-fx-font-size: 11px; -fx-text-fill: #6A3E1A; -fx-font-style: italic;"
      wrapText = true
      maxWidth = 420

    val description = new Label(
      "A notation editor for Hindustani classical music in the Bhatkhande style.\n" +
        "Designed primarily for sitar compositions — Gat, Bandish, and Palta."
    ):
      style = "-fx-font-size: 13px; -fx-text-fill: #2D2926;"
      wrapText = true
      maxWidth = 420

    val techNote = new Label("Built with Scala 3 + ScalaFX (desktop) and Elm + Tapir (web)"):
      style = "-fx-font-size: 11px; -fx-text-fill: #6A5A4A; -fx-font-style: italic;"

    val linksBox = new VBox:
      spacing = 4
      padding = Insets(8, 0, 8, 0)
      children = Seq(
        link("Web version: sangeet-editor.in", WebUrl),
        link("Download desktop app", ReleasesUrl),
        link("User guide & documentation", UserGuideUrl),
        link("GitHub repository", RepoUrl),
        link("MIT License", LicenseUrl)
      )

    val licenseNote = new Label("Free and open source. Copyright (c) 2026 Bharadwaj."):
      style = "-fx-font-size: 11px; -fx-text-fill: #6A5A4A;"

    // Task 5: re-enable / disable the read-only Yaman sample that loads on startup.
    // Reads the latest persisted value each time the dialog opens so it always reflects
    // the current state. Mutating the checkbox writes back to disk synchronously — small
    // file, no perceptible cost.
    val currentConfig = ConfigStore.load()
    val sampleToggle: CheckBox = new CheckBox("Show sample composition on startup"):
      selected = currentConfig.showSampleOnStartup
      style = "-fx-font-size: 11px; -fx-text-fill: #4A3F32; -fx-padding: 4 0 0 0;"
    sampleToggle.selected.onChange { (_, _, newVal) =>
      try ConfigStore.save(currentConfig.copy(showSampleOnStartup = newVal))
      catch case _: Exception => ()
    }

    val privacyNote = new Label(
      "Anonymous usage stats (which features get touched, how long sessions are — never the " +
        "content you type) are sent to PostHog so I can prioritise what to build next. Set the " +
        "SANGEET_ANALYTICS_DISABLED=1 environment variable to turn this off."
    ):
      style = "-fx-font-size: 10px; -fx-text-fill: #6A5A4A; -fx-font-style: italic;"
      wrapText = true
      maxWidth = 420

    val closeBtn = new Button("Close"):
      style = "-fx-font-size: 12px;"
      defaultButton = true

    val buttonRow = new HBox:
      alignment = Pos.CenterRight
      spacing = 8
      padding = Insets(8, 0, 0, 0)
      children = Seq(closeBtn)

    val rootPane = new VBox:
      spacing = 4
      padding = Insets(20)
      style = "-fx-background-color: #FDF6EC;"
      children = Seq(
        titleLabel,
        subtitle,
        betaNote,
        description,
        techNote,
        linksBox,
        licenseNote,
        privacyNote,
        sampleToggle,
        buttonRow
      )

    val dialogStage = new Stage:
      initStyle(StageStyle.Utility)
      initModality(Modality.WindowModal)
      width = 470
      scene = new Scene:
        root = rootPane
    dialogStage.title = "About Sangeet Notes Editor"
    dialogStage.initOwner(owner)
    closeBtn.onAction = _ => dialogStage.close()
    dialogStage.showAndWait()
