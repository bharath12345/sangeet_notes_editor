package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.editor.CompositionEditor
import com.varpas.sangeet.core.model._

object OrnamentApi:

  /** Add a simple ornament (Gamak, Andolan, Gitkari) to the last swar. */
  def addSimpleOrnament(
      input: EditorInput,
      ornament: Ornament
  ): Either[ApiError, EditorResult] =
    val editor = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
    editor.modifyLastSwar(s => s.copy(ornaments = s.ornaments :+ ornament)) match
      case Some(newEditor) =>
        Right(EditorResult(newEditor.composition, input.cursor, "Ornament added"))
      case None =>
        Left(ApiError.NoSwarTarget)

  /** Add a single-note ornament (KanSwar, Sparsh, Ghaseet) to the last swar. */
  def addSingleNoteOrnament(
      input: EditorInput,
      ornament: Ornament & Product // KanSwar, Sparsh, or Ghaseet
  ): Either[ApiError, EditorResult] =
    val editor = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
    editor.modifyLastSwar(s => s.copy(ornaments = s.ornaments :+ ornament)) match
      case Some(newEditor) =>
        Right(EditorResult(newEditor.composition, input.cursor, "Ornament added"))
      case None =>
        Left(ApiError.NoSwarTarget)

  /** Add a Meend ornament to the last swar. */
  def addMeend(
      input: EditorInput,
      startNote: NoteRef,
      endNote: NoteRef,
      direction: MeendDirection,
      intermediateNotes: List[NoteRef] = Nil
  ): Either[ApiError, EditorResult] =
    val editor   = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
    val ornament = Meend(startNote, endNote, direction, intermediateNotes)
    editor.modifyLastSwar(s => s.copy(ornaments = s.ornaments :+ ornament)) match
      case Some(newEditor) =>
        Right(EditorResult(newEditor.composition, input.cursor, "Meend added"))
      case None =>
        Left(ApiError.NoSwarTarget)

  /** Add a Krintan ornament to the last swar. */
  def addKrintan(
      input: EditorInput,
      notes: List[NoteRef]
  ): Either[ApiError, EditorResult] =
    if notes.size < 2 then Left(ApiError.InsufficientNotes(2, notes.size))
    else
      val editor   = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      val ornament = Krintan(notes)
      editor.modifyLastSwar(s => s.copy(ornaments = s.ornaments :+ ornament)) match
        case Some(newEditor) =>
          Right(EditorResult(newEditor.composition, input.cursor, "Krintan added"))
        case None =>
          Left(ApiError.NoSwarTarget)

  /** Add a Murki ornament to the last swar. */
  def addMurki(
      input: EditorInput,
      notes: List[NoteRef]
  ): Either[ApiError, EditorResult] =
    if notes.isEmpty then Left(ApiError.EmptyNotes)
    else
      val editor   = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      val ornament = Murki(notes)
      editor.modifyLastSwar(s => s.copy(ornaments = s.ornaments :+ ornament)) match
        case Some(newEditor) =>
          Right(EditorResult(newEditor.composition, input.cursor, "Murki added"))
        case None =>
          Left(ApiError.NoSwarTarget)

  /** Add a Zamzama ornament to the last swar. */
  def addZamzama(
      input: EditorInput,
      notes: List[NoteRef]
  ): Either[ApiError, EditorResult] =
    if notes.isEmpty then Left(ApiError.EmptyNotes)
    else
      val editor   = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
      val ornament = Zamzama(notes)
      editor.modifyLastSwar(s => s.copy(ornaments = s.ornaments :+ ornament)) match
        case Some(newEditor) =>
          Right(EditorResult(newEditor.composition, input.cursor, "Zamzama added"))
        case None =>
          Left(ApiError.NoSwarTarget)

  /** Add a custom ornament to the last swar. */
  def addCustomOrnament(
      input: EditorInput,
      name: String,
      parameters: Map[String, String]
  ): Either[ApiError, EditorResult] =
    val editor   = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
    val ornament = CustomOrnament(name, parameters)
    editor.modifyLastSwar(s => s.copy(ornaments = s.ornaments :+ ornament)) match
      case Some(newEditor) =>
        Right(EditorResult(newEditor.composition, input.cursor, s"Custom ornament '$name' added"))
      case None =>
        Left(ApiError.NoSwarTarget)
