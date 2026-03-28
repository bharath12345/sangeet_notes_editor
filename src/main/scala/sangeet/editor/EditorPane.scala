package sangeet.editor

import scalafx.scene.layout.StackPane
import scalafx.scene.canvas.Canvas
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.Includes.*
import sangeet.model.*
import sangeet.layout.LayoutConfig
import sangeet.render.CanvasRenderer

class EditorPane extends StackPane:
  private val canvas = new Canvas(1100, 700)
  children = List(canvas)
  focusTraversable = true

  private var editor: Option[CompositionEditor] = None
  private val config = LayoutConfig()

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
      val newEditor = e.code match
        case KeyCode.Right => ed.copy(cursor = ed.cursor.nextBeat)
        case KeyCode.Left  => ed.copy(cursor = ed.cursor.prevBeat)
        case KeyCode.Space =>
          KeyHandler.handleSpecialKey(ed, "SPACE")
        case KeyCode.Minus =>
          KeyHandler.handleSpecialKey(ed, "MINUS")
        case KeyCode.Period if !e.isControlDown =>
          KeyHandler.handleOctaveKey(ed, "PERIOD")
        case KeyCode.Quote =>
          KeyHandler.handleOctaveKey(ed, "QUOTE")
        case _ => ed

      editor = Some(newEditor)
      redraw()
    }

  onKeyTyped = (e: KeyEvent) =>
    editor.foreach { ed =>
      val ch = e.character.headOption.getOrElse(' ')
      if ch.isLetter then
        val (newEditor, _) = KeyHandler.handleSwarKey(ed, ch, e.isShiftDown)
        editor = Some(newEditor)
        redraw()
    }
