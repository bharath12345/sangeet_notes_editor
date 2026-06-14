module State.Update exposing (update)

import Api.Client exposing (ApiResult(..))
import Api.Composition as ApiComposition
import Api.Cursor as ApiCursor
import Api.Editor as ApiEditor
import Api.Export as ApiExport
import Api.GoogleDrive
import Api.Layout as ApiLayout
import Api.Ornament as ApiOrnament
import Api.Section as ApiSection
import Api.Stroke as ApiStroke
import Debug.Interpreter
import Http
import Input.KeyHandler as KeyHandler exposing (KeyAction(..))
import Input.OrnamentMode as OrnamentMode exposing (OrnamentAction(..))
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Composition exposing (Composition, CompositionType(..), SectionType(..))
import Model.Cursor exposing (CursorModel)
import Model.Layout exposing (ClipboardResult, EditorResult, SectionGrid)
import Model.Taal exposing (VibhagMarker(..))
import Model.Types
    exposing
        ( Laya(..)
        , MeendDirection(..)
        , Note
        , Octave(..)
        , Stroke(..)
        , SwarScript(..)
        , Variant
        )
import Ports
import State.AppAction as AppAction
import State.Model as Model
    exposing
        ( DriveItem
        , DriveState(..)
        , EditMode(..)
        , FileTab
        , FolderState
        , Model
        , OrnamentMode(..)
        , PendingTabSource(..)
        )
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import Task
import Time
import UiStrings
import Util.TabNameResolver


{-| Grouping threshold in milliseconds — notes typed within this window
on the same beat are grouped onto a single beat with equal subdivisions.
-}
groupingThresholdMs : Int
groupingThresholdMs =
    500


{-| GitHub-hosted user guide entry point. The directory listing renders the
files in order so users land on a browsable index.
-}
userGuideUrl : String
userGuideUrl =
    "https://github.com/bharath12345/sangeet_notes_editor/tree/main/docs/user-guide"


{-| Main update function. Delegates to the case-split inner update, then
checks whether a queued debug ack can now be drained (i.e., pendingApiCall
flipped from True to False as a result of this dispatch). The drain ensures
that responses to debug commands that trigger async API work (e.g., TypeChar
which hits /editor/insert-swar) only fire after the API completes — which is
how the parity-test runner gets reliable sequencing across many TypeChar in a
row.
-}
update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    let
        ( newModel, cmd ) =
            updateInner msg model

        ( drainedModel, drainCmd ) =
            drainPendingDebugAck newModel

        markedModel =
            markActiveTabDirtyIfEdited msg model drainedModel
    in
    ( markedModel, Cmd.batch [ cmd, drainCmd ] )


{-| If `msg` caused the active tab's composition to actually change (compared by
present-snapshot equality), flip its `isDirty` flag true. Msgs that don't
mutate the composition (cursor moves, dialog opens, save, etc.) leave the flag
alone so we don't get false-positive asterisks. Save msgs explicitly clear
the flag elsewhere; tab/switch/etc. msgs are skipped here.
-}
markActiveTabDirtyIfEdited : Msg -> Model -> Model -> Model
markActiveTabDirtyIfEdited msg before after =
    let
        beforeComp =
            UndoHistory.present before.history |> .composition

        afterComp =
            UndoHistory.present after.history |> .composition
    in
    if beforeComp == afterComp then
        after

    else
        case msg of
            -- Save msgs clear dirty in their own handlers; don't re-set it.
            SaveFile ->
                after

            SaveFileAs ->
                after

            -- Loads / undo / redo are not user edits in the dirty sense, but
            -- in practice they should still set dirty=true because the on-disk
            -- file no longer matches in-memory state. Skip undo/redo though:
            -- those navigate within the SAME committed history.
            Undo ->
                after

            Redo ->
                after

            _ ->
                let
                    updatedTabs =
                        after.tabs
                            |> List.map
                                (\t ->
                                    if Just t.id == after.activeTabId then
                                        { t | isDirty = True }

                                    else
                                        t
                                )
                in
                { after | tabs = updatedTabs }


{-| Emit a WS response for the queued debug ack id, if any, provided no API
call is currently in flight. The next debug command will set
pendingDebugAck again, so this is a one-shot drain per update.
-}
drainPendingDebugAck : Model -> ( Model, Cmd Msg )
drainPendingDebugAck model =
    case ( model.pendingDebugAck, model.pendingApiCall ) of
        ( Just ackId, False ) ->
            ( { model | pendingDebugAck = Nothing }
            , Ports.debugResponse
                { id = ackId, result = Encode.string "OK", error = Nothing }
            )

        _ ->
            ( model, Cmd.none )


