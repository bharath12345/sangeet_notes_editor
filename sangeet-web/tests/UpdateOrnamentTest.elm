module UpdateOrnamentTest exposing (..)

import Expect
import Model.Types exposing (MeendDirection(..), Note(..), Octave(..), Variant(..))
import State.Model exposing (OrnamentMode(..))
import State.Msg exposing (Msg(..))
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultModel)


suite : Test
suite =
    describe "Update ornament operations"
        [ enterOrnamentModeTests
        , ornamentCancelTests
        , simpleOrnamentTests
        , singleNoteCollectionTests
        , meendCollectionTests
        , krintanCollectionTests
        , murkiCollectionTests
        , zamzamaCollectionTests
        ]


enterOrnamentModeTests : Test
enterOrnamentModeTests =
    describe "Entering ornament mode via Alt keys"
        [ test "Alt+k enters SingleNoteMode kanSwar" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "k" False False True) defaultModel
                in
                Expect.equal (SingleNoteMode "kanSwar") newModel.ornamentMode
        , test "Alt+s enters SingleNoteMode sparsh" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "s" False False True) defaultModel
                in
                Expect.equal (SingleNoteMode "sparsh") newModel.ornamentMode
        , test "Alt+h enters SingleNoteMode ghaseet" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "h" False False True) defaultModel
                in
                Expect.equal (SingleNoteMode "ghaseet") newModel.ornamentMode
        , test "Alt+m enters MeendStartMode Ascending" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "m" False False True) defaultModel
                in
                Expect.equal (MeendStartMode Ascending) newModel.ornamentMode
        , test "Alt+M enters MeendStartMode Descending" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "M" False False True) defaultModel
                in
                Expect.equal (MeendStartMode Descending) newModel.ornamentMode
        , test "Alt+r enters KrintanStartMode" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "r" False False True) defaultModel
                in
                Expect.equal KrintanStartMode newModel.ornamentMode
        , test "Alt+u enters MurkiCollectMode" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "u" False False True) defaultModel
                in
                Expect.equal (MurkiCollectMode []) newModel.ornamentMode
        , test "Alt+z enters ZamzamaCollectMode" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "z" False False True) defaultModel
                in
                Expect.equal (ZamzamaCollectMode []) newModel.ornamentMode
        ]


ornamentCancelTests : Test
ornamentCancelTests =
    describe "Cancelling ornament mode"
        [ test "Escape cancels ornament mode" <|
            \_ ->
                let
                    model =
                        { defaultModel | ornamentMode = SingleNoteMode "kanSwar" }

                    ( newModel, _ ) =
                        update (KeyPressed "Escape" False False False) model
                in
                Expect.equal NoOrnament newModel.ornamentMode
        , test "Alt+Escape cancels ornament mode" <|
            \_ ->
                let
                    model =
                        { defaultModel | ornamentMode = MeendStartMode Ascending }

                    ( newModel, _ ) =
                        update (KeyPressed "Escape" False False True) model
                in
                Expect.equal NoOrnament newModel.ornamentMode
        ]


simpleOrnamentTests : Test
simpleOrnamentTests =
    describe "Simple ornament application"
        [ test "Alt+g sets pendingApiCall for gamak" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "g" False False True) defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "Alt+a sets pendingApiCall for andolan" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "a" False False True) defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "Alt+i sets pendingApiCall for gitkari" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (KeyPressed "i" False False True) defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        ]


singleNoteCollectionTests : Test
singleNoteCollectionTests =
    describe "SingleNoteMode note collection"
        [ test "typing 's' in kanSwar mode dispatches API call" <|
            \_ ->
                let
                    model =
                        { defaultModel | ornamentMode = SingleNoteMode "kanSwar" }

                    ( newModel, _ ) =
                        update (KeyPressed "s" False False False) model
                in
                Expect.all
                    [ \m -> Expect.equal NoOrnament m.ornamentMode
                    , \m -> Expect.equal True m.pendingApiCall
                    ]
                    newModel
        , test "typing unknown key in kanSwar mode cancels" <|
            \_ ->
                let
                    model =
                        { defaultModel | ornamentMode = SingleNoteMode "kanSwar" }

                    ( newModel, _ ) =
                        update (KeyPressed "q" False False False) model
                in
                Expect.equal NoOrnament newModel.ornamentMode
        ]


