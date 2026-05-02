package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.*
import sttp.tapir.server.ServerEndpoint
import com.varpas.sangeet.core.api.CursorApi
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.server.{ApiEnvelope, ErrorMapping}
import com.varpas.sangeet.server.endpoints.CursorEndpoints
import com.varpas.sangeet.server.routes.JsonParsing.*
import com.varpas.sangeet.server.routes.EditorResultCodec.*

object CursorRoutes:

  val nextBeat: ServerEndpoint[Any, IO] =
    CursorEndpoints.nextBeat.serverLogic { body =>
      parseCursor(body.hcursor) match
        case Right(cursor) =>
          val result = CursorApi.nextBeat(cursor)
          IO.pure(Right(ApiEnvelope.successRaw(encodeCursor(result))))
        case Left(err) =>
          IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val prevBeat: ServerEndpoint[Any, IO] =
    CursorEndpoints.prevBeat.serverLogic { body =>
      parseCursor(body.hcursor) match
        case Right(cursor) =>
          val result = CursorApi.prevBeat(cursor)
          IO.pure(Right(ApiEnvelope.successRaw(encodeCursor(result))))
        case Left(err) =>
          IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val nextSubBeat: ServerEndpoint[Any, IO] =
    CursorEndpoints.nextSubBeat.serverLogic { body =>
      parseCursor(body.hcursor) match
        case Right(cursor) =>
          val result = CursorApi.nextSubBeat(cursor)
          IO.pure(Right(ApiEnvelope.successRaw(encodeCursor(result))))
        case Left(err) =>
          IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val setSubdivisions: ServerEndpoint[Any, IO] =
    CursorEndpoints.setSubdivisions.serverLogic { body =>
      val c = body.hcursor
      val result = for
        cursor <- parseCursor(c)
        n <- parseField[Int](c, "subdivisions")
      yield CursorApi.setSubdivisions(cursor, n)

      result match
        case Right(cur) => IO.pure(Right(ApiEnvelope.successRaw(encodeCursor(cur))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val setOctave: ServerEndpoint[Any, IO] =
    CursorEndpoints.setOctave.serverLogic { body =>
      val c = body.hcursor
      val result = for
        cursor <- parseCursor(c)
        octave <- parseField[Octave](c, "octave")
      yield CursorApi.setOctave(cursor, octave)

      result match
        case Right(cur) => IO.pure(Right(ApiEnvelope.successRaw(encodeCursor(cur))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val moveTo: ServerEndpoint[Any, IO] =
    CursorEndpoints.moveTo.serverLogic { body =>
      val c = body.hcursor
      val result = for
        cursor <- parseCursor(c)
        cycle <- parseField[Int](c, "cycle")
        beat <- parseField[Int](c, "beat")
      yield CursorApi.moveTo(cursor, cycle, beat)

      result match
        case Right(cur) => IO.pure(Right(ApiEnvelope.successRaw(encodeCursor(cur))))
        case Left(err) => IO.pure(Left(ErrorMapping.toResponse(err)))
    }

  val all: List[ServerEndpoint[Any, IO]] = List(
    nextBeat, prevBeat, nextSubBeat, setSubdivisions, setOctave, moveTo
  )
