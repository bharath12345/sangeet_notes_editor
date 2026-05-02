port module Ports exposing
    ( playNotes
    , stopPlayback
    , downloadFile
    , downloadBinaryFile
    , exportPdf
    , selectFile
    , fileSelected
    , fileLoaded
    )

{-| Port module for JavaScript interop.

This module provides ports for:
  - Web Audio playback (playNotes, stopPlayback)
  - File downloads (downloadFile for text, downloadBinaryFile for PDF)
  - PDF export (exportPdf - uses fetch to get binary from server)
  - File uploads (selectFile, fileSelected, fileLoaded)

-}

import Json.Encode exposing (Value)


-- OUTGOING PORTS (Elm -> JS)


{-| Send timed notes to JavaScript for Web Audio playback.
-}
port playNotes : Value -> Cmd msg


{-| Stop all currently playing audio.
-}
port stopPlayback : () -> Cmd msg


{-| Download a text file (e.g., .swar JSON, .html).
-}
port downloadFile :
    { filename : String
    , mimeType : String
    , content : String
    }
    -> Cmd msg


{-| Download a binary file (e.g., PDF).
Expects JSON value with: { filename, mimeType, bytes: [array of byte values] }
-}
port downloadBinaryFile : Value -> Cmd msg


{-| Export PDF via server endpoint.
Expects: { apiBaseUrl, composition, script, landscape, filename }
JS will fetch() the PDF bytes and trigger download.
-}
port exportPdf : Value -> Cmd msg


{-| Trigger file picker dialog.
Expects file extension filter, e.g., ".swar"
-}
port selectFile : String -> Cmd msg


-- INCOMING PORTS (JS -> Elm)


{-| Receive notification that a file was selected (with filename).
-}
port fileSelected : (String -> msg) -> Sub msg


{-| Receive the file content after it has been loaded.
-}
port fileLoaded : (String -> msg) -> Sub msg
