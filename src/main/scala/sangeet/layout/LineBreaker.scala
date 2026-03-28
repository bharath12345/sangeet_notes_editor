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
    val (breaks, markers) = computeVibhagInfo(taal)
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

  private def computeVibhagInfo(taal: Taal): (List[Int], List[(Int, VibhagMarker)]) =
    var offset = 0
    val breaks = scala.collection.mutable.ListBuffer[Int]()
    val markers = scala.collection.mutable.ListBuffer[(Int, VibhagMarker)]()
    taal.vibhags.foreach { v =>
      markers += ((offset, v.marker))
      if offset > 0 then breaks += offset
      offset += v.beats
    }
    (breaks.toList, markers.toList)
