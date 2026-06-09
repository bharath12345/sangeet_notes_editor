package com.varpas.sangeet.core.config

import java.nio.file.Files

import io.circe.parser.{parse => parseJson}
import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AppConfigSpec extends AnyFlatSpec with Matchers:

  import ConfigCodecs.given

  "AppConfig" should "roundtrip through JSON with all fields populated" in {
    val config = AppConfig(
      bookmarks = List(
        BookmarkEntry("/home/user/raags/yaman", isDirectory = true, label = "Yaman"),
        BookmarkEntry("/home/user/raags/bhairav/gat.swar", isDirectory = false, label = "Bhairav Gat")
      ),
      openTabs = List(
        OpenTab("/home/user/raags/yaman/vilambit.swar", sectionIndex = 0),
        OpenTab("/home/user/raags/yaman/drut.swar", sectionIndex = 2)
      ),
      activeTabPath = Some("/home/user/raags/yaman/vilambit.swar"),
      leftPanelWidth = 300.0,
      leftPanelCollapsed = true
    )

    val json    = config.asJson
    val decoded = json.as[AppConfig]
    decoded shouldBe Right(config)
  }

  it should "decode empty JSON object to defaults" in {
    val json    = parseJson("{}").getOrElse(fail("Invalid JSON"))
    val decoded = json.as[AppConfig]
    decoded shouldBe Right(AppConfig())
  }

  it should "decode partial JSON with missing optional fields" in {
    val json    = parseJson("""{"leftPanelWidth": 400.0}""").getOrElse(fail("Invalid JSON"))
    val decoded = json.as[AppConfig]
    decoded shouldBe Right(AppConfig(leftPanelWidth = 400.0))
  }

  it should "decode config with bookmarks but no tabs" in {
    val json = parseJson("""{
      "bookmarks": [{"path": "/tmp/raags", "isDirectory": true, "label": "Raags"}],
      "leftPanelCollapsed": true
    }""").getOrElse(fail("Invalid JSON"))
    val decoded = json.as[AppConfig]
    decoded shouldBe Right(
      AppConfig(
        bookmarks = List(BookmarkEntry("/tmp/raags", isDirectory = true, label = "Raags")),
        leftPanelCollapsed = true
      )
    )
  }

  "BookmarkEntry" should "roundtrip through JSON" in {
    val entry = BookmarkEntry("/home/user/music", isDirectory = true, label = "Music")
    val json  = entry.asJson
    json.as[BookmarkEntry] shouldBe Right(entry)
  }

  "OpenTab" should "roundtrip through JSON" in {
    val tab  = OpenTab("/home/user/comp.swar", sectionIndex = 3)
    val json = tab.asJson
    json.as[OpenTab] shouldBe Right(tab)
  }

  "ConfigStore" should "return defaults when file does not exist" in {
    val nonExistent = Files.createTempDirectory("config-test").resolve("no-such-file.json")
    val result      = ConfigStore.loadFrom(nonExistent)
    result shouldBe AppConfig()
  }

  it should "roundtrip save and load via temp file" in {
    val tmpDir  = Files.createTempDirectory("config-test")
    val tmpFile = tmpDir.resolve("test-config.json")

    val config = AppConfig(
      bookmarks = List(BookmarkEntry("/music/yaman", isDirectory = true, label = "Yaman")),
      openTabs = List(OpenTab("/music/yaman/gat.swar", sectionIndex = 1)),
      activeTabPath = Some("/music/yaman/gat.swar"),
      leftPanelWidth = 280.0,
      leftPanelCollapsed = false
    )

    ConfigStore.saveTo(config, tmpFile)
    val loaded = ConfigStore.loadFrom(tmpFile)
    loaded shouldBe config

    Files.deleteIfExists(tmpFile)
    Files.deleteIfExists(tmpDir)
  }

  it should "handle corrupt JSON gracefully" in {
    val tmpDir  = Files.createTempDirectory("config-test")
    val tmpFile = tmpDir.resolve("corrupt.json")
    Files.writeString(tmpFile, "not valid json {{{")

    val result = ConfigStore.loadFrom(tmpFile)
    result shouldBe AppConfig()

    Files.deleteIfExists(tmpFile)
    Files.deleteIfExists(tmpDir)
  }

  it should "produce pretty-printed JSON" in {
    val tmpDir  = Files.createTempDirectory("config-test")
    val tmpFile = tmpDir.resolve("pretty.json")

    ConfigStore.saveTo(AppConfig(), tmpFile)
    val content = Files.readString(tmpFile)
    content should include("\n")
    content should include("  ")

    Files.deleteIfExists(tmpFile)
    Files.deleteIfExists(tmpDir)
  }
