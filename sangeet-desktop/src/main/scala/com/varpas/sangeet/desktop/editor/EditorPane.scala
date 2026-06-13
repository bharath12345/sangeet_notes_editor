package com.varpas.sangeet.desktop.editor

import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.{Timer, TimerTask}

import scalafx.animation.{KeyFrame, Timeline}
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.ScrollPane
import scalafx.scene.layout.{Pane, Priority, VBox}
import scalafx.util.Duration

import com.varpas.sangeet.core.editor._
import com.varpas.sangeet.core.format.SwarFormat
import com.varpas.sangeet.core.layout.{GridLayout, LayoutConfig, SectionGrid}
import com.varpas.sangeet.core.model._
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

  private var history: Option[UndoHistory]               = None
  private def editor: Option[CompositionEditor]          = history.map(_.present)
  private val config                                     = LayoutConfig()
  private[editor] var ornamentMode: Option[OrnamentMode] = None
  private var sectionBounds: List[SectionBounds]         = Nil
  private var cursorVisible: Boolean                     = true
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
    keyHandler.clearGrouping()
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
        val newCursor = clickedBeat match
          case Some((cycle, beat)) =>
            val minBeat     = if cycle == 0 then targetSection.startingBeat - 1 else 0
            val clampedBeat = math.max(minBeat, math.min(beat, ed.composition.metadata.taal.matras - 1))
            ed.cursor.copy(cycle = cycle, beat = clampedBeat, subIndex = 0, totalSubdivisions = 1)
          case None if switchedSection =>
            CursorModel(ed.composition.metadata.taal).copy(beat = targetSection.startingBeat - 1)
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
    keyHandler.resetEditMode()
    header.update(comp.metadata)
    redraw()

  def setEditor(ed: CompositionEditor): Unit =
    history = Some(UndoHistory(ed))
    keyHandler.resetEditMode()
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
  private[editor] def getHistory: Option[UndoHistory]                      = history
  private[editor] def setHistory(h: Option[UndoHistory]): Unit             = history = h

  private[editor] val keyHandler = new EditorKeyHandler(this, statusBar)

  private[editor] def typeCharTimed(ch: Char, timestampMs: Long): String =
    keyHandler.typeCharTimed(ch, timestampMs)

  def currentEditMode: String = keyHandler.currentEditMode

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
      val matras = ed.composition.metadata.taal.matras
      var comp   = ed.composition
      beats.foreach { (idx, newBeat) =>
        if idx >= 0 && idx < comp.sections.length then
          val updated = CompositionEditor.changeStartingBeat(comp.sections(idx), newBeat, matras)
          comp = comp.copy(sections = comp.sections.updated(idx, updated))
      }
      val newEd = ed.copy(composition = comp)
      pushEditor(newEd)
      cachedGrids = None
      redraw()
    }

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
        val strokeEditMode = keyHandler.isStrokeEditMode
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

  keyHandler.install(scrollPane)
