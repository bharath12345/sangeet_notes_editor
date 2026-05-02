module View.StatusBar exposing (view)

import Html exposing (Html, div, li, text, ul)
import Html.Attributes exposing (class, id)
import State.Msg exposing (Msg)


{-| Render a scrollable status log panel at the bottom of the editor.
Shows the most recent log messages first.
-}
view : List String -> Html Msg
view statusLog =
    div [ class "status-bar", id "status-bar" ]
        [ ul [ class "status-log" ]
            (List.map
                (\entry ->
                    li [ class "status-entry" ] [ text entry ]
                )
                statusLog
            )
        ]
