package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.model.{Taal, Raag}
import com.varpas.sangeet.core.taal.Taals
import com.varpas.sangeet.core.raag.Raags

object ReferenceApi:

  /** Get all built-in taals. */
  def allTaals: Map[String, Taal] =
    Taals.all

  /** Get a taal by name (case-insensitive). */
  def taalByName(name: String): Either[ApiError, Taal] =
    Taals.byName(name) match
      case Some(taal) => Right(taal)
      case None => Left(ApiError.NotFound("taal", name))

  /** Get all built-in raags. */
  def allRaags: Map[String, Raag] =
    Raags.all

  /** Get a raag by name (case-insensitive). */
  def raagByName(name: String): Either[ApiError, Raag] =
    Raags.byName(name) match
      case Some(raag) => Right(raag)
      case None => Left(ApiError.NotFound("raag", name))