updateInner : Msg -> Model -> ( Model, Cmd Msg )
updateInner msg model =
    case msg of
        -- Keyboard input
        KeyPressed key shiftKey ctrlKey altKey ->
            handleKeyPress key shiftKey ctrlKey altKey model

        -- Mouse click on canvas
        CanvasClicked cycle beat ->
            let
                cur =
                    Model.cursor model
            in
            ( { model | groupingState = Nothing }
            , ApiCursor.moveTo model.apiBaseUrl cur cycle beat (Model.currentStartingBeat model) GotCursorResult
            )

        -- File operations
        NewComposition ->
            ( { model | showNewDialog = True }, Cmd.none )

        OpenFile ->
            ( model, Ports.selectFile ".swar" )

        SaveFile ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiComposition.serializeComposition model.apiBaseUrl comp GotSerializedComposition
            )

        ExportHtml ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiExport.exportHtml model.apiBaseUrl comp model.currentScript GotExportHtml
            )

        -- Edit operations
        Undo ->
            handleUndo model

        Redo ->
            handleRedo model

        -- Script
        ChangeScript script ->
            ( { model | currentScript = script }
                |> addLog (UiStrings.statusScriptChanged |> String.replace "{scriptName}" (scriptName script))
            , Cmd.none
            )

        -- Section operations
        SelectSection idx ->
            let
                sectionStartBeat =
                    (Model.composition model).sections
                        |> List.drop idx
                        |> List.head
                        |> Maybe.map .startingBeat
                        |> Maybe.withDefault 1

                minBeat =
                    sectionStartBeat - 1

                cur =
                    Model.cursor model

                clampedModel =
                    if cur.cycle == 0 && cur.beat < minBeat then
                        updateCursorInPlace { cur | beat = minBeat, subIndex = 0 } model

                    else
                        model
            in
            ( { clampedModel | currentSectionIndex = idx }
                |> addLog (UiStrings.statusSwitchedToSection |> String.replace "{number}" (String.fromInt (idx + 1)))
            , requestLayout clampedModel
            )

        AddSection name sectionType ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiSection.addSection model.apiBaseUrl comp name sectionType GotSectionAdd
            )

        RemoveSection idx ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiSection.removeSection model.apiBaseUrl comp model.currentSectionIndex idx GotSectionRemove
            )

        RenameSection idx newName ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiSection.renameSection model.apiBaseUrl comp idx newName GotSectionRename
            )

        RequestRenameSection idx currentName ->
            ( model
            , Ports.requestRenameSection
                { sectionIndex = idx, currentName = currentName }
            )

        MoveSectionUp idx ->
            if idx > 0 then
                let
                    comp =
                        Model.composition model
                in
                ( { model | pendingApiCall = True }
                , ApiSection.reorderSections model.apiBaseUrl comp model.currentSectionIndex idx (idx - 1) GotSectionReorder
                )

            else
                ( model, Cmd.none )

        MoveSectionDown idx ->
            let
                comp =
                    Model.composition model

                maxIdx =
                    List.length comp.sections - 1
            in
            if idx < maxIdx then
                ( { model | pendingApiCall = True }
                , ApiSection.reorderSections model.apiBaseUrl comp model.currentSectionIndex idx (idx + 1) GotSectionReorder
                )

            else
                ( model, Cmd.none )

        -- New dialog
        ShowNewDialog ->
            ( { model | showNewDialog = True }, Cmd.none )

        NewDialogSetTitle t ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | title = t } }, Cmd.none )

        NewDialogSetType t ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | compositionType = t } }, Cmd.none )

        NewDialogSetRaag r ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | raagName = r } }, Cmd.none )

        NewDialogSetTaal t ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | taalName = t } }, Cmd.none )

        NewDialogSetLaya l ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | layaName = l } }, Cmd.none )

        NewDialogSetTaanCount s ->
            let
                form =
                    model.newDialogForm

                count =
                    String.toInt s |> Maybe.withDefault 0
            in
            ( { model | newDialogForm = { form | taanCount = count } }, Cmd.none )

        NewDialogSetShowStrokes b ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | showStrokes = b } }, Cmd.none )

        NewDialogSetShowSahitya b ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | showSahitya = b } }, Cmd.none )

        NewDialogSetGatStartingBeat s ->
            let
                form =
                    model.newDialogForm

                beat =
                    String.toInt s |> Maybe.withDefault 1 |> max 1
            in
            ( { model | newDialogForm = { form | gatStartingBeat = beat } }, Cmd.none )

        NewDialogSetAntaraStartingBeat s ->
            let
                form =
                    model.newDialogForm

                beat =
                    String.toInt s |> Maybe.withDefault 1 |> max 1
            in
            ( { model | newDialogForm = { form | antaraStartingBeat = beat } }, Cmd.none )

        NewDialogSetTaanStartingBeat s ->
            let
                form =
                    model.newDialogForm

                beat =
                    String.toInt s |> Maybe.withDefault 1 |> max 1
            in
            ( { model | newDialogForm = { form | taanStartingBeat = beat } }, Cmd.none )

        NewDialogSubmit ->
            handleNewDialogSubmit model

        NewDialogCancel ->
            ( { model | showNewDialog = False }, Cmd.none )

        -- Properties dialog
        ShowPropsDialog ->
            let
                comp =
                    Model.composition model

                isGatOrBandish =
                    comp.metadata.compositionType == Gat || comp.metadata.compositionType == Bandish

                sectionBeats =
                    if not isGatOrBandish then
                        []

                    else
                        let
                            indexed =
                                List.indexedMap Tuple.pair comp.sections

                            mainEntry =
                                indexed
                                    |> List.filter
                                        (\( _, s ) ->
                                            s.sectionType == Sthayi || s.sectionType == CustomSectionType "Gat"
                                        )
                                    |> List.head
                                    |> Maybe.map
                                        (\( i, s ) ->
                                            let
                                                mainLabel =
                                                    if comp.metadata.compositionType == Bandish then
                                                        "Sthayi"

                                                    else
                                                        "Gat"
                                            in
                                            { sectionIndex = i, name = mainLabel, startingBeat = s.startingBeat }
                                        )

                            antaraEntry =
                                indexed
                                    |> List.filter (\( _, s ) -> s.sectionType == Antara)
                                    |> List.head
                                    |> Maybe.map
                                        (\( i, s ) ->
                                            { sectionIndex = i, name = "Antara", startingBeat = s.startingBeat }
                                        )

                            taanEntry =
                                indexed
                                    |> List.filter (\( _, s ) -> s.sectionType == Taan)
                                    |> List.head
                                    |> Maybe.map
                                        (\( i, s ) ->
                                            { sectionIndex = i, name = "Taan", startingBeat = s.startingBeat }
                                        )
                        in
                        List.filterMap identity [ mainEntry, antaraEntry, taanEntry ]

                compTypeStr =
                    case comp.metadata.compositionType of
                        Gat ->
                            "gat"

                        Bandish ->
                            "bandish"

                        _ ->
                            ""
            in
            ( { model
                | showPropsDialog = True
                , propsDialogForm =
                    { title = comp.metadata.title
                    , taalName = String.toLower comp.metadata.taal.name
                    , sectionStartingBeats = sectionBeats
                    , compositionType = compTypeStr
                    }
              }
            , Cmd.none
            )

        PropsDialogSetTitle t ->
            let
                form =
                    model.propsDialogForm
            in
            ( { model | propsDialogForm = { form | title = t } }, Cmd.none )

        PropsDialogSetTaal t ->
            let
                form =
                    model.propsDialogForm
            in
            ( { model | propsDialogForm = { form | taalName = t } }, Cmd.none )

        PropsDialogSetStartingBeat sectionIndex beatStr ->
            let
                form =
                    model.propsDialogForm

                beat =
                    String.toInt beatStr |> Maybe.withDefault 1 |> max 1

                updatedBeats =
                    List.map
                        (\entry ->
                            if entry.sectionIndex == sectionIndex then
                                { entry | startingBeat = beat }

                            else
                                entry
                        )
                        form.sectionStartingBeats
            in
            ( { model | propsDialogForm = { form | sectionStartingBeats = updatedBeats } }, Cmd.none )

        PropsDialogSubmit ->
            let
                form =
                    model.propsDialogForm

                maybeTaal =
                    findByName form.taalName model.availableTaals
            in
            case maybeTaal of
                Just newTaal ->
                    let
                        comp =
                            Model.composition model

                        meta =
                            comp.metadata

                        -- Always apply title change locally; the taal change
                        -- goes through the server below if needed.
                        compWithTitle =
                            { comp | metadata = { meta | title = form.title } }

                        taalChanged =
                            comp.metadata.taal.name /= newTaal.name

                        changedBeats =
                            form.sectionStartingBeats
                                |> List.filterMap
                                    (\entry ->
                                        let
                                            currentBeat =
                                                compWithTitle.sections
                                                    |> List.drop entry.sectionIndex
                                                    |> List.head
                                                    |> Maybe.map .startingBeat
                                                    |> Maybe.withDefault 1
                                        in
                                        if entry.startingBeat /= currentBeat then
                                            Just ( entry.sectionIndex, entry.startingBeat )

                                        else
                                            Nothing
                                    )
                    in
                    if taalChanged then
                        -- Server endpoint re-maps event positions so events
                        -- past the new taal's matras flow into subsequent
                        -- cycles. The response carries the re-mapped
                        -- composition + a fresh cursor; the result handler
                        -- pushes the snapshot and continues with any
                        -- pending starting-beat changes.
                        ( { model
                            | showPropsDialog = False
                            , pendingApiCall = True
                            , pendingStartingBeatChanges = changedBeats
                          }
                            |> addLog (UiStrings.statusPropertiesUpdatedTaal |> String.replace "{taalName}" newTaal.name)
                        , ApiEditor.changeTaal
                            model.apiBaseUrl
                            compWithTitle
                            model.currentSectionIndex
                            newTaal
                            GotTaalChangeResult
                        )

                    else
                        -- No taal change: keep the original local-snapshot
                        -- path. Title still flows through `compWithTitle`.
                        let
                            cur =
                                Model.cursor model

                            newSectionStartBeat =
                                let
                                    formBeat =
                                        form.sectionStartingBeats
                                            |> List.filter (\e -> e.sectionIndex == model.currentSectionIndex)
                                            |> List.head
                                            |> Maybe.map .startingBeat
                                in
                                case formBeat of
                                    Just b ->
                                        b

                                    Nothing ->
                                        compWithTitle.sections
                                            |> List.drop model.currentSectionIndex
                                            |> List.head
                                            |> Maybe.map .startingBeat
                                            |> Maybe.withDefault 1

                            newCursor =
                                { cur | taal = newTaal, cycle = 0, beat = newSectionStartBeat - 1, subIndex = 0, totalSubdivisions = 1 }

                            snapshot =
                                { composition = compWithTitle
                                , cursor = newCursor
                                , sectionIndex = model.currentSectionIndex
                                }

                            baseModel =
                                { model
                                    | history = UndoHistory.push snapshot model.history
                                    , showPropsDialog = False
                                }
                                    |> addLog (UiStrings.statusPropertiesUpdatedTaal |> String.replace "{taalName}" newTaal.name)
                        in
                        case changedBeats of
                            ( sectionIdx, beatVal ) :: rest ->
                                ( { baseModel
                                    | pendingStartingBeatChanges = rest
                                    , pendingApiCall = True
                                  }
                                , ApiEditor.changeStartingBeat
                                    model.apiBaseUrl
                                    compWithTitle
                                    sectionIdx
                                    beatVal
                                    GotStartingBeatResult
                                )

                            [] ->
                                ( baseModel, requestLayout baseModel )

                Nothing ->
                    ( { model | showPropsDialog = False }
                        |> addLog UiStrings.statusPropertiesUpdatedTaalNotFound
                    , Cmd.none
                    )

        PropsDialogCancel ->
            ( { model | showPropsDialog = False }, Cmd.none )

        -- About dialog
        ShowAboutDialog ->
            ( { model | showAboutDialog = True }, Cmd.none )

        CloseAboutDialog ->
            ( { model | showAboutDialog = False }, Cmd.none )

        -- Support dialog (donations) — split out from About for parity with desktop's SupportDialog
        ShowSupportDialog ->
            ( { model | showSupportDialog = True }, Cmd.none )

        CloseSupportDialog ->
            ( { model | showSupportDialog = False }, Cmd.none )

        -- Keyboard cheat sheet
        ShowKeyboardCheatSheet ->
            ( { model | showKeyboardCheatSheet = True }, Cmd.none )

        CloseKeyboardCheatSheet ->
            ( { model | showKeyboardCheatSheet = False }, Cmd.none )

        -- User guide (external link)
        OpenUserGuide ->
            ( model, Ports.openExternalUrl userGuideUrl )

        -- Command palette
        ShowCommandPalette ->
            ( { model | showCommandPalette = True, paletteQuery = "", paletteSelectedIndex = 0 }, Cmd.none )

        CloseCommandPalette ->
            ( { model | showCommandPalette = False }, Cmd.none )

        PaletteQueryChanged q ->
            ( { model | paletteQuery = q, paletteSelectedIndex = 0 }, Cmd.none )

        PaletteSelectIndex i ->
            let
                results =
                    AppAction.filter model.paletteQuery AppAction.all

                clamped =
                    max 0 (min i (List.length results - 1))
            in
            ( { model | paletteSelectedIndex = clamped }, Cmd.none )

        PaletteRunSelected ->
            runPaletteAction model.paletteSelectedIndex model

        PaletteRunIndex i ->
            runPaletteAction i model

        -- Bug report dialog
        ShowBugReportDialog ->
            ( { model
                | showBugReportDialog = True
                , bugReportForm = Model.defaultBugReportForm
              }
            , Cmd.none
            )

        BugReportSetDescription d ->
            let
                form =
                    model.bugReportForm
            in
            ( { model | bugReportForm = { form | description = d } }, Cmd.none )

        BugReportSetEmail e ->
            let
                form =
                    model.bugReportForm
            in
            ( { model | bugReportForm = { form | email = e } }, Cmd.none )

        BugReportSubmit ->
            let
                form =
                    model.bugReportForm
            in
            ( { model | bugReportForm = { form | sending = True } }
            , Ports.submitBugReport
                { description = String.trim form.description
                , email = String.trim form.email
                , apiBaseUrl = model.apiBaseUrl
                }
            )

        BugReportCancel ->
            ( { model
                | showBugReportDialog = False
                , bugReportForm = Model.defaultBugReportForm
              }
            , Cmd.none
            )

        BugReportResult success message ->
            if success then
                ( { model
                    | showBugReportDialog = False
                    , bugReportForm = Model.defaultBugReportForm
                  }
                    |> addLog (UiStrings.statusBugReportSent |> String.replace "{message}" message)
                , Cmd.none
                )

            else
                let
                    form =
                        model.bugReportForm
                in
                ( { model | bugReportForm = { form | sending = False } }
                    |> addLog (UiStrings.statusBugReportFailed |> String.replace "{message}" message)
                , Cmd.none
                )

        -- API Responses
        GotStartingBeatResult result ->
            handleStartingBeatResult result model

        GotTaalChangeResult result ->
            handleTaalChangeResult result model

        GotEditorResult result ->
            handleEditorApiResult result model

        GotCursorResult result ->
            handleCursorApiResult result model

        GotLayoutResult result ->
            handleLayoutApiResult result model

        GotTaals result ->
            handleApiResult result
                (\taals ->
                    ( { model | availableTaals = taals }
                        |> addLog (UiStrings.statusLoadedTaals |> String.replace "{count}" (String.fromInt (List.length taals)))
                    , Cmd.none
                    )
                )
                model

        GotRaags result ->
            handleApiResult result
                (\raags ->
                    ( { model | availableRaags = raags }
                        |> addLog (UiStrings.statusLoadedRaags |> String.replace "{count}" (String.fromInt (List.length raags)))
                    , Cmd.none
                    )
                )
                model

        GotColors result ->
            handleApiResult result
                (\colors ->
                    ( { model | notationColors = Just colors }
                        |> addLog UiStrings.statusColorsLoaded
                    , Cmd.none
                    )
                )
                model

        GotScripts result ->
            handleApiResult result
                (\scripts ->
                    ( { model | availableScripts = scripts }, Cmd.none )
                )
                model

        GotNewComposition result ->
            handleApiResult result
                (\comp ->
                    let
                        firstStartingBeat =
                            comp.sections
                                |> List.head
                                |> Maybe.map .startingBeat
                                |> Maybe.withDefault 1

                        defaultCursor =
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
                            , cursor = defaultCursor
                            , sectionIndex = 0
                            }

                        newHistory =
                            UndoHistory.init snapshot

                        tabId =
                            "tab-" ++ String.fromInt model.nextTabId

                        newTab =
                            { id = tabId
                            , filename = comp.metadata.title
                            , filePath = Nothing
                            , isReadOnly = False
                            , history = newHistory
                            , currentSectionIndex = 0
                            , editMode = SwarEdit
                            , ornamentMode = NoOrnament
                            , groupingState = Nothing
                            , layoutGrids = []
                            , isDirty = False
                            }

                        savedModel =
                            Model.saveActiveTabState model

                        existingTitles =
                            savedModel.tabs |> List.map .filename
                    in
                    if List.member comp.metadata.title existingTitles then
                        let
                            conflicting =
                                savedModel.tabs
                                    |> List.filter (\t -> t.filename == comp.metadata.title)
                                    |> List.head
                                    |> Maybe.map .id
                                    |> Maybe.withDefault ""

                            proposed =
                                Util.TabNameResolver.nextAvailableTitle comp.metadata.title existingTitles

                            pending =
                                { composition = comp
                                , source = PendingFromNewComposition
                                , proposedTitle = proposed
                                , conflictingTabId = conflicting
                                }
                        in
                        ( { savedModel
                            | showNewDialog = False
                            , pendingApiCall = False
                            , pendingTabOpen = Just pending
                            , showDuplicateTabDialog = True
                          }
                        , Cmd.none
                        )

                    else
                        let
                            newModel =
                                { savedModel
                                    | history = newHistory
                                    , currentSectionIndex = 0
                                    , editMode = SwarEdit
                                    , ornamentMode = NoOrnament
                                    , groupingState = Nothing
                                    , layoutGrids = []
                                    , showNewDialog = False
                                    , pendingApiCall = False
                                    , tabs = savedModel.tabs ++ [ newTab ]
                                    , activeTabId = Just tabId
                                    , nextTabId = model.nextTabId + 1
                                }
                                    |> addLog (UiStrings.statusCreated |> String.replace "{title}" comp.metadata.title)
                        in
                        ( newModel, requestLayout newModel )
                )
                model

        GotSectionAdd result ->
            handleApiResult result
                (\comp ->
                    let
                        newModel =
                            updateComposition comp model
                                |> addLog UiStrings.statusSectionAdded
                    in
                    ( newModel, requestLayout newModel )
                )
                model

        GotSectionRemove result ->
            handleApiResult result
                (\r ->
                    let
                        newModel =
                            updateComposition r.composition model
                                |> (\m -> { m | currentSectionIndex = r.currentSectionIndex })
                                |> addLog UiStrings.statusSectionRemoved
                    in
                    ( newModel, requestLayout newModel )
                )
                model

        GotSectionRename result ->
            handleApiResult result
                (\comp ->
                    let
                        newModel =
                            updateComposition comp model
                                |> addLog UiStrings.statusSectionRenamed
                    in
                    ( newModel, requestLayout newModel )
                )
                model

        GotSectionReorder result ->
            handleApiResult result
                (\r ->
                    let
                        newModel =
                            updateComposition r.composition model
                                |> (\m -> { m | currentSectionIndex = r.currentSectionIndex })
                                |> addLog UiStrings.statusSectionsReordered
                    in
                    ( newModel, requestLayout newModel )
                )
                model

        GotExportHtml result ->
            handleApiResult result
                (\htmlString ->
                    let
                        comp =
                            Model.composition model

                        filename =
                            comp.metadata.title ++ ".html"
                    in
                    ( { model | pendingApiCall = False }
                        |> addLog UiStrings.statusExportingHtml
                    , Ports.downloadFile
                        { filename = filename
                        , mimeType = "text/html"
                        , content = htmlString
                        , forcePicker = True
                        }
                    )
                )
                model

        GotSerializedComposition result ->
            handleApiResult result
                (\swarString ->
                    let
                        comp =
                            Model.composition model

                        filename =
                            comp.metadata.title ++ ".swar"

                        clearedTabs =
                            model.tabs
                                |> List.map
                                    (\t ->
                                        if Just t.id == model.activeTabId then
                                            { t | isDirty = False }

                                        else
                                            t
                                    )
                    in
                    ( { model | pendingApiCall = False, tabs = clearedTabs, pendingSaveAs = False }
                        |> addLog UiStrings.statusSavingComposition
                    , Ports.downloadFile
                        { filename = filename
                        , mimeType = "application/json"
                        , content = swarString
                        , forcePicker = model.pendingSaveAs
                        }
                    )
                )
                model

        GotParsedComposition result ->
            handleApiResult result
                (\comp ->
                    let
                        firstStartingBeat =
                            comp.sections
                                |> List.head
                                |> Maybe.map .startingBeat
                                |> Maybe.withDefault 1

                        defaultCursor =
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
                            , cursor = defaultCursor
                            , sectionIndex = 0
                            }

                        newHistory =
                            UndoHistory.init snapshot

                        tabId =
                            "tab-" ++ String.fromInt model.nextTabId

                        newTab =
                            { id = tabId
                            , filename = comp.metadata.title
                            , filePath = Nothing
                            , isReadOnly = False
                            , history = newHistory
                            , currentSectionIndex = 0
                            , editMode = SwarEdit
                            , ornamentMode = NoOrnament
                            , groupingState = Nothing
                            , layoutGrids = []
                            , isDirty = False
                            }

                        savedModel =
                            Model.saveActiveTabState model

                        existingTitles =
                            savedModel.tabs |> List.map .filename
                    in
                    if List.member comp.metadata.title existingTitles then
                        let
                            conflicting =
                                savedModel.tabs
                                    |> List.filter (\t -> t.filename == comp.metadata.title)
                                    |> List.head
                                    |> Maybe.map .id
                                    |> Maybe.withDefault ""

                            proposed =
                                Util.TabNameResolver.nextAvailableTitle comp.metadata.title existingTitles

                            pending =
                                { composition = comp
                                , source = PendingFromOpenedFile
                                , proposedTitle = proposed
                                , conflictingTabId = conflicting
                                }
                        in
                        ( { savedModel
                            | pendingApiCall = False
                            , pendingTabOpen = Just pending
                            , showDuplicateTabDialog = True
                          }
                        , Cmd.none
                        )

                    else
                        let
                            newModel =
                                { savedModel
                                    | history = newHistory
                                    , currentSectionIndex = 0
                                    , editMode = SwarEdit
                                    , ornamentMode = NoOrnament
                                    , groupingState = Nothing
                                    , layoutGrids = []
                                    , pendingApiCall = False
                                    , tabs = savedModel.tabs ++ [ newTab ]
                                    , activeTabId = Just tabId
                                    , nextTabId = model.nextTabId + 1
                                }
                                    |> addLog (UiStrings.statusOpened |> String.replace "{title}" comp.metadata.title)
                        in
                        ( newModel, requestLayout newModel )
                )
                model

        -- Clipboard operations
        GotClipboardResult result ->
            handleClipboardApiResult result model

        ClipboardContentReceived jsonString ->
            handlePasteFromClipboard jsonString model

        -- File port responses
        FileSelected filename ->
            ( addLog (UiStrings.statusFileSelected |> String.replace "{filename}" filename) model, Cmd.none )

        FileLoaded content ->
            ( { model | pendingApiCall = True }
            , ApiComposition.parseComposition model.apiBaseUrl content GotParsedComposition
            )

        -- Swar key timing for grouping detection
        GotSwarKeyTime posix note variant _ ->
            handleSwarKeyTimed posix note variant model

        -- Tab management
        SwitchTab tabId ->
            handleSwitchTab tabId model

        CloseTab tabId ->
            handleCloseTab tabId model

        NewTab ->
            handleNewTab model

        -- File browser
        ToggleFileBrowser ->
            ( { model | fileBrowserCollapsed = not model.fileBrowserCollapsed }, Cmd.none )

        -- Google Drive
        ConnectDrive ->
            handleConnectDrive model

        GotDriveAuthResult value ->
            handleDriveAuthResult value model

        GotDriveDirListing value ->
            handleDriveDirListing value model

        GotDriveFileContent value ->
            handleDriveFileContent value model

        GotDriveWriteResult _ ->
            ( addLog UiStrings.statusFileSavedToDrive model, Cmd.none )

        GotDriveError errMsg ->
            ( addLog (UiStrings.statusDriveError |> String.replace "{message}" errMsg) model, Cmd.none )

        DriveOpenFolder folderId ->
            handleDriveOpenFolder folderId model

        DriveOpenFile fileId fileName ->
            handleDriveOpenFile fileId fileName model

        DriveToggleBookmark folderId ->
            handleDriveToggleBookmark folderId model

        DriveRefreshFolder folderId ->
            ( model, Api.GoogleDrive.listDir folderId )

        DriveCreateFile parentId ->
            handleDriveCreateFile parentId model

        DriveCreateFolder parentId ->
            handleDriveCreateFolder parentId model

        DriveRenameItem fileId newName ->
            ( model, Api.GoogleDrive.renameItem { fileId = fileId, newName = newName } )

        DriveDeleteItem parentFolderId fileId ->
            ( model
            , Cmd.batch
                [ Api.GoogleDrive.deleteItem fileId
                , Api.GoogleDrive.listDir parentFolderId
                ]
            )

        -- Config persistence
        SaveConfig ->
            ( model, saveConfigCmd model )

        GotConfigLoaded configJson ->
            handleConfigLoaded configJson model

        -- Debug bridge (WS only)
        DebugCommandReceived raw ->
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
                    update result.msg preDispatchModel

                responseCmd =
                    case result.immediateResponse of
                        Just r ->
                            Ports.debugResponse r

                        Nothing ->
                            Cmd.none
            in
            ( newModel, Cmd.batch [ msgCmd, responseCmd, result.extraCmd ] )

        DebugResetReceived reqId result ->
            handleDebugResetReceived reqId result model

        DebugDumpReceived reqId result ->
            handleDebugDumpReceived reqId result model

        DebugExportReceived reqId result ->
            handleDebugExportReceived reqId result model

        -- Save As
        SaveFileAs ->
            handleSaveFileAs model

        -- Duplicate-tab dialog resolution
        DuplicateTabSwitch ->
            handleDuplicateTabSwitch model

        DuplicateTabOpenWithNewName ->
            handleDuplicateTabRename model

        DuplicateTabCancel ->
            ( { model | pendingTabOpen = Nothing, showDuplicateTabDialog = False }, Cmd.none )

        -- Unsaved-changes dialog
        UnsavedChangesCancel ->
            ( { model | showUnsavedChangesDialog = Nothing }, Cmd.none )

        UnsavedChangesDiscard ->
            handleUnsavedChangesDiscard model

        UnsavedChangesSave ->
            handleUnsavedChangesSave model

        -- Autosave tick
        AutosaveTick _ ->
            handleAutosaveTick model

        -- No-op
        NoOp ->
            ( model, Cmd.none )



