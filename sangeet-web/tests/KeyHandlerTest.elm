module KeyHandlerTest exposing (altOrnamentKeys, ctrlKeys, editKeys, finishCancelKeys, modeToggleKeys, navigationKeys, octaveKeys, plainSwarKeys, shiftKomalTivraKeys, subdivisionKeys, suite, unknownKeys)

import Expect
import Input.KeyHandler exposing (KeyAction(..), mapKeyToAction)
import Model.Types exposing (Note(..), Variant(..))
import Test exposing (Test, describe, test)


suite : Test
suite =
    describe "KeyHandler.mapKeyToAction"
        [ plainSwarKeys
        , shiftKomalTivraKeys
        , ctrlKeys
        , altOrnamentKeys
        , navigationKeys
        , subdivisionKeys
        , octaveKeys
        , editKeys
        , modeToggleKeys
        , finishCancelKeys
        , unknownKeys
        ]


plainSwarKeys : Test
plainSwarKeys =
    describe "Plain swar keys"
        [ test "s maps to Sa Shuddha" <|
            \_ ->
                mapKeyToAction "s" False False False
                    |> Expect.equal (SwarInput Sa Shuddha)
        , test "r maps to Re Shuddha" <|
            \_ ->
                mapKeyToAction "r" False False False
                    |> Expect.equal (SwarInput Re Shuddha)
        , test "g maps to Ga Shuddha" <|
            \_ ->
                mapKeyToAction "g" False False False
                    |> Expect.equal (SwarInput Ga Shuddha)
        , test "m maps to Ma Shuddha" <|
            \_ ->
                mapKeyToAction "m" False False False
                    |> Expect.equal (SwarInput Ma Shuddha)
        , test "p maps to Pa Shuddha" <|
            \_ ->
                mapKeyToAction "p" False False False
                    |> Expect.equal (SwarInput Pa Shuddha)
        , test "d maps to Dha Shuddha" <|
            \_ ->
                mapKeyToAction "d" False False False
                    |> Expect.equal (SwarInput Dha Shuddha)
        , test "n maps to Ni Shuddha" <|
            \_ ->
                mapKeyToAction "n" False False False
                    |> Expect.equal (SwarInput Ni Shuddha)
        ]


shiftKomalTivraKeys : Test
shiftKomalTivraKeys =
    describe "Shift komal/tivra keys"
        [ test "Shift+R maps to Re Komal" <|
            \_ ->
                mapKeyToAction "R" True False False
                    |> Expect.equal (SwarInput Re Komal)
        , test "Shift+G maps to Ga Komal" <|
            \_ ->
                mapKeyToAction "G" True False False
                    |> Expect.equal (SwarInput Ga Komal)
        , test "Shift+D maps to Dha Komal" <|
            \_ ->
                mapKeyToAction "D" True False False
                    |> Expect.equal (SwarInput Dha Komal)
        , test "Shift+N maps to Ni Komal" <|
            \_ ->
                mapKeyToAction "N" True False False
                    |> Expect.equal (SwarInput Ni Komal)
        , test "Shift+M maps to Ma Tivra" <|
            \_ ->
                mapKeyToAction "M" True False False
                    |> Expect.equal (SwarInput Ma Tivra)
        ]


ctrlKeys : Test
ctrlKeys =
    describe "Ctrl key combos"
        [ test "Ctrl+z maps to UndoAction" <|
            \_ ->
                mapKeyToAction "z" False True False
                    |> Expect.equal UndoAction
        , test "Ctrl+y maps to RedoAction" <|
            \_ ->
                mapKeyToAction "y" False True False
                    |> Expect.equal RedoAction
        , test "Ctrl+Shift+Z maps to RedoAction" <|
            \_ ->
                mapKeyToAction "Z" False True False
                    |> Expect.equal RedoAction
        ]


altOrnamentKeys : Test
altOrnamentKeys =
    describe "Alt ornament keys"
        [ test "Alt+g maps to OrnamentGamak" <|
            \_ ->
                mapKeyToAction "g" False False True
                    |> Expect.equal OrnamentGamak
        , test "Alt+a maps to OrnamentAndolan" <|
            \_ ->
                mapKeyToAction "a" False False True
                    |> Expect.equal OrnamentAndolan
        , test "Alt+i maps to OrnamentGitkari" <|
            \_ ->
                mapKeyToAction "i" False False True
                    |> Expect.equal OrnamentGitkari
        , test "Alt+k maps to OrnamentKanSwar" <|
            \_ ->
                mapKeyToAction "k" False False True
                    |> Expect.equal OrnamentKanSwar
        , test "Alt+s maps to OrnamentSparsh" <|
            \_ ->
                mapKeyToAction "s" False False True
                    |> Expect.equal OrnamentSparsh
        , test "Alt+h maps to OrnamentGhaseet" <|
            \_ ->
                mapKeyToAction "h" False False True
                    |> Expect.equal OrnamentGhaseet
        , test "Alt+m maps to OrnamentMeendAsc" <|
            \_ ->
                mapKeyToAction "m" False False True
                    |> Expect.equal OrnamentMeendAsc
        , test "Alt+M maps to OrnamentMeendDesc" <|
            \_ ->
                mapKeyToAction "M" False False True
                    |> Expect.equal OrnamentMeendDesc
        , test "Alt+r maps to OrnamentKrintan" <|
            \_ ->
                mapKeyToAction "r" False False True
                    |> Expect.equal OrnamentKrintan
        , test "Alt+u maps to OrnamentMurki" <|
            \_ ->
                mapKeyToAction "u" False False True
                    |> Expect.equal OrnamentMurki
        , test "Alt+z maps to OrnamentZamzama" <|
            \_ ->
                mapKeyToAction "z" False False True
                    |> Expect.equal OrnamentZamzama
        , test "Alt+Escape maps to OrnamentCancel" <|
            \_ ->
                mapKeyToAction "Escape" False False True
                    |> Expect.equal OrnamentCancel
        ]


