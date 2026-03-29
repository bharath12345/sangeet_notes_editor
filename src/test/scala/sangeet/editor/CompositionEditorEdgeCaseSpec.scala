package sangeet.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals
import sangeet.raag.Raags

class CompositionEditorEdgeCaseSpec extends AnyFlatSpec with Matchers:

  private val yaman = Raags.all("yaman")
  private val teentaal = Taals.teentaal

  private def mkEditor = CompositionEditor.empty(teentaal, yaman)

  private def addNote(ed: CompositionEditor, note: Note, cycle: Int = 0, beat: Int = 0): CompositionEditor =
    val event = Event.Swar(note, Variant.Shuddha, Octave.Madhya,
      BeatPosition(cycle, beat, Rational.onBeat), Rational.fullBeat, None, Nil, None)
    ed.addEvent(event)

  "maxCycle" should "return 0 for empty section" in {
    mkEditor.maxCycle shouldBe 0
  }

  it should "return correct cycle for events" in {
    val ed = addNote(addNote(mkEditor, Note.Sa, 0, 0), Note.Re, 2, 5)
    ed.maxCycle shouldBe 2
  }

  "removeSection" should "return None for single-section composition" in {
    val ed = CompositionEditor.create("Test", CompositionType.Palta, teentaal, yaman, None)
    ed.removeSection(0) shouldBe None
  }

  it should "remove a section and adjust index" in {
    val ed = mkEditor // Gat has 2 sections
    ed.composition.sections.size shouldBe 2
    val result = ed.removeSection(0)
    result shouldBe defined
    result.get.composition.sections.size shouldBe 1
    result.get.currentSectionIndex shouldBe 0
  }

  it should "adjust currentSectionIndex when removing before it" in {
    val ed = mkEditor.copy(currentSectionIndex = 1)
    val result = ed.removeSection(0)
    result.get.currentSectionIndex shouldBe 0
  }

  "renameSection" should "change section name" in {
    val ed = mkEditor.renameSection(0, "My Gat")
    ed.composition.sections(0).name shouldBe "My Gat"
  }

  "moveSection" should "swap sections" in {
    val ed = mkEditor
    val moved = ed.moveSection(0, 1)
    moved.composition.sections(0).name shouldBe "Antara"
    moved.composition.sections(1).name shouldBe "Gat"
  }

  it should "track currentSectionIndex when moving current section" in {
    val ed = mkEditor // currentSectionIndex = 0
    val moved = ed.moveSection(0, 1)
    moved.currentSectionIndex shouldBe 1
  }

  it should "be no-op for same index" in {
    val ed = mkEditor
    val moved = ed.moveSection(0, 0)
    moved.composition.sections shouldBe ed.composition.sections
  }

  "modifyLastSwar" should "return None when no swar events exist" in {
    mkEditor.modifyLastSwar(s => s.copy(stroke = Some(Stroke.Da))) shouldBe None
  }

  it should "modify last swar in section" in {
    val ed = addNote(mkEditor, Note.Sa)
    val result = ed.modifyLastSwar(s => s.copy(stroke = Some(Stroke.Da)))
    result shouldBe defined
    val swar = result.get.currentSection.events.head.asInstanceOf[Event.Swar]
    swar.stroke shouldBe Some(Stroke.Da)
  }

  "Event.position" should "return beat for all event types" in {
    val pos = BeatPosition(1, 5, Rational.onBeat)
    Event.Swar(Note.Sa, Variant.Shuddha, Octave.Madhya, pos, Rational.fullBeat, None, Nil, None).position shouldBe pos
    Event.Rest(pos, Rational.fullBeat).position shouldBe pos
    Event.Sustain(pos, Rational.fullBeat).position shouldBe pos
  }