meendCollectionTests : Test
meendCollectionTests =
    describe "Meend note collection"
        [ test "typing start note in MeendStartMode enters MeendEndMode" <|
            \_ ->
                let
                    model =
                        { defaultModel | ornamentMode = MeendStartMode Ascending }

                    ( newModel, _ ) =
                        update (KeyPressed "s" False False False) model
                in
                case newModel.ornamentMode of
                    MeendEndMode _ Ascending ->
                        Expect.pass

                    _ ->
                        Expect.fail ("Expected MeendEndMode Ascending, got: " ++ Debug.toString newModel.ornamentMode)
        , test "typing end note in MeendEndMode dispatches API" <|
            \_ ->
                let
                    noteRef =
                        { note = Sa, variant = Shuddha, octave = Madhya }

                    model =
                        { defaultModel | ornamentMode = MeendEndMode noteRef Ascending }

                    ( newModel, _ ) =
                        update (KeyPressed "r" False False False) model
                in
                Expect.all
                    [ \m -> Expect.equal NoOrnament m.ornamentMode
                    , \m -> Expect.equal True m.pendingApiCall
                    ]
                    newModel
        ]


krintanCollectionTests : Test
krintanCollectionTests =
    describe "Krintan note collection"
        [ test "typing note in KrintanStartMode enters KrintanEndMode" <|
            \_ ->
                let
                    model =
                        { defaultModel | ornamentMode = KrintanStartMode }

                    ( newModel, _ ) =
                        update (KeyPressed "s" False False False) model
                in
                case newModel.ornamentMode of
                    KrintanEndMode _ ->
                        Expect.pass

                    _ ->
                        Expect.fail "Expected KrintanEndMode"
        , test "Enter in KrintanEndMode dispatches API" <|
            \_ ->
                let
                    noteRef =
                        { note = Sa, variant = Shuddha, octave = Madhya }

                    model =
                        { defaultModel | ornamentMode = KrintanEndMode noteRef }

                    ( newModel, _ ) =
                        update (KeyPressed "Enter" False False False) model
                in
                Expect.all
                    [ \m -> Expect.equal NoOrnament m.ornamentMode
                    , \m -> Expect.equal True m.pendingApiCall
                    ]
                    newModel
        ]


murkiCollectionTests : Test
murkiCollectionTests =
    describe "Murki note collection"
        [ test "typing notes in MurkiCollectMode accumulates" <|
            \_ ->
                let
                    model =
                        { defaultModel | ornamentMode = MurkiCollectMode [] }

                    ( newModel, _ ) =
                        update (KeyPressed "s" False False False) model
                in
                case newModel.ornamentMode of
                    MurkiCollectMode notes ->
                        Expect.equal 1 (List.length notes)

                    _ ->
                        Expect.fail "Expected MurkiCollectMode with one note"
        , test "Enter in MurkiCollectMode with notes dispatches API" <|
            \_ ->
                let
                    noteRef =
                        { note = Sa, variant = Shuddha, octave = Madhya }

                    model =
                        { defaultModel | ornamentMode = MurkiCollectMode [ noteRef ] }

                    ( newModel, _ ) =
                        update (KeyPressed "Enter" False False False) model
                in
                Expect.all
                    [ \m -> Expect.equal NoOrnament m.ornamentMode
                    , \m -> Expect.equal True m.pendingApiCall
                    ]
                    newModel
        , test "Enter in MurkiCollectMode with empty list cancels" <|
            \_ ->
                let
                    model =
                        { defaultModel | ornamentMode = MurkiCollectMode [] }

                    ( newModel, _ ) =
                        update (KeyPressed "Enter" False False False) model
                in
                Expect.equal NoOrnament newModel.ornamentMode
        ]


zamzamaCollectionTests : Test
zamzamaCollectionTests =
    describe "Zamzama note collection"
        [ test "typing notes in ZamzamaCollectMode accumulates" <|
            \_ ->
                let
                    model =
                        { defaultModel | ornamentMode = ZamzamaCollectMode [] }

                    ( newModel, _ ) =
                        update (KeyPressed "s" False False False) model
                in
                case newModel.ornamentMode of
                    ZamzamaCollectMode notes ->
                        Expect.equal 1 (List.length notes)

                    _ ->
                        Expect.fail "Expected ZamzamaCollectMode with one note"
        , test "Enter in ZamzamaCollectMode with notes dispatches API" <|
            \_ ->
                let
                    noteRef =
                        { note = Sa, variant = Shuddha, octave = Madhya }

                    model =
                        { defaultModel | ornamentMode = ZamzamaCollectMode [ noteRef ] }

                    ( newModel, _ ) =
                        update (KeyPressed "Enter" False False False) model
                in
                Expect.all
                    [ \m -> Expect.equal NoOrnament m.ornamentMode
                    , \m -> Expect.equal True m.pendingApiCall
                    ]
                    newModel
        ]
