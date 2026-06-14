package com.varpas.sangeet.core.debug

import io.circe._
import io.circe.syntax._

/** A single integration test loaded from tests/integration/ *.json. Both the ScalaTest runner (desktop) and the
  * Playwright runner (web) read the same files and dispatch each step through their respective transport.
  */
case class TestDefinition(
    name: String,
    description: Option[String],
    steps: List[TestStep]
)

object TestDefinition:
  given Encoder[TestDefinition] = Encoder.instance { td =>
    val fields = List(
      Some("name" -> td.name.asJson),
      td.description.map(d => "description" -> d.asJson),
      Some("steps" -> td.steps.asJson)
    ).flatten
    Json.obj(fields*)
  }

  given Decoder[TestDefinition] = Decoder.instance { c =>
    for
      name        <- c.downField("name").as[String]
      description <- c.downField("description").as[Option[String]]
      steps       <- c.downField("steps").as[List[TestStep]]
    yield TestDefinition(name, description, steps)
  }

/** Each step is either (a) a DebugCommand to send over the wire, or (b) a runner-side directive (Checkpoint,
  * AssertGoldenSwar, AssertGoldenHtml) that the runner interprets locally without sending it to the app.
  */
enum TestStep:
  case Cmd(cmd: DebugCommand)
  case Checkpoint(expect: ExpectedState)
  case AssertGoldenSwar(fixture: String)
  case AssertGoldenHtml(fixture: String)

object TestStep:
  given Encoder[TestStep] = Encoder.instance {
    case Cmd(cmd)              => Json.obj("Cmd" -> Json.obj("cmd" -> cmd.asJson))
    case Checkpoint(expect)    => Json.obj("Checkpoint" -> Json.obj("expect" -> expect.asJson))
    case AssertGoldenSwar(fix) => Json.obj("AssertGoldenSwar" -> Json.obj("fixture" -> fix.asJson))
    case AssertGoldenHtml(fix) => Json.obj("AssertGoldenHtml" -> Json.obj("fixture" -> fix.asJson))
  }

  given Decoder[TestStep] = Decoder.instance { c =>
    c.keys.toList.flatten.headOption match
      case Some("Cmd")        => c.downField("Cmd").downField("cmd").as[DebugCommand].map(Cmd.apply)
      case Some("Checkpoint") => c.downField("Checkpoint").downField("expect").as[ExpectedState].map(Checkpoint.apply)
      case Some("AssertGoldenSwar") =>
        c.downField("AssertGoldenSwar").downField("fixture").as[String].map(AssertGoldenSwar.apply)
      case Some("AssertGoldenHtml") =>
        c.downField("AssertGoldenHtml").downField("fixture").as[String].map(AssertGoldenHtml.apply)
      case Some(other) => Left(DecodingFailure(s"Unknown TestStep variant: $other", c.history))
      case None        => Left(DecodingFailure("TestStep must be an object with a single key", c.history))
  }

case class ExpectedState(
    eventCount: Option[Int] = None,
    cursorBeat: Option[Int] = None,
    cursorCycle: Option[Int] = None,
    sectionName: Option[String] = None,
    taalName: Option[String] = None,
    raagName: Option[String] = None,
    sectionCount: Option[Int] = None
)

object ExpectedState:
  given Encoder[ExpectedState] = Encoder.instance { es =>
    val fields = List(
      es.eventCount.map(v => "eventCount" -> v.asJson),
      es.cursorBeat.map(v => "cursorBeat" -> v.asJson),
      es.cursorCycle.map(v => "cursorCycle" -> v.asJson),
      es.sectionName.map(v => "sectionName" -> v.asJson),
      es.taalName.map(v => "taalName" -> v.asJson),
      es.raagName.map(v => "raagName" -> v.asJson),
      es.sectionCount.map(v => "sectionCount" -> v.asJson)
    ).flatten
    Json.obj(fields*)
  }

  given Decoder[ExpectedState] = Decoder.instance { c =>
    for
      eventCount   <- c.downField("eventCount").as[Option[Int]]
      cursorBeat   <- c.downField("cursorBeat").as[Option[Int]]
      cursorCycle  <- c.downField("cursorCycle").as[Option[Int]]
      sectionName  <- c.downField("sectionName").as[Option[String]]
      taalName     <- c.downField("taalName").as[Option[String]]
      raagName     <- c.downField("raagName").as[Option[String]]
      sectionCount <- c.downField("sectionCount").as[Option[Int]]
    yield ExpectedState(eventCount, cursorBeat, cursorCycle, sectionName, taalName, raagName, sectionCount)
  }
