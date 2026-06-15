module State.Update.Net exposing
    ( handleBugReportResult
    , handleColors
    , handleConfigLoaded
    , handleCursorApiResult
    , handleDebugCommandReceived
    , handleDebugDumpReceived
    , handleDebugEditorResultReceived
    , handleDebugExportReceived
    , handleDebugResetReceived
    , handleEditorApiResult
    , handleLayoutApiResult
    , handleRaags
    , handleScripts
    , handleStartingBeatResult
    , handleTaalChangeResult
    , handleTaals
    , saveConfigCmd
    )

{-| HTTP/debug-bridge handlers: API success/failure responses for editor,
cursor, layout, reference data, plus the WS debug bridge plumbing and the
bug-report result wiring. The shared response-decoding helpers
(handleApiResult, httpErrorToString) live in State.Update.Helpers.
-}

import Api.Client exposing (ApiResult(..))
import Api.Editor as ApiEditor
import Api.Reference exposing (NotationColors, ScriptInfo)
import Debug.Interpreter
import Http
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Composition exposing (Composition)
import Model.Cursor exposing (CursorModel)
import Model.Layout exposing (EditorResult, SectionGrid)
import Model.Raag exposing (Raag)
import Model.Taal exposing (Taal)
import Model.Types exposing (Octave(..))
import Ports
import State.Model as Model
    exposing
        ( EditMode(..)
        , Model
        , OrnamentMode(..)
        )
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import State.Update.Helpers as Helpers
import UiStrings



-- REFERENCE DATA RESPONSES


handleTaals : Result Http.Error (ApiResult (List ( String, Taal ))) -> Model -> ( Model, Cmd Msg )
handleTaals result model =
    Helpers.handleApiResult result
        (\taals ->
            ( { model | availableTaals = taals }
                |> Helpers.addLog (UiStrings.statusLoadedTaals |> String.replace "{count}" (String.fromInt (List.length taals)))
            , Cmd.none
            )
        )
        model


handleRaags : Result Http.Error (ApiResult (List ( String, Raag ))) -> Model -> ( Model, Cmd Msg )
handleRaags result model =
    Helpers.handleApiResult result
        (\raags ->
            ( { model | availableRaags = raags }
                |> Helpers.addLog (UiStrings.statusLoadedRaags |> String.replace "{count}" (String.fromInt (List.length raags)))
            , Cmd.none
            )
        )
        model


handleColors : Result Http.Error (ApiResult NotationColors) -> Model -> ( Model, Cmd Msg )
handleColors result model =
    Helpers.handleApiResult result
        (\colors ->
            ( { model | notationColors = Just colors }
                |> Helpers.addLog UiStrings.statusColorsLoaded
            , Cmd.none
            )
        )
        model


handleScripts : Result Http.Error (ApiResult (List ( String, ScriptInfo ))) -> Model -> ( Model, Cmd Msg )
handleScripts result model =
    Helpers.handleApiResult result
        (\scripts ->
            ( { model | availableScripts = scripts }, Cmd.none )
        )
        model



-- EDITOR / CURSOR / LAYOUT API RESPONSES


handleStartingBeatResult : Result Http.Error (ApiResult Composition) -> Model -> ( Model, Cmd Msg )
handleStartingBeatResult result model =
    Helpers.handleApiResult result
        (\comp ->
            let
                updatedModel =
                    Helpers.updateComposition comp model
            in
            case model.pendingStartingBeatChanges of
                ( sectionIdx, beatVal ) :: rest ->
                    ( { updatedModel | pendingStartingBeatChanges = rest }
                    , ApiEditor.changeStartingBeat
                        model.apiBaseUrl
                        comp
                        sectionIdx
                        beatVal
                        GotStartingBeatResult
                    )

                [] ->
                    let
                        newModel =
                            { updatedModel | pendingStartingBeatChanges = [] }
                                |> Helpers.addLog UiStrings.statusStartingBeatsUpdated
                    in
                    ( newModel, Helpers.requestLayout newModel )
        )
        model


