package com.varpas.sangeet.server

import io.circe.Json
import sttp.model.StatusCode

import com.varpas.sangeet.core.api.ApiError

object ErrorMapping:

  def toStatusCode(error: ApiError): StatusCode = error match
    case _: ApiError.InvalidNoteVariant  => StatusCode.BadRequest
    case _: ApiError.InvalidSectionIndex => StatusCode.BadRequest
    case ApiError.LastSection            => StatusCode.BadRequest
    case ApiError.EmptySection           => StatusCode.BadRequest
    case ApiError.NoSwarTarget           => StatusCode.BadRequest
    case ApiError.NoSwarAtPosition       => StatusCode.NotFound
    case ApiError.EmptyNotes             => StatusCode.BadRequest
    case _: ApiError.InsufficientNotes   => StatusCode.BadRequest
    case _: ApiError.InvalidOrnamentType => StatusCode.BadRequest
    case _: ApiError.ParseError          => StatusCode.BadRequest
    case _: ApiError.VersionError        => StatusCode.BadRequest
    case _: ApiError.ValidationError     => StatusCode.BadRequest
    case _: ApiError.NotFound            => StatusCode.NotFound
    case _: ApiError.ExportError         => StatusCode.InternalServerError
    case _: ApiError.MissingField        => StatusCode.BadRequest

  def toErrorCode(error: ApiError): String = error match
    case _: ApiError.InvalidNoteVariant  => "INVALID_NOTE_VARIANT"
    case _: ApiError.InvalidSectionIndex => "INVALID_SECTION_INDEX"
    case ApiError.LastSection            => "LAST_SECTION"
    case ApiError.EmptySection           => "EMPTY_SECTION"
    case ApiError.NoSwarTarget           => "NO_SWAR_TARGET"
    case ApiError.NoSwarAtPosition       => "NO_SWAR_AT_POSITION"
    case ApiError.EmptyNotes             => "EMPTY_NOTES"
    case _: ApiError.InsufficientNotes   => "INSUFFICIENT_NOTES"
    case _: ApiError.InvalidOrnamentType => "INVALID_ORNAMENT_TYPE"
    case _: ApiError.ParseError          => "PARSE_ERROR"
    case _: ApiError.VersionError        => "VERSION_ERROR"
    case _: ApiError.ValidationError     => "VALIDATION_ERROR"
    case _: ApiError.NotFound            => "NOT_FOUND"
    case _: ApiError.ExportError         => "EXPORT_ERROR"
    case _: ApiError.MissingField        => "MISSING_FIELD"

  def toMessage(error: ApiError): String = error match
    case ApiError.InvalidNoteVariant(note, variant) => s"Invalid variant '$variant' for note '$note'"
    case ApiError.InvalidSectionIndex(idx, max)     => s"Section index $idx out of range (max: $max)"
    case ApiError.LastSection                       => "Cannot remove the last section"
    case ApiError.EmptySection                      => "Section is empty"
    case ApiError.NoSwarTarget                      => "No swar note to modify"
    case ApiError.NoSwarAtPosition                  => "No swar at the current position"
    case ApiError.EmptyNotes                        => "Notes list cannot be empty"
    case ApiError.InsufficientNotes(req, prov)      => s"Need at least $req notes, got $prov"
    case ApiError.InvalidOrnamentType(name)         => s"Unknown ornament type: $name"
    case ApiError.ParseError(msg)                   => s"Parse error: $msg"
    case ApiError.VersionError(ver)                 => s"Unsupported version: $ver"
    case ApiError.ValidationError(msg)              => s"Validation error: $msg"
    case ApiError.NotFound(entity, name)            => s"$entity '$name' not found"
    case ApiError.ExportError(msg)                  => s"Export error: $msg"
    case ApiError.MissingField(field)               => s"Missing required field: $field"

  def toResponse(error: ApiError): (StatusCode, Json) =
    (toStatusCode(error), ApiEnvelope.failure(toErrorCode(error), toMessage(error)))
