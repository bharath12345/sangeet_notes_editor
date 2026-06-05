module View.KeyboardLegend exposing (view)

import Html exposing (Html, div, h3, kbd, table, td, text, tr)
import Html.Attributes exposing (class)
import State.Msg exposing (Msg)


{-| Keyboard shortcut reference sidebar.
-}
view : Html Msg
view =
    div [ class "keyboard-legend" ]
        [ h3 [ class "legend-title" ] [ text "Keyboard Shortcuts" ]
        , viewSection "Swar Input"
            [ ( "s r g m p d n", "Shuddha notes" )
            , ( "Shift+R G D N", "Komal variants" )
            , ( "Shift+M", "Tivra Ma" )
            , ( "- (dash)", "Rest" )
            , ( "= (equals)", "Sustain" )
            , ( "ss rr gg ...", "Dual swar (double-tap)" )
            ]
        , viewSection "Navigation"
            [ ( "← →", "Previous / Next beat" )
            , ( "Tab", "Next sub-beat" )
            , ( "Backspace", "Delete last" )
            ]
        , viewSection "Octave"
            [ ( "[", "Mandra (lower)" )
            , ( "\\", "Madhya (middle)" )
            , ( "]", "Taar (upper)" )
            ]
        , viewSection "Subdivision"
            [ ( "1-8", "Set subdivisions per beat" )
            ]
        , viewSection "Strokes"
            [ ( "Shift+Tab", "Toggle Swar/Stroke mode" )
            , ( "d a c j", "Da / Ra / Chikari / Jod (in stroke mode)" )
            ]
        , viewSection "Ornaments (Alt+key)"
            [ ( "Alt+g", "Gamak" )
            , ( "Alt+a", "Andolan" )
            , ( "Alt+i", "Gitkari" )
            , ( "Alt+k", "Kan Swar (then type note)" )
            , ( "Alt+s", "Sparsh (then type note)" )
            , ( "Alt+h", "Ghaseet (then type note)" )
            , ( "Alt+m", "Meend Asc (type start, end)" )
            , ( "Alt+M", "Meend Desc" )
            , ( "Alt+r", "Krintan (type notes, Enter)" )
            , ( "Alt+u", "Murki (type notes, Enter)" )
            , ( "Alt+z", "Zamzama (type notes, Enter)" )
            , ( "Escape", "Cancel ornament mode" )
            ]
        , viewSection "Undo/Redo"
            [ ( "Ctrl+Z", "Undo" )
            , ( "Ctrl+Y", "Redo" )
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
