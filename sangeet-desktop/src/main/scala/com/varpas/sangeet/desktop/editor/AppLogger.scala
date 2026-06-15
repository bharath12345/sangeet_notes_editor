package com.varpas.sangeet.desktop.editor

import java.nio.file.Path
import java.util.logging._

object AppLogger:
  private val logger                 = Logger.getLogger("sangeet")
  @volatile private var debugEnabled = false

  def initialize(): Path =
    val logPath =
      if System.getProperty("os.name", "").toLowerCase.contains("win") then
        Path.of(System.getenv("TEMP"), "sangeet-notes-editor.log")
      else Path.of("/tmp", "sangeet-notes-editor.log")

    logger.setUseParentHandlers(false)
    logger.setLevel(Level.ALL)

    val handler = new FileHandler(
      logPath.toString.replace(".log", ".%g.log"), // pattern for rolling
      20 * 1024 * 1024,                            // 20MB per file
      5,                                           // 5 files max = 100MB total
      true                                         // append
    )
    handler.setLevel(Level.INFO)
    handler.setFormatter(
      new SimpleFormatter():
        override def format(record: LogRecord): String =
          val time  = java.time.LocalDateTime.now().toString
          val level = record.getLevel.getName
          s"$time [$level] ${record.getMessage}\n"
    )
    logger.addHandler(handler)

    info("=== Sangeet Notes Editor started ===")
    info(s"Log file: $logPath")
    logPath

  def info(msg: String): Unit =
    logger.log(Level.INFO, msg)

  def warn(msg: String): Unit =
    logger.log(Level.WARNING, msg)

  /** Log a recoverable error with optional throwable context. Use this when an exception was caught and silently
    * recovered from (silent fallback, best-effort cleanup) so investigators can correlate the user-visible symptom
    * against the underlying cause.
    */
  def warn(msg: String, t: Throwable): Unit =
    logger.log(Level.WARNING, s"$msg: ${t.getClass.getSimpleName}: ${t.getMessage}")

  def debug(msg: String): Unit =
    if debugEnabled then logger.log(Level.FINE, msg)

  def setDebugEnabled(enabled: Boolean): Unit =
    debugEnabled = enabled
    logger.getHandlers.foreach { h =>
      h.setLevel(if enabled then Level.ALL else Level.INFO)
    }
    info(s"Debug logging ${if enabled then "enabled" else "disabled"}")

  def isDebugEnabled: Boolean = debugEnabled
