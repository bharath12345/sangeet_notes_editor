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
    , loadConfig
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
