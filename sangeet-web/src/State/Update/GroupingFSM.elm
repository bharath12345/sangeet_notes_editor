module State.Update.GroupingFSM exposing
    ( CursorTriple
    , Decision(..)
    , GroupedNote
    , cursorMatches
    , cursorTripleFromCursor
    , decide
    , extendedState
    , maxGroupSize
    , startedState
    , thresholdMs
    )

{- This is a hand-port of GroupingFSM in sangeet-core.
   Source of truth: sangeet-core/src/main/scala/com/varpas/sangeet/core/editor/GroupingFSM.scala
   When changing logic here, update both sides + verify against GroupingFSMSpec.

   Pure fast-typing grouping state machine. When a user types swar notes in rapid
   succession, they should be grouped onto a single beat with equal subdivisions
   (e.g., typing "sRgm" within ~500ms produces a 4-swar group on one beat). This
   module encodes the rules that decide when to start a new group, extend an in-
   progress group, or cancel grouping — without touching any UI, history, or I/O
   concerns.

   The Scala-side decision tree (`GroupingFSM.decide`) and this Elm port must
   agree byte-for-byte on:

     1. Sliding 500ms window — a keystroke that arrives more than `thresholdMs`
        after the MOST RECENT keystroke in the in-progress group cannot extend
        it. The window is refreshed on every keystroke (i.e., `extendedState`
        advances `lastTypedTimeMs` to the keystroke's `nowMs`).

     2. 4-note cap — a group can hold at most `maxGroupSize` notes. The 5th
        keystroke starts a fresh group.

     3. Cursor-alignment guard (bug 4) — if the observed cursor at the next
        keystroke differs from where the previous insert advanced the cursor to,
        the user must have navigated between keystrokes. We start a fresh group
        rather than incorrectly collapse the new note onto the old group's beat.
-}

import Model.Cursor exposing (CursorModel)
import Model.Types exposing (Note, Octave, Variant)
import State.Model exposing (GroupingState)


{-| Maximum number of notes allowed in a single fast-typed group.
Mirrors `GroupingFSM.MaxGroupSize` in Scala.
-}
maxGroupSize : Int
maxGroupSize =
    4


{-| Maximum time (in milliseconds) between two consecutive keystrokes for the
latter to extend an in-progress group. The window is sliding — refreshed on
every keystroke (see module doc).
Mirrors `GroupingFSM.ThresholdMs` in Scala.
-}
thresholdMs : Int
thresholdMs =
    500


{-| A subset of `CursorModel` carrying only the fields used by the alignment
check. Mirrors `GroupingFSM.CursorTriple` in Scala.
-}
type alias CursorTriple =
    { beat : Int
    , cycle : Int
    , subIndex : Int
    }


cursorTripleFromCursor : CursorModel -> CursorTriple
cursorTripleFromCursor c =
    { beat = c.beat, cycle = c.cycle, subIndex = c.subIndex }


{-| A note that participates in a group. Mirrors `GroupingFSM.GroupedNote`.
-}
type alias GroupedNote =
    { note : Note, variant : Variant, octave : Octave }


{-| The action a host should perform in response to a new keystroke.
Mirrors `GroupingFSM.Decision` in Scala.

  - `StartNew`: insert the single note normally and seed a new state via
    `startedState`.
  - `Extend allNotes`: undo the previous insert, replay with `allNotes`, and
    call `extendedState` with the new cursor.

-}
type Decision
    = StartNew
    | Extend (List GroupedNote)


{-| Decide what to do with `thisNote` typed at `nowMs` while the cursor is at
`observed`.

  - `Nothing` → always `StartNew` (no in-progress group).
  - In-progress group + within-threshold + under cap + cursor aligned →
    `Extend (gs.notes ++ [thisNote])`.
  - Otherwise → `StartNew`.

Pure: does NOT mutate state, perform the insert, or touch history. The host
is responsible for executing the decision and producing the next state via
`startedState` / `extendedState`.

Mirrors `GroupingFSM.decide` in Scala.

-}
decide :
    Maybe GroupingState
    -> Int
    -> CursorTriple
    -> GroupedNote
    -> Decision
decide currentState nowMs observed thisNote =
    case currentState of
        Nothing ->
            StartNew

        Just gs ->
            let
                withinWindow =
                    -- `startTime` here carries sliding-window semantics: it is
                    -- updated on every `extendedState`, so it acts as the
                    -- canonical `lastTypedTimeMs`.
                    (nowMs - gs.startTime) < thresholdMs

                underCap =
                    List.length gs.notes < maxGroupSize

                aligned =
                    cursorMatches observed
                        { beat = gs.nextBeat
                        , cycle = gs.nextCycle
                        , subIndex = gs.nextSubIndex
                        }
            in
            if withinWindow && underCap && aligned then
                Extend (gs.notes ++ [ thisNote ])

            else
                StartNew


{-| Cursor-alignment predicate (bug 4 guard). Beat / cycle / subIndex must all
match; the rest of `CursorModel` (selection anchors, octave overrides) is
metadata that shouldn't cancel a group.
Mirrors `GroupingFSM.cursorMatches` in Scala.
-}
cursorMatches : CursorTriple -> CursorTriple -> Bool
cursorMatches observed expected =
    observed.beat == expected.beat && observed.cycle == expected.cycle && observed.subIndex == expected.subIndex


{-| Construct the next `GroupingState` after the host has executed an
`Extend allNotes` decision. The sliding window advances: `startTime` is
updated to `nowMs` so the next keystroke is compared against this one.
Mirrors `GroupingFSM.extendedState` in Scala.
-}
extendedState :
    GroupingState
    -> List GroupedNote
    -> Int
    -> CursorTriple
    -> GroupingState
extendedState previous allNotes nowMs newNextCursor =
    { previous
        | notes = allNotes
        , startTime = nowMs
        , nextBeat = newNextCursor.beat
        , nextCycle = newNextCursor.cycle
        , nextSubIndex = newNextCursor.subIndex
    }


{-| Construct the initial `GroupingState` for a new group, given the cursor the
editor advanced to after the first single insert.
Mirrors `GroupingFSM.startedState` in Scala.
-}
startedState :
    CursorTriple
    -> GroupedNote
    -> Int
    -> CursorTriple
    -> GroupingState
startedState preInsertCursor thisNote nowMs postInsertCursor =
    { notes = [ thisNote ]
    , startTime = nowMs
    , beat = preInsertCursor.beat
    , cycle = preInsertCursor.cycle
    , nextBeat = postInsertCursor.beat
    , nextCycle = postInsertCursor.cycle
    , nextSubIndex = postInsertCursor.subIndex
    }
