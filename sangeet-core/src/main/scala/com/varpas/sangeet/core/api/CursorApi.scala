package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.editor.CursorModel
import com.varpas.sangeet.core.model._

object CursorApi:

  def nextBeat(cursor: CursorModel, startingBeat: Int = 1): CursorModel =
    cursor.nextBeat(startingBeat)

  def prevBeat(cursor: CursorModel, startingBeat: Int = 1): CursorModel =
    cursor.prevBeat(startingBeat)

  def nextSubBeat(cursor: CursorModel, startingBeat: Int = 1): CursorModel =
    cursor.nextSubBeat(startingBeat)

  def setSubdivisions(cursor: CursorModel, n: Int): Either[ApiError, CursorModel] =
    if n < 1 || n > 8 then Left(ApiError.ValidationError(s"Subdivision must be 1-8, got $n"))
    else Right(cursor.withSubdivisions(n))

  def setOctave(cursor: CursorModel, octave: Octave): CursorModel =
    cursor.withOctave(octave)

  def moveTo(cursor: CursorModel, cycle: Int, beat: Int, startingBeat: Int = 1): Either[ApiError, CursorModel] =
    if cycle < 0 then Left(ApiError.ValidationError(s"Cycle must be non-negative, got $cycle"))
    else if beat < 0 then Left(ApiError.ValidationError(s"Beat must be non-negative, got $beat"))
    else Right(cursor.moveTo(cycle, beat, startingBeat))
