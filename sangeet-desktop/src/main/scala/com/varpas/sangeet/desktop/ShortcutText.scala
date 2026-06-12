package com.varpas.sangeet.desktop

/** Single source of truth for how keyboard shortcuts render in tooltips and the cheat-sheet. macOS gets `⌘`/`⇧`/`⌥`;
  * everywhere else gets `Ctrl+`/`Shift+`/`Alt+`. The OS is detected once at class-load and cached.
  */
object ShortcutText:

  val isMac: Boolean = System.getProperty("os.name", "").toLowerCase.contains("mac")

  private val mod   = if isMac then "⌘" else "Ctrl+"
  private val shift = if isMac then "⇧" else "Shift+"
  private val alt   = if isMac then "⌥" else "Alt+"

  /** Build a display string. `key` is the bare key name (e.g. "N", "Tab", "/", ","). */
  def shortcut(key: String, withShift: Boolean = false, withAlt: Boolean = false): String =
    val parts = List(
      if withShift then shift else "",
      if withAlt then alt else "",
      mod,
      key
    )
    parts.mkString

  /** Suffix in parentheses, ready to drop onto the end of a tooltip. */
  def parens(key: String, withShift: Boolean = false, withAlt: Boolean = false): String =
    s" (${shortcut(key, withShift, withAlt)})"

  /** Bare modifier-less keys (F1, ?). */
  def plain(key: String): String = key

  def plainParens(key: String): String = s" ($key)"
