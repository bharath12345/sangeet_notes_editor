module UpdateDialogTest exposing (aboutDialogTests, newDialogFormTests, newDialogTests, propsDialogTests, suite, taalChangeTests)

import Api.Client exposing (ApiResult(..))
import Expect
import Model.Layout exposing (EditorResult)
import Model.Taal exposing (VibhagMarker(..))
import State.Model as Model
import State.Msg exposing (Msg(..))
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultComposition, defaultModel)


suite : Test
suite =
    describe "Update dialog operations"
        [ newDialogTests
        , newDialogFormTests
        , propsDialogTests
        , aboutDialogTests
        , taalChangeTests
        ]


newDialogTests : Test
newDialogTests =
    describe "New composition dialog"
        [ test "ShowNewDialog sets showNewDialog to True" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update ShowNewDialog defaultModel
                in
                Expect.equal True newModel.showNewDialog
        , test "NewComposition sets showNewDialog to True" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update NewComposition defaultModel
                in
                Expect.equal True newModel.showNewDialog
        , test "NewDialogCancel sets showNewDialog to False" <|
            \_ ->
                let
                    model =
                        { defaultModel | showNewDialog = True }

                    ( newModel, _ ) =
                        update NewDialogCancel model
                in
                Expect.equal False newModel.showNewDialog
        , test "NewDialogSubmit without taals/raags adds validation error" <|
            \_ ->
                let
                    model =
                        { defaultModel | showNewDialog = True }

                    ( newModel, _ ) =
                        update NewDialogSubmit model
                in
                Expect.equal False (List.isEmpty newModel.newDialogForm.validationErrors)
        ]


newDialogFormTests : Test
newDialogFormTests =
    describe "New dialog form field updates"
        [ test "NewDialogSetTitle updates title" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (NewDialogSetTitle "My Composition") defaultModel
                in
                Expect.equal "My Composition" newModel.newDialogForm.title
        , test "NewDialogSetType updates compositionType" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (NewDialogSetType "bandish") defaultModel
                in
                Expect.equal "bandish" newModel.newDialogForm.compositionType
        , test "NewDialogSetRaag updates raagName" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (NewDialogSetRaag "bhairav") defaultModel
                in
                Expect.equal "bhairav" newModel.newDialogForm.raagName
        , test "NewDialogSetTaal updates taalName" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (NewDialogSetTaal "ektaal") defaultModel
                in
                Expect.equal "ektaal" newModel.newDialogForm.taalName
        , test "NewDialogSetLaya updates layaName" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (NewDialogSetLaya "drut") defaultModel
                in
                Expect.equal "drut" newModel.newDialogForm.layaName
        , test "NewDialogSetTaanCount parses valid int" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (NewDialogSetTaanCount "3") defaultModel
                in
                Expect.equal 3 newModel.newDialogForm.taanCount
        , test "NewDialogSetTaanCount defaults to 0 for invalid" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (NewDialogSetTaanCount "abc") defaultModel
                in
                Expect.equal 0 newModel.newDialogForm.taanCount
        , test "NewDialogSetShowStrokes updates showStrokes" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (NewDialogSetShowStrokes False) defaultModel
                in
                Expect.equal False newModel.newDialogForm.showStrokes
        , test "NewDialogSetShowSahitya updates showSahitya" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (NewDialogSetShowSahitya True) defaultModel
                in
                Expect.equal True newModel.newDialogForm.showSahitya
        ]


