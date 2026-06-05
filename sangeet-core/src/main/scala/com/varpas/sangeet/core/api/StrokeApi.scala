package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.editor.CompositionEditor
import com.varpas.sangeet.core.model._

object StrokeApi:

  /** Set a stroke on the swar at the cursor position. */
  def setStroke(
      input: EditorInput,
      stroke: Stroke
  ): Either[ApiError, EditorResult] =
    val editor = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
    editor.setStrokeAt(input.cursor, stroke) match
      case Some(newEditor) =>
        Right(EditorResult(newEditor.composition, input.cursor, s"Stroke ${stroke} set"))
      case None =>
        Left(ApiError.NoSwarAtPosition)

  /** Clear the stroke on the swar at the cursor position (revert to auto Da/Ra). */
  def clearStroke(
      input: EditorInput
  ): Either[ApiError, EditorResult] =
    val editor = CompositionEditor(input.composition, input.sectionIndex, input.cursor)
    editor.clearStrokeAt(input.cursor) match
      case Some(newEditor) =>
        Right(EditorResult(newEditor.composition, input.cursor, "Stroke cleared"))
      case None =>
        Left(ApiError.NoSwarAtPosition)
