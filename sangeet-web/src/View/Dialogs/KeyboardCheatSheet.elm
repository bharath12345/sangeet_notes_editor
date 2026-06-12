module View.Dialogs.KeyboardCheatSheet exposing (view)

import Html exposing (Html, a, button, code, div, h2, h3, kbd, li, p, span, text, ul)
import Html.Attributes exposing (class, href, target)
import Html.Events exposing (onClick)
import State.Msg exposing (Msg(..))


view : Html Msg
view =
    div [ class "modal-overlay" ]
        [ div [ class "modal-dialog modal-cheatsheet" ]
            [ h2 [ class "modal-title" ] [ text "Keyboard Shortcuts" ]
            , div [ class "modal-body" ]
                [ p [ class "cheatsheet-hint" ]
                    [ text "Tip: most toolbar actions are accessible via the buttons above. The desktop app has the full set of "
                    , kbd [] [ text "Ctrl" ]
                    , text "-shortcuts wired (browsers reserve many of them on web). Full reference: "
                    , a
                        [ href "https://github.com/bharath12345/sangeet_notes_editor/blob/main/docs/user-guide/08-keyboard-reference.md"
                        , target "_blank"
                        ]
                        [ text "Keyboard Reference" ]
                    , text "."
                    ]
                , section "Swar (notes)"
                    [ row "s r g m p d n" "Shuddha swaras"
                    , row "Shift+R / G / D / N" "Komal Re / Ga / Dha / Ni"
                    , row "Shift+M" "Tivra Ma"
                    , row "1" "Chikari (open strings)"
                    , row "Space" "Rest"
                    , row "-" "Sustain"
                    , row "Backspace / Delete" "Delete event"
                    ]
                , section "Octave (saptak)"
                    [ row "[" "Mandra (lower)"
                    , row "]" "Taar (upper)"
                    , row "\\" "Madhya (default)"
                    ]
                , section "Subdivisions"
                    [ row "Ctrl+2 … Ctrl+8" "Set notes per beat"
                    , row "ss, rr, gg …" "Double-tap dual swar"
                    , row "Fast typing" "Type 2–4 notes within 500 ms to auto-group"
                    ]
                , section "Navigation"
                    [ row "← / →" "Move cursor one beat"
                    , row "Tab" "Next sub-beat"
                    , row "Enter" "Finish multi-note ornament"
                    ]
                , section "Selection & clipboard"
                    [ row "Shift+← / Shift+→" "Extend selection"
                    , row "Ctrl+X / C / V" "Cut / Copy / Paste"
                    , row "Ctrl+Z / Ctrl+Shift+Z" "Undo / Redo"
                    ]
                , section "Ornaments"
                    [ row "Alt+G / A / I" "Gamak / Andolan / Gitkari"
                    , row "Alt+K + swar" "Kan swar"
                    , row "Alt+H + swar" "Sparsh"
                    , row "Alt+M + swar+swar" "Meend ↑"
                    , row "Alt+Shift+M + swar+swar" "Meend ↓"
                    , row "Esc" "Cancel ornament mode"
                    ]
                , section "Strokes"
                    [ row "Ctrl+D / Ctrl+R" "Da (inward) / Ra (outward)"
                    ]
                , section "Help"
                    [ row "?" "Show this cheat sheet"
                    ]
                ]
            , div [ class "modal-footer" ]
                [ button [ class "btn btn-primary", onClick CloseKeyboardCheatSheet ]
                    [ text "Close" ]
                ]
            ]
        ]


section : String -> List (Html Msg) -> Html Msg
section title items =
    div [ class "cheatsheet-section" ]
        [ h3 [ class "cheatsheet-group-header" ] [ text title ]
        , ul [ class "cheatsheet-rows" ] items
        ]


row : String -> String -> Html Msg
row keys label =
    li [ class "cheatsheet-row" ]
        [ kbd [ class "cheatsheet-keys" ] [ text keys ]
        , span [ class "cheatsheet-label" ] [ text label ]
        ]
