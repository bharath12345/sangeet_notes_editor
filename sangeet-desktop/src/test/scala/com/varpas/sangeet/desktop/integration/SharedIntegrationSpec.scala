package com.varpas.sangeet.desktop.integration

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.Socket
import java.nio.file.{Files, Path, Paths}

import scala.jdk.CollectionConverters._
import scala.compiletime.uninitialized

import io.circe.parser.{decode, parse}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.core.debug.{DebugCommand, ExpectedState, TestDefinition, TestStep}
import com.varpas.sangeet.desktop.editor.{AppLogger, DebugConsole, StatusBar, TabManager}

class SharedIntegrationSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll:

  private val testPort                   = 28083 // Different port from DebugConsoleTcpSpec to avoid conflicts
  private var tabManager: TabManager     = uninitialized
  private var statusBar: StatusBar       = uninitialized
  private var debugConsole: DebugConsole = uninitialized

  // Discover test files at load time
  private val testsDir  = Paths.get("tests/integration")
  private val goldenDir = testsDir.resolve("golden")

  private def resolveTestsDir: Path =
    if Files.isDirectory(testsDir) then testsDir
    else Paths.get(System.getProperty("user.dir"), "../tests/integration")

  private val testFiles: List[Path] =
    val dir = resolveTestsDir
    if Files.isDirectory(dir) then
      Files
        .list(dir)
        .iterator
        .asScala
        .filter(_.toString.endsWith(".json"))
        .toList
        .sortBy(_.getFileName.toString)
    else Nil

  override def beforeAll(): Unit =
    super.beforeAll()
    // Start JavaFX platform
    try javafx.application.Platform.startup(() => ())
    catch case _: IllegalStateException => () // already started
    javafx.application.Platform.setImplicitExit(false)
    AppLogger.initialize()

    // Initialize the TabManager and DebugConsole on the FX thread
    val latch = new java.util.concurrent.CountDownLatch(1)
    javafx.application.Platform.runLater(() =>
      statusBar = new StatusBar()
      tabManager = new TabManager(statusBar)
      tabManager.newTab()
      latch.countDown()
    )
    latch.await(10, java.util.concurrent.TimeUnit.SECONDS)

    debugConsole = new DebugConsole(tabManager, statusBar, testPort)
    debugConsole.start()
    Thread.sleep(200) // let server socket bind

  override def afterAll(): Unit =
    debugConsole.stop()
    super.afterAll()

  // Generate one test per JSON file
  testFiles.foreach { path =>
    val raw = new String(Files.readAllBytes(path), "UTF-8")
    val defn = decode[TestDefinition](raw).fold(
      err => throw new RuntimeException(s"Failed to parse $path: $err"),
      identity
    )

    s"${defn.name}" should "produce expected state on desktop (TCP)" in {
      withTcpClient { client =>
        defn.steps.foreach(step => executeStep(client, step))
      }
    }
  }

  // ---------------------------------------------------------------------------
  // TCP plumbing — copied from DebugConsoleTcpSpec
  // ---------------------------------------------------------------------------

  private def withTcpClient[T](f: TcpClient => T): T =
    val client = new TcpClient("127.0.0.1", testPort)
    try f(client)
    finally client.close()

  // ---------------------------------------------------------------------------
  // Step dispatcher
  // ---------------------------------------------------------------------------

  private def executeStep(client: TcpClient, step: TestStep): Unit =
    step match
      case TestStep.Cmd(cmd) =>
        val response = client.send(cmd.toTcpText)
        // For dispatch commands, just verify we didn't get an ERROR line.
        if response.startsWith("ERROR") then throw new RuntimeException(s"Command failed: $cmd → $response")

      case TestStep.Checkpoint(expect) =>
        val stateJson = client.send("get-state")
        assertExpectedState(stateJson, expect)

      case TestStep.AssertGoldenSwar(fixture) =>
        val actual            = client.send("dump-composition")
        val resolvedGoldenDir = if Files.isDirectory(goldenDir) then goldenDir else resolveTestsDir.resolve("golden")
        val expected = new String(Files.readAllBytes(resolvedGoldenDir.resolve(fixture.stripPrefix("golden/"))))
        actual shouldBe expected

      case TestStep.AssertGoldenHtml(fixture) =>
        val actual            = client.send("export-html")
        val resolvedGoldenDir = if Files.isDirectory(goldenDir) then goldenDir else resolveTestsDir.resolve("golden")
        val expected = new String(Files.readAllBytes(resolvedGoldenDir.resolve(fixture.stripPrefix("golden/"))))
        actual shouldBe expected

  private def assertExpectedState(stateJson: String, expect: ExpectedState): Unit =
    val parsed = parse(stateJson).getOrElse(
      throw new RuntimeException(s"State response was not valid JSON: $stateJson")
    )

    val cursor = parsed.hcursor

    expect.eventCount.foreach { e =>
      cursor.downField("eventCount").as[Int] shouldBe Right(e)
    }
    expect.cursorBeat.foreach { e =>
      cursor.downField("cursorBeat").as[Int] shouldBe Right(e)
    }
    expect.cursorCycle.foreach { e =>
      cursor.downField("cursorCycle").as[Int] shouldBe Right(e)
    }
    expect.sectionName.foreach { e =>
      cursor.downField("sectionName").as[String] shouldBe Right(e)
    }
    expect.taalName.foreach { e =>
      cursor.downField("taalName").as[String] shouldBe Right(e)
    }
    expect.raagName.foreach { e =>
      cursor.downField("raagName").as[String] shouldBe Right(e)
    }
    expect.sectionCount.foreach { e =>
      cursor.downField("sectionCount").as[Int] shouldBe Right(e)
    }

  // ---------------------------------------------------------------------------
  // DebugCommand → TCP text format converter
  // ---------------------------------------------------------------------------

  extension (cmd: DebugCommand)
    private def toTcpText: String = cmd match
      case DebugCommand.Ping                    => "ping"
      case DebugCommand.Help                    => "help"
      case DebugCommand.ThreadDump              => "thread-dump"
      case DebugCommand.SetDebug(enabled)       => s"set-debug $enabled"
      case DebugCommand.ThrowCrash              => "throw-crash"
      case DebugCommand.ListTabs                => "list-tabs"
      case DebugCommand.SelectTab(id)           => s"select-tab $id"
      case DebugCommand.NewTab                  => "new-tab"
      case DebugCommand.CloseTab(id)            => s"close-tab $id"
      case DebugCommand.TabInfo                 => "tab-info"
      case DebugCommand.Reset(t, r, ta)         => r.fold(s"reset $t $ta")(raag => s"reset $t $raag $ta")
      case DebugCommand.SetTaal(taal)           => s"set-taal $taal"
      case DebugCommand.CheckFocus              => "check-focus"
      case DebugCommand.FocusEditor             => "focus-editor"
      case DebugCommand.SetOctave(o)            => s"set-octave $o"
      case DebugCommand.SetSubdivision(n)       => s"set-subdivision $n"
      case DebugCommand.TypeChar(s)             => s"type $s"
      case DebugCommand.Press(key)              => s"press $key"
      case DebugCommand.TypeTimed(s, d)         => s"type-timed $s $d"
      case DebugCommand.DualSwar(first, second) => s"dual-swar $first $second"
      case DebugCommand.SwarGroup(notes)        => s"swar-group ${notes.mkString(" ")}"
      case DebugCommand.Stroke(stroke)          => s"stroke $stroke"
      case DebugCommand.SimpleOrnament(name)    => s"simple-ornament $name"
      case DebugCommand.OrnamentStart(kind)     => s"ornament-start $kind"
      case DebugCommand.OrnamentNote(note)      => s"ornament-note $note"
      case DebugCommand.FinishOrnament          => "finish-ornament"
      case DebugCommand.SwitchSection(idx)      => s"switch-section $idx"
      case DebugCommand.GetState                => "get-state"
      case DebugCommand.GetEvents               => "get-events"
      case DebugCommand.DumpComposition         => "dump-composition"
      case DebugCommand.DumpHistory             => "dump-history"
      case DebugCommand.ExportHtml              => "export-html"

// ---------------------------------------------------------------------------
// TCP Client — copied from DebugConsoleTcpSpec
// ---------------------------------------------------------------------------

class TcpClient(host: String, port: Int):
  private val socket = new Socket(host, port)
  private val writer = new PrintWriter(socket.getOutputStream, true)
  private val reader = new BufferedReader(new InputStreamReader(socket.getInputStream, "UTF-8"))

  // Consume welcome message
  readUntilEnd()

  def send(line: String): String =
    writer.println(line)
    readUntilEnd()

  private def readUntilEnd(): String =
    val sb   = new StringBuilder
    var line = reader.readLine()
    while line != null && line != "---END---" do
      if sb.nonEmpty then sb.append("\n")
      sb.append(line)
      line = reader.readLine()
    sb.toString

  def close(): Unit =
    socket.close()
