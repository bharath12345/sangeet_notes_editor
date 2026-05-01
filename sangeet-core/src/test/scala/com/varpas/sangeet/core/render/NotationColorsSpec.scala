package com.varpas.sangeet.core.render

import org.scalatest.funsuite.AnyFunSuite

class NotationColorsSpec extends AnyFunSuite:

  test("all color values are defined and non-empty") {
    assert(NotationColors.taalMarker.nonEmpty)
    assert(NotationColors.taalMarkerSam.nonEmpty)
    assert(NotationColors.swar.nonEmpty)
    assert(NotationColors.octaveDot.nonEmpty)
    assert(NotationColors.ornament.nonEmpty)
    assert(NotationColors.stroke.nonEmpty)
    assert(NotationColors.sahitya.nonEmpty)
    assert(NotationColors.rest.nonEmpty)
    assert(NotationColors.sustain.nonEmpty)
    assert(NotationColors.komalMark.nonEmpty)
    assert(NotationColors.tivraMark.nonEmpty)
  }

  test("all colors start with #") {
    assert(NotationColors.taalMarker.startsWith("#"))
    assert(NotationColors.taalMarkerSam.startsWith("#"))
    assert(NotationColors.swar.startsWith("#"))
    assert(NotationColors.octaveDot.startsWith("#"))
    assert(NotationColors.ornament.startsWith("#"))
    assert(NotationColors.stroke.startsWith("#"))
    assert(NotationColors.sahitya.startsWith("#"))
    assert(NotationColors.rest.startsWith("#"))
    assert(NotationColors.sustain.startsWith("#"))
    assert(NotationColors.komalMark.startsWith("#"))
    assert(NotationColors.tivraMark.startsWith("#"))
  }

  test("all colors are valid 6-digit hex") {
    def isValidHex(color: String): Boolean =
      color.length == 7 && color.startsWith("#") &&
        color.substring(1).forall(c => c.isDigit || "ABCDEFabcdef".contains(c))

    assert(isValidHex(NotationColors.taalMarker))
    assert(isValidHex(NotationColors.taalMarkerSam))
    assert(isValidHex(NotationColors.swar))
    assert(isValidHex(NotationColors.octaveDot))
    assert(isValidHex(NotationColors.ornament))
    assert(isValidHex(NotationColors.stroke))
    assert(isValidHex(NotationColors.sahitya))
    assert(isValidHex(NotationColors.rest))
    assert(isValidHex(NotationColors.sustain))
    assert(isValidHex(NotationColors.komalMark))
    assert(isValidHex(NotationColors.tivraMark))
  }

  test("hexToRgb parses dark red correctly") {
    val (r, g, b) = NotationColors.hexToRgb("#B71C1C")
    assert(r >= 0.71f && r <= 0.72f) // 183/255 ≈ 0.718
    assert(g >= 0.10f && g <= 0.11f) // 28/255 ≈ 0.110
    assert(b >= 0.10f && b <= 0.11f) // 28/255 ≈ 0.110
  }

  test("hexToRgb parses white correctly") {
    val (r, g, b) = NotationColors.hexToRgb("#FFFFFF")
    assert(r == 1.0f)
    assert(g == 1.0f)
    assert(b == 1.0f)
  }

  test("hexToRgb parses black correctly") {
    val (r, g, b) = NotationColors.hexToRgb("#000000")
    assert(r == 0.0f)
    assert(g == 0.0f)
    assert(b == 0.0f)
  }

  test("hexToRgb handles # prefix") {
    val (r1, g1, b1) = NotationColors.hexToRgb("#FF0000")
    val (r2, g2, b2) = NotationColors.hexToRgb("FF0000")
    assert(r1 == r2)
    assert(g1 == g2)
    assert(b1 == b2)
  }

  test("hexToRgb returns values in 0-1 range") {
    val colors = List(
      NotationColors.taalMarker,
      NotationColors.swar,
      NotationColors.octaveDot,
      NotationColors.ornament,
      NotationColors.stroke,
      NotationColors.sahitya
    )

    colors.foreach { color =>
      val (r, g, b) = NotationColors.hexToRgb(color)
      assert(r >= 0.0f && r <= 1.0f, s"Red component out of range for $color")
      assert(g >= 0.0f && g <= 1.0f, s"Green component out of range for $color")
      assert(b >= 0.0f && b <= 1.0f, s"Blue component out of range for $color")
    }
  }

  test("specific color values match expected hex codes") {
    assert(NotationColors.taalMarker == "#B71C1C")
    assert(NotationColors.taalMarkerSam == "#D32F2F")
    assert(NotationColors.swar == "#1A237E")
    assert(NotationColors.octaveDot == "#E65100")
    assert(NotationColors.ornament == "#4A148C")
    assert(NotationColors.stroke == "#00695C")
    assert(NotationColors.sahitya == "#2E7D32")
    assert(NotationColors.rest == "#616161")
    assert(NotationColors.sustain == "#9E9E9E")
    assert(NotationColors.komalMark == "#1A237E")
    assert(NotationColors.tivraMark == "#1A237E")
  }

  test("komal and tivra marks share same color as swar") {
    assert(NotationColors.komalMark == NotationColors.swar)
    assert(NotationColors.tivraMark == NotationColors.swar)
  }
