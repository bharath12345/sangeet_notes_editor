package com.varpas.sangeet.server

import cats.effect.IO
import io.circe.Json
import io.circe.syntax._
import org.http4s._

import com.varpas.sangeet.core.editor.CursorModel
import com.varpas.sangeet.core.format.Codecs.given
import com.varpas.sangeet.core.model._
import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.taal.Taals

object TestFixtures:

  val teentaal: Taal = Taals.byName("teentaal").get
  val yaman: Raag    = Raags.byName("yaman").get

  def minimalComposition: Composition =
    val metadata = Metadata(
      title = "Test Composition",
      compositionType = CompositionType.Gat,
      raag = yaman,
      taal = teentaal,
      laya = Some(Laya.Vilambit),
      instrument = None,
      composer = None,
      author = None,
      source = None,
      showStrokeLine = false,
      showSahityaLine = false,
      createdAt = "2026-05-01T00:00:00Z",
      updatedAt = "2026-05-01T00:00:00Z"
    )
    val section = Section(
      name = "Sthayi",
      sectionType = SectionType.Sthayi,
      events = Nil,
      tihai = None
    )
    Composition(metadata, List(section))

  def compositionWithSwar: Composition =
    val comp = minimalComposition
    val swarEvent = Event.Swar(
      note = Note.Sa,
      variant = Variant.Shuddha,
      octave = Octave.Madhya,
      beat = BeatPosition(0, 0, Rational(0, 1)),
      duration = Rational(1, 1),
      stroke = None,
      ornaments = Nil,
      sahitya = None
    )
    val section = comp.sections.head.copy(events = List(swarEvent))
    comp.copy(sections = List(section))

  def minimalCursor: CursorModel =
    CursorModel(
      taal = teentaal,
      cycle = 0,
      beat = 0,
      subIndex = 0,
      totalSubdivisions = 1,
      currentOctave = Octave.Madhya
    )

  def cursorJson(cursor: CursorModel = minimalCursor): Json =
    Json.obj(
      "taal"              -> cursor.taal.asJson,
      "cycle"             -> Json.fromInt(cursor.cycle),
      "beat"              -> Json.fromInt(cursor.beat),
      "subIndex"          -> Json.fromInt(cursor.subIndex),
      "totalSubdivisions" -> Json.fromInt(cursor.totalSubdivisions),
      "currentOctave"     -> Json.fromString(cursor.currentOctave.toString.toLowerCase)
    )

  def editorInputJson(
      composition: Composition = minimalComposition,
      sectionIndex: Int = 0,
      cursor: CursorModel = minimalCursor
  ): Json =
    Json.obj(
      "composition"  -> composition.asJson,
      "sectionIndex" -> Json.fromInt(sectionIndex),
      "cursor"       -> cursorJson(cursor)
    )

  def noteRefJson(note: String, variant: String = "shuddha", octave: String = "madhya"): Json =
    Json.obj(
      "note"    -> Json.fromString(note),
      "variant" -> Json.fromString(variant),
      "octave"  -> Json.fromString(octave)
    )

  def postRequest(uri: org.http4s.Uri, body: Json): Request[IO] =
    Request[IO](Method.POST, uri)
      .withEntity(body.noSpaces)
      .withContentType(headers.`Content-Type`(MediaType.application.json))
