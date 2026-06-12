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

    val metadata    = c.downField("metadata")
    val pageUrl     = metadata.get[String]("url").toOption
    val userAgent   = metadata.get[String]("userAgent").toOption
    val viewportW   = metadata.get[Int]("viewportW").toOption
    val viewportH   = metadata.get[Int]("viewportH").toOption
    val timestamp   = metadata.get[String]("timestamp").toOption
    val replayCount = c.downField("replay").values.map(_.size)

    val titlePrefix = if crashTrigger then "Crash — " else "Bug report — "
    val title       = titlePrefix + truncate(description.replace('\n', ' ').trim, MaxTitleSnippet)

    val sb = new StringBuilder
    sb.append("**Description**\n\n").append(description.trim).append("\n\n")
    replayBaseUrl.foreach { base =>
      val viewerUrl = s"${base.stripSuffix("/")}/replay/$reportId"
      sb.append("**[▶ Play replay](").append(viewerUrl).append(")** _(login required)_\n\n")
    }
    email.foreach(e => sb.append("**Email:** ").append(e).append("\n"))
    sb.append("**Report ID:** `").append(reportId).append("`\n")
    sb.append("**Platform:** ").append(platform).append("\n")
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
    val baseLabels    = List("bug", "from-user", platformLabel)
    val labels        = if crashTrigger then baseLabels :+ "crash" else baseLabels
    Issue(title = title, body = sb.toString, labels = labels)

  private def truncate(s: String, max: Int): String =
    if s.length <= max then s
    else s.take(max - 1) + "…"
