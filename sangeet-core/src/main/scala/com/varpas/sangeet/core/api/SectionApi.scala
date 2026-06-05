package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.editor.{CompositionEditor, CursorModel}
import com.varpas.sangeet.core.model._

object SectionApi:

  /** Add a new section to the composition. */
  def addSection(
      composition: Composition,
      name: String,
      sectionType: SectionType
  ): Either[ApiError, Composition] =
    val newSection  = Section(name, sectionType, Nil)
    val newSections = composition.sections :+ newSection
    Right(composition.copy(sections = newSections))

  /** Remove a section by index. Returns error if it's the last section. */
  def removeSection(
      composition: Composition,
      currentSectionIndex: Int,
      indexToRemove: Int
  ): Either[ApiError, (Composition, Int)] =
    if composition.sections.size <= 1 then Left(ApiError.LastSection)
    else if indexToRemove < 0 || indexToRemove >= composition.sections.size then
      Left(ApiError.InvalidSectionIndex(indexToRemove, composition.sections.size - 1))
    else
      val editor = CompositionEditor(composition, currentSectionIndex, CursorModel(composition.metadata.taal))
      editor.removeSection(indexToRemove) match
        case Some(newEditor) =>
          Right((newEditor.composition, newEditor.currentSectionIndex))
        case None =>
          Left(ApiError.LastSection)

  /** Rename a section by index. */
  def renameSection(
      composition: Composition,
      index: Int,
      newName: String
  ): Either[ApiError, Composition] =
    if index < 0 || index >= composition.sections.size then
      Left(ApiError.InvalidSectionIndex(index, composition.sections.size - 1))
    else
      val section     = composition.sections(index)
      val newSections = composition.sections.updated(index, section.copy(name = newName))
      Right(composition.copy(sections = newSections))

  /** Move a section from one index to another. */
  def moveSection(
      composition: Composition,
      currentSectionIndex: Int,
      from: Int,
      to: Int
  ): Either[ApiError, (Composition, Int)] =
    if from < 0 || to < 0 || from >= composition.sections.size || to >= composition.sections.size then
      Left(ApiError.InvalidSectionIndex(from, composition.sections.size - 1))
    else if from == to then Right((composition, currentSectionIndex))
    else
      val editor    = CompositionEditor(composition, currentSectionIndex, CursorModel(composition.metadata.taal))
      val newEditor = editor.moveSection(from, to)
      Right((newEditor.composition, newEditor.currentSectionIndex))
