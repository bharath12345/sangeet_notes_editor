module EventCodecPropTest exposing (suite)

{-| Plan-19 Tier 4 Phase B — `Event` codec round-trip + per-variant
properties. Collapses one example per `Event` constructor (5 variants:
SwarEvent, RestEvent, SustainEvent, ChikariEvent, LockedBeatEvent) plus
the SwarEvent × ornament-list cross product.

The genesis-phase `Generators.Composition.event` is the union fuzzer;
`swarEvent` is the swar-only narrowing. We test both so the rarer
non-swar variants (1-in-5 from `event`) still get focused coverage.

-}

import Expect
import Generators.Composition exposing (event, swarEvent)
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Event exposing (encodeEvent, eventDecoder)
import Test exposing (Test, describe, fuzz)


suite : Test
suite =
    describe "Event codec"
        [ propEventRoundTrip
        , propSwarEventRoundTrip
        , propEventTypeTagPresent
        ]


propEventRoundTrip : Test
propEventRoundTrip =
    fuzz event "propEventRoundTrip: decode(encode(e)) == Ok e for any Event variant" <|
        \e ->
            encodeEvent e
                |> Encode.encode 0
                |> Decode.decodeString eventDecoder
                |> Expect.equal (Ok e)


{-| Focused property over SwarEvent — the only variant with non-trivial
fields (note, variant, octave, ornaments, stroke, sahitya). The union
fuzzer only emits this 1-in-5 runs, so we burn another 100 runs purely
on swar events to give the achal/ornament cross-product more coverage.
-}
propSwarEventRoundTrip : Test
propSwarEventRoundTrip =
    fuzz swarEvent "propSwarEventRoundTrip: decode(encode(s)) == Ok s for any SwarEvent" <|
        \s ->
            encodeEvent s
                |> Encode.encode 0
                |> Decode.decodeString eventDecoder
                |> Expect.equal (Ok s)


{-| Every encoded event carries a `type` discriminator the decoder
dispatches on. If the encoder ever drops it for a "default" variant
(e.g. RestEvent), this property fires immediately.
-}
propEventTypeTagPresent : Test
propEventTypeTagPresent =
    fuzz event "propEventTypeTagPresent: every encoded event has a string `type` field" <|
        \e ->
            encodeEvent e
                |> Encode.encode 0
                |> Decode.decodeString (Decode.field "type" Decode.string)
                |> Result.map (always True)
                |> Expect.equal (Ok True)
