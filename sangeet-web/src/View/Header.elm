module View.Header exposing (view)

import Html exposing (Html, div, span, text)
import Html.Attributes exposing (class)
import Model.Composition exposing (Metadata)
import Model.Cursor exposing (CursorModel)
import Model.Types exposing (Octave(..))
import State.Model exposing (EditMode(..))
import State.Msg exposing (Msg)
import UiStrings


{-| Render the editor status header showing cursor position, octave, and edit mode.
-}
view : Metadata -> CursorModel -> EditMode -> Html Msg
view metadata cursor editMode =
    div [ class "editor-header" ]
        [ div [ class "header-info" ]
            [ span [ class "header-chip" ]
                [ text (UiStrings.headerCyclePrefix ++ String.fromInt (cursor.cycle + 1)) ]
            , span [ class "header-chip" ]
                [ text (UiStrings.headerBeatPrefix ++ String.fromInt (cursor.beat + 1) ++ "/" ++ String.fromInt metadata.taal.matras) ]
            , span [ class "header-chip" ]
                [ text (UiStrings.headerSubPrefix ++ String.fromInt (cursor.subIndex + 1) ++ "/" ++ String.fromInt cursor.totalSubdivisions) ]
            , span [ class "header-chip" ]
                [ text (UiStrings.headerOctaveLabel ++ octaveToString cursor.currentOctave) ]
            , span [ class "header-chip" ]
                [ text
                    (UiStrings.headerModeLabel
                        ++ (case editMode of
                                SwarEdit ->
                                    UiStrings.headerModeSwar

                                StrokeEdit ->
                                    UiStrings.headerModeStroke
                           )
                    )
                ]
            ]
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