-- TAB MANAGEMENT


handleSwitchTab : String -> Model -> ( Model, Cmd Msg )
handleSwitchTab tabId model =
    if model.activeTabId == Just tabId then
        ( model, Cmd.none )

    else
        let
            savedModel =
                Model.saveActiveTabState model

            maybeTab =
                savedModel.tabs
                    |> List.filter (\t -> t.id == tabId)
                    |> List.head
        in
        case maybeTab of
            Just tab ->
                let
                    newModel =
                        Model.loadTabState tab savedModel
                            |> addLog (UiStrings.statusSwitchedToTab |> String.replace "{filename}" tab.filename)
                in
                ( newModel, requestLayout newModel )

            Nothing ->
                ( model, Cmd.none )


handleCloseTab : String -> Model -> ( Model, Cmd Msg )
handleCloseTab tabId model =
    -- If the target tab has unsaved changes, surface the 3-button confirmation
    -- modal first; the user's choice (Cancel / Discard / Save) consumes the
    -- pending close via handleUnsavedChangesDiscard / handleUnsavedChangesSave.
    let
        savedModel =
            Model.saveActiveTabState model

        targetTab =
            savedModel.tabs |> List.filter (\t -> t.id == tabId) |> List.head

        isDirty =
            targetTab |> Maybe.map .isDirty |> Maybe.withDefault False
    in
    if isDirty then
        ( { savedModel | showUnsavedChangesDialog = Just tabId }, Cmd.none )

    else
        doCloseTabImmediate tabId savedModel


