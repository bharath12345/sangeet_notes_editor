package sangeet.layout

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals

class LineBreakerMultiTaalSpec extends AnyFlatSpec with Matchers:

  private val config = LayoutConfig()

  private def eventsForBeats(numBeats: Int): List[Event] =
    (0 until numBeats).map { beat =>
      Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya,
        BeatPosition(0, beat, Rational.onBeat), Rational.fullBeat, None, Nil, None)
    }.toList

  private def breakLine(taal: Taal, events: List[Event]): List[GridLine] =
    val cells = BeatGrouper.group(events)
    LineBreaker.break(cells, taal, config)

  "LineBreaker with Rupak (7 matras)" should "produce one line for full cycle" in {
    val lines = breakLine(Taals.all("rupak"), eventsForBeats(7))
    lines.size shouldBe 1
    lines.head.cells.size shouldBe 7
  }

  it should "have vibhag breaks at correct positions" in {
    val lines = breakLine(Taals.all("rupak"), eventsForBeats(7))
    // Rupak: 3+2+2, breaks at cell indices 3 and 5
    lines.head.vibhagBreaks should contain allOf (3, 5)
  }

  "LineBreaker with Dadra (6 matras)" should "produce one line" in {
    val lines = breakLine(Taals.all("dadra"), eventsForBeats(6))
    lines.size shouldBe 1
    lines.head.cells.size shouldBe 6
  }

  "LineBreaker with Ektaal (12 matras)" should "produce one line" in {
    val lines = breakLine(Taals.all("ektaal"), eventsForBeats(12))
    lines.size shouldBe 1
    lines.head.cells.size shouldBe 12
  }

  "LineBreaker with Jhaptaal (10 matras)" should "produce one line" in {
    val lines = breakLine(Taals.all("jhaptaal"), eventsForBeats(10))
    lines.size shouldBe 1
    lines.head.cells.size shouldBe 10
  }

  "LineBreaker with Keherwa (8 matras)" should "produce one line" in {
    val lines = breakLine(Taals.all("keherwa"), eventsForBeats(8))
    lines.size shouldBe 1
    lines.head.cells.size shouldBe 8
  }

  "LineBreaker with Dhamar (14 matras)" should "produce one line" in {
    val lines = breakLine(Taals.all("dhamar"), eventsForBeats(14))
    lines.size shouldBe 1
    lines.head.cells.size shouldBe 14
  }

  "LineBreaker" should "handle empty events" in {
    val lines = breakLine(Taals.teentaal, Nil)
    lines shouldBe empty
  }

  it should "handle multiple cycles" in {
    val events = (0 until 2).flatMap { cycle =>
      (0 until 16).map { beat =>
        Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya,
          BeatPosition(cycle, beat, Rational.onBeat), Rational.fullBeat, None, Nil, None)
      }
    }.toList
    val lines = breakLine(Taals.teentaal, events)
    lines.size shouldBe 2 // one line per cycle
  }
