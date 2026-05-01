package com.varpas.sangeet.core.taal

import com.varpas.sangeet.core.model.{Taal, Vibhag, VibhagMarker}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TaalsSpec extends AnyFunSuite with Matchers:

  test("all taals should be accessible"):
    Taals.all.size shouldBe 11

  test("all taals should have correct names"):
    val expectedNames = Set(
      "teentaal", "ektaal", "jhaptaal", "rupak", "dadra", "keherwa",
      "chautaal", "dhamar", "tilwada", "jhoomra", "deepchandi"
    )
    Taals.all.keySet shouldBe expectedNames

  test("teentaal should have 16 matras"):
    Taals.teentaal.matras shouldBe 16

  test("teentaal should have correct vibhag structure"):
    Taals.teentaal.vibhags should have size 4
    Taals.teentaal.vibhags(0) shouldBe Vibhag(4, VibhagMarker.Sam)
    Taals.teentaal.vibhags(1) shouldBe Vibhag(4, VibhagMarker.Taali(2))
    Taals.teentaal.vibhags(2) shouldBe Vibhag(4, VibhagMarker.Khali)
    Taals.teentaal.vibhags(3) shouldBe Vibhag(4, VibhagMarker.Taali(3))

  test("teentaal should have theka"):
    Taals.teentaal.theka shouldBe defined
    Taals.teentaal.theka.get should have size 16

  test("ektaal should have 12 matras"):
    Taals.ektaal.matras shouldBe 12

  test("ektaal should have correct vibhag structure"):
    Taals.ektaal.vibhags should have size 6

  test("jhaptaal should have 10 matras"):
    Taals.jhaptaal.matras shouldBe 10

  test("jhaptaal should have correct vibhag structure"):
    Taals.jhaptaal.vibhags should have size 4

  test("rupak should have 7 matras"):
    Taals.rupak.matras shouldBe 7

  test("rupak should start with khali (special case)"):
    Taals.rupak.vibhags(0).marker shouldBe VibhagMarker.Khali

  test("dadra should have 6 matras"):
    Taals.dadra.matras shouldBe 6

  test("dadra should have correct vibhag structure"):
    Taals.dadra.vibhags should have size 2

  test("keherwa should have 8 matras"):
    Taals.keherwa.matras shouldBe 8

  test("keherwa should have correct vibhag structure"):
    Taals.keherwa.vibhags should have size 2

  test("chautaal should have 12 matras"):
    Taals.chautaal.matras shouldBe 12

  test("dhamar should have 14 matras"):
    Taals.dhamar.matras shouldBe 14

  test("tilwada should have 16 matras"):
    Taals.tilwada.matras shouldBe 16

  test("jhoomra should have 14 matras"):
    Taals.jhoomra.matras shouldBe 14

  test("deepchandi should have 14 matras"):
    Taals.deepchandi.matras shouldBe 14

  test("byName should find taals case-insensitively"):
    Taals.byName("Teentaal") shouldBe Some(Taals.teentaal)
    Taals.byName("EKTAAL") shouldBe Some(Taals.ektaal)
    Taals.byName("rupak") shouldBe Some(Taals.rupak)

  test("byName should return None for non-existent taal"):
    Taals.byName("NonExistent") shouldBe None

  test("all taals should have theka defined"):
    Taals.all.values.foreach { taal =>
      taal.theka shouldBe defined
    }

  test("theka length should match matras for all taals"):
    Taals.all.values.foreach { taal =>
      taal.theka.foreach { theka =>
        theka.size shouldBe taal.matras
      }
    }

  test("vibhag beats should sum to matras for all taals"):
    Taals.all.values.foreach { taal =>
      val totalBeats = taal.vibhags.map(_.beats).sum
      totalBeats shouldBe taal.matras
    }

  test("all taals should have sam as first vibhag marker except rupak"):
    Taals.all.values.foreach { taal =>
      if taal.name != "Rupak" then
        taal.vibhags.head.marker shouldBe VibhagMarker.Sam
    }
