port module Ports exposing
    ( bugReportResult
    , clipboardContent
    , configLoaded
    , copyToClipboard
    , downloadBinaryFile
    , downloadFile
    , fileLoaded
    , fileSelected
    , loadConfig
    , saveConfig
    , selectFile
    , submitBugReport
    )

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
