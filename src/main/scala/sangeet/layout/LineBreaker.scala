package sangeet.layout

import sangeet.model.*

object LineBreaker:

  def break(cells: List[BeatCell], taal: Taal, config: LayoutConfig): List[GridLine] =
    val byCycle = cells.groupBy(_.position.cycle).toList.sortBy(_._1)
    byCycle.flatMap { (cycle, cycleCells) =>
      val sorted = cycleCells.sortBy(_.position.beat)
      val maxDensity = if sorted.isEmpty then 1
                       else sorted.map(_.events.size).max
      if maxDensity >= config.highDensityThreshold then
        splitByVibhag(sorted, taal)
      else
        List(makeGridLine(sorted, taal))
    }

  private def makeGridLine(cells: List[BeatCell], taal: Taal): GridLine =
    val cellBeats = cells.map(_.position.beat).toSet
    val (breaks, markers) = computeVibhagInfoRelative(taal, cellBeats)
    GridLine(cells, breaks, markers)

  private def splitByVibhag(cells: List[BeatCell], taal: Taal): List[GridLine] =
    var beatOffset = 0
    taal.vibhags.zipWithIndex.map { (vibhag, idx) =>
      val vibhagCells = cells.filter { c =>
        c.position.beat >= beatOffset && c.position.beat < beatOffset + vibhag.beats
      }
      val markers = List((0, vibhag.marker))
      val line = GridLine(vibhagCells, Nil, markers)
      beatOffset += vibhag.beats
      line
    }

  /** Compute vibhag breaks and markers as cell indices (not absolute beat numbers). */
  private def computeVibhagInfoRelative(taal: Taal, cellBeats: Set[Int]): (List[Int], List[(Int, VibhagMarker)]) =
    val sortedBeats = cellBeats.toList.sorted
    val beatToCell = sortedBeats.zipWithIndex.toMap

    var beatOffset = 0
    val breaks = scala.collection.mutable.ListBuffer[Int]()
    val markers = scala.collection.mutable.ListBuffer[(Int, VibhagMarker)]()
    taal.vibhags.foreach { v =>
      beatToCell.get(beatOffset).foreach { cellIdx =>
        markers += ((cellIdx, v.marker))
        if cellIdx > 0 then breaks += cellIdx
      }
      beatOffset += v.beats
    }
    (breaks.toList, markers.toList)
