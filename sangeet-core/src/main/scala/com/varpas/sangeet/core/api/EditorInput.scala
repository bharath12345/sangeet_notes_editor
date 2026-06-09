package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.editor._
import com.varpas.sangeet.core.model._

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

case class ClipboardResult(
    clipboardJson: String,
    composition: Composition,
    cursor: CursorModel,
    message: String
)
