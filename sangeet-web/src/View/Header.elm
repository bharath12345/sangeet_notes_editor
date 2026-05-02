module View.Header exposing (view)

import Html exposing (Html, div, span, text)
import Html.Attributes exposing (class)
import Model.Composition exposing (Metadata)
import Model.Cursor exposing (CursorModel)
import Model.Types exposing (Octave(..))
import State.Model exposing (EditMode(..))
import State.Msg exposing (Msg)


{-| Render the editor status header showing cursor position, octave, and edit mode.
-}
view : Metadata -> CursorModel -> EditMode -> Html Msg
view metadata cursor editMode =
    div [ class "editor-header" ]
        [ div [ class "header-info" ]
            [ span [ class "header-chip" ]
                [ text ("Cycle " ++ String.fromInt (cursor.cycle + 1)) ]
            , span [ class "header-chip" ]
                [ text ("Beat " ++ String.fromInt (cursor.beat + 1) ++ "/" ++ String.fromInt metadata.taal.matras) ]
            , span [ class "header-chip" ]
                [ text ("Sub " ++ String.fromInt (cursor.subIndex + 1) ++ "/" ++ String.fromInt cursor.totalSubdivisions) ]
            , span [ class "header-chip" ]
                [ text ("Octave: " ++ octaveToString cursor.currentOctave) ]
            , span [ class "header-chip" ]
                [ text
                    ("Mode: "
                        ++ (case editMode of
                                SwarEdit ->
                                    "Swar"

                                StrokeEdit ->
                                    "Stroke"
                           )
                    )
                ]
            ]
        ]


octaveToString : Octave -> String
octaveToString octave =
    case octave of
        AtiMandra ->
            "Ati-Mandra"

        Mandra ->
            "Mandra"

        Madhya ->
            "Madhya"

        Taar ->
            "Taar"

        AtiTaar ->
            "Ati-Taar"
