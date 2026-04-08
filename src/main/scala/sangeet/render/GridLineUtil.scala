package sangeet.render

import sangeet.model.Event
import sangeet.layout.GridLine

/** Shared helpers for querying GridLine content, used by all renderers. */
object GridLineUtil:

  /** Whether any cell in the line contains a swar with ornaments */
  def hasOrnaments(line: GridLine): Boolean =
    line.cells.exists(_.events.exists {
      case s: Event.Swar => s.ornaments.nonEmpty
      case _ => false
    })

  /** Whether any cell in the line contains a swar with sahitya */
  def hasSahitya(line: GridLine): Boolean =
    line.cells.exists(_.events.exists {
      case s: Event.Swar => s.sahitya.isDefined
      case _ => false
    })
