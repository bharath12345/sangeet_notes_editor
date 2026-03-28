package sangeet.render

import sangeet.model.*

object ScriptMap:

  private val devanagari: Map[Note, String] = Map(
    Note.Sa  -> "सा",
    Note.Re  -> "रे",
    Note.Ga  -> "ग",
    Note.Ma  -> "म",
    Note.Pa  -> "प",
    Note.Dha -> "ध",
    Note.Ni  -> "नि"
  )

  private val kannada: Map[Note, String] = Map(
    Note.Sa  -> "ಸಾ",
    Note.Re  -> "ರಿ",
    Note.Ga  -> "ಗ",
    Note.Ma  -> "ಮ",
    Note.Pa  -> "ಪ",
    Note.Dha -> "ಧ",
    Note.Ni  -> "ನಿ"
  )

  private val telugu: Map[Note, String] = Map(
    Note.Sa  -> "స",
    Note.Re  -> "రి",
    Note.Ga  -> "గ",
    Note.Ma  -> "మ",
    Note.Pa  -> "ప",
    Note.Dha -> "ధ",
    Note.Ni  -> "ని"
  )

  private val english: Map[Note, String] = Map(
    Note.Sa  -> "Sa",
    Note.Re  -> "Re",
    Note.Ga  -> "Ga",
    Note.Ma  -> "Ma",
    Note.Pa  -> "Pa",
    Note.Dha -> "Dha",
    Note.Ni  -> "Ni"
  )

  private val glyphMaps: Map[SwarScript, Map[Note, String]] = Map(
    SwarScript.Devanagari -> devanagari,
    SwarScript.Kannada    -> kannada,
    SwarScript.Telugu     -> telugu,
    SwarScript.English    -> english
  )

  def glyph(note: Note, script: SwarScript): String =
    glyphMaps(script)(note)

  def fontName(script: SwarScript): String = script match
    case SwarScript.Devanagari => "Noto Sans Devanagari"
    case SwarScript.Kannada    => "Noto Sans Kannada"
    case SwarScript.Telugu     => "Noto Sans Telugu"
    case SwarScript.English    => "System"

  def legendEntries(script: SwarScript): List[(String, String, String)] =
    val g = glyphMaps(script)
    List(
      ("s", s"Sa (${g(Note.Sa)})", ""),
      ("r", s"Re (${g(Note.Re)})", "shuddha"),
      ("R", s"Re (${g(Note.Re)})", "komal"),
      ("g", s"Ga (${g(Note.Ga)})", "shuddha"),
      ("G", s"Ga (${g(Note.Ga)})", "komal"),
      ("m", s"Ma (${g(Note.Ma)})", "shuddha"),
      ("M", s"Ma (${g(Note.Ma)})", "tivra"),
      ("p", s"Pa (${g(Note.Pa)})", ""),
      ("d", s"Dha (${g(Note.Dha)})", "shuddha"),
      ("D", s"Dha (${g(Note.Dha)})", "komal"),
      ("n", s"Ni (${g(Note.Ni)})", "shuddha"),
      ("N", s"Ni (${g(Note.Ni)})", "komal"),
    )

  def displayName(script: SwarScript): String = script match
    case SwarScript.Devanagari => "Devanagari (Hindi)"
    case SwarScript.Kannada    => "Kannada"
    case SwarScript.Telugu     => "Telugu"
    case SwarScript.English    => "English"
