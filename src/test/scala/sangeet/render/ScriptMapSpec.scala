package sangeet.render

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*

class ScriptMapSpec extends AnyFlatSpec with Matchers:

  "ScriptMap.glyph" should "return Devanagari glyphs" in {
    ScriptMap.glyph(Note.Sa, SwarScript.Devanagari) shouldBe "सा"
    ScriptMap.glyph(Note.Re, SwarScript.Devanagari) shouldBe "रे"
    ScriptMap.glyph(Note.Ga, SwarScript.Devanagari) shouldBe "ग"
    ScriptMap.glyph(Note.Ma, SwarScript.Devanagari) shouldBe "म"
    ScriptMap.glyph(Note.Pa, SwarScript.Devanagari) shouldBe "प"
    ScriptMap.glyph(Note.Dha, SwarScript.Devanagari) shouldBe "ध"
    ScriptMap.glyph(Note.Ni, SwarScript.Devanagari) shouldBe "नि"
  }

  it should "return Kannada glyphs" in {
    ScriptMap.glyph(Note.Sa, SwarScript.Kannada) shouldBe "ಸಾ"
    ScriptMap.glyph(Note.Re, SwarScript.Kannada) shouldBe "ರಿ"
    ScriptMap.glyph(Note.Ga, SwarScript.Kannada) shouldBe "ಗ"
    ScriptMap.glyph(Note.Ma, SwarScript.Kannada) shouldBe "ಮ"
    ScriptMap.glyph(Note.Pa, SwarScript.Kannada) shouldBe "ಪ"
    ScriptMap.glyph(Note.Dha, SwarScript.Kannada) shouldBe "ಧ"
    ScriptMap.glyph(Note.Ni, SwarScript.Kannada) shouldBe "ನಿ"
  }

  it should "return Telugu glyphs" in {
    ScriptMap.glyph(Note.Sa, SwarScript.Telugu) shouldBe "స"
    ScriptMap.glyph(Note.Re, SwarScript.Telugu) shouldBe "రి"
    ScriptMap.glyph(Note.Ga, SwarScript.Telugu) shouldBe "గ"
    ScriptMap.glyph(Note.Ma, SwarScript.Telugu) shouldBe "మ"
    ScriptMap.glyph(Note.Pa, SwarScript.Telugu) shouldBe "ప"
    ScriptMap.glyph(Note.Dha, SwarScript.Telugu) shouldBe "ధ"
    ScriptMap.glyph(Note.Ni, SwarScript.Telugu) shouldBe "ని"
  }

  it should "return English glyphs" in {
    ScriptMap.glyph(Note.Sa, SwarScript.English) shouldBe "Sa"
    ScriptMap.glyph(Note.Re, SwarScript.English) shouldBe "Re"
    ScriptMap.glyph(Note.Ga, SwarScript.English) shouldBe "Ga"
    ScriptMap.glyph(Note.Ma, SwarScript.English) shouldBe "Ma"
    ScriptMap.glyph(Note.Pa, SwarScript.English) shouldBe "Pa"
    ScriptMap.glyph(Note.Dha, SwarScript.English) shouldBe "Dha"
    ScriptMap.glyph(Note.Ni, SwarScript.English) shouldBe "Ni"
  }

  it should "return distinct glyphs per script for each note" in {
    val scripts = List(SwarScript.Devanagari, SwarScript.Kannada, SwarScript.Telugu, SwarScript.English)
    for note <- Note.values do
      val glyphs = scripts.map(ScriptMap.glyph(note, _))
      glyphs.distinct should have length 4
  }

  "ScriptMap.fontName" should "return correct font for each script" in {
    ScriptMap.fontName(SwarScript.Devanagari) shouldBe "Noto Sans Devanagari"
    ScriptMap.fontName(SwarScript.Kannada) shouldBe "Noto Sans Kannada"
    ScriptMap.fontName(SwarScript.Telugu) shouldBe "Noto Sans Telugu"
    ScriptMap.fontName(SwarScript.English) shouldBe "System"
  }

  "ScriptMap.legendEntries" should "return 12 entries for each script" in {
    for script <- SwarScript.values do
      ScriptMap.legendEntries(script) should have length 12
  }

  it should "have correct key mappings" in {
    val entries = ScriptMap.legendEntries(SwarScript.Devanagari)
    entries.head._1 shouldBe "s" // Sa
    entries(1)._1 shouldBe "r"  // shuddha Re
    entries(2)._1 shouldBe "R"  // komal Re
    entries(6)._1 shouldBe "M"  // tivra Ma
    entries(7)._1 shouldBe "p"  // Pa
  }

  it should "include variant info for Re, Ga, Ma, Dha, Ni" in {
    val entries = ScriptMap.legendEntries(SwarScript.English)
    entries(1)._3 shouldBe "shuddha" // r = shuddha Re
    entries(2)._3 shouldBe "komal"   // R = komal Re
    entries(6)._3 shouldBe "tivra"   // M = tivra Ma
  }

  it should "have no variant info for Sa and Pa" in {
    val entries = ScriptMap.legendEntries(SwarScript.English)
    entries.head._3 shouldBe ""  // Sa
    entries(7)._3 shouldBe ""    // Pa
  }

  "ScriptMap.displayName" should "return human-readable names" in {
    ScriptMap.displayName(SwarScript.Devanagari) shouldBe "Devanagari (Hindi)"
    ScriptMap.displayName(SwarScript.Kannada) shouldBe "Kannada"
    ScriptMap.displayName(SwarScript.Telugu) shouldBe "Telugu"
    ScriptMap.displayName(SwarScript.English) shouldBe "English"
  }
