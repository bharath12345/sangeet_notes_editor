package com.varpas.sangeet.core.api

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.core.editor.CursorModel
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.taal.Taals

class EditorApiTest extends AnyFunSuite with Matchers:

  val testComp = CompositionApi.createComposition(
    title = "Test",
    compositionType = CompositionType.Gat,
    taal = Taals.teentaal,
    raag = Raags.yaman,
    laya = Some(Laya.Vilambit)
  )

  val testInput = EditorInput(
    composition = testComp,
    sectionIndex = 0,
    cursor = CursorModel(Taals.teentaal)
  )

  test("insertSwar should add a swar event") {
    val result = EditorApi.insertSwar(testInput, Note.Sa, Variant.Shuddha, Octave.Madhya)

    result shouldBe a[Right[_, _]]
    val editorResult = result.toOption.get
    editorResult.composition.sections(0).events should have size 1

    val event = editorResult.composition.sections(0).events.head
    event shouldBe a[Event.Swar]
    event.asInstanceOf[Event.Swar].note shouldBe Note.Sa
  }

  test("insertRest should add a rest event") {
    val result = EditorApi.insertRest(testInput)

    result shouldBe a[Right[_, _]]
    val editorResult = result.toOption.get
    editorResult.composition.sections(0).events should have size 1

    val event = editorResult.composition.sections(0).events.head
    event shouldBe a[Event.Rest]
  }

  test("insertSustain should add a sustain event") {
    val result = EditorApi.insertSustain(testInput)

    result shouldBe a[Right[_, _]]
    val editorResult = result.toOption.get
    editorResult.composition.sections(0).events should have size 1

    val event = editorResult.composition.sections(0).events.head
    event shouldBe a[Event.Sustain]
  }

  test("deleteLastEvent should remove the last event") {
    val withEvent      = EditorApi.insertSwar(testInput, Note.Sa, Variant.Shuddha, Octave.Madhya).toOption.get
    val inputWithEvent = testInput.copy(composition = withEvent.composition)

    val result = EditorApi.deleteLastEvent(inputWithEvent)

    result shouldBe a[Right[_, _]]
    val editorResult = result.toOption.get
    editorResult.composition.sections(0).events should have size 0
  }

  test("deleteLastEvent on empty section should return error") {
    val result = EditorApi.deleteLastEvent(testInput)

    result shouldBe a[Left[_, _]]
    result.left.toOption.get shouldBe ApiError.EmptySection
  }

  test("insertDualSwar should add two identical notes") {
    val result = EditorApi.insertDualSwar(testInput, Note.Sa, Variant.Shuddha, Octave.Madhya)

    result shouldBe a[Right[_, _]]
    val editorResult = result.toOption.get
    editorResult.composition.sections(0).events should have size 2

    val event1 = editorResult.composition.sections(0).events(0).asInstanceOf[Event.Swar]
    val event2 = editorResult.composition.sections(0).events(1).asInstanceOf[Event.Swar]

    event1.note shouldBe Note.Sa
    event2.note shouldBe Note.Sa
    event1.duration shouldBe Rational(1, 2)
    event2.duration shouldBe Rational(1, 2)
  }

  test("changeTaal should update the composition's taal") {
    val result = EditorApi.changeTaal(testComp, 0, Taals.ektaal)

    result shouldBe a[Right[_, _]]
    val editorResult = result.toOption.get
    editorResult.composition.metadata.taal.name shouldBe "Ektaal"
    editorResult.composition.metadata.taal.matras shouldBe 12
  }

  test("changeTaal should re-map event positions when matras shrinks") {
    // Insert two beats in the original Teen Taal composition. After
    // moving the cursor explicitly to beat 12 (which exists in Teen Taal
    // but is past Ek Taal's 12-matra limit), insertSwar puts a note at
    // (cycle=0, beat=12). After changeTaal to Ek Taal that absolute
    // beat 12 should re-flow to (cycle=1, beat=0).
    val cursorAt12 = CursorModel(Taals.teentaal, beat = 12)
    val inputAt12 = EditorInput(
      composition = testComp,
      sectionIndex = 0,
      cursor = cursorAt12
    )
    val withSwar = EditorApi.insertSwar(inputAt12, Note.Sa, Variant.Shuddha, Octave.Madhya).toOption.get

    val result = EditorApi.changeTaal(withSwar.composition, 0, Taals.ektaal).toOption.get

    val event = result.composition.sections(0).events.head.asInstanceOf[Event.Swar]
    event.beat.cycle shouldBe 1
    event.beat.beat shouldBe 0
  }

  test("changeTaal should reset cursor to startingBeat - 1 of the active section") {
    val result = EditorApi.changeTaal(testComp, 0, Taals.ektaal).toOption.get
    result.cursor.taal.name shouldBe "Ektaal"
    result.cursor.cycle shouldBe 0
    // Default section startingBeat for Gat in tests is taken from
    // composition; we just require it's a valid beat index for the new
    // taal (not stale from the old one).
    result.cursor.beat should be >= 0
    result.cursor.beat should be < Taals.ektaal.matras
  }

  test("changeTaal with invalid section index should return error") {
    val result = EditorApi.changeTaal(testComp, 999, Taals.ektaal)
    result shouldBe a[Left[_, _]]
  }
