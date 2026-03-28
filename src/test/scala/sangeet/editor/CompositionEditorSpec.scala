package sangeet.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*
import sangeet.taal.Taals
import sangeet.raag.Raags

class CompositionEditorSpec extends AnyFlatSpec with Matchers:

  val testRaag = Raag("Yaman", None, None, None, None, None, None, None)

  "CompositionEditor.empty" should "create an untitled Gat composition" in {
    val editor = CompositionEditor.empty(Taals.teentaal, testRaag)
    editor.composition.metadata.title shouldBe "Untitled"
    editor.composition.metadata.compositionType shouldBe CompositionType.Gat
    editor.composition.metadata.instrument shouldBe Some("Sitar")
  }

  it should "have a single Sthayi section" in {
    val editor = CompositionEditor.empty(Taals.teentaal, testRaag)
    editor.composition.sections should have length 1
    editor.composition.sections.head.sectionType shouldBe SectionType.Sthayi
    editor.composition.sections.head.name shouldBe "Sthayi"
  }

  it should "start at section index 0" in {
    val editor = CompositionEditor.empty(Taals.teentaal, testRaag)
    editor.currentSectionIndex shouldBe 0
  }

  it should "have an empty events list" in {
    val editor = CompositionEditor.empty(Taals.teentaal, testRaag)
    editor.currentSection.events shouldBe empty
  }

  "CompositionEditor.create with Palta" should "create a Palta section instead of Sthayi" in {
    val editor = CompositionEditor.create(
      title = "My Palta",
      compositionType = CompositionType.Palta,
      taal = Taals.teentaal,
      raag = testRaag,
      laya = None
    )
    editor.composition.sections should have length 1
    editor.composition.sections.head.sectionType shouldBe SectionType.Palta
    editor.composition.sections.head.name shouldBe "Palta"
    editor.composition.metadata.compositionType shouldBe CompositionType.Palta
  }

  "CompositionEditor.create with Gat" should "create a Sthayi section" in {
    val editor = CompositionEditor.create(
      title = "Vilambit Gat",
      compositionType = CompositionType.Gat,
      taal = Taals.teentaal,
      raag = testRaag,
      laya = Some(Laya.Vilambit)
    )
    editor.composition.sections.head.sectionType shouldBe SectionType.Sthayi
    editor.composition.metadata.laya shouldBe Some(Laya.Vilambit)
  }

  "CompositionEditor.create with Bandish" should "create a Sthayi section" in {
    val editor = CompositionEditor.create(
      title = "Bandish",
      compositionType = CompositionType.Bandish,
      taal = Taals.teentaal,
      raag = testRaag,
      laya = Some(Laya.Madhya)
    )
    editor.composition.sections.head.sectionType shouldBe SectionType.Sthayi
    editor.composition.metadata.compositionType shouldBe CompositionType.Bandish
  }

  "CompositionEditor.create" should "set metadata fields correctly" in {
    val editor = CompositionEditor.create(
      title = "Test Composition",
      compositionType = CompositionType.Gat,
      taal = Taals.teentaal,
      raag = Raags.yaman,
      laya = Some(Laya.Drut)
    )
    val meta = editor.composition.metadata
    meta.title shouldBe "Test Composition"
    meta.raag.name shouldBe "Yaman"
    meta.taal.name shouldBe "Teentaal"
    meta.laya shouldBe Some(Laya.Drut)
    meta.instrument shouldBe Some("Sitar")
    meta.composer shouldBe None
    meta.createdAt should not be empty
    meta.updatedAt should not be empty
  }

  it should "initialize cursor for the given taal" in {
    val editor = CompositionEditor.create(
      title = "Test",
      compositionType = CompositionType.Gat,
      taal = Taals.teentaal,
      raag = testRaag,
      laya = None
    )
    editor.cursor.beat shouldBe 0
    editor.cursor.cycle shouldBe 0
  }

  "addEvent" should "append event to current section" in {
    val editor = CompositionEditor.empty(Taals.teentaal, testRaag)
    val event = Event.Rest(BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat)
    val updated = editor.addEvent(event)
    updated.currentSection.events should have length 1
    updated.currentSection.events.head shouldBe event
  }

  it should "preserve previous events" in {
    val editor = CompositionEditor.empty(Taals.teentaal, testRaag)
    val e1 = Event.Rest(BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat)
    val e2 = Event.Rest(BeatPosition(0, 1, Rational.onBeat), Rational.fullBeat)
    val updated = editor.addEvent(e1).addEvent(e2)
    updated.currentSection.events should have length 2
  }

  "removeLastEvent" should "remove the last event" in {
    val editor = CompositionEditor.empty(Taals.teentaal, testRaag)
    val event = Event.Rest(BeatPosition(0, 0, Rational.onBeat), Rational.fullBeat)
    val withEvent = editor.addEvent(event)
    val removed = withEvent.removeLastEvent
    removed shouldBe defined
    removed.get.currentSection.events shouldBe empty
  }

  it should "return None when section is empty" in {
    val editor = CompositionEditor.empty(Taals.teentaal, testRaag)
    editor.removeLastEvent shouldBe None
  }

  "updateCurrentSection" should "replace the current section" in {
    val editor = CompositionEditor.empty(Taals.teentaal, testRaag)
    val newSection = Section("Antara", SectionType.Antara, Nil)
    val updated = editor.updateCurrentSection(newSection)
    updated.currentSection.name shouldBe "Antara"
    updated.currentSection.sectionType shouldBe SectionType.Antara
  }

  "create with different taals" should "work with Jhaptaal" in {
    val editor = CompositionEditor.create(
      title = "Jhaptaal Gat",
      compositionType = CompositionType.Gat,
      taal = Taals.jhaptaal,
      raag = testRaag,
      laya = None
    )
    editor.composition.metadata.taal.matras shouldBe 10
  }

  it should "work with Rupak" in {
    val editor = CompositionEditor.create(
      title = "Rupak Gat",
      compositionType = CompositionType.Gat,
      taal = Taals.rupak,
      raag = testRaag,
      laya = None
    )
    editor.composition.metadata.taal.matras shouldBe 7
  }
