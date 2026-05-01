package com.varpas.sangeet.desktop.editor

import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.taal.Taals

/** A rich sample Yaman Vilambit Gat that demonstrates all editor features.
  * Loaded on startup as a read-only showcase. */
object SampleComposition:

  private def swar(note: Note, variant: Variant, octave: Octave,
                   cycle: Int, beat: Int, sub: Rational = Rational.onBeat,
                   stroke: Option[Stroke] = None,
                   ornaments: List[Ornament] = Nil,
                   sahitya: Option[String] = None): Event.Swar =
    Event.Swar(note, variant, octave,
      BeatPosition(cycle, beat, sub), Rational.fullBeat, stroke, ornaments, sahitya)

  private def rest(cycle: Int, beat: Int, sub: Rational = Rational.onBeat): Event.Rest =
    Event.Rest(BeatPosition(cycle, beat, sub), Rational.fullBeat)

  private def sustain(cycle: Int, beat: Int, sub: Rational = Rational.onBeat): Event.Sustain =
    Event.Sustain(BeatPosition(cycle, beat, sub), Rational.fullBeat)

  private val nr = NoteRef.apply

  def build(): Composition =
    val raag = Raag(
      name = "Yaman",
      thaat = Some("Kalyan"),
      arohana = Some(List("Ni'", "Re", "Ga", "Ma\u2020", "Dha", "Ni", "Sa'")),
      avarohana = Some(List("Sa'", "Ni", "Dha", "Pa", "Ma\u2020", "Ga", "Re", "Sa")),
      vadi = Some("Ga"),
      samvadi = Some("Ni"),
      pakad = Some("Ni Re Ga, Re -- Ga Ma\u2020 Dha Pa"),
      prahar = Some(1)
    )

    val metadata = Metadata(
      title = "Yaman Vilambit Gat -- Sample",
      compositionType = CompositionType.Gat,
      raag = raag,
      taal = Taals.teentaal,
      laya = Some(Laya.Vilambit),
      instrument = Some("Sitar"),
      composer = Some("Traditional"),
      author = None,
      source = Some("Sample composition"),
      showStrokeLine = true,
      showSahityaLine = true,
      createdAt = "2026-03-29T00:00:00Z",
      updatedAt = "2026-03-29T00:00:00Z"
    )

    // --- Gat section (cycle 0): melodic theme with varied ornaments ---
    val gatEvents = List(
      // Beat 0 (Sam): Ni from mandra with meend up to Re
      swar(Note.Ni, Variant.Shuddha, Octave.Mandra, 0, 0,
        stroke = Some(Stroke.Da),
        ornaments = List(Meend(
          nr(Note.Ni, Variant.Shuddha, Octave.Mandra),
          nr(Note.Re, Variant.Shuddha, Octave.Madhya),
          MeendDirection.Ascending, Nil)),
        sahitya = Some("\u092f\u093e")),
      // Beat 1: Re with kan swar from Sa
      swar(Note.Re, Variant.Shuddha, Octave.Madhya, 0, 1,
        stroke = Some(Stroke.Ra),
        ornaments = List(KanSwar(nr(Note.Sa, Variant.Shuddha, Octave.Madhya))),
        sahitya = Some("\u092e\u0928")),
      // Beat 2: Ga with gamak
      swar(Note.Ga, Variant.Shuddha, Octave.Madhya, 0, 2,
        stroke = Some(Stroke.Da),
        ornaments = List(Gamak()),
        sahitya = Some("\u0915")),
      // Beat 3: Ma tivra with andolan
      swar(Note.Ma, Variant.Tivra, Octave.Madhya, 0, 3,
        stroke = Some(Stroke.Ra),
        ornaments = List(Andolan()),
        sahitya = Some("\u0932\u094d\u092f\u093e")),
      // Beat 4: Pa
      swar(Note.Pa, Variant.Shuddha, Octave.Madhya, 0, 4,
        stroke = Some(Stroke.Da),
        sahitya = Some("\u0923")),
      // Beat 5: Sustain
      sustain(0, 5),
      // Beat 6: Dha with sparsh from Pa
      swar(Note.Dha, Variant.Shuddha, Octave.Madhya, 0, 6,
        stroke = Some(Stroke.Ra),
        ornaments = List(Sparsh(nr(Note.Pa, Variant.Shuddha, Octave.Madhya))),
        sahitya = Some("\u0917\u0941")),
      // Beat 7: Ni
      swar(Note.Ni, Variant.Shuddha, Octave.Madhya, 0, 7,
        stroke = Some(Stroke.Da),
        sahitya = Some("\u0923")),
      // Beat 8 (Khali): Sa taar
      swar(Note.Sa, Variant.Shuddha, Octave.Taar, 0, 8,
        stroke = Some(Stroke.Ra),
        sahitya = Some("\u0938")),
      // Beat 9: Re taar with krintan
      swar(Note.Re, Variant.Shuddha, Octave.Taar, 0, 9,
        stroke = Some(Stroke.Da),
        ornaments = List(Krintan(List(
          nr(Note.Re, Variant.Shuddha, Octave.Taar),
          nr(Note.Sa, Variant.Shuddha, Octave.Taar)))),
        sahitya = Some("\u092d\u093e")),
      // Beat 10: Ga taar with murki
      swar(Note.Ga, Variant.Shuddha, Octave.Taar, 0, 10,
        stroke = Some(Stroke.Ra),
        ornaments = List(Murki(List(
          nr(Note.Re, Variant.Shuddha, Octave.Taar),
          nr(Note.Ga, Variant.Shuddha, Octave.Taar),
          nr(Note.Re, Variant.Shuddha, Octave.Taar)))),
        sahitya = Some("\u0917")),
      // Beat 11: Descending meend Ga->Re
      swar(Note.Ga, Variant.Shuddha, Octave.Madhya, 0, 11,
        stroke = Some(Stroke.Da),
        ornaments = List(Meend(
          nr(Note.Ga, Variant.Shuddha, Octave.Madhya),
          nr(Note.Re, Variant.Shuddha, Octave.Madhya),
          MeendDirection.Descending, Nil)),
        sahitya = Some("\u0935\u093e")),
      // Beat 12: Re
      swar(Note.Re, Variant.Shuddha, Octave.Madhya, 0, 12,
        stroke = Some(Stroke.Ra),
        sahitya = Some("\u0928")),
      // Beat 13: Sa with ghaseet to Re
      swar(Note.Sa, Variant.Shuddha, Octave.Madhya, 0, 13,
        stroke = Some(Stroke.Da),
        ornaments = List(Ghaseet(nr(Note.Re, Variant.Shuddha, Octave.Madhya))),
        sahitya = Some("\u0938\u0941")),
      // Beat 14: Ni mandra with gitkari
      swar(Note.Ni, Variant.Shuddha, Octave.Mandra, 0, 14,
        stroke = Some(Stroke.Ra),
        ornaments = List(Gitkari()),
        sahitya = Some("\u0930")),
      // Beat 15: Rest
      rest(0, 15)
    )

    // --- Antara section (cycle 0): upper register ---
    val antaraEvents = List(
      // Beat 0 (Sam): Pa with zamzama
      swar(Note.Pa, Variant.Shuddha, Octave.Madhya, 0, 0,
        stroke = Some(Stroke.Da),
        ornaments = List(Zamzama(List(
          nr(Note.Pa, Variant.Shuddha, Octave.Madhya),
          nr(Note.Pa, Variant.Shuddha, Octave.Madhya),
          nr(Note.Pa, Variant.Shuddha, Octave.Madhya))))),
      // Beat 1: Dha
      swar(Note.Dha, Variant.Shuddha, Octave.Madhya, 0, 1,
        stroke = Some(Stroke.Ra)),
      // Beat 2: Ni
      swar(Note.Ni, Variant.Shuddha, Octave.Madhya, 0, 2,
        stroke = Some(Stroke.Da)),
      // Beat 3: Sa taar with meend ascending
      swar(Note.Sa, Variant.Shuddha, Octave.Taar, 0, 3,
        stroke = Some(Stroke.Ra),
        ornaments = List(Meend(
          nr(Note.Ni, Variant.Shuddha, Octave.Madhya),
          nr(Note.Sa, Variant.Shuddha, Octave.Taar),
          MeendDirection.Ascending, Nil))),
      // Beat 4: Re taar
      swar(Note.Re, Variant.Shuddha, Octave.Taar, 0, 4,
        stroke = Some(Stroke.Da)),
      // Beat 5: Ga taar with gamak
      swar(Note.Ga, Variant.Shuddha, Octave.Taar, 0, 5,
        stroke = Some(Stroke.Ra),
        ornaments = List(Gamak())),
      // Beat 6: Ma tivra taar
      swar(Note.Ma, Variant.Tivra, Octave.Taar, 0, 6,
        stroke = Some(Stroke.Da)),
      // Beat 7: Sustain
      sustain(0, 7),
      // Beat 8 (Khali): Ga taar with kan from Ma tivra
      swar(Note.Ga, Variant.Shuddha, Octave.Taar, 0, 8,
        stroke = Some(Stroke.Ra),
        ornaments = List(KanSwar(nr(Note.Ma, Variant.Tivra, Octave.Taar)))),
      // Beat 9: Re taar
      swar(Note.Re, Variant.Shuddha, Octave.Taar, 0, 9,
        stroke = Some(Stroke.Da)),
      // Beat 10: Sa taar
      swar(Note.Sa, Variant.Shuddha, Octave.Taar, 0, 10,
        stroke = Some(Stroke.Ra)),
      // Beat 11: Ni with andolan
      swar(Note.Ni, Variant.Shuddha, Octave.Madhya, 0, 11,
        stroke = Some(Stroke.Da),
        ornaments = List(Andolan())),
      // Beat 12: Dha
      swar(Note.Dha, Variant.Shuddha, Octave.Madhya, 0, 12,
        stroke = Some(Stroke.Ra)),
      // Beat 13: Pa
      swar(Note.Pa, Variant.Shuddha, Octave.Madhya, 0, 13,
        stroke = Some(Stroke.Da)),
      // Beat 14: Ma tivra with sparsh
      swar(Note.Ma, Variant.Tivra, Octave.Madhya, 0, 14,
        stroke = Some(Stroke.Ra),
        ornaments = List(Sparsh(nr(Note.Pa, Variant.Shuddha, Octave.Madhya)))),
      // Beat 15: Ga
      swar(Note.Ga, Variant.Shuddha, Octave.Madhya, 0, 15,
        stroke = Some(Stroke.Da))
    )

    // --- Taan 1 (cycle 0): fast subdivision run ---
    val taan1Events = List(
      // Beat 0-3: Ascending run -- 2 per beat
      swar(Note.Ni, Variant.Shuddha, Octave.Mandra, 0, 0,
        sub = Rational(0, 2), stroke = Some(Stroke.Da)),
      swar(Note.Re, Variant.Shuddha, Octave.Madhya, 0, 0,
        sub = Rational(1, 2), stroke = Some(Stroke.Ra)),
      swar(Note.Ga, Variant.Shuddha, Octave.Madhya, 0, 1,
        sub = Rational(0, 2), stroke = Some(Stroke.Da)),
      swar(Note.Ma, Variant.Tivra, Octave.Madhya, 0, 1,
        sub = Rational(1, 2), stroke = Some(Stroke.Ra)),
      swar(Note.Pa, Variant.Shuddha, Octave.Madhya, 0, 2,
        sub = Rational(0, 2), stroke = Some(Stroke.Da)),
      swar(Note.Dha, Variant.Shuddha, Octave.Madhya, 0, 2,
        sub = Rational(1, 2), stroke = Some(Stroke.Ra)),
      swar(Note.Ni, Variant.Shuddha, Octave.Madhya, 0, 3,
        sub = Rational(0, 2), stroke = Some(Stroke.Da)),
      swar(Note.Sa, Variant.Shuddha, Octave.Taar, 0, 3,
        sub = Rational(1, 2), stroke = Some(Stroke.Ra)),
      // Beat 4-7: Descending run
      swar(Note.Sa, Variant.Shuddha, Octave.Taar, 0, 4,
        sub = Rational(0, 2), stroke = Some(Stroke.Da)),
      swar(Note.Ni, Variant.Shuddha, Octave.Madhya, 0, 4,
        sub = Rational(1, 2), stroke = Some(Stroke.Ra)),
      swar(Note.Dha, Variant.Shuddha, Octave.Madhya, 0, 5,
        sub = Rational(0, 2), stroke = Some(Stroke.Da)),
      swar(Note.Pa, Variant.Shuddha, Octave.Madhya, 0, 5,
        sub = Rational(1, 2), stroke = Some(Stroke.Ra)),
      swar(Note.Ma, Variant.Tivra, Octave.Madhya, 0, 6,
        sub = Rational(0, 2), stroke = Some(Stroke.Da)),
      swar(Note.Ga, Variant.Shuddha, Octave.Madhya, 0, 6,
        sub = Rational(1, 2), stroke = Some(Stroke.Ra)),
      swar(Note.Re, Variant.Shuddha, Octave.Madhya, 0, 7,
        sub = Rational(0, 2), stroke = Some(Stroke.Da)),
      swar(Note.Sa, Variant.Shuddha, Octave.Madhya, 0, 7,
        sub = Rational(1, 2), stroke = Some(Stroke.Ra)),
      // Beat 8-11: Ascending -- single notes
      swar(Note.Ni, Variant.Shuddha, Octave.Mandra, 0, 8,
        stroke = Some(Stroke.Da)),
      swar(Note.Re, Variant.Shuddha, Octave.Madhya, 0, 9,
        stroke = Some(Stroke.Ra)),
      swar(Note.Ga, Variant.Shuddha, Octave.Madhya, 0, 10,
        stroke = Some(Stroke.Da)),
      swar(Note.Ma, Variant.Tivra, Octave.Madhya, 0, 11,
        stroke = Some(Stroke.Ra)),
      // Beat 12-15: Landing phrase
      swar(Note.Dha, Variant.Shuddha, Octave.Madhya, 0, 12,
        stroke = Some(Stroke.Da)),
      swar(Note.Ni, Variant.Shuddha, Octave.Madhya, 0, 13,
        stroke = Some(Stroke.Ra)),
      swar(Note.Sa, Variant.Shuddha, Octave.Taar, 0, 14,
        stroke = Some(Stroke.Da)),
      rest(0, 15)
    )

    val sections = List(
      Section("Gat", SectionType.Custom("Gat"), gatEvents),
      Section("Antara", SectionType.Antara, antaraEvents),
      Section("Taan 1", SectionType.Taan, taan1Events)
    )

    Composition(metadata = metadata, sections = sections)
