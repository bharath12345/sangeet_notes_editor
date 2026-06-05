package com.varpas.sangeet.core.api

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.taal.Taals

class SectionApiTest extends AnyFunSuite with Matchers:

  val testComp = CompositionApi.createComposition(
    title = "Test",
    compositionType = CompositionType.Gat,
    taal = Taals.teentaal,
    raag = Raags.yaman,
    laya = Some(Laya.Vilambit)
  )

  test("addSection should add a new section") {
    val result = SectionApi.addSection(testComp, "Taan 1", SectionType.Taan)

    result shouldBe a[Right[_, _]]
    val newComp = result.toOption.get
    newComp.sections should have size 3
    newComp.sections(2).name shouldBe "Taan 1"
  }

  test("renameSection should rename the section") {
    val result = SectionApi.renameSection(testComp, 0, "Sthayi")

    result shouldBe a[Right[_, _]]
    val newComp = result.toOption.get
    newComp.sections(0).name shouldBe "Sthayi"
  }

  test("renameSection with invalid index should return error") {
    val result = SectionApi.renameSection(testComp, 10, "Invalid")

    result shouldBe a[Left[_, _]]
    result.left.toOption.get shouldBe a[ApiError.InvalidSectionIndex]
  }

  test("removeSection should remove a section") {
    val threeSection = SectionApi.addSection(testComp, "Taan 1", SectionType.Taan).toOption.get
    val result       = SectionApi.removeSection(threeSection, 0, 2)

    result shouldBe a[Right[_, _]]
    val (newComp, newIndex) = result.toOption.get
    newComp.sections should have size 2
  }

  test("removeSection on last section should return error") {
    val oneSection = testComp.copy(sections = testComp.sections.take(1))
    val result     = SectionApi.removeSection(oneSection, 0, 0)

    result shouldBe a[Left[_, _]]
    result.left.toOption.get shouldBe ApiError.LastSection
  }

  test("moveSection should reorder sections") {
    val result = SectionApi.moveSection(testComp, 0, 0, 1)

    result shouldBe a[Right[_, _]]
    val (newComp, newIndex) = result.toOption.get
    newComp.sections(0).name shouldBe "Antara"
    newComp.sections(1).name shouldBe "Gat"
  }

  test("moveSection with same indices should not change anything") {
    val result = SectionApi.moveSection(testComp, 0, 0, 0)

    result shouldBe a[Right[_, _]]
    val (newComp, newIndex) = result.toOption.get
    newComp shouldBe testComp
  }
