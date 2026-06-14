module Api.GoogleDrive exposing
    ( createFile
    , createFolder
    , deleteItem
    , googleDriveAuthResult
    , googleDriveDirListing
    , googleDriveError
    , googleDriveFileContent
    , googleDriveWriteResult
    , initiateAuth
    , listDir
    , readFile
    , renameItem
    )

import Json.Decode as Decode
import Json.Encode as Encode
import Ports



-- PORT DELEGATES
-- All actual port declarations live in Ports.elm. This module provides a
-- convenient API layer that matches the previous port-based interface.
-- OUTGOING PORTS (Elm -> JS)


googleDriveAuth : () -> Cmd msg
googleDriveAuth =
    Ports.googleDriveAuth


googleDriveListDir : String -> Cmd msg
googleDriveListDir =
    Ports.googleDriveListDir


googleDriveReadFile : String -> Cmd msg
googleDriveReadFile =
    Ports.googleDriveReadFile


googleDriveWriteFile : Encode.Value -> Cmd msg
googleDriveWriteFile =
    Ports.googleDriveWriteFile


googleDriveCreateFile : Encode.Value -> Cmd msg
googleDriveCreateFile =
    Ports.googleDriveCreateFile


googleDriveCreateFolder : Encode.Value -> Cmd msg
googleDriveCreateFolder =
    Ports.googleDriveCreateFolder


googleDriveRenameItem : Encode.Value -> Cmd msg
googleDriveRenameItem =
    Ports.googleDriveRenameItem


googleDriveDeleteItem : String -> Cmd msg
googleDriveDeleteItem =
    Ports.googleDriveDeleteItem


googleDriveMoveItem : Encode.Value -> Cmd msg
googleDriveMoveItem =
    Ports.googleDriveMoveItem



-- INCOMING PORTS (JS -> Elm)


googleDriveAuthResult : (Decode.Value -> msg) -> Sub msg
googleDriveAuthResult =
    Ports.googleDriveAuthResult


googleDriveDirListing : (Decode.Value -> msg) -> Sub msg
googleDriveDirListing =
    Ports.googleDriveDirListing


googleDriveFileContent : (Decode.Value -> msg) -> Sub msg
googleDriveFileContent =
    Ports.googleDriveFileContent


googleDriveWriteResult : (Decode.Value -> msg) -> Sub msg
googleDriveWriteResult =
    Ports.googleDriveWriteResult


googleDriveError : (String -> msg) -> Sub msg
googleDriveError =
    Ports.googleDriveError



-- CONVENIENCE FUNCTIONS


initiateAuth : Cmd msg
initiateAuth =
    googleDriveAuth ()


listDir : String -> Cmd msg
listDir folderId =
    googleDriveListDir folderId


readFile : String -> Cmd msg
readFile fileId =
    googleDriveReadFile fileId


createFile : { name : String, parentId : String, content : String, mimeType : String } -> Cmd msg
createFile params =
    googleDriveCreateFile
        (Encode.object
            [ ( "name", Encode.string params.name )
            , ( "parentId", Encode.string params.parentId )
            , ( "content", Encode.string params.content )
            , ( "mimeType", Encode.string params.mimeType )
            ]
        )


createFolder : { name : String, parentId : String } -> Cmd msg
createFolder params =
    googleDriveCreateFolder
        (Encode.object
            [ ( "name", Encode.string params.name )
            , ( "parentId", Encode.string params.parentId )
            ]
        )


renameItem : { fileId : String, newName : String } -> Cmd msg
renameItem params =
    googleDriveRenameItem
        (Encode.object
            [ ( "fileId", Encode.string params.fileId )
            , ( "newName", Encode.string params.newName )
            ]
        )


deleteItem : String -> Cmd msg
deleteItem fileId =
    googleDriveDeleteItem fileId
