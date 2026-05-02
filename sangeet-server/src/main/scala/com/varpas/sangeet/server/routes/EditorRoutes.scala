package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.*
import sttp.tapir.server.ServerEndpoint
import com.varpas.sangeet.core.api.{ApiError, EditorApi}
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.server.{ApiEnvelope, ErrorMapping}
import com.varpas.sangeet.server.endpoints.EditorEndpoints
import com.varpas.sangeet.server.routes.JsonParsing.*
import com.varpas.sangeet.server.routes.EditorResultCodec.*

object EditorRoutes:

  val insertSwar: ServerEndpoint[Any, IO] =
    EditorEndpoints.insertSwar.serverLogic { body =>
      val c = body.hcursor
      val result = for
        input <- parseEditorInput(c)
        note <- parseField[Note](c, "note")
        variant <- parseField[Variant](c, "variant")
        octave <- parseField[Octave](c, "octave")
        editorResult <- EditorApi.insertSwar(input, note, variant, octave)
      yield editorResult

      result match
        case Right(r) => IO.pure(Right(ApiEnvelope.successRaw(encodeEditorResult(r))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val insertRest: ServerEndpoint[Any, IO] =
    EditorEndpoints.insertRest.serverLogic { body =>
      val c = body.hcursor
      val result = for
        input <- parseEditorInput(c)
        editorResult <- EditorApi.insertRest(input)
      yield editorResult

      result match
        case Right(r) => IO.pure(Right(ApiEnvelope.successRaw(encodeEditorResult(r))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val insertSustain: ServerEndpoint[Any, IO] =
    EditorEndpoints.insertSustain.serverLogic { body =>
      val c = body.hcursor
      val result = for
        input <- parseEditorInput(c)
        editorResult <- EditorApi.insertSustain(input)
      yield editorResult

      result match
        case Right(r) => IO.pure(Right(ApiEnvelope.successRaw(encodeEditorResult(r))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val deleteLast: ServerEndpoint[Any, IO] =
    EditorEndpoints.deleteLast.serverLogic { body =>
      val c = body.hcursor
      val result = for
        input <- parseEditorInput(c)
        editorResult <- EditorApi.deleteLastEvent(input)
      yield editorResult

      result match
        case Right(r) => IO.pure(Right(ApiEnvelope.successRaw(encodeEditorResult(r))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val insertDualSwar: ServerEndpoint[Any, IO] =
    EditorEndpoints.insertDualSwar.serverLogic { body =>
      val c = body.hcursor
      val result = for
        input <- parseEditorInput(c)
        note <- parseField[Note](c, "note")
        variant <- parseField[Variant](c, "variant")
        octave <- parseField[Octave](c, "octave")
        editorResult <- EditorApi.insertDualSwar(input, note, variant, octave)
      yield editorResult

      result match
        case Right(r) => IO.pure(Right(ApiEnvelope.successRaw(encodeEditorResult(r))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val all: List[ServerEndpoint[Any, IO]] = List(
    insertSwar, insertRest, insertSustain, deleteLast, insertDualSwar
  )
