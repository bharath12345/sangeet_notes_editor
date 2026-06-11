package com.varpas.sangeet.server.routes

import java.util.UUID

import cats.effect.IO
import io.circe.Json
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

import com.varpas.sangeet.server.bugreports.BugReportStorage
import com.varpas.sangeet.server.endpoints.BugReportEndpoints

object BugReportRoutes:

  /** Build the route against an explicit storage. Lets tests inject a fake storage without touching GCS or env vars.
    */
  def createBugReport(storage: BugReportStorage): ServerEndpoint[Any, IO] =
    BugReportEndpoints.createBugReport.serverLogic { body =>
      val reportId = UUID.randomUUID().toString
      storage.store(reportId, body).map {
        case Right(_) =>
          Right(
            Json.obj(
              "reportId" -> Json.fromString(reportId),
              "status"   -> Json.fromString("received")
            )
          )
        case Left(msg) =>
          Left(
            (
              StatusCode.ServiceUnavailable,
              Json.obj(
                "error"   -> Json.fromString("bug_report_storage_failed"),
                "message" -> Json.fromString(msg)
              )
            )
          )
      }
    }

  /** Default route, wired to the env-configured storage. Used by Main.scala via [[AllRoutes]]. */
  private lazy val defaultStorage: BugReportStorage = BugReportStorage.fromEnv

  val all: List[ServerEndpoint[Any, IO]] = List(createBugReport(defaultStorage))
