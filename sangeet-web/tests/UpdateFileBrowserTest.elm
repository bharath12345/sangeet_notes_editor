module UpdateFileBrowserTest exposing (configPersistenceTests, driveAuthTests, driveFolderTests, fileBrowserToggleTests, suite)

import Expect
import Json.Encode as Encode
import State.Model exposing (DriveState(..), FolderState)
import State.Msg exposing (Msg(..))
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultModel)


suite : Test
suite =
    describe "File browser and Drive"
        [ fileBrowserToggleTests
        , driveAuthTests
        , driveFolderTests
        , configPersistenceTests
        ]


fileBrowserToggleTests : Test
fileBrowserToggleTests =
    describe "ToggleFileBrowser"
        [ test "ToggleFileBrowser expands when collapsed" <|
            \_ ->
                let
                    model =
                        { defaultModel | fileBrowserCollapsed = True }

                    ( newModel, _ ) =
                        update ToggleFileBrowser model
                in
                Expect.equal False newModel.fileBrowserCollapsed
        , test "ToggleFileBrowser collapses when expanded" <|
            \_ ->
                let
                    model =
                        { defaultModel | fileBrowserCollapsed = False }

                    ( newModel, _ ) =
                        update ToggleFileBrowser model
                in
                Expect.equal True newModel.fileBrowserCollapsed
        ]


driveAuthTests : Test
driveAuthTests =
    describe "Drive authentication"
        [ test "ConnectDrive sets state to DriveConnecting" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update ConnectDrive defaultModel
                in
                Expect.equal DriveConnecting newModel.driveState
        , test "ConnectDrive expands file browser" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update ConnectDrive defaultModel
                in
                Expect.equal False newModel.fileBrowserCollapsed
        , test "GotDriveAuthResult success sets DriveConnected" <|
            \_ ->
                let
                    value =
                        Encode.object [ ( "success", Encode.bool True ) ]

                    model =
                        { defaultModel | driveState = DriveConnecting }

                    ( newModel, _ ) =
                        update (GotDriveAuthResult value) model
                in
                Expect.equal DriveConnected newModel.driveState
        , test "GotDriveAuthResult success adds log" <|
            \_ ->
                let
                    value =
                        Encode.object [ ( "success", Encode.bool True ) ]

                    model =
                        { defaultModel | driveState = DriveConnecting }

                    ( newModel, _ ) =
                        update (GotDriveAuthResult value) model
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "Connected" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        , test "GotDriveAuthResult failure sets DriveDisconnected" <|
            \_ ->
                let
                    value =
                        Encode.object [ ( "success", Encode.bool False ) ]

                    model =
                        { defaultModel | driveState = DriveConnecting }

                    ( newModel, _ ) =
                        update (GotDriveAuthResult value) model
                in
                Expect.equal DriveDisconnected newModel.driveState
        , test "GotDriveError adds error to log" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (GotDriveError "Token expired") defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "Token expired" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        ]


