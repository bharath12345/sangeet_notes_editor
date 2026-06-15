package com.varpas.sangeet.core.codegen

import java.nio.file.{Files, Paths, StandardOpenOption}

import io.circe.Json
import io.circe.syntax._

/** PR-1c (plan 18): exports a JSON Schema (draft-07) describing the on-disk `.swar` composition file format. Lives at
  * `docs/developer/specs/swar.schema.json`.
  *
  * The schema is hand-written rather than circe-derived because:
  *   - The repo's circe codecs are not pure derived encoders (see `format/CompositionCodecs.scala`); they apply
  *     `dropNullValues`, custom discriminators, rationals as `[num, den]` arrays, etc. Derivation would produce a
  *     schema that doesn't match the file.
  *   - circe-json-schema lags Scala 3 / circe 0.14 support; pulling in a reflection-based generator (like guardrail's)
  *     would add a heavy dep for one regenerated file.
  *
  * The shape MUST be kept in sync with `CompositionCodecs`, `ModelCodecs`, `OrnamentCodecs`. The `make gen-specs` flow
  * regenerates this file; CI's `check-specs` job fails on drift. If you change a codec, regenerate.
  */
object SwarSchemaExporter:

  /** Default path when no argument is given. Resolved against cwd; only valid when launched from the build root. The
    * sbt `generateSwarSchema` task passes an absolute path explicitly to avoid the cwd-confusion that `fork := true`
    * introduces (each sub-project's forked JVM inherits its own baseDirectory as cwd).
    */
  private val DefaultOutputPath = Paths.get("docs", "developer", "specs", "swar.schema.json")

  def main(args: Array[String]): Unit =
    val outputPath = args.headOption.map(Paths.get(_)).getOrElse(DefaultOutputPath)
    val schema     = buildSchema()
    val text       = schema.spaces2 + "\n"
    Files.createDirectories(outputPath.getParent)
    Files.writeString(
      outputPath,
      text,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    )
    println(s"Wrote ${outputPath.toAbsolutePath} (${text.getBytes("UTF-8").length} bytes)")

  // ──────────────────────────────────────────────────────────────────────
  // Helpers — small DSL for building draft-07 JSON Schema fragments.
  // ──────────────────────────────────────────────────────────────────────

  private def obj(fields: (String, Json)*): Json = Json.obj(fields*)

  private def ref(name: String): Json = obj("$ref" -> s"#/definitions/$name".asJson)

  private def enumStr(values: String*): Json =
    obj(
      "type" -> "string".asJson,
      "enum" -> Json.arr(values.map(Json.fromString)*)
    )

  // ──────────────────────────────────────────────────────────────────────
  // Definitions — one entry per domain type. Field shapes mirror the
  // serialized form produced by the codecs in sangeet-core/format/.
  // ──────────────────────────────────────────────────────────────────────

  private val noteDef     = enumStr("sa", "re", "ga", "ma", "pa", "dha", "ni")
  private val variantDef  = enumStr("shuddha", "komal", "tivra")
  private val octaveDef   = enumStr("atiMandra", "mandra", "madhya", "taar", "atiTaar")
  private val strokeDef   = enumStr("da", "ra", "jod")
  private val layaDef     = enumStr("atiVilambit", "vilambit", "madhya", "drut", "atiDrut")
  private val scriptDef   = enumStr("devanagari", "kannada", "telugu", "english")
  private val meendDirDef = enumStr("ascending", "descending")

  /** Rationals serialize as `[numerator, denominator]` int pairs (see `ModelCodecs.scala`). draft-07 supports tuple
    * validation via `items` as an array of schemas plus `minItems`/`maxItems`.
    */
  private val rationalDef = obj(
    "type"        -> "array".asJson,
    "description" -> "Rational number serialized as [numerator, denominator]".asJson,
    "items"       -> Json.arr(obj("type" -> "integer".asJson), obj("type" -> "integer".asJson)),
    "minItems"    -> 2.asJson,
    "maxItems"    -> 2.asJson
  )

  private val beatPositionDef = obj(
    "type"     -> "object".asJson,
    "required" -> Json.arr("cycle".asJson, "beat".asJson, "subdivision".asJson),
    "properties" -> obj(
      "cycle"       -> obj("type" -> "integer".asJson),
      "beat"        -> obj("type" -> "integer".asJson),
      "subdivision" -> ref("Rational")
    )
  )

  private val noteRefDef = obj(
    "type"     -> "object".asJson,
    "required" -> Json.arr("note".asJson, "variant".asJson, "octave".asJson),
    "properties" -> obj(
      "note"    -> ref("Note"),
      "variant" -> ref("Variant"),
      "octave"  -> ref("Octave")
    )
  )

  /** VibhagMarker is either the literal string "sam"/"khali" or an object `{ "taali": <int> }`. Modeled as a `oneOf`.
    */
  private val vibhagMarkerDef = obj(
    "oneOf" -> Json.arr(
      enumStr("sam", "khali"),
      obj(
        "type"       -> "object".asJson,
        "required"   -> Json.arr("taali".asJson),
        "properties" -> obj("taali" -> obj("type" -> "integer".asJson))
      )
    )
  )

  private val vibhagDef = obj(
    "type"     -> "object".asJson,
    "required" -> Json.arr("beats".asJson, "marker".asJson),
    "properties" -> obj(
      "beats"  -> obj("type" -> "integer".asJson),
      "marker" -> ref("VibhagMarker")
    )
  )

  private val taalDef = obj(
    "type"     -> "object".asJson,
    "required" -> Json.arr("name".asJson, "matras".asJson, "vibhags".asJson),
    "properties" -> obj(
      "name"    -> obj("type" -> "string".asJson),
      "matras"  -> obj("type" -> "integer".asJson),
      "vibhags" -> obj("type" -> "array".asJson, "items" -> ref("Vibhag")),
      "theka"   -> obj("type" -> "array".asJson, "items" -> obj("type" -> "string".asJson))
    )
  )

  private val raagDef = obj(
    "type"     -> "object".asJson,
    "required" -> Json.arr("name".asJson),
    "properties" -> obj(
      "name"      -> obj("type" -> "string".asJson),
      "thaat"     -> obj("type" -> "string".asJson),
      "arohana"   -> obj("type" -> "array".asJson, "items" -> obj("type" -> "string".asJson)),
      "avarohana" -> obj("type" -> "array".asJson, "items" -> obj("type" -> "string".asJson)),
      "vadi"      -> obj("type" -> "string".asJson),
      "samvadi"   -> obj("type" -> "string".asJson),
      "pakad"     -> obj("type" -> "string".asJson),
      "prahar"    -> obj("type" -> "integer".asJson)
    )
  )

  /** Ornament is a sealed trait with a `type` discriminator. draft-07 has no native discriminator, so we model each
    * branch as a separate object and union with `oneOf` — tooling can still pick the right branch by the `type` const.
    */
  private val ornamentDef = obj(
    "oneOf" -> Json.arr(
      // Meend
      obj(
        "type" -> "object".asJson,
        "required" -> Json.arr(
          "type".asJson,
          "startNote".asJson,
          "endNote".asJson,
          "direction".asJson,
          "intermediateNotes".asJson
        ),
        "properties" -> obj(
          "type"              -> obj("const" -> "meend".asJson),
          "startNote"         -> ref("NoteRef"),
          "endNote"           -> ref("NoteRef"),
          "direction"         -> ref("MeendDirection"),
          "intermediateNotes" -> obj("type" -> "array".asJson, "items" -> ref("NoteRef"))
        )
      ),
      // KanSwar
      obj(
        "type"     -> "object".asJson,
        "required" -> Json.arr("type".asJson, "graceNote".asJson),
        "properties" -> obj(
          "type"      -> obj("const" -> "kanSwar".asJson),
          "graceNote" -> ref("NoteRef")
        )
      ),
      // Murki
      obj(
        "type"     -> "object".asJson,
        "required" -> Json.arr("type".asJson, "notes".asJson),
        "properties" -> obj(
          "type"  -> obj("const" -> "murki".asJson),
          "notes" -> obj("type" -> "array".asJson, "items" -> ref("NoteRef"))
        )
      ),
      // Gamak
      obj(
        "type"       -> "object".asJson,
        "required"   -> Json.arr("type".asJson),
        "properties" -> obj("type" -> obj("const" -> "gamak".asJson))
      ),
      // Andolan
      obj(
        "type"       -> "object".asJson,
        "required"   -> Json.arr("type".asJson),
        "properties" -> obj("type" -> obj("const" -> "andolan".asJson))
      ),
      // Krintan
      obj(
        "type"     -> "object".asJson,
        "required" -> Json.arr("type".asJson, "notes".asJson),
        "properties" -> obj(
          "type"  -> obj("const" -> "krintan".asJson),
          "notes" -> obj("type" -> "array".asJson, "items" -> ref("NoteRef"))
        )
      ),
      // Gitkari
      obj(
        "type"       -> "object".asJson,
        "required"   -> Json.arr("type".asJson),
        "properties" -> obj("type" -> obj("const" -> "gitkari".asJson))
      ),
      // Ghaseet
      obj(
        "type"     -> "object".asJson,
        "required" -> Json.arr("type".asJson, "targetNote".asJson),
        "properties" -> obj(
          "type"       -> obj("const" -> "ghaseet".asJson),
          "targetNote" -> ref("NoteRef")
        )
      ),
      // Sparsh
      obj(
        "type"     -> "object".asJson,
        "required" -> Json.arr("type".asJson, "touchNote".asJson),
        "properties" -> obj(
          "type"      -> obj("const" -> "sparsh".asJson),
          "touchNote" -> ref("NoteRef")
        )
      ),
      // Zamzama
      obj(
        "type"     -> "object".asJson,
        "required" -> Json.arr("type".asJson, "notes".asJson),
        "properties" -> obj(
          "type"  -> obj("const" -> "zamzama".asJson),
          "notes" -> obj("type" -> "array".asJson, "items" -> ref("NoteRef"))
        )
      ),
      // CustomOrnament
      obj(
        "type"     -> "object".asJson,
        "required" -> Json.arr("type".asJson, "name".asJson, "parameters".asJson),
        "properties" -> obj(
          "type" -> obj("const" -> "custom".asJson),
          "name" -> obj("type" -> "string".asJson),
          "parameters" -> obj(
            "type"                 -> "object".asJson,
            "additionalProperties" -> obj("type" -> "string".asJson)
          )
        )
      )
    )
  )

  /** Event is a discriminated union by `type`: swar/rest/sustain/chikari/lockedbeat. */
  private val eventDef = obj(
    "oneOf" -> Json.arr(
      // Swar
      obj(
        "type" -> "object".asJson,
        "required" -> Json.arr(
          "type".asJson,
          "note".asJson,
          "variant".asJson,
          "octave".asJson,
          "beat".asJson,
          "duration".asJson,
          "ornaments".asJson
        ),
        "properties" -> obj(
          "type"      -> obj("const" -> "swar".asJson),
          "note"      -> ref("Note"),
          "variant"   -> ref("Variant"),
          "octave"    -> ref("Octave"),
          "beat"      -> ref("BeatPosition"),
          "duration"  -> ref("Rational"),
          "stroke"    -> ref("Stroke"),
          "ornaments" -> obj("type" -> "array".asJson, "items" -> ref("Ornament")),
          "sahitya"   -> obj("type" -> "string".asJson)
        )
      ),
      // Rest
      obj(
        "type"     -> "object".asJson,
        "required" -> Json.arr("type".asJson, "beat".asJson, "duration".asJson),
        "properties" -> obj(
          "type"     -> obj("const" -> "rest".asJson),
          "beat"     -> ref("BeatPosition"),
          "duration" -> ref("Rational")
        )
      ),
      // Sustain
      obj(
        "type"     -> "object".asJson,
        "required" -> Json.arr("type".asJson, "beat".asJson, "duration".asJson),
        "properties" -> obj(
          "type"     -> obj("const" -> "sustain".asJson),
          "beat"     -> ref("BeatPosition"),
          "duration" -> ref("Rational")
        )
      ),
      // Chikari
      obj(
        "type"     -> "object".asJson,
        "required" -> Json.arr("type".asJson, "beat".asJson, "duration".asJson),
        "properties" -> obj(
          "type"     -> obj("const" -> "chikari".asJson),
          "beat"     -> ref("BeatPosition"),
          "duration" -> ref("Rational")
        )
      ),
      // LockedBeat — emitted before the sam on cycle 0 when a section has a
      // non-default startingBeat. Deletion-guarded in the editor; round-trips
      // through .swar files like any other event.
      obj(
        "type"     -> "object".asJson,
        "required" -> Json.arr("type".asJson, "beat".asJson, "duration".asJson),
        "properties" -> obj(
          "type"     -> obj("const" -> "lockedbeat".asJson),
          "beat"     -> ref("BeatPosition"),
          "duration" -> ref("Rational")
        )
      )
    )
  )

  /** SectionType is either a known enum value or `{ "custom": <name> }`. */
  private val sectionTypeDef = obj(
    "oneOf" -> Json.arr(
      enumStr("sthayi", "antara", "sanchari", "abhog", "taan", "toda", "jhala", "palta", "arohi", "avarohi", "sargam"),
      obj(
        "type"       -> "object".asJson,
        "required"   -> Json.arr("custom".asJson),
        "properties" -> obj("custom" -> obj("type" -> "string".asJson))
      )
    )
  )

  /** CompositionType has the same shape as SectionType (string enum or custom-object). */
  private val compositionTypeDef = obj(
    "oneOf" -> Json.arr(
      enumStr("bandish", "gat", "palta", "sargam"),
      obj(
        "type"       -> "object".asJson,
        "required"   -> Json.arr("custom".asJson),
        "properties" -> obj("custom" -> obj("type" -> "string".asJson))
      )
    )
  )

  private val tihaiDef = obj(
    "type"     -> "object".asJson,
    "required" -> Json.arr("startBeat".asJson, "landingBeat".asJson),
    "properties" -> obj(
      "startBeat"   -> ref("BeatPosition"),
      "landingBeat" -> ref("BeatPosition")
    )
  )

  private val sectionDef = obj(
    "type"     -> "object".asJson,
    "required" -> Json.arr("name".asJson, "type".asJson, "events".asJson, "startingBeat".asJson),
    "properties" -> obj(
      "name"         -> obj("type" -> "string".asJson),
      "type"         -> ref("SectionType"),
      "events"       -> obj("type" -> "array".asJson, "items" -> ref("Event")),
      "tihai"        -> ref("Tihai"),
      "startingBeat" -> obj("type" -> "integer".asJson)
    )
  )

  private val metadataDef = obj(
    "type" -> "object".asJson,
    "required" -> Json.arr(
      "title".asJson,
      "compositionType".asJson,
      "raag".asJson,
      "taal".asJson,
      "createdAt".asJson,
      "updatedAt".asJson
    ),
    "properties" -> obj(
      "title"           -> obj("type" -> "string".asJson),
      "compositionType" -> ref("CompositionType"),
      "raag"            -> ref("Raag"),
      "taal"            -> ref("Taal"),
      "laya"            -> ref("Laya"),
      "script"          -> ref("SwarScript"),
      "instrument"      -> obj("type" -> "string".asJson),
      "composer"        -> obj("type" -> "string".asJson),
      "author"          -> obj("type" -> "string".asJson),
      "source"          -> obj("type" -> "string".asJson),
      "showStrokeLine"  -> obj("type" -> "boolean".asJson),
      "showSahityaLine" -> obj("type" -> "boolean".asJson),
      "createdAt"       -> obj("type" -> "string".asJson),
      "updatedAt"       -> obj("type" -> "string".asJson)
    )
  )

  // ──────────────────────────────────────────────────────────────────────
  // Top-level Composition schema + definitions table.
  // ──────────────────────────────────────────────────────────────────────

  private def buildSchema(): Json =
    obj(
      "$schema" -> "http://json-schema.org/draft-07/schema#".asJson,
      "$id"     -> "https://sangeet.varpas.com/schemas/swar.schema.json".asJson,
      "title"   -> "Sangeet Composition (.swar)".asJson,
      "description" -> "JSON Schema for .swar Hindustani composition files. Generated by SwarSchemaExporter; do not hand-edit.".asJson,
      "type"     -> "object".asJson,
      "required" -> Json.arr("metadata".asJson, "sections".asJson),
      "properties" -> obj(
        "metadata" -> ref("Metadata"),
        "sections" -> obj("type" -> "array".asJson, "items" -> ref("Section"))
      ),
      // Definitions emitted in dependency-friendly order, but JSON Schema doesn't
      // care about ordering — all `$ref`s resolve within the same document.
      "definitions" -> obj(
        "Note"            -> noteDef,
        "Variant"         -> variantDef,
        "Octave"          -> octaveDef,
        "Stroke"          -> strokeDef,
        "Laya"            -> layaDef,
        "SwarScript"      -> scriptDef,
        "MeendDirection"  -> meendDirDef,
        "Rational"        -> rationalDef,
        "BeatPosition"    -> beatPositionDef,
        "NoteRef"         -> noteRefDef,
        "VibhagMarker"    -> vibhagMarkerDef,
        "Vibhag"          -> vibhagDef,
        "Taal"            -> taalDef,
        "Raag"            -> raagDef,
        "Ornament"        -> ornamentDef,
        "Event"           -> eventDef,
        "SectionType"     -> sectionTypeDef,
        "CompositionType" -> compositionTypeDef,
        "Tihai"           -> tihaiDef,
        "Section"         -> sectionDef,
        "Metadata"        -> metadataDef
      )
    )
