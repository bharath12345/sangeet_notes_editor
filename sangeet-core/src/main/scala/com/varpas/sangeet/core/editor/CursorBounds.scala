package com.varpas.sangeet.core.editor

/** Pure cursor-boundary predicates shared by desktop and web.
  *
  * The editor allows the cursor to sit at ONE cycle past the last event-bearing cycle (i.e., on an empty trailing cycle
  * that the user could start filling). Advancing beyond that is rejected so the cursor doesn't visually disappear off
  * the bottom of the rendered grid.
  *
  * Pre-PR-2d, this `cycle <= maxCycle + 1` check was inlined in 5+ places in desktop `EditorKeyHandler` and at least
  * once in web `State.Update.Editor` (Plan-16 B.5a clamp). Centralizing it keeps the magic `+1` in one place. The Elm
  * port at `sangeet-web/src/State/Update/Helpers.elm` mirrors these rules function-for-function.
  */
object CursorBounds:

  /** The first cycle past the editable range — i.e., one cycle beyond the highest cycle that currently holds events.
    * The cursor may visit this cycle (to start filling it) but may not advance further.
    */
  def maxAllowedCycle(maxCycle: Int): Int = maxCycle + 1

  /** True if `candidateCycle` is within the allowed cursor range — i.e., it does not exceed `maxCycle + 1`. */
  def canAdvanceTo(candidateCycle: Int, maxCycle: Int): Boolean =
    candidateCycle <= maxAllowedCycle(maxCycle)
