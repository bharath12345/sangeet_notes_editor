module DirtyFlagTest exposing (suite)

import Expect
import State.Model exposing (FileTab)
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultModel, defaultSnapshot)


activeTab : { a | tabs : List FileTab, activeTabId : Maybe String } -> Maybe FileTab
activeTab m =
    case m.activeTabId of
        Just id ->
            m.tabs |> List.filter (\t -> t.id == id) |> List.head

        Nothing ->
            Nothing


suite : Test
suite =
    describe "Dirty flag transitions"
        [ test "fresh model: active tab starts clean" <|
            \_ ->
                activeTab defaultModel
                    |> Maybe.map .isDirty
                    |> Expect.equal (Just False)
        , test "switching tabs does not set dirty" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update NewTab defaultModel

                    ( m2, _ ) =
                        update (SwitchTab "tab-1") m1
                in
                activeTab m2
                    |> Maybe.map .isDirty
                    |> Expect.equal (Just False)
        , test "opening a new (untouched) tab leaves it clean" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update NewTab defaultModel
                in
                activeTab m1
                    |> Maybe.map .isDirty
                    |> Expect.equal (Just False)
        , test "Undo / Redo on a clean tab does NOT flip dirty" <|
            \_ ->
                -- Even if Undo dispatches via the wrapping `update`, the dirty
                -- watcher excludes Undo/Redo (they navigate within committed
                -- history, not a fresh edit).
                let
                    ( m1, _ ) =
                        update Undo defaultModel

                    ( m2, _ ) =
                        update Redo m1
                in
                activeTab m2
                    |> Maybe.map .isDirty
                    |> Expect.equal (Just False)
        , test "history present snapshot is preserved across SwitchTab" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update NewTab defaultModel

                    ( m2, _ ) =
                        update (SwitchTab "tab-1") m1
                in
                UndoHistory.present m2.history
                    |> Expect.equal defaultSnapshot
        ]
