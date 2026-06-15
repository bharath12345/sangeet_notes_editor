module UpdateTabTest exposing (closeTabTests, newTabTests, suite, switchTabTests)

import Expect
import State.Model exposing (EditMode(..))
import State.Msg exposing (Msg(..))
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultModel)


suite : Test
suite =
    describe "Tab management"
        [ newTabTests
        , switchTabTests
        , closeTabTests
        ]


newTabTests : Test
newTabTests =
    describe "NewTab"
        [ test "NewTab adds a tab to the tabs list" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update NewTab defaultModel
                in
                Expect.equal 2 (List.length newModel.tabs)
        , test "NewTab sets activeTabId to the new tab" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update NewTab defaultModel
                in
                Expect.equal (Just "tab-2") newModel.activeTabId
        , test "NewTab increments nextTabId" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update NewTab defaultModel
                in
                Expect.equal 3 newModel.nextTabId
        , test "NewTab resets editMode to SwarEdit" <|
            \_ ->
                let
                    model =
                        { defaultModel | editMode = StrokeEdit }

                    ( newModel, _ ) =
                        update NewTab model
                in
                Expect.equal SwarEdit newModel.editMode
        , test "NewTab resets currentSectionIndex to 0" <|
            \_ ->
                let
                    model =
                        { defaultModel | currentSectionIndex = 3 }

                    ( newModel, _ ) =
                        update NewTab model
                in
                Expect.equal 0 newModel.currentSectionIndex
        , test "NewTab adds log entry" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update NewTab defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "New tab" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        , test "Multiple NewTab calls create distinct tabs" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update NewTab defaultModel

                    ( m2, _ ) =
                        update NewTab m1
                in
                Expect.equal 3 (List.length m2.tabs)
        ]


switchTabTests : Test
switchTabTests =
    describe "SwitchTab"
        [ test "SwitchTab to same tab is a no-op" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (SwitchTab "tab-1") defaultModel
                in
                Expect.equal defaultModel newModel
        , test "SwitchTab to existing tab changes activeTabId" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update NewTab defaultModel

                    ( m2, _ ) =
                        update (SwitchTab "tab-1") m1
                in
                Expect.equal (Just "tab-1") m2.activeTabId
        , test "SwitchTab preserves state of outgoing tab" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update NewTab defaultModel

                    modelWithStroke =
                        { m1 | editMode = StrokeEdit }

                    ( m2, _ ) =
                        update (SwitchTab "tab-1") modelWithStroke

                    tab2 =
                        m2.tabs
                            |> List.filter (\t -> t.id == "tab-2")
                            |> List.head
                in
                case tab2 of
                    Just t ->
                        Expect.equal StrokeEdit t.editMode

                    Nothing ->
                        Expect.fail "tab-2 should still exist"
        , test "SwitchTab to nonexistent tab is a no-op" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (SwitchTab "tab-999") defaultModel
                in
                Expect.equal (Just "tab-1") newModel.activeTabId
        , test "SwitchTab restores editMode from target tab" <|
            \_ ->
                let
                    -- Create second tab
                    ( m1, _ ) =
                        update NewTab defaultModel

                    -- Set stroke mode on original tab
                    ( m2, _ ) =
                        update (SwitchTab "tab-1") m1

                    m3 =
                        { m2 | editMode = StrokeEdit }

                    -- Switch back to tab-2
                    ( m4, _ ) =
                        update (SwitchTab "tab-2") m3
                in
                Expect.equal SwarEdit m4.editMode
        ]


closeTabTests : Test
closeTabTests =
    describe "CloseTab"
        [ test "CloseTab removes the tab from list" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update NewTab defaultModel

                    ( m2, _ ) =
                        update (CloseTab "tab-1") m1
                in
                Expect.equal 1 (List.length m2.tabs)
        , test "CloseTab switches to remaining tab when active is closed" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update NewTab defaultModel

                    ( m2, _ ) =
                        update (CloseTab "tab-2") m1
                in
                Expect.equal (Just "tab-1") m2.activeTabId
        , test "Closing last tab leaves empty tabs list" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (CloseTab "tab-1") defaultModel
                in
                Expect.equal 0 (List.length newModel.tabs)
        , test "Closing last tab sets activeTabId to Nothing" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (CloseTab "tab-1") defaultModel
                in
                Expect.equal Nothing newModel.activeTabId
        , test "Closing inactive tab preserves activeTabId" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update NewTab defaultModel

                    ( m2, _ ) =
                        update (CloseTab "tab-1") m1
                in
                Expect.equal (Just "tab-2") m2.activeTabId
        , test "CloseTab adds log entry" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update NewTab defaultModel

                    ( m2, _ ) =
                        update (CloseTab "tab-1") m1
                in
                case m2.statusLog of
                    first :: _ ->
                        Expect.equal True
                            (String.contains "Closed" first
                                || String.contains "closed" first
                                || String.contains "switched" first
                            )

                    [] ->
                        Expect.fail "statusLog should not be empty"
        ]
