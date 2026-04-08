package sangeet.format

import io.circe.*
import io.circe.syntax.*
import sangeet.model.*

/** Codecs for primitive model types: Note, Variant, Octave, Stroke, Laya,
  * MeendDirection, Rational, BeatPosition, NoteRef, VibhagMarker,
  * CompositionType, SectionType */
object ModelCodecs:

  /** Generic encoder/decoder for simple enums that serialize as lowercase strings */
  private def simpleEnumCodecs[E <: scala.reflect.Enum](values: Array[E], typeName: String): (Encoder[E], Decoder[E]) =
    val encoder: Encoder[E] = Encoder.encodeString.contramap(_.toString.toLowerCase)
    val decoder: Decoder[E] = Decoder.decodeString.emap { s =>
      values.find(_.toString.equalsIgnoreCase(s)).toRight(s"Invalid $typeName: $s")
    }
    (encoder, decoder)

  /** Generic encoder/decoder for enums with camelCase multi-word variants,
    * using an explicit mapping for serialization */
  private def mappedEnumCodecs[E](mapping: Map[String, E], encode: E => String, typeName: String): (Encoder[E], Decoder[E]) =
    val encoder: Encoder[E] = Encoder.encodeString.contramap(encode)
    val decoder: Decoder[E] = Decoder.decodeString.emap { s =>
      mapping.get(s.toLowerCase).toRight(s"Invalid $typeName: $s")
    }
    (encoder, decoder)

  private val (noteEnc, noteDec) = simpleEnumCodecs(Note.values, "Note")
  given Encoder[Note] = noteEnc
  given Decoder[Note] = noteDec

  private val (variantEnc, variantDec) = simpleEnumCodecs(Variant.values, "Variant")
  given Encoder[Variant] = variantEnc
  given Decoder[Variant] = variantDec

  private val (strokeEnc, strokeDec) = simpleEnumCodecs(Stroke.values, "Stroke")
  given Encoder[Stroke] = strokeEnc
  given Decoder[Stroke] = strokeDec

  private val (meendDirEnc, meendDirDec) = simpleEnumCodecs(MeendDirection.values, "MeendDirection")
  given Encoder[MeendDirection] = meendDirEnc
  given Decoder[MeendDirection] = meendDirDec

  private val (octaveEnc, octaveDec) = mappedEnumCodecs[Octave](
    Map("atimandra" -> Octave.AtiMandra, "mandra" -> Octave.Mandra,
        "madhya" -> Octave.Madhya, "taar" -> Octave.Taar, "atitaar" -> Octave.AtiTaar),
    {
      case Octave.AtiMandra => "atiMandra"
      case Octave.AtiTaar   => "atiTaar"
      case o                => s"${o.toString.head.toLower}${o.toString.tail}"
    },
    "Octave"
  )
  given Encoder[Octave] = octaveEnc
  given Decoder[Octave] = octaveDec

  private val (layaEnc, layaDec) = mappedEnumCodecs[Laya](
    Map("ativilambit" -> Laya.AtiVilambit, "vilambit" -> Laya.Vilambit,
        "madhya" -> Laya.Madhya, "drut" -> Laya.Drut, "atidrut" -> Laya.AtiDrut),
    {
      case Laya.AtiVilambit => "atiVilambit"
      case Laya.AtiDrut     => "atiDrut"
      case l                => s"${l.toString.head.toLower}${l.toString.tail}"
    },
    "Laya"
  )
  given Encoder[Laya] = layaEnc
  given Decoder[Laya] = layaDec

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
