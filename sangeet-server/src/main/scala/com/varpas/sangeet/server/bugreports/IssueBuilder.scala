package com.varpas.sangeet.server.bugreports

import io.circe.Json

/** Pure builder that turns a stored bug-report payload into the (title, body, labels) of a GitHub issue. Kept separate
  * from [[GitHubIssuesClient]] so it can be unit-tested without any HTTP, and so the same builder can be reused for the
  * desktop sender once it lands.
  */
object IssueBuilder:

  final case class Issue(title: String, body: String, labels: List[String])

  private val MaxTitleSnippet = 60

  /** @param reportId
    *   the UUID minted by the route — used to build the GCS console + replay viewer links
    * @param payload
    *   the JSON body the client POSTed; fields are best-effort optional
    * @param bucket
    *   the bucket name where the payload was written; if `None`, omits the GCS link
    * @param replayBaseUrl
    *   base URL of the deployed sangeet-server (no trailing slash); if `None`, omits the "▶ Play replay" link. The
    *   resulting URL is `<replayBaseUrl>/replay/<reportId>` and lives behind Basic Auth (Phase 6).
    */
  def build(reportId: String, payload: Json, bucket: Option[String], replayBaseUrl: Option[String]): Issue =
    val c = payload.hcursor

    val description  = c.get[String]("description").toOption.getOrElse("(no description)")
    val email        = c.get[String]("email").toOption.filter(_.trim.nonEmpty)
    val platform     = c.get[String]("type").toOption.getOrElse("unknown")
    val crashTrigger = c.get[Boolean]("crashTrigger").toOption.getOrElse(false)
    // Plan 18 PR-3c: `source` distinguishes auto-captured (uncaught JS error)
    // reports from user-initiated ones. Defaults to "manual" for backwards
    // compatibility with reports submitted before this field existed.
    val source       = c.get[String]("source").toOption.filter(_.trim.nonEmpty).getOrElse("manual")
    val autoCaptured = source == "uncaught"

    val metadata    = c.downField("metadata")
    val pageUrl     = metadata.get[String]("url").toOption
    val userAgent   = metadata.get[String]("userAgent").toOption
    val viewportW   = metadata.get[Int]("viewportW").toOption
    val viewportH   = metadata.get[Int]("viewportH").toOption
    val timestamp   = metadata.get[String]("timestamp").toOption
    val replayCount = c.downField("replay").values.map(_.size)

    // Title prefix priority: crash > auto-captured > bug-report. Crashes are
    // the loudest signal and override the auto-captured prefix on the rare
    // overlap (a desktop crash report happens to flow through this builder
    // with `source: "uncaught"` from a future hypothetical client). For
    // today's web flow they're disjoint.
    val titlePrefix =
      if crashTrigger then "Crash — "
      else if autoCaptured then "Uncaught error — "
      else "Bug report — "
    val title = titlePrefix + truncate(description.replace('\n', ' ').trim, MaxTitleSnippet)

    val sb = new StringBuilder
    sb.append("**Description**\n\n").append(description.trim).append("\n\n")
    replayBaseUrl.foreach { base =>
      val viewerUrl = s"${base.stripSuffix("/")}/replay/$reportId"
      sb.append("**[▶ Play replay](").append(viewerUrl).append(")** _(login required)_\n\n")
    }
    email.foreach(e => sb.append("**Email:** ").append(e).append("\n"))
    sb.append("**Report ID:** `").append(reportId).append("`\n")
    sb.append("**Platform:** ").append(platform).append("\n")
    sb.append("**Source:** ").append(source).append("\n")
    timestamp.foreach(t => sb.append("**Submitted:** ").append(t).append("\n"))
    sb.append("\n**Browser context**\n\n")
    pageUrl.foreach(u => sb.append("- URL: ").append(u).append("\n"))
    userAgent.foreach(ua => sb.append("- User-Agent: `").append(ua).append("`\n"))
    (viewportW, viewportH) match
      case (Some(w), Some(h)) => sb.append(s"- Viewport: ${w}×${h}\n")
      case _                  => ()
    replayCount.foreach(n => sb.append(s"- Replay events captured: $n\n"))

    bucket.foreach { b =>
      val consoleUrl = s"https://console.cloud.google.com/storage/browser/_details/$b/$reportId.json"
      sb.append("\n**Raw payload (rrweb replay + metadata)**\n\n")
      sb.append("[`").append(reportId).append(".json`](").append(consoleUrl).append(")\n")
    }

    sb.append("\n---\n_Filed automatically by sangeet-server bug-report endpoint._\n")

    val platformLabel = "platform-" + platform
    // `from-user` for human-submitted reports, `from-uncaught` for the
    // auto-capture path. Triagers filter on these to separate signal: a
    // burst of auto-captured reports usually means a regression, while
    // user-submitted ones are individually triageable feedback.
    val sourceLabel = if autoCaptured then "from-uncaught" else "from-user"
    val baseLabels  = List("bug", sourceLabel, platformLabel)
    val labels      = if crashTrigger then baseLabels :+ "crash" else baseLabels
    Issue(title = title, body = sb.toString, labels = labels)

  private def truncate(s: String, max: Int): String =
    if s.length <= max then s
    else s.take(max - 1) + "…"
