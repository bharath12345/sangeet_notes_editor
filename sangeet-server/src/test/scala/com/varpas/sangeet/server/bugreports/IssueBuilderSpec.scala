package com.varpas.sangeet.server.bugreports

import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class IssueBuilderSpec extends AnyFlatSpec with Matchers:

  private val reportId = "11111111-2222-3333-4444-555555555555"

  // Helper that defaults the new replayBaseUrl param so most tests don't have to repeat it.
  private def build(payload: Json, bucket: Option[String] = None, replayBaseUrl: Option[String] = None) =
    IssueBuilder.build(reportId, payload, bucket, replayBaseUrl)

  "IssueBuilder.build" should "produce a title from the first line of the description" in {
    val payload = Json.obj(
      "type"        -> Json.fromString("web"),
      "description" -> Json.fromString("Cursor jumps to wrong row after delete")
    )
    val issue = build(payload)
    issue.title shouldBe "Bug report — Cursor jumps to wrong row after delete"
  }

  it should "truncate long descriptions in the title with an ellipsis" in {
    val long    = "a" * 200
    val payload = Json.obj("description" -> Json.fromString(long))
    val issue   = build(payload)
    issue.title.length should be <= ("Bug report — ".length + 60)
    issue.title should endWith("…")
  }

  it should "include a GCS console link when bucket is provided" in {
    val payload = Json.obj("description" -> Json.fromString("x"), "type" -> Json.fromString("web"))
    val issue   = build(payload, bucket = Some("my-bucket"))
    issue.body should include(s"https://console.cloud.google.com/storage/browser/_details/my-bucket/$reportId.json")
  }

  it should "omit the GCS link when bucket is None" in {
    val payload = Json.obj("description" -> Json.fromString("x"))
    val issue   = build(payload)
    issue.body should not include "console.cloud.google.com"
  }

  it should "include a Play Replay link when replayBaseUrl is provided" in {
    val payload = Json.obj("description" -> Json.fromString("x"))
    val issue   = build(payload, replayBaseUrl = Some("https://server.example.com"))
    issue.body should include(s"https://server.example.com/replay/$reportId")
    issue.body should include("▶ Play replay")
  }

  it should "omit the Play Replay link when replayBaseUrl is None" in {
    val payload = Json.obj("description" -> Json.fromString("x"))
    val issue   = build(payload)
    issue.body should not include "/replay/"
    issue.body should not include "Play replay"
  }

  it should "trim a trailing slash from replayBaseUrl" in {
    val payload = Json.obj("description" -> Json.fromString("x"))
    val issue   = build(payload, replayBaseUrl = Some("https://server.example.com/"))
    issue.body should include(s"https://server.example.com/replay/$reportId")
    issue.body should not include "//replay/"
  }

  it should "derive the platform label from the type field" in {
    build(Json.obj("type" -> Json.fromString("web"), "description" -> Json.fromString("x"))).labels should
      contain("platform-web")

    build(Json.obj("type" -> Json.fromString("desktop"), "description" -> Json.fromString("x"))).labels should
      contain("platform-desktop")
  }

  it should "include email when present and skip it when blank" in {
    val withEmail = build(
      Json.obj("description" -> Json.fromString("x"), "email" -> Json.fromString("user@example.com"))
    )
    withEmail.body should include("user@example.com")

    val blankEmail = build(
      Json.obj("description" -> Json.fromString("x"), "email" -> Json.fromString("   "))
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
    val issue = build(payload)
    issue.body should include("https://app.example.com/edit")
    issue.body should include("Mozilla/5.0")
    issue.body should include("1280×800")
    issue.body should include("2026-06-11T10:00:00Z")
    issue.body should include("Replay events captured: 2")
  }

  it should "fall back to '(no description)' when description is missing" in {
    build(Json.obj()).title shouldBe "Bug report — (no description)"
  }

  it should "always include the bug + from-user labels" in {
    build(Json.obj("description" -> Json.fromString("x"))).labels should contain allOf ("bug", "from-user")
  }

  it should "add the 'crash' label and 'Crash —' title prefix when crashTrigger is true" in {
    val payload = Json.obj(
      "type"         -> Json.fromString("desktop"),
      "description"  -> Json.fromString("NullPointerException at FooBar.scala:42"),
      "crashTrigger" -> Json.fromBoolean(true)
    )
    val issue = build(payload)
    issue.labels should contain("crash")
    issue.title should startWith("Crash — ")
    issue.title should include("NullPointerException")
  }

  it should "not add the 'crash' label when crashTrigger is absent or false" in {
    build(Json.obj("description" -> Json.fromString("x"))).labels should not contain "crash"

    build(
      Json.obj("description" -> Json.fromString("x"), "crashTrigger" -> Json.fromBoolean(false))
    ).labels should not contain "crash"
  }

  // Plan 18 PR-3c — auto-capture path
  it should "tag auto-captured reports with the 'from-uncaught' label and 'Uncaught error —' title prefix" in {
    val payload = Json.obj(
      "type"        -> Json.fromString("web"),
      "source"      -> Json.fromString("uncaught"),
      "description" -> Json.fromString("Uncaught error: TypeError x.foo is not a function")
    )
    val issue = build(payload)
    issue.labels should contain("from-uncaught")
    issue.labels should not contain "from-user"
    issue.title should startWith("Uncaught error — ")
    issue.body should include("**Source:** uncaught")
  }

  it should "default the source label to 'from-user' when source is missing (backwards compat)" in {
    val issue = build(Json.obj("description" -> Json.fromString("x")))
    issue.labels should contain("from-user")
    issue.labels should not contain "from-uncaught"
    issue.body should include("**Source:** manual")
  }

  it should "treat crashTrigger as overriding the auto-captured title prefix" in {
    val payload = Json.obj(
      "type"         -> Json.fromString("desktop"),
      "source"       -> Json.fromString("uncaught"),
      "description"  -> Json.fromString("NPE"),
      "crashTrigger" -> Json.fromBoolean(true)
    )
    val issue = build(payload)
    issue.title should startWith("Crash — ")
    issue.labels should contain("crash")
    // Source label still reflects auto-capture origin even if it also crashed.
    issue.labels should contain("from-uncaught")
  }
