package com.varpas.sangeet.core.api

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.editor.CursorModel
import com.varpas.sangeet.core.taal.Taals
import com.varpas.sangeet.core.raag.Raags

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
    val withEvent = EditorApi.insertSwar(testInput, Note.Sa, Variant.Shuddha, Octave.Madhya).toOption.get
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
