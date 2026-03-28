package sangeet.editor

import sangeet.model.*

object KeyHandler:

  private val swarKeys: Map[Char, Note] = Map(
    's' -> Note.Sa, 'r' -> Note.Re, 'g' -> Note.Ga,
    'm' -> Note.Ma, 'p' -> Note.Pa, 'd' -> Note.Dha, 'n' -> Note.Ni
  )

  /** Returns (newEditor, statusMessage) */
  def handleSwarKey(editor: CompositionEditor, key: Char,
                    shiftDown: Boolean): (CompositionEditor, String) =
    val lowerKey = key.toLower
    swarKeys.get(lowerKey) match
      case Some(note) =>
        val variant = resolveVariant(note, shiftDown)
        val octave = editor.cursor.currentOctave
        val event = Event.Swar(
          note = note,
          variant = variant,
          octave = octave,
          beat = editor.cursor.position,
          duration = Rational(1, editor.cursor.totalSubdivisions),
          stroke = None,
          ornaments = Nil,
          sahitya = None
        )
        val newEditor = editor.addEvent(event)
        val newCursor = editor.cursor.nextSubBeat.withOctave(Octave.Madhya)
        val variantStr = variant match
          case Variant.Komal => " komal"
          case Variant.Tivra => " tivra"
          case _ => ""
        val octaveStr = octave match
          case Octave.Mandra => " (mandra)"
          case Octave.Taar => " (taar)"
          case Octave.AtiMandra => " (ati-mandra)"
          case Octave.AtiTaar => " (ati-taar)"
          case _ => ""
        (newEditor.copy(cursor = newCursor), s"✓ ${note}${variantStr}${octaveStr}")
      case None =>
        (editor, s"✗ Unknown key '$key' — use s/r/g/m/p/d/n for swar notes")

  def handleSpecialKey(editor: CompositionEditor, keyName: String): (CompositionEditor, String) =
    keyName match
      case "SPACE" =>
        val event = Event.Rest(editor.cursor.position, Rational.fullBeat)
        val newEditor = editor.addEvent(event)
        val newCursor = editor.cursor.nextBeat
        (newEditor.copy(cursor = newCursor), "✓ Rest (silence)")
      case "MINUS" =>
        val event = Event.Sustain(editor.cursor.position, Rational.fullBeat)
        val newEditor = editor.addEvent(event)
        val newCursor = editor.cursor.nextBeat
        (newEditor.copy(cursor = newCursor), "✓ Sustain (hold previous note)")
      case "BACKSPACE" =>
        editor.removeLastEvent match
          case Some(newEditor) =>
            val newCursor = editor.cursor.prevBeat
            (newEditor.copy(cursor = newCursor), "✓ Deleted last note")
          case None =>
            (editor, "✗ Nothing to delete")
      case _ => (editor, s"✗ Unhandled key: $keyName")

  def handleOctaveKey(editor: CompositionEditor, keyName: String): (CompositionEditor, String) =
    keyName match
      case "PERIOD" =>
        (editor.copy(cursor = editor.cursor.withOctave(Octave.Mandra)),
         "◆ Next note in Mandra saptak (lower octave)")
      case "QUOTE" =>
        (editor.copy(cursor = editor.cursor.withOctave(Octave.Taar)),
         "◆ Next note in Taar saptak (upper octave)")
      case "BACKTICK" =>
        (editor.copy(cursor = editor.cursor.withOctave(Octave.Madhya)),
         "◆ Back to Madhya saptak (default octave)")
      case _ => (editor, s"✗ Unhandled octave key: $keyName")

  def handleSubdivision(editor: CompositionEditor, n: Int): CompositionEditor =
    editor.copy(cursor = editor.cursor.withSubdivisions(n))

  private def resolveVariant(note: Note, shiftDown: Boolean): Variant =
    if !shiftDown then Variant.Shuddha
    else note match
      case Note.Ma => Variant.Tivra
      case Note.Sa | Note.Pa => Variant.Shuddha
      case _ => Variant.Komal
