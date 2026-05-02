module View.SwarGlyph exposing
    ( drawSwar
    , drawRest
    , drawSustain
    )

import Html exposing (Html, div, span, text)
import Html.Attributes exposing (class, style)
import Model.Types exposing (Note(..), Octave(..), SwarScript(..), Variant(..))
import View.Colors exposing (NotationColors)


{-| Render a swar note glyph with octave dots, komal underline, and tivra overbar.
-}
drawSwar : NotationColors -> SwarScript -> Note -> Variant -> Octave -> Html msg
drawSwar colors script note variant octave =
    let
        noteText =
            swarToScript script note

        variantClass =
            case variant of
                Komal ->
                    "swar-komal"

                Tivra ->
                    "swar-tivra"

                Shuddha ->
                    ""

        variantStyle =
            case variant of
                Komal ->
                    [ style "text-decoration" "underline"
                    , style "text-decoration-color" colors.komalMark
                    ]

                Tivra ->
                    [ style "text-decoration" "overline"
                    , style "text-decoration-color" colors.tivraMark
                    ]

                Shuddha ->
                    []

        octaveDots =
            case octave of
                AtiMandra ->
                    [ div [ class "octave-dots octave-dots-below", style "color" colors.octaveDot ]
                        [ text "\u{2022}\u{2022}" ]
                    ]

                Mandra ->
                    [ div [ class "octave-dots octave-dots-below", style "color" colors.octaveDot ]
                        [ text "\u{2022}" ]
                    ]

                Madhya ->
                    []

                Taar ->
                    [ div [ class "octave-dots octave-dots-above", style "color" colors.octaveDot ]
                        [ text "\u{2022}" ]
                    ]

                AtiTaar ->
                    [ div [ class "octave-dots octave-dots-above", style "color" colors.octaveDot ]
                        [ text "\u{2022}\u{2022}" ]
                    ]

        aboveDots =
            List.filter (\el -> isAboveDot octave) octaveDots

        belowDots =
            List.filter (\el -> isBelowDot octave) octaveDots
    in
    div [ class ("swar-glyph " ++ variantClass) ]
        (octaveDotsAbove octave colors
            ++ [ span
                    ([ class "swar-text"
                     , style "color" colors.swar
                     ]
                        ++ variantStyle
                    )
                    [ text noteText ]
               ]
            ++ octaveDotsBelow octave colors
        )


octaveDotsAbove : Octave -> NotationColors -> List (Html msg)
octaveDotsAbove octave colors =
    case octave of
        Taar ->
            [ div [ class "octave-dots octave-dots-above", style "color" colors.octaveDot ]
                [ text "\u{2022}" ]
            ]

        AtiTaar ->
            [ div [ class "octave-dots octave-dots-above", style "color" colors.octaveDot ]
                [ text "\u{2022}\u{2022}" ]
            ]

        _ ->
            []


octaveDotsBelow : Octave -> NotationColors -> List (Html msg)
octaveDotsBelow octave colors =
    case octave of
        Mandra ->
            [ div [ class "octave-dots octave-dots-below", style "color" colors.octaveDot ]
                [ text "\u{2022}" ]
            ]

        AtiMandra ->
            [ div [ class "octave-dots octave-dots-below", style "color" colors.octaveDot ]
                [ text "\u{2022}\u{2022}" ]
            ]

        _ ->
            []


isAboveDot : Octave -> Bool
isAboveDot octave =
    case octave of
        Taar ->
            True

        AtiTaar ->
            True

        _ ->
            False


isBelowDot : Octave -> Bool
isBelowDot octave =
    case octave of
        Mandra ->
            True

        AtiMandra ->
            True

        _ ->
            False


{-| Map a note to its display string in the given script.
-}
swarToScript : SwarScript -> Note -> String
swarToScript script note =
    case script of
        Devanagari ->
            case note of
                Sa ->
                    "\u{0938}"

                Re ->
                    "\u{0930}\u{0947}"

                Ga ->
                    "\u{0917}"

                Ma ->
                    "\u{092E}"

                Pa ->
                    "\u{092A}"

                Dha ->
                    "\u{0927}"

                Ni ->
                    "\u{0928}\u{093F}"

        Kannada ->
            case note of
                Sa ->
                    "\u{0CB8}"

                Re ->
                    "\u{0CB0}\u{0CBF}"

                Ga ->
                    "\u{0C97}"

                Ma ->
                    "\u{0CAE}"

                Pa ->
                    "\u{0CAA}"

                Dha ->
                    "\u{0CA7}"

                Ni ->
                    "\u{0CA8}\u{0CBF}"

        Telugu ->
            case note of
                Sa ->
                    "\u{0C38}"

                Re ->
                    "\u{0C30}\u{0C3F}"

                Ga ->
                    "\u{0C17}"

                Ma ->
                    "\u{0C2E}"

                Pa ->
                    "\u{0C2A}"

                Dha ->
                    "\u{0C27}"

                Ni ->
                    "\u{0C28}\u{0C3F}"

        English ->
            case note of
                Sa ->
                    "Sa"

                Re ->
                    "Re"

                Ga ->
                    "Ga"

                Ma ->
                    "Ma"

                Pa ->
                    "Pa"

                Dha ->
                    "Dha"

                Ni ->
                    "Ni"


{-| Render a rest symbol.
-}
drawRest : NotationColors -> Html msg
drawRest colors =
    div [ class "swar-glyph swar-rest" ]
        [ span [ class "swar-text", style "color" colors.rest ]
            [ text "\u{2013}" ]
        ]


{-| Render a sustain symbol.
-}
drawSustain : NotationColors -> Html msg
drawSustain colors =
    div [ class "swar-glyph swar-sustain" ]
        [ span [ class "swar-text", style "color" colors.sustain ]
            [ text "\u{2014}" ]
        ]
