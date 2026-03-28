package sangeet.editor

import javafx.scene.control.{Dialog, ButtonType, Label, TextField, ComboBox, ButtonBar}
import javafx.scene.layout.GridPane
import javafx.geometry.Insets
import javafx.collections.FXCollections
import sangeet.model.*
import sangeet.taal.Taals

object CompositionPropertiesDialog:

  def show(meta: Metadata): Option[Metadata] =
    val dialog = new Dialog[Metadata]()
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

    dialog.getDialogPane.setContent(grid)
    titleField.requestFocus()

    dialog.setResultConverter(bt =>
      if bt.getButtonData == ButtonBar.ButtonData.OK_DONE then
        val titleText =
          if titleField.getText == null || titleField.getText.trim.isEmpty then "Untitled"
          else titleField.getText.trim

        val newTaal = Taals.byName(taalCombo.getValue).getOrElse(meta.taal)

        meta.copy(
          title = titleText,
          taal = newTaal,
          updatedAt = java.time.Instant.now().toString
        )
      else null
    )

    val result = dialog.showAndWait()
    if result.isPresent && result.get() != null then Some(result.get())
    else None
