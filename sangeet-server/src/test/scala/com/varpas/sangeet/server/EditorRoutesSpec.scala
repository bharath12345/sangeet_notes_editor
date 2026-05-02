package com.varpas.sangeet.server

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.Json
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.varpas.sangeet.server.routes.EditorRoutes
import com.varpas.sangeet.core.model.*
import com.varpas.sangeet.core.editor.CursorModel
import com.varpas.sangeet.core.taal.Taals
import com.varpas.sangeet.core.raag.Raags
import com.varpas.sangeet.core.format.Codecs.given

class EditorRoutesSpec extends AnyFlatSpec with Matchers:

  val routes = Http4sServerInterpreter[IO]().toRoutes(EditorRoutes.all).orNotFound

  def minimalComposition: Composition =
    val raag = Raags.byName("yaman").get
    val taal = Taals.byName("teentaal").get
    val metadata = Metadata(
      title = "Test Composition",
      compositionType = CompositionType.Gat,
      raag = raag,
      taal = taal,
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

  def minimalCursor: CursorModel =
    CursorModel(
      taal = Taals.byName("teentaal").get,
      cycle = 0,
      beat = 0,
      subIndex = 0,
      totalSubdivisions = 1,
      currentOctave = Octave.Madhya
    )

  "POST /api/v1/editor/insert-rest" should "insert a rest and return updated composition" in {
    val composition = minimalComposition
    val cursor = minimalCursor

    val requestBody = Json.obj(
      "composition" -> composition.asJson,
      "sectionIndex" -> Json.fromInt(0),
      "cursor" -> Json.obj(
        "taal" -> cursor.taal.asJson,
        "cycle" -> Json.fromInt(cursor.cycle),
        "beat" -> Json.fromInt(cursor.beat),
        "subIndex" -> Json.fromInt(cursor.subIndex),
        "totalSubdivisions" -> Json.fromInt(cursor.totalSubdivisions),
        "currentOctave" -> Json.fromString("madhya")
      )
    )

    val req = Request[IO](Method.POST, uri"/api/v1/editor/insert-rest")
      .withEntity(requestBody.noSpaces)
      .withContentType(headers.`Content-Type`(MediaType.application.json))

    val resp = routes.run(req).unsafeRunSync()

    resp.status shouldBe Status.Ok

    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail("Failed to parse JSON"))

    val responseCursor = json.hcursor
    responseCursor.get[Boolean]("success").getOrElse(false) shouldBe true

    val data = responseCursor.downField("data")
    data.downField("composition").succeeded shouldBe true
    data.downField("cursor").succeeded shouldBe true
    data.downField("message").as[String].getOrElse("") should include("rest")
  }

  "POST /api/v1/editor/insert-rest" should "return error with invalid input" in {
    val invalidBody = Json.obj(
      "invalid" -> Json.fromString("data")
    )

    val req = Request[IO](Method.POST, uri"/api/v1/editor/insert-rest")
      .withEntity(invalidBody.noSpaces)
      .withContentType(headers.`Content-Type`(MediaType.application.json))

    val resp = routes.run(req).unsafeRunSync()

    resp.status should not be Status.Ok

    val body = resp.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail("Failed to parse JSON"))

    val cursor = json.hcursor
    cursor.get[Boolean]("success").getOrElse(true) shouldBe false
    cursor.downField("error").downField("code").succeeded shouldBe true
  }
