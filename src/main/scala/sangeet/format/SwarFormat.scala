package sangeet.format

import io.circe.*
import io.circe.syntax.*
import io.circe.parser.{parse => parseJson}
import sangeet.model.*
import java.nio.file.{Files, Path}
import java.nio.charset.StandardCharsets
import scala.util.Try

object SwarFormat:
  import Codecs.given

  val currentVersion = "1.0"
  val supportedVersions = Set("1.0")

  def toJson(composition: Composition): Json =
    Json.obj(
      "version" -> Json.fromString(currentVersion),
    ).deepMerge(composition.asJson)

  def fromJson(jsonString: String): Either[Error, Composition] =
    for
      json <- parseJson(jsonString)
      _ <- validateVersion(json)
      comp <- json.as[Composition]
    yield comp

  private def validateVersion(json: Json): Either[Error, Unit] =
    json.hcursor.get[String]("version") match
      case Right(v) if supportedVersions.contains(v) => Right(())
      case Right(v) =>
        System.err.println(s"Warning: unknown .swar file version '$v', attempting best-effort parsing")
        Right(())
      case Left(_) =>
        System.err.println("Warning: .swar file has no version field, attempting best-effort parsing")
        Right(())

  def writeFile(path: Path, composition: Composition): Unit =
    val json = toJson(composition)
    Files.writeString(path, json.spaces2, StandardCharsets.UTF_8)

  def readFile(path: Path): Either[Error, Composition] =
    Try(Files.readString(path, StandardCharsets.UTF_8))
      .toEither
      .left.map(e => ParsingFailure(s"Failed to read file: ${e.getMessage}", e))
      .flatMap(fromJson)
