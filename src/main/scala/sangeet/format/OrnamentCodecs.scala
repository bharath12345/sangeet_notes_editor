package sangeet.format

import io.circe.*
import io.circe.syntax.*
import sangeet.model.*
import ModelCodecs.given

/** Codecs for the Ornament discriminated union */
object OrnamentCodecs:

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
