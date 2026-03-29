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

  private val canvas = new Canvas(1100, 600)
  private val canvasHolder = new Pane:
    children = List(canvas)
    prefWidth = 1100
    prefHeight = 600

  private val scrollPane = new ScrollPane:
    content = canvasHolder
    focusTraversable = true
    fitToWidth = true
    hbarPolicy = ScrollPane.ScrollBarPolicy.AsNeeded
    vbarPolicy = ScrollPane.ScrollBarPolicy.AsNeeded

  VBox.setVgrow(scrollPane, Priority.Always)
  children = List(header, scrollPane)

  private var history: Option[UndoHistory] = None
  private def editor: Option[CompositionEditor] = history.map(_.present)
  private val config = LayoutConfig()
  private var ornamentMode: Option[OrnamentMode] = None
  private var sectionBounds: List[SectionBounds] = Nil
  private var cursorVisible: Boolean = true

  // Double-tap detection for dual swar (ss = SaSa, rr = ReRe, etc.)
  private var lastTypedChar: Char = '\u0000'
  private var lastTypedTime: Long = 0L
  private val doubleTapThresholdMs = 350L

  // Cursor region from last render for partial redraw on blink
  private var lastCursorRegion: Option[(Double, Double, Double, Double)] = None

  // Blink timer: toggle cursor visibility every 530ms
  private val blinkTimeline = new Timeline:
    cycleCount = Timeline.Indefinite
    keyFrames = Seq(
      KeyFrame(Duration(530), onFinished = _ =>
        cursorVisible = !cursorVisible
        // Full redraw needed since cursor is drawn as part of the grid
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
      val clickX = e.getX
      val clickY = e.getY
      sectionBounds.find(b => clickY >= b.startY && clickY <= b.endY).foreach { bounds =>
        // Try to find the clicked beat cell within this section
        val clickedBeat = bounds.lines.find(lb => clickY >= lb.startY && clickY <= lb.endY).flatMap { lb =>
          val relX = clickX - lb.startX
          if relX >= 0 && relX < lb.cellCount * lb.cellWidth then
            val cellIdx = (relX / lb.cellWidth).toInt
            Some((lb.cycle, lb.firstBeat + cellIdx))
          else None
        }

        val switchedSection = bounds.sectionIndex != ed.currentSectionIndex
        val newCursor = clickedBeat match
          case Some((cycle, beat)) =>
            val clampedBeat = math.min(beat, ed.composition.metadata.taal.matras - 1)
            ed.cursor.copy(cycle = cycle, beat = clampedBeat, subIndex = 0, totalSubdivisions = 1)
          case None if switchedSection =>
            CursorModel(ed.composition.metadata.taal)
          case None =>
            ed.cursor // clicked in section but not on a cell — keep cursor

        val newEditor = ed.copy(
          currentSectionIndex = bounds.sectionIndex,
          cursor = newCursor
        )
        if switchedSection then
          val sectionName = ed.composition.sections(bounds.sectionIndex).name
          statusBar.log(s"◆ Switched to section: $sectionName")
        else if clickedBeat.isDefined then
          statusBar.log(s"◆ Cursor placed at cycle ${newCursor.cycle}, beat ${newCursor.beat}")

        setEditorDirect(newEditor)
        resetBlink()
        redraw()
      }
    }
  }

  /** Push a new editor state onto the undo stack. */
  private def pushEditor(newEd: CompositionEditor): Unit =
    history = history.map(_.push(newEd))

  /** Set editor state without undo history (for cursor-only moves). */
  private def setEditorDirect(newEd: CompositionEditor): Unit =
    history = history.map(h => h.copy(present = newEd))

  def setComposition(comp: Composition): Unit =
    val ed = CompositionEditor(comp, 0, CursorModel(comp.metadata.taal))
    history = Some(UndoHistory(ed))
    header.update(comp.metadata)
    redraw()

  def setEditor(ed: CompositionEditor): Unit =
    history = Some(UndoHistory(ed))
    header.update(ed.composition.metadata)
    redraw()

  def getComposition: Option[Composition] = editor.map(_.composition)
  def getEditor: Option[CompositionEditor] = editor

  def updateHeader(meta: Metadata): Unit =
    header.update(meta)

  def redraw(): Unit =
    editor.foreach { ed =>
      sectionBounds = CanvasRenderer.render(canvas, ed.composition, config,
        Some(ed.currentSectionIndex, ed.cursor.cycle, ed.cursor.beat), cursorVisible)
      // Auto-size canvas to fit content
      val contentHeight = sectionBounds.lastOption.map(_.endY + 40).getOrElse(200.0)
      val minHeight = scrollPane.height.value.max(400)
      val newHeight = contentHeight.max(minHeight)
      if Math.abs(canvas.height.value - newHeight) > 10 then
        canvas.height = newHeight
        canvasHolder.prefHeight = newHeight
        // Re-render at new size
        sectionBounds = CanvasRenderer.render(canvas, ed.composition, config,
          Some(ed.currentSectionIndex, ed.cursor.cycle, ed.cursor.beat), cursorVisible)
    }

  override def requestFocus(): Unit =
    scrollPane.requestFocus()

  // Determine if a key action is a content change (needs undo) vs cursor-only move
  private enum EditAction:
    case ContentChange(ed: CompositionEditor, msg: String)
    case CursorMove(ed: CompositionEditor, msg: String)
    case NoOp

  scrollPane.delegate.setOnKeyPressed { (e: javafx.scene.input.KeyEvent) =>
    editor.foreach { ed =>
      val code = KeyCode.jfxEnum2sfx(e.getCode)

      // Handle undo/redo first
      if (e.isControlDown || e.isMetaDown) && code == KeyCode.Z then
        e.consume()
        if e.isShiftDown then
          // Redo
          history.flatMap(_.redo).foreach { newHist =>
            history = Some(newHist)
            header.update(newHist.present.composition.metadata)
            statusBar.log("↷ Redo")
            resetBlink()
            redraw()
          }
        else
          // Undo
          history.flatMap(_.undo).foreach { newHist =>
            history = Some(newHist)
            header.update(newHist.present.composition.metadata)
            statusBar.log("↶ Undo")
            resetBlink()
            redraw()
          }
      else
        val action = if e.isControlDown || e.isMetaDown then
          code match
            case KeyCode.D =>
              e.consume()
              val (ne, m) = KeyHandler.handleStroke(ed, Stroke.Da)
              EditAction.ContentChange(ne, m)
            case KeyCode.R =>
              e.consume()
              val (ne, m) = KeyHandler.handleStroke(ed, Stroke.Ra)
              EditAction.ContentChange(ne, m)
            case KeyCode.G =>
              e.consume()
              val (ne, m) = KeyHandler.handleSimpleOrnament(ed, Gamak(), "Gamak")
              EditAction.ContentChange(ne, m)
            case KeyCode.A =>
              e.consume()
              val (ne, m) = KeyHandler.handleSimpleOrnament(ed, Andolan(), "Andolan")
              EditAction.ContentChange(ne, m)
            case KeyCode.I =>
              e.consume()
              val (ne, m) = KeyHandler.handleSimpleOrnament(ed, Gitkari(), "Gitkari")
              EditAction.ContentChange(ne, m)
            case KeyCode.K =>
              e.consume()
              ornamentMode = Some(OrnamentMode.KanSwar)
              EditAction.CursorMove(ed, "◆ Kan Swar mode — type a note for the grace note")
            case KeyCode.H =>
              e.consume()
              ornamentMode = Some(OrnamentMode.Sparsh)
              EditAction.CursorMove(ed, "◆ Sparsh mode — type a note for the touch note")
            case KeyCode.E =>
              e.consume()
              ornamentMode = Some(OrnamentMode.Ghaseet)
              EditAction.CursorMove(ed, "◆ Ghaseet mode — type a note for the target note")
            case KeyCode.M =>
              e.consume()
              if e.isShiftDown then
                ornamentMode = Some(OrnamentMode.MeendStart(MeendDirection.Descending))
                EditAction.CursorMove(ed, "◆ Meend ↓ (descending) — type the start note")
              else
                ornamentMode = Some(OrnamentMode.MeendStart(MeendDirection.Ascending))
                EditAction.CursorMove(ed, "◆ Meend ↑ (ascending) — type the start note")
            case KeyCode.J =>
              e.consume()
              ornamentMode = Some(OrnamentMode.KrintanStart)
              EditAction.CursorMove(ed, "◆ Krintan mode — type the start note")
            case KeyCode.U =>
              e.consume()
              ornamentMode = Some(OrnamentMode.MurkiCollect(Nil))
              EditAction.CursorMove(ed, "◆ Murki mode — type notes, then press Enter to finish")
            case KeyCode.W =>
              e.consume()
              ornamentMode = Some(OrnamentMode.ZamzamaCollect(Nil))
              EditAction.CursorMove(ed, "◆ Zamzama mode — type notes, then press Enter to finish")
            case KeyCode.C =>
              e.consume()
              val (ne, m) = KeyHandler.handleStroke(ed, Stroke.Chikari)
              EditAction.ContentChange(ne, m)
            case KeyCode.Digit2 | KeyCode.Numpad2 =>
              e.consume()
              EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 2), "◆ Subdivision: 2 per beat")
            case KeyCode.Digit3 | KeyCode.Numpad3 =>
              e.consume()
              EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 3), "◆ Subdivision: 3 per beat")
            case KeyCode.Digit4 | KeyCode.Numpad4 =>
              e.consume()
              EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 4), "◆ Subdivision: 4 per beat")
            case KeyCode.Digit5 | KeyCode.Numpad5 =>
              e.consume()
              EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 5), "◆ Subdivision: 5 per beat")
            case KeyCode.Digit6 | KeyCode.Numpad6 =>
              e.consume()
              EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 6), "◆ Subdivision: 6 per beat")
            case KeyCode.Digit7 | KeyCode.Numpad7 =>
              e.consume()
              EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 7), "◆ Subdivision: 7 per beat")
            case KeyCode.Digit8 | KeyCode.Numpad8 =>
              e.consume()
              EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 8), "◆ Subdivision: 8 per beat")
            case _ => EditAction.NoOp
        else
          code match
            case KeyCode.Right =>
              e.consume()
              val next = ed.cursor.nextBeat
              val maxAllowedCycle = ed.maxCycle + 1
              if next.cycle > maxAllowedCycle then EditAction.NoOp
              else EditAction.CursorMove(ed.copy(cursor = next), "→ Cursor forward")
            case KeyCode.Left =>
              e.consume()
              EditAction.CursorMove(ed.copy(cursor = ed.cursor.prevBeat), "← Cursor back")
            case KeyCode.Tab =>
              e.consume()
              val next = ed.cursor.nextBeat
              val maxAllowedCycle = ed.maxCycle + 1
              if next.cycle > maxAllowedCycle then EditAction.NoOp
              else EditAction.CursorMove(ed.copy(cursor = next), "→ Cursor forward")
            case KeyCode.Space =>
              e.consume()
              val (ne, m) = KeyHandler.handleSpecialKey(ed, "SPACE")
              EditAction.ContentChange(ne, m)
            case KeyCode.Minus =>
              e.consume()
              val (ne, m) = KeyHandler.handleSpecialKey(ed, "MINUS")
              EditAction.ContentChange(ne, m)
            case KeyCode.BackSpace | KeyCode.Delete =>
              e.consume()
              val (ne, m) = KeyHandler.handleSpecialKey(ed, "BACKSPACE")
              EditAction.ContentChange(ne, m)
            case KeyCode.Period if !e.isControlDown =>
              e.consume()
              val (ne, m) = KeyHandler.handleOctaveKey(ed, "PERIOD")
              EditAction.CursorMove(ne, m)
            case KeyCode.Quote =>
              e.consume()
              val (ne, m) = KeyHandler.handleOctaveKey(ed, "QUOTE")
              EditAction.CursorMove(ne, m)
            case KeyCode.BackQuote =>
              e.consume()
              val (ne, m) = KeyHandler.handleOctaveKey(ed, "BACKTICK")
              EditAction.CursorMove(ne, m)
            case KeyCode.Escape =>
              e.consume()
              ornamentMode = None
              EditAction.CursorMove(ed, "◆ Ornament mode cancelled")
            case KeyCode.Enter =>
              e.consume()
              ornamentMode match
                case Some(mode @ (OrnamentMode.MurkiCollect(_) | OrnamentMode.ZamzamaCollect(_))) =>
                  val (newEd, msg) = KeyHandler.finishMultiNoteOrnament(ed, mode)
                  ornamentMode = None
                  EditAction.ContentChange(newEd, msg)
                case None =>
                  // Enter in normal mode: advance to next cycle
                  val newCursor = ed.cursor.copy(beat = 0, cycle = ed.cursor.cycle + 1, subIndex = 0, totalSubdivisions = 1)
                  val maxAllowedCycle = ed.maxCycle + 1
                  if newCursor.cycle > maxAllowedCycle then EditAction.NoOp
                  else EditAction.CursorMove(ed.copy(cursor = newCursor), "↵ Next cycle")
                case _ => EditAction.NoOp
            case _ => EditAction.NoOp

        action match
          case EditAction.ContentChange(newEd, msg) =>
            if msg.nonEmpty then statusBar.log(msg)
            pushEditor(newEd)
            resetBlink()
            redraw()
          case EditAction.CursorMove(newEd, msg) =>
            if msg.nonEmpty then statusBar.log(msg)
            setEditorDirect(newEd)
            resetBlink()
            redraw()
          case EditAction.NoOp => ()
    }
  }

  scrollPane.delegate.setOnKeyTyped { (e: javafx.scene.input.KeyEvent) =>
    editor.foreach { ed =>
      val ch = if e.getCharacter != null && e.getCharacter.nonEmpty then e.getCharacter.charAt(0) else '\u0000'
      if ch.isLetter then
        e.consume()
        val isShifted = ch.isUpper
        val now = System.currentTimeMillis()
        ornamentMode match
          case Some(mode) =>
            val (newEd, msg, nextMode) = KeyHandler.handleNoteOrnament(ed, ch, isShifted, mode)
            statusBar.log(msg)
            if newEd ne ed then pushEditor(newEd) else setEditorDirect(newEd)
            ornamentMode = nextMode
            lastTypedChar = '\u0000'
            redraw()
          case None =>
            // Double-tap detection: same key within threshold
            val lowerCh = ch.toLower
            if lowerCh == lastTypedChar && (now - lastTypedTime) < doubleTapThresholdMs then
              // Undo the first note, then enter both as subdivided pair
              history.flatMap(_.undo).foreach { undone =>
                history = Some(undone)
                val edBefore = undone.present
                val (newEd, msg) = KeyHandler.handleDualSwar(edBefore, ch, isShifted)
                statusBar.log(msg)
                pushEditor(newEd)
              }
              lastTypedChar = '\u0000'
            else
              val (newEd, msg) = KeyHandler.handleSwarKey(ed, ch, isShifted)
              statusBar.log(msg)
              pushEditor(newEd)
              lastTypedChar = lowerCh
              lastTypedTime = now
            redraw()
      else if ch >= ' ' && ch != '`' && ch != '.' && ch != '\'' && ch != '-' then
        statusBar.log(s"✗ Unknown key '${ch}' — use s/r/g/m/p/d/n for notes, . ' ` for octave")
    }
  }
