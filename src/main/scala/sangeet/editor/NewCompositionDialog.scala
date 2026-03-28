package sangeet.editor

import javafx.scene.control.{Dialog, ButtonType, Label, TextField, ComboBox, ButtonBar}
import javafx.scene.layout.GridPane
import javafx.geometry.Insets
import javafx.collections.FXCollections
import javafx.collections.transformation.FilteredList
import sangeet.model.*
import sangeet.taal.Taals
import sangeet.raag.Raags
import sangeet.render.ScriptMap

object NewCompositionDialog:

  case class Result(
    title: String,
    compositionType: CompositionType,
    raag: Raag,
    taalName: String,
    laya: Option[Laya],
    script: SwarScript
  )

  def show(): Option[Result] =
    val dialog = new Dialog[Result]()
    dialog.setTitle("New Composition")
    dialog.setHeaderText("Create a new composition")
    dialog.getDialogPane.getButtonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

    val titleField = new TextField()
    titleField.setPromptText("e.g. Yaman Vilambit Gat")
    titleField.setPrefColumnCount(25)

    val typeCombo = new ComboBox[String]()
    typeCombo.setItems(FXCollections.observableArrayList("Gat", "Bandish", "Palta"))
    typeCombo.setValue("Gat")

    // Editable combo with filtering for raag selection
    val allRaagNames = Raags.all.values.toList.sortBy(_.name).map(_.name)
    val raagCombo = new ComboBox[String]()
    raagCombo.setItems(FXCollections.observableArrayList(allRaagNames*))
    raagCombo.setEditable(true)
    raagCombo.setPromptText("Type to search or enter custom raag")
    raagCombo.setPrefWidth(250)

    val thaatField = new TextField()
    thaatField.setPromptText("auto-detected or enter manually")
    thaatField.setPrefColumnCount(25)

    val arohanField = new TextField()
    arohanField.setPromptText("auto-detected or enter manually")
    arohanField.setPrefColumnCount(25)

    val avarohanField = new TextField()
    avarohanField.setPromptText("auto-detected or enter manually")
    avarohanField.setPrefColumnCount(25)

    val vadiField = new TextField()
    vadiField.setPromptText("auto-detected")

    val samvadiField = new TextField()
    samvadiField.setPromptText("auto-detected")

    val detectedLabel = new Label("")
    detectedLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 11px;")

    def fillRaagDetails(name: String): Unit =
      if name != null && name.trim.nonEmpty then
        Raags.byName(name) match
          case Some(raag) =>
            detectedLabel.setText(s"✓ Raag ${raag.name} recognized")
            thaatField.setText(raag.thaat.getOrElse(""))
            arohanField.setText(raag.arohana.map(_.mkString(" ")).getOrElse(""))
            avarohanField.setText(raag.avarohana.map(_.mkString(" ")).getOrElse(""))
            vadiField.setText(raag.vadi.getOrElse(""))
            samvadiField.setText(raag.samvadi.getOrElse(""))
          case None =>
            detectedLabel.setText("(raag not in database — enter details manually)")
      else
        detectedLabel.setText("")

    // Guard to prevent feedback loops
    var updatingFromCode = false

    // Filter list and auto-detect as user types
    raagCombo.getEditor.textProperty().addListener { (_, _, newVal) =>
      if !updatingFromCode then
        updatingFromCode = true
        try
          val filter = if newVal == null then "" else newVal.trim.toLowerCase
          if filter.nonEmpty then
            val filtered = allRaagNames.filter(_.toLowerCase.contains(filter))
            raagCombo.getItems.setAll(FXCollections.observableArrayList(filtered*))
            if filtered.nonEmpty then raagCombo.show()
          else
            raagCombo.getItems.setAll(FXCollections.observableArrayList(allRaagNames*))
          fillRaagDetails(newVal)
        finally
          updatingFromCode = false
    }

    // Auto-fill when user selects from dropdown
    raagCombo.setOnAction(_ =>
      if !updatingFromCode then
        updatingFromCode = true
        try
          val selected = raagCombo.getValue
          if selected != null then
            // Restore full list after selection so dropdown works next time
            raagCombo.getItems.setAll(FXCollections.observableArrayList(allRaagNames*))
            fillRaagDetails(selected)
        finally
          updatingFromCode = false
    )

    val taalNames = Taals.all.keys.toList.sorted.map(_.capitalize)
    val taalCombo = new ComboBox[String]()
    taalCombo.setItems(FXCollections.observableArrayList(taalNames*))
    taalCombo.setValue("Teentaal")

    val layaCombo = new ComboBox[String]()
    layaCombo.setItems(FXCollections.observableArrayList(
      "(none)", "Ati-Vilambit", "Vilambit", "Madhya", "Drut", "Ati-Drut"))
    layaCombo.setValue("(none)")

    val scriptCombo = new ComboBox[String]()
    scriptCombo.setItems(FXCollections.observableArrayList(
      "Devanagari (Hindi)", "Kannada", "Telugu", "English"))
    scriptCombo.setValue("Devanagari (Hindi)")

    val grid = new GridPane()
    grid.setHgap(10)
    grid.setVgap(8)
    grid.setPadding(new Insets(20))

    grid.add(new Label("Title:"), 0, 0)
    grid.add(titleField, 1, 0)
    grid.add(new Label("Type:"), 0, 1)
    grid.add(typeCombo, 1, 1)
    grid.add(new Label("Raag:"), 0, 2)
    grid.add(raagCombo, 1, 2)
    grid.add(detectedLabel, 1, 3)
    grid.add(new Label("Thaat:"), 0, 4)
    grid.add(thaatField, 1, 4)
    grid.add(new Label("Arohan:"), 0, 5)
    grid.add(arohanField, 1, 5)
    grid.add(new Label("Avrohan:"), 0, 6)
    grid.add(avarohanField, 1, 6)
    grid.add(new Label("Vadi:"), 0, 7)
    grid.add(vadiField, 1, 7)
    grid.add(new Label("Samvadi:"), 0, 8)
    grid.add(samvadiField, 1, 8)
    grid.add(new Label("Taal:"), 0, 9)
    grid.add(taalCombo, 1, 9)
    grid.add(new Label("Laya:"), 0, 10)
    grid.add(layaCombo, 1, 10)
    grid.add(new Label("Script:"), 0, 11)
    grid.add(scriptCombo, 1, 11)

    dialog.getDialogPane.setContent(grid)
    titleField.requestFocus()

    dialog.setResultConverter(bt =>
      if bt.getButtonData == ButtonBar.ButtonData.OK_DONE then
        val compType = typeCombo.getValue match
          case "Bandish" => CompositionType.Bandish
          case "Palta"   => CompositionType.Palta
          case _         => CompositionType.Gat

        val laya = layaCombo.getValue match
          case "Ati-Vilambit" => Some(Laya.AtiVilambit)
          case "Vilambit"     => Some(Laya.Vilambit)
          case "Madhya"       => Some(Laya.Madhya)
          case "Drut"         => Some(Laya.Drut)
          case "Ati-Drut"     => Some(Laya.AtiDrut)
          case _              => None

        val titleText = if titleField.getText == null || titleField.getText.trim.isEmpty
                        then "Untitled" else titleField.getText.trim

        // Get raag name from editor text (works for both selected and custom)
        val raagName = Option(raagCombo.getEditor.getText).map(_.trim).getOrElse("")

        def parseList(s: String): Option[List[String]] =
          if s == null then None
          else
            val parts = s.trim.split("\\s+").toList.filter(_.nonEmpty)
            if parts.isEmpty then None else Some(parts)

        def opt(s: String): Option[String] =
          Option(s).map(_.trim).filter(_.nonEmpty)

        val raag = Raag(
          name = raagName,
          thaat = opt(thaatField.getText),
          arohana = parseList(arohanField.getText),
          avarohana = parseList(avarohanField.getText),
          vadi = opt(vadiField.getText),
          samvadi = opt(samvadiField.getText),
          pakad = Raags.byName(raagName).flatMap(_.pakad),
          prahar = Raags.byName(raagName).flatMap(_.prahar)
        )

        val script = scriptCombo.getValue match
          case "Kannada"  => SwarScript.Kannada
          case "Telugu"   => SwarScript.Telugu
          case "English"  => SwarScript.English
          case _          => SwarScript.Devanagari

        Result(
          title = titleText,
          compositionType = compType,
          raag = raag,
          taalName = if taalCombo.getValue != null then taalCombo.getValue.toLowerCase else "teentaal",
          laya = laya,
          script = script
        )
      else null
    )

    val result = dialog.showAndWait()
    if result.isPresent && result.get() != null then Some(result.get())
    else None
