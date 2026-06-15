module SwarRulesPropTest exposing (suite)

{-| Plan-19 Tier 4 Phase B — domain-invariant properties for the achal rule.

The rule: **Sa and Pa are achal (fixed) — they have no komal/tivra
variants. Any Sa or Pa must carry `Shuddha`.** This is encoded in
`Generators.Common.variantFor` and the achal-aware `noteRef` /
`swarEvent` fuzzers. These properties prove that the generators
themselves obey the rule, so any test that relies on them gets a
"musically valid by construction" guarantee for free.

If someone ever changes `variantFor` to return raw `variant` for Sa or
Pa (a likely accidental regression), every property here fires.

-}

import Expect
import Generators.Common as Common
import Generators.Composition exposing (swarEvent)
import Model.Event exposing (Event(..))
import Model.Types
    exposing
        ( Note(..)
        , Variant(..)
        )
import Test exposing (Test, describe, fuzz)


suite : Test
suite =
    describe "Achal rule (Sa and Pa always Shuddha)"
        [ propVariantForSaIsShuddha
        , propVariantForPaIsShuddha
        , propNoteRefRespectsAchal
        , propSwarEventRespectsAchal
        ]


propVariantForSaIsShuddha : Test
propVariantForSaIsShuddha =
    -- Even though `variantFor Sa` is a constant fuzzer, exercise it 100×
    -- so any change that re-introduces the underlying `variant` fuzzer
    -- is caught on the first runs (Komal/Tivra would appear ~67% of runs).
    fuzz (Common.variantFor Sa)
        "propVariantForSaIsShuddha: variantFor Sa always yields Shuddha"
    <|
        \v -> Expect.equal Shuddha v


propVariantForPaIsShuddha : Test
propVariantForPaIsShuddha =
    fuzz (Common.variantFor Pa)
        "propVariantForPaIsShuddha: variantFor Pa always yields Shuddha"
    <|
        \v -> Expect.equal Shuddha v


{-| The `noteRef` fuzzer composes `variantFor` with `note`. Drawing
across the full distribution, any NoteRef with note == Sa or Pa must
carry Shuddha.
-}
propNoteRefRespectsAchal : Test
propNoteRefRespectsAchal =
    fuzz Common.noteRef
        "propNoteRefRespectsAchal: NoteRef with note ∈ {Sa, Pa} has variant Shuddha"
    <|
        \nr ->
            case nr.note of
                Sa ->
                    Expect.equal Shuddha nr.variant

                Pa ->
                    Expect.equal Shuddha nr.variant

                _ ->
                    -- Re/Ga/Ma/Dha/Ni can carry any variant. We don't
                    -- assert variant ∈ valid-set for them here because
                    -- the Phase-A generators are a deliberate
                    -- super-set (they let Ma be Komal etc.) for codec
                    -- coverage. A future "domain-validating fuzzer"
                    -- would tighten this — out of scope for T4B.
                    Expect.pass


{-| `SwarEvent` is the only Event variant carrying a Note. The generator
must respect achal at the event level, since downstream renderers /
exporters trust the model to be well-formed.
-}
propSwarEventRespectsAchal : Test
propSwarEventRespectsAchal =
    fuzz swarEvent
        "propSwarEventRespectsAchal: SwarEvent with note ∈ {Sa, Pa} has variant Shuddha"
    <|
        \e ->
            case e of
                SwarEvent r ->
                    case r.note of
                        Sa ->
                            Expect.equal Shuddha r.variant

                        Pa ->
                            Expect.equal Shuddha r.variant

                        _ ->
                            Expect.pass

                _ ->
                    -- `swarEvent` only ever emits `SwarEvent` — this
                    -- arm exists purely to satisfy totality. If it
                    -- ever fires, the generator broke its contract.
                    Expect.fail "swarEvent fuzzer emitted a non-SwarEvent"