driveFolderTests : Test
driveFolderTests =
    describe "Drive folder operations"
        [ test "GotDriveDirListing adds folder to list" <|
            \_ ->
                let
                    value =
                        Encode.object
                            [ ( "folderId", Encode.string "root" )
                            , ( "folderName", Encode.string "My Drive" )
                            , ( "items"
                              , Encode.list identity
                                    [ Encode.object
                                        [ ( "id", Encode.string "file1" )
                                        , ( "name", Encode.string "test.swar" )
                                        , ( "mimeType", Encode.string "application/json" )
                                        ]
                                    ]
                              )
                            ]

                    ( newModel, _ ) =
                        update (GotDriveDirListing value) defaultModel
                in
                Expect.equal 1 (List.length newModel.driveFolders)
        , test "GotDriveDirListing sets folder expanded" <|
            \_ ->
                let
                    value =
                        Encode.object
                            [ ( "folderId", Encode.string "root" )
                            , ( "folderName", Encode.string "My Drive" )
                            , ( "items", Encode.list identity [] )
                            ]

                    ( newModel, _ ) =
                        update (GotDriveDirListing value) defaultModel

                    expanded =
                        newModel.driveFolders
                            |> List.head
                            |> Maybe.map .expanded
                in
                Expect.equal (Just True) expanded
        , test "GotDriveDirListing updates existing folder" <|
            \_ ->
                let
                    existingFolder : FolderState
                    existingFolder =
                        { folderId = "root"
                        , name = "My Drive"
                        , items = []
                        , expanded = False
                        , isBookmarked = True
                        }

                    model =
                        { defaultModel | driveFolders = [ existingFolder ] }

                    value =
                        Encode.object
                            [ ( "folderId", Encode.string "root" )
                            , ( "folderName", Encode.string "My Drive" )
                            , ( "items"
                              , Encode.list identity
                                    [ Encode.object
                                        [ ( "id", Encode.string "f1" )
                                        , ( "name", Encode.string "folder1" )
                                        , ( "mimeType", Encode.string "application/vnd.google-apps.folder" )
                                        ]
                                    ]
                              )
                            ]

                    ( newModel, _ ) =
                        update (GotDriveDirListing value) model
                in
                Expect.equal 1 (List.length newModel.driveFolders)
        , test "GotDriveDirListing preserves bookmark on existing folder" <|
            \_ ->
                let
                    existingFolder : FolderState
                    existingFolder =
                        { folderId = "root"
                        , name = "My Drive"
                        , items = []
                        , expanded = False
                        , isBookmarked = True
                        }

                    model =
                        { defaultModel | driveFolders = [ existingFolder ] }

                    value =
                        Encode.object
                            [ ( "folderId", Encode.string "root" )
                            , ( "folderName", Encode.string "My Drive" )
                            , ( "items", Encode.list identity [] )
                            ]

                    ( newModel, _ ) =
                        update (GotDriveDirListing value) model

                    bookmarked =
                        newModel.driveFolders
                            |> List.head
                            |> Maybe.map .isBookmarked
                in
                Expect.equal (Just True) bookmarked
        , test "DriveOpenFolder toggles expanded on loaded folder" <|
            \_ ->
                let
                    folder : FolderState
                    folder =
                        { folderId = "abc123"
                        , name = "Test"
                        , items = []
                        , expanded = True
                        , isBookmarked = False
                        }

                    model =
                        { defaultModel | driveFolders = [ folder ] }

                    ( newModel, _ ) =
                        update (DriveOpenFolder "abc123") model

                    isExpanded =
                        newModel.driveFolders
                            |> List.head
                            |> Maybe.map .expanded
                in
                Expect.equal (Just False) isExpanded
        , test "DriveToggleBookmark toggles bookmark state" <|
            \_ ->
                let
                    folder : FolderState
                    folder =
                        { folderId = "abc123"
                        , name = "Test"
                        , items = []
                        , expanded = False
                        , isBookmarked = False
                        }

                    model =
                        { defaultModel | driveFolders = [ folder ] }

                    ( newModel, _ ) =
                        update (DriveToggleBookmark "abc123") model

                    bookmarked =
                        newModel.driveFolders
                            |> List.head
                            |> Maybe.map .isBookmarked
                in
                Expect.equal (Just True) bookmarked
        , test "DriveOpenFile adds log entry" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (DriveOpenFile "file1" "Yaman.swar") defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "Yaman.swar" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        , test "GotDriveWriteResult adds log" <|
            \_ ->
                let
                    value =
                        Encode.object
                            [ ( "fileId", Encode.string "file1" )
                            , ( "success", Encode.bool True )
                            ]

                    ( newModel, _ ) =
                        update (GotDriveWriteResult value) defaultModel
                in
                case newModel.statusLog of
                    first :: _ ->
                        Expect.equal True (String.contains "saved" first || String.contains "Drive" first)

                    [] ->
                        Expect.fail "statusLog should not be empty"
        ]


configPersistenceTests : Test
configPersistenceTests =
    describe "Config persistence"
        [ test "GotConfigLoaded with valid config restores fileBrowserCollapsed" <|
            \_ ->
                let
                    configJson =
                        "{\"fileBrowserCollapsed\":false,\"fileBrowserWidth\":300.0}"

                    ( newModel, _ ) =
                        update (GotConfigLoaded configJson) defaultModel
                in
                Expect.equal False newModel.fileBrowserCollapsed
        , test "GotConfigLoaded with valid config restores fileBrowserWidth" <|
            \_ ->
                let
                    configJson =
                        "{\"fileBrowserCollapsed\":false,\"fileBrowserWidth\":300.0}"

                    ( newModel, _ ) =
                        update (GotConfigLoaded configJson) defaultModel
                in
                Expect.within (Expect.Absolute 0.01) 300.0 newModel.fileBrowserWidth
        , test "GotConfigLoaded with empty JSON uses defaults" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (GotConfigLoaded "{}") defaultModel
                in
                Expect.equal True newModel.fileBrowserCollapsed
        , test "GotConfigLoaded with invalid JSON is no-op" <|
            \_ ->
                let
                    ( newModel, _ ) =
                        update (GotConfigLoaded "not json") defaultModel
                in
                Expect.equal defaultModel newModel
        , test "GotConfigLoaded with partial JSON uses defaults for missing" <|
            \_ ->
                let
                    configJson =
                        "{\"fileBrowserCollapsed\":false}"

                    ( newModel, _ ) =
                        update (GotConfigLoaded configJson) defaultModel
                in
                Expect.all
                    [ \m -> Expect.equal False m.fileBrowserCollapsed
                    , \m -> Expect.within (Expect.Absolute 0.01) 250.0 m.fileBrowserWidth
                    ]
                    newModel
        ]