doCloseTabImmediate : String -> Model -> ( Model, Cmd Msg )
doCloseTabImmediate tabId model =
    let
        remainingTabs =
            List.filter (\t -> t.id /= tabId) model.tabs
    in
    if List.isEmpty remainingTabs then
        let
            newModel =
                handleNewTabHelper { model | tabs = [] }
                    |> addLog UiStrings.statusLastTabClosedNewCreated
        in
        ( newModel, requestLayout newModel )

    else if model.activeTabId == Just tabId then
        let
            nextTab =
                List.head remainingTabs
        in
        case nextTab of
            Just tab ->
                let
                    newModel =
                        Model.loadTabState tab { model | tabs = remainingTabs }
                            |> addLog (UiStrings.statusClosedTabSwitched |> String.replace "{filename}" tab.filename)
                in
                ( newModel, requestLayout newModel )

            Nothing ->
                ( { model | tabs = remainingTabs }, Cmd.none )

    else
        ( { model | tabs = remainingTabs }
            |> addLog UiStrings.statusTabClosed
        , Cmd.none
        )


handleNewTab : Model -> ( Model, Cmd Msg )
handleNewTab model =
    let
        newModel =
            handleNewTabHelper model
                |> addLog UiStrings.statusNewTab
    in
    ( newModel, requestLayout newModel )


handleNewTabHelper : Model -> Model
handleNewTabHelper model =
    let
        savedModel =
            Model.saveActiveTabState model

        tabId =
            "tab-" ++ String.fromInt savedModel.nextTabId

        defaultTaal =
            { name = "Teentaal"
            , matras = 16
            , vibhags =
                [ { beats = 4, marker = Sam }
                , { beats = 4, marker = TaaliMarker 2 }
                , { beats = 4, marker = KhaliMarker }
                , { beats = 4, marker = TaaliMarker 3 }
                ]
            , theka = Nothing
            }

        defaultRaag =
            { name = "Yaman"
            , thaat = Just "Kalyan"
            , arohana = Nothing
            , avarohana = Nothing
            , vadi = Nothing
            , samvadi = Nothing
            , pakad = Nothing
            , prahar = Nothing
            }

        defaultComposition =
            { metadata =
                { title = "Untitled"
                , compositionType = Gat
                , raag = defaultRaag
                , taal = defaultTaal
                , laya = Nothing
                , instrument = Nothing
                , composer = Nothing
                , author = Nothing
                , source = Nothing
                , showStrokeLine = True
                , showSahityaLine = False
                , createdAt = ""
                , updatedAt = ""
                }
            , sections =
                [ { name = "Sthayi"
                  , sectionType = Sthayi
                  , events = []
                  , tihai = Nothing
                  , startingBeat = 1
                  }
                ]
            }

        defaultCursor =
            { taal = defaultTaal
            , cycle = 0
            , beat = 0
            , subIndex = 0
            , totalSubdivisions = 1
            , currentOctave = Madhya
            , selectionAnchor = Nothing
            }

        snapshot =
            { composition = defaultComposition
            , cursor = defaultCursor
            , sectionIndex = 0
            }

        newHistory =
            UndoHistory.init snapshot

        newTab =
            { id = tabId
            , filename = "Untitled"
            , filePath = Nothing
            , isReadOnly = False
            , history = newHistory
            , currentSectionIndex = 0
            , editMode = SwarEdit
            , ornamentMode = NoOrnament
            , groupingState = Nothing
            , layoutGrids = []
            , isDirty = False
            }
    in
    { savedModel
        | history = newHistory
        , currentSectionIndex = 0
        , editMode = SwarEdit
        , ornamentMode = NoOrnament
        , groupingState = Nothing
        , layoutGrids = []
        , tabs = savedModel.tabs ++ [ newTab ]
        , activeTabId = Just tabId
        , nextTabId = savedModel.nextTabId + 1
    }



