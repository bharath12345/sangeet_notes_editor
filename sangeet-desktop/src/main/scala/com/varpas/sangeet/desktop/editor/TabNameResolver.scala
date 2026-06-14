package com.varpas.sangeet.desktop.editor

/** Pure helpers for resolving duplicate tab-title collisions when opening / creating tabs. Extracted from TabManager so
  * the rename logic (`abc` → `abc (2)`) can be unit-tested without spinning up a JavaFX toolkit.
  */
object TabNameResolver:

  /** True if `title` already appears in `existing`. Case-sensitive — matches what the renderer shows. */
  def hasCollision(title: String, existing: Seq[String]): Boolean =
    existing.contains(title)

  /** Generate a unique title by appending ` (N)`. Picks the lowest N (starting at 2) such that `"$baseTitle (N)"` is
    * not in `existing`.
    *
    * If `baseTitle` itself already ends with ` (N)`, the digits-suffix is stripped before re-applying — so re-resolving
    * `"abc (2)"` against a set that contains it returns `"abc (3)"`, not `"abc (2) (2)"`.
    */
  def nextAvailableTitle(baseTitle: String, existing: Seq[String]): String =
    val stripped          = stripParenSuffix(baseTitle)
    val set               = existing.toSet
    def candidate(n: Int) = s"$stripped ($n)"
    var n                 = 2
    while set.contains(candidate(n)) do n += 1
    candidate(n)

  /** Strip a trailing ` (N)` (one or more digits, parenthesised) from a tab title so the auto-rename suffix doesn't
    * compound on repeated resolutions.
    */
  private val parenSuffix = """^(.*?)\s*\(\d+\)\s*$""".r

  def stripParenSuffix(title: String): String =
    title match
      case parenSuffix(stem) => stem
      case other             => other

  /** Outcome of asking the user how to handle a duplicate tab title. */
  enum DuplicateResolution:
    case Switch
    case Rename(newTitle: String)
    case Cancel
