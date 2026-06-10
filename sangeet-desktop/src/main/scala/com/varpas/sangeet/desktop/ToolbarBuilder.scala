package com.varpas.sangeet.desktop

import java.nio.file.Path

import scalafx.collections.ObservableBuffer
import scalafx.scene.control._
import scalafx.scene.layout.{HBox, Priority, Region}
import scalafx.stage.FileChooser

import com.varpas.sangeet.core.editor.CompositionEditor
import com.varpas.sangeet.core.format.{HtmlExport, SwarFormat}
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.render.ScriptMap
import com.varpas.sangeet.core.taal.Taals
import com.varpas.sangeet.desktop.dialog.{CompositionPropertiesDialog, NewCompositionDialog}
import com.varpas.sangeet.desktop.editor.{AppLogger, KeyboardLegend, StatusBar, TabManager}

class ToolbarBuilder(
    stageProvider: () => javafx.stage.Stage,
    tabManager: TabManager,
    statusBar: StatusBar,
    keyboardLegend: KeyboardLegend
):
  private def btnStyle = "-fx-font-size: 11px;"
  private def iconLabel(symbol: String) = new Label(symbol):
    style = "-fx-font-size: 14px;"

  private def withActiveEditor(f: com.varpas.sangeet.desktop.editor.EditorPane => Unit): Unit =
    tabManager.withActiveEditor(f)

  private def focusActiveEditor(): Unit =
    tabManager.activeTab.foreach(_.editorPane.requestFocus())

  private def stage = stageProvider()

  val openFolderBtn: Button = new Button("Open Folder"):
    style = btnStyle
    graphic = iconLabel("📂")
    tooltip = new Tooltip("Open a folder in the file browser")

  def build(): ToolBar =
    val newBtn = new Button("New"):
      style = btnStyle
      graphic = iconLabel("➕")
      tooltip = new Tooltip("Create a new composition")
      onAction = _ =>
        NewCompositionDialog.show(stage).foreach { result =>
          val taal = Taals.byName(result.taalName).getOrElse(Taals.teentaal)
          val editor = CompositionEditor.create(
            title = result.title,
            compositionType = result.compositionType,
            taal = taal,
            raag = result.raag,
            laya = result.laya,
            taanCount = result.taanCount,
            showStrokeLine = result.showStrokeLine,
            showSahityaLine = result.showSahityaLine,
            gatStartingBeat = result.gatStartingBeat,
            antaraStartingBeat = result.antaraStartingBeat,
            taanStartingBeat = result.taanStartingBeat
          )
          AppLogger.info(
            s"New composition: type=${result.compositionType}, title=${result.title}, taal=${result.taalName}, file=${result.filePath}"
          )
          val et = tabManager.newTab()
          et.editorPane.setReadOnly(false)
          et.editorPane.setEditor(editor)
          et.editorPane.setFilePathAndSave(result.filePath)
          et.editorPane.changeScript(result.script)
          et.filePath = Some(result.filePath)
          tabManager.tabPane.selectionModel.value.select(et.tab)
          keyboardLegend.updateScript(result.script)
          statusBar.log(s"New ${result.compositionType} created: ${result.title} -> ${result.filePath}")
        }
        focusActiveEditor()

    val openBtn = new Button("Open File"):
      style = btnStyle
      graphic = iconLabel("📄")
      tooltip = new Tooltip("Open a .swar file")
      onAction = _ =>
        val fc = new FileChooser:
          title = "Open Composition"
          extensionFilters.add(new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
        val file = fc.showOpenDialog(stage)
        if file != null then tabManager.openFile(file.toPath)
        focusActiveEditor()

    val saveBtn = new Button("Save"):
      style = btnStyle
      graphic = iconLabel("💾")
      tooltip = new Tooltip("Save composition to current file")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getComposition.foreach { comp =>
            et.filePath match
              case Some(path) =>
                SwarFormat.writeFile(path, comp)
                AppLogger.info(s"File saved: $path")
                statusBar.log(s"Saved: ${path.getFileName}")
              case None =>
                val fc = new FileChooser:
                  title = "Save Composition"
                  extensionFilters.add(new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
                val file = fc.showSaveDialog(stage)
                if file != null then
                  val path =
                    if file.getName.endsWith(".swar") then file.toPath
                    else Path.of(file.getPath + ".swar")
                  SwarFormat.writeFile(path, comp)
                  et.editorPane.setFilePath(path)
                  et.filePath = Some(path)
                  AppLogger.info(s"File saved: $path")
                  statusBar.log(s"Saved: ${file.getName}")
          }
        }
        focusActiveEditor()

    val saveAsBtn = new Button("Save As"):
      style = btnStyle
      graphic = iconLabel("📋")
      tooltip = new Tooltip("Save composition as a new .swar file")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getComposition.foreach { comp =>
            val fc = new FileChooser:
              title = "Save Composition As"
              extensionFilters.add(new FileChooser.ExtensionFilter("Swar Files", "*.swar"))
            val file = fc.showSaveDialog(stage)
            if file != null then
              val path =
                if file.getName.endsWith(".swar") then file.toPath
                else Path.of(file.getPath + ".swar")
              SwarFormat.writeFile(path, comp)
              et.editorPane.setFilePath(path)
              et.filePath = Some(path)
              AppLogger.info(s"File saved as: $path")
              statusBar.log(s"Saved as: ${file.getName}")
          }
        }
        focusActiveEditor()

    val cutBtn = new Button("Cut"):
      style = btnStyle
      graphic = iconLabel("✂")
      tooltip = new Tooltip("Cut selected events (Ctrl+X)")
      onAction = _ =>
        withActiveEditor(_.cutSelection())
        focusActiveEditor()

    val copyBtn = new Button("Copy"):
      style = btnStyle
      graphic = iconLabel("📋")
      tooltip = new Tooltip("Copy selected events (Ctrl+C)")
      onAction = _ =>
        withActiveEditor(_.copySelection())
        focusActiveEditor()

    val pasteBtn = new Button("Paste"):
      style = btnStyle
      graphic = iconLabel("📌")
      tooltip = new Tooltip("Paste clipboard events (Ctrl+V)")
      onAction = _ =>
        withActiveEditor(_.pasteClipboard())
        focusActiveEditor()

    val htmlBtn = new Button("HTML"):
      style = btnStyle
      graphic = iconLabel("🌐")
      tooltip = new Tooltip("Export composition as HTML")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getComposition.foreach { comp =>
            val fc = new FileChooser:
              title = "Export HTML"
              extensionFilters.add(new FileChooser.ExtensionFilter("HTML Files", "*.html"))
            val file = fc.showSaveDialog(stage)
            if file != null then
              val path =
                if file.getName.endsWith(".html") then file.toPath
                else Path.of(file.getPath + ".html")
              HtmlExport.exportHtml(comp, path, et.editorPane.currentScript)
              AppLogger.info(s"HTML exported: $path")
              statusBar.log(s"Exported HTML: ${file.getName}")
          }
        }
        focusActiveEditor()

    val propertiesBtn = new Button("Properties"):
      style = btnStyle
      graphic = iconLabel("⚙")
      tooltip = new Tooltip("Edit composition metadata")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getComposition.foreach { comp =>
            CompositionPropertiesDialog.show(comp.metadata, comp.sections, stage).foreach { result =>
              et.editorPane.applyMetadataChange(result.metadata)
              if result.sectionStartingBeats.nonEmpty then
                et.editorPane.applySectionStartingBeats(result.sectionStartingBeats)
              statusBar.log(s"Updated composition properties")
            }
          }
        }
        focusActiveEditor()

    val addSectionBtn = new Button("Add Section"):
      style = btnStyle
      graphic = iconLabel("➕")
      tooltip = new Tooltip("Add a new section to the composition")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getComposition.foreach { comp =>
            if comp.metadata.compositionType != CompositionType.Gat then
              statusBar.log("Sections can only be added to Gat compositions")
            else
              val choices = java.util.Arrays.asList("Gat", "Sthayi", "Antara", "Taan", "Jhala", "Jod")
              val dialog  = new javafx.scene.control.ChoiceDialog[String]("Taan", choices)
              dialog.initOwner(stage)
              dialog.setTitle("Add Section")
              dialog.setHeaderText("Choose section type")
              val result = dialog.showAndWait()
              if result.isPresent then
                val choice = result.get()
                val sType = choice match
                  case "Gat"    => SectionType.Custom("Gat")
                  case "Sthayi" => SectionType.Sthayi
                  case "Antara" => SectionType.Antara
                  case "Taan"   => SectionType.Taan
                  case "Jhala"  => SectionType.Jhala
                  case "Jod"    => SectionType.Custom("Jod")
                  case other    => SectionType.Custom(other)
                val newSection = Section(choice, sType, Nil)
                val newComp    = comp.copy(sections = comp.sections :+ newSection)
                et.editorPane.setComposition(newComp)
                statusBar.log(s"Added section: $choice")
          }
        }
        focusActiveEditor()

    val renameSectionBtn = new Button("Rename Section"):
      style = btnStyle
      graphic = iconLabel("✏")
      tooltip = new Tooltip("Rename the current section")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getEditor.foreach { ed =>
            val section = ed.currentSection
            val dialog  = new javafx.scene.control.TextInputDialog(section.name)
            dialog.initOwner(stage)
            dialog.setTitle("Rename Section")
            dialog.setHeaderText("Enter new section name")
            val result = dialog.showAndWait()
            if result.isPresent && result.get().trim.nonEmpty then
              val newEd = ed.renameSection(ed.currentSectionIndex, result.get().trim)
              et.editorPane.setEditor(newEd)
              statusBar.log(s"Renamed section to: ${result.get().trim}")
          }
        }
        focusActiveEditor()

    val removeSectionBtn = new Button("Remove Section"):
      style = btnStyle
      graphic = iconLabel("➖")
      tooltip = new Tooltip("Remove the current section")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getEditor.foreach { ed =>
            val sectionName = ed.currentSection.name
            ed.removeSection(ed.currentSectionIndex) match
              case Some(newEd) =>
                et.editorPane.setEditor(newEd)
                statusBar.log(s"Removed section: $sectionName")
              case None =>
                statusBar.log("Cannot remove the last section")
          }
        }
        focusActiveEditor()

    val moveUpBtn = new Button("Move Up"):
      style = btnStyle
      graphic = iconLabel("⬆")
      tooltip = new Tooltip("Move current section up")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getEditor.foreach { ed =>
            if ed.currentSectionIndex > 0 then
              val newEd = ed.moveSection(ed.currentSectionIndex, ed.currentSectionIndex - 1)
              et.editorPane.setEditor(newEd)
              statusBar.log(s"Moved section up")
            else statusBar.log("Already at top")
          }
        }
        focusActiveEditor()

    val moveDownBtn = new Button("Move Down"):
      style = btnStyle
      graphic = iconLabel("⬇")
      tooltip = new Tooltip("Move current section down")
      onAction = _ =>
        tabManager.activeTab.foreach { et =>
          et.editorPane.getEditor.foreach { ed =>
            if ed.currentSectionIndex < ed.composition.sections.size - 1 then
              val newEd = ed.moveSection(ed.currentSectionIndex, ed.currentSectionIndex + 1)
              et.editorPane.setEditor(newEd)
              statusBar.log(s"Moved section down")
            else statusBar.log("Already at bottom")
          }
        }
        focusActiveEditor()

    val scriptCombo = new ComboBox[String](
      ObservableBuffer(
        "Devanagari (Hindi)",
        "Kannada",
        "Telugu",
        "English"
      )
    ):
      style = btnStyle
      value = "Devanagari (Hindi)"
      tooltip = new Tooltip("Change notation script")
    scriptCombo.value.addListener { (_, _, newVal) =>
      if newVal != null then
        val script = newVal match
          case "Kannada" => SwarScript.Kannada
          case "Telugu"  => SwarScript.Telugu
          case "English" => SwarScript.English
          case _         => SwarScript.Devanagari
        AppLogger.info(s"Script changed: $script")
        withActiveEditor(_.changeScript(script))
        keyboardLegend.updateScript(script)
        statusBar.log(s"Script changed to ${ScriptMap.displayName(script)}")
        focusActiveEditor()
    }

    val undoBtn = new Button("Undo"):
      style = btnStyle
      graphic = iconLabel("↩")
      tooltip = new Tooltip("Undo last edit (Ctrl+Z)")
      onAction = _ =>
        statusBar.log("Use Ctrl+Z (Cmd+Z on Mac) for undo")
        focusActiveEditor()

    val redoBtn = new Button("Redo"):
      style = btnStyle
      graphic = iconLabel("↪")
      tooltip = new Tooltip("Redo (Ctrl+Shift+Z)")
      onAction = _ =>
        statusBar.log("Use Ctrl+Shift+Z (Cmd+Shift+Z on Mac) for redo")
        focusActiveEditor()

    val spacer = new Region()
    HBox.setHgrow(spacer, Priority.Always)

    val aboutBtn = new Button("About"):
      style = btnStyle
      graphic = iconLabel("ℹ")
      tooltip = new Tooltip("About Sangeet Notes Editor")
      onAction = _ =>
        val dialog = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
        dialog.initOwner(stage)
        dialog.setTitle("About")
        dialog.setHeaderText("Sangeet Notes Editor")
        dialog.setContentText("""A desktop notation editor for Hindustani classical music
            |in the Bhatkhande notation style.
            |
            |Designed for sitar compositions -- Gat, Bandish, and Palta.
            |
            |Version 1.0
            |Built with Scala 3 + ScalaFX""".stripMargin)
        dialog.showAndWait()
        focusActiveEditor()

    new ToolBar:
      items = List(
        newBtn,
        openBtn,
        openFolderBtn,
        saveBtn,
        saveAsBtn,
        cutBtn,
        copyBtn,
        pasteBtn,
        htmlBtn,
        new Separator(),
        propertiesBtn,
        addSectionBtn,
        renameSectionBtn,
        removeSectionBtn,
        moveUpBtn,
        moveDownBtn,
        new Separator(),
        undoBtn,
        redoBtn,
        new Separator(),
        new Label("Script:"):
          style = "-fx-font-size: 11px;"
        ,
        scriptCombo,
        spacer,
        aboutBtn
      )