-- KEYBOARD HANDLING


handleKeyPress : String -> Bool -> Bool -> Bool -> Model -> ( Model, Cmd Msg )
handleKeyPress key shiftKey ctrlKey altKey model =
    -- Palette has its own key handling: ↑/↓ navigate, Enter runs, Esc closes.
    -- Any other key (including text input into the search field) falls through
    -- so the input element processes it naturally.
    if model.showCommandPalette then
        case key of
            "Escape" ->
                update CloseCommandPalette model

            "ArrowDown" ->
                update (PaletteSelectIndex (model.paletteSelectedIndex + 1)) model

            "ArrowUp" ->
                update (PaletteSelectIndex (model.paletteSelectedIndex - 1)) model

            "Enter" ->
                update PaletteRunSelected model

            _ ->
                ( model, Cmd.none )
        -- Ctrl/Cmd+K opens the palette. Alt+K is taken by the kanSwar ornament.

    else
        let
            anyDialogOpen =
                model.showNewDialog
                    || model.showPropsDialog
                    || model.showAboutDialog
                    || model.showBugReportDialog
                    || model.showKeyboardCheatSheet
                    || model.showSupportDialog
                    || model.showDuplicateTabDialog
                    || model.showUnsavedChangesDialog
                    /= Nothing
        in
        -- Ctrl/Cmd+K opens the palette. Alt+K is taken by the kanSwar ornament.
        if ctrlKey && not altKey && not shiftKey && key == "k" && not anyDialogOpen then
            update ShowCommandPalette model
            -- Bare `?` opens the cheat sheet — guarded so it doesn't fire while the
            -- user is typing into a dialog text field or in ornament mode.

        else if key == "?" && not ctrlKey && not altKey && not anyDialogOpen && model.ornamentMode == NoOrnament then
            update ShowKeyboardCheatSheet model

        else
            let
                action =
                    KeyHandler.mapKeyToAction key shiftKey ctrlKey altKey
            in
            case model.ornamentMode of
                NoOrnament ->
                    handleKeyAction action key model

                _ ->
                    handleOrnamentInput action model


{-| Look up the AppAction at the given filtered-list index and dispatch its Msg by
recursively calling update. Closes the palette regardless of whether the index was
valid (no-op if the index falls outside the filtered list).
-}
runPaletteAction : Int -> Model -> ( Model, Cmd Msg )
runPaletteAction i model =
    let
        results =
            AppAction.filter model.paletteQuery AppAction.all

        closed =
            { model | showCommandPalette = False }
    in
    case List.head (List.drop i results) of
        Just action ->
            update action.msg closed

        Nothing ->
            ( closed, Cmd.none )


handleKeyAction : KeyAction -> String -> Model -> ( Model, Cmd Msg )
handleKeyAction action key model =
    let
        -- Clear grouping state for any action other than SwarInput in SwarEdit mode
        m =
            case ( action, model.editMode ) of
                ( SwarInput _ _, SwarEdit ) ->
                    model

                _ ->
                    { model | groupingState = Nothing }
    in
    case action of
        SwarInput note variant ->
            case m.editMode of
                StrokeEdit ->
                    case String.toLower key of
                        "d" ->
                            handleStroke Da m

                        "r" ->
                            handleStroke Ra m

                        "j" ->
                            handleStroke Jod m

                        _ ->
                            ( m, Cmd.none )

                SwarEdit ->
                    handleSwarKey note variant key m

        InsertRest ->
            let
                comp =
                    Model.composition m

                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiEditor.insertRest m.apiBaseUrl comp m.currentSectionIndex cur GotEditorResult
            )

        InsertSustain ->
            let
                comp =
                    Model.composition m

                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiEditor.insertSustain m.apiBaseUrl comp m.currentSectionIndex cur GotEditorResult
            )

        InsertChikari ->
            let
                comp =
                    Model.composition m

                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiEditor.insertChikari m.apiBaseUrl comp m.currentSectionIndex cur GotEditorResult
            )

        DeleteLast ->
            let
                comp =
                    Model.composition m

                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiEditor.deleteAtCursor m.apiBaseUrl comp m.currentSectionIndex cur GotEditorResult
            )

        NavRight ->
            let
                cur =
                    Model.cursor m

                cleared =
                    { cur | selectionAnchor = Nothing }

                -- Match desktop's clamp: NavRight is a no-op once the
                -- cursor is already at the "one cycle past the last
                -- event" position. Otherwise the cursor advances into a
                -- cycle that has no rendered cells and visually
                -- disappears (plan-16 B.5a). Server-side nextBeat would
                -- still happily advance, so we clamp here before firing.
                maxAllowedCycle =
                    Model.currentSectionMaxCycle m + 1

                taal =
                    cleared.taal

                wouldOverflowCycle =
                    cleared.beat + 1 >= taal.matras
            in
            if wouldOverflowCycle && cleared.cycle >= maxAllowedCycle then
                ( m, Cmd.none )

            else
                ( updateCursorInPlace cleared m
                , ApiCursor.nextBeat m.apiBaseUrl cleared (Model.currentStartingBeat m) GotCursorResult
                )

        NavLeft ->
            let
                cur =
                    Model.cursor m

                cleared =
                    { cur | selectionAnchor = Nothing }
            in
            ( updateCursorInPlace cleared m
            , ApiCursor.prevBeat m.apiBaseUrl cleared (Model.currentStartingBeat m) GotCursorResult
            )

        NavNextSubBeat ->
            let
                cur =
                    Model.cursor m
            in
            ( m
            , ApiCursor.nextSubBeat m.apiBaseUrl cur (Model.currentStartingBeat m) GotCursorResult
            )

        UndoAction ->
            handleUndo m

        RedoAction ->
            handleRedo m

        Subdivision n ->
            let
                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiCursor.setSubdivisions m.apiBaseUrl cur n GotCursorResult
            )

        OctaveMandra ->
            let
                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiCursor.setOctave m.apiBaseUrl cur Mandra GotCursorResult
            )

        OctaveMadhya ->
            let
                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiCursor.setOctave m.apiBaseUrl cur Madhya GotCursorResult
            )

        OctaveTaar ->
            let
                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiCursor.setOctave m.apiBaseUrl cur Taar GotCursorResult
            )

        StrokeDa ->
            handleStroke Da m

        StrokeRa ->
            handleStroke Ra m

        StrokeJod ->
            handleStroke Jod m

        StrokeClear ->
            let
                comp =
                    Model.composition m

                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiStroke.clearStroke m.apiBaseUrl comp m.currentSectionIndex cur GotEditorResult
            )

        -- Ornament shortcuts: enter ornament mode
        OrnamentGamak ->
            applySimpleOrnament "gamak" m

        OrnamentAndolan ->
            applySimpleOrnament "andolan" m

        OrnamentGitkari ->
            applySimpleOrnament "gitkari" m

        OrnamentKanSwar ->
            ( { m | ornamentMode = SingleNoteMode "kanSwar" }
                |> addLog UiStrings.statusOrnamentKanSwar
            , Cmd.none
            )

        OrnamentSparsh ->
            ( { m | ornamentMode = SingleNoteMode "sparsh" }
                |> addLog UiStrings.statusOrnamentSparsh
            , Cmd.none
            )

        OrnamentGhaseet ->
            ( { m | ornamentMode = SingleNoteMode "ghaseet" }
                |> addLog UiStrings.statusOrnamentGhaseet
            , Cmd.none
            )

        OrnamentMeendAsc ->
            ( { m | ornamentMode = MeendStartMode Ascending }
                |> addLog UiStrings.statusOrnamentMeendAsc
            , Cmd.none
            )

        OrnamentMeendDesc ->
            ( { m | ornamentMode = MeendStartMode Descending }
                |> addLog UiStrings.statusOrnamentMeendDesc
            , Cmd.none
            )

        OrnamentKrintan ->
            ( { m | ornamentMode = KrintanStartMode }
                |> addLog UiStrings.statusOrnamentKrintan
            , Cmd.none
            )

        OrnamentMurki ->
            ( { m | ornamentMode = MurkiCollectMode [] }
                |> addLog UiStrings.statusOrnamentMurki
            , Cmd.none
            )

        OrnamentZamzama ->
            ( { m | ornamentMode = ZamzamaCollectMode [] }
                |> addLog UiStrings.statusOrnamentZamzama
            , Cmd.none
            )

        OrnamentCancel ->
            ( { m | ornamentMode = NoOrnament }
                |> addLog UiStrings.statusOrnamentCancelled
            , Cmd.none
            )

        FinishOrnament ->
            ( m, Cmd.none )

        SelectRight ->
            handleSelectRight m

        SelectLeft ->
            handleSelectLeft m

        CopySelection ->
            handleCopySelection m

        CutSelection ->
            handleCutSelection m

        PasteClipboard ->
            ( m, Cmd.none )

        ToggleEditMode ->
            let
                newMode =
                    case m.editMode of
                        SwarEdit ->
                            StrokeEdit

                        StrokeEdit ->
                            SwarEdit
            in
            ( { m | editMode = newMode }
                |> addLog
                    ("Edit mode: "
                        ++ (case newMode of
                                SwarEdit ->
                                    "Swar"

                                StrokeEdit ->
                                    "Stroke"
                           )
                    )
            , Cmd.none
            )

        NoAction ->
            case m.editMode of
                StrokeEdit ->
                    case String.toLower key of
                        "j" ->
                            handleStroke Jod m

                        "x" ->
                            let
                                comp =
                                    Model.composition m

                                cur =
                                    Model.cursor m
                            in
                            ( { m | pendingApiCall = True }
                            , ApiStroke.clearStroke m.apiBaseUrl comp m.currentSectionIndex cur GotEditorResult
                            )

                        _ ->
                            ( m, Cmd.none )

                SwarEdit ->
                    ( m, Cmd.none )


