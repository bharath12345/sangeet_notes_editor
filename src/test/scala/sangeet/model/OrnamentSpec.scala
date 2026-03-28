package sangeet.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OrnamentSpec extends AnyFlatSpec with Matchers:

  val saRef = NoteRef(Note.Sa, Variant.Shuddha, Octave.Madhya)
  val reRef = NoteRef(Note.Re, Variant.Shuddha, Octave.Madhya)
  val gaRef = NoteRef(Note.Ga, Variant.Shuddha, Octave.Madhya)

  "Meend" should "have start, end, direction, and intermediate notes" in {
    val m = Meend(saRef, gaRef, MeendDirection.Ascending, List(reRef))
    m.startNote shouldBe saRef
    m.endNote shouldBe gaRef
    m.direction shouldBe MeendDirection.Ascending
    m.intermediateNotes shouldBe List(reRef)
  }

  "KanSwar" should "hold a grace note" in {
    val k = KanSwar(reRef)
    k.graceNote shouldBe reRef
  }

  "Murki" should "hold a sequence of notes" in {
    val m = Murki(List(gaRef, reRef, saRef))
    m.notes should have length 3
  }

  "CustomOrnament" should "hold arbitrary parameters" in {
    val c = CustomOrnament("newTechnique", Map("speed" -> "fast", "intensity" -> "high"))
    c.name shouldBe "newTechnique"
    c.parameters("speed") shouldBe "fast"
  }

  "All ornament types" should "be subtypes of Ornament" in {
    val ornaments: List[Ornament] = List(
      Meend(saRef, gaRef, MeendDirection.Ascending, Nil),
      KanSwar(reRef),
      Murki(List(saRef)),
      Gamak(),
      Andolan(),
      Krintan(List(gaRef, reRef)),
      Gitkari(),
      Ghaseet(gaRef),
      Sparsh(reRef),
      Zamzama(List(saRef, reRef)),
      CustomOrnament("test", Map.empty)
    )
    ornaments should have length 11
  }
