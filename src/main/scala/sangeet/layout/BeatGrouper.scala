package sangeet.layout

import sangeet.model.*

object BeatGrouper:

  def group(events: List[Event]): List[BeatCell] =
    events
      .groupBy { e =>
        val bp = e.position
        CycleAndBeat(bp.cycle, bp.beat)
      }
      .toList
      .sortBy { (cab, _) => (cab.cycle, cab.beat) }
      .map { (cab, evts) =>
        BeatCell(cab, evts.sortBy(_.position.subdivision))
      }
