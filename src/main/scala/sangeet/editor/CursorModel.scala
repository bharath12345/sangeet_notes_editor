package sangeet.editor

import sangeet.model.*

case class CursorModel(
  taal: Taal,
  cycle: Int = 0,
  beat: Int = 0,
  subIndex: Int = 0,
  totalSubdivisions: Int = 1,
  currentOctave: Octave = Octave.Madhya
):

  def position: BeatPosition =
    BeatPosition(cycle, beat, Rational(subIndex, totalSubdivisions))

  def nextBeat: CursorModel =
    val newBeat = beat + 1
    if newBeat >= taal.matras then
      copy(beat = 0, cycle = cycle + 1, subIndex = 0, totalSubdivisions = 1)
    else
      copy(beat = newBeat, subIndex = 0, totalSubdivisions = 1)

  def prevBeat: CursorModel =
    val newBeat = beat - 1
    if newBeat < 0 then
      copy(beat = taal.matras - 1, cycle = cycle - 1, subIndex = 0, totalSubdivisions = 1)
    else
      copy(beat = newBeat, subIndex = 0, totalSubdivisions = 1)

  def nextSubBeat: CursorModel =
    val newSub = subIndex + 1
    if newSub >= totalSubdivisions then
      nextBeat
    else
      copy(subIndex = newSub)

  def withSubdivisions(n: Int): CursorModel =
    copy(totalSubdivisions = n, subIndex = 0)

  def withOctave(oct: Octave): CursorModel =
    copy(currentOctave = oct)
