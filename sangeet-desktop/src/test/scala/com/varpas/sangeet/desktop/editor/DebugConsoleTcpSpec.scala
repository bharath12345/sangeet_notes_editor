package com.varpas.sangeet.desktop.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import java.net.Socket
import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.*
import scala.compiletime.uninitialized

class DebugConsoleTcpSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll:

  private val testPort = 28082
  private var editorPane: EditorPane = uninitialized
  private var statusBar: StatusBar = uninitialized
  private var debugConsole: DebugConsole = uninitialized
  private val swarKeys = List('s', 'r', 'g', 'm', 'p', 'd', 'n')

  override def beforeAll(): Unit =
    super.beforeAll()
    try javafx.application.Platform.startup(() => ())
    catch case _: IllegalStateException => () // already started
    javafx.application.Platform.setImplicitExit(false)
    AppLogger.initialize()
    val latch = new java.util.concurrent.CountDownLatch(1)
    javafx.application.Platform.runLater(() =>
      statusBar = new StatusBar()
      editorPane = new EditorPane(statusBar)
      latch.countDown()
    )
    latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
    debugConsole = new DebugConsole(editorPane, statusBar, testPort)
    debugConsole.start()
    Thread.sleep(200) // let server socket bind

  override def afterAll(): Unit =
    debugConsole.stop()
    super.afterAll()

  // --- TCP client helper ---

  private def withClient[T](f: (PrintWriter, BufferedReader) => T): T =
    val socket = new Socket("127.0.0.1", testPort)
    try
      val writer = new PrintWriter(socket.getOutputStream, true)
      val reader = new BufferedReader(new InputStreamReader(socket.getInputStream, "UTF-8"))
      // consume welcome message
      readUntilEnd(reader)
      f(writer, reader)
    finally
      socket.close()

  private def readUntilEnd(reader: BufferedReader): String =
    val sb = new StringBuilder
    var line = reader.readLine()
    while line != null && line != "---END---" do
      if sb.nonEmpty then sb.append("\n")
      sb.append(line)
      line = reader.readLine()
    sb.toString

  private def send(writer: PrintWriter, reader: BufferedReader, cmd: String): String =
    writer.println(cmd)
    readUntilEnd(reader)

  private def sendN(writer: PrintWriter, reader: BufferedReader, cmds: Seq[String]): Seq[String] =
    cmds.map(cmd => send(writer, reader, cmd))

  private def getEventCount(writer: PrintWriter, reader: BufferedReader): Int =
    val state = send(writer, reader, "get-state")
    val eventsLine = state.split("\n").find(_.startsWith("events:"))
    eventsLine.map(_.split(":")(1).trim.toInt).getOrElse(-1)

  private def getCursorInfo(writer: PrintWriter, reader: BufferedReader): Map[String, String] =
    val state = send(writer, reader, "get-state")
    state.split("\n").flatMap { line =>
      val parts = line.split(":", 2)
      if parts.length == 2 then Some(parts(0).trim -> parts(1).trim) else None
    }.toMap

  private def getLogLines: List[String] =
    val logDir = Paths.get("/tmp")
    val stream = Files.list(logDir)
    try
      val candidates = stream
        .filter(p => p.getFileName.toString.startsWith("sangeet-notes-editor.") && p.getFileName.toString.contains(".log"))
        .filter(p => !p.getFileName.toString.endsWith(".lck"))
        .sorted(java.util.Comparator.comparingLong[Path](p => Files.getLastModifiedTime(p).toMillis).reversed)
        .collect(java.util.stream.Collectors.toList[Path])
        .asScala.toList
      candidates.headOption match
        case Some(logPath) =>
          val content = new String(Files.readAllBytes(logPath), "UTF-8")
          content.split("\n").toList
        case None => Nil
    finally
      stream.close()

  private def countPushEditorLogs(keyword: String = "pushEditor:"): Int =
    getLogLines.count(_.contains(keyword))

  private def reset(writer: PrintWriter, reader: BufferedReader, compType: String = "gat", taal: String = "teentaal", taanCount: Int = 0): String =
    val args = s"$compType $taal $taanCount".trim
    send(writer, reader, s"reset $args")

  // =====================================================================
  // CONNECTION & BASIC PROTOCOL
  // =====================================================================

  "TCP protocol" should "respond to ping" in withClient { (w, r) =>
    send(w, r, "ping") shouldBe "pong"
  }

  it should "return help text" in withClient { (w, r) =>
    val help = send(w, r, "help")
    help should include ("ping")
    help should include ("type")
    help should include ("ornament")
    help should include ("reset")
  }

  it should "reject unknown commands" in withClient { (w, r) =>
    val resp = send(w, r, "foobar")
    resp should include ("ERROR")
    resp should include ("unknown command")
  }

  // =====================================================================
  // RESET COMMAND
  // =====================================================================

  "Reset command" should "create empty Gat with Teentaal" in withClient { (w, r) =>
    val resp = reset(w, r)
    resp should include ("Gat")
    resp should include ("Teentaal")
    getEventCount(w, r) shouldBe 0
  }

  it should "create Palta composition" in withClient { (w, r) =>
    val resp = reset(w, r, "palta")
    resp should include ("Palta")
  }

  it should "create Bandish composition" in withClient { (w, r) =>
    val resp = reset(w, r, "bandish")
    resp should include ("Bandish")
  }

  it should "create Gat with 3 Taans" in withClient { (w, r) =>
    val resp = reset(w, r, "gat", "teentaal", 3)
    resp should include ("5 sections")
  }

  it should "create Jhaptaal composition" in withClient { (w, r) =>
    val resp = reset(w, r, "gat", "jhaptaal")
    resp should include ("Jhaptaal")
  }

  it should "create Rupak composition" in withClient { (w, r) =>
    val resp = reset(w, r, "gat", "rupak")
    resp should include ("Rupak")
  }

  // =====================================================================
  // BASIC SWAR INPUT — type notes, verify via get-state and get-events
  // =====================================================================

  "Basic swar input" should "insert 10 notes and verify event count" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 10 do
      val key = swarKeys(i % 7)
      val resp = send(w, r, s"type $key")
      resp should not startWith "ERROR"
    getEventCount(w, r) shouldBe 10
  }

  it should "insert 50 notes" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 50 do send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 50
  }

  it should "insert 100 notes (Gat-sized)" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 100 do
      val resp = send(w, r, s"type ${swarKeys(i % 7)}")
      if resp.startsWith("ERROR") then fail(s"Note $i failed:\n$resp")
    getEventCount(w, r) shouldBe 100
  }

  it should "produce pushEditor log entries" in withClient { (w, r) =>
    reset(w, r)
    val logsBefore = countPushEditorLogs()
    for i <- 0 until 5 do send(w, r, s"type ${swarKeys(i)}")
    Thread.sleep(100) // let logger flush
    val logsAfter = countPushEditorLogs()
    (logsAfter - logsBefore) should be >= 5
  }

  // =====================================================================
  // ALL 7 SWAR KEYS — verify each note in get-events
  // =====================================================================

  "All swar keys" should "produce correct notes" in withClient { (w, r) =>
    reset(w, r)
    val expectedNotes = List("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni")
    for (key, expected) <- swarKeys.zip(expectedNotes) do
      val resp = send(w, r, s"type $key")
      resp should include (expected)
    getEventCount(w, r) shouldBe 7
    val events = send(w, r, "get-events")
    events should include ("Sa")
    events should include ("Re")
    events should include ("Ga")
    events should include ("Ma")
    events should include ("Pa")
    events should include ("Dha")
    events should include ("Ni")
  }

  // =====================================================================
  // KOMAL / TIVRA VARIANTS
  // =====================================================================

  "Komal/tivra variants" should "produce correct variants for shifted keys" in withClient { (w, r) =>
    reset(w, r)
    // Uppercase = shifted: S (Sa shuddha), R (komal Re), G (komal Ga), M (tivra Ma), P (Pa shuddha), D (komal Dha), N (komal Ni)
    for key <- List('S', 'R', 'G', 'M', 'P', 'D', 'N') do
      send(w, r, s"type $key")
    val events = send(w, r, "get-events")
    events should include ("komal")
  }

  it should "handle 100 mixed variant notes" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 100 do
      val key = swarKeys(i % 7)
      val ch = if (i % 3) == 1 then key.toUpper else key
      send(w, r, s"type $ch")
    getEventCount(w, r) shouldBe 100
    val events = send(w, r, "get-events")
    events should include ("komal")
  }

  // =====================================================================
  // OCTAVE CHANGES
  // =====================================================================

  "Octave changes" should "set mandra/taar/madhya octaves" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "octave period") // mandra
    send(w, r, "type s")
    send(w, r, "octave quote") // taar
    send(w, r, "type r")
    send(w, r, "octave backtick") // madhya
    send(w, r, "type g")
    getEventCount(w, r) shouldBe 3
    val events = send(w, r, "get-events")
    events should include ("Mandra")
    events should include ("Taar")
    events should include ("Madhya")
  }

  it should "handle 100 notes with octave cycling" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 100 do
      val octave = (i % 12) match
        case n if n < 4  => "period"
        case n if n >= 8 => "quote"
        case _           => "backtick"
      send(w, r, s"octave $octave")
      send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 100
    val events = send(w, r, "get-events")
    events should include ("Mandra")
    events should include ("Taar")
    events should include ("Madhya")
  }

  // =====================================================================
  // REST AND SUSTAIN
  // =====================================================================

  "Rest and sustain" should "intersperse with swar" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 50 do
      (i % 5) match
        case 3 => send(w, r, "press space")
        case 4 => send(w, r, "press minus")
        case _ => send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 50
    val events = send(w, r, "get-events")
    events should include ("Rest")
    events should include ("Sustain")
  }

  // =====================================================================
  // DUAL SWAR
  // =====================================================================

  "Dual swar" should "insert pairs of 2 notes" in withClient { (w, r) =>
    reset(w, r)
    val resp = send(w, r, "dual s")
    resp should not startWith "ERROR"
    getEventCount(w, r) shouldBe 2
  }

  it should "handle 25 dual swar insertions (50 events)" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 25 do send(w, r, s"dual ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 50
  }

  // =====================================================================
  // BACKSPACE
  // =====================================================================

  "Backspace" should "delete events" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 10 do send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 10
    for _ <- 0 until 5 do
      val resp = send(w, r, "press backspace")
      resp should include ("Deleted")
    getEventCount(w, r) shouldBe 5
  }

  it should "delete all events" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 20 do send(w, r, s"type ${swarKeys(i % 7)}")
    for _ <- 0 until 20 do send(w, r, "press backspace")
    getEventCount(w, r) shouldBe 0
  }

  it should "handle backspace on empty editor" in withClient { (w, r) =>
    reset(w, r)
    val resp = send(w, r, "press backspace")
    resp should include ("Nothing")
  }

  it should "handle insert-delete cycles" in withClient { (w, r) =>
    reset(w, r)
    for _ <- 0 until 5 do
      for i <- 0 until 10 do send(w, r, s"type ${swarKeys(i % 7)}")
      for _ <- 0 until 5 do send(w, r, "press backspace")
    getEventCount(w, r) shouldBe 25
  }

  // =====================================================================
  // SUBDIVISIONS
  // =====================================================================

  "Subdivisions" should "set subdivision via TCP" in withClient { (w, r) =>
    reset(w, r)
    for subdiv <- 2 to 8 do
      val resp = send(w, r, s"subdivision $subdiv")
      resp should include (s"Subdivision set to $subdiv")
  }

  it should "insert notes at subdivision 4" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 20 do
      send(w, r, "subdivision 4")
      send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 20
  }

  // =====================================================================
  // STROKES
  // =====================================================================

  "Strokes" should "attach Da and Ra" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type s")
    val daResp = send(w, r, "stroke da")
    daResp should include ("Da")
    send(w, r, "type r")
    val raResp = send(w, r, "stroke ra")
    raResp should include ("Ra")
    val events = send(w, r, "get-events")
    events should include ("stroke=Da")
    events should include ("stroke=Ra")
  }

  it should "attach Chikari and Jod" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type s")
    send(w, r, "stroke chikari")
    send(w, r, "type r")
    send(w, r, "stroke jod")
    val events = send(w, r, "get-events")
    events should include ("stroke=Chikari")
    events should include ("stroke=Jod")
  }

  it should "fail on empty section" in withClient { (w, r) =>
    reset(w, r)
    val resp = send(w, r, "stroke da")
    resp should include ("No swar")
  }

  it should "handle alternating Da/Ra for 20 notes" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 20 do
      send(w, r, s"type ${swarKeys(i % 7)}")
      val stroke = if i % 2 == 0 then "da" else "ra"
      send(w, r, s"stroke $stroke")
    getEventCount(w, r) shouldBe 20
    val events = send(w, r, "get-events")
    events should include ("stroke=")
  }

  // =====================================================================
  // SIMPLE ORNAMENTS — Gamak, Andolan, Gitkari
  // =====================================================================

  "Simple ornaments" should "attach Gamak" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type s")
    val resp = send(w, r, "ornament gamak")
    resp should include ("Gamak")
    val events = send(w, r, "get-events")
    events should include ("ornaments=1")
  }

  it should "attach Andolan" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type m")
    val resp = send(w, r, "ornament andolan")
    resp should include ("Andolan")
  }

  it should "attach Gitkari" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type g")
    val resp = send(w, r, "ornament gitkari")
    resp should include ("Gitkari")
  }

  it should "stack multiple ornaments" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type s")
    send(w, r, "ornament gamak")
    send(w, r, "ornament andolan")
    send(w, r, "ornament gitkari")
    val events = send(w, r, "get-events")
    events should include ("ornaments=3")
  }

  it should "fail on empty section" in withClient { (w, r) =>
    reset(w, r)
    val resp = send(w, r, "ornament gamak")
    resp should include ("No swar")
  }

  it should "handle Gamak on 20 notes" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 20 do
      send(w, r, s"type ${swarKeys(i % 7)}")
      send(w, r, "ornament gamak")
    getEventCount(w, r) shouldBe 20
  }

  // =====================================================================
  // NOTE ORNAMENTS — KanSwar, Sparsh, Ghaseet
  // =====================================================================

  "KanSwar ornament" should "attach grace note" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type s")
    send(w, r, "ornament-start kanswar")
    val resp = send(w, r, "ornament-note r")
    resp should include ("Kan swar")
    val events = send(w, r, "get-events")
    events should include ("ornaments=1")
  }

  "Sparsh ornament" should "attach touch note" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type m")
    send(w, r, "ornament-start sparsh")
    val resp = send(w, r, "ornament-note p")
    resp should include ("Sparsh")
  }

  "Ghaseet ornament" should "attach target note" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type g")
    send(w, r, "ornament-start ghaseet")
    val resp = send(w, r, "ornament-note m")
    resp should include ("Ghaseet")
  }

  "Note ornaments at scale" should "handle KanSwar on 20 notes" in withClient { (w, r) =>
    reset(w, r)
    val graceKeys = List('r', 'g', 'm', 'p', 'd', 'n', 's')
    for i <- 0 until 20 do
      send(w, r, s"type ${swarKeys(i % 7)}")
      send(w, r, "ornament-start kanswar")
      send(w, r, s"ornament-note ${graceKeys(i % 7)}")
    getEventCount(w, r) shouldBe 20
  }

  // =====================================================================
  // MEEND — ascending and descending
  // =====================================================================

  "Meend ornament" should "complete ascending meend" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type s")
    val startResp = send(w, r, "ornament-start meend-asc")
    startResp should include ("Meend ascending")
    val note1 = send(w, r, "ornament-note r")
    note1 should include ("Meend")
    val note2 = send(w, r, "ornament-note g")
    note2 should include ("Meend")
    val events = send(w, r, "get-events")
    events should include ("ornaments=1")
  }

  it should "complete descending meend" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type p")
    send(w, r, "ornament-start meend-desc")
    send(w, r, "ornament-note g")
    send(w, r, "ornament-note r")
    val events = send(w, r, "get-events")
    events should include ("ornaments=1")
  }

  it should "handle meend on 20 notes" in withClient { (w, r) =>
    reset(w, r)
    val startKeys = List('r', 'g', 'm', 'p', 'd', 'n', 's')
    val endKeys = List('g', 'm', 'p', 'd', 'n', 's', 'r')
    for i <- 0 until 20 do
      send(w, r, s"type ${swarKeys(i % 7)}")
      val dir = if i % 2 == 0 then "meend-asc" else "meend-desc"
      send(w, r, s"ornament-start $dir")
      send(w, r, s"ornament-note ${startKeys(i % 7)}")
      send(w, r, s"ornament-note ${endKeys(i % 7)}")
    getEventCount(w, r) shouldBe 20
  }

  // =====================================================================
  // KRINTAN
  // =====================================================================

  "Krintan ornament" should "complete in 2 steps" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type g")
    send(w, r, "ornament-start krintan")
    send(w, r, "ornament-note r")
    val resp = send(w, r, "ornament-note s")
    resp should include ("Krintan")
  }

  // =====================================================================
  // MURKI — multi-note collect then finish
  // =====================================================================

  "Murki ornament" should "collect notes then finish" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type s")
    send(w, r, "ornament-start murki")
    send(w, r, "ornament-note r")
    send(w, r, "ornament-note g")
    send(w, r, "ornament-note r")
    val resp = send(w, r, "finish-ornament")
    resp should include ("Murki")
    val events = send(w, r, "get-events")
    events should include ("ornaments=1")
  }

  // =====================================================================
  // ZAMZAMA — multi-note collect then finish
  // =====================================================================

  "Zamzama ornament" should "collect notes then finish" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type m")
    send(w, r, "ornament-start zamzama")
    send(w, r, "ornament-note p")
    send(w, r, "ornament-note d")
    send(w, r, "ornament-note n")
    send(w, r, "ornament-note s")
    val resp = send(w, r, "finish-ornament")
    resp should include ("Zamzama")
  }

  // =====================================================================
  // ALL ORNAMENT TYPES — single composition
  // =====================================================================

  "All ornament types" should "be attachable in one session" in withClient { (w, r) =>
    reset(w, r)

    // Note 1: Gamak
    send(w, r, "type s"); send(w, r, "ornament gamak")
    // Note 2: Andolan
    send(w, r, "type r"); send(w, r, "ornament andolan")
    // Note 3: Gitkari
    send(w, r, "type g"); send(w, r, "ornament gitkari")
    // Note 4: KanSwar
    send(w, r, "type m"); send(w, r, "ornament-start kanswar"); send(w, r, "ornament-note p")
    // Note 5: Sparsh
    send(w, r, "type p"); send(w, r, "ornament-start sparsh"); send(w, r, "ornament-note d")
    // Note 6: Ghaseet
    send(w, r, "type d"); send(w, r, "ornament-start ghaseet"); send(w, r, "ornament-note n")
    // Note 7: Ascending Meend
    send(w, r, "type n")
    send(w, r, "ornament-start meend-asc"); send(w, r, "ornament-note s"); send(w, r, "ornament-note r")
    // Note 8: Descending Meend
    send(w, r, "type s")
    send(w, r, "ornament-start meend-desc"); send(w, r, "ornament-note p"); send(w, r, "ornament-note m")
    // Note 9: Krintan
    send(w, r, "type r")
    send(w, r, "ornament-start krintan"); send(w, r, "ornament-note g"); send(w, r, "ornament-note s")
    // Note 10: Murki
    send(w, r, "type g")
    send(w, r, "ornament-start murki")
    send(w, r, "ornament-note r"); send(w, r, "ornament-note s"); send(w, r, "ornament-note r")
    send(w, r, "finish-ornament")
    // Note 11: Zamzama
    send(w, r, "type m")
    send(w, r, "ornament-start zamzama")
    send(w, r, "ornament-note p"); send(w, r, "ornament-note d")
    send(w, r, "finish-ornament")

    getEventCount(w, r) shouldBe 11
  }

  // =====================================================================
  // SECTION SWITCHING
  // =====================================================================

  "Section switching" should "populate Gat and Antara independently" in withClient { (w, r) =>
    reset(w, r)
    // Gat section (index 0): insert 20 notes
    for i <- 0 until 20 do send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 20

    // Switch to Antara (index 1)
    val switchResp = send(w, r, "section 1")
    switchResp should include ("Switched to section 1")
    getEventCount(w, r) shouldBe 0

    // Insert 20 notes in Antara
    for i <- 0 until 20 do send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 20

    // Switch back to Gat and verify
    send(w, r, "section 0")
    getEventCount(w, r) shouldBe 20
  }

  it should "populate Gat with 3 Taans" in withClient { (w, r) =>
    val resp = reset(w, r, "gat", "teentaal", 3)
    resp should include ("5 sections")

    // Gat: 20 notes
    for i <- 0 until 20 do send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 20

    // Antara: 20 notes
    send(w, r, "section 1")
    for i <- 0 until 20 do send(w, r, s"type ${swarKeys(i % 7)}")

    // Taan 1: 30 notes
    send(w, r, "section 2")
    for i <- 0 until 30 do send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 30

    // Taan 2: 30 notes
    send(w, r, "section 3")
    for i <- 0 until 30 do send(w, r, s"type ${swarKeys(i % 7)}")

    // Taan 3: 30 notes
    send(w, r, "section 4")
    for i <- 0 until 30 do send(w, r, s"type ${swarKeys(i % 7)}")

    // Verify all sections via dump-composition
    val json = send(w, r, "dump-composition")
    json should include ("Teentaal")
    json should include ("Yaman")
  }

  it should "reject out-of-range section" in withClient { (w, r) =>
    reset(w, r)
    val resp = send(w, r, "section 99")
    resp should include ("ERROR")
    resp should include ("out of range")
  }

  // =====================================================================
  // PALTA COMPOSITION
  // =====================================================================

  "Palta composition" should "handle 50 swar" in withClient { (w, r) =>
    reset(w, r, "palta")
    for i <- 0 until 50 do send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 50
  }

  // =====================================================================
  // BANDISH COMPOSITION
  // =====================================================================

  "Bandish composition" should "handle 50 swar in Sthayi" in withClient { (w, r) =>
    reset(w, r, "bandish")
    for i <- 0 until 50 do send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 50
  }

  // =====================================================================
  // DIFFERENT TAALS
  // =====================================================================

  "Different taals" should "handle swar in Jhaptaal" in withClient { (w, r) =>
    reset(w, r, "gat", "jhaptaal")
    for i <- 0 until 30 do send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 30
  }

  it should "handle swar in Rupak" in withClient { (w, r) =>
    reset(w, r, "gat", "rupak")
    for i <- 0 until 30 do send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 30
  }

  // =====================================================================
  // COMBINED STRESS — ornaments + strokes + octaves + subdivisions
  // =====================================================================

  "Combined stress test" should "handle 50 fully-decorated notes" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 50 do
      // Subdivision
      val subdiv = (i % 4) + 1
      send(w, r, s"subdivision $subdiv")

      // Octave
      val octave = (i % 6) match
        case 0 | 1 => "period"
        case 4 | 5 => "quote"
        case _     => "backtick"
      send(w, r, s"octave $octave")

      // Insert note with optional shift
      val key = swarKeys(i % 7)
      val ch = if (i % 5) == 2 then key.toUpper else key
      send(w, r, s"type $ch")

      // Stroke
      val stroke = if i % 2 == 0 then "da" else "ra"
      send(w, r, s"stroke $stroke")

      // Ornament (simple ones only for speed)
      (i % 5) match
        case 0 => send(w, r, "ornament gamak")
        case 1 => send(w, r, "ornament andolan")
        case 2 => send(w, r, "ornament gitkari")
        case 3 => send(w, r, "ornament-start kanswar"); send(w, r, "ornament-note r")
        case _ => () // no ornament

    getEventCount(w, r) shouldBe 50
    val events = send(w, r, "get-events")
    events should include ("stroke=")
    events should include ("ornaments=")
  }

  // =====================================================================
  // FULL MULTI-SECTION STRESS
  // =====================================================================

  "Full Gat composition stress" should "fill multiple sections with mixed input" in withClient { (w, r) =>
    reset(w, r, "gat", "teentaal", 3)

    // Gat section: 30 notes with strokes
    for i <- 0 until 30 do
      send(w, r, s"type ${swarKeys(i % 7)}")
      val stroke = if i % 2 == 0 then "da" else "ra"
      send(w, r, s"stroke $stroke")
    getEventCount(w, r) shouldBe 30

    // Antara: 30 notes with ornaments
    send(w, r, "section 1")
    for i <- 0 until 30 do
      send(w, r, s"type ${swarKeys(i % 7)}")
      if i % 3 == 0 then send(w, r, "ornament gamak")
    getEventCount(w, r) shouldBe 30

    // Taan 1: 40 plain swar
    send(w, r, "section 2")
    for i <- 0 until 40 do send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 40

    // Taan 2: 40 with subdivisions
    send(w, r, "section 3")
    for i <- 0 until 40 do
      send(w, r, s"subdivision ${(i % 4) + 1}")
      send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 40

    // Taan 3: 40 with rests and sustains
    send(w, r, "section 4")
    for i <- 0 until 40 do
      (i % 5) match
        case 3 => send(w, r, "press space")
        case 4 => send(w, r, "press minus")
        case _ => send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 40
  }

  // =====================================================================
  // UNDO HISTORY
  // =====================================================================

  "Undo history" should "track edits via dump-history" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 20 do send(w, r, s"type ${swarKeys(i % 7)}")
    val history = send(w, r, "dump-history")
    val pastLine = history.split("\n").find(_.startsWith("past:"))
    pastLine shouldBe defined
    val pastCount = pastLine.get.split(":")(1).trim.toInt
    pastCount should be > 0
  }

  // =====================================================================
  // CURSOR TRACKING
  // =====================================================================

  "Cursor tracking" should "advance beat correctly" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 32 do send(w, r, s"type ${swarKeys(i % 7)}")
    val info = getCursorInfo(w, r)
    // 32 notes in Teentaal (16 matras): cycle=2, beat=0
    info("cursor.cycle") shouldBe "2"
    info("cursor.beat") shouldBe "0"
  }

  it should "track cursor in Jhaptaal (10 matras)" in withClient { (w, r) =>
    reset(w, r, "gat", "jhaptaal")
    for i <- 0 until 20 do send(w, r, s"type ${swarKeys(i % 7)}")
    val info = getCursorInfo(w, r)
    info("cursor.cycle") shouldBe "2"
    info("cursor.beat") shouldBe "0"
  }

  it should "track cursor in Rupak (7 matras)" in withClient { (w, r) =>
    reset(w, r, "gat", "rupak")
    for i <- 0 until 14 do send(w, r, s"type ${swarKeys(i % 7)}")
    val info = getCursorInfo(w, r)
    info("cursor.cycle") shouldBe "2"
    info("cursor.beat") shouldBe "0"
  }

  // =====================================================================
  // SERIALIZATION ROUND-TRIP
  // =====================================================================

  "Serialization via TCP" should "dump a composition as JSON" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 50 do send(w, r, s"type ${swarKeys(i % 7)}")
    val json = send(w, r, "dump-composition")
    json should not be empty
    json should include ("Teentaal")
    json should include ("Yaman")
  }

  // =====================================================================
  // EDGE CASES
  // =====================================================================

  "Edge cases" should "handle unknown swar key" in withClient { (w, r) =>
    reset(w, r)
    val resp = send(w, r, "type x")
    resp should include ("Unknown")
    getEventCount(w, r) shouldBe 0
  }

  it should "handle rapid insert-delete" in withClient { (w, r) =>
    reset(w, r)
    for _ <- 0 until 20 do
      send(w, r, "type s")
      send(w, r, "press backspace")
    getEventCount(w, r) shouldBe 0
  }

  it should "handle subdivision changes during input" in withClient { (w, r) =>
    reset(w, r)
    for i <- 0 until 20 do
      send(w, r, s"subdivision ${(i % 7) + 2}")
      send(w, r, s"type ${swarKeys(i % 7)}")
    getEventCount(w, r) shouldBe 20
  }

  // =====================================================================
  // LOG VERIFICATION
  // =====================================================================

  "Log output" should "record pushEditor entries for swar input" in withClient { (w, r) =>
    reset(w, r)
    val logsBefore = countPushEditorLogs()
    for i <- 0 until 10 do send(w, r, s"type ${swarKeys(i % 7)}")
    Thread.sleep(200)
    val logsAfter = countPushEditorLogs()
    (logsAfter - logsBefore) should be >= 10
  }

  it should "record pushEditor entries for dual swar" in withClient { (w, r) =>
    reset(w, r)
    val logsBefore = countPushEditorLogs()
    send(w, r, "dual s")
    send(w, r, "dual r")
    Thread.sleep(200)
    val logsAfter = countPushEditorLogs()
    (logsAfter - logsBefore) should be >= 2
  }

  it should "record pushEditor entries with correct event counts" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type s")
    send(w, r, "type r")
    send(w, r, "type g")
    Thread.sleep(200)
    val logs = getLogLines.filter(_.contains("pushEditor:"))
    val last3 = logs.takeRight(3)
    last3.length should be >= 3
    last3.last should include ("events=3")
  }

  // =====================================================================
  // THREAD DUMP (works even when other tests are running)
  // =====================================================================

  "Thread dump" should "return thread information" in withClient { (w, r) =>
    val dump = send(w, r, "thread-dump")
    dump should include ("state=")
    dump should include ("main")
  }

  // =====================================================================
  // CHECK-FOCUS and FOCUS
  // =====================================================================

  // =====================================================================
  // CURSOR-AWARE DELETE (BACKSPACE / DELETE)
  // =====================================================================

  "Cursor-aware backspace" should "delete note at cursor position, not the last note" in withClient { (w, r) =>
    reset(w, r)
    // Type Sa Re Ga Ma Pa (5 notes at beats 0-4)
    for key <- List('s', 'r', 'g', 'm', 'p') do send(w, r, s"type $key")
    getEventCount(w, r) shouldBe 5
    // Move cursor back to beat 2 (Ga)
    send(w, r, "press left")
    send(w, r, "press left")
    send(w, r, "press left")
    val cursor = getCursorInfo(w, r)
    cursor("cursor.beat") shouldBe "2"
    // Delete Ga at cursor
    val resp = send(w, r, "press backspace")
    resp should include ("Deleted note at cursor")
    getEventCount(w, r) shouldBe 4
    // Verify remaining notes: Sa Re Ma Pa (Ga removed)
    val events = send(w, r, "get-events")
    events should include ("Swar Sa")
    events should include ("Swar Re")
    events should not include ("Swar Ga")
    events should include ("Swar Pa")
  }

  it should "delete note before cursor when cursor is on empty beat" in withClient { (w, r) =>
    reset(w, r)
    for key <- List('s', 'r', 'g') do send(w, r, s"type $key")
    getEventCount(w, r) shouldBe 3
    // Cursor is at beat 3 after typing, no note there
    val resp = send(w, r, "press backspace")
    resp should include ("Deleted note before cursor")
    getEventCount(w, r) shouldBe 2
    val events = send(w, r, "get-events")
    events should include ("Swar Sa")
    events should include ("Swar Re")
    events should not include ("Swar Ga")
  }

  it should "not delete the last note when cursor is in the middle" in withClient { (w, r) =>
    reset(w, r)
    for key <- List('s', 'r', 'g', 'm', 'p', 'd', 'n') do send(w, r, s"type $key")
    getEventCount(w, r) shouldBe 7
    // Move to beat 3 (Ma)
    send(w, r, "press left")
    send(w, r, "press left")
    send(w, r, "press left")
    send(w, r, "press left")
    // Delete Ma at beat 3
    send(w, r, "press backspace")
    getEventCount(w, r) shouldBe 6
    // Ni (the last note) should still be there, Ma should be gone
    val events = send(w, r, "get-events")
    events should include ("Swar Ni")
    events should not include ("Swar Ma ")
  }

  "Delete key" should "delete note at cursor without moving cursor back" in withClient { (w, r) =>
    reset(w, r)
    for key <- List('s', 'r', 'g', 'm', 'p') do send(w, r, s"type $key")
    // Move to beat 2 (Ga)
    send(w, r, "press left")
    send(w, r, "press left")
    send(w, r, "press left")
    val resp = send(w, r, "press delete")
    resp should include ("Deleted note at cursor")
    getEventCount(w, r) shouldBe 4
    // Cursor should stay at beat 2
    val cursor = getCursorInfo(w, r)
    cursor("cursor.beat") shouldBe "2"
  }

  it should "report error when no note at cursor" in withClient { (w, r) =>
    reset(w, r)
    send(w, r, "type s")
    // Move cursor to beat 5 (empty)
    for _ <- 0 until 4 do send(w, r, "press right")
    val resp = send(w, r, "press delete")
    resp should include ("No note at cursor")
  }

  "Focus commands" should "report focus state" in withClient { (w, r) =>
    val resp = send(w, r, "check-focus")
    resp should include ("scrollPaneFocused:")
    resp should include ("focusOwner:")
  }

  it should "request focus" in withClient { (w, r) =>
    val resp = send(w, r, "focus")
    resp shouldBe "Focus requested"
  }

  // =====================================================================
  // SET-DEBUG
  // =====================================================================

  "Debug toggle" should "toggle debug logging" in withClient { (w, r) =>
    send(w, r, "set-debug on") should include ("enabled")
    send(w, r, "set-debug off") should include ("disabled")
  }
