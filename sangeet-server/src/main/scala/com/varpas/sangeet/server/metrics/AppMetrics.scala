package com.varpas.sangeet.server.metrics

import io.micrometer.core.instrument.Tags

/** App-level (as opposed to HTTP-level) counters incremented by clients via `POST /api/v1/metrics/event`.
  *
  * Phase 3b (Plan 18) — wires desktop + web mutation events, file ops, section switches, clipboard, and ornament
  * finishes into the same [[MetricsRegistry]] composite that already feeds Prometheus + Cloud Monitoring. By design,
  * clients can NEVER create arbitrary metrics: [[increment]] only accepts counters in [[AllowedCounters]] with label
  * values in the per-counter whitelist. Anything else returns `Left(reason)` and the route maps to HTTP 400.
  *
  * Cardinality budget: 5 counters × ~10 label values average = ~50 time series. Tight enough that we don't need
  * sampling/aggregation in the path; loose enough that we can add a few more label values without re-thinking. Adding a
  * new counter or label value requires editing this file (NOT a client deploy) so abuse surface is minimal.
  */
object AppMetrics:

  // ────────────────────────────────────────────────────────────────────
  // Counter names — public so tests + clients can reference them by symbol.
  // The string values are the on-the-wire identifiers; do not rename without
  // a coordinated client release.
  // ────────────────────────────────────────────────────────────────────

  val EditorMutation: String = "sangeet_editor_mutation_total"
  val FileOp: String         = "sangeet_file_op_total"
  val SectionSwitch: String  = "sangeet_section_switch_total"
  val ClipboardOp: String    = "sangeet_clipboard_op_total"
  val OrnamentFinish: String = "sangeet_ornament_finish_total"

  /** Per-counter label whitelist. Empty inner-map means "no labels expected" (e.g. SectionSwitch). Validators reject
    * unknown label keys and any value not listed here.
    */
  val AllowedCounters: Map[String, Map[String, Set[String]]] = Map(
    EditorMutation -> Map(
      "kind" -> Set(
        "swar_insert",
        "delete",
        "ornament_finish",
        "undo",
        "redo",
        "paste",
        "cut"
      )
    ),
    FileOp -> Map(
      "op"     -> Set("open", "save", "save_as", "export_html"),
      "result" -> Set("success", "error")
    ),
    SectionSwitch -> Map.empty,
    ClipboardOp -> Map(
      "op" -> Set("cut", "copy", "paste")
    ),
    OrnamentFinish -> Map(
      "type" -> Set("meend", "kan", "gamak", "andolan", "custom")
    )
  )

  /** Result type for the public [[increment]] entry point. `Left(reason)` causes the HTTP route to return 400 with a
    * diagnostic body; `Right(())` returns 204 No Content.
    */
  sealed trait ValidationError extends Product with Serializable
  object ValidationError:
    final case class UnknownCounter(name: String)                               extends ValidationError
    final case class UnknownLabelKey(counter: String, key: String)              extends ValidationError
    final case class UnknownLabelValue(counter: String, key: String, v: String) extends ValidationError
    final case class MissingLabelKey(counter: String, key: String)              extends ValidationError

    def message(e: ValidationError): String = e match
      case UnknownCounter(n)          => s"unknown counter: $n"
      case UnknownLabelKey(c, k)      => s"counter '$c' does not accept label key '$k'"
      case UnknownLabelValue(c, k, v) => s"counter '$c' label '$k' does not accept value '$v'"
      case MissingLabelKey(c, k)      => s"counter '$c' requires label key '$k'"

  /** Validate + increment in one shot. Threadsafe (Micrometer's counter is internally synchronised). */
  def increment(counter: String, labels: Map[String, String]): Either[ValidationError, Unit] =
    AllowedCounters.get(counter) match
      case None                => Left(ValidationError.UnknownCounter(counter))
      case Some(allowedLabels) =>
        // Every supplied key must be in the allowlist with a permitted value.
        labels.iterator
          .map { case (k, v) =>
            allowedLabels.get(k) match
              case None                        => Left(ValidationError.UnknownLabelKey(counter, k))
              case Some(vs) if !vs.contains(v) => Left(ValidationError.UnknownLabelValue(counter, k, v))
              case _                           => Right(())
          }
          .find(_.isLeft)
          .getOrElse(Right(()))
          .flatMap { _ =>
            // Every required key (one we listed in the allowlist) must be present.
            allowedLabels.keys
              .find(k => !labels.contains(k))
              .map(k => Left(ValidationError.MissingLabelKey(counter, k)))
              .getOrElse(Right(()))
          }
          .map { _ =>
            val tags = Tags.of(labels.toSeq.flatMap { case (k, v) => Seq(k, v) }*)
            MetricsRegistry.registry.counter(counter, tags).increment()
          }
