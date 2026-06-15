port module Ports exposing
    ( bugReportResult
    , clipboardContent
    , configLoaded
    , consoleError
    , copyToClipboard
    , debugCommandReceived
    , debugResponse
    , downloadFile
    , fileLoaded
    , fileSelected
    , googleDriveAuth
    , googleDriveAuthResult
    , googleDriveDeleteItem
    , googleDriveDirListing
    , googleDriveError
    , googleDriveFileContent
    , googleDriveListDir
    , googleDriveReadFile
    , googleDriveWriteResult
    , loadConfig
    , openExternalUrl
    , requestDebugConnection
    , saveConfig
    , selectFile
    , setTheme
    , submitBugReport
    , uncaughtError
    )

import Json.Decode as Decode



-- OUTGOING PORTS (Elm -> JS)


port downloadFile :
    { filename : String
    , mimeType : String
    , content : String
    , forcePicker : Bool
    }
    -> Cmd msg


port selectFile : String -> Cmd msg


port copyToClipboard : String -> Cmd msg


{-| Open an external URL in a new tab. JS handler in `ports.js` uses
`window.open(url, '_blank', 'noopener,noreferrer')`.
-}
port openExternalUrl : String -> Cmd msg


{-| Emit a developer-facing error message to the browser console
(`console.error`). Used for surfacing details that were previously
discarded — e.g. JSON decode errors from Drive payloads / config /
HTTP responses. The user-visible affordance stays in `statusLog`;
this port is the diagnostic trail an investigator can grep.

JS handler in `ports.js` calls `console.error("[sangeet]", message)`.

-}
port consoleError : String -> Cmd msg



-- INCOMING PORTS (JS -> Elm)


port fileSelected : (String -> msg) -> Sub msg


port fileLoaded : (String -> msg) -> Sub msg


port clipboardContent : (String -> msg) -> Sub msg



-- CONFIG PERSISTENCE (localStorage)


port saveConfig : String -> Cmd msg


port loadConfig : () -> Cmd msg


port configLoaded : (String -> msg) -> Sub msg



-- THEME PERSISTENCE (localStorage + <body data-theme>)
-- JS handler sets document.body.dataset.theme = "light"|"dark" so the
-- :root / body[data-theme="dark"] CSS variable overrides in styles.css
-- swap palettes instantly, and writes the same value to localStorage
-- under the key "sangeet:theme" so the choice survives reload. Init
-- reads the same key via a flag from index.html so the initial paint
-- already uses the right palette (no Light → Dark flash).


port setTheme : String -> Cmd msg



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



-- UNCAUGHT ERROR CAPTURE (Plan 18 PR-3c)
-- JS-side window.onerror + unhandledrejection listeners forward error
-- payloads here. The Msg handler decodes the value, builds a BugReport
-- envelope tagged source="uncaught", and POSTs it to the same
-- /api/v1/bug-reports endpoint used for user-initiated reports. Auto-send,
-- no user UI — matches PostHog's auto-event posture. See
-- docs/developer/operations/observability-and-bug-reporting.md for the
-- privacy decision and stack-trace truncation policy (8000 chars).


port uncaughtError : (Decode.Value -> msg) -> Sub msg



-- SECTION RENAME PROMPT
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


port googleDriveDeleteItem : String -> Cmd msg



-- Incoming ports (JS -> Elm) for Google Drive API results


port googleDriveAuthResult : (Decode.Value -> msg) -> Sub msg


port googleDriveDirListing : (Decode.Value -> msg) -> Sub msg


port googleDriveFileContent : (Decode.Value -> msg) -> Sub msg


port googleDriveWriteResult : (Decode.Value -> msg) -> Sub msg


port googleDriveError : (String -> msg) -> Sub msg
