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
        )
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import Task
import Time


{-| Grouping threshold in milliseconds — notes typed within this window
on the same beat are grouped onto a single beat with equal subdivisions.
-}
groupingThresholdMs : Int
groupingThresholdMs =
    500


{-| Main update function handling all Msg variants.
-}
update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
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
                |> addLog ("Script changed to " ++ scriptName script)
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
                |> addLog ("Switched to section " ++ String.fromInt (idx + 1))
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

        -- View toggles
        ToggleStrokeLine ->
            let
                comp =
                    Model.composition model

                meta =
                    comp.metadata

                newComp =
                    { comp | metadata = { meta | showStrokeLine = not meta.showStrokeLine } }

                newModel =
                    updateComposition newComp model
                        |> addLog
                            (if not meta.showStrokeLine then
                                "Stroke line shown"

                             else
                                "Stroke line hidden"
                            )
            in
            ( newModel, requestLayout newModel )

        ToggleSahityaLine ->
            let
                comp =
                    Model.composition model

                meta =
                    comp.metadata

                newComp =
                    { comp | metadata = { meta | showSahityaLine = not meta.showSahityaLine } }

                newModel =
                    updateComposition newComp model
                        |> addLog
                            (if not meta.showSahityaLine then
                                "Sahitya line shown"

                             else
                                "Sahitya line hidden"
                            )
            in
            ( newModel, requestLayout newModel )

        ToggleKeyboardLegend ->
            ( { model | showKeyboardLegend = not model.showKeyboardLegend }, Cmd.none )

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

                        cur =
                            Model.cursor model

                        meta =
                            comp.metadata

                        newComp =
                            { comp | metadata = { meta | title = form.title, taal = newTaal } }

                        changedBeats =
                            form.sectionStartingBeats
                                |> List.filterMap
                                    (\entry ->
                                        let
                                            currentBeat =
                                                newComp.sections
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
                                    newComp.sections
                                        |> List.drop model.currentSectionIndex
                                        |> List.head
                                        |> Maybe.map .startingBeat
                                        |> Maybe.withDefault 1

                        newCursor =
                            { cur | taal = newTaal, cycle = 0, beat = newSectionStartBeat - 1, subIndex = 0, totalSubdivisions = 1 }

                        snapshot =
                            { composition = newComp
                            , cursor = newCursor
                            , sectionIndex = model.currentSectionIndex
                            }

                        baseModel =
                            { model
                                | history = UndoHistory.push snapshot model.history
                                , showPropsDialog = False
                            }
                                |> addLog ("Properties updated — taal: " ++ newTaal.name)
                    in
                    case changedBeats of
                        ( sectionIdx, beatVal ) :: rest ->
                            ( { baseModel
                                | pendingStartingBeatChanges = rest
                                , pendingApiCall = True
                              }
                            , ApiEditor.changeStartingBeat
                                model.apiBaseUrl
                                newComp
                                sectionIdx
                                beatVal
                                GotStartingBeatResult
                            )

                        [] ->
                            ( baseModel, requestLayout baseModel )

                Nothing ->
                    ( { model | showPropsDialog = False }
                        |> addLog "Properties updated (taal not found, kept previous)"
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
                    |> addLog ("Bug report sent — thanks! (" ++ message ++ ")")
                , Cmd.none
                )

            else
                let
                    form =
                        model.bugReportForm
                in
                ( { model | bugReportForm = { form | sending = False } }
                    |> addLog ("Bug report failed: " ++ message)
                , Cmd.none
                )

        -- API Responses
        GotStartingBeatResult result ->
            handleStartingBeatResult result model

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
                        |> addLog ("Loaded " ++ String.fromInt (List.length taals) ++ " taals")
                    , Cmd.none
                    )
                )
                model

        GotRaags result ->
            handleApiResult result
                (\raags ->
                    ( { model | availableRaags = raags }
                        |> addLog ("Loaded " ++ String.fromInt (List.length raags) ++ " raags")
                    , Cmd.none
                    )
                )
                model

        GotColors result ->
            handleApiResult result
                (\colors ->
                    ( { model | notationColors = Just colors }
                        |> addLog "Colors loaded"
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
                            }

                        savedModel =
                            Model.saveActiveTabState model

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
                                |> addLog ("Created: " ++ comp.metadata.title)
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
                                |> addLog "Section added"
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
                                |> addLog "Section removed"
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
                                |> addLog "Section renamed"
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
                                |> addLog "Sections reordered"
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
                        |> addLog "Exporting HTML..."
                    , Ports.downloadFile
                        { filename = filename
                        , mimeType = "text/html"
                        , content = htmlString
                        }
                    )
                )
                model

        GotSerializedComposition result ->
            handleApiResult result
                (\jsonValue ->
                    let
                        comp =
                            Model.composition model

                        filename =
                            comp.metadata.title ++ ".swar"

                        content =
                            Encode.encode 2 jsonValue
                    in
                    ( { model | pendingApiCall = False }
                        |> addLog "Saving composition..."
                    , Ports.downloadFile
                        { filename = filename
                        , mimeType = "application/json"
                        , content = content
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
                            }

                        savedModel =
                            Model.saveActiveTabState model

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
                                |> addLog ("Opened: " ++ comp.metadata.title)
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
            ( addLog ("File selected: " ++ filename) model, Cmd.none )

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
            ( addLog "File saved to Drive" model, Cmd.none )

        GotDriveError errMsg ->
            ( addLog ("Drive error: " ++ errMsg) model, Cmd.none )

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

        -- Timers
        CursorBlink _ ->
            ( { model | cursorVisible = not model.cursorVisible }, Cmd.none )

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
                            |> addLog ("Switched to " ++ tab.filename)
                in
                ( newModel, requestLayout newModel )

            Nothing ->
                ( model, Cmd.none )


handleCloseTab : String -> Model -> ( Model, Cmd Msg )
handleCloseTab tabId model =
    let
        savedModel =
            Model.saveActiveTabState model

        remainingTabs =
            List.filter (\t -> t.id /= tabId) savedModel.tabs
    in
    if List.isEmpty remainingTabs then
        let
            newModel =
                handleNewTabHelper { savedModel | tabs = [] }
                    |> addLog "Last tab closed — new tab created"
        in
        ( newModel, requestLayout newModel )

    else if savedModel.activeTabId == Just tabId then
        let
            nextTab =
                List.head remainingTabs
        in
        case nextTab of
            Just tab ->
                let
                    newModel =
                        Model.loadTabState tab { savedModel | tabs = remainingTabs }
                            |> addLog ("Closed tab, switched to " ++ tab.filename)
                in
                ( newModel, requestLayout newModel )

            Nothing ->
                ( { savedModel | tabs = remainingTabs }, Cmd.none )

    else
        ( { savedModel | tabs = remainingTabs }
            |> addLog "Tab closed"
        , Cmd.none
        )


handleNewTab : Model -> ( Model, Cmd Msg )
handleNewTab model =
    let
        newModel =
            handleNewTabHelper model
                |> addLog "New tab"
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
            in
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
            ( m
            , ApiCursor.setSubdivisions m.apiBaseUrl cur n GotCursorResult
            )

        OctaveMandra ->
            let
                cur =
                    Model.cursor m
            in
            ( m
            , ApiCursor.setOctave m.apiBaseUrl cur Mandra GotCursorResult
            )

        OctaveMadhya ->
            let
                cur =
                    Model.cursor m
            in
            ( m
            , ApiCursor.setOctave m.apiBaseUrl cur Madhya GotCursorResult
            )

        OctaveTaar ->
            let
                cur =
                    Model.cursor m
            in
            ( m
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
                |> addLog "Kan Swar: type the grace note"
            , Cmd.none
            )

        OrnamentSparsh ->
            ( { m | ornamentMode = SingleNoteMode "sparsh" }
                |> addLog "Sparsh: type the touch note"
            , Cmd.none
            )

        OrnamentGhaseet ->
            ( { m | ornamentMode = SingleNoteMode "ghaseet" }
                |> addLog "Ghaseet: type the target note"
            , Cmd.none
            )

        OrnamentMeendAsc ->
            ( { m | ornamentMode = MeendStartMode Ascending }
                |> addLog "Meend (ascending): type start note"
            , Cmd.none
            )

        OrnamentMeendDesc ->
            ( { m | ornamentMode = MeendStartMode Descending }
                |> addLog "Meend (descending): type start note"
            , Cmd.none
            )

        OrnamentKrintan ->
            ( { m | ornamentMode = KrintanStartMode }
                |> addLog "Krintan: type notes, then Enter"
            , Cmd.none
            )

        OrnamentMurki ->
            ( { m | ornamentMode = MurkiCollectMode [] }
                |> addLog "Murki: type notes, then Enter"
            , Cmd.none
            )

        OrnamentZamzama ->
            ( { m | ornamentMode = ZamzamaCollectMode [] }
                |> addLog "Zamzama: type notes, then Enter"
            , Cmd.none
            )

        OrnamentCancel ->
            ( { m | ornamentMode = NoOrnament }
                |> addLog "Ornament mode cancelled"
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
-}
handleSwarKey : Note -> Variant -> String -> Model -> ( Model, Cmd Msg )
handleSwarKey note variant key model =
    ( model
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
                |> addLog "Ornament mode cancelled"
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
                |> addLog "Collecting ornament notes..."
            , Cmd.none
            )

        Cancelled ->
            ( { model | ornamentMode = NoOrnament }
                |> addLog "Ornament cancelled"
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
            ( addLog "No selection to copy" model, Cmd.none )


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
            ( addLog "No selection to cut" model, Cmd.none )


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
                        |> addLog "Undo"
            in
            ( newModel, requestLayout newModel )

        Nothing ->
            ( addLog "Nothing to undo" model, Cmd.none )


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
                        |> addLog "Redo"
            in
            ( newModel, requestLayout newModel )

        Nothing ->
            ( addLog "Nothing to redo" model, Cmd.none )



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
                                |> addLog "Starting beats updated"
                    in
                    ( newModel, requestLayout newModel )
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
                    }
            in
            ( newModel, Cmd.none )
        )
        model


handleLayoutApiResult : Result Http.Error (ApiResult (List SectionGrid)) -> Model -> ( Model, Cmd Msg )
handleLayoutApiResult result model =
    handleApiResult result
        (\grids ->
            ( { model | layoutGrids = grids, pendingApiCall = False }, Cmd.none )
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
                |> addLog ("API error: " ++ apiError.message)
            , Cmd.none
            )

        Ok (HttpError httpErr) ->
            ( { model | pendingApiCall = False }
                |> addLog ("HTTP error: " ++ httpErrorToString httpErr)
            , Cmd.none
            )

        Err httpError ->
            ( { model | pendingApiCall = False }
                |> addLog ("HTTP error: " ++ httpErrorToString httpError)
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
            ( addLog "Please select a valid taal and raag" model, Cmd.none )



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
            "Bad URL: " ++ url

        Http.Timeout ->
            "Request timed out"

        Http.NetworkError ->
            "Network error"

        Http.BadStatus code ->
            "Bad status: " ++ String.fromInt code

        Http.BadBody msg ->
            "Bad body: " ++ msg



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
            ( addLog "Connected to Google Drive"
                { model | driveState = DriveConnected }
            , Api.GoogleDrive.listDir "root"
            )

        _ ->
            ( addLog "Drive authentication failed"
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
            ( addLog "Failed to parse Drive folder listing" model, Cmd.none )


handleDriveFileContent : Decode.Value -> Model -> ( Model, Cmd Msg )
handleDriveFileContent value model =
    case Decode.decodeValue driveFileContentDecoder value of
        Ok fileContent ->
            let
                _ =
                    fileContent
            in
            ( addLog ("Loading file from Drive: " ++ fileContent.fileName) model
            , ApiComposition.parseComposition model.apiBaseUrl fileContent.content GotParsedComposition
            )

        Err _ ->
            ( addLog "Failed to parse Drive file content" model, Cmd.none )


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
    ( addLog ("Opening from Drive: " ++ fileName) model
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
        { name = "New Folder"
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
