package sangeet.format

import io.circe.*
import io.circe.syntax.*
import io.circe.parser.{parse => parseJson}
import sangeet.model.*
import java.nio.file.{Files, Path}
import java.nio.charset.StandardCharsets

object SwarFormat:
  import Codecs.given

  val currentVersion = "1.0"

  def toJson(composition: Composition): Json =
    Json.obj(
      "version" -> Json.fromString(currentVersion),
    ).deepMerge(composition.asJson)

  def fromJson(jsonString: String): Either[Error, Composition] =
    for
      json <- parseJson(jsonString)
      comp <- json.as[Composition]
    yield comp

  def writeFile(path: Path, composition: Composition): Unit =
    val json = toJson(composition)
    Files.writeString(path, json.spaces2, StandardCharsets.UTF_8)

  def readFile(path: Path): Either[Error, Composition] =
    val content = Files.readString(path, StandardCharsets.UTF_8)
    fromJson(content)
