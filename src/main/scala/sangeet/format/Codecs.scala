package sangeet.format

import io.circe.*
import io.circe.syntax.*
import sangeet.model.*

object Codecs:

  // --- Note ---
  given Encoder[Note] = Encoder.encodeString.contramap(_.toString.toLowerCase)
  given Decoder[Note] = Decoder.decodeString.emap { s =>
    Note.values.find(_.toString.equalsIgnoreCase(s))
      .toRight(s"Invalid Note: $s")
  }

  // --- Variant ---
  given Encoder[Variant] = Encoder.encodeString.contramap(_.toString.toLowerCase)
  given Decoder[Variant] = Decoder.decodeString.emap { s =>
    Variant.values.find(_.toString.equalsIgnoreCase(s))
      .toRight(s"Invalid Variant: $s")
  }

  // --- Octave ---
  given Encoder[Octave] = Encoder.encodeString.contramap {
    case Octave.AtiMandra => "atiMandra"
    case Octave.AtiTaar   => "atiTaar"
    case o                => s"${o.toString.head.toLower}${o.toString.tail}"
  }
  given Decoder[Octave] = Decoder.decodeString.emap { s =>
    val mapping = Map(
      "atimandra" -> Octave.AtiMandra, "mandra" -> Octave.Mandra,
      "madhya" -> Octave.Madhya, "taar" -> Octave.Taar, "atitaar" -> Octave.AtiTaar
    )
    mapping.get(s.toLowerCase).toRight(s"Invalid Octave: $s")
  }

  // --- Stroke ---
  given Encoder[Stroke] = Encoder.encodeString.contramap(_.toString.toLowerCase)
  given Decoder[Stroke] = Decoder.decodeString.emap { s =>
    Stroke.values.find(_.toString.equalsIgnoreCase(s))
      .toRight(s"Invalid Stroke: $s")
  }

  // --- Laya ---
  given Encoder[Laya] = Encoder.encodeString.contramap {
    case Laya.AtiVilambit => "atiVilambit"
    case Laya.AtiDrut     => "atiDrut"
    case l                => s"${l.toString.head.toLower}${l.toString.tail}"
  }
  given Decoder[Laya] = Decoder.decodeString.emap { s =>
    val mapping = Map(
      "ativilambit" -> Laya.AtiVilambit, "vilambit" -> Laya.Vilambit,
      "madhya" -> Laya.Madhya, "drut" -> Laya.Drut, "atidrut" -> Laya.AtiDrut
    )
    mapping.get(s.toLowerCase).toRight(s"Invalid Laya: $s")
  }

  // --- MeendDirection ---
  given Encoder[MeendDirection] = Encoder.encodeString.contramap(_.toString.toLowerCase)
  given Decoder[MeendDirection] = Decoder.decodeString.emap { s =>
    MeendDirection.values.find(_.toString.equalsIgnoreCase(s))
      .toRight(s"Invalid MeendDirection: $s")
  }

  // --- Rational (as [num, den] array) ---
  given Encoder[Rational] = Encoder.instance { r =>
    Json.arr(Json.fromInt(r.numerator), Json.fromInt(r.denominator))
  }
  given Decoder[Rational] = Decoder.instance { c =>
    for
      arr <- c.as[List[Int]]
      result <- arr match
        case List(n, d) => Right(Rational(n, d))
        case _          => Left(DecodingFailure("Rational must be [num, den]", c.history))
    yield result
  }

  // --- BeatPosition ---
  given Encoder[BeatPosition] = Encoder.instance { bp =>
    Json.obj(
      "cycle" -> Json.fromInt(bp.cycle),
      "beat" -> Json.fromInt(bp.beat),
      "subdivision" -> bp.subdivision.asJson
    )
  }
  given Decoder[BeatPosition] = Decoder.instance { c =>
    for
      cycle <- c.downField("cycle").as[Int]
      beat <- c.downField("beat").as[Int]
      sub <- c.downField("subdivision").as[Rational]
    yield BeatPosition(cycle, beat, sub)
  }

  // --- NoteRef ---
  given Encoder[NoteRef] = Encoder.instance { nr =>
    Json.obj(
      "note" -> nr.note.asJson,
      "variant" -> nr.variant.asJson,
      "octave" -> nr.octave.asJson
    )
  }
  given Decoder[NoteRef] = Decoder.instance { c =>
    for
      note <- c.downField("note").as[Note]
      variant <- c.downField("variant").as[Variant]
      octave <- c.downField("octave").as[Octave]
    yield NoteRef(note, variant, octave)
  }

  // --- VibhagMarker ---
  given Encoder[VibhagMarker] = Encoder.instance {
    case VibhagMarker.Sam      => Json.fromString("sam")
    case VibhagMarker.Khali    => Json.fromString("khali")
    case VibhagMarker.Taali(n) => Json.obj("taali" -> Json.fromInt(n))
  }
  given Decoder[VibhagMarker] = Decoder.instance { c =>
    c.as[String].map {
      case "sam"   => VibhagMarker.Sam
      case "khali" => VibhagMarker.Khali
    }.orElse {
      c.downField("taali").as[Int].map(VibhagMarker.Taali(_))
    }
  }

  // --- CompositionType ---
  given Encoder[CompositionType] = Encoder.instance {
    case CompositionType.Bandish    => Json.fromString("bandish")
    case CompositionType.Gat        => Json.fromString("gat")
    case CompositionType.Palta      => Json.fromString("palta")
    case CompositionType.Custom(n)  => Json.obj("custom" -> Json.fromString(n))
  }
  given Decoder[CompositionType] = Decoder.instance { c =>
    c.as[String].map {
      case "bandish" => CompositionType.Bandish
      case "gat"     => CompositionType.Gat
      case "palta"   => CompositionType.Palta
    }.orElse {
      c.downField("custom").as[String].map(CompositionType.Custom(_))
    }
  }

  // --- SectionType ---
  given Encoder[SectionType] = Encoder.instance {
    case SectionType.Custom(n) => Json.obj("custom" -> Json.fromString(n))
    case st                    => Json.fromString(s"${st.toString.head.toLower}${st.toString.tail}")
  }
  given Decoder[SectionType] = Decoder.instance { c =>
    c.as[String].flatMap {
      case s if s.equalsIgnoreCase("sthayi")   => Right(SectionType.Sthayi)
      case s if s.equalsIgnoreCase("antara")   => Right(SectionType.Antara)
      case s if s.equalsIgnoreCase("sanchari")  => Right(SectionType.Sanchari)
      case s if s.equalsIgnoreCase("abhog")    => Right(SectionType.Abhog)
      case s if s.equalsIgnoreCase("taan")     => Right(SectionType.Taan)
      case s if s.equalsIgnoreCase("toda")     => Right(SectionType.Toda)
      case s if s.equalsIgnoreCase("jhala")    => Right(SectionType.Jhala)
      case s if s.equalsIgnoreCase("palta")    => Right(SectionType.Palta)
      case s if s.equalsIgnoreCase("arohi")    => Right(SectionType.Arohi)
      case s if s.equalsIgnoreCase("avarohi")  => Right(SectionType.Avarohi)
      case s => Left(DecodingFailure(s"Invalid SectionType: $s", c.history))
    }.orElse {
      c.downField("custom").as[String].map(SectionType.Custom(_))
    }
  }

  // --- Ornament (discriminated union via "type" field) ---
  given Encoder[Ornament] = Encoder.instance {
    case m: Meend => Json.obj(
      "type" -> "meend".asJson,
      "startNote" -> m.startNote.asJson,
      "endNote" -> m.endNote.asJson,
      "direction" -> m.direction.asJson,
      "intermediateNotes" -> m.intermediateNotes.asJson
    )
    case k: KanSwar => Json.obj(
      "type" -> "kanSwar".asJson,
      "graceNote" -> k.graceNote.asJson
    )
    case m: Murki => Json.obj(
      "type" -> "murki".asJson, "notes" -> m.notes.asJson
    )
    case _: Gamak => Json.obj("type" -> "gamak".asJson)
    case _: Andolan => Json.obj("type" -> "andolan".asJson)
    case k: Krintan => Json.obj(
      "type" -> "krintan".asJson, "notes" -> k.notes.asJson
    )
    case _: Gitkari => Json.obj("type" -> "gitkari".asJson)
    case g: Ghaseet => Json.obj(
      "type" -> "ghaseet".asJson, "targetNote" -> g.targetNote.asJson
    )
    case s: Sparsh => Json.obj(
      "type" -> "sparsh".asJson, "touchNote" -> s.touchNote.asJson
    )
    case z: Zamzama => Json.obj(
      "type" -> "zamzama".asJson, "notes" -> z.notes.asJson
    )
    case c: CustomOrnament => Json.obj(
      "type" -> "custom".asJson,
      "name" -> c.name.asJson,
      "parameters" -> c.parameters.asJson
    )
  }

  given Decoder[Ornament] = Decoder.instance { c =>
    c.downField("type").as[String].flatMap {
      case "meend" => for
        s <- c.downField("startNote").as[NoteRef]
        e <- c.downField("endNote").as[NoteRef]
        d <- c.downField("direction").as[MeendDirection]
        i <- c.downField("intermediateNotes").as[List[NoteRef]]
      yield Meend(s, e, d, i)
      case "kanSwar" => c.downField("graceNote").as[NoteRef].map(KanSwar(_))
      case "murki"   => c.downField("notes").as[List[NoteRef]].map(Murki(_))
      case "gamak"   => Right(Gamak())
      case "andolan" => Right(Andolan())
      case "krintan" => c.downField("notes").as[List[NoteRef]].map(Krintan(_))
      case "gitkari" => Right(Gitkari())
      case "ghaseet" => c.downField("targetNote").as[NoteRef].map(Ghaseet(_))
      case "sparsh"  => c.downField("touchNote").as[NoteRef].map(Sparsh(_))
      case "zamzama" => c.downField("notes").as[List[NoteRef]].map(Zamzama(_))
      case "custom" => for
        name <- c.downField("name").as[String]
        params <- c.downField("parameters").as[Map[String, String]]
      yield CustomOrnament(name, params)
      case other => Left(DecodingFailure(s"Unknown ornament type: $other", c.history))
    }
  }

  // --- Event (discriminated union via "type" field) ---
  given Encoder[Event] = Encoder.instance {
    case s: Event.Swar =>
      val base = Json.obj(
        "type" -> "swar".asJson,
        "note" -> s.note.asJson,
        "variant" -> s.variant.asJson,
        "octave" -> s.octave.asJson,
        "beat" -> s.beat.asJson,
        "duration" -> s.duration.asJson,
        "ornaments" -> s.ornaments.asJson
      )
      val withStroke = s.stroke.fold(base)(st => base.deepMerge(Json.obj("stroke" -> st.asJson)))
      val withSahitya = s.sahitya.fold(withStroke)(sa => withStroke.deepMerge(Json.obj("sahitya" -> sa.asJson)))
      withSahitya
    case r: Event.Rest => Json.obj(
      "type" -> "rest".asJson,
      "beat" -> r.beat.asJson,
      "duration" -> r.duration.asJson
    )
    case s: Event.Sustain => Json.obj(
      "type" -> "sustain".asJson,
      "beat" -> s.beat.asJson,
      "duration" -> s.duration.asJson
    )
  }

  given Decoder[Event] = Decoder.instance { c =>
    c.downField("type").as[String].flatMap {
      case "swar" => for
        note <- c.downField("note").as[Note]
        variant <- c.downField("variant").as[Variant]
        octave <- c.downField("octave").as[Octave]
        beat <- c.downField("beat").as[BeatPosition]
        duration <- c.downField("duration").as[Rational]
        stroke <- c.downField("stroke").as[Option[Stroke]]
        ornaments <- c.downField("ornaments").as[List[Ornament]]
        sahitya <- c.downField("sahitya").as[Option[String]]
      yield Event.Swar(note, variant, octave, beat, duration, stroke, ornaments, sahitya)
      case "rest" => for
        beat <- c.downField("beat").as[BeatPosition]
        duration <- c.downField("duration").as[Rational]
      yield Event.Rest(beat, duration)
      case "sustain" => for
        beat <- c.downField("beat").as[BeatPosition]
        duration <- c.downField("duration").as[Rational]
      yield Event.Sustain(beat, duration)
      case other => Left(DecodingFailure(s"Unknown event type: $other", c.history))
    }
  }

  // --- Vibhag ---
  given Encoder[Vibhag] = Encoder.instance { v =>
    Json.obj("beats" -> Json.fromInt(v.beats), "marker" -> v.marker.asJson)
  }
  given Decoder[Vibhag] = Decoder.instance { c =>
    for
      beats <- c.downField("beats").as[Int]
      marker <- c.downField("marker").as[VibhagMarker]
    yield Vibhag(beats, marker)
  }

  // --- Taal ---
  given Encoder[Taal] = Encoder.instance { t =>
    val base = Json.obj(
      "name" -> t.name.asJson,
      "matras" -> Json.fromInt(t.matras),
      "vibhags" -> t.vibhags.asJson
    )
    t.theka.fold(base)(th => base.deepMerge(Json.obj("theka" -> th.asJson)))
  }
  given Decoder[Taal] = Decoder.instance { c =>
    for
      name <- c.downField("name").as[String]
      matras <- c.downField("matras").as[Int]
      vibhags <- c.downField("vibhags").as[List[Vibhag]]
      theka <- c.downField("theka").as[Option[List[String]]]
    yield Taal(name, matras, vibhags, theka)
  }

  // --- Raag ---
  given Encoder[Raag] = Encoder.instance { r =>
    Json.obj(
      "name" -> r.name.asJson,
      "thaat" -> r.thaat.asJson,
      "arohana" -> r.arohana.asJson,
      "avarohana" -> r.avarohana.asJson,
      "vadi" -> r.vadi.asJson,
      "samvadi" -> r.samvadi.asJson,
      "pakad" -> r.pakad.asJson,
      "prahar" -> r.prahar.asJson
    ).dropNullValues
  }
  given Decoder[Raag] = Decoder.instance { c =>
    for
      name <- c.downField("name").as[String]
      thaat <- c.downField("thaat").as[Option[String]]
      arohana <- c.downField("arohana").as[Option[List[String]]]
      avarohana <- c.downField("avarohana").as[Option[List[String]]]
      vadi <- c.downField("vadi").as[Option[String]]
      samvadi <- c.downField("samvadi").as[Option[String]]
      pakad <- c.downField("pakad").as[Option[String]]
      prahar <- c.downField("prahar").as[Option[Int]]
    yield Raag(name, thaat, arohana, avarohana, vadi, samvadi, pakad, prahar)
  }

  // --- Section ---
  given Encoder[Section] = Encoder.instance { s =>
    Json.obj(
      "name" -> s.name.asJson,
      "type" -> s.sectionType.asJson,
      "events" -> s.events.asJson
    )
  }
  given Decoder[Section] = Decoder.instance { c =>
    for
      name <- c.downField("name").as[String]
      stype <- c.downField("type").as[SectionType]
      events <- c.downField("events").as[List[Event]]
    yield Section(name, stype, events)
  }

  // --- Tihai ---
  given Encoder[Tihai] = Encoder.instance { t =>
    Json.obj(
      "section" -> t.sectionName.asJson,
      "startBeat" -> t.startBeat.asJson,
      "landingBeat" -> t.landingBeat.asJson
    )
  }
  given Decoder[Tihai] = Decoder.instance { c =>
    for
      section <- c.downField("section").as[String]
      start <- c.downField("startBeat").as[BeatPosition]
      landing <- c.downField("landingBeat").as[BeatPosition]
    yield Tihai(section, start, landing)
  }

  // --- Metadata ---
  given Encoder[Metadata] = Encoder.instance { m =>
    Json.obj(
      "title" -> m.title.asJson,
      "compositionType" -> m.compositionType.asJson,
      "raag" -> m.raag.asJson,
      "taal" -> m.taal.asJson,
      "laya" -> m.laya.asJson,
      "instrument" -> m.instrument.asJson,
      "composer" -> m.composer.asJson,
      "author" -> m.author.asJson,
      "source" -> m.source.asJson,
      "createdAt" -> m.createdAt.asJson,
      "updatedAt" -> m.updatedAt.asJson
    ).dropNullValues
  }
  given Decoder[Metadata] = Decoder.instance { c =>
    for
      title <- c.downField("title").as[String]
      ct <- c.downField("compositionType").as[CompositionType]
      raag <- c.downField("raag").as[Raag]
      taal <- c.downField("taal").as[Taal]
      laya <- c.downField("laya").as[Option[Laya]]
      instrument <- c.downField("instrument").as[Option[String]]
      composer <- c.downField("composer").as[Option[String]]
      author <- c.downField("author").as[Option[String]]
      source <- c.downField("source").as[Option[String]]
      createdAt <- c.downField("createdAt").as[String]
      updatedAt <- c.downField("updatedAt").as[String]
    yield Metadata(title, ct, raag, taal, laya, instrument, composer, author, source, createdAt, updatedAt)
  }

  // --- Composition ---
  given Encoder[Composition] = Encoder.instance { comp =>
    Json.obj(
      "metadata" -> comp.metadata.asJson,
      "sections" -> comp.sections.asJson,
      "tihais" -> comp.tihais.asJson
    )
  }
  given Decoder[Composition] = Decoder.instance { c =>
    for
      metadata <- c.downField("metadata").as[Metadata]
      sections <- c.downField("sections").as[List[Section]]
      tihais <- c.downField("tihais").as[List[Tihai]]
    yield Composition(metadata, sections, tihais)
  }