{-| Handle the changeTaal API response. Push the re-mapped composition +
fresh cursor into history, then chain any pending startingBeat changes
(set by PropsDialogSubmit when the user changed both taal and a starting
beat in the same submit). When the chain finishes, request a fresh
layout so the grid reflects the new taal's vibhag structure.
-}
handleTaalChangeResult : Result Http.Error (ApiResult EditorResult) -> Model -> ( Model, Cmd Msg )
handleTaalChangeResult result model =
    Helpers.handleApiResult result
        (\editorResult ->
            let
                snapshot =
                    { composition = editorResult.composition
                    , cursor = editorResult.cursor
                    , sectionIndex = model.currentSectionIndex
                    }

                updatedModel =
                    { model
                        | history = UndoHistory.push snapshot model.history
                        , pendingApiCall = False
                    }
            in
            case model.pendingStartingBeatChanges of
                ( sectionIdx, beatVal ) :: rest ->
                    ( { updatedModel
                        | pendingStartingBeatChanges = rest
                        , pendingApiCall = True
                      }
                    , ApiEditor.changeStartingBeat
                        model.apiBaseUrl
                        editorResult.composition
                        sectionIdx
                        beatVal
                        GotStartingBeatResult
                    )

                [] ->
                    ( updatedModel, Helpers.requestLayout updatedModel )
        )
        model


handleEditorApiResult : Result Http.Error (ApiResult EditorResult) -> Model -> ( Model, Cmd Msg )
handleEditorApiResult result model =
    Helpers.handleApiResult result
        (\editorResult ->
            let
                snapshot =
                    { composition = editorResult.composition
                    , cursor = editorResult.cursor
                    , sectionIndex = model.currentSectionIndex
                    }

                -- Bug 4 fix: refresh groupingState's expected next-cursor
                -- with where the server actually advanced the cursor. The
                -- next keystroke will compare its observed cursor against
                -- this; a mismatch (because the user moved the cursor)
                -- prevents incorrect regrouping at the old beat.
                updatedGrouping =
                    case model.groupingState of
                        Just gs ->
                            Just
                                { gs
                                    | nextBeat = editorResult.cursor.beat
                                    , nextCycle = editorResult.cursor.cycle
                                    , nextSubIndex = editorResult.cursor.subIndex
                                }

                        Nothing ->
                            Nothing

                newModel =
                    { model
                        | history = UndoHistory.push snapshot model.history
                        , pendingApiCall = False
                        , groupingState = updatedGrouping
                    }
                        |> Helpers.addLog editorResult.message
            in
            ( newModel, Helpers.requestLayout newModel )
        )
        model


handleCursorApiResult : Result Http.Error (ApiResult CursorModel) -> Model -> ( Model, Cmd Msg )
handleCursorApiResult result model =
    Helpers.handleApiResult result
        (\newCursor ->
            let
                currentSnapshot =
                    UndoHistory.present model.history

                preservedCursor =
                    { newCursor | selectionAnchor = currentSnapshot.cursor.selectionAnchor }

                snapshot =
                    { currentSnapshot | cursor = preservedCursor }

                newModel =
                    { model
                        | history = UndoHistory.push snapshot model.history
                        , pendingApiCall = False
                    }
            in
            ( newModel, Cmd.none )
        )
        model


handleLayoutApiResult : Result Http.Error (ApiResult (List SectionGrid)) -> Model -> ( Model, Cmd Msg )
handleLayoutApiResult result model =
    -- NOTE: requestLayout does not set pendingApiCall, so we deliberately
    -- leave that flag alone here. Resetting it would race with the debug
    -- bridge's drainPendingDebugAck logic — a layout response that arrives
    -- mid-edit would otherwise fire a queued ack before the underlying
    -- editor API call completed.
    Helpers.handleApiResult result
        (\grids ->
            ( { model | layoutGrids = grids }, Cmd.none )
        )
        model