{-| Defer swar insertion until we have a timestamp for grouping detection.

We optimistically set pendingApiCall = True so the debug-bridge ack drain
waits until GotSwarKeyTime fires (and that handler in turn sets the flag
when it dispatches the actual API call). Without this, a fast-fire sequence
of debug TypeChar commands would resolve their acks before the Time.now
Task completes, losing the per-command sequencing the parity runner relies
on. Production keyboard handling sees no functional change — pendingApiCall
is only read by UI affordances that already gate on it being True for
brief windows after each key press.

-}
handleSwarKey : Note -> Variant -> String -> Model -> ( Model, Cmd Msg )
handleSwarKey note variant key model =
    ( { model | pendingApiCall = True }
    , Task.perform (\posix -> GotSwarKeyTime posix note variant key) Time.now
    )


{-| Handle swar input with timestamp — implements fast-typing grouping.
Notes typed within groupingThresholdMs on the same beat are accumulated
into a single beat via undo-and-replay with insertSwarGroup.
-}
handleSwarKeyTimed : Time.Posix -> Note -> Variant -> Model -> ( Model, Cmd Msg )
handleSwarKeyTimed posix note variant model =
    let
        now =
            Time.posixToMillis posix

        cur =
            Model.cursor model

        octave =
            cur.currentOctave
    in
    case model.groupingState of
        Just gs ->
            if now - gs.startTime < groupingThresholdMs && List.length gs.notes < 4 then
                case UndoHistory.undo model.history of
                    Just undoneHistory ->
                        let
                            thisNote =
                                { note = note, variant = variant, octave = octave }

                            undoneSnapshot =
                                UndoHistory.present undoneHistory

                            allNotes =
                                gs.notes ++ [ thisNote ]
                        in
                        ( { model
                            | history = undoneHistory
                            , pendingApiCall = True
                            , groupingState = Just { gs | notes = allNotes }
                          }
                        , ApiEditor.insertSwarGroup
                            model.apiBaseUrl
                            undoneSnapshot.composition
                            undoneSnapshot.sectionIndex
                            undoneSnapshot.cursor
                            allNotes
                            GotEditorResult
                        )

                    Nothing ->
                        startNewGroup model note variant octave now

            else
                startNewGroup model note variant octave now

        Nothing ->
            startNewGroup model note variant octave now


startNewGroup : Model -> Note -> Variant -> Octave -> Int -> ( Model, Cmd Msg )
startNewGroup model note variant octave now =
    let
        comp =
            Model.composition model

        cur =
            Model.cursor model

        thisNote =
            { note = note, variant = variant, octave = octave }
    in
    ( { model
        | pendingApiCall = True
        , groupingState =
            Just
                { notes = [ thisNote ]
                , startTime = now
                , beat = cur.beat
                , cycle = cur.cycle
                }
      }
    , ApiEditor.insertSwar model.apiBaseUrl comp model.currentSectionIndex cur note variant octave GotEditorResult
    )


{-| Handle ornament input when in an ornament mode.
-}
handleOrnamentInput : KeyAction -> Model -> ( Model, Cmd Msg )
handleOrnamentInput action model =
    case action of
        OrnamentCancel ->
            ( { model | ornamentMode = NoOrnament }
                |> addLog UiStrings.statusOrnamentCancelled
            , Cmd.none
            )

        _ ->
            let
                maybeNoteRef =
                    case action of
                        SwarInput note variant ->
                            Just
                                { note = note
                                , variant = variant
                                , octave = (Model.cursor model).currentOctave
                                }

                        _ ->
                            Nothing

                isEnter =
                    case action of
                        FinishOrnament ->
                            True

                        _ ->
                            False

                -- Check if the raw action was triggered by Enter
                -- We handle this via the OrnamentCancel action check below
                ornamentAction =
                    OrnamentMode.transition model.ornamentMode maybeNoteRef isEnter
            in
            applyOrnamentAction ornamentAction model


applyOrnamentAction : OrnamentAction -> Model -> ( Model, Cmd Msg )
applyOrnamentAction action model =
    let
        comp =
            Model.composition model

        cur =
            Model.cursor model

        secIdx =
            model.currentSectionIndex
    in
    case action of
        ApplySimple ornamentType ->
            ( { model | ornamentMode = NoOrnament, pendingApiCall = True }
            , ApiOrnament.addSimple model.apiBaseUrl comp secIdx cur ornamentType GotEditorResult
            )

        ApplySingleNote ornamentType noteRef ->
            ( { model | ornamentMode = NoOrnament, pendingApiCall = True }
            , ApiOrnament.addSingleNote model.apiBaseUrl comp secIdx cur ornamentType noteRef GotEditorResult
            )

        ApplyMeend startNote endNote direction ->
            ( { model | ornamentMode = NoOrnament, pendingApiCall = True }
            , ApiOrnament.addMeend model.apiBaseUrl
                comp
                secIdx
                cur
                { startNote = startNote
                , endNote = endNote
                , direction = direction
                , intermediateNotes = []
                }
                GotEditorResult
            )

        ApplyKrintan notes ->
            ( { model | ornamentMode = NoOrnament, pendingApiCall = True }
            , ApiOrnament.addKrintan model.apiBaseUrl comp secIdx cur notes GotEditorResult
            )

        ApplyMurki notes ->
            ( { model | ornamentMode = NoOrnament, pendingApiCall = True }
            , ApiOrnament.addMurki model.apiBaseUrl comp secIdx cur notes GotEditorResult
            )

        ApplyZamzama notes ->
            ( { model | ornamentMode = NoOrnament, pendingApiCall = True }
            , ApiOrnament.addZamzama model.apiBaseUrl comp secIdx cur notes GotEditorResult
            )

        StillCollecting newMode ->
            ( { model | ornamentMode = newMode }
                |> addLog UiStrings.statusOrnamentCollecting
            , Cmd.none
            )

        Cancelled ->
            ( { model | ornamentMode = NoOrnament }
                |> addLog UiStrings.statusOrnamentCancelled
            , Cmd.none
            )


applySimpleOrnament : String -> Model -> ( Model, Cmd Msg )
applySimpleOrnament ornamentType model =
    let
        comp =
            Model.composition model

        cur =
            Model.cursor model
    in
    ( { model | pendingApiCall = True }
    , ApiOrnament.addSimple model.apiBaseUrl comp model.currentSectionIndex cur ornamentType GotEditorResult
    )


handleStroke : Stroke -> Model -> ( Model, Cmd Msg )
handleStroke stroke model =
    let
        comp =
            Model.composition model

        cur =
            Model.cursor model
    in
    ( { model | pendingApiCall = True }
    , ApiStroke.setStroke model.apiBaseUrl comp model.currentSectionIndex cur stroke GotEditorResult
    )



-- SELECTION / CLIPBOARD


handleSelectRight : Model -> ( Model, Cmd Msg )
handleSelectRight model =
    let
        cur =
            Model.cursor model

        anchor =
            case cur.selectionAnchor of
                Just _ ->
                    cur.selectionAnchor

                Nothing ->
                    Just { cycle = cur.cycle, beat = cur.beat, subdivision = { numerator = 0, denominator = 1 } }

        newCursor =
            { cur | selectionAnchor = anchor }
    in
    ( updateCursorInPlace newCursor model
    , ApiCursor.nextBeat model.apiBaseUrl newCursor (Model.currentStartingBeat model) GotCursorResult
    )


handleSelectLeft : Model -> ( Model, Cmd Msg )
handleSelectLeft model =
    let
        cur =
            Model.cursor model

        anchor =
            case cur.selectionAnchor of
                Just _ ->
                    cur.selectionAnchor

                Nothing ->
                    Just { cycle = cur.cycle, beat = cur.beat, subdivision = { numerator = 0, denominator = 1 } }

        newCursor =
            { cur | selectionAnchor = anchor }
    in
    ( updateCursorInPlace newCursor model
    , ApiCursor.prevBeat model.apiBaseUrl newCursor (Model.currentStartingBeat model) GotCursorResult
    )


