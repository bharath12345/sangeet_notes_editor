module Main exposing (main)

import Browser
import Html exposing (Html, div, h1, p, text)
import Html.Attributes exposing (class)
import Json.Decode as Decode


-- FLAGS


type alias Flags =
    { apiBaseUrl : String
    }


flagsDecoder : Decode.Decoder Flags
flagsDecoder =
    Decode.map Flags
        (Decode.field "apiBaseUrl" Decode.string)


-- MODEL


type alias Model =
    { apiBaseUrl : String
    }


init : Decode.Value -> ( Model, Cmd Msg )
init flagsValue =
    case Decode.decodeValue flagsDecoder flagsValue of
        Ok flags ->
            ( { apiBaseUrl = flags.apiBaseUrl }
            , Cmd.none
            )

        Err _ ->
            ( { apiBaseUrl = "http://localhost:8080/api/v1" }
            , Cmd.none
            )


-- MSG


type Msg
    = NoOp


-- UPDATE


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        NoOp ->
            ( model, Cmd.none )


-- VIEW


view : Model -> Html Msg
view model =
    div [ class "app" ]
        [ h1 [] [ text "Sangeet Notes Editor" ]
        , p [] [ text ("API: " ++ model.apiBaseUrl) ]
        ]


-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions _ =
    Sub.none


-- MAIN


main : Program Decode.Value Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , view = view
        , subscriptions = subscriptions
        }
