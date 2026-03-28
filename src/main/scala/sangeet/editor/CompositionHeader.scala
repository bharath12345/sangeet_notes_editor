package sangeet.editor

import scalafx.scene.control.Label
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets

import sangeet.model.Metadata

class CompositionHeader extends VBox:
  spacing = 2
  padding = Insets(10, 15, 10, 15)
  style = "-fx-background-color: #f0efe8; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;"

  private val titleLabel = new Label(""):
    style = "-fx-font-size: 16px; -fx-font-weight: bold;"

  private val typeLine = new Label(""):
    style = "-fx-font-size: 12px; -fx-text-fill: #333;"

  private val raagLine = new Label(""):
    style = "-fx-font-size: 13px;"

  private val thaatLine = new Label(""):
    style = "-fx-font-size: 12px; -fx-text-fill: #333;"

  private val arohanLine = new Label(""):
    style = "-fx-font-size: 12px; -fx-text-fill: #333;"

  private val avarohanLine = new Label(""):
    style = "-fx-font-size: 12px; -fx-text-fill: #333;"

  private val vadiLine = new Label(""):
    style = "-fx-font-size: 12px; -fx-text-fill: #333;"

  private val taalLine = new Label(""):
    style = "-fx-font-size: 12px; -fx-text-fill: #333;"

  private val layaLine = new Label(""):
    style = "-fx-font-size: 12px; -fx-text-fill: #333;"

  children = List(titleLabel, typeLine, raagLine, thaatLine, arohanLine, avarohanLine, vadiLine, taalLine, layaLine)

  def update(meta: Metadata): Unit =
    titleLabel.text = meta.title

    typeLine.text = s"Type: ${meta.compositionType}"

    val raagText = s"Raag: ${meta.raag.name}"
    raagLine.text = raagText
    raagLine.visible = meta.raag.name.nonEmpty
    raagLine.managed = meta.raag.name.nonEmpty

    meta.raag.thaat match
      case Some(t) =>
        thaatLine.text = s"Thaat: $t"
        thaatLine.visible = true
        thaatLine.managed = true
      case None =>
        thaatLine.visible = false
        thaatLine.managed = false

    meta.raag.arohana match
      case Some(ar) =>
        arohanLine.text = s"Arohan:   ${ar.mkString(" ")}"
        arohanLine.visible = true
        arohanLine.managed = true
      case None =>
        arohanLine.visible = false
        arohanLine.managed = false

    meta.raag.avarohana match
      case Some(av) =>
        avarohanLine.text = s"Avrohan: ${av.mkString(" ")}"
        avarohanLine.visible = true
        avarohanLine.managed = true
      case None =>
        avarohanLine.visible = false
        avarohanLine.managed = false

    val vadiParts = List(
      meta.raag.vadi.map(v => s"Vadi: $v"),
      meta.raag.samvadi.map(s => s"Samvadi: $s")
    ).flatten.mkString("  |  ")
    vadiLine.text = vadiParts
    vadiLine.visible = vadiParts.nonEmpty
    vadiLine.managed = vadiParts.nonEmpty

    taalLine.text = s"Taal: ${meta.taal.name} (${meta.taal.matras} matras)"

    meta.laya match
      case Some(l) =>
        layaLine.text = s"Laya: ${l.toString}"
        layaLine.visible = true
        layaLine.managed = true
      case None =>
        layaLine.visible = false
        layaLine.managed = false
