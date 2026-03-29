package sangeet.editor

import scalafx.scene.control.Label
import scalafx.scene.layout.{VBox, HBox, FlowPane, Region, Priority}
import scalafx.geometry.{Insets, Orientation}

import sangeet.model.Metadata

class CompositionHeader extends VBox:
  spacing = 0
  padding = Insets(8, 15, 8, 15)
  style = "-fx-background-color: #f0efe8; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;"

  private val titleLabel = new Label(""):
    style = "-fx-font-size: 15px; -fx-font-weight: bold;"

  private val typeBadge = new Label(""):
    style = "-fx-font-size: 10px; -fx-text-fill: white; -fx-background-color: #5c6bc0; " +
            "-fx-padding: 1 6 1 6; -fx-background-radius: 3;"

  /** Build a small key–value chip label */
  private def chip(key: String, value: String): Label =
    new Label(s"$key: $value"):
      style = "-fx-font-size: 11px; -fx-text-fill: #444; -fx-padding: 0 8 0 0;"

  /** Thin vertical separator */
  private def sep: Label =
    new Label("·"):
      style = "-fx-font-size: 11px; -fx-text-fill: #aaa; -fx-padding: 0 4 0 4;"

  // Top row: title + type badge
  private val titleRow = new HBox:
    spacing = 8
    alignment = scalafx.geometry.Pos.CenterLeft
    children = List(titleLabel, typeBadge)

  // Detail row: raag, thaat, taal, laya, vadi/samvadi in a flowing line
  private val detailFlow = new FlowPane:
    orientation = Orientation.Horizontal
    hgap = 0
    vgap = 2
    padding = Insets(2, 0, 0, 0)

  // Arohan/Avrohan row (only if present — can be slightly longer)
  private val scaleFlow = new FlowPane:
    orientation = Orientation.Horizontal
    hgap = 0
    vgap = 2
    padding = Insets(1, 0, 0, 0)

  children = List(titleRow, detailFlow, scaleFlow)

  def update(meta: Metadata): Unit =
    titleLabel.text = meta.title

    val typeText = meta.compositionType.toString
    typeBadge.text = typeText

    // Build detail chips
    val details = List.newBuilder[javafx.scene.Node]

    details += chip("Raag", meta.raag.name).delegate

    meta.raag.thaat.foreach { t =>
      details += sep.delegate
      details += chip("Thaat", t).delegate
    }

    details += sep.delegate
    details += chip("Taal", s"${meta.taal.name} (${meta.taal.matras})").delegate

    meta.laya.foreach { l =>
      details += sep.delegate
      details += chip("Laya", l.toString).delegate
    }

    meta.raag.vadi.foreach { v =>
      details += sep.delegate
      details += chip("Vadi", v).delegate
    }

    meta.raag.samvadi.foreach { s =>
      details += sep.delegate
      details += chip("Samvadi", s).delegate
    }

    detailFlow.children.clear()
    detailFlow.children.addAll(details.result()*)

    // Arohan / Avrohan on a second compact line
    val scales = List.newBuilder[javafx.scene.Node]
    meta.raag.arohana.foreach { ar =>
      scales += chip("Arohan", ar.mkString(" ")).delegate
    }
    meta.raag.avarohana.foreach { av =>
      if meta.raag.arohana.isDefined then scales += sep.delegate
      scales += chip("Avrohan", av.mkString(" ")).delegate
    }

    val scaleItems = scales.result()
    scaleFlow.children.clear()
    if scaleItems.nonEmpty then
      scaleFlow.children.addAll(scaleItems*)
      scaleFlow.visible = true
      scaleFlow.managed = true
    else
      scaleFlow.visible = false
      scaleFlow.managed = false
