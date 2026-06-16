module DecoderFaultTolerancePropTest exposing (suite)

{-| Plan-19 Tier 4 Phase C — fault-tolerance properties for the
sangeet-web JSON decoders.

Elm decoders are total — `Decode.decodeString` always returns
`Result Decode.Error a`, never crashes. These properties make that
guarantee EXPLICIT for our model decoders by feeding them deliberately
malformed inputs and asserting they fall through to `Err _`:

  - **Random garbage strings decode to `Err`** — for any random
    short ASCII string, every model decoder either succeeds (by
    coincidence — e.g. `"null"` for a `Maybe` field) or fails with
    `Err`. The runtime never crashes.
  - **Empty JSON `{}` decodes to `Err` for record decoders** that
    require at least one field.
  - **Wrong-shape JSON decodes to `Err` for tagged-union decoders**
    (Event, Ornament) — passing `{"type": "doesNotExist"}` must
    produce `Err`, not silently default.
  - **JSON array passed to a record decoder decodes to `Err`** —
    type-mismatch surfaces as an error.

These act as a regression net for any future decoder change that
accidentally introduces a partial pattern match (e.g. by switching
from `Decode.andThen` to a non-exhaustive case).

-}

import Expect
import Fuzz exposing (Fuzzer)
import Json.Decode as Decode
import Model.Composition exposing (compositionDecoder)
import Model.Cursor exposing (cursorDecoder)
import Model.Event exposing (eventDecoder)
import Model.Ornament exposing (ornamentDecoder)
import Model.Raag exposing (raagDecoder)
import Model.Taal exposing (taalDecoder)
import Test exposing (Test, describe, fuzz, test)



-- LOCAL FUZZERS


{-| Garbage string fuzzer: random ASCII, never valid JSON for our records.
We intentionally include bytes that COULD form syntactically valid JSON
(braces, quotes, commas, digits) so the fuzzer occasionally hits the
parser successfully — what matters is the decoder never crashes the VM.
-}
garbageString : Fuzzer String
garbageString =
    Fuzz.asciiStringOfLengthBetween 0 32



-- PROPERTIES


suite : Test
suite =
    describe "Decoder fault tolerance"
        [ propCompositionDecoderTotal
        , propCursorDecoderTotal
        , propEventDecoderTotal
        , propOrnamentDecoderTotal
        , propRaagDecoderTotal
        , propTaalDecoderTotal
        , describe "Empty-object / wrong-shape regression"
            [ testEmptyObjectFailsForRequiredRecords
            , testUnknownEventTypeFails
            , testUnknownOrnamentTypeFails
            , testArrayFailsForRecordDecoders
            ]
        ]



-- TOTALITY: random ASCII input never crashes. We assert "the decoder
-- returned SOMETHING" — either Ok (rare; happens when the random
-- string parses as valid JSON that matches the shape) or Err. The
-- contract is "no crash"; we use a trivial Expect.pass after pattern-
-- matching to enforce totality without locking in which result.


propCompositionDecoderTotal : Test
propCompositionDecoderTotal =
    fuzz garbageString
        "propCompositionDecoderTotal: random input never crashes the composition decoder"
    <|
        \s ->
            case Decode.decodeString compositionDecoder s of
                Ok _ ->
                    Expect.pass

                Err _ ->
                    Expect.pass


propCursorDecoderTotal : Test
propCursorDecoderTotal =
    fuzz garbageString
        "propCursorDecoderTotal: random input never crashes the cursor decoder"
    <|
        \s ->
            case Decode.decodeString cursorDecoder s of
                Ok _ ->
                    Expect.pass

                Err _ ->
                    Expect.pass


propEventDecoderTotal : Test
propEventDecoderTotal =
    fuzz garbageString
        "propEventDecoderTotal: random input never crashes the event decoder"
    <|
        \s ->
            case Decode.decodeString eventDecoder s of
                Ok _ ->
                    Expect.pass

                Err _ ->
                    Expect.pass


propOrnamentDecoderTotal : Test
propOrnamentDecoderTotal =
    fuzz garbageString
        "propOrnamentDecoderTotal: random input never crashes the ornament decoder"
    <|
        \s ->
            case Decode.decodeString ornamentDecoder s of
                Ok _ ->
                    Expect.pass

                Err _ ->
                    Expect.pass


propRaagDecoderTotal : Test
propRaagDecoderTotal =
    fuzz garbageString
        "propRaagDecoderTotal: random input never crashes the raag decoder"
    <|
        \s ->
            case Decode.decodeString raagDecoder s of
                Ok _ ->
                    Expect.pass

                Err _ ->
                    Expect.pass


propTaalDecoderTotal : Test
propTaalDecoderTotal =
    fuzz garbageString
        "propTaalDecoderTotal: random input never crashes the taal decoder"
    <|
        \s ->
            case Decode.decodeString taalDecoder s of
                Ok _ ->
                    Expect.pass

                Err _ ->
                    Expect.pass



-- TARGETED NEGATIVE-CASE EXAMPLES. These don't need fuzzing — they
-- assert specific malformed payloads must fail. Grouped under a
-- describe inside this suite so they live alongside the totality
-- properties they reinforce.


testEmptyObjectFailsForRequiredRecords : Test
testEmptyObjectFailsForRequiredRecords =
    test "empty object {} fails for composition, raag, taal, cursor (all have required fields)" <|
        \_ ->
            let
                emptyObj =
                    "{}"

                expectErr decoder name =
                    case Decode.decodeString decoder emptyObj of
                        Ok _ ->
                            Expect.fail (name ++ " decoder should fail on {}")

                        Err _ ->
                            Expect.pass
            in
            Expect.all
                [ \_ -> expectErr compositionDecoder "composition"
                , \_ -> expectErr raagDecoder "raag"
                , \_ -> expectErr taalDecoder "taal"
                , \_ -> expectErr cursorDecoder "cursor"
                ]
                ()


testUnknownEventTypeFails : Test
testUnknownEventTypeFails =
    test "Event decoder fails on unknown discriminator: {\"type\": \"madeUp\"}" <|
        \_ ->
            case Decode.decodeString eventDecoder "{\"type\":\"madeUp\"}" of
                Ok _ ->
                    Expect.fail "Event decoder must not accept unknown type"

                Err _ ->
                    Expect.pass


testUnknownOrnamentTypeFails : Test
testUnknownOrnamentTypeFails =
    test "Ornament decoder fails on unknown discriminator: {\"type\": \"madeUp\"}" <|
        \_ ->
            case Decode.decodeString ornamentDecoder "{\"type\":\"madeUp\"}" of
                Ok _ ->
                    Expect.fail "Ornament decoder must not accept unknown type"

                Err _ ->
                    Expect.pass


testArrayFailsForRecordDecoders : Test
testArrayFailsForRecordDecoders =
    test "record decoders (composition, taal, raag) fail when fed a JSON array" <|
        \_ ->
            let
                jsonArray =
                    "[1,2,3]"

                expectErr decoder name =
                    case Decode.decodeString decoder jsonArray of
                        Ok _ ->
                            Expect.fail (name ++ " decoder should fail on JSON array")

                        Err _ ->
                            Expect.pass
            in
            Expect.all
                [ \_ -> expectErr compositionDecoder "composition"
                , \_ -> expectErr taalDecoder "taal"
                , \_ -> expectErr raagDecoder "raag"
                ]
                ()
