package com.varpas.sangeet.core.strings

import io.circe._
import io.circe.parser._

object UiStringsCodegen:

  case class Param(name: String, paramType: String)
  case class Entry(
      key: String,
      value: Option[String],
      template: Option[String],
      params: List[Param],
      platform: String,
      description: String
  )

  def emitScala(catalogJson: String): String =
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

    val body =
      if (constLines.isEmpty && funcLines.isEmpty)
        Seq("  // Empty catalog — no strings defined yet", "  private val _placeholder = ()")
      else Seq("")
    (Seq(header) ++ body ++ constLines ++ (if funcLines.nonEmpty then Seq("") else Nil) ++ funcLines)
      .mkString("\n") + "\n"

  private def parseCatalog(catalogJson: String): List[Entry] =
    parse(catalogJson) match
      case Right(root) =>
        val cursor     = root.hcursor
        val entriesObj = cursor.downField("entries").as[Map[String, Json]].getOrElse(Map.empty)
        entriesObj.toList.map { case (key, body) =>
          val c = body.hcursor
          Entry(
            key = key,
            value = c.downField("value").as[String].toOption,
            template = c.downField("template").as[String].toOption,
            params = c.downField("params").as[List[Json]].toOption.getOrElse(Nil).map { p =>
              val pc = p.hcursor
              Param(
                pc.downField("name").as[String].toOption.get,
                pc.downField("type").as[String].toOption.get
              )
            },
            platform = c.downField("platform").as[String].toOption.getOrElse("both"),
            description = c.downField("description").as[String].toOption.getOrElse("")
          )
        }
      case Left(err) => throw new IllegalArgumentException(s"Invalid catalog JSON: $err")

  private def keyToScalaIdent(key: String): String =
    val parts = key.split('.')
    parts.head + parts.tail.map(p => p.head.toUpper.toString + p.tail).mkString

  private def typeToScala(t: String): String = t match
    case "int"    => "Int"
    case "string" => "String"
    case other    => throw new IllegalArgumentException(s"Unsupported param type: $other")

  private def escapeScala(s: String): String =
    if s.contains('$') then
      throw new IllegalArgumentException(
        s"Catalog string contains unsupported '$$' character: \"$s\". " +
          s"Use parameterized templates ({name}) for dynamic values."
      )
    s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

  private def emitScalaTemplateBody(template: String, params: List[Param]): String =
    // Replace {name} placeholders with $name interpolation, then wrap in s"..."
    // escapeScala rejects literal '$' in values; placeholder substitution
    // intentionally emits '$name' for s"..." interpolation, which is safe.
    val escaped = escapeScala(template)
    val substituted = params.foldLeft(escaped) { (acc, p) =>
      acc.replace(s"{${p.name}}", s"$$${p.name}")
    }
    s"""s"$substituted""""
