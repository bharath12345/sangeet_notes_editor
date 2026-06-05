package com.varpas.sangeet.server.routes

import io.circe.Json
import io.circe.syntax._

import com.varpas.sangeet.core.api.EditorResult
import com.varpas.sangeet.core.editor.CursorModel
import com.varpas.sangeet.core.format.Codecs.given

/** Circe encoders for server-specific types that don't have codecs in core. */
object EditorResultCodec:

  def encodeCursor(c: CursorModel): Json =
    Json.obj(
      "taal"              -> c.taal.asJson,
      "cycle"             -> Json.fromInt(c.cycle),
      "beat"              -> Json.fromInt(c.beat),
      "subIndex"          -> Json.fromInt(c.subIndex),
      "totalSubdivisions" -> Json.fromInt(c.totalSubdivisions),
      "currentOctave"     -> c.currentOctave.asJson
    )

  def encodeEditorResult(r: EditorResult): Json =
    Json.obj(
      "composition" -> r.composition.asJson,
      "cursor"      -> encodeCursor(r.cursor),
      "message"     -> Json.fromString(r.message)
    )
