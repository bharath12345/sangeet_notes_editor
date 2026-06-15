package com.varpas.sangeet.desktop.metrics

/** Symbolic helpers for emitting the Plan 18 PR-3b app-level counters from desktop call sites.
  *
  * Hardcoded counter names + label keys so the call sites can't typo against the server-side whitelist
  * (`sangeet-server/.../metrics/AppMetrics.scala`) — if the whitelist changes the lookup here will need a coordinated
  * update, but at least the desktop code can't accidentally emit `kindd=swar_insert` and get the increment dropped.
  *
  * All methods route through [[DesktopMetrics.client]] which is a daemon-thread fire-and-forget, so calling these from
  * the FX Application thread is safe.
  */
object AppMetricEvents:

  // ── EditorMutation counter ────────────────────────────────────────────

  def mutationSwarInsert(): Unit     = mutation("swar_insert")
  def mutationDelete(): Unit         = mutation("delete")
  def mutationOrnamentFinish(): Unit = mutation("ornament_finish")
  def mutationUndo(): Unit           = mutation("undo")
  def mutationRedo(): Unit           = mutation("redo")
  def mutationPaste(): Unit          = mutation("paste")
  def mutationCut(): Unit            = mutation("cut")

  private def mutation(kind: String): Unit =
    DesktopMetrics.client.increment("sangeet_editor_mutation_total", Map("kind" -> kind))

  // ── FileOp counter ────────────────────────────────────────────────────

  def fileOpen(success: Boolean): Unit       = fileOp("open", success)
  def fileSave(success: Boolean): Unit       = fileOp("save", success)
  def fileSaveAs(success: Boolean): Unit     = fileOp("save_as", success)
  def fileExportHtml(success: Boolean): Unit = fileOp("export_html", success)

  private def fileOp(op: String, success: Boolean): Unit =
    DesktopMetrics.client.increment(
      "sangeet_file_op_total",
      Map("op" -> op, "result" -> (if success then "success" else "error"))
    )

  // ── SectionSwitch / ClipboardOp / OrnamentFinish ──────────────────────

  def sectionSwitch(): Unit =
    DesktopMetrics.client.increment("sangeet_section_switch_total", Map.empty)

  def clipboardCopy(): Unit  = clipboard("copy")
  def clipboardCut(): Unit   = clipboard("cut")
  def clipboardPaste(): Unit = clipboard("paste")

  private def clipboard(op: String): Unit =
    DesktopMetrics.client.increment("sangeet_clipboard_op_total", Map("op" -> op))

  /** Bucket an internal ornament name onto the 5-value whitelist. Anything not explicitly mapped collapses to "custom"
    * so a future ornament type doesn't blow the cardinality budget — it just lands in the catch-all bucket until we
    * widen the whitelist.
    */
  def ornamentFinish(ornamentType: String): Unit =
    val label = ornamentType match
      case "Gamak" | "gamak"                                                   => "gamak"
      case "Andolan" | "andolan"                                               => "andolan"
      case "Meend" | "meend"                                                   => "meend"
      case "KanSwar" | "kanSwar" | "Sparsh" | "sparsh" | "Ghaseet" | "ghaseet" => "kan"
      case _                                                                   => "custom"
    DesktopMetrics.client.increment("sangeet_ornament_finish_total", Map("type" -> label))
