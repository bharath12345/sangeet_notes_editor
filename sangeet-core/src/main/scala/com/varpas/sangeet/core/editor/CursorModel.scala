package com.varpas.sangeet.core.editor

import com.varpas.sangeet.core.model._

case class CursorModel(
    taal: Taal,
    cycle: Int = 0,
    beat: Int = 0,
    subIndex: Int = 0,
    totalSubdivisions: Int = 1,
    currentOctave: Octave = Octave.Madhya,
    selectionAnchor: Option[BeatPosition] = None
):

  def position: BeatPosition =
    BeatPosition(cycle, beat, Rational(subIndex, totalSubdivisions))

  def nextBeat: CursorModel = nextBeat(1)

  def nextBeat(startingBeat: Int): CursorModel =
    val minBeat = startingBeat - 1
    val newBeat = beat + 1
    if newBeat >= taal.matras then
      copy(beat = minBeat, cycle = cycle + 1, subIndex = 0, totalSubdivisions = 1, selectionAnchor = None)
    else copy(beat = newBeat, subIndex = 0, totalSubdivisions = 1, selectionAnchor = None)

  def prevBeat: CursorModel = prevBeat(1)

  def prevBeat(startingBeat: Int): CursorModel =
    val minBeat = startingBeat - 1
    val newBeat = beat - 1
    if newBeat < minBeat then
      if cycle > 0 then
        copy(beat = taal.matras - 1, cycle = cycle - 1, subIndex = 0, totalSubdivisions = 1, selectionAnchor = None)
      else this
    else copy(beat = newBeat, subIndex = 0, totalSubdivisions = 1, selectionAnchor = None)

  def nextSubBeat: CursorModel = nextSubBeat(1)

  def nextSubBeat(startingBeat: Int): CursorModel =
    val newSub = subIndex + 1
    if newSub >= totalSubdivisions then nextBeat(startingBeat)
    else copy(subIndex = newSub)

  def withSubdivisions(n: Int): CursorModel =
    copy(totalSubdivisions = n, subIndex = 0)

  def withOctave(oct: Octave): CursorModel =
    copy(currentOctave = oct)

  def moveTo(cycle: Int, beat: Int): CursorModel = moveTo(cycle, beat, 1)

  def moveTo(cycle: Int, beat: Int, startingBeat: Int): CursorModel =
    val minBeat = startingBeat - 1
    copy(cycle = cycle, beat = math.max(beat, minBeat), subIndex = 0, selectionAnchor = None)

  // --- Selection ---

  def startSelection: CursorModel =
    if selectionAnchor.isDefined then this
    else copy(selectionAnchor = Some(position))

  def clearSelection: CursorModel =
    copy(selectionAnchor = None)

  def hasSelection: Boolean =
    selectionAnchor.isDefined

  def selectionRange: Option[(BeatPosition, BeatPosition)] =
    selectionAnchor.map { anchor =>
      val cur = position
      if anchor <= cur then (anchor, cur) else (cur, anchor)
    }

  def selectNextBeat: CursorModel = selectNextBeat(1)

  def selectNextBeat(startingBeat: Int): CursorModel =
    val minBeat  = startingBeat - 1
    val anchored = startSelection
    val newBeat  = anchored.beat + 1
    if newBeat >= taal.matras then
      anchored.copy(beat = minBeat, cycle = anchored.cycle + 1, subIndex = 0, totalSubdivisions = 1)
    else anchored.copy(beat = newBeat, subIndex = 0, totalSubdivisions = 1)

  def selectPrevBeat: CursorModel = selectPrevBeat(1)

  def selectPrevBeat(startingBeat: Int): CursorModel =
    val minBeat  = startingBeat - 1
    val anchored = startSelection
    val newBeat  = anchored.beat - 1
    if newBeat < minBeat then
      if anchored.cycle > 0 then
        anchored.copy(beat = taal.matras - 1, cycle = anchored.cycle - 1, subIndex = 0, totalSubdivisions = 1)
      else anchored
    else anchored.copy(beat = newBeat, subIndex = 0, totalSubdivisions = 1)

  def selectToStart: CursorModel = selectToStart(1)

  def selectToStart(startingBeat: Int): CursorModel =
    val minBeat  = startingBeat - 1
    val anchored = startSelection
    anchored.copy(cycle = 0, beat = minBeat, subIndex = 0, totalSubdivisions = 1)

  def selectToEnd(maxCycle: Int): CursorModel =
    val anchored = startSelection
    anchored.copy(cycle = maxCycle, beat = taal.matras - 1, subIndex = 0, totalSubdivisions = 1)

  def selectAll(maxCycle: Int): CursorModel = selectAll(maxCycle, 1)

  def selectAll(maxCycle: Int, startingBeat: Int): CursorModel =
    val minBeat = startingBeat - 1
    copy(
      selectionAnchor = Some(BeatPosition(0, minBeat, Rational.onBeat)),
      cycle = maxCycle,
      beat = taal.matras - 1,
      subIndex = 0,
      totalSubdivisions = 1
    )