-- BUG REPORT RESULT


handleBugReportResult : Bool -> String -> Model -> ( Model, Cmd Msg )
handleBugReportResult success message model =
    if success then
        ( { model
            | showBugReportDialog = False
            , bugReportForm = Model.defaultBugReportForm
          }
            |> Helpers.addLog (UiStrings.statusBugReportSent |> String.replace "{message}" message)
        , Cmd.none
        )

    else
        let
            form =
                model.bugReportForm
        in
        ( { model | bugReportForm = { form | sending = False } }
            |> Helpers.addLog (UiStrings.statusBugReportFailed |> String.replace "{message}" message)
        , Cmd.none
        )



-- CONFIG PERSISTENCE


saveConfigCmd : Model -> Cmd Msg
saveConfigCmd model =
    let
        bookmarks =
            model.driveFolders
                |> List.filter .isBookmarked
                |> List.map
                    (\f ->
                        Encode.object
                            [ ( "folderId", Encode.string f.folderId )
                            , ( "name", Encode.string f.name )
                            ]
                    )

        openTabs =
            model.tabs
                |> List.map
                    (\t ->
                        Encode.object
                            [ ( "id", Encode.string t.id )
                            , ( "filename", Encode.string t.filename )
                            ]
                    )

        config =
            Encode.object
                [ ( "bookmarks", Encode.list identity bookmarks )
                , ( "openTabs", Encode.list identity openTabs )
                , ( "activeTabId"
                  , model.activeTabId
                        |> Maybe.map Encode.string
                        |> Maybe.withDefault Encode.null
                  )
                , ( "fileBrowserCollapsed", Encode.bool model.fileBrowserCollapsed )
                , ( "fileBrowserWidth", Encode.float model.fileBrowserWidth )
                ]
    in
    Ports.saveConfig (Encode.encode 0 config)


handleConfigLoaded : String -> Model -> ( Model, Cmd Msg )
handleConfigLoaded configJson model =
    case Decode.decodeString configDecoder configJson of
        Ok config ->
            ( { model
                | fileBrowserCollapsed = config.fileBrowserCollapsed
                , fileBrowserWidth = config.fileBrowserWidth
              }
            , Cmd.none
            )

        Err _ ->
            ( model, Cmd.none )


type alias WebConfig =
    { fileBrowserCollapsed : Bool
    , fileBrowserWidth : Float
    }


configDecoder : Decode.Decoder WebConfig
configDecoder =
    Decode.map2 WebConfig
        (Decode.field "fileBrowserCollapsed" Decode.bool
            |> Decode.maybe
            |> Decode.map (Maybe.withDefault True)
        )
        (Decode.field "fileBrowserWidth" Decode.float
            |> Decode.maybe
            |> Decode.map (Maybe.withDefault 250.0)
        )



-- DEBUG BRIDGE (WS ONLY)


{-| Process an incoming WS debug command. Decodes the JSON via
Debug.Interpreter, applies any pre-dispatch transform (e.g. clearing
groupingState), then dispatches the interpreter's chosen Msg through the
top-level `update` (passed in as `runUpdate`) so the wrapper's
drainPendingDebugAck pass still runs.
-}
handleDebugCommandReceived :
    (Msg -> Model -> ( Model, Cmd Msg ))
    -> Decode.Value
    -> Model
    -> ( Model, Cmd Msg )
