module TaalDecoderPropTest exposing (suite)

{-| Plan-19 Tier 4 Phase B — codec round-trip properties for `Taal`, `Vibhag`,
and `VibhagMarker`. Collapses what otherwise would be one example per built-in
taal (×11) and one per marker variant (Sam / TaaliMarker N / KhaliMarker).

Per the project rule "taals are data, not code" — a single round-trip
property is the right abstraction here. Any future taal added to the
catalog goes through the same fuzzer shape and is automatically covered.

-}

import Expect
import Generators.Composition as CompositionGen
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Taal
    exposing
        ( encodeTaal
        , encodeVibhag
        , encodeVibhagMarker
        , taalDecoder
        , vibhagDecoder
        , vibhagMarkerDecoder
        )
import Test exposing (Test, describe, fuzz)


suite : Test
suite =
    describe "Taal / Vibhag / VibhagMarker codecs"
        [ propVibhagMarkerRoundTrip
        , propVibhagRoundTrip
        , propTaalRoundTrip
        , propTaalMatrasPreserved
        ]


propVibhagMarkerRoundTrip : Test
propVibhagMarkerRoundTrip =
    fuzz CompositionGen.vibhagMarker
        "propVibhagMarkerRoundTrip: decode(encode(m)) == Ok m"
    <|
        \m ->
            encodeVibhagMarker m
                |> Encode.encode 0
                |> Decode.decodeString vibhagMarkerDecoder
                |> Expect.equal (Ok m)


propVibhagRoundTrip : Test
propVibhagRoundTrip =
    fuzz CompositionGen.vibhag
        "propVibhagRoundTrip: decode(encode(v)) == Ok v"
    <|
        \v ->
            encodeVibhag v
                |> Encode.encode 0
                |> Decode.decodeString vibhagDecoder
                |> Expect.equal (Ok v)


propTaalRoundTrip : Test
propTaalRoundTrip =
    fuzz CompositionGen.taal
        "propTaalRoundTrip: decode(encode(t)) == Ok t"
    <|
        \t ->
            encodeTaal t
                |> Encode.encode 0
                |> Decode.decodeString taalDecoder
                |> Expect.equal (Ok t)


{-| `matras` is an Int and a non-optional field. Spot-check that the
round-trip preserves it identically — guards against any future change
that might accidentally coerce it through `Decode.float` or similar.
-}
propTaalMatrasPreserved : Test
propTaalMatrasPreserved =
    fuzz CompositionGen.taal
        "propTaalMatrasPreserved: decoded.matras == original.matras"
    <|
        \t ->
            encodeTaal t
                |> Encode.encode 0
                |> Decode.decodeString taalDecoder
                |> Result.map .matras
                |> Expect.equal (Ok t.matras)
