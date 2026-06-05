package com.varpas.sangeet.core.api

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.core.editor.CursorModel
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.taal.Taals

class OrnamentApiTest extends AnyFunSuite with Matchers:

  val testComp = CompositionApi.createComposition(
    title = "Test",
    compositionType = CompositionType.Gat,
    taal = Taals.teentaal,
    raag = Raags.yaman,
    laya = Some(Laya.Vilambit)
  )

  def inputWithSwar: EditorInput =
    val input    = EditorInput(testComp, 0, CursorModel(Taals.teentaal))
    val withSwar = EditorApi.insertSwar(input, Note.Sa, Variant.Shuddha, Octave.Madhya).toOption.get
    input.copy(composition = withSwar.composition)

  test("addSimpleOrnament should add Gamak to last swar") {
    val input  = inputWithSwar
    val result = OrnamentApi.addSimpleOrnament(input, Gamak())

    result shouldBe a[Right[_, _]]
    val editorResult = result.toOption.get
    val event        = editorResult.composition.sections(0).events.head.asInstanceOf[Event.Swar]
    event.ornaments should have size 1
    event.ornaments.head shouldBe a[Gamak]
  }

  test("addSimpleOrnament with no swar should return error") {
    val input  = EditorInput(testComp, 0, CursorModel(Taals.teentaal))
    val result = OrnamentApi.addSimpleOrnament(input, Gamak())

    result shouldBe a[Left[_, _]]
    result.left.toOption.get shouldBe ApiError.NoSwarTarget
  }

  test("addSingleNoteOrnament should add KanSwar to last swar") {
    val input   = inputWithSwar
    val noteRef = NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya)
    val result  = OrnamentApi.addSingleNoteOrnament(input, KanSwar(noteRef))

    result shouldBe a[Right[_, _]]
    val editorResult = result.toOption.get
    val event        = editorResult.composition.sections(0).events.head.asInstanceOf[Event.Swar]
    event.ornaments should have size 1
    event.ornaments.head shouldBe a[KanSwar]
  }

  test("addMeend should add Meend ornament") {
    val input    = inputWithSwar
    val startRef = NoteRef(Note.Sa, Variant.Shuddha, Octave.Madhya)
    val endRef   = NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya)
    val result   = OrnamentApi.addMeend(input, startRef, endRef, MeendDirection.Ascending)

    result shouldBe a[Right[_, _]]
    val editorResult = result.toOption.get
    val event        = editorResult.composition.sections(0).events.head.asInstanceOf[Event.Swar]
    event.ornaments should have size 1
    event.ornaments.head shouldBe a[Meend]
  }

  test("addKrintan with sufficient notes should succeed") {
    val input = inputWithSwar
    val notes = List(
      NoteRef(Note.Sa, Variant.Shuddha, Octave.Madhya),
      NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya)
    )
    val result = OrnamentApi.addKrintan(input, notes)

    result shouldBe a[Right[_, _]]
    val editorResult = result.toOption.get
    val event        = editorResult.composition.sections(0).events.head.asInstanceOf[Event.Swar]
    event.ornaments should have size 1
    event.ornaments.head shouldBe a[Krintan]
  }

  test("addKrintan with insufficient notes should return error") {
    val input  = inputWithSwar
    val notes  = List(NoteRef(Note.Sa, Variant.Shuddha, Octave.Madhya))
    val result = OrnamentApi.addKrintan(input, notes)

    result shouldBe a[Left[_, _]]
    result.left.toOption.get shouldBe a[ApiError.InsufficientNotes]
  }

  test("addMurki should add Murki ornament") {
    val input = inputWithSwar
    val notes = List(
      NoteRef(Note.Sa, Variant.Shuddha, Octave.Madhya),
      NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya),
      NoteRef(Note.Ga, Variant.Shuddha, Octave.Madhya)
    )
    val result = OrnamentApi.addMurki(input, notes)

    result shouldBe a[Right[_, _]]
    val editorResult = result.toOption.get
    val event        = editorResult.composition.sections(0).events.head.asInstanceOf[Event.Swar]
    event.ornaments should have size 1
    event.ornaments.head shouldBe a[Murki]
  }

  test("addMurki with empty notes should return error") {
    val input  = inputWithSwar
    val result = OrnamentApi.addMurki(input, Nil)

    result shouldBe a[Left[_, _]]
    result.left.toOption.get shouldBe ApiError.EmptyNotes
  }

  test("addZamzama should add Zamzama ornament") {
    val input = inputWithSwar
    val notes = List(
      NoteRef(Note.Sa, Variant.Shuddha, Octave.Madhya),
      NoteRef(Note.Sa, Variant.Shuddha, Octave.Madhya)
    )
    val result = OrnamentApi.addZamzama(input, notes)

    result shouldBe a[Right[_, _]]
    val editorResult = result.toOption.get
    val event        = editorResult.composition.sections(0).events.head.asInstanceOf[Event.Swar]
    event.ornaments should have size 1
    event.ornaments.head shouldBe a[Zamzama]
  }

  test("addCustomOrnament should add custom ornament") {
    val input  = inputWithSwar
    val params = Map("param1" -> "value1", "param2" -> "value2")
    val result = OrnamentApi.addCustomOrnament(input, "MyOrnament", params)

    result shouldBe a[Right[_, _]]
    val editorResult = result.toOption.get
    val event        = editorResult.composition.sections(0).events.head.asInstanceOf[Event.Swar]
    event.ornaments should have size 1
    event.ornaments.head shouldBe a[CustomOrnament]
    event.ornaments.head.asInstanceOf[CustomOrnament].name shouldBe "MyOrnament"
  }
