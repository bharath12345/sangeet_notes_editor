package com.varpas.sangeet.server.routes

import cats.effect.IO
import sttp.tapir.server.ServerEndpoint

import com.varpas.sangeet.core.api.CursorApi
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.server.endpoints.CursorEndpoints
import com.varpas.sangeet.server.routes.EditorResultCodec._
import com.varpas.sangeet.server.routes.JsonParsing._
import com.varpas.sangeet.server.routes.RouteHelper._

object CursorRoutes:

  val nextBeat: ServerEndpoint[Any, IO] =
    CursorEndpoints.nextBeat.serverLogic { body =>
      val c  = body.hcursor
      val sb = c.downField("startingBeat").as[Int].getOrElse(1)
      handleResult(parseCursor(c))(cur => encodeCursor(CursorApi.nextBeat(cur, sb)))
    }

  val prevBeat: ServerEndpoint[Any, IO] =
    CursorEndpoints.prevBeat.serverLogic { body =>
      val c  = body.hcursor
      val sb = c.downField("startingBeat").as[Int].getOrElse(1)
      handleResult(parseCursor(c))(cur => encodeCursor(CursorApi.prevBeat(cur, sb)))
    }

  val nextSubBeat: ServerEndpoint[Any, IO] =
    CursorEndpoints.nextSubBeat.serverLogic { body =>
      val c  = body.hcursor
      val sb = c.downField("startingBeat").as[Int].getOrElse(1)
      handleResult(parseCursor(c))(cur => encodeCursor(CursorApi.nextSubBeat(cur, sb)))
    }

  val setSubdivisions: ServerEndpoint[Any, IO] =
    CursorEndpoints.setSubdivisions.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        cursor <- parseCursor(c)
        n      <- parseField[Int](c, "subdivisions")
        cur    <- CursorApi.setSubdivisions(cursor, n)
      yield cur)(encodeCursor)
    }

  val setOctave: ServerEndpoint[Any, IO] =
    CursorEndpoints.setOctave.serverLogic { body =>
      val c = body.hcursor
      handleResult(for
        cursor <- parseCursor(c)
        octave <- parseField[Octave](c, "octave")
      yield CursorApi.setOctave(cursor, octave))(encodeCursor)
    }

  val moveTo: ServerEndpoint[Any, IO] =
    CursorEndpoints.moveTo.serverLogic { body =>
      val c  = body.hcursor
      val sb = c.downField("startingBeat").as[Int].getOrElse(1)
      handleResult(for
        cursor <- parseCursor(c)
        cycle  <- parseField[Int](c, "cycle")
        beat   <- parseField[Int](c, "beat")
        cur    <- CursorApi.moveTo(cursor, cycle, beat, sb)
      yield cur)(encodeCursor)
    }

  val all: List[ServerEndpoint[Any, IO]] = List(
    nextBeat,
    prevBeat,
    nextSubBeat,
    setSubdivisions,
    setOctave,
    moveTo
  )
