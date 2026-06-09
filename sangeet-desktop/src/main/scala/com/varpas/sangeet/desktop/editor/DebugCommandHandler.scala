package com.varpas.sangeet.desktop.editor

import com.varpas.sangeet.core.editor._
import com.varpas.sangeet.core.model.{Andolan, Gamak, Gitkari, MeendDirection, _}

class DebugCommandHandler(pane: EditorPane, statusBar: StatusBar):

  private def withEditor(f: CompositionEditor => String): String =
    pane.getEditor match
      case None     => "ERROR: no composition loaded"
      case Some(ed) => f(ed)

  private def withWritableEditor(f: CompositionEditor => String): String =
    withEditor { ed =>
      if pane.isReadOnly then "ERROR: editor is read-only"
      else f(ed)
    }

  private def pushAndRefresh(ed: CompositionEditor, msg: String): Unit =
    statusBar.log(msg)
    pane.pushEditorState(ed)
    pane.resetCursorBlink()
    pane.redraw()

  def typeChar(ch: Char): String =
    withWritableEditor { ed =>
      if ch == '1' then
        val (newEd, msg) = KeyHandler.handleChikariKey(ed)
        pushAndRefresh(newEd, msg)
        msg
      else
        val (newEd, msg) = KeyHandler.handleSwarKey(ed, ch, ch.isUpper)
        if newEd ne ed then pushAndRefresh(newEd, msg)
        msg
    }

  def pressKey(keyName: String): String =
    withWritableEditor { ed =>
      keyName.toUpperCase match
        case k @ ("SPACE" | "BACKSPACE" | "DELETE" | "MINUS") =>
          val (newEd, msg) = KeyHandler.handleSpecialKey(ed, k)
          pushAndRefresh(newEd, msg)
          msg
        case "LEFT" =>
          val newCursor = ed.cursor.prevBeat
          pane.setEditorDirectState(ed.copy(cursor = newCursor))
          pane.resetCursorBlink()
          pane.redraw()
          s"Cursor back: cycle=${newCursor.cycle} beat=${newCursor.beat}"
        case "RIGHT" =>
          val next = ed.cursor.nextBeat
          if next.cycle <= ed.maxCycle + 1 then
            pane.setEditorDirectState(ed.copy(cursor = next))
            pane.resetCursorBlink()
            pane.redraw()
            s"Cursor forward: cycle=${next.cycle} beat=${next.beat}"
          else "At end -- cannot advance"
        case other =>
          s"ERROR: unknown key '$other' -- use space, backspace, delete, minus, left, right"
    }

  def octaveKey(keyName: String): String =
    withEditor { ed =>
      keyName.toUpperCase match
        case k @ ("PERIOD" | "QUOTE" | "BACKTICK") =>
          val (newEd, msg) = KeyHandler.handleOctaveKey(ed, k)
          pane.setEditorDirectState(newEd)
          pane.redraw()
          msg
        case other =>
          s"ERROR: unknown octave key '$other' -- use period, quote, backtick"
    }

  def subdivision(n: Int): String =
    withEditor { ed =>
      if n < 1 || n > 8 then s"ERROR: subdivision must be 1-8 (got $n)"
      else
        pane.setEditorDirectState(KeyHandler.handleSubdivision(ed, n))
        s"Subdivision set to $n"
    }

  def dualSwar(ch: Char): String =
    withWritableEditor { ed =>
      val (newEd, msg) = KeyHandler.handleDualSwar(ed, ch, ch.isUpper)
      pushAndRefresh(newEd, msg)
      msg
    }

  def typeTimed(entries: List[(Char, Long)]): String =
    if entries.isEmpty then return "ERROR: no entries"
    val results = entries.map { (ch, delayMs) =>
      pane.typeCharTimed(ch, delayMs)
    }
    results.mkString("; ")

  def swarGroup(chars: String): String =
    withWritableEditor { ed =>
      val notes = chars.toList.flatMap { ch =>
        KeyHandler.charToNote(ch).map { note =>
          (note, KeyHandler.resolveVariant(note, ch.isUpper), Octave.Madhya)
        }
      }
      if notes.isEmpty then "ERROR: no valid swar keys"
      else if notes.size > 4 then "ERROR: max 4 notes per group"
      else
        val (newEd, msg) = KeyHandler.handleSwarGroup(ed, notes)
        pushAndRefresh(newEd, msg)
        msg
    }

  def stroke(strokeName: String): String =
    withEditor { ed =>
      val strokeOpt = strokeName.toLowerCase match
        case "da"  => Some(Stroke.Da)
        case "ra"  => Some(Stroke.Ra)
        case "jod" => Some(Stroke.Jod)
        case _     => None
      strokeOpt match
        case None => s"ERROR: unknown stroke '$strokeName' -- use da, ra, jod"
        case Some(s) =>
          val (newEd, msg) = KeyHandler.handleStroke(ed, s)
          if newEd ne ed then
            pane.pushEditorState(newEd)
            pane.redraw()
          msg
    }

  def simpleOrnament(ornamentName: String): String =
    withEditor { ed =>
      val ornResult: Option[(Ornament, String)] = ornamentName.toLowerCase match
        case "gamak"   => Some((Gamak(), "Gamak"))
        case "andolan" => Some((Andolan(), "Andolan"))
        case "gitkari" => Some((Gitkari(), "Gitkari"))
        case _         => None
      ornResult match
        case None => s"ERROR: unknown ornament '$ornamentName' -- use gamak, andolan, gitkari"
        case Some((ornament, name)) =>
          val (newEd, msg) = KeyHandler.handleSimpleOrnament(ed, ornament, name)
          if newEd ne ed then
            pane.pushEditorState(newEd)
            pane.redraw()
          msg
    }

  def ornamentStart(modeName: String): String =
    modeName.toLowerCase match
      case "kanswar" => pane.setOrnamentMode(Some(OrnamentMode.KanSwar)); "KanSwar mode: type note"
      case "sparsh"  => pane.setOrnamentMode(Some(OrnamentMode.Sparsh)); "Sparsh mode: type note"
      case "ghaseet" => pane.setOrnamentMode(Some(OrnamentMode.Ghaseet)); "Ghaseet mode: type note"
      case "meend-asc" =>
        pane.setOrnamentMode(Some(OrnamentMode.MeendStart(MeendDirection.Ascending)));
        "Meend ascending: type start note"
      case "meend-desc" =>
        pane.setOrnamentMode(Some(OrnamentMode.MeendStart(MeendDirection.Descending)));
        "Meend descending: type start note"
      case "krintan" => pane.setOrnamentMode(Some(OrnamentMode.KrintanStart)); "Krintan: type start note"
      case "murki" =>
        pane.setOrnamentMode(Some(OrnamentMode.MurkiCollect(Nil))); "Murki collect: type notes, then finish-ornament"
      case "zamzama" =>
        pane.setOrnamentMode(Some(OrnamentMode.ZamzamaCollect(Nil)));
        "Zamzama collect: type notes, then finish-ornament"
      case other => s"ERROR: unknown mode '$other'"

  def ornamentNote(ch: Char): String =
    withEditor { ed =>
      pane.getOrnamentMode match
        case None => "ERROR: not in ornament mode -- use ornament-start first"
        case Some(mode) =>
          val (newEd, msg, nextMode) = KeyHandler.handleNoteOrnament(ed, ch, ch.isUpper, mode)
          if newEd ne ed then pane.pushEditorState(newEd) else pane.setEditorDirectState(newEd)
          pane.setOrnamentMode(nextMode)
          pane.redraw()
          msg
    }

  def finishOrnament(): String =
    withEditor { ed =>
      pane.getOrnamentMode match
        case None => "ERROR: not in ornament mode"
        case Some(mode) =>
          val (newEd, msg) = KeyHandler.finishMultiNoteOrnament(ed, mode)
          if newEd ne ed then pane.pushEditorState(newEd)
          pane.setOrnamentMode(None)
          pane.redraw()
          msg
    }

  def switchSection(idx: Int): String =
    withEditor { ed =>
      if idx < 0 || idx >= ed.composition.sections.size then
        s"ERROR: section index $idx out of range (0 to ${ed.composition.sections.size - 1})"
      else
        val newEd = ed.copy(currentSectionIndex = idx, cursor = CursorModel(ed.composition.metadata.taal))
        pane.setEditorDirectState(newEd)
        pane.redraw()
        s"Switched to section $idx: ${ed.composition.sections(idx).name}"
    }

  def changeTaal(taalName: String): String =
    withWritableEditor { ed =>
      import com.varpas.sangeet.core.taal.Taals
      Taals.byName(taalName) match
        case None => s"ERROR: unknown taal '$taalName'"
        case Some(newTaal) =>
          if newTaal.name == ed.composition.metadata.taal.name then s"Taal unchanged: ${newTaal.name}"
          else
            val newEd = ed.changeTaal(newTaal)
            pushAndRefresh(newEd, s"Taal changed to ${newTaal.name} (${newTaal.matras} matras)")
            s"Taal changed to ${newTaal.name} (${newTaal.matras} matras)"
    }

  def resetComposition(compType: String = "gat", taalName: String = "teentaal", taanCount: Int = 0): String =
    import com.varpas.sangeet.core.taal.Taals
    val taal = taalName.toLowerCase match
      case "teentaal" => Taals.teentaal
      case "jhaptaal" => Taals.jhaptaal
      case "rupak"    => Taals.rupak
      case "ektaal"   => Taals.ektaal
      case "dadra"    => Taals.dadra
      case "keherwa"  => Taals.keherwa
      case other      => return s"ERROR: unknown taal '$other'"
    val ct = compType.toLowerCase match
      case "gat"     => CompositionType.Gat
      case "bandish" => CompositionType.Bandish
      case "palta"   => CompositionType.Palta
      case "sargam"  => CompositionType.Sargam
      case other     => return s"ERROR: unknown type '$other'"
    val raag = Raag("Yaman", None, None, None, None, None, None, None)
    val ed = CompositionEditor.create(
      title = "Debug Test",
      compositionType = ct,
      taal = taal,
      raag = raag,
      laya = if ct == CompositionType.Palta then None else Some(Laya.Madhya),
      taanCount = taanCount
    )
    pane.setEditor(ed)
    pane.setReadOnly(false)
    pane.setOrnamentMode(None)
    s"Reset: ${ct} ${taal.name} (${ed.composition.sections.size} sections)"
