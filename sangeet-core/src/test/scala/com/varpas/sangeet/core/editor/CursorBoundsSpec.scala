package com.varpas.sangeet.core.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CursorBoundsSpec extends AnyFlatSpec with Matchers:

  "CursorBounds.maxAllowedCycle" should "be maxCycle + 1 (one trailing empty cycle)" in {
    CursorBounds.maxAllowedCycle(0) shouldBe 1
    CursorBounds.maxAllowedCycle(5) shouldBe 6
    CursorBounds.maxAllowedCycle(100) shouldBe 101
  }

  "CursorBounds.canAdvanceTo" should "permit advancing to maxCycle" in {
    CursorBounds.canAdvanceTo(candidateCycle = 5, maxCycle = 5) shouldBe true
  }

  it should "permit advancing to one cycle past maxCycle (the trailing empty cycle)" in {
    CursorBounds.canAdvanceTo(candidateCycle = 6, maxCycle = 5) shouldBe true
  }

  it should "reject advancing two cycles past maxCycle" in {
    CursorBounds.canAdvanceTo(candidateCycle = 7, maxCycle = 5) shouldBe false
  }

  it should "permit advancing to cycle 1 in an empty composition (maxCycle == 0)" in {
    CursorBounds.canAdvanceTo(candidateCycle = 0, maxCycle = 0) shouldBe true
    CursorBounds.canAdvanceTo(candidateCycle = 1, maxCycle = 0) shouldBe true
    CursorBounds.canAdvanceTo(candidateCycle = 2, maxCycle = 0) shouldBe false
  }
