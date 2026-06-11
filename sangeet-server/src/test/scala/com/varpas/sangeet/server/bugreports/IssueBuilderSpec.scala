package com.varpas.sangeet.server.bugreports

import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class IssueBuilderSpec extends AnyFlatSpec with Matchers:

  private val reportId = "11111111-2222-3333-4444-555555555555"

  "IssueBuilder.build" should "produce a title from the first line of the description" in {
    val payload = Json.obj(
      "type"        -> Json.fromString("web"),
      "description" -> Json.fromString("Cursor jumps to wrong row after delete")
    )
    val issue = IssueBuilder.build(reportId, payload, bucket = None)
    issue.title shouldBe "Bug report — Cursor jumps to wrong row after delete"
  }

  it should "truncate long descriptions in the title with an ellipsis" in {
    val long    = "a" * 200
    val payload = Json.obj("description" -> Json.fromString(long))
    val issue   = IssueBuilder.build(reportId, payload, bucket = None)
    issue.title.length should be <= ("Bug report — ".length + 60)
    issue.title should endWith("…")
  }

  it should "include a GCS console link when bucket is provided" in {
    val payload = Json.obj("description" -> Json.fromString("x"), "type" -> Json.fromString("web"))
    val issue   = IssueBuilder.build(reportId, payload, bucket = Some("my-bucket"))
    issue.body should include(s"https://console.cloud.google.com/storage/browser/_details/my-bucket/$reportId.json")
  }

  it should "omit the GCS link when bucket is None" in {
    val payload = Json.obj("description" -> Json.fromString("x"))
    val issue   = IssueBuilder.build(reportId, payload, bucket = None)
    issue.body should not include "console.cloud.google.com"
  }

  it should "derive the platform label from the type field" in {
    IssueBuilder
      .build(reportId, Json.obj("type" -> Json.fromString("web"), "description" -> Json.fromString("x")), None)
      .labels should contain("platform-web")

    IssueBuilder
      .build(reportId, Json.obj("type" -> Json.fromString("desktop"), "description" -> Json.fromString("x")), None)
      .labels should contain("platform-desktop")
  }

  it should "include email when present and skip it when blank" in {
    val withEmail = IssueBuilder.build(
      reportId,
      Json.obj("description" -> Json.fromString("x"), "email" -> Json.fromString("user@example.com")),
      None
    )
    withEmail.body should include("user@example.com")

    val blankEmail = IssueBuilder.build(
      reportId,
      Json.obj("description" -> Json.fromString("x"), "email" -> Json.fromString("   ")),
      None
    )
    blankEmail.body should not include "**Email:**"
  }

  it should "render browser metadata when present" in {
    val payload = Json.obj(
      "description" -> Json.fromString("x"),
      "metadata" -> Json.obj(
        "url"       -> Json.fromString("https://app.example.com/edit"),
        "userAgent" -> Json.fromString("Mozilla/5.0"),
        "viewportW" -> Json.fromInt(1280),
        "viewportH" -> Json.fromInt(800),
        "timestamp" -> Json.fromString("2026-06-11T10:00:00Z")
      ),
      "replay" -> Json.arr(Json.obj(), Json.obj())
    )
    val issue = IssueBuilder.build(reportId, payload, bucket = None)
    issue.body should include("https://app.example.com/edit")
    issue.body should include("Mozilla/5.0")
    issue.body should include("1280×800")
    issue.body should include("2026-06-11T10:00:00Z")
    issue.body should include("Replay events captured: 2")
  }

  it should "fall back to '(no description)' when description is missing" in {
    val issue = IssueBuilder.build(reportId, Json.obj(), bucket = None)
    issue.title shouldBe "Bug report — (no description)"
  }

  it should "always include the bug + from-user labels" in {
    val issue = IssueBuilder.build(reportId, Json.obj("description" -> Json.fromString("x")), None)
    issue.labels should contain allOf ("bug", "from-user")
  }
