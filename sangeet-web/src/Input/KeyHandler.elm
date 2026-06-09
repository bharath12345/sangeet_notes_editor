module Input.KeyHandler exposing
    ( KeyAction(..)
    , mapKeyToAction
    )

import Model.Types exposing (Note(..), Variant(..))


{-| Actions that keyboard input can trigger.
-}
type KeyAction
    = SwarInput Note Variant
    | InsertRest
    | InsertSustain
    | DeleteLast
    | NavRight
    | NavLeft
    | NavNextSubBeat
    | UndoAction
    | RedoAction
    | Subdivision Int
    | OctaveMandra
    | OctaveMadhya
    | OctaveTaar
    | InsertChikari
    | StrokeDa
    | StrokeRa
    | StrokeJod
    | StrokeClear
    | OrnamentGamak
    | OrnamentAndolan
    | OrnamentGitkari
    | OrnamentKanSwar
    | OrnamentSparsh
    | OrnamentGhaseet
    | OrnamentMeendAsc
    | OrnamentMeendDesc
    | OrnamentKrintan
    | OrnamentMurki
    | OrnamentZamzama
    | OrnamentCancel
    | FinishOrnament
    | ToggleEditMode
    | NoAction


{-| Map a raw key event to a KeyAction.
key: the key string from the browser event
shiftKey, ctrlKey, altKey: modifier flags
-}
mapKeyToAction : String -> Bool -> Bool -> Bool -> KeyAction
mapKeyToAction key shiftKey ctrlKey altKey =
    if ctrlKey then
        mapCtrlKey key

    else if altKey then
        mapAltKey key

    else if shiftKey then
        mapShiftKey key

    else
        mapPlainKey key


mapCtrlKey : String -> KeyAction
mapCtrlKey key =
    case key of
        "z" ->
            UndoAction

        "y" ->
            RedoAction

        "Z" ->
            RedoAction

        _ ->
            NoAction


mapAltKey : String -> KeyAction
mapAltKey key =
    case key of
        -- Ornament shortcuts with Alt
        "g" ->
            OrnamentGamak

        "a" ->
            OrnamentAndolan

        "i" ->
            OrnamentGitkari

        "k" ->
            OrnamentKanSwar

        "s" ->
            OrnamentSparsh

        "h" ->
            OrnamentGhaseet

        "m" ->
            OrnamentMeendAsc

        "M" ->
            OrnamentMeendDesc

        "r" ->
            OrnamentKrintan

        "u" ->
            OrnamentMurki

        "z" ->
            OrnamentZamzama

        "Escape" ->
            OrnamentCancel

        _ ->
            NoAction


mapShiftKey : String -> KeyAction
mapShiftKey key =
    case key of
        -- Komal variants (Shift + note key)
        "R" ->
            SwarInput Re Komal

        "G" ->
            SwarInput Ga Komal

        "D" ->
            SwarInput Dha Komal

        "N" ->
            SwarInput Ni Komal

        -- Tivra Ma
        "M" ->
            SwarInput Ma Tivra

        -- Tab for toggle edit mode
        "Tab" ->
            ToggleEditMode

        _ ->
            NoAction


mapPlainKey : String -> KeyAction
mapPlainKey key =
    case key of
        -- Swar input (shuddha variants)
        "s" ->
            SwarInput Sa Shuddha

        "r" ->
            SwarInput Re Shuddha

        "g" ->
            SwarInput Ga Shuddha

        "m" ->
            SwarInput Ma Shuddha

        "p" ->
            SwarInput Pa Shuddha

        "d" ->
            SwarInput Dha Shuddha

        "n" ->
            SwarInput Ni Shuddha

        -- Rest and sustain
        "-" ->
            InsertRest

        "=" ->
            InsertSustain

        -- Delete
        "Backspace" ->
            DeleteLast

        "Delete" ->
            DeleteLast

        -- Navigation
        "ArrowRight" ->
            NavRight

        "ArrowLeft" ->
            NavLeft

        "Tab" ->
            NavNextSubBeat

        -- Chikari (open strings)
        "1" ->
            InsertChikari

        -- Subdivision (number keys 2-8)
        "2" ->
            Subdivision 2

        "3" ->
            Subdivision 3

        "4" ->
            Subdivision 4

        "5" ->
            Subdivision 5

        "6" ->
            Subdivision 6

        "7" ->
            Subdivision 7

        "8" ->
            Subdivision 8

        -- Octave selection (brackets)
        "[" ->
            OctaveMandra

        "]" ->
            OctaveTaar

        "\\" ->
            OctaveMadhya

        -- Enter finishes multi-note ornament
        "Enter" ->
            FinishOrnament

        -- F2 toggles edit mode (swar/stroke)
        "F2" ->
            ToggleEditMode

        -- Escape cancels ornament mode
        "Escape" ->
            OrnamentCancel

        _ ->
            NoAction
