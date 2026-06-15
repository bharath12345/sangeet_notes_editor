module State.Update exposing (update)

{-| Top-level Msg dispatcher.

The actual handler logic lives in per-concern submodules under
`State.Update.*`:

  - `Editor` — typing, cursor, ornament, grouping, undo/redo, copy/paste,
    theme/script toggles, mouse clicks on the canvas.
  - `File` — file open/save/save-as, HTML export, autosave, file-picker
    ports, and Google Drive directory/file operations.
  - `Tab` — open/close/switch tabs, duplicate-tab and unsaved-changes
    dialogs, file-browser collapse toggle.
  - `Dialog` — New Composition, Properties, About, Support, Keyboard
    Cheat Sheet, Command Palette, Bug Report.
  - `Section` — select/add/remove/clear/reorder sections + their API
    responses.
  - `Net` — HTTP API success/failure response wiring (reference data,
    editor/cursor/layout, debug bridge async callbacks), config
    persistence.
  - `Helpers` — tiny shared utilities (addLog, requestLayout,
    handleApiResult, etc.) used by every submodule.

This file keeps the `update` wrapper (markActiveTabDirtyIfEdited +
drainPendingDebugAck) and the big `case` that routes each `Msg`
variant to the right submodule handler. When a handler needs to
recursively dispatch another `Msg` (e.g. keyboard shortcuts that fire
SaveFile, palette actions, autosave) it receives `update` as a
parameter so the wrapper continues to run.

-}

