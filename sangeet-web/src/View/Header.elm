module View.Header exposing (view)

import Html exposing (Html, div, span, text)
import Html.Attributes exposing (class)
import Model.Composition exposing (CompositionType(..), Metadata)
import Model.Cursor exposing (CursorModel)
import Model.Raag exposing (Raag)
import Model.Types exposing (Laya(..), Octave(..))
import State.Msg exposing (Msg)
import UiStrings


{-| Render the editor header showing composition metadata (Raag, Taal, Laya, Type)
together with the cursor position chips (Cycle, Beat, Sub, Octave). Arohan and
Avrohan render as a separate single line beneath the chip row.
-}
view : Metadata -> CursorModel -> Html Msg
view metadata cursor =
    div [ class "editor-header" ]
        [ div [ class "header-info" ]
            [ viewRaagChip metadata.raag
            , viewTaalChip metadata
            , viewLayaChip metadata.laya
            , viewTypeChip metadata.compositionType
            , span [ class "header-chip" ]
                [ text (UiStrings.headerCyclePrefix ++ String.fromInt (cursor.cycle + 1)) ]
            , span [ class "header-chip" ]
                [ text (UiStrings.headerBeatPrefix ++ String.fromInt (cursor.beat + 1) ++ "/" ++ String.fromInt metadata.taal.matras) ]
            , span [ class "header-chip" ]
                [ text (UiStrings.headerSubPrefix ++ String.fromInt (cursor.subIndex + 1) ++ "/" ++ String.fromInt cursor.totalSubdivisions) ]
            , span [ class "header-chip" ]
                [ text (UiStrings.headerOctaveLabel ++ octaveToString cursor.currentOctave) ]
            ]
        , viewArohanAvrohan metadata.raag
        ]


viewRaagChip : Raag -> Html Msg
viewRaagChip raag =
    span [ class "header-chip header-chip-meta" ]
        [ span [ class "header-chip-label" ] [ text UiStrings.headerRaagLabel ]
        , span [ class "header-chip-value" ] [ text raag.name ]
        ]


viewTaalChip : Metadata -> Html Msg
viewTaalChip metadata =
    span [ class "header-chip header-chip-meta" ]
        [ span [ class "header-chip-label" ] [ text UiStrings.headerTaalLabel ]
        , span [ class "header-chip-value" ]
            [ text (metadata.taal.name ++ " (" ++ String.fromInt metadata.taal.matras ++ ")") ]
        ]


viewLayaChip : Maybe Laya -> Html Msg
viewLayaChip maybeLaya =
    case maybeLaya of
        Just laya ->
            span [ class "header-chip header-chip-meta" ]
                [ span [ class "header-chip-label" ] [ text UiStrings.headerLayaLabel ]
                , span [ class "header-chip-value" ] [ text (layaToString laya) ]
                ]

        Nothing ->
            text ""


viewTypeChip : CompositionType -> Html Msg
viewTypeChip compType =
    span [ class "header-chip header-chip-meta" ]
        [ span [ class "header-chip-label" ] [ text "Type" ]
        , span [ class "header-chip-value" ] [ text (compositionTypeToString compType) ]
        ]


viewArohanAvrohan : Raag -> Html Msg
viewArohanAvrohan raag =
    let
        arohan =
            raag.arohana
                |> Maybe.map (\notes -> UiStrings.headerArohanLabel ++ ": " ++ String.join " " notes)
                |> Maybe.withDefault ""

        avrohan =
            raag.avarohana
                |> Maybe.map (\notes -> UiStrings.headerAvrohanLabel ++ ": " ++ String.join " " notes)
                |> Maybe.withDefault ""
    in
    if arohan == "" && avrohan == "" then
        text ""

    else
        div [ class "header-arohan-avrohan" ]
            [ if arohan /= "" then
                span [ class "header-arohan" ] [ text arohan ]

              else
                text ""
            , if avrohan /= "" then
                span [ class "header-avrohan" ] [ text avrohan ]

              else
                text ""
            ]


octaveToString : Octave -> String
octaveToString octave =
    case octave of
        AtiMandra ->
            UiStrings.headerOctaveAtiMandra

        Mandra ->
            UiStrings.headerOctaveMandra

        Madhya ->
            UiStrings.headerOctaveMadhya

        Taar ->
            UiStrings.headerOctaveTaar

        AtiTaar ->
            UiStrings.headerOctaveAtiTaar


layaToString : Laya -> String
layaToString laya =
    case laya of
        AtiVilambit ->
            "Ati-vilambit"

        Vilambit ->
            "Vilambit"

        MadhyaLaya ->
            "Madhya"

        Drut ->
            "Drut"

        AtiDrut ->
            "Ati-drut"


compositionTypeToString : CompositionType -> String
compositionTypeToString ct =
    case ct of
        Bandish ->
            "Bandish"

        Gat ->
            "Gat"

        Palta ->
            "Palta"

        Sargam ->
            "Sargam"

        CustomCompositionType name ->
            name
