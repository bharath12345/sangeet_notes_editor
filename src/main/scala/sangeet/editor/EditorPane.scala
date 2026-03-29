package sangeet.editor

import scalafx.scene.layout.{Pane, VBox, Priority}
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.ScrollPane
import scalafx.scene.input.KeyCode
import sangeet.model.*
import sangeet.model.{Gamak, Andolan, Gitkari}
import sangeet.layout.LayoutConfig
import sangeet.render.CanvasRenderer

class EditorPane(statusBar: StatusBar) extends VBox:
  private val header = new CompositionHeader()

  private val canvas = new Canvas(1100, 2000)
  private val canvasHolder = new Pane:
    children = List(canvas)
    prefWidth = 1100
    prefHeight = 2000

  private val scrollPane = new ScrollPane:
    content = canvasHolder
    focusTraversable = true
    fitToWidth = true
    hbarPolicy = ScrollPane.ScrollBarPolicy.AsNeeded
    vbarPolicy = ScrollPane.ScrollBarPolicy.AsNeeded

  VBox.setVgrow(scrollPane, Priority.Always)
  children = List(header, scrollPane)

  private var editor: Option[CompositionEditor] = None
  private val config = LayoutConfig()
  private var ornamentMode: Option[OrnamentMode] = None

  scrollPane.delegate.setOnMouseClicked(_ => scrollPane.requestFocus())

  def setComposition(comp: Composition): Unit =
    editor = Some(CompositionEditor(comp, 0, CursorModel(comp.metadata.taal)))
    header.update(comp.metadata)
    redraw()

  def setEditor(ed: CompositionEditor): Unit =
    editor = Some(ed)
    header.update(ed.composition.metadata)
    redraw()

  def getComposition: Option[Composition] = editor.map(_.composition)

  def updateHeader(meta: Metadata): Unit =
    header.update(meta)

  def redraw(): Unit =
    editor.foreach { ed =>
      CanvasRenderer.render(canvas, ed.composition, config,
        Some(ed.currentSectionIndex, ed.cursor.cycle, ed.cursor.beat))
    }

  override def requestFocus(): Unit =
    scrollPane.requestFocus()

  scrollPane.delegate.setOnKeyPressed { (e: javafx.scene.input.KeyEvent) =>
    editor.foreach { ed =>
      val code = KeyCode.jfxEnum2sfx(e.getCode)
      val (newEditor, msg) = if e.isControlDown || e.isMetaDown then
        code match
          case KeyCode.D =>
            e.consume()
            KeyHandler.handleStroke(ed, Stroke.Da)
          case KeyCode.R =>
            e.consume()
            KeyHandler.handleStroke(ed, Stroke.Ra)
          case KeyCode.G =>
            e.consume()
            KeyHandler.handleSimpleOrnament(ed, Gamak(), "Gamak")
          case KeyCode.A =>
            e.consume()
            KeyHandler.handleSimpleOrnament(ed, Andolan(), "Andolan")
          case KeyCode.I =>
            e.consume()
            KeyHandler.handleSimpleOrnament(ed, Gitkari(), "Gitkari")
          case KeyCode.K =>
            e.consume()
            ornamentMode = Some(OrnamentMode.KanSwar)
            (ed, "◆ Kan Swar mode — type a note for the grace note")
          case KeyCode.H =>
            e.consume()
            ornamentMode = Some(OrnamentMode.Sparsh)
            (ed, "◆ Sparsh mode — type a note for the touch note")
          case KeyCode.E =>
            e.consume()
            ornamentMode = Some(OrnamentMode.Ghaseet)
            (ed, "◆ Ghaseet mode — type a note for the target note")
          case _ => (ed, "")
      else
        code match
          case KeyCode.Right =>
            e.consume()
            (ed.copy(cursor = ed.cursor.nextBeat), "→ Cursor forward")
          case KeyCode.Left =>
            e.consume()
            (ed.copy(cursor = ed.cursor.prevBeat), "← Cursor back")
          case KeyCode.Space =>
            e.consume()
            KeyHandler.handleSpecialKey(ed, "SPACE")
          case KeyCode.Minus =>
            e.consume()
            KeyHandler.handleSpecialKey(ed, "MINUS")
          case KeyCode.BackSpace | KeyCode.Delete =>
            e.consume()
            KeyHandler.handleSpecialKey(ed, "BACKSPACE")
          case KeyCode.Period if !e.isControlDown =>
            e.consume()
            KeyHandler.handleOctaveKey(ed, "PERIOD")
          case KeyCode.Quote =>
            e.consume()
            KeyHandler.handleOctaveKey(ed, "QUOTE")
          case KeyCode.BackQuote =>
            e.consume()
            KeyHandler.handleOctaveKey(ed, "BACKTICK")
          case KeyCode.Escape =>
            e.consume()
            ornamentMode = None
            (ed, "◆ Ornament mode cancelled")
          case _ => (ed, "")

      if msg.nonEmpty then statusBar.log(msg)
      editor = Some(newEditor)
      redraw()
    }
  }

  scrollPane.delegate.setOnKeyTyped { (e: javafx.scene.input.KeyEvent) =>
    editor.foreach { ed =>
      val ch = if e.getCharacter != null && e.getCharacter.nonEmpty then e.getCharacter.charAt(0) else '\u0000'
      if ch.isLetter then
        e.consume()
        val isShifted = ch.isUpper
        ornamentMode match
          case Some(mode) =>
            val (newEditor, msg) = KeyHandler.handleNoteOrnament(ed, ch, isShifted, mode)
            statusBar.log(msg)
            editor = Some(newEditor)
            ornamentMode = None
            redraw()
          case None =>
            val (newEditor, msg) = KeyHandler.handleSwarKey(ed, ch, isShifted)
            statusBar.log(msg)
            editor = Some(newEditor)
            redraw()
      else if ch >= ' ' && ch != '`' && ch != '.' && ch != '\'' && ch != '-' then
        statusBar.log(s"✗ Unknown key '${ch}' — use s/r/g/m/p/d/n for notes, . ' ` for octave")
    }
  }
