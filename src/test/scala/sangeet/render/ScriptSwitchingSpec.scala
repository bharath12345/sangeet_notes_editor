package sangeet.render

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*

class ScriptSwitchingSpec extends AnyFlatSpec with Matchers:

  "DevanagariMap script switching" should "default to Devanagari" in {
    DevanagariMap.setScript(SwarScript.Devanagari)
    DevanagariMap.currentScript shouldBe SwarScript.Devanagari
  }

  it should "switch to Kannada and return Kannada glyphs" in {
    DevanagariMap.setScript(SwarScript.Kannada)
    DevanagariMap.currentScript shouldBe SwarScript.Kannada
    DevanagariMap.glyph(Note.Sa, Variant.Shuddha) shouldBe "ಸಾ"
    DevanagariMap.glyph(Note.Re, Variant.Shuddha) shouldBe "ರಿ"
    DevanagariMap.fontName shouldBe "Noto Sans Kannada"
  }

  it should "switch to Telugu and return Telugu glyphs" in {
    DevanagariMap.setScript(SwarScript.Telugu)
    DevanagariMap.currentScript shouldBe SwarScript.Telugu
    DevanagariMap.glyph(Note.Sa, Variant.Shuddha) shouldBe "స"
    DevanagariMap.glyph(Note.Ni, Variant.Shuddha) shouldBe "ని"
    DevanagariMap.fontName shouldBe "Noto Sans Telugu"
  }

  it should "switch to English and return English glyphs" in {
    DevanagariMap.setScript(SwarScript.English)
    DevanagariMap.currentScript shouldBe SwarScript.English
    DevanagariMap.glyph(Note.Sa, Variant.Shuddha) shouldBe "Sa"
    DevanagariMap.glyph(Note.Pa, Variant.Shuddha) shouldBe "Pa"
    DevanagariMap.fontName shouldBe "System"
  }

  it should "switch back to Devanagari" in {
    DevanagariMap.setScript(SwarScript.English)
    DevanagariMap.glyph(Note.Sa, Variant.Shuddha) shouldBe "Sa"
    DevanagariMap.setScript(SwarScript.Devanagari)
    DevanagariMap.glyph(Note.Sa, Variant.Shuddha) shouldBe "सा"
    DevanagariMap.fontName shouldBe "Noto Sans Devanagari"
  }

  it should "not affect komal/tivra mark detection when switching scripts" in {
    for script <- SwarScript.values do
      DevanagariMap.setScript(script)
      DevanagariMap.needsKomalMark(Note.Re, Variant.Komal) shouldBe true
      DevanagariMap.needsKomalMark(Note.Ga, Variant.Komal) shouldBe true
      DevanagariMap.needsTivraMark(Note.Ma, Variant.Tivra) shouldBe true
      DevanagariMap.needsKomalMark(Note.Sa, Variant.Shuddha) shouldBe false
      DevanagariMap.needsTivraMark(Note.Re, Variant.Komal) shouldBe false

    // Reset to default
    DevanagariMap.setScript(SwarScript.Devanagari)
  }

  it should "not affect octave dots when switching scripts" in {
    for script <- SwarScript.values do
      DevanagariMap.setScript(script)
      DevanagariMap.octaveDots(Octave.Mandra) shouldBe (1, DotPosition.Below)
      DevanagariMap.octaveDots(Octave.Madhya) shouldBe (0, DotPosition.None)
      DevanagariMap.octaveDots(Octave.Taar) shouldBe (1, DotPosition.Above)

    DevanagariMap.setScript(SwarScript.Devanagari)
  }

  it should "not affect rest and sustain symbols" in {
    for script <- SwarScript.values do
      DevanagariMap.setScript(script)
      DevanagariMap.restSymbol shouldBe "-"
      DevanagariMap.sustainSymbol shouldBe "\u2014"

    DevanagariMap.setScript(SwarScript.Devanagari)
  }
