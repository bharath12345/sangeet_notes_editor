package com.varpas.sangeet.desktop.diagnostics

/** Strongly-typed event vocabulary for desktop analytics. Capped at the events worth tracking for MVP — each new event
  * has to earn its place because every additional series shows up in the PostHog dashboard and competes for attention.
  * Adding events later is cheap; deprecating them is not.
  *
  * Convention:
  *   - `name` is the snake_case identifier PostHog stores under "event" — must stay stable once shipped
  *   - `props` is the per-event property bag; serialized as the `properties` map on the capture call
  *   - Global properties (appVersion, os, javaVersion) are merged in by HttpPostHogClient at send time, not here
  */
sealed trait DesktopEvent:
  def name: String
  def props: Map[String, AnyRef]

object DesktopEvent:

  // Lifecycle — bracket every session
  final case class AppStarted(
      appVersion: String,
      os: String,
      osVersion: String,
      javaVersion: String,
      screenW: Int,
      screenH: Int
  ) extends DesktopEvent:
    val name: String = "app_started"
    def props: Map[String, AnyRef] = Map(
      "appVersion"  -> appVersion,
      "os"          -> os,
      "osVersion"   -> osVersion,
      "javaVersion" -> javaVersion,
      "screenW"     -> Integer.valueOf(screenW),
      "screenH"     -> Integer.valueOf(screenH)
    )

  final case class AppQuit(sessionDurationMs: Long, swarInputCount: Int) extends DesktopEvent:
    val name: String = "app_quit"
    def props: Map[String, AnyRef] = Map(
      "sessionDurationMs" -> java.lang.Long.valueOf(sessionDurationMs),
      "swarInputCount"    -> Integer.valueOf(swarInputCount)
    )

  // Tab + composition lifecycle
  case object TabOpened extends DesktopEvent:
    val name: String               = "tab_opened"
    val props: Map[String, AnyRef] = Map.empty

  /** source ∈ {"file-browser", "open-button", "restored"} — kept as a String to avoid an enum churn cost when we add
    * new entry points (drag-and-drop, recent-files menu, etc.). Low cardinality regardless.
    */
  final case class CompositionOpened(taalName: String, source: String) extends DesktopEvent:
    val name: String               = "composition_opened"
    def props: Map[String, AnyRef] = Map("taalName" -> taalName, "source" -> source)

  final case class CompositionCreated(compositionType: String, taalName: String) extends DesktopEvent:
    val name: String               = "composition_created"
    def props: Map[String, AnyRef] = Map("compositionType" -> compositionType, "taalName" -> taalName)

  case object CompositionSaved extends DesktopEvent:
    val name: String               = "composition_saved"
    val props: Map[String, AnyRef] = Map.empty

  case object CompositionExportedHtml extends DesktopEvent:
    val name: String               = "composition_exported_html"
    val props: Map[String, AnyRef] = Map.empty

  // Editing
  case object SectionAdded extends DesktopEvent:
    val name: String               = "section_added"
    val props: Map[String, AnyRef] = Map.empty

  case object SectionRemoved extends DesktopEvent:
    val name: String               = "section_removed"
    val props: Map[String, AnyRef] = Map.empty

  case object SectionCleared extends DesktopEvent:
    val name: String               = "section_cleared"
    val props: Map[String, AnyRef] = Map.empty

  /** ornamentType uses the ScalaFX-side identifier ("Gamak", "Meend", etc.) — kept as a String because the editor's
    * KEY_PRESSED branches already deal with these as raw names.
    */
  final case class OrnamentAdded(ornamentType: String) extends DesktopEvent:
    val name: String               = "ornament_added"
    def props: Map[String, AnyRef] = Map("ornamentType" -> ornamentType)

  case object ScriptChanged extends DesktopEvent:
    val name: String               = "script_changed"
    val props: Map[String, AnyRef] = Map.empty

  case object ThemeToggled extends DesktopEvent:
    val name: String               = "theme_toggled"
    val props: Map[String, AnyRef] = Map.empty

  case object PropertiesEdited extends DesktopEvent:
    val name: String               = "properties_edited"
    val props: Map[String, AnyRef] = Map.empty

  // Diagnostics — close the loop with the Phase 8 + Phase 9 flows
  case object BugReportSent extends DesktopEvent:
    val name: String               = "bug_report_sent"
    val props: Map[String, AnyRef] = Map.empty

  case object CrashRecoverySent extends DesktopEvent:
    val name: String               = "crash_recovery_sent"
    val props: Map[String, AnyRef] = Map.empty

  case object CrashRecoveryDiscarded extends DesktopEvent:
    val name: String               = "crash_recovery_discarded"
    val props: Map[String, AnyRef] = Map.empty

  /** Dialog opens are folded into a single event with `name` as a prop — keeps the event count small while still
    * letting dashboards group by dialog.
    */
  final case class DialogOpened(dialog: String) extends DesktopEvent:
    val name: String               = "dialog_opened"
    def props: Map[String, AnyRef] = Map("dialog" -> dialog)
