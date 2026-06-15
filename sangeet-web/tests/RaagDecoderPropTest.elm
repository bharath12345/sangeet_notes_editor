module RaagDecoderPropTest exposing (suite)

{-| Plan-19 Tier 4 Phase B — `Raag` codec round-trip property. Replaces what
otherwise would be one example per built-in raag (×26) or per `Maybe` field
combination (×2⁶) with a single property over every shape `Generators.Composition.raag`
can emit.

The fuzzer covers the full record shape: name, thaat (optional), arohana /
avarohana (optional list of strings), vadi / samvadi / pakad (optional
strings), and prahar (optional int 1–8). 100 fuzz runs is enough to hit
each Maybe present/absent combination roughly 1-2× given the geometric
distribution `Fuzz.maybe` uses.

-}

import Expect
import Generators.Composition exposing (raag)
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Raag exposing (encodeRaag, raagDecoder)
import Test exposing (Test, describe, fuzz)


suite : Test
suite =
    describe "Raag codec"
        [ propRaagRoundTrip
        , propRaagNameStable
        ]


propRaagRoundTrip : Test
propRaagRoundTrip =
    fuzz raag "propRaagRoundTrip: decode(encode(r)) == Ok r" <|
        \r ->
            encodeRaag r
                |> Encode.encode 0
                |> Decode.decodeString raagDecoder
                |> Expect.equal (Ok r)


{-| The `name` field is the only one without a Maybe wrapper — assert it
survives the round-trip identically, independent of which optional fields
were emitted. Acts as a sanity check that the codec doesn't lossy-trim
arbitrary names (e.g. with leading/trailing spaces, dashes, dots from
the bounded ASCII alphabet).
-}
propRaagNameStable : Test
propRaagNameStable =
    fuzz raag "propRaagNameStable: decoded.name == original.name" <|
        \r ->
            encodeRaag r
                |> Encode.encode 0
                |> Decode.decodeString raagDecoder
                |> Result.map .name
                |> Expect.equal (Ok r.name)
