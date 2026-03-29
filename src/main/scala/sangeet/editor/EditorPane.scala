package sangeet.editor

import scalafx.scene.layout.{Pane, VBox, Priority}
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.ScrollPane
import scalafx.scene.input.KeyCode
import scalafx.animation.{Timeline, KeyFrame}
import scalafx.util.Duration
import sangeet.model.*
import sangeet.model.{Gamak, Andolan, Gitkari, MeendDirection}
import sangeet.layout.LayoutConfig
import sangeet.render.{CanvasRenderer, SectionBounds}

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
  private var sectionBounds: List[SectionBounds] = Nil
  private var cursorVisible: Boolean = true

  // Blink timer: toggle cursor visibility every 530ms
  private val blinkTimeline = new Timeline:
    cycleCount = Timeline.Indefinite
    keyFrames = Seq(
      KeyFrame(Duration(530), onFinished = _ =>
        cursorVisible = !cursorVisible
        redraw()
      )
    )
  blinkTimeline.play()

  private def resetBlink(): Unit =
    cursorVisible = true
    blinkTimeline.stop()
    blinkTimeline.playFromStart()

  canvas.delegate.setOnMouseClicked { (e: javafx.scene.input.MouseEvent) =>
    scrollPane.requestFocus()
    editor.foreach { ed =>
      val clickY = e.getY
      sectionBounds.find(b => clickY >= b.startY && clickY <= b.endY).foreach { bounds =>
        if bounds.sectionIndex != ed.currentSectionIndex then
          val newCursor = CursorModel(ed.composition.metadata.taal)
          val newEditor = ed.copy(currentSectionIndex = bounds.sectionIndex, cursor = newCursor)
          editor = Some(newEditor)
          val sectionName = ed.composition.sections(bounds.sectionIndex).name
          statusBar.log(s"◆ Switched to section: $sectionName")
          resetBlink()
          redraw()
      }
    }
  }

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
      sectionBounds = CanvasRenderer.render(canvas, ed.composition, config,
        Some(ed.currentSectionIndex, ed.cursor.cycle, ed.cursor.beat), cursorVisible)
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
          case KeyCode.M =>
            e.consume()
            if e.isShiftDown then
              ornamentMode = Some(OrnamentMode.MeendStart(MeendDirection.Descending))
              (ed, "◆ Meend ↓ (descending) — type the start note")
            else
              ornamentMode = Some(OrnamentMode.MeendStart(MeendDirection.Ascending))
              (ed, "◆ Meend ↑ (ascending) — type the start note")
          case KeyCode.J =>
            e.consume()
            ornamentMode = Some(OrnamentMode.KrintanStart)
            (ed, "◆ Krintan mode — type the start note")
          case KeyCode.U =>
            e.consume()
            ornamentMode = Some(OrnamentMode.MurkiCollect(Nil))
            (ed, "◆ Murki mode — type notes, then press Enter to finish")
          case KeyCode.Z =>
            e.consume()
            ornamentMode = Some(OrnamentMode.ZamzamaCollect(Nil))
            (ed, "◆ Zamzama mode — type notes, then press Enter to finish")
          case _ => (ed, "")
      else
        code match
          case KeyCode.Right =>
            e.consume()
            val next = ed.cursor.nextBeat
            val maxAllowedCycle = ed.maxCycle + 1
            if next.cycle > maxAllowedCycle then
              (ed, "") // already at end, don't move further
            else
              (ed.copy(cursor = next), "→ Cursor forward")
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
          case KeyCode.Enter =>
            e.consume()
            ornamentMode match
              case Some(mode @ (OrnamentMode.MurkiCollect(_) | OrnamentMode.ZamzamaCollect(_))) =>
                val (newEd, msg) = KeyHandler.finishMultiNoteOrnament(ed, mode)
                ornamentMode = None
                (newEd, msg)
              case _ => (ed, "")
          case _ => (ed, "")

      if msg.nonEmpty then statusBar.log(msg)
      editor = Some(newEditor)
      resetBlink()
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
            val (newEditor, msg, nextMode) = KeyHandler.handleNoteOrnament(ed, ch, isShifted, mode)
            statusBar.log(msg)
            editor = Some(newEditor)
            ornamentMode = nextMode
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
