package com.varpas.sangeet.desktop.editor

import com.varpas.sangeet.core.api.CompositionApi
import com.varpas.sangeet.core.debug.DebugCommand
import com.varpas.sangeet.core.editor.*
import com.varpas.sangeet.core.model.{Andolan, Gamak, Gitkari, MeendDirection, *}

class DebugCommandHandler(tabManager: TabManager, statusBar: StatusBar):

  private def editorPane: EditorPane =
    tabManager.activeTab
      .map(_.editorPane)
      .getOrElse(
        throw new IllegalStateException("No active tab")
      )

  /** Entry point preserved for backward compat with DebugConsole.scala which sends raw text lines from sockets. Parse
    * then dispatch.
    */
  def handleCommand(line: String): String =
    DebugCommand.fromText(line) match
      case Right(cmd) => applyDebugCommand(cmd)
      case Left(err)  => s"ERROR: $err"

  /** New entry point — apply a typed DebugCommand. Both the TCP path (via handleCommand) and any future direct-call
    * path (e.g., in-process tests) route through here.
    */
  def applyDebugCommand(cmd: DebugCommand): String =
    import DebugCommand.*
    cmd match
      case Ping                    => "PONG"
      case Help                    => helpText
      case ThreadDump              => threadDumpText
      case SetDebug(enabled)       => setDebug(enabled)
      case ThrowCrash              => throwCrash()
      case ListTabs                => listTabs()
      case SelectTab(id)           => selectTab(id)
      case NewTab                  => newTab()
      case CloseTab(id)            => closeTab(id)
      case TabInfo                 => tabInfo()
      case Reset(t, raag, taal)    => resetComposition(t, raag, taal)
      case SetTaal(taal)           => changeTaal(taal)
      case CheckFocus              => checkFocus()
      case FocusEditor             => focusEditor()
      case SetOctave(oct)          => setOctave(oct)
      case SetSubdivision(n)       => subdivision(n)
      case TypeChar(chars)         => typeChars(chars)
      case Press(key)              => pressKey(key)
      case TypeTimed(ch, delay)    => typeTimed(ch, delay)
      case DualSwar(first, second) => dualSwarPair(first, second)
      case SwarGroup(notes)        => swarGroup(notes.mkString(""))
      case Stroke(s)               => stroke(s)
      case SimpleOrnament(name)    => simpleOrnament(name)
      case OrnamentStart(kind)     => ornamentStart(kind)
      case OrnamentNote(note)      => ornamentNoteStr(note)
      case FinishOrnament          => finishOrnament()
      case SwitchSection(idx)      => switchSection(idx)
      case GetState                => getState()
      case GetEvents               => getEvents()
      case DumpComposition         => dumpComposition()
      case DumpHistory             => dumpHistory()

  // ========== Common helpers ==========

  private def withEditor(f: CompositionEditor => String): String =
    editorPane.getEditor match
      case None     => "ERROR: no composition loaded"
      case Some(ed) => f(ed)

  private def withWritableEditor(f: CompositionEditor => String): String =
    withEditor { ed =>
      if editorPane.isReadOnly then "ERROR: editor is read-only"
      else f(ed)
    }

  private def pushAndRefresh(ed: CompositionEditor, msg: String): Unit =
    statusBar.log(msg)
    editorPane.pushEditorState(ed)
    editorPane.resetCursorBlink()
    editorPane.redraw()

  // ========== Introspection commands ==========

  private def helpText: String =
    """Commands:
      |  ping                    Health check
      |  help                    This message
      |  thread-dump             JVM thread dump (works during freeze)
      |  set-debug on|off        Toggle debug logging
      |
      |  Tab management:
      |  list-tabs               List all open tabs with index and title
      |  select-tab <index>      Switch to tab by index
      |  new-tab                 Create a new empty tab
      |  close-tab [index]       Close tab (default: active tab)
      |  tab-info                Info about the active tab
      |
      |  Editor commands (operate on active tab):
      |  type <char>             Simulate swar key (s r g m p d n, uppercase=komal/tivra)
      |  press <key>             Simulate special key (space, backspace, delete, minus, left, right)
      |  octave <key>            Set octave (period=mandra, quote=taar, backtick=madhya)
      |  subdivision <n>         Set beat subdivision (2-8)
      |  dual <char>             Enter dual swar (ss=SaSa, rr=ReRe, etc.)
      |  group <chars>           Enter swar group (sr=SaRe, srg=SaReGa, etc.)
      |  type-timed <c:ms,...>   Type with timing (e.g., s:0,r:100 groups; s:0,r:600 separates)
      |  stroke <name>           Set stroke on last note (da, ra, jod)
      |  ornament <name>         Add simple ornament (gamak, andolan, gitkari)
      |  ornament-start <mode>   Begin multi-step ornament (kanswar, sparsh, ghaseet,
      |                          meend-asc, meend-desc, krintan, murki, zamzama)
      |  ornament-note <char>    Add note to current ornament
      |  finish-ornament         Finish multi-note ornament (murki, zamzama)
      |  section <index>         Switch to section by index
      |  set-taal <name>         Change taal (teentaal, jhaptaal, rupak, ektaal, dadra, keherwa, ...)
      |  reset [type] [taal] [taanCount]  Reset to empty composition
      |  get-state               Editor state: cursor, section, events, mode
      |  get-events              All events in current section
      |  dump-composition        Full composition as JSON
      |  dump-history            Undo/redo stack sizes
      |  check-focus             Which UI node has focus
      |  focus                   Force focus to editor
      |
      |  Diagnostics:
      |  throw [msg]             Throw an unchecked exception on a new thread.
      |                          Triggers CrashCapture; writes a sentinel file under
      |                          ~/.sangeet/crash-pending/ that the next launch will
      |                          surface in the recovery dialog. Does NOT kill the
      |                          JVM (only the spawned thread dies).""".stripMargin

  private def threadDumpText: String =
    val sb      = new StringBuilder
    val threads = Thread.getAllStackTraces
    threads.forEach { (thread, stack) =>
      sb.append(s""""${thread.getName}" state=${thread.getState}""")
      sb.append("\n")
      stack.foreach { frame =>
        sb.append(s"  at ${frame.getClassName}.${frame.getMethodName}(${frame.getFileName}:${frame.getLineNumber})\n")
      }
      sb.append("\n")
    }
    sb.toString.trim

  private def setDebug(enabled: Boolean): String =
    AppLogger.setDebugEnabled(enabled)
    s"Debug logging ${if enabled then "enabled" else "disabled"}"

  private def throwCrash(): String =
    val msg = "Debug-console synthetic crash"
    val t = new Thread(
      () => throw new RuntimeException(msg),
      "sangeet-debug-throw"
    )
    t.setDaemon(true)
    t.start()
    s"OK: spawned thread to throw RuntimeException('$msg'). Check ~/.sangeet/crash-pending/ for the sentinel file."

  // ========== Tab management ==========

  private def listTabs(): String =
    val tabs = tabManager.allTabs
    if tabs.isEmpty then "No tabs open"
    else
      val activeIdx = tabManager.activeTabIndex
      tabs.zipWithIndex
        .map { (et, i) =>
          val marker = if i == activeIdx then " *" else ""
          val path   = et.filePath.map(_.toString).getOrElse("(unsaved)")
          s"[$i] ${et.title} — $path$marker"
        }
        .mkString("\n")

  private def selectTab(id: String): String =
    val tabs  = tabManager.allTabs
    val index = id.toIntOption.getOrElse(-1)
    if index < 0 || index >= tabs.size then s"ERROR: tab index $index out of range (0..${tabs.size - 1})"
    else
      tabManager.selectTabByIndex(index)
      val et = tabs(index)
      s"Switched to tab $index: ${et.title}"

  private def newTab(): String =
    val et  = tabManager.newTab()
    val idx = tabManager.allTabs.indexOf(et)
    tabManager.selectTabByIndex(idx)
    s"Created new tab at index $idx: ${et.title}"

  private def closeTab(id: String): String =
    val tabs = tabManager.allTabs
    if tabs.isEmpty then "ERROR: no tabs to close"
    else
      val idx =
        if id.trim.isEmpty then tabManager.activeTabIndex
        else id.trim.toIntOption.getOrElse(-1)
      if idx < 0 || idx >= tabs.size then s"ERROR: tab index $idx out of range (0..${tabs.size - 1})"
      else
        val et    = tabs(idx)
        val title = et.title
        tabManager.closeTab(et)
        s"Closed tab: $title"

  private def tabInfo(): String =
    tabManager.activeTab match
      case None => "No active tab"
      case Some(et) =>
        val idx      = tabManager.activeTabIndex
        val path     = et.filePath.map(_.toString).getOrElse("(unsaved)")
        val readOnly = et.editorPane.isReadOnly
        s"""tab: $idx
           |title: ${et.title}
           |path: $path
           |readOnly: $readOnly""".stripMargin

  // ========== Focus commands ==========

  private def checkFocus(): String =
    val scrollFocused = editorPane.isScrollPaneFocused
    val focusOwner    = Option(editorPane.delegate.getScene).flatMap(s => Option(s.getFocusOwner))
    val ownerStr      = focusOwner.map(n => n.getClass.getSimpleName).getOrElse("none")
    s"scrollPaneFocused: $scrollFocused\nfocusOwner: $ownerStr"

  private def focusEditor(): String =
    editorPane.requestFocus()
    "Focus requested"

  // ========== Cursor / mode setters ==========

  private def setOctave(octave: String): String =
    octaveKey(octave)

  // ========== Swar input ==========

  private def typeChars(chars: String): String =
    // TypeChar can contain multiple chars (legacy TCP "type s r g m" behavior)
    chars.toList.map(ch => typeChar(ch)).mkString("; ")

  private def typeTimed(ch: String, delayMs: Int): String =
    if ch.isEmpty then "ERROR: no character provided"
    else
      val entries = List((ch.charAt(0), delayMs.toLong))
      typeTimed(entries)

  private def dualSwarPair(first: String, second: String): String =
    // Handle both "dual s s" and "dual s" (where second defaults to first)
    val ch = if second.isEmpty then first.charAt(0) else second.charAt(0)
    dualSwar(ch)

  // ========== State read-back ==========

  private def getState(): String =
    editorPane.getEditor match
      case None => "No composition loaded"
      case Some(ed) =>
        val c       = ed.cursor
        val section = ed.composition.sections(ed.currentSectionIndex)
        s"""section: ${ed.currentSectionIndex} (${section.sectionType})
           |cursor.cycle: ${c.cycle}
           |cursor.beat: ${c.beat}
           |cursor.subIndex: ${c.subIndex}
           |cursor.totalSubdivisions: ${c.totalSubdivisions}
           |cursor.octave: ${c.currentOctave}
           |events: ${section.events.size}
           |readOnly: ${editorPane.isReadOnly}
           |editMode: ${editorPane.currentEditMode}
           |scrollPaneFocused: ${editorPane.isScrollPaneFocused}""".stripMargin

  private def getEvents(): String =
    editorPane.getEditor match
      case None => "No composition loaded"
      case Some(ed) =>
        val section = ed.composition.sections(ed.currentSectionIndex)
        if section.events.isEmpty then "No events in section"
        else
          section.events.zipWithIndex
            .map { (event, i) =>
              event match
                case Event.Swar(note, variant, octave, beat, duration, stroke, ornaments, sahitya) =>
                  val varStr = variant match
                    case Variant.Komal => " komal"
                    case Variant.Tivra => " tivra"
                    case _             => ""
                  val strokeStr = stroke.map(s => s" stroke=$s").getOrElse("")
                  val ornStr    = if ornaments.nonEmpty then s" ornaments=${ornaments.size}" else ""
                  s"[$i] Swar ${note}${varStr} ${octave} @${beat}${strokeStr}${ornStr}"
                case Event.Rest(beat, _) =>
                  s"[$i] Rest @${beat}"
                case Event.Sustain(beat, _) =>
                  s"[$i] Sustain @${beat}"
                case Event.Chikari(beat, _) =>
                  s"[$i] Chikari @${beat}"
                case Event.LockedBeat(beat, _) =>
                  s"[$i] LockedBeat @${beat}"
            }
            .mkString("\n")

  private def dumpComposition(): String =
    editorPane.getComposition match
      case None => "No composition loaded"
      case Some(comp) =>
        CompositionApi.serializeCompositionString(comp)

  private def dumpHistory(): String =
    val (past, future) = editorPane.undoHistoryInfo
    s"past: $past\nfuture: $future"

  // ========== Existing public methods (called by EditorPane.debug*) ==========

  def typeChar(ch: Char): String =
    withWritableEditor { ed =>
      if ch == '1' then
        val (newEd, msg) = KeyHandler.handleChikariKey(ed)
        pushAndRefresh(newEd, msg)
        msg
      else
        val (newEd, msg) = KeyHandler.handleSwarKey(ed, ch, ch.isUpper)
        if newEd ne ed then pushAndRefresh(newEd, msg)
        msg
    }

  def pressKey(keyName: String): String =
    withWritableEditor { ed =>
      keyName.toUpperCase match
        case k @ ("SPACE" | "BACKSPACE" | "DELETE" | "MINUS") =>
          val (newEd, msg) = KeyHandler.handleSpecialKey(ed, k)
          pushAndRefresh(newEd, msg)
          msg
        case "LEFT" =>
          val newCursor = ed.cursor.prevBeat
          editorPane.setEditorDirectState(ed.copy(cursor = newCursor))
          editorPane.resetCursorBlink()
          editorPane.redraw()
          s"Cursor back: cycle=${newCursor.cycle} beat=${newCursor.beat}"
        case "RIGHT" =>
          val next = ed.cursor.nextBeat
          if next.cycle <= ed.maxCycle + 1 then
            editorPane.setEditorDirectState(ed.copy(cursor = next))
            editorPane.resetCursorBlink()
            editorPane.redraw()
            s"Cursor forward: cycle=${next.cycle} beat=${next.beat}"
          else "At end -- cannot advance"
        case other =>
          s"ERROR: unknown key '$other' -- use space, backspace, delete, minus, left, right"
    }

  def octaveKey(keyName: String): String =
    withEditor { ed =>
      keyName.toUpperCase match
        case k @ ("PERIOD" | "QUOTE" | "BACKTICK") =>
          val (newEd, msg) = KeyHandler.handleOctaveKey(ed, k)
          editorPane.setEditorDirectState(newEd)
          editorPane.redraw()
          msg
        case other =>
          s"ERROR: unknown octave key '$other' -- use period, quote, backtick"
    }

  def subdivision(n: Int): String =
    withEditor { ed =>
      if n < 1 || n > 8 then s"ERROR: subdivision must be 1-8 (got $n)"
      else
        editorPane.setEditorDirectState(KeyHandler.handleSubdivision(ed, n))
        s"Subdivision set to $n"
    }

  def dualSwar(ch: Char): String =
    withWritableEditor { ed =>
      val (newEd, msg) = KeyHandler.handleDualSwar(ed, ch, ch.isUpper)
      pushAndRefresh(newEd, msg)
      msg
    }

  def typeTimed(entries: List[(Char, Long)]): String =
    // TODO: refactor to Either to avoid `return`
    if entries.isEmpty then return "ERROR: no entries"
    val results = entries.map { (ch, delayMs) =>
      editorPane.typeCharTimed(ch, delayMs)
    }
    results.mkString("; ")

  def swarGroup(chars: String): String =
    withWritableEditor { ed =>
      val notes = chars.toList.flatMap { ch =>
        KeyHandler.charToNote(ch).map { note =>
          (note, KeyHandler.resolveVariant(note, ch.isUpper), Octave.Madhya)
        }
      }
      if notes.isEmpty then "ERROR: no valid swar keys"
      else if notes.size > 4 then "ERROR: max 4 notes per group"
      else
        val (newEd, msg) = KeyHandler.handleSwarGroup(ed, notes)
        pushAndRefresh(newEd, msg)
        msg
    }

  def stroke(strokeName: String): String =
    withEditor { ed =>
      val strokeOpt = strokeName.toLowerCase match
        case "da"  => Some(Stroke.Da)
        case "ra"  => Some(Stroke.Ra)
        case "jod" => Some(Stroke.Jod)
        case _     => None
      strokeOpt match
        case None => s"ERROR: unknown stroke '$strokeName' -- use da, ra, jod"
        case Some(s) =>
          val (newEd, msg) = KeyHandler.handleStroke(ed, s)
          if newEd ne ed then
            editorPane.pushEditorState(newEd)
            editorPane.redraw()
          msg
    }

  def simpleOrnament(ornamentName: String): String =
    withEditor { ed =>
      val ornResult: Option[(Ornament, String)] = ornamentName.toLowerCase match
        case "gamak"   => Some((Gamak(), "Gamak"))
        case "andolan" => Some((Andolan(), "Andolan"))
        case "gitkari" => Some((Gitkari(), "Gitkari"))
        case _         => None
      ornResult match
        case None => s"ERROR: unknown ornament '$ornamentName' -- use gamak, andolan, gitkari"
        case Some((ornament, name)) =>
          val (newEd, msg) = KeyHandler.handleSimpleOrnament(ed, ornament, name)
          if newEd ne ed then
            editorPane.pushEditorState(newEd)
            editorPane.redraw()
          msg
    }

  def ornamentStart(modeName: String): String =
    modeName.toLowerCase match
      case "kanswar" => editorPane.setOrnamentMode(Some(OrnamentMode.KanSwar)); "KanSwar mode: type note"
      case "sparsh"  => editorPane.setOrnamentMode(Some(OrnamentMode.Sparsh)); "Sparsh mode: type note"
      case "ghaseet" => editorPane.setOrnamentMode(Some(OrnamentMode.Ghaseet)); "Ghaseet mode: type note"
      case "meend-asc" =>
        editorPane.setOrnamentMode(Some(OrnamentMode.MeendStart(MeendDirection.Ascending)));
        "Meend ascending: type start note"
      case "meend-desc" =>
        editorPane.setOrnamentMode(Some(OrnamentMode.MeendStart(MeendDirection.Descending)));
        "Meend descending: type start note"
      case "krintan" => editorPane.setOrnamentMode(Some(OrnamentMode.KrintanStart)); "Krintan: type start note"
      case "murki" =>
        editorPane.setOrnamentMode(Some(OrnamentMode.MurkiCollect(Nil)));
        "Murki collect: type notes, then finish-ornament"
      case "zamzama" =>
        editorPane.setOrnamentMode(Some(OrnamentMode.ZamzamaCollect(Nil)));
        "Zamzama collect: type notes, then finish-ornament"
      case other => s"ERROR: unknown mode '$other'"

  private def ornamentNoteStr(note: String): String =
    if note.isEmpty then "ERROR: no note provided"
    else ornamentNote(note.charAt(0))

  def ornamentNote(ch: Char): String =
    withEditor { ed =>
      editorPane.getOrnamentMode match
        case None => "ERROR: not in ornament mode -- use ornament-start first"
        case Some(mode) =>
          val (newEd, msg, nextMode) = KeyHandler.handleNoteOrnament(ed, ch, ch.isUpper, mode)
          if newEd ne ed then editorPane.pushEditorState(newEd) else editorPane.setEditorDirectState(newEd)
          editorPane.setOrnamentMode(nextMode)
          editorPane.redraw()
          msg
    }

  def finishOrnament(): String =
    withEditor { ed =>
      editorPane.getOrnamentMode match
        case None => "ERROR: not in ornament mode"
        case Some(mode) =>
          val (newEd, msg) = KeyHandler.finishMultiNoteOrnament(ed, mode)
          if newEd ne ed then editorPane.pushEditorState(newEd)
          editorPane.setOrnamentMode(None)
          editorPane.redraw()
          msg
    }

  def switchSection(idx: Int): String =
    withEditor { ed =>
      if idx < 0 || idx >= ed.composition.sections.size then
        s"ERROR: section index $idx out of range (0 to ${ed.composition.sections.size - 1})"
      else
        val newEd = ed.copy(currentSectionIndex = idx, cursor = CursorModel(ed.composition.metadata.taal))
        editorPane.setEditorDirectState(newEd)
        editorPane.redraw()
        s"Switched to section $idx: ${ed.composition.sections(idx).name}"
    }

  def changeTaal(taalName: String): String =
    withWritableEditor { ed =>
      import com.varpas.sangeet.core.taal.Taals
      Taals.byName(taalName) match
        case None => s"ERROR: unknown taal '$taalName'"
        case Some(newTaal) =>
          if newTaal.name == ed.composition.metadata.taal.name then s"Taal unchanged: ${newTaal.name}"
          else
            val newEd = ed.changeTaal(newTaal)
            pushAndRefresh(newEd, s"Taal changed to ${newTaal.name} (${newTaal.matras} matras)")
            s"Taal changed to ${newTaal.name} (${newTaal.matras} matras)"
    }

  private def resetComposition(compType: String, raagOpt: Option[String], taalName: String): String =
    import com.varpas.sangeet.core.taal.Taals
    // TODO(taals-as-data): use Taals.byName like changeTaal does (this whitelist predates Phase 2 — separate cleanup)
    val taal = taalName.toLowerCase match
      case "teentaal" => Taals.teentaal
      case "jhaptaal" => Taals.jhaptaal
      case "rupak"    => Taals.rupak
      case "ektaal"   => Taals.ektaal
      case "dadra"    => Taals.dadra
      case "keherwa"  => Taals.keherwa
      case other      => return s"ERROR: unknown taal '$other'"
    val ct = compType.toLowerCase match
      case "gat"     => CompositionType.Gat
      case "bandish" => CompositionType.Bandish
      case "palta"   => CompositionType.Palta
      case "sargam"  => CompositionType.Sargam
      case other     => return s"ERROR: unknown type '$other'"
    val raag      = Raag(raagOpt.getOrElse("Yaman"), None, None, None, None, None, None, None)
    val taanCount = 0 // Fixed to 0; if we need configurable taanCount later, add it to DebugCommand.Reset
    val ed = CompositionEditor.create(
      title = "Debug Test",
      compositionType = ct,
      taal = taal,
      raag = raag,
      laya = if ct == CompositionType.Palta then None else Some(Laya.Madhya),
      taanCount = taanCount
    )
    editorPane.setEditor(ed)
    editorPane.setReadOnly(false)
    editorPane.setOrnamentMode(None)
    s"Reset: ${ct} ${taal.name} (${ed.composition.sections.size} sections)"
