package com.varpas.sangeet.core.debug

import io.circe._
import io.circe.syntax._

/** Single source of truth for every command both the TCP debug console and the web WebSocket debug bridge accept.
  * Adding a new command means:
  *   1. add a case here 2. add a dispatch arm in sangeet-desktop's DebugCommandHandler.applyDebugCommand 3. add a
  *      dispatch arm in sangeet-web's Debug.Interpreter.interpret
  * Steps 2 and 3 won't compile until both are done, so drift is caught at build time.
  */
enum DebugCommand:
  // Connection / introspection
  case Ping
  case Help
  case ThreadDump
  case SetDebug(enabled: Boolean)
  case ThrowCrash

  // Tab management (desktop-only at apply time; web ignores)
  case ListTabs
  case SelectTab(id: String)
  case NewTab
  case CloseTab(id: String)
  case TabInfo

  // Composition reset / creation
  case Reset(compositionType: String, raag: Option[String], taal: String)
  case SetTaal(taal: String)

  // Editor focus
  case CheckFocus
  case FocusEditor

  // Cursor / mode setters
  case SetOctave(octave: String)
  case SetSubdivision(n: Int)

  // Swar input
  case TypeChar(ch: String)
  case Press(key: String)
  case TypeTimed(ch: String, delayMs: Int)
  case DualSwar(first: String, second: String)
  case SwarGroup(notes: List[String])

  // Strokes
  case Stroke(stroke: String)

  // Ornaments
  case SimpleOrnament(name: String)
  case OrnamentStart(kind: String)
  case OrnamentNote(note: String)
  case FinishOrnament

  // Sections
  case SwitchSection(idx: Int)

  // State read-back
  case GetState
  case GetEvents
  case DumpComposition
  case DumpHistory

