package sangeet.render

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*

class DevanagariMapSpec extends AnyFlatSpec with Matchers:

  "DevanagariMap.glyph" should "return Devanagari for shuddha swaras" in {
    DevanagariMap.glyph(Note.Sa, Variant.Shuddha) shouldBe "सा"
    DevanagariMap.glyph(Note.Re, Variant.Shuddha) shouldBe "रे"
    DevanagariMap.glyph(Note.Ga, Variant.Shuddha) shouldBe "ग"
    DevanagariMap.glyph(Note.Ma, Variant.Shuddha) shouldBe "म"
    DevanagariMap.glyph(Note.Pa, Variant.Shuddha) shouldBe "प"
    DevanagariMap.glyph(Note.Dha, Variant.Shuddha) shouldBe "ध"
    DevanagariMap.glyph(Note.Ni, Variant.Shuddha) shouldBe "नि"
  }

  it should "return same base glyph for komal/tivra (modifiers rendered separately)" in {
    DevanagariMap.glyph(Note.Re, Variant.Komal) shouldBe "रे"
    DevanagariMap.glyph(Note.Ma, Variant.Tivra) shouldBe "म"
  }

  "DevanagariMap.needsKomalMark" should "be true for komal Re, Ga, Dha, Ni" in {
    DevanagariMap.needsKomalMark(Note.Re, Variant.Komal) shouldBe true
    DevanagariMap.needsKomalMark(Note.Ga, Variant.Komal) shouldBe true
    DevanagariMap.needsKomalMark(Note.Dha, Variant.Komal) shouldBe true
    DevanagariMap.needsKomalMark(Note.Ni, Variant.Komal) shouldBe true
  }

  it should "be false for shuddha and for Sa/Pa" in {
    DevanagariMap.needsKomalMark(Note.Sa, Variant.Shuddha) shouldBe false
    DevanagariMap.needsKomalMark(Note.Re, Variant.Shuddha) shouldBe false
  }

  "DevanagariMap.needsTivraMark" should "be true only for tivra Ma" in {
    DevanagariMap.needsTivraMark(Note.Ma, Variant.Tivra) shouldBe true
    DevanagariMap.needsTivraMark(Note.Ma, Variant.Shuddha) shouldBe false
    DevanagariMap.needsTivraMark(Note.Re, Variant.Tivra) shouldBe false
  }

  "DevanagariMap.octaveDots" should "return dot count and direction" in {
    DevanagariMap.octaveDots(Octave.Mandra) shouldBe (1, DotPosition.Below)
    DevanagariMap.octaveDots(Octave.Madhya) shouldBe (0, DotPosition.None)
    DevanagariMap.octaveDots(Octave.Taar) shouldBe (1, DotPosition.Above)
    DevanagariMap.octaveDots(Octave.AtiMandra) shouldBe (2, DotPosition.Below)
    DevanagariMap.octaveDots(Octave.AtiTaar) shouldBe (2, DotPosition.Above)
  }

  "DevanagariMap.restSymbol" should "return dash" in {
    DevanagariMap.restSymbol shouldBe "-"
  }

  "DevanagariMap.sustainSymbol" should "return dash" in {
    DevanagariMap.sustainSymbol shouldBe "-"
  }
