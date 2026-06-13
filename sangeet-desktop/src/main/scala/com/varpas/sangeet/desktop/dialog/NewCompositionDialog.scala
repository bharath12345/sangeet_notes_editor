package com.varpas.sangeet.desktop.dialog

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.control.{ButtonBar, ButtonType, ComboBox, Dialog, Label, TextField}
import javafx.scene.layout.GridPane

import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.strings.UiStrings
import com.varpas.sangeet.core.taal.Taals

object NewCompositionDialog:

  case class Result(
      title: String,
      compositionType: CompositionType,
      raag: Raag,
      taalName: String,
      laya: Option[Laya],
      script: SwarScript,
      taanCount: Int = 0,
      showStrokeLine: Boolean = false,
      showSahityaLine: Boolean = false,
      filePath: java.nio.file.Path,
      gatStartingBeat: Int = 1,
      antaraStartingBeat: Int = 1,
      taanStartingBeat: Int = 1
  )

  /** Field visibility rules per composition type. Returns (showLaya, showTaanCount, showStrokeOption,
    * showSahityaOption)
    */
  def fieldVisibility(compType: CompositionType): (Boolean, Boolean, Boolean, Boolean) =
    compType match
      case CompositionType.Palta     => (false, false, true, false)
      case CompositionType.Sargam    => (false, false, true, false)
      case CompositionType.Gat       => (true, true, true, true)
      case CompositionType.Bandish   => (true, false, true, true)
      case CompositionType.Custom(_) => (true, false, true, true)

  def show(owner: javafx.stage.Window = null): Option[Result] =
    val dialog = new Dialog[Result]()
    if owner != null then dialog.initOwner(owner)
    dialog.setTitle(UiStrings.dialogNewCompositionTitle)
    dialog.setHeaderText(UiStrings.dialogNewCompositionHeader)
    dialog.getDialogPane.getButtonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

    val titleField = new TextField()
    titleField.setPromptText(UiStrings.dialogNewCompositionFieldTitlePlaceholderDesktop)
    titleField.setPrefColumnCount(25)

    val typeCombo = new ComboBox[String]()
    typeCombo.setItems(
      FXCollections.observableArrayList(
        UiStrings.dialogNewCompositionFieldTypeGatDesktop,
        UiStrings.dialogNewCompositionFieldTypeBandishDesktop,
        UiStrings.dialogNewCompositionFieldTypePaltaDesktop,
        UiStrings.dialogNewCompositionFieldTypeSargamDesktop
      )
    )
    typeCombo.setValue(UiStrings.dialogNewCompositionFieldTypeGatDesktop)

    // Editable combo with filtering for raag selection
    val allRaagNames = Raags.all.values.toList.sortBy(_.name).map(_.name)
    val raagCombo    = new ComboBox[String]()
    raagCombo.setItems(FXCollections.observableArrayList(allRaagNames*))
    raagCombo.setEditable(true)
    raagCombo.setPromptText(UiStrings.dialogNewCompositionFieldRaagPlaceholder)
    raagCombo.setPrefWidth(250)

    val layaLabel = new Label(UiStrings.dialogNewCompositionFieldLayaLabelDesktop)
    val layaCombo = new ComboBox[String]()
    layaCombo.setItems(
      FXCollections.observableArrayList(
        UiStrings.dialogNewCompositionFieldLayaNoneDesktop,
        UiStrings.dialogNewCompositionFieldLayaAtivilambitDesktop,
        UiStrings.dialogNewCompositionFieldLayaVilambit,
        UiStrings.dialogNewCompositionFieldLayaMadhya,
        UiStrings.dialogNewCompositionFieldLayaDrut,
        UiStrings.dialogNewCompositionFieldLayaAtidrutDesktop
      )
    )
    layaCombo.setValue(UiStrings.dialogNewCompositionFieldLayaNoneDesktop)

    val taanLabel   = new Label(UiStrings.dialogNewCompositionFieldTaanCountLabelDesktop)
    val taanSpinner = new javafx.scene.control.Spinner[Integer](0, 50, 5)
    taanSpinner.setEditable(true)
    taanSpinner.setPrefWidth(80)

    val filePathField = new TextField()
    filePathField.setPromptText(UiStrings.dialogNewCompositionFieldFilePathPlaceholder)
    filePathField.setPrefColumnCount(25)
    val browseButton = new javafx.scene.control.Button(UiStrings.dialogNewCompositionFieldFilePathBrowseButton)
    browseButton.setOnAction(_ =>
      val fc = new javafx.stage.FileChooser()
      fc.setTitle(UiStrings.dialogNewCompositionFieldFilePathBrowserTitle)
      fc.getExtensionFilters.add(new javafx.stage.FileChooser.ExtensionFilter("Swar Files", "*.swar"))
      // Default filename from title
      val titleText = Option(titleField.getText).map(_.trim).getOrElse("")
      if titleText.nonEmpty then fc.setInitialFileName(titleText.replaceAll("[^a-zA-Z0-9_-]", "_"))
      val file = fc.showSaveDialog(dialog.getOwner)
      if file != null then
        val path = if file.getName.endsWith(".swar") then file.getPath else file.getPath + ".swar"
        filePathField.setText(path)
    )
    val filePathBox = new javafx.scene.layout.HBox(8, filePathField, browseButton)

    val strokeCheckLabel = new Label(UiStrings.dialogNewCompositionFieldShowStrokesLabelDesktop)
    val strokeCheck = new javafx.scene.control.CheckBox(UiStrings.dialogNewCompositionFieldShowStrokesCheckboxDesktop)
    val sahityaCheckLabel = new Label(UiStrings.dialogNewCompositionFieldShowSahityaLabelDesktop)
    val sahityaCheck = new javafx.scene.control.CheckBox(UiStrings.dialogNewCompositionFieldShowSahityaCheckboxDesktop)

    val thaatField = new TextField()
    thaatField.setPromptText(UiStrings.dialogNewCompositionFieldThaatPlaceholder)
    thaatField.setPrefColumnCount(25)

    val arohanField = new TextField()
    arohanField.setPromptText(UiStrings.dialogNewCompositionFieldArohanPlaceholder)
    arohanField.setPrefColumnCount(25)

    val avarohanField = new TextField()
    avarohanField.setPromptText(UiStrings.dialogNewCompositionFieldAvrohanPlaceholder)
    avarohanField.setPrefColumnCount(25)

    val vadiField = new TextField()
    vadiField.setPromptText(UiStrings.dialogNewCompositionFieldVadiPlaceholder)

    val samvadiField = new TextField()
    samvadiField.setPromptText(UiStrings.dialogNewCompositionFieldSamvadiPlaceholder)

    val detectedLabel = new Label("")
    detectedLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 11px;")

    val errorLabel = new Label("")
    errorLabel.setStyle("-fx-text-fill: #c62828; -fx-font-size: 11px;")

    val taalNames = Taals.all.keys.toList.sorted.map(_.capitalize)
    val taalCombo = new ComboBox[String]()
    taalCombo.setItems(FXCollections.observableArrayList(taalNames*))
    taalCombo.setValue("Teentaal")

    val scriptCombo = new ComboBox[String]()
    scriptCombo.setItems(FXCollections.observableArrayList("Devanagari (Hindi)", "Kannada", "Telugu", "English"))
    scriptCombo.setValue("Devanagari (Hindi)")

    def taalMatras: Int =
      val taalKey = Option(taalCombo.getValue).map(_.toLowerCase).getOrElse("teentaal")
      Taals.all.get(taalKey).map(_.matras).getOrElse(16)

    val gatStartLabel = new Label(UiStrings.dialogNewCompositionFieldGatStartingBeatLabelDesktop)
    val gatStartSpinner =
      new javafx.scene.control.Spinner[Integer](1, taalMatras, 1)
    gatStartSpinner.setEditable(true)
    gatStartSpinner.setPrefWidth(80)

    val antaraStartLabel = new Label(UiStrings.dialogNewCompositionFieldAntaraStartingBeatLabelDesktop)
    val antaraStartSpinner =
      new javafx.scene.control.Spinner[Integer](1, taalMatras, 1)
    antaraStartSpinner.setEditable(true)
    antaraStartSpinner.setPrefWidth(80)

    val taanStartLabel = new Label(UiStrings.dialogNewCompositionFieldTaanStartingBeatLabelDesktop)
    val taanStartSpinner =
      new javafx.scene.control.Spinner[Integer](1, taalMatras, 1)
    taanStartSpinner.setEditable(true)
    taanStartSpinner.setPrefWidth(80)

    def updateStartingBeatRange(): Unit =
      val matras = taalMatras
      def updateSpinner(spinner: javafx.scene.control.Spinner[Integer]): Unit =
        val current = spinner.getValue.intValue
        val clamped = math.min(current, matras)
        spinner.setValueFactory(
          new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(1, matras, clamped)
        )
      updateSpinner(gatStartSpinner)
      updateSpinner(antaraStartSpinner)
      updateSpinner(taanStartSpinner)

    taalCombo.setOnAction(_ => updateStartingBeatRange())

    def fillRaagDetails(name: String): Unit =
      if name != null && name.trim.nonEmpty then
        Raags.byName(name) match
          case Some(raag) =>
            detectedLabel.setText(UiStrings.dialogNewCompositionRaagDetected(raag.name))
            thaatField.setText(raag.thaat.getOrElse(""))
            arohanField.setText(raag.arohana.map(_.mkString(" ")).getOrElse(""))
            avarohanField.setText(raag.avarohana.map(_.mkString(" ")).getOrElse(""))
            vadiField.setText(raag.vadi.getOrElse(""))
            samvadiField.setText(raag.samvadi.getOrElse(""))
          case None =>
            detectedLabel.setText(UiStrings.dialogNewCompositionRaagNotFound)
      else detectedLabel.setText("")

    // Guard to prevent feedback loops
    var updatingFromCode = false
    // Track the confirmed raag name (set on selection or after typing)
    var confirmedRaagName = ""

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
          else raagCombo.getItems.setAll(FXCollections.observableArrayList(allRaagNames*))
          confirmedRaagName = if newVal == null then "" else newVal.trim
          fillRaagDetails(newVal)
        finally updatingFromCode = false
    }

    // Auto-fill when user selects from dropdown
    raagCombo.setOnAction(_ =>
      if !updatingFromCode then
        updatingFromCode = true
        try
          val selected = raagCombo.getValue
          if selected != null && selected.trim.nonEmpty then
            confirmedRaagName = selected.trim
            fillRaagDetails(selected)
            // Restore editor text and full list on next UI tick
            javafx.application.Platform.runLater { () =>
              updatingFromCode = true
              try
                raagCombo.getEditor.setText(confirmedRaagName)
                raagCombo.getItems.setAll(FXCollections.observableArrayList(allRaagNames*))
              finally updatingFromCode = false
            }
        finally updatingFromCode = false
    )

    def updateVisibility(): Unit =
      val selected = typeCombo.getValue
      val compType =
        if selected == UiStrings.dialogNewCompositionFieldTypeGatDesktop then CompositionType.Gat
        else if selected == UiStrings.dialogNewCompositionFieldTypeBandishDesktop then CompositionType.Bandish
        else if selected == UiStrings.dialogNewCompositionFieldTypePaltaDesktop then CompositionType.Palta
        else if selected == UiStrings.dialogNewCompositionFieldTypeSargamDesktop then CompositionType.Sargam
        else CompositionType.Custom(selected)
      val (showLaya, showTaan, _, showSahitya) = fieldVisibility(compType)
      layaLabel.setVisible(showLaya)
      layaLabel.setManaged(showLaya)
      layaCombo.setVisible(showLaya)
      layaCombo.setManaged(showLaya)
      if !showLaya then layaCombo.setValue(UiStrings.dialogNewCompositionFieldLayaNoneDesktop)
      taanLabel.setVisible(showTaan)
      taanLabel.setManaged(showTaan)
      taanSpinner.setVisible(showTaan)
      taanSpinner.setManaged(showTaan)
      sahityaCheckLabel.setVisible(showSahitya)
      sahityaCheckLabel.setManaged(showSahitya)
      sahityaCheck.setVisible(showSahitya)
      sahityaCheck.setManaged(showSahitya)
      if !showSahitya then sahityaCheck.setSelected(false)

      val showStartBeats = compType == CompositionType.Gat || compType == CompositionType.Bandish
      val showTaanStart  = compType == CompositionType.Gat
      gatStartLabel.setText(
        if compType == CompositionType.Bandish then UiStrings.dialogNewCompositionFieldSthayiStartingBeatLabelDesktop
        else UiStrings.dialogNewCompositionFieldGatStartingBeatLabelDesktop
      )
      gatStartLabel.setVisible(showStartBeats)
      gatStartLabel.setManaged(showStartBeats)
      gatStartSpinner.setVisible(showStartBeats)
      gatStartSpinner.setManaged(showStartBeats)
      antaraStartLabel.setVisible(showStartBeats)
      antaraStartLabel.setManaged(showStartBeats)
      antaraStartSpinner.setVisible(showStartBeats)
      antaraStartSpinner.setManaged(showStartBeats)
      taanStartLabel.setVisible(showTaanStart)
      taanStartLabel.setManaged(showTaanStart)
      taanStartSpinner.setVisible(showTaanStart)
      taanStartSpinner.setManaged(showTaanStart)
      errorLabel.setText("")

    typeCombo.setOnAction(_ => updateVisibility())

    val grid = new GridPane()
    grid.setHgap(10)
    grid.setVgap(8)
    grid.setPadding(new Insets(20))

    grid.add(new Label(UiStrings.dialogNewCompositionFieldTitleLabelDesktop), 0, 0)
    grid.add(titleField, 1, 0)
    grid.add(new Label(UiStrings.dialogNewCompositionFieldFilePathLabel), 0, 1)
    grid.add(filePathBox, 1, 1)
    grid.add(new Label(UiStrings.dialogNewCompositionFieldTypeLabelDesktop), 0, 2)
    grid.add(typeCombo, 1, 2)
    grid.add(new Label(UiStrings.dialogNewCompositionFieldRaagLabelDesktop), 0, 3)
    grid.add(raagCombo, 1, 3)
    grid.add(detectedLabel, 1, 4)
    grid.add(layaLabel, 0, 5)
    grid.add(layaCombo, 1, 5)
    grid.add(taanLabel, 0, 6)
    grid.add(taanSpinner, 1, 6)
    grid.add(strokeCheckLabel, 0, 7)
    grid.add(strokeCheck, 1, 7)
    grid.add(sahityaCheckLabel, 0, 8)
    grid.add(sahityaCheck, 1, 8)
    grid.add(new Label(UiStrings.dialogNewCompositionFieldTaalLabelDesktop), 0, 9)
    grid.add(taalCombo, 1, 9)
    grid.add(gatStartLabel, 0, 10)
    grid.add(gatStartSpinner, 1, 10)
    grid.add(antaraStartLabel, 0, 11)
    grid.add(antaraStartSpinner, 1, 11)
    grid.add(taanStartLabel, 0, 12)
    grid.add(taanStartSpinner, 1, 12)
    grid.add(new Label(UiStrings.dialogNewCompositionFieldThaatLabel), 0, 13)
    grid.add(thaatField, 1, 13)
    grid.add(new Label(UiStrings.dialogNewCompositionFieldArohanLabel), 0, 14)
    grid.add(arohanField, 1, 14)
    grid.add(new Label(UiStrings.dialogNewCompositionFieldAvrohanLabel), 0, 15)
    grid.add(avarohanField, 1, 15)
    grid.add(new Label(UiStrings.dialogNewCompositionFieldVadiLabel), 0, 16)
    grid.add(vadiField, 1, 16)
    grid.add(new Label(UiStrings.dialogNewCompositionFieldSamvadiLabel), 0, 17)
    grid.add(samvadiField, 1, 17)
    grid.add(new Label(UiStrings.dialogNewCompositionFieldScriptLabel), 0, 18)
    grid.add(scriptCombo, 1, 18)
    grid.add(errorLabel, 0, 19, 2, 1)

    dialog.getDialogPane.setContent(grid)
    updateVisibility() // set initial checkbox visibility
    titleField.requestFocus()

    // Intercept OK button to validate before closing
    val okButton = dialog.getDialogPane.lookupButton(ButtonType.OK)
    okButton.addEventFilter(
      javafx.event.ActionEvent.ACTION,
      (event: javafx.event.ActionEvent) =>
        val errors    = scala.collection.mutable.ListBuffer[String]()
        val titleText = Option(titleField.getText).map(_.trim).getOrElse("")
        val raagText =
          if confirmedRaagName.nonEmpty then confirmedRaagName
          else Option(raagCombo.getEditor.getText).map(_.trim).getOrElse("")
        val isGat   = typeCombo.getValue == UiStrings.dialogNewCompositionFieldTypeGatDesktop
        val layaVal = layaCombo.getValue

        val filePathText = Option(filePathField.getText).map(_.trim).getOrElse("")

        if titleText.isEmpty then errors += UiStrings.dialogNewCompositionValidationTitleRequired
        if filePathText.isEmpty then errors += UiStrings.dialogNewCompositionValidationFilePathRequired
        if raagText.isEmpty then errors += UiStrings.dialogNewCompositionValidationRaagRequired
        if isGat && (layaVal == null || layaVal == UiStrings.dialogNewCompositionFieldLayaNoneDesktop) then
          errors += UiStrings.dialogNewCompositionValidationLayaRequired

        if errors.nonEmpty then
          errorLabel.setText(errors.mkString(". "))
          event.consume() // prevent dialog from closing
    )

    dialog.setResultConverter(bt =>
      if bt.getButtonData == ButtonBar.ButtonData.OK_DONE then
        val compType =
          if typeCombo.getValue == UiStrings.dialogNewCompositionFieldTypeBandishDesktop then CompositionType.Bandish
          else if typeCombo.getValue == UiStrings.dialogNewCompositionFieldTypePaltaDesktop then CompositionType.Palta
          else if typeCombo.getValue == UiStrings.dialogNewCompositionFieldTypeSargamDesktop then CompositionType.Sargam
          else CompositionType.Gat

        val laya =
          if layaCombo.getValue == UiStrings.dialogNewCompositionFieldLayaAtivilambitDesktop then Some(Laya.AtiVilambit)
          else if layaCombo.getValue == UiStrings.dialogNewCompositionFieldLayaVilambit then Some(Laya.Vilambit)
          else if layaCombo.getValue == UiStrings.dialogNewCompositionFieldLayaMadhya then Some(Laya.Madhya)
          else if layaCombo.getValue == UiStrings.dialogNewCompositionFieldLayaDrut then Some(Laya.Drut)
          else if layaCombo.getValue == UiStrings.dialogNewCompositionFieldLayaAtidrutDesktop then Some(Laya.AtiDrut)
          else None

        val titleText = titleField.getText.trim

        // Use confirmed name (from selection or typing), fall back to editor text
        val raagName =
          if confirmedRaagName.nonEmpty then confirmedRaagName
          else Option(raagCombo.getEditor.getText).map(_.trim).getOrElse("")

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
          case "Kannada" => SwarScript.Kannada
          case "Telugu"  => SwarScript.Telugu
          case "English" => SwarScript.English
          case _         => SwarScript.Devanagari

        val taanCount =
          if compType == CompositionType.Gat then taanSpinner.getValue.intValue
          else 0

        val filePathText = Option(filePathField.getText).map(_.trim).getOrElse("")
        val filePath = java.nio.file.Path.of(
          if filePathText.endsWith(".swar") then filePathText else filePathText + ".swar"
        )

        val isGatOrBandish = compType == CompositionType.Gat || compType == CompositionType.Bandish

        Result(
          title = titleText,
          compositionType = compType,
          raag = raag,
          taalName = if taalCombo.getValue != null then taalCombo.getValue.toLowerCase else "teentaal",
          laya = laya,
          script = script,
          taanCount = taanCount,
          showStrokeLine = strokeCheck.isSelected,
          showSahityaLine = sahityaCheck.isSelected,
          filePath = filePath,
          gatStartingBeat = if isGatOrBandish then gatStartSpinner.getValue.intValue else 1,
          antaraStartingBeat = if isGatOrBandish then antaraStartSpinner.getValue.intValue else 1,
          taanStartingBeat = if compType == CompositionType.Gat then taanStartSpinner.getValue.intValue else 1
        )
      else null
    )

    val result = dialog.showAndWait()
    if result.isPresent && result.get() != null then Some(result.get())
    else None
