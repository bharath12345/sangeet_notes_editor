package sangeet.editor

import scalafx.scene.layout.{Pane, VBox, Priority}
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.ScrollPane
import scalafx.scene.input.KeyCode
import sangeet.model.*
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
      val (newEditor, msg) = code match
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
        val (newEditor, msg) = KeyHandler.handleSwarKey(ed, ch, isShifted)
        statusBar.log(msg)
        editor = Some(newEditor)
        redraw()
      else if ch >= ' ' && ch != '`' && ch != '.' && ch != '\'' && ch != '-' then
        statusBar.log(s"✗ Unknown key '${ch}' — use s/r/g/m/p/d/n for notes, . ' ` for octave")
    }
  }
