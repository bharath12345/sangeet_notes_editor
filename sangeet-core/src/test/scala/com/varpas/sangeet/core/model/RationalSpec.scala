package com.varpas.sangeet.core.model

import org.scalatest.funsuite.AnyFunSuite

class RationalSpec extends AnyFunSuite:

  test("normalization reduces fraction to simplest form") {
    val r1 = Rational(2, 4)
    val r2 = Rational(1, 2)
    assert(r1 == r2)
  }

  test("normalization handles zero numerator") {
    val r = Rational(0, 5)
    assert(r.numerator == 0)
    assert(r.denominator == 1)
  }

  test("normalization computes GCD correctly") {
    val r = Rational(6, 9)
    assert(r.numerator == 2)
    assert(r.denominator == 3)
  }

  test("normalization handles negative denominator") {
    val r = Rational(3, -6)
    assert(r.numerator == -1)
    assert(r.denominator == 2)
  }

  test("addition of rationals") {
    val r1     = Rational(1, 3)
    val r2     = Rational(1, 6)
    val result = r1 + r2
    assert(result == Rational(1, 2))
  }

  test("comparison works correctly") {
    val r1 = Rational(1, 2)
    val r2 = Rational(1, 3)
    assert(r1 > r2)
  }

  test("toDouble converts correctly") {
    val r = Rational(1, 2)
    assert(r.toDouble == 0.5)
  }

  test("onBeat is zero") {
    assert(Rational.onBeat == Rational(0, 1))
  }

  test("fullBeat is one") {
    assert(Rational.fullBeat == Rational(1, 1))
  }

  test("require fails with zero denominator") {
    assertThrows[IllegalArgumentException] {
      Rational(1, 0)
    }
  }