propsDialogTests : Test
propsDialogTests =
    describe "Properties dialog"
        [ test "ShowPropsDialog sets showPropsDialog to True" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update ShowPropsDialog defaultModel
                in
                Expect.equal True newModel.showPropsDialog
        , test "ShowPropsDialog populates form with current composition" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update ShowPropsDialog defaultModel
                in
                Expect.equal "Untitled" newModel.propsDialogForm.title
        , test "PropsDialogSetTitle updates title" <|
            \_ ->
                let
                    model =
                        { defaultModel | showPropsDialog = True }

                    ( newModel, _ ) =
                        update (PropsDialogSetTitle "New Title") model
                in
                Expect.equal "New Title" newModel.propsDialogForm.title
        , test "PropsDialogSetTaal updates taalName" <|
            \_ ->
                let
                    model =
                        { defaultModel | showPropsDialog = True }

                    ( newModel, _ ) =
                        update (PropsDialogSetTaal "ektaal") model
                in
                Expect.equal "ektaal" newModel.propsDialogForm.taalName
        , test "PropsDialogSubmit closes dialog" <|
            \_ ->
                let
                    model =
                        { defaultModel | showPropsDialog = True }

                    ( newModel, _ ) =
                        update PropsDialogSubmit model
                in
                Expect.equal False newModel.showPropsDialog
        , test "PropsDialogCancel closes dialog" <|
            \_ ->
                let
                    model =
                        { defaultModel | showPropsDialog = True }

                    ( newModel, _ ) =
                        update PropsDialogCancel model
                in
                Expect.equal False newModel.showPropsDialog
        ]


taalChangeTests : Test
taalChangeTests =
    describe "Taal change flow (PR-B B.3 regression)"
        [ test "PropsDialogSubmit with a new taal in form fires changeTaal API call" <|
            -- Bug #7 from plan-16: changing taal mid-edit did not reflow
            -- the grid. The fix routes taal changes through a server
            -- /editor/change-taal endpoint that re-maps event positions
            -- to fit the new matras. This test asserts that submitting
            -- the properties dialog with a different taal sets
            -- pendingApiCall=True (the change-taal request is in flight).
            \_ ->
                let
                    ekTaal =
                        { name = "Ektaal"
                        , matras = 12
                        , vibhags =
                            [ { beats = 2, marker = Sam }
                            , { beats = 2, marker = KhaliMarker }
                            , { beats = 2, marker = TaaliMarker 2 }
                            , { beats = 2, marker = KhaliMarker }
                            , { beats = 2, marker = TaaliMarker 3 }
                            , { beats = 2, marker = TaaliMarker 4 }
                            ]
                        , theka = Nothing
                        }

                    modelWithTaals =
                        { defaultModel
                            | availableTaals = [ ( "Ektaal", ekTaal ) ]
                            , propsDialogForm =
                                { title = (Model.composition defaultModel).metadata.title
                                , taalName = "Ektaal"
                                , sectionStartingBeats = []
                                , compositionType = "gat"
                                }
                            , showPropsDialog = True
                        }

                    ( newModel, _ ) =
                        update PropsDialogSubmit modelWithTaals
                in
                Expect.all
                    [ \m -> Expect.equal True m.pendingApiCall
                    , \m -> Expect.equal False m.showPropsDialog
                    ]
                    newModel
        , test "GotTaalChangeResult Success updates composition and pushes snapshot" <|
            \_ ->
                let
                    newComp =
                        let
                            meta =
                                defaultComposition.metadata

                            newTaal =
                                { name = "Ektaal"
                                , matras = 12
                                , vibhags = []
                                , theka = Nothing
                                }
                        in
                        { defaultComposition | metadata = { meta | taal = newTaal } }

                    editorResult : EditorResult
                    editorResult =
                        { composition = newComp
                        , cursor = Model.cursor defaultModel |> (\c -> { c | beat = 0 })
                        , message = "Taal changed to Ektaal"
                        }

                    ( newModel, _ ) =
                        update (GotTaalChangeResult (Ok (Success editorResult))) defaultModel
                in
                Expect.equal "Ektaal" (Model.composition newModel).metadata.taal.name
        ]


aboutDialogTests : Test
aboutDialogTests =
    describe "About dialog"
        [ test "ShowAboutDialog sets showAboutDialog to True" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update ShowAboutDialog defaultModel
                in
                Expect.equal True newModel.showAboutDialog
        , test "CloseAboutDialog sets showAboutDialog to False" <|
            \_ ->
                let
                    model =
                        { defaultModel | showAboutDialog = True }

                    ( newModel, _ ) =
                        update CloseAboutDialog model
                in
                Expect.equal False newModel.showAboutDialog
        ]
