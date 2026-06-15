module GroupingFSMTest exposing (suite)

{-| Property/example tests for `State.Update.GroupingFSM`, the Elm port of
`com.varpas.sangeet.core.editor.GroupingFSM`.

Mirrors the example cases in
`sangeet-core/src/test/scala/com/varpas/sangeet/core/editor/GroupingFSMSpec.scala`.
When updating the Scala spec, update the matching cases here so the two
implementations stay byte-for-byte aligned on the FSM contract.

-}

import Expect
import Model.Types exposing (Note(..), Octave(..), Variant(..))
import State.Model exposing (GroupingState)
import State.Update.GroupingFSM as GroupingFSM
import Test exposing (Test, describe, test)


sa : GroupingFSM.GroupedNote
sa =
    { note = Sa, variant = Shuddha, octave = Madhya }


re : GroupingFSM.GroupedNote
re =
    { note = Re, variant = Shuddha, octave = Madhya }


ga : GroupingFSM.GroupedNote
ga =
    { note = Ga, variant = Shuddha, octave = Madhya }


ma : GroupingFSM.GroupedNote
ma =
    { note = Ma, variant = Shuddha, octave = Madhya }


pa : GroupingFSM.GroupedNote
pa =
    { note = Pa, variant = Shuddha, octave = Madhya }


cursor0 : GroupingFSM.CursorTriple
cursor0 =
    { beat = 0, cycle = 0, subIndex = 0 }


cursor1 : GroupingFSM.CursorTriple
cursor1 =
    { beat = 1, cycle = 0, subIndex = 0 }


cursor5 : GroupingFSM.CursorTriple
cursor5 =
    { beat = 5, cycle = 0, subIndex = 0 }


{-| Build a `GroupingState` with the given fields. The `nextBeat`/`nextCycle`/
`nextSubIndex` come from a `CursorTriple` so callers can reuse the same triples
that drive `decide`'s `observed` argument.
-}
stateWith :
    List GroupingFSM.GroupedNote
    -> GroupingFSM.CursorTriple
    -> Int
    -> GroupingFSM.CursorTriple
    -> GroupingState
stateWith notes preInsert lastTyped nextCursor =
    { notes = notes
    , startTime = lastTyped
    , beat = preInsert.beat
    , cycle = preInsert.cycle
    , nextBeat = nextCursor.beat
    , nextCycle = nextCursor.cycle
    , nextSubIndex = nextCursor.subIndex
    }


suite : Test
suite =
    describe "State.Update.GroupingFSM (hand-port of sangeet-core GroupingFSM)"
        [ decideTests
        , cursorMatchesTests
        , startedStateTests
        , extendedStateTests
        , constantsTests
        ]


decideTests : Test
decideTests =
    describe "decide"
        [ test "StartNew when there is no in-progress group" <|
            \_ ->
                GroupingFSM.decide Nothing 1000 cursor0 sa
                    |> Expect.equal GroupingFSM.StartNew

        --
        , test "Extend when within threshold, under cap, and cursor aligned" <|
            \_ ->
                let
                    state =
                        stateWith [ sa ] cursor0 1000 cursor1
                in
                GroupingFSM.decide (Just state) 1200 cursor1 re
                    |> Expect.equal (GroupingFSM.Extend [ sa, re ])

        --
        , test "StartNew when more than thresholdMs has elapsed (sliding window)" <|
            \_ ->
                -- 1500 - 1000 = 500 which is NOT < thresholdMs (500)
                let
                    state =
                        stateWith [ sa ] cursor0 1000 cursor1
                in
                GroupingFSM.decide (Just state) 1500 cursor1 re
                    |> Expect.equal GroupingFSM.StartNew

        --
        , test "Extend when exactly one ms before the threshold boundary" <|
            \_ ->
                -- 1499 - 1000 = 499 < 500 -> extend
                let
                    state =
                        stateWith [ sa ] cursor0 1000 cursor1
                in
                GroupingFSM.decide (Just state) 1499 cursor1 re
                    |> Expect.equal (GroupingFSM.Extend [ sa, re ])

        --
        , test "StartNew when the group already has maxGroupSize notes" <|
            \_ ->
                let
                    state =
                        stateWith [ sa, re, ga, ma ] cursor0 1000 cursor1
                in
                GroupingFSM.decide (Just state) 1100 cursor1 pa
                    |> Expect.equal GroupingFSM.StartNew

        --
        , test "StartNew when observed cursor has drifted from nextCursor (bug 4)" <|
            \_ ->
                -- group expected cursor to be at beat=5, observed is at beat=0
                let
                    state =
                        stateWith [ sa ] cursor0 1000 cursor5
                in
                GroupingFSM.decide (Just state) 1100 cursor0 re
                    |> Expect.equal GroupingFSM.StartNew

        --
        , test "Extend up to exactly maxGroupSize on consecutive keystrokes" <|
            \_ ->
                let
                    s1 =
                        GroupingFSM.startedState cursor0 sa 1000 cursor1

                    d2 =
                        GroupingFSM.decide (Just s1) 1100 cursor1 re

                    s2 =
                        GroupingFSM.extendedState s1 [ sa, re ] 1100 cursor1

                    d3 =
                        GroupingFSM.decide (Just s2) 1200 cursor1 ga

                    s3 =
                        GroupingFSM.extendedState s2 [ sa, re, ga ] 1200 cursor1

                    d4 =
                        GroupingFSM.decide (Just s3) 1300 cursor1 ma

                    s4 =
                        GroupingFSM.extendedState s3 [ sa, re, ga, ma ] 1300 cursor1

                    d5 =
                        GroupingFSM.decide (Just s4) 1400 cursor1 pa
                in
                Expect.all
                    [ \_ -> d2 |> Expect.equal (GroupingFSM.Extend [ sa, re ])
                    , \_ -> d3 |> Expect.equal (GroupingFSM.Extend [ sa, re, ga ])
                    , \_ -> d4 |> Expect.equal (GroupingFSM.Extend [ sa, re, ga, ma ])
                    , \_ -> d5 |> Expect.equal GroupingFSM.StartNew
                    ]
                    ()

        --
        , test "stays extendable when slow keystrokes drift across the original first-keystroke time (sliding window)" <|
            \_ ->
                -- Sliding semantics: the window resets on every keystroke.
                -- Steady 400ms drift forms one group even though total elapsed > 500ms.
                let
                    s1 =
                        GroupingFSM.startedState cursor0 sa 0 cursor1

                    d2 =
                        GroupingFSM.decide (Just s1) 400 cursor1 re

                    s2 =
                        GroupingFSM.extendedState s1 [ sa, re ] 400 cursor1

                    d3 =
                        GroupingFSM.decide (Just s2) 800 cursor1 ga
                in
                Expect.all
                    [ \_ -> d2 |> Expect.equal (GroupingFSM.Extend [ sa, re ])
                    , \_ -> d3 |> Expect.equal (GroupingFSM.Extend [ sa, re, ga ])
                    ]
                    ()
        ]


