package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.editor.*

case class EditorInput(
  composition: Composition,
  sectionIndex: Int,
  cursor: CursorModel
)

case class EditorResult(
  composition: Composition,
  cursor: CursorModel,
  message: String
)
