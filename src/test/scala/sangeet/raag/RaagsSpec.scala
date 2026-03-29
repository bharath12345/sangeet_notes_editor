package sangeet.raag

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RaagsSpec extends AnyFlatSpec with Matchers:

  "Raags.all" should "contain 26 raags" in {
    Raags.all should have size 26
  }

  it should "use lowercase keys" in {
    Raags.all.keys.foreach { key =>
      key shouldBe key.toLowerCase
    }
  }

  "Raags.byName" should "find Yaman by exact name" in {
    val raag = Raags.byName("Yaman")
    raag shouldBe defined
    raag.get.name shouldBe "Yaman"
  }

  it should "find raags case-insensitively" in {
    Raags.byName("yaman") shouldBe defined
    Raags.byName("YAMAN") shouldBe defined
    Raags.byName("YaMaN") shouldBe defined
  }

  it should "find raags with spaces in name" in {
    val raag = Raags.byName("Miyan ki Malhar")
    raag shouldBe defined
    raag.get.name shouldBe "Miyan ki Malhar"
  }

  it should "find Ahir Bhairav" in {
    val raag = Raags.byName("ahir bhairav")
    raag shouldBe defined
    raag.get.name shouldBe "Ahir Bhairav"
  }

  it should "return None for unknown raag" in {
    Raags.byName("NonExistent") shouldBe None
  }

  it should "return None for empty string" in {
    Raags.byName("") shouldBe None
  }

  it should "trim whitespace" in {
    Raags.byName("  Yaman  ") shouldBe defined
  }

  "Yaman" should "have correct thaat and key notes" in {
    val yaman = Raags.yaman
    yaman.thaat shouldBe Some("Kalyan")
    yaman.vadi shouldBe Some("Ga")
    yaman.samvadi shouldBe Some("Ni")
    yaman.prahar shouldBe Some(1)
  }

  it should "have arohana and avarohana" in {
    val yaman = Raags.yaman
    yaman.arohana shouldBe defined
    yaman.arohana.get should contain ("Sa")
    yaman.arohana.get should contain ("Ma♯")
    yaman.avarohana shouldBe defined
  }

  "Durga" should "be a pentatonic raag (5 notes in arohana)" in {
    val durga = Raags.durga
    durga.arohana shouldBe defined
    // Durga: Sa Re Ma Pa Dha Sa' — 6 entries including octave Sa
    durga.arohana.get should have length 6
    durga.thaat shouldBe Some("Bilawal")
    durga.vadi shouldBe Some("Ma")
    durga.samvadi shouldBe Some("Sa")
  }

  "Bhairavi" should "have no prahar (sung at any time)" in {
    Raags.bhairavi.prahar shouldBe None
  }

  "Pilu" should "have no pakad" in {
    Raags.pilu.pakad shouldBe None
    Raags.pilu.prahar shouldBe None
  }

  "Each raag" should "have a non-empty name" in {
    Raags.all.values.foreach { raag =>
      raag.name should not be empty
    }
  }

  it should "have a thaat" in {
    Raags.all.values.foreach { raag =>
      raag.thaat shouldBe defined
    }
  }

  it should "have arohana and avarohana" in {
    Raags.all.values.foreach { raag =>
      raag.arohana shouldBe defined
      raag.avarohana shouldBe defined
    }
  }

  it should "have vadi and samvadi" in {
    Raags.all.values.foreach { raag =>
      raag.vadi shouldBe defined
      raag.samvadi shouldBe defined
    }
  }

  "All raag names in map" should "match the raag's own name (lowercased)" in {
    Raags.all.foreach { (key, raag) =>
      key shouldBe raag.name.toLowerCase
    }
  }
