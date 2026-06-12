package com.varpas.sangeet.desktop.diagnostics

import java.time.Instant

import scala.collection.mutable

import io.circe.Json

/** In-memory rolling buffer of user-visible events that ride along in a bug report so a human can reconstruct what
  * happened before the user clicked Send. Singleton object — the editor has only one window in the JavaFX runtime, so a
  * global buffer is the right shape and dodges threading the dependency through every place that wants to record.
  *
  * Distinct from [[com.varpas.sangeet.desktop.editor.AppLogger]] which writes to disk for crash forensics. This one
  * lives entirely in RAM, caps memory aggressively, and only escapes the process when the user explicitly opts in via
  * the Report-a-Bug flow.
  */
object EventLogger:

  enum LoggedEvent(val timestamp: Long):
    case Key(t: Long, code: String, modifiers: List[String])      extends LoggedEvent(t)
    case Lifecycle(t: Long, kind: String, detail: Option[String]) extends LoggedEvent(t)

  // Tunables. Five minutes mirrors the web rrweb buffer. The hard cap is a
  // defensive backstop in case sustained key spam outruns the time-window
  // sweep — at ~10 keys/sec, 5000 events = ~8 minutes of typing.
  private val MaxAgeMs: Long = 5 * 60 * 1000L
  private val MaxEvents: Int = 5000

  // mutable.Queue gives O(1) enqueue + dequeue from both ends, which is what
  // the eviction sweep needs.
  private val buffer = mutable.Queue.empty[LoggedEvent]

  /** Record a key event. Always-on, called from `EditorKeyHandler` before it consumes the event so the buffer reflects
    * intent regardless of whether the handler succeeded.
    */
  def recordKey(code: String, modifiers: List[String]): Unit =
    record(LoggedEvent.Key(now(), code, modifiers))

  /** Record an app-lifecycle event. Detail is a free-form string (filename, tab id, etc.) — kept loose because the
    * shape varies per call site.
    */
  def recordLifecycle(kind: String, detail: Option[String] = None): Unit =
    record(LoggedEvent.Lifecycle(now(), kind, detail))

  private def record(event: LoggedEvent): Unit = buffer.synchronized {
    buffer.enqueue(event)
    evict()
  }

  /** Evict events older than 5 min and trim to the hard cap. Called on every record so the buffer is always coherent —
    * cheap because the work is bounded by how many events arrived since the last call.
    */
  private def evict(): Unit =
    val cutoff = now() - MaxAgeMs
    while buffer.nonEmpty && buffer.head.timestamp < cutoff do
      val _ = buffer.dequeue()
    while buffer.size > MaxEvents do
      val _ = buffer.dequeue()

  /** Snapshot the current buffer as a list of circe Json events for inclusion in a bug-report payload. Returns a copy —
    * caller is free to mutate the buffer afterwards (e.g., the screenshot capture path adds more events).
    */
  def snapshot(): List[Json] = buffer.synchronized {
    buffer.toList.map(toJson)
  }

  /** Clear the buffer. Exposed mainly for tests; production code does not call this. */
  def clear(): Unit = buffer.synchronized {
    buffer.clear()
  }

  /** Current buffer size — for status-bar / diagnostics readouts. */
  def size: Int = buffer.synchronized(buffer.size)

  private def now(): Long = Instant.now().toEpochMilli

  private def toJson(e: LoggedEvent): Json = e match
    case LoggedEvent.Key(t, code, mods) =>
      Json.obj(
        "type"      -> Json.fromString("key"),
        "timestamp" -> Json.fromLong(t),
        "code"      -> Json.fromString(code),
        "modifiers" -> Json.fromValues(mods.map(Json.fromString))
      )
    case LoggedEvent.Lifecycle(t, kind, detail) =>
      val base = Json.obj(
        "type"      -> Json.fromString("lifecycle"),
        "timestamp" -> Json.fromLong(t),
        "kind"      -> Json.fromString(kind)
      )
      detail.fold(base)(d => base.deepMerge(Json.obj("detail" -> Json.fromString(d))))
