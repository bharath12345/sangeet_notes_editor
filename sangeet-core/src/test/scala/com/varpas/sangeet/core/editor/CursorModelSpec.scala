package com.varpas.sangeet.core.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.core.model._

class CursorModelSpec extends AnyFlatSpec with Matchers:

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

  val cursor = CursorModel(teentaal)

  "CursorModel" should "start at beat 0, cycle 0" in {
    cursor.beat shouldBe 0
    cursor.cycle shouldBe 0
    cursor.subIndex shouldBe 0
  }

  it should "advance to next beat" in {
    val next = cursor.nextBeat
    next.beat shouldBe 1
    next.cycle shouldBe 0
  }

  it should "wrap to next cycle at end of taal" in {
    var c = cursor
    for _ <- 0 until 16 do c = c.nextBeat
    c.beat shouldBe 0
    c.cycle shouldBe 1
  }

  it should "go to previous beat" in {
    val c = cursor.nextBeat.nextBeat.prevBeat
    c.beat shouldBe 1
  }

  it should "stay at beginning when already at cycle 0 beat 0" in {
    val c = cursor.prevBeat
    c.beat shouldBe 0
    c.cycle shouldBe 0
  }

  it should "wrap backward to previous cycle from cycle 1" in {
    var c = cursor
    for _ <- 0 until 16 do c = c.nextBeat // advance to cycle 1
    c = c.prevBeat
    c.beat shouldBe 15
    c.cycle shouldBe 0
  }

  it should "return current BeatPosition" in {
    val bp = cursor.nextBeat.position
    bp shouldBe BeatPosition(0, 1, Rational.onBeat)
  }

  it should "support setting subdivision count" in {
    val c = cursor.withSubdivisions(4)
    c.totalSubdivisions shouldBe 4
    c.subIndex shouldBe 0
  }

  it should "advance sub-index within subdivisions" in {
    val c = cursor.withSubdivisions(4).nextSubBeat
    c.subIndex shouldBe 1
    c.position.subdivision shouldBe Rational(1, 4)
  }

  it should "advance to next beat when sub-beats exhausted" in {
    var c = cursor.withSubdivisions(2)
    c = c.nextSubBeat // sub 1
    c = c.nextSubBeat // wraps to next beat, sub 0
    c.beat shouldBe 1
    c.subIndex shouldBe 0
  }

  it should "move to specific cycle and beat" in {
    val c = cursor.moveTo(2, 5)
    c.cycle shouldBe 2
    c.beat shouldBe 5
    c.subIndex shouldBe 0
  }

  // --- Selection tests ---

  it should "start with no selection" in {
    cursor.hasSelection shouldBe false
    cursor.selectionRange shouldBe None
  }

  it should "set selection anchor at current position" in {
    val c = cursor.nextBeat.nextBeat.startSelection
    c.hasSelection shouldBe true
    c.selectionAnchor shouldBe Some(BeatPosition(0, 2, Rational.onBeat))
  }

  it should "not overwrite existing anchor on repeated startSelection" in {
    val c  = cursor.nextBeat.startSelection
    val c2 = c.selectNextBeat.selectNextBeat.startSelection
    c2.selectionAnchor shouldBe Some(BeatPosition(0, 1, Rational.onBeat))
    c2.beat shouldBe 3
  }

  it should "clear selection" in {
    val c = cursor.startSelection.clearSelection
    c.hasSelection shouldBe false
  }

  it should "clear selection on nextBeat" in {
    val c = cursor.startSelection.nextBeat
    c.hasSelection shouldBe false
  }

  it should "clear selection on prevBeat" in {
    val c = cursor.nextBeat.nextBeat.startSelection.prevBeat
    c.hasSelection shouldBe false
  }

  it should "clear selection on moveTo" in {
    val c = cursor.startSelection.moveTo(1, 3)
    c.hasSelection shouldBe false
  }

  it should "selectNextBeat sets anchor and advances cursor" in {
    val c = cursor.nextBeat.selectNextBeat
    c.hasSelection shouldBe true
    c.selectionAnchor shouldBe Some(BeatPosition(0, 1, Rational.onBeat))
    c.beat shouldBe 2
  }

  it should "selectNextBeat extends from existing anchor" in {
    val c = cursor.nextBeat.selectNextBeat.selectNextBeat
    c.selectionAnchor shouldBe Some(BeatPosition(0, 1, Rational.onBeat))
    c.beat shouldBe 3
  }

  it should "selectPrevBeat sets anchor and moves cursor back" in {
    val c = cursor.nextBeat.nextBeat.nextBeat.selectPrevBeat
    c.hasSelection shouldBe true
    c.selectionAnchor shouldBe Some(BeatPosition(0, 3, Rational.onBeat))
    c.beat shouldBe 2
  }

  it should "selectPrevBeat does not go below cycle 0 beat 0" in {
    val c = cursor.selectPrevBeat
    c.beat shouldBe 0
    c.cycle shouldBe 0
  }

  it should "selectNextBeat wraps to next cycle" in {
    var c = cursor
    for _ <- 0 until 15 do c = c.nextBeat
    c = c.clearSelection
    val s = c.selectNextBeat
    s.beat shouldBe 0
    s.cycle shouldBe 1
    s.hasSelection shouldBe true
  }

  it should "compute selectionRange with anchor before cursor" in {
    val c     = cursor.nextBeat.selectNextBeat.selectNextBeat
    val range = c.selectionRange
    range shouldBe Some((BeatPosition(0, 1, Rational.onBeat), BeatPosition(0, 3, Rational.onBeat)))
  }

  it should "compute selectionRange with anchor after cursor" in {
    val c     = cursor.nextBeat.nextBeat.nextBeat.selectPrevBeat.selectPrevBeat
    val range = c.selectionRange
    range shouldBe Some((BeatPosition(0, 1, Rational.onBeat), BeatPosition(0, 3, Rational.onBeat)))
  }

  it should "selectToStart moves cursor to beginning" in {
    val c = cursor.nextBeat.nextBeat.nextBeat.selectToStart
    c.hasSelection shouldBe true
    c.selectionAnchor shouldBe Some(BeatPosition(0, 3, Rational.onBeat))
    c.beat shouldBe 0
    c.cycle shouldBe 0
  }

  it should "selectToEnd moves cursor to last beat" in {
    val c = cursor.nextBeat.selectToEnd(2)
    c.hasSelection shouldBe true
    c.selectionAnchor shouldBe Some(BeatPosition(0, 1, Rational.onBeat))
    c.beat shouldBe 15
    c.cycle shouldBe 2
  }

  it should "selectAll spans from start to end" in {
    val c = cursor.nextBeat.selectAll(2)
    c.selectionAnchor shouldBe Some(BeatPosition(0, 0, Rational.onBeat))
    c.beat shouldBe 15
    c.cycle shouldBe 2
  }
