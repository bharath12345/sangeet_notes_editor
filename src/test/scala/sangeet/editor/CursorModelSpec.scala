package sangeet.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals

class CursorModelSpec extends AnyFlatSpec with Matchers:

  val cursor = CursorModel(Taals.teentaal)

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
