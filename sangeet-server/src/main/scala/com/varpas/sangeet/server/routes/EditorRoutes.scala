package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import sttp.tapir.server.ServerEndpoint

import com.varpas.sangeet.core.api.{ApiError, EditorApi}
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.server.endpoints.EditorEndpoints
import com.varpas.sangeet.server.routes.EditorResultCodec._
import com.varpas.sangeet.server.routes.JsonParsing._
import com.varpas.sangeet.server.routes.RouteHelper._

object EditorRoutes:

  val insertSwar: ServerEndpoint[Any, IO] =
    EditorEndpoints.insertSwar.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        input        <- parseEditorInput(c)
        note         <- parseField[Note](c, "note")
        variant      <- parseField[Variant](c, "variant")
        octave       <- parseField[Octave](c, "octave")
        editorResult <- EditorApi.insertSwar(input, note, variant, octave)
      yield editorResult)(encodeEditorResult)
    }

  val insertChikari: ServerEndpoint[Any, IO] =
    EditorEndpoints.insertChikari.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        input        <- parseEditorInput(c)
        editorResult <- EditorApi.insertChikari(input)
      yield editorResult)(encodeEditorResult)
    }

  val insertRest: ServerEndpoint[Any, IO] =
    EditorEndpoints.insertRest.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        input        <- parseEditorInput(c)
        editorResult <- EditorApi.insertRest(input)
      yield editorResult)(encodeEditorResult)
    }

  val insertSustain: ServerEndpoint[Any, IO] =
    EditorEndpoints.insertSustain.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        input        <- parseEditorInput(c)
        editorResult <- EditorApi.insertSustain(input)
      yield editorResult)(encodeEditorResult)
    }

  val deleteLast: ServerEndpoint[Any, IO] =
    EditorEndpoints.deleteLast.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        input        <- parseEditorInput(c)
        editorResult <- EditorApi.deleteLastEvent(input)
      yield editorResult)(encodeEditorResult)
    }

  val insertDualSwar: ServerEndpoint[Any, IO] =
    EditorEndpoints.insertDualSwar.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        input        <- parseEditorInput(c)
        note         <- parseField[Note](c, "note")
        variant      <- parseField[Variant](c, "variant")
        octave       <- parseField[Octave](c, "octave")
        editorResult <- EditorApi.insertDualSwar(input, note, variant, octave)
      yield editorResult)(encodeEditorResult)
    }

  val insertSwarGroup: ServerEndpoint[Any, IO] =
    EditorEndpoints.insertSwarGroup.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        input     <- parseEditorInput(c)
        notesJson <- c.downField("notes").as[List[Json]].left.map(e => ApiError.MissingField(s"notes: ${e.message}"))
        notes <- notesJson.foldLeft(
          Right(List.empty[(Note, Variant, Octave)]): Either[ApiError, List[(Note, Variant, Octave)]]
        ) { (acc, nj) =>
          acc.flatMap { list =>
            val nc = nj.hcursor
            for
              note    <- parseField[Note](nc, "note")
              variant <- parseField[Variant](nc, "variant")
              octave  <- parseField[Octave](nc, "octave")
            yield list :+ (note, variant, octave)
          }
        }
        editorResult <- EditorApi.insertSwarGroup(input, notes)
      yield editorResult)(encodeEditorResult)
    }

  val deleteAtCursor: ServerEndpoint[Any, IO] =
    EditorEndpoints.deleteAtCursor.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        input        <- parseEditorInput(c)
        editorResult <- EditorApi.deleteAtCursor(input)
      yield editorResult)(encodeEditorResult)
    }

  val copySelection: ServerEndpoint[Any, IO] =
    EditorEndpoints.copySelection.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        input  <- parseEditorInput(c)
        result <- EditorApi.copySelection(input)
      yield result)(encodeClipboardResult)
    }

  val cutSelection: ServerEndpoint[Any, IO] =
    EditorEndpoints.cutSelection.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        input  <- parseEditorInput(c)
        result <- EditorApi.cutSelection(input)
      yield result)(encodeClipboardResult)
    }

  val pasteClipboard: ServerEndpoint[Any, IO] =
    EditorEndpoints.pasteClipboard.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        input <- parseEditorInput(c)
        clipboardJson <- c
          .downField("clipboardJson")
          .as[String]
          .left
          .map(e => ApiError.MissingField(s"clipboardJson: ${e.message}"))
        result <- EditorApi.pasteClipboard(input, clipboardJson)
      yield result)(encodeEditorResult)
    }

  val all: List[ServerEndpoint[Any, IO]] = List(
    insertSwar,
    insertChikari,
    insertRest,
    insertSustain,
    deleteLast,
    insertDualSwar,
    insertSwarGroup,
    deleteAtCursor,
    copySelection,
    cutSelection,
    pasteClipboard
  )
