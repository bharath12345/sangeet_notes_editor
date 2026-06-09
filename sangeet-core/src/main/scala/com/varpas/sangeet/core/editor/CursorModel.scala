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

  def nextBeat: CursorModel =
    val newBeat = beat + 1
    if newBeat >= taal.matras then
      copy(beat = 0, cycle = cycle + 1, subIndex = 0, totalSubdivisions = 1, selectionAnchor = None)
    else copy(beat = newBeat, subIndex = 0, totalSubdivisions = 1, selectionAnchor = None)

  def prevBeat: CursorModel =
    val newBeat = beat - 1
    if newBeat < 0 then
      if cycle > 0 then
        copy(beat = taal.matras - 1, cycle = cycle - 1, subIndex = 0, totalSubdivisions = 1, selectionAnchor = None)
      else this
    else copy(beat = newBeat, subIndex = 0, totalSubdivisions = 1, selectionAnchor = None)

  def nextSubBeat: CursorModel =
    val newSub = subIndex + 1
    if newSub >= totalSubdivisions then nextBeat
    else copy(subIndex = newSub)

  def withSubdivisions(n: Int): CursorModel =
    copy(totalSubdivisions = n, subIndex = 0)

  def withOctave(oct: Octave): CursorModel =
    copy(currentOctave = oct)

  def moveTo(cycle: Int, beat: Int): CursorModel =
    copy(cycle = cycle, beat = beat, subIndex = 0, selectionAnchor = None)

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

  def selectNextBeat: CursorModel =
    val anchored = startSelection
    val newBeat  = anchored.beat + 1
    if newBeat >= taal.matras then
      anchored.copy(beat = 0, cycle = anchored.cycle + 1, subIndex = 0, totalSubdivisions = 1)
    else anchored.copy(beat = newBeat, subIndex = 0, totalSubdivisions = 1)

  def selectPrevBeat: CursorModel =
    val anchored = startSelection
    val newBeat  = anchored.beat - 1
    if newBeat < 0 then
      if anchored.cycle > 0 then
        anchored.copy(beat = taal.matras - 1, cycle = anchored.cycle - 1, subIndex = 0, totalSubdivisions = 1)
      else anchored
    else anchored.copy(beat = newBeat, subIndex = 0, totalSubdivisions = 1)

  def selectToStart: CursorModel =
    val anchored = startSelection
    anchored.copy(cycle = 0, beat = 0, subIndex = 0, totalSubdivisions = 1)

  def selectToEnd(maxCycle: Int): CursorModel =
    val anchored = startSelection
    anchored.copy(cycle = maxCycle, beat = taal.matras - 1, subIndex = 0, totalSubdivisions = 1)

  def selectAll(maxCycle: Int): CursorModel =
    copy(
      selectionAnchor = Some(BeatPosition(0, 0, Rational.onBeat)),
      cycle = maxCycle,
      beat = taal.matras - 1,
      subIndex = 0,
      totalSubdivisions = 1
    )
