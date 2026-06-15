module Generators.Common exposing
    ( beatPosition
    , laya
    , meendDirection
    , note
    , noteRef
    , octave
    , rational
    , shortAsciiString
    , stroke
    , variant
    , variantFor
    )

{-| Primitive fuzzers for the sangeet-web domain model — plan-19 Tier 4 Phase A
genesis. These map 1-to-1 onto the enums in `Model.Types` (Note, Variant,
Octave, Stroke, Laya, MeendDirection) plus the small record types `Rational`,
`BeatPosition`, and `NoteRef`.

Generated values are intentionally bounded (small ints, short strings) so
property tests stay fast and shrinking stays effective.

The musically-correct Sa/Pa-are-achal rule lives in `Generators.Composition`
where Swar values are assembled — these primitives just emit independent
enum values. See `variantFor` for a Note-aware variant fuzzer that callers
can use when they need to respect that invariant directly.

-}

import Fuzz exposing (Fuzzer)
import Model.Types
    exposing
        ( BeatPosition
        , Laya(..)
        , MeendDirection(..)
        , Note(..)
        , NoteRef
        , Octave(..)
        , Rational
        , Stroke(..)
        , Variant(..)
        )



-- ENUM FUZZERS


note : Fuzzer Note
note =
    Fuzz.oneOfValues [ Sa, Re, Ga, Ma, Pa, Dha, Ni ]


variant : Fuzzer Variant
variant =
    Fuzz.oneOfValues [ Shuddha, Komal, Tivra ]


{-| Note-aware variant fuzzer. Sa and Pa are achal (fixed) — they have no
komal/tivra variants. For those notes, this always yields `Shuddha`.
-}
variantFor : Note -> Fuzzer Variant
variantFor n =
    case n of
        Sa ->
            Fuzz.constant Shuddha

        Pa ->
            Fuzz.constant Shuddha

        _ ->
            variant


octave : Fuzzer Octave
octave =
    Fuzz.oneOfValues [ AtiMandra, Mandra, Madhya, Taar, AtiTaar ]


stroke : Fuzzer Stroke
stroke =
    Fuzz.oneOfValues [ Da, Ra, Jod ]


laya : Fuzzer Laya
laya =
    Fuzz.oneOfValues [ AtiVilambit, Vilambit, MadhyaLaya, Drut, AtiDrut ]


meendDirection : Fuzzer MeendDirection
meendDirection =
    Fuzz.oneOfValues [ Ascending, Descending ]



-- RECORD / VALUE FUZZERS


{-| `Rational` fuzzer. Numerator in `[0, 16]`, denominator in `[1, 16]` so we
never emit zero denominators. Bounds chosen to match the editor's grouping
limits (max 4-way grouping, max 16th-note subdivisions).
-}
rational : Fuzzer Rational
rational =
    Fuzz.map2 (\n d -> { numerator = n, denominator = d })
        (Fuzz.intRange 0 16)
        (Fuzz.intRange 1 16)


{-| `BeatPosition` fuzzer. Cycles in `[0, 8]`, beats in `[1, 16]` to cover the
common taals (Tintaal=16, Jhaptaal=10, etc.) without exploding the search space.
-}
beatPosition : Fuzzer BeatPosition
beatPosition =
    Fuzz.map3 (\c b s -> { cycle = c, beat = b, subdivision = s })
        (Fuzz.intRange 0 8)
        (Fuzz.intRange 1 16)
        rational


{-| `NoteRef` fuzzer that respects the achal rule for Sa/Pa.
-}
noteRef : Fuzzer NoteRef
noteRef =
    note
        |> Fuzz.andThen
            (\n ->
                Fuzz.map2 (\v o -> { note = n, variant = v, octave = o })
                    (variantFor n)
                    octave
            )


{-| Short ASCII string fuzzer for free-form text fields (names, sahitya,
custom-type labels). Bounded to 0-12 chars to keep shrinking fast and to
avoid stressing Elm's String encoder in property runs.
-}
shortAsciiString : Fuzzer String
shortAsciiString =
    Fuzz.asciiStringOfLengthBetween 0 12
