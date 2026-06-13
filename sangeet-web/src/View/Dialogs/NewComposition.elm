module View.Dialogs.NewComposition exposing (view)

import Html exposing (Html, button, div, h2, input, label, option, select, text)
import Html.Attributes exposing (checked, class, for, id, placeholder, selected, type_, value)
import Html.Events exposing (onCheck, onClick, onInput)
import Model.Raag exposing (Raag)
import Model.Taal exposing (Taal)
import State.Model exposing (NewDialogForm)
import State.Msg exposing (Msg(..))
import UiStrings


{-| Modal dialog for creating a new composition.
-}
view : NewDialogForm -> List ( String, Taal ) -> List ( String, Raag ) -> Html Msg
view form taals raags =
    div [ class "modal-overlay" ]
        [ div [ class "modal-dialog modal-new-composition" ]
            [ h2 [ class "modal-title" ] [ text UiStrings.dialogNewCompositionTitle ]
            , div [ class "modal-body" ]
                [ -- Title
                  div [ class "form-group" ]
                    [ label [ for "new-title" ] [ text UiStrings.dialogNewCompositionFieldTitleLabel ]
                    , input
                        [ type_ "text"
                        , id "new-title"
                        , class "form-input"
                        , placeholder UiStrings.dialogNewCompositionFieldTitlePlaceholder
                        , value form.title
                        , onInput NewDialogSetTitle
                        ]
                        []
                    ]

                -- Composition Type
                , div [ class "form-group" ]
                    [ label [ for "new-type" ] [ text UiStrings.dialogNewCompositionFieldTypeLabel ]
                    , select
                        [ id "new-type"
                        , class "form-select"
                        , onInput NewDialogSetType
                        ]
                        [ option [ value "gat", selected (form.compositionType == "gat") ]
                            [ text UiStrings.dialogNewCompositionFieldTypeGat ]
                        , option [ value "bandish", selected (form.compositionType == "bandish") ]
                            [ text UiStrings.dialogNewCompositionFieldTypeBandish ]
                        , option [ value "palta", selected (form.compositionType == "palta") ]
                            [ text UiStrings.dialogNewCompositionFieldTypePalta ]
                        , option [ value "sargam", selected (form.compositionType == "sargam") ]
                            [ text UiStrings.dialogNewCompositionFieldTypeSargam ]
                        ]
                    ]

                -- Raag
                , div [ class "form-group" ]
                    [ label [ for "new-raag" ] [ text UiStrings.dialogNewCompositionFieldRaagLabel ]
                    , select
                        [ id "new-raag"
                        , class "form-select"
                        , onInput NewDialogSetRaag
                        ]
                        (List.map
                            (\( name, raag ) ->
                                option
                                    [ value name
                                    , selected (form.raagName == name)
                                    ]
                                    [ text raag.name ]
                            )
                            raags
                        )
                    ]

                -- Taal
                , div [ class "form-group" ]
                    [ label [ for "new-taal" ] [ text UiStrings.dialogNewCompositionFieldTaalLabel ]
                    , select
                        [ id "new-taal"
                        , class "form-select"
                        , onInput NewDialogSetTaal
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

                -- Laya
                , div [ class "form-group" ]
                    [ label [ for "new-laya" ] [ text UiStrings.dialogNewCompositionFieldLayaLabel ]
                    , select
                        [ id "new-laya"
                        , class "form-select"
                        , onInput NewDialogSetLaya
                        ]
                        [ option [ value "ativilambit", selected (form.layaName == "ativilambit") ]
                            [ text UiStrings.dialogNewCompositionFieldLayaAtivilambit ]
                        , option [ value "vilambit", selected (form.layaName == "vilambit") ]
                            [ text UiStrings.dialogNewCompositionFieldLayaVilambit ]
                        , option [ value "madhya", selected (form.layaName == "madhya") ]
                            [ text UiStrings.dialogNewCompositionFieldLayaMadhya ]
                        , option [ value "drut", selected (form.layaName == "drut") ]
                            [ text UiStrings.dialogNewCompositionFieldLayaDrut ]
                        , option [ value "atidrut", selected (form.layaName == "atidrut") ]
                            [ text UiStrings.dialogNewCompositionFieldLayaAtidrut ]
                        , option [ value "none", selected (form.layaName == "none") ]
                            [ text UiStrings.dialogNewCompositionFieldLayaNone ]
                        ]
                    ]

                -- Starting Beats (Gat/Bandish only)
                , if form.compositionType == "gat" || form.compositionType == "bandish" then
                    let
                        matras =
                            taals
                                |> List.filter (\( name, _ ) -> name == form.taalName)
                                |> List.head
                                |> Maybe.map (\( _, t ) -> t.matras)
                                |> Maybe.withDefault 16

                        mainLabel =
                            if form.compositionType == "bandish" then
                                UiStrings.dialogNewCompositionFieldSthayiStartingBeatLabel matras

                            else
                                UiStrings.dialogNewCompositionFieldGatStartingBeatLabel matras
                    in
                    div []
                        [ div [ class "form-group" ]
                            [ label [ for "new-gat-starting-beat" ] [ text mainLabel ]
                            , input
                                [ type_ "number"
                                , id "new-gat-starting-beat"
                                , class "form-input"
                                , value (String.fromInt form.gatStartingBeat)
                                , onInput NewDialogSetGatStartingBeat
                                , Html.Attributes.min "1"
                                , Html.Attributes.max (String.fromInt matras)
                                ]
                                []
                            ]
                        , div [ class "form-group" ]
                            [ label [ for "new-antara-starting-beat" ]
                                [ text (UiStrings.dialogNewCompositionFieldAntaraStartingBeatLabel matras) ]
                            , input
                                [ type_ "number"
                                , id "new-antara-starting-beat"
                                , class "form-input"
                                , value (String.fromInt form.antaraStartingBeat)
                                , onInput NewDialogSetAntaraStartingBeat
                                , Html.Attributes.min "1"
                                , Html.Attributes.max (String.fromInt matras)
                                ]
                                []
                            ]
                        , if form.compositionType == "gat" then
                            div [ class "form-group" ]
                                [ label [ for "new-taan-starting-beat" ]
                                    [ text (UiStrings.dialogNewCompositionFieldTaanStartingBeatLabel matras) ]
                                , input
                                    [ type_ "number"
                                    , id "new-taan-starting-beat"
                                    , class "form-input"
                                    , value (String.fromInt form.taanStartingBeat)
                                    , onInput NewDialogSetTaanStartingBeat
                                    , Html.Attributes.min "1"
                                    , Html.Attributes.max (String.fromInt matras)
                                    ]
                                    []
                                ]

                          else
                            text ""
                        ]

                  else
                    text ""

                -- Taan Count
                , div [ class "form-group" ]
                    [ label [ for "new-taan-count" ] [ text UiStrings.dialogNewCompositionFieldTaanCountLabel ]
                    , input
                        [ type_ "number"
                        , id "new-taan-count"
                        , class "form-input"
                        , value (String.fromInt form.taanCount)
                        , onInput NewDialogSetTaanCount
                        , Html.Attributes.min "0"
                        , Html.Attributes.max "20"
                        ]
                        []
                    ]

                -- Show Strokes
                , div [ class "form-group form-group-checkbox" ]
                    [ input
                        [ type_ "checkbox"
                        , id "new-show-strokes"
                        , checked form.showStrokes
                        , onCheck NewDialogSetShowStrokes
                        ]
                        []
                    , label [ for "new-show-strokes" ] [ text UiStrings.dialogNewCompositionFieldShowStrokesLabel ]
                    ]

                -- Show Sahitya
                , div [ class "form-group form-group-checkbox" ]
                    [ input
                        [ type_ "checkbox"
                        , id "new-show-sahitya"
                        , checked form.showSahitya
                        , onCheck NewDialogSetShowSahitya
                        ]
                        []
                    , label [ for "new-show-sahitya" ] [ text UiStrings.dialogNewCompositionFieldShowSahityaLabel ]
                    ]
                ]
            , div [ class "modal-footer" ]
                [ button [ class "btn btn-secondary", onClick NewDialogCancel ]
                    [ text UiStrings.dialogNewCompositionButtonCancel ]
                , button [ class "btn btn-primary", onClick NewDialogSubmit ]
                    [ text UiStrings.dialogNewCompositionButtonCreate ]
                ]
            ]
        ]
