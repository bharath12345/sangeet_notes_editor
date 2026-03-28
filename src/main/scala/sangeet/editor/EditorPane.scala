package sangeet.editor

import scalafx.scene.layout.Pane
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.ScrollPane
import scalafx.scene.input.{KeyCode, KeyEvent, MouseEvent}
import scalafx.Includes.*
import sangeet.model.*
import sangeet.layout.LayoutConfig
import sangeet.render.CanvasRenderer

class EditorPane(statusBar: StatusBar) extends ScrollPane:
  private val canvas = new Canvas(1100, 2000)
  private val canvasHolder = new Pane:
    children = List(canvas)
    prefWidth = 1100
    prefHeight = 2000

  content = canvasHolder
  focusTraversable = true
  fitToWidth = true
  hbarPolicy = ScrollPane.ScrollBarPolicy.AsNeeded
  vbarPolicy = ScrollPane.ScrollBarPolicy.AsNeeded

  private var editor: Option[CompositionEditor] = None
  private val config = LayoutConfig()

  onMouseClicked = (_: MouseEvent) =>
    this.requestFocus()

  def setComposition(comp: Composition): Unit =
    editor = Some(CompositionEditor(comp, 0, CursorModel(comp.metadata.taal)))
    redraw()

  def setEditor(ed: CompositionEditor): Unit =
    editor = Some(ed)
    redraw()

  def getComposition: Option[Composition] = editor.map(_.composition)

  def redraw(): Unit =
    editor.foreach { ed =>
      CanvasRenderer.render(canvas, ed.composition, config)
    }

  onKeyPressed = (e: KeyEvent) =>
    editor.foreach { ed =>
      val (newEditor, msg) = e.code match
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

  onKeyTyped = (e: KeyEvent) =>
    editor.foreach { ed =>
      val ch = e.character.headOption.getOrElse('\u0000')
      // Skip control chars and keys already handled in onKeyPressed
      if ch.isLetter then
        e.consume()
        val isShifted = ch.isUpper
        val (newEditor, msg) = KeyHandler.handleSwarKey(ed, ch, isShifted)
        statusBar.log(msg)
        editor = Some(newEditor)
        redraw()
      else if ch >= ' ' && ch != '`' && ch != '.' && ch != '\'' && ch != '-' then
        // Printable char not handled elsewhere
        statusBar.log(s"✗ Unknown key '${ch}' — use s/r/g/m/p/d/n for notes, . ' ` for octave")
    }
