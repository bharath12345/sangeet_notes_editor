package sangeet.editor

import sangeet.model.*
import sangeet.taal.Taals

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
      arohana = Some(List("Ni'", "Re", "Ga", "Ma†", "Dha", "Ni", "Sa'")),
      avarohana = Some(List("Sa'", "Ni", "Dha", "Pa", "Ma†", "Ga", "Re", "Sa")),
      vadi = Some("Ga"),
      samvadi = Some("Ni"),
      pakad = Some("Ni Re Ga, Re — Ga Ma† Dha Pa"),
      prahar = Some(1)
    )

    val metadata = Metadata(
      title = "Yaman Vilambit Gat — Sample",
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
    // Vilambit Gat typically has 4-8 notes per beat in some beats
    val gatEvents = List(
      // Beat 0 (Sam): Ni from mandra with meend up to Re — "da"
      swar(Note.Ni, Variant.Shuddha, Octave.Mandra, 0, 0,
        stroke = Some(Stroke.Da),
        ornaments = List(Meend(
          nr(Note.Ni, Variant.Shuddha, Octave.Mandra),
          nr(Note.Re, Variant.Shuddha, Octave.Madhya),
          MeendDirection.Ascending, Nil)),
        sahitya = Some("या")),
      // Beat 1: Re with kan swar from Sa — "ra"
      swar(Note.Re, Variant.Shuddha, Octave.Madhya, 0, 1,
        stroke = Some(Stroke.Ra),
        ornaments = List(KanSwar(nr(Note.Sa, Variant.Shuddha, Octave.Madhya))),
        sahitya = Some("मन")),
      // Beat 2: Ga with gamak — "da"
      swar(Note.Ga, Variant.Shuddha, Octave.Madhya, 0, 2,
        stroke = Some(Stroke.Da),
        ornaments = List(Gamak()),
        sahitya = Some("क")),
      // Beat 3: Ma tivra — "ra" with andolan
      swar(Note.Ma, Variant.Tivra, Octave.Madhya, 0, 3,
        stroke = Some(Stroke.Ra),
        ornaments = List(Andolan()),
        sahitya = Some("ल्या")),
      // Beat 4: Pa — sustain — "da"
      swar(Note.Pa, Variant.Shuddha, Octave.Madhya, 0, 4,
        stroke = Some(Stroke.Da),
        sahitya = Some("ण")),
      // Beat 5: Sustain
      sustain(0, 5),
      // Beat 6: Dha with sparsh from Pa — "ra"
      swar(Note.Dha, Variant.Shuddha, Octave.Madhya, 0, 6,
        stroke = Some(Stroke.Ra),
        ornaments = List(Sparsh(nr(Note.Pa, Variant.Shuddha, Octave.Madhya))),
        sahitya = Some("गु")),
      // Beat 7: Ni — "da"
      swar(Note.Ni, Variant.Shuddha, Octave.Madhya, 0, 7,
        stroke = Some(Stroke.Da),
        sahitya = Some("ण")),
      // Beat 8 (Khali): Sa taar — "ra"
      swar(Note.Sa, Variant.Shuddha, Octave.Taar, 0, 8,
        stroke = Some(Stroke.Ra),
        sahitya = Some("स")),
      // Beat 9: Re taar with krintan — "da"
      swar(Note.Re, Variant.Shuddha, Octave.Taar, 0, 9,
        stroke = Some(Stroke.Da),
        ornaments = List(Krintan(List(
          nr(Note.Re, Variant.Shuddha, Octave.Taar),
          nr(Note.Sa, Variant.Shuddha, Octave.Taar)))),
        sahitya = Some("भा")),
      // Beat 10: Ga taar — "ra" with murki
      swar(Note.Ga, Variant.Shuddha, Octave.Taar, 0, 10,
        stroke = Some(Stroke.Ra),
        ornaments = List(Murki(List(
          nr(Note.Re, Variant.Shuddha, Octave.Taar),
          nr(Note.Ga, Variant.Shuddha, Octave.Taar),
          nr(Note.Re, Variant.Shuddha, Octave.Taar)))),
        sahitya = Some("ग")),
      // Beat 11: Descending meend Ga→Re — "da"
      swar(Note.Ga, Variant.Shuddha, Octave.Madhya, 0, 11,
        stroke = Some(Stroke.Da),
        ornaments = List(Meend(
          nr(Note.Ga, Variant.Shuddha, Octave.Madhya),
          nr(Note.Re, Variant.Shuddha, Octave.Madhya),
          MeendDirection.Descending, Nil)),
        sahitya = Some("वा")),
      // Beat 12: Re — "ra"
      swar(Note.Re, Variant.Shuddha, Octave.Madhya, 0, 12,
        stroke = Some(Stroke.Ra),
        sahitya = Some("न")),
      // Beat 13: Sa — "da" with ghaseet to Re
      swar(Note.Sa, Variant.Shuddha, Octave.Madhya, 0, 13,
        stroke = Some(Stroke.Da),
        ornaments = List(Ghaseet(nr(Note.Re, Variant.Shuddha, Octave.Madhya))),
        sahitya = Some("सु")),
      // Beat 14: Ni mandra — "ra" with gitkari
      swar(Note.Ni, Variant.Shuddha, Octave.Mandra, 0, 14,
        stroke = Some(Stroke.Ra),
        ornaments = List(Gitkari()),
        sahitya = Some("र")),
      // Beat 15: Rest
      rest(0, 15)
    )

    // --- Antara section (cycle 0): upper register ---
    val antaraEvents = List(
      // Beat 0 (Sam): Pa with zamzama — "da"
      swar(Note.Pa, Variant.Shuddha, Octave.Madhya, 0, 0,
        stroke = Some(Stroke.Da),
        ornaments = List(Zamzama(List(
          nr(Note.Pa, Variant.Shuddha, Octave.Madhya),
          nr(Note.Pa, Variant.Shuddha, Octave.Madhya),
          nr(Note.Pa, Variant.Shuddha, Octave.Madhya))))),
      // Beat 1: Dha — "ra"
      swar(Note.Dha, Variant.Shuddha, Octave.Madhya, 0, 1,
        stroke = Some(Stroke.Ra)),
      // Beat 2: Ni — "da"
      swar(Note.Ni, Variant.Shuddha, Octave.Madhya, 0, 2,
        stroke = Some(Stroke.Da)),
      // Beat 3: Sa taar with meend ascending — "ra"
      swar(Note.Sa, Variant.Shuddha, Octave.Taar, 0, 3,
        stroke = Some(Stroke.Ra),
        ornaments = List(Meend(
          nr(Note.Ni, Variant.Shuddha, Octave.Madhya),
          nr(Note.Sa, Variant.Shuddha, Octave.Taar),
          MeendDirection.Ascending, Nil))),
      // Beat 4: Re taar — "da"
      swar(Note.Re, Variant.Shuddha, Octave.Taar, 0, 4,
        stroke = Some(Stroke.Da)),
      // Beat 5: Ga taar with gamak — "ra"
      swar(Note.Ga, Variant.Shuddha, Octave.Taar, 0, 5,
        stroke = Some(Stroke.Ra),
        ornaments = List(Gamak())),
      // Beat 6: Ma tivra taar — "da"
      swar(Note.Ma, Variant.Tivra, Octave.Taar, 0, 6,
        stroke = Some(Stroke.Da)),
      // Beat 7: Sustain
      sustain(0, 7),
      // Beat 8 (Khali): Descend — Ga taar with kan from Ma† — "ra"
      swar(Note.Ga, Variant.Shuddha, Octave.Taar, 0, 8,
        stroke = Some(Stroke.Ra),
        ornaments = List(KanSwar(nr(Note.Ma, Variant.Tivra, Octave.Taar)))),
      // Beat 9: Re taar — "da"
      swar(Note.Re, Variant.Shuddha, Octave.Taar, 0, 9,
        stroke = Some(Stroke.Da)),
      // Beat 10: Sa taar — "ra"
      swar(Note.Sa, Variant.Shuddha, Octave.Taar, 0, 10,
        stroke = Some(Stroke.Ra)),
      // Beat 11: Ni — "da" with andolan
      swar(Note.Ni, Variant.Shuddha, Octave.Madhya, 0, 11,
        stroke = Some(Stroke.Da),
        ornaments = List(Andolan())),
      // Beat 12: Dha — "ra"
      swar(Note.Dha, Variant.Shuddha, Octave.Madhya, 0, 12,
        stroke = Some(Stroke.Ra)),
      // Beat 13: Pa — "da"
      swar(Note.Pa, Variant.Shuddha, Octave.Madhya, 0, 13,
        stroke = Some(Stroke.Da)),
      // Beat 14: Ma tivra with sparsh — "ra"
      swar(Note.Ma, Variant.Tivra, Octave.Madhya, 0, 14,
        stroke = Some(Stroke.Ra),
        ornaments = List(Sparsh(nr(Note.Pa, Variant.Shuddha, Octave.Madhya)))),
      // Beat 15: Ga — "da"
      swar(Note.Ga, Variant.Shuddha, Octave.Madhya, 0, 15,
        stroke = Some(Stroke.Da))
    )

    // --- Taan 1 (cycle 0): fast subdivision run demonstrating grouping ---
    // Two notes per beat for a rapid ascending-descending passage
    val taan1Events = List(
      // Beat 0-3: Ascending run Ni'ReGaMa† — 2 per beat
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
      // Beat 4-7: Descending run Sa'NiDhaPa
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
      // Beat 8-11: Ascending to taar saptak — single notes
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
