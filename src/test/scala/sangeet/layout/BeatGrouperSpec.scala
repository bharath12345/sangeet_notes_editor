package sangeet.layout

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*

class BeatGrouperSpec extends AnyFlatSpec with Matchers:

  def swar(beat: Int, sub: Rational = Rational.onBeat, note: Note = Note.Sa): Event.Swar =
    Event.Swar(note, Variant.Shuddha, Octave.Madhya,
      BeatPosition(0, beat, sub), Rational.fullBeat, None, Nil, None)

  "BeatGrouper" should "group events by (cycle, beat)" in {
    val events = List(swar(0), swar(1), swar(2))
    val cells = BeatGrouper.group(events)
    cells should have length 3
    cells.head.events should have length 1
  }

  it should "put multiple sub-beat events into the same cell" in {
    val events = List(
      swar(0, Rational.onBeat, Note.Sa),
      swar(0, Rational(1, 2), Note.Re)
    )
    val cells = BeatGrouper.group(events)
    cells should have length 1
    cells.head.events should have length 2
  }

  it should "order events within a cell by subdivision" in {
    val events = List(
      swar(0, Rational(1, 2), Note.Re),
      swar(0, Rational.onBeat, Note.Sa)
    )
    val cells = BeatGrouper.group(events)
    cells.head.events.head match
      case s: Event.Swar => s.note shouldBe Note.Sa
      case _ => fail("Expected Swar")
  }

  it should "handle events across multiple cycles" in {
    val events = List(
      swar(0).copy(beat = BeatPosition(0, 0, Rational.onBeat)),
      swar(0).copy(beat = BeatPosition(1, 0, Rational.onBeat))
    )
    val cells = BeatGrouper.group(events)
    cells should have length 2
    cells.head.position.cycle shouldBe 0
    cells(1).position.cycle shouldBe 1
  }

  it should "compute max subdivisions per cell" in {
    val events = List(
      swar(0, Rational.onBeat, Note.Sa),
      swar(0, Rational(1, 4), Note.Re),
      swar(0, Rational(2, 4), Note.Ga),
      swar(0, Rational(3, 4), Note.Ma)
    )
    val cells = BeatGrouper.group(events)
    cells.head.events should have length 4
  }
