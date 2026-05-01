package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.editor.CursorModel

object CursorApi:

  /** Move cursor to next beat. */
  def nextBeat(cursor: CursorModel): CursorModel =
    cursor.nextBeat

  /** Move cursor to previous beat. */
  def prevBeat(cursor: CursorModel): CursorModel =
    cursor.prevBeat

  /** Move cursor to next sub-beat within the current beat. */
  def nextSubBeat(cursor: CursorModel): CursorModel =
    cursor.nextSubBeat

  /** Set the number of subdivisions for the current beat. */
  def setSubdivisions(cursor: CursorModel, n: Int): CursorModel =
    cursor.withSubdivisions(n)

  /** Set the current octave for next note input. */
  def setOctave(cursor: CursorModel, octave: Octave): CursorModel =
    cursor.withOctave(octave)

  /** Move cursor to specific cycle and beat. */
  def moveTo(cursor: CursorModel, cycle: Int, beat: Int): CursorModel =
    cursor.moveTo(cycle, beat)
