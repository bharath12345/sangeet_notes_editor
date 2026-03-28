package sangeet

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SanitySpec extends AnyFlatSpec with Matchers:
  "The project" should "compile and run tests" in {
    1 + 1 shouldBe 2
  }
