package com.varpas.sangeet.desktop.editor

import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.{Timer, TimerTask}

import scalafx.animation.{KeyFrame, Timeline}
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.ScrollPane
import scalafx.scene.input.KeyCode
import scalafx.scene.layout.{Pane, Priority, VBox}
import scalafx.util.Duration

import com.varpas.sangeet.core.editor._
import com.varpas.sangeet.core.format.SwarFormat
import com.varpas.sangeet.core.layout.{GridLayout, LayoutConfig, SectionGrid}
import com.varpas.sangeet.core.model.{Andolan, Gamak, Gitkari, MeendDirection, _}
import com.varpas.sangeet.desktop.render.{CanvasRendererFX, SectionBounds}

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

  scrollPane.width.onChange { (_, _, _) =>
    if editor.isDefined then redraw()
  }

  // Editing mode: swar notes vs stroke pattern
  enum EditMode:
    case SwarEdit, StrokeEdit

  private var history: Option[UndoHistory]               = None
  private def editor: Option[CompositionEditor]          = history.map(_.present)
  private val config                                     = LayoutConfig()
  private[editor] var ornamentMode: Option[OrnamentMode] = None
  private var sectionBounds: List[SectionBounds]         = Nil
  private var cursorVisible: Boolean                     = true
  private var editMode: EditMode                         = EditMode.SwarEdit
  private var currentFilePath: Option[Path]              = None
  private var readOnly: Boolean                          = false

  // Script for rendering (local mutable state, replaces global DevanagariMap._script)
  private var script: SwarScript = SwarScript.Devanagari

  // Layout cache: reuse grids when composition hasn't changed (cursor-only moves)
  private var cachedGrids: Option[(Composition, List[SectionGrid])] = None

  private def getGrids(comp: Composition): List[SectionGrid] =
    cachedGrids match
      case Some((cached, grids)) if cached eq comp => grids
      case _ =>
        val grids = GridLayout.layoutAll(comp, config)
        cachedGrids = Some((comp, grids))
        grids

  // Fast-typing grouping: tracks notes being built on the same beat
  private case class GroupingState(
      beat: Int,
      cycle: Int,
      notes: List[(Note, Variant, Octave)],
      lastTypedTime: Long
  )
  private var groupingState: Option[GroupingState] = None
  private val fastTypeThresholdMs                  = 500L

  // Debounced auto-save: saves 500ms after last edit, on background thread
  private val saveExecutor = Executors.newSingleThreadExecutor { r =>
    val t = new Thread(r, "auto-save")
    t.setDaemon(true)
    t
  }
  private var saveTimer: Option[TimerTask] = None
  private val saveTimerScheduler           = new Timer("auto-save-timer", true)

  // Blink timer: toggle cursor visibility every 530ms
  private val blinkTimeline = new Timeline:
    cycleCount = Timeline.Indefinite
    keyFrames = Seq(
      KeyFrame(
        Duration(530),
        onFinished = _ =>
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
    groupingState = None
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
        if switchedSection then AppLogger.info(s"Mouse click: switching to section ${bounds.sectionIndex}")
        else AppLogger.info(s"Mouse click: cursor placed at clickX=$clickX, clickY=$clickY")
        val targetSection = ed.composition.sections(bounds.sectionIndex)
        val minBeat       = targetSection.startingBeat - 1
        val newCursor = clickedBeat match
          case Some((cycle, beat)) =>
            val clampedBeat = math.max(minBeat, math.min(beat, ed.composition.metadata.taal.matras - 1))
            ed.cursor.copy(cycle = cycle, beat = clampedBeat, subIndex = 0, totalSubdivisions = 1)
          case None if switchedSection =>
            CursorModel(ed.composition.metadata.taal).copy(beat = minBeat)
          case None =>
            ed.cursor // clicked in section but not on a cell - keep cursor

        val newEditor = ed.copy(
          currentSectionIndex = bounds.sectionIndex,
          cursor = newCursor
        )
        if switchedSection then
          val sectionName = ed.composition.sections(bounds.sectionIndex).name
          statusBar.log(s"Switched to section: $sectionName")
        else if clickedBeat.isDefined then
          statusBar.log(s"Cursor placed at cycle ${newCursor.cycle}, beat ${newCursor.beat}")

        setEditorDirect(newEditor)
        resetBlink()
        redraw()
      }
    }
  }

  /** Push a new editor state onto the undo stack and auto-save. */
  private def pushEditor(newEd: CompositionEditor): Unit =
    AppLogger.info(
      s"pushEditor: events=${newEd.currentSection.events.size}, cursor=cycle${newEd.cursor.cycle}/beat${newEd.cursor.beat}/sub${newEd.cursor.subIndex}"
    )
    history = history.map(_.push(newEd))
    autoSave()

  /** Auto-save current composition to its file path (debounced, background thread). */
  private def autoSave(): Unit =
    saveTimer.foreach(_.cancel())
    for
      ed   <- editor
      path <- currentFilePath
    do
      val comp = ed.composition
      val task = new TimerTask:
        def run(): Unit =
          saveExecutor.submit(
            new Runnable:
              def run(): Unit =
                try SwarFormat.writeFile(path, comp)
                catch case ex: Exception => AppLogger.info(s"Auto-save failed for $path: ${ex.getMessage}")
          )
      saveTimer = Some(task)
      saveTimerScheduler.schedule(task, 500L)

  /** Set editor state without undo history (for cursor-only moves). */
  private def setEditorDirect(newEd: CompositionEditor): Unit =
    AppLogger.debug(
      s"setEditorDirect: cursor=cycle${newEd.cursor.cycle}/beat${newEd.cursor.beat}/sub${newEd.cursor.subIndex}, section=${newEd.currentSectionIndex}"
    )
    history = history.map(h => h.copy(present = newEd))

  def setComposition(comp: Composition): Unit =
    AppLogger.info(
      s"Composition loaded: title=${comp.metadata.title}, sections=${comp.sections.size}, taal=${comp.metadata.taal.name}"
    )
    val ed = CompositionEditor(comp, 0, CursorModel(comp.metadata.taal))
    history = Some(UndoHistory(ed))
    editMode = EditMode.SwarEdit
    groupingState = None
    header.update(comp.metadata)
    redraw()

  def setEditor(ed: CompositionEditor): Unit =
    history = Some(UndoHistory(ed))
    editMode = EditMode.SwarEdit
    groupingState = None
    header.update(ed.composition.metadata)
    redraw()

  def getComposition: Option[Composition]  = editor.map(_.composition)
  def getEditor: Option[CompositionEditor] = editor
  def getFilePath: Option[Path]            = currentFilePath

  def setReadOnly(ro: Boolean): Unit =
    readOnly = ro
    if ro then
      blinkTimeline.stop()
      cursorVisible = false
      redraw()
    else blinkTimeline.playFromStart()

  def isReadOnly: Boolean = readOnly

  def undoHistoryInfo: (Int, Int) =
    history.map(h => (h.past.size, h.future.size)).getOrElse((0, 0))

  def isScrollPaneFocused: Boolean =
    scrollPane.delegate.isFocused

  private[editor] def pushEditorState(newEd: CompositionEditor): Unit      = pushEditor(newEd)
  private[editor] def setEditorDirectState(newEd: CompositionEditor): Unit = setEditorDirect(newEd)
  private[editor] def resetCursorBlink(): Unit                             = resetBlink()
  private[editor] def getOrnamentMode: Option[OrnamentMode]                = ornamentMode
  private[editor] def setOrnamentMode(m: Option[OrnamentMode]): Unit       = ornamentMode = m

  private[editor] def typeCharTimed(ch: Char, timestampMs: Long): String =
    val ed = editor match
      case None    => return "ERROR: no composition loaded"
      case Some(e) => e
    if isReadOnly then return "ERROR: editor is read-only"
    val isShifted = ch.isUpper
    val lowerCh   = ch.toLower
    KeyHandler.charToNote(lowerCh) match
      case None =>
        val (newEd, msg) = KeyHandler.handleSwarKey(ed, ch, isShifted)
        pushEditor(newEd)
        groupingState = None
        redraw()
        msg
      case Some(note) =>
        val variant = KeyHandler.resolveVariant(note, isShifted)
        val octave  = ed.cursor.currentOctave
        val extending = groupingState match
          case Some(gs) =>
            (timestampMs - gs.lastTypedTime) < fastTypeThresholdMs && gs.notes.size < 4
          case None => false
        if extending then
          val gs       = groupingState.get
          val newNotes = gs.notes :+ (note, variant, octave)
          history.flatMap(_.undo) match
            case Some(undone) =>
              history = Some(undone)
              val edBefore     = undone.present
              val (newEd, msg) = KeyHandler.handleSwarGroup(edBefore, newNotes)
              pushEditor(newEd)
              groupingState = Some(GroupingState(gs.beat, gs.cycle, newNotes, timestampMs))
              redraw()
              msg
            case None =>
              val (newEd, msg) = KeyHandler.handleSwarKey(ed, ch, isShifted)
              pushEditor(newEd)
              groupingState = None
              redraw()
              s"$msg (undo failed, inserted as single)"
        else
          val (newEd, msg) = KeyHandler.handleSwarKey(ed, ch, isShifted)
          pushEditor(newEd)
          groupingState = Some(
            GroupingState(
              ed.cursor.beat,
              ed.cursor.cycle,
              List((note, variant, octave)),
              timestampMs
            )
          )
          redraw()
          msg

  private val debugHandler = new DebugCommandHandler(this, statusBar)

  def debugTypeChar(ch: Char): String                     = debugHandler.typeChar(ch)
  def debugPressKey(keyName: String): String              = debugHandler.pressKey(keyName)
  def debugOctaveKey(keyName: String): String             = debugHandler.octaveKey(keyName)
  def debugSubdivision(n: Int): String                    = debugHandler.subdivision(n)
  def debugDualSwar(ch: Char): String                     = debugHandler.dualSwar(ch)
  def debugSwarGroup(chars: String): String               = debugHandler.swarGroup(chars)
  def debugTypeTimed(entries: List[(Char, Long)]): String = debugHandler.typeTimed(entries)
  def debugStroke(strokeName: String): String             = debugHandler.stroke(strokeName)
  def debugSimpleOrnament(ornamentName: String): String   = debugHandler.simpleOrnament(ornamentName)
  def debugOrnamentStart(modeName: String): String        = debugHandler.ornamentStart(modeName)
  def debugOrnamentNote(ch: Char): String                 = debugHandler.ornamentNote(ch)
  def debugFinishOrnament(): String                       = debugHandler.finishOrnament()
  def debugSwitchSection(idx: Int): String                = debugHandler.switchSection(idx)
  def debugResetComposition(compType: String = "gat", taalName: String = "teentaal", taanCount: Int = 0): String =
    debugHandler.resetComposition(compType, taalName, taanCount)

  def currentEditMode: String = editMode.toString

  def applyMetadataChange(newMeta: Metadata): Unit =
    editor.foreach { ed =>
      val oldTaal     = ed.composition.metadata.taal
      val taalChanged = newMeta.taal.name != oldTaal.name || newMeta.taal.matras != oldTaal.matras
      val newEd = if taalChanged then
        val remapped = ed.changeTaal(newMeta.taal)
        remapped.copy(composition = remapped.composition.copy(metadata = newMeta))
      else ed.copy(composition = ed.composition.copy(metadata = newMeta))
      pushEditor(newEd)
      cachedGrids = None
      header.update(newMeta)
      redraw()
    }

  def applySectionStartingBeats(beats: Map[Int, Int]): Unit =
    editor.foreach { ed =>
      val updatedSections = ed.composition.sections.zipWithIndex.map { (section, idx) =>
        beats.get(idx) match
          case Some(beat) => section.copy(startingBeat = beat)
          case None       => section
      }
      val newEd = ed.copy(composition = ed.composition.copy(sections = updatedSections))
      pushEditor(newEd)
      cachedGrids = None
      redraw()
    }

  def debugChangeTaal(taalName: String): String = debugHandler.changeTaal(taalName)

  def setFilePath(path: Path): Unit =
    currentFilePath = Some(path)

  /** Set file path and immediately save. */
  def setFilePathAndSave(path: Path): Unit =
    currentFilePath = Some(path)
    autoSave()

  def copySelection(): Unit =
    editor.foreach { ed =>
      ed.cursor.selectionRange match
        case Some((start, end)) =>
          val events = ed.eventsInRange(start, end)
          if events.isEmpty then statusBar.log("No events in selection")
          else
            import io.circe.syntax._
            import com.varpas.sangeet.core.editor.ClipboardCodecs.given
            val json    = ClipboardData(events).asJson.noSpaces
            val cb      = javafx.scene.input.Clipboard.getSystemClipboard
            val content = new javafx.scene.input.ClipboardContent()
            content.putString(json)
            cb.setContent(content)
            statusBar.log(s"Copied ${events.size} event(s)")
        case None => statusBar.log("No selection")
    }

  def cutSelection(): Unit =
    editor.foreach { ed =>
      ed.cursor.selectionRange match
        case Some((start, end)) =>
          val (newEd, events) = ed.cutRange(start, end)
          if events.isEmpty then statusBar.log("No events in selection")
          else
            import io.circe.syntax._
            import com.varpas.sangeet.core.editor.ClipboardCodecs.given
            val json    = ClipboardData(events).asJson.noSpaces
            val cb      = javafx.scene.input.Clipboard.getSystemClipboard
            val content = new javafx.scene.input.ClipboardContent()
            content.putString(json)
            cb.setContent(content)
            val cleared = newEd.copy(cursor = newEd.cursor.clearSelection)
            pushEditor(cleared)
            statusBar.log(s"Cut ${events.size} event(s)")
            redraw()
        case None => statusBar.log("No selection")
    }

  def pasteClipboard(): Unit =
    editor.foreach { ed =>
      val cb = javafx.scene.input.Clipboard.getSystemClipboard
      if cb.hasString then
        import io.circe.parser._
        import com.varpas.sangeet.core.editor.ClipboardCodecs.given
        parse(cb.getString).flatMap(_.as[ClipboardData]) match
          case Right(cd) if cd.events.nonEmpty =>
            val newEd = ed.pasteEvents(cd.events, ed.cursor.position)
            pushEditor(newEd.copy(cursor = newEd.cursor.clearSelection))
            statusBar.log(s"Pasted ${cd.events.size} event(s)")
            redraw()
          case Right(_) => statusBar.log("Clipboard is empty")
          case Left(_)  => statusBar.log("Clipboard does not contain Sangeet data")
      else statusBar.log("Clipboard is empty")
    }

  def updateHeader(meta: Metadata): Unit =
    header.update(meta)

  /** Change the rendering script and redraw. */
  def changeScript(newScript: SwarScript): Unit =
    script = newScript
    redraw()

  def currentScript: SwarScript = script

  def redraw(): Unit =
    AppLogger.debug("redraw()")
    try
      val availableWidth = (scrollPane.width.value - 2).max(800)
      if Math.abs(canvas.width.value - availableWidth) > 5 then
        canvas.width = availableWidth
        canvasHolder.prefWidth = availableWidth
      editor.foreach { ed =>
        val strokeEditMode = editMode == EditMode.StrokeEdit
        val grids          = getGrids(ed.composition)
        val cursorInfo     = if readOnly then None else Some(ed.currentSectionIndex, ed.cursor.cycle, ed.cursor.beat)
        val selRange = ed.cursor.selectionRange.map { (start, end) =>
          ((start.cycle, start.beat), (end.cycle, end.beat))
        }
        sectionBounds = CanvasRendererFX.render(
          canvas,
          ed.composition,
          grids,
          config,
          cursorInfo,
          cursorVisible,
          strokeEditMode,
          script,
          readOnly,
          selRange
        )
        val contentHeight = sectionBounds.lastOption.map(_.endY + 40).getOrElse(200.0)
        val minHeight     = scrollPane.height.value.max(400)
        val newHeight     = contentHeight.max(minHeight)
        if Math.abs(canvas.height.value - newHeight) > 10 then
          canvas.height = newHeight
          canvasHolder.prefHeight = newHeight
          sectionBounds = CanvasRendererFX.render(
            canvas,
            ed.composition,
            grids,
            config,
            cursorInfo,
            cursorVisible,
            strokeEditMode,
            script,
            readOnly,
            selRange
          )
      }
    catch
      case ex: Exception =>
        AppLogger.debug(s"redraw failed: ${ex.getMessage}")

  override def requestFocus(): Unit =
    scrollPane.requestFocus()

  // Determine if a key action is a content change (needs undo) vs cursor-only move
  private enum EditAction:
    case ContentChange(ed: CompositionEditor, msg: String)
    case CursorMove(ed: CompositionEditor, msg: String)
    case NoOp

  // Intercept Space in filter to prevent ScrollPane scroll, then handle rest insertion
  scrollPane.delegate.addEventFilter(
    javafx.scene.input.KeyEvent.KEY_PRESSED,
    (e: javafx.scene.input.KeyEvent) =>
      if KeyCode.jfxEnum2sfx(e.getCode) == KeyCode.Space then
        e.consume()
        editor.foreach { ed =>
          if !readOnly then
            groupingState = None
            val (ne, m) = KeyHandler.handleSpecialKey(ed, "SPACE")
            statusBar.log(m)
            pushEditor(ne)
            resetBlink()
            redraw()
        }
  )

  scrollPane.delegate.setOnKeyTyped { (e: javafx.scene.input.KeyEvent) =>
    if readOnly then ()
    else
      editor.foreach { ed =>
        val ch = if e.getCharacter != null && e.getCharacter.nonEmpty then e.getCharacter.charAt(0) else '\u0000'
        AppLogger.debug(s"keyTyped: char='$ch' (${ch.toInt})")

        // Stroke edit mode: only 'd' and 'r' are valid
        if editMode == EditMode.StrokeEdit then
          if ch.toLower == 'd' || ch.toLower == 'r' then
            e.consume()
            val stroke = if ch.toLower == 'd' then Stroke.Da else Stroke.Ra
            ed.setStrokeAt(ed.cursor, stroke) match
              case Some(newEd) =>
                val strokeName = if ch.toLower == 'd' then "Da" else "Ra"
                statusBar.log(s"$strokeName stroke set")
                pushEditor(newEd)
                val swarsHere = newEd.swarsAtBeat(ed.cursor.cycle, ed.cursor.beat)
                if ed.cursor.subIndex + 1 < swarsHere then
                  setEditorDirect(newEd.copy(cursor = ed.cursor.copy(subIndex = ed.cursor.subIndex + 1)))
                else
                  val next = ed.cursor.nextBeat(ed.currentSection.startingBeat)
                  if next.cycle <= newEd.maxCycle + 1 then setEditorDirect(newEd.copy(cursor = next))
                resetBlink()
                redraw()
              case None =>
                statusBar.log("No swar at this position")
        else if ch.isLetter then
          e.consume()
          val isShifted = ch.isUpper
          val now       = System.currentTimeMillis()
          ornamentMode match
            case Some(mode) =>
              val (newEd, msg, nextMode) = KeyHandler.handleNoteOrnament(ed, ch, isShifted, mode)
              statusBar.log(msg)
              if newEd ne ed then pushEditor(newEd) else setEditorDirect(newEd)
              ornamentMode = nextMode
              groupingState = None
              redraw()
            case None =>
              val lowerCh = ch.toLower
              KeyHandler.charToNote(lowerCh) match
                case None =>
                  val (newEd, msg) = KeyHandler.handleSwarKey(ed, ch, isShifted)
                  statusBar.log(msg)
                  pushEditor(newEd)
                  groupingState = None
                case Some(note) =>
                  val variant = KeyHandler.resolveVariant(note, isShifted)
                  val octave  = ed.cursor.currentOctave
                  val extending = groupingState match
                    case Some(gs) =>
                      val timeDelta = now - gs.lastTypedTime
                      timeDelta < fastTypeThresholdMs && gs.notes.size < 4
                    case None => false
                  if extending then
                    val gs       = groupingState.get
                    val newNotes = gs.notes :+ (note, variant, octave)
                    history.flatMap(_.undo) match
                      case Some(undone) =>
                        history = Some(undone)
                        val edBefore     = undone.present
                        val (newEd, msg) = KeyHandler.handleSwarGroup(edBefore, newNotes)
                        statusBar.log(msg)
                        pushEditor(newEd)
                      case None =>
                        val (newEd, msg) = KeyHandler.handleSwarKey(ed, ch, isShifted)
                        statusBar.log(msg)
                        pushEditor(newEd)
                    groupingState = Some(GroupingState(gs.beat, gs.cycle, newNotes, now))
                  else
                    val (newEd, msg) = KeyHandler.handleSwarKey(ed, ch, isShifted)
                    statusBar.log(msg)
                    pushEditor(newEd)
                    groupingState = Some(
                      GroupingState(
                        ed.cursor.beat,
                        ed.cursor.cycle,
                        List((note, variant, octave)),
                        now
                      )
                    )
              redraw()
        else if ch == '1' then
          e.consume()
          val (newEd, msg) = KeyHandler.handleChikariKey(ed)
          statusBar.log(msg)
          pushEditor(newEd)
          groupingState = None
          redraw()
        else if ch > ' ' && ch != '`' && ch != '.' && ch != '\'' && ch != '-' then
          statusBar.log(s"Unknown key '${ch}' -- use s/r/g/m/p/d/n for notes, 1 for chikari, . ' ` for octave")
      }
  }

  scrollPane.delegate.setOnKeyPressed { (e: javafx.scene.input.KeyEvent) =>
    editor.foreach { ed =>
      val code = KeyCode.jfxEnum2sfx(e.getCode)
      AppLogger.debug(s"keyPressed: code=$code, ctrl=${e.isControlDown}, meta=${e.isMetaDown}, shift=${e.isShiftDown}")

      if readOnly then
        // Read-only mode: only allow cursor navigation
        code match
          case KeyCode.Right | KeyCode.Tab =>
            e.consume()
            val next = ed.cursor.nextBeat(ed.currentSection.startingBeat)
            if next.cycle <= ed.maxCycle + 1 then
              setEditorDirect(ed.copy(cursor = next))
              resetBlink()
              redraw()
          case KeyCode.Left =>
            e.consume()
            setEditorDirect(ed.copy(cursor = ed.cursor.prevBeat(ed.currentSection.startingBeat)))
            resetBlink()
            redraw()
          case _ =>
            if code != KeyCode.Shift && code != KeyCode.Control && code != KeyCode.Alt &&
              code != KeyCode.Meta && code != KeyCode.Caps
            then statusBar.log("Sample is read-only -- use File > New to create a composition")

      // F2 toggles stroke edit mode (only when stroke line is visible)
      else if code == KeyCode.F2 then
        e.consume()
        if ed.composition.metadata.showStrokeLine then
          editMode = editMode match
            case EditMode.SwarEdit =>
              statusBar.log("Stroke edit mode -- d=Da, r=Ra, Backspace=clear, Escape/F2=exit")
              EditMode.StrokeEdit
            case EditMode.StrokeEdit =>
              statusBar.log("Swar edit mode")
              EditMode.SwarEdit
          resetBlink()
          redraw()
        else statusBar.log("Enable 'Show Da/Ra stroke indicators' first")

      // In stroke edit mode, Escape returns to swar edit
      else if editMode == EditMode.StrokeEdit && code == KeyCode.Escape then
        e.consume()
        editMode = EditMode.SwarEdit
        statusBar.log("Swar edit mode")
        resetBlink()
        redraw()

      // Stroke edit mode: arrow navigation through swar positions, Backspace clears
      else if editMode == EditMode.StrokeEdit && !e.isControlDown && !e.isMetaDown then
        code match
          case KeyCode.Right | KeyCode.Tab =>
            e.consume()
            val swarsHere = ed.swarsAtBeat(ed.cursor.cycle, ed.cursor.beat)
            val newCursor =
              if ed.cursor.subIndex + 1 < swarsHere then ed.cursor.copy(subIndex = ed.cursor.subIndex + 1)
              else
                val next = ed.cursor.nextBeat(ed.currentSection.startingBeat)
                if next.cycle <= ed.maxCycle + 1 then next else ed.cursor
            setEditorDirect(ed.copy(cursor = newCursor))
            resetBlink()
            redraw()
          case KeyCode.Left =>
            e.consume()
            if ed.cursor.subIndex > 0 then
              setEditorDirect(ed.copy(cursor = ed.cursor.copy(subIndex = ed.cursor.subIndex - 1)))
            else
              val prev = ed.cursor.prevBeat(ed.currentSection.startingBeat)
              // Set subIndex to last swar at the previous beat
              val swarsAtPrev = ed.swarsAtBeat(prev.cycle, prev.beat)
              val newCursor   = prev.copy(subIndex = math.max(0, swarsAtPrev - 1))
              setEditorDirect(ed.copy(cursor = newCursor))
            resetBlink()
            redraw()
          case KeyCode.BackSpace | KeyCode.Delete =>
            e.consume()
            ed.clearStrokeAt(ed.cursor) match
              case Some(newEd) =>
                statusBar.log("Stroke cleared (will use auto Da/Ra)")
                pushEditor(newEd)
                resetBlink()
                redraw()
              case None =>
                statusBar.log("No swar at this position")
          case _ => () // other keys ignored in stroke mode

      // Handle undo/redo first
      else if (e.isControlDown || e.isMetaDown) && code == KeyCode.Z then
        e.consume()
        groupingState = None
        if e.isShiftDown then
          // Redo
          history.flatMap(_.redo).foreach { newHist =>
            history = Some(newHist)
            header.update(newHist.present.composition.metadata)
            statusBar.log("Redo")
            resetBlink()
            redraw()
          }
        else
          // Undo
          history.flatMap(_.undo).foreach { newHist =>
            history = Some(newHist)
            header.update(newHist.present.composition.metadata)
            statusBar.log("Undo")
            resetBlink()
            redraw()
          }
      else
        val action =
          if e.isControlDown || e.isMetaDown then
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
                EditAction.CursorMove(ed, "Kan Swar mode -- type a note for the grace note")
              case KeyCode.H =>
                e.consume()
                ornamentMode = Some(OrnamentMode.Sparsh)
                EditAction.CursorMove(ed, "Sparsh mode -- type a note for the touch note")
              case KeyCode.E =>
                e.consume()
                ornamentMode = Some(OrnamentMode.Ghaseet)
                EditAction.CursorMove(ed, "Ghaseet mode -- type a note for the target note")
              case KeyCode.M =>
                e.consume()
                if e.isShiftDown then
                  ornamentMode = Some(OrnamentMode.MeendStart(MeendDirection.Descending))
                  EditAction.CursorMove(ed, "Meend (descending) -- type the start note")
                else
                  ornamentMode = Some(OrnamentMode.MeendStart(MeendDirection.Ascending))
                  EditAction.CursorMove(ed, "Meend (ascending) -- type the start note")
              case KeyCode.J =>
                e.consume()
                ornamentMode = Some(OrnamentMode.KrintanStart)
                EditAction.CursorMove(ed, "Krintan mode -- type the start note")
              case KeyCode.U =>
                e.consume()
                ornamentMode = Some(OrnamentMode.MurkiCollect(Nil))
                EditAction.CursorMove(ed, "Murki mode -- type notes, then press Enter to finish")
              case KeyCode.W =>
                e.consume()
                ornamentMode = Some(OrnamentMode.ZamzamaCollect(Nil))
                EditAction.CursorMove(ed, "Zamzama mode -- type notes, then press Enter to finish")
              case KeyCode.C =>
                e.consume()
                ed.cursor.selectionRange match
                  case Some((start, end)) =>
                    val events = ed.eventsInRange(start, end)
                    if events.isEmpty then EditAction.CursorMove(ed, "No events in selection")
                    else
                      import io.circe.syntax._
                      import com.varpas.sangeet.core.editor.ClipboardCodecs.given
                      val json    = ClipboardData(events).asJson.noSpaces
                      val cb      = javafx.scene.input.Clipboard.getSystemClipboard
                      val content = new javafx.scene.input.ClipboardContent()
                      content.putString(json)
                      cb.setContent(content)
                      EditAction.CursorMove(ed, s"Copied ${events.size} event(s)")
                  case None => EditAction.CursorMove(ed, "No selection")
              case KeyCode.X =>
                e.consume()
                ed.cursor.selectionRange match
                  case Some((start, end)) =>
                    val (newEd, events) = ed.cutRange(start, end)
                    if events.isEmpty then EditAction.CursorMove(ed, "No events in selection")
                    else
                      import io.circe.syntax._
                      import com.varpas.sangeet.core.editor.ClipboardCodecs.given
                      val json    = ClipboardData(events).asJson.noSpaces
                      val cb      = javafx.scene.input.Clipboard.getSystemClipboard
                      val content = new javafx.scene.input.ClipboardContent()
                      content.putString(json)
                      cb.setContent(content)
                      val cleared = newEd.copy(cursor = newEd.cursor.clearSelection)
                      EditAction.ContentChange(cleared, s"Cut ${events.size} event(s)")
                  case None => EditAction.CursorMove(ed, "No selection")
              case KeyCode.V =>
                e.consume()
                val cb = javafx.scene.input.Clipboard.getSystemClipboard
                if cb.hasString then
                  import io.circe.parser._
                  import com.varpas.sangeet.core.editor.ClipboardCodecs.given
                  parse(cb.getString).flatMap(_.as[ClipboardData]) match
                    case Right(cd) if cd.events.nonEmpty =>
                      val newEd = ed.pasteEvents(cd.events, ed.cursor.position)
                      EditAction.ContentChange(
                        newEd.copy(cursor = newEd.cursor.clearSelection),
                        s"Pasted ${cd.events.size} event(s)"
                      )
                    case Right(_) => EditAction.CursorMove(ed, "Clipboard is empty")
                    case Left(_)  => EditAction.CursorMove(ed, "Clipboard does not contain Sangeet data")
                else EditAction.CursorMove(ed, "Clipboard is empty")
              case KeyCode.Digit2 | KeyCode.Numpad2 =>
                e.consume()
                EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 2), "Subdivision: 2 per beat")
              case KeyCode.Digit3 | KeyCode.Numpad3 =>
                e.consume()
                EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 3), "Subdivision: 3 per beat")
              case KeyCode.Digit4 | KeyCode.Numpad4 =>
                e.consume()
                EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 4), "Subdivision: 4 per beat")
              case KeyCode.Digit5 | KeyCode.Numpad5 =>
                e.consume()
                EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 5), "Subdivision: 5 per beat")
              case KeyCode.Digit6 | KeyCode.Numpad6 =>
                e.consume()
                EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 6), "Subdivision: 6 per beat")
              case KeyCode.Digit7 | KeyCode.Numpad7 =>
                e.consume()
                EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 7), "Subdivision: 7 per beat")
              case KeyCode.Digit8 | KeyCode.Numpad8 =>
                e.consume()
                EditAction.CursorMove(KeyHandler.handleSubdivision(ed, 8), "Subdivision: 8 per beat")
              case _ => EditAction.NoOp
          else if e.isShiftDown then
            code match
              case KeyCode.Right =>
                e.consume()
                val sel             = ed.cursor.selectNextBeat(ed.currentSection.startingBeat)
                val maxAllowedCycle = ed.maxCycle + 1
                if sel.cycle > maxAllowedCycle then EditAction.NoOp
                else EditAction.CursorMove(ed.copy(cursor = sel), "Selection extended right")
              case KeyCode.Left =>
                e.consume()
                EditAction.CursorMove(
                  ed.copy(cursor = ed.cursor.selectPrevBeat(ed.currentSection.startingBeat)),
                  "Selection extended left"
                )
              case KeyCode.Home =>
                e.consume()
                EditAction.CursorMove(
                  ed.copy(cursor = ed.cursor.selectToStart(ed.currentSection.startingBeat)),
                  "Selection extended to start"
                )
              case KeyCode.End =>
                e.consume()
                EditAction.CursorMove(ed.copy(cursor = ed.cursor.selectToEnd(ed.maxCycle)), "Selection extended to end")
              case _ => EditAction.NoOp
          else
            code match
              case KeyCode.Right =>
                e.consume()
                val next            = ed.cursor.nextBeat(ed.currentSection.startingBeat)
                val maxAllowedCycle = ed.maxCycle + 1
                if next.cycle > maxAllowedCycle then EditAction.NoOp
                else EditAction.CursorMove(ed.copy(cursor = next), "Cursor forward")
              case KeyCode.Left =>
                e.consume()
                EditAction.CursorMove(
                  ed.copy(cursor = ed.cursor.prevBeat(ed.currentSection.startingBeat)),
                  "Cursor back"
                )
              case KeyCode.Tab =>
                e.consume()
                val next            = ed.cursor.nextBeat(ed.currentSection.startingBeat)
                val maxAllowedCycle = ed.maxCycle + 1
                if next.cycle > maxAllowedCycle then EditAction.NoOp
                else EditAction.CursorMove(ed.copy(cursor = next), "Cursor forward")
              // Space is handled in the event filter (to prevent ScrollPane scrolling)
              case KeyCode.Minus =>
                e.consume()
                val (ne, m) = KeyHandler.handleSpecialKey(ed, "MINUS")
                EditAction.ContentChange(ne, m)
              case KeyCode.BackSpace =>
                e.consume()
                val (ne, m) = KeyHandler.handleSpecialKey(ed, "BACKSPACE")
                EditAction.ContentChange(ne, m)
              case KeyCode.Delete =>
                e.consume()
                val (ne, m) = KeyHandler.handleSpecialKey(ed, "DELETE")
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
                EditAction.CursorMove(ed, "Ornament mode cancelled")
              case KeyCode.Enter =>
                e.consume()
                ornamentMode match
                  case Some(mode @ (OrnamentMode.MurkiCollect(_) | OrnamentMode.ZamzamaCollect(_))) =>
                    val (newEd, msg) = KeyHandler.finishMultiNoteOrnament(ed, mode)
                    ornamentMode = None
                    EditAction.ContentChange(newEd, msg)
                  case None =>
                    // Enter in normal mode: advance to next cycle
                    val newCursor =
                      ed.cursor.copy(beat = 0, cycle = ed.cursor.cycle + 1, subIndex = 0, totalSubdivisions = 1)
                    val maxAllowedCycle = ed.maxCycle + 1
                    if newCursor.cycle > maxAllowedCycle then EditAction.NoOp
                    else EditAction.CursorMove(ed.copy(cursor = newCursor), "Next cycle")
                  case _ => EditAction.NoOp
              case _ => EditAction.NoOp

        action match
          case EditAction.ContentChange(newEd, msg) =>
            groupingState = None
            if msg.nonEmpty then statusBar.log(msg)
            pushEditor(newEd)
            resetBlink()
            redraw()
          case EditAction.CursorMove(newEd, msg) =>
            groupingState = None
            if msg.nonEmpty then statusBar.log(msg)
            setEditorDirect(newEd)
            resetBlink()
            redraw()
          case EditAction.NoOp => ()
    }
  }
