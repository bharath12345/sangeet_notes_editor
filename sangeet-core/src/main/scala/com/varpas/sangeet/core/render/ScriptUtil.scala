package com.varpas.sangeet.core.render

/** Shared utilities for detecting and splitting text by script (Latin vs Indic). Used by HtmlExport and other renderers
  * that need font switching.
  */
object ScriptUtil:

  def isIndicChar(ch: Char): Boolean =
    val cp = ch.toInt
    (cp >= 0x0900 && cp <= 0x097f) ||
    (cp >= 0x0980 && cp <= 0x09ff) ||
    (cp >= 0x0a80 && cp <= 0x0aff) ||
    (cp >= 0x0b00 && cp <= 0x0b7f) ||
    (cp >= 0x0b80 && cp <= 0x0bff) ||
    (cp >= 0x0c00 && cp <= 0x0c7f) ||
    (cp >= 0x0c80 && cp <= 0x0cff) ||
    (cp >= 0x0d00 && cp <= 0x0d7f) ||
    (cp >= 0xa8e0 && cp <= 0xa8ff)

  def containsNonLatin(s: String): Boolean =
    s.exists(isIndicChar)

  def splitByScript(s: String): List[(String, Boolean)] =
    if s.isEmpty then Nil
    else
      val result         = List.newBuilder[(String, Boolean)]
      val buf            = new StringBuilder
      var currentIsIndic = isIndicChar(s.head)
      s.foreach { ch =>
        val isIndic = isIndicChar(ch)
        if isIndic != currentIsIndic then
          result += ((buf.toString, currentIsIndic))
          buf.clear()
          currentIsIndic = isIndic
        buf += ch
      }
      if buf.nonEmpty then result += ((buf.toString, currentIsIndic))
      result.result()

  /** Replace characters that standard fonts cannot render with safe ASCII equivalents. */
  def sanitizeForFont(s: String): String =
    s.map {
      case '\u2014'                                  => '-'
      case '\u2013'                                  => '-'
      case '\u2018' | '\u2019'                       => '\''
      case '\u201C' | '\u201D'                       => '"'
      case '\u2026'                                  => '.'
      case '\u2020'                                  => '+'
      case ch if ch.toInt > 0xff && !isIndicChar(ch) => '?'
      case ch                                        => ch
    }.mkString
