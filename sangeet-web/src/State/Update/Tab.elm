module State.Update.Tab exposing
    ( handleCloseTab
    , handleDuplicateTabCancel
    , handleDuplicateTabRename
    , handleDuplicateTabSwitch
    , handleNewTab
    , handleSwitchTab
    , handleToggleFileBrowser
    , handleUnsavedChangesCancel
    , handleUnsavedChangesDiscard
    , handleUnsavedChangesSave
    )

{-| Tab-management handlers: open / close / switch / new tab, plus the
duplicate-tab and unsaved-changes confirmation dialogs that gate close.
The file-browser collapse toggle lives here too since it shares the same
multi-document workspace surface.
-}

import Model.Composition exposing (CompositionType(..), SectionType(..))
import Model.Taal exposing (VibhagMarker(..))
import Model.Types exposing (Octave(..))
import State.Model as Model
    exposing
        ( EditMode(..)
        , Model
        , OrnamentMode(..)
        , PendingTabSource(..)
        )
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import State.Update.Helpers as Helpers
import UiStrings



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
                            |> Helpers.addLog (UiStrings.statusSwitchedToTab |> String.replace "{filename}" tab.filename)
                in
                ( newModel, Helpers.requestLayout newModel )

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
        ( { model | tabs = [], activeTabId = Nothing }
            |> Helpers.addLog UiStrings.statusAllTabsClosed
        , Cmd.none
        )

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
                            |> Helpers.addLog (UiStrings.statusClosedTabSwitched |> String.replace "{filename}" tab.filename)
                in
                ( newModel, Helpers.requestLayout newModel )

            Nothing ->
                ( { model | tabs = remainingTabs }, Cmd.none )

    else
        ( { model | tabs = remainingTabs }
            |> Helpers.addLog UiStrings.statusTabClosed
        , Cmd.none
        )


handleNewTab : Model -> ( Model, Cmd Msg )
handleNewTab model =
    let
        newModel =
            handleNewTabHelper model
                |> Helpers.addLog UiStrings.statusNewTab
    in
    ( newModel, Helpers.requestLayout newModel )


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


handleToggleFileBrowser : Model -> ( Model, Cmd Msg )
handleToggleFileBrowser model =
    ( { model | fileBrowserCollapsed = not model.fileBrowserCollapsed }, Cmd.none )



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
                        |> Helpers.addLog (logTemplate |> String.replace "{title}" pending.proposedTitle)
            in
            ( newModel, Helpers.requestLayout newModel )

        Nothing ->
            ( { model | showDuplicateTabDialog = False }, Cmd.none )


handleDuplicateTabCancel : Model -> ( Model, Cmd Msg )
handleDuplicateTabCancel model =
    ( { model | pendingTabOpen = Nothing, showDuplicateTabDialog = False }, Cmd.none )



-- UNSAVED-CHANGES DIALOG (C.2)


handleUnsavedChangesCancel : Model -> ( Model, Cmd Msg )
handleUnsavedChangesCancel model =
    ( { model | showUnsavedChangesDialog = Nothing }, Cmd.none )


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


{-| Resolve the unsaved-changes dialog's "Save" button: dispatch a Save (or
Save As) Msg via the top-level update so the wrapper's dirty-flag tracking
and debug-ack drain pass run normally.
-}
handleUnsavedChangesSave : (Msg -> Model -> ( Model, Cmd Msg )) -> Model -> ( Model, Cmd Msg )
handleUnsavedChangesSave runUpdate model =
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
            runUpdate saveMsg cleared

        Nothing ->
            ( model, Cmd.none )
