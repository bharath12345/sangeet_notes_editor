package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.editor.{CompositionEditor, KeyHandler}

object EditorApi:

  /** Insert a swar note at the current cursor position. */
  def insertSwar(
    input: EditorInput,
    note: Note,
    variant: Variant,
    octave: Octave
  ): Either[ApiError, EditorResult] =
    val editor = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
    val event = Event.Swar(
      note = note,
      variant = variant,
      octave = octave,
      beat = input.cursor.position,
      duration = Rational(1, input.cursor.totalSubdivisions),
      stroke = None,
      ornaments = Nil,
      sahitya = None
    )
    val newEditor = editor.addEvent(event)
    val newCursor = input.cursor.nextSubBeat.withOctave(Octave.Madhya)
    Right(EditorResult(newEditor.composition, newCursor, s"Inserted ${note} ${variant} ${octave}"))

  /** Insert a rest (silence) at the current cursor position. */
  def insertRest(input: EditorInput): Either[ApiError, EditorResult] =
    val editor = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
    val event = Event.Rest(input.cursor.position, Rational.fullBeat)
    val newEditor = editor.addEvent(event)
    val newCursor = input.cursor.nextBeat
    Right(EditorResult(newEditor.composition, newCursor, "Inserted rest"))

  /** Insert a sustain (hold previous note) at the current cursor position. */
  def insertSustain(input: EditorInput): Either[ApiError, EditorResult] =
    val editor = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
    val event = Event.Sustain(input.cursor.position, Rational.fullBeat)
    val newEditor = editor.addEvent(event)
    val newCursor = input.cursor.nextBeat
    Right(EditorResult(newEditor.composition, newCursor, "Inserted sustain"))

  /** Delete the last event in the current section. */
  def deleteLastEvent(input: EditorInput): Either[ApiError, EditorResult] =
    val editor = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
    editor.removeLastEvent match
      case Some(newEditor) =>
        val newCursor = input.cursor.prevBeat
        Right(EditorResult(newEditor.composition, newCursor, "Deleted last event"))
      case None =>
        Left(ApiError.EmptySection)

  /** Insert dual swar (two identical notes at half-duration each). */
  def insertDualSwar(
    input: EditorInput,
    note: Note,
    variant: Variant,
    octave: Octave
  ): Either[ApiError, EditorResult] =
    val editor = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
    val halfDuration = Rational(1, 2)
    val event1 = Event.Swar(note, variant, octave,
      BeatPosition(input.cursor.cycle, input.cursor.beat, Rational(0, 2)),
      halfDuration, None, Nil, None)
    val event2 = Event.Swar(note, variant, octave,
      BeatPosition(input.cursor.cycle, input.cursor.beat, Rational(1, 2)),
      halfDuration, None, Nil, None)
    val newEditor = editor.addEvent(event1).addEvent(event2)
    val newCursor = input.cursor.nextBeat.withOctave(Octave.Madhya)
    Right(EditorResult(newEditor.composition, newCursor, s"Inserted dual ${note}"))
