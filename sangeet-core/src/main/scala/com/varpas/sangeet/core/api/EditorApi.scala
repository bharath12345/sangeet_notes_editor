package com.varpas.sangeet.core.api

import io.circe.parser._
import io.circe.syntax._

import com.varpas.sangeet.core.editor.ClipboardCodecs.given
import com.varpas.sangeet.core.editor.{ClipboardCodecs, ClipboardData, CompositionEditor, CursorModel, KeyHandler}
import com.varpas.sangeet.core.model._

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
      val editor    = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      val event     = Event.Rest(input.cursor.position, Rational.fullBeat)
      val newEditor = editor.addEvent(event)
      val newCursor = input.cursor.nextBeat
      EditorResult(newEditor.composition, newCursor, "Inserted rest")

  def insertChikari(input: EditorInput): Either[ApiError, EditorResult] =
    for _ <- validateSectionIndex(input)
    yield
      val editor    = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      val event     = Event.Chikari(input.cursor.position, Rational.fullBeat)
      val newEditor = editor.addEvent(event)
      val newCursor = input.cursor.nextBeat
      EditorResult(newEditor.composition, newCursor, "Inserted chikari")

  def insertSustain(input: EditorInput): Either[ApiError, EditorResult] =
    for _ <- validateSectionIndex(input)
    yield
      val editor    = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      val event     = Event.Sustain(input.cursor.position, Rational.fullBeat)
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
      val editor       = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      val halfDuration = Rational(1, 2)
      val event1 = Event.Swar(
        note,
        variant,
        octave,
        BeatPosition(input.cursor.cycle, input.cursor.beat, Rational(0, 2)),
        halfDuration,
        None,
        Nil,
        None
      )
      val event2 = Event.Swar(
        note,
        variant,
        octave,
        BeatPosition(input.cursor.cycle, input.cursor.beat, Rational(1, 2)),
        halfDuration,
        None,
        Nil,
        None
      )
      val newEditor = editor.addEvent(event1).addEvent(event2)
      val newCursor = input.cursor.nextBeat.withOctave(Octave.Madhya)
      EditorResult(newEditor.composition, newCursor, s"Inserted dual ${note}")

  def insertSwarGroup(
      input: EditorInput,
      notes: List[(Note, Variant, Octave)]
  ): Either[ApiError, EditorResult] =
    for
      _ <- validateSectionIndex(input)
      _ <- notes.foldLeft(Right(()): Either[ApiError, Unit]) { case (acc, (note, variant, _)) =>
        acc.flatMap(_ => validateNoteVariant(note, variant))
      }
    yield
      val editor           = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      val (newEditor, msg) = KeyHandler.handleSwarGroup(editor, notes)
      EditorResult(newEditor.composition, newEditor.cursor, msg)

  def deleteAtCursor(input: EditorInput): Either[ApiError, EditorResult] =
    validateSectionIndex(input).map { _ =>
      val editor = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      editor.removeGroupAt(input.cursor) match
        case Some(newEditor) =>
          val newCursor = input.cursor.prevBeat
          EditorResult(newEditor.composition, newCursor, "Deleted at cursor")
        case None =>
          val prev = input.cursor.prevBeat
          if prev != input.cursor then
            editor.removeGroupAt(prev) match
              case Some(newEditor) =>
                EditorResult(newEditor.composition, prev, "Deleted before cursor")
              case None =>
                EditorResult(input.composition, prev, "Moved back (empty beat)")
          else EditorResult(input.composition, input.cursor, "Nothing to delete")
    }

  def copySelection(input: EditorInput): Either[ApiError, ClipboardResult] =
    for
      _     <- validateSectionIndex(input)
      range <- input.cursor.selectionRange.toRight(ApiError.EmptySelection)
    yield
      val (start, end) = range
      val editor       = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      val events       = editor.eventsInRange(start, end)
      if events.isEmpty then
        ClipboardResult(ClipboardData(Nil).asJson.noSpaces, input.composition, input.cursor, "No events in selection")
      else
        val clipJson = ClipboardData(events).asJson.noSpaces
        ClipboardResult(clipJson, input.composition, input.cursor, s"Copied ${events.size} event(s)")

  def cutSelection(input: EditorInput): Either[ApiError, ClipboardResult] =
    for
      _     <- validateSectionIndex(input)
      range <- input.cursor.selectionRange.toRight(ApiError.EmptySelection)
    yield
      val (start, end)        = range
      val editor              = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      val (newEditor, events) = editor.cutRange(start, end)
      if events.isEmpty then
        ClipboardResult(ClipboardData(Nil).asJson.noSpaces, input.composition, input.cursor, "No events in selection")
      else
        val clipJson  = ClipboardData(events).asJson.noSpaces
        val newCursor = input.cursor.copy(cycle = start.cycle, beat = start.beat, subIndex = 0, selectionAnchor = None)
        ClipboardResult(clipJson, newEditor.composition, newCursor, s"Cut ${events.size} event(s)")

  def pasteClipboard(input: EditorInput, clipboardJson: String): Either[ApiError, EditorResult] =
    for
      _  <- validateSectionIndex(input)
      cd <- parse(clipboardJson).flatMap(_.as[ClipboardData]).left.map(e => ApiError.InvalidClipboard(e.getMessage))
    yield
      if cd.events.isEmpty then EditorResult(input.composition, input.cursor, "Nothing to paste")
      else
        val editor    = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
        val newEditor = editor.pasteEvents(cd.events, input.cursor.position)
        val newCursor = input.cursor.clearSelection
        EditorResult(newEditor.composition, newCursor, s"Pasted ${cd.events.size} event(s)")

  def changeStartingBeat(
      composition: Composition,
      sectionIndex: Int,
      newStartingBeat: Int
  ): Either[ApiError, Composition] =
    val sections = composition.sections
    if sectionIndex < 0 || sectionIndex >= sections.size then
      Left(ApiError.InvalidSectionIndex(sectionIndex, sections.size - 1))
    else if newStartingBeat < 1 || newStartingBeat > composition.metadata.taal.matras then
      Left(ApiError.ValidationError(s"Starting beat must be between 1 and ${composition.metadata.taal.matras}"))
    else
      val matras         = composition.metadata.taal.matras
      val section        = sections(sectionIndex)
      val updatedSection = CompositionEditor.changeStartingBeat(section, newStartingBeat, matras)
      Right(composition.copy(sections = sections.updated(sectionIndex, updatedSection)))

  /** Change the composition's taal, re-mapping all event positions across sections so events overflowing the new taal's
    * matra count flow into subsequent cycles. Mirrors desktop's CompositionEditor.changeTaal behavior so a Teen Taal →
    * Ek Taal switch reflows the grid (16-beat rows become 12-beat rows) instead of leaving events stranded past the
    * vibhag boundaries. The new cursor is reset to (cycle 0, beat = startingBeat-1 of the current section).
    */
  def changeTaal(
      composition: Composition,
      sectionIndex: Int,
      newTaal: Taal
  ): Either[ApiError, EditorResult] =
    val sections = composition.sections
    if sectionIndex < 0 || sectionIndex >= sections.size then
      Left(ApiError.InvalidSectionIndex(sectionIndex, sections.size - 1))
    else
      val oldMatras = composition.metadata.taal.matras
      val newMatras = newTaal.matras
      val newSections = sections.map { section =>
        val newEvents = section.events.map { event =>
          val pos          = event.position
          val absoluteBeat = pos.cycle * oldMatras + pos.beat
          val newCycle     = absoluteBeat / newMatras
          val newBeat      = absoluteBeat % newMatras
          val newPos       = BeatPosition(newCycle, newBeat, pos.subdivision)
          event.withPosition(newPos)
        }
        section.copy(events = newEvents)
      }
      val newMeta = composition.metadata.copy(
        taal = newTaal,
        updatedAt = java.time.Instant.now().toString
      )
      val newComp     = composition.copy(metadata = newMeta, sections = newSections)
      val targetBeat  = newSections(sectionIndex).startingBeat - 1
      val clampedBeat = math.max(0, math.min(targetBeat, newMatras - 1))
      val newCursor   = CursorModel(taal = newTaal, cycle = 0, beat = clampedBeat)
      Right(EditorResult(newComp, newCursor, s"Taal changed to ${newTaal.name}"))
