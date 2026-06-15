package com.varpas.sangeet.desktop.editor

import scalafx.scene.control.ScrollPane
import scalafx.scene.input.KeyCode

import com.varpas.sangeet.core.editor._
import com.varpas.sangeet.core.model.{Andolan, Gamak, Gitkari, MeendDirection, _}
import com.varpas.sangeet.desktop.metrics.AppMetricEvents

class EditorKeyHandler(pane: EditorPane, statusBar: StatusBar):

  enum EditMode:
    case SwarEdit, StrokeEdit

  // Fast-typing grouping state, cursor-bound checks, and the 500ms threshold all live in
  // `sangeet-core.editor.GroupingFSM` / `CursorBounds`. This class is the JavaFX glue: it
  // observes keystrokes, asks the FSM what to do, and performs the resulting edit. See
  // `GroupingFSM.scala` for the canonical decision tree; `GroupingFSMSpec` for the rules.

  private[editor] var editMode: EditMode               = EditMode.SwarEdit
  private var groupingState: Option[GroupingFSM.State] = None

  def isStrokeEditMode: Boolean             = editMode == EditMode.StrokeEdit
  def currentEditMode: String               = editMode.toString
  private[editor] def clearGrouping(): Unit = groupingState = None
  private[editor] def resetEditMode(): Unit =
    editMode = EditMode.SwarEdit
    groupingState = None

  private enum EditAction:
    case ContentChange(ed: CompositionEditor, msg: String)
    case CursorMove(ed: CompositionEditor, msg: String)
    case NoOp

  def install(scrollPane: ScrollPane): Unit =
    scrollPane.delegate.addEventFilter(
      javafx.scene.input.KeyEvent.KEY_PRESSED,
      (e: javafx.scene.input.KeyEvent) =>
        // Bug-report diagnostics: record every key press into the rolling
        // buffer regardless of where the handler ends up dispatching it.
        // The user only sees this data if they explicitly click Report Bug.
        com.varpas.sangeet.desktop.diagnostics.EventLogger.recordKey(
          code = Option(e.getCode).map(_.getName).getOrElse("unknown"),
          modifiers = List(
            if e.isShiftDown then Some("Shift") else None,
            if e.isControlDown then Some("Ctrl") else None,
            if e.isAltDown then Some("Alt") else None,
            if e.isMetaDown then Some("Meta") else None
          ).flatten
        )

        if KeyCode.jfxEnum2sfx(e.getCode) == KeyCode.Space then
          e.consume()
          pane.getEditor.foreach { ed =>
            if !pane.isReadOnly then
              groupingState = None
              val (ne, m) = KeyHandler.handleSpecialKey(ed, "SPACE")
              statusBar.log(m)
              pane.pushEditorState(ne)
              AppMetricEvents.mutationSwarInsert()
              pane.resetCursorBlink()
              pane.redraw()
          }
    )

    scrollPane.delegate.setOnKeyTyped { (e: javafx.scene.input.KeyEvent) =>
      if !pane.isReadOnly then
        pane.getEditor.foreach { ed =>
          // Phase 10: count each typed-character event into the session-scoped swar
          // input counter. Aggregated, flushed once on AppQuit so we don't blow
          // PostHog's per-event quota with 5000 captures per session.
          com.varpas.sangeet.desktop.diagnostics.SessionStats.incrementSwarInput()
          handleKeyTyped(e, ed)
        }
    }

    scrollPane.delegate.setOnKeyPressed { (e: javafx.scene.input.KeyEvent) =>
      pane.getEditor.foreach { ed =>
        handleKeyPressed(e, ed)
      }
    }

  private[editor] def typeCharTimed(ch: Char, timestampMs: Long): String =
    val ed = pane.getEditor match
      case None    => return "ERROR: no composition loaded"
      case Some(e) => e
    if pane.isReadOnly then return "ERROR: editor is read-only"
    val isShifted = ch.isUpper
    val lowerCh   = ch.toLower
    val msg = KeyHandler.charToNote(lowerCh) match
      case None =>
        val (newEd, m) = KeyHandler.handleSwarKey(ed, ch, isShifted)
        pane.pushEditorState(newEd)
        AppMetricEvents.mutationSwarInsert()
        groupingState = None
        m
      case Some(note) =>
        applySwarWithGrouping(ed, ch, note, isShifted, timestampMs)
    pane.redraw()
    msg

  /** Shared swar-input path used by both the interactive typing handler (`handleKeyTyped`) and the debug-bridge
    * timed-typing entry point (`typeCharTimed`). Asks `GroupingFSM` for the decision and performs the corresponding
    * insert (start-new vs. undo-and-replay-as-group). Updates `groupingState` in place. Caller is responsible for
    * `pane.redraw()` and any cursor-blink reset.
    *
    * Plan 18 PR-3b: every successful insert path here is a swar mutation — count it. The Extend branch issues exactly
    * one increment per keystroke (replacing the prior single insert with the regrouped one is still one user-visible
    * mutation), matching the web side's `applySwarInsertWithGrouping` behavior.
    */
  private def applySwarWithGrouping(
      ed: CompositionEditor,
      ch: Char,
      note: Note,
      isShifted: Boolean,
      nowMs: Long
  ): String =
    val variant  = KeyHandler.resolveVariant(note, isShifted)
    val octave   = ed.cursor.currentOctave
    val thisNote = (note, variant, octave)
    val observed = GroupingFSM.CursorTriple.of(ed.cursor)
    GroupingFSM.decide(groupingState, nowMs, observed, thisNote) match
      case GroupingFSM.Decision.Extend(allNotes) =>
        // Replay-as-group path: undo the last insert, replay handleSwarGroup with the full
        // note list at the group's original beat. If undo fails (shouldn't, but be defensive)
        // we fall back to a single insert.
        val gs = groupingState.get
        pane.getHistory.flatMap(_.undo) match
          case Some(undone) =>
            pane.setHistory(Some(undone))
            val edBefore     = undone.present
            val (newEd, msg) = KeyHandler.handleSwarGroup(edBefore, allNotes)
            pane.pushEditorState(newEd)
            AppMetricEvents.mutationSwarInsert()
            groupingState = Some(
              GroupingFSM.extendedState(gs, allNotes, nowMs, GroupingFSM.CursorTriple.of(newEd.cursor))
            )
            msg
          case None =>
            val (newEd, msg) = KeyHandler.handleSwarKey(ed, ch, isShifted)
            pane.pushEditorState(newEd)
            AppMetricEvents.mutationSwarInsert()
            groupingState = None
            s"$msg (undo failed, inserted as single)"
      case GroupingFSM.Decision.StartNew =>
        val (newEd, msg) = KeyHandler.handleSwarKey(ed, ch, isShifted)
        pane.pushEditorState(newEd)
        AppMetricEvents.mutationSwarInsert()
        groupingState = Some(
          GroupingFSM.startedState(
            preInsertCursor = observed,
            thisNote = thisNote,
            nowMs = nowMs,
            postInsertCursor = GroupingFSM.CursorTriple.of(newEd.cursor)
          )
        )
        msg

  private def handleKeyTyped(e: javafx.scene.input.KeyEvent, ed: CompositionEditor): Unit =
    val ch = if e.getCharacter != null && e.getCharacter.nonEmpty then e.getCharacter.charAt(0) else ' '
    AppLogger.debug(s"keyTyped: char='$ch' (${ch.toInt})")

    if editMode == EditMode.StrokeEdit then
      if ch.toLower == 'd' || ch.toLower == 'r' || ch.toLower == 'j' then
        e.consume()
        val stroke = ch.toLower match
          case 'd' => Stroke.Da
          case 'r' => Stroke.Ra
          case _   => Stroke.Jod
        ed.setStrokeAt(ed.cursor, stroke) match
          case Some(newEd) =>
            val strokeName = ch.toLower match
              case 'd' => "Da"
              case 'r' => "Ra"
              case _   => "Jod"
            statusBar.log(s"$strokeName stroke set")
            pane.pushEditorState(newEd)
            val swarsHere = newEd.swarsAtBeat(ed.cursor.cycle, ed.cursor.beat)
            if ed.cursor.subIndex + 1 < swarsHere then
              pane.setEditorDirectState(newEd.copy(cursor = ed.cursor.copy(subIndex = ed.cursor.subIndex + 1)))
            else
              val next = ed.cursor.nextBeat(ed.currentSection.startingBeat)
              if CursorBounds.canAdvanceTo(next.cycle, newEd.maxCycle) then
                pane.setEditorDirectState(newEd.copy(cursor = next))
            pane.resetCursorBlink()
            pane.redraw()
          case None =>
            statusBar.log("No swar at this position")
    else if ch.isLetter then
      e.consume()
      val isShifted = ch.isUpper
      val now       = System.currentTimeMillis()
      pane.getOrnamentMode match
        case Some(mode) =>
          val (newEd, msg, nextMode) = KeyHandler.handleNoteOrnament(ed, ch, isShifted, mode)
          statusBar.log(msg)
          if newEd ne ed then pane.pushEditorState(newEd) else pane.setEditorDirectState(newEd)
          // Plan 18 PR-3b: if the ornament-input branch produced a new editor state AND advanced
          // out of ornament mode (nextMode is None), that was the user "finishing" a single-note
          // ornament (KanSwar/Sparsh/Ghaseet/Meend/Krintan). Mirrors the web's applyOrnamentAction.
          if (newEd ne ed) && nextMode.isEmpty then
            AppMetricEvents.mutationOrnamentFinish()
            AppMetricEvents.ornamentFinish(ornamentTypeOf(mode))
          pane.setOrnamentMode(nextMode)
          groupingState = None
          pane.redraw()
        case None =>
          val lowerCh = ch.toLower
          KeyHandler.charToNote(lowerCh) match
            case None =>
              val (newEd, msg) = KeyHandler.handleSwarKey(ed, ch, isShifted)
              statusBar.log(msg)
              pane.pushEditorState(newEd)
              // Non-note swar key (e.g. minus / chikari char arriving here): still a mutation, count it.
              AppMetricEvents.mutationSwarInsert()
              groupingState = None
            case Some(note) =>
              // Decision tree (start-new vs. undo-and-replay-as-group) lives in
              // sangeet-core.editor.GroupingFSM. See GroupingFSMSpec. Metric increment
              // happens inside applySwarWithGrouping per insert.
              val msg = applySwarWithGrouping(ed, ch, note, isShifted, now)
              statusBar.log(msg)
          pane.redraw()
    else if ch == '1' then
      e.consume()
      val (newEd, msg) = KeyHandler.handleChikariKey(ed)
      statusBar.log(msg)
      pane.pushEditorState(newEd)
      AppMetricEvents.mutationSwarInsert()
      groupingState = None
      pane.redraw()
    else if ch > ' ' && ch != '`' && ch != '.' && ch != '\'' && ch != '-' then
      statusBar.log(s"Unknown key '${ch}' -- use s/r/g/m/p/d/n for notes, 1 for chikari, . ' ` for octave")

  private def handleKeyPressed(e: javafx.scene.input.KeyEvent, ed: CompositionEditor): Unit =
    val code = KeyCode.jfxEnum2sfx(e.getCode)
    AppLogger.debug(s"keyPressed: code=$code, ctrl=${e.isControlDown}, meta=${e.isMetaDown}, shift=${e.isShiftDown}")

    if pane.isReadOnly then
      code match
        case KeyCode.Right | KeyCode.Tab =>
          e.consume()
          val next = ed.cursor.nextBeat(ed.currentSection.startingBeat)
          if CursorBounds.canAdvanceTo(next.cycle, ed.maxCycle) then
            pane.setEditorDirectState(ed.copy(cursor = next))
            pane.resetCursorBlink()
            pane.redraw()
        case KeyCode.Left =>
          e.consume()
          pane.setEditorDirectState(ed.copy(cursor = ed.cursor.prevBeat(ed.currentSection.startingBeat)))
          pane.resetCursorBlink()
          pane.redraw()
        case _ =>
          if code != KeyCode.Shift && code != KeyCode.Control && code != KeyCode.Alt &&
            code != KeyCode.Meta && code != KeyCode.Caps
          then statusBar.log("Sample is read-only -- use File > New to create a composition")
    else if code == KeyCode.F2 then
      e.consume()
      if ed.composition.metadata.showStrokeLine then
        editMode = editMode match
          case EditMode.SwarEdit =>
            statusBar.log("Stroke edit mode -- d=Da, r=Ra, j=Jod, Backspace=clear, Escape/F2=exit")
            EditMode.StrokeEdit
          case EditMode.StrokeEdit =>
            statusBar.log("Swar edit mode")
            EditMode.SwarEdit
        pane.resetCursorBlink()
        pane.redraw()
      else statusBar.log("Enable 'Show Da/Ra stroke indicators' first")
    else if editMode == EditMode.StrokeEdit && code == KeyCode.Escape then
      e.consume()
      editMode = EditMode.SwarEdit
      statusBar.log("Swar edit mode")
      pane.resetCursorBlink()
      pane.redraw()
    else if editMode == EditMode.StrokeEdit && !e.isControlDown && !e.isMetaDown then
      code match
        case KeyCode.Right | KeyCode.Tab =>
          e.consume()
          val swarsHere = ed.swarsAtBeat(ed.cursor.cycle, ed.cursor.beat)
          val newCursor =
            if ed.cursor.subIndex + 1 < swarsHere then ed.cursor.copy(subIndex = ed.cursor.subIndex + 1)
            else
              val next = ed.cursor.nextBeat(ed.currentSection.startingBeat)
              if CursorBounds.canAdvanceTo(next.cycle, ed.maxCycle) then next else ed.cursor
          pane.setEditorDirectState(ed.copy(cursor = newCursor))
          pane.resetCursorBlink()
          pane.redraw()
        case KeyCode.Left =>
          e.consume()
          if ed.cursor.subIndex > 0 then
            pane.setEditorDirectState(ed.copy(cursor = ed.cursor.copy(subIndex = ed.cursor.subIndex - 1)))
          else
            val prev        = ed.cursor.prevBeat(ed.currentSection.startingBeat)
            val swarsAtPrev = ed.swarsAtBeat(prev.cycle, prev.beat)
            val newCursor   = prev.copy(subIndex = math.max(0, swarsAtPrev - 1))
            pane.setEditorDirectState(ed.copy(cursor = newCursor))
          pane.resetCursorBlink()
          pane.redraw()
        case KeyCode.BackSpace | KeyCode.Delete =>
          e.consume()
          ed.clearStrokeAt(ed.cursor) match
            case Some(newEd) =>
              statusBar.log("Stroke cleared (will use auto Da/Ra)")
              pane.pushEditorState(newEd)
              pane.resetCursorBlink()
              pane.redraw()
            case None =>
              statusBar.log("No swar at this position")
        case _ => ()
    else if (e.isControlDown || e.isMetaDown) && code == KeyCode.Z then
      e.consume()
      groupingState = None
      if e.isShiftDown then
        pane.getHistory.flatMap(_.redo).foreach { newHist =>
          pane.setHistory(Some(newHist))
          pane.updateHeader(newHist.present.composition.metadata)
          // Plan 18 PR-3b: count actual redo applications, not redo keypresses
          // (the .foreach branch only fires when redo had something to redo).
          AppMetricEvents.mutationRedo()
          statusBar.log("Redo")
          pane.resetCursorBlink()
          pane.redraw()
        }
      else
        pane.getHistory.flatMap(_.undo).foreach { newHist =>
          pane.setHistory(Some(newHist))
          pane.updateHeader(newHist.present.composition.metadata)
          AppMetricEvents.mutationUndo()
          statusBar.log("Undo")
          pane.resetCursorBlink()
          pane.redraw()
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
              AppMetricEvents.mutationOrnamentFinish()
              AppMetricEvents.ornamentFinish("Gamak")
              EditAction.ContentChange(ne, m)
            case KeyCode.A =>
              e.consume()
              val (ne, m) = KeyHandler.handleSimpleOrnament(ed, Andolan(), "Andolan")
              AppMetricEvents.mutationOrnamentFinish()
              AppMetricEvents.ornamentFinish("Andolan")
              EditAction.ContentChange(ne, m)
            case KeyCode.I =>
              e.consume()
              val (ne, m) = KeyHandler.handleSimpleOrnament(ed, Gitkari(), "Gitkari")
              AppMetricEvents.mutationOrnamentFinish()
              AppMetricEvents.ornamentFinish("Gitkari")
              EditAction.ContentChange(ne, m)
            case KeyCode.K =>
              e.consume()
              pane.setOrnamentMode(Some(OrnamentMode.KanSwar))
              EditAction.CursorMove(ed, "Kan Swar mode -- type a note for the grace note")
            case KeyCode.H =>
              e.consume()
              pane.setOrnamentMode(Some(OrnamentMode.Sparsh))
              EditAction.CursorMove(ed, "Sparsh mode -- type a note for the touch note")
            case KeyCode.E =>
              e.consume()
              pane.setOrnamentMode(Some(OrnamentMode.Ghaseet))
              EditAction.CursorMove(ed, "Ghaseet mode -- type a note for the target note")
            case KeyCode.M =>
              e.consume()
              if e.isShiftDown then
                pane.setOrnamentMode(Some(OrnamentMode.MeendStart(MeendDirection.Descending)))
                EditAction.CursorMove(ed, "Meend (descending) -- type the start note")
              else
                pane.setOrnamentMode(Some(OrnamentMode.MeendStart(MeendDirection.Ascending)))
                EditAction.CursorMove(ed, "Meend (ascending) -- type the start note")
            case KeyCode.J =>
              e.consume()
              pane.setOrnamentMode(Some(OrnamentMode.KrintanStart))
              EditAction.CursorMove(ed, "Krintan mode -- type the start note")
            case KeyCode.U =>
              e.consume()
              pane.setOrnamentMode(Some(OrnamentMode.MurkiCollect(Nil)))
              EditAction.CursorMove(ed, "Murki mode -- type notes, then press Enter to finish")
            case KeyCode.W =>
              e.consume()
              pane.setOrnamentMode(Some(OrnamentMode.ZamzamaCollect(Nil)))
              EditAction.CursorMove(ed, "Zamzama mode -- type notes, then press Enter to finish")
            case KeyCode.C =>
              e.consume()
              pane.copySelection()
              groupingState = None
              pane.resetCursorBlink()
              EditAction.NoOp
            case KeyCode.X =>
              e.consume()
              pane.cutSelection()
              groupingState = None
              pane.resetCursorBlink()
              EditAction.NoOp
            case KeyCode.V =>
              e.consume()
              pane.pasteClipboard()
              groupingState = None
              pane.resetCursorBlink()
              EditAction.NoOp
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
              val sel = ed.cursor.selectNextBeat(ed.currentSection.startingBeat)
              if !CursorBounds.canAdvanceTo(sel.cycle, ed.maxCycle) then EditAction.NoOp
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
              val next = ed.cursor.nextBeat(ed.currentSection.startingBeat)
              if !CursorBounds.canAdvanceTo(next.cycle, ed.maxCycle) then EditAction.NoOp
              else EditAction.CursorMove(ed.copy(cursor = next), "Cursor forward")
            case KeyCode.Left =>
              e.consume()
              EditAction.CursorMove(
                ed.copy(cursor = ed.cursor.prevBeat(ed.currentSection.startingBeat)),
                "Cursor back"
              )
            case KeyCode.Tab =>
              e.consume()
              val next = ed.cursor.nextBeat(ed.currentSection.startingBeat)
              if !CursorBounds.canAdvanceTo(next.cycle, ed.maxCycle) then EditAction.NoOp
              else EditAction.CursorMove(ed.copy(cursor = next), "Cursor forward")
            case KeyCode.Minus =>
              e.consume()
              val (ne, m) = KeyHandler.handleSpecialKey(ed, "MINUS")
              EditAction.ContentChange(ne, m)
            case KeyCode.BackSpace =>
              e.consume()
              val (ne, m) = KeyHandler.handleSpecialKey(ed, "BACKSPACE")
              // Plan 18 PR-3b: count deletes (BackSpace + Delete share the same intent).
              AppMetricEvents.mutationDelete()
              EditAction.ContentChange(ne, m)
            case KeyCode.Delete =>
              e.consume()
              val (ne, m) = KeyHandler.handleSpecialKey(ed, "DELETE")
              AppMetricEvents.mutationDelete()
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
              pane.setOrnamentMode(None)
              EditAction.CursorMove(ed, "Ornament mode cancelled")
            case KeyCode.Enter =>
              e.consume()
              pane.getOrnamentMode match
                case Some(mode @ (OrnamentMode.MurkiCollect(_) | OrnamentMode.ZamzamaCollect(_))) =>
                  val (newEd, msg) = KeyHandler.finishMultiNoteOrnament(ed, mode)
                  pane.setOrnamentMode(None)
                  // Plan 18 PR-3b: bucket Murki/Zamzama under "custom" per AppMetricEvents.ornamentFinish.
                  AppMetricEvents.mutationOrnamentFinish()
                  AppMetricEvents.ornamentFinish(ornamentTypeOf(mode))
                  EditAction.ContentChange(newEd, msg)
                case None =>
                  val newCursor =
                    ed.cursor.copy(beat = 0, cycle = ed.cursor.cycle + 1, subIndex = 0, totalSubdivisions = 1)
                  if !CursorBounds.canAdvanceTo(newCursor.cycle, ed.maxCycle) then EditAction.NoOp
                  else EditAction.CursorMove(ed.copy(cursor = newCursor), "Next cycle")
                case _ => EditAction.NoOp
            case _ => EditAction.NoOp

      action match
        case EditAction.ContentChange(newEd, msg) =>
          groupingState = None
          if msg.nonEmpty then statusBar.log(msg)
          pane.pushEditorState(newEd)
          pane.resetCursorBlink()
          pane.redraw()
        case EditAction.CursorMove(newEd, msg) =>
          groupingState = None
          if msg.nonEmpty then statusBar.log(msg)
          pane.setEditorDirectState(newEd)
          pane.resetCursorBlink()
          pane.redraw()
        case EditAction.NoOp => ()

  /** Bucket an OrnamentMode into the symbolic name AppMetricEvents.ornamentFinish understands.
    *
    * The Plan 18 PR-3b whitelist accepts meend/kan/gamak/andolan/custom. We don't see Gamak/Andolan here because
    * they're applied via Ctrl+G/Ctrl+A as ContentChange actions (counted at that site, not the ornament-mode path).
    * Anything else collapses to "custom" via AppMetricEvents.
    */
  private def ornamentTypeOf(mode: OrnamentMode): String = mode match
    case OrnamentMode.KanSwar           => "KanSwar"
    case OrnamentMode.Sparsh            => "Sparsh"
    case OrnamentMode.Ghaseet           => "Ghaseet"
    case OrnamentMode.MeendStart(_)     => "Meend"
    case OrnamentMode.MeendEnd(_, _)    => "Meend"
    case OrnamentMode.KrintanStart      => "Krintan"
    case OrnamentMode.KrintanEnd(_)     => "Krintan"
    case OrnamentMode.MurkiCollect(_)   => "Murki"
    case OrnamentMode.ZamzamaCollect(_) => "Zamzama"