handleCopySelection : Model -> ( Model, Cmd Msg )
handleCopySelection model =
    let
        cur =
            Model.cursor model
    in
    case cur.selectionAnchor of
        Just _ ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiEditor.copySelection model.apiBaseUrl comp model.currentSectionIndex cur GotClipboardResult
            )

        Nothing ->
            ( addLog UiStrings.statusNoSelectionToCopy model, Cmd.none )


handleCutSelection : Model -> ( Model, Cmd Msg )
handleCutSelection model =
    let
        cur =
            Model.cursor model
    in
    case cur.selectionAnchor of
        Just _ ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiEditor.cutSelection model.apiBaseUrl comp model.currentSectionIndex cur GotClipboardResult
            )

        Nothing ->
            ( addLog UiStrings.statusNoSelectionToCut model, Cmd.none )


handleClipboardApiResult : Result Http.Error (ApiResult ClipboardResult) -> Model -> ( Model, Cmd Msg )
handleClipboardApiResult result model =
    handleApiResult result
        (\clipResult ->
            let
                snapshot =
                    { composition = clipResult.composition
                    , cursor = clipResult.cursor
                    , sectionIndex = model.currentSectionIndex
                    }

                newModel =
                    { model
                        | history = UndoHistory.push snapshot model.history
                        , pendingApiCall = False
                    }
                        |> addLog clipResult.message
            in
            ( newModel
            , Cmd.batch
                [ Ports.copyToClipboard clipResult.clipboardJson
                , requestLayout newModel
                ]
            )
        )
        model


handlePasteFromClipboard : String -> Model -> ( Model, Cmd Msg )
handlePasteFromClipboard jsonString model =
    let
        comp =
            Model.composition model

        cur =
            Model.cursor model
    in
    ( { model | pendingApiCall = True }
    , ApiEditor.pasteClipboard model.apiBaseUrl comp model.currentSectionIndex cur jsonString GotEditorResult
    )


updateCursorInPlace : CursorModel -> Model -> Model
updateCursorInPlace newCursor model =
    let
        currentSnapshot =
            UndoHistory.present model.history

        snapshot =
            { currentSnapshot | cursor = newCursor }
    in
    { model | history = UndoHistory.push snapshot model.history }



-- UNDO / REDO


handleUndo : Model -> ( Model, Cmd Msg )
handleUndo model =
    case UndoHistory.undo model.history of
        Just newHistory ->
            let
                newModel =
                    { model
                        | history = newHistory
                        , currentSectionIndex = (UndoHistory.present newHistory).sectionIndex
                    }
                        |> addLog UiStrings.statusUndo
            in
            ( newModel, requestLayout newModel )

        Nothing ->
            ( addLog UiStrings.statusNothingToUndo model, Cmd.none )


handleRedo : Model -> ( Model, Cmd Msg )
handleRedo model =
    case UndoHistory.redo model.history of
        Just newHistory ->
            let
                newModel =
                    { model
                        | history = newHistory
                        , currentSectionIndex = (UndoHistory.present newHistory).sectionIndex
                    }
                        |> addLog UiStrings.statusRedo
            in
            ( newModel, requestLayout newModel )

        Nothing ->
            ( addLog UiStrings.statusNothingToRedo model, Cmd.none )



-- API RESPONSE HANDLERS


handleStartingBeatResult : Result Http.Error (ApiResult Composition) -> Model -> ( Model, Cmd Msg )
handleStartingBeatResult result model =
    handleApiResult result
        (\comp ->
            let
                updatedModel =
                    updateComposition comp model
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
                                |> addLog UiStrings.statusStartingBeatsUpdated
                    in
                    ( newModel, requestLayout newModel )
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
    handleApiResult result
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
                    ( updatedModel, requestLayout updatedModel )
        )
        model


handleEditorApiResult : Result Http.Error (ApiResult EditorResult) -> Model -> ( Model, Cmd Msg )
handleEditorApiResult result model =
    handleApiResult result
        (\editorResult ->
            let
                snapshot =
                    { composition = editorResult.composition
                    , cursor = editorResult.cursor
                    , sectionIndex = model.currentSectionIndex
                    }

                newModel =
                    { model
                        | history = UndoHistory.push snapshot model.history
                        , pendingApiCall = False
                    }
                        |> addLog editorResult.message
            in
            ( newModel, requestLayout newModel )
        )
        model


handleCursorApiResult : Result Http.Error (ApiResult CursorModel) -> Model -> ( Model, Cmd Msg )
handleCursorApiResult result model =
    handleApiResult result
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
    handleApiResult result
        (\grids ->
            ( { model | layoutGrids = grids }, Cmd.none )
        )
        model


