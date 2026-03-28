package sangeet.layout

import sangeet.model.*

object BeatGrouper:

  private def eventBeat(e: Event): BeatPosition = e match
    case s: Event.Swar    => s.beat
    case r: Event.Rest    => r.beat
    case s: Event.Sustain => s.beat

  def group(events: List[Event]): List[BeatCell] =
    events
      .groupBy { e =>
        val bp = eventBeat(e)
        CycleAndBeat(bp.cycle, bp.beat)
      }
      .toList
      .sortBy { (cab, _) => (cab.cycle, cab.beat) }
      .map { (cab, evts) =>
        BeatCell(cab, evts.sortBy(e => eventBeat(e).subdivision))
      }
