package sangeet.layout

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals

class LineBreakerSpec extends AnyFlatSpec with Matchers:

  def makeCells(matras: Int, notesPerBeat: Int = 1): List[BeatCell] =
    (0 until matras).toList.map { beat =>
      val events = (0 until notesPerBeat).toList.map { sub =>
        Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya,
          BeatPosition(0, beat, Rational(sub, math.max(notesPerBeat, 1))),
          Rational(1, notesPerBeat), None, Nil, None)
      }
      BeatCell(CycleAndBeat(0, beat), events)
    }

  val config = LayoutConfig()

  "LineBreaker" should "put a full Teentaal cycle on one line for low density" in {
    val cells = makeCells(16, notesPerBeat = 1)
    val lines = LineBreaker.break(cells, Taals.teentaal, config)
    lines should have length 1
    lines.head.cells should have length 16
  }

  it should "mark vibhag boundaries" in {
    val cells = makeCells(16, notesPerBeat = 1)
    val lines = LineBreaker.break(cells, Taals.teentaal, config)
    // Teentaal: vibhags at 0, 4, 8, 12
    lines.head.vibhagBreaks shouldBe List(4, 8, 12)
  }

  it should "include vibhag markers" in {
    val cells = makeCells(16, notesPerBeat = 1)
    val lines = LineBreaker.break(cells, Taals.teentaal, config)
    val markers = lines.head.markers
    markers should contain ((0, VibhagMarker.Sam))
    markers should contain ((8, VibhagMarker.Khali))
  }

  it should "split high density into vibhag-per-line" in {
    val cells = makeCells(16, notesPerBeat = 6)
    val highDensityConfig = LayoutConfig(highDensityThreshold = 5)
    val lines = LineBreaker.break(cells, Taals.teentaal, highDensityConfig)
    lines.length should be > 1
  }

  it should "handle multiple cycles" in {
    val cells = (0 to 1).flatMap { cycle =>
      (0 until 16).map { beat =>
        BeatCell(CycleAndBeat(cycle, beat), List(
          Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya,
            BeatPosition(cycle, beat, Rational.onBeat), Rational.fullBeat, None, Nil, None)
        ))
      }
    }.toList
    val lines = LineBreaker.break(cells, Taals.teentaal, config)
    lines should have length 2
  }
