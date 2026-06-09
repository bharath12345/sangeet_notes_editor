package com.varpas.sangeet.desktop.dialog

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.control.{ButtonBar, ButtonType, ComboBox, Dialog, Label, TextField}
import javafx.scene.layout.GridPane

import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.taal.Taals

object CompositionPropertiesDialog:

  case class Result(
      metadata: Metadata,
      sectionStartingBeats: Map[Int, Int]
  )

  def show(
      meta: Metadata,
      sections: List[Section],
      owner: javafx.stage.Window = null
  ): Option[Result] =
    val dialog = new Dialog[Result]()
    if owner != null then dialog.initOwner(owner)
    dialog.setTitle("Composition Properties")
    dialog.setHeaderText("Edit composition details")
    dialog.getDialogPane.getButtonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

    val titleField = new TextField(meta.title)
    titleField.setPrefColumnCount(25)

    val taalNames = Taals.all.keys.toList.sorted.map(_.capitalize)
    val taalCombo = new ComboBox[String]()
    taalCombo.setItems(FXCollections.observableArrayList(taalNames*))
    taalCombo.setValue(meta.taal.name.capitalize)

    val grid = new GridPane()
    grid.setHgap(10)
    grid.setVgap(8)
    grid.setPadding(new Insets(20))

    grid.add(new Label("Title:"), 0, 0)
    grid.add(titleField, 1, 0)

    val typeLabel = new Label(meta.compositionType.toString)
    typeLabel.setStyle("-fx-text-fill: #555;")
    grid.add(new Label("Type:"), 0, 1)
    grid.add(typeLabel, 1, 1)

    val raagLabel = new Label(meta.raag.name)
    raagLabel.setStyle("-fx-text-fill: #555;")
    grid.add(new Label("Raag:"), 0, 2)
    grid.add(raagLabel, 1, 2)

    grid.add(new Label("Taal:"), 0, 3)
    grid.add(taalCombo, 1, 3)

    val errorLabel = new Label("")
    errorLabel.setStyle("-fx-text-fill: #c62828; -fx-font-size: 11px;")

    val isGatOrBandish =
      meta.compositionType == CompositionType.Gat || meta.compositionType == CompositionType.Bandish

    case class StartBeatEntry(
        label: Label,
        spinner: javafx.scene.control.Spinner[Integer],
        sectionIndices: List[Int]
    )

    val startBeatEntries: List[StartBeatEntry] =
      if !isGatOrBandish then Nil
      else
        val matras = meta.taal.matras

        val mainIndices = sections.zipWithIndex.collect {
          case (s, i)
              if s.sectionType == SectionType.Sthayi ||
                s.sectionType == SectionType.Custom("Gat") =>
            i
        }
        val antaraIndices = sections.zipWithIndex.collect {
          case (s, i) if s.sectionType == SectionType.Antara => i
        }
        val taanIndices = sections.zipWithIndex.collect {
          case (s, i) if s.sectionType == SectionType.Taan => i
        }

        val mainLabel =
          if meta.compositionType == CompositionType.Bandish then "Sthayi Starting Beat:"
          else "Gat Starting Beat:"

        val mainBeat = mainIndices.headOption.flatMap(i => sections.lift(i)).map(_.startingBeat).getOrElse(1)
        val antaraBeat =
          antaraIndices.headOption.flatMap(i => sections.lift(i)).map(_.startingBeat).getOrElse(1)
        val taanBeat = taanIndices.headOption.flatMap(i => sections.lift(i)).map(_.startingBeat).getOrElse(1)

        val entries = scala.collection.mutable.ListBuffer[StartBeatEntry]()

        if mainIndices.nonEmpty then
          val lbl     = new Label(mainLabel)
          val spinner = new javafx.scene.control.Spinner[Integer](1, matras, mainBeat)
          spinner.setEditable(true)
          spinner.setPrefWidth(80)
          entries += StartBeatEntry(lbl, spinner, mainIndices)

        if antaraIndices.nonEmpty then
          val lbl     = new Label("Antara Starting Beat:")
          val spinner = new javafx.scene.control.Spinner[Integer](1, matras, antaraBeat)
          spinner.setEditable(true)
          spinner.setPrefWidth(80)
          entries += StartBeatEntry(lbl, spinner, antaraIndices)

        if taanIndices.nonEmpty then
          val lbl     = new Label("Taan Starting Beat:")
          val spinner = new javafx.scene.control.Spinner[Integer](1, matras, taanBeat)
          spinner.setEditable(true)
          spinner.setPrefWidth(80)
          entries += StartBeatEntry(lbl, spinner, taanIndices)

        entries.toList

    var nextRow = 4
    startBeatEntries.foreach { entry =>
      grid.add(entry.label, 0, nextRow)
      grid.add(entry.spinner, 1, nextRow)
      nextRow += 1
    }
    grid.add(errorLabel, 0, nextRow, 2, 1)

    def updateStartingBeatRange(): Unit =
      val taalKey  = Option(taalCombo.getValue).map(_.toLowerCase).getOrElse("teentaal")
      val matras   = Taals.all.get(taalKey).map(_.matras).getOrElse(16)
      var hasError = false
      startBeatEntries.foreach { entry =>
        val current = entry.spinner.getValue.intValue
        if current > matras then hasError = true
        val clamped = math.min(current, matras)
        entry.spinner.setValueFactory(
          new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(1, matras, clamped)
        )
      }
      if hasError then errorLabel.setText(s"Starting beats clamped to new taal range (1-$matras)")
      else errorLabel.setText("")

    taalCombo.setOnAction(_ => updateStartingBeatRange())

    dialog.getDialogPane.setContent(grid)
    titleField.requestFocus()

    dialog.setResultConverter(bt =>
      if bt.getButtonData == ButtonBar.ButtonData.OK_DONE then
        val titleText =
          if titleField.getText == null || titleField.getText.trim.isEmpty then "Untitled"
          else titleField.getText.trim

        val newTaal = Taals.byName(taalCombo.getValue).getOrElse(meta.taal)

        val newMeta = meta.copy(
          title = titleText,
          taal = newTaal,
          updatedAt = java.time.Instant.now().toString
        )

        val beatMap: Map[Int, Int] = startBeatEntries.flatMap { entry =>
          val beat = entry.spinner.getValue.intValue
          entry.sectionIndices.map(idx => idx -> beat)
        }.toMap

        Result(newMeta, beatMap)
      else null
    )

    val result = dialog.showAndWait()
    if result.isPresent && result.get() != null then Some(result.get())
    else None
