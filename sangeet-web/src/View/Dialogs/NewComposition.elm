module View.Dialogs.NewComposition exposing (view)

import Html exposing (Html, button, div, h2, input, label, option, select, text)
import Html.Attributes exposing (checked, class, for, id, placeholder, selected, type_, value)
import Html.Events exposing (onCheck, onClick, onInput)
import Model.Raag exposing (Raag)
import Model.Taal exposing (Taal)
import State.Model exposing (NewDialogForm)
import State.Msg exposing (Msg(..))


{-| Modal dialog for creating a new composition.
-}
view : NewDialogForm -> List ( String, Taal ) -> List ( String, Raag ) -> Html Msg
view form taals raags =
    div [ class "modal-overlay" ]
        [ div [ class "modal-dialog modal-new-composition" ]
            [ h2 [ class "modal-title" ] [ text "New Composition" ]
            , div [ class "modal-body" ]
                [ -- Title
                  div [ class "form-group" ]
                    [ label [ for "new-title" ] [ text "Title" ]
                    , input
                        [ type_ "text"
                        , id "new-title"
                        , class "form-input"
                        , placeholder "Enter composition title"
                        , value form.title
                        , onInput NewDialogSetTitle
                        ]
                        []
                    ]

                -- Composition Type
                , div [ class "form-group" ]
                    [ label [ for "new-type" ] [ text "Type" ]
                    , select
                        [ id "new-type"
                        , class "form-select"
                        , onInput NewDialogSetType
                        ]
                        [ option [ value "gat", selected (form.compositionType == "gat") ]
                            [ text "Gat (Instrumental)" ]
                        , option [ value "bandish", selected (form.compositionType == "bandish") ]
                            [ text "Bandish (Vocal)" ]
                        , option [ value "palta", selected (form.compositionType == "palta") ]
                            [ text "Palta (Practice)" ]
                        , option [ value "sargam", selected (form.compositionType == "sargam") ]
                            [ text "Sargam (Practice)" ]
                        ]
                    ]

                -- Raag
                , div [ class "form-group" ]
                    [ label [ for "new-raag" ] [ text "Raag" ]
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
                    [ label [ for "new-taal" ] [ text "Taal" ]
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
                    [ label [ for "new-laya" ] [ text "Laya" ]
                    , select
                        [ id "new-laya"
                        , class "form-select"
                        , onInput NewDialogSetLaya
                        ]
                        [ option [ value "ativilambit", selected (form.layaName == "ativilambit") ]
                            [ text "Ati-vilambit" ]
                        , option [ value "vilambit", selected (form.layaName == "vilambit") ]
                            [ text "Vilambit" ]
                        , option [ value "madhya", selected (form.layaName == "madhya") ]
                            [ text "Madhya" ]
                        , option [ value "drut", selected (form.layaName == "drut") ]
                            [ text "Drut" ]
                        , option [ value "atidrut", selected (form.layaName == "atidrut") ]
                            [ text "Ati-drut" ]
                        , option [ value "none", selected (form.layaName == "none") ]
                            [ text "None (Palta)" ]
                        ]
                    ]

                -- Taan Count
                , div [ class "form-group" ]
                    [ label [ for "new-taan-count" ] [ text "Taan Count" ]
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
                    , label [ for "new-show-strokes" ] [ text "Show Stroke Line (Da/Ra)" ]
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
                    , label [ for "new-show-sahitya" ] [ text "Show Sahitya Line (Lyrics)" ]
                    ]
                ]
            , div [ class "modal-footer" ]
                [ button [ class "btn btn-secondary", onClick NewDialogCancel ]
                    [ text "Cancel" ]
                , button [ class "btn btn-primary", onClick NewDialogSubmit ]
                    [ text "Create" ]
                ]
            ]
        ]
