module Generators.Composition exposing
    ( composition
    , compositionType
    , event
    , metadata
    , ornament
    , raag
    , section
    , sectionType
    , swarEvent
    , taal
    , tihai
    , vibhag
    , vibhagMarker
    )

{-| Composition / Event / Ornament / Section / Metadata fuzzers for sangeet-web.
Plan-19 Tier 4 Phase A genesis.

Sizes are intentionally small: max 3 sections per composition, max 8 events
per section, max 2 ornaments per swar. This keeps a 100-run fuzz pass under
a second and keeps shrunken counter-examples readable.

The achal rule (Sa and Pa always Shuddha) is enforced inside `swarEvent` via
`Common.variantFor`, the same helper used inside `noteRef` so any ornament
that carries `NoteRef`s also respects the rule.

-}

import Fuzz exposing (Fuzzer)
import Generators.Common as Common
import Model.Composition
    exposing
        ( Composition
        , CompositionType(..)
        , Metadata
        , Section
        , SectionType(..)
        , Tihai
        )
import Model.Event exposing (Event(..))
import Model.Ornament exposing (Ornament(..))
import Model.Raag exposing (Raag)
import Model.Taal exposing (Taal, Vibhag, VibhagMarker(..))



-- ENUM-LIKE FUZZERS WITH A "CUSTOM" ARM


compositionType : Fuzzer CompositionType
compositionType =
    Fuzz.oneOf
        [ Fuzz.constant Bandish
        , Fuzz.constant Gat
        , Fuzz.constant Palta
        , Fuzz.constant Sargam
        , Fuzz.map CustomCompositionType Common.shortAsciiString
        ]


sectionType : Fuzzer SectionType
sectionType =
    Fuzz.oneOf
        [ Fuzz.constant Sthayi
        , Fuzz.constant Antara
        , Fuzz.constant Sanchari
        , Fuzz.constant Abhog
        , Fuzz.constant Taan
        , Fuzz.constant Toda
        , Fuzz.constant Jhala
        , Fuzz.constant PaltaSection
        , Fuzz.constant Arohi
        , Fuzz.constant Avarohi
        , Fuzz.constant SargamSection
        , Fuzz.map CustomSectionType Common.shortAsciiString
        ]


vibhagMarker : Fuzzer VibhagMarker
vibhagMarker =
    Fuzz.oneOf
        [ Fuzz.constant Sam
        , Fuzz.constant KhaliMarker
        , Fuzz.map TaaliMarker (Fuzz.intRange 1 8)
        ]



-- TAAL + RAAG


vibhag : Fuzzer Vibhag
vibhag =
    Fuzz.map2 (\b m -> { beats = b, marker = m })
        (Fuzz.intRange 1 8)
        vibhagMarker


taal : Fuzzer Taal
taal =
    Fuzz.map4
        (\name matras vibhags theka ->
            { name = name
            , matras = matras
            , vibhags = vibhags
            , theka = theka
            }
        )
        Common.shortAsciiString
        (Fuzz.intRange 1 16)
        (Fuzz.listOfLengthBetween 1 4 vibhag)
        (Fuzz.maybe (Fuzz.listOfLengthBetween 0 4 Common.shortAsciiString))


raag : Fuzzer Raag
raag =
    -- map8 keeps it within Fuzz's max-arity helpers
    Fuzz.map8
        (\name thaat aro avaro vadi samvadi pakad prahar ->
            { name = name
            , thaat = thaat
            , arohana = aro
            , avarohana = avaro
            , vadi = vadi
            , samvadi = samvadi
            , pakad = pakad
            , prahar = prahar
            }
        )
        Common.shortAsciiString
        (Fuzz.maybe Common.shortAsciiString)
        (Fuzz.maybe (Fuzz.listOfLengthBetween 0 4 Common.shortAsciiString))
        (Fuzz.maybe (Fuzz.listOfLengthBetween 0 4 Common.shortAsciiString))
        (Fuzz.maybe Common.shortAsciiString)
        (Fuzz.maybe Common.shortAsciiString)
        (Fuzz.maybe Common.shortAsciiString)
        (Fuzz.maybe (Fuzz.intRange 1 8))



-- ORNAMENT


