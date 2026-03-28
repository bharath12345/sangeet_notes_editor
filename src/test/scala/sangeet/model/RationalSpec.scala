package sangeet.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RationalSpec extends AnyFlatSpec with Matchers:

  "Rational" should "represent fractions" in {
    val half = Rational(1, 2)
    half.numerator shouldBe 1
    half.denominator shouldBe 2
  }

  it should "convert to double" in {
    Rational(1, 2).toDouble shouldBe 0.5
    Rational(1, 3).toDouble shouldBe (1.0 / 3.0) +- 0.0001
    Rational(0, 1).toDouble shouldBe 0.0
  }

  it should "add two rationals" in {
    val r = Rational(1, 4) + Rational(1, 4)
    r.toDouble shouldBe 0.5
  }

  it should "compare rationals" in {
    Rational(1, 2) should be > Rational(1, 3)
    Rational(1, 4) should be < Rational(1, 3)
    Rational(2, 4) shouldBe Rational(1, 2)
  }

  it should "normalize fractions" in {
    Rational(2, 4).numerator shouldBe 1
    Rational(2, 4).denominator shouldBe 2
    Rational(6, 8).numerator shouldBe 3
    Rational(6, 8).denominator shouldBe 4
  }

  it should "represent on-beat as 0/1" in {
    Rational.onBeat shouldBe Rational(0, 1)
  }

  it should "represent a full beat as 1/1" in {
    Rational.fullBeat shouldBe Rational(1, 1)
  }
