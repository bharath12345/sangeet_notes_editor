module UpdateSectionTest exposing (addSectionTests, clearSectionTests, moveSectionTests, removeSectionTests, selectSectionTests, suite)

import Expect
import Model.Composition exposing (SectionType(..))
import State.Msg exposing (Msg(..))
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultModel)


suite : Test
suite =
    describe "Update section operations"
        [ selectSectionTests
        , addSectionTests
        , removeSectionTests
        , clearSectionTests
        , moveSectionTests
        ]


selectSectionTests : Test
selectSectionTests =
    describe "SelectSection"
        [ test "SelectSection updates currentSectionIndex" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (SelectSection 2) defaultModel
                in
                Expect.equal 2 newModel.currentSectionIndex
        , test "SelectSection adds log entry" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (SelectSection 1) defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "section" (String.toLower first))

                    [] ->
                        Expect.fail "statusLog should not be empty"
        ]


addSectionTests : Test
addSectionTests =
    describe "AddSection"
        [ test "AddSection sets pendingApiCall" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (AddSection "Antara" Antara) defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        ]


removeSectionTests : Test
removeSectionTests =
    describe "RemoveSection"
        [ test "RemoveSection sets pendingApiCall" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (RemoveSection 0) defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        ]


clearSectionTests : Test
clearSectionTests =
    describe "ClearSection"
        [ test "ClearSection sets pendingApiCall" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (ClearSection 0) defaultModel
                in
                Expect.equal True newModel.pendingApiCall
        , test "RequestClearSection shows dialog" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (RequestClearSection 0) defaultModel
                in
                Expect.equal True newModel.showClearSectionDialog
        ]


moveSectionTests : Test
moveSectionTests =
    describe "MoveSectionUp / MoveSectionDown"
        [ test "MoveSectionUp at index 0 does nothing" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (MoveSectionUp 0) defaultModel
                in
                Expect.equal False newModel.pendingApiCall
        , test "MoveSectionDown at last index does nothing" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (MoveSectionDown 0) defaultModel
                in
                Expect.equal False newModel.pendingApiCall
        ]
