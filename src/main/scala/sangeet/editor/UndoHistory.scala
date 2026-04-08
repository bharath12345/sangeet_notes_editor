package sangeet.editor

/** Immutable undo/redo history for CompositionEditor state. */
case class UndoHistory(
  past: List[CompositionEditor],
  present: CompositionEditor,
  future: List[CompositionEditor],
  maxSize: Int
):

  /** Push current state to undo stack, set new state as present. Clears redo stack. */
  def push(newState: CompositionEditor): UndoHistory =
    val trimmedPast = (present :: past).take(maxSize)
    copy(past = trimmedPast, present = newState, future = Nil)

  /** Undo: restore previous state, push current to redo stack. */
  def undo: Option[UndoHistory] =
    past match
      case prev :: rest =>
        Some(copy(past = rest, present = prev, future = present :: future))
      case Nil => None

  /** Redo: restore next state from redo stack, push current to undo stack. */
  def redo: Option[UndoHistory] =
    future match
      case next :: rest =>
        Some(copy(past = present :: past, present = next, future = rest))
      case Nil => None

  def canUndo: Boolean = past.nonEmpty
  def canRedo: Boolean = future.nonEmpty

object UndoHistory:
  def apply(initial: CompositionEditor, maxSize: Int = 50): UndoHistory =
    new UndoHistory(Nil, initial, Nil, maxSize)
