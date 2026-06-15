package com.varpas.sangeet.desktop.editor

import com.varpas.sangeet.core.config.{AppConfig, BookmarkEntry, ConfigCodecs, OpenTab}
import io.circe.parser.decode
import io.circe.syntax.*
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/** Plan 19, Tier 3 (sangeet-desktop), Phase A — PBT genesis.
  *
  * Desktop's PBT scope is intentionally minimal (only ~3 files per the plan); this spec exists to anchor the pattern
  * and prove ScalaCheck integrates with the existing ScalaTest suite. A real shared `Generators` module lives in
  * sangeet-core (added in T1A); desktop reuses it where possible and inlines small generators (like the one below)
  * where it doesn't.
  *
  * The single property covers the canonical first PBT shape: JSON round-trip through `ConfigCodecs` for `AppConfig` —
  * the type that owns session persistence (open tabs, bookmarks, panel state, theme).
  */
class AppConfigPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  import ConfigCodecs.given

  private val genBookmarkEntry: Gen[BookmarkEntry] =
    for
      path        <- Gen.alphaNumStr.suchThat(_.nonEmpty)
      isDirectory <- Arbitrary.arbitrary[Boolean]
      label       <- Gen.alphaNumStr.suchThat(_.nonEmpty)
    yield BookmarkEntry(path, isDirectory, label)

  private val genOpenTab: Gen[OpenTab] =
    for
      filePath     <- Gen.alphaNumStr.suchThat(_.nonEmpty)
      sectionIndex <- Gen.choose(0, 32)
    yield OpenTab(filePath, sectionIndex)

  given Arbitrary[AppConfig] = Arbitrary {
    for
      bookmarks            <- Gen.listOfN(3, genBookmarkEntry).flatMap(b => Gen.choose(0, 3).map(b.take))
      openTabs             <- Gen.listOfN(3, genOpenTab).flatMap(t => Gen.choose(0, 3).map(t.take))
      activeTabPath        <- Gen.option(Gen.alphaNumStr.suchThat(_.nonEmpty))
      leftPanelWidth       <- Gen.choose(100.0, 800.0)
      leftPanelCollapsed   <- Arbitrary.arbitrary[Boolean]
      bottomPanelCollapsed <- Arbitrary.arbitrary[Boolean]
      rightPanelCollapsed  <- Arbitrary.arbitrary[Boolean]
      theme                <- Gen.oneOf("light", "dark")
      showSampleOnStartup  <- Arbitrary.arbitrary[Boolean]
    yield AppConfig(
      bookmarks = bookmarks,
      openTabs = openTabs,
      activeTabPath = activeTabPath,
      leftPanelWidth = leftPanelWidth,
      leftPanelCollapsed = leftPanelCollapsed,
      bottomPanelCollapsed = bottomPanelCollapsed,
      rightPanelCollapsed = rightPanelCollapsed,
      theme = theme,
      showSampleOnStartup = showSampleOnStartup
    )
  }

  test("propAppConfigRoundTrip: encode then decode == identity") {
    forAll { (config: AppConfig) =>
      val encoded = config.asJson.noSpaces
      val decoded = decode[AppConfig](encoded)
      assert(decoded == Right(config))
    }
  }
