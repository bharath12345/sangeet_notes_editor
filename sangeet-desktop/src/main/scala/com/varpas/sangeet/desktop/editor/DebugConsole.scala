package com.varpas.sangeet.desktop.editor

import java.net.{ServerSocket, Socket, InetAddress}
import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.util.concurrent.{CompletableFuture, CopyOnWriteArrayList, TimeUnit}
import java.util.concurrent.atomic.AtomicBoolean
import com.varpas.sangeet.core.api.CompositionApi
import com.varpas.sangeet.core.model.*

class DebugConsole(editorPane: EditorPane, statusBar: StatusBar, port: Int = 28081):

  private val running = new AtomicBoolean(false)
  private val activeClients = new CopyOnWriteArrayList[Socket]()
  private var serverSocket: Option[ServerSocket] = None
  private var acceptThread: Option[Thread] = None
  private val END_MARKER = "---END---"

  def start(): Unit =
    try
      val ss = new ServerSocket(port, 5, InetAddress.getLoopbackAddress)
      serverSocket = Some(ss)
      running.set(true)
      val thread = new Thread(() => acceptLoop(), "debug-console-accept")
      thread.setDaemon(true)
      thread.start()
      acceptThread = Some(thread)
      AppLogger.info(s"Debug console listening on 127.0.0.1:$port")
      System.err.println(s"Debug console: nc 127.0.0.1 $port")
    catch
      case ex: Exception =>
        AppLogger.info(s"Debug console failed to start on port $port: ${ex.getMessage}")
        System.err.println(s"Debug console failed: ${ex.getMessage}")

  def stop(): Unit =
    running.set(false)
    serverSocket.foreach { ss =>
      try ss.close() catch case _: Exception => ()
    }
    activeClients.forEach { s =>
      try s.close() catch case _: Exception => ()
    }
    activeClients.clear()

  private def acceptLoop(): Unit =
    val ss = serverSocket.getOrElse(return)
    while running.get() do
      try
        val client = ss.accept()
        activeClients.add(client)
        val handler = new Thread(() => handleClient(client), s"debug-client-${client.getPort}")
        handler.setDaemon(true)
        handler.start()
      catch
        case _: java.net.SocketException if !running.get() => ()
        case ex: Exception =>
          if running.get() then
            AppLogger.info(s"Debug console accept error: ${ex.getMessage}")

  private def handleClient(socket: Socket): Unit =
    try
      val reader = new BufferedReader(new InputStreamReader(socket.getInputStream, "UTF-8"))
      val writer = new PrintWriter(socket.getOutputStream, true)
      writer.println(s"Sangeet Debug Console. Type 'help' for commands.")
      writer.println(END_MARKER)

      var line = reader.readLine()
      while line != null && running.get() do
        val trimmed = line.trim
        if trimmed.nonEmpty then
          val response = try dispatch(trimmed) catch case ex: Exception => s"ERROR: ${ex.getMessage}"
          writer.println(response)
          writer.println(END_MARKER)
        line = reader.readLine()
    catch
      case _: java.net.SocketException => ()
      case _: java.io.IOException => ()
    finally
      activeClients.remove(socket)
      try socket.close() catch case _: Exception => ()

  private def dispatch(input: String): String =
    val parts = input.split("\\s+", 2)
    val cmd = parts(0).toLowerCase
    val args = if parts.length > 1 then parts(1).trim else ""

    cmd match
      case "ping"             => "pong"
      case "help"             => cmdHelp()
      case "thread-dump"      => cmdThreadDump()
      case "set-debug"        => cmdSetDebug(args)
      case "get-state"        => runOnFx(cmdGetState())
      case "get-events"       => runOnFx(cmdGetEvents())
      case "dump-composition" => runOnFx(cmdDumpComposition())
      case "dump-history"     => runOnFx(cmdDumpHistory())
      case "check-focus"      => runOnFx(cmdCheckFocus())
      case "focus"            => runOnFx(cmdFocus())
      case "type"             => if args.isEmpty then "ERROR: usage: type <char>" else runOnFx(cmdType(args))
      case "press"            => if args.isEmpty then "ERROR: usage: press <key>" else runOnFx(cmdPress(args))
      case "octave"           => if args.isEmpty then "ERROR: usage: octave <period|quote|backtick>" else runOnFx(editorPane.debugOctaveKey(args))
      case "subdivision"      => if args.isEmpty then "ERROR: usage: subdivision <2-8>" else runOnFx(editorPane.debugSubdivision(args.trim.toInt))
      case "dual"             => if args.isEmpty then "ERROR: usage: dual <char>" else runOnFx(editorPane.debugDualSwar(args.charAt(0)))
      case "group"            => if args.isEmpty then "ERROR: usage: group <chars> (e.g., sr, srg, srgm)" else runOnFx(editorPane.debugSwarGroup(args.trim))
      case "type-timed"       => if args.isEmpty then "ERROR: usage: type-timed s:0,r:100,g:200" else runOnFx(cmdTypeTimed(args))
      case "stroke"           => if args.isEmpty then "ERROR: usage: stroke <da|ra|chikari|jod>" else runOnFx(editorPane.debugStroke(args))
      case "ornament"         => if args.isEmpty then "ERROR: usage: ornament <gamak|andolan|gitkari>" else runOnFx(editorPane.debugSimpleOrnament(args))
      case "ornament-start"   => if args.isEmpty then "ERROR: usage: ornament-start <mode>" else runOnFx(editorPane.debugOrnamentStart(args))
      case "ornament-note"    => if args.isEmpty then "ERROR: usage: ornament-note <char>" else runOnFx(editorPane.debugOrnamentNote(args.charAt(0)))
      case "finish-ornament"  => runOnFx(editorPane.debugFinishOrnament())
      case "section"          => if args.isEmpty then "ERROR: usage: section <index>" else runOnFx(editorPane.debugSwitchSection(args.trim.toInt))
      case "reset"            => runOnFx(cmdReset(args))
      case other              => s"ERROR: unknown command '$other'. Type 'help' for available commands."

  private def runOnFx(f: => String): String =
    if javafx.application.Platform.isFxApplicationThread then
      try f catch case ex: Exception => s"ERROR: ${ex.getMessage}"
    else
      val future = new CompletableFuture[String]()
      javafx.application.Platform.runLater(() =>
        try future.complete(f)
        catch case ex: Exception => future.completeExceptionally(ex)
      )
      try future.get(5, TimeUnit.SECONDS)
      catch
        case _: java.util.concurrent.TimeoutException =>
          "ERROR: FX thread did not respond in 5 seconds (possible freeze)"
        case ex: java.util.concurrent.ExecutionException =>
          val cause = ex.getCause
          s"ERROR: ${cause.getClass.getSimpleName}: ${cause.getMessage}"
        case ex: Exception =>
          s"ERROR: ${ex.getMessage}"

  private def cmdHelp(): String =
    """Commands:
      |  ping                    Health check
      |  help                    This message
      |  thread-dump             JVM thread dump (works during freeze)
      |  set-debug on|off        Toggle debug logging
      |  type <char>             Simulate swar key (s r g m p d n, uppercase=komal/tivra)
      |  press <key>             Simulate special key (space, backspace, delete, minus, left, right)
      |  octave <key>            Set octave (period=mandra, quote=taar, backtick=madhya)
      |  subdivision <n>         Set beat subdivision (2-8)
      |  dual <char>             Enter dual swar (ss=SaSa, rr=ReRe, etc.)
      |  group <chars>           Enter swar group (sr=SaRe, srg=SaReGa, etc.)
      |  type-timed <c:ms,...>   Type with timing (e.g., s:0,r:100 groups; s:0,r:600 separates)
      |  stroke <name>           Set stroke on last note (da, ra, chikari, jod)
      |  ornament <name>         Add simple ornament (gamak, andolan, gitkari)
      |  ornament-start <mode>   Begin multi-step ornament (kanswar, sparsh, ghaseet,
      |                          meend-asc, meend-desc, krintan, murki, zamzama)
      |  ornament-note <char>    Add note to current ornament
      |  finish-ornament         Finish multi-note ornament (murki, zamzama)
      |  section <index>         Switch to section by index
      |  reset [type] [taal] [taanCount]  Reset to empty composition
      |  get-state               Editor state: cursor, section, events, mode
      |  get-events              All events in current section
      |  dump-composition        Full composition as JSON
      |  dump-history            Undo/redo stack sizes
      |  check-focus             Which UI node has focus
      |  focus                   Force focus to editor""".stripMargin

  private def cmdThreadDump(): String =
    val sb = new StringBuilder
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

  private def cmdSetDebug(args: String): String =
    args.toLowerCase match
      case "on"  => AppLogger.setDebugEnabled(true); "Debug logging enabled"
      case "off" => AppLogger.setDebugEnabled(false); "Debug logging disabled"
      case ""    => s"Debug logging is ${if AppLogger.isDebugEnabled then "on" else "off"}"
      case other => s"ERROR: usage: set-debug on|off (got '$other')"

  private def cmdGetState(): String =
    editorPane.getEditor match
      case None => "No composition loaded"
      case Some(ed) =>
        val c = ed.cursor
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

  private def cmdGetEvents(): String =
    editorPane.getEditor match
      case None => "No composition loaded"
      case Some(ed) =>
        val section = ed.composition.sections(ed.currentSectionIndex)
        if section.events.isEmpty then "No events in section"
        else
          section.events.zipWithIndex.map { (event, i) =>
            event match
              case Event.Swar(note, variant, octave, beat, duration, stroke, ornaments, sahitya) =>
                val varStr = variant match
                  case Variant.Komal => " komal"
                  case Variant.Tivra => " tivra"
                  case _ => ""
                val strokeStr = stroke.map(s => s" stroke=$s").getOrElse("")
                val ornStr = if ornaments.nonEmpty then s" ornaments=${ornaments.size}" else ""
                s"[$i] Swar ${note}${varStr} ${octave} @${beat}${strokeStr}${ornStr}"
              case Event.Rest(beat, _) =>
                s"[$i] Rest @${beat}"
              case Event.Sustain(beat, _) =>
                s"[$i] Sustain @${beat}"
          }.mkString("\n")

  private def cmdDumpComposition(): String =
    editorPane.getComposition match
      case None => "No composition loaded"
      case Some(comp) =>
        CompositionApi.serializeCompositionString(comp)

  private def cmdDumpHistory(): String =
    val (past, future) = editorPane.undoHistoryInfo
    s"past: $past\nfuture: $future"

  private def cmdCheckFocus(): String =
    val scrollFocused = editorPane.isScrollPaneFocused
    val focusOwner = Option(editorPane.delegate.getScene).flatMap(s => Option(s.getFocusOwner))
    val ownerStr = focusOwner.map(n => n.getClass.getSimpleName).getOrElse("none")
    s"scrollPaneFocused: $scrollFocused\nfocusOwner: $ownerStr"

  private def cmdFocus(): String =
    editorPane.requestFocus()
    "Focus requested"

  private def cmdType(args: String): String =
    val ch = args.charAt(0)
    editorPane.debugTypeChar(ch)

  private def cmdTypeTimed(args: String): String =
    val entries = args.trim.split(",").toList.flatMap { entry =>
      entry.split(":") match
        case Array(charPart, msPart) if charPart.nonEmpty =>
          try Some((charPart.charAt(0), msPart.toLong))
          catch case _: NumberFormatException => None
        case _ => None
    }
    if entries.isEmpty then "ERROR: no valid entries. Usage: type-timed s:0,r:100,g:200"
    else editorPane.debugTypeTimed(entries)

  private def cmdPress(args: String): String =
    editorPane.debugPressKey(args)

  private def cmdReset(args: String): String =
    val parts = args.split("\\s+").filter(_.nonEmpty)
    val compType = if parts.length > 0 then parts(0) else "gat"
    val taalName = if parts.length > 1 then parts(1) else "teentaal"
    val taanCount = if parts.length > 2 then parts(2).toInt else 0
    editorPane.debugResetComposition(compType, taalName, taanCount)
