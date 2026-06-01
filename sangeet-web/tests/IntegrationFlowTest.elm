module IntegrationFlowTest exposing (..)

import Api.Client exposing (ApiResult(..))
import Expect
import Model.Composition exposing (Composition, CompositionType(..), SectionType(..))
import Model.Layout exposing (EditorResult)
import Model.Types exposing (MeendDirection(..), Note(..), Octave(..), Variant(..))
import State.Model as Model exposing (EditMode(..), OrnamentMode(..), PlaybackState(..))
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultComposition, defaultCursor, defaultModel)
import Time


suite : Test
suite =
    describe "Integration flow tests"
        [ insertAndUndoFlow
        , editModeToggleFlow
        , ornamentModeEscapeFlow
        , dialogOpenCloseFlow
        , viewToggleFlow
        , multiStepPlaybackFlow
        , scriptSwitchingFlow
        , sectionSwitchingFlow
        ]


insertAndUndoFlow : Test
insertAndUndoFlow =
    describe "Insert then undo via API result simulation"
        [ test "editor result followed by undo restores previous composition" <|
            \_ ->
                let
                    editorResult =
                        { composition = defaultComposition
                        , cursor = { defaultCursor | beat = 1 }
                        , message = "Inserted note"
                        }

                    ( afterInsert, _ ) =
                        update (GotEditorResult (Ok (Success editorResult))) defaultModel

                    ( afterUndo, _ ) =
                        update Undo afterInsert
                in
                Expect.equal "Untitled" (Model.composition afterUndo).metadata.title
        , test "two inserts then two undos returns to original" <|
            \_ ->
                let
                    result1 =
                        { composition = defaultComposition
                        , cursor = { defaultCursor | beat = 1 }
                        , message = "Insert 1"
                        }

                    result2 =
                        { composition = defaultComposition
                        , cursor = { defaultCursor | beat = 2 }
                        , message = "Insert 2"
                        }

                    ( m1, _ ) =
                        update (GotEditorResult (Ok (Success result1))) defaultModel

                    ( m2, _ ) =
                        update (GotEditorResult (Ok (Success result2))) m1

                    ( m3, _ ) =
                        update Undo m2

                    ( m4, _ ) =
                        update Undo m3
                in
                Expect.equal 0 (Model.cursor m4).beat
        , test "undo then redo restores state" <|
            \_ ->
                let
                    editorResult =
                        { composition = defaultComposition
                        , cursor = { defaultCursor | beat = 5 }
                        , message = "Insert"
                        }

                    ( afterInsert, _ ) =
                        update (GotEditorResult (Ok (Success editorResult))) defaultModel

                    ( afterUndo, _ ) =
                        update Undo afterInsert

                    ( afterRedo, _ ) =
                        update Redo afterUndo
                in
                Expect.equal 5 (Model.cursor afterRedo).beat
        ]


editModeToggleFlow : Test
editModeToggleFlow =
    describe "Edit mode toggle flow"
        [ test "toggle SwarEdit -> StrokeEdit -> SwarEdit" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update (KeyPressed "F2" False False False) defaultModel

                    ( m2, _ ) =
                        update (KeyPressed "F2" False False False) m1
                in
                Expect.equal SwarEdit m2.editMode
        ]


ornamentModeEscapeFlow : Test
ornamentModeEscapeFlow =
    describe "Ornament mode entry and escape"
        [ test "enter murki mode, type note, then escape cancels" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update (KeyPressed "u" False False True) defaultModel

                    ( m2, _ ) =
                        update (KeyPressed "s" False False False) m1

                    ( m3, _ ) =
                        update (KeyPressed "Escape" False False False) m2
                in
                Expect.equal NoOrnament m3.ornamentMode
        , test "enter meend mode, type start, then cancel" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update (KeyPressed "m" False False True) defaultModel

                    ( m2, _ ) =
                        update (KeyPressed "s" False False False) m1

                    ( m3, _ ) =
                        update (KeyPressed "Escape" False False False) m2
                in
                Expect.equal NoOrnament m3.ornamentMode
        ]


dialogOpenCloseFlow : Test
dialogOpenCloseFlow =
    describe "Dialog open/close flows"
        [ test "open new dialog, change fields, cancel restores state" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update ShowNewDialog defaultModel

                    ( m2, _ ) =
                        update (NewDialogSetTitle "Test") m1

                    ( m3, _ ) =
                        update NewDialogCancel m2
                in
                Expect.all
                    [ \m -> Expect.equal False m.showNewDialog
                    , \m -> Expect.equal "Test" m.newDialogForm.title
                    ]
                    m3
        , test "open props dialog populates from composition then cancel" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update ShowPropsDialog defaultModel

                    ( m2, _ ) =
                        update PropsDialogCancel m1
                in
                Expect.equal False m2.showPropsDialog
        ]


viewToggleFlow : Test
viewToggleFlow =
    describe "View toggle flows"
        [ test "toggle keyboard legend on and off" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update ToggleKeyboardLegend defaultModel

                    ( m2, _ ) =
                        update ToggleKeyboardLegend m1
                in
                Expect.equal False m2.showKeyboardLegend
        ]


multiStepPlaybackFlow : Test
multiStepPlaybackFlow =
    describe "Multi-step playback flow"
        [ test "play then pause then stop returns to Stopped" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update Play defaultModel

                    ( m2, _ ) =
                        update Pause m1

                    ( m3, _ ) =
                        update Stop m2
                in
                Expect.equal Stopped m3.playbackState
        ]


scriptSwitchingFlow : Test
scriptSwitchingFlow =
    describe "Script switching"
        [ test "switch through all scripts" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update (ChangeScript Model.Types.Kannada) defaultModel

                    ( m2, _ ) =
                        update (ChangeScript Model.Types.Telugu) m1

                    ( m3, _ ) =
                        update (ChangeScript Model.Types.English) m2

                    ( m4, _ ) =
                        update (ChangeScript Model.Types.Devanagari) m3
                in
                Expect.equal Model.Types.Devanagari m4.currentScript
        ]


sectionSwitchingFlow : Test
sectionSwitchingFlow =
    describe "Section switching"
        [ test "switching sections updates currentSectionIndex" <|
            \_ ->
                let
                    ( m1, _ ) =
                        update (SelectSection 1) defaultModel

                    ( m2, _ ) =
                        update (SelectSection 0) m1
                in
                Expect.equal 0 m2.currentSectionIndex
        ]
