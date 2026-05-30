package com.varpas.sangeet.core.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.varpas.sangeet.core.model.*

class EditorStressSpec extends AnyFlatSpec with Matchers:

  // --- Taals ---
  val teentaal = Taal("Teentaal", 16, List(
    Vibhag(4, VibhagMarker.Sam),
    Vibhag(4, VibhagMarker.Taali(2)),
    Vibhag(4, VibhagMarker.Khali),
    Vibhag(4, VibhagMarker.Taali(3))
  ), None)

  val jhaptaal = Taal("Jhaptaal", 10, List(
    Vibhag(2, VibhagMarker.Sam),
    Vibhag(3, VibhagMarker.Taali(2)),
    Vibhag(2, VibhagMarker.Khali),
    Vibhag(3, VibhagMarker.Taali(3))
  ), None)

  val rupak = Taal("Rupak", 7, List(
    Vibhag(3, VibhagMarker.Khali),
    Vibhag(2, VibhagMarker.Taali(2)),
    Vibhag(2, VibhagMarker.Taali(3))
  ), None)

  val yaman = Raag("Yaman", None, None, None, None, None, None, None)

  val swarKeys = List('s', 'r', 'g', 'm', 'p', 'd', 'n')

  // --- Helpers ---

  /** Insert n swar notes cycling through Sa Re Ga Ma Pa Dha Ni, all shuddha madhya. */
  def insertNSwar(editor: CompositionEditor, n: Int): CompositionEditor =
    (0 until n).foldLeft(editor) { (ed, i) =>
      val key = swarKeys(i % 7)
      val (newEd, msg) = KeyHandler.handleSwarKey(ed, key, shiftDown = false)
      msg should startWith ("✓")
      newEd
    }

  /** Insert n notes with mixed variants: shuddha, komal, tivra. */
  def insertMixedVariants(editor: CompositionEditor, n: Int): CompositionEditor =
    (0 until n).foldLeft(editor) { (ed, i) =>
      val key = swarKeys(i % 7)
      val shift = (i % 3) == 1
      val (newEd, _) = KeyHandler.handleSwarKey(ed, key, shiftDown = shift)
      newEd
    }

  /** Insert notes with octave changes every few notes. */
  def insertWithOctaveChanges(editor: CompositionEditor, n: Int): CompositionEditor =
    (0 until n).foldLeft(editor) { (ed, i) =>
      val octaveEd = (i % 12) match
        case 0 | 1 | 2 | 3 =>
          val (e, _) = KeyHandler.handleOctaveKey(ed, "PERIOD")
          e
        case 8 | 9 | 10 | 11 =>
          val (e, _) = KeyHandler.handleOctaveKey(ed, "QUOTE")
          e
        case _ =>
          val (e, _) = KeyHandler.handleOctaveKey(ed, "BACKTICK")
          e
      val key = swarKeys(i % 7)
      val (newEd, _) = KeyHandler.handleSwarKey(octaveEd, key, shiftDown = false)
      newEd
    }

  /** Insert notes interspersed with rests and sustains. */
  def insertWithRestsAndSustains(editor: CompositionEditor, n: Int): CompositionEditor =
    (0 until n).foldLeft(editor) { (ed, i) =>
      i % 5 match
        case 3 =>
          val (newEd, _) = KeyHandler.handleSpecialKey(ed, "SPACE")
          newEd
        case 4 =>
          val (newEd, _) = KeyHandler.handleSpecialKey(ed, "MINUS")
          newEd
        case _ =>
          val key = swarKeys(i % 7)
          val (newEd, _) = KeyHandler.handleSwarKey(ed, key, shiftDown = false)
          newEd
    }

  /** Switch to a section by index, returning new editor. */
  def switchSection(editor: CompositionEditor, idx: Int): CompositionEditor =
    val taal = editor.composition.metadata.taal
    editor.copy(currentSectionIndex = idx, cursor = CursorModel(taal))

  // =====================================================================
  // BASIC SWAR INPUT — scale up from 10 to 200
  // =====================================================================

  "Basic swar input" should "handle 10 notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = insertNSwar(editor, 10)
    result.currentSection.events should have length 10
  }

  it should "handle 50 notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = insertNSwar(editor, 50)
    result.currentSection.events should have length 50
  }

  it should "handle 100 notes (Gat-sized)" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = insertNSwar(editor, 100)
    result.currentSection.events should have length 100
  }

  it should "handle 200 notes (Taan-sized)" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = insertNSwar(editor, 200)
    result.currentSection.events should have length 200
  }

  it should "handle 500 notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = insertNSwar(editor, 500)
    result.currentSection.events should have length 500
  }

  it should "handle 1000 notes (Palta-sized)" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = insertNSwar(editor, 1000)
    result.currentSection.events should have length 1000
  }

  // =====================================================================
  // ALL 7 SWAR KEYS — verify each note individually
  // =====================================================================

  "All swar keys" should "produce correct notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val expectedNotes = List(Note.Sa, Note.Re, Note.Ga, Note.Ma, Note.Pa, Note.Dha, Note.Ni)
    val result = swarKeys.zip(expectedNotes).foldLeft(editor) { case (ed, (key, expectedNote)) =>
      val (newEd, msg) = KeyHandler.handleSwarKey(ed, key, shiftDown = false)
      val lastEvent = newEd.currentSection.events.last.asInstanceOf[Event.Swar]
      lastEvent.note shouldBe expectedNote
      lastEvent.variant shouldBe Variant.Shuddha
      msg should include (expectedNote.toString)
      newEd
    }
    result.currentSection.events should have length 7
  }

  // =====================================================================
  // KOMAL / TIVRA VARIANTS
  // =====================================================================

  "Komal/tivra variants" should "produce correct variants for all shifted keys" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val expectations = List(
      ('s', Variant.Shuddha), // Sa has no komal
      ('r', Variant.Komal),
      ('g', Variant.Komal),
      ('m', Variant.Tivra),
      ('p', Variant.Shuddha), // Pa has no komal
      ('d', Variant.Komal),
      ('n', Variant.Komal)
    )
    expectations.foldLeft(editor) { case (ed, (key, expectedVariant)) =>
      val (newEd, _) = KeyHandler.handleSwarKey(ed, key, shiftDown = true)
      val lastEvent = newEd.currentSection.events.last.asInstanceOf[Event.Swar]
      lastEvent.variant shouldBe expectedVariant
      newEd
    }
  }

  it should "handle 100 mixed variant notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = insertMixedVariants(editor, 100)
    result.currentSection.events should have length 100
    val swars = result.currentSection.events.collect { case s: Event.Swar => s }
    swars.map(_.variant).toSet should contain allOf (Variant.Shuddha, Variant.Komal)
  }

  // =====================================================================
  // OCTAVE CHANGES
  // =====================================================================

  "Octave changes" should "cycle through mandra/madhya/taar for 100 notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = insertWithOctaveChanges(editor, 100)
    result.currentSection.events should have length 100
    val octaves = result.currentSection.events.collect { case s: Event.Swar => s.octave }.toSet
    octaves should contain (Octave.Mandra)
    octaves should contain (Octave.Madhya)
    octaves should contain (Octave.Taar)
  }

  it should "reset octave to Madhya after each swar insertion" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withMandra, _) = KeyHandler.handleOctaveKey(editor, "PERIOD")
    withMandra.cursor.currentOctave shouldBe Octave.Mandra
    val (afterNote, _) = KeyHandler.handleSwarKey(withMandra, 's', shiftDown = false)
    afterNote.cursor.currentOctave shouldBe Octave.Madhya
  }

  it should "handle 300 notes with octave changes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = insertWithOctaveChanges(editor, 300)
    result.currentSection.events should have length 300
  }

  // =====================================================================
  // REST AND SUSTAIN
  // =====================================================================

  "Rest and sustain" should "intersperse correctly with swar over 100 events" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = insertWithRestsAndSustains(editor, 100)
    result.currentSection.events should have length 100
    val rests = result.currentSection.events.count(_.isInstanceOf[Event.Rest])
    val sustains = result.currentSection.events.count(_.isInstanceOf[Event.Sustain])
    rests should be > 0
    sustains should be > 0
  }

  it should "handle 500 mixed events" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = insertWithRestsAndSustains(editor, 500)
    result.currentSection.events should have length 500
  }

  // =====================================================================
  // DUAL SWAR
  // =====================================================================

  "Dual swar" should "insert pairs of 2 notes per call" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (result, msg) = KeyHandler.handleDualSwar(editor, 's', shiftDown = false)
    result.currentSection.events should have length 2
    val e1 = result.currentSection.events(0).asInstanceOf[Event.Swar]
    val e2 = result.currentSection.events(1).asInstanceOf[Event.Swar]
    e1.note shouldBe Note.Sa
    e2.note shouldBe Note.Sa
    e1.duration shouldBe Rational(1, 2)
    e2.duration shouldBe Rational(1, 2)
  }

  it should "handle 50 dual swar insertions (100 events)" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = (0 until 50).foldLeft(editor) { (ed, i) =>
      val key = swarKeys(i % 7)
      val (newEd, _) = KeyHandler.handleDualSwar(ed, key, shiftDown = false)
      newEd
    }
    result.currentSection.events should have length 100
  }

  // =====================================================================
  // BACKSPACE — insert then delete
  // =====================================================================

  "Backspace" should "delete last event correctly at small scale" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val withNotes = insertNSwar(editor, 10)
    withNotes.currentSection.events should have length 10
    val afterDelete = (0 until 5).foldLeft(withNotes) { (ed, _) =>
      val (newEd, msg) = KeyHandler.handleSpecialKey(ed, "BACKSPACE")
      msg should include ("Deleted")
      newEd
    }
    afterDelete.currentSection.events should have length 5
  }

  it should "delete all events when deleting as many as were inserted" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val withNotes = insertNSwar(editor, 50)
    val afterDelete = (0 until 50).foldLeft(withNotes) { (ed, _) =>
      val (newEd, _) = KeyHandler.handleSpecialKey(ed, "BACKSPACE")
      newEd
    }
    afterDelete.currentSection.events shouldBe empty
  }

  it should "handle insert-delete-insert cycles at 200 scale" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    var ed = editor
    for cycle <- 0 until 10 do
      ed = insertNSwar(ed, 20)
      for _ <- 0 until 10 do
        val (newEd, _) = KeyHandler.handleSpecialKey(ed, "BACKSPACE")
        ed = newEd
    ed.currentSection.events should have length 100
  }

  it should "handle backspace on empty editor gracefully" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (result, msg) = KeyHandler.handleSpecialKey(editor, "BACKSPACE")
    msg should include ("Nothing")
    result.currentSection.events shouldBe empty
  }

  it should "handle 100 excess backspaces after emptying" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val withNotes = insertNSwar(editor, 10)
    val result = (0 until 110).foldLeft(withNotes) { (ed, _) =>
      val (newEd, _) = KeyHandler.handleSpecialKey(ed, "BACKSPACE")
      newEd
    }
    result.currentSection.events shouldBe empty
  }

  // =====================================================================
  // SUBDIVISIONS
  // =====================================================================

  "Subdivisions" should "set subdivision count and produce correct durations" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    for subdiv <- 2 to 8 do
      val subdivEd = KeyHandler.handleSubdivision(editor, subdiv)
      subdivEd.cursor.totalSubdivisions shouldBe subdiv
      val (withNote, _) = KeyHandler.handleSwarKey(subdivEd, 's', shiftDown = false)
      val swar = withNote.currentSection.events.last.asInstanceOf[Event.Swar]
      swar.duration shouldBe Rational(1, subdiv)
  }

  it should "handle 100 notes at subdivision 4 (re-set each beat)" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = (0 until 100).foldLeft(editor) { (ed, i) =>
      val subdivEd = KeyHandler.handleSubdivision(ed, 4)
      val key = swarKeys(i % 7)
      val (newEd, _) = KeyHandler.handleSwarKey(subdivEd, key, shiftDown = false)
      newEd
    }
    result.currentSection.events should have length 100
    result.currentSection.events.foreach {
      case s: Event.Swar => s.duration shouldBe Rational(1, 4)
      case _ =>
    }
  }

  it should "handle mixed subdivisions over 200 notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = (0 until 200).foldLeft(editor) { (ed, i) =>
      val subdiv = (i % 7) + 2 // 2 through 8
      val subdivEd = KeyHandler.handleSubdivision(ed, subdiv)
      val key = swarKeys(i % 7)
      val (newEd, _) = KeyHandler.handleSwarKey(subdivEd, key, shiftDown = false)
      newEd
    }
    result.currentSection.events should have length 200
  }

  // =====================================================================
  // STROKES
  // =====================================================================

  "Strokes" should "attach Da and Ra to swar notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (withDa, msgDa) = KeyHandler.handleStroke(withNote, Stroke.Da)
    withDa.currentSection.events.last.asInstanceOf[Event.Swar].stroke shouldBe Some(Stroke.Da)
    msgDa should include ("Da")

    val (withNote2, _) = KeyHandler.handleSwarKey(withDa, 'r', shiftDown = false)
    val (withRa, msgRa) = KeyHandler.handleStroke(withNote2, Stroke.Ra)
    withRa.currentSection.events.last.asInstanceOf[Event.Swar].stroke shouldBe Some(Stroke.Ra)
    msgRa should include ("Ra")
  }

  it should "attach Chikari and Jod strokes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (withChikari, _) = KeyHandler.handleStroke(withNote, Stroke.Chikari)
    withChikari.currentSection.events.last.asInstanceOf[Event.Swar].stroke shouldBe Some(Stroke.Chikari)

    val (withNote2, _) = KeyHandler.handleSwarKey(withChikari, 'r', shiftDown = false)
    val (withJod, _) = KeyHandler.handleStroke(withNote2, Stroke.Jod)
    withJod.currentSection.events.last.asInstanceOf[Event.Swar].stroke shouldBe Some(Stroke.Jod)
  }

  it should "handle alternating Da/Ra for 100 notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = (0 until 100).foldLeft(editor) { (ed, i) =>
      val key = swarKeys(i % 7)
      val (withNote, _) = KeyHandler.handleSwarKey(ed, key, shiftDown = false)
      val stroke = if i % 2 == 0 then Stroke.Da else Stroke.Ra
      val (withStroke, _) = KeyHandler.handleStroke(withNote, stroke)
      withStroke
    }
    result.currentSection.events should have length 100
    result.currentSection.events.foreach {
      case s: Event.Swar => s.stroke shouldBe defined
      case _ =>
    }
  }

  it should "fail gracefully when no swar to attach to" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (result, msg) = KeyHandler.handleStroke(editor, Stroke.Da)
    msg should include ("No swar")
    result shouldBe editor
  }

  // =====================================================================
  // SIMPLE ORNAMENTS — Gamak, Andolan, Gitkari
  // =====================================================================

  "Simple ornaments" should "attach Gamak to last swar" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (result, msg) = KeyHandler.handleSimpleOrnament(withNote, Gamak(), "Gamak")
    result.currentSection.events.last.asInstanceOf[Event.Swar].ornaments should contain (Gamak())
    msg should include ("Gamak")
  }

  it should "attach Andolan to last swar" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 'm', shiftDown = false)
    val (result, msg) = KeyHandler.handleSimpleOrnament(withNote, Andolan(), "Andolan")
    result.currentSection.events.last.asInstanceOf[Event.Swar].ornaments should contain (Andolan())
    msg should include ("Andolan")
  }

  it should "attach Gitkari to last swar" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 'g', shiftDown = false)
    val (result, msg) = KeyHandler.handleSimpleOrnament(withNote, Gitkari(), "Gitkari")
    result.currentSection.events.last.asInstanceOf[Event.Swar].ornaments should contain (Gitkari())
    msg should include ("Gitkari")
  }

  it should "stack multiple ornaments on the same swar" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (with1, _) = KeyHandler.handleSimpleOrnament(withNote, Gamak(), "Gamak")
    val (with2, _) = KeyHandler.handleSimpleOrnament(with1, Andolan(), "Andolan")
    val (with3, _) = KeyHandler.handleSimpleOrnament(with2, Gitkari(), "Gitkari")
    val ornaments = with3.currentSection.events.last.asInstanceOf[Event.Swar].ornaments
    ornaments should have length 3
    ornaments should contain (Gamak())
    ornaments should contain (Andolan())
    ornaments should contain (Gitkari())
  }

  it should "handle Gamak on every note for 100 notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = (0 until 100).foldLeft(editor) { (ed, i) =>
      val key = swarKeys(i % 7)
      val (withNote, _) = KeyHandler.handleSwarKey(ed, key, shiftDown = false)
      val (withGamak, _) = KeyHandler.handleSimpleOrnament(withNote, Gamak(), "Gamak")
      withGamak
    }
    result.currentSection.events should have length 100
    result.currentSection.events.foreach {
      case s: Event.Swar => s.ornaments should contain (Gamak())
      case _ =>
    }
  }

  // =====================================================================
  // NOTE ORNAMENTS — KanSwar, Sparsh, Ghaseet
  // =====================================================================

  "KanSwar ornament" should "attach grace note to last swar" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (result, msg, nextMode) = KeyHandler.handleNoteOrnament(withNote, 'r', shiftDown = false, OrnamentMode.KanSwar)
    msg should include ("Kan swar")
    nextMode shouldBe None
    val ornaments = result.currentSection.events.last.asInstanceOf[Event.Swar].ornaments
    ornaments should have length 1
    ornaments.head shouldBe a[KanSwar]
    ornaments.head.asInstanceOf[KanSwar].graceNote.note shouldBe Note.Re
  }

  "Sparsh ornament" should "attach touch note to last swar" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 'm', shiftDown = false)
    val (result, msg, nextMode) = KeyHandler.handleNoteOrnament(withNote, 'p', shiftDown = false, OrnamentMode.Sparsh)
    msg should include ("Sparsh")
    nextMode shouldBe None
    result.currentSection.events.last.asInstanceOf[Event.Swar].ornaments.head shouldBe a[Sparsh]
  }

  "Ghaseet ornament" should "attach target note to last swar" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 'g', shiftDown = false)
    val (result, msg, nextMode) = KeyHandler.handleNoteOrnament(withNote, 'm', shiftDown = false, OrnamentMode.Ghaseet)
    msg should include ("Ghaseet")
    nextMode shouldBe None
    result.currentSection.events.last.asInstanceOf[Event.Swar].ornaments.head shouldBe a[Ghaseet]
  }

  "Note ornaments at scale" should "handle KanSwar on every note for 100 notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = (0 until 100).foldLeft(editor) { (ed, i) =>
      val key = swarKeys(i % 7)
      val graceKey = swarKeys((i + 1) % 7)
      val (withNote, _) = KeyHandler.handleSwarKey(ed, key, shiftDown = false)
      val (withKan, _, _) = KeyHandler.handleNoteOrnament(withNote, graceKey, shiftDown = false, OrnamentMode.KanSwar)
      withKan
    }
    result.currentSection.events should have length 100
    result.currentSection.events.foreach {
      case s: Event.Swar =>
        s.ornaments should have length 1
        s.ornaments.head shouldBe a[KanSwar]
      case _ =>
    }
  }

  // =====================================================================
  // MEEND — 2-note ornament with direction
  // =====================================================================

  "Meend ornament" should "complete ascending meend in 2 steps" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (ed1, msg1, mode1) = KeyHandler.handleNoteOrnament(withNote, 'r', shiftDown = false,
      OrnamentMode.MeendStart(MeendDirection.Ascending))
    msg1 should include ("Meend")
    mode1 shouldBe defined
    mode1.get shouldBe a[OrnamentMode.MeendEnd]
    val (ed2, msg2, mode2) = KeyHandler.handleNoteOrnament(ed1, 'g', shiftDown = false, mode1.get)
    msg2 should include ("Meend")
    mode2 shouldBe None
    val ornaments = ed2.currentSection.events.last.asInstanceOf[Event.Swar].ornaments
    ornaments should have length 1
    ornaments.head shouldBe a[Meend]
    val meend = ornaments.head.asInstanceOf[Meend]
    meend.startNote.note shouldBe Note.Re
    meend.endNote.note shouldBe Note.Ga
    meend.direction shouldBe MeendDirection.Ascending
  }

  it should "complete descending meend" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 'p', shiftDown = false)
    val (ed1, _, mode1) = KeyHandler.handleNoteOrnament(withNote, 'g', shiftDown = false,
      OrnamentMode.MeendStart(MeendDirection.Descending))
    mode1 shouldBe defined
    val (ed2, _, mode2) = KeyHandler.handleNoteOrnament(ed1, 'r', shiftDown = false, mode1.get)
    mode2 shouldBe None
    val meend = ed2.currentSection.events.last.asInstanceOf[Event.Swar].ornaments.head.asInstanceOf[Meend]
    meend.direction shouldBe MeendDirection.Descending
  }

  it should "handle meend on 50 notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = (0 until 50).foldLeft(editor) { (ed, i) =>
      val key = swarKeys(i % 7)
      val startKey = swarKeys((i + 1) % 7)
      val endKey = swarKeys((i + 2) % 7)
      val dir = if i % 2 == 0 then MeendDirection.Ascending else MeendDirection.Descending
      val (withNote, _) = KeyHandler.handleSwarKey(ed, key, shiftDown = false)
      val (ed1, _, mode1) = KeyHandler.handleNoteOrnament(withNote, startKey, shiftDown = false,
        OrnamentMode.MeendStart(dir))
      mode1 shouldBe defined
      val (ed2, _, mode2) = KeyHandler.handleNoteOrnament(ed1, endKey, shiftDown = false, mode1.get)
      mode2 shouldBe None
      ed2
    }
    result.currentSection.events should have length 50
    result.currentSection.events.foreach {
      case s: Event.Swar =>
        s.ornaments should have length 1
        s.ornaments.head shouldBe a[Meend]
      case _ =>
    }
  }

  // =====================================================================
  // KRINTAN — 2-note ornament
  // =====================================================================

  "Krintan ornament" should "complete in 2 steps" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 'g', shiftDown = false)
    val (ed1, _, mode1) = KeyHandler.handleNoteOrnament(withNote, 'r', shiftDown = false,
      OrnamentMode.KrintanStart)
    mode1 shouldBe defined
    mode1.get shouldBe a[OrnamentMode.KrintanEnd]
    val (ed2, msg2, mode2) = KeyHandler.handleNoteOrnament(ed1, 's', shiftDown = false, mode1.get)
    msg2 should include ("Krintan")
    mode2 shouldBe None
    val ornaments = ed2.currentSection.events.last.asInstanceOf[Event.Swar].ornaments
    ornaments.head shouldBe a[Krintan]
    ornaments.head.asInstanceOf[Krintan].notes should have length 2
  }

  // =====================================================================
  // MURKI — multi-note collect then finish
  // =====================================================================

  "Murki ornament" should "collect notes then finish" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val mode0 = OrnamentMode.MurkiCollect(Nil)
    val (ed1, _, mode1) = KeyHandler.handleNoteOrnament(withNote, 'r', shiftDown = false, mode0)
    mode1 shouldBe defined
    val (ed2, _, mode2) = KeyHandler.handleNoteOrnament(ed1, 'g', shiftDown = false, mode1.get)
    mode2 shouldBe defined
    val (ed3, _, mode3) = KeyHandler.handleNoteOrnament(ed2, 'r', shiftDown = false, mode2.get)
    mode3 shouldBe defined
    val (result, msg) = KeyHandler.finishMultiNoteOrnament(ed3, mode3.get)
    msg should include ("Murki")
    val ornaments = result.currentSection.events.last.asInstanceOf[Event.Swar].ornaments
    ornaments.head shouldBe a[Murki]
    ornaments.head.asInstanceOf[Murki].notes should have length 3
  }

  it should "handle empty note list" in {
    val mode = OrnamentMode.MurkiCollect(Nil)
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (_, msg) = KeyHandler.finishMultiNoteOrnament(editor, mode)
    msg should include ("No notes")
  }

  // =====================================================================
  // ZAMZAMA — multi-note collect then finish
  // =====================================================================

  "Zamzama ornament" should "collect notes then finish" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 'm', shiftDown = false)
    val mode0 = OrnamentMode.ZamzamaCollect(Nil)
    val (ed1, _, mode1) = KeyHandler.handleNoteOrnament(withNote, 'p', shiftDown = false, mode0)
    mode1 shouldBe defined
    val (ed2, _, mode2) = KeyHandler.handleNoteOrnament(ed1, 'd', shiftDown = false, mode1.get)
    mode2 shouldBe defined
    val (ed3, _, mode3) = KeyHandler.handleNoteOrnament(ed2, 'n', shiftDown = false, mode2.get)
    mode3 shouldBe defined
    val (ed4, _, mode4) = KeyHandler.handleNoteOrnament(ed3, 's', shiftDown = false, mode3.get)
    mode4 shouldBe defined
    val (result, msg) = KeyHandler.finishMultiNoteOrnament(ed4, mode4.get)
    msg should include ("Zamzama")
    val ornaments = result.currentSection.events.last.asInstanceOf[Event.Swar].ornaments
    ornaments.head shouldBe a[Zamzama]
    ornaments.head.asInstanceOf[Zamzama].notes should have length 4
  }

  // =====================================================================
  // FULL ORNAMENT COVERAGE — every ornament type on one composition
  // =====================================================================

  "All ornament types" should "be attachable to swar notes in a single composition" in {
    val editor = CompositionEditor.empty(teentaal, yaman)

    // Note 1: Gamak
    val (e1, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (e2, _) = KeyHandler.handleSimpleOrnament(e1, Gamak(), "Gamak")

    // Note 2: Andolan
    val (e3, _) = KeyHandler.handleSwarKey(e2, 'r', shiftDown = false)
    val (e4, _) = KeyHandler.handleSimpleOrnament(e3, Andolan(), "Andolan")

    // Note 3: Gitkari
    val (e5, _) = KeyHandler.handleSwarKey(e4, 'g', shiftDown = false)
    val (e6, _) = KeyHandler.handleSimpleOrnament(e5, Gitkari(), "Gitkari")

    // Note 4: KanSwar
    val (e7, _) = KeyHandler.handleSwarKey(e6, 'm', shiftDown = false)
    val (e8, _, _) = KeyHandler.handleNoteOrnament(e7, 'p', shiftDown = false, OrnamentMode.KanSwar)

    // Note 5: Sparsh
    val (e9, _) = KeyHandler.handleSwarKey(e8, 'p', shiftDown = false)
    val (e10, _, _) = KeyHandler.handleNoteOrnament(e9, 'd', shiftDown = false, OrnamentMode.Sparsh)

    // Note 6: Ghaseet
    val (e11, _) = KeyHandler.handleSwarKey(e10, 'd', shiftDown = false)
    val (e12, _, _) = KeyHandler.handleNoteOrnament(e11, 'n', shiftDown = false, OrnamentMode.Ghaseet)

    // Note 7: Ascending Meend
    val (e13, _) = KeyHandler.handleSwarKey(e12, 'n', shiftDown = false)
    val (e14, _, mode14) = KeyHandler.handleNoteOrnament(e13, 's', shiftDown = false,
      OrnamentMode.MeendStart(MeendDirection.Ascending))
    val (e15, _, _) = KeyHandler.handleNoteOrnament(e14, 'r', shiftDown = false, mode14.get)

    // Note 8: Descending Meend
    val (e16, _) = KeyHandler.handleSwarKey(e15, 's', shiftDown = false)
    val (e17, _, mode17) = KeyHandler.handleNoteOrnament(e16, 'p', shiftDown = false,
      OrnamentMode.MeendStart(MeendDirection.Descending))
    val (e18, _, _) = KeyHandler.handleNoteOrnament(e17, 'm', shiftDown = false, mode17.get)

    // Note 9: Krintan
    val (e19, _) = KeyHandler.handleSwarKey(e18, 'r', shiftDown = false)
    val (e20, _, mode20) = KeyHandler.handleNoteOrnament(e19, 'g', shiftDown = false,
      OrnamentMode.KrintanStart)
    val (e21, _, _) = KeyHandler.handleNoteOrnament(e20, 's', shiftDown = false, mode20.get)

    // Note 10: Murki
    val (e22, _) = KeyHandler.handleSwarKey(e21, 'g', shiftDown = false)
    val murkiMode0 = OrnamentMode.MurkiCollect(Nil)
    val (e23, _, mm1) = KeyHandler.handleNoteOrnament(e22, 'r', shiftDown = false, murkiMode0)
    val (e24, _, mm2) = KeyHandler.handleNoteOrnament(e23, 's', shiftDown = false, mm1.get)
    val (e25, _, mm3) = KeyHandler.handleNoteOrnament(e24, 'r', shiftDown = false, mm2.get)
    val (e26, _) = KeyHandler.finishMultiNoteOrnament(e25, mm3.get)

    // Note 11: Zamzama
    val (e27, _) = KeyHandler.handleSwarKey(e26, 'm', shiftDown = false)
    val zamMode0 = OrnamentMode.ZamzamaCollect(Nil)
    val (e28, _, zm1) = KeyHandler.handleNoteOrnament(e27, 'p', shiftDown = false, zamMode0)
    val (e29, _, zm2) = KeyHandler.handleNoteOrnament(e28, 'd', shiftDown = false, zm1.get)
    val (e30, _) = KeyHandler.finishMultiNoteOrnament(e29, zm2.get)

    val finalEvents = e30.currentSection.events
    finalEvents should have length 11

    val ornamentTypes = finalEvents.collect { case s: Event.Swar => s.ornaments.head.getClass.getSimpleName }
    ornamentTypes should contain ("Gamak")
    ornamentTypes should contain ("Andolan")
    ornamentTypes should contain ("Gitkari")
    ornamentTypes should contain ("KanSwar")
    ornamentTypes should contain ("Sparsh")
    ornamentTypes should contain ("Ghaseet")
    ornamentTypes should contain ("Meend")
    ornamentTypes should contain ("Krintan")
    ornamentTypes should contain ("Murki")
    ornamentTypes should contain ("Zamzama")
  }

  // =====================================================================
  // SECTION SWITCHING — Gat composition with Taans
  // =====================================================================

  "Section switching" should "populate Gat and Antara sections independently" in {
    val editor = CompositionEditor.create(
      title = "Test Gat",
      compositionType = CompositionType.Gat,
      taal = teentaal,
      raag = yaman,
      laya = Some(Laya.Vilambit)
    )
    editor.composition.sections should have length 2

    val gatFilled = insertNSwar(editor, 100)
    gatFilled.currentSection.events should have length 100
    gatFilled.currentSectionIndex shouldBe 0

    val antaraEd = switchSection(gatFilled, 1)
    antaraEd.currentSectionIndex shouldBe 1
    antaraEd.currentSection.events shouldBe empty

    val antaraFilled = insertNSwar(antaraEd, 100)
    antaraFilled.currentSection.events should have length 100
    antaraFilled.composition.sections(0).events should have length 100
  }

  it should "populate Gat with 3 Taans" in {
    val editor = CompositionEditor.create(
      title = "Gat with Taans",
      compositionType = CompositionType.Gat,
      taal = teentaal,
      raag = yaman,
      laya = Some(Laya.Drut),
      taanCount = 3
    )
    editor.composition.sections should have length 5

    val gatFilled = insertNSwar(editor, 100)
    val antaraEd = switchSection(gatFilled, 1)
    val antaraFilled = insertNSwar(antaraEd, 100)

    val taan1Ed = switchSection(antaraFilled, 2)
    taan1Ed.currentSection.name shouldBe "Taan 1"
    val taan1Filled = insertNSwar(taan1Ed, 200)
    taan1Filled.currentSection.events should have length 200

    val taan2Ed = switchSection(taan1Filled, 3)
    taan2Ed.currentSection.name shouldBe "Taan 2"
    val taan2Filled = insertNSwar(taan2Ed, 200)

    val taan3Ed = switchSection(taan2Filled, 4)
    taan3Ed.currentSection.name shouldBe "Taan 3"
    val taan3Filled = insertNSwar(taan3Ed, 200)

    taan3Filled.composition.sections(0).events should have length 100
    taan3Filled.composition.sections(1).events should have length 100
    taan3Filled.composition.sections(2).events should have length 200
    taan3Filled.composition.sections(3).events should have length 200
    taan3Filled.composition.sections(4).events should have length 200

    val totalEvents = taan3Filled.composition.sections.map(_.events.length).sum
    totalEvents shouldBe 800
  }

  // =====================================================================
  // PALTA COMPOSITION — up to 1000 swar
  // =====================================================================

  "Palta composition" should "handle up to 500 swar in a single section" in {
    val editor = CompositionEditor.create(
      title = "Palta Practice",
      compositionType = CompositionType.Palta,
      taal = teentaal,
      raag = yaman,
      laya = None
    )
    editor.composition.sections should have length 1
    editor.currentSection.sectionType shouldBe SectionType.Palta

    val result = insertNSwar(editor, 500)
    result.currentSection.events should have length 500
  }

  it should "handle 1000 swar" in {
    val editor = CompositionEditor.create(
      title = "Long Palta",
      compositionType = CompositionType.Palta,
      taal = teentaal,
      raag = yaman,
      laya = None
    )
    val result = insertNSwar(editor, 1000)
    result.currentSection.events should have length 1000
  }

  // =====================================================================
  // BANDISH COMPOSITION
  // =====================================================================

  "Bandish composition" should "handle 500 swar in Sthayi" in {
    val editor = CompositionEditor.create(
      title = "Test Bandish",
      compositionType = CompositionType.Bandish,
      taal = teentaal,
      raag = yaman,
      laya = Some(Laya.Madhya)
    )
    editor.currentSection.sectionType shouldBe SectionType.Sthayi
    val result = insertNSwar(editor, 500)
    result.currentSection.events should have length 500
  }

  // =====================================================================
  // DIFFERENT TAALS
  // =====================================================================

  "Different taals" should "handle 100 swar in Jhaptaal (10 matras)" in {
    val editor = CompositionEditor.empty(jhaptaal, yaman)
    val result = insertNSwar(editor, 100)
    result.currentSection.events should have length 100
  }

  it should "handle 100 swar in Rupak (7 matras)" in {
    val editor = CompositionEditor.empty(rupak, yaman)
    val result = insertNSwar(editor, 100)
    result.currentSection.events should have length 100
  }

  // =====================================================================
  // COMBINED STRESS — ornaments + strokes + octaves + subdivisions
  // =====================================================================

  "Combined stress test" should "handle 200 fully-decorated notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = (0 until 200).foldLeft(editor) { (ed, i) =>
      // Set subdivision
      val subdiv = (i % 4) + 1
      val subdivEd = KeyHandler.handleSubdivision(ed, subdiv)

      // Set octave
      val octaveEd = (i % 6) match
        case 0 | 1 =>
          val (e, _) = KeyHandler.handleOctaveKey(subdivEd, "PERIOD")
          e
        case 4 | 5 =>
          val (e, _) = KeyHandler.handleOctaveKey(subdivEd, "QUOTE")
          e
        case _ =>
          val (e, _) = KeyHandler.handleOctaveKey(subdivEd, "BACKTICK")
          e

      // Insert note with variant
      val key = swarKeys(i % 7)
      val shift = (i % 5) == 2
      val (withNote, _) = KeyHandler.handleSwarKey(octaveEd, key, shiftDown = shift)

      // Add stroke
      val stroke = if i % 2 == 0 then Stroke.Da else Stroke.Ra
      val (withStroke, _) = KeyHandler.handleStroke(withNote, stroke)

      // Add ornament
      val withOrnament = (i % 10) match
        case 0 =>
          val (e, _) = KeyHandler.handleSimpleOrnament(withStroke, Gamak(), "Gamak")
          e
        case 1 =>
          val (e, _) = KeyHandler.handleSimpleOrnament(withStroke, Andolan(), "Andolan")
          e
        case 2 =>
          val (e, _) = KeyHandler.handleSimpleOrnament(withStroke, Gitkari(), "Gitkari")
          e
        case 3 =>
          val (e, _, _) = KeyHandler.handleNoteOrnament(withStroke, 'r', shiftDown = false, OrnamentMode.KanSwar)
          e
        case 4 =>
          val (e, _, _) = KeyHandler.handleNoteOrnament(withStroke, 'g', shiftDown = false, OrnamentMode.Sparsh)
          e
        case 5 =>
          val (e, _, _) = KeyHandler.handleNoteOrnament(withStroke, 'm', shiftDown = false, OrnamentMode.Ghaseet)
          e
        case 6 =>
          val (e1, _, m1) = KeyHandler.handleNoteOrnament(withStroke, 's', shiftDown = false,
            OrnamentMode.MeendStart(MeendDirection.Ascending))
          val (e2, _, _) = KeyHandler.handleNoteOrnament(e1, 'p', shiftDown = false, m1.get)
          e2
        case 7 =>
          val (e1, _, m1) = KeyHandler.handleNoteOrnament(withStroke, 'n', shiftDown = false,
            OrnamentMode.KrintanStart)
          val (e2, _, _) = KeyHandler.handleNoteOrnament(e1, 'd', shiftDown = false, m1.get)
          e2
        case _ => withStroke

      withOrnament
    }
    result.currentSection.events should have length 200
    val swars = result.currentSection.events.collect { case s: Event.Swar => s }
    swars should have length 200
    swars.count(_.stroke.isDefined) shouldBe 200
    swars.count(_.ornaments.nonEmpty) should be >= 160  // 80% have ornaments (all except cases 8,9)
  }

  // =====================================================================
  // FULL MULTI-SECTION STRESS — realistic Gat composition
  // =====================================================================

  "Full Gat composition stress" should "fill Gat(100) + Antara(100) + 3 Taans(200 each) with mixed input" in {
    val editor = CompositionEditor.create(
      title = "Stress Test Gat",
      compositionType = CompositionType.Gat,
      taal = teentaal,
      raag = yaman,
      laya = Some(Laya.Vilambit),
      taanCount = 3
    )

    // Gat section: 100 notes with octave changes and strokes
    var ed = editor
    ed = (0 until 100).foldLeft(ed) { (e, i) =>
      val octE = if i % 4 == 0 then
        val (o, _) = KeyHandler.handleOctaveKey(e, "PERIOD")
        o
      else if i % 4 == 3 then
        val (o, _) = KeyHandler.handleOctaveKey(e, "QUOTE")
        o
      else e
      val key = swarKeys(i % 7)
      val (withNote, _) = KeyHandler.handleSwarKey(octE, key, shiftDown = i % 5 == 1)
      val stroke = if i % 2 == 0 then Stroke.Da else Stroke.Ra
      val (withStroke, _) = KeyHandler.handleStroke(withNote, stroke)
      withStroke
    }
    ed.currentSection.events should have length 100

    // Antara section: 100 notes with ornaments
    ed = switchSection(ed, 1)
    ed = (0 until 100).foldLeft(ed) { (e, i) =>
      val key = swarKeys(i % 7)
      val (withNote, _) = KeyHandler.handleSwarKey(e, key, shiftDown = false)
      val withOrn = if i % 3 == 0 then
        val (o, _) = KeyHandler.handleSimpleOrnament(withNote, Gamak(), "Gamak")
        o
      else withNote
      withOrn
    }
    ed.currentSection.events should have length 100

    // Taan 1: 200 notes, fast, plain swar
    ed = switchSection(ed, 2)
    ed = insertNSwar(ed, 200)
    ed.currentSection.events should have length 200

    // Taan 2: 200 notes with subdivisions
    ed = switchSection(ed, 3)
    ed = (0 until 200).foldLeft(ed) { (e, i) =>
      val subdivEd = KeyHandler.handleSubdivision(e, (i % 4) + 1)
      val key = swarKeys(i % 7)
      val (newEd, _) = KeyHandler.handleSwarKey(subdivEd, key, shiftDown = false)
      newEd
    }
    ed.currentSection.events should have length 200

    // Taan 3: 200 notes with rests and sustains mixed in
    ed = switchSection(ed, 4)
    ed = insertWithRestsAndSustains(ed, 200)
    ed.currentSection.events should have length 200

    // Verify totals
    val sections = ed.composition.sections
    sections should have length 5
    val sectionCounts = sections.map(_.events.length)
    sectionCounts shouldBe List(100, 100, 200, 200, 200)
    sectionCounts.sum shouldBe 800
  }

  // =====================================================================
  // UNDO HISTORY INTEGRATION
  // =====================================================================

  "Undo history" should "track 100 edits without overflow" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    var hist = UndoHistory(editor, maxSize = 50)
    for i <- 0 until 100 do
      val key = swarKeys(i % 7)
      val (newEditor, _) = KeyHandler.handleSwarKey(hist.present, key, shiftDown = false)
      hist = hist.push(newEditor)
    hist.present.currentSection.events should have length 100
    hist.past.size shouldBe 50 // maxSize clamp
    hist.future.size shouldBe 0
  }

  it should "support undo/redo cycles" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    var hist = UndoHistory(editor, maxSize = 50)
    for i <- 0 until 20 do
      val key = swarKeys(i % 7)
      val (newEd, _) = KeyHandler.handleSwarKey(hist.present, key, shiftDown = false)
      hist = hist.push(newEd)
    hist.present.currentSection.events should have length 20

    // Undo 10
    for _ <- 0 until 10 do
      hist.undo match
        case Some(newHist) => hist = newHist
        case None => fail("Expected undo to succeed")
    hist.present.currentSection.events should have length 10
    hist.future.size shouldBe 10

    // Redo 5
    for _ <- 0 until 5 do
      hist.redo match
        case Some(newHist) => hist = newHist
        case None => fail("Expected redo to succeed")
    hist.present.currentSection.events should have length 15
  }

  // =====================================================================
  // CURSOR POSITION TRACKING
  // =====================================================================

  "Cursor tracking" should "advance beat correctly over 100 notes" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val result = insertNSwar(editor, 100)
    // After 100 notes in Teentaal (16 matras), cursor wraps:
    // beat = 100 % 16 = 4, cycle = 100 / 16 = 6
    result.cursor.beat shouldBe 4
    result.cursor.cycle shouldBe 6
  }

  it should "track cursor in Jhaptaal (10 matras) over 50 notes" in {
    val editor = CompositionEditor.empty(jhaptaal, yaman)
    val result = insertNSwar(editor, 50)
    result.cursor.beat shouldBe 0
    result.cursor.cycle shouldBe 5
  }

  it should "track cursor in Rupak (7 matras) over 35 notes" in {
    val editor = CompositionEditor.empty(rupak, yaman)
    val result = insertNSwar(editor, 35)
    result.cursor.beat shouldBe 0
    result.cursor.cycle shouldBe 5
  }

  // =====================================================================
  // SERIALIZATION ROUND-TRIP — ensure large compositions serialize
  // =====================================================================

  "Serialization" should "round-trip a 500-event composition" in {
    import com.varpas.sangeet.core.api.CompositionApi
    val editor = CompositionEditor.empty(teentaal, yaman)
    val filled = insertNSwar(editor, 500)
    val json = CompositionApi.serializeCompositionString(filled.composition)
    json should not be empty
    json should include ("Teentaal")
    json should include ("Yaman")
  }

  it should "round-trip an 800-event multi-section composition" in {
    import com.varpas.sangeet.core.api.CompositionApi
    val editor = CompositionEditor.create(
      title = "Serialization Test",
      compositionType = CompositionType.Gat,
      taal = teentaal,
      raag = yaman,
      laya = Some(Laya.Vilambit),
      taanCount = 3
    )
    var ed = insertNSwar(editor, 100)
    ed = insertNSwar(switchSection(ed, 1), 100)
    ed = insertNSwar(switchSection(ed, 2), 200)
    ed = insertNSwar(switchSection(ed, 3), 200)
    ed = insertNSwar(switchSection(ed, 4), 200)
    val json = CompositionApi.serializeCompositionString(ed.composition)
    json should not be empty
    json.length should be > 10000
  }

  // =====================================================================
  // EDGE CASES
  // =====================================================================

  "Edge cases" should "handle unknown swar key gracefully" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (result, msg) = KeyHandler.handleSwarKey(editor, 'x', shiftDown = false)
    msg should include ("Unknown")
    result.currentSection.events shouldBe empty
  }

  it should "handle stroke on empty section" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (result, msg) = KeyHandler.handleStroke(editor, Stroke.Da)
    msg should include ("No swar")
  }

  it should "handle ornament on empty section" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (result, msg) = KeyHandler.handleSimpleOrnament(editor, Gamak(), "Gamak")
    msg should include ("No swar")
  }

  it should "handle KanSwar note ornament with invalid key" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    val (withNote, _) = KeyHandler.handleSwarKey(editor, 's', shiftDown = false)
    val (_, msg, nextMode) = KeyHandler.handleNoteOrnament(withNote, 'x', shiftDown = false, OrnamentMode.KanSwar)
    msg should include ("Invalid")
    nextMode shouldBe None
  }

  it should "handle rapid insert-delete at boundaries" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    var ed = editor
    for _ <- 0 until 100 do
      val (withNote, _) = KeyHandler.handleSwarKey(ed, 's', shiftDown = false)
      val (afterDelete, _) = KeyHandler.handleSpecialKey(withNote, "BACKSPACE")
      ed = afterDelete
    ed.currentSection.events shouldBe empty
  }

  it should "handle subdivision changes during input" in {
    val editor = CompositionEditor.empty(teentaal, yaman)
    var ed = editor
    for i <- 0 until 50 do
      ed = KeyHandler.handleSubdivision(ed, (i % 7) + 2)
      val (newEd, _) = KeyHandler.handleSwarKey(ed, swarKeys(i % 7), shiftDown = false)
      ed = newEd
    ed.currentSection.events should have length 50
  }
