module View.KeyboardLegend exposing (view)

import Html exposing (Html, div, h3, kbd, table, td, text, tr)
import Html.Attributes exposing (class)
import State.Msg exposing (Msg)
import UiStrings


{-| Keyboard shortcut reference sidebar.
-}
view : Html Msg
view =
    div [ class "keyboard-legend" ]
        [ h3 [ class "legend-title" ] [ text UiStrings.keyboardLegendTitleWeb ]
        , viewSection UiStrings.keyboardLegendSectionSwarInput
            [ ( "s r g m p d n", UiStrings.keyboardLegendSwarShuddha )
            , ( "Shift+R G D N", UiStrings.keyboardLegendSwarKomal )
            , ( "Shift+M", UiStrings.keyboardLegendSwarTivraMa )
            , ( "- (dash)", UiStrings.keyboardLegendSwarRest )
            , ( "= (equals)", UiStrings.keyboardLegendSwarSustain )
            , ( "ss rr gg ...", UiStrings.keyboardLegendSwarDualSwar )
            ]
        , viewSection UiStrings.keyboardLegendSectionNavigation
            [ ( "← →", UiStrings.keyboardLegendNavPrevNextBeatWeb )
            , ( "Tab", UiStrings.keyboardLegendNavTabWeb )
            , ( "Backspace", UiStrings.keyboardLegendNavBackspaceWeb )
            ]
        , viewSection UiStrings.keyboardLegendSectionOctave
            [ ( "[", UiStrings.keyboardLegendOctaveMandraWeb )
            , ( "\\", UiStrings.keyboardLegendOctaveMadhyaWeb )
            , ( "]", UiStrings.keyboardLegendOctaveTaarWeb )
            ]
        , viewSection UiStrings.keyboardLegendSectionSpecial
            [ ( "1", UiStrings.keyboardLegendSpecialChikari )
            , ( "2-8", UiStrings.keyboardLegendSpecialSubdivisions )
            ]
        , viewSection UiStrings.keyboardLegendSectionStrokes
            [ ( "Shift+Tab", UiStrings.keyboardLegendStrokesToggleModeWeb )
            , ( "d r j", UiStrings.keyboardLegendStrokesKeysWeb )
            ]
        , viewSection UiStrings.keyboardLegendSectionOrnaments
            [ ( "Alt+g", UiStrings.keyboardLegendOrnamentsGamak )
            , ( "Alt+a", UiStrings.keyboardLegendOrnamentsAndolan )
            , ( "Alt+i", UiStrings.keyboardLegendOrnamentsGitkari )
            , ( "Alt+k", UiStrings.keyboardLegendOrnamentsKan )
            , ( "Alt+s", UiStrings.keyboardLegendOrnamentsSparsh )
            , ( "Alt+h", UiStrings.keyboardLegendOrnamentsGhaseet )
            , ( "Alt+m", UiStrings.keyboardLegendOrnamentsMeendAsc )
            , ( "Alt+M", UiStrings.keyboardLegendOrnamentsMeendDesc )
            , ( "Alt+r", UiStrings.keyboardLegendOrnamentsKrintanWeb )
            , ( "Alt+u", UiStrings.keyboardLegendOrnamentsMurki )
            , ( "Alt+z", UiStrings.keyboardLegendOrnamentsZamzama )
            , ( "Escape", UiStrings.keyboardLegendOrnamentsCancel )
            ]
        , viewSection UiStrings.keyboardLegendSectionUndoRedo
            [ ( "Ctrl+Z", UiStrings.keyboardLegendUndo )
            , ( "Ctrl+Y", UiStrings.keyboardLegendRedoWeb )
            ]
        ]


viewSection : String -> List ( String, String ) -> Html msg
viewSection title shortcuts =
    div [ class "legend-section" ]
        [ h3 [ class "legend-section-title" ] [ text title ]
        , table [ class "legend-table" ]
            (List.map
                (\( key, desc ) ->
                    tr []
                        [ td [ class "legend-key" ] [ kbd [] [ text key ] ]
                        , td [ class "legend-desc" ] [ text desc ]
                        ]
                )
                shortcuts
            )
        ]