handleDebugCommandReceived runUpdate raw model =
    let
        result =
            Debug.Interpreter.interpret raw model

        -- Capture the deferred ack id and run any interpreter-
        -- supplied pre-dispatch model transform (e.g. clearing
        -- groupingState for TypeChar). Both happen BEFORE the
        -- dispatched Msg so the update wrapper's
        -- drainPendingDebugAck doesn't fire the ack prematurely
        -- (deferred acks wait for the API result, not the
        -- synchronous Msg dispatch).
        modelAfterTransform =
            result.preDispatchTransform model

        preDispatchModel =
            case result.deferredAckId of
                Just ackId ->
                    { modelAfterTransform | pendingDebugAck = Just ackId }

                Nothing ->
                    modelAfterTransform

        -- The recursive update call is safe because Debug.Interpreter.interpret never
        -- returns DebugCommandReceived as its Msg — only existing app Msgs. Future
        -- changes that route debug commands BACK to DebugCommandReceived would create
        -- unbounded recursion. We use the wrapping `update` so the
        -- drainPendingDebugAck pass runs — for purely synchronous
        -- dispatched Msgs (e.g. SelectSection), pendingApiCall is
        -- False after dispatch and the ack fires right away.
        ( newModel, msgCmd ) =
            runUpdate result.msg preDispatchModel

        responseCmd =
            case result.immediateResponse of
                Just r ->
                    Ports.debugResponse r

                Nothing ->
                    Cmd.none
    in
    ( newModel, Cmd.batch [ msgCmd, responseCmd, result.extraCmd ] )



-- DEBUG BRIDGE ASYNC HANDLERS
--
-- These mirror GotNewComposition / GotSerializedComposition / GotExportHtml
-- but route the result back to the parity-test WebSocket bridge instead of
-- the production UI side-effects (file download, dialog dismissal). The Got*
-- handlers stay untouched so production paths aren't affected by debug
-- behaviour.


handleDebugResetReceived :
    String
    -> Result Http.Error (ApiResult Composition)
    -> Model
    -> ( Model, Cmd Msg )
handleDebugResetReceived reqId result model =
    case result of
        Ok (Success comp) ->
            let
                firstStartingBeat =
                    comp.sections
                        |> List.head
                        |> Maybe.map .startingBeat
                        |> Maybe.withDefault 1

                newCursor =
                    { taal = comp.metadata.taal
                    , cycle = 0
                    , beat = firstStartingBeat - 1
                    , subIndex = 0
                    , totalSubdivisions = 1
                    , currentOctave = Madhya
                    , selectionAnchor = Nothing
                    }

                snapshot =
                    { composition = comp
                    , cursor = newCursor
                    , sectionIndex = 0
                    }

                newHistory =
                    UndoHistory.init snapshot

                -- Replace the active tab's state in-place (instead of opening
                -- a new tab) so the parity runner's per-test state remains
                -- predictable.
                updatedTabs =
                    List.map
                        (\t ->
                            if Just t.id == model.activeTabId then
                                { t
                                    | history = newHistory
                                    , currentSectionIndex = 0
                                    , editMode = SwarEdit
                                    , ornamentMode = NoOrnament
                                    , groupingState = Nothing
                                    , layoutGrids = []
                                    , filename = comp.metadata.title
                                    , isReadOnly = False
                                    , isDirty = False
                                }

                            else
                                t
                        )
                        model.tabs

                newModel =
                    { model
                        | history = newHistory
                        , currentSectionIndex = 0
                        , editMode = SwarEdit
                        , ornamentMode = NoOrnament
                        , groupingState = Nothing
                        , layoutGrids = []
                        , tabs = updatedTabs
                    }
                        |> Helpers.addLog ("Debug reset: " ++ comp.metadata.title)
            in
            ( newModel
            , Cmd.batch
                [ Helpers.requestLayout newModel
                , Ports.debugResponse
                    { id = reqId, result = Encode.string "OK", error = Nothing }
                ]
            )

        Ok (ApiFailure err) ->
            ( model
            , Ports.debugResponse
                { id = reqId
                , result = Encode.null
                , error = Just ("API error: " ++ err.message)
                }
            )

        Ok (HttpError httpErr) ->
            ( model
            , Ports.debugResponse
                { id = reqId
                , result = Encode.null
                , error = Just ("HTTP error: " ++ Helpers.httpErrorToString httpErr)
                }
            )

        Err httpErr ->
            ( model
            , Ports.debugResponse
                { id = reqId
                , result = Encode.null
                , error = Just ("HTTP error: " ++ Helpers.httpErrorToString httpErr)
                }
            )


