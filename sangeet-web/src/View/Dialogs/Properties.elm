module View.Dialogs.Properties exposing (view)

import Html exposing (Html, button, div, h2, input, label, option, select, text)
import Html.Attributes exposing (class, for, id, placeholder, selected, type_, value)
import Html.Events exposing (onClick, onInput)
import Model.Taal exposing (Taal)
import State.Model exposing (PropsDialogForm)
import State.Msg exposing (Msg(..))


{-| Modal dialog for editing composition properties (title and taal).
-}
view : PropsDialogForm -> List ( String, Taal ) -> Html Msg
view form taals =
    div [ class "modal-overlay" ]
        [ div [ class "modal-dialog modal-properties" ]
            [ h2 [ class "modal-title" ] [ text "Composition Properties" ]
            , div [ class "modal-body" ]
                [ -- Title
                  div [ class "form-group" ]
                    [ label [ for "props-title" ] [ text "Title" ]
                    , input
                        [ type_ "text"
                        , id "props-title"
                        , class "form-input"
                        , placeholder "Composition title"
                        , value form.title
                        , onInput PropsDialogSetTitle
                        ]
                        []
                    ]

                -- Taal
                , div [ class "form-group" ]
                    [ label [ for "props-taal" ] [ text "Taal" ]
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
            , div [ class "modal-footer" ]
                [ button [ class "btn btn-secondary", onClick PropsDialogCancel ]
                    [ text "Cancel" ]
                , button [ class "btn btn-primary", onClick PropsDialogSubmit ]
                    [ text "Save" ]
                ]
            ]
        ]
