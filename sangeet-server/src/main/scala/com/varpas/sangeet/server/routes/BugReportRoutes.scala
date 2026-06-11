package com.varpas.sangeet.server.routes

import java.util.UUID

import cats.effect.IO
import io.circe.Json
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

import com.varpas.sangeet.server.bugreports.{BugReportStorage, GitHubIssuesClient, IssueBuilder}
import com.varpas.sangeet.server.endpoints.BugReportEndpoints

object BugReportRoutes:

  /** Build the route against explicit dependencies. Lets tests inject fakes without touching GCS / GitHub / env vars.
    *
    * After a successful storage write we kick off a background fiber that files a GitHub issue. It's deliberately
    * fire-and-forget: GCS is the source of truth, so a slow/broken GitHub API must not delay or break the user's
    * response.
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
              case Right(url) => IO.println(s"[bug-report] GitHub issue created: $url")
              case Left(msg)  => IO.println(s"[bug-report] GitHub issue not filed ($msg)")
            }
            .handleErrorWith(t => IO.println(s"[bug-report] GitHub issue fiber crashed: ${t.getMessage}"))
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
