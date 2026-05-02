module State.UndoHistory exposing
    ( UndoHistory
    , Snapshot
    , init
    , push
    , undo
    , redo
    , present
    , canUndo
    , canRedo
    )

import Model.Composition exposing (Composition)
import Model.Cursor exposing (CursorModel)


{-| A snapshot of the editor state at a point in time.
-}
type alias Snapshot =
    { composition : Composition
    , cursor : CursorModel
    , sectionIndex : Int
    }


{-| Immutable undo/redo history with a maximum depth of 50.
Opaque type: callers cannot access the internal lists directly.
-}
type UndoHistory
    = UndoHistory
        { past : List Snapshot
        , current : Snapshot
        , future : List Snapshot
        }


maxHistory : Int
maxHistory =
    50


{-| Create an undo history with an initial snapshot.
-}
init : Snapshot -> UndoHistory
init snapshot =
    UndoHistory
        { past = []
        , current = snapshot
        , future = []
        }


{-| Push a new snapshot, clearing the redo stack.
The current snapshot moves to past. Past is trimmed to maxHistory.
-}
push : Snapshot -> UndoHistory -> UndoHistory
push snapshot (UndoHistory h) =
    UndoHistory
        { past = List.take maxHistory (h.current :: h.past)
        , current = snapshot
        , future = []
        }


{-| Undo: move current to future, pop from past.
Returns Nothing if there is no past state.
-}
undo : UndoHistory -> Maybe UndoHistory
undo (UndoHistory h) =
    case h.past of
        [] ->
            Nothing

        prev :: rest ->
            Just
                (UndoHistory
                    { past = rest
                    , current = prev
                    , future = h.current :: h.future
                    }
                )


{-| Redo: move current to past, pop from future.
Returns Nothing if there is no future state.
-}
redo : UndoHistory -> Maybe UndoHistory
redo (UndoHistory h) =
    case h.future of
        [] ->
            Nothing

        next :: rest ->
            Just
                (UndoHistory
                    { past = h.current :: h.past
                    , current = next
                    , future = rest
                    }
                )


{-| Get the current snapshot.
-}
present : UndoHistory -> Snapshot
present (UndoHistory h) =
    h.current


{-| Whether undo is available.
-}
canUndo : UndoHistory -> Bool
canUndo (UndoHistory h) =
    not (List.isEmpty h.past)


{-| Whether redo is available.
-}
canRedo : UndoHistory -> Bool
canRedo (UndoHistory h) =
    not (List.isEmpty h.future)
