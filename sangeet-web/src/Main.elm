module Main exposing (main)

import Api.Reference as ApiReference
import Browser
import Browser.Events
import Html exposing (Html)
import Json.Decode as Decode
import Ports
import State.Model as Model exposing (Model)
import State.Msg exposing (Msg(..))
import State.Update exposing (update)
import Time
import View.Layout as Layout



-- FLAGS


type alias Flags =
    { apiBaseUrl : String
    }


flagsDecoder : Decode.Decoder Flags
flagsDecoder =
    Decode.map Flags
        (Decode.field "apiBaseUrl" Decode.string)



-- INIT


init : Decode.Value -> ( Model, Cmd Msg )
init flagsValue =
    let
        apiBaseUrl =
            case Decode.decodeValue flagsDecoder flagsValue of
                Ok flags ->
                    flags.apiBaseUrl

                Err _ ->
                    "http://localhost:8080/api/v1"

        model =
            Model.init apiBaseUrl
    in
    ( model
    , Cmd.batch
        [ ApiReference.fetchTaals apiBaseUrl GotTaals
        , ApiReference.fetchRaags apiBaseUrl GotRaags
        , ApiReference.fetchColors apiBaseUrl GotColors
        , ApiReference.fetchScripts apiBaseUrl GotScripts
        ]
    )



-- VIEW


view : Model -> Html Msg
view model =
    Layout.view model



-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions _ =
    Sub.batch
        [ -- Keyboard events
          Browser.Events.onKeyDown keyDecoder

        -- Cursor blink timer (every 500ms)
        , Time.every 500 CursorBlink

        -- File port subscriptions
        , Ports.fileSelected FileSelected
        , Ports.fileLoaded FileLoaded

        -- Clipboard port subscription
        , Ports.clipboardContent ClipboardContentReceived
        ]


{-| Decode keyboard events into KeyPressed messages.
-}
keyDecoder : Decode.Decoder Msg
keyDecoder =
    Decode.map4 KeyPressed
        (Decode.field "key" Decode.string)
        (Decode.field "shiftKey" Decode.bool)
        (Decode.field "ctrlKey" Decode.bool)
        (Decode.field "altKey" Decode.bool)



-- MAIN


main : Program Decode.Value Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , view = view
        , subscriptions = subscriptions
        }
