module TypesCodecPropTest exposing (suite)

{-| Plan-19 Tier 4 Phase B — round-trip properties for the primitive types in
`Model.Types`. Each enum + small record gets a `decode (encode x) == Ok x`
property, replacing what otherwise would be N hand-written examples per
variant.

These collapse the most obvious duplication category in the web tier: a
codec per enum × every variant. One fuzz test per type covers every variant
in 100 runs, and the achal-aware `noteRef` fuzzer guarantees we never
fabricate a musically-illegal NoteRef.

Generators live in `Generators.Common` (genesis from T4A) — this suite
strictly imports + uses them; it does not modify any production code or
existing fuzzers.

-}

import Expect
import Fuzz
import Generators.Common as Common
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Types as Types exposing (SwarScript(..))
import Test exposing (Test, describe, fuzz)


suite : Test
suite =
    describe "Model.Types round-trip properties"
        [ propNoteRoundTrip
        , propVariantRoundTrip
        , propOctaveRoundTrip
        , propStrokeRoundTrip
        , propLayaRoundTrip
        , propMeendDirectionRoundTrip
        , propSwarScriptRoundTrip
        , propRationalRoundTrip
        , propBeatPositionRoundTrip
        , propNoteRefRoundTrip
        ]



-- ENUM ROUND-TRIPS


propNoteRoundTrip : Test
propNoteRoundTrip =
    fuzz Common.note "propNoteRoundTrip: decode(encode(n)) == Ok n" <|
        \n ->
            Types.encodeNote n
                |> Encode.encode 0
                |> Decode.decodeString Types.noteDecoder
                |> Expect.equal (Ok n)


propVariantRoundTrip : Test
propVariantRoundTrip =
    fuzz Common.variant "propVariantRoundTrip: decode(encode(v)) == Ok v" <|
        \v ->
            Types.encodeVariant v
                |> Encode.encode 0
                |> Decode.decodeString Types.variantDecoder
                |> Expect.equal (Ok v)


propOctaveRoundTrip : Test
propOctaveRoundTrip =
    fuzz Common.octave "propOctaveRoundTrip: decode(encode(o)) == Ok o" <|
        \o ->
            Types.encodeOctave o
                |> Encode.encode 0
                |> Decode.decodeString Types.octaveDecoder
                |> Expect.equal (Ok o)


propStrokeRoundTrip : Test
propStrokeRoundTrip =
    fuzz Common.stroke "propStrokeRoundTrip: decode(encode(s)) == Ok s" <|
        \s ->
            Types.encodeStroke s
                |> Encode.encode 0
                |> Decode.decodeString Types.strokeDecoder
                |> Expect.equal (Ok s)


propLayaRoundTrip : Test
propLayaRoundTrip =
    fuzz Common.laya "propLayaRoundTrip: decode(encode(l)) == Ok l" <|
        \l ->
            Types.encodeLaya l
                |> Encode.encode 0
                |> Decode.decodeString Types.layaDecoder
                |> Expect.equal (Ok l)


propMeendDirectionRoundTrip : Test
propMeendDirectionRoundTrip =
    fuzz Common.meendDirection "propMeendDirectionRoundTrip: decode(encode(d)) == Ok d" <|
        \d ->
            Types.encodeMeendDirection d
                |> Encode.encode 0
                |> Decode.decodeString Types.meendDirectionDecoder
                |> Expect.equal (Ok d)


{-| `SwarScript` has no fuzzer in `Generators.Common` — it isn't reachable
from a `Composition`, so the Phase A genesis didn't ship one. We define
a local `oneOfValues` fuzzer here rather than touching the genesis module,
per the T4B constraint to leave existing fuzzers untouched.
-}
propSwarScriptRoundTrip : Test
propSwarScriptRoundTrip =
    fuzz
        (Fuzz.oneOfValues [ Devanagari, Kannada, Telugu, English ])
        "propSwarScriptRoundTrip: decode(encode(s)) == Ok s"
    <|
        \s ->
            Types.encodeSwarScript s
                |> Encode.encode 0
                |> Decode.decodeString Types.swarScriptDecoder
                |> Expect.equal (Ok s)



-- RECORD ROUND-TRIPS


propRationalRoundTrip : Test
propRationalRoundTrip =
    fuzz Common.rational "propRationalRoundTrip: decode(encode(r)) == Ok r" <|
        \r ->
            Types.encodeRational r
                |> Encode.encode 0
                |> Decode.decodeString Types.rationalDecoder
                |> Expect.equal (Ok r)


propBeatPositionRoundTrip : Test
propBeatPositionRoundTrip =
    fuzz Common.beatPosition "propBeatPositionRoundTrip: decode(encode(bp)) == Ok bp" <|
        \bp ->
            Types.encodeBeatPosition bp
                |> Encode.encode 0
                |> Decode.decodeString Types.beatPositionDecoder
                |> Expect.equal (Ok bp)


propNoteRefRoundTrip : Test
propNoteRefRoundTrip =
    fuzz Common.noteRef "propNoteRefRoundTrip: decode(encode(nr)) == Ok nr" <|
        \nr ->
            Types.encodeNoteRef nr
                |> Encode.encode 0
                |> Decode.decodeString Types.noteRefDecoder
                |> Expect.equal (Ok nr)
