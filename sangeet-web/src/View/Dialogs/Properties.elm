module View.Dialogs.Properties exposing (view)

import Html exposing (Html, button, div, h2, input, label, option, select, text)
import Html.Attributes exposing (class, for, id, placeholder, selected, type_, value)
import Html.Events exposing (onClick, onInput)
import Model.Taal exposing (Taal)
import State.Model exposing (PropsDialogForm, SectionStartingBeatEntry)
import State.Msg exposing (Msg(..))
import UiStrings


{-| Modal dialog for editing composition properties (title, taal, starting beats).
-}
view : PropsDialogForm -> List ( String, Taal ) -> Html Msg
view form taals =
    let
        matras =
            taals
                |> List.filter (\( name, _ ) -> name == form.taalName)
                |> List.head
                |> Maybe.map (\( _, t ) -> t.matras)
                |> Maybe.withDefault 16
    in
    div [ class "modal-overlay" ]
        [ div [ class "modal-dialog modal-properties" ]
            [ h2 [ class "modal-title" ] [ text UiStrings.dialogPropertiesTitle ]
            , div [ class "modal-body" ]
                ([ div [ class "form-group" ]
                    [ label [ for "props-title" ] [ text UiStrings.dialogPropertiesFieldTitleLabel ]
                    , input
                        [ type_ "text"
                        , id "props-title"
                        , class "form-input"
                        , placeholder UiStrings.dialogPropertiesFieldTitlePlaceholder
                        , value form.title
                        , onInput PropsDialogSetTitle
                        ]
                        []
                    ]
                 , div [ class "form-group" ]
                    [ label [ for "props-taal" ] [ text UiStrings.dialogPropertiesFieldTaalLabel ]
                    , select
                        [ id "props-taal"
                        , class "form-select"
                        , onInput PropsDialogSetTaal
                        ]
                        (List.map
                            (\( name, taal ) ->
                                option
                                    [ value name
                                    , selected (form.taalName == name)
                                    ]
                                    [ text (taal.name ++ " (" ++ String.fromInt taal.matras ++ ")") ]
                            )
                            taals
                        )
                    ]
                 ]
                    ++ List.map (viewStartingBeatEntry matras) form.sectionStartingBeats
                )
            , div [ class "modal-footer" ]
                [ button [ class "btn btn-secondary", onClick PropsDialogCancel ]
                    [ text UiStrings.dialogPropertiesButtonCancel ]
                , button [ class "btn btn-primary", onClick PropsDialogSubmit ]
                    [ text UiStrings.dialogPropertiesButtonSave ]
                ]
            ]
        ]


viewStartingBeatEntry : Int -> SectionStartingBeatEntry -> Html Msg
viewStartingBeatEntry matras entry =
    let
        fieldId =
            "props-starting-beat-" ++ String.fromInt entry.sectionIndex

        fieldLabel =
            UiStrings.dialogPropertiesFieldSectionStartingBeatLabel entry.name matras
    in
    div [ class "form-group" ]
        [ label [ for fieldId ] [ text fieldLabel ]
        , input
            [ type_ "number"
            , id fieldId
            , class "form-input"
            , value (String.fromInt entry.startingBeat)
            , onInput (PropsDialogSetStartingBeat entry.sectionIndex)
            , Html.Attributes.min "1"
            , Html.Attributes.max (String.fromInt matras)
            ]
            []
        ]