cursorMatchesTests : Test
cursorMatchesTests =
    describe "cursorMatches"
        [ test "true when all three components match" <|
            \_ ->
                GroupingFSM.cursorMatches cursor1 cursor1
                    |> Expect.equal True

        --
        , test "false when beat differs" <|
            \_ ->
                GroupingFSM.cursorMatches cursor0 cursor1
                    |> Expect.equal False

        --
        , test "false when cycle differs" <|
            \_ ->
                let
                    a =
                        { beat = 0, cycle = 0, subIndex = 0 }

                    b =
                        { beat = 0, cycle = 1, subIndex = 0 }
                in
                GroupingFSM.cursorMatches a b |> Expect.equal False

        --
        , test "false when subIndex differs" <|
            \_ ->
                let
                    a =
                        { beat = 0, cycle = 0, subIndex = 0 }

                    b =
                        { beat = 0, cycle = 0, subIndex = 1 }
                in
                GroupingFSM.cursorMatches a b |> Expect.equal False
        ]


startedStateTests : Test
startedStateTests =
    describe "startedState"
        [ test "carries the pre-insert beat/cycle and post-insert nextCursor" <|
            \_ ->
                let
                    s =
                        GroupingFSM.startedState cursor0 sa 1000 cursor1
                in
                Expect.all
                    [ \st -> List.length st.notes |> Expect.equal 1
                    , \st -> st.beat |> Expect.equal 0
                    , \st -> st.cycle |> Expect.equal 0
                    , \st -> st.startTime |> Expect.equal 1000
                    , \st -> st.nextBeat |> Expect.equal cursor1.beat
                    , \st -> st.nextCycle |> Expect.equal cursor1.cycle
                    , \st -> st.nextSubIndex |> Expect.equal cursor1.subIndex
                    ]
                    s
        ]


extendedStateTests : Test
extendedStateTests =
    describe "extendedState"
        [ test "advances startTime (== lastTypedTimeMs) and updates nextCursor, preserves beat/cycle" <|
            \_ ->
                let
                    s0 =
                        GroupingFSM.startedState cursor0 sa 1000 cursor1

                    s1 =
                        GroupingFSM.extendedState s0 [ sa, re ] 1200 cursor1
                in
                Expect.all
                    [ \st -> List.length st.notes |> Expect.equal 2
                    , \st -> st.beat |> Expect.equal 0
                    , \st -> st.cycle |> Expect.equal 0
                    , \st -> st.startTime |> Expect.equal 1200
                    , \st -> st.nextBeat |> Expect.equal cursor1.beat
                    , \st -> st.nextCycle |> Expect.equal cursor1.cycle
                    , \st -> st.nextSubIndex |> Expect.equal cursor1.subIndex
                    ]
                    s1
        ]


constantsTests : Test
constantsTests =
    describe "constants"
        [ test "maxGroupSize matches the documented value" <|
            \_ -> GroupingFSM.maxGroupSize |> Expect.equal 4

        --
        , test "thresholdMs matches the documented value" <|
            \_ -> GroupingFSM.thresholdMs |> Expect.equal 500
        ]
