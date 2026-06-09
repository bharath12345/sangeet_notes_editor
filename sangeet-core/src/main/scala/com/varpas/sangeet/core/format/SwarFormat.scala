package com.varpas.sangeet.core.format

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import scala.util.Try

import io.circe._
import io.circe.parser.{parse => parseJson}
import io.circe.syntax._

import com.varpas.sangeet.core.editor.CompositionEditor
import com.varpas.sangeet.core.model._

object SwarFormat:
  import Codecs.given

  val currentVersion    = "2.0"
  val supportedVersions = Set("1.0", "2.0")

  def toJson(composition: Composition): Json =
    Json
      .obj(
        "version" -> Json.fromString(currentVersion)
      )
      .deepMerge(composition.asJson)

  def fromJson(jsonString: String): Either[Error, Composition] =
    for
      json <- parseJson(jsonString)
      _    <- validateVersion(json)
      comp <- json.as[Composition]
    yield migrateLockedBeats(comp)

  private def migrateLockedBeats(comp: Composition): Composition =
    val matras = comp.metadata.taal.matras
    comp.copy(sections = comp.sections.map { section =>
      if section.startingBeat > 1 && !section.events.exists(_.isInstanceOf[Event.LockedBeat]) then
        section.copy(events = CompositionEditor.generateLockedBeats(matras, section.startingBeat) ++ section.events)
      else section
    })

  private def validateVersion(json: Json): Either[Error, Unit] =
    json.hcursor.get[String]("version") match
      case Right(v) if supportedVersions.contains(v) => Right(())
      case Right(_) | Left(_)                        => Right(())

  def writeFile(path: Path, composition: Composition): Unit =
    val json = toJson(composition)
    Files.writeString(path, json.noSpaces, StandardCharsets.UTF_8)

  def readFile(path: Path): Either[Error, Composition] =
    Try(Files.readString(path, StandardCharsets.UTF_8)).toEither.left
      .map(e => ParsingFailure(s"Failed to read file: ${e.getMessage}", e))
      .flatMap(fromJson)
