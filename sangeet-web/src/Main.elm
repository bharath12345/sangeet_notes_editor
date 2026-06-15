module Main exposing (main)

import Api.GoogleDrive
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
-- `initialTheme` is read by `public/index.html` from `localStorage` (key
-- `sangeet:theme`) before mount, with a fallback to the OS-level
-- `prefers-color-scheme` media query. Threading it through a flag (rather
-- than fetching from JS via a port after mount) avoids a Light → Dark
-- flash on reload for users on the dark palette.


type alias Flags =
    { apiBaseUrl : String
    , debugUrl : Maybe String
    , initialTheme : Maybe String
    }


flagsDecoder : Decode.Decoder Flags
flagsDecoder =
    Decode.map3 Flags
        (Decode.field "apiBaseUrl" Decode.string)
        (Decode.maybe (Decode.field "debugUrl" Decode.string))
        (Decode.maybe (Decode.field "initialTheme" Decode.string))



-- INIT


init : Decode.Value -> ( Model, Cmd Msg )
init flagsValue =
    let
        ( apiBaseUrl, maybeDebugUrl, initialTheme ) =
            case Decode.decodeValue flagsDecoder flagsValue of
                Ok flags ->
                    ( flags.apiBaseUrl
                    , flags.debugUrl
                    , flags.initialTheme |> Maybe.map Model.parseTheme |> Maybe.withDefault Model.Light
                    )

                Err _ ->
                    ( "http://localhost:28080/api/v1", Nothing, Model.Light )

        model =
            Model.init apiBaseUrl initialTheme

        debugCmd =
            case maybeDebugUrl of
                Just url ->
                    Ports.requestDebugConnection url

                Nothing ->
                    Cmd.none
    in
    ( model
    , Cmd.batch
        [ ApiReference.fetchTaals apiBaseUrl GotTaals
        , ApiReference.fetchRaags apiBaseUrl GotRaags
        , ApiReference.fetchColors apiBaseUrl GotColors
        , ApiReference.fetchScripts apiBaseUrl GotScripts
        , Ports.loadConfig ()
        , debugCmd

        -- Apply the initial theme to <body data-theme> + persist back to
        -- localStorage so the value seen by Elm and the DOM stay in sync.
        -- (index.html only writes the data-theme attr to the inline
        -- <body> tag from JS if a saved value exists; the OS-preference
        -- fallback path is implicit and never echoed back.)
        , Ports.setTheme (Model.themeName initialTheme)
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

        -- Autosave tick (PR-C C.2): 500ms cadence, matching desktop's
        -- debounce. Handler is a no-op poll unless the active tab is
        -- dirty AND has a known filePath.
        , Time.every 500 AutosaveTick

        -- File port subscriptions
        , Ports.fileSelected FileSelected
        , Ports.fileLoaded FileLoaded

        -- Clipboard port subscription
        , Ports.clipboardContent ClipboardContentReceived

        -- Config persistence
        , Ports.configLoaded GotConfigLoaded

        -- Bug report result (Phase 4b)
        , Ports.bugReportResult (\r -> BugReportResult r.success r.message)

        -- Uncaught JS error capture (Plan 18 PR-3c) — JS-side
        -- window.onerror + unhandledrejection forward here so the
        -- handler can auto-POST a bug-report with source="uncaught".
        , Ports.uncaughtError UncaughtErrorReceived

        -- Google Drive
        , Api.GoogleDrive.googleDriveAuthResult GotDriveAuthResult
        , Api.GoogleDrive.googleDriveDirListing GotDriveDirListing
        , Api.GoogleDrive.googleDriveFileContent GotDriveFileContent
        , Api.GoogleDrive.googleDriveWriteResult GotDriveWriteResult
        , Api.GoogleDrive.googleDriveError GotDriveError

        -- Debug bridge (WS only)
        , Ports.debugCommandReceived DebugCommandReceived
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
