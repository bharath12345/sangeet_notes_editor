package sangeet.editor

import scalafx.scene.control.*
import scalafx.scene.layout.HBox
import scalafx.geometry.Insets
import scalafx.collections.ObservableBuffer
import sangeet.taal.Taals
import sangeet.model.*

class ToolBar(onTaalChange: Taal => Unit, onAddSection: () => Unit) extends HBox:
  spacing = 10
  padding = Insets(5, 10, 5, 10)

  private val taalCombo = new ComboBox[String]:
    items = ObservableBuffer.from(
      Taals.all.keys.toList.sorted.map(_.capitalize)
    )
    value = "Teentaal"
    onAction = _ =>
      Taals.byName(value.value).foreach(onTaalChange)

  private val addSectionBtn = new Button("+ Section"):
    onAction = _ => onAddSection()

  children = List(
    new Label("Taal:"), taalCombo,
    new Separator(),
    addSectionBtn
  )
