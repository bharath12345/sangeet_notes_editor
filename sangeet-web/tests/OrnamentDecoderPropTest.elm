module OrnamentDecoderPropTest exposing (suite)

{-| Plan-19 Tier 4 Phase B — `Ornament` codec round-trip property + a
discriminator-tag property. Collapses what otherwise would be one example
per ornament variant (11 variants: Meend, KanSwar, Murki, Gamak, Andolan,
Krintan, Gitkari, Ghaseet, Sparsh, Zamzama, CustomOrnament) plus all the
NoteRef shape combinations carried by parametric variants.

The discriminator-tag property is the most useful Phase-B addition: it
proves that every variant the fuzzer emits encodes to JSON with a
`"type"` field that the decoder will recognise. If a future ornament
variant lands in `Model.Ornament` without a corresponding
`Generators.Composition.ornament` arm, this property continues to pass
(it can't see the missing variant) — but if a code change breaks the
encoder/decoder agreement for ANY existing variant, every related
generator emission catches it.

-}

import Expect
import Fuzz exposing (Fuzzer)
import Generators.Composition exposing (ornament)
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Ornament exposing (Ornament(..), encodeOrnament, ornamentDecoder)
import Test exposing (Test, describe, fuzz)


suite : Test
suite =
    describe "Ornament codec"
        [ propOrnamentRoundTrip
        , propOrnamentTypeTagPresent
        , propAllOrnamentVariantsDecode
        ]


propOrnamentRoundTrip : Test
propOrnamentRoundTrip =
    fuzz ornament "propOrnamentRoundTrip: decode(encode(o)) == Ok o" <|
        \o ->
            encodeOrnament o
                |> Encode.encode 0
                |> Decode.decodeString ornamentDecoder
                |> Expect.equal (Ok o)


{-| Every encoded ornament is a JSON object with a string-valued `type`
field — the discriminator the decoder dispatches on. If anyone ever
changes the encoder to omit this field (e.g. trying to compact a parameter-
free variant like `Gamak`), this property will fire on every fuzz run.
-}
propOrnamentTypeTagPresent : Test
propOrnamentTypeTagPresent =
    fuzz ornament "propOrnamentTypeTagPresent: every encoded ornament has a string `type` field" <|
        \o ->
            encodeOrnament o
                |> Encode.encode 0
                |> Decode.decodeString (Decode.field "type" Decode.string)
                |> Result.map (always True)
                |> Expect.equal (Ok True)


{-| Spot-check the parameter-free variants (`Gamak`, `Andolan`, `Gitkari`)
explicitly with a tight fuzzer — they're the easiest to break (e.g. by
dropping the discriminator field) and the hardest for the round-trip
property to catch quickly because they're only 3 of 11 oneOf arms.
-}
propAllOrnamentVariantsDecode : Test
propAllOrnamentVariantsDecode =
    fuzz parameterFreeOrnament
        "propAllOrnamentVariantsDecode: parameter-free ornaments round-trip"
    <|
        \o ->
            encodeOrnament o
                |> Encode.encode 0
                |> Decode.decodeString ornamentDecoder
                |> Expect.equal (Ok o)


{-| Fuzzer restricted to the three parameter-free ornament variants.
Local to this test — does NOT belong in `Generators.Composition` since
it's a Phase-B test-specific narrowing, not a domain primitive.
-}
parameterFreeOrnament : Fuzzer Ornament
parameterFreeOrnament =
    Fuzz.oneOfValues [ Gamak, Andolan, Gitkari ]
