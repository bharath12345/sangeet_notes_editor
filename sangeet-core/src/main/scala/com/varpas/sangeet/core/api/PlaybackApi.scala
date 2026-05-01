package com.varpas.sangeet.core.api

import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.audio.{PlaybackScheduler, TimedNote}

object PlaybackApi:

  /** Schedule playback events from a list of composition events.
    * Returns a list of timed notes ready for audio engine consumption. */
  def schedulePlayback(
    events: List[Event],
    bpm: Double,
    matras: Int
  ): List[TimedNote] =
    PlaybackScheduler.schedule(events, bpm, matras)

  /** Schedule playback for a specific section. */
  def scheduleSectionPlayback(
    section: Section,
    bpm: Double,
    matras: Int
  ): List[TimedNote] =
    PlaybackScheduler.schedule(section.events, bpm, matras)

  /** Schedule playback for an entire composition. */
  def scheduleCompositionPlayback(
    composition: Composition,
    bpm: Double
  ): List[TimedNote] =
    val allEvents = composition.sections.flatMap(_.events)
    PlaybackScheduler.schedule(allEvents, bpm, composition.metadata.taal.matras)