{-| Generic API result handler that extracts Success, logs ApiFailure/HttpError.
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
            , Cmd.none
            )

        Ok (HttpError httpErr) ->
            ( { model | pendingApiCall = False }
                |> addLog (UiStrings.statusHttpError |> String.replace "{message}" (httpErrorToString httpErr))
            , Cmd.none
            )

        Err httpError ->
            ( { model | pendingApiCall = False }
                |> addLog (UiStrings.statusHttpError |> String.replace "{message}" (httpErrorToString httpError))
            , Cmd.none
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



-- NEW DIALOG SUBMISSION


handleNewDialogSubmit : Model -> ( Model, Cmd Msg )
handleNewDialogSubmit model =
    let
        form =
            model.newDialogForm

        maybeTaal =
            findByName form.taalName model.availableTaals

        maybeRaag =
            findByName form.raagName model.availableRaags
    in
    case ( maybeTaal, maybeRaag ) of
        ( Just taal, Just raag ) ->
            let
                compType =
                    case form.compositionType of
                        "bandish" ->
                            Bandish

                        "palta" ->
                            Palta

                        "sargam" ->
                            Sargam

                        _ ->
                            Gat

                laya =
                    case form.layaName of
                        "ativilambit" ->
                            Just AtiVilambit

                        "vilambit" ->
                            Just Vilambit

                        "madhya" ->
                            Just MadhyaLaya

                        "drut" ->
                            Just Drut

                        "atidrut" ->
                            Just AtiDrut

                        _ ->
                            Nothing
            in
            ( { model | pendingApiCall = True }
            , ApiComposition.createComposition model.apiBaseUrl
                { title = form.title
                , compositionType = compType
                , taal = taal
                , raag = raag
                , laya = laya
                , taanCount = form.taanCount
                , showStrokeLine = form.showStrokes
                , showSahityaLine = form.showSahitya
                , gatStartingBeat = form.gatStartingBeat
                , antaraStartingBeat = form.antaraStartingBeat
                , taanStartingBeat = form.taanStartingBeat
                }
                GotNewComposition
            )

        _ ->
            ( addLog UiStrings.statusPleaseSelectValidTaalRaag model, Cmd.none )



-- HELPERS


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



-- GOOGLE DRIVE HANDLERS


handleConnectDrive : Model -> ( Model, Cmd Msg )
handleConnectDrive model =
    ( { model
        | driveState = DriveConnecting
        , fileBrowserCollapsed = False
      }
    , Api.GoogleDrive.initiateAuth
    )


handleDriveAuthResult : Decode.Value -> Model -> ( Model, Cmd Msg )
handleDriveAuthResult value model =
    case Decode.decodeValue (Decode.field "success" Decode.bool) value of
        Ok True ->
            ( addLog UiStrings.statusConnectedToDrive
                { model | driveState = DriveConnected }
            , Api.GoogleDrive.listDir "root"
            )

        _ ->
            ( addLog UiStrings.statusDriveAuthFailed
                { model | driveState = DriveDisconnected }
            , Cmd.none
            )


handleDriveDirListing : Decode.Value -> Model -> ( Model, Cmd Msg )
handleDriveDirListing value model =
    case Decode.decodeValue driveDirListingDecoder value of
        Ok listing ->
            let
                folderId =
                    listing.folderId

                folderName =
                    listing.folderName

                items =
                    listing.items

                existingFolder =
                    model.driveFolders
                        |> List.filter (\f -> f.folderId == folderId)
                        |> List.head

                updatedFolder =
                    { folderId = folderId
                    , name = folderName
                    , items = items
                    , expanded = True
                    , isBookmarked =
                        existingFolder
                            |> Maybe.map .isBookmarked
                            |> Maybe.withDefault False
                    }

                updatedFolders =
                    if List.any (\f -> f.folderId == folderId) model.driveFolders then
                        List.map
                            (\f ->
                                if f.folderId == folderId then
                                    updatedFolder

                                else
                                    f
                            )
                            model.driveFolders

                    else
                        model.driveFolders ++ [ updatedFolder ]
            in
            ( { model | driveFolders = updatedFolders }, Cmd.none )

        Err _ ->
            ( addLog UiStrings.statusFailedToParseDriveFolderListing model, Cmd.none )


handleDriveFileContent : Decode.Value -> Model -> ( Model, Cmd Msg )
handleDriveFileContent value model =
    case Decode.decodeValue driveFileContentDecoder value of
        Ok fileContent ->
            let
                _ =
                    fileContent
            in
            ( addLog (UiStrings.statusLoadingFileFromDrive |> String.replace "{filename}" fileContent.fileName) model
            , ApiComposition.parseComposition model.apiBaseUrl fileContent.content GotParsedComposition
            )

        Err _ ->
            ( addLog UiStrings.statusFailedToParseDriveFileContent model, Cmd.none )


handleDriveOpenFolder : String -> Model -> ( Model, Cmd Msg )
handleDriveOpenFolder folderId model =
    let
        alreadyLoaded =
            model.driveFolders
                |> List.filter (\f -> f.folderId == folderId)
                |> List.head
    in
    case alreadyLoaded of
        Just _ ->
            let
                toggledFolders =
                    List.map
                        (\f ->
                            if f.folderId == folderId then
                                { f | expanded = not f.expanded }

                            else
                                f
                        )
                        model.driveFolders
            in
            ( { model | driveFolders = toggledFolders }, Cmd.none )

        Nothing ->
            ( model, Api.GoogleDrive.listDir folderId )


handleDriveOpenFile : String -> String -> Model -> ( Model, Cmd Msg )
handleDriveOpenFile fileId fileName model =
    ( addLog (UiStrings.statusOpeningFromDrive |> String.replace "{filename}" fileName) model
    , Api.GoogleDrive.readFile fileId
    )


handleDriveToggleBookmark : String -> Model -> ( Model, Cmd Msg )
handleDriveToggleBookmark folderId model =
    let
        updatedFolders =
            List.map
                (\f ->
                    if f.folderId == folderId then
                        { f | isBookmarked = not f.isBookmarked }

                    else
                        f
                )
                model.driveFolders
    in
    ( { model | driveFolders = updatedFolders }
    , saveConfigCmd { model | driveFolders = updatedFolders }
    )


handleDriveCreateFile : String -> Model -> ( Model, Cmd Msg )
handleDriveCreateFile parentId model =
    ( model
    , Api.GoogleDrive.createFile
        { name = "Untitled.swar"
        , parentId = parentId
        , content = "{}"
        , mimeType = "application/json"
        }
    )


handleDriveCreateFolder : String -> Model -> ( Model, Cmd Msg )
handleDriveCreateFolder parentId model =
    ( model
    , Api.GoogleDrive.createFolder
        { name = UiStrings.fileBrowserNewFolderDefaultName
        , parentId = parentId
        }
    )



-- DRIVE JSON DECODERS


type alias DriveDirListing =
    { folderId : String
    , folderName : String
    , items : List DriveItem
    }


driveDirListingDecoder : Decode.Decoder DriveDirListing
driveDirListingDecoder =
    Decode.map3 DriveDirListing
        (Decode.field "folderId" Decode.string)
        (Decode.field "folderName" Decode.string)
        (Decode.field "items" (Decode.list driveItemDecoder))


driveItemDecoder : Decode.Decoder DriveItem
driveItemDecoder =
    Decode.map4 DriveItem
        (Decode.field "id" Decode.string)
        (Decode.field "name" Decode.string)
        (Decode.field "mimeType" Decode.string)
        (Decode.succeed False)


type alias DriveFileContent =
    { fileId : String
    , fileName : String
    , content : String
    }


driveFileContentDecoder : Decode.Decoder DriveFileContent
driveFileContentDecoder =
    Decode.map3 DriveFileContent
        (Decode.field "fileId" Decode.string)
        (Decode.field "fileName" Decode.string)
        (Decode.field "content" Decode.string)



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
                        |> addLog ("Debug reset: " ++ comp.metadata.title)
            in
            ( newModel
            , Cmd.batch
                [ requestLayout newModel
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
                , error = Just ("HTTP error: " ++ httpErrorToString httpErr)
                }
            )

        Err httpErr ->
            ( model
            , Ports.debugResponse
                { id = reqId
                , result = Encode.null
                , error = Just ("HTTP error: " ++ httpErrorToString httpErr)
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
                , error = Just ("HTTP error: " ++ httpErrorToString httpErr)
                }
            )

        Err httpErr ->
            ( model
            , respond
                { id = reqId
                , result = Encode.null
                , error = Just ("HTTP error: " ++ httpErrorToString httpErr)
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
                , error = Just ("HTTP error: " ++ httpErrorToString httpErr)
                }
            )

        Err httpErr ->
            ( model
            , respond
                { id = reqId
                , result = Encode.null
                , error = Just ("HTTP error: " ++ httpErrorToString httpErr)
                }
            )



-- DUPLICATE-TAB DIALOG (C.1)


handleDuplicateTabSwitch : Model -> ( Model, Cmd Msg )
handleDuplicateTabSwitch model =
    case model.pendingTabOpen of
        Just pending ->
            let
                cleared =
                    { model
                        | pendingTabOpen = Nothing
                        , showDuplicateTabDialog = False
                    }
            in
            handleSwitchTab pending.conflictingTabId cleared

        Nothing ->
            ( { model | showDuplicateTabDialog = False }, Cmd.none )


handleDuplicateTabRename : Model -> ( Model, Cmd Msg )
handleDuplicateTabRename model =
    case model.pendingTabOpen of
        Just pending ->
            let
                comp =
                    pending.composition

                firstStartingBeat =
                    comp.sections
                        |> List.head
                        |> Maybe.map .startingBeat
                        |> Maybe.withDefault 1

                defaultCursor =
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
                    , cursor = defaultCursor
                    , sectionIndex = 0
                    }

                newHistory =
                    UndoHistory.init snapshot

                tabId =
                    "tab-" ++ String.fromInt model.nextTabId

                newTab =
                    { id = tabId
                    , filename = pending.proposedTitle
                    , filePath = Nothing
                    , isReadOnly = False
                    , history = newHistory
                    , currentSectionIndex = 0
                    , editMode = SwarEdit
                    , ornamentMode = NoOrnament
                    , groupingState = Nothing
                    , layoutGrids = []
                    , isDirty = False
                    }

                savedModel =
                    Model.saveActiveTabState model

                logTemplate =
                    case pending.source of
                        PendingFromNewComposition ->
                            UiStrings.statusCreated

                        PendingFromOpenedFile ->
                            UiStrings.statusOpened

                newModel =
                    { savedModel
                        | history = newHistory
                        , currentSectionIndex = 0
                        , editMode = SwarEdit
                        , ornamentMode = NoOrnament
                        , groupingState = Nothing
                        , layoutGrids = []
                        , tabs = savedModel.tabs ++ [ newTab ]
                        , activeTabId = Just tabId
                        , nextTabId = model.nextTabId + 1
                        , pendingTabOpen = Nothing
                        , showDuplicateTabDialog = False
                    }
                        |> addLog (logTemplate |> String.replace "{title}" pending.proposedTitle)
            in
            ( newModel, requestLayout newModel )

        Nothing ->
            ( { model | showDuplicateTabDialog = False }, Cmd.none )



-- UNSAVED-CHANGES DIALOG (C.2)


handleUnsavedChangesDiscard : Model -> ( Model, Cmd Msg )
handleUnsavedChangesDiscard model =
    case model.showUnsavedChangesDialog of
        Just tabId ->
            let
                cleared =
                    { model | showUnsavedChangesDialog = Nothing }

                saved =
                    Model.saveActiveTabState cleared
            in
            doCloseTabImmediate tabId saved

        Nothing ->
            ( model, Cmd.none )


handleUnsavedChangesSave : Model -> ( Model, Cmd Msg )
handleUnsavedChangesSave model =
    -- Trigger Save (Save As if the tab has never been saved). Today both code
    -- paths route through the same download port; the asterisk clears via the
    -- FileTab.isDirty flip in the save handler once the save completes.
    case model.showUnsavedChangesDialog of
        Just tabId ->
            let
                cleared =
                    { model | showUnsavedChangesDialog = Nothing }

                targetTab =
                    cleared.tabs |> List.filter (\t -> t.id == tabId) |> List.head

                saveMsg =
                    case targetTab |> Maybe.andThen .filePath of
                        Just _ ->
                            SaveFile

                        Nothing ->
                            SaveFileAs
            in
            update saveMsg cleared

        Nothing ->
            ( model, Cmd.none )



-- SAVE AS (C.3)


handleSaveFileAs : Model -> ( Model, Cmd Msg )
handleSaveFileAs model =
    -- Save As always prompts. We mark pendingSaveAs so that when the API round-
    -- trips back through GotSerializedComposition, the downloadFile port carries
    -- forcePicker=True. On browsers with the File System Access API that triggers
    -- a fresh showSaveFilePicker; on legacy browsers the <a download> path
    -- already prompts on every save so the flag is effectively a no-op there.
    update SaveFile { model | pendingSaveAs = True }



-- AUTOSAVE TICK (C.2)


handleAutosaveTick : Model -> ( Model, Cmd Msg )
handleAutosaveTick model =
    -- Autosave fires only when the active tab has a known filePath AND is
    -- dirty. Without a filePath we can't write back (browser sandbox); the
    -- asterisk just stays on until the user runs Save As.
    let
        activeTab =
            model.activeTabId
                |> Maybe.andThen
                    (\id -> model.tabs |> List.filter (\t -> t.id == id) |> List.head)

        shouldSave =
            activeTab
                |> Maybe.map (\t -> t.isDirty && t.filePath /= Nothing)
                |> Maybe.withDefault False
    in
    if shouldSave then
        update SaveFile model

    else
        ( model, Cmd.none )
