package com.varpas.sangeet.server.routes

import java.util.UUID

import cats.effect.IO
import io.circe.Json
import org.slf4j.LoggerFactory
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

import com.varpas.sangeet.server.bugreports.{BugReportStorage, GitHubIssuesClient, IssueBuilder}
import com.varpas.sangeet.server.endpoints.BugReportEndpoints
import com.varpas.sangeet.server.metrics.MetricsRegistry

object BugReportRoutes:

  private val log = LoggerFactory.getLogger(getClass)

  /** Counter for GitHub-issue side-effect failures. Lives alongside the route so this PR doesn't depend on a separate
    * metrics-helper module shipping first. The `reason` tag distinguishes the failure mode (api_error vs fiber_crash)
    * so an investigator can tell a 4xx from a thrown exception without grep'ing logs.
    *
    * Backwards-compatible name: `sangeet_bug_report_github_failure_total` (Micrometer normalizes the underscore form
    * for Prometheus).
    */
  private val githubFailureCounter =
    MetricsRegistry.composite.counter("sangeet.bug_report.github_failure")

  /** Build the route against explicit dependencies. Lets tests inject fakes without touching GCS / GitHub / env vars.
    *
    * After a successful storage write we kick off a background fiber that files a GitHub issue. It's deliberately
    * fire-and-forget: GCS is the source of truth, so a slow/broken GitHub API must not delay or break the user's
    * response.
    *
    * Failure paths: api_error (GitHub returned non-2xx) and fiber_crash (e.g. network IO threw) are both:
    *   - logged via slf4j with the issue title + reason so investigators can grep `slf4j-simple` stderr or Cloud
    *     Logging for the structured event
    *   - counted in `sangeet.bug_report.github_failure` (Micrometer composite registry — flows to both Prometheus
    *     scrape and Cloud Monitoring), so an alert can fire when this rate spikes
    *
    * The fire-and-forget shape is preserved: the user's response status is unchanged regardless of GitHub outcome.
    */
  def createBugReport(
      storage: BugReportStorage,
      issues: GitHubIssuesClient,
      gcsBucket: Option[String],
      replayBaseUrl: Option[String]
  ): ServerEndpoint[Any, IO] =
    BugReportEndpoints.createBugReport.serverLogic { body =>
      val reportId = UUID.randomUUID().toString
      storage.store(reportId, body).flatMap {
        case Right(_) =>
          val issue = IssueBuilder.build(reportId, body, gcsBucket, replayBaseUrl)
          val fileIssue = issues
            .createIssue(issue.title, issue.body, issue.labels)
            .flatMap {
              case Right(url) =>
                IO(log.info(s"[bug-report] GitHub issue created (reportId=$reportId): $url"))
              case Left(msg) =>
                IO {
                  log.warn(
                    s"[bug-report] GitHub issue not filed (reportId=$reportId, title='${issue.title}'): $msg"
                  )
                  githubFailureCounter.increment()
                }
            }
            .handleErrorWith { t =>
              IO {
                log.error(
                  s"[bug-report] GitHub issue fiber crashed (reportId=$reportId, title='${issue.title}'): " +
                    s"${t.getClass.getSimpleName}: ${t.getMessage}",
                  t
                )
                githubFailureCounter.increment()
              }
            }
          fileIssue.start.as(
            Right(
              Json.obj(
                "reportId" -> Json.fromString(reportId),
                "status"   -> Json.fromString("received")
              )
            )
          )
        case Left(msg) =>
          IO.pure(
            Left(
              (
                StatusCode.ServiceUnavailable,
                Json.obj(
                  "error"   -> Json.fromString("bug_report_storage_failed"),
                  "message" -> Json.fromString(msg)
                )
              )
            )
          )
      }
    }

  /** Default route, wired to env-configured deps. Used by Main.scala via [[AllRoutes]]. */
  private lazy val defaultStorage: BugReportStorage  = BugReportStorage.fromEnv
  private lazy val defaultIssues: GitHubIssuesClient = GitHubIssuesClient.fromEnv
  private lazy val defaultBucket: Option[String]     = sys.env.get("BUG_REPORTS_BUCKET")
  // `REPLAY_BASE_URL` is the externally-visible origin of this sangeet-server
  // (no trailing slash), used to build the "▶ Play replay" link in the
  // GitHub issue body. When unset, the link is just omitted.
  private lazy val defaultReplayBase: Option[String] = sys.env.get("REPLAY_BASE_URL").filter(_.nonEmpty)

  val all: List[ServerEndpoint[Any, IO]] =
    List(createBugReport(defaultStorage, defaultIssues, defaultBucket, defaultReplayBase))
