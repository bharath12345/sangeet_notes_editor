package com.varpas.sangeet.core.api

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.taal.Taals

class CompositionApiTest extends AnyFunSuite with Matchers:

  test("createComposition should create a valid composition") {
    val comp = CompositionApi.createComposition(
      title = "Test Gat",
      compositionType = CompositionType.Gat,
      taal = Taals.teentaal,
      raag = Raags.yaman,
      laya = Some(Laya.Vilambit)
    )

    comp.metadata.title shouldBe "Test Gat"
    comp.metadata.compositionType shouldBe CompositionType.Gat
    comp.sections should have size 2 // Gat and Antara
    comp.sections(0).name shouldBe "Gat"
    comp.sections(1).name shouldBe "Antara"
  }

  test("createComposition with taans should create correct sections") {
    val comp = CompositionApi.createComposition(
      title = "Test Gat",
      compositionType = CompositionType.Gat,
      taal = Taals.teentaal,
      raag = Raags.yaman,
      laya = Some(Laya.Vilambit),
      taanCount = 3
    )

    comp.sections should have size 5 // Gat, Antara, Taan 1, Taan 2, Taan 3
    comp.sections(2).name shouldBe "Taan 1"
    comp.sections(3).name shouldBe "Taan 2"
    comp.sections(4).name shouldBe "Taan 3"
  }

  test("serializeComposition and parseComposition should round-trip") {
    val original = CompositionApi.createComposition(
      title = "Round Trip Test",
      compositionType = CompositionType.Palta,
      taal = Taals.teentaal,
      raag = Raags.yaman,
      laya = None
    )

    val jsonString = CompositionApi.serializeCompositionString(original)
    val parsed     = CompositionApi.parseComposition(jsonString)

    parsed shouldBe a[Right[_, _]]
    parsed.toOption.get.metadata.title shouldBe "Round Trip Test"
  }

  test("parseComposition should fail on invalid JSON") {
    val result = CompositionApi.parseComposition("{ invalid json")
    result shouldBe a[Left[_, _]]
    result.left.toOption.get shouldBe a[ApiError.ParseError]
  }
