package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.*
import sttp.tapir.server.ServerEndpoint
import com.varpas.sangeet.core.api.{ApiError, OrnamentApi}
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.server.{ApiEnvelope, ErrorMapping}
import com.varpas.sangeet.server.endpoints.OrnamentEndpoints
import com.varpas.sangeet.server.routes.JsonParsing.*
import com.varpas.sangeet.server.routes.EditorResultCodec.*

object OrnamentRoutes:

  val simple: ServerEndpoint[Any, IO] =
    OrnamentEndpoints.simple.serverLogic { body =>
      val c = body.hcursor
      val result = for
        input <- parseEditorInput(c)
        ornamentType <- parseField[String](c, "ornamentType")
        ornament <- ornamentType.toLowerCase match
          case "gamak"   => Right(Gamak())
          case "andolan" => Right(Andolan())
          case "gitkari" => Right(Gitkari())
          case other     => Left(ApiError.InvalidOrnamentType(other))
        editorResult <- OrnamentApi.addSimpleOrnament(input, ornament)
      yield editorResult

      result match
        case Right(r) => IO.pure(Right(ApiEnvelope.successRaw(encodeEditorResult(r))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val singleNote: ServerEndpoint[Any, IO] =
    OrnamentEndpoints.singleNote.serverLogic { body =>
      val c = body.hcursor
      val result = for
        input <- parseEditorInput(c)
        ornamentType <- parseField[String](c, "ornamentType")
        noteRef <- parseField[NoteRef](c, "noteRef")
        ornament <- ornamentType.toLowerCase match
          case "kanswar" => Right(KanSwar(noteRef))
          case "sparsh"  => Right(Sparsh(noteRef))
          case "ghaseet" => Right(Ghaseet(noteRef))
          case other     => Left(ApiError.InvalidOrnamentType(other))
        editorResult <- OrnamentApi.addSingleNoteOrnament(input, ornament)
      yield editorResult

      result match
        case Right(r) => IO.pure(Right(ApiEnvelope.successRaw(encodeEditorResult(r))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val meend: ServerEndpoint[Any, IO] =
    OrnamentEndpoints.meend.serverLogic { body =>
      val c = body.hcursor
      val result = for
        input <- parseEditorInput(c)
        startNote <- parseField[NoteRef](c, "startNote")
        endNote <- parseField[NoteRef](c, "endNote")
        direction <- parseField[MeendDirection](c, "direction")
        intermediateNotes <- parseFieldOr[List[NoteRef]](c, "intermediateNotes", Nil)
        editorResult <- OrnamentApi.addMeend(input, startNote, endNote, direction, intermediateNotes)
      yield editorResult

      result match
        case Right(r) => IO.pure(Right(ApiEnvelope.successRaw(encodeEditorResult(r))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val krintan: ServerEndpoint[Any, IO] =
    OrnamentEndpoints.krintan.serverLogic { body =>
      val c = body.hcursor
      val result = for
        input <- parseEditorInput(c)
        notes <- parseField[List[NoteRef]](c, "notes")
        editorResult <- OrnamentApi.addKrintan(input, notes)
      yield editorResult

      result match
        case Right(r) => IO.pure(Right(ApiEnvelope.successRaw(encodeEditorResult(r))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val murki: ServerEndpoint[Any, IO] =
    OrnamentEndpoints.murki.serverLogic { body =>
      val c = body.hcursor
      val result = for
        input <- parseEditorInput(c)
        notes <- parseField[List[NoteRef]](c, "notes")
        editorResult <- OrnamentApi.addMurki(input, notes)
      yield editorResult

      result match
        case Right(r) => IO.pure(Right(ApiEnvelope.successRaw(encodeEditorResult(r))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val zamzama: ServerEndpoint[Any, IO] =
    OrnamentEndpoints.zamzama.serverLogic { body =>
      val c = body.hcursor
      val result = for
        input <- parseEditorInput(c)
        notes <- parseField[List[NoteRef]](c, "notes")
        editorResult <- OrnamentApi.addZamzama(input, notes)
      yield editorResult

      result match
        case Right(r) => IO.pure(Right(ApiEnvelope.successRaw(encodeEditorResult(r))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val all: List[ServerEndpoint[Any, IO]] = List(
    simple, singleNote, meend, krintan, murki, zamzama
  )
