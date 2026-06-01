package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.editor.{CompositionEditor, KeyHandler}

object EditorApi:

  // Sa and Pa are achal (fixed) — komal/tivra variants are invalid
  private def validateNoteVariant(note: Note, variant: Variant): Either[ApiError, Unit] =
    (note, variant) match
      case (Note.Sa, Variant.Komal | Variant.Tivra) =>
        Left(ApiError.InvalidNoteVariant(note.toString, variant.toString))
      case (Note.Pa, Variant.Komal | Variant.Tivra) =>
        Left(ApiError.InvalidNoteVariant(note.toString, variant.toString))
      // Ma can only be tivra, not komal
      case (Note.Ma, Variant.Komal) =>
        Left(ApiError.InvalidNoteVariant(note.toString, variant.toString))
      // Re, Ga, Dha, Ni can only be komal, not tivra
      case (Note.Re | Note.Ga | Note.Dha | Note.Ni, Variant.Tivra) =>
        Left(ApiError.InvalidNoteVariant(note.toString, variant.toString))
      case _ => Right(())

  private def validateSectionIndex(input: EditorInput): Either[ApiError, Unit] =
    val size = input.composition.sections.size
    if input.sectionIndex < 0 || input.sectionIndex >= size then
      Left(ApiError.InvalidSectionIndex(input.sectionIndex, size - 1))
    else Right(())

  def insertSwar(
    input: EditorInput,
    note: Note,
    variant: Variant,
    octave: Octave
  ): Either[ApiError, EditorResult] =
    for
      _ <- validateSectionIndex(input)
      _ <- validateNoteVariant(note, variant)
    yield
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
      EditorResult(newEditor.composition, newCursor, s"Inserted ${note} ${variant} ${octave}")

  def insertRest(input: EditorInput): Either[ApiError, EditorResult] =
    for _ <- validateSectionIndex(input)
    yield
      val editor = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      val event = Event.Rest(input.cursor.position, Rational.fullBeat)
      val newEditor = editor.addEvent(event)
      val newCursor = input.cursor.nextBeat
      EditorResult(newEditor.composition, newCursor, "Inserted rest")

  def insertSustain(input: EditorInput): Either[ApiError, EditorResult] =
    for _ <- validateSectionIndex(input)
    yield
      val editor = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      val event = Event.Sustain(input.cursor.position, Rational.fullBeat)
      val newEditor = editor.addEvent(event)
      val newCursor = input.cursor.nextBeat
      EditorResult(newEditor.composition, newCursor, "Inserted sustain")

  def deleteLastEvent(input: EditorInput): Either[ApiError, EditorResult] =
    validateSectionIndex(input).flatMap { _ =>
      val editor = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      editor.removeLastEvent match
        case Some(newEditor) =>
          val newCursor = input.cursor.prevBeat
          Right(EditorResult(newEditor.composition, newCursor, "Deleted last event"))
        case None =>
          Left(ApiError.EmptySection)
    }

  def insertDualSwar(
    input: EditorInput,
    note: Note,
    variant: Variant,
    octave: Octave
  ): Either[ApiError, EditorResult] =
    for
      _ <- validateSectionIndex(input)
      _ <- validateNoteVariant(note, variant)
    yield
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
      EditorResult(newEditor.composition, newCursor, s"Inserted dual ${note}")