object DebugCommand:

  /** Circe encoder using discriminated-union format: the variant name is the top-level key, and its parameters (if any)
    * go in a nested object. Parameterless variants encode as {"Ping": {}}. Variants with params encode as {"TypeChar":
    * {"ch": "s"}}.
    */
  given Encoder[DebugCommand] = Encoder.instance {
    case Ping          => Json.obj("Ping" -> Json.obj())
    case Help          => Json.obj("Help" -> Json.obj())
    case ThreadDump    => Json.obj("ThreadDump" -> Json.obj())
    case SetDebug(e)   => Json.obj("SetDebug" -> Json.obj("enabled" -> e.asJson))
    case ThrowCrash    => Json.obj("ThrowCrash" -> Json.obj())
    case ListTabs      => Json.obj("ListTabs" -> Json.obj())
    case SelectTab(id) => Json.obj("SelectTab" -> Json.obj("id" -> id.asJson))
    case NewTab        => Json.obj("NewTab" -> Json.obj())
    case CloseTab(id)  => Json.obj("CloseTab" -> Json.obj("id" -> id.asJson))
    case TabInfo       => Json.obj("TabInfo" -> Json.obj())
    case Reset(ct, r, t) =>
      Json.obj("Reset" -> Json.obj("compositionType" -> ct.asJson, "raag" -> r.asJson, "taal" -> t.asJson))
    case SetTaal(t)        => Json.obj("SetTaal" -> Json.obj("taal" -> t.asJson))
    case CheckFocus        => Json.obj("CheckFocus" -> Json.obj())
    case FocusEditor       => Json.obj("FocusEditor" -> Json.obj())
    case SetOctave(o)      => Json.obj("SetOctave" -> Json.obj("octave" -> o.asJson))
    case SetSubdivision(n) => Json.obj("SetSubdivision" -> Json.obj("n" -> n.asJson))
    case TypeChar(c)       => Json.obj("TypeChar" -> Json.obj("ch" -> c.asJson))
    case Press(k)          => Json.obj("Press" -> Json.obj("key" -> k.asJson))
    case TypeTimed(c, d)   => Json.obj("TypeTimed" -> Json.obj("ch" -> c.asJson, "delayMs" -> d.asJson))
    case DualSwar(f, s)    => Json.obj("DualSwar" -> Json.obj("first" -> f.asJson, "second" -> s.asJson))
    case SwarGroup(n)      => Json.obj("SwarGroup" -> Json.obj("notes" -> n.asJson))
    case Stroke(s)         => Json.obj("Stroke" -> Json.obj("stroke" -> s.asJson))
    case SimpleOrnament(n) => Json.obj("SimpleOrnament" -> Json.obj("name" -> n.asJson))
    case OrnamentStart(k)  => Json.obj("OrnamentStart" -> Json.obj("kind" -> k.asJson))
    case OrnamentNote(n)   => Json.obj("OrnamentNote" -> Json.obj("note" -> n.asJson))
    case FinishOrnament    => Json.obj("FinishOrnament" -> Json.obj())
    case SwitchSection(i)  => Json.obj("SwitchSection" -> Json.obj("idx" -> i.asJson))
    case GetState          => Json.obj("GetState" -> Json.obj())
    case GetEvents         => Json.obj("GetEvents" -> Json.obj())
    case DumpComposition   => Json.obj("DumpComposition" -> Json.obj())
    case DumpHistory       => Json.obj("DumpHistory" -> Json.obj())
  }

  /** Circe decoder — inverse of the encoder above */
  given Decoder[DebugCommand] = Decoder.instance { c =>
    c.keys.toList.flatten.headOption match
      case Some("Ping")       => Right(Ping)
      case Some("Help")       => Right(Help)
      case Some("ThreadDump") => Right(ThreadDump)
      case Some("SetDebug")   => c.downField("SetDebug").downField("enabled").as[Boolean].map(SetDebug.apply)
      case Some("ThrowCrash") => Right(ThrowCrash)
      case Some("ListTabs")   => Right(ListTabs)
      case Some("SelectTab")  => c.downField("SelectTab").downField("id").as[String].map(SelectTab.apply)
      case Some("NewTab")     => Right(NewTab)
      case Some("CloseTab")   => c.downField("CloseTab").downField("id").as[String].map(CloseTab.apply)
      case Some("TabInfo")    => Right(TabInfo)
      case Some("Reset") =>
        for
          ct <- c.downField("Reset").downField("compositionType").as[String]
          r  <- c.downField("Reset").downField("raag").as[Option[String]]
          t  <- c.downField("Reset").downField("taal").as[String]
        yield Reset(ct, r, t)
      case Some("SetTaal")        => c.downField("SetTaal").downField("taal").as[String].map(SetTaal.apply)
      case Some("CheckFocus")     => Right(CheckFocus)
      case Some("FocusEditor")    => Right(FocusEditor)
      case Some("SetOctave")      => c.downField("SetOctave").downField("octave").as[String].map(SetOctave.apply)
      case Some("SetSubdivision") => c.downField("SetSubdivision").downField("n").as[Int].map(SetSubdivision.apply)
      case Some("TypeChar")       => c.downField("TypeChar").downField("ch").as[String].map(TypeChar.apply)
      case Some("Press")          => c.downField("Press").downField("key").as[String].map(Press.apply)
      case Some("TypeTimed") =>
        for
          ch <- c.downField("TypeTimed").downField("ch").as[String]
          d  <- c.downField("TypeTimed").downField("delayMs").as[Int]
        yield TypeTimed(ch, d)
      case Some("DualSwar") =>
        for
          f <- c.downField("DualSwar").downField("first").as[String]
          s <- c.downField("DualSwar").downField("second").as[String]
        yield DualSwar(f, s)
      case Some("SwarGroup") => c.downField("SwarGroup").downField("notes").as[List[String]].map(SwarGroup.apply)
      case Some("Stroke")    => c.downField("Stroke").downField("stroke").as[String].map(Stroke.apply)
      case Some("SimpleOrnament") =>
        c.downField("SimpleOrnament").downField("name").as[String].map(SimpleOrnament.apply)
      case Some("OrnamentStart")   => c.downField("OrnamentStart").downField("kind").as[String].map(OrnamentStart.apply)
      case Some("OrnamentNote")    => c.downField("OrnamentNote").downField("note").as[String].map(OrnamentNote.apply)
      case Some("FinishOrnament")  => Right(FinishOrnament)
      case Some("SwitchSection")   => c.downField("SwitchSection").downField("idx").as[Int].map(SwitchSection.apply)
      case Some("GetState")        => Right(GetState)
      case Some("GetEvents")       => Right(GetEvents)
      case Some("DumpComposition") => Right(DumpComposition)
      case Some("DumpHistory")     => Right(DumpHistory)
      case Some(other)             => Left(DecodingFailure(s"Unknown DebugCommand variant: $other", c.history))
      case None => Left(DecodingFailure("DebugCommand must be an object with a single key", c.history))
  }