ornament : Fuzzer Ornament
ornament =
    Fuzz.oneOf
        [ Fuzz.map4
            (\s e d i ->
                Meend
                    { startNote = s
                    , endNote = e
                    , direction = d
                    , intermediateNotes = i
                    }
            )
            Common.noteRef
            Common.noteRef
            Common.meendDirection
            (Fuzz.listOfLengthBetween 0 2 Common.noteRef)
        , Fuzz.map (\g -> KanSwar { graceNote = g }) Common.noteRef
        , Fuzz.map (\ns -> Murki { notes = ns })
            (Fuzz.listOfLengthBetween 0 3 Common.noteRef)
        , Fuzz.constant Gamak
        , Fuzz.constant Andolan
        , Fuzz.map (\ns -> Krintan { notes = ns })
            (Fuzz.listOfLengthBetween 0 3 Common.noteRef)
        , Fuzz.constant Gitkari
        , Fuzz.map (\t -> Ghaseet { targetNote = t }) Common.noteRef
        , Fuzz.map (\t -> Sparsh { touchNote = t }) Common.noteRef
        , Fuzz.map (\ns -> Zamzama { notes = ns })
            (Fuzz.listOfLengthBetween 0 3 Common.noteRef)
        , Fuzz.map2
            (\n p -> CustomOrnament { name = n, parameters = p })
            Common.shortAsciiString
            (Fuzz.listOfLengthBetween 0
                2
                (Fuzz.pair Common.shortAsciiString Common.shortAsciiString)
            )
        ]



-- EVENT


swarEvent : Fuzzer Event
swarEvent =
    Common.note
        |> Fuzz.andThen
            (\n ->
                -- Sa and Pa are achal — variant must be Shuddha. The
                -- `variantFor` helper enforces this; ornaments carry their
                -- own NoteRefs which are independently filtered the same way.
                Fuzz.map6
                    (\v oct beat dur strk orn ->
                        SwarEvent
                            { note = n
                            , variant = v
                            , octave = oct
                            , beat = beat
                            , duration = dur
                            , stroke = strk
                            , ornaments = orn

                            -- `sahitya` is fixed to Nothing here. The
                            -- encoder omits the field when Nothing, and an
                            -- ASCII-only round-trip would silently lose
                            -- any non-printable bytes. Sahitya-specific
                            -- generators will be added in Phase B with a
                            -- targeted property.
                            , sahitya = Nothing
                            }
                    )
                    (Common.variantFor n)
                    Common.octave
                    Common.beatPosition
                    Common.rational
                    (Fuzz.maybe Common.stroke)
                    (Fuzz.listOfLengthBetween 0 2 ornament)
            )


event : Fuzzer Event
event =
    Fuzz.oneOf
        [ swarEvent
        , Fuzz.map2
            (\b d -> RestEvent { beat = b, duration = d })
            Common.beatPosition
            Common.rational
        , Fuzz.map2
            (\b d -> SustainEvent { beat = b, duration = d })
            Common.beatPosition
            Common.rational
        , Fuzz.map2
            (\b d -> ChikariEvent { beat = b, duration = d })
            Common.beatPosition
            Common.rational
        , Fuzz.map2
            (\b d -> LockedBeatEvent { beat = b, duration = d })
            Common.beatPosition
            Common.rational
        ]



-- SECTION + TIHAI


tihai : Fuzzer Tihai
tihai =
    Fuzz.map2 (\s l -> { startBeat = s, landingBeat = l })
        Common.beatPosition
        Common.beatPosition


section : Fuzzer Section
section =
    Fuzz.map5
        (\name st events tih start ->
            { name = name
            , sectionType = st
            , events = events
            , tihai = tih
            , startingBeat = start
            }
        )
        Common.shortAsciiString
        sectionType
        (Fuzz.listOfLengthBetween 0 8 event)
        (Fuzz.maybe tihai)
        (Fuzz.intRange 1 16)



-- METADATA + COMPOSITION


metadata : Fuzzer Metadata
metadata =
    -- map8 + andMap because Metadata has 13 fields.
    Fuzz.constant
        (\title cType r t lya instr composer author source showStroke showSahitya created updated ->
            { title = title
            , compositionType = cType
            , raag = r
            , taal = t
            , laya = lya
            , instrument = instr
            , composer = composer
            , author = author
            , source = source
            , showStrokeLine = showStroke
            , showSahityaLine = showSahitya
            , createdAt = created
            , updatedAt = updated
            }
        )
        |> Fuzz.andMap Common.shortAsciiString
        |> Fuzz.andMap compositionType
        |> Fuzz.andMap raag
        |> Fuzz.andMap taal
        |> Fuzz.andMap (Fuzz.maybe Common.laya)
        |> Fuzz.andMap (Fuzz.maybe Common.shortAsciiString)
        |> Fuzz.andMap (Fuzz.maybe Common.shortAsciiString)
        |> Fuzz.andMap (Fuzz.maybe Common.shortAsciiString)
        |> Fuzz.andMap (Fuzz.maybe Common.shortAsciiString)
        |> Fuzz.andMap Fuzz.bool
        |> Fuzz.andMap Fuzz.bool
        |> Fuzz.andMap Common.shortAsciiString
        |> Fuzz.andMap Common.shortAsciiString


composition : Fuzzer Composition
composition =
    Fuzz.map2 (\m ss -> { metadata = m, sections = ss })
        metadata
        (Fuzz.listOfLengthBetween 0 3 section)
