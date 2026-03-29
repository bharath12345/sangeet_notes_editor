package sangeet.editor

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*

class NewCompositionDialogSpec extends AnyFlatSpec with Matchers:

  // fieldVisibility returns (showLaya, showTaanCount, showStrokeOption, showSahityaOption)

  "fieldVisibility for Gat" should "show laya, taan count, stroke, and sahitya" in {
    val (showLaya, showTaan, showStroke, showSahitya) = NewCompositionDialog.fieldVisibility(CompositionType.Gat)
    showLaya shouldBe true
    showTaan shouldBe true
    showStroke shouldBe true
    showSahitya shouldBe true
  }

  "fieldVisibility for Bandish" should "show laya, stroke, and sahitya but not taan count" in {
    val (showLaya, showTaan, showStroke, showSahitya) = NewCompositionDialog.fieldVisibility(CompositionType.Bandish)
    showLaya shouldBe true
    showTaan shouldBe false
    showStroke shouldBe true
    showSahitya shouldBe true
  }

  "fieldVisibility for Palta" should "show stroke but not laya, taan count, or sahitya" in {
    val (showLaya, showTaan, showStroke, showSahitya) = NewCompositionDialog.fieldVisibility(CompositionType.Palta)
    showLaya shouldBe false
    showTaan shouldBe false
    showStroke shouldBe true
    showSahitya shouldBe false
  }

  "fieldVisibility for Custom" should "show laya, stroke, and sahitya but not taan count" in {
    val (showLaya, showTaan, showStroke, showSahitya) = NewCompositionDialog.fieldVisibility(CompositionType.Custom("Thumri"))
    showLaya shouldBe true
    showTaan shouldBe false
    showStroke shouldBe true
    showSahitya shouldBe true
  }
