package com.varpas.sangeet.core.editor

import io.circe._
import io.circe.syntax._

import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.core.model.Event

object ClipboardCodecs:

  given Encoder[ClipboardData] = Encoder.instance { cd =>
    Json.obj(
      "sangeet-clipboard" -> Json.True,
      "version"           -> Json.fromString("2.0"),
      "events"            -> cd.events.asJson
    )
  }

  given Decoder[ClipboardData] = Decoder.instance { c =>
    for
      marker <- c.downField("sangeet-clipboard").as[Boolean]
      _      <- if marker then Right(()) else Left(DecodingFailure("Not a Sangeet clipboard payload", c.history))
      events <- c.downField("events").as[List[Event]]
    yield ClipboardData(events)
  }
