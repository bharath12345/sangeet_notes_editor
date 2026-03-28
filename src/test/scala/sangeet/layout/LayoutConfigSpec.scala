package sangeet.layout

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LayoutConfigSpec extends AnyFlatSpec with Matchers:

  "LayoutConfig defaults" should "have line spacing of 40" in {
    LayoutConfig().lineSpacing shouldBe 40.0
  }

  it should "have cell width base of 60" in {
    LayoutConfig().cellWidthBase shouldBe 60.0
  }

  it should "have high density threshold of 5" in {
    LayoutConfig().highDensityThreshold shouldBe 5
  }

  it should "have cell overflow expand of 15" in {
    LayoutConfig().cellOverflowExpand shouldBe 15.0
  }

  it should "have header height of 120" in {
    LayoutConfig().headerHeight shouldBe 120.0
  }

  "LayoutConfig" should "allow custom values" in {
    val config = LayoutConfig(
      highDensityThreshold = 3,
      cellWidthBase = 80.0,
      cellOverflowExpand = 20.0,
      lineSpacing = 50.0,
      headerHeight = 100.0
    )
    config.highDensityThreshold shouldBe 3
    config.cellWidthBase shouldBe 80.0
    config.lineSpacing shouldBe 50.0
  }
