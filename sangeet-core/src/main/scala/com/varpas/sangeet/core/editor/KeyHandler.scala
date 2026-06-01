package com.varpas.sangeet.core.editor

import com.varpas.sangeet.core.model.*

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

  /** Handle double-tap: enter two identical notes on the same beat, each half-duration. */
  def handleDualSwar(editor: CompositionEditor, key: Char,
                     shiftDown: Boolean): (CompositionEditor, String) =
    val lowerKey = key.toLower
    swarKeys.get(lowerKey) match
      case Some(note) =>
        val variant = resolveVariant(note, shiftDown)
        val octave = editor.cursor.currentOctave
        handleSwarGroup(editor, List((note, variant, octave), (note, variant, octave)))
      case None =>
        (editor, s"✗ Unknown key '$key'")

  /** Place N notes on the same beat, each with equal duration 1/N. */
  def handleSwarGroup(editor: CompositionEditor,
                      notes: List[(Note, Variant, Octave)]): (CompositionEditor, String) =
    if notes.isEmpty then (editor, "✗ No notes to group")
    else if notes.size > 4 then (editor, "✗ Maximum 4 notes per beat")
    else
      val n = notes.size
      val duration = Rational(1, n)
      val events = notes.zipWithIndex.map { case ((note, variant, octave), i) =>
        Event.Swar(note, variant, octave,
          BeatPosition(editor.cursor.cycle, editor.cursor.beat, Rational(i, n)),
          duration, None, Nil, None)
      }
      val newEditor = events.foldLeft(editor)(_.addEvent(_))
      val newCursor = editor.cursor.nextBeat.withOctave(Octave.Madhya)
      val noteNames = notes.map(_._1).mkString("")
      (newEditor.copy(cursor = newCursor), s"✓ $noteNames (${n}-swar group)")

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
        editor.removeGroupAt(editor.cursor) match
          case Some(newEditor) =>
            val newCursor = editor.cursor.prevBeat
            (newEditor.copy(cursor = newCursor), "✓ Deleted at cursor")
          case None =>
            val prev = editor.cursor.prevBeat
            if prev != editor.cursor then
              editor.removeGroupAt(prev) match
                case Some(newEditor) =>
                  (newEditor.copy(cursor = prev), "✓ Deleted before cursor")
                case None =>
                  (editor.copy(cursor = prev), "← Moved back (empty beat)")
            else
              (editor, "✗ Nothing to delete")
      case "DELETE" =>
        editor.removeGroupAt(editor.cursor) match
          case Some(newEditor) =>
            (newEditor.copy(cursor = editor.cursor), "✓ Deleted at cursor")
          case None =>
            (editor, "✗ No note at cursor to delete")
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

  def handleStroke(editor: CompositionEditor, stroke: Stroke): (CompositionEditor, String) =
    editor.modifyLastSwar(s => s.copy(stroke = Some(stroke))) match
      case Some(newEditor) =>
        (newEditor, s"✓ ${stroke} stroke added")
      case None =>
        (editor, "✗ No swar note to attach stroke to")

  def handleSimpleOrnament(editor: CompositionEditor, ornament: Ornament, name: String): (CompositionEditor, String) =
    editor.modifyLastSwar(s => s.copy(ornaments = s.ornaments :+ ornament)) match
      case Some(newEditor) =>
        (newEditor, s"✓ $name added")
      case None =>
        (editor, "✗ No swar note to attach ornament to")

  /** Handle a note ornament. Returns (editor, message, nextMode).
    * nextMode is Some if another note input is needed (e.g., Meend second note). */
  def handleNoteOrnament(editor: CompositionEditor, ornamentNote: Char, shiftDown: Boolean,
                         mode: OrnamentMode): (CompositionEditor, String, Option[OrnamentMode]) =
    val lowerKey = ornamentNote.toLower
    swarKeys.get(lowerKey) match
      case Some(note) =>
        val variant = resolveVariant(note, shiftDown)
        val noteRef = NoteRef(note, variant, Octave.Madhya)
        mode match
          case OrnamentMode.MeendStart(dir) =>
            val dirName = if dir == MeendDirection.Ascending then "↑" else "↓"
            (editor, s"◆ Meend $dirName start: ${note} — now type the end note", Some(OrnamentMode.MeendEnd(noteRef, dir)))
          case OrnamentMode.MeendEnd(startRef, dir) =>
            val ornament = Meend(startRef, noteRef, dir, Nil)
            editor.modifyLastSwar(s => s.copy(ornaments = s.ornaments :+ ornament)) match
              case Some(newEditor) =>
                (newEditor, s"✓ Meend (${startRef.note} → ${note}) added", None)
              case None =>
                (editor, "✗ No swar note to attach Meend to", None)
          case OrnamentMode.KrintanStart =>
            (editor, s"◆ Krintan start: ${note} — now type the end note", Some(OrnamentMode.KrintanEnd(noteRef)))
          case OrnamentMode.KrintanEnd(startRef) =>
            val ornament = Krintan(List(startRef, noteRef))
            editor.modifyLastSwar(s => s.copy(ornaments = s.ornaments :+ ornament)) match
              case Some(newEditor) =>
                (newEditor, s"✓ Krintan (${startRef.note} → ${note}) added", None)
              case None =>
                (editor, "✗ No swar note to attach Krintan to", None)
          case OrnamentMode.MurkiCollect(collected) =>
            val updated = collected :+ noteRef
            (editor, s"◆ Murki: ${updated.size} notes — type more or press Enter to finish",
             Some(OrnamentMode.MurkiCollect(updated)))
          case OrnamentMode.ZamzamaCollect(collected) =>
            val updated = collected :+ noteRef
            (editor, s"◆ Zamzama: ${updated.size} notes — type more or press Enter to finish",
             Some(OrnamentMode.ZamzamaCollect(updated)))
          case _ =>
            val (ornament, name) = mode match
              case OrnamentMode.KanSwar => (KanSwar(noteRef), "Kan swar")
              case OrnamentMode.Sparsh  => (Sparsh(noteRef), "Sparsh")
              case OrnamentMode.Ghaseet => (Ghaseet(noteRef), "Ghaseet")
              case _ => (KanSwar(noteRef), "Unknown")
            editor.modifyLastSwar(s => s.copy(ornaments = s.ornaments :+ ornament)) match
              case Some(newEditor) =>
                (newEditor, s"✓ $name (${note}) added", None)
              case None =>
                (editor, s"✗ No swar note to attach $name to", None)
      case None =>
        (editor, s"✗ Invalid note key '$ornamentNote' for ornament", None)

  /** Finish a multi-note ornament (Murki/Zamzama) when user presses Enter. */
  def finishMultiNoteOrnament(editor: CompositionEditor, mode: OrnamentMode): (CompositionEditor, String) =
    mode match
      case OrnamentMode.MurkiCollect(notes) if notes.nonEmpty =>
        val ornament = Murki(notes)
        editor.modifyLastSwar(s => s.copy(ornaments = s.ornaments :+ ornament)) match
          case Some(newEditor) =>
            (newEditor, s"✓ Murki (${notes.size} notes) added")
          case None =>
            (editor, "✗ No swar note to attach Murki to")
      case OrnamentMode.ZamzamaCollect(notes) if notes.nonEmpty =>
        val ornament = Zamzama(notes)
        editor.modifyLastSwar(s => s.copy(ornaments = s.ornaments :+ ornament)) match
          case Some(newEditor) =>
            (newEditor, s"✓ Zamzama (${notes.size} notes) added")
          case None =>
            (editor, "✗ No swar note to attach Zamzama to")
      case _ =>
        (editor, "✗ No notes entered for ornament")

  def charToNote(ch: Char): Option[Note] = swarKeys.get(ch.toLower)

  def resolveVariant(note: Note, shiftDown: Boolean): Variant =
    if !shiftDown then Variant.Shuddha
    else note match
      case Note.Ma => Variant.Tivra
      case Note.Sa | Note.Pa => Variant.Shuddha
      case _ => Variant.Komal
