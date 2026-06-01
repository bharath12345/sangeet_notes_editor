package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.audio.{PlaybackScheduler, TimedNote}

object PlaybackApi:

  private def validateBpm(bpm: Double): Either[ApiError, Unit] =
    if bpm <= 0 then Left(ApiError.ValidationError(s"BPM must be positive, got $bpm"))
    else Right(())

  private def validateMatras(matras: Int): Either[ApiError, Unit] =
    if matras <= 0 then Left(ApiError.ValidationError(s"Matras must be positive, got $matras"))
    else Right(())

  def schedulePlayback(
    events: List[Event],
    bpm: Double,
    matras: Int
  ): Either[ApiError, List[TimedNote]] =
    for
      _ <- validateBpm(bpm)
      _ <- validateMatras(matras)
    yield PlaybackScheduler.schedule(events, bpm, matras)

  def scheduleSectionPlayback(
    section: Section,
    bpm: Double,
    matras: Int
  ): Either[ApiError, List[TimedNote]] =
    for
      _ <- validateBpm(bpm)
      _ <- validateMatras(matras)
    yield PlaybackScheduler.schedule(section.events, bpm, matras)

  def scheduleCompositionPlayback(
    composition: Composition,
    bpm: Double
  ): Either[ApiError, List[TimedNote]] =
    for _ <- validateBpm(bpm)
    yield
      val allEvents = composition.sections.flatMap(_.events)
      PlaybackScheduler.schedule(allEvents, bpm, composition.metadata.taal.matras)
