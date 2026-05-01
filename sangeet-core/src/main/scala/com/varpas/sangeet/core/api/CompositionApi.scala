package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.editor.CompositionEditor
import com.varpas.sangeet.core.format.SwarFormat
import io.circe.{Json, Error}

object CompositionApi:

  /** Create a new composition with the given parameters. */
  def createComposition(
    title: String,
    compositionType: CompositionType,
    taal: Taal,
    raag: Raag,
    laya: Option[Laya],
    taanCount: Int = 0,
    showStrokeLine: Boolean = false,
    showSahityaLine: Boolean = false
  ): Composition =
    CompositionEditor.create(
      title = title,
      compositionType = compositionType,
      taal = taal,
      raag = raag,
      laya = laya,
      taanCount = taanCount,
      showStrokeLine = showStrokeLine,
      showSahityaLine = showSahityaLine
    ).composition

  /** Parse a composition from JSON string. */
  def parseComposition(jsonString: String): Either[ApiError, Composition] =
    SwarFormat.fromJson(jsonString)
      .left.map(e => ApiError.ParseError(e.getMessage))

  /** Serialize a composition to JSON. */
  def serializeComposition(composition: Composition): Json =
    SwarFormat.toJson(composition)

  /** Serialize a composition to JSON string with formatting. */
  def serializeCompositionString(composition: Composition): String =
    SwarFormat.toJson(composition).spaces2
