module View.Dialogs.Properties exposing (view)

import Html exposing (Html, button, div, input, label, option, select, text)
import Html.Attributes exposing (class, for, id, placeholder, selected, type_, value)
import Html.Events exposing (onClick, onInput)
import Model.Taal exposing (Taal)
import State.Model exposing (PropsDialogForm, SectionStartingBeatEntry)
import State.Msg exposing (Msg(..))
import UiStrings
import View.Dialogs.Frame as Frame


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

        baseFields =
            [ div [ class "form-group" ]
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
    in
    Frame.view
        { title = UiStrings.dialogPropertiesTitle
        , variantClass = "modal-properties"
        , body = baseFields ++ List.map (viewStartingBeatEntry matras) form.sectionStartingBeats
        , footer =
            [ button [ class "btn btn-secondary", onClick PropsDialogCancel ]
                [ text UiStrings.dialogPropertiesButtonCancel ]
            , button [ class "btn btn-primary", onClick PropsDialogSubmit ]
                [ text UiStrings.dialogPropertiesButtonSave ]
            ]
        }


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
