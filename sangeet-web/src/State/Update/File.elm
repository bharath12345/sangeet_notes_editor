module State.Update.File exposing
    ( handleAutosaveTick
    , handleConnectDrive
    , handleDriveAuthResult
    , handleDriveDeleteItem
    , handleDriveDirListing
    , handleDriveError
    , handleDriveFileContent
    , handleDriveOpenFile
    , handleDriveOpenFolder
    , handleDriveRefreshFolder
    , handleDriveToggleBookmark
    , handleDriveWriteResult
    , handleExportHtml
    , handleFileLoaded
    , handleFileSelected
    , handleGotExportHtml
    , handleGotParsedComposition
    , handleGotSerializedComposition
    , handleOpenFile
    , handleSaveFile
    , handleSaveFileAs
    )

{-| File-handling handlers: open / save / save-as, export to HTML, the
file-picker and file-loaded ports, plus Google Drive directory listing,
file open/read, bookmark toggle, and write-result wiring. The
GotParsedComposition / GotSerializedComposition / GotExportHtml API
responses also live here because they are 1:1 with file operations.
-}

import Api.Client exposing (ApiResult)
import Api.Composition as ApiComposition
import Api.Export as ApiExport
import Api.GoogleDrive
import Http
import Json.Decode as Decode
import Model.Composition exposing (Composition)
import Model.Types exposing (Octave(..))
import Ports
import State.Model as Model
    exposing
        ( DriveItem
        , DriveState(..)
        , EditMode(..)
        , Model
        , OrnamentMode(..)
        , PendingTabSource(..)
        )
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import State.Update.Helpers as Helpers
import State.Update.Net as Net
import UiStrings
import Util.TabNameResolver



-- FILE OPS


handleOpenFile : Model -> ( Model, Cmd Msg )
handleOpenFile model =
    ( model, Ports.selectFile ".swar" )


handleSaveFile : Model -> ( Model, Cmd Msg )
handleSaveFile model =
    let
        comp =
            Model.composition model
    in
    ( { model | pendingApiCall = True }
    , ApiComposition.serializeComposition model.apiBaseUrl comp GotSerializedComposition
    )


{-| Save As always prompts. We mark pendingSaveAs so that when the API round-
trips back through GotSerializedComposition, the downloadFile port carries
forcePicker=True. On browsers with the File System Access API that triggers
a fresh showSaveFilePicker; on legacy browsers the <a download> path
already prompts on every save so the flag is effectively a no-op there.
-}
handleSaveFileAs : (Msg -> Model -> ( Model, Cmd Msg )) -> Model -> ( Model, Cmd Msg )
handleSaveFileAs runUpdate model =
    runUpdate SaveFile { model | pendingSaveAs = True }


handleExportHtml : Model -> ( Model, Cmd Msg )
handleExportHtml model =
    let
        comp =
            Model.composition model
    in
    ( { model | pendingApiCall = True }
    , ApiExport.exportHtml model.apiBaseUrl comp model.currentScript GotExportHtml
    )


handleFileSelected : String -> Model -> ( Model, Cmd Msg )
handleFileSelected filename model =
    ( Helpers.addLog (UiStrings.statusFileSelected |> String.replace "{filename}" filename) model, Cmd.none )


handleFileLoaded : String -> Model -> ( Model, Cmd Msg )
handleFileLoaded content model =
    ( { model | pendingApiCall = True }
    , ApiComposition.parseComposition model.apiBaseUrl content GotParsedComposition
    )



-- AUTOSAVE


{-| Autosave fires only when the active tab has a known filePath AND is
dirty. Without a filePath we can't write back (browser sandbox); the
asterisk just stays on until the user runs Save As.
-}
handleAutosaveTick : (Msg -> Model -> ( Model, Cmd Msg )) -> Model -> ( Model, Cmd Msg )
handleAutosaveTick runUpdate model =
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
        runUpdate SaveFile model

    else
        ( model, Cmd.none )



-- API RESPONSE HANDLERS (FILE OPS)


handleGotExportHtml : Result Http.Error (ApiResult String) -> Model -> ( Model, Cmd Msg )
handleGotExportHtml result model =
    Helpers.handleApiResult result
        (\htmlString ->
            let
                comp =
                    Model.composition model

                filename =
                    comp.metadata.title ++ ".html"
            in
            ( { model | pendingApiCall = False }
                |> Helpers.addLog UiStrings.statusExportingHtml
            , Ports.downloadFile
                { filename = filename
                , mimeType = "text/html"
                , content = htmlString
                , forcePicker = True
                }
            )
        )
        model


handleGotSerializedComposition : Result Http.Error (ApiResult String) -> Model -> ( Model, Cmd Msg )
handleGotSerializedComposition result model =
    Helpers.handleApiResult result
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
                |> Helpers.addLog UiStrings.statusSavingComposition
            , Ports.downloadFile
                { filename = filename
                , mimeType = "application/json"
                , content = swarString
                , forcePicker = model.pendingSaveAs
                }
            )
        )
        model


handleGotParsedComposition : Result Http.Error (ApiResult Composition) -> Model -> ( Model, Cmd Msg )
handleGotParsedComposition result model =
    Helpers.handleApiResult result
        (\comp ->
            let
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
                            |> Helpers.addLog (UiStrings.statusOpened |> String.replace "{title}" comp.metadata.title)
                in
                ( newModel, Helpers.requestLayout newModel )
        )
        model



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
            ( Helpers.addLog UiStrings.statusConnectedToDrive
                { model | driveState = DriveConnected }
            , Api.GoogleDrive.listDir "root"
            )

        _ ->
            ( Helpers.addLog UiStrings.statusDriveAuthFailed
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
            ( Helpers.addLog UiStrings.statusFailedToParseDriveFolderListing model, Cmd.none )


handleDriveFileContent : Decode.Value -> Model -> ( Model, Cmd Msg )
handleDriveFileContent value model =
    case Decode.decodeValue driveFileContentDecoder value of
        Ok fileContent ->
            ( Helpers.addLog (UiStrings.statusLoadingFileFromDrive |> String.replace "{filename}" fileContent.fileName) model
            , ApiComposition.parseComposition model.apiBaseUrl fileContent.content GotParsedComposition
            )

        Err _ ->
            ( Helpers.addLog UiStrings.statusFailedToParseDriveFileContent model, Cmd.none )


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
    ( Helpers.addLog (UiStrings.statusOpeningFromDrive |> String.replace "{filename}" fileName) model
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
    , Net.saveConfigCmd { model | driveFolders = updatedFolders }
    )


handleDriveRefreshFolder : String -> Model -> ( Model, Cmd Msg )
handleDriveRefreshFolder folderId model =
    ( model, Api.GoogleDrive.listDir folderId )


handleDriveDeleteItem : String -> String -> Model -> ( Model, Cmd Msg )
handleDriveDeleteItem parentFolderId fileId model =
    ( model
    , Cmd.batch
        [ Api.GoogleDrive.deleteItem fileId
        , Api.GoogleDrive.listDir parentFolderId
        ]
    )


handleDriveWriteResult : Model -> ( Model, Cmd Msg )
handleDriveWriteResult model =
    ( Helpers.addLog UiStrings.statusFileSavedToDrive model, Cmd.none )


handleDriveError : String -> Model -> ( Model, Cmd Msg )
handleDriveError errMsg model =
    ( Helpers.addLog (UiStrings.statusDriveError |> String.replace "{message}" errMsg) model, Cmd.none )



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
