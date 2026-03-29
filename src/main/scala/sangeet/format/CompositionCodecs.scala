package sangeet.format

import io.circe.*
import io.circe.syntax.*
import sangeet.model.*
import ModelCodecs.given
import OrnamentCodecs.given

/** Codecs for composite types: Event, Vibhag, Taal, Raag, Section, Tihai, Metadata, Composition */
object CompositionCodecs:

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

  given Encoder[Vibhag] = Encoder.instance { v =>
    Json.obj("beats" -> Json.fromInt(v.beats), "marker" -> v.marker.asJson)
  }
  given Decoder[Vibhag] = Decoder.instance { c =>
    for
      beats <- c.downField("beats").as[Int]
      marker <- c.downField("marker").as[VibhagMarker]
    yield Vibhag(beats, marker)
  }

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

  given Encoder[Tihai] = Encoder.instance { t =>
    Json.obj(
      "startBeat" -> t.startBeat.asJson,
      "landingBeat" -> t.landingBeat.asJson
    )
  }
  given Decoder[Tihai] = Decoder.instance { c =>
    for
      start <- c.downField("startBeat").as[BeatPosition]
      landing <- c.downField("landingBeat").as[BeatPosition]
    yield Tihai(start, landing)
  }

  given Encoder[Section] = Encoder.instance { s =>
    val base = Json.obj(
      "name" -> s.name.asJson,
      "type" -> s.sectionType.asJson,
      "events" -> s.events.asJson
    )
    s.tihai.fold(base)(t => base.deepMerge(Json.obj("tihai" -> t.asJson)))
  }
  given Decoder[Section] = Decoder.instance { c =>
    for
      name <- c.downField("name").as[String]
      stype <- c.downField("type").as[SectionType]
      events <- c.downField("events").as[List[Event]]
      tihai <- c.downField("tihai").as[Option[Tihai]]
    yield Section(name, stype, events, tihai)
  }

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
      "showStrokeLine" -> m.showStrokeLine.asJson,
      "showSahityaLine" -> m.showSahityaLine.asJson,
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
      showStrokeLine <- c.downField("showStrokeLine").as[Option[Boolean]].map(_.getOrElse(false))
      showSahityaLine <- c.downField("showSahityaLine").as[Option[Boolean]].map(_.getOrElse(false))
      createdAt <- c.downField("createdAt").as[String]
      updatedAt <- c.downField("updatedAt").as[String]
    yield Metadata(title, ct, raag, taal, laya, instrument, composer, author, source, showStrokeLine, showSahityaLine, createdAt, updatedAt)
  }

  given Encoder[Composition] = Encoder.instance { comp =>
    Json.obj(
      "metadata" -> comp.metadata.asJson,
      "sections" -> comp.sections.asJson
    )
  }
  given Decoder[Composition] = Decoder.instance { c =>
    for
      metadata <- c.downField("metadata").as[Metadata]
      sections <- c.downField("sections").as[List[Section]]
    yield Composition(metadata, sections)
  }
