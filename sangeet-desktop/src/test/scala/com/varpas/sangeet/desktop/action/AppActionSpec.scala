package com.varpas.sangeet.desktop.action

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AppActionSpec extends AnyFlatSpec with Matchers:

  private val actions = List(
    AppAction("New composition", "File", Some("⌘N"), () => ()),
    AppAction("Open file", "File", Some("⌘O"), () => ()),
    AppAction("Save", "File", Some("⌘S"), () => ()),
    AppAction("Add section", "Sections", Some("⌘⇧A"), () => ()),
    AppAction("Toggle theme", "View", Some("⌘⇧T"), () => ())
  )

  "AppAction.filter" should "return all actions for an empty query" in {
    AppAction.filter(actions, "") shouldBe actions
  }

  it should "match by title substring (case-insensitive)" in {
    AppAction.filter(actions, "save").map(_.title) shouldBe List("Save")
    AppAction.filter(actions, "SAVE").map(_.title) shouldBe List("Save")
  }

  it should "match by group name" in {
    AppAction.filter(actions, "file").map(_.title) shouldBe List("New composition", "Open file", "Save")
  }

  it should "preserve the original order of matches" in {
    AppAction.filter(actions, "n").map(_.title) shouldBe List("New composition", "Open file", "Add section")
  }

  it should "trim whitespace from the query" in {
    AppAction.filter(actions, "  save  ").map(_.title) shouldBe List("Save")
  }

  it should "return empty for a query that matches nothing" in {
    AppAction.filter(actions, "zzz") shouldBe empty
  }
