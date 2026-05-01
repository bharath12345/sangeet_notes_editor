package com.varpas.sangeet.core.render

import org.scalatest.funsuite.AnyFunSuite
import com.varpas.sangeet.core.model.*

class ScriptMapSpec extends AnyFunSuite:

  test("Devanagari glyphs") {
    assert(ScriptMap.glyph(Note.Sa, SwarScript.Devanagari) == "सा")
    assert(ScriptMap.glyph(Note.Re, SwarScript.Devanagari) == "रे")
    assert(ScriptMap.glyph(Note.Ga, SwarScript.Devanagari) == "ग")
    assert(ScriptMap.glyph(Note.Ma, SwarScript.Devanagari) == "म")
    assert(ScriptMap.glyph(Note.Pa, SwarScript.Devanagari) == "प")
    assert(ScriptMap.glyph(Note.Dha, SwarScript.Devanagari) == "ध")
    assert(ScriptMap.glyph(Note.Ni, SwarScript.Devanagari) == "नि")
  }

  test("Kannada glyphs") {
    assert(ScriptMap.glyph(Note.Sa, SwarScript.Kannada) == "ಸಾ")
    assert(ScriptMap.glyph(Note.Re, SwarScript.Kannada) == "ರಿ")
    assert(ScriptMap.glyph(Note.Ga, SwarScript.Kannada) == "ಗ")
    assert(ScriptMap.glyph(Note.Ma, SwarScript.Kannada) == "ಮ")
    assert(ScriptMap.glyph(Note.Pa, SwarScript.Kannada) == "ಪ")
    assert(ScriptMap.glyph(Note.Dha, SwarScript.Kannada) == "ಧ")
    assert(ScriptMap.glyph(Note.Ni, SwarScript.Kannada) == "ನಿ")
  }

  test("Telugu glyphs") {
    assert(ScriptMap.glyph(Note.Sa, SwarScript.Telugu) == "స")
    assert(ScriptMap.glyph(Note.Re, SwarScript.Telugu) == "రి")
    assert(ScriptMap.glyph(Note.Ga, SwarScript.Telugu) == "గ")
    assert(ScriptMap.glyph(Note.Ma, SwarScript.Telugu) == "మ")
    assert(ScriptMap.glyph(Note.Pa, SwarScript.Telugu) == "ప")
    assert(ScriptMap.glyph(Note.Dha, SwarScript.Telugu) == "ధ")
    assert(ScriptMap.glyph(Note.Ni, SwarScript.Telugu) == "ని")
  }

  test("English glyphs") {
    assert(ScriptMap.glyph(Note.Sa, SwarScript.English) == "Sa")
    assert(ScriptMap.glyph(Note.Re, SwarScript.English) == "Re")
    assert(ScriptMap.glyph(Note.Ga, SwarScript.English) == "Ga")
    assert(ScriptMap.glyph(Note.Ma, SwarScript.English) == "Ma")
    assert(ScriptMap.glyph(Note.Pa, SwarScript.English) == "Pa")
    assert(ScriptMap.glyph(Note.Dha, SwarScript.English) == "Dha")
    assert(ScriptMap.glyph(Note.Ni, SwarScript.English) == "Ni")
  }

  test("fontName returns correct fonts") {
    assert(ScriptMap.fontName(SwarScript.Devanagari) == "Noto Sans Devanagari")
    assert(ScriptMap.fontName(SwarScript.Kannada) == "Noto Sans Kannada")
    assert(ScriptMap.fontName(SwarScript.Telugu) == "Noto Sans Telugu")
    assert(ScriptMap.fontName(SwarScript.English) == "System")
  }

  test("displayName returns correct display names") {
    assert(ScriptMap.displayName(SwarScript.Devanagari) == "Devanagari (Hindi)")
    assert(ScriptMap.displayName(SwarScript.Kannada) == "Kannada")
    assert(ScriptMap.displayName(SwarScript.Telugu) == "Telugu")
    assert(ScriptMap.displayName(SwarScript.English) == "English")
  }

  test("legendEntries returns correct number of entries") {
    val devanagariLegend = ScriptMap.legendEntries(SwarScript.Devanagari)
    assert(devanagariLegend.length == 12)

    val kannadaLegend = ScriptMap.legendEntries(SwarScript.Kannada)
    assert(kannadaLegend.length == 12)
  }

  test("legendEntries contains correct key mappings") {
    val legend = ScriptMap.legendEntries(SwarScript.Devanagari)
    val keys = legend.map(_._1)
    assert(keys == List("s", "r", "R", "g", "G", "m", "M", "p", "d", "D", "n", "N"))
  }

  test("legendEntries marks komal and tivra variants correctly") {
    val legend = ScriptMap.legendEntries(SwarScript.English)

    // Find komal Re
    val komalRe = legend.find(_._1 == "R")
    assert(komalRe.isDefined)
    assert(komalRe.get._3 == "komal")

    // Find tivra Ma
    val tivraMa = legend.find(_._1 == "M")
    assert(tivraMa.isDefined)
    assert(tivraMa.get._3 == "tivra")

    // Find shuddha variants
    val shuddhaRe = legend.find(_._1 == "r")
    assert(shuddhaRe.isDefined)
    assert(shuddhaRe.get._3 == "shuddha")
  }
