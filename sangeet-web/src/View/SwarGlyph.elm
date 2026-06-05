module View.SwarGlyph exposing
    ( drawRest
    , drawSustain
    , drawSwar
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
                [ text "•" ]
            ]

        AtiTaar ->
            [ div [ class "octave-dots octave-dots-above", style "color" colors.octaveDot ]
                [ text "••" ]
            ]

        _ ->
            []


octaveDotsBelow : Octave -> NotationColors -> List (Html msg)
octaveDotsBelow octave colors =
    case octave of
        Mandra ->
            [ div [ class "octave-dots octave-dots-below", style "color" colors.octaveDot ]
                [ text "•" ]
            ]

        AtiMandra ->
            [ div [ class "octave-dots octave-dots-below", style "color" colors.octaveDot ]
                [ text "••" ]
            ]

        _ ->
            []


{-| Map a note to its display string in the given script.
-}
swarToScript : SwarScript -> Note -> String
swarToScript script note =
    case script of
        Devanagari ->
            case note of
                Sa ->
                    "स"

                Re ->
                    "रे"

                Ga ->
                    "ग"

                Ma ->
                    "म"

                Pa ->
                    "प"

                Dha ->
                    "ध"

                Ni ->
                    "नि"

        Kannada ->
            case note of
                Sa ->
                    "ಸ"

                Re ->
                    "ರಿ"

                Ga ->
                    "ಗ"

                Ma ->
                    "ಮ"

                Pa ->
                    "ಪ"

                Dha ->
                    "ಧ"

                Ni ->
                    "ನಿ"

        Telugu ->
            case note of
                Sa ->
                    "స"

                Re ->
                    "రి"

                Ga ->
                    "గ"

                Ma ->
                    "మ"

                Pa ->
                    "ప"

                Dha ->
                    "ధ"

                Ni ->
                    "ని"

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
            [ text "–" ]
        ]


{-| Render a sustain symbol.
-}
drawSustain : NotationColors -> Html msg
drawSustain colors =
    div [ class "swar-glyph swar-sustain" ]
        [ span [ class "swar-text", style "color" colors.sustain ]
            [ text "—" ]
        ]
