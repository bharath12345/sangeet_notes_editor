package sangeet.render

/** Shared utilities for detecting and splitting text by script (Latin vs Indic).
  * Used by PdfExport, HtmlExport, and other renderers that need font switching. */
object ScriptUtil:

  def isIndicChar(ch: Char): Boolean =
    val cp = ch.toInt
    (cp >= 0x0900 && cp <= 0x097F) ||
    (cp >= 0x0980 && cp <= 0x09FF) ||
    (cp >= 0x0A80 && cp <= 0x0AFF) ||
    (cp >= 0x0B00 && cp <= 0x0B7F) ||
    (cp >= 0x0B80 && cp <= 0x0BFF) ||
    (cp >= 0x0C00 && cp <= 0x0C7F) ||
    (cp >= 0x0C80 && cp <= 0x0CFF) ||
    (cp >= 0x0D00 && cp <= 0x0D7F) ||
    (cp >= 0xA8E0 && cp <= 0xA8FF)

  def containsNonLatin(s: String): Boolean =
    s.exists(isIndicChar)

  def splitByScript(s: String): List[(String, Boolean)] =
    if s.isEmpty then Nil
    else
      val result = List.newBuilder[(String, Boolean)]
      val buf = new StringBuilder
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

  /** Replace characters that standard PDF fonts cannot render with safe ASCII equivalents. */
  def sanitizeForFont(s: String): String =
    s.map {
      case '\u2014' => '-'
      case '\u2013' => '-'
      case '\u2018' | '\u2019' => '\''
      case '\u201C' | '\u201D' => '"'
      case '\u2026' => '.'
      case '\u2020' => '+'
      case ch if ch.toInt > 0xFF && !isIndicChar(ch) => '?'
      case ch => ch
    }.mkString
