package com.varpas.sangeet.core.format

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.varpas.sangeet.core.generators.Generators.given
import com.varpas.sangeet.core.model.Composition

/** Plan 19 T1A — sample property: anchors the ScalaCheck-with-ScalaTest integration pattern for the rest of the
  * sangeet-core PBT migration. Phase B adds the bulk of properties; this file is the seed.
  *
  * Naming follows the convention in `docs/developer/testing/property-based-testing.md`:
  *   - `propXxxRoundTrip` — `decode(encode(x)) == x`.
  */
class SwarFormatPropSpec extends AnyFunSuite with ScalaCheckPropertyChecks:

  test("propCompositionRoundTrip: encode then decode == identity") {
    forAll { (c: Composition) =>
      val encoded = SwarFormat.toJson(c).noSpaces
      val decoded = SwarFormat.fromJson(encoded)
      assert(
        decoded == Right(c),
        s"Round-trip failed for composition: $c\nEncoded: $encoded\nDecoded: $decoded"
      )
    }
  }
