package com.varpas.sangeet.desktop.diagnostics

import java.io.{PrintWriter, StringWriter}
import java.nio.file.{Files, Path, StandardOpenOption}
import java.time.Instant
import java.util.UUID

import io.circe.Json

/** Catches uncaught exceptions, writes a JSON sentinel file with the stack trace + EventLogger snapshot + metadata into
  * `~/.sangeet/crash-pending/`. On the next launch [[MainApp]] scans that directory; one file per pending crash. When
  * the user sends a recovery report (or chooses Discard) the file is deleted.
  *
  * Two reasons this lives outside [[BugReportClient]]:
  *   1. Crashes happen at the worst possible moments — disk full, JVM mid-shutdown, FX thread already torn down. We
  *      want zero network or async machinery in the crash path; just write a small file synchronously and let the JVM
  *      die. 2. The send happens on the *next* launch, not at crash time, so the recovery flow can do it cleanly via
  *      the already-tested BugReportClient.
  *
  * Wrap everything in try/catch so the handler itself never throws — secondary failure is silent. We tried.
  */
object CrashCapture:

  /** Subdirectory under the user home where crash sentinel files live. Chosen to be the same parent dir as the
    * AppLogger output so a future cleanup script can scoop both.
    */
  val SentinelDir: Path = Path.of(System.getProperty("user.home"), ".sangeet", "crash-pending")

  /** Install both handlers. Call once at app startup. */
  def install(): Unit =
    try
      Files.createDirectories(SentinelDir)
      Thread.setDefaultUncaughtExceptionHandler((thread, throwable) => handle(thread, throwable))
    catch case _: Throwable => () // never break startup

  /** Install on a specific thread (e.g., the JavaFX Application Thread, which has its own handler chain that the
    * default doesn't always cover).
    */
  def installOnCurrentThread(): Unit =
    try Thread.currentThread.setUncaughtExceptionHandler((thread, throwable) => handle(thread, throwable))
    catch case _: Throwable => ()

  /** Serialize the crash to a JSON file. Public for testing — production code only hits this via the installed
    * handlers.
    */
  def handle(thread: Thread, throwable: Throwable): Unit =
    try
      val crashId = UUID.randomUUID().toString
      val payload = serialize(thread, throwable)
      // Self-sufficient: don't assume install() ran successfully. The cost is
      // one syscall per crash, which is dwarfed by the JVM tearing down.
      Files.createDirectories(SentinelDir)
      val target = SentinelDir.resolve(s"$crashId.json")
      Files.write(
        target,
        payload.noSpaces.getBytes("UTF-8"),
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
      // stderr not stdout — stdout may have been closed by a shutdown hook
      System.err.println(s"[crash-capture] Wrote crash sentinel to $target")
    catch
      case t: Throwable =>
        // Secondary failure: there's nothing useful to do. Log to stderr and
        // let the JVM continue to die.
        try System.err.println(s"[crash-capture] handler failed: ${t.getClass.getSimpleName}: ${t.getMessage}")
        catch case _: Throwable => ()

  /** List all pending crash files. Returns empty if the directory doesn't exist. Used by MainApp at startup. */
  def pending(): List[Path] =
    try
      if !Files.isDirectory(SentinelDir) then List.empty
      else
        val stream = Files.list(SentinelDir)
        try
          import scala.jdk.CollectionConverters.*
          stream
            .iterator()
            .asScala
            .filter(p => p.getFileName.toString.endsWith(".json"))
            .toList
        finally stream.close()
    catch case _: Throwable => List.empty

  /** Read a sentinel file and return the parsed JSON. None if read or parse fails. */
  def read(path: Path): Option[Json] =
    try
      val bytes = Files.readAllBytes(path)
      io.circe.parser.parse(new String(bytes, "UTF-8")).toOption
    catch case _: Throwable => None

  /** Delete a sentinel file after the user sends or discards. */
  def delete(path: Path): Unit =
    try Files.deleteIfExists(path)
    catch case _: Throwable => ()

  /** Internal — build the JSON payload. Visible for tests. */
  def serialize(thread: Thread, throwable: Throwable): Json =
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    throwable.printStackTrace(pw)
    pw.flush()
    val stackTrace = sw.toString

    Json.obj(
      "crashId"     -> Json.fromString(UUID.randomUUID().toString),
      "timestamp"   -> Json.fromString(Instant.now().toString),
      "threadName"  -> Json.fromString(Option(thread).map(_.getName).getOrElse("unknown")),
      "exception"   -> Json.fromString(Option(throwable.getClass.getName).getOrElse("unknown")),
      "message"     -> Json.fromString(Option(throwable.getMessage).getOrElse("")),
      "stackTrace"  -> Json.fromString(stackTrace),
      "eventLogger" -> Json.fromValues(EventLogger.snapshot()),
      "metadata" -> Json.obj(
        "os" -> Json.fromString(
          s"${sys.props.getOrElse("os.name", "?")} ${sys.props.getOrElse("os.version", "?")}"
        ),
        "javaVersion" -> Json.fromString(sys.props.getOrElse("java.version", "?")),
        "appVersion"  -> Json.fromString("1.0")
      )
    )
