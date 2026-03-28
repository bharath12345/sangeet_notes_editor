package sangeet.editor

import sangeet.model.*

object KeyHandler:

  private val swarKeys: Map[Char, Note] = Map(
    's' -> Note.Sa, 'r' -> Note.Re, 'g' -> Note.Ga,
    'm' -> Note.Ma, 'p' -> Note.Pa, 'd' -> Note.Dha, 'n' -> Note.Ni
  )

  def handleSwarKey(editor: CompositionEditor, key: Char,
                    shiftDown: Boolean): (CompositionEditor, CursorModel) =
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
        val newCursor = editor.cursor.nextSubBeat
        (newEditor.copy(cursor = newCursor), newCursor)
      case None =>
        (editor, editor.cursor)

  def handleSpecialKey(editor: CompositionEditor, keyName: String): CompositionEditor =
    keyName match
      case "SPACE" =>
        val event = Event.Rest(editor.cursor.position, Rational.fullBeat)
        val newEditor = editor.addEvent(event)
        val newCursor = editor.cursor.nextBeat
        newEditor.copy(cursor = newCursor)
      case "MINUS" =>
        val event = Event.Sustain(editor.cursor.position, Rational.fullBeat)
        val newEditor = editor.addEvent(event)
        val newCursor = editor.cursor.nextBeat
        newEditor.copy(cursor = newCursor)
      case _ => editor

  def handleOctaveKey(editor: CompositionEditor, keyName: String): CompositionEditor =
    keyName match
      case "PERIOD" =>
        editor.copy(cursor = editor.cursor.withOctave(Octave.Mandra))
      case "QUOTE" =>
        editor.copy(cursor = editor.cursor.withOctave(Octave.Taar))
      case _ => editor

  def handleSubdivision(editor: CompositionEditor, n: Int): CompositionEditor =
    editor.copy(cursor = editor.cursor.withSubdivisions(n))

  private def resolveVariant(note: Note, shiftDown: Boolean): Variant =
    if !shiftDown then Variant.Shuddha
    else note match
      case Note.Ma => Variant.Tivra
      case Note.Sa | Note.Pa => Variant.Shuddha
      case _ => Variant.Komal
