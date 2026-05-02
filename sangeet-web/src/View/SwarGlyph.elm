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
                        [ text "\u2022\u2022" ]
                    ]

                Mandra ->
                    [ div [ class "octave-dots octave-dots-below", style "color" colors.octaveDot ]
                        [ text "\u2022" ]
                    ]

                Madhya ->
                    []

                Taar ->
                    [ div [ class "octave-dots octave-dots-above", style "color" colors.octaveDot ]
                        [ text "\u2022" ]
                    ]

                AtiTaar ->
                    [ div [ class "octave-dots octave-dots-above", style "color" colors.octaveDot ]
                        [ text "\u2022\u2022" ]
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
                [ text "\u2022" ]
            ]

        AtiTaar ->
            [ div [ class "octave-dots octave-dots-above", style "color" colors.octaveDot ]
                [ text "\u2022\u2022" ]
            ]

        _ ->
            []


octaveDotsBelow : Octave -> NotationColors -> List (Html msg)
octaveDotsBelow octave colors =
    case octave of
        Mandra ->
            [ div [ class "octave-dots octave-dots-below", style "color" colors.octaveDot ]
                [ text "\u2022" ]
            ]

        AtiMandra ->
            [ div [ class "octave-dots octave-dots-below", style "color" colors.octaveDot ]
                [ text "\u2022\u2022" ]
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
                    "\u0938"

                Re ->
                    "\u0930\u0947"

                Ga ->
                    "\u0917"

                Ma ->
                    "\u092E"

                Pa ->
                    "\u092A"

                Dha ->
                    "\u0927"

                Ni ->
                    "\u0928\u093F"

        Kannada ->
            case note of
                Sa ->
                    "\u0CB8"

                Re ->
                    "\u0CB0\u0CBF"

                Ga ->
                    "\u0C97"

                Ma ->
                    "\u0CAE"

                Pa ->
                    "\u0CAA"

                Dha ->
                    "\u0CA7"

                Ni ->
                    "\u0CA8\u0CBF"

        Telugu ->
            case note of
                Sa ->
                    "\u0C38"

                Re ->
                    "\u0C30\u0C3F"

                Ga ->
                    "\u0C17"

                Ma ->
                    "\u0C2E"

                Pa ->
                    "\u0C2A"

                Dha ->
                    "\u0C27"

                Ni ->
                    "\u0C28\u0C3F"

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
            [ text "\u2013" ]
        ]


{-| Render a sustain symbol.
-}
drawSustain : NotationColors -> Html msg
drawSustain colors =
    div [ class "swar-glyph swar-sustain" ]
        [ span [ class "swar-text", style "color" colors.sustain ]
            [ text "\u2014" ]
        ]
