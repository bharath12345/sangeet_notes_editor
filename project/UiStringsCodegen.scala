// format: off
import sbt._
import scala.io.Source
import scala.util.parsing.json._

object UiStringsCodegen {

  case class Param(name: String, paramType: String)
  case class Entry(
      key: String,
      value: Option[String],
      template: Option[String],
      params: List[Param],
      platform: String,
      description: String
  )

  // Public entry point for sbt
  def run(catalog: File, output: File): Seq[File] = {
    val text     = Source.fromFile(catalog, "UTF-8").mkString
    val rendered = emitScala(text)
    IO.write(output, rendered, java.nio.charset.StandardCharsets.UTF_8)
    Seq(output)
  }

  // Public for tests  
  def emitScala(catalogJson: String): String = {
    val entries                    = parseCatalog(catalogJson)
    val (parameterized, constants) = entries.partition(_.template.isDefined)
    val sortedConsts               = constants.sortBy(_.key)
    val sortedParams               = parameterized.sortBy(_.key)

    val header =
      """package com.varpas.sangeet.core.strings
        |
        |// GENERATED FILE — DO NOT EDIT MANUALLY.
        |// Source:    sangeet-core/src/main/resources/ui-strings.json
        |// Regenerate: sbt sangeetCore/genUiStrings   (or: make gen-strings)
        |//
        |// To add or change a string: edit ui-strings.json, then run `make gen-strings`,
        |// then use `UiStrings.<key>` on both desktop and web. See
        |// docs/developer/ui-strings-catalog.md for the full guide.
        |
        |object UiStrings:""".stripMargin

    val constLines = sortedConsts.map { e =>
      val ident   = keyToScalaIdent(e.key)
      val escaped = escapeScala(e.value.get)
      s"""  val $ident: String = "$escaped""""
    }

    val funcLines = sortedParams.map { e =>
      val ident = keyToScalaIdent(e.key)
      val args  = e.params.map(p => s"${p.name}: ${typeToScala(p.paramType)}").mkString(", ")
      val body  = emitScalaTemplateBody(e.template.get, e.params)
      s"""  def $ident($args): String = $body"""
    }

    val body = if (constLines.isEmpty && funcLines.isEmpty) {
      Seq("  // Empty catalog — no strings defined yet", "  private val _placeholder = ()")
    } else {
      Seq("")
    }
    (Seq(header) ++ body ++ constLines ++ (if (funcLines.nonEmpty) Seq("") else Nil) ++ funcLines).mkString("\n") + "\n"
  }

  private def parseCatalog(catalogJson: String): List[Entry] = {
    JSON.parseFull(catalogJson) match {
      case Some(root: Map[String, Any] @unchecked) =>
        val entries = root.getOrElse("entries", Map.empty[String, Any]).asInstanceOf[Map[String, Any]]
        entries.toList.map { case (key, body) =>
          val entryMap    = body.asInstanceOf[Map[String, Any]]
          val valueOpt    = entryMap.get("value").map(_.asInstanceOf[String])
          val templateOpt = entryMap.get("template").map(_.asInstanceOf[String])
          val platform    = entryMap.getOrElse("platform", "both").asInstanceOf[String]
          val description = entryMap.getOrElse("description", "").asInstanceOf[String]
          val params = entryMap.get("params") match {
            case Some(plist: List[Map[String, Any] @unchecked]) =>
              plist.map { p =>
                Param(
                  p("name").asInstanceOf[String],
                  p("type").asInstanceOf[String]
                )
              }
            case _ => Nil
          }
          Entry(key, valueOpt, templateOpt, params, platform, description)
        }
      case _ => throw new IllegalArgumentException("Invalid catalog JSON")
    }
  }

  private def keyToScalaIdent(key: String): String = {
    val parts = key.split('.')
    parts.head + parts.tail.map(p => p.head.toUpper.toString + p.tail).mkString
  }

  private def typeToScala(t: String): String = t match {
    case "int"    => "Int"
    case "string" => "String"
    case other    => throw new IllegalArgumentException(s"Unsupported param type: $other")
  }

  private def escapeScala(s: String): String = {
    s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
  }

  private def emitScalaTemplateBody(template: String, params: List[Param]): String = {
    // Replace {name} placeholders with $name interpolation, then wrap in s"..."
    // CRITICAL: Escape $ first BEFORE doing placeholder replacement to avoid double-escape
    var body = escapeScala(template)
    params.foreach(p => body = body.replace(s"{${p.name}}", s"$$${p.name}"))
    s"""s"$body""""
  }
}
