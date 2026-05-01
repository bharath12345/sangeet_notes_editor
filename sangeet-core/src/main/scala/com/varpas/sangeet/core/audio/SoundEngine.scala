package com.varpas.sangeet.core.audio

import com.varpas.sangeet.core.model.*

trait SoundEngine:
  def init(): Unit
  def playNote(note: TimedNote): Unit
  def noteOff(midiNote: Int): Unit
  def stop(): Unit
  def shutdown(): Unit
