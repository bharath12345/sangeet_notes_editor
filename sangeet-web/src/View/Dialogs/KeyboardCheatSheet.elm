module View.Dialogs.KeyboardCheatSheet exposing (view)

import Html exposing (Html, a, button, div, h2, h3, kbd, li, p, span, text, ul)
import Html.Attributes exposing (class, href, target)
import Html.Events exposing (onClick)
import State.Msg exposing (Msg(..))
import UiStrings


view : Html Msg
view =
    div [ class "modal-overlay" ]
        [ div [ class "modal-dialog modal-cheatsheet" ]
            [ h2 [ class "modal-title" ] [ text UiStrings.dialogKeyboardCheatSheetTitle ]
            , div [ class "modal-body" ]
                [ p [ class "cheatsheet-hint" ]
                    [ text UiStrings.dialogKeyboardCheatSheetHintWeb
                    , kbd [] [ text "Ctrl" ]
                    , text UiStrings.dialogKeyboardCheatSheetHintDesktopFull
                    , a
                        [ href "https://github.com/bharath12345/sangeet_notes_editor/blob/main/docs/user-guide/08-keyboard-reference.md"
                        , target "_blank"
                        ]
                        [ text UiStrings.dialogKeyboardCheatSheetHintKeyboardRef ]
                    , text "."
                    ]
                , section UiStrings.dialogKeyboardCheatSheetSectionSwar
                    [ row "s r g m p d n" UiStrings.dialogKeyboardCheatSheetLabelShuddhaSwaras
                    , row "Shift+R / G / D / N" UiStrings.dialogKeyboardCheatSheetLabelKomalRe
                    , row "Shift+M" UiStrings.dialogKeyboardCheatSheetLabelTivraMa
                    , row "1" UiStrings.dialogKeyboardCheatSheetLabelChikari
                    , row "Space" UiStrings.dialogKeyboardCheatSheetLabelRest
                    , row "-" UiStrings.dialogKeyboardCheatSheetLabelSustain
                    , row "Backspace / Delete" UiStrings.dialogKeyboardCheatSheetLabelDeleteEvent
                    ]
                , section UiStrings.dialogKeyboardCheatSheetSectionOctave
                    [ row "[" UiStrings.dialogKeyboardCheatSheetLabelMandraLower
                    , row "]" UiStrings.dialogKeyboardCheatSheetLabelTaarUpper
                    , row "\\" UiStrings.dialogKeyboardCheatSheetLabelMadhyaDefault
                    ]
                , section UiStrings.dialogKeyboardCheatSheetSectionSubdivisions
                    [ row "Ctrl+2 … Ctrl+8" UiStrings.dialogKeyboardCheatSheetLabelSetNotesPerBeat
                    , row "ss, rr, gg …" UiStrings.dialogKeyboardCheatSheetLabelDoubleTapDual
                    , row "Fast typing" UiStrings.dialogKeyboardCheatSheetLabelFastTyping
                    ]
                , section UiStrings.dialogKeyboardCheatSheetSectionNavigation
                    [ row "← / →" UiStrings.dialogKeyboardCheatSheetLabelMoveCursor
                    , row "Tab" UiStrings.dialogKeyboardCheatSheetLabelNextSubbeat
                    , row "Enter" UiStrings.dialogKeyboardCheatSheetLabelFinishOrnament
                    ]
                , section UiStrings.dialogKeyboardCheatSheetSectionSelectionClipboard
                    [ row "Shift+← / Shift+→" UiStrings.dialogKeyboardCheatSheetLabelExtendSelection
                    , row "Ctrl+X / C / V" UiStrings.dialogKeyboardCheatSheetLabelCutCopyPaste
                    , row "Ctrl+Z / Ctrl+Shift+Z" UiStrings.dialogKeyboardCheatSheetLabelUndoRedo
                    ]
                , section UiStrings.dialogKeyboardCheatSheetSectionOrnaments
                    [ row "Alt+G / A / I" UiStrings.dialogKeyboardCheatSheetLabelGamakAndolan
                    , row "Alt+K + swar" UiStrings.dialogKeyboardCheatSheetLabelKanSwar
                    , row "Alt+H + swar" UiStrings.dialogKeyboardCheatSheetLabelSparsh
                    , row "Alt+M + swar+swar" UiStrings.dialogKeyboardCheatSheetLabelMeendUp
                    , row "Alt+Shift+M + swar+swar" UiStrings.dialogKeyboardCheatSheetLabelMeendDown
                    , row "Esc" UiStrings.dialogKeyboardCheatSheetLabelCancelOrnament
                    ]
                , section UiStrings.dialogKeyboardCheatSheetSectionStrokes
                    [ row "Ctrl+D / Ctrl+R" UiStrings.dialogKeyboardCheatSheetLabelDaRaStrokes
                    ]
                , section UiStrings.dialogKeyboardCheatSheetSectionHelpWeb
                    [ row "?" UiStrings.dialogKeyboardCheatSheetLabelShowCheatSheet
                    ]

                -- PR-C C.4: keyboard reference (formerly the right-side panel)
                -- merged into the cheat sheet. Hardcoded strings here (the
                -- plan permits this — see "Open questions for future
                -- iteration" in the spec).
                , section "Swar (notes) — quick reference"
                    [ row "s r g m p d n" "Shuddha swaras"
                    , row "Shift+R / G / D / N" "Komal Re / Ga / Dha / Ni"
                    , row "Shift+M" "Tivra Ma"
                    , row "1" "Chikari (open strings)"
                    , row "Space" "Rest (silence)"
                    , row "-" "Sustain (hold previous note)"
                    , row "Backspace / Delete" "Delete event"
                    ]
                , section "Octave (saptak)"
                    [ row "[" "Mandra (lower) — dot below"
                    , row "]" "Taar (upper) — dot above"
                    , row "\\" "Madhya (default)"
                    ]
                , section "Strokes (mizrab Da / Ra)"
                    [ row "Ctrl+D" "Mark last note as Da"
                    , row "Ctrl+R" "Mark last note as Ra"
                    ]
                , section "Ornaments — reference"
                    [ row "Alt+G / A / I" "Gamak / Andolan / Gitkari (apply to last)"
                    , row "Alt+K + swar" "Kan swar (grace note)"
                    , row "Alt+H + swar" "Sparsh (light touch)"
                    , row "Alt+S + swar" "Ghaseet (heavy pull)"
                    , row "Alt+M + swar + swar" "Meend ↑ (ascending glide)"
                    , row "Alt+Shift+M + swar + swar" "Meend ↓ (descending glide)"
                    , row "Alt+R + swar + swar" "Krintan (pull-off sequence)"
                    , row "Alt+U, swars, Enter" "Murki (ornamental turn)"
                    , row "Alt+Z, swars, Enter" "Zamzama (rapid cluster)"
                    , row "Esc" "Cancel ornament mode"
                    ]
                ]
            , div [ class "modal-footer" ]
                [ button [ class "btn btn-primary", onClick CloseKeyboardCheatSheet ]
                    [ text UiStrings.dialogKeyboardCheatSheetButtonClose ]
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
