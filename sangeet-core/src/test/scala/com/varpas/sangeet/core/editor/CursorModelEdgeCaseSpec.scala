package com.varpas.sangeet.core.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.core.model._

class CursorModelEdgeCaseSpec extends AnyFlatSpec with Matchers:

  val teentaal = Taal(
    "Teentaal",
    16,
    List(
      Vibhag(4, VibhagMarker.Sam),
      Vibhag(4, VibhagMarker.Taali(2)),
      Vibhag(4, VibhagMarker.Khali),
      Vibhag(4, VibhagMarker.Taali(3))
    ),
    None
  )

  val rupak = Taal(
    "Rupak",
    7,
    List(
      Vibhag(3, VibhagMarker.Khali),
      Vibhag(2, VibhagMarker.Taali(2)),
      Vibhag(2, VibhagMarker.Taali(3))
    ),
    None
  )

  val dadra = Taal(
    "Dadra",
    6,
    List(
      Vibhag(3, VibhagMarker.Sam),
      Vibhag(3, VibhagMarker.Khali)
    ),
    None
  )

  val jhaptaal = Taal(
    "Jhaptaal",
    10,
    List(
      Vibhag(2, VibhagMarker.Sam),
      Vibhag(3, VibhagMarker.Taali(2)),
      Vibhag(2, VibhagMarker.Khali),
      Vibhag(3, VibhagMarker.Taali(3))
    ),
    None
  )

  "CursorModel with Rupak (7 matras)" should "wrap at beat 7" in {
    val cursor   = CursorModel(rupak)
    val advanced = (0 until 7).foldLeft(cursor)((c, _) => c.nextBeat)
    advanced.cycle shouldBe 1
    advanced.beat shouldBe 0
  }

  "CursorModel with Dadra (6 matras)" should "wrap at beat 6" in {
    val cursor   = CursorModel(dadra)
    val advanced = (0 until 6).foldLeft(cursor)((c, _) => c.nextBeat)
    advanced.cycle shouldBe 1
    advanced.beat shouldBe 0
  }

  "CursorModel with Jhaptaal (10 matras)" should "wrap at beat 10" in {
    val cursor   = CursorModel(jhaptaal)
    val advanced = (0 until 10).foldLeft(cursor)((c, _) => c.nextBeat)
    advanced.cycle shouldBe 1
    advanced.beat shouldBe 0
  }

  "prevBeat" should "not go below cycle 0 beat 0" in {
    val cursor = CursorModel(teentaal)
    val prev   = cursor.prevBeat
    prev.cycle shouldBe 0
    prev.beat shouldBe 0
  }

  it should "wrap backward from cycle 1 beat 0 to cycle 0 last beat" in {
    val cursor = CursorModel(teentaal).copy(cycle = 1, beat = 0)
    val prev   = cursor.prevBeat
    prev.cycle shouldBe 0
    prev.beat shouldBe 15 // Teentaal has 16 matras
  }

  "nextSubBeat" should "advance within subdivisions" in {
    val cursor = CursorModel(teentaal).withSubdivisions(4)
    val c1     = cursor.nextSubBeat
    c1.subIndex shouldBe 1
    c1.beat shouldBe 0
    val c2 = c1.nextSubBeat
    c2.subIndex shouldBe 2
    val c3 = c2.nextSubBeat
    c3.subIndex shouldBe 3
    // Next sub-beat should wrap to next beat
    val c4 = c3.nextSubBeat
    c4.beat shouldBe 1
    c4.subIndex shouldBe 0
  }

  "withOctave" should "set and preserve octave" in {
    val cursor = CursorModel(teentaal).withOctave(Octave.Taar)
    cursor.currentOctave shouldBe Octave.Taar
    // nextBeat preserves octave (KeyHandler resets it, not CursorModel)
    cursor.nextBeat.currentOctave shouldBe Octave.Taar
  }

  "position" should "return correct BeatPosition" in {
    val cursor = CursorModel(teentaal).copy(cycle = 2, beat = 5).withSubdivisions(3).nextSubBeat
    cursor.position shouldBe BeatPosition(2, 5, Rational(1, 3))
  }

  "moveTo" should "reset subIndex to 0" in {
    val cursor = CursorModel(teentaal).withSubdivisions(4).nextSubBeat.nextSubBeat
    cursor.subIndex shouldBe 2
    val moved = cursor.moveTo(1, 3)
    moved.subIndex shouldBe 0
  }
