module CompositionCodecPropTest exposing (suite)

{-| Plan-19 Tier 4 Phase A — sangeet-web PBT genesis. One sample property
asserting that the `Composition` JSON codec is a round-trip:

    decode (encode c) == Ok c

This is THE Phase A sample property. Phase B will broaden coverage to
events, ornaments, layout invariants, etc. Adding more properties here
during Phase A is out of scope (see plan-19 §Parallelism Strategy).

-}

import Expect
import Generators.Composition exposing (composition)
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Composition exposing (compositionDecoder, encodeComposition)
import Test exposing (Test, fuzz)


suite : Test
suite =
    fuzz composition
        "propCompositionRoundTrip: decode(encode(c)) == Ok c for any generated Composition"
    <|
        \c ->
            c
                |> encodeComposition
                |> Encode.encode 0
                |> Decode.decodeString compositionDecoder
                |> Expect.equal (Ok c)
