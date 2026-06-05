package com.varpas.sangeet.server.routes

import io.circe.{DecodingFailure, HCursor, Json}

import com.varpas.sangeet.core.api.{ApiError, EditorInput}
import com.varpas.sangeet.core.editor.CursorModel
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.core.model._

/** Shared JSON parsing helpers for route implementations. */
object JsonParsing:

  def parseCursor(c: HCursor): Either[ApiError, CursorModel] =
    val cursorField = c.downField("cursor")
    val cursorC     = cursorField.as[Json].map(_.hcursor).getOrElse(c)
    for
      taal              <- parseField[Taal](cursorC, "taal")
      cycle             <- parseFieldOr(cursorC, "cycle", 0)
      beat              <- parseFieldOr(cursorC, "beat", 0)
      subIndex          <- parseFieldOr(cursorC, "subIndex", 0)
      totalSubdivisions <- parseFieldOr(cursorC, "totalSubdivisions", 1)
      octaveStr         <- parseFieldOr(cursorC, "currentOctave", "madhya")
      octave            <- parseOctaveString(octaveStr)
    yield CursorModel(taal, cycle, beat, subIndex, totalSubdivisions, octave)

  def parseEditorInput(c: HCursor): Either[ApiError, EditorInput] =
    for
      composition  <- parseField[Composition](c, "composition")
      sectionIndex <- parseFieldOr(c, "sectionIndex", 0)
      cursor       <- parseCursor(c)
    yield EditorInput(composition, sectionIndex, cursor)

  def parseField[A](c: HCursor, field: String)(using d: io.circe.Decoder[A]): Either[ApiError, A] =
    c.downField(field).as[A].left.map(e => ApiError.MissingField(s"$field: ${e.message}"))

  def parseFieldOr[A](c: HCursor, field: String, default: A)(using d: io.circe.Decoder[A]): Either[ApiError, A] =
    c.downField(field).as[A] match
      case Right(v) => Right(v)
      case Left(_)  => Right(default)

  def parseOctaveString(s: String): Either[ApiError, Octave] =
    s.toLowerCase match
      case "atimandra" => Right(Octave.AtiMandra)
      case "mandra"    => Right(Octave.Mandra)
      case "madhya"    => Right(Octave.Madhya)
      case "taar"      => Right(Octave.Taar)
      case "atitaar"   => Right(Octave.AtiTaar)
      case other       => Left(ApiError.ValidationError(s"Invalid octave: $other"))
