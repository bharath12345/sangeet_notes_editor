package com.varpas.sangeet.core.generators

import org.scalacheck.{Arbitrary, Gen}

import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.taal.Taals

/** Canonical ScalaCheck generator module for the sangeet-core domain.
  *
  * This is the source-of-truth set of generators for the entire Scala side of the codebase. sangeet-server and
  * sangeet-desktop test sources import from here rather than redefining domain generators of their own.
  *
  * See `docs/developer/testing/property-based-testing.md` for conventions.
  *
  * Sizing notes (kept intentionally small so PR-time CI stays under budget — see plan 19 CI policy):
  *   - Compositions have 0–3 sections.
  *   - Sections have 0–16 events.
  *   - Ornament lists per swar are 0–2.
  *   - Strings (titles, custom names, sahitya) cap at 16 chars and use a safe alphabet.
  *
  * Achal-swar rule: Sa and Pa never carry komal or tivra variants. This is encoded in `genSwar` and `genNoteRef` so
  * generated data is musically valid by construction; tests for the rule itself can still build invalid examples by
  * hand.
  */
object Generators:

  // ---- Small string alphabet for human-readable fields ------------------------------------

  private val safeChar: Gen[Char] =
    Gen.frequency(
      26 -> Gen.alphaLowerChar,
      26 -> Gen.alphaUpperChar,
      10 -> Gen.numChar,
      4  -> Gen.const(' '),
      2  -> Gen.const('-')
    )

  /** Non-empty, trim-friendly identifier-like string. Avoids JSON-hostile characters. */
  val genShortText: Gen[String] =
    for
      n  <- Gen.choose(1, 16)
      cs <- Gen.listOfN(n, safeChar)
    yield cs.mkString.trim match
      case "" => "x"
      case s  => s

  val genOptShortText: Gen[Option[String]] = Gen.option(genShortText)

  // ---- Primitive model enums --------------------------------------------------------------

  val genNote: Gen[Note] = Gen.oneOf(Note.values.toIndexedSeq)

  val genVariant: Gen[Variant] = Gen.oneOf(Variant.values.toIndexedSeq)

  val genOctave: Gen[Octave] = Gen.oneOf(Octave.values.toIndexedSeq)

  val genStroke: Gen[Stroke] = Gen.oneOf(Stroke.values.toIndexedSeq)

  val genLaya: Gen[Laya] = Gen.oneOf(Laya.values.toIndexedSeq)

  val genMeendDirection: Gen[MeendDirection] = Gen.oneOf(MeendDirection.values.toIndexedSeq)

  val genSwarScript: Gen[SwarScript] = Gen.oneOf(SwarScript.values.toIndexedSeq)

  // ---- Achal-swar–aware variant picker -----------------------------------------------------

  /** Sa and Pa are fixed (achal): they must only ever be Shuddha. Komal/Tivra are invalid for these notes.
    *
    * Re/Ga/Dha/Ni can be Shuddha or Komal but not Tivra. Ma can be Shuddha or Tivra but not Komal.
    *
    * This generator encodes the full rule so domain code never sees musically illegal swars.
    */
  def variantFor(note: Note): Gen[Variant] = note match
    case Note.Sa | Note.Pa => Gen.const(Variant.Shuddha)
    case Note.Re | Note.Ga | Note.Dha | Note.Ni =>
      Gen.oneOf(Variant.Shuddha, Variant.Komal)
    case Note.Ma => Gen.oneOf(Variant.Shuddha, Variant.Tivra)

  // ---- Rational, BeatPosition --------------------------------------------------------------

  val genRational: Gen[Rational] =
    for
      n <- Gen.choose(0, 8)
      d <- Gen.choose(1, 8)
    yield Rational(n, d)

  val genBeatPosition: Gen[BeatPosition] =
    for
      cycle <- Gen.choose(0, 4)
      beat  <- Gen.choose(0, 15)
      sub   <- genRational
    yield BeatPosition(cycle, beat, sub)

  val genDuration: Gen[Rational] =
    for
      n <- Gen.choose(1, 4)
      d <- Gen.oneOf(1, 2, 4, 8)
    yield Rational(n, d)

  // ---- NoteRef (achal-aware) ---------------------------------------------------------------

  val genNoteRef: Gen[NoteRef] =
    for
      note    <- genNote
      variant <- variantFor(note)
      octave  <- genOctave
    yield NoteRef(note, variant, octave)

  // ---- Ornaments (all 11 case classes — sealed-trait Ornament has 11 children) -----------

  val genMeend: Gen[Meend] =
    for
      s         <- genNoteRef
      e         <- genNoteRef
      dir       <- genMeendDirection
      interSize <- Gen.choose(0, 2)
      inter     <- Gen.listOfN(interSize, genNoteRef)
    yield Meend(s, e, dir, inter)

  val genKanSwar: Gen[KanSwar] = genNoteRef.map(KanSwar(_))
  val genGamak: Gen[Gamak]     = Gen.const(Gamak())
  val genAndolan: Gen[Andolan] = Gen.const(Andolan())
  val genGitkari: Gen[Gitkari] = Gen.const(Gitkari())
  val genGhaseet: Gen[Ghaseet] = genNoteRef.map(Ghaseet(_))
  val genSparsh: Gen[Sparsh]   = genNoteRef.map(Sparsh(_))

  private def listOfNoteRefs(min: Int, max: Int): Gen[List[NoteRef]] =
    for
      n  <- Gen.choose(min, max)
      ns <- Gen.listOfN(n, genNoteRef)
    yield ns

  val genMurki: Gen[Murki]     = listOfNoteRefs(1, 3).map(Murki(_))
  val genKrintan: Gen[Krintan] = listOfNoteRefs(1, 3).map(Krintan(_))
  val genZamzama: Gen[Zamzama] = listOfNoteRefs(1, 3).map(Zamzama(_))

  val genCustomOrnament: Gen[CustomOrnament] =
    for
      name      <- genShortText
      paramSize <- Gen.choose(0, 2)
      params    <- Gen.mapOfN(paramSize, Gen.zip(genShortText, genShortText))
    yield CustomOrnament(name, params)

  val genOrnament: Gen[Ornament] = Gen.oneOf[Ornament](
    genMeend,
    genKanSwar,
    genMurki,
    genGamak,
    genAndolan,
    genKrintan,
    genGitkari,
    genGhaseet,
    genSparsh,
    genZamzama,
    genCustomOrnament
  )

  // ---- Events ------------------------------------------------------------------------------

  val genSwar: Gen[Event.Swar] =
    for
      note      <- genNote
      variant   <- variantFor(note)
      octave    <- genOctave
      beat      <- genBeatPosition
      duration  <- genDuration
      stroke    <- Gen.option(genStroke)
      ornSize   <- Gen.choose(0, 2)
      ornaments <- Gen.listOfN(ornSize, genOrnament)
      sahitya   <- Gen.option(genShortText)
    yield Event.Swar(note, variant, octave, beat, duration, stroke, ornaments, sahitya)

  val genRest: Gen[Event.Rest] =
    for
      beat     <- genBeatPosition
      duration <- genDuration
    yield Event.Rest(beat, duration)

  val genSustain: Gen[Event.Sustain] =
    for
      beat     <- genBeatPosition
      duration <- genDuration
    yield Event.Sustain(beat, duration)

  val genChikari: Gen[Event.Chikari] =
    for
      beat     <- genBeatPosition
      duration <- genDuration
    yield Event.Chikari(beat, duration)

  val genLockedBeat: Gen[Event.LockedBeat] =
    for
      beat     <- genBeatPosition
      duration <- genDuration
    yield Event.LockedBeat(beat, duration)

  val genEvent: Gen[Event] = Gen.frequency(
    8 -> genSwar,
    2 -> genRest,
    2 -> genSustain,
    1 -> genChikari,
    1 -> genLockedBeat
  )

  // ---- Built-in raag/taal catalogs --------------------------------------------------------

  val genRaagName: Gen[String] = Gen.oneOf(Raags.all.values.map(_.name).toIndexedSeq)
  val genTaalName: Gen[String] = Gen.oneOf(Taals.all.values.map(_.name).toIndexedSeq)

  val genRaag: Gen[Raag] = Gen.oneOf(Raags.all.values.toIndexedSeq)
  val genTaal: Gen[Taal] = Gen.oneOf(Taals.all.values.toIndexedSeq)

  // ---- Composition pieces -----------------------------------------------------------------

  val genCompositionType: Gen[CompositionType] = Gen.oneOf(
    Gen.const(CompositionType.Bandish),
    Gen.const(CompositionType.Gat),
    Gen.const(CompositionType.Palta),
    Gen.const(CompositionType.Sargam),
    genShortText.map(CompositionType.Custom(_))
  )

  val genSectionType: Gen[SectionType] = Gen.oneOf(
    Gen.const(SectionType.Sthayi),
    Gen.const(SectionType.Antara),
    Gen.const(SectionType.Sanchari),
    Gen.const(SectionType.Abhog),
    Gen.const(SectionType.Taan),
    Gen.const(SectionType.Toda),
    Gen.const(SectionType.Jhala),
    Gen.const(SectionType.Palta),
    Gen.const(SectionType.Arohi),
    Gen.const(SectionType.Avarohi),
    Gen.const(SectionType.Sargam),
    genShortText.map(SectionType.Custom(_))
  )

  val genTihai: Gen[Tihai] =
    for
      s <- genBeatPosition
      e <- genBeatPosition
    yield Tihai(s, e)

  /** Section generator. Pinned to `startingBeat = 1` so SwarFormat round-trips cleanly — the format performs a one-way
    * migration that synthesises LockedBeat events when `startingBeat > 1` and they aren't already present, which would
    * defeat property-based round-trip checks. Tests that need other starting beats can construct sections explicitly.
    */
  val genSection: Gen[Section] =
    for
      name     <- genShortText
      stype    <- genSectionType
      eventCnt <- Gen.choose(0, 16)
      events   <- Gen.listOfN(eventCnt, genEvent)
      tihai    <- Gen.option(genTihai)
    yield Section(name, stype, events, tihai, startingBeat = 1)

  val genMetadata: Gen[Metadata] =
    for
      title  <- genShortText
      ctype  <- genCompositionType
      raag   <- genRaag
      taal   <- genTaal
      laya   <- Gen.option(genLaya)
      script <- Gen.option(genSwarScript)
      inst   <- genOptShortText
      comp   <- genOptShortText
      auth   <- genOptShortText
      src    <- genOptShortText
      stroke <- Arbitrary.arbBool.arbitrary
      sah    <- Arbitrary.arbBool.arbitrary
      cAt    <- Gen.const("2026-01-01T00:00:00Z")
      uAt    <- Gen.const("2026-01-01T00:00:00Z")
    yield Metadata(title, ctype, raag, taal, laya, script, inst, comp, auth, src, stroke, sah, cAt, uAt)

  val genComposition: Gen[Composition] =
    for
      meta     <- genMetadata
      sectCnt  <- Gen.choose(0, 3)
      sections <- Gen.listOfN(sectCnt, genSection)
    yield Composition(meta, sections)

  // ---- Arbitrary instances (let tests use `forAll { (x: T) => ... }`) ---------------------

  given Arbitrary[Note]             = Arbitrary(genNote)
  given Arbitrary[Variant]          = Arbitrary(genVariant)
  given Arbitrary[Octave]           = Arbitrary(genOctave)
  given Arbitrary[Stroke]           = Arbitrary(genStroke)
  given Arbitrary[Laya]             = Arbitrary(genLaya)
  given Arbitrary[MeendDirection]   = Arbitrary(genMeendDirection)
  given Arbitrary[SwarScript]       = Arbitrary(genSwarScript)
  given Arbitrary[Rational]         = Arbitrary(genRational)
  given Arbitrary[BeatPosition]     = Arbitrary(genBeatPosition)
  given Arbitrary[NoteRef]          = Arbitrary(genNoteRef)
  given Arbitrary[Ornament]         = Arbitrary(genOrnament)
  given Arbitrary[Event.Swar]       = Arbitrary(genSwar)
  given Arbitrary[Event.Rest]       = Arbitrary(genRest)
  given Arbitrary[Event.Sustain]    = Arbitrary(genSustain)
  given Arbitrary[Event.Chikari]    = Arbitrary(genChikari)
  given Arbitrary[Event.LockedBeat] = Arbitrary(genLockedBeat)
  given Arbitrary[Event]            = Arbitrary(genEvent)
  given Arbitrary[Raag]             = Arbitrary(genRaag)
  given Arbitrary[Taal]             = Arbitrary(genTaal)
  given Arbitrary[CompositionType]  = Arbitrary(genCompositionType)
  given Arbitrary[SectionType]      = Arbitrary(genSectionType)
  given Arbitrary[Tihai]            = Arbitrary(genTihai)
  given Arbitrary[Section]          = Arbitrary(genSection)
  given Arbitrary[Metadata]         = Arbitrary(genMetadata)
  given Arbitrary[Composition]      = Arbitrary(genComposition)
