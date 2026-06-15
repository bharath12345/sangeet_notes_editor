module State.Update.Helpers exposing
    ( addLog
    , findByName
    , handleApiResult
    , httpErrorToString
    , logToConsole
    , requestLayout
    , scriptName
    , updateComposition
    , updateCursorInPlace
    )

{-| Shared helpers used by every State.Update submodule. Kept tiny and
import-only so each handler module pulls in just the pieces it needs.
-}

import Api.Client exposing (ApiResult(..))
import Api.Layout as ApiLayout
import Http
import Model.Composition exposing (Composition)
import Model.Cursor exposing (CursorModel)
import Model.Types exposing (SwarScript(..))
import Ports
import State.Model as Model exposing (Model)
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import UiStrings


{-| Request layout computation from the server.
-}
requestLayout : Model -> Cmd Msg
requestLayout model =
    let
        comp =
            Model.composition model
    in
    ApiLayout.computeLayout model.apiBaseUrl comp Model.defaultLayoutConfig GotLayoutResult


{-| Add a log entry to the status log (newest first), capped at 100 entries.
-}
addLog : String -> Model -> Model
addLog message model =
    { model | statusLog = List.take 100 (message :: model.statusLog) }


{-| Emit a developer-facing diagnostic to the browser console via the
`Ports.consoleError` port. Use this for details that used to be discarded
(decoder errors, HTTP error bodies) so the status-log entry stays
user-readable but the full context is available in DevTools.

The call returns a `Cmd` so you can compose it with `Cmd.batch` alongside
the user-facing log update.

-}
logToConsole : String -> Cmd msg
logToConsole message =
    Ports.consoleError message


findByName : String -> List ( String, a ) -> Maybe a
findByName name pairs =
    pairs
        |> List.filter (\( n, _ ) -> String.toLower n == String.toLower name)
        |> List.head
        |> Maybe.map Tuple.second


scriptName : SwarScript -> String
scriptName script =
    case script of
        Devanagari ->
            "Devanagari"

        Kannada ->
            "Kannada"

        Telugu ->
            "Telugu"

        English ->
            "English"


httpErrorToString : Http.Error -> String
httpErrorToString error =
    case error of
        Http.BadUrl url ->
            UiStrings.statusBadUrl |> String.replace "{url}" url

        Http.Timeout ->
            UiStrings.statusRequestTimeout

        Http.NetworkError ->
            UiStrings.statusNetworkError

        Http.BadStatus code ->
            UiStrings.statusBadStatus |> String.replace "{code}" (String.fromInt code)

        Http.BadBody msg ->
            UiStrings.statusBadBody |> String.replace "{error}" msg


{-| Generic API result handler that extracts Success, logs ApiFailure/HttpError.

In addition to the user-facing status-log entry, ApiFailure and HttpError
branches emit a `console.error` so developers investigating a bug have
the full error string (and underlying http error code) available in
DevTools, not just the truncated status-bar line the user sees.

-}
handleApiResult :
    Result Http.Error (ApiResult a)
    -> (a -> ( Model, Cmd Msg ))
    -> Model
    -> ( Model, Cmd Msg )
handleApiResult result onSuccess model =
    case result of
        Ok (Success data) ->
            onSuccess data

        Ok (ApiFailure apiError) ->
            ( { model | pendingApiCall = False }
                |> addLog (UiStrings.statusApiError |> String.replace "{message}" apiError.message)
            , logToConsole ("API error: code=" ++ apiError.code ++ " message=" ++ apiError.message)
            )

        Ok (HttpError httpErr) ->
            ( { model | pendingApiCall = False }
                |> addLog (UiStrings.statusHttpError |> String.replace "{message}" (httpErrorToString httpErr))
            , logToConsole ("HTTP error (envelope): " ++ httpErrorToString httpErr)
            )

        Err httpError ->
            ( { model | pendingApiCall = False }
                |> addLog (UiStrings.statusHttpError |> String.replace "{message}" (httpErrorToString httpError))
            , logToConsole ("HTTP error: " ++ httpErrorToString httpError)
            )


updateComposition : Composition -> Model -> Model
updateComposition comp model =
    let
        currentSnapshot =
            UndoHistory.present model.history

        snapshot =
            { currentSnapshot | composition = comp }
    in
    { model
        | history = UndoHistory.push snapshot model.history
        , pendingApiCall = False
    }


updateCursorInPlace : CursorModel -> Model -> Model
updateCursorInPlace newCursor model =
    let
        currentSnapshot =
            UndoHistory.present model.history

        snapshot =
            { currentSnapshot | cursor = newCursor }
    in
    { model | history = UndoHistory.push snapshot model.history }
