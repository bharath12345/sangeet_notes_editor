package sangeet.layout

import sangeet.model.*

/** A single beat cell containing all events at one (cycle, beat) position. */
case class BeatCell(
  position: CycleAndBeat,
  events: List[Event]
)

case class CycleAndBeat(cycle: Int, beat: Int)

/** A line of beat cells ready for rendering. */
case class GridLine(
  cells: List[BeatCell],
  vibhagBreaks: List[Int],   // indices where vibhag boundaries fall
  markers: List[(Int, VibhagMarker)] // (cell index, marker)
)

/** Complete grid layout for a section. */
case class SectionGrid(
  sectionName: String,
  sectionType: SectionType,
  lines: List[GridLine]
)
