package com.varpas.sangeet.desktop.action

/** An action discoverable from the command palette and the cheat sheet. The shortcut is a display string (e.g. "⌘N") —
  * actual key binding lives in MainApp's scene event filter. The `run` callback fires the action; for toolbar buttons
  * it's `button.fire()` so analytics + status-bar logging stays consistent.
  */
case class AppAction(
    title: String,
    group: String,
    shortcut: Option[String],
    run: () => Unit
)

object AppAction:
  /** Lowercase haystack/needle substring match. Empty needle matches everything. Returns matches in the original list's
    * order — no relevance scoring. If the palette grows past ~30 actions, swap in a fuzzy ranker.
    */
  def filter(actions: List[AppAction], needle: String): List[AppAction] =
    val n = needle.trim.toLowerCase
    if n.isEmpty then actions
    else actions.filter(a => a.title.toLowerCase.contains(n) || a.group.toLowerCase.contains(n))
