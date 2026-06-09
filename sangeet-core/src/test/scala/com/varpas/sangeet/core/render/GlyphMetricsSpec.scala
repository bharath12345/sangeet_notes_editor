package com.varpas.sangeet.core.render

import org.scalatest.funsuite.AnyFunSuite

import com.varpas.sangeet.core.model._

class GlyphMetricsSpec extends AnyFunSuite:

  test("glyph delegates to ScriptMap") {
    assert(GlyphMetrics.glyph(Note.Sa, Variant.Shuddha, SwarScript.Devanagari) == "सा")
    assert(GlyphMetrics.glyph(Note.Re, Variant.Komal, SwarScript.English) == "Re")
    assert(GlyphMetrics.glyph(Note.Ma, Variant.Tivra, SwarScript.Kannada) == "ಮ")
  }

  test("needsKomalMark returns true for komal Re, Ga, Dha, Ni") {
    assert(GlyphMetrics.needsKomalMark(Note.Re, Variant.Komal))
    assert(GlyphMetrics.needsKomalMark(Note.Ga, Variant.Komal))
    assert(GlyphMetrics.needsKomalMark(Note.Dha, Variant.Komal))
    assert(GlyphMetrics.needsKomalMark(Note.Ni, Variant.Komal))
  }

  test("needsKomalMark returns false for komal Sa, Ma, Pa") {
    assert(!GlyphMetrics.needsKomalMark(Note.Sa, Variant.Komal))
    assert(!GlyphMetrics.needsKomalMark(Note.Ma, Variant.Komal))
    assert(!GlyphMetrics.needsKomalMark(Note.Pa, Variant.Komal))
  }

  test("needsKomalMark returns false for shuddha notes") {
    assert(!GlyphMetrics.needsKomalMark(Note.Re, Variant.Shuddha))
    assert(!GlyphMetrics.needsKomalMark(Note.Ga, Variant.Shuddha))
    assert(!GlyphMetrics.needsKomalMark(Note.Dha, Variant.Shuddha))
    assert(!GlyphMetrics.needsKomalMark(Note.Ni, Variant.Shuddha))
  }

  test("needsTivraMark returns true only for tivra Ma") {
    assert(GlyphMetrics.needsTivraMark(Note.Ma, Variant.Tivra))
  }

  test("needsTivraMark returns false for all other notes") {
    assert(!GlyphMetrics.needsTivraMark(Note.Sa, Variant.Tivra))
    assert(!GlyphMetrics.needsTivraMark(Note.Re, Variant.Tivra))
    assert(!GlyphMetrics.needsTivraMark(Note.Ga, Variant.Tivra))
    assert(!GlyphMetrics.needsTivraMark(Note.Pa, Variant.Tivra))
    assert(!GlyphMetrics.needsTivraMark(Note.Dha, Variant.Tivra))
    assert(!GlyphMetrics.needsTivraMark(Note.Ni, Variant.Tivra))
  }

  test("needsTivraMark returns false for shuddha Ma") {
    assert(!GlyphMetrics.needsTivraMark(Note.Ma, Variant.Shuddha))
  }

  test("octaveDots returns correct values for all octaves") {
    assert(GlyphMetrics.octaveDots(Octave.AtiMandra) == (2, DotPosition.Below))
    assert(GlyphMetrics.octaveDots(Octave.Mandra) == (1, DotPosition.Below))
    assert(GlyphMetrics.octaveDots(Octave.Madhya) == (0, DotPosition.None))
    assert(GlyphMetrics.octaveDots(Octave.Taar) == (1, DotPosition.Above))
    assert(GlyphMetrics.octaveDots(Octave.AtiTaar) == (2, DotPosition.Above))
  }

  test("rest and sustain symbols are defined") {
    assert(GlyphMetrics.restSymbol == "-")
    assert(GlyphMetrics.sustainSymbol == "\u2014")
  }

  test("vibhagMarkerText returns correct text for Sam") {
    assert(GlyphMetrics.vibhagMarkerText(VibhagMarker.Sam) == "X")
  }

  test("vibhagMarkerText returns correct text for Khali") {
    assert(GlyphMetrics.vibhagMarkerText(VibhagMarker.Khali) == "0")
  }

  test("vibhagMarkerText returns correct text for Taali") {
    assert(GlyphMetrics.vibhagMarkerText(VibhagMarker.Taali(2)) == "2")
    assert(GlyphMetrics.vibhagMarkerText(VibhagMarker.Taali(3)) == "3")
    assert(GlyphMetrics.vibhagMarkerText(VibhagMarker.Taali(4)) == "4")
  }

  test("strokeText returns English text for English script") {
    assert(GlyphMetrics.strokeText(Stroke.Da, SwarScript.English) == "Da")
    assert(GlyphMetrics.strokeText(Stroke.Ra, SwarScript.English) == "Ra")
    assert(GlyphMetrics.strokeText(Stroke.Jod, SwarScript.English) == "Jo")
  }

  test("strokeText returns Devanagari text for Devanagari script") {
    assert(GlyphMetrics.strokeText(Stroke.Da, SwarScript.Devanagari) == "दा")
    assert(GlyphMetrics.strokeText(Stroke.Ra, SwarScript.Devanagari) == "रा")
    assert(GlyphMetrics.strokeText(Stroke.Jod, SwarScript.Devanagari) == "जो")
  }

  test("chikariSwarText returns 1") {
    assert(GlyphMetrics.chikariSwarText == "1")
  }

  test("chikariStrokeText returns English or Devanagari") {
    assert(GlyphMetrics.chikariStrokeText(SwarScript.English) == "Ch")
    assert(GlyphMetrics.chikariStrokeText(SwarScript.Devanagari) == "ची")
  }

  test("strokeText returns Devanagari text for Kannada script") {
    assert(GlyphMetrics.strokeText(Stroke.Da, SwarScript.Kannada) == "दा")
    assert(GlyphMetrics.strokeText(Stroke.Ra, SwarScript.Kannada) == "रा")
  }

  test("strokeText returns Devanagari text for Telugu script") {
    assert(GlyphMetrics.strokeText(Stroke.Da, SwarScript.Telugu) == "दा")
    assert(GlyphMetrics.strokeText(Stroke.Ra, SwarScript.Telugu) == "रा")
  }
