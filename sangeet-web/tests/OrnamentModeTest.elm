module OrnamentModeTest exposing (..)

import Expect
import Input.OrnamentMode exposing (OrnamentAction(..), transition)
import Model.Types exposing (MeendDirection(..), Note(..), Octave(..), Variant(..))
import State.Model exposing (OrnamentMode(..))
import Test exposing (Test, describe, test)


saRef : { note : Note, variant : Variant, octave : Octave }
saRef =
    { note = Sa, variant = Shuddha, octave = Madhya }


reRef : { note : Note, variant : Variant, octave : Octave }
reRef =
    { note = Re, variant = Shuddha, octave = Madhya }


gaRef : { note : Note, variant : Variant, octave : Octave }
gaRef =
    { note = Ga, variant = Shuddha, octave = Madhya }


suite : Test
suite =
    describe "OrnamentMode.transition"
        [ noOrnamentTests
        , singleNoteModeTests
        , meendTests
        , krintanTests
        , murkiTests
        , zamzamaTests
        ]


noOrnamentTests : Test
noOrnamentTests =
    describe "NoOrnament mode"
        [ test "NoOrnament with note returns Cancelled" <|
            \_ ->
                transition NoOrnament (Just saRef) False
                    |> Expect.equal Cancelled
        , test "NoOrnament without note returns Cancelled" <|
            \_ ->
                transition NoOrnament Nothing False
                    |> Expect.equal Cancelled
        ]


singleNoteModeTests : Test
singleNoteModeTests =
    describe "SingleNoteMode"
        [ test "with note returns ApplySingleNote" <|
            \_ ->
                transition (SingleNoteMode "kanSwar") (Just saRef) False
                    |> Expect.equal (ApplySingleNote "kanSwar" saRef)
        , test "without note returns Cancelled" <|
            \_ ->
                transition (SingleNoteMode "kanSwar") Nothing False
                    |> Expect.equal Cancelled
        , test "sparsh type with note returns ApplySingleNote" <|
            \_ ->
                transition (SingleNoteMode "sparsh") (Just reRef) False
                    |> Expect.equal (ApplySingleNote "sparsh" reRef)
        ]


meendTests : Test
meendTests =
    describe "Meend mode"
        [ test "MeendStartMode with note enters MeendEndMode" <|
            \_ ->
                transition (MeendStartMode Ascending) (Just saRef) False
                    |> Expect.equal (StillCollecting (MeendEndMode saRef Ascending))
        , test "MeendStartMode without note returns Cancelled" <|
            \_ ->
                transition (MeendStartMode Ascending) Nothing False
                    |> Expect.equal Cancelled
        , test "MeendStartMode Descending with note enters MeendEndMode Descending" <|
            \_ ->
                transition (MeendStartMode Descending) (Just saRef) False
                    |> Expect.equal (StillCollecting (MeendEndMode saRef Descending))
        , test "MeendEndMode with end note applies meend" <|
            \_ ->
                transition (MeendEndMode saRef Ascending) (Just reRef) False
                    |> Expect.equal (ApplyMeend saRef reRef Ascending)
        , test "MeendEndMode without note returns Cancelled" <|
            \_ ->
                transition (MeendEndMode saRef Ascending) Nothing False
                    |> Expect.equal Cancelled
        ]


krintanTests : Test
krintanTests =
    describe "Krintan mode"
        [ test "KrintanStartMode with note enters KrintanEndMode" <|
            \_ ->
                transition KrintanStartMode (Just saRef) False
                    |> Expect.equal (StillCollecting (KrintanEndMode saRef))
        , test "KrintanStartMode without note returns Cancelled" <|
            \_ ->
                transition KrintanStartMode Nothing False
                    |> Expect.equal Cancelled
        , test "KrintanEndMode with end note applies krintan with 2 notes" <|
            \_ ->
                transition (KrintanEndMode saRef) (Just reRef) False
                    |> Expect.equal (ApplyKrintan [ saRef, reRef ])
        , test "KrintanEndMode with Enter and no note applies krintan with 1 note" <|
            \_ ->
                transition (KrintanEndMode saRef) Nothing True
                    |> Expect.equal (ApplyKrintan [ saRef ])
        , test "KrintanEndMode without note and not Enter returns Cancelled" <|
            \_ ->
                transition (KrintanEndMode saRef) Nothing False
                    |> Expect.equal Cancelled
        ]


murkiTests : Test
murkiTests =
    describe "Murki mode"
        [ test "MurkiCollectMode with note adds to collection" <|
            \_ ->
                transition (MurkiCollectMode []) (Just saRef) False
                    |> Expect.equal (StillCollecting (MurkiCollectMode [ saRef ]))
        , test "MurkiCollectMode accumulates notes in reverse" <|
            \_ ->
                transition (MurkiCollectMode [ saRef ]) (Just reRef) False
                    |> Expect.equal (StillCollecting (MurkiCollectMode [ reRef, saRef ]))
        , test "MurkiCollectMode Enter with notes applies murki (reversed)" <|
            \_ ->
                transition (MurkiCollectMode [ reRef, saRef ]) Nothing True
                    |> Expect.equal (ApplyMurki [ saRef, reRef ])
        , test "MurkiCollectMode Enter with empty list returns Cancelled" <|
            \_ ->
                transition (MurkiCollectMode []) Nothing True
                    |> Expect.equal Cancelled
        , test "MurkiCollectMode without note and not Enter returns Cancelled" <|
            \_ ->
                transition (MurkiCollectMode [ saRef ]) Nothing False
                    |> Expect.equal Cancelled
        ]


zamzamaTests : Test
zamzamaTests =
    describe "Zamzama mode"
        [ test "ZamzamaCollectMode with note adds to collection" <|
            \_ ->
                transition (ZamzamaCollectMode []) (Just saRef) False
                    |> Expect.equal (StillCollecting (ZamzamaCollectMode [ saRef ]))
        , test "ZamzamaCollectMode accumulates notes in reverse" <|
            \_ ->
                transition (ZamzamaCollectMode [ saRef ]) (Just reRef) False
                    |> Expect.equal (StillCollecting (ZamzamaCollectMode [ reRef, saRef ]))
        , test "ZamzamaCollectMode Enter with notes applies zamzama (reversed)" <|
            \_ ->
                transition (ZamzamaCollectMode [ gaRef, reRef, saRef ]) Nothing True
                    |> Expect.equal (ApplyZamzama [ saRef, reRef, gaRef ])
        , test "ZamzamaCollectMode Enter with empty list returns Cancelled" <|
            \_ ->
                transition (ZamzamaCollectMode []) Nothing True
                    |> Expect.equal Cancelled
        , test "ZamzamaCollectMode without note and not Enter returns Cancelled" <|
            \_ ->
                transition (ZamzamaCollectMode [ saRef ]) Nothing False
                    |> Expect.equal Cancelled
        ]