import Json.Encode as Encode
import Ports
import State.Model as Model exposing (Model)
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import State.Update.Dialog as Dialog
import State.Update.Editor as Editor
import State.Update.File as File
import State.Update.Net as Net
import State.Update.Section as Section
import State.Update.Tab as Tab


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
        -- Keyboard
        KeyPressed key shiftKey ctrlKey altKey ->
            Editor.handleKeyPress update key shiftKey ctrlKey altKey model

        -- Mouse
        CanvasClicked cycle beat ->
            Editor.handleCanvasClicked cycle beat model

        -- File operations
        NewComposition ->
            Dialog.handleNewComposition model

        OpenFile ->
            File.handleOpenFile model

        SaveFile ->
            File.handleSaveFile model

        SaveFileAs ->
            File.handleSaveFileAs update model

        ExportHtml ->
            File.handleExportHtml model

        -- Edit operations
        Undo ->
            Editor.handleUndo model

        Redo ->
            Editor.handleRedo model

        -- Script
        ChangeScript script ->
            Editor.handleChangeScript script model

        -- Theme
        ToggleTheme ->
            Editor.handleToggleTheme model

        -- Section operations
        SelectSection idx ->
            Section.handleSelectSection idx model

        AddSection name sectionType ->
            Section.handleAddSection name sectionType model

        RemoveSection idx ->
            Section.handleRemoveSection idx model

        RequestClearSection idx ->
            Section.handleRequestClearSection idx model

        ConfirmClearSection ->
            Section.handleConfirmClearSection model

        CancelClearSection ->
            Section.handleCancelClearSection model

        ClearSection idx ->
            Section.handleClearSection idx model

        MoveSectionUp idx ->
            Section.handleMoveSectionUp idx model

        MoveSectionDown idx ->
            Section.handleMoveSectionDown idx model

        -- New dialog
        ShowNewDialog ->
            Dialog.handleShowNewDialog model

        NewDialogSetTitle t ->
            Dialog.handleNewDialogSetTitle t model

        NewDialogSetType t ->
            Dialog.handleNewDialogSetType t model

        NewDialogSetRaag r ->
            Dialog.handleNewDialogSetRaag r model

        NewDialogSetTaal t ->
            Dialog.handleNewDialogSetTaal t model

        NewDialogSetLaya l ->
            Dialog.handleNewDialogSetLaya l model

        NewDialogSetTaanCount s ->
            Dialog.handleNewDialogSetTaanCount s model

        NewDialogSetShowStrokes b ->
            Dialog.handleNewDialogSetShowStrokes b model

        NewDialogSetShowSahitya b ->
            Dialog.handleNewDialogSetShowSahitya b model

        NewDialogSetGatStartingBeat s ->
            Dialog.handleNewDialogSetGatStartingBeat s model

        NewDialogSetAntaraStartingBeat s ->
            Dialog.handleNewDialogSetAntaraStartingBeat s model

        NewDialogSetTaanStartingBeat s ->
            Dialog.handleNewDialogSetTaanStartingBeat s model

        NewDialogSetThaat t ->
            Dialog.handleNewDialogSetThaat t model

        NewDialogSetArohan a ->
            Dialog.handleNewDialogSetArohan a model

        NewDialogSetAvrohan a ->
            Dialog.handleNewDialogSetAvrohan a model

        NewDialogSetVadi v ->
            Dialog.handleNewDialogSetVadi v model

        NewDialogSetSamvadi s ->
            Dialog.handleNewDialogSetSamvadi s model

        NewDialogSetScript s ->
            Dialog.handleNewDialogSetScript s model

        NewDialogSubmit ->
            Dialog.handleNewDialogSubmit model

        NewDialogCancel ->
            Dialog.handleNewDialogCancel model

        -- Properties dialog
        ShowPropsDialog ->
            Dialog.handleShowPropsDialog model

        PropsDialogSetTitle t ->
            Dialog.handlePropsDialogSetTitle t model

        PropsDialogSetTaal t ->
            Dialog.handlePropsDialogSetTaal t model

        PropsDialogSetStartingBeat sectionIndex beatStr ->
            Dialog.handlePropsDialogSetStartingBeat sectionIndex beatStr model

        PropsDialogSubmit ->
            Dialog.handlePropsDialogSubmit model

        PropsDialogCancel ->
            Dialog.handlePropsDialogCancel model

        -- About dialog
        ShowAboutDialog ->
            Dialog.handleShowAboutDialog model

        CloseAboutDialog ->
            Dialog.handleCloseAboutDialog model

        -- Support dialog
        ShowSupportDialog ->
            Dialog.handleShowSupportDialog model

        CloseSupportDialog ->
            Dialog.handleCloseSupportDialog model

        -- Keyboard cheat sheet
        ShowKeyboardCheatSheet ->
            Dialog.handleShowKeyboardCheatSheet model

        CloseKeyboardCheatSheet ->
            Dialog.handleCloseKeyboardCheatSheet model

        -- User guide
        OpenUserGuide ->
            Dialog.handleOpenUserGuide model

        -- Command palette
        ShowCommandPalette ->
            Dialog.handleShowCommandPalette model

        CloseCommandPalette ->
            Dialog.handleCloseCommandPalette model

        PaletteQueryChanged q ->
            Dialog.handlePaletteQueryChanged q model

        PaletteSelectIndex i ->
            Dialog.handlePaletteSelectIndex i model

        PaletteRunSelected ->
            Dialog.handlePaletteRunSelected update model

        PaletteRunIndex i ->
            Dialog.handlePaletteRunIndex update i model

        -- Bug report dialog
        ShowBugReportDialog ->
            Dialog.handleShowBugReportDialog model

        BugReportSetDescription d ->
            Dialog.handleBugReportSetDescription d model

        BugReportSetEmail e ->
            Dialog.handleBugReportSetEmail e model

        BugReportSubmit ->
            Dialog.handleBugReportSubmit model

        BugReportCancel ->
            Dialog.handleBugReportCancel model

        BugReportResult success message ->
            Net.handleBugReportResult success message model

        -- API responses
        GotStartingBeatResult result ->
            Net.handleStartingBeatResult result model

        GotTaalChangeResult result ->
            Net.handleTaalChangeResult result model

        GotEditorResult result ->
            Net.handleEditorApiResult result model

        GotCursorResult result ->
            Net.handleCursorApiResult result model

        GotLayoutResult result ->
            Net.handleLayoutApiResult result model

        GotTaals result ->
            Net.handleTaals result model

        GotRaags result ->
            Net.handleRaags result model

        GotColors result ->
            Net.handleColors result model

        GotScripts result ->
            Net.handleScripts result model

        GotNewComposition result ->
            Dialog.handleNewGotComposition result model

        GotSectionAdd result ->
            Section.handleSectionAdd result model

        GotSectionRemove result ->
            Section.handleSectionRemove result model

        GotSectionClear result ->
            Section.handleSectionClear result model

        GotSectionReorder result ->
            Section.handleSectionReorder result model

        GotExportHtml result ->
            File.handleGotExportHtml result model

        GotSerializedComposition result ->
            File.handleGotSerializedComposition result model

        GotParsedComposition result ->
            File.handleGotParsedComposition result model

        -- Clipboard operations
        GotClipboardResult result ->
            Editor.handleGotClipboardResult result model

        ClipboardContentReceived jsonString ->
            Editor.handleClipboardContentReceived jsonString model

        -- File port responses
        FileSelected filename ->
            File.handleFileSelected filename model

        FileLoaded content ->
            File.handleFileLoaded content model

        -- Swar key timing for grouping detection
        GotSwarKeyTime posix note variant _ ->
            Editor.handleGotSwarKeyTime posix note variant model

        -- Tab management
        SwitchTab tabId ->
            Tab.handleSwitchTab tabId model

        CloseTab tabId ->
            Tab.handleCloseTab tabId model

        NewTab ->
            Tab.handleNewTab model

        -- File browser
        ToggleFileBrowser ->
            Tab.handleToggleFileBrowser model

        -- Google Drive
        ConnectDrive ->
            File.handleConnectDrive model

        GotDriveAuthResult value ->
            File.handleDriveAuthResult value model

        GotDriveDirListing value ->
            File.handleDriveDirListing value model

        GotDriveFileContent value ->
            File.handleDriveFileContent value model

        GotDriveWriteResult _ ->
            File.handleDriveWriteResult model

        GotDriveError errMsg ->
            File.handleDriveError errMsg model

        DriveOpenFolder folderId ->
            File.handleDriveOpenFolder folderId model

        DriveOpenFile fileId fileName ->
            File.handleDriveOpenFile fileId fileName model

        DriveToggleBookmark folderId ->
            File.handleDriveToggleBookmark folderId model

        DriveRefreshFolder folderId ->
            File.handleDriveRefreshFolder folderId model

        DriveDeleteItem parentFolderId fileId ->
            File.handleDriveDeleteItem parentFolderId fileId model

        -- Config persistence
        GotConfigLoaded configJson ->
            Net.handleConfigLoaded configJson model

        -- Debug bridge (WS only)
        DebugCommandReceived raw ->
            Net.handleDebugCommandReceived update raw model

        DebugResetReceived reqId result ->
            Net.handleDebugResetReceived reqId result model

        DebugDumpReceived reqId result ->
            Net.handleDebugDumpReceived reqId result model

        DebugExportReceived reqId result ->
            Net.handleDebugExportReceived reqId result model

        DebugSetTaalReceived reqId result ->
            Net.handleDebugEditorResultReceived reqId result model

        DebugStrokeReceived reqId result ->
            Net.handleDebugEditorResultReceived reqId result model

        -- Uncaught JS error capture (Plan 18 PR-3c)
        UncaughtErrorReceived raw ->
            Net.handleUncaughtError raw model

        -- Duplicate-tab dialog resolution
        DuplicateTabSwitch ->
            Tab.handleDuplicateTabSwitch model

        DuplicateTabOpenWithNewName ->
            Tab.handleDuplicateTabRename model

        DuplicateTabCancel ->
            Tab.handleDuplicateTabCancel model

        -- Unsaved-changes dialog
        UnsavedChangesCancel ->
            Tab.handleUnsavedChangesCancel model

        UnsavedChangesDiscard ->
            Tab.handleUnsavedChangesDiscard model

        UnsavedChangesSave ->
            Tab.handleUnsavedChangesSave update model

        -- Autosave tick
        AutosaveTick _ ->
            File.handleAutosaveTick update model

        -- No-op
        NoOp ->
            ( model, Cmd.none )