handleDebugDumpReceived :
    String
    -> Result Http.Error (ApiResult String)
    -> Model
    -> ( Model, Cmd Msg )
handleDebugDumpReceived reqId result model =
    let
        respond payload =
            Ports.debugResponse payload
    in
    case result of
        Ok (Success swarJson) ->
            ( model
            , respond { id = reqId, result = Encode.string swarJson, error = Nothing }
            )

        Ok (ApiFailure err) ->
            ( model
            , respond
                { id = reqId
                , result = Encode.null
                , error = Just ("API error: " ++ err.message)
                }
            )

        Ok (HttpError httpErr) ->
            ( model
            , respond
                { id = reqId
                , result = Encode.null
                , error = Just ("HTTP error: " ++ Helpers.httpErrorToString httpErr)
                }
            )

        Err httpErr ->
            ( model
            , respond
                { id = reqId
                , result = Encode.null
                , error = Just ("HTTP error: " ++ Helpers.httpErrorToString httpErr)
                }
            )


handleDebugExportReceived :
    String
    -> Result Http.Error (ApiResult String)
    -> Model
    -> ( Model, Cmd Msg )
handleDebugExportReceived reqId result model =
    let
        respond payload =
            Ports.debugResponse payload
    in
    case result of
        Ok (Success htmlString) ->
            ( model
            , respond { id = reqId, result = Encode.string htmlString, error = Nothing }
            )

        Ok (ApiFailure err) ->
            ( model
            , respond
                { id = reqId
                , result = Encode.null
                , error = Just ("API error: " ++ err.message)
                }
            )

        Ok (HttpError httpErr) ->
            ( model
            , respond
                { id = reqId
                , result = Encode.null
                , error = Just ("HTTP error: " ++ Helpers.httpErrorToString httpErr)
                }
            )

        Err httpErr ->
            ( model
            , respond
                { id = reqId
                , result = Encode.null
                , error = Just ("HTTP error: " ++ Helpers.httpErrorToString httpErr)
                }
            )


{-| Shared handler for debug commands whose async response is an EditorResult
(re-mapped composition + fresh cursor). On success: push the snapshot into
history so the next GetState / GetEvents debug call sees the updated state,
then ack the WS request. Used by both SetTaal and Stroke.

This is intentionally separate from `handleEditorApiResult` (the production
path used by user-triggered editing). The production handler calls
`addLog editorResult.message` and `requestLayout`; we skip both — log noise
is bad for parity-test stability, and the next debug command will request
a fresh layout if it needs one.

-}
handleDebugEditorResultReceived :
    String
    -> Result Http.Error (ApiResult EditorResult)
    -> Model
    -> ( Model, Cmd Msg )
handleDebugEditorResultReceived reqId result model =
    let
        respond payload =
            Ports.debugResponse payload
    in
    case result of
        Ok (Success editorResult) ->
            let
                snapshot =
                    { composition = editorResult.composition
                    , cursor = editorResult.cursor
                    , sectionIndex = model.currentSectionIndex
                    }

                newModel =
                    { model | history = UndoHistory.push snapshot model.history }
            in
            ( newModel
            , respond { id = reqId, result = Encode.string "OK", error = Nothing }
            )

        Ok (ApiFailure err) ->
            ( model
            , respond
                { id = reqId
                , result = Encode.null
                , error = Just ("API error: " ++ err.message)
                }
            )

        Ok (HttpError httpErr) ->
            ( model
            , respond
                { id = reqId
                , result = Encode.null
                , error = Just ("HTTP error: " ++ Helpers.httpErrorToString httpErr)
                }
            )

        Err httpErr ->
            ( model
            , respond
                { id = reqId
                , result = Encode.null
                , error = Just ("HTTP error: " ++ Helpers.httpErrorToString httpErr)
                }
            )
