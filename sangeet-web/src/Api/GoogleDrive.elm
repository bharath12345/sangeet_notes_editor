module Api.GoogleDrive exposing
    ( deleteItem
    , googleDriveAuthResult
    , googleDriveDirListing
    , googleDriveError
    , googleDriveFileContent
    , googleDriveWriteResult
    , initiateAuth
    , listDir
    , readFile
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


googleDriveDeleteItem : String -> Cmd msg
googleDriveDeleteItem =
    Ports.googleDriveDeleteItem



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


deleteItem : String -> Cmd msg
deleteItem fileId =
    googleDriveDeleteItem fileId
