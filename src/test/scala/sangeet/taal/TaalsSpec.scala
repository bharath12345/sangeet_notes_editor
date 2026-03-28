package sangeet.taal

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sangeet.model.*

class TaalsSpec extends AnyFlatSpec with Matchers:

  "Taals.teentaal" should "have 16 matras in 4 vibhags of 4" in {
    val t = Taals.teentaal
    t.matras shouldBe 16
    t.vibhags should have length 4
    t.vibhags.map(_.beats) shouldBe List(4, 4, 4, 4)
    t.vibhags.head.marker shouldBe VibhagMarker.Sam
    t.vibhags(2).marker shouldBe VibhagMarker.Khali
  }

  "Taals.ektaal" should "have 12 matras in 6 vibhags of 2" in {
    val t = Taals.ektaal
    t.matras shouldBe 12
    t.vibhags should have length 6
    t.vibhags.map(_.beats).forall(_ == 2) shouldBe true
  }

  "Taals.jhaptaal" should "have 10 matras in vibhags of 2+3+2+3" in {
    val t = Taals.jhaptaal
    t.matras shouldBe 10
    t.vibhags.map(_.beats) shouldBe List(2, 3, 2, 3)
  }

  "Taals.rupak" should "have sam on khali" in {
    val t = Taals.rupak
    t.matras shouldBe 7
    t.vibhags.head.marker shouldBe VibhagMarker.Khali
  }

  "Taals.all" should "contain all built-in taals" in {
    Taals.all.size should be >= 11
  }

  "Taals.byName" should "find taal by name case-insensitive" in {
    Taals.byName("teentaal") shouldBe Some(Taals.teentaal)
    Taals.byName("Teentaal") shouldBe Some(Taals.teentaal)
    Taals.byName("unknown") shouldBe None
  }
