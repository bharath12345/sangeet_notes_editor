package com.varpas.sangeet.core.api

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ReferenceApiTest extends AnyFunSuite with Matchers:

  test("allTaals should return all built-in taals") {
    val taals = ReferenceApi.allTaals
    taals should not be empty
    taals.keySet should contain("teentaal")
    taals.keySet should contain("ektaal")
  }

  test("taalByName should find teentaal") {
    val result = ReferenceApi.taalByName("Teentaal")
    result shouldBe a[Right[_, _]]
    result.toOption.get.name shouldBe "Teentaal"
    result.toOption.get.matras shouldBe 16
  }

  test("taalByName should be case-insensitive") {
    val result = ReferenceApi.taalByName("TEENTAAL")
    result shouldBe a[Right[_, _]]
    result.toOption.get.name shouldBe "Teentaal"
  }

  test("taalByName with unknown name should return error") {
    val result = ReferenceApi.taalByName("NonExistentTaal")
    result shouldBe a[Left[_, _]]
    result.left.toOption.get shouldBe a[ApiError.NotFound]
  }

  test("allRaags should return all built-in raags") {
    val raags = ReferenceApi.allRaags
    raags should not be empty
    raags.keySet should contain("yaman")
    raags.keySet should contain("bhairav")
  }

  test("raagByName should find yaman") {
    val result = ReferenceApi.raagByName("Yaman")
    result shouldBe a[Right[_, _]]
    result.toOption.get.name shouldBe "Yaman"
  }

  test("raagByName should be case-insensitive") {
    val result = ReferenceApi.raagByName("YAMAN")
    result shouldBe a[Right[_, _]]
    result.toOption.get.name shouldBe "Yaman"
  }

  test("raagByName with unknown name should return error") {
    val result = ReferenceApi.raagByName("NonExistentRaag")
    result shouldBe a[Left[_, _]]
    result.left.toOption.get shouldBe a[ApiError.NotFound]
  }
