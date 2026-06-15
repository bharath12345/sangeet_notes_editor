module CursorBoundsPropTest exposing (suite)

{-| Plan-19 Tier 4 Phase B — properties for `Model.Cursor` (codec) and the
`GroupingFSM.cursorMatches` predicate. The web tier doesn't own the
cursor-advancement algorithm (that lives on the server), so we test what
IS pure in the web codebase: the JSON round-trip and the equivalence
predicate used by the grouping FSM.

Properties:

  - `propCursorRoundTrip`: `decode (encode c) == Ok c` for any
    fuzzer-generated `CursorModel`. Exercises the optional
    `selectionAnchor` field in both presence configurations.
  - `propCursorMatchesReflexive`: `cursorMatches c c == True` for any
    `CursorTriple`.
  - `propCursorMatchesSymmetric`: `cursorMatches a b == cursorMatches b a`.
  - `propCursorTripleFromCursorPreservesFields`: the triple projection
    carries beat/cycle/subIndex verbatim from the full cursor.

These cover the algebraic shape of cursor equivalence without depending
on the server-side advancement logic.

-}

import Expect
import Fuzz exposing (Fuzzer)
import Generators.Common as Common
import Generators.Composition as CompositionGen
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Cursor exposing (CursorModel, cursorDecoder, encodeCursor)
import State.Update.GroupingFSM as FSM exposing (CursorTriple)
import Test exposing (Test, describe, fuzz, fuzz2)



-- LOCAL FUZZERS


cursorModel : Fuzzer CursorModel
cursorModel =
    Fuzz.constant
        (\t cy b si total oct anch ->
            { taal = t
            , cycle = cy
            , beat = b
            , subIndex = si
            , totalSubdivisions = total
            , currentOctave = oct
            , selectionAnchor = anch
            }
        )
        |> Fuzz.andMap CompositionGen.taal
        |> Fuzz.andMap (Fuzz.intRange 0 8)
        |> Fuzz.andMap (Fuzz.intRange 0 16)
        |> Fuzz.andMap (Fuzz.intRange 0 4)
        |> Fuzz.andMap (Fuzz.intRange 1 16)
        |> Fuzz.andMap Common.octave
        |> Fuzz.andMap (Fuzz.maybe Common.beatPosition)


cursorTriple : Fuzzer CursorTriple
cursorTriple =
    Fuzz.map3 (\b c s -> { beat = b, cycle = c, subIndex = s })
        (Fuzz.intRange 0 16)
        (Fuzz.intRange 0 8)
        (Fuzz.intRange 0 4)



-- PROPERTIES


suite : Test
suite =
    describe "Cursor + cursorMatches properties"
        [ propCursorRoundTrip
        , propCursorMatchesReflexive
        , propCursorMatchesSymmetric
        , propCursorTripleFromCursorPreservesFields
        ]


propCursorRoundTrip : Test
propCursorRoundTrip =
    fuzz cursorModel "propCursorRoundTrip: decode(encode(c)) == Ok c" <|
        \c ->
            encodeCursor c
                |> Encode.encode 0
                |> Decode.decodeString cursorDecoder
                |> Expect.equal (Ok c)


propCursorMatchesReflexive : Test
propCursorMatchesReflexive =
    fuzz cursorTriple "propCursorMatchesReflexive: cursorMatches c c == True" <|
        \c ->
            FSM.cursorMatches c c
                |> Expect.equal True


propCursorMatchesSymmetric : Test
propCursorMatchesSymmetric =
    fuzz2 cursorTriple
        cursorTriple
        "propCursorMatchesSymmetric: cursorMatches a b == cursorMatches b a"
    <|
        \a b ->
            FSM.cursorMatches a b
                |> Expect.equal (FSM.cursorMatches b a)


propCursorTripleFromCursorPreservesFields : Test
propCursorTripleFromCursorPreservesFields =
    fuzz cursorModel
        "propCursorTripleFromCursorPreservesFields: triple projection is field-equal"
    <|
        \c ->
            let
                t =
                    FSM.cursorTripleFromCursor c
            in
            Expect.all
                [ \tr -> tr.beat |> Expect.equal c.beat
                , \tr -> tr.cycle |> Expect.equal c.cycle
                , \tr -> tr.subIndex |> Expect.equal c.subIndex
                ]
                t
