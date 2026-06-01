package com.varpas.sangeet.core.render

import org.scalatest.funsuite.AnyFunSuite

class ScriptUtilSpec extends AnyFunSuite:

  // --- isIndicChar ---

  test("isIndicChar recognizes Devanagari characters") {
    assert(ScriptUtil.isIndicChar('स'))
    assert(ScriptUtil.isIndicChar('र'))
    assert(ScriptUtil.isIndicChar('ग'))
    assert(ScriptUtil.isIndicChar('म'))
  }

  test("isIndicChar recognizes Kannada characters") {
    assert(ScriptUtil.isIndicChar('ಸ'))
    assert(ScriptUtil.isIndicChar('ರ'))
  }

  test("isIndicChar recognizes Telugu characters") {
    assert(ScriptUtil.isIndicChar('స'))
    assert(ScriptUtil.isIndicChar('ర'))
  }

  test("isIndicChar recognizes Bengali characters") {
    assert(ScriptUtil.isIndicChar('স'))
    assert(ScriptUtil.isIndicChar('র'))
  }

  test("isIndicChar recognizes Gujarati characters") {
    assert(ScriptUtil.isIndicChar('સ'))
  }

  test("isIndicChar rejects ASCII characters") {
    assert(!ScriptUtil.isIndicChar('a'))
    assert(!ScriptUtil.isIndicChar('Z'))
    assert(!ScriptUtil.isIndicChar('0'))
    assert(!ScriptUtil.isIndicChar(' '))
  }

  test("isIndicChar rejects common punctuation") {
    assert(!ScriptUtil.isIndicChar('.'))
    assert(!ScriptUtil.isIndicChar(','))
    assert(!ScriptUtil.isIndicChar('-'))
  }

  // --- containsNonLatin ---

  test("containsNonLatin returns true for Devanagari text") {
    assert(ScriptUtil.containsNonLatin("सारेगम"))
  }

  test("containsNonLatin returns true for mixed text") {
    assert(ScriptUtil.containsNonLatin("Sa = सा"))
  }

  test("containsNonLatin returns false for pure ASCII") {
    assert(!ScriptUtil.containsNonLatin("Sa Re Ga Ma"))
  }

  test("containsNonLatin returns false for empty string") {
    assert(!ScriptUtil.containsNonLatin(""))
  }

  // --- splitByScript ---

  test("splitByScript returns empty for empty string") {
    assert(ScriptUtil.splitByScript("") == Nil)
  }

  test("splitByScript handles pure ASCII") {
    val result = ScriptUtil.splitByScript("Hello")
    assert(result == List(("Hello", false)))
  }

  test("splitByScript handles pure Devanagari") {
    val result = ScriptUtil.splitByScript("सारेगम")
    assert(result == List(("सारेगम", true)))
  }

  test("splitByScript splits mixed Latin and Devanagari") {
    val result = ScriptUtil.splitByScript("Sa सा Re रे")
    assert(result.size >= 3)
    assert(result.head == ("Sa ", false))
    assert(result(1)._2 == true) // Devanagari segment
  }

  test("splitByScript preserves all characters") {
    val input = "Sa सा Re रे"
    val result = ScriptUtil.splitByScript(input)
    assert(result.map(_._1).mkString == input)
  }

  test("splitByScript handles alternating scripts") {
    val result = ScriptUtil.splitByScript("aसb")
    assert(result == List(("a", false), ("स", true), ("b", false)))
  }

  // --- sanitizeForFont ---

  test("sanitizeForFont replaces em dash with hyphen") {
    assert(ScriptUtil.sanitizeForFont("A—B") == "A-B")
  }

  test("sanitizeForFont replaces en dash with hyphen") {
    assert(ScriptUtil.sanitizeForFont("A–B") == "A-B")
  }

  test("sanitizeForFont replaces smart single quotes") {
    assert(ScriptUtil.sanitizeForFont("‘hello’") == "'hello'")
  }

  test("sanitizeForFont replaces smart double quotes") {
    assert(ScriptUtil.sanitizeForFont("“hello”") == "\"hello\"")
  }

  test("sanitizeForFont replaces ellipsis") {
    assert(ScriptUtil.sanitizeForFont("wait…") == "wait.")
  }

  test("sanitizeForFont replaces dagger with plus") {
    assert(ScriptUtil.sanitizeForFont("†") == "+")
  }

  test("sanitizeForFont preserves Indic characters") {
    val devanagari = "सारेगम"
    assert(ScriptUtil.sanitizeForFont(devanagari) == devanagari)
  }

  test("sanitizeForFont preserves plain ASCII") {
    assert(ScriptUtil.sanitizeForFont("Hello World 123") == "Hello World 123")
  }

  test("sanitizeForFont replaces unknown non-Latin non-Indic with ?") {
    // Chinese character — not Indic, above 0xFF
    assert(ScriptUtil.sanitizeForFont("中") == "?")
  }
