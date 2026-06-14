port module Ports exposing
    ( bugReportResult
    , clipboardContent
    , configLoaded
    , copyToClipboard
    , debugCommandReceived
    , debugResponse
    , downloadBinaryFile
    , downloadFile
    , fileLoaded
    , fileSelected
    , googleDriveAuth
    , googleDriveAuthResult
    , googleDriveCreateFile
    , googleDriveCreateFolder
    , googleDriveDeleteItem
    , googleDriveDirListing
    , googleDriveError
    , googleDriveFileContent
    , googleDriveListDir
    , googleDriveMoveItem
    , googleDriveReadFile
    , googleDriveRenameItem
    , googleDriveWriteFile
    , googleDriveWriteResult
    , loadConfig
    , openExternalUrl
    , renameSectionConfirmed
    , requestDebugConnection
    , requestRenameSection
    , saveConfig
    , selectFile
    , submitBugReport
    )

import Json.Decode as Decode
import Json.Encode exposing (Value)



-- OUTGOING PORTS (Elm -> JS)


port downloadFile :
    { filename : String
    , mimeType : String
    , content : String
    }
    -> Cmd msg


port downloadBinaryFile : Value -> Cmd msg


port selectFile : String -> Cmd msg


port copyToClipboard : String -> Cmd msg


{-| Open an external URL in a new tab. JS handler in `ports.js` uses
`window.open(url, '_blank', 'noopener,noreferrer')`.
-}
port openExternalUrl : String -> Cmd msg



-- INCOMING PORTS (JS -> Elm)


port fileSelected : (String -> msg) -> Sub msg


port fileLoaded : (String -> msg) -> Sub msg


port clipboardContent : (String -> msg) -> Sub msg



-- CONFIG PERSISTENCE (localStorage)


port saveConfig : String -> Cmd msg


port loadConfig : () -> Cmd msg


port configLoaded : (String -> msg) -> Sub msg



-- BUG REPORTS (Phase 4b)
-- Outbound carries the user-provided fields + the API URL; JS gathers the
-- rrweb replay buffer + metadata before POSTing.


port submitBugReport :
    { description : String
    , email : String
    , apiBaseUrl : String
    }
    -> Cmd msg


port bugReportResult : ({ success : Bool, message : String } -> msg) -> Sub msg



-- SECTION RENAME PROMPT
-- Desktop opens a native TextInputDialog; web has no native equivalent in Elm,
-- so we round-trip through window.prompt via JS. Two dedicated ports keep this
-- self-contained (no generic "pending prompt" state machine in Model).


port requestRenameSection : { sectionIndex : Int, currentName : String } -> Cmd msg


port renameSectionConfirmed : ({ sectionIndex : Int, newName : String } -> msg) -> Sub msg



-- DEBUG BRIDGE
-- Gated by URL param presence (?debug=ws://localhost:PORT). JS in ports.js
-- opens the WebSocket and forwards messages in both directions. Production
-- bundles WITHOUT the param simply never call requestDebugConnection.


port requestDebugConnection : String -> Cmd msg


port debugCommandReceived : (Decode.Value -> msg) -> Sub msg


port debugResponse : { id : String, result : Decode.Value, error : Maybe String } -> Cmd msg



-- GOOGLE DRIVE INTEGRATION
-- Outgoing ports (Elm -> JS) for Google Drive API operations


port googleDriveAuth : () -> Cmd msg


port googleDriveListDir : String -> Cmd msg


port googleDriveReadFile : String -> Cmd msg


port googleDriveWriteFile : Value -> Cmd msg


port googleDriveCreateFile : Value -> Cmd msg


port googleDriveCreateFolder : Value -> Cmd msg


port googleDriveRenameItem : Value -> Cmd msg


port googleDriveDeleteItem : String -> Cmd msg


port googleDriveMoveItem : Value -> Cmd msg



-- Incoming ports (JS -> Elm) for Google Drive API results


port googleDriveAuthResult : (Decode.Value -> msg) -> Sub msg


port googleDriveDirListing : (Decode.Value -> msg) -> Sub msg


port googleDriveFileContent : (Decode.Value -> msg) -> Sub msg


port googleDriveWriteResult : (Decode.Value -> msg) -> Sub msg


port googleDriveError : (String -> msg) -> Sub msg
