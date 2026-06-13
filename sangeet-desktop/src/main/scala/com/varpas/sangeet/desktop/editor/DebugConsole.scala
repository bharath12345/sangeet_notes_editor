package com.varpas.sangeet.desktop.editor

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{InetAddress, ServerSocket, Socket}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{CompletableFuture, CopyOnWriteArrayList, TimeUnit}

class DebugConsole(tabManager: TabManager, statusBar: StatusBar, port: Int = 28081):

  private val running                            = new AtomicBoolean(false)
  private val activeClients                      = new CopyOnWriteArrayList[Socket]()
  private var serverSocket: Option[ServerSocket] = None
  private var acceptThread: Option[Thread]       = None
  private val END_MARKER                         = "---END---"
  private val commandHandler                     = new DebugCommandHandler(tabManager, statusBar)

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
      try ss.close()
      catch case _: Exception => ()
    }
    activeClients.forEach { s =>
      try s.close()
      catch case _: Exception => ()
    }
    activeClients.clear()

  private def acceptLoop(): Unit =
    val ss = serverSocket.getOrElse(
      return
    )
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
          if running.get() then AppLogger.info(s"Debug console accept error: ${ex.getMessage}")

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
          val response =
            try dispatch(trimmed)
            catch case ex: Exception => s"ERROR: ${ex.getMessage}"
          writer.println(response)
          writer.println(END_MARKER)
        line = reader.readLine()
    catch
      case _: java.net.SocketException => ()
      case _: java.io.IOException      => ()
    finally
      activeClients.remove(socket)
      try socket.close()
      catch case _: Exception => ()

  private def dispatch(input: String): String =
    runOnFx(commandHandler.handleCommand(input))

  private def runOnFx(f: => String): String =
    if javafx.application.Platform.isFxApplicationThread then
      try f
      catch case ex: Exception => s"ERROR: ${ex.getMessage}"
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