navigationKeys : Test
navigationKeys =
    describe "Navigation keys"
        [ test "ArrowRight maps to NavRight" <|
            \_ ->
                mapKeyToAction "ArrowRight" False False False
                    |> Expect.equal NavRight
        , test "ArrowLeft maps to NavLeft" <|
            \_ ->
                mapKeyToAction "ArrowLeft" False False False
                    |> Expect.equal NavLeft
        , test "Tab maps to NavNextSubBeat" <|
            \_ ->
                mapKeyToAction "Tab" False False False
                    |> Expect.equal NavNextSubBeat
        ]


subdivisionKeys : Test
subdivisionKeys =
    describe "Subdivision keys"
        [ test "1 maps to Subdivision 1" <|
            \_ ->
                mapKeyToAction "1" False False False
                    |> Expect.equal (Subdivision 1)
        , test "2 maps to Subdivision 2" <|
            \_ ->
                mapKeyToAction "2" False False False
                    |> Expect.equal (Subdivision 2)
        , test "3 maps to Subdivision 3" <|
            \_ ->
                mapKeyToAction "3" False False False
                    |> Expect.equal (Subdivision 3)
        , test "4 maps to Subdivision 4" <|
            \_ ->
                mapKeyToAction "4" False False False
                    |> Expect.equal (Subdivision 4)
        , test "5 maps to Subdivision 5" <|
            \_ ->
                mapKeyToAction "5" False False False
                    |> Expect.equal (Subdivision 5)
        , test "6 maps to Subdivision 6" <|
            \_ ->
                mapKeyToAction "6" False False False
                    |> Expect.equal (Subdivision 6)
        , test "7 maps to Subdivision 7" <|
            \_ ->
                mapKeyToAction "7" False False False
                    |> Expect.equal (Subdivision 7)
        , test "8 maps to Subdivision 8" <|
            \_ ->
                mapKeyToAction "8" False False False
                    |> Expect.equal (Subdivision 8)
        ]


octaveKeys : Test
octaveKeys =
    describe "Octave keys"
        [ test "[ maps to OctaveMandra" <|
            \_ ->
                mapKeyToAction "[" False False False
                    |> Expect.equal OctaveMandra
        , test "] maps to OctaveTaar" <|
            \_ ->
                mapKeyToAction "]" False False False
                    |> Expect.equal OctaveTaar
        , test "\\ maps to OctaveMadhya" <|
            \_ ->
                mapKeyToAction "\\" False False False
                    |> Expect.equal OctaveMadhya
        ]


editKeys : Test
editKeys =
    describe "Edit keys"
        [ test "- maps to InsertRest" <|
            \_ ->
                mapKeyToAction "-" False False False
                    |> Expect.equal InsertRest
        , test "= maps to InsertSustain" <|
            \_ ->
                mapKeyToAction "=" False False False
                    |> Expect.equal InsertSustain
        , test "Backspace maps to DeleteLast" <|
            \_ ->
                mapKeyToAction "Backspace" False False False
                    |> Expect.equal DeleteLast
        , test "Delete maps to DeleteLast" <|
            \_ ->
                mapKeyToAction "Delete" False False False
                    |> Expect.equal DeleteLast
        ]


modeToggleKeys : Test
modeToggleKeys =
    describe "Mode toggle keys"
        [ test "F2 maps to ToggleEditMode" <|
            \_ ->
                mapKeyToAction "F2" False False False
                    |> Expect.equal ToggleEditMode
        , test "Shift+Tab maps to ToggleEditMode" <|
            \_ ->
                mapKeyToAction "Tab" True False False
                    |> Expect.equal ToggleEditMode
        ]


finishCancelKeys : Test
finishCancelKeys =
    describe "Finish and cancel keys"
        [ test "Enter maps to FinishOrnament" <|
            \_ ->
                mapKeyToAction "Enter" False False False
                    |> Expect.equal FinishOrnament
        , test "Escape maps to OrnamentCancel" <|
            \_ ->
                mapKeyToAction "Escape" False False False
                    |> Expect.equal OrnamentCancel
        ]


unknownKeys : Test
unknownKeys =
    describe "Unknown keys"
        [ test "q maps to NoAction" <|
            \_ ->
                mapKeyToAction "q" False False False
                    |> Expect.equal NoAction
        , test "x maps to NoAction" <|
            \_ ->
                mapKeyToAction "x" False False False
                    |> Expect.equal NoAction
        , test "Alt+unknown maps to NoAction" <|
            \_ ->
                mapKeyToAction "x" False False True
                    |> Expect.equal NoAction
        , test "Ctrl+unknown maps to NoAction" <|
            \_ ->
                mapKeyToAction "x" False True False
                    |> Expect.equal NoAction
        , test "Shift+unknown maps to NoAction" <|
            \_ ->
                mapKeyToAction "x" True False False
                    |> Expect.equal NoAction
        ]
