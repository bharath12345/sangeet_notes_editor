package com.varpas.sangeet.core.raag

import com.varpas.sangeet.core.model.Raag
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class RaagsSpec extends AnyFunSuite with Matchers:

  test("all raags should be accessible"):
    Raags.all.size shouldBe 26

  test("all raags should have correct names"):
    val expectedNames = Set(
      "yaman", "bhairav", "durga", "bhupali", "malkauns", "bageshree",
      "desh", "kafi", "bihag", "kedar", "hansadhwani", "jaunpuri",
      "todi", "marwa", "puriya", "shree", "miyan ki malhar", "megh",
      "pilu", "khamaj", "bilawal", "bhairavi", "asavari", "ahir bhairav",
      "hindol", "madmad sarang"
    )
    Raags.all.keySet shouldBe expectedNames

  test("yaman should have correct properties"):
    Raags.yaman.name shouldBe "Yaman"
    Raags.yaman.thaat shouldBe Some("Kalyan")
    Raags.yaman.vadi shouldBe Some("Ga")
    Raags.yaman.samvadi shouldBe Some("Ni")
    Raags.yaman.prahar shouldBe Some(1)

  test("yaman should have arohana defined"):
    Raags.yaman.arohana shouldBe defined
    Raags.yaman.arohana.get shouldBe List("Sa", "Re", "Ga", "Ma♯", "Pa", "Dha", "Ni", "Sa'")

  test("yaman should have avarohana defined"):
    Raags.yaman.avarohana shouldBe defined
    Raags.yaman.avarohana.get shouldBe List("Sa'", "Ni", "Dha", "Pa", "Ma♯", "Ga", "Re", "Sa")

  test("bhairav should have correct properties"):
    Raags.bhairav.name shouldBe "Bhairav"
    Raags.bhairav.thaat shouldBe Some("Bhairav")
    Raags.bhairav.vadi shouldBe Some("Dha")
    Raags.bhairav.samvadi shouldBe Some("Re")

  test("rupak (unusual taal case) should have correct arohana"):
    Raags.durga.arohana shouldBe defined
    Raags.durga.arohana.get shouldBe List("Sa", "Re", "Ma", "Pa", "Dha", "Sa'")

  test("byName should find raags case-insensitively"):
    Raags.byName("Yaman") shouldBe Some(Raags.yaman)
    Raags.byName("BHAIRAV") shouldBe Some(Raags.bhairav)
    Raags.byName("durga") shouldBe Some(Raags.durga)

  test("byName should handle whitespace"):
    Raags.byName("  Yaman  ") shouldBe Some(Raags.yaman)

  test("byName should return None for non-existent raag"):
    Raags.byName("NonExistent") shouldBe None

  test("all raags should have name defined"):
    Raags.all.values.foreach { raag =>
      raag.name should not be empty
    }

  test("all raags should have arohana defined"):
    Raags.all.values.foreach { raag =>
      raag.arohana shouldBe defined
      raag.arohana.get should not be empty
    }

  test("all raags should have avarohana defined"):
    Raags.all.values.foreach { raag =>
      raag.avarohana shouldBe defined
      raag.avarohana.get should not be empty
    }

  test("all raags should have thaat defined"):
    Raags.all.values.foreach { raag =>
      raag.thaat shouldBe defined
    }

  test("all raags should have vadi defined"):
    Raags.all.values.foreach { raag =>
      raag.vadi shouldBe defined
    }

  test("all raags should have samvadi defined"):
    Raags.all.values.foreach { raag =>
      raag.samvadi shouldBe defined
    }

  test("specific raags should have pakad defined"):
    Raags.yaman.pakad shouldBe defined
    Raags.bhairav.pakad shouldBe defined
    Raags.durga.pakad shouldBe defined

  test("specific raags should have prahar defined"):
    Raags.yaman.prahar shouldBe Some(1)
    Raags.bhupali.prahar shouldBe Some(1)
    Raags.malkauns.prahar shouldBe Some(3)

  test("some raags may not have prahar (like Durga)"):
    Raags.durga.prahar shouldBe None
    Raags.hansadhwani.prahar shouldBe None

  test("some raags may not have pakad (like Pilu)"):
    Raags.pilu.pakad shouldBe None

  test("all raags with prahar should have valid prahar (1-4)"):
    Raags.all.values.foreach { raag =>
      raag.prahar.foreach { p =>
        p should (be >= 1 and be <= 4)
      }
    }

  test("malkauns should be a night raag (prahar 3)"):
    Raags.malkauns.prahar shouldBe Some(3)

  test("bhupali should be an evening raag (prahar 1)"):
    Raags.bhupali.prahar shouldBe Some(1)

  test("bageshree should be a night raag (prahar 2)"):
    Raags.bageshree.prahar shouldBe Some(2)

  test("kafi should be a late night raag (prahar 3)"):
    Raags.kafi.prahar shouldBe Some(3)

  test("miyan ki malhar should be a monsoon raag"):
    Raags.miyanKiMalhar.name shouldBe "Miyan ki Malhar"
    Raags.miyanKiMalhar.thaat shouldBe Some("Kafi")
