package com.varpas.sangeet.server.routes

import io.circe.Json
import io.circe.syntax._

import com.varpas.sangeet.core.api.{ClipboardResult, EditorResult}
import com.varpas.sangeet.core.editor.CursorModel
import com.varpas.sangeet.core.format.Codecs.given

/** Circe encoders for server-specific types that don't have codecs in core. */
object EditorResultCodec:

  def encodeCursor(c: CursorModel): Json =
    val base = List(
      "taal"              -> c.taal.asJson,
      "cycle"             -> Json.fromInt(c.cycle),
      "beat"              -> Json.fromInt(c.beat),
      "subIndex"          -> Json.fromInt(c.subIndex),
      "totalSubdivisions" -> Json.fromInt(c.totalSubdivisions),
      "currentOctave"     -> c.currentOctave.asJson
    )
    val anchor = c.selectionAnchor.map(a => "selectionAnchor" -> a.asJson).toList
    Json.obj((base ++ anchor)*)

  def encodeEditorResult(r: EditorResult): Json =
    Json.obj(
      "composition" -> r.composition.asJson,
      "cursor"      -> encodeCursor(r.cursor),
      "message"     -> Json.fromString(r.message)
    )

  def encodeClipboardResult(r: ClipboardResult): Json =
    Json.obj(
      "clipboardJson" -> Json.fromString(r.clipboardJson),
      "composition"   -> r.composition.asJson,
      "cursor"        -> encodeCursor(r.cursor),
      "message"       -> Json.fromString(r.message)
    )
