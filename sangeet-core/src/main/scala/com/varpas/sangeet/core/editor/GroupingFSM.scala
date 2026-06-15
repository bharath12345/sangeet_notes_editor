package com.varpas.sangeet.core.editor

import com.varpas.sangeet.core.model._

/** Pure fast-typing grouping state machine, shared by desktop and web.
  *
  * When a user types swar notes in rapid succession, they should be grouped onto a single beat with equal subdivisions
  * (e.g., typing "sRgm" within ~500ms produces a 4-swar group on one beat). This module encodes the rules that decide
  * when to start a new group, extend an in-progress group, or cancel grouping — without touching any UI, history, or
  * I/O concerns.
  *
  * The same decision tree exists in the desktop `EditorKeyHandler` (where it backs both the interactive typing path and
  * the debug-console `typeCharTimed` path) and in the web `State.Update.Editor.handleGotSwarKeyTime`. Three copies of
  * the same predicate is what drove the bug-4 cursor-drift fix (PR #86) to be written and reviewed three times. This
  * FSM is the canonical reference; the Elm port at `sangeet-web/src/State/Update/Helpers.elm` mirrors these rules
  * function-for-function.
  *
  * # Invariants
  *
  *   - **Sliding 500ms window**: a keystroke that arrives more than `ThresholdMs` after the MOST RECENT keystroke in
  *     the in-progress group cannot extend it. The window is refreshed on every keystroke. Pre-PR-2d, desktop used this
  *     sliding semantics (`lastTypedTime`) and web used an anchored-at-first-keystroke semantics (`startTime`). PR-2d
  *     standardizes on the desktop semantics so a steady typing rhythm just under 500ms/key forms a single group
  *     instead of getting cut off after the second keystroke.
  *   - **4-note cap**: a group can hold at most `MaxGroupSize` notes. The Nth keystroke (N > 4) starts a fresh group.
  *   - **Cursor-alignment guard** (bug 4): if the observed cursor at the next keystroke differs from where the previous
  *     insert advanced the cursor to, the user must have navigated between keystrokes. We start a fresh group rather
  *     than incorrectly collapse the new note onto the old group's beat.
  */
object GroupingFSM:

  /** Maximum number of notes allowed in a single fast-typed group. */
  val MaxGroupSize: Int = 4

  /** Maximum time (in milliseconds) between two consecutive keystrokes for the latter to extend an in-progress group.
    * Window is sliding — refreshed on every keystroke (see class doc).
    */
  val ThresholdMs: Long = 500L

  /** A subset of `CursorModel` carrying only the fields used by the alignment check. We don't take a full `CursorModel`
    * because callers track different shapes (the web client doesn't materialize `Octave` on every snapshot).
    */
  final case class CursorTriple(beat: Int, cycle: Int, subIndex: Int)

  object CursorTriple:
    def of(cursor: CursorModel): CursorTriple =
      CursorTriple(cursor.beat, cursor.cycle, cursor.subIndex)

  /** A note that participates in a group. Kept as a triple to mirror what `KeyHandler.handleSwarGroup` already takes,
    * so callers can pass `state.notes` straight through after appending the new note.
    */
  type GroupedNote = (Note, Variant, Octave)

  /** In-progress group state.
    *
    *   - `notes`: the notes already inserted, in typing order.
    *   - `beat`/`cycle`: the cursor position before the FIRST note of the group was inserted. Both platforms preserve
    *     this for diagnostics and for the undo-and-replay path that rewrites the group at this beat.
    *   - `lastTypedTimeMs`: the timestamp of the most recent keystroke in this group. Used for the sliding 500ms
    *     window. Updated on every successful `Extend`.
    *   - `nextCursor`: where the editor advanced the cursor to after the most recent insert. The next keystroke
    *     compares the observed cursor against this; a mismatch trips the bug-4 guard.
    */
  final case class State(
      notes: List[GroupedNote],
      beat: Int,
      cycle: Int,
      lastTypedTimeMs: Long,
      nextCursor: CursorTriple
  )

  /** The action a host should perform in response to a new keystroke. */
  enum Decision:
    /** Start a brand-new group. The host should insert the single note normally (`handleSwarKey`-style) and seed a new
      * `State` via `startedState(...)`.
      */
    case StartNew

    /** Extend the existing group by appending `thisNote` to the previous group's notes. The host should undo the
      * previous insert, replay `handleSwarGroup` with `allNotes`, and call `extendedState(...)` with the new cursor.
      *
      * `allNotes` is provided pre-computed because every caller needs it.
      */
    case Extend(allNotes: List[GroupedNote])

  /** Decide what to do with `thisNote` typed at `nowMs` while the cursor is at `observed`.
    *
    *   - `currentState = None` → always `StartNew` (no in-progress group).
    *   - In-progress group + within-threshold + under cap + cursor aligned → `Extend(state.notes :+ thisNote)`.
    *   - Otherwise → `StartNew` (timed out, full, or cursor moved).
    *
    * This function does NOT mutate state, perform the insert, or touch history. The host is responsible for executing
    * the decision and producing the next `State` via `startedState` / `extendedState`.
    */
  def decide(
      currentState: Option[State],
      nowMs: Long,
      observed: CursorTriple,
      thisNote: GroupedNote
  ): Decision =
    currentState match
      case None => Decision.StartNew
      case Some(gs) =>
        val withinWindow = (nowMs - gs.lastTypedTimeMs) < ThresholdMs
        val underCap     = gs.notes.size < MaxGroupSize
        val aligned      = cursorMatches(observed, gs.nextCursor)
        if withinWindow && underCap && aligned then Decision.Extend(gs.notes :+ thisNote)
        else Decision.StartNew

  /** Cursor-alignment predicate (bug 4 guard). Beat / cycle / subIndex must all match; the rest of `CursorModel`
    * (selection anchors, octave overrides) is metadata that shouldn't cancel a group.
    */
  def cursorMatches(observed: CursorTriple, expected: CursorTriple): Boolean =
    observed.beat == expected.beat &&
      observed.cycle == expected.cycle &&
      observed.subIndex == expected.subIndex

  /** Construct the next `State` after the host has executed an `Extend(allNotes)` decision.
    *
    *   - `nowMs` is the timestamp of the keystroke that triggered the extend; it slides the window forward.
    *   - `newNextCursor` is the cursor the editor advanced to after the `handleSwarGroup` replay.
    */
  def extendedState(
      previous: State,
      allNotes: List[GroupedNote],
      nowMs: Long,
      newNextCursor: CursorTriple
  ): State =
    previous.copy(notes = allNotes, lastTypedTimeMs = nowMs, nextCursor = newNextCursor)

  /** Construct the initial `State` for a new group, given the cursor the editor advanced to after the first single
    * insert.
    */
  def startedState(
      preInsertCursor: CursorTriple,
      thisNote: GroupedNote,
      nowMs: Long,
      postInsertCursor: CursorTriple
  ): State =
    State(
      notes = List(thisNote),
      beat = preInsertCursor.beat,
      cycle = preInsertCursor.cycle,
      lastTypedTimeMs = nowMs,
      nextCursor = postInsertCursor
    )
