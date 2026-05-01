package com.varpas.sangeet.core.api

enum ApiError:
  case InvalidNoteVariant(note: String, variant: String)
  case InvalidSectionIndex(index: Int, max: Int)
  case LastSection
  case EmptySection
  case NoSwarTarget
  case NoSwarAtPosition
  case EmptyNotes
  case InsufficientNotes(required: Int, provided: Int)
  case InvalidOrnamentType(name: String)
  case ParseError(message: String)
  case VersionError(version: String)
  case ValidationError(message: String)
  case NotFound(entity: String, name: String)
  case ExportError(message: String)
  case MissingField(field: String)
